package guiCuratorRefine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import applicationMain.FoundationsMain;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.RefinementDraft;
import guiTools.CurationNote;
import guiTools.LessonResult;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCuratorRefine Class. </p>
 *
 * <p> Description: The Java/FX-based Refine with AI page (TP3 Aspect 6; Epic E3).  This class
 * provides the controller actions based on the user's use of the JavaFX GUI widgets defined by
 * the View class.</p>
 *
 * <p> The Controller reacts to the widgets but does NOT check the role, validate text, or
 * touch the lesson itself.  It passes the logged-in user, the review snapshot, and the
 * Curator's choices to Daniel Martinez Julio's ModelCuratorRefine and shows whatever comes
 * back, in place on the page rather than in a pop-up (User Experience.pdf).  The pop-ups are
 * the shared AI notice, shown once per user (US-AI-D2-01), and the "Are you sure?" question
 * before a refinement is saved, the same kind the Curator Lock and Merge Statistics pages
 * use.</p>
 *
 * <p> The Controller keeps the Curator's final wording for each field separately, so switching
 * between Title and Description, or an AI failure, never loses what the Curator typed
 * (US-C-E3-08).  The AI request runs on a background thread, so the page does not freeze while
 * it waits; the page's request controls are disabled until the answer arrives.</p>
 *
 * <p> The class has been written assuming that the View or the Model are the only class methods
 * that can invoke these methods.  This is why each has been declared "protected".  Do not change
 * any of these methods to public.</p>
 *
 * <p> Tested by the manual tests M-RF1 to M-RF7 in Manual Tests.pdf.  The Model methods these
 * actions call are tested by curatorRefinementTests.CuratorRefinementTests and
 * RefinePageTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-18 Initial version for TP3 Aspect 6
 */
public class ControllerCuratorRefine {

	/** Shown while the AI is working. */
	protected static final String WAITING = "Waiting for the AI...";
	/** Shown when a suggestion arrives.  Source: US-C-E3-04. */
	protected static final String REVIEW = "Review the suggestion and edit it if needed, then "
			+ "press Save Refinement, or Discard Changes.";
	/** Shown after Discard Changes.  Source: US-C-E3-04 V sentence. */
	protected static final String DISCARDED = "Changes discarded. Nothing was saved.";

	/** The shared AI service.  Not final, so a test harness can substitute a simulated
	 * provider; the application always uses the default, which sends requests to Groq. */
	private static SharedAiService aiService = new SharedAiService();
	/** The Model for the current visit.  A new one is made each time the page is displayed. */
	private static ModelCuratorRefine theModel;
	/** The lesson as it was when the page was opened or last saved; null if it could not be
	 * opened.  Source: US-C-E3-06, the stale-review check at save time. */
	private static RefinementDraft theDraft;
	/** The Curator's final wording for each field, title first.  Source: US-C-E3-04 and
	 * US-C-E3-08, "the Curator's unsaved draft is preserved". */
	private static Map<String, String> finalText = new LinkedHashMap<String, String>();
	/** The field whose texts are on screen, or null before the first one is shown. */
	private static String shownField;
	/** Counts page visits and reloads, so an AI answer that arrives after the Curator has left
	 * or reloaded the page is ignored instead of being shown on the wrong lesson. */
	private static int visit = 0;
	/** True while an AI request is running. */
	private static boolean busy = false;

	/**
	 * Default constructor is not used.
	 */
	public ControllerCuratorRefine() {
	}


	/**********
	 * <p> Method: loadPage() </p>
	 *
	 * <p> Description: Fills the page for the lesson with a new Model whose AI requests go
	 * through the shared service for this Curator.  The description is shown first, because it
	 * is the field most often refined.  If the user is not a Curator or the lesson no longer
	 * exists, the reason is shown where the title would have been, no lesson text is shown, and
	 * every action is disabled, so the only thing left to do is return (US-C-E3-02). </p>
	 */
	protected static void loadPage() {
		theModel = new ModelCuratorRefine(FoundationsMain.database,
				ModelCuratorRefine.sharedProvider(aiService,
						ViewCuratorRefine.theUser.getUserName()));
		ViewCuratorRefine.text_Goal.clear();
		openLesson(LessonsLearned.FIELD_DESCRIPTION);
		showStatus("", true);
	}


