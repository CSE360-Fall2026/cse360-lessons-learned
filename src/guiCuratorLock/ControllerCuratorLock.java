package guiCuratorLock;

import java.util.List;
import java.util.Optional;

import entityClasses.LessonsLearned;
import guiCuratorHome.ModelCuratorHome;
import guiTools.LessonResult;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCuratorLock Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Lock page (TP3 Aspect 2; Epic E6).  This class
 * provides the controller actions based on the user's use of the JavaFX GUI widgets defined by
 * the View class.</p>
 *
 * <p> The Controller reacts to the widgets but does NOT check the role, decide which field
 * names are valid, word the confirmation question, or touch the database itself.  It passes the
 * logged-in user and the ticked boxes to ModelCuratorLock and shows whatever comes back, in
 * place on the page rather than in a pop-up (User Experience.pdf).  The one pop-up is the
 * "Are you sure?" confirmation, the same kind the Contributor's Delete action uses.</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Tested by the manual tests M-CL1 to M-CL6 in Manual Tests.pdf.  The Model methods these
 * actions call, and the order in which they are called, are tested by
 * curatorLockTests.CuratorLockTests and curatorLockTests.CuratorLockPageTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-13 Initial version for TP3 Aspect 2
 */

public class ControllerCuratorLock {

	/*-*******************************************************************************************

	User Interface Actions for this page

	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and
	the Model is often just a stub, or will be a singleton instantiated object.

	 */

	/**
	 * Default constructor is not used.
	 */
	public ControllerCuratorLock() {
	}


	/**********
	 * <p> Method: loadLesson() </p>
	 *
	 * <p> Description: Fills the page with the lesson being locked or unlocked: its title, its
	 * owner and category, and what is locked now, both as a line of text and as "(locked)"
	 * beside each box (US-CL1, US-CL2).  Every box starts unticked.  The View calls this each
	 * time the page is displayed.</p>
	 *
	 * <p> The lesson is read through ModelCuratorLock.lessonToLock(), which checks the Curator
	 * role first.  If the user is not a Curator or the lesson no longer exists, the reason is
	 * shown where the title would have been and the boxes and both buttons are disabled, so
	 * the only thing left to do is return (US-CL-V; scenario S5). </p>
	 */
	protected static void loadLesson() {
		showStatus("", true);
		LessonResult result = ModelCuratorLock.lessonToLock(ViewCuratorLock.theUser,
				ViewCuratorLock.theLessonId);
		if (!result.isSuccess()) {
			// The same approach as the Contributor's Lesson Details page: the reason is the
			// first thing the user reads, and nothing on the page can be acted on
			ViewCuratorLock.label_LessonTitle.setText(result.getMessage());
			ViewCuratorLock.label_LessonInfo.setText("");
			ViewCuratorLock.label_CurrentLocks.setText("");
			setBoxes(null);
			setActionsDisabled(true);
			showStatus(result.getMessage(), false);								// US-CL-V
			return;
		}

		LessonsLearned lesson = result.getLesson();
		ViewCuratorLock.label_LessonTitle.setText(lesson.getTitle());
		ViewCuratorLock.label_LessonInfo.setText("Owner: " + lesson.getOwnerUserName()
				+ "        Category: " + lesson.getCategory());
		// The same words the Curator Home Page uses for what is locked, from the same method
		ViewCuratorLock.label_CurrentLocks.setText("Currently locked: "
				+ ModelCuratorHome.lockSummary(lesson));
		setBoxes(lesson);
		setActionsDisabled(false);
	}


	/**********
	 * <p> Method: performLock() </p>
	 *
	 * <p> Description: Locks the ticked parts of the lesson (US-CL1; scenarios S1 and S2). </p>
	 */
	protected static void performLock() {
		changeLocks(true);
	}


