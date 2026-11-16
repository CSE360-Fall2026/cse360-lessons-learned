package guiLessonDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import entityClasses.MemberEffort;
import guiTools.LessonResult;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerLessonDetails Class. </p>
 *
 * <p> Description: The Java/FX-based Lesson Details Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class: filling
 * the page with one lesson (US-R3), moving to the edit and add-experience pages, and the
 * two-step delete with its "Are you sure?" confirmation (US-D1, US-D1-V).</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2
 */

public class ControllerLessonDetails {

	/** How dates are shown on this page, for example 2026-10-14. */
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/**
	 * Default constructor is not used.
	 */
	public ControllerLessonDetails() {
	}


	/**********
	 * <p> Method: loadLesson() </p>
	 *
	 * <p> Description: Asks the Model for the lesson in ViewLessonDetails.theLessonId and fills
	 * the page with it (US-R3).  If the Model reports "Lesson not found." (the lesson is missing
	 * or belongs to someone else), the page is cleared, the message is shown, and every action
	 * is disabled, so no other Contributor's data can be shown or changed (US-R3-V, US-R4). </p>
	 */
	protected static void loadLesson() {
		showStatus("", true);
		LessonResult result = ModelLessonDetails.viewLesson(currentUser(),
				ViewLessonDetails.theLessonId);
		if (!result.isSuccess()) {
			clearPage();
			showStatus(result.getMessage(), false);									// US-R3-V
			// Repeat the reason at the top of the page, where the lesson would have been, so it
			// is the first thing the user reads; it never says whether the lesson exists (US-R4)
			ViewLessonDetails.label_LessonTitle.setText(result.getMessage());
			ViewLessonDetails.label_LockBanner.setText("It may have been deleted, or it is not "
					+ "one of your lessons.  Press Return to go back to your list.");
			return;
		}
		showLesson(result.getLesson());												// US-R3
	}


	/**********
	 * <p> Method: performEdit() </p>
	 *
	 * <p> Description: Opens the Update Lesson page for this lesson (US-U1). </p>
	 */
	protected static void performEdit() {
		guiUpdateLesson.ViewUpdateLesson.displayUpdateLesson(ViewLessonDetails.theStage,
				ViewLessonDetails.theUser, ViewLessonDetails.theLessonId);
	}


	/**********
	 * <p> Method: performAddExperience() </p>
	 *
	 * <p> Description: Opens the Add Experience page for this lesson (US-U2). </p>
	 */
	protected static void performAddExperience() {
		guiAddExperience.ViewAddExperience.displayAddExperience(ViewLessonDetails.theStage,
				ViewLessonDetails.theUser, ViewLessonDetails.theLessonId);
	}


