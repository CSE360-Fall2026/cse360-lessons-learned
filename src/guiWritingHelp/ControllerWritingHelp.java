package guiWritingHelp;

import java.util.ArrayList;
import java.util.List;

import applicationMain.FoundationsMain;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerWritingHelp Class. </p>
 *
 * <p> Description: The actions of the Writing Help window (TP3 Aspect 7, Epic E1).  The
 * Controller reacts to the widgets of ViewWritingHelp and shows what ModelWritingHelp returns;
 * it does not word messages, call the AI provider, or change the draft itself.</p>
 *
 * <p> Before anything is sent to the AI, the shared notice is shown unless this user already
 * accepted it (SharedAiService, US-AI-D2-01); declining sends nothing.  AI requests run on a
 * background thread, as on the Create and Edit Lesson pages, with the AI buttons disabled
 * until the reply arrives, so two requests never overlap.  A reply that arrives after the
 * window was closed and opened again is discarded.</p>
 *
 * <p> The class has been written assuming that the View is the only class that invokes these
 * methods.  This is why each has been declared "protected".</p>
 *
 * <p> Tested by M-WH1 to M-WH5 (Manual Tests.pdf). The Model methods these actions call are
 * tested by lessonTests.WritingHelpTests WH-01 to WH-11 (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-17 Initial version for TP3 Aspect 7 (US-P-E1-01, -02, -03)
 */
public class ControllerWritingHelp {

	/** The shared AI service: one consent policy and one provider for every AI tool. */
	private static final SharedAiService aiService = new SharedAiService();

	/** The Model of the current opening of the window. */
	private static ModelWritingHelp theModel;

	/** Counts the openings of the window, so a late reply for an earlier one is discarded. */
	private static long session = 0;

	/**
	 * Default constructor is not used.
	 */
	public ControllerWritingHelp() {
	}


	/**********
	 * <p> Method: reset() </p>
	 *
	 * <p> Description: Starts a new opening of the window: a new Model (so a new conversation),
	 * every widget cleared, and the Brainstorm tab shown.  The View calls this each time the
	 * window opens. </p>
	 */
	protected static void reset() {
		session++;
		theModel = new ModelWritingHelp(FoundationsMain.database, aiService,
				ViewWritingHelp.theUser.getUserName());
		ViewWritingHelp.text_Transcript.setText(theModel.transcript());
		ViewWritingHelp.text_Answer.clear();
		ViewWritingHelp.listView_Takeaways.setItems(FXCollections.observableArrayList());
		ViewWritingHelp.text_Search.clear();
		ViewWritingHelp.listedExampleIds.clear();
		ViewWritingHelp.listView_Examples.setItems(FXCollections.observableArrayList());
		ViewWritingHelp.text_Example.clear();
		ViewWritingHelp.tabPane.getSelectionModel().select(0);
		setAiBusy(false);
		showStatus("", true);
	}


	/**********
	 * <p> Method: performSend() </p>
	 *
	 * <p> Description: Sends the typed answer and shows the AI's next question (US-P-E1-01).
	 * If the notice is declined, or the request is refused or fails, the answer stays in the
	 * box and the reason is shown (US-AI-D2-02). </p>
	 */
	protected static void performSend() {
		final String answer = ViewWritingHelp.text_Answer.getText();
		if (answer == null || answer.trim().isEmpty()) {
			showStatus(ModelWritingHelp.EMPTY_ANSWER, false);
			return;
		}
		if (!consented()) return;
		final long mySession = session;
		final ModelWritingHelp model = theModel;
		setAiBusy(true);
		showStatus("Waiting for the AI...", true);
		runInBackground(() -> {
			ModelWritingHelp.Result result = model.send(answer);
			Platform.runLater(() -> {
				if (mySession != session) return;
				setAiBusy(false);
				if (result.isSuccess()) {
					ViewWritingHelp.text_Transcript.setText(model.transcript());
					ViewWritingHelp.text_Transcript.setScrollTop(Double.MAX_VALUE);
					ViewWritingHelp.text_Answer.clear();
					showStatus("", true);
				} else {
					showStatus(result.getMessage(), false);
				}
			});
		});
	}


	/**********
	 * <p> Method: performTakeaways() </p>
	 *
	 * <p> Description: Shows the lessons learned in the conversation, for the Contributor to
	 * choose from (US-P-E1-01). </p>
	 */
	protected static void performTakeaways() {
		if (theModel.answerCount() == 0) {
			showStatus(ModelWritingHelp.NO_CONVERSATION, false);
			return;
		}
		if (!consented()) return;
		final long mySession = session;
		final ModelWritingHelp model = theModel;
		setAiBusy(true);
		showStatus("Waiting for the AI...", true);
		runInBackground(() -> {
			ModelWritingHelp.Result result = model.takeaways();
			Platform.runLater(() -> {
				if (mySession != session) return;
				setAiBusy(false);
				if (result.isSuccess())
					ViewWritingHelp.listView_Takeaways.setItems(
							FXCollections.observableArrayList(result.getItems()));
				showStatus(result.getMessage(), result.isSuccess());
			});
		});
	}


	/**********
	 * <p> Method: performAddDescription() </p>
	 *
	 * <p> Description: Adds the chosen takeaways to the end of the Description on the lesson
	 * page, unless the Description is locked (US-P-E1-02). </p>
	 */
	protected static void performAddDescription() {
		addChosen(LessonsLearned.FIELD_DESCRIPTION);
	}


	/**********
	 * <p> Method: performAddRecommendation() </p>
	 *
	 * <p> Description: Adds the chosen takeaways to the end of the Recommendation on the lesson
	 * page (US-P-E1-02). </p>
	 */
	protected static void performAddRecommendation() {
		addChosen(LessonsLearned.FIELD_RECOMMENDATION);
	}


	/**********
	 * <p> Method: performSearch() </p>
	 *
	 * <p> Description: Lists the Contributor's own lessons that match the words (US-P-E1-03).
	 * Empty or invalid words, and no matches, are explained; the draft is never touched. </p>
	 */
	protected static void performSearch() {
		String words = ViewWritingHelp.text_Search.getText();
		ViewWritingHelp.text_Example.clear();
		ViewWritingHelp.listedExampleIds.clear();
		String error = ModelWritingHelp.searchError(words);
		if (!error.isEmpty()) {
			ViewWritingHelp.listView_Examples.setItems(FXCollections.observableArrayList());
			showStatus(error, false);
			return;
		}
		List<String> rows = new ArrayList<String>();
		for (LessonsLearned lesson : theModel.searchExamples(words)) {
			ViewWritingHelp.listedExampleIds.add(lesson.getId());
			rows.add(lesson.getTitle() + "  [" + lesson.getCategory() + "]");
		}
		ViewWritingHelp.listView_Examples.setItems(FXCollections.observableArrayList(rows));
		if (rows.isEmpty()) showStatus(ModelWritingHelp.NO_EXAMPLES, false);
		else showStatus(rows.size() + (rows.size() == 1 ? " lesson matches." :
				" lessons match.") + "  Select one to read it.", true);
	}


	/**********
	 * <p> Method: performExampleSelected() </p>
	 *
	 * <p> Description: Shows the selected example.  The Model checks again that the lesson is
	 * the Contributor's own (US-P-E1-03). </p>
	 */
	protected static void performExampleSelected() {
		int row = ViewWritingHelp.listView_Examples.getSelectionModel().getSelectedIndex();
		if (row < 0 || row >= ViewWritingHelp.listedExampleIds.size()) {
			ViewWritingHelp.text_Example.clear();
			return;
		}
		ViewWritingHelp.text_Example.setText(
				theModel.exampleText(ViewWritingHelp.listedExampleIds.get(row)));
	}


	/**********
	 * <p> Method: performClose() </p>
	 *
	 * <p> Description: Closes the window.  The lesson page keeps its draft, including anything
	 * added, and nothing is saved until the Contributor saves the lesson there (US-P-E1-01). </p>
	 */
	protected static void performClose() {
		ViewWritingHelp.theStage.close();
	}


	/*-*******************************************************************************************

	Private helpers

	 */

	/**********
	 * Asks for consent through the shared notice unless this user already accepted it, and
	 * shows the declined message otherwise.
	 *
	 * @return true if the user has accepted the notice
	 */
	private static boolean consented() {
		if (aiService.requestConsentIfNeeded(ViewWritingHelp.theUser.getUserName())) return true;
		showStatus(SharedAiService.CONSENT_DECLINED_MESSAGE, false);
		return false;
	}

	/**********
	 * Adds the chosen takeaways to one field of the draft and shows the result.
	 *
	 * @param field	LessonsLearned.FIELD_DESCRIPTION or LessonsLearned.FIELD_RECOMMENDATION
	 */
	private static void addChosen(String field) {
		List<String> chosen = new ArrayList<String>(
				ViewWritingHelp.listView_Takeaways.getSelectionModel().getSelectedItems());
		ModelWritingHelp.Result result =
				ModelWritingHelp.addToDraft(ViewWritingHelp.theTarget, field, chosen);
		showStatus(result.getMessage(), result.isSuccess());
	}

	/**********
	 * Disables the AI buttons while a request is running, and enables them again after.
	 *
	 * @param busy	true while a request is running
	 */
	private static void setAiBusy(boolean busy) {
		ViewWritingHelp.button_Send.setDisable(busy);
		ViewWritingHelp.button_Takeaways.setDisable(busy);
	}

	/**********
	 * Runs work on a daemon thread, so the window stays responsive while the AI answers.
	 *
	 * @param work	the work to run
	 */
	private static void runInBackground(Runnable work) {
		Thread worker = new Thread(work, "writing-help-ai");
		worker.setDaemon(true);
		worker.start();
	}

	/**********
	 * Shows a message: green for good news, red for a problem.
	 *
	 * @param message	the text
	 * @param good		true for good news
	 */
	private static void showStatus(String message, boolean good) {
		ViewWritingHelp.label_Status.setTextFill(good ? Color.web("#2e7d32") : Color.RED);
		ViewWritingHelp.label_Status.setText(message == null ? "" : message);
	}
}
