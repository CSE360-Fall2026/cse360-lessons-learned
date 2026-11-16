
package guiViewerHome;

import java.util.ArrayList;
import java.util.List;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiTools.LessonResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;


/*******
 * <p> Title: ControllerViewerHome Class </p>
 *
 * <p> Description: Handles user interactions on the Viewer Home
 * Page for TP3 Aspect 9. This includes displaying lessons,
 * searching by keyword, selecting lessons, and navigation. </p>
 *
 * <p> The Controller delegates authorization, input validation,
 * and lesson retrieval to ModelViewerHome. The Viewer interface
 * provides no operations for modifying lessons. </p>
 *
 * <p> Supports US-V-01, US-V-02, US-V-03, and US-V-04. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 9
 * @version 1.01 2026-11-16 Emilia Davey, TP3 Aspect 4: the details show a
 *          visible merged effort summary (US-V-04)
 */
public class ControllerViewerHome {

    /** Message displayed when no lesson is selected. */
    private static final String NO_SELECTION =
            "Select a lesson to view its details.";

    /**********
     * <p> Method: ControllerViewerHome() </p>
     *
     * <p> Description: Default constructor. All Controller
     * operations are static and do not require an instance. </p>
     */
    public ControllerViewerHome() {
    }

    /**********
     * <p> Method: performLogout() </p>
     *
     * <p> Description: Logs out the current user and returns
     * to the application's login page. </p>
     */
    protected static void performLogout() {

        guiUserLogin.ViewUserLogin.displayUserLogin(
                ViewViewerHome.theStage);
    }

    /**********
     * <p> Method: performQuit() </p>
     *
     * <p> Description: Exits the application. The Viewer
     * interface does not modify stored lesson data. </p>
     */
    protected static void performQuit() {

        System.exit(0);
    }

    /**********
     * <p> Method: showAllLessons() </p>
     *
     * <p> Description: Clears the search box and retrieves
     * all lessons available to an authorized Viewer (US-V-01).
     * Displays an empty-state message if no lessons exist. </p>
     */
    protected static void showAllLessons() {

        ViewViewerHome.text_Search.clear();
        ViewViewerHome.label_SearchError.setText("");

        LessonResult result =
                ModelViewerHome.viewAllLessons(ViewViewerHome.theUser);

        if (!result.isSuccess()) {

            refreshLessonList(new LessonsLearnedList(), null);
            ViewViewerHome.label_ListStatus.setText("");
            showStatus(result.getMessage(), false);
            return;
        }

        refreshLessonList(result.getList(), null);
    }

    /**********
     * <p> Method: performSearch() </p>
     *
     * <p> Description: Searches all lessons using the keywords
     * entered by the Viewer (US-V-02). Validation errors appear
     * beneath the search field. An invalid search does not
     * replace the existing list. </p>
     */
    protected static void performSearch() {

        ViewViewerHome.label_SearchError.setText("");
        showStatus("", true);

        LessonResult result =
                ModelViewerHome.searchAllLessons(
                        ViewViewerHome.theUser,
                        ViewViewerHome.text_Search.getText());

        if (!result.isSuccess()) {

            if ("searchText".equals(result.getFailedField())) {

                ViewViewerHome.label_SearchError.setText(
                        result.getMessage());

            } else {

                showStatus(result.getMessage(), false);
            }

            return;
        }

        refreshLessonList(result.getList(), "matching");
    }

    /**********
     * <p> Method: performClearSearch() </p>
     *
     * <p> Description: Clears the current search and restores
     * the complete lesson list (US-V-02). </p>
     */
    protected static void performClearSearch() {

        showStatus("", true);
        showAllLessons();
    }

    /**********
     * <p> Method: performSelectionChanged() </p>
     *
     * <p> Description: Retrieves the selected lesson and
     * displays its read-only details (US-V-03). If the lesson
     * no longer exists, the list is refreshed and an error
     * is displayed. </p>
     */
    protected static void performSelectionChanged() {

        int row = ViewViewerHome.listView_Lessons
                .getSelectionModel().getSelectedIndex();

        if (row < 0 ||
                row >= ViewViewerHome.listedLessonIds.size()) {

            ViewViewerHome.text_Details.setText(NO_SELECTION);
            return;
        }

        LessonResult result =
                ModelViewerHome.viewLesson(
                        ViewViewerHome.theUser,
                        ViewViewerHome.listedLessonIds.get(row));

        if (!result.isSuccess()) {

            String message = result.getMessage();

            // Wait until JavaFX finishes the selection event
            // before replacing the list.
            Platform.runLater(() -> {

                showAllLessons();
                showStatus(message, false);
            });

            return;
        }

        showStatus("", true);

        ViewViewerHome.text_Details.setText(
                formatLessonDetails(result.getLesson()));
    }

