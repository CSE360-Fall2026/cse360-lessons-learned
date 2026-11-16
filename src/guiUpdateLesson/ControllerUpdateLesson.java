package guiUpdateLesson;

import java.util.ArrayList;
import java.util.List;

import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerUpdateLesson Class. </p>
 *
 * <p> Description: The Java/FX-based Update Lesson Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class: filling
 * the form with the stored lesson, disabling and labelling locked fields, live guidance while
 * the user edits, and saving through ModelUpdateLesson (US-U1, US-U1-V).</p>
 *
 * <p> As on the Create Lesson Page, the live checks are guidance only.  performSave() always
 * sends the values to the Model, which enforces the lock rule and the field rules itself.</p>
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

public class ControllerUpdateLesson {

	/** Maximum title length shown by the counter (the rule itself is in LessonRecognizer). */
	private static final int TITLE_MAX = 100;
	/** Maximum description length shown by the counter. */
	private static final int DESCRIPTION_MAX = 5000;
	/** Maximum length of the whole tags field shown by the counter. */
	private static final int TAGS_MAX = 200;

	/** The lesson as it was loaded, used to detect changes and to know which fields are locked.
	 * Null when the lesson could not be loaded. */
	private static LessonsLearned theOriginal = null;

	/**
	 * Default constructor is not used.
	 */
	public ControllerUpdateLesson() {
	}


