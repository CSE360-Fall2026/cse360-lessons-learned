
package viewerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiTools.LessonResult;
import guiViewerHome.ModelViewerHome;

/*******
 * <p> Title: ViewerDatabaseIntegrationTests Class </p>
 *
 * <p> Description: Integration tests for TP3 Aspect 9
 * (Viewer) and Aspect 5 (Curator Search Terms). Verifies
 * lesson retrieval, keyword searches, Curator permissions,
 * and search-term persistence using an isolated H2 database. </p>
 *
 * <p> Each test uses a separate in-memory database so
 * application data remains unchanged. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-16 Initial integration tests
 */
public class ViewerDatabaseIntegrationTests {

    private Database database;
    private User viewer;
    private User contributor;
    private User curator;

    /**********
     * <p> Method: setUp() </p>
     *
     * <p> Description: Creates an isolated H2 database
     * and registers the users needed for testing. </p>
     *
     * @throws SQLException if database setup fails
     */
    @BeforeEach
    public void setUp() throws SQLException {

        String databaseName =
                "tp3_test_" + UUID.randomUUID().toString()
                        .replace("-", "");

        database = new Database(
                "jdbc:h2:mem:" + databaseName);

        database.connectToDatabase();

        viewer = new User(
                "testViewer", "Password123!",
                "Test", "", "Viewer", "", "",
                false, false, false, true);

        contributor = new User(
                "testContributor", "Password123!",
                "Test", "", "Contributor", "", "",
                false, true, false, false);

        curator = new User(
                "testCurator", "Password123!",
                "Test", "", "Curator", "", "",
                false, false, true, false);

        database.register(viewer);
        database.register(contributor);
        database.register(curator);
    }

    /**********
     * <p> Method: tearDown() </p>
     *
     * <p> Description: Closes the database connection
     * after each test. </p>
     */
    @AfterEach
    public void tearDown() {

        if (database != null) {
            database.closeConnection();
        }
    }

    /**********
     * <p> Method: createLesson(String, String, String) </p>
     *
     * <p> Description: Creates and saves a lesson
     * owned by the specified Contributor. </p>
     *
     * @param owner username of the lesson owner
     * @param title lesson title
     * @param description lesson description
     * @return the saved lesson ID
     * @throws SQLException if the insert fails
     */
    private int createLesson(
            String owner, String title, String description)
            throws SQLException {

        LessonsLearned lesson = new LessonsLearned(
                owner,
                title,
                description,
                "testing",
                "Other");

        int id = database.createLesson(lesson);

        assertTrue(id > 0, "Lesson should be saved.");

        return id;
    }

