
package guiCuratorSearchTerms;

import java.util.Objects;

import database.Database;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelCuratorSearchTerms Class </p>
 *
 * <p> Description: Supports TP3 Aspect 5, Epic E2.
 * Authorized Curators can request AI-generated search terms,
 * review and edit the suggestions, and explicitly save
 * approved terms for a lesson. </p>
 *
 * <p> Generation never modifies the database. Saved terms
 * become searchable through LessonsLearnedList.filterByKeywords().
 * The Model uses an injectable provider so tests can supply
 * deterministic responses without making external API calls. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 5
 */
public class ModelCuratorSearchTerms {

    /** Message returned when the user is not an authorized Curator. */
    public static final String NOT_CURATOR =
            "Only a Curator can manage lesson search terms.";

    /** Message returned when no AI provider is configured. */
    public static final String AI_UNAVAILABLE =
            "AI suggestions are currently unavailable.";

    /** Message returned when the lesson changed during review. */
    public static final String STALE =
            "The lesson has changed. Reopen it before saving.";

    /**
     * <p> Description: Adapter for an external AI suggestion
     * provider. Tests may supply a simulated provider. </p>
     */
    @FunctionalInterface
    public interface SuggestionProvider {

        String suggest(String title, String description, String tags)
                throws Exception;
    }

    /**
     * <p> Description: Immutable lesson snapshot used during
     * the Curator's review of search terms. </p>
     */
    public record Draft(
            int lessonId,
            String title,
            String description,
            String tags,
            String savedTerms) {
    }

    /**
     * <p> Description: Result of opening a lesson or requesting
     * suggestions. Suggestions are not automatically approved. </p>
     */
    public record Review(
            boolean success,
            String message,
            Draft draft,
            String suggestion) {
    }

    private final Database database;
    private final SharedAiService cleaner;
    private final SuggestionProvider provider;

    /**********
     * <p> Method: ModelCuratorSearchTerms(
     * Database database, SuggestionProvider provider) </p>
     *
     * <p> Description: Initializes the search-term Model using
     * the application database and an optional AI provider. </p>
     *
     * @param database the connected application database
     * @param provider the AI suggestion provider; may be null
     */
    public ModelCuratorSearchTerms(
            Database database, SuggestionProvider provider) {

        this.database = Objects.requireNonNull(database);
        this.provider = provider;
        this.cleaner = new SharedAiService();
    }

    /**********
     * <p> Method: boolean authorized(User actor) </p>
     *
     * <p> Description: Verifies the Curator role using both
     * the logged-in User and the current database record. </p>
     *
     * @param actor the user requesting the operation
     * @return true if the user is an authorized Curator
     */
    private boolean authorized(User actor) {

        return CuratorAccess.isCurator(actor)
                && database.hasCuratorRole(actor.getUserName());
    }

    /**********
     * <p> Method: Draft snapshot(LessonsLearned lesson) </p>
     *
     * <p> Description: Creates an immutable snapshot of the
     * fields used by the search-term workflow. </p>
     *
     * @param lesson the lesson to capture
     * @return the lesson snapshot
     */
    private static Draft snapshot(LessonsLearned lesson) {

        return new Draft(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getTags(),
                lesson.getSearchTerms());
    }

    /**********
     * <p> Method: boolean isCurrent(Draft draft) </p>
     *
     * <p> Description: Checks whether the lesson's searchable
     * content or saved search terms changed during review. </p>
     *
     * @param draft the previously opened lesson snapshot
     * @return true if the reviewed fields are unchanged
     */
    private boolean isCurrent(Draft draft) {

        if (draft == null) {
            return false;
        }

        LessonsLearned current =
                database.getLessonById(draft.lessonId());

        return current != null
                && Objects.equals(draft.title(), current.getTitle())
                && Objects.equals(draft.description(), current.getDescription())
                && Objects.equals(draft.tags(), current.getTags())
                && Objects.equals(
                        draft.savedTerms(), current.getSearchTerms());
    }

