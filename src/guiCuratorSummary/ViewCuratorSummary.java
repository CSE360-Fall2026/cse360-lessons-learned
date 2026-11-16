package guiCuratorSummary;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;


/*******
 * <p> Title: ViewCuratorSummary Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Merge Statistics page (TP3 Aspects 3 and 4; Epic
 * E4: "As a lessons-learned curator, I can merge time and effort data from two or more
 * lessons-learned instances into a statistically useful form", and Epic E5: "I can make the
 * statistically useful time and effort information I add visible or not to the contributor").
 * The Curator reaches it from the Curator Home Page with the target lesson selected, chooses
 * two or more source lessons, presses Preview to see the labeled statistics, and then Confirm
 * (with a Yes/No question) or Cancel.  Show to Contributors and Hide change only whether
 * Contributors can see the saved summary.</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * the widgets do lives in ControllerCuratorSummary, and the data work, including the Curator
 * role check, lives in ModelCuratorSummary and Alex Meyers' CuratorSummaryController.  The page
 * is a singleton: the widgets are created once, and each call to displayCuratorSummary()
 * refills the parts that depend on the lesson.</p>
 *
 * <p> The layout follows User Experience.pdf and the Curator Lock page: the lesson at the top,
 * the choices on the left and the result on the right, messages shown in place, and Return,
 * Logout, and Quit at the bottom in the same places as on the Lock page.</p>
 *
 * <p> Tested by the manual tests M-CS1 to M-CS8 in Manual Tests.pdf.  The Model is tested by
 * curatorSummaryTests.CuratorSummaryPageTests (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-16 Initial version for TP3 Aspects 3 and 4
 *
 */

public class ViewCuratorSummary {

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

	// GUI Area 2: the target lesson, the sources, the statistics, and the actions

	/** The target lesson, or the reason it could not be opened.  Source: US-C-E4-01, "a target
	 * lesson". */
	protected static Label label_LessonTitle = new Label();
	/** Whether the lesson has a saved summary and whether Contributors can see it.  Source:
	 * US-C-E5-01. */
	protected static Label label_Saved = new Label();
	/** Tells the Curator what the list is for. */
	protected static Label label_Choose = new Label("Source lessons (choose two or more)");
	/** Every lesson, with its owner and number of experience records.  Source: US-C-E4-01,
	 * "selects at least two distinct source lessons"; US-C-E4-02, comparable observations. */
	protected static ListView<String> listView_Sources = new ListView<String>();
	/** How many lessons are selected, and how to select more than one. */
	protected static Label label_SelectedCount = new Label();
	/** The heading over the statistics. */
	protected static Label label_TableHeading = new Label("Merged statistics");
	/** The labeled table of a preview or the saved summary.  Source: US-C-E4-03. */
	protected static TextArea text_Table = new TextArea();
	/** Computes the summary without saving it.  Source: US-C-E4-01, "previews a summary". */
	protected static Button button_Preview = new Button("Preview");
	/** Saves the previewed summary, after a Yes/No question.  Source: US-C-E4-01, "explicitly
	 * confirms before saving". */
	protected static Button button_Confirm = new Button("Confirm...");
	/** Discards the preview.  Source: US-C-E4-01 V sentence, "should I cancel, nothing is saved". */
	protected static Button button_Cancel = new Button("Cancel");
	/** Makes the saved summary visible to Contributors.  Source: US-C-E5-01. */
	protected static Button button_Show = new Button("Show to Contributors");
	/** Hides the saved summary from Contributors.  Source: US-C-E5-01. */
	protected static Button button_Hide = new Button("Hide from Contributors");
	/** A refusal in red, or a confirmation in green.  Source: the V sentences of US-C-E4-01 and
	 * US-C-E4-02, "I receive" a specific message. */
	protected static Label label_Status = new Label();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: returning, logging out, and quitting

	/** Returns to the Curator Home Page.  An unconfirmed preview is discarded. */
	protected static Button button_Return = new Button("Return");
	/** Logs out and returns to the login page.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The ids of the lessons in listView_Sources, in the same order.  The ListView holds
	 * display text only, so a selected row is matched back to its lesson here. */
	protected static List<Integer> listedLessonIds = new ArrayList<Integer>();

	/** The singleton instance; null until the page is first displayed. */
	private static ViewCuratorSummary theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in user.  Every Model call is made with this user, so the Curator role is
	 * checked for every action (US-C-E5-02). */
	protected static User theUser;
	/** The id of the target lesson; -1 until the page is first displayed. */
	protected static int theLessonId = -1;

