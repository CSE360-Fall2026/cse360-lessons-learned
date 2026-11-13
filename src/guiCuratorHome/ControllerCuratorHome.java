package guiCuratorHome;

import java.util.ArrayList;
import java.util.List;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiTools.LessonResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCuratorHome Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Home Page (TP3 Aspect 1).  This class provides the
 * controller actions based on the user's use of the JavaFX GUI widgets defined by the View
 * class.</p>
 *
 * <p> The Controller reacts to the widgets but does NOT check the role, validate input, or
 * touch the database itself.  It passes the logged-in user and what was typed to
 * ModelCuratorHome and displays whatever comes back, showing a search error directly beneath
 * the search box (InputValidation.pdf).</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Tested by the manual tests M-CH1 to M-CH6 in Manual Tests.pdf; performLock() by M-CL1.
 * The Model methods these actions call are tested by curatorHomeTests.CuratorHomeTests
 * (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Lynn Robert Carter (Curator home page actions by Emilia Davey)
 *
 * @version 1.00		2025-08-17 Initial version (as the Role2 home page stub)
 * @version 2.00		2026-11-10 TP3 Aspect 1: the Curator Home Page, replacing the Role2 stub
 * @version 2.01		2026-11-13 TP3 Aspect 2: performLock() opens the Curator Lock page
 */

public class ControllerCuratorHome {

	/*-*******************************************************************************************

	User Interface Actions for this page

	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and
	the Model is often just a stub, or will be a singleton instantiated object.

	 */

	/** What the details box says while no lesson is selected. */
	private static final String NO_SELECTION = "Select a lesson to read its details.";

	/**
	 * Default constructor is not used.
	 */
	public ControllerCuratorHome() {
	}


	/**********
	 * <p> Method: performUpdate() </p>
	 *
	 * <p> Description: Directs the user to the Foundations User Update Page so the user can change
	 * the account attributes.  That page returns here because displayCuratorHome() set
	 * FoundationsMain.activeHomePage to this page's role number. </p>
	 */
	protected static void performUpdate () {
		guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewCuratorHome.theStage,
				ViewCuratorHome.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewCuratorHome.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  This page changes nothing, so there is nothing
	 * to clean up. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/**********
	 * <p> Method: showAllLessons() </p>
	 *
	 * <p> Description: Puts the page into its starting state: the search cleared and the full
	 * list of every Contributor's lessons (US-CH1).  The View calls this every time the page is
	 * displayed, so the list always reflects the latest changes.  If the Model refuses the user
	 * (US-CH-V), the list is left empty and the refusal is shown in red. </p>
	 */
	protected static void showAllLessons() {
		ViewCuratorHome.text_Search.clear();
		ViewCuratorHome.label_SearchError.setText("");
		LessonResult result = ModelCuratorHome.viewAllLessons(ViewCuratorHome.theUser);
		if (!result.isSuccess()) {
			// The dispatch code only sends Curators here, so this is not expected to happen.
			// It is still handled, because the Model's refusal is the real protection and the
			// page must show nothing rather than fail if it is ever reached another way.
			refreshLessonList(new LessonsLearnedList(), null);
			ViewCuratorHome.label_ListStatus.setText("");
			showStatus(result.getMessage(), false);								// US-CH-V
			return;
		}
		refreshLessonList(result.getList(), null);								// US-CH1
	}


	/**********
	 * <p> Method: performSearch() </p>
	 *
	 * <p> Description: Runs the search box text through the Model (US-CH2, US-CH2-V).  If the
	 * text is invalid, the message is shown beneath the search box and the list is left exactly
	 * as it was.  A refusal of the user is shown as a red status instead, because it is not
	 * about the search text. </p>
	 */
	protected static void performSearch() {
		ViewCuratorHome.label_SearchError.setText("");
		showStatus("", true);
		LessonResult result = ModelCuratorHome.searchAllLessons(ViewCuratorHome.theUser,
				ViewCuratorHome.text_Search.getText());
		if (!result.isSuccess()) {
			// failedField tells the two kinds of failure apart, so each message appears where
			// the user will look for it
			if ("searchText".equals(result.getFailedField()))
				ViewCuratorHome.label_SearchError.setText(result.getMessage());	// US-CH2-V
			else
				showStatus(result.getMessage(), false);							// US-CH-V
			return;
		}
		refreshLessonList(result.getList(), "matching");						// US-CH2
	}


	/**********
	 * <p> Method: performClearSearch() </p>
	 *
	 * <p> Description: Clears the search box and shows every lesson again.  An empty search
	 * shows nothing (design decision D6), so this button is how the full list comes back. </p>
	 */
	protected static void performClearSearch() {
		showStatus("", true);
		showAllLessons();
	}


	/**********
	 * <p> Method: performSelectionChanged() </p>
	 *
	 * <p> Description: Shows the selected lesson in full in the details box (US-CH3), or the
	 * "select a lesson" note when no row is selected.  The lesson is read again through the
	 * Model, so the details are current and the role is checked for this action too.  If the
	 * lesson has been deleted since the list was drawn, "Lesson not found." is shown and the
	 * list is reloaded so the stale row disappears. </p>
	 */
	protected static void performSelectionChanged() {
		int row = ViewCuratorHome.listView_Lessons.getSelectionModel().getSelectedIndex();
		// The Lock / Unlock button works on the selected lesson, so it is usable only while a
		// row is selected
		ViewCuratorHome.button_Lock.setDisable(
				row < 0 || row >= ViewCuratorHome.listedLessonIds.size());
		if (row < 0 || row >= ViewCuratorHome.listedLessonIds.size()) {
			ViewCuratorHome.text_Details.setText(NO_SELECTION);
			return;
		}
		LessonResult result = ModelCuratorHome.viewLesson(ViewCuratorHome.theUser,
				ViewCuratorHome.listedLessonIds.get(row));
		if (!result.isSuccess()) {
			String message = result.getMessage();
			// This method runs inside the list's own selection listener, and JavaFX does not
			// allow a list's rows to be replaced while it is still reporting a selection
			// change.  runLater() waits until that change has finished.  Reloading clears the
			// selection, which calls this method again with no row selected; the message is
			// shown after the reload so that second call cannot erase it.
			Platform.runLater(() -> {
				showAllLessons();
				showStatus(message, false);
			});
			return;
		}
		showStatus("", true);
		ViewCuratorHome.text_Details.setText(ModelCuratorHome.detailsText(result.getLesson()));
	}


	/**********
	 * <p> Method: performLock() </p>
	 *
	 * <p> Description: Opens the Curator Lock page for the selected lesson (US-CL1: "I can
	 * choose a lesson from my Curator Home page and lock ...").  Nothing happens if no row is
	 * selected.  The lock page itself checks the Curator role and that the lesson still
	 * exists. </p>
	 */
	protected static void performLock() {
		int row = ViewCuratorHome.listView_Lessons.getSelectionModel().getSelectedIndex();
		// The button is disabled without a selection; this check keeps the action safe even if
		// it is ever reached another way
		if (row < 0 || row >= ViewCuratorHome.listedLessonIds.size()) return;
		guiCuratorLock.ViewCuratorLock.displayCuratorLock(ViewCuratorHome.theStage,
				ViewCuratorHome.theUser, ViewCuratorHome.listedLessonIds.get(row));
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
		ViewCuratorHome.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewCuratorHome.label_Status.setText(message == null ? "" : message);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: refreshLessonList(LessonsLearnedList list, String what) </p>
	 *
	 * <p> Description: Replaces the rows of the list.  Each row is ModelCuratorHome.rowText():
	 * the title, the category, the owner, and "(locked)" when anything is locked.  The ids are
	 * kept in a parallel list so a selected row can be matched back to its lesson. </p>
	 *
	 * @param list	the lessons to show
	 * @param what	null for "all lessons", or a word such as "matching" for the count text
	 */
	private static void refreshLessonList(LessonsLearnedList list, String what) {
		// The ids are replaced BEFORE the rows.  Setting the rows clears the selection, which
		// fires performSelectionChanged(); doing it in this order means that call can never
		// pair a row with an id from the previous list.
		ViewCuratorHome.listedLessonIds.clear();
		List<String> rows = new ArrayList<String>();
		for (LessonsLearned lesson : list.getLessons()) {
			ViewCuratorHome.listedLessonIds.add(lesson.getId());
			rows.add(ModelCuratorHome.rowText(lesson));
		}
		ViewCuratorHome.listView_Lessons.setItems(FXCollections.observableArrayList(rows));
		ViewCuratorHome.text_Details.setText(NO_SELECTION);		// nothing is selected yet
		ViewCuratorHome.button_Lock.setDisable(true);			// so there is nothing to lock

		int n = list.size();
		String count = "Showing " + n + ((what == null) ? " " : " " + what + " ")
				+ ((n == 1) ? "lesson" : "lessons");
		if (n == 0 && what == null) count = "No contributor has added a lesson yet.";
		ViewCuratorHome.label_ListStatus.setText(count);
	}
}
