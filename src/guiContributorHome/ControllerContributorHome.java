package guiContributorHome;

import java.util.ArrayList;
import java.util.List;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiTools.LessonResult;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerContributorHome Class. </p>
 *
 * <p> Description: The Java/FX-based Contributor Home Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class.</p>
 *
 * <p> The Controller reacts to the widgets but does NOT validate input or touch the database
 * itself.  It passes what the user typed to ModelContributorHome and displays whatever comes
 * back, showing an error directly beneath the field that caused it (InputValidation.pdf).  It
 * also moves the user to the other Contributor pages: guiCreateLesson to add a lesson and
 * guiLessonDetails to examine one.</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Lynn Robert Carter (Contributor home page actions by Emilia Davey)
 *
 * @version 1.00		2025-08-17 Initial version (as the Role1 home page)
 * @version 2.00		2026-10-14 TP2: the Contributor Home Page, split out of the HW2 Role1 page
 */

public class ControllerContributorHome {

	/*-*******************************************************************************************

	User Interface Actions for this page

	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and
	the Model is often just a stub, or will be a singleton instantiated object.

	 */

	/**
	 * Default constructor is not used.
	 */
	public ControllerContributorHome() {
	}


	/**********
	 * <p> Method: performUpdate() </p>
	 *
	 * <p> Description: Directs the user to the Foundations User Update Page so the user can change
	 * the account attributes.  That page returns here because displayContributorHome() set
	 * FoundationsMain.activeHomePage to this page's role number. </p>
	 */
	protected static void performUpdate () {
		guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewContributorHome.theStage,
				ViewContributorHome.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewContributorHome.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  Every lesson is already saved in the database,
	 * so there is nothing to clean up. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/**********
	 * <p> Method: showAllMyLessons() </p>
	 *
	 * <p> Description: Puts the page into its starting state: the search cleared and the full
	 * list of this Contributor's lessons (US-R1).  The View calls this every time the page is
	 * displayed, so the list always reflects the latest changes. </p>
	 */
	protected static void showAllMyLessons() {
		ViewContributorHome.text_Search.clear();
		ViewContributorHome.label_SearchError.setText("");
		refreshLessonList(ModelContributorHome.viewMyLessons(currentUser()), null);	// US-R1
	}


	/**********
	 * <p> Method: performSearch() </p>
	 *
	 * <p> Description: Runs the search box text through the Model (US-R2, US-R2-V).  If the text
	 * is invalid, the message is shown beneath the search box and the list is left exactly as it
	 * was (Test Case "Search Input - Invalid Text Rejected"). </p>
	 */
	protected static void performSearch() {
		ViewContributorHome.label_SearchError.setText("");
		showStatus("", true);
		LessonResult result = ModelContributorHome.searchMyLessons(currentUser(),
				ViewContributorHome.text_Search.getText());
		if (!result.isSuccess()) {
			ViewContributorHome.label_SearchError.setText(result.getMessage());	// US-R2-V
			return;
		}
		refreshLessonList(result.getList(), "matching");						// US-R2
	}


	/**********
	 * <p> Method: performClearSearch() </p>
	 *
	 * <p> Description: Clears the search box and shows every lesson again.  An empty search
	 * shows nothing (design decision D6), so this button is how the full list comes back. </p>
	 */
	protected static void performClearSearch() {
		showStatus("", true);
		showAllMyLessons();
	}


	/**********
	 * <p> Method: performSelectionChanged() </p>
	 *
	 * <p> Description: Enables Open Lesson only while a row is selected, so the button's state
	 * shows whether it can be used (User Experience.pdf, "make validation state visible"). </p>
	 */
	protected static void performSelectionChanged() {
		int row = ViewContributorHome.listView_Lessons.getSelectionModel().getSelectedIndex();
		ViewContributorHome.button_OpenLesson.setDisable(row < 0);
	}


	/**********
	 * <p> Method: performOpenLesson() </p>
	 *
	 * <p> Description: Opens the selected lesson on the Lesson Details page (US-R3, V1: "I can
	 * use that list to specify which lesson learned to examine more closely").  Nothing happens
	 * if no row is selected.  The details page itself checks that the lesson still exists and
	 * belongs to this Contributor (US-R3-V). </p>
	 */
	protected static void performOpenLesson() {
		int row = ViewContributorHome.listView_Lessons.getSelectionModel().getSelectedIndex();
		if (row < 0 || row >= ViewContributorHome.listedLessonIds.size()) return;
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewContributorHome.theStage,
				ViewContributorHome.theUser, ViewContributorHome.listedLessonIds.get(row));
	}


	/**********
	 * <p> Method: performNewLesson() </p>
	 *
	 * <p> Description: Opens the Create Lesson page (US-C1). </p>
	 */
	protected static void performNewLesson() {
		guiCreateLesson.ViewCreateLesson.displayCreateLesson(ViewContributorHome.theStage,
				ViewContributorHome.theUser);
	}


	/**********
	 * <p> Method: showStatus(String message, boolean good) </p>
	 *
	 * <p> Description: Shows a status message beneath the list: green for good news, red for a
	 * problem.  An empty message clears it. </p>
	 *
	 * @param message	the text to show
	 * @param good		true for a confirmation, false for a problem
	 */
	protected static void showStatus(String message, boolean good) {
		ViewContributorHome.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewContributorHome.label_Status.setText(message == null ? "" : message);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: currentUser() </p>
	 *
	 * <p> Description: The username of whoever is logged in.  Every Model call is made with this
	 * name, so a Contributor only ever works with their own lessons (US-R4). </p>
	 *
	 * @return the logged-in user's username
	 */
	private static String currentUser() {
		return ViewContributorHome.theUser.getUserName();
	}


	/**********
	 * <p> Method: refreshLessonList(LessonsLearnedList list, String what) </p>
	 *
	 * <p> Description: Replaces the rows of the list.  Each row shows the title, the category,
	 * and "(locked)" when the lesson or any of its fields is locked, so the Contributor can see
	 * at a glance which lessons cannot be deleted (US-D1-V).  The ids are kept in a parallel list
	 * so a selected row can be matched back to its lesson. </p>
	 *
	 * @param list	the lessons to show
	 * @param what	null for "all my lessons", or a word such as "matching" for the count text
	 */
	private static void refreshLessonList(LessonsLearnedList list, String what) {
		ViewContributorHome.listedLessonIds.clear();
		List<String> rows = new ArrayList<String>();
		for (LessonsLearned lesson : list.getLessons()) {
			ViewContributorHome.listedLessonIds.add(lesson.getId());
			rows.add(lesson.getTitle() + "   [" + lesson.getCategory() + "]"
					+ (lesson.isLocked() ? "   (locked)" : ""));
		}
		ViewContributorHome.listView_Lessons.setItems(FXCollections.observableArrayList(rows));
		ViewContributorHome.button_OpenLesson.setDisable(true);		// nothing is selected yet

		int n = list.size();
		String count = "Showing " + n + ((what == null) ? " " : " " + what + " ")
				+ ((n == 1) ? "lesson" : "lessons");
		if (n == 0 && what == null) count = "You have not added any lessons yet.  "
				+ "Press New Lesson to add your first one.";
		ViewContributorHome.label_ListStatus.setText(count);
	}
}
