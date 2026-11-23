
package guiCuratorSearchTerms;

import database.Database;
import database.SharedAiService;
import entityClasses.User;
import guiCuratorSearchTerms.ModelCuratorSearchTerms.Draft;
import guiCuratorSearchTerms.ModelCuratorSearchTerms.Review;
import guiTools.LessonResult;
import javafx.application.Platform;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerCuratorSearchTerms Class </p>
 *
 * <p> Description: Handles interactions on the Curator Search
 * Terms page for TP3 Aspect 5, Epic E2. Curators can open
 * a lesson, request AI suggestions, review and edit search
 * terms, and explicitly save approved terms. </p>
 *
 * <p> The Controller delegates authorization, validation,
 * and persistence to ModelCuratorSearchTerms. AI requests
 * run on a background thread to avoid blocking JavaFX. </p>
 *
 * <p> Tested by M-ST1 to M-ST6, M-WH1, and M-WH6 (Manual Tests.pdf). The Model methods these
 * actions call are tested by curatorTests.CuratorSearchTermsTests and
 * lessonTests.WritingHelpTests WH-12 (JUnit). </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 5
 * @version 1.01 2026-11-17 Emilia Davey: Generate asks for consent through the shared
 *          SharedAiService policy (US-AI-D2-01) and gets real suggestions from it,
 *          instead of a separate checkbox and no provider
 */
public class ControllerCuratorSearchTerms {

    /** The application's shared database. */
    private static final Database database =
            applicationMain.FoundationsMain.database;

    /** The shared AI service: one consent policy and one provider for every AI tool
     * (US-AI-D2-01). */
    private static final SharedAiService aiService = new SharedAiService();

    /** The Curator who pressed Generate, captured on the JavaFX thread, so the request on
     * the worker thread is made for that Curator even if the page changes meanwhile. */
    private static volatile String requestingUserName = null;

    /** The search-term Model.  Its provider asks the shared AI service for search terms
     * for the Curator who pressed Generate (US-C-E2-01). */
    private static final ModelCuratorSearchTerms model =
            new ModelCuratorSearchTerms(database,
                    (title, description, tags) -> aiService.requestLessonSearchTerms(
                            requestingUserName, title, description, tags));

    /** The lesson snapshot currently being reviewed. */
    private static Draft currentDraft;

    /** Prevents duplicate AI requests. */
    private static boolean generating = false;

    /** Identifies the current editing session. */
    private static long sessionVersion = 0;

    /**********
     * <p> Method: ControllerCuratorSearchTerms() </p>
     *
     * <p> Description: Default constructor. Controller
     * operations are static and do not require an instance. </p>
     */
    public ControllerCuratorSearchTerms() {
    }

    /**********
     * <p> Method: openLesson() </p>
     *
     * <p> Description: Loads the selected lesson and its
     * previously approved search terms for editing (US-C-E2-01).
     * The Model checks Curator authorization before returning
     * lesson information. </p>
     */
    protected static void openLesson() {

        sessionVersion++;
        generating = false;
        currentDraft = null;

        ViewCuratorSearchTerms.button_Generate.setDisable(false);
        ViewCuratorSearchTerms.button_Save.setDisable(true);
		ViewCuratorSearchTerms.button_UseSuggestions.setDisable(true);

        User actor = ViewCuratorSearchTerms.theUser;
        int lessonId = ViewCuratorSearchTerms.lessonId;

        Review result = model.openLesson(actor, lessonId);

        if (!result.success()) {
            showStatus(result.message(), false);
            ViewCuratorSearchTerms.label_LessonTitle.setText(
                    "Lesson unavailable");
            ViewCuratorSearchTerms.label_CurrentTerms.setText("");
            ViewCuratorSearchTerms.button_Generate.setDisable(true);
            return;
        }

        currentDraft = result.draft();

        ViewCuratorSearchTerms.label_LessonTitle.setText(
                "Lesson: " + currentDraft.title());

        String savedTerms = currentDraft.savedTerms();

        ViewCuratorSearchTerms.label_CurrentTerms.setText(
                "Current search terms: "
                + (savedTerms.isEmpty() ? "None" : savedTerms));

        ViewCuratorSearchTerms.text_ApprovedTerms.setText(savedTerms);
        ViewCuratorSearchTerms.button_Save.setDisable(false);

        showStatus("", true);
    }

