
package guiCuratorSearchTerms;

import entityClasses.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewCuratorSearchTerms Class </p>
 *
 * <p> Description: Provides the Curator Search Terms page
 * for TP3 Aspect 5, Epic E2. Curators can request AI
 * suggestions, review existing search terms, edit the
 * proposed terms, and explicitly save approved terms. </p>
 *
 * <p> AI-generated text is never saved automatically.
 * The Controller delegates authorization, validation,
 * and persistence to ModelCuratorSearchTerms. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 5
 * @version 1.01 2026-11-17 Emilia Davey: the consent checkbox is replaced by a note; consent
 *          now uses the shared AI notice (US-AI-D2-01)
 */
public class ViewCuratorSearchTerms {

    /** The current application stage. */
    protected static Stage theStage;

    /** The currently logged-in Curator. */
    protected static User theUser;

    /** The selected lesson identifier. */
    protected static int lessonId;

    /** The search-term management scene. */
    private static Scene theScene;

    /** The singleton view instance. */
    private static ViewCuratorSearchTerms theView;

	/** Displays the currently logged-in Curator. */
	protected static Label label_UserDetails = new Label();

    /** Displays the selected lesson title. */
    protected static Label label_LessonTitle =
            new Label("Selected lesson");

    /** Displays the existing approved terms. */
    protected static Label label_CurrentTerms =
            new Label("Current search terms:");

    /** Displays AI-generated suggestions. */
    protected static TextArea text_Suggestions =
            new TextArea();

    /** Allows the Curator to edit the final terms. */
    protected static TextArea text_ApprovedTerms =
            new TextArea();

	/** Displays the approved search terms character count. */
	protected static Label label_CharacterCount = new Label("0 / 500");

	/** Displays search-term validation errors. */
	protected static Label label_ValidationError = new Label();

    /** Says that Generate sends the lesson text to the AI provider, after the shared AI
     * notice (US-AI-D2-01), so the Curator knows before pressing it. */
    protected static Label label_AiNote = new Label("Generate sends this lesson's text to "
            + "an external AI provider, after you accept the AI notice.");

    /** Requests suggested search terms. */
    protected static Button button_Generate =
            new Button("Generate Suggestions");
	/** Copies AI suggestions into the editable search terms field. */
	protected static Button button_UseSuggestions = new Button("Use Suggestions");

    /** Saves the Curator's approved terms. */
    protected static Button button_Save =
            new Button("Save Approved Terms");

    /** Returns to Curator Home. */
    protected static Button button_Return =
            new Button("Return");

    /** Displays confirmation and error messages. */
    protected static Label label_Status = new Label();

    /**********
     * <p> Method: displayCuratorSearchTerms(
     * Stage ps, User user, int selectedLessonId) </p>
     *
     * <p> Description: Displays the search-term editor
     * for the selected lesson. The Controller loads
     * the existing data through the Model. </p>
     *
     * @param ps the application Stage
     * @param user the logged-in Curator
     * @param selectedLessonId the selected lesson
     */
    public static void displayCuratorSearchTerms(
            Stage ps, User user, int selectedLessonId) {

        theStage = ps;
        theUser = user;
        lessonId = selectedLessonId;

		label_UserDetails.setText("User: " + user.getUserName());

        if (theView == null) {
            theView = new ViewCuratorSearchTerms();
        }

        // Reset the page before opening another lesson.
        text_Suggestions.clear();
        text_ApprovedTerms.clear();
		button_UseSuggestions.setDisable(true);
        label_Status.setText("");
        button_Generate.setDisable(false);
        button_Save.setDisable(true);

        ControllerCuratorSearchTerms.openLesson();
		ControllerCuratorSearchTerms.validateApprovedTerms();

        theStage.setTitle(
                "CSE 360 Foundations: Manage Search Terms");

        theStage.setScene(theScene);
        theStage.show();
    }

    /**********
     * <p> Method: ViewCuratorSearchTerms() </p>
     *
     * <p> Description: Initializes the search-term editor,
     * including the review fields, AI consent checkbox,
     * buttons, and event handlers. </p>
     */
    private ViewCuratorSearchTerms() {

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("Manage Search Terms");

		title.setFont(Font.font("Arial", 28));
		title.setMaxWidth(Double.MAX_VALUE);
		title.setAlignment(Pos.CENTER);

		label_UserDetails.setFont(Font.font("Arial", 20));

		Separator headerSeparator = new Separator();

        label_LessonTitle.setFont(
                Font.font("Arial", FontWeight.BOLD, 16));

        Label suggestionsHeading =
                new Label("AI-suggested search terms");

        suggestionsHeading.setFont(
                Font.font("Arial", FontWeight.BOLD, 14));

        text_Suggestions.setPromptText(
                "AI-generated suggestions will appear here.");

        text_Suggestions.setEditable(false);
        text_Suggestions.setWrapText(true);
        text_Suggestions.setPrefRowCount(3);

        Label approvedHeading =
                new Label("Approved search terms");

        approvedHeading.setFont(
                Font.font("Arial", FontWeight.BOLD, 14));

        Label instructions = new Label(
                "Review and edit the terms below. "
                + "Separate each term with a comma.");

        text_ApprovedTerms.setPromptText(
                "Enter or edit search terms here.");

        text_ApprovedTerms.setWrapText(true);
        text_ApprovedTerms.setPrefRowCount(3);

		text_ApprovedTerms.textProperty()
				.addListener((_, _, _) -> ControllerCuratorSearchTerms.validateApprovedTerms());

		label_CharacterCount.setTextFill(javafx.scene.paint.Color.GRAY);

		label_ValidationError.setTextFill(javafx.scene.paint.Color.RED);

		label_AiNote.setTextFill(javafx.scene.paint.Color.GRAY);
		label_AiNote.setWrapText(true);

        // The Model enforces the 500-character total limit.
		button_Generate.setOnAction(_ -> ControllerCuratorSearchTerms.performGenerate());

		button_Save.setOnAction(_ -> ControllerCuratorSearchTerms.performSave());

		button_Return.setOnAction(_ -> ControllerCuratorSearchTerms.performReturn());

		button_UseSuggestions.setDisable(true);

		button_UseSuggestions.setOnAction(event -> ControllerCuratorSearchTerms.performUseSuggestions());

        label_Status.setWrapText(true);

        javafx.scene.layout.HBox actions =
                new javafx.scene.layout.HBox(
                        12, button_Save, button_Return);

        root.getChildren().addAll(
                title,
				label_UserDetails, headerSeparator,
                label_LessonTitle,
                label_CurrentTerms,
                label_AiNote,
                button_Generate,
                suggestionsHeading,
                text_Suggestions,
				button_UseSuggestions,
                approvedHeading,
                instructions,
                text_ApprovedTerms,
				label_CharacterCount, label_ValidationError,
                actions,
                label_Status
        );

        theScene = new Scene(
                root,
                applicationMain.FoundationsMain.WINDOW_WIDTH,
                applicationMain.FoundationsMain.WINDOW_HEIGHT);
    }
}
