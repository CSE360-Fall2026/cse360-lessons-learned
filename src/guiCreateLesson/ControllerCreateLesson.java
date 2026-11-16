package guiCreateLesson;

import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCreateLesson Class. </p>
 *
 * <p> Description: The Java/FX-based Create Lesson Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class.</p>
 *
 * <p> Two kinds of checking happen here, and they have different jobs:</p>
 * <ul>
 * <li> <b>Guidance while typing.</b>  updateFormState() and checkField() use the same rules
 * 		(through LessonsLearned.validateField()) to update the counters, show or clear an error
 * 		when the user leaves a field, and enable Create Lesson only when every field is valid.
 * 		This is the "validate proactively, not just on failure" pattern in User
 * 		Experience.pdf.</li>
 * <li> <b>The real check on save.</b>  performCreate() always sends the values to
 * 		ModelCreateLesson, which validates them again and refuses anything invalid (US-C1-V).
 * 		The button state is a convenience, not the safeguard: nothing is saved unless the Model
 * 		accepts it.</li>
 * </ul>
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

public class ControllerCreateLesson {

	/** Maximum title length shown by the counter (the rule itself is in LessonRecognizer). */
	private static final int TITLE_MAX = 100;
	/** Maximum description length shown by the counter. */
	private static final int DESCRIPTION_MAX = 5000;
	/** Maximum length of the whole tags field shown by the counter. */
	private static final int TAGS_MAX = 200;

	/**
	 * Default constructor is not used.
	 */
	public ControllerCreateLesson() {
	}


	/**********
	 * <p> Method: resetForm() </p>
	 *
	 * <p> Description: Clears every field, error, and message, and selects the "&lt;Select
	 * one&gt;" category prompt, so each visit starts with a blank form. </p>
	 */
	protected static void resetForm() {
		ViewCreateLesson.text_Title.clear();
		ViewCreateLesson.text_Description.clear();
		ViewCreateLesson.text_Tags.clear();
		ViewCreateLesson.combobox_Category.getSelectionModel().select(0);
		clearErrors();
		ViewCreateLesson.label_Status.setText("");
		updateFormState();
	}