	/** The shared Scene each invocation populates. */
	private static Scene theCuratorSummaryScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayCuratorSummary(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Curator
	 * Merge Statistics page for one target lesson.  It sets up the shared attributes,
	 * instantiates the singleton if needed, fills in the parts that change with the user and the
	 * lesson (with a new Model, so no preview is carried over from an earlier visit), and shows
	 * the page.</p>
	 *
	 * <p> FoundationsMain.activeHomePage is left as the Curator Home Page set it, because this
	 * page is reached only from there and has no Account Update button of its own.</p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the target lesson
	 */
	public static void displayCuratorSummary(Stage ps, User user, int lessonId) {

		theStage = ps;
		theUser = user;
		theLessonId = lessonId;

		if (theView == null) theView = new ViewCuratorSummary();

		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerCuratorSummary.loadPage();

		theStage.setTitle("CSE 360 Foundations: Merge Lesson Statistics");
		theStage.setScene(theCuratorSummaryScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewCuratorSummary() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once;
	 * displayCuratorSummary() fills in the changeable fields on every later use.</p>
	 */
	private ViewCuratorSummary() {

		theRootPane = new Pane();
		theCuratorSummaryScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Merge Lesson Statistics");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the target lesson and its saved summary
		setupLabelUI(label_LessonTitle, "Arial", 20, 760, Pos.BASELINE_LEFT, 20, 103);
		label_LessonTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
		label_LessonTitle.setMaxWidth(760);
		setupLabelUI(label_Saved, "Arial", 14, 760, Pos.BASELINE_LEFT, 20, 135);

		// Left: the lessons to merge
		setupLabelUI(label_Choose, "Arial", 13, 380, Pos.BASELINE_LEFT, 20, 165);
		label_Choose.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		listView_Sources.setLayoutX(20);
		listView_Sources.setLayoutY(188);
		listView_Sources.setPrefSize(380, 230);
		listView_Sources.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		listView_Sources.getSelectionModel().getSelectedIndices().addListener(
				(javafx.collections.ListChangeListener<Integer>) (_) -> {
					ControllerCuratorSummary.performSelectionChanged(); });
		setupLabelUI(label_SelectedCount, "Arial", 12, 380, Pos.BASELINE_LEFT, 20, 422);
		label_SelectedCount.setTextFill(Color.GRAY);

		// Right: the statistics.  A fixed-width font keeps the table's columns lined up.
		setupLabelUI(label_TableHeading, "Arial", 13, 365, Pos.BASELINE_LEFT, 415, 165);
		label_TableHeading.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		text_Table.setLayoutX(415);
		text_Table.setLayoutY(188);
		text_Table.setPrefSize(365, 250);
		text_Table.setFont(Font.font("Monospaced", 12));
		text_Table.setWrapText(true);
		// Read-only: the table can be scrolled and copied from, but never typed into
		text_Table.setEditable(false);

		// The merge actions under the list, the visibility actions under the table
		setupButtonUI(button_Preview, "Dialog", 14, 110, Pos.CENTER, 20, 448);
		button_Preview.setOnAction((_) -> {ControllerCuratorSummary.performPreview(); });
		setupButtonUI(button_Confirm, "Dialog", 14, 110, Pos.CENTER, 140, 448);
		button_Confirm.setOnAction((_) -> {ControllerCuratorSummary.performConfirm(); });
		setupButtonUI(button_Cancel, "Dialog", 14, 110, Pos.CENTER, 260, 448);
		button_Cancel.setOnAction((_) -> {ControllerCuratorSummary.performCancel(); });

		setupButtonUI(button_Show, "Dialog", 14, 175, Pos.CENTER, 415, 448);
		button_Show.setOnAction((_) -> {ControllerCuratorSummary.performShow(); });
		setupButtonUI(button_Hide, "Dialog", 14, 180, Pos.CENTER, 600, 448);
		button_Hide.setOnAction((_) -> {ControllerCuratorSummary.performHide(); });

		setupLabelUI(label_Status, "Arial", 14, 760, Pos.BASELINE_LEFT, 20, 488);
		label_Status.setMaxWidth(760);
		label_Status.setWrapText(true);

		// GUI Area 3, in the same places as on the Curator Lock page
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerCuratorSummary.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerCuratorSummary.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerCuratorSummary.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1,
			label_LessonTitle, label_Saved, label_Choose, listView_Sources, label_SelectedCount,
			label_TableHeading, text_Table, button_Preview, button_Confirm, button_Cancel,
			button_Show, button_Hide, label_Status,
			line_Separator4, button_Return, button_Logout, button_Quit);
	}


	/*-********************************************************************************************

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