    /**********
     * <p> Method: performGenerate() </p>
     *
     * <p> Description: Requests AI-generated search terms for
     * the selected lesson (US-C-E2-01). The request runs
     * outside the JavaFX application thread. Suggestions
     * are displayed for review but are never saved
     * automatically. </p>
     */
    protected static void performGenerate() {

        if (currentDraft == null || generating) {
            return;
        }

        // One consent policy for every AI tool: the shared notice is shown unless this
        // Curator already accepted it in this run (US-AI-D2-01).  Declining sends nothing.
        final User actor = ViewCuratorSearchTerms.theUser;
        if (actor == null
                || !aiService.requestConsentIfNeeded(actor.getUserName())) {
            showStatus(SharedAiService.CONSENT_DECLINED_MESSAGE, false);
            return;
        }
        requestingUserName = actor.getUserName();

        generating = true;

        ViewCuratorSearchTerms.button_Generate.setDisable(true);
        ViewCuratorSearchTerms.button_Save.setDisable(true);

        showStatus("Generating search-term suggestions...", true);

        final Draft requestedDraft = currentDraft;
        final long requestedSession = sessionVersion;

        Thread worker = new Thread(() -> {

            Review result = model.requestSuggestion(actor, requestedDraft,
                    SharedAiService.hasUserConsented(actor.getUserName()));

            Platform.runLater(() -> {

                // Discard responses belonging to a page or lesson
                // that the Curator has already left.
                if (requestedSession != sessionVersion) {
                    return;
                }

                generating = false;

                ViewCuratorSearchTerms.button_Generate
                        .setDisable(false);

                validateApprovedTerms();

                if (!result.success()) {
                    showStatus(result.message(), false);
                    return;
                }

                ViewCuratorSearchTerms.text_Suggestions.setText(
                        result.suggestion());

				ViewCuratorSearchTerms.button_UseSuggestions.setDisable(false);

                showStatus(
                        "Review the suggested terms before saving.",
                        true);
            });

        }, "curator-search-term-generation");

        worker.setDaemon(true);
        worker.start();
    }

	/**********
	 * <p>
	 * Method: performUseSuggestions()
	 * </p>
	 *
	 * <p>
	 * Description: Copies generated search-term suggestions into the editable
	 * approved terms field only when the Curator explicitly chooses to use them.
	 * This does not save anything to the database (TP3 Aspect 5, E2).
	 * </p>
	 */
	protected static void performUseSuggestions() {

		if (currentDraft == null || generating) {
			return;
		}

		String suggestions = ViewCuratorSearchTerms.text_Suggestions.getText();

		if (suggestions == null || suggestions.isBlank()) {
			return;
		}

		ViewCuratorSearchTerms.text_ApprovedTerms.setText(suggestions);

		validateApprovedTerms();

		showStatus("Review and edit the terms before saving.", true);
	}

	/**********
	 * <p> displayCuratorSearchTerms Method: validateApprovedTerms() </p>
	 *
	 * <p> Description: Validates the Curator's edited search terms, updates the
	 * character counter, and enables Save only when the terms have changed and the
	 * input is valid. </p>
	 */
	protected static void validateApprovedTerms() {

		String terms = ViewCuratorSearchTerms.text_ApprovedTerms.getText();

		ViewCuratorSearchTerms.label_CharacterCount.setText(terms.length() + " / 500");

		String error = inputValidation.LessonRecognizer.checkForValidSearchTerms(terms);

		ViewCuratorSearchTerms.label_ValidationError.setText(error);

		boolean changed = currentDraft != null && !terms.trim().equals(currentDraft.savedTerms());

		ViewCuratorSearchTerms.button_Save
				.setDisable(currentDraft == null || generating || !error.isEmpty() || !changed);
	}

    /**********
     * <p> Method: performSave() </p>
     *
     * <p> Description: Saves the final search terms explicitly
     * approved by the Curator (US-C-E2-02). The editable field
     * may contain accepted suggestions, modified suggestions,
     * or manually entered terms. </p>
     */
    protected static void performSave() {

        if (currentDraft == null || generating) {
            return;
        }

        String approvedTerms =
                ViewCuratorSearchTerms.text_ApprovedTerms.getText();

        LessonResult result = model.saveApprovedTerms(
                ViewCuratorSearchTerms.theUser,
                currentDraft,
                approvedTerms,
                true);

        if (!result.isSuccess()) {
            showStatus(result.getMessage(), false);
            return;
        }

        // Reload the stored state so a subsequent save uses
        // the latest approved search terms.
        Review refreshed = model.openLesson(
                ViewCuratorSearchTerms.theUser,
                ViewCuratorSearchTerms.lessonId);

        if (!refreshed.success()) {
            currentDraft = null;
            ViewCuratorSearchTerms.button_Save.setDisable(true);
            showStatus(refreshed.message(), false);
            return;
        }

        currentDraft = refreshed.draft();

        ViewCuratorSearchTerms.label_CurrentTerms.setText(
                "Current search terms: "
                + (currentDraft.savedTerms().isEmpty()
                        ? "None"
                        : currentDraft.savedTerms()));

		validateApprovedTerms();

        showStatus("Search terms saved successfully.", true);

    }

    /**********
     * <p> Method: performReturn() </p>
     *
     * <p> Description: Returns to the Curator Home Page
     * without automatically saving unsaved search-term edits.
     * </p>
     */
    protected static void performReturn() {

        sessionVersion++;
        generating = false;
        currentDraft = null;

        guiCuratorHome.ViewCuratorHome.displayCuratorHome(
                ViewCuratorSearchTerms.theStage,
                ViewCuratorSearchTerms.theUser);
    }

    /**********
     * <p> Method: showStatus(String message, boolean success) </p>
     *
     * <p> Description: Displays a confirmation or error
     * message on the search-term management page. </p>
     *
     * @param message the message to display
     * @param success true for success, false for an error
     */
    private static void showStatus(
            String message, boolean success) {

        ViewCuratorSearchTerms.label_Status.setTextFill(
                success ? Color.web("#2e7d32") : Color.RED);

        ViewCuratorSearchTerms.label_Status.setText(
                message == null ? "" : message);
    }
}
