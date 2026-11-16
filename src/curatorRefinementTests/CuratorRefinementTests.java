package curatorRefinementTests;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.RefinementDraft;
import entityClasses.User;
import guiCreateLesson.ModelCreateLesson;
import guiCuratorRefine.ModelCuratorRefine;
import guiCuratorRefine.ModelCuratorRefine.Review;
import guiCuratorRefine.ModelCuratorRefine.SuggestionProvider;
import guiLessonDetails.ModelLessonDetails;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import guiUpdateLesson.ModelUpdateLesson;

/**
 * Real-H2 integration and model tests for TP3-1 / Aspect 6 (Epic E3).
 * RF-01..08/18..27/34..35 cover authorized, consented, untrusted AI review;
 * RF-09..17/28..33/36..38 cover validation, preservation, attribution,
 * Contributor locks, stale reviews, and all-or-nothing database saves.
 * Tests never use an external AI provider. Test-Aspect6.ps1 runs in a disposable
 * user.home so the normal FoundationDatabase is not opened.
 * @author Daniel Martinez Julio
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class CuratorRefinementTests {
    private static final Database DB = FoundationsMain.database;
    private static final String TOKEN = Long.toUnsignedString(System.nanoTime());
    private static final String OWNER = "rfOwner" + TOKEN;
    private static final User CURATOR = user("rfCurator" + TOKEN, false, false, true);
    private static final User OTHER = user("rfOther" + TOKEN, false, false, true);
    private static final String TITLE = "Deployment recovery";
    private static final String DESCRIPTION = "Restart the worker after restoring configuration.";
    private static final String SUGGESTION = "Restart the worker after restoring the validated configuration.";
    private static final long SAVE_TIME = 1_800_000_000_000L;
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(SAVE_TIME), ZoneOffset.UTC);
    private int lessonId;
    private ModelCuratorRefine model;
    private RefinementDraft draft;

    /** Connects/registers only test accounts; failures propagate to JUnit. */
    @BeforeAll static void connect() throws SQLException {
        DB.connectToDatabase();
        DB.register(CURATOR);
        DB.register(OTHER);
        DB.register(user(OWNER, false, true, false));
    }

    /** Removes accounts created by this suite and closes the real connection. */
    @AfterAll static void disconnect() {
        DB.deleteUser(CURATOR.getUserName());
        DB.deleteUser(OTHER.getUserName());
        DB.deleteUser(OWNER);
        DB.closeConnection();
    }

    /** Each case begins with a new lesson created through the Contributor model. */
    @BeforeEach void setup() {
        DB.updateUserRole(CURATOR.getUserName(), "Curator", "true");
        LessonResult made = ModelCreateLesson.createLesson(OWNER, TITLE, DESCRIPTION, "deploy", "Technical");
        assertTrue(made.isSuccess(), made.getMessage());
        lessonId = made.getLessonId();
        model = model((field, original, goal) -> SUGGESTION);
        draft = model.openLesson(CURATOR, lessonId).draft();
        assertNotNull(draft);
    }

    /** Direct database cleanup also works when the lesson remains locked. */
    @AfterEach void cleanup() { DB.deleteLesson(lessonId); }

    @Test @DisplayName("RF-01 Open a stored lesson without mutating it")
    void rf01_openSnapshot() {
        String before = state();
        Review result = model.openLesson(CURATOR, lessonId);
        assertTrue(result.success());
        assertEquals(TITLE, result.draft().title());
        assertEquals(DESCRIPTION, result.draft().description());
        assertTrue(result.draft().matches(stored()));
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-02 Null, Contributor, Admin, no-role, and unregistered actors are refused")
    void rf02_roleBoundary() {
        List<User> refused = Arrays.asList(null, user(OWNER, false, true, false),
                user("admin", true, false, false), user("none", false, false, false),
                user("unregistered", false, false, true));
        for (User actor : refused) {
            Review result = model.openLesson(actor, lessonId);
            assertFalse(result.success());
            assertEquals(ModelCuratorRefine.NOT_CURATOR, result.message());
            assertNull(result.draft());
            assertEquals(ModelCuratorRefine.NOT_CURATOR, model.openLesson(actor, -1).message());
            assertEquals(ModelCuratorRefine.NOT_CURATOR,
                    model.requestSuggestion(actor, draft, "title", "", true).message());
            assertEquals(ModelCuratorRefine.NOT_CURATOR,
                    model.saveRefinement(actor, draft, Map.of("title", "Unauthorized"), true).getMessage());
        }
    }

    @Test @DisplayName("RF-03 Missing lessons are safely refused for an authorized Curator")
    void rf03_missingLesson() {
        assertEquals(LessonAccess.NOT_FOUND, model.openLesson(CURATOR, -1).message());
    }

    @Test @DisplayName("RF-04 No consent means no provider call and no save")
    void rf04_consentBeforeTransmission() {
        AtomicInteger calls = new AtomicInteger();
        ModelCuratorRefine guarded = model((f, o, g) -> { calls.incrementAndGet(); return SUGGESTION; });
        String before = state();
        Review result = guarded.requestSuggestion(CURATOR, draft, "description", "", false);
        assertEquals(ModelCuratorRefine.CONSENT_REQUIRED, result.message());
        assertEquals(0, calls.get());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-05 Title request sends only the chosen field and shows its original")
    void rf05_titleRequest() {
        AtomicReference<String> input = new AtomicReference<>();
        Review result = model((f, o, g) -> { input.set(f + ":" + o); return "Clear recovery steps"; })
                .requestSuggestion(CURATOR, draft, "title", "", true);
        assertTrue(result.success());
        assertEquals("title:" + TITLE, input.get());
        assertEquals(TITLE, result.original());
        assertEquals("Clear recovery steps", result.suggestion());
    }

    @Test @DisplayName("RF-06 Description suggestion is cleaned, displayed, and never automatically applied")
    void rf06_descriptionRequest() {
        String before = state();
        Review result = model((f, o, g) -> "  " + SUGGESTION + "  ")
                .requestSuggestion(CURATOR, draft, "description", "clearer", true);
        assertTrue(result.success());
        assertEquals(DESCRIPTION, result.original());
        assertEquals(SUGGESTION, result.suggestion());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-07 Unknown, null, and non-E3 fields never call the provider")
    void rf07_selectedFieldBoundary() {
        AtomicInteger calls = new AtomicInteger();
        ModelCuratorRefine guarded = model((f, o, g) -> { calls.incrementAndGet(); return SUGGESTION; });
        for (String field : Arrays.asList(null, "tags", "category", "Title", "title = ?"))
            assertEquals("field", guarded.requestSuggestion(CURATOR, draft, field, "", true).failedField());
        assertEquals(0, calls.get());
    }

    @Test @DisplayName("RF-08 Optional goals are trimmed and supplied to the provider")
    void rf08_optionalGoal() {
        AtomicReference<String> goal = new AtomicReference<>();
        ModelCuratorRefine capture = model((f, o, g) -> { goal.set(g); return SUGGESTION; });
        assertTrue(capture.requestSuggestion(CURATOR, draft, "description", " clearer ", true).success());
        assertEquals("clearer", goal.get());
        assertTrue(capture.requestSuggestion(CURATOR, draft, "description", null, true).success());
        assertEquals("", goal.get());
    }

    @Test @DisplayName("RF-09 Save title and first originals with attribution in one write")
    void rf09_titleSave() {
        assertTrue(save(Map.of("title", "  Clear recovery steps  ")).isSuccess());
        LessonsLearned actual = stored();
        assertEquals("Clear recovery steps", actual.getTitle());
        assertEquals(DESCRIPTION, actual.getDescription());
        assertEquals(TITLE, actual.getOriginalTitle());
        assertEquals(DESCRIPTION, actual.getOriginalDescription());
        assertEquals(CURATOR.getUserName(), actual.getCuratedBy());
        assertEquals(SAVE_TIME, actual.getCuratedAt());
        assertEquals(SAVE_TIME, actual.getUpdatedAt());
    }

    @Test @DisplayName("RF-10 Save description without replacing the title")
    void rf10_descriptionSave() {
        assertTrue(save(Map.of("description", SUGGESTION)).isSuccess());
        assertEquals(TITLE, stored().getTitle());
        assertEquals(SUGGESTION, stored().getDescription());
    }

    @Test @DisplayName("RF-11 Saved lesson exposes an edited-by-curator note and timestamp")
    void rf11_attributionNote() {
        assertTrue(save(Map.of("title", "Recovery procedure")).isSuccess());
        String note = ModelCuratorRefine.curationNote(stored());
        assertTrue(note.contains("Edited by curator: " + CURATOR.getUserName()));
        assertTrue(note.contains(Instant.ofEpochMilli(SAVE_TIME).toString()));
    }

    @Test @DisplayName("RF-12 Uncurated and absent lessons have no attribution note")
    void rf12_noFalseAttribution() {
        assertEquals("", ModelCuratorRefine.curationNote(stored()));
        assertEquals("", ModelCuratorRefine.curationNote(null));
    }

    @Test @DisplayName("RF-13 Later Curators preserve the first originals and update attribution")
    void rf13_secondRefinement() {
        assertTrue(save(Map.of("title", "First refinement")).isSuccess());
        ModelCuratorRefine later = new ModelCuratorRefine(DB, null,
                Clock.fixed(Instant.ofEpochMilli(SAVE_TIME + 1000), ZoneOffset.UTC));
        RefinementDraft next = later.openLesson(OTHER, lessonId).draft();
        assertTrue(later.saveRefinement(OTHER, next, Map.of("description", SUGGESTION), true).isSuccess());
        assertEquals(TITLE, stored().getOriginalTitle());
        assertEquals(DESCRIPTION, stored().getOriginalDescription());
        assertEquals(OTHER.getUserName(), stored().getCuratedBy());
        assertEquals(SAVE_TIME + 1000, stored().getCuratedAt());
    }

    @Test @DisplayName("RF-14 Curator refines locked text while Contributor edit/delete stay denied")
    void rf14_locksAndContributorRegression() {
        assertTrue(DB.setLessonLocks(lessonId, List.of("entire", "title", "description", "tags", "category"), true));
        assertTrue(save(Map.of("title", "Curated locked title", "description", SUGGESTION)).isSuccess());
        LessonsLearned actual = stored();
        assertTrue(actual.isLockedEntire() && actual.isLockedTitle() && actual.isLockedDescription()
                && actual.isLockedTags() && actual.isLockedCategory());
        assertFalse(ModelUpdateLesson.updateLesson(OWNER, lessonId, "Contributor replacement",
                SUGGESTION, actual.getTags(), actual.getCategory()).isSuccess());
        assertFalse(ModelLessonDetails.deleteLesson(OWNER, lessonId, true).isSuccess());
        assertEquals("Curated locked title", stored().getTitle());
    }

    @Test @DisplayName("RF-15 Concurrent Contributor change makes the review stale")
    void rf15_staleContributorEdit() {
        assertTrue(ModelUpdateLesson.updateLesson(OWNER, lessonId, "Contributor revision",
                DESCRIPTION, "deploy", "Technical").isSuccess());
        String before = state();
        assertEquals(ModelCuratorRefine.STALE, save(Map.of("title", "Old draft overwrite")).getMessage());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-16 Database independently rejects a stale snapshot")
    void rf16_databaseCompareAndSave() {
        assertTrue(save(Map.of("title", "First saved revision")).isSuccess());
        String before = state();
        assertFalse(DB.saveCuratorRefinement(draft, CURATOR.getUserName(), "Stale overwrite", DESCRIPTION, SAVE_TIME));
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-17 Two Curators cannot silently overwrite each other's reviews")
    void rf17_overlappingCurators() {
        RefinementDraft otherDraft = model.openLesson(OTHER, lessonId).draft();
        assertTrue(save(Map.of("description", SUGGESTION)).isSuccess());
        assertEquals(ModelCuratorRefine.STALE, model.saveRefinement(OTHER, otherDraft,
                Map.of("title", "Another curator's old draft"), true).getMessage());
        assertEquals(CURATOR.getUserName(), stored().getCuratedBy());
    }

    @Test @DisplayName("RF-18 Null AI response preserves the lesson and review")
    void rf18_nullResponse() { assertBadResponse(null, "description"); }

    @Test @DisplayName("RF-19 Blank AI response cannot become a saved refinement")
    void rf19_blankResponse() { assertBadResponse("   ", "description"); }

    @Test @DisplayName("RF-20 Overlength AI output is rejected for the selected field")
    void rf20_overlengthResponse() {
        assertBadResponse("T".repeat(101), "title");
        assertBadResponse("D".repeat(5001), "description");
    }

    @Test @DisplayName("RF-21 AI control characters are rejected")
    void rf21_controlResponse() { assertBadResponse("Corrupt\u0000response", "description"); }

    @Test @DisplayName("RF-22 Provider failure never exposes its exception or credential")
    void rf22_sanitizedFailure() {
        String before = state();
        Review result = model((f, o, g) -> { throw new Exception("secret-provider-key"); })
                .requestSuggestion(CURATOR, draft, "description", "", true);
        assertEquals(ModelCuratorRefine.UNAVAILABLE, result.message());
        assertFalse(result.message().contains("secret-provider-key"));
        assertEquals(draft, result.draft());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-23 Missing provider still permits a manually reviewed refinement")
    void rf23_manualFallback() {
        ModelCuratorRefine manual = model(null);
        assertEquals(ModelCuratorRefine.UNAVAILABLE,
                manual.requestSuggestion(CURATOR, draft, "description", "", true).message());
        assertTrue(manual.saveRefinement(CURATOR, draft, Map.of("description", SUGGESTION), true).isSuccess());
    }

    @Test @DisplayName("RF-24 Explicit retry uses the preserved review and still does not save")
    void rf24_retry() {
        AtomicInteger calls = new AtomicInteger();
        ModelCuratorRefine retry = model((f, o, g) -> {
            if (calls.incrementAndGet() == 1) throw new Exception("timeout");
            return SUGGESTION;
        });
        String before = state();
        Review failed = retry.requestSuggestion(CURATOR, draft, "description", "", true);
        assertFalse(failed.success());
        assertTrue(retry.requestSuggestion(CURATOR, failed.draft(), "description", "", true).success());
        assertEquals(2, calls.get());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-25 Cancel after a suggestion leaves all text and audit data unchanged")
    void rf25_cancel() {
        String before = state();
        assertTrue(model.requestSuggestion(CURATOR, draft, "description", "", true).success());
        assertEquals(ModelCuratorRefine.NO_CHANGE,
                model.saveRefinement(CURATOR, draft, Map.of("description", SUGGESTION), false).getMessage());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-26 Invalid goals are refused before a provider call")
    void rf26_invalidGoal() {
        AtomicInteger calls = new AtomicInteger();
        ModelCuratorRefine guarded = model((f, o, g) -> { calls.incrementAndGet(); return SUGGESTION; });
        for (String goal : List.of("x".repeat(501), "bad\u0000goal"))
            assertEquals("goal", guarded.requestSuggestion(CURATOR, draft, "description", goal, true).failedField());
        assertEquals(0, calls.get());
    }

    @Test @DisplayName("RF-27 Displayed AI text must still pass normal final-field validation")
    void rf27_suggestionIsNotTrusted() {
        Review result = model((f, o, g) -> "short").requestSuggestion(CURATOR, draft, "description", "", true);
        assertTrue(result.success());
        String before = state();
        LessonResult saved = save(Map.of("description", result.suggestion()));
        assertFalse(saved.isSuccess());
        assertEquals("description", saved.getFailedField());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-28 Revoked Curator role blocks AI requests despite stale session flags")
    void rf28_revokeBeforeRequest() {
        AtomicInteger calls = new AtomicInteger();
        ModelCuratorRefine guarded = model((f, o, g) -> { calls.incrementAndGet(); return SUGGESTION; });
        DB.updateUserRole(CURATOR.getUserName(), "Curator", "false");
        assertTrue(CURATOR.getNewRole2()); // The old session must not defeat the stored role.
        Review result = guarded.requestSuggestion(CURATOR, draft, "description", "", true);
        assertEquals(ModelCuratorRefine.NOT_CURATOR, result.message());
        assertNull(result.draft());
        assertEquals(0, calls.get());
    }

    @Test @DisplayName("RF-29 Revoked role blocks final save without any content/audit change")
    void rf29_revokeBeforeSave() {
        String before = state();
        DB.updateUserRole(CURATOR.getUserName(), "Curator", "false");
        assertEquals(ModelCuratorRefine.NOT_CURATOR, save(Map.of("title", "Unauthorized edit")).getMessage());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-30 Invalid second field prevents both text changes and attribution")
    void rf30_validateWholeChange() {
        String before = state();
        LessonResult result = save(Map.of("title", "Valid new title", "description", "short"));
        assertFalse(result.isSuccess());
        assertEquals("description", result.getFailedField());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-31 SQL write failure cannot leave title changed without originals/audit")
    void rf31_sqlFailureIsAtomic() {
        String before = state();
        // Bypass the Model deliberately to force a real database column-length failure.
        assertFalse(DB.saveCuratorRefinement(draft, CURATOR.getUserName(), "Must not persist",
                "x".repeat(5001), SAVE_TIME));
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-32 E3 saves preserve other aspects' data and original ownership")
    void rf32_unrelatedFields() {
        LessonsLearned seed = stored();
        seed.setRootCause("Configuration was not checked.");
        seed.setRecommendation("Validate configuration before deployment.");
        seed.setAiAssisted(true);
        assertTrue(DB.updateLesson(seed));
        seed.setSearchTerms("recovery deployment");
        seed.setSummaryId(42);
        seed.setStatsVisibleToContributor(true);
        assertTrue(DB.updateLessonCuratorFields(seed));
        draft = model.openLesson(CURATOR, lessonId).draft();
        assertTrue(save(Map.of("title", "Reviewed recovery")).isSuccess());
        LessonsLearned actual = stored();
        assertEquals(OWNER, actual.getOwnerUserName());
        assertEquals(seed.getCreatedAt(), actual.getCreatedAt());
        assertEquals(seed.getTags(), actual.getTags());
        assertEquals(seed.getCategory(), actual.getCategory());
        assertEquals(seed.getRootCause(), actual.getRootCause());
        assertEquals(seed.getRecommendation(), actual.getRecommendation());
        assertEquals(seed.isAiAssisted(), actual.isAiAssisted());
        assertEquals(seed.getSearchTerms(), actual.getSearchTerms());
        assertEquals(42, actual.getSummaryId());
        assertTrue(actual.isStatsVisibleToContributor());
    }

    @Test @DisplayName("RF-33 Unchanged text creates no false curation history")
    void rf33_noOp() {
        String before = state();
        assertEquals(ModelCuratorRefine.NO_CHANGE, save(Map.of("title", TITLE)).getMessage());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-34 Role removed during a provider call prevents returning lesson text")
    void rf34_revokeDuringProvider() {
        Review result = model((f, o, g) -> {
            DB.updateUserRole(CURATOR.getUserName(), "Curator", "false");
            return SUGGESTION;
        }).requestSuggestion(CURATOR, draft, "description", "", true);
        assertEquals(ModelCuratorRefine.NOT_CURATOR, result.message());
        assertNull(result.draft());
        assertEquals("", result.suggestion());
    }

    @Test @DisplayName("RF-35 Lesson changed during a provider call is reported as stale")
    void rf35_changeDuringProvider() {
        Review result = model((f, o, g) -> {
            LessonsLearned changed = stored();
            changed.setTitle("Changed during AI request");
            assertTrue(DB.updateLesson(changed));
            return SUGGESTION;
        }).requestSuggestion(CURATOR, draft, "description", "", true);
        assertEquals(ModelCuratorRefine.STALE, result.message());
        assertEquals("Changed during AI request", stored().getTitle());
        assertEquals("", stored().getCuratedBy());
    }

    @Test @DisplayName("RF-36 Database save independently rechecks the current Curator role")
    void rf36_databaseRoleGuard() {
        String before = state();
        DB.updateUserRole(CURATOR.getUserName(), "Curator", "false");
        assertFalse(DB.saveCuratorRefinement(draft, CURATOR.getUserName(), "Must be refused", DESCRIPTION, SAVE_TIME));
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-37 Missing reviews, empty changes, null and unsupported fields are refused")
    void rf37_malformedSaveInputs() {
        String before = state();
        assertFalse(model.saveRefinement(CURATOR, null, Map.of("title", "New"), true).isSuccess());
        assertFalse(save(null).isSuccess());
        assertFalse(save(Map.of()).isSuccess());
        assertFalse(save(Map.of("tags", "must not save")).isSuccess());
        Map<String, String> invalid = new HashMap<>();
        invalid.put(null, "value");
        assertFalse(save(invalid).isSuccess());
        invalid.clear();
        invalid.put("title", null);
        assertFalse(save(invalid).isSuccess());
        assertFalse(model.requestSuggestion(CURATOR, null, "title", "", true).success());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-38 Curator's edited final wording, including SQL-like text, is saved literally")
    void rf38_reviewedFinalText() {
        assertTrue(model.requestSuggestion(CURATOR, draft, "description", "", true).success());
        String reviewed = "Curator's checked wording; DROP TABLE userDB; -- is ordinary lesson text.";
        assertTrue(save(Map.of("description", reviewed)).isSuccess());
        assertEquals(reviewed, stored().getDescription());
        assertEquals(DESCRIPTION, stored().getOriginalDescription());
        assertTrue(DB.hasCuratorRole(CURATOR.getUserName()));
    }

    @Test @DisplayName("RF-39 Failed storage write returns a failure, not a success confirmation")
    void rf39_modelWriteFailure() {
        String before = state();
        Database unavailableWriter = new Database() {
            @Override public boolean hasCuratorRole(String name) { return DB.hasCuratorRole(name); }
            @Override public LessonsLearned getLessonById(int id) { return DB.getLessonById(id); }
            @Override public boolean saveCuratorRefinement(RefinementDraft expected, String actor,
                    String title, String description, long when) { return false; }
        };
        LessonResult result = new ModelCuratorRefine(unavailableWriter, null, CLOCK)
                .saveRefinement(CURATOR, draft, Map.of("title", "Must not claim success"), true);
        assertFalse(result.isSuccess());
        assertEquals(ModelCuratorRefine.NOT_SAVED, result.getMessage());
        assertEquals(before, state());
    }

    @Test @DisplayName("RF-40 Normal field boundaries and controls apply to the final Curator text")
    void rf40_finalValidationBoundaries() {
        for (String title : List.of(" ", "T".repeat(101), "Bad\u0000title")) {
            String before = state();
            assertEquals("title", save(Map.of("title", title)).getFailedField());
            assertEquals(before, state());
        }
        assertTrue(save(Map.of("title", "T".repeat(100))).isSuccess());
        draft = model.openLesson(CURATOR, lessonId).draft();
        for (String description : List.of("short", "D".repeat(5001), "Bad\u0000description", "         a ")) {
            String before = state();
            assertEquals("description", save(Map.of("description", description)).getFailedField());
            assertEquals(before, state());
        }
        assertTrue(save(Map.of("description", "D".repeat(10))).isSuccess());
        draft = model.openLesson(CURATOR, lessonId).draft();
        assertTrue(save(Map.of("description", "D".repeat(5000))).isSuccess());
    }

    @Test @DisplayName("RF-41 Interrupted provider preserves the interrupt and leaves the lesson untouched")
    void rf41_interruptedRequest() {
        String before = state();
        try {
            Review result = model((f, o, g) -> { throw new InterruptedException(); })
                    .requestSuggestion(CURATOR, draft, "description", "", true);
            assertEquals(ModelCuratorRefine.UNAVAILABLE, result.message());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(draft, result.draft());
            assertEquals(before, state());
        } finally {
            // Do not let this intentional interruption affect the following JUnit tests.
            Thread.interrupted();
        }
    }

    @Test @DisplayName("RF-42 Role revoked during a failing provider call does not return a retained lesson")
    void rf42_revokeDuringProviderFailure() {
        Review result = model((f, o, g) -> {
            DB.updateUserRole(CURATOR.getUserName(), "Curator", "false");
            throw new Exception("provider failed after role removal");
        }).requestSuggestion(CURATOR, draft, "description", "", true);
        assertEquals(ModelCuratorRefine.NOT_CURATOR, result.message());
        assertNull(result.draft());
        assertEquals("", result.original());
        assertEquals("", result.suggestion());
    }

    private static User user(String name, boolean admin, boolean contributor, boolean curator) {
        return new User(name, "Example1!", "", "", "", "", "", admin, contributor, curator);
    }

    private ModelCuratorRefine model(SuggestionProvider provider) {
        return new ModelCuratorRefine(DB, provider, CLOCK);
    }

    private LessonResult save(Map<String, String> changes) {
        return model.saveRefinement(CURATOR, draft, changes, true);
    }

    private LessonsLearned stored() { return DB.getLessonById(lessonId); }

    private String state() {
        LessonsLearned lesson = stored();
        return List.of(lesson.getTitle(), lesson.getDescription(), lesson.getOriginalTitle(),
                lesson.getOriginalDescription(), lesson.getCuratedBy(), lesson.getCuratedAt(),
                lesson.getUpdatedAt(), lesson.getOwnerUserName(), lesson.isLockedEntire(),
                lesson.isLockedTitle(), lesson.isLockedDescription(), lesson.isLockedTags(),
                lesson.isLockedCategory()).toString();
    }

    private void assertBadResponse(String raw, String field) {
        String before = state();
        Review result = model((f, o, g) -> raw).requestSuggestion(CURATOR, draft, field, "", true);
        assertFalse(result.success());
        assertEquals(ModelCuratorRefine.UNAVAILABLE, result.message());
        assertEquals(draft, result.draft());
        assertEquals(before, state());
    }
}
