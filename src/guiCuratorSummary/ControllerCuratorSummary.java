package guiCuratorSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import applicationMain.FoundationsMain;
import entityClasses.LessonSummary;
import entityClasses.LessonsLearned;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCuratorSummary Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Merge Statistics page (TP3 Aspects 3 and 4; Epics
 * E4 and E5).  This class provides the controller actions based on the user's use of the
 * JavaFX GUI widgets defined by the View class.</p>
 *
 * <p> The Controller reacts to the widgets but does NOT check the role, compute statistics,
 * word messages, or touch the database itself.  It passes the logged-in user, the target
 * lesson, and the selected lessons to ModelCuratorSummary and shows whatever comes back, in
 * place on the page rather than in a pop-up (User Experience.pdf).  The one pop-up is the
 * "Are you sure?" question before a summary is saved, the same kind the Curator Lock page and
 * the Contributor's Delete action use.</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Tested by the manual tests M-CS1 to M-CS8 in Manual Tests.pdf.  The Model methods these
 * actions call are tested by curatorSummaryTests.CuratorSummaryPageTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-16 Initial version for TP3 Aspects 3 and 4
 */

public class ControllerCuratorSummary {

	/** The Model for the current visit.  A new one is made each time the page is displayed, so a
	 * preview never survives leaving the page. */
	private static ModelCuratorSummary theModel;

	/** The target lesson's saved summary as last read, or null if it has none. */
	private static LessonSummary savedSummary;

	/** The target lesson's title as last read. */
	private static String targetTitle = "";

	/**
	 * Default constructor is not used.
	 */
	public ControllerCuratorSummary() {
	}


	/**********
	 * <p> Method: loadPage() </p>
	 *
	 * <p> Description: Fills the page for the target lesson: its title, whether it has a saved
	 * summary and who can see it, every lesson to choose sources from, and the saved summary's
	 * table.  If the lesson already has a summary, its source lessons are selected, so repeating
	 * the merge is one press of Preview.  The View calls this each time the page is displayed.</p>
	 *
	 * <p> If the user is not a Curator or the lesson no longer exists, the reason is shown where
	 * the title would have been and every action is disabled, so the only thing left to do is
	 * return. </p>
	 */
	protected static void loadPage() {
		theModel = new ModelCuratorSummary(FoundationsMain.database);
		showStatus("", true);

		ModelCuratorSummary.Result opened = theModel.openLesson(ViewCuratorSummary.theUser,
				ViewCuratorSummary.theLessonId);
		if (!opened.isSuccess()) {
			ViewCuratorSummary.label_LessonTitle.setText(opened.getMessage());
			ViewCuratorSummary.label_Saved.setText("");
			ViewCuratorSummary.listedLessonIds.clear();
			ViewCuratorSummary.listView_Sources.setItems(FXCollections.observableArrayList());
			ViewCuratorSummary.text_Table.setText("");
			savedSummary = null;
			setAllDisabled(true);
			showStatus(opened.getMessage(), false);
			return;
		}

		targetTitle = opened.getLesson().getTitle();
		ViewCuratorSummary.label_LessonTitle.setText("Target lesson: " + targetTitle);
		savedSummary = opened.getSummary();

		// The ids are replaced BEFORE the rows, so the selection listener can never pair a row
		// with an id from the previous visit
		ViewCuratorSummary.listedLessonIds.clear();
		List<String> rows = new ArrayList<String>();
		for (LessonsLearned lesson : theModel.sourceChoices(ViewCuratorSummary.theUser)) {
			ViewCuratorSummary.listedLessonIds.add(lesson.getId());
			rows.add(theModel.choiceRow(lesson));
		}
		ViewCuratorSummary.listView_Sources.setItems(FXCollections.observableArrayList(rows));
		ViewCuratorSummary.listView_Sources.getSelectionModel().clearSelection();
		if (savedSummary != null) {
			for (int id : savedSummary.getSourceLessonIds()) {
				int row = ViewCuratorSummary.listedLessonIds.indexOf(id);
				if (row >= 0) ViewCuratorSummary.listView_Sources.getSelectionModel().select(row);
			}
		}

		setAllDisabled(false);
		showSaved();
		performSelectionChanged();
	}


