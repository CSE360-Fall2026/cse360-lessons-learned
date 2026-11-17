package guiUpdateLesson;

import java.util.ArrayList;
import java.util.List;

import database.SharedAiService;
import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerUpdateLesson Class. </p>
 *
 * <p> Description: The Java/FX-based Update Lesson Page.  This class provides the controller
 * actions based on the user's use of the JavaFX GUI widgets defined by the View class: filling
 * the form with the stored lesson, disabling and labeling locked fields, live guidance while
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
 * @author Virgil Jones
 *
 * @version 1.00		2026-10-14 Initial version for TP2
 * @version 1.01		2026-11-16 TP3 Aspect 7: AI coaching workflow and aiAssisted tracking
 */

public class ControllerUpdateLesson {

	/** Maximum title length shown by the counter (the rule itself is in LessonRecognizer). */
	private static final int TITLE_MAX = 100;
	/** Maximum description length shown by the counter. */
	private static final int DESCRIPTION_MAX = 5000;
	/** Maximum length of the whole tags field shown by the counter. */
	private static final int TAGS_MAX = 200;
	/** Maximum length for AI coached text fields (rootCause / recommendation). */
	private static final int COACH_TEXT_MAX = 2000;

	/** Shared AI service instance (Aspect 8). */
	private static final SharedAiService aiService = new SharedAiService();
	/** Tracks whether AI assistance was used on this lesson (stored or newly applied). */
	private static boolean aiAssistedUsed = false;

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
		ViewUpdateLesson.vbox_CoachPanel.setVisible(false);
		LessonResult result = ModelUpdateLesson.loadLessonForEdit(currentUser(),
				ViewUpdateLesson.theLessonId);
		if (!result.isSuccess()) {
			theOriginal = null;
			aiAssistedUsed = false;
			fillForm("", "", "", "<Select one>", "", "");
			setLocked(true, true, true, true, true);
			ViewUpdateLesson.label_LockNote.setText("");
			ViewUpdateLesson.label_AiAppliedBadge.setText("");
			ViewUpdateLesson.label_Status.setText(result.getMessage());			// US-R3-V
			ViewUpdateLesson.button_SaveChanges.setDisable(true);
			ViewUpdateLesson.button_Coach.setDisable(true);
			ViewUpdateLesson.label_SaveHint.setVisible(false);
			return;
		}

