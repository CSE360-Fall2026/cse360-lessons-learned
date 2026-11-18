package guiCuratorRefine;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;

/*******
 * <p> Title: ViewCuratorRefine Class. </p>
 *
 * <p> Description: The Java/FX-based Refine with AI page (TP3 Aspect 6; Epic E3: "As a
 * lessons-learned curator, I can use an AI tool to help me edit and refine lessons-learned
 * details so others can better understand the lesson").  The Curator reaches it from the
 * Curator Home Page with a lesson selected, chooses the title or the description, optionally
 * types a goal such as "shorter", and presses Get AI Suggestion.  The original stays on the
 * left; the suggestion appears on the right, where the Curator can edit it, discard it, or type
 * their own wording.  Save Refinement asks "Are you sure?" and then saves.</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * the widgets do lives in ControllerCuratorRefine, and the rules (the Curator role, the
 * lesson's revision, the consent, the validation, and keeping the original text) live in
 * Daniel Martinez Julio's ModelCuratorRefine.  The page is a singleton: the widgets are
 * created once, and each call to displayCuratorRefine() refills the parts that depend on the
 * lesson.</p>
 *
 * <p> The layout follows User Experience.pdf and the other Curator pages: the lesson at the
 * top, the original on the left and the final wording on the right (US-C-E3-04, "side by
 * side"), messages shown in place, and Return, Logout, and Quit at the bottom in the same
 * places as on the Curator Lock and Merge Statistics pages.</p>
 *
 * <p> Tested by the manual tests M-RF1 to M-RF7 in Manual Tests.pdf.  The Model is tested by
 * curatorRefinementTests.CuratorRefinementTests and RefinePageTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-18 Initial version for TP3 Aspect 6
 */
public class ViewCuratorRefine {

	/*-*******************************************************************************************

	Attributes

	 */

	/** The window width shared by every page.  Source: FoundationsMain. */
	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	/** The window height shared by every page.  Source: FoundationsMain. */
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	// GUI Area 1: the purpose of this page and whose account is being used

	/** The page title.  Source: the Foundations page pattern. */
	protected static Label label_PageTitle = new Label();
	/** Whose account is in use.  Source: the Foundations page pattern. */
	protected static Label label_UserDetails = new Label();
	/** Separator between GUI Areas 1 and 2. */
	protected static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2: the lesson, the request, the two texts, and the actions

	/** The lesson being refined, or the reason it could not be opened.  Source: US-C-E3-03,
	 * "it clearly identifies the lesson". */
	protected static Label label_LessonTitle = new Label();
	/** The owner, the category, and the "Edited by curator" note or "not yet refined".
	 * Source: US-C-E3-07. */
	protected static Label label_Info = new Label();
	/** The caption for the field choice. */
	protected static Label label_Field = new Label("Field:");
	/** Chooses the title.  Source: US-C-E3-03, "select the lesson field to refine". */
	protected static RadioButton radio_Title = new RadioButton("Title");
	/** Chooses the description.  Source: US-C-E3-03. */
	protected static RadioButton radio_Description = new RadioButton("Description");
	/** Makes the two field choices exclusive. */
	protected static ToggleGroup group_Field = new ToggleGroup();
	/** The caption for the goal. */
	protected static Label label_Goal = new Label("Goal (optional):");
	/** The Curator's goal, such as "shorter".  Source: US-C-E3-03, "optionally specify a goal
	 * such as clearer or shorter wording". */
	protected static TextField text_Goal = new TextField();
	/** Asks the AI for a suggestion for the chosen field.  Source: US-C-E3-01, "request a
	 * suggestion". */
	protected static Button button_Suggest = new Button("Get AI Suggestion");
	/** The heading over the original. */
	protected static Label label_OriginalHeading = new Label();
	/** The stored text of the chosen field.  Read-only.  Source: US-C-E3-04, "the original
	 * remains visible while the Curator reviews the suggestion". */
	protected static TextArea text_Original = new TextArea();
	/** The heading over the final wording. */
	protected static Label label_FinalHeading = new Label();
	/** The suggestion, which the Curator can edit, or their own wording.  Source: US-C-E3-04,
	 * "edit the proposed wording"; US-C-E3-08, "continue manual refinement". */
	protected static TextArea text_Final = new TextArea();
	/** How long the final wording is, against the field's limit. */
	protected static Label label_Count = new Label();
	/** Says the original contributor's text is kept.  Source: US-C-E3-06. */
	protected static Label label_KeptNote = new Label("The contributor's original text is kept.");
	/** Puts the stored text back on the right.  Source: US-C-E3-04, "discard it". */
	protected static Button button_Discard = new Button("Discard Changes");
	/** Saves the final wording after an explicit Yes.  Source: US-C-E3-04, "saving requires an
	 * explicit Curator action"; US-C-E3-05. */
	protected static Button button_Save = new Button("Save Refinement...");
	/** A refusal or validation message in red, or a confirmation in green.  Source: the V
	 * sentences of US-C-E3-05 and US-C-E3-08. */
	protected static Label label_Status = new Label();
	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: returning, logging out, and quitting