	/**********
	 * <p> Method: performSelectionChanged() </p>
	 *
	 * <p> Description: Shows how many lessons are selected, with how to select more than one.  A preview made before the change no
	 * longer matches the selection; Confirm will say so and ask for a new preview
	 * (US-C-E4-02). </p>
	 */
	protected static void performSelectionChanged() {
		int n = selectedLessonIds().size();
		ViewCuratorSummary.label_SelectedCount.setText(n + (n == 1 ? " lesson" : " lessons")
				+ " selected   (Ctrl-click to add or remove one)");
	}


	/**********
	 * <p> Method: performPreview() </p>
	 *
	 * <p> Description: Shows the merged statistics of the selected lessons without saving them
	 * (US-C-E4-01).  A refusal is shown in red and the table goes back to the saved summary. </p>
	 */
	protected static void performPreview() {
		ModelCuratorSummary.Result result = theModel.preview(ViewCuratorSummary.theUser,
				ViewCuratorSummary.theLessonId, selectedLessonIds());
		if (!result.isSuccess()) {
			showSaved();
			showStatus(result.getMessage(), false);
			return;
		}
		showPreview("PREVIEW (not saved)", result.getSummary());
		showStatus("Review the preview, then press Confirm to save it or Cancel to discard it.",
				true);
	}


	/**********
	 * <p> Method: performConfirm() </p>
	 *
	 * <p> Description: Saves the previewed summary after an explicit Yes (US-C-E4-01).  The
	 * question says whether the lesson's current summary is replaced and that the new one starts
	 * hidden.  "No", or closing the dialog, saves nothing and keeps the preview.  If the data
	 * changed since the preview, the refreshed numbers are shown and the Curator must confirm
	 * again (US-C-E4-02). </p>
	 */
	protected static void performConfirm() {
		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Save Merged Summary");
		confirm.setHeaderText("Are you sure?");
		confirm.setContentText(ModelCuratorSummary.confirmationQuestion(targetTitle,
				savedSummary != null));
		confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		Optional<ButtonType> answer = confirm.showAndWait();
		if (!answer.isPresent() || answer.get() != ButtonType.YES) {
			showStatus(ModelCuratorSummary.NO_CHANGE, true);
			return;
		}

		ModelCuratorSummary.Result result = theModel.confirm(ViewCuratorSummary.theUser,
				ViewCuratorSummary.theLessonId, selectedLessonIds());
		if (result.isSuccess()) {
			savedSummary = result.getSummary();
			showSaved();
			showStatus(result.getMessage(), true);
			return;
		}
		if (result.getSummary() != null)
			// The data changed: show the refreshed preview, which Confirm can now save
			showPreview("PREVIEW (refreshed, not saved)", result.getSummary());
		else
			showSaved();
		showStatus(result.getMessage(), false);
	}


	/**********
	 * <p> Method: performCancel() </p>
	 *
	 * <p> Description: Discards the preview; nothing is saved and the saved summary is
	 * unchanged (US-C-E4-01 V sentence). </p>
	 */
	protected static void performCancel() {
		theModel.cancel();
		showSaved();
		showStatus(ModelCuratorSummary.CANCELLED, true);
	}


	/**********
	 * <p> Method: performShow() </p>
	 *
	 * <p> Description: Makes the saved summary visible to Contributors (US-C-E5-01). </p>
	 */
	protected static void performShow() {
		changeVisibility(true);
	}