		theOriginal = result.getLesson();
		aiAssistedUsed = theOriginal.isAiAssisted();
		ViewUpdateLesson.label_AiAppliedBadge.setText(
				aiAssistedUsed ? "AI-assisted lesson" : "");
		fillForm(theOriginal.getTitle(), theOriginal.getDescription(), theOriginal.getTags(),
				theOriginal.getCategory(), theOriginal.getRootCause(),
				theOriginal.getRecommendation());
		setLocked(theOriginal.isFieldLocked(LessonsLearned.FIELD_TITLE),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_TAGS),
				theOriginal.isFieldLocked(LessonsLearned.FIELD_CATEGORY),
				theOriginal.isLockedEntire());
		ViewUpdateLesson.button_Coach.setDisable(theOriginal.isLockedEntire());

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
		boolean changed = (aiAssistedUsed != theOriginal.isAiAssisted());
		for (String field : new String[] { LessonsLearned.FIELD_TITLE,
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_ROOT_CAUSE,
				LessonsLearned.FIELD_RECOMMENDATION, LessonsLearned.FIELD_TAGS,
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
	 * @param field one of the form field names
	 */
	protected static void checkField(String field) {
		if (theOriginal == null || theOriginal.isFieldLocked(field)) return;
		errorLabelFor(field).setText(LessonsLearned.validateField(field, valueOf(field)));
	}


	/**********
	 * <p> Method: performCoach() </p>
	 *
	 * <p> Description: Requests optional AI writing feedback through SharedAiService after
	 * confirming one-time user consent (TP3 Aspect 7 / Epic E1). </p>
	 */
	protected static void performCoach() {
		if (theOriginal == null || theOriginal.isLockedEntire()) return;
		String title = ViewUpdateLesson.text_Title.getText().trim();
		String description = ViewUpdateLesson.text_Description.getText().trim();
		if (title.isEmpty() || description.isEmpty()) {
			ViewUpdateLesson.label_Status.setText(
					"Enter a title and description first before requesting AI coaching.");
			return;
		}
		if (!aiService.requestConsentIfNeeded()) {
			ViewUpdateLesson.label_Status.setText(SharedAiService.CONSENT_DECLINED_MESSAGE);
			return;
		}

		ViewUpdateLesson.label_Status.setText("");
		ViewUpdateLesson.button_Coach.setDisable(true);
		ViewUpdateLesson.label_CoachStatus.setText("Analyzing your draft...");
		ViewUpdateLesson.text_CoachFeedback.setText("");
		ViewUpdateLesson.button_ApplyDescription.setDisable(true);
		ViewUpdateLesson.button_ApplyRecommendation.setDisable(true);
		ViewUpdateLesson.vbox_CoachPanel.setVisible(true);
		ViewUpdateLesson.vbox_CoachPanel.toFront();

		StringBuilder combinedDraft = new StringBuilder(description);
		String rc = ViewUpdateLesson.text_RootCause.getText().trim();
		String rec = ViewUpdateLesson.text_Recommendation.getText().trim();
		if (!rc.isEmpty()) combinedDraft.append("\nRoot cause: ").append(rc);
		if (!rec.isEmpty()) combinedDraft.append("\nRecommendation: ").append(rec);

		boolean descLocked = theOriginal.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION);

		Thread worker = new Thread(() -> {
			try {
				String feedback = aiService.requestCoaching(title, combinedDraft.toString(),
						COACH_TEXT_MAX);
				Platform.runLater(() -> {
					ViewUpdateLesson.label_CoachStatus.setText(
							"Review suggestions below. Choose Apply to use them or Dismiss:");
					ViewUpdateLesson.text_CoachFeedback.setText(feedback);
					ViewUpdateLesson.button_ApplyDescription.setDisable(descLocked);
					ViewUpdateLesson.button_ApplyRecommendation.setDisable(false);
					ViewUpdateLesson.button_Coach.setDisable(false);
				});
			} catch (Exception ex) {
				Platform.runLater(() -> {
					ViewUpdateLesson.label_CoachStatus.setText(SharedAiService.UNAVAILABLE_MESSAGE);
					ViewUpdateLesson.button_Coach.setDisable(false);
				});
			}
		});
		worker.setDaemon(true);
		worker.start();
	}


	protected static void applyToDescription() {
		if (theOriginal != null && theOriginal.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION))
			return;
		String suggestion = ViewUpdateLesson.text_CoachFeedback.getText().trim();
		if (suggestion.isEmpty()) return;
		String current = ViewUpdateLesson.text_Description.getText().trim();
		String updated = current.isEmpty() ? suggestion : current + "\n\n" + suggestion;
		ViewUpdateLesson.text_Description.setText(updated);
		markAiAssisted();
		dismissCoach();
		checkField(LessonsLearned.FIELD_DESCRIPTION);
	}


	protected static void applyToRecommendation() {
		if (theOriginal != null && theOriginal.isLockedEntire()) return;
		String suggestion = ViewUpdateLesson.text_CoachFeedback.getText().trim();
		if (suggestion.isEmpty()) return;
		String current = ViewUpdateLesson.text_Recommendation.getText().trim();
		String updated = current.isEmpty() ? suggestion : current + " " + suggestion;
		ViewUpdateLesson.text_Recommendation.setText(updated);
		markAiAssisted();
		dismissCoach();
		checkField(LessonsLearned.FIELD_RECOMMENDATION);
	}


	protected static void dismissCoach() {
		ViewUpdateLesson.vbox_CoachPanel.setVisible(false);
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
				valueOf(LessonsLearned.FIELD_CATEGORY),
				ViewUpdateLesson.text_RootCause.getText(),
				ViewUpdateLesson.text_Recommendation.getText(),
				aiAssistedUsed);
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


	protected static void performReturn() {
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewUpdateLesson.theStage,
				ViewUpdateLesson.theUser, ViewUpdateLesson.theLessonId);
	}

	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewUpdateLesson.theStage);
	}

	protected static void performQuit() {
		System.exit(0);
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	private static void markAiAssisted() {
		aiAssistedUsed = true;
		ViewUpdateLesson.label_AiAppliedBadge.setText("AI-assisted suggestion applied");
		updateFormState();
	}


	private static void fillForm(String title, String description, String tags,
			String category, String rootCause, String recommendation) {
		ViewUpdateLesson.text_Title.setText(title);
		ViewUpdateLesson.text_Description.setText(description);
		ViewUpdateLesson.text_Tags.setText(tags);
		ViewUpdateLesson.combobox_Category.setValue(category);
		ViewUpdateLesson.text_RootCause.setText(rootCause == null ? "" : rootCause);
		ViewUpdateLesson.text_Recommendation.setText(recommendation == null ? "" : recommendation);
	}


	private static void setLocked(boolean title, boolean description, boolean tags,
			boolean category, boolean entireLocked) {
		ViewUpdateLesson.text_Title.setDisable(title);
		ViewUpdateLesson.text_Description.setDisable(description);
		ViewUpdateLesson.text_Tags.setDisable(tags);
		ViewUpdateLesson.combobox_Category.setDisable(category);
		ViewUpdateLesson.text_RootCause.setDisable(entireLocked);
		ViewUpdateLesson.text_Recommendation.setDisable(entireLocked);
		ViewUpdateLesson.label_TitleCaption.setText(
				"Title (required, up to 100 characters)" + (title ? "   (locked)" : ""));
		ViewUpdateLesson.label_DescriptionCaption.setText(
				"Description (required, 10 to 5000 characters)" + (description ? "   (locked)" : ""));
		ViewUpdateLesson.label_RootCauseCaption.setText(
				"Root Cause (optional, what went wrong & why, up to 2000 chars)"
				+ (entireLocked ? "   (locked)" : ""));
		ViewUpdateLesson.label_RecommendationCaption.setText(
				"Recommendation (optional, what to do next time, up to 2000 chars)"
				+ (entireLocked ? "   (locked)" : ""));
		ViewUpdateLesson.label_TagsCaption.setText(
				"Tags (optional, comma-separated, up to 30 chars each)"
				+ (tags ? "   (locked)" : ""));
		ViewUpdateLesson.label_CategoryCaption.setText(
				"Category (required)" + (category ? "   (locked)" : ""));
	}


	private static String valueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewUpdateLesson.text_Title.getText();
			case LessonsLearned.FIELD_DESCRIPTION: return ViewUpdateLesson.text_Description.getText();
			case LessonsLearned.FIELD_ROOT_CAUSE: return ViewUpdateLesson.text_RootCause.getText();
			case LessonsLearned.FIELD_RECOMMENDATION:
				return ViewUpdateLesson.text_Recommendation.getText();
			case LessonsLearned.FIELD_TAGS: return ViewUpdateLesson.text_Tags.getText();
			default:
				String category = ViewUpdateLesson.combobox_Category.getValue();
				return (category == null || category.startsWith("<")) ? "" : category;
		}
	}


	private static String storedValueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return theOriginal.getTitle();
			case LessonsLearned.FIELD_DESCRIPTION: return theOriginal.getDescription();
			case LessonsLearned.FIELD_ROOT_CAUSE: return theOriginal.getRootCause();
			case LessonsLearned.FIELD_RECOMMENDATION: return theOriginal.getRecommendation();
			case LessonsLearned.FIELD_TAGS: return theOriginal.getTags();
			default: return theOriginal.getCategory();
		}
	}


	private static Label errorLabelFor(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewUpdateLesson.label_TitleError;
			case LessonsLearned.FIELD_DESCRIPTION: return ViewUpdateLesson.label_DescriptionError;
			case LessonsLearned.FIELD_ROOT_CAUSE: return ViewUpdateLesson.label_RootCauseError;
			case LessonsLearned.FIELD_RECOMMENDATION:
				return ViewUpdateLesson.label_RecommendationError;
			case LessonsLearned.FIELD_TAGS: return ViewUpdateLesson.label_TagsError;
			case LessonsLearned.FIELD_CATEGORY: return ViewUpdateLesson.label_CategoryError;
			default: return ViewUpdateLesson.label_Status;
		}
	}


	private static void clearErrors() {
		ViewUpdateLesson.label_TitleError.setText("");
		ViewUpdateLesson.label_DescriptionError.setText("");
		ViewUpdateLesson.label_RootCauseError.setText("");
		ViewUpdateLesson.label_RecommendationError.setText("");
		ViewUpdateLesson.label_TagsError.setText("");
		ViewUpdateLesson.label_CategoryError.setText("");
	}


	private static void updateCounter(Label counter, String text, int max) {
		int length = (text == null) ? 0 : text.length();
		counter.setText(length + " / " + max);
		counter.setTextFill(length > max ? Color.RED : Color.GRAY);
	}


	private static String currentUser() {
		return ViewUpdateLesson.theUser.getUserName();
	}
}