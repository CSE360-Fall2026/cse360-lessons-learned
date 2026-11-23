package guiUpdateLesson;

import java.util.ArrayList;
import java.util.List;

import database.SharedAiService;
import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import guiWritingHelp.DraftTarget;
import guiWritingHelp.ViewWritingHelp;
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
 * <p> Tested by the TP2 semi-automated tests M4 and M5 in lessonTests.ManualGuiTests and by
 * M-CL5 and M-WH4 (Manual Tests.pdf). The Model methods these actions call are tested by
 * lessonTests.UpdateLessonTests and ContributorAiToolsTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 * @author Virgil Jones
 *
 * @version 1.00		2026-10-14 Initial version for TP2
 * @version 1.01		2026-11-16 TP3 Aspect 7: AI coaching workflow and aiAssisted tracking
 * @version 1.02		2026-11-17 Emilia Davey, TP3 Aspect 7: per-user AI consent; performWritingHelp
 * 							and the draft hookup for the Writing Help window, which refuses locked
 * 							fields; a shorter Tags caption, so it no longer overlaps the counter
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
		// The user name is read here, on the JavaFX thread, for the consent check and the
		// request, so the worker thread never reads the page's user after it changed
		final String userName = ViewUpdateLesson.theUser.getUserName();
		if (!aiService.requestConsentIfNeeded(userName)) {
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
				String feedback = aiService.requestCoaching(userName, title, combinedDraft.toString(),
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


	/**********
	 * <p> Method: applyToDescription() </p>
	 *
	 * <p> Description: TP3 Aspect 7.  Adds the coaching reply to the end of the Description,
	 * marks the draft as AI-assisted, and re-checks the field.  Nothing happens if the Description
	 * is locked, so a Curator's lock is never bypassed (US-P-E1-02). </p>
	 */
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


	/**********
	 * <p> Method: applyToRecommendation() </p>
	 *
	 * <p> Description: TP3 Aspect 7.  Adds the coaching reply to the end of the Recommendation,
	 * marks the draft as AI-assisted, and re-checks the field.  Nothing happens if the whole lesson
	 * is locked (US-P-E1-02). </p>
	 */
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


	/**********
	 * <p> Method: dismissCoach() </p>
	 *
	 * <p> Description: Hides the coaching panel without changing the draft (US-P-E1-01 V
	 * sentence). </p>
	 */
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


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Lesson Details page without saving the changes. </p>
	 */
	protected static void performReturn() {
		guiLessonDetails.ViewLessonDetails.displayLessonDetails(ViewUpdateLesson.theStage,
				ViewUpdateLesson.theUser, ViewUpdateLesson.theLessonId);
	}

	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out without saving the changes and returns to the login page. </p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewUpdateLesson.theStage);
	}

	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Ends the application without saving the changes. </p>
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
	 * writingHelpTarget, which refuses a locked field, so a Curator's lock is never bypassed
	 * (US-P-E1-02). </p>
	 */
	protected static void performWritingHelp() {
		ViewWritingHelp.displayWritingHelp(ViewUpdateLesson.theStage, ViewUpdateLesson.theUser,
				writingHelpTarget);
	}

	/** The draft as the Writing Help window sees it.  The locks follow the coaching panel: the
	 * Description has its own lock, and the Recommendation is locked only with the whole
	 * lesson. */
	private static final DraftTarget writingHelpTarget = new DraftTarget() {
		public String getText(String field) {
			return LessonsLearned.FIELD_DESCRIPTION.equals(field)
					? ViewUpdateLesson.text_Description.getText()
					: ViewUpdateLesson.text_Recommendation.getText();
		}
		public boolean isLocked(String field) {
			if (theOriginal == null) return true;
			return LessonsLearned.FIELD_DESCRIPTION.equals(field)
					? theOriginal.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION)
					: theOriginal.isLockedEntire();
		}
		public void setTextFromAi(String field, String text) {
			if (LessonsLearned.FIELD_DESCRIPTION.equals(field))
				ViewUpdateLesson.text_Description.setText(text);
			else
				ViewUpdateLesson.text_Recommendation.setText(text);
			markAiAssisted();
			checkField(field);
		}
	};


	/**********
	 * Records that AI-assisted text was added to the draft, shows the badge, and updates the
	 * form, so a successful save keeps the AI-assistance indicator (US-P-E1-02).
	 */
	private static void markAiAssisted() {
		aiAssistedUsed = true;
		ViewUpdateLesson.label_AiAppliedBadge.setText("AI-assisted suggestion applied");
		updateFormState();
	}


	/**********
	 * Fills every field of the form.
	 *
	 * @param title				the title
	 * @param description		the description
	 * @param tags				the tags
	 * @param category			the category
	 * @param rootCause			the root cause, or null
	 * @param recommendation	the recommendation, or null
	 */
	private static void fillForm(String title, String description, String tags,
			String category, String rootCause, String recommendation) {
		ViewUpdateLesson.text_Title.setText(title);
		ViewUpdateLesson.text_Description.setText(description);
		ViewUpdateLesson.text_Tags.setText(tags);
		ViewUpdateLesson.combobox_Category.setValue(category);
		ViewUpdateLesson.text_RootCause.setText(rootCause == null ? "" : rootCause);
		ViewUpdateLesson.text_Recommendation.setText(recommendation == null ? "" : recommendation);
	}


	/**********
	 * Disables each locked field and adds "(locked)" to its caption (Epic E6).  The root cause and
	 * recommendation are locked only with the whole lesson.
	 *
	 * @param title			true if the title is locked
	 * @param description	true if the description is locked
	 * @param tags			true if the tags are locked
	 * @param category		true if the category is locked
	 * @param entireLocked	true if the whole lesson is locked
	 */
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
				"Tags (optional, max 30 chars each)"
				+ (tags ? "   (locked)" : ""));
		ViewUpdateLesson.label_CategoryCaption.setText(
				"Category (required)" + (category ? "   (locked)" : ""));
	}


	/**********
	 * The current value of one form field.  The category prompt counts as no category.
	 *
	 * @param field	one of the LessonsLearned.FIELD_ names
	 * @return the value in the form
	 */
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


	/**********
	 * The saved value of one field, from the lesson as it was opened.
	 *
	 * @param field	one of the LessonsLearned.FIELD_ names
	 * @return the saved value
	 */
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


	/**********
	 * The error label under one field; the page status label for any other name.
	 *
	 * @param field	one of the LessonsLearned.FIELD_ names
	 * @return the label that shows that field's error
	 */
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


	/**********
	 * Clears every field's error message.
	 */
	private static void clearErrors() {
		ViewUpdateLesson.label_TitleError.setText("");
		ViewUpdateLesson.label_DescriptionError.setText("");
		ViewUpdateLesson.label_RootCauseError.setText("");
		ViewUpdateLesson.label_RecommendationError.setText("");
		ViewUpdateLesson.label_TagsError.setText("");
		ViewUpdateLesson.label_CategoryError.setText("");
	}


	/**********
	 * Shows a field's length as "n / max", in red once it is over the limit.
	 *
	 * @param counter	the counter label
	 * @param text		the field's text
	 * @param max		the field's limit
	 */
	private static void updateCounter(Label counter, String text, int max) {
		int length = (text == null) ? 0 : text.length();
		counter.setText(length + " / " + max);
		counter.setTextFill(length > max ? Color.RED : Color.GRAY);
	}


	/**********
	 * The logged-in user's name.
	 *
	 * @return the user name
	 */
	private static String currentUser() {
		return ViewUpdateLesson.theUser.getUserName();
	}
}