	/**********
	 * <p> Method: performFieldChanged() </p>
	 *
	 * <p> Description: Shows the chosen field: its stored text on the left and the Curator's
	 * final wording for it on the right.  The wording for the field being left is kept, so
	 * nothing typed is lost by switching (US-C-E3-03). </p>
	 */
	protected static void performFieldChanged() {
		if (theDraft == null) return;
		if (shownField != null) finalText.put(shownField, ViewCuratorRefine.text_Final.getText());
		shownField = selectedField();
		boolean title = LessonsLearned.FIELD_TITLE.equals(shownField);
		ViewCuratorRefine.label_OriginalHeading.setText(title ? "Original title (stored)"
				: "Original description (stored)");
		ViewCuratorRefine.label_FinalHeading.setText(title ? "Final title (edit before saving)"
				: "Final description (edit before saving)");
		ViewCuratorRefine.text_Original.setText(original(shownField));
		ViewCuratorRefine.text_Final.setText(finalText.get(shownField));
		performFinalEdited();
	}


	/**********
	 * <p> Method: performFinalEdited() </p>
	 *
	 * <p> Description: Updates the character count and the Discard and Save buttons as the
	 * final wording changes.  Nothing is saved (US-C-E3-04, "editing a suggestion does not
	 * save it automatically"). </p>
	 */
	protected static void performFinalEdited() {
		if (theDraft == null || shownField == null) return;
		String text = ViewCuratorRefine.text_Final.getText();
		int max = LessonsLearned.FIELD_TITLE.equals(shownField) ? 100 : 5000;
		ViewCuratorRefine.label_Count.setText(text.length() + " / " + max + " characters"
				+ (text.length() > max ? "  (too long)" : ""));
		ViewCuratorRefine.label_Count.setTextFill(text.length() > max ? Color.RED : Color.GRAY);
		updateButtons();
	}


	/**********
	 * <p> Method: performSuggest() </p>
	 *
	 * <p> Description: Asks the AI to rewrite the chosen field (US-C-E3-03).  The shared AI
	 * notice is shown first if this Curator has not answered it; Cancel sends nothing.  The
	 * request then runs on a background thread through the Model, which checks the role, the
	 * lesson's revision, the goal, and the consent again before anything is sent.  The answer
	 * replaces the final wording for that field only; the stored lesson is not changed. </p>
	 */
	protected static void performSuggest() {
		if (theDraft == null || busy) return;
		finalText.put(shownField, ViewCuratorRefine.text_Final.getText());
		String userName = ViewCuratorRefine.theUser.getUserName();
		if (!aiService.requestConsentIfNeeded(userName)) {
			showStatus(SharedAiService.CONSENT_DECLINED_MESSAGE, false);
			return;
		}
		// Everything the background thread needs is copied first, so it never reads the page
		final int myVisit = visit;
		final ModelCuratorRefine model = theModel;
		final RefinementDraft draft = theDraft;
		final String field = shownField;
		final String goal = ViewCuratorRefine.text_Goal.getText();
		final entityClasses.User user = ViewCuratorRefine.theUser;
		final boolean consented = SharedAiService.hasUserConsented(userName);
		setBusy(true);
		showStatus(WAITING, true);
		Thread worker = new Thread(() -> {
			ModelCuratorRefine.Review review;
			try {
				review = model.requestSuggestion(user, draft, field, goal, consented);
			} catch (RuntimeException unexpected) {
				// For example, the database closed while waiting; shown as the AI being unavailable
				review = null;
			}
			final ModelCuratorRefine.Review answer = review;
			Platform.runLater(() -> finishSuggest(myVisit, field, answer));
		}, "curator-refine-ai");
		// A waiting request never keeps the application running after Quit
		worker.setDaemon(true);
		worker.start();
	}