    /**********
     * <p> Method: testViewerCanRetrieveAllLessons() </p>
     *
     * <p> Description: Verifies that a Viewer can
     * retrieve lessons from multiple Contributors. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testViewerCanRetrieveAllLessons()
            throws SQLException {

        createLesson(
                "contributorOne",
                "Java Debugging",
                "Finding errors in Java.");

        createLesson(
                "contributorTwo",
                "Database Testing",
                "Testing SQL queries.");

        LessonResult result =
                ModelViewerHome.viewAllLessons(
                        viewer, database);

        assertTrue(result.isSuccess());
        assertNotNull(result.getList());
        assertEquals(2, result.getList().size());
    }

    /**********
     * <p> Method: testViewerCanOpenSavedLesson() </p>
     *
     * <p> Description: Verifies that a Viewer can
     * open a lesson saved in the database. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testViewerCanOpenSavedLesson()
            throws SQLException {

        int lessonId = createLesson(
                "contributorOne",
                "Learning Java",
                "Working with Java classes.");

        LessonResult result =
                ModelViewerHome.viewLesson(
                        viewer, lessonId, database);

        assertTrue(result.isSuccess());
        assertNotNull(result.getLesson());
        assertEquals(
                "Learning Java",
                result.getLesson().getTitle());
    }

    /**********
     * <p> Method: testViewerCanSearchSavedLessons() </p>
     *
     * <p> Description: Verifies that a Viewer can
     * search lessons stored in the database. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testViewerCanSearchSavedLessons()
            throws SQLException {

        createLesson(
                "contributorOne",
                "Java Debugging",
                "Troubleshooting software errors.");

        createLesson(
                "contributorTwo",
                "Database Design",
                "Creating relational tables.");

        LessonResult result =
                ModelViewerHome.searchAllLessons(
                        viewer, "troubleshooting", database);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getList().size());
        assertEquals(
                "Java Debugging",
                result.getList().get(0).getTitle());
    }

    /**********
     * <p> Method: testCuratorCanSaveSearchTerms() </p>
     *
     * <p> Description: Verifies that an authorized
     * Curator can save approved search terms. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testCuratorCanSaveSearchTerms()
            throws SQLException {

        int lessonId = createLesson(
                "contributorOne",
                "Software Errors",
                "Finding problems in applications.");

        LessonsLearned original =
                database.getLessonById(lessonId);

        assertNotNull(original);
        assertNotNull(original.getSearchTerms());

        boolean saved = database.saveCuratorSearchTerms(
                lessonId,
                curator.getUserName(),
                original.getSearchTerms(),
                "debugging, troubleshooting");

        assertTrue(saved);

        LessonsLearned updated =
                database.getLessonById(lessonId);

        assertEquals(
                "debugging, troubleshooting",
                updated.getSearchTerms());
    }

    /**********
     * <p> Method: testViewerCanSearchCuratorTerms() </p>
     *
     * <p> Description: Verifies that approved search
     * terms make a lesson searchable by Viewers. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testViewerCanSearchCuratorTerms()
            throws SQLException {

        int lessonId = createLesson(
                "contributorOne",
                "Software Errors",
                "Finding problems in applications.");

        LessonsLearned original =
                database.getLessonById(lessonId);

        assertNotNull(original);
        assertNotNull(original.getSearchTerms());

        assertTrue(database.saveCuratorSearchTerms(
                lessonId,
                curator.getUserName(),
                original.getSearchTerms(),
                "troubleshooting"));

        LessonResult result =
                ModelViewerHome.searchAllLessons(
                        viewer,
                        "troubleshooting",
                        database);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getList().size());
        assertEquals(
                lessonId,
                result.getList().get(0).getId());
    }

    /**********
     * <p> Method: testNonCuratorCannotSaveSearchTerms() </p>
     *
     * <p> Description: Verifies that users without
     * the Curator role cannot save approved terms. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testNonCuratorCannotSaveSearchTerms()
            throws SQLException {

        int lessonId = createLesson(
                "contributorOne",
                "Software Testing",
                "Testing application functionality.");

        LessonsLearned original =
                database.getLessonById(lessonId);

        assertNotNull(original);
        assertNotNull(original.getSearchTerms());

        boolean saved = database.saveCuratorSearchTerms(
                lessonId,
                contributor.getUserName(),
                original.getSearchTerms(),
                "unauthorized");

        assertFalse(saved);

        LessonsLearned unchanged =
                database.getLessonById(lessonId);

        assertEquals(
                original.getSearchTerms(),
                unchanged.getSearchTerms());
    }

    /**********
     * <p> Method: testOutdatedSearchTermsCannotOverwrite() </p>
     *
     * <p> Description: Verifies that outdated Curator
     * search terms cannot overwrite a newer edit. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testOutdatedSearchTermsCannotOverwrite()
            throws SQLException {

        int lessonId = createLesson(
                "contributorOne",
                "Java Errors",
                "Fixing software problems.");

        LessonsLearned original =
                database.getLessonById(lessonId);

        assertNotNull(original);
        assertNotNull(original.getSearchTerms());

        String originalTerms =
                original.getSearchTerms();

        assertTrue(database.saveCuratorSearchTerms(
                lessonId,
                curator.getUserName(),
                originalTerms,
                "debugging"));

        boolean secondSave =
                database.saveCuratorSearchTerms(
                        lessonId,
                        curator.getUserName(),
                        originalTerms,
                        "outdated");

        assertFalse(secondSave);

        LessonsLearned current =
                database.getLessonById(lessonId);

        assertEquals(
                "debugging",
                current.getSearchTerms());
    }

    /**********
     * <p> Method: testNonViewerCannotRetrieveSavedLessons() </p>
     *
     * <p> Description: Verifies that a Contributor
     * cannot access all lessons through the Viewer Model. </p>
     *
     * @throws SQLException if lesson creation fails
     */
    @Test
    public void testNonViewerCannotRetrieveSavedLessons()
            throws SQLException {

        createLesson(
                "contributorOne",
                "Private Test Lesson",
                "Testing Viewer authorization.");

        LessonResult result =
                ModelViewerHome.viewAllLessons(
                        contributor, database);

        assertFalse(result.isSuccess());
    }
}