	/**********
	 * <p> Method: updateFormState() </p>
	 *
	 * <p> Description: Called on every change to any field.  It updates the three counters,
	 * clears an error that has just been fixed, and enables Create Lesson only when every field
	 * is valid.  New errors are not shown while the user is still typing; checkField() shows
	 * them when the user leaves the field, which is less distracting. </p>
	 */
	protected static void updateFormState() {
		updateCounter(ViewCreateLesson.label_TitleCounter,
				ViewCreateLesson.text_Title.getText(), TITLE_MAX);
		updateCounter(ViewCreateLesson.label_DescriptionCounter,
				ViewCreateLesson.text_Description.getText(), DESCRIPTION_MAX);
		updateCounter(ViewCreateLesson.label_TagsCounter,
				ViewCreateLesson.text_Tags.getText(), TAGS_MAX);

		boolean allValid = true;
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_TAGS,
				LessonsLearned.FIELD_CATEGORY }) {
			boolean valid = LessonsLearned.validateField(field, valueOf(field)).isEmpty();
			if (valid) errorLabelFor(field).setText("");		// the user just fixed it
			allValid = allValid && valid;
		}
		ViewCreateLesson.button_CreateLesson.setDisable(!allValid);
		ViewCreateLesson.label_CreateHint.setVisible(!allValid);
	}


	/**********
	 * <p> Method: checkField(String field) </p>
	 *
	 * <p> Description: Called when the user leaves a field.  Shows that field's specific error
	 * beneath it, or clears the error if the field is now valid (US-C1-V; GitHub's "error
	 * messages displayed as soon as the user changes focus" in User Experience.pdf). </p>
	 *
	 * @param field one of LessonsLearned.FIELD_TITLE, FIELD_DESCRIPTION, FIELD_TAGS, or
	 * 		FIELD_CATEGORY
	 */
	protected static void checkField(String field) {
		errorLabelFor(field).setText(LessonsLearned.validateField(field, valueOf(field)));
	}


	/**********
	 * <p> Method: performCreate() </p>
	 *
	 * <p> Description: Sends the form to the Model (US-C1).  On success the user is taken to the
	 * new lesson's details with the confirmation "Lesson created".  On failure the specific
	 * message is shown beneath the field named in the result, the form keeps everything the user
	 * typed, and nothing is saved (US-C1-V). </p>
	 */
	protected static void performCreate() {
		clearErrors();
		ViewCreateLesson.label_Status.setText("");
		LessonResult result = ModelCreateLesson.createLesson(
				ViewCreateLesson.theUser.getUserName(),
				ViewCreateLesson.text_Title.getText(),
				ViewCreateLesson.text_Description.getText(),
				ViewCreateLesson.text_Tags.getText(),
				valueOf(LessonsLearned.FIELD_CATEGORY));

		if (!result.isSuccess()) {
			if (result.getFailedField().isEmpty())
				ViewCreateLesson.label_Status.setText(result.getMessage());
			else
				errorLabelFor(result.getFailedField()).setText(result.getMessage());	// US-C1-V
			return;
		}
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewCreateLesson.theStage,
				ViewCreateLesson.theUser, result.getLessonId(), result.getMessage());	// US-C1
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Contributor Home Page without saving anything. </p>
	 */
	protected static void performReturn() {
		guiContributorHome.ViewContributorHome.displayContributorHome(ViewCreateLesson.theStage,
				ViewCreateLesson.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewCreateLesson.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  An unsaved form is simply discarded. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: valueOf(String field) </p>
	 *
	 * <p> Description: The current value of one form field.  The "&lt;Select one&gt;" prompt in
	 * the category list is not a category, so it is reported as an empty choice. </p>
	 *
	 * @param field one of the four core field names
	 *
	 * @return the field's current text
	 */
	private static String valueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewCreateLesson.text_Title.getText();
			case LessonsLearned.FIELD_DESCRIPTION: return ViewCreateLesson.text_Description.getText();
			case LessonsLearned.FIELD_TAGS: return ViewCreateLesson.text_Tags.getText();
			default:
				String category = ViewCreateLesson.combobox_Category.getValue();
				return (category == null || category.startsWith("<")) ? "" : category;
		}
	}


	/**********
	 * <p> Method: errorLabelFor(String field) </p>
	 *
	 * <p> Description: The error label beneath one form field.  A field name that is not on this
	 * form (which should not happen) maps to the general status label so its message is still
	 * shown. </p>
	 *
	 * @param field a field name from a LessonResult
	 *
	 * @return the label that shows that field's error
	 */
	private static Label errorLabelFor(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewCreateLesson.label_TitleError;
			case LessonsLearned.FIELD_DESCRIPTION: return ViewCreateLesson.label_DescriptionError;
			case LessonsLearned.FIELD_TAGS: return ViewCreateLesson.label_TagsError;
			case LessonsLearned.FIELD_CATEGORY: return ViewCreateLesson.label_CategoryError;
			default: return ViewCreateLesson.label_Status;
		}
	}


	/**********
	 * <p> Method: clearErrors() </p>
	 *
	 * <p> Description: Clears the four field error labels. </p>
	 */
	private static void clearErrors() {
		ViewCreateLesson.label_TitleError.setText("");
		ViewCreateLesson.label_DescriptionError.setText("");
		ViewCreateLesson.label_TagsError.setText("");
		ViewCreateLesson.label_CategoryError.setText("");
	}


	/**********
	 * <p> Method: updateCounter(Label counter, String text, int max) </p>
	 *
	 * <p> Description: Shows "length / max", in red once the limit is exceeded. </p>
	 *
	 * @param counter	the counter label to update
	 * @param text		the field's current text
	 * @param max		the field's maximum length
	 */
	private static void updateCounter(Label counter, String text, int max) {
		int length = (text == null) ? 0 : text.length();
		counter.setText(length + " / " + max);
		counter.setTextFill(length > max ? Color.RED : Color.GRAY);
	}
}
