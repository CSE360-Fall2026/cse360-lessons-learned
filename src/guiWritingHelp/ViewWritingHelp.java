package guiWritingHelp;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import entityClasses.User;

/*******
 * <p> Title: ViewWritingHelp Class. </p>
 *
 * <p> Description: The Java/FX-based Writing Help window (TP3 Aspect 7, Epic E1: "As a
 * contributor producing and enhancing a lesson learned, I can use AI-based interactive writing,
 * brainstorming, and searching tools to help me find good examples, include critical
 * information, and express my thoughts well").  It opens on top of the Create Lesson or Edit
 * Lesson page, which keeps the draft, and has two tabs:</p>
 * <ul>
 * <li> <b>Brainstorm</b>: the AI asks one follow-up question at a time; the Contributor
 * answers, can ask for the takeaways, and adds the ones they choose to the Description or
 * Recommendation (US-P-E1-01, US-P-E1-02).</li>
 * <li> <b>Find Examples</b>: a keyword search of the Contributor's own lessons, to read one as
 * an example (US-P-E1-03).</li>
 * </ul>
 *
 * <p> The window is modal for its lesson page, so the draft cannot change while it is open.
 * As in every Foundations View, this class only declares and positions the widgets;
 * ControllerWritingHelp reacts to them and ModelWritingHelp does the work.  The widgets are
 * created once and reset on every opening, so no conversation carries over to another opening
 * or another user.</p>
 *
 * <p> Tested by the manual tests M-WH1 to M-WH5 in Manual Tests.pdf.  The Model is tested by
 * lessonTests.WritingHelpTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-17 Initial version for TP3 Aspect 7 (US-P-E1-01, -02, -03)
 */
public class ViewWritingHelp {

	/** The window width. */
	private static final double WIDTH = 720;
	/** The window height. */
	private static final double HEIGHT = 560;

	// The window title and the two tabs

	/** The window's heading. */
	protected static Label label_Title = new Label("Writing Help");
	/** Holds the Brainstorm and Find Examples tabs. */
	protected static TabPane tabPane = new TabPane();

	// The Brainstorm tab (US-P-E1-01, US-P-E1-02)

	/** Explains how brainstorming works and that nothing changes until text is added. */
	protected static Label label_BrainstormHelp = new Label("The AI asks one question at a "
			+ "time.  Answer in your own words.  Nothing in your lesson changes until you add a "
			+ "takeaway below.");
	/** The conversation so far.  Read-only. */
	protected static TextArea text_Transcript = new TextArea();
	/** Where the Contributor types an answer. */
	protected static TextArea text_Answer = new TextArea();
	/** Sends the answer.  Source: US-P-E1-01, "the user can answer follow-up questions". */
	protected static Button button_Send = new Button("Send");
	/** Asks for the lessons learned in the conversation. */
	protected static Button button_Takeaways = new Button("Get Takeaways");
	/** The heading over the takeaways. */
	protected static Label label_Takeaways = new Label("Takeaways (Ctrl-click to choose more "
			+ "than one):");
	/** The takeaways to choose from.  Source: US-P-E1-01, "choose the final content". */
	protected static ListView<String> listView_Takeaways = new ListView<String>();
	/** Adds the chosen takeaways to the Description.  Source: US-P-E1-02. */
	protected static Button button_AddDescription = new Button("Add to Description");
	/** Adds the chosen takeaways to the Recommendation.  Source: US-P-E1-02. */
	protected static Button button_AddRecommendation = new Button("Add to Recommendation");

	// The Find Examples tab (US-P-E1-03)

	/** The words to search for. */
	protected static TextField text_Search = new TextField();
	/** Searches the Contributor's own lessons. */
	protected static Button button_Search = new Button("Search");
	/** The matching lessons. */
	protected static ListView<String> listView_Examples = new ListView<String>();
	/** The selected example.  Read-only. */
	protected static TextArea text_Example = new TextArea();
	/** Says that only the Contributor's own lessons are searched. */
	protected static Label label_ExampleHelp = new Label("Only your own lessons are searched.");

	// Bottom of the window

	/** A problem in red, or good news in green. */
	protected static Label label_Status = new Label();
	/** Closes the window and returns to the lesson page, whose draft is unchanged unless text
	 * was added. */
	protected static Button button_Close = new Button("Close");

	/** The ids of the lessons in listView_Examples, in the same order. */
	protected static java.util.List<Integer> listedExampleIds = new java.util.ArrayList<Integer>();

	/** The window; created the first time it is opened. */
	protected static Stage theStage;
	/** The logged-in Contributor. */
	protected static User theUser;
	/** The lesson page that opened the window. */
	protected static DraftTarget theTarget;

	/** The singleton; null until the window is first opened. */
	private static ViewWritingHelp theView;


	/**********
	 * <p> Method: displayWritingHelp(Stage owner, User user, DraftTarget target) </p>
	 *
	 * <p> Description: Opens the Writing Help window over a lesson page, with a new
	 * conversation, and waits until it is closed.  The first time, the window is created and
	 * made modal for the owner stage, so the lesson page cannot be edited while it is open.</p>
	 *
	 * @param owner		the application's main stage, which shows the lesson page
	 * @param user		the logged-in Contributor
	 * @param target	the lesson page, which receives any added text
	 */
	public static void displayWritingHelp(Stage owner, User user, DraftTarget target) {
		theUser = user;
		theTarget = target;
		if (theView == null) {
			theStage = new Stage();
			theStage.initOwner(owner);
			theStage.initModality(Modality.WINDOW_MODAL);
			theView = new ViewWritingHelp();
		}
		ControllerWritingHelp.reset();
		theStage.setTitle("CSE 360 Foundations: Writing Help");
		theStage.showAndWait();
	}


