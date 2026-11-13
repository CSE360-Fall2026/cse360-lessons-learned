package guiCuratorRefine;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import database.Database;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.RefinementDraft;
import entityClasses.User;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;
import guiTools.LessonResult;

/**
 * HW3/TP3-1 risk-reduction prototype for Aspect 6, Epic E3: review AI-assisted
 * refinements, validate explicitly chosen final text, and atomically save text,
 * first originals, and attribution. It uses D1's Curator rule and D2's existing
 * response cleaner. The injectable provider is the connection point for Virgil's
 * future AI request/consent service, not a second provider implementation.
 *
 * No JavaFX widgets or network requests are needed to exercise the real model.
 * CuratorRefinementTests covers US-C-E3-01 through US-C-E3-08, including stale
 * reviews, role revocation, untrusted responses, failures, and locked fields.
 * The later TP3 page can keep its unsaved text when a Review reports an error.
 *
 * @author Daniel Martinez Julio
 */
public final class ModelCuratorRefine {
    /** Refusal shown before exposing lesson data to an unauthorized actor. */
    public static final String NOT_CURATOR = "Only a curator can refine lessons.";
    /** Consent must precede transmission to the D2 provider. */
    public static final String CONSENT_REQUIRED = "Accept the AI consent notice before requesting a suggestion.";
    /** Fixed provider-failure message; contains no exception details or credentials. */
    public static final String UNAVAILABLE = "The coach is unavailable right now.";
    /** The review's original values no longer match the stored revision. */
    public static final String STALE = "This lesson changed during your review. Reopen it before saving.";
    /** The atomic database save did not succeed. */
    public static final String NOT_SAVED = "The refinement could not be saved. Reopen the lesson and try again.";
    /** Cancelling or submitting unchanged text produces no curation history. */
    public static final String NO_CHANGE = "No change was made.";

    /**
     * D2 adapter boundary. Production must supply its consent-aware provider;
     * tests/demo supply deterministic responses and never send course data out.
     */
    @FunctionalInterface
    public interface SuggestionProvider {
        /**
         * Returns an untrusted suggestion for one selected field.
         * @param field title or description
         * @param original selected stored text
         * @param goal optional Curator goal
         * @return untrusted provider response
         * @throws Exception provider failure, timeout, or rate limit
         */
        String suggest(String field, String original, String goal) throws Exception;
    }

    /**
     * Result of opening/requesting, including the original for side-by-side review.
     * A failure may retain an authorized draft for manual refinement; an authorization
     * failure never returns lesson text.
     * @param success whether the open/request succeeded
     * @param message confirmation or safe refusal
     * @param failedField field for an input error, otherwise empty
     * @param draft immutable snapshot, or null when access/open failed
     * @param original selected stored text for a successful suggestion
     * @param suggestion cleaned response for review, never automatically saved
     */
    public record Review(boolean success, String message, String failedField,
            RefinementDraft draft, String original, String suggestion) { }

    private final Database database;
    private final SharedAiService cleaner;
    private final SuggestionProvider provider;
    private final Clock clock;

    /**
     * Uses the supplied application database/provider and the real clock.
     * Tested by RF-01 through RF-42 and the runnable prototype demo.
     * @param database connected application database
     * @param provider D2 adapter, or null
     */
    public ModelCuratorRefine(Database database, SuggestionProvider provider) {
        this(database, provider, Clock.systemUTC());
    }

    /**
     * Clock injection makes attribution assertions deterministic (RF-09/10/13).
     * @param database connected database
     * @param provider D2 adapter, or null
     * @param clock source of save timestamps
     */
    public ModelCuratorRefine(Database database, SuggestionProvider provider, Clock clock) {
        this.database = Objects.requireNonNull(database);
        this.provider = provider;
        this.clock = Objects.requireNonNull(clock);
        this.cleaner = new SharedAiService();
    }

    /**
     * Opens an immutable review without writing anything (RF-01/02/03).
     * Authorization is first, before revealing whether a lesson exists.
     * @param actor logged-in user
     * @param lessonId selected lesson
     * @return review or refusal
     */
    public Review openLesson(User actor, int lessonId) {
        if (!authorized(actor)) return refused(NOT_CURATOR, "", null);
        LessonsLearned lesson = database.getLessonById(lessonId);
        if (lesson == null) return refused(LessonAccess.NOT_FOUND, "", null);
        return new Review(true, "", "", RefinementDraft.fromLesson(lesson), "", "");
    }