	/** Returns to the Curator Home Page.  Unsaved wording is discarded (US-C-E3-04). */
	protected static Button button_Return = new Button("Return");
	/** Logs out and returns to the login page.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewCuratorRefine theView;
	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in user.  Every Model call is made with this user, so the Curator role is
	 * checked for every action (US-C-E3-02). */
	protected static User theUser;
	/** The id of the lesson; -1 until the page is first displayed. */
	protected static int theLessonId = -1;
	/** The shared Scene each invocation populates. */
	private static Scene theCuratorRefineScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayCuratorRefine(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Refine
	 * with AI page for one lesson.  It sets up the shared attributes, instantiates the singleton
	 * if needed, fills in the parts that change with the user and the lesson (with a new Model,
	 * so nothing from an earlier visit is carried over), and shows the page.</p>
	 *
	 * <p> FoundationsMain.activeHomePage is left as the Curator Home Page set it, because this
	 * page is reached only from there and has no Account Update button of its own.</p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson to refine
	 */
	public static void displayCuratorRefine(Stage ps, User user, int lessonId) {
		theStage = ps;
		theUser = user;
		theLessonId = lessonId;
		if (theView == null) theView = new ViewCuratorRefine();
		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerCuratorRefine.loadPage();
		theStage.setTitle("CSE 360 Foundations: Refine Lesson with AI");
		theStage.setScene(theCuratorRefineScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewCuratorRefine() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once;
	 * displayCuratorRefine() fills in the changeable fields on every later use.</p>
	 */
	private ViewCuratorRefine() {
		theRootPane = new Pane();
		theCuratorRefineScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Refine Lesson with AI");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the lesson
		setupLabelUI(label_LessonTitle, "Arial", 20, 760, Pos.BASELINE_LEFT, 20, 103);
		label_LessonTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
		label_LessonTitle.setMaxWidth(760);
		setupLabelUI(label_Info, "Arial", 13, 760, Pos.BASELINE_LEFT, 20, 133);
		label_Info.setMaxWidth(760);
		label_Info.setTextFill(Color.GRAY);

		// The request: which field, an optional goal, and the button
		setupLabelUI(label_Field, "Arial", 14, 50, Pos.BASELINE_LEFT, 20, 163);
		radio_Title.setToggleGroup(group_Field);
		radio_Description.setToggleGroup(group_Field);
		setupRadioUI(radio_Title, 70, 164);
		setupRadioUI(radio_Description, 135, 164);
		// Changing the field shows that field's original and the wording kept for it
		group_Field.selectedToggleProperty().addListener(
				(_, _, _) -> {ControllerCuratorRefine.performFieldChanged(); });
		setupLabelUI(label_Goal, "Arial", 14, 110, Pos.BASELINE_LEFT, 265, 163);
		text_Goal.setFont(Font.font("Arial", 14));
		text_Goal.setLayoutX(375);
		text_Goal.setLayoutY(158);
		text_Goal.setPrefWidth(235);
		text_Goal.setPromptText("e.g. shorter, clearer");
		// Enter in the goal box asks for a suggestion, as Enter does in other search boxes
		text_Goal.setOnAction((_) -> {ControllerCuratorRefine.performSuggest(); });
		setupButtonUI(button_Suggest, "Dialog", 14, 160, Pos.CENTER, 620, 157);
		button_Suggest.setOnAction((_) -> {ControllerCuratorRefine.performSuggest(); });

		// Left: the original.  Right: the final wording.
		setupLabelUI(label_OriginalHeading, "Arial", 13, 365, Pos.BASELINE_LEFT, 20, 197);
		label_OriginalHeading.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		setupAreaUI(text_Original, 20, 218, 365, 200, false);
		setupLabelUI(label_FinalHeading, "Arial", 13, 365, Pos.BASELINE_LEFT, 415, 197);
		label_FinalHeading.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		setupAreaUI(text_Final, 415, 218, 365, 200, true);
		text_Final.setPromptText("Press Get AI Suggestion, or type your own wording here.");
		// Typing in the final wording updates the count and the Save and Discard buttons
		text_Final.textProperty().addListener(
				(_, _, _) -> {ControllerCuratorRefine.performFinalEdited(); });

		// Each label is held to its own column, so the two can never run into each other
		setupLabelUI(label_KeptNote, "Arial", 12, 365, Pos.BASELINE_LEFT, 20, 423);
		label_KeptNote.setMaxWidth(365);
		label_KeptNote.setTextFill(Color.GRAY);
		setupLabelUI(label_Count, "Arial", 12, 365, Pos.BASELINE_LEFT, 415, 423);
		label_Count.setMaxWidth(365);
		label_Count.setTextFill(Color.GRAY);

		setupButtonUI(button_Discard, "Dialog", 14, 175, Pos.CENTER, 415, 452);
		button_Discard.setOnAction((_) -> {ControllerCuratorRefine.performDiscard(); });
		setupButtonUI(button_Save, "Dialog", 14, 180, Pos.CENTER, 600, 452);
		button_Save.setOnAction((_) -> {ControllerCuratorRefine.performSave(); });

		// The status line ends at x = 405, so it never lies over the two buttons beside it and
		// takes their clicks; a long message wraps onto a second line
		setupLabelUI(label_Status, "Arial", 14, 385, Pos.TOP_LEFT, 20, 455);
		label_Status.setMaxWidth(385);
		label_Status.setWrapText(true);

		// GUI Area 3, in the same places as on the Curator Lock page
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerCuratorRefine.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerCuratorRefine.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerCuratorRefine.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1,
			label_LessonTitle, label_Info, label_Field, radio_Title, radio_Description,
			label_Goal, text_Goal, button_Suggest, label_OriginalHeading, text_Original,
			label_FinalHeading, text_Final, label_KeptNote, label_Count, button_Discard,
			button_Save, label_Status, line_Separator4, button_Return, button_Logout, button_Quit);
	}


	/*-*******************************************************************************************

	Helper methods to reduce code length

	 */

	/**********
	 * Private local method to initialize the standard fields for a label
	 *
	 * @param l		The Label object to be initialized
	 * @param ff	The font to be used
	 * @param f		The size of the font to be used
	 * @param w		The width of the Label
	 * @param p		The alignment (e.g. left, centered, or right)
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupLabelUI(Label l, String ff, double f, double w, Pos p, double x,
			double y){
		l.setFont(Font.font(ff, f));
		l.setMinWidth(w);
		l.setAlignment(p);
		l.setLayoutX(x);
		l.setLayoutY(y);
	}

	/**********
	 * Private local method to initialize the standard fields for a radio button
	 *
	 * @param r		The RadioButton object to be initialized
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupRadioUI(RadioButton r, double x, double y){
		r.setFont(Font.font("Arial", 14));
		r.setLayoutX(x);
		r.setLayoutY(y);
	}

	/**********
	 * Private local method to initialize the standard fields for a text area
	 *
	 * @param t			The TextArea object to be initialized
	 * @param x			The location from the left edge (x axis)
	 * @param y			The location from the top (y axis)
	 * @param w			The width of the TextArea
	 * @param h			The height of the TextArea
	 * @param editable	false for a read-only area
	 */
	private static void setupAreaUI(TextArea t, double x, double y, double w, double h,
			boolean editable){
		t.setLayoutX(x);
		t.setLayoutY(y);
		t.setPrefSize(w, h);
		t.setFont(Font.font("Arial", 14));
		t.setWrapText(true);
		t.setEditable(editable);
	}

	/**********
	 * Private local method to initialize the standard fields for a button
	 *
	 * @param b		The Button object to be initialized
	 * @param ff	The font to be used
	 * @param f		The size of the font to be used
	 * @param w		The width of the Button
	 * @param p		The alignment (e.g. left, centered, or right)
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupButtonUI(Button b, String ff, double f, double w, Pos p, double x,
			double y){
		b.setFont(Font.font(ff, f));
		b.setMinWidth(w);
		b.setAlignment(p);
		b.setLayoutX(x);
		b.setLayoutY(y);
	}
}