	/**********
	 * <p> Method: performHide() </p>
	 *
	 * <p> Description: Hides the saved summary from Contributors (US-C-E5-01). </p>
	 */
	protected static void performHide() {
		changeVisibility(false);
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Curator Home Page.  An unconfirmed preview is discarded,
	 * because a new Model is made the next time this page is displayed. </p>
	 */
	protected static void performReturn() {
		guiCuratorHome.ViewCuratorHome.displayCuratorHome(ViewCuratorSummary.theStage,
				ViewCuratorSummary.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewCuratorSummary.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  A summary is saved the moment it is confirmed,
	 * so there is nothing unsaved to clean up. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/**********
	 * <p> Method: showStatus(String message, boolean good) </p>
	 *
	 * <p> Description: Shows a status message on the page: green for good news, red for a
	 * problem.  An empty message clears it. </p>
	 *
	 * @param message	the text to show
	 * @param good		true for a confirmation, false for a problem
	 */
	protected static void showStatus(String message, boolean good) {
		ViewCuratorSummary.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewCuratorSummary.label_Status.setText(message == null ? "" : message);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: changeVisibility(boolean visible) </p>
	 *
	 * <p> Description: The one sequence behind Show and Hide.  The saved line, the table, and
	 * the buttons are refreshed from what the database now holds. </p>
	 *
	 * @param visible	true to show the summary, false to hide it
	 */
	private static void changeVisibility(boolean visible) {
		ModelCuratorSummary.Result result = theModel.setVisibility(ViewCuratorSummary.theUser,
				ViewCuratorSummary.theLessonId, visible);
		if (result.isSuccess()) savedSummary = result.getSummary();
		// A preview being reviewed stays on screen; otherwise the table shows the saved summary
		if (theModel.hasPreview()) refreshSavedLineAndButtons();
		else showSaved();
		showStatus(result.getMessage(), result.isSuccess());
	}


	/**********
	 * <p> Method: showPreview(String heading, LessonSummary preview) </p>
	 *
	 * <p> Description: Shows a preview in the table and enables Confirm and Cancel. </p>
	 *
	 * @param heading	the first line of the table
	 * @param preview	the preview to show
	 */
	private static void showPreview(String heading, LessonSummary preview) {
		ViewCuratorSummary.text_Table.setText(theModel.tableText(heading, preview));
		refreshSavedLineAndButtons();
	}


	/**********
	 * <p> Method: showSaved() </p>
	 *
	 * <p> Description: Shows the saved summary in the table, or the note on how to make one,
	 * and sets the saved line and the buttons. </p>
	 */
	private static void showSaved() {
		if (savedSummary == null)
			ViewCuratorSummary.text_Table.setText(ModelCuratorSummary.NOTHING_PREVIEWED);
		else
			ViewCuratorSummary.text_Table.setText(theModel.tableText("SAVED SUMMARY ("
					+ (savedSummary.isVisibleToContributor() ? "visible to contributors"
							: "hidden from contributors") + ")", savedSummary));
		refreshSavedLineAndButtons();
	}


	/**********
	 * <p> Method: refreshSavedLineAndButtons() </p>
	 *
	 * <p> Description: Sets the saved-summary line, and enables each button only when it can do
	 * something: Confirm and Cancel while a preview waits, Show for a hidden saved summary, and
	 * Hide for a visible one. </p>
	 */
	private static void refreshSavedLineAndButtons() {
		ViewCuratorSummary.label_Saved.setText(ModelCuratorSummary.savedLine(savedSummary));
		boolean preview = theModel.hasPreview();
		ViewCuratorSummary.button_Confirm.setDisable(!preview);
		ViewCuratorSummary.button_Cancel.setDisable(!preview);
		ViewCuratorSummary.button_Show.setDisable(savedSummary == null
				|| savedSummary.isVisibleToContributor());
		ViewCuratorSummary.button_Hide.setDisable(savedSummary == null
				|| !savedSummary.isVisibleToContributor());
	}


	/**********
	 * <p> Method: setAllDisabled(boolean disabled) </p>
	 *
	 * <p> Description: Enables or disables the list and every action except Return, Logout, and
	 * Quit. </p>
	 *
	 * @param disabled	true when there is no lesson to act on
	 */
	private static void setAllDisabled(boolean disabled) {
		ViewCuratorSummary.listView_Sources.setDisable(disabled);
		ViewCuratorSummary.button_Preview.setDisable(disabled);
		ViewCuratorSummary.button_Confirm.setDisable(disabled);
		ViewCuratorSummary.button_Cancel.setDisable(disabled);
		ViewCuratorSummary.button_Show.setDisable(disabled);
		ViewCuratorSummary.button_Hide.setDisable(disabled);
	}


	/**********
	 * <p> Method: List&lt;Integer&gt; selectedLessonIds() </p>
	 *
	 * <p> Description: The ids of the selected lessons, in list order. </p>
	 *
	 * @return the selected ids
	 */
	private static List<Integer> selectedLessonIds() {
		List<Integer> ids = new ArrayList<Integer>();
		for (int row : ViewCuratorSummary.listView_Sources.getSelectionModel().getSelectedIndices())
			if (row >= 0 && row < ViewCuratorSummary.listedLessonIds.size())
				ids.add(ViewCuratorSummary.listedLessonIds.get(row));
		return ids;
	}
}
