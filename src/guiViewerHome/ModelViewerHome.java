
package guiViewerHome;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.User;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import guiTools.ViewerAccess;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelViewerHome Class </p>
 *
 * <p> Description: Provides the read-only lesson operations for
 * the Viewer Home Page in TP3 Aspect 9. Authorized Viewers can
 * list all lessons, search by keyword, and retrieve an individual
 * lesson without modifying its stored contents. </p>
 *
 * <p> This class supports US-V-01, US-V-02, US-V-03, and
 * US-C-E2-03. Viewer authorization is checked before lesson
 * data is retrieved or search input is validated. </p>
 *
 * <p> The Model reuses LessonsLearnedList and LessonResult
 * rather than duplicating the existing lesson storage and
 * search functionality. Database overloads allow isolated
 * integration testing without modifying the shared database. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 9
 * @version 1.01 2026-11-16 Added database injection for testing
 */
public class ModelViewerHome {

    /** Message returned when a user lacks the Viewer role. */
    public static final String NOT_VIEWER =
            "Only a viewer can view all lessons.";

    /*****
     * <p> Method: ModelViewerHome() </p>
     *
     * <p> Description: Default constructor. The Model provides
     * static methods and does not require an instance. </p>
     */
    public ModelViewerHome() {
    }

    /*****
     * <p> Method: LessonResult viewAllLessons(User actingUser) </p>
     *
     * <p> Description: Retrieves all lessons using the shared
     * application database. Preserves the original Viewer
     * interface behavior. </p>
     *
     * @param actingUser the currently logged-in user
     * @return the Viewer lesson list result
     */
    public static LessonResult viewAllLessons(User actingUser) {
        return viewAllLessons(actingUser, FoundationsMain.database);
    }

    /*****
     * <p> Method: LessonResult viewAllLessons(
     * User actingUser, Database database) </p>
     *
     * <p> Description: Retrieves lessons from every Contributor
     * for an authorized Viewer (US-V-01). The database can be
     * supplied by integration tests. </p>
     *
     * @param actingUser the currently logged-in user
     * @param database the database used to retrieve lessons
     * @return a successful lesson list or an authorization error
     */
    public static LessonResult viewAllLessons(
            User actingUser, Database database) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        if (database == null) {
            return LessonResult.failed(
                    "Database is unavailable.", "");
        }

        LessonsLearnedList lessons = database.getAllLessons();

        return LessonResult.passedList("All lessons", lessons);
    }

    /*****
     * <p> Method: LessonResult searchAllLessons(
     * User actingUser, String searchText) </p>
     *
     * <p> Description: Searches the shared application database
     * for lessons matching the provided keywords (US-V-02,
     * US-C-E2-03). </p>
     *
     * @param actingUser the currently logged-in user
     * @param searchText the keywords used to search lessons
     * @return the keyword search result
     */
    public static LessonResult searchAllLessons(
            User actingUser, String searchText) {

        return searchAllLessons(
                actingUser, searchText, FoundationsMain.database);
    }

    /*****
     * <p> Method: LessonResult searchAllLessons(
     * User actingUser, String searchText, Database database) </p>
     *
     * <p> Description: Searches all lessons using their titles,
     * descriptions, tags, and Curator-approved search terms.
     * The search is case-insensitive and requires all keywords
     * to match (US-V-02, US-C-E2-03). </p>
     *
     * <p> Authorization and search text validation are checked
     * before retrieving lessons. </p>
     *
     * @param actingUser the currently logged-in user
     * @param searchText the keywords used to search lessons
     * @param database the database used to retrieve lessons
     * @return matching lessons or an error result
     */
    public static LessonResult searchAllLessons(
            User actingUser, String searchText, Database database) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        String error =
                LessonRecognizer.checkForValidSearchText(searchText);

        if (!error.isEmpty()) {
            return LessonResult.failed(error, "searchText");
        }

        if (database == null) {
            return LessonResult.failed(
                    "Database is unavailable.", "");
        }

        LessonsLearnedList matches = database.getAllLessons()
                .filterByKeywords(searchText);

        return LessonResult.passedList("Search complete", matches);
    }

    /*****
     * <p> Method: LessonResult viewLesson(
     * User actingUser, int lessonId) </p>
     *
     * <p> Description: Retrieves a specific lesson using the
     * shared application database (US-V-03). </p>
     *
     * @param actingUser the currently logged-in user
     * @param lessonId the identifier of the lesson to retrieve
     * @return the lesson or an error result
     */
    public static LessonResult viewLesson(
            User actingUser, int lessonId) {

        return viewLesson(
                actingUser, lessonId, FoundationsMain.database);
    }

    /*****
     * <p> Method: LessonResult viewLesson(
     * User actingUser, int lessonId, Database database) </p>
     *
     * <p> Description: Retrieves a specific lesson for an
     * authorized Viewer (US-V-03). Lessons can be opened
     * regardless of which Contributor created them. </p>
     *
     * <p> This operation does not update or delete lessons.
     * Curator-only statistics must remain restricted in the
     * Viewer presentation workflow (US-V-04). </p>
     *
     * @param actingUser the currently logged-in user
     * @param lessonId the identifier of the lesson to retrieve
     * @param database the database used to retrieve the lesson
     * @return the lesson or an error result
     */
    public static LessonResult viewLesson(
            User actingUser, int lessonId, Database database) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        if (database == null) {
            return LessonResult.failed(
                    "Database is unavailable.", "");
        }

        LessonsLearned lesson = database.getLessonById(lessonId);

        if (lesson == null) {
            return LessonResult.failed(
                    LessonAccess.NOT_FOUND, "");
        }

        return LessonResult.passed("", lessonId, lesson);
    }
}