	/**********
	 * <p> Method: performDelete() </p>
	 *
	 * <p> Description: Delete (US-D1, US-D1-V), in two steps.  First the Model is asked with
	 * confirmed = false.  If it answers with a message, the deletion is refused (the lesson is
	 * locked or no longer exists), the message is shown, and NO confirmation is asked, because
	 * asking "Are you sure?" about something that cannot happen would only confuse.  An EMPTY
	 * message means the lesson can be deleted, so the "Are you sure?" dialog is shown with
	 * explicit Yes and No buttons.  Only "Yes" makes the second call with confirmed = true;
	 * "No" or closing the dialog changes nothing (Test Case "Delete Lesson - Declined
	 * Confirmation Cancels the Deletion").  After a successful deletion the user returns to the
	 * Contributor Home Page with the confirmation "Lesson deleted". </p>
	 */
	protected static void performDelete() {
		int id = ViewLessonDetails.theLessonId;
		LessonResult check = ModelLessonDetails.deleteLesson(currentUser(), id, false);
		if (!check.getMessage().isEmpty()) {
			showStatus(check.getMessage(), false);									// US-D1-V
			return;
		}

		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Delete Lesson");
		confirm.setHeaderText("Are you sure?");
		confirm.setContentText("Delete \"" + ViewLessonDetails.label_LessonTitle.getText()
				+ "\" and its experience entries?  This cannot be undone.");
		confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		Optional<ButtonType> answer = confirm.showAndWait();

		// US-D1: only an explicit "Yes" deletes
		if (answer.isPresent() && answer.get() == ButtonType.YES) {
			LessonResult result = ModelLessonDetails.deleteLesson(currentUser(), id, true);
			if (result.isSuccess())
				guiContributorHome.ViewContributorHome.displayContributorHome(
						ViewLessonDetails.theStage, ViewLessonDetails.theUser, result.getMessage());
			else
				showStatus(result.getMessage(), false);
		}
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Contributor Home Page. </p>
	 */
	protected static void performReturn() {
		guiContributorHome.ViewContributorHome.displayContributorHome(ViewLessonDetails.theStage,
				ViewLessonDetails.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewLessonDetails.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/**********
	 * <p> Method: showStatus(String message, boolean good) </p>
	 *
	 * <p> Description: Shows a status message: green for good news, red for a problem. </p>
	 *
	 * @param message	the text to show; an empty String clears it
	 * @param good		true for a confirmation, false for a problem
	 */
	protected static void showStatus(String message, boolean good) {
		ViewLessonDetails.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewLessonDetails.label_Status.setText(message == null ? "" : message);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: showLesson(LessonsLearned lesson) </p>
	 *
	 * <p> Description: Fills the page with one lesson.  Every locked field is marked "(locked)"
	 * beside its name, and the banner explains in words what the locks mean (US-R3).  Edit is
	 * disabled when all four core fields are locked, because there would be nothing to edit;
	 * Delete stays enabled so that a Contributor who tries it is told why it is refused
	 * (US-D1-V), and a hint beneath it says so in advance. </p>
	 *
	 * @param lesson the lesson to show, with its experience entries loaded
	 */
	private static void showLesson(LessonsLearned lesson) {
		ViewLessonDetails.label_LessonTitle.setText(lesson.getTitle()
				+ lockMark(lesson, LessonsLearned.FIELD_TITLE));
		ViewLessonDetails.label_Category.setText("Category: " + lesson.getCategory()
				+ lockMark(lesson, LessonsLearned.FIELD_CATEGORY));
		String tags = lesson.getTags().isEmpty() ? "(none)" : lesson.getTags();
		ViewLessonDetails.label_Tags.setText("Tags: " + tags
				+ lockMark(lesson, LessonsLearned.FIELD_TAGS));
		ViewLessonDetails.label_DescriptionCaption.setText("Description"
				+ lockMark(lesson, LessonsLearned.FIELD_DESCRIPTION));
		ViewLessonDetails.text_Description.setText(lesson.getDescription());
		ViewLessonDetails.label_Dates.setText("Created " + formatDate(lesson.getCreatedAt())
				+ "   |   Last updated " + formatDate(lesson.getUpdatedAt()));
		ViewLessonDetails.label_LockBanner.setText(lockBanner(lesson));

		List<ExperienceEntry> entries = lesson.getExperienceEntries();
		ViewLessonDetails.label_ExperienceHeading.setText("Experience log (" + entries.size()
				+ (entries.size() == 1 ? " entry)" : " entries)"));
		ViewLessonDetails.text_Experience.setText(formatExperience(entries));

		boolean everythingLocked = true;
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_TAGS,
				LessonsLearned.FIELD_CATEGORY })
			everythingLocked = everythingLocked && lesson.isFieldLocked(field);
		ViewLessonDetails.button_Edit.setDisable(everythingLocked);
		ViewLessonDetails.button_AddExperience.setDisable(false);
		ViewLessonDetails.button_Delete.setDisable(false);
		setContentVisible(true);
		ViewLessonDetails.label_DeleteHint.setText(lesson.isLocked()
				? "A locked lesson, or a lesson with a locked field, cannot be deleted." : "");
	}


	/**********
	 * <p> Method: clearPage() </p>
	 *
	 * <p> Description: Empties every content widget and disables every action.  Used when the
	 * lesson cannot be shown, so nothing from a previous visit remains on the page. </p>
	 */
	private static void clearPage() {
		ViewLessonDetails.label_LessonTitle.setText("");
		ViewLessonDetails.label_LockBanner.setText("");
		ViewLessonDetails.label_Category.setText("");
		ViewLessonDetails.label_Tags.setText("");
		ViewLessonDetails.label_DescriptionCaption.setText("");
		ViewLessonDetails.text_Description.setText("");
		ViewLessonDetails.label_Dates.setText("");
		ViewLessonDetails.label_ExperienceHeading.setText("");
		ViewLessonDetails.text_Experience.setText("");
		ViewLessonDetails.label_DeleteHint.setText("");
		ViewLessonDetails.button_Edit.setDisable(true);
		ViewLessonDetails.button_AddExperience.setDisable(true);
		ViewLessonDetails.button_Delete.setDisable(true);
		setContentVisible(false);
	}


	/**********
	 * <p> Method: setContentVisible(boolean visible) </p>
	 *
	 * <p> Description: Shows or hides the description box, the experience log, and the danger
	 * zone.  They are hidden when no lesson can be shown, so the "Lesson not found." page does
	 * not display empty boxes that look like a lesson failed to load. </p>
	 *
	 * @param visible true to show the lesson content, false to hide it
	 */
	private static void setContentVisible(boolean visible) {
		ViewLessonDetails.text_Description.setVisible(visible);
		ViewLessonDetails.text_Experience.setVisible(visible);
		ViewLessonDetails.label_DangerZone.setVisible(visible);
		ViewLessonDetails.button_Delete.setVisible(visible);
	}


	/**********
	 * <p> Method: lockMark(LessonsLearned lesson, String field) </p>
	 *
	 * <p> Description: "   (locked)" if the field is locked (by its own flag or because the whole
	 * lesson is locked), otherwise nothing. </p>
	 *
	 * @param lesson	the lesson being shown
	 * @param field		one of the four core field names
	 * @return			the text to append after the field
	 */
	private static String lockMark(LessonsLearned lesson, String field) {
		return lesson.isFieldLocked(field) ? "   (locked)" : "";
	}


	/**********
	 * <p> Method: lockBanner(LessonsLearned lesson) </p>
	 *
	 * <p> Description: One sentence that names what is locked and what that prevents, or an
	 * empty String if nothing is locked. </p>
	 *
	 * @param lesson	the lesson being shown
	 * @return			the banner text
	 */
	private static String lockBanner(LessonsLearned lesson) {
		if (lesson.isLockedEntire())
			return "A curator has locked this whole lesson.  It cannot be edited or deleted, "
					+ "but you can still add experience.";
		List<String> locked = new ArrayList<String>();
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_TAGS,
				LessonsLearned.FIELD_CATEGORY })
			if (lesson.isFieldLocked(field)) locked.add(field);
		if (locked.isEmpty()) return "";
		return "A curator has locked: " + String.join(", ", locked) + ".  Locked fields cannot "
				+ "be edited, and a lesson with a locked field cannot be deleted.";
	}


