package guiCreateLesson;

import database.SharedAiService;
import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import guiWritingHelp.DraftTarget;
import guiWritingHelp.ViewWritingHelp;
import javafx.application.Platform;
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
 * @author Virgil Jones
 *
 * @version 1.00		2026-10-14 Initial version for TP2
 * @version 1.01		2026-11-16 TP3 Aspect 7: AI coaching workflow and aiAssisted tracking
 * @version 1.02		2026-11-17 Emilia Davey, TP3 Aspect 7: per-user AI consent; performWritingHelp
 * 							and the draft hookup for the Writing Help window
 */

public class ControllerCreateLesson {
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
	/** Tracks whether the Contributor applied an AI coaching suggestion on this draft. */
	private static boolean aiAssistedUsed = false;

	/**
	 * Default constructor is not used.
	 */
	public ControllerCreateLesson() {
	}


	/**********
	 * <p> Method: resetForm() </p>
	 *
	 * <p> Description: Clears every field, error, and message, and selects the "<Select
	 * one>" category prompt, so each visit starts with a blank form. </p>
	 */
	protected static void resetForm() {
		ViewCreateLesson.text_Title.clear();
		ViewCreateLesson.text_Description.clear();
		ViewCreateLesson.text_RootCause.clear();
		ViewCreateLesson.text_Recommendation.clear();
		ViewCreateLesson.text_Tags.clear();
		ViewCreateLesson.combobox_Category.getSelectionModel().select(0);
		aiAssistedUsed = false;
		ViewCreateLesson.label_AiAppliedBadge.setText("");
		ViewCreateLesson.vbox_CoachPanel.setVisible(false);
		ViewCreateLesson.button_Coach.setDisable(false);
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
				LessonsLearned.FIELD_DESCRIPTION, LessonsLearned.FIELD_ROOT_CAUSE,
				LessonsLearned.FIELD_RECOMMENDATION, LessonsLearned.FIELD_TAGS,
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
	 * @param field one of LessonsLearned.FIELD_TITLE, FIELD_DESCRIPTION, FIELD_ROOT_CAUSE,
	 * 		FIELD_RECOMMENDATION, FIELD_TAGS, or FIELD_CATEGORY
	 */
	protected static void checkField(String field) {
		errorLabelFor(field).setText(LessonsLearned.validateField(field, valueOf(field)));
	}


	/**********
	 * <p> Method: performCoach() </p>
	 *
	 * <p> Description: Requests optional AI writing feedback through SharedAiService after
	 * confirming one-time user consent (TP3 Aspect 7 / Epic E1). </p>
	 */
	protected static void performCoach() {
		String title = ViewCreateLesson.text_Title.getText().trim();
		String description = ViewCreateLesson.text_Description.getText().trim();
		if (title.isEmpty() || description.isEmpty()) {
			ViewCreateLesson.label_Status.setText(
					"Enter a title and description first before requesting AI coaching.");
			return;
		}
		// The user name is read here, on the JavaFX thread, for the consent check and the
		// request, so the worker thread never reads the page's user after it changed
		final String userName = ViewCreateLesson.theUser.getUserName();
		if (!aiService.requestConsentIfNeeded(userName)) {
			ViewCreateLesson.label_Status.setText(SharedAiService.CONSENT_DECLINED_MESSAGE);
			return;
		}

		ViewCreateLesson.label_Status.setText("");
		ViewCreateLesson.button_Coach.setDisable(true);
		ViewCreateLesson.label_CoachStatus.setText("Analyzing your draft...");
		ViewCreateLesson.text_CoachFeedback.setText("");
		ViewCreateLesson.button_ApplyDescription.setDisable(true);
		ViewCreateLesson.button_ApplyRecommendation.setDisable(true);
		ViewCreateLesson.vbox_CoachPanel.setVisible(true);
		ViewCreateLesson.vbox_CoachPanel.toFront();

		StringBuilder combinedDraft = new StringBuilder(description);
		String rc = ViewCreateLesson.text_RootCause.getText().trim();
		String rec = ViewCreateLesson.text_Recommendation.getText().trim();
		if (!rc.isEmpty()) combinedDraft.append("\nRoot cause: ").append(rc);
		if (!rec.isEmpty()) combinedDraft.append("\nRecommendation: ").append(rec);

		Thread worker = new Thread(() -> {
			try {
				String feedback = aiService.requestCoaching(userName, title, combinedDraft.toString(),
						COACH_TEXT_MAX);
				Platform.runLater(() -> {
					ViewCreateLesson.label_CoachStatus.setText(
							"Review suggestions below. Choose Apply to use them or Dismiss:");
					ViewCreateLesson.text_CoachFeedback.setText(feedback);
					ViewCreateLesson.button_ApplyDescription.setDisable(false);
					ViewCreateLesson.button_ApplyRecommendation.setDisable(false);
					ViewCreateLesson.button_Coach.setDisable(false);
				});
			} catch (Exception ex) {
				Platform.runLater(() -> {
					ViewCreateLesson.label_CoachStatus.setText(SharedAiService.UNAVAILABLE_MESSAGE);
					ViewCreateLesson.button_Coach.setDisable(false);
				});
			}
		});
		worker.setDaemon(true);
		worker.start();
	}


	/**********
	 * <p> Method: applyToDescription() </p>
	 *
	 * <p> Description: Explicitly applies the AI suggestion to Description and marks the lesson
	 * as AI-assisted. </p>
	 */
	protected static void applyToDescription() {
		String suggestion = ViewCreateLesson.text_CoachFeedback.getText().trim();
		if (suggestion.isEmpty()) return;
		String current = ViewCreateLesson.text_Description.getText().trim();
		String updated = current.isEmpty() ? suggestion : current + "\n\n" + suggestion;
		ViewCreateLesson.text_Description.setText(updated);
		markAiAssisted();
		dismissCoach();
		checkField(LessonsLearned.FIELD_DESCRIPTION);
	}


	/**********
	 * <p> Method: applyToRecommendation() </p>
	 *
	 * <p> Description: Explicitly applies the AI suggestion to Recommendation and marks the
	 * lesson as AI-assisted. </p>
	 */
	protected static void applyToRecommendation() {
		String suggestion = ViewCreateLesson.text_CoachFeedback.getText().trim();
		if (suggestion.isEmpty()) return;
		String current = ViewCreateLesson.text_Recommendation.getText().trim();
		String updated = current.isEmpty() ? suggestion : current + " " + suggestion;
		ViewCreateLesson.text_Recommendation.setText(updated);
		markAiAssisted();
		dismissCoach();
		checkField(LessonsLearned.FIELD_RECOMMENDATION);
	}


	/**********
	 * <p> Method: dismissCoach() </p>
	 *
	 * <p> Description: Closes the AI Coach panel without modifying the user's draft. </p>
	 */
	protected static void dismissCoach() {
		ViewCreateLesson.vbox_CoachPanel.setVisible(false);
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
				valueOf(LessonsLearned.FIELD_CATEGORY),
				ViewCreateLesson.text_RootCause.getText(),
				ViewCreateLesson.text_Recommendation.getText(),
				aiAssistedUsed);

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
	 * <p> Method: performWritingHelp() </p>
	 *
	 * <p> Description: TP3 Aspect 7.  Opens the Writing Help window over this page, so the
	 * Contributor can brainstorm or look at their own lessons as examples without losing the
	 * draft (US-P-E1-01, US-P-E1-03).  Text the Contributor adds from the window goes through
	 * writingHelpTarget, so it is checked and marked as AI-assisted like coaching text. </p>
	 */
	protected static void performWritingHelp() {
		ViewWritingHelp.displayWritingHelp(ViewCreateLesson.theStage, ViewCreateLesson.theUser,
				writingHelpTarget);
	}

	/** The draft as the Writing Help window sees it.  Nothing is locked while creating. */
	private static final DraftTarget writingHelpTarget = new DraftTarget() {
		public String getText(String field) {
			return LessonsLearned.FIELD_DESCRIPTION.equals(field)
					? ViewCreateLesson.text_Description.getText()
					: ViewCreateLesson.text_Recommendation.getText();
		}
		public boolean isLocked(String field) {
			return false;
		}
		public void setTextFromAi(String field, String text) {
			if (LessonsLearned.FIELD_DESCRIPTION.equals(field))
				ViewCreateLesson.text_Description.setText(text);
			else
				ViewCreateLesson.text_Recommendation.setText(text);
			markAiAssisted();
			checkField(field);
		}
	};


	/**********
	 * Records that AI-assisted text was added to the draft and shows the badge, so a successful
	 * save keeps the AI-assistance indicator (US-P-E1-02).
	 */
	private static void markAiAssisted() {
		aiAssistedUsed = true;
		ViewCreateLesson.label_AiAppliedBadge.setText("AI-assisted suggestion applied");
	}


	/**********
	 * <p> Method: valueOf(String field) </p>
	 *
	 * <p> Description: The current value of one form field.  The "<Select one>" prompt in
	 * the category list is not a category, so it is reported as an empty choice. </p>
	 *
	 * @param field one of the form field names
	 *
	 * @return the field's current text
	 */
	private static String valueOf(String field) {
		switch (field) {
			case LessonsLearned.FIELD_TITLE: return ViewCreateLesson.text_Title.getText();
			case LessonsLearned.FIELD_DESCRIPTION: return ViewCreateLesson.text_Description.getText();
			case LessonsLearned.FIELD_ROOT_CAUSE: return ViewCreateLesson.text_RootCause.getText();
			case LessonsLearned.FIELD_RECOMMENDATION:
				return ViewCreateLesson.text_Recommendation.getText();
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
			case LessonsLearned.FIELD_ROOT_CAUSE: return ViewCreateLesson.label_RootCauseError;
			case LessonsLearned.FIELD_RECOMMENDATION:
				return ViewCreateLesson.label_RecommendationError;
			case LessonsLearned.FIELD_TAGS: return ViewCreateLesson.label_TagsError;
			case LessonsLearned.FIELD_CATEGORY: return ViewCreateLesson.label_CategoryError;
			default: return ViewCreateLesson.label_Status;
		}
	}


	/**********
	 * <p> Method: clearErrors() </p>
	 *
	 * <p> Description: Clears the field error labels. </p>
	 */
	private static void clearErrors() {
		ViewCreateLesson.label_TitleError.setText("");
		ViewCreateLesson.label_DescriptionError.setText("");
		ViewCreateLesson.label_RootCauseError.setText("");
		ViewCreateLesson.label_RecommendationError.setText("");
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