	/**********
	 * <p> Method: performUnlock() </p>
	 *
	 * <p> Description: Unlocks the ticked parts of the lesson (US-CL2; scenario S3). </p>
	 */
	protected static void performUnlock() {
		changeLocks(false);
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Curator Home Page without changing anything. </p>
	 */
	protected static void performReturn() {
		guiCuratorHome.ViewCuratorHome.displayCuratorHome(ViewCuratorLock.theStage,
				ViewCuratorLock.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewCuratorLock.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  A lock is saved the moment it is confirmed, so
	 * there is nothing unsaved to clean up. </p>
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
		ViewCuratorLock.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewCuratorLock.label_Status.setText(message == null ? "" : message);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: changeLocks(boolean lock) </p>
	 *
	 * <p> Description: The one sequence behind both buttons, in the two steps the Model's
	 * convention requires (the same convention as the Contributor's Delete action).
	 * <br> 1. The Model is asked with confirmed = false.  If it answers with a message, the
	 * request is refused (nothing ticked, the lesson is gone, or the user is not a Curator),
	 * the message is shown, and NO confirmation is asked, because asking "Are you sure?" about
	 * something that cannot happen would only confuse.
	 * <br> 2. An EMPTY message means the request is valid, so the confirmation is shown with
	 * explicit Yes and No buttons.  Only "Yes" makes the second call with confirmed = true.
	 * "No", or closing the dialog, saves nothing and shows "No change was made."</p>
	 *
	 * <p> After a successful change the Curator returns to the Curator Home Page, which shows
	 * the Model's message ("Locked: title, tags.") and the lesson's new state in the list. </p>
	 *
	 * @param lock	true to lock the ticked parts, false to unlock them
	 */
	private static void changeLocks(boolean lock) {
		List<String> fields = ModelCuratorLock.chosenFields(
				ViewCuratorLock.checkBox_Entire.isSelected(),
				ViewCuratorLock.checkBox_Title.isSelected(),
				ViewCuratorLock.checkBox_Description.isSelected(),
				ViewCuratorLock.checkBox_Tags.isSelected(),
				ViewCuratorLock.checkBox_Category.isSelected());

		LessonResult check = ModelCuratorLock.changeLocks(ViewCuratorLock.theUser,
				ViewCuratorLock.theLessonId, fields, lock, false);
		if (!check.getMessage().isEmpty()) {
			showStatus(check.getMessage(), false);								// US-CL-V
			return;
		}

		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle(lock ? "Lock Lesson" : "Unlock Lesson");
		confirm.setHeaderText("Are you sure?");
		confirm.setContentText(ModelCuratorLock.confirmationQuestion(
				ViewCuratorLock.label_LessonTitle.getText(), fields, lock));
		confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		Optional<ButtonType> answer = confirm.showAndWait();

		// OB-6: only an explicit "Yes" saves.  Closing the dialog counts as "No".
		if (!answer.isPresent() || answer.get() != ButtonType.YES) {
			showStatus(ModelCuratorLock.NO_CHANGE, true);						// scenario S4
			return;
		}

		LessonResult result = ModelCuratorLock.changeLocks(ViewCuratorLock.theUser,
				ViewCuratorLock.theLessonId, fields, lock, true);
		if (result.isSuccess())
			guiCuratorHome.ViewCuratorHome.displayCuratorHome(ViewCuratorLock.theStage,
					ViewCuratorLock.theUser, result.getMessage());				// OB-10
		else
			// The lesson can disappear, or the write can fail, between the question and the
			// answer; the Curator stays here and reads why (scenario S5)
			showStatus(result.getMessage(), false);
	}


	/**********
	 * <p> Method: setBoxes(LessonsLearned lesson) </p>
	 *
	 * <p> Description: Unticks every box and labels each one with its part of the lesson,
	 * adding "(locked)" where that lock is set now, so the Curator can see what an Unlock
	 * would change. </p>
	 *
	 * @param lesson	the lesson on the page, or null if it could not be loaded
	 */
	private static void setBoxes(LessonsLearned lesson) {
		// The five stored flags are read one by one, not isFieldLocked(), which answers true
		// for every field once the whole lesson is locked; unlocking "Title" would then appear
		// to do something when the title's own flag was never set
		setBox(ViewCuratorLock.checkBox_Entire, "Entire lesson",
				lesson != null && lesson.isLockedEntire());
		setBox(ViewCuratorLock.checkBox_Title, "Title",
				lesson != null && lesson.isLockedTitle());
		setBox(ViewCuratorLock.checkBox_Description, "Description",
				lesson != null && lesson.isLockedDescription());
		setBox(ViewCuratorLock.checkBox_Tags, "Tags",
				lesson != null && lesson.isLockedTags());
		setBox(ViewCuratorLock.checkBox_Category, "Category",
				lesson != null && lesson.isLockedCategory());
	}


	/**********
	 * <p> Method: setBox(CheckBox box, String name, boolean locked) </p>
	 *
	 * <p> Description: Unticks one box and sets its label. </p>
	 *
	 * @param box		the box to set
	 * @param name		the part of the lesson the box stands for
	 * @param locked	true if that lock is set now
	 */
	private static void setBox(javafx.scene.control.CheckBox box, String name, boolean locked) {
		// Unticked every time: a tick left over from the previous lesson could lock a field
		// of this one that the Curator never chose
		box.setSelected(false);
		box.setText(name + (locked ? "   (locked)" : ""));
	}


	/**********
	 * <p> Method: setActionsDisabled(boolean disabled) </p>
	 *
	 * <p> Description: Enables or disables the five boxes and the Lock and Unlock buttons
	 * together. </p>
	 *
	 * @param disabled	true when there is no lesson to act on
	 */
	private static void setActionsDisabled(boolean disabled) {
		ViewCuratorLock.checkBox_Entire.setDisable(disabled);
		ViewCuratorLock.checkBox_Title.setDisable(disabled);
		ViewCuratorLock.checkBox_Description.setDisable(disabled);
		ViewCuratorLock.checkBox_Tags.setDisable(disabled);
		ViewCuratorLock.checkBox_Category.setDisable(disabled);
		ViewCuratorLock.button_Lock.setDisable(disabled);
		ViewCuratorLock.button_Unlock.setDisable(disabled);
	}
}