	/**********
	 * <p> Method: formatExperience(List&lt;ExperienceEntry&gt; entries) </p>
	 *
	 * <p> Description: The text of the experience log: one paragraph per entry with its date,
	 * hours, and description, followed by the team members' shares when there are any. </p>
	 *
	 * @param entries	the entries, oldest first
	 * @return			the log text, or a short note if there are no entries yet
	 */
	private static String formatExperience(List<ExperienceEntry> entries) {
		if (entries.isEmpty())
			return "No experience entries yet.  Use Add Experience after the lesson has been "
					+ "reused.";
		StringBuilder log = new StringBuilder();
		for (ExperienceEntry entry : entries) {
			if (log.length() > 0) log.append("\n\n");
			log.append(formatDate(entry.getCreatedAt())).append("   ")
				.append(formatHours(entry.getTimeEffortHours())).append(" h   ")
				.append(entry.getDescription());
			List<String> shares = new ArrayList<String>();
			for (MemberEffort share : entry.getMemberEfforts())
				shares.add(share.getMemberName() + " " + formatHours(share.getHours()) + " h");
			if (!shares.isEmpty()) log.append("\n   Team: ").append(String.join(", ", shares));
		}
		return log.toString();
	}


	/**********
	 * <p> Method: formatHours(double hours) </p>
	 *
	 * <p> Description: Hours without a needless ".0", so 3.0 shows as "3" and 3.25 as "3.25". </p>
	 *
	 * @param hours	the number of hours
	 * @return		the hours as text
	 */
	private static String formatHours(double hours) {
		return BigDecimal.valueOf(hours).stripTrailingZeros().toPlainString();
	}


	/**********
	 * <p> Method: formatDate(long epochMillis) </p>
	 *
	 * <p> Description: A stored time shown as a date in the computer's time zone. </p>
	 *
	 * @param epochMillis	the time in epoch milliseconds
	 * @return				the date, for example 2026-10-14
	 */
	private static String formatDate(long epochMillis) {
		return DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
	}


	/**********
	 * <p> Method: currentUser() </p>
	 *
	 * <p> Description: The username of whoever is logged in.  Every Model call is made with this
	 * name, so a Contributor only ever works with their own lessons (US-R4). </p>
	 *
	 * @return the logged-in user's username
	 */
	private static String currentUser() {
		return ViewLessonDetails.theUser.getUserName();
	}
}
