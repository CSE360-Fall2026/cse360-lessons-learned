
package curatorTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiCuratorSearchTerms.ModelCuratorSearchTerms;
import guiCuratorSearchTerms.ModelCuratorSearchTerms.Draft;
import guiCuratorSearchTerms.ModelCuratorSearchTerms.Review;
import guiTools.LessonResult;

/*******
 * <p> Title: CuratorSearchTermsTests Class </p>
 *
 * <p> Description: Tests the Curator search-term workflow
 * for TP3 Aspect 5. Verifies authorization, lesson review,
 * AI suggestion handling, explicit approval, persistence,
 * and protection against outdated edits. </p>
 *
 * <p> Tests use separate in-memory H2 databases and simulated
 * AI providers. No external AI service is contacted. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-16 Initial Curator workflow tests
 */
public class CuratorSearchTermsTests {

    private Database database;
    private User curator;
    private User contributor;
    private int lessonId;

    /**********
     * <p> Method: setUp() </p>
     *
     * <p> Description: Creates an isolated H2 database,
     * registers test accounts, and inserts a sample lesson. </p>
     *
     * @throws SQLException if database setup fails
     */
    @BeforeEach
    public void setUp() throws SQLException {

        String name = "tp3_curator_" +
                UUID.randomUUID().toString().replace("-", "");

        database = new Database("jdbc:h2:mem:" + name);
        database.connectToDatabase();

        curator = new User(
                "testCurator", "Password123!",
                "Test", "", "Curator", "", "",
                false, false, true, false);

        contributor = new User(
                "testContributor", "Password123!",
                "Test", "", "Contributor", "", "",
                false, true, false, false);

        database.register(curator);
        database.register(contributor);

        LessonsLearned lesson = new LessonsLearned(
                contributor.getUserName(),
                "Java Exception Handling",
                "Learning how to handle errors in Java.",
                "java, exceptions",
                "Other");

        lessonId = database.createLesson(lesson);

        assertTrue(lessonId > 0);
    }

    /**********
     * <p> Method: tearDown() </p>
     *
     * <p> Description: Closes the test database connection. </p>
     */
    @AfterEach
    public void tearDown() {

        if (database != null) {
            database.closeConnection();
        }
    }

    /**********
     * <p> Method: createModel(SuggestionProvider provider) </p>
     *
     * <p> Description: Creates the Curator Model with an
     * optional simulated AI suggestion provider. </p>
     *
     * @param provider the suggestion provider, or null
     * @return the Curator Model
     */
    private ModelCuratorSearchTerms createModel(
            ModelCuratorSearchTerms.SuggestionProvider provider) {

        return new ModelCuratorSearchTerms(database, provider);
    }

    /**********
     * <p> Method: openDraft(ModelCuratorSearchTerms model) </p>
     *
     * <p> Description: Opens the sample lesson and verifies
     * that a valid review snapshot was created. </p>
     *
     * @param model the Curator Model
     * @return the lesson review draft
     */
    private Draft openDraft(ModelCuratorSearchTerms model) {

        Review review = model.openLesson(curator, lessonId);

        assertTrue(review.success(), review.message());
        assertNotNull(review.draft());

        return review.draft();
    }

    /**********
     * <p> Method: testCuratorCanOpenLesson() </p>
     *
     * <p> Description: Verifies that an authorized Curator
     * can open a lesson for search-term review. </p>
     */
    @Test
    public void testCuratorCanOpenLesson() {

        ModelCuratorSearchTerms model = createModel(null);

        Review review = model.openLesson(curator, lessonId);

        assertTrue(review.success());
        assertNotNull(review.draft());
        assertEquals(lessonId, review.draft().lessonId());
        assertEquals(
                "Java Exception Handling",
                review.draft().title());
    }

    /**********
     * <p> Method: testContributorCannotOpenLesson() </p>
     *
     * <p> Description: Verifies that a Contributor cannot
     * access the Curator search-term workflow. </p>
     */
    @Test
    public void testContributorCannotOpenLesson() {

        ModelCuratorSearchTerms model = createModel(null);

        Review review = model.openLesson(
                contributor, lessonId);

        assertFalse(review.success());
        assertEquals(
                ModelCuratorSearchTerms.NOT_CURATOR,
                review.message());
    }

    /**********
     * <p> Method: testCuratorCanManuallySaveTerms() </p>
     *
     * <p> Description: Verifies that a Curator can manually
     * enter, approve, and save search terms. </p>
     */
    @Test
    public void testCuratorCanManuallySaveTerms() {

        ModelCuratorSearchTerms model = createModel(null);
        Draft draft = openDraft(model);

        LessonResult result = model.saveApprovedTerms(
                curator,
                draft,
                "debugging, troubleshooting",
                true);

        assertTrue(result.isSuccess(), result.getMessage());

        LessonsLearned saved =
                database.getLessonById(lessonId);

        assertEquals(
                "debugging, troubleshooting",
                saved.getSearchTerms());
    }