    /**
     * Requests/cleans a suggestion after role, revision, selection, and consent checks.
     * Never persists or automatically accepts the returned text (RF-04 through RF-08,
     * RF-18 through RF-27, RF-34/35). The caller retains its own edited draft.
     * @param actor logged-in Curator
     * @param draft snapshot from openLesson
     * @param field title or description
     * @param goal optional goal, maximum 500 characters
     * @param consentAccepted whether the required D2 consent was accepted
     * @return original plus suggestion, or a safe error with the authorized snapshot
     */
    public Review requestSuggestion(User actor, RefinementDraft draft, String field,
            String goal, boolean consentAccepted) {
        if (!authorized(actor)) return refused(NOT_CURATOR, "", null);
        if (draft == null) return refused("Open a lesson before requesting a suggestion.", "", null);
        if (!supported(field)) return refused("Choose title or description.", "field", draft);
        if (!draft.matches(database.getLessonById(draft.lessonId())))
            return refused(STALE, "", draft);
        if (!consentAccepted) return refused(CONSENT_REQUIRED, "consent", draft);
        String requestedGoal = goal == null ? "" : goal.trim();
        if (requestedGoal.length() > 500 || requestedGoal.chars().anyMatch(Character::isISOControl))
            return refused("Use a refinement goal of at most 500 characters without control characters.", "goal", draft);
        if (provider == null) return refused(UNAVAILABLE, "", draft);
        String original = LessonsLearned.FIELD_TITLE.equals(field) ? draft.title() : draft.description();
        try {
            String raw = provider.suggest(field, original, requestedGoal);
            // D2's existing cleaner is reused; final field validation still happens on save.
            String suggestion = cleaner.cleanAiSuggestion(raw,
                    LessonsLearned.FIELD_TITLE.equals(field) ? 100 : 5000);
            // A slow provider may return after an Admin removes the Curator role.
            if (!authorized(actor)) return refused(NOT_CURATOR, "", null);
            if (!draft.matches(database.getLessonById(draft.lessonId())))
                return refused(STALE, "", draft);
            return new Review(true, "Review the suggestion before saving.", "", draft, original, suggestion);
        } catch (InterruptedException e) {
            Review failure = providerFailure(actor, draft);
            Thread.currentThread().interrupt();
            return failure;
        } catch (Exception e) {
            // Provider exception messages may contain request text or credentials.
            // The UI gets a fixed message and can still save a manually reviewed edit.
            return providerFailure(actor, draft);
        }
    }

    /**
     * Explicitly saves selected final title/description values (RF-09 through RF-17,
     * RF-28 through RF-40). Unselected fields and all locks remain untouched.
     * E6 locks restrict Contributors; an authorized Curator may refine locked text.
     * @param actor logged-in Curator
     * @param draft immutable review snapshot
     * @param changes selected field names and reviewed final values
     * @param confirmed true only after an explicit save action
     * @return saved lesson or a specific refusal; failures never partially save
     */
    public LessonResult saveRefinement(User actor, RefinementDraft draft,
            Map<String, String> changes, boolean confirmed) {
        if (!authorized(actor)) return LessonResult.failed(NOT_CURATOR, "");
        if (draft == null) return LessonResult.failed("Open a lesson before saving.", "");
        if (!confirmed) return LessonResult.failed(NO_CHANGE, "");
        if (!draft.matches(database.getLessonById(draft.lessonId())))
            return LessonResult.failed(STALE, "");
        if (changes == null || changes.isEmpty())
            return LessonResult.failed("Select title or description to refine.", "field");
        for (String field : changes.keySet())
            if (!supported(field)) return LessonResult.failed("Choose title or description.", "field");

        String title = draft.title();
        String description = draft.description();
        // Fixed order makes the first reported error predictable. Validate both as typed
        // and after trimming so normalization cannot turn an accepted value into invalid data.
        for (String field : new String[] { LessonsLearned.FIELD_TITLE, LessonsLearned.FIELD_DESCRIPTION }) {
            if (!changes.containsKey(field)) continue;
            String raw = changes.get(field);
            String error = LessonsLearned.validateField(field, raw);
            if (!error.isEmpty()) return LessonResult.failed(error, field);
            String value = raw.trim();
            error = LessonsLearned.validateField(field, value);
            if (!error.isEmpty()) return LessonResult.failed(error, field);
            if (LessonsLearned.FIELD_TITLE.equals(field)) title = value;
            else description = value;
        }
        if (title.equals(draft.title()) && description.equals(draft.description()))
            return LessonResult.failed(NO_CHANGE, "");

        // Do not call updateLesson() and updateLessonCuratorFields() separately:
        // failure of the second write would leave edited text without its audit/originals.
        if (!database.saveCuratorRefinement(draft, actor.getUserName(), title, description,
                Math.max(1, clock.millis()))) return LessonResult.failed(NOT_SAVED, "");
        return LessonResult.passed("Lesson refined. Edited by curator: " + actor.getUserName() + ".",
                draft.lessonId(), database.getLessonById(draft.lessonId()));
    }

    /**
     * Label for an already authorized lesson view; grants no access itself (RF-11/12).
     * @param lesson authorized stored lesson
     * @return attribution note, or empty if uncurated
     */
    public static String curationNote(LessonsLearned lesson) {
        if (lesson == null || lesson.getCuratedAt() <= 0 || lesson.getCuratedBy().isBlank()) return "";
        return "Edited by curator: " + lesson.getCuratedBy() + " at "
                + Instant.ofEpochMilli(lesson.getCuratedAt()) + ".";
    }

    private boolean authorized(User actor) {
        // Keep D1's shared session rule and additionally recheck the stored role.
        // Reading global current-user fields would overwrite someone else's session state.
        return CuratorAccess.isCurator(actor) && database.hasCuratorRole(actor.getUserName());
    }

    private Review providerFailure(User actor, RefinementDraft draft) {
        // Role revocation also takes priority when a slow provider throws instead of
        // returning. Only a still-authorized caller receives the retained review (RF-42).
        return authorized(actor) ? refused(UNAVAILABLE, "", draft) : refused(NOT_CURATOR, "", null);
    }

    private static boolean supported(String field) {
        return LessonsLearned.FIELD_TITLE.equals(field) || LessonsLearned.FIELD_DESCRIPTION.equals(field);
    }

    private static Review refused(String message, String field, RefinementDraft draft) {
        return new Review(false, message, field, draft, "", "");
    }
}