    /**********
     * <p> Method: Review openLesson(User actor, int lessonId) </p>
     *
     * <p> Description: Opens a lesson for Curator search-term
     * review. Authorization is checked before retrieving
     * lesson information (US-C-E2-01). </p>
     *
     * @param actor the logged-in Curator
     * @param lessonId the selected lesson identifier
     * @return a review containing the lesson snapshot
     */
    public Review openLesson(User actor, int lessonId) {

        if (!authorized(actor)) {
            return new Review(false, NOT_CURATOR, null, "");
        }

        LessonsLearned lesson = database.getLessonById(lessonId);

        if (lesson == null) {
            return new Review(
                    false, LessonAccess.NOT_FOUND, null, "");
        }

        return new Review(true, "", snapshot(lesson), "");
    }

    /**********
     * <p> Method: Review requestSuggestion(User actor,
     * Draft draft, boolean consentAccepted) </p>
     *
     * <p> Description: Requests AI-generated search terms for
     * a selected lesson (US-C-E2-01). The response is cleaned
     * and validated before being returned for review.
     * No database changes occur. </p>
     *
     * @param actor the logged-in Curator
     * @param draft the selected lesson snapshot
     * @param consentAccepted whether external AI use was approved
     * @return suggested search terms or an error message
     */
    public Review requestSuggestion(
            User actor, Draft draft, boolean consentAccepted) {

        if (!authorized(actor)) {
            return new Review(false, NOT_CURATOR, null, "");
        }

        if (draft == null) {
            return new Review(
                    false, "Select a lesson first.", null, "");
        }

        if (!isCurrent(draft)) {
            return new Review(false, STALE, draft, "");
        }

        if (!consentAccepted) {
            return new Review(
                    false, "AI consent is required.", draft, "");
        }

        if (provider == null) {
            return new Review(false, AI_UNAVAILABLE, draft, "");
        }

        try {
            String raw = provider.suggest(
                    draft.title(),
                    draft.description(),
                    draft.tags());

            String suggestion =
                    cleaner.cleanAiSuggestion(raw, 500);

            String error =
                    LessonRecognizer.checkForValidSearchTerms(
                            suggestion);

            if (!error.isEmpty()) {
                return new Review(false, error, draft, "");
            }

            if (!authorized(actor)) {
                return new Review(false, NOT_CURATOR, null, "");
            }

            if (!isCurrent(draft)) {
                return new Review(false, STALE, draft, "");
            }

            return new Review(
                    true,
                    "Review and edit the suggestions before saving.",
                    draft,
                    suggestion);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Review(false, AI_UNAVAILABLE, draft, "");

        } catch (Exception e) {
            return new Review(false, AI_UNAVAILABLE, draft, "");
        }
    }

    /**********
     * <p> Method: LessonResult saveApprovedTerms(User actor,
     * Draft draft, String approvedTerms, boolean confirmed) </p>
     *
     * <p> Description: Saves search terms explicitly approved
     * by the Curator (US-C-E2-02). The Curator may accept,
     * edit, or manually enter terms before confirming.
     * Only the searchTerms database field is updated. </p>
     *
     * @param actor the logged-in Curator
     * @param draft the lesson snapshot being reviewed
     * @param approvedTerms the final terms approved by the Curator
     * @param confirmed true only after explicit save confirmation
     * @return the result of the save operation
     */
    public LessonResult saveApprovedTerms(
            User actor,
            Draft draft,
            String approvedTerms,
            boolean confirmed) {

        if (!authorized(actor)) {
            return LessonResult.failed(NOT_CURATOR, "");
        }

        if (draft == null) {
            return LessonResult.failed(
                    "Select a lesson first.", "");
        }

        if (!confirmed) {
            return LessonResult.failed(
                    "Search terms were not saved.", "");
        }

        if (approvedTerms == null) {
            return LessonResult.failed(
                    "Search terms cannot be null.", "searchTerms");
        }

        String finalTerms = approvedTerms.trim();

        String error =
                LessonRecognizer.checkForValidSearchTerms(
                        finalTerms);

        if (!error.isEmpty()) {
            return LessonResult.failed(error, "searchTerms");
        }

        if (!isCurrent(draft)) {
            return LessonResult.failed(STALE, "");
        }

        boolean saved = database.saveCuratorSearchTerms(
                draft.lessonId(),
                actor.getUserName(),
				draft.savedTerms(),
                finalTerms);

        if (!saved) {
            return LessonResult.failed(
                    "Search terms could not be saved.", "");
        }

        LessonsLearned lesson =
                database.getLessonById(draft.lessonId());

        return LessonResult.passed(
                "Search terms saved.",
                draft.lessonId(),
                lesson);
    }
}