
package guiViewerHome;

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
 * search functionality. </p>
 *
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 9
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
     * <p> Description: Retrieves all lessons learned from every
     * Contributor for an authorized Viewer (US-V-01).
     * An empty collection is returned successfully rather than
     * producing an error. </p>
     *
     * <p> Viewer authorization is checked before accessing
     * lesson data. This prevents users without the Viewer
     * role from retrieving the complete collection. </p>
     *
     * @param actingUser the currently logged-in user; may be null
     *
     * @return a successful result containing all lessons,
     *         or a failed result if the user is not a Viewer
     */
    public static LessonResult viewAllLessons(User actingUser) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        LessonsLearnedList lessons = LessonsLearnedList.loadAll();

        return LessonResult.passedList("All lessons", lessons);
    }

    /*****
     * <p> Method: LessonResult searchAllLessons(User actingUser,
     * String searchText) </p>
     *
     * <p> Description: Searches all lessons using the provided
     * keywords (US-V-02, US-C-E2-03). The search includes
     * titles, descriptions, tags, and saved Curator-approved
     * search terms. </p>
     *
     * <p> Search input is validated before execution. Invalid
     * input produces a specific error, while an empty search
     * or a search with no matches returns an empty list. </p>
     *
     * <p> The search is case-insensitive and requires every
     * keyword to match at least one searchable field. </p>
     *
     * @param actingUser the currently logged-in user; may be null
     *
     * @param searchText the keywords used to search lessons
     *
     * @return a successful result containing matching lessons,
     *         or a failed result with an authorization or
     *         validation error
     */
    public static LessonResult searchAllLessons(
            User actingUser, String searchText) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        String error =
                LessonRecognizer.checkForValidSearchText(searchText);

        if (!error.isEmpty()) {
            return LessonResult.failed(error, "searchText");
        }

        LessonsLearnedList matches = LessonsLearnedList.loadAll()
                .filterByKeywords(searchText);

        return LessonResult.passedList("Search complete", matches);
    }

    /*****
     * <p> Method: LessonResult viewLesson(User actingUser,
     * int lessonId) </p>
     *
     * <p> Description: Retrieves a specific lesson for an
     * authorized Viewer (US-V-03). Lessons can be opened
     * regardless of which Contributor created them. </p>
     *
     * <p> The Viewer role is checked before reading the
     * lesson. If the lesson no longer exists, the method
     * returns "Lesson not found." rather than an error. </p>
     *
     * <p> This operation does not update or delete the lesson.
     * Presentation of Curator-only statistics must be
     * restricted by the Viewer read workflow (US-V-04). </p>
     *
     * @param actingUser the currently logged-in user; may be null
     *
     * @param lessonId the identifier of the lesson to retrieve
     *
     * @return a successful result containing the lesson,
     *         or a failed result if access is denied or
     *         the lesson does not exist
     */
    public static LessonResult viewLesson(
            User actingUser, int lessonId) {

        if (!ViewerAccess.isViewer(actingUser)) {
            return LessonResult.failed(NOT_VIEWER, "");
        }

        LessonsLearned lesson = LessonsLearned.read(lessonId);

        if (lesson == null) {
            return LessonResult.failed(LessonAccess.NOT_FOUND, "");
        }

        return LessonResult.passed("", lessonId, lesson);
    }
}