    /**********
     * <p> Method: showStatus(String message, boolean good) </p>
     *
     * <p> Description: Displays a status message in green for
     * success or red for an error. </p>
     *
     * @param message the status message to display
     *
     * @param good true for success, false for an error
     */
    protected static void showStatus(
            String message, boolean good) {

        ViewViewerHome.label_Status.setTextFill(
                good ? Color.web("#2e7d32") : Color.RED);

        ViewViewerHome.label_Status.setText(
                message == null ? "" : message);
    }

    /**********
     * <p> Method: refreshLessonList(LessonsLearnedList list,
     * String what) </p>
     *
     * <p> Description: Updates the lesson list, tracks lesson
     * identifiers, and displays the appropriate result count
     * or empty-state message (US-V-01, US-V-02). </p>
     *
     * @param list the lessons to display
     *
     * @param what null for all lessons or "matching" for
     *             search results
     */
    private static void refreshLessonList(
            LessonsLearnedList list, String what) {

        ViewViewerHome.listedLessonIds.clear();

        List<String> rows = new ArrayList<String>();

        for (LessonsLearned lesson : list.getLessons()) {

            ViewViewerHome.listedLessonIds.add(lesson.getId());

            rows.add(formatLessonRow(lesson));
        }

        ViewViewerHome.listView_Lessons.setItems(
                FXCollections.observableArrayList(rows));

        ViewViewerHome.text_Details.setText(NO_SELECTION);

        int n = list.size();

        String count = "Showing " + n
                + ((what == null) ? " " : " " + what + " ")
                + ((n == 1) ? "lesson" : "lessons");

        if (n == 0 && what == null) {
            count = "No contributor has added a lesson yet.";
        } else if (n == 0) {
            count = "No lessons match your search.";
        }

        ViewViewerHome.label_ListStatus.setText(count);
    }

    /**********
     * <p> Method: formatLessonRow(LessonsLearned lesson) </p>
     *
     * <p> Description: Creates the text shown for each lesson
     * in the Viewer list. Only basic lesson information is
     * displayed, without Curator-specific status details. </p>
     *
     * @param lesson the lesson to format
     *
     * @return the text displayed in the lesson list
     */
    private static String formatLessonRow(
            LessonsLearned lesson) {

        return lesson.getTitle();
    }

    /**********
     * <p> Method: formatLessonDetails(LessonsLearned lesson) </p>
     *
     * <p> Description: Creates a read-only presentation of
     * the selected lesson for the Viewer. Only basic lesson
     * fields are included, plus a Curator's merged effort
     * summary only when the Curator made it visible; lock
     * controls are excluded (US-V-03, US-V-04). </p>
     *
     * @param lesson the selected lesson
     *
     * @return formatted text containing permitted lesson details
     */
    private static String formatLessonDetails(
            LessonsLearned lesson) {

        StringBuilder details = new StringBuilder();

        details.append("Title: ")
                .append(lesson.getTitle())
                .append("\n\n");

        details.append("Description:\n")
                .append(lesson.getDescription());

        if (lesson.getCuratedBy() != null
                && !lesson.getCuratedBy().isBlank()
                && lesson.getCuratedAt() > 0) {

            String editedDate = java.time.Instant
                    .ofEpochMilli(lesson.getCuratedAt())
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter
                            .ofPattern("MMM d, yyyy 'at' h:mm a"));

            details.append("\n\nEdited by curator: ")
                    .append(lesson.getCuratedBy())
                    .append("\nLast curated: ")
                    .append(editedDate);
        }

        // TP3 Aspect 4: only a summary the Curator made visible
        String summary = ModelViewerHome.visibleSummaryText(
                lesson.getId(), applicationMain.FoundationsMain.database);
        if (!summary.isEmpty()) {
            details.append("\n\nMerged effort summary:\n")
                    .append(summary);
        }

        return details.toString();
    }
}
