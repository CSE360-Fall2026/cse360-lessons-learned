package guiAddExperience;

import java.util.ArrayList;
import java.util.List;

import entityClasses.ExperienceEntry;
import guiTools.LessonResult;
import inputValidation.LessonRecognizer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/*******
 * <p> Title: ControllerAddExperience Class. </p>
 *
 * <p> Description: The Java/FX-based Add Experience Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class: resetting
 * the form for a lesson, adding and removing team member rows, live guidance while the user
 * types, and saving through ModelAddExperience (US-U2, US-U2-V).</p>
 *
 * <p> As on the other Contributor forms, the live checks are guidance only: performAdd() always
 * sends the values to the Model, which validates them again and saves nothing unless every value
 * is acceptable.</p>
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

public class ControllerAddExperience {

	/** Maximum description length shown by the counter (the rule itself is in LessonRecognizer). */
	private static final int DESCRIPTION_MAX = 2000;

	/**
	 * Default constructor is not used.
	 */
	public ControllerAddExperience() {
	}


	/**********
	 * <p> Method: resetForm() </p>
	 *
	 * <p> Description: Clears the form, removes every team member row, and shows which lesson
	 * the entry is for.  If the lesson cannot be loaded ("Lesson not found."), every input and
	 * button is disabled and the message is shown, so nothing can be saved against a lesson the
	 * Contributor does not own (US-R3-V). </p>
	 */
	protected static void resetForm() {
		ViewAddExperience.text_Hours.clear();
		ViewAddExperience.text_Description.clear();
		ViewAddExperience.vbox_Members.getChildren().clear();
		ViewAddExperience.memberNameFields.clear();
		ViewAddExperience.memberHoursFields.clear();
		ViewAddExperience.label_HoursError.setText("");
		ViewAddExperience.label_DescriptionError.setText("");
		ViewAddExperience.label_MembersError.setText("");
		ViewAddExperience.label_Status.setText("");

		LessonResult result = ModelAddExperience.loadLesson(currentUser(),
				ViewAddExperience.theLessonId);
		boolean found = result.isSuccess();
		ViewAddExperience.label_LessonName.setText(found
				? "For lesson: " + result.getLesson().getTitle() : "");
		ViewAddExperience.text_Hours.setDisable(!found);
		ViewAddExperience.text_Description.setDisable(!found);
		ViewAddExperience.button_AddMember.setDisable(!found);
		if (!found) ViewAddExperience.label_Status.setText(result.getMessage());	// US-R3-V
		updateFormState();
		if (!found) {
			ViewAddExperience.button_AddExperience.setDisable(true);
			ViewAddExperience.label_AddHint.setVisible(false);
		}
	}


	/**********
	 * <p> Method: performAddMemberRow() </p>
	 *
	 * <p> Description: Adds one blank team member row (name, hours, Remove) and moves the cursor
	 * into its name field.  "Add Team Member" is disabled once ExperienceEntry.MAX_MEMBERS rows
	 * exist, so the limit is visible before it is reached. </p>
	 */
	protected static void performAddMemberRow() {
		if (ViewAddExperience.memberNameFields.size() >= ExperienceEntry.MAX_MEMBERS) return;

		TextField name = new TextField();
		name.setFont(Font.font("Arial", 14));
		name.setPrefWidth(300);
		TextField hours = new TextField();
		hours.setFont(Font.font("Arial", 14));
		hours.setPrefWidth(100);
		hours.setPromptText("Hours");
		Button remove = new Button("Remove");
		remove.setFont(Font.font("Dialog", 13));
		HBox row = new HBox(8, name, hours, remove);
		row.setAlignment(Pos.CENTER_LEFT);

		name.textProperty().addListener((_, _, _) -> updateFormState());
		hours.textProperty().addListener((_, _, _) -> updateFormState());
		name.focusedProperty().addListener((_, _, focused) -> { if (!focused) checkMembers(); });
		hours.focusedProperty().addListener((_, _, focused) -> { if (!focused) checkMembers(); });
		remove.setOnAction((_) -> {
			int index = ViewAddExperience.vbox_Members.getChildren().indexOf(row);
			ViewAddExperience.vbox_Members.getChildren().remove(row);
			ViewAddExperience.memberNameFields.remove(index);
			ViewAddExperience.memberHoursFields.remove(index);
			numberRows();
			ViewAddExperience.label_MembersError.setText("");
			updateFormState();
		});

		ViewAddExperience.vbox_Members.getChildren().add(row);
		ViewAddExperience.memberNameFields.add(name);
		ViewAddExperience.memberHoursFields.add(hours);
		numberRows();
		updateFormState();
		name.requestFocus();
	}


	/**********
	 * <p> Method: updateFormState() </p>
	 *
	 * <p> Description: Called on every change.  It updates the description counter, clears an
	 * error that has just been fixed, limits the number of rows, and enables Add Experience only
	 * when the hours, the description, and every team member row are valid. </p>
	 */
	protected static void updateFormState() {
		String description = ViewAddExperience.text_Description.getText();
		int length = (description == null) ? 0 : description.length();
		ViewAddExperience.label_DescriptionCounter.setText(length + " / " + DESCRIPTION_MAX);
		ViewAddExperience.label_DescriptionCounter.setTextFill(
				length > DESCRIPTION_MAX ? Color.RED : Color.GRAY);

		boolean hoursValid = LessonRecognizer.checkForValidExperienceValue(
				ViewAddExperience.text_Hours.getText()).isEmpty();
		boolean descriptionValid = LessonRecognizer.checkForValidExperienceDescription(
				description).isEmpty();
		boolean membersValid = membersProblem().isEmpty();
		if (hoursValid) ViewAddExperience.label_HoursError.setText("");
		if (descriptionValid) ViewAddExperience.label_DescriptionError.setText("");
		if (membersValid) ViewAddExperience.label_MembersError.setText("");

		ViewAddExperience.button_AddMember.setDisable(ViewAddExperience.text_Hours.isDisabled()
				|| ViewAddExperience.memberNameFields.size() >= ExperienceEntry.MAX_MEMBERS);
		boolean allValid = hoursValid && descriptionValid && membersValid;
		ViewAddExperience.button_AddExperience.setDisable(!allValid);
		ViewAddExperience.label_AddHint.setVisible(!allValid);
	}


	/**********
	 * <p> Method: checkField(String field) </p>
	 *
	 * <p> Description: Called when the user leaves the hours or description field.  Shows the
	 * field's specific error beneath it, or clears it once the field is valid (US-U2-V). </p>
	 *
	 * @param field "experienceValue" or "experienceDescription"
	 */
	protected static void checkField(String field) {
		if (field.equals("experienceValue"))
			ViewAddExperience.label_HoursError.setText(LessonRecognizer
					.checkForValidExperienceValue(ViewAddExperience.text_Hours.getText()));
		else
			ViewAddExperience.label_DescriptionError.setText(LessonRecognizer
					.checkForValidExperienceDescription(ViewAddExperience.text_Description.getText()));
	}


	/**********
	 * <p> Method: checkMembers() </p>
	 *
	 * <p> Description: Called when the user leaves a team member field.  Shows the first problem
	 * in the team member list, naming its row, or clears the message once every row is
	 * valid. </p>
	 */
	protected static void checkMembers() {
		ViewAddExperience.label_MembersError.setText(membersProblem());
	}


	/**********
	 * <p> Method: performAdd() </p>
	 *
	 * <p> Description: Sends the form to the Model (US-U2).  On success the user returns to the
	 * lesson's details, where the new entry appears in the experience log, with the confirmation
	 * "Experience added".  On failure the specific message is shown beneath the related field
	 * and nothing is saved (US-U2-V). </p>
	 */
	protected static void performAdd() {
		ViewAddExperience.label_Status.setText("");
		List<String> names = new ArrayList<String>();
		List<String> hours = new ArrayList<String>();
		for (int ndx = 0; ndx < ViewAddExperience.memberNameFields.size(); ndx++) {
			names.add(ViewAddExperience.memberNameFields.get(ndx).getText());
			hours.add(ViewAddExperience.memberHoursFields.get(ndx).getText());
		}
		LessonResult result = ModelAddExperience.addExperience(currentUser(),
				ViewAddExperience.theLessonId, ViewAddExperience.text_Hours.getText(),
				ViewAddExperience.text_Description.getText(), names, hours);

		if (!result.isSuccess()) {
			switch (result.getFailedField()) {
				case "experienceValue":
					ViewAddExperience.label_HoursError.setText(result.getMessage()); break;
				case "experienceDescription":
					ViewAddExperience.label_DescriptionError.setText(result.getMessage()); break;
				case "memberName": case "memberHours": case "memberEfforts":
					ViewAddExperience.label_MembersError.setText(result.getMessage()); break;
				default:
					ViewAddExperience.label_Status.setText(result.getMessage());
			}
			return;
		}
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewAddExperience.theStage,
				ViewAddExperience.theUser, ViewAddExperience.theLessonId, result.getMessage());
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the lesson's details without saving anything. </p>
	 */
	protected static void performReturn() {
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewAddExperience.theStage,
				ViewAddExperience.theUser, ViewAddExperience.theLessonId);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewAddExperience.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  An unsaved entry is discarded. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: membersProblem() </p>
	 *
	 * <p> Description: The first problem in the team member list, using the same rules the Model
	 * applies: each row needs a valid name and valid hours, and the rows may not add up to more
	 * than the total time/effort.  The row number is included so the user knows which row to
	 * fix. </p>
	 *
	 * @return the message for the first problem, or an empty String if every row is valid
	 */
	private static String membersProblem() {
		double sum = 0;
		for (int ndx = 0; ndx < ViewAddExperience.memberNameFields.size(); ndx++) {
			String name = ViewAddExperience.memberNameFields.get(ndx).getText();
			String hours = ViewAddExperience.memberHoursFields.get(ndx).getText();
			String error = LessonRecognizer.checkForValidMemberName(name);
			if (error.isEmpty()) error = LessonRecognizer.checkForValidExperienceValue(hours);
			if (!error.isEmpty()) return "Team member " + (ndx + 1) + ": " + error;
			sum += Double.parseDouble(hours.trim());	// proven a plain number just above
		}
		String total = ViewAddExperience.text_Hours.getText();
		if (!ViewAddExperience.memberNameFields.isEmpty()
				&& LessonRecognizer.checkForValidExperienceValue(total).isEmpty()
				&& sum > Double.parseDouble(total.trim()) + 1e-9)
			return "Team member hours cannot add up to more than the total time/effort.";
		return "";
	}


	/**********
	 * <p> Method: numberRows() </p>
	 *
	 * <p> Description: Gives each name field a numbered prompt ("Team member 1", "Team member
	 * 2", ...) so the row numbers in error messages match what the user sees. </p>
	 */
	private static void numberRows() {
		for (int ndx = 0; ndx < ViewAddExperience.memberNameFields.size(); ndx++)
			ViewAddExperience.memberNameFields.get(ndx).setPromptText("Team member " + (ndx + 1));
	}


	/**********
	 * <p> Method: currentUser() </p>
	 *
	 * <p> Description: The username of whoever is logged in (US-R4). </p>
	 *
	 * @return the logged-in user's username
	 */
	private static String currentUser() {
		return ViewAddExperience.theUser.getUserName();
	}
}