	/**********
	 * <p> Method: performDiscard() </p>
	 *
	 * <p> Description: Puts the stored text back as the final wording of the chosen field.
	 * Nothing is saved, and the stored lesson and its attribution are unchanged (US-C-E3-04,
	 * "discarding a suggestion leaves the persisted lesson and its attribution unchanged"). </p>
	 */
	protected static void performDiscard() {
		if (theDraft == null || busy) return;
		finalText.put(shownField, original(shownField));
		ViewCuratorRefine.text_Final.setText(original(shownField));
		showStatus(DISCARDED, true);
	}


	/**********
	 * <p> Method: performSave() </p>
	 *
	 * <p> Description: Saves every field whose final wording differs from the stored text,
	 * after an explicit Yes (US-C-E3-04, US-C-E3-05).  The question names the fields and says
	 * that the original text is kept.  "No" saves nothing and keeps the wording.  The Model
	 * validates the final text with the normal lesson rules, refuses if the lesson changed since
	 * it was opened, and saves the text, the first originals, and the Curator's name and time
	 * together (US-C-E3-06).  After a save the page shows the stored lesson again, with its
	 * new "Edited by curator" note (US-C-E3-07). </p>
	 */
	protected static void performSave() {
		if (theDraft == null || busy) return;
		finalText.put(shownField, ViewCuratorRefine.text_Final.getText());
		Map<String, String> changes = changedFields();
		if (changes.isEmpty()) {
			showStatus(ModelCuratorRefine.NO_CHANGE, true);
			return;
		}

		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Save Refinement");
		confirm.setHeaderText("Are you sure?");
		confirm.setContentText(confirmationQuestion(changes.keySet(), theDraft.title(),
				ViewCuratorRefine.theUser.getUserName()));
		confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
		Optional<ButtonType> answer = confirm.showAndWait();
		if (!answer.isPresent() || answer.get() != ButtonType.YES) {
			showStatus(ModelCuratorRefine.NO_CHANGE, true);
			return;
		}

		LessonResult result = theModel.saveRefinement(ViewCuratorRefine.theUser, theDraft,
				changes, true);
		if (result.isSuccess()) {
			// The stored lesson changed, so a new snapshot is taken for any further refinement
			openLesson(shownField);
			showStatus(result.getMessage(), true);
			return;
		}
		if (ModelCuratorRefine.NOT_CURATOR.equals(result.getMessage())) {
			closePage(result.getMessage());
			return;
		}
		// Show the field the message is about, with the Curator's wording still there to fix
		String failed = result.getFailedField();
		if (LessonsLearned.FIELD_TITLE.equals(failed)) ViewCuratorRefine.radio_Title.setSelected(true);
		if (LessonsLearned.FIELD_DESCRIPTION.equals(failed))
			ViewCuratorRefine.radio_Description.setSelected(true);
		showStatus(result.getMessage(), false);
	}


	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Returns to the Curator Home Page.  Unsaved wording is discarded, and an
	 * AI answer still on its way is ignored. </p>
	 */
	protected static void performReturn() {
		visit++;
		guiCuratorHome.ViewCuratorHome.displayCuratorHome(ViewCuratorRefine.theStage,
				ViewCuratorRefine.theUser);
	}


	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Logs out and shows the login page.  Unsaved wording is discarded. </p>
	 */
	protected static void performLogout() {
		visit++;
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewCuratorRefine.theStage);
	}


	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Ends the application. </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}


	/**********
	 * <p> Method: showStatus(String message, boolean good) </p>
	 *
	 * <p> Description: Shows a message beneath the texts: green for good news, red for a
	 * problem.  An empty message clears it. </p>
	 *
	 * @param message	the text to show
	 * @param good		true for a confirmation, false for a problem
	 */
	protected static void showStatus(String message, boolean good) {
		ViewCuratorRefine.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewCuratorRefine.label_Status.setText(message == null ? "" : message);
	}


	/**********
	 * <p> Method: String confirmationQuestion(Iterable&lt;String&gt; fields, String title,
	 * String curator) </p>
	 *
	 * <p> Description: The words of the "Are you sure?" question: which fields change, in which
	 * lesson, and that the original is kept and the lesson will name the Curator. </p>
	 *
	 * @param fields	the fields that will change, title first
	 * @param title		the lesson's current title
	 * @param curator	the Curator's user name
	 * @return			the question
	 */
	protected static String confirmationQuestion(Iterable<String> fields, String title,
			String curator) {
		StringBuilder names = new StringBuilder();
		for (String field : fields) {
			if (names.length() > 0) names.append(" and the ");
			names.append(field);
		}
		return "Save the refined " + names + " of \"" + title + "\"?\n\n"
				+ "The contributor's original text is kept, and the lesson will show "
				+ "\"Edited by curator: " + curator + "\".";
	}


	/*-*******************************************************************************************

	Private helpers for the actions above

	 */

	/**********
	 * <p> Method: openLesson(String field) </p>
	 *
	 * <p> Description: Takes a new snapshot of the lesson through the Model and shows it, with
	 * the final wording reset to the stored text.  Used when the page opens and after a save.
	 * Any AI answer for the earlier snapshot is ignored. </p>
	 *
	 * @param field the field to show
	 */
	private static void openLesson(String field) {
		visit++;
		setBusy(false);
		ModelCuratorRefine.Review opened = theModel.openLesson(ViewCuratorRefine.theUser,
				ViewCuratorRefine.theLessonId);
		if (!opened.success()) {
			closePage(opened.message());
			return;
		}
		theDraft = opened.draft();
		finalText.clear();
		finalText.put(LessonsLearned.FIELD_TITLE, theDraft.title());
		finalText.put(LessonsLearned.FIELD_DESCRIPTION, theDraft.description());
		LessonsLearned lesson = FoundationsMain.database.getLessonById(theDraft.lessonId());
		ViewCuratorRefine.label_LessonTitle.setText("Lesson: " + theDraft.title());
		String note = CurationNote.text(lesson);
		ViewCuratorRefine.label_Info.setText((lesson == null ? "" :
				"Owner: " + lesson.getOwnerUserName() + "   |   Category: " + lesson.getCategory()
				+ "   |   ") + (note.isEmpty() ? "Not yet refined by a curator." : note));
		setAllDisabled(false);
		// shownField is cleared so performFieldChanged() does not store the old page's text
		shownField = null;
		if (LessonsLearned.FIELD_TITLE.equals(field)) ViewCuratorRefine.radio_Title.setSelected(true);
		else ViewCuratorRefine.radio_Description.setSelected(true);
		// Selecting the radio button that is already selected does not fire the listener
		performFieldChanged();
	}


	/**********
	 * <p> Method: finishSuggest(int forVisit, String field, ModelCuratorRefine.Review review) </p>
	 *
	 * <p> Description: Shows the AI's answer on the JavaFX thread.  A suggestion becomes the
	 * final wording of its field; a failure keeps the Curator's wording and says why
	 * (US-C-E3-08).  If the Curator role was lost while waiting, the page is closed down. </p>
	 *
	 * @param forVisit	the visit the request was made in; an older one is ignored
	 * @param field		the field that was asked about
	 * @param review	the Model's answer, or null if the request failed unexpectedly
	 */
	private static void finishSuggest(int forVisit, String field,
			ModelCuratorRefine.Review review) {
		if (forVisit != visit) return;
		setBusy(false);
		if (review == null) {
			showStatus(ModelCuratorRefine.UNAVAILABLE, false);
			return;
		}
		if (!review.success()) {
			// An authorization failure returns no snapshot; no lesson text may stay on screen
			if (review.draft() == null) closePage(review.message());
			else showStatus(review.message(), false);
			return;
		}
		finalText.put(field, review.suggestion());
		if (field.equals(shownField)) ViewCuratorRefine.text_Final.setText(review.suggestion());
		showStatus(REVIEW, true);
	}


	/**********
	 * <p> Method: closePage(String message) </p>
	 *
	 * <p> Description: Removes every lesson text from the page and disables every action but
	 * Return, Logout, and Quit, with the reason in place of the title and in red. </p>
	 *
	 * @param message the reason, such as "Only a curator can refine lessons."
	 */
	private static void closePage(String message) {
		visit++;
		theDraft = null;
		shownField = null;
		finalText.clear();
		ViewCuratorRefine.label_LessonTitle.setText(message);
		ViewCuratorRefine.label_Info.setText("");
		ViewCuratorRefine.label_OriginalHeading.setText("");
		ViewCuratorRefine.label_FinalHeading.setText("");
		ViewCuratorRefine.text_Original.setText("");
		ViewCuratorRefine.text_Final.setText("");
		ViewCuratorRefine.label_Count.setText("");
		setBusy(false);
		setAllDisabled(true);
		showStatus(message, false);
	}


	/**********
	 * <p> Method: setBusy(boolean waiting) </p>
	 *
	 * <p> Description: While the AI is working, the field choice, the goal, the final wording,
	 * and every action but Return, Logout, and Quit are disabled, so the answer cannot land on
	 * text the Curator is changing. </p>
	 *
	 * @param waiting true while a request is running
	 */
	private static void setBusy(boolean waiting) {
		busy = waiting;
		ViewCuratorRefine.radio_Title.setDisable(waiting);
		ViewCuratorRefine.radio_Description.setDisable(waiting);
		ViewCuratorRefine.text_Goal.setDisable(waiting);
		ViewCuratorRefine.button_Suggest.setDisable(waiting);
		ViewCuratorRefine.text_Final.setEditable(!waiting);
		updateButtons();
	}


	/**********
	 * <p> Method: setAllDisabled(boolean disabled) </p>
	 *
	 * <p> Description: Enables or disables every lesson action on the page. </p>
	 *
	 * @param disabled true to disable them
	 */
	private static void setAllDisabled(boolean disabled) {
		ViewCuratorRefine.radio_Title.setDisable(disabled);
		ViewCuratorRefine.radio_Description.setDisable(disabled);
		ViewCuratorRefine.text_Goal.setDisable(disabled);
		ViewCuratorRefine.button_Suggest.setDisable(disabled);
		ViewCuratorRefine.text_Final.setEditable(!disabled);
		ViewCuratorRefine.text_Final.setDisable(disabled);
		ViewCuratorRefine.text_Original.setDisable(disabled);
		updateButtons();
	}


	/**********
	 * <p> Method: updateButtons() </p>
	 *
	 * <p> Description: Discard is usable when the chosen field's final wording differs from its
	 * stored text; Save is usable when any field's does.  Both are disabled while the AI is
	 * working or when no lesson is open. </p>
	 */
	private static void updateButtons() {
		boolean open = theDraft != null && shownField != null && !busy;
		boolean shownChanged = open
				&& !ViewCuratorRefine.text_Final.getText().equals(original(shownField));
		boolean anyChanged = shownChanged;
		if (open) {
			for (String field : finalText.keySet())
				if (!field.equals(shownField) && !finalText.get(field).equals(original(field)))
					anyChanged = true;
		}
		ViewCuratorRefine.button_Discard.setDisable(!shownChanged);
		ViewCuratorRefine.button_Save.setDisable(!anyChanged);
	}


	/**********
	 * <p> Method: changedFields() </p>
	 *
	 * <p> Description: The fields whose final wording differs from the stored text, title
	 * first, with that wording.  Only these are sent to the Model, so a field the Curator did
	 * not change is never written (US-C-E3-05, "a successful save updates only the intended
	 * fields"). </p>
	 *
	 * @return the changed fields and their final wording
	 */
	private static Map<String, String> changedFields() {
		Map<String, String> changes = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : finalText.entrySet())
			if (!entry.getValue().equals(original(entry.getKey())))
				changes.put(entry.getKey(), entry.getValue());
		return changes;
	}


	/**********
	 * <p> Method: original(String field) </p>
	 *
	 * <p> Description: The stored text of a field when the page was opened or last saved. </p>
	 *
	 * @param field	title or description
	 * @return		the stored text, or an empty String if no lesson is open
	 */
	private static String original(String field) {
		if (theDraft == null) return "";
		return LessonsLearned.FIELD_TITLE.equals(field) ? theDraft.title() : theDraft.description();
	}


	/**********
	 * <p> Method: selectedField() </p>
	 *
	 * <p> Description: The field chosen with the radio buttons. </p>
	 *
	 * @return title or description
	 */
	private static String selectedField() {
		return ViewCuratorRefine.radio_Title.isSelected() ? LessonsLearned.FIELD_TITLE
				: LessonsLearned.FIELD_DESCRIPTION;
	}
}