	/**********
	 * <p> Method: ViewWritingHelp() </p>
	 *
	 * <p> Description: Creates and places every widget of the window.  Runs once. </p>
	 */
	private ViewWritingHelp() {
		Pane root = new Pane();
		theStage.setScene(new Scene(root, WIDTH, HEIGHT));
		theStage.setResizable(false);

		label_Title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
		label_Title.setLayoutX(20);
		label_Title.setLayoutY(12);

		// The Brainstorm tab
		Pane brainstorm = new Pane();
		place(label_BrainstormHelp, 10, 8, 680);
		label_BrainstormHelp.setWrapText(true);
		label_BrainstormHelp.setFont(Font.font("Arial", 13));
		area(text_Transcript, 10, 48, 680, 170, false);
		area(text_Answer, 10, 228, 550, 62, true);
		text_Answer.setPromptText("Type your answer here.");
		button(button_Send, 570, 228, 120);
		button_Send.setOnAction((_) -> ControllerWritingHelp.performSend());
		button(button_Takeaways, 570, 262, 120);
		button_Takeaways.setOnAction((_) -> ControllerWritingHelp.performTakeaways());
		place(label_Takeaways, 10, 298, 680);
		label_Takeaways.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		listView_Takeaways.setLayoutX(10);
		listView_Takeaways.setLayoutY(318);
		listView_Takeaways.setPrefSize(680, 60);
		listView_Takeaways.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		button(button_AddDescription, 10, 386, 180);
		button_AddDescription.setOnAction((_) -> ControllerWritingHelp.performAddDescription());
		button(button_AddRecommendation, 200, 386, 200);
		button_AddRecommendation.setOnAction((_) ->
				ControllerWritingHelp.performAddRecommendation());
		brainstorm.getChildren().addAll(label_BrainstormHelp, text_Transcript, text_Answer,
				button_Send, button_Takeaways, label_Takeaways, listView_Takeaways,
				button_AddDescription, button_AddRecommendation);

		// The Find Examples tab
		Pane examples = new Pane();
		text_Search.setLayoutX(10);
		text_Search.setLayoutY(10);
		text_Search.setPrefWidth(570);
		text_Search.setPromptText("Words from one of your earlier lessons");
		text_Search.setOnAction((_) -> ControllerWritingHelp.performSearch());
		button(button_Search, 590, 10, 100);
		button_Search.setOnAction((_) -> ControllerWritingHelp.performSearch());
		place(label_ExampleHelp, 10, 44, 680);
		label_ExampleHelp.setTextFill(Color.GRAY);
		listView_Examples.setLayoutX(10);
		listView_Examples.setLayoutY(66);
		listView_Examples.setPrefSize(260, 350);
		listView_Examples.getSelectionModel().selectedIndexProperty().addListener(
				(_, _, _) -> ControllerWritingHelp.performExampleSelected());
		area(text_Example, 280, 66, 410, 350, false);
		examples.getChildren().addAll(text_Search, button_Search, label_ExampleHelp,
				listView_Examples, text_Example);

		Tab brainstormTab = new Tab("Brainstorm", brainstorm);
		Tab examplesTab = new Tab("Find Examples", examples);
		brainstormTab.setClosable(false);
		examplesTab.setClosable(false);
		tabPane.getTabs().addAll(brainstormTab, examplesTab);
		tabPane.setLayoutX(10);
		tabPane.setLayoutY(50);
		tabPane.setPrefSize(700, 460);

		label_Status.setLayoutX(20);
		label_Status.setLayoutY(518);
		label_Status.setPrefWidth(560);
		label_Status.setWrapText(true);
		label_Status.setFont(Font.font("Arial", 13));
		button(button_Close, 600, 515, 100);
		button_Close.setOnAction((_) -> ControllerWritingHelp.performClose());

		root.getChildren().addAll(label_Title, tabPane, label_Status, button_Close);
	}


	/**********
	 * Places a label.
	 *
	 * @param l	the label
	 * @param x	the distance from the left edge
	 * @param y	the distance from the top
	 * @param w	the width
	 */
	private static void place(Label l, double x, double y, double w) {
		l.setLayoutX(x);
		l.setLayoutY(y);
		l.setPrefWidth(w);
	}

	/**********
	 * Places a text area.
	 *
	 * @param a			the text area
	 * @param x			the distance from the left edge
	 * @param y			the distance from the top
	 * @param w			the width
	 * @param h			the height
	 * @param editable	false for a read-only area
	 */
	private static void area(TextArea a, double x, double y, double w, double h,
			boolean editable) {
		a.setLayoutX(x);
		a.setLayoutY(y);
		a.setPrefSize(w, h);
		a.setWrapText(true);
		a.setEditable(editable);
	}

	/**********
	 * Places a button.
	 *
	 * @param b	the button
	 * @param x	the distance from the left edge
	 * @param y	the distance from the top
	 * @param w	the width
	 */
	private static void button(Button b, double x, double y, double w) {
		b.setLayoutX(x);
		b.setLayoutY(y);
		b.setMinWidth(w);
		b.setAlignment(Pos.CENTER);
		b.setFont(Font.font("Dialog", 14));
	}
}