    /**********
     * <p> Method: testSaveRequiresConfirmation() </p>
     *
     * <p> Description: Verifies that search terms cannot
     * be saved without explicit Curator confirmation. </p>
     */
    @Test
    public void testSaveRequiresConfirmation() {

        ModelCuratorSearchTerms model = createModel(null);
        Draft draft = openDraft(model);

        LessonResult result = model.saveApprovedTerms(
                curator,
                draft,
                "debugging",
                false);

        assertFalse(result.isSuccess());

        LessonsLearned saved =
                database.getLessonById(lessonId);

        assertEquals(
                draft.savedTerms(),
                saved.getSearchTerms());
    }

    /**********
     * <p> Method: testContributorCannotSaveTerms() </p>
     *
     * <p> Description: Verifies that a Contributor cannot
     * approve or save Curator search terms. </p>
     */
    @Test
    public void testContributorCannotSaveTerms() {

        ModelCuratorSearchTerms model = createModel(null);
        Draft draft = openDraft(model);

        LessonResult result = model.saveApprovedTerms(
                contributor,
                draft,
                "unauthorized",
                true);

        assertFalse(result.isSuccess());

        assertEquals(
                ModelCuratorSearchTerms.NOT_CURATOR,
                result.getMessage());
    }

    /**********
     * <p> Method: testOutdatedDraftCannotBeSaved() </p>
     *
     * <p> Description: Verifies that an outdated review
     * cannot overwrite newer search terms. </p>
     */
    @Test
    public void testOutdatedDraftCannotBeSaved() {

        ModelCuratorSearchTerms model = createModel(null);
        Draft originalDraft = openDraft(model);

        LessonResult firstSave = model.saveApprovedTerms(
                curator,
                originalDraft,
                "debugging",
                true);

        assertTrue(firstSave.isSuccess());

        LessonResult secondSave = model.saveApprovedTerms(
                curator,
                originalDraft,
                "outdated",
                true);

        assertFalse(secondSave.isSuccess());
        assertEquals(
                ModelCuratorSearchTerms.STALE,
                secondSave.getMessage());

        assertEquals(
                "debugging",
                database.getLessonById(lessonId)
                        .getSearchTerms());
    }

    /**********
     * <p> Method: testAiRequiresConsent() </p>
     *
     * <p> Description: Verifies that AI suggestions are
     * not requested unless the Curator gives consent. </p>
     */
    @Test
    public void testAiRequiresConsent() {

        ModelCuratorSearchTerms model = createModel(
                (title, description, tags) -> {
                    fail("AI provider should not be called.");
                    return "debugging";
                });

        Draft draft = openDraft(model);

        Review review = model.requestSuggestion(
                curator, draft, false);

        assertFalse(review.success());
        assertEquals(
                "AI consent is required.",
                review.message());
    }

    /**********
     * <p> Method: testUnavailableAiProvider() </p>
     *
     * <p> Description: Verifies that the Model handles
     * a missing AI suggestion provider gracefully. </p>
     */
    @Test
    public void testUnavailableAiProvider() {

        ModelCuratorSearchTerms model = createModel(null);
        Draft draft = openDraft(model);

        Review review = model.requestSuggestion(
                curator, draft, true);

        assertFalse(review.success());
        assertEquals(
                ModelCuratorSearchTerms.AI_UNAVAILABLE,
                review.message());
    }

    /**********
     * <p> Method: testAiSuggestionRequiresApproval() </p>
     *
     * <p> Description: Verifies that an AI suggestion is
     * returned for review without automatically saving it. </p>
     */
    @Test
    public void testAiSuggestionRequiresApproval() {

        ModelCuratorSearchTerms model = createModel(
                (title, description, tags) ->
                        "debugging, troubleshooting");

        Draft draft = openDraft(model);

        Review review = model.requestSuggestion(
                curator, draft, true);

        assertTrue(review.success(), review.message());
        assertTrue(
                review.suggestion().contains("debugging"));

        LessonsLearned saved =
                database.getLessonById(lessonId);

        assertEquals(
                draft.savedTerms(),
                saved.getSearchTerms());
    }

    /**********
     * <p> Method: testCuratorCanEditAiSuggestion() </p>
     *
     * <p> Description: Verifies that a Curator can modify
     * suggested terms before approving their save. </p>
     */
    @Test
    public void testCuratorCanEditAiSuggestion() {

        ModelCuratorSearchTerms model = createModel(
                (title, description, tags) -> "debugging");

        Draft draft = openDraft(model);

        Review review = model.requestSuggestion(
                curator, draft, true);

        assertTrue(review.success(), review.message());

        String editedTerms =
                review.suggestion() + ", troubleshooting";

        LessonResult result = model.saveApprovedTerms(
                curator,
                draft,
                editedTerms,
                true);

        assertTrue(result.isSuccess(), result.getMessage());

        assertEquals(
                editedTerms,
                database.getLessonById(lessonId)
                        .getSearchTerms());
    }
}