	/**********
	 * <p> Method: loadForm() </p>
	 *
	 * <p> Description: Loads the lesson through the Model and fills the form.  Each locked field
	 * is disabled, "(locked)" is added to its caption, and the note beside the heading names the
	 * locked fields (US-U1-V).  If the lesson cannot be loaded ("Lesson not found."), the form is
	 * emptied and disabled and the message is shown (US-R3-V). </p>
	 */
	protected static void loadForm() {
		clearErrors();
		ViewUpdateLesson.label_Status.setText("");
		LessonResult result = ModelUpdateLesson.loadLessonForEdit(currentUser(),
				ViewUpdateLesson.theLessonId);
		if (!result.isSuccess()) {
			theOriginal = null;
			fillForm("", "", "", "<Select one>");
			setLocked(true, true, true, true);
			ViewUpdateLesson.label_LockNote.setText("");
			ViewUpdateLesson.label_Status.setText(result.getMessage());			// US-R3-V
			ViewUpdateLesson.button_SaveChanges.setDisable(true);
			ViewUpdateLesson.label_SaveHint.setVisible(false);
			return;
		}

		theOriginal = result.getLesson();
		fillForm(theOriginal.getTitle(), theOriginal.getDescription(), theOriginal.getTags(),
				theOriginal.getCategory());
		setLocked(theOriginal.isFieldLocked(LessonsLearned.FIELD_TITLE),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_TAGS),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_CATEGORY));

		List<String> locked = new ArrayList<String>();
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_TAGS,
				LessonsLearned.FIELD_CATEGORY })
			if (theOriginal.isFieldLocked(field)) locked.add(field);
		ViewUpdateLesson.label_LockNote.setText(locked.isEmpty() ? ""
				: "Locked by a curator and shown for reference only: "
					+ String.join(", ", locked) + ".");
		updateFormState();
	}


	/**********
	 * <p> Method: updateFormState() </p>
	 *
	 * <p> Description: Called on every change.  It updates the counters, clears an error that has
	 * just been fixed, and enables Save Changes only when at least one field differs from the
	 * stored lesson AND every editable field is valid. </p>
	 */
	protected static void updateFormState() {
		updateCounter(ViewUpdateLesson.label_TitleCounter,
				ViewUpdateLesson.text_Title.getText(), TITLE_MAX);
		updateCounter(ViewUpdateLesson.label_DescriptionCounter,
				ViewUpdateLesson.text_Description.getText(), DESCRIPTION_MAX);
		updateCounter(ViewUpdateLesson.label_TagsCounter,
				ViewUpdateLesson.text_Tags.getText(), TAGS_MAX);
		if (theOriginal == null) return;

		boolean allValid = true;
		boolean changed = false;
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_TAGS,
				LessonsLearned.FIELD_CATEGORY }) {
			if (!valueOf(field).trim().equals(storedValueOf(field))) changed = true;
			if (theOriginal.isFieldLocked(field)) continue;
			boolean valid = LessonsLearned.validateField(field, valueOf(field)).isEmpty();
			if (valid) errorLabelFor(field).setText("");
			allValid = allValid && valid;
		}
		boolean canSave = allValid && changed;
		ViewUpdateLesson.button_SaveChanges.setDisable(!canSave);
		ViewUpdateLesson.label_SaveHint.setVisible(!canSave);
	}


	/**********
	 * <p> Method: checkField(String field) </p>
	 *
	 * <p> Description: Called when the user leaves a field.  Shows the field's specific error
	 * beneath it, or clears the error once the field is valid (US-U1-V).  Locked fields cannot
	 * receive focus, so they are never checked here. </p>
	 *
	 * @param field one of the four core field names
	 */
	protected static void checkField(String field) {
		if (theOriginal == null || theOriginal.isFieldLocked(field)) return;
		errorLabelFor(field).setText(LessonsLearned.validateField(field, valueOf(field)));
	}


	/**********
	 * <p> Method: performSave() </p>
	 *
	 * <p> Description: Sends every field to the Model (US-U1).  On success the user returns to
	 * the lesson's details with the confirmation "Lesson updated".  On failure the specific
	 * message is shown beneath the field named in the result and the stored lesson is unchanged
	 * (US-U1-V). </p>
	 */
	protected static void performSave() {
		clearErrors();
		ViewUpdateLesson.label_Status.setText("");
		LessonResult result = ModelUpdateLesson.updateLesson(currentUser(),
				ViewUpdateLesson.theLessonId, ViewUpdateLesson.text_Title.getText(),
				ViewUpdateLesson.text_Description.getText(), ViewUpdateLesson.text_Tags.getText(),
				valueOf(LessonsLearned.FIELD_CATEGORY));
		if (!result.isSuccess()) {
			if (result.getFailedField().isEmpty())
				ViewUpdateLesson.label_Status.setText(result.getMessage());
			else
				errorLabelFor(result.getFailedField()).setText(result.getMessage());	// US-U1-V
			return;
		}
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewUpdateLesson.theStage,
				ViewUpdateLesson.theUser, ViewUpdateLesson.theLessonId, result.getMessage());
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the lesson's details without saving anything. </p>
	 */
	protected static void performReturn() {
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewUpdateLesson.theStage,
				ViewUpdateLesson.theUser, ViewUpdateLesson.theLessonId);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out the current user and proceeds to the normal login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewUpdateLesson.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Terminates the program.  Unsaved changes are discarded. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: fillForm(String title, String description, String tags, String category) </p>
	 *
	 * <p> Description: Puts values into the four inputs. </p>
	 *
	 * @param title			the title to show
	 * @param description	the description to show
	 * @param tags			the tags to show
	 * @param category		the category to select
	 */
	private static void fillForm(String title, String description, String tags,
			String category) {
		ViewUpdateLesson.text_Title.setText(title);
		ViewUpdateLesson.text_Description.setText(description);
		ViewUpdateLesson.text_Tags.setText(tags);
		ViewUpdateLesson.combobox_Category.setValue(category);
	}


	/**********
	 * <p> Method: setLocked(boolean title, boolean description, boolean tags, boolean category)
	 * </p>
	 *
	 * <p> Description: Disables the locked inputs and writes each caption, adding "(locked)" to
	 * the locked ones (US-R3, US-U1-V). </p>
	 *
	 * @param title			true if the title is locked
	 * @param description	true if the description is locked
	 * @param tags			true if the tags are locked
	 * @param category		true if the category is locked
	 */
	private static void setLocked(boolean title, boolean description, boolean tags,
			boolean category) {
		ViewUpdateLesson.text_Title.setDisable(title);
		ViewUpdateLesson.text_Description.setDisable(description);
		ViewUpdateLesson.text_Tags.setDisable(tags);
		ViewUpdateLesson.combobox_Category.setDisable(category);
		ViewUpdateLesson.label_TitleCaption.setText(
				"Title (required, up to 100 characters)" + (title ? "   (locked)" : ""));
		ViewUpdateLesson.label_DescriptionCaption.setText(
				"Description (required, 10 to 5000 characters)" + (description ? "   (locked)" : ""));
		ViewUpdateLesson.label_TagsCaption.setText(
				"Tags (optional, separated by commas, each up to 30 characters)"
				+ (tags ? "   (locked)" : ""));
		ViewUpdateLesson.label_CategoryCaption.setText(
				"Category (required)" + (category ? "   (locked)" : ""));
	}


	/**********
	 * <p> Method: valueOf(String field) </p>
	 *
	 * <p> Description: The current value of one form field.  The "&lt;Select one&gt;" prompt is
	 * reported as an empty choice. </p>
	 *
	 * @param field one of the four core field names
	 *
	 * @return the field's current text
	 */
	private static String valueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewUpdateLesson.text_Title.getText();
			case LessonsLearned.FIELD_DESCRIPTION: return ViewUpdateLesson.text_Description.getText();
			case LessonsLearned.FIELD_TAGS: return ViewUpdateLesson.text_Tags.getText();
			default:
				String category = ViewUpdateLesson.combobox_Category.getValue();
				return (category == null || category.startsWith("<")) ? "" : category;
		}
	}


	/**********
	 * <p> Method: storedValueOf(String field) </p>
	 *
	 * <p> Description: The value of one field in the lesson as it was loaded. </p>
	 *
	 * @param field one of the four core field names
	 *
	 * @return the stored value
	 */
	private static String storedValueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return theOriginal.getTitle();
			case LessonsLearned.FIELD_DESCRIPTION: return theOriginal.getDescription();
			case LessonsLearned.FIELD_TAGS: return theOriginal.getTags();
			default: return theOriginal.getCategory();
		}
	}


	/**********
	 * <p> Method: errorLabelFor(String field) </p>
	 *
	 * <p> Description: The error label beneath one form field; any other name maps to the
	 * general status label so its message is still shown. </p>
	 *
	 * @param field a field name from a LessonResult
	 *
	 * @return the label that shows that field's error
	 */
	private static Label errorLabelFor(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewUpdateLesson.label_TitleError;
			case LessonsLearned.FIELD_DESCRIPTION: return ViewUpdateLesson.label_DescriptionError;
			case LessonsLearned.FIELD_TAGS: return ViewUpdateLesson.label_TagsError;
			case LessonsLearned.FIELD_CATEGORY: return ViewUpdateLesson.label_CategoryError;
			default: return ViewUpdateLesson.label_Status;
		}
	}


	/**********
	 * <p> Method: clearErrors() </p>
	 *
	 * <p> Description: Clears the four field error labels. </p>
	 */
	private static void clearErrors() {
		ViewUpdateLesson.label_TitleError.setText("");
		ViewUpdateLesson.label_DescriptionError.setText("");
		ViewUpdateLesson.label_TagsError.setText("");
		ViewUpdateLesson.label_CategoryError.setText("");
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


	/**********
	 * <p> Method: currentUser() </p>
	 *
	 * <p> Description: The username of whoever is logged in (US-R4). </p>
	 *
	 * @return the logged-in user's username
	 */
	private static String currentUser() {
		return ViewUpdateLesson.theUser.getUserName();
	}
}
