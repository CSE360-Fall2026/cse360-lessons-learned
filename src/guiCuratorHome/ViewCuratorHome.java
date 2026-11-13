package guiCuratorHome;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import database.Database;
import entityClasses.User;


/*******
 * <p> Title: ViewCuratorHome Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Home Page (TP3 Aspect 1; Epics.pdf dependency D1).
 * This is the page a user playing the Curator role sees after logging in.  It lists every
 * lesson learned from every Contributor (US-CH1), lets the Curator search them by keyword
 * (US-CH2, US-CH2-V), and shows the selected lesson in full beside the list (US-CH3).  The
 * Curator actions of Epics E2 to E6 (search terms, AI refining, merged statistics, statistics
 * visibility, and locking) each work on the lesson selected here.  The first of them, the
 * Lock / Unlock button (Epic E6), opens the Curator Lock page for the selected lesson.</p>
 *
 * <p> This page replaces the Foundations Role2 home page stub.  The Lessons Learned System V1
 * stories say new roles will replace Role1 and Role2; this is the Curator role.  The user's
 * role is still stored in the Foundations newRole2 database column (Initial ConOps assumption
 * A1; guiTools.RoleLabels), so existing accounts and invitations keep working, and the page
 * keeps the Role2 home page number 3.</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * the widgets do lives in ControllerCuratorHome, and the data work, including the Curator role
 * check, lives in ModelCuratorHome.  The page is a singleton: the widgets are created once, and
 * each call to displayCuratorHome() refills the parts that depend on the user and the data.</p>
 *
 * <p> The layout follows User Experience.pdf and the Contributor Home Page, so a user who has
 * both roles finds the same things in the same places: the search row above the list, messages
 * shown in place rather than in pop-ups, and Logout and Quit at the bottom.</p>
 *
 * <p> Tested by the manual tests M-CH1 to M-CH6 in Manual Tests.pdf; the Lock / Unlock button
 * by M-CL1. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Lynn Robert Carter (Curator home page by Emilia Davey)
 *
 * @version 1.00		2025-04-20 Initial version (as the Role2 home page stub)
 * @version 2.00		2026-11-10 TP3 Aspect 1: the Curator Home Page, replacing the Role2 stub
 * @version 2.01		2026-11-13 TP3 Aspect 2: the Lock / Unlock button for the selected lesson
 *
 */

public class ViewCuratorHome {

	/*-*******************************************************************************************

	Attributes

	 */

	// These are the application values required by the user interface

	/** The window width shared by every page.  Source: FoundationsMain. */
	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	/** The window height shared by every page.  Source: FoundationsMain. */
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;


	// These are the widget attributes for the GUI. There are 3 areas for this GUI.

	// GUI Area 1: It informs the user about the purpose of this page, whose account is being used,
	// and a button to allow this user to update the account settings

	/** The page title.  Source: the Foundations page pattern. */
	protected static Label label_PageTitle = new Label();
	/** Whose account is in use.  Source: the Foundations page pattern. */
	protected static Label label_UserDetails = new Label();
	/** Opens the Foundations account update page.  Source: the Foundations page pattern. */
	protected static Button button_UpdateThisUser = new Button("Account Update");

	/** Separator between GUI Areas 1 and 2. */
	protected static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2: the list of every lesson, the search box, and the selected lesson's details

	/** Heading for the list.  Rationale: says plainly that this is everyone's lessons, not
	 * only the user's own (US-CH1). */
	protected static Label label_ListHeading = new Label("All Lessons Learned");
	/** The search text.  Source: US-CH2, "I can search all lessons by keyword." */
	protected static TextField text_Search = new TextField();
	/** Runs the search (US-CH2). */
	protected static Button button_Search = new Button("Search");
	/** Clears the search and shows every lesson again.  Rationale: an empty search shows
	 * nothing (design decision D6), so this button is how the full list comes back. */
	protected static Button button_ClearSearch = new Button("Clear");
	/** Shows a search-text error beneath the search box (US-CH2-V; InputValidation.pdf). */
	protected static Label label_SearchError = new Label();
	/** Every Contributor's lessons, one row each, newest first.  Source: US-CH1. */
	protected static ListView<String> listView_Lessons = new ListView<String>();
	/** Heading for the details box. */
	protected static Label label_DetailsHeading = new Label("Selected lesson");
	/** The selected lesson in full.  Source: US-CH3.  Rationale: read-only, because this page
	 * only shows lessons; each Curator action that changes one has its own page. */
	protected static TextArea text_Details = new TextArea();
	/** Opens the Curator Lock page for the selected lesson.  Source: Epic E6 and frame 1 of
	 * the Initial ConOps storyboard, "Lock / Unlock...".  Rationale: disabled until a lesson is
	 * selected, so it can never be pressed with nothing to act on. */
	protected static Button button_Lock = new Button("Lock / Unlock...");
	/** Tells the user how to read a lesson.  Rationale: validation-as-guidance, User
	 * Experience.pdf; the user is told what to do before having to guess. */
	protected static Label label_ListHint =
			new Label("Select a lesson to read its details and to lock or unlock it.");
	/** How many lessons the list is showing. */
	protected static Label label_ListStatus = new Label();
	/** A confirmation carried over from another Curator page, or a problem such as "Lesson not
	 * found." */
	protected static Label label_Status = new Label();

	/** The ids of the lessons in the list, in the same order as its rows.  Rationale: the
	 * ListView holds display text only, so a selected row is matched back to its lesson here. */
	protected static List<Integer> listedLessonIds = new ArrayList<Integer>();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: This is last of the GUI areas.  It is used for quitting the application and for
	// logging out.

	/** Logs out and returns to the login page.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewCuratorHome theView;

	/** Reference to the shared in-memory database so this package has access. */
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in user.  Every Model call is made with this user, so the Model can check the
	 * Curator role (US-CH-V). */
	protected static User theUser;

	/** The shared Scene each invocation populates. */
	private static Scene theCuratorHomeScene;
	/** This page's home-page number, used by the account update page to return here.  The
	 * numbering is the Foundations one: Admin 1, Role1 (Contributor) 2, Role2 (Curator) 3. */
	protected static final int theRole = 3;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayCuratorHome(Stage ps, User user) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Curator
	 * Home Page with no status message.  See the three-parameter version. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 */
	public static void displayCuratorHome(Stage ps, User user) {
		displayCuratorHome(ps, user, "");
	}


	/**********
	 * <p> Method: displayCuratorHome(Stage ps, User user, String statusMessage) </p>
	 *
	 * <p> Description: Displays the Curator Home Page and shows a message carried over from the
	 * page the user just left (for example the lock page's "Locked: title.").
	 *
	 * It first sets up the shared attributes so parameters do not have to be passed around.  It
	 * then instantiates the singleton if needed, which positions and wires every widget.  After
	 * that it fills in the parts that change with the user and the data (the user name and the
	 * current lessons), sets the Scene onto the Stage, and shows it.</p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param statusMessage a confirmation to show in green, or an empty String for none
	 */
	public static void displayCuratorHome(Stage ps, User user, String statusMessage) {

		// Establish the references to the GUI and the current user
		theStage = ps;
		theUser = user;

		// If not yet established, populate the static aspects of the GUI
		if (theView == null) theView = new ViewCuratorHome();

		// Populate the dynamic aspects of the GUI with the data from the user and the current
		// state of the system.
		theDatabase.getUserAccountDetails(user.getUserName());
		applicationMain.FoundationsMain.activeHomePage = theRole;
		label_UserDetails.setText("User: " + theUser.getUserName());

		// This page is reused for every login, so the lessons are reloaded every time rather
		// than showing what the last Curator saw.  The carried-over message is shown only when
		// there is one, so it cannot erase a refusal that showAllLessons() has just displayed.
		ControllerCuratorHome.showStatus("", true);
		ControllerCuratorHome.showAllLessons();
		if (statusMessage != null && !statusMessage.isEmpty())
			ControllerCuratorHome.showStatus(statusMessage, true);

		// Set the title for the window, display the page, and wait for the user to do something
		theStage.setTitle("CSE 360 Foundations: Curator Home Page");
		theStage.setScene(theCuratorHomeScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewCuratorHome() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once;
	 * displayCuratorHome() fills in the changeable fields on every later use.</p>
	 */
	private ViewCuratorHome() {

		// Create the Pane for the list of widgets and the Scene for the window
		theRootPane = new Pane();
		theCuratorHomeScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Curator Home Page");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);

		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		setupButtonUI(button_UpdateThisUser, "Dialog", 18, 170, Pos.CENTER, 610, 45);
		button_UpdateThisUser.setOnAction((_) -> {ControllerCuratorHome.performUpdate(); });

		// GUI Area 2: heading, search row, the list on the left, and the details on the right
		setupLabelUI(label_ListHeading, "Arial", 18, 300, Pos.BASELINE_LEFT, 20, 105);
		label_ListHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

		setupTextFieldUI(text_Search, 400, 20, 138);
		text_Search.setPromptText("Search all lessons by keyword");
		text_Search.setOnAction((_) -> {ControllerCuratorHome.performSearch(); });

		setupButtonUI(button_Search, "Dialog", 14, 80, Pos.CENTER, 430, 138);
		button_Search.setOnAction((_) -> {ControllerCuratorHome.performSearch(); });

		setupButtonUI(button_ClearSearch, "Dialog", 14, 80, Pos.CENTER, 520, 138);
		button_ClearSearch.setOnAction((_) -> {ControllerCuratorHome.performClearSearch(); });

		setupErrorLabelUI(label_SearchError, 380, 20, 170);

		listView_Lessons.setLayoutX(20);
		listView_Lessons.setLayoutY(192);
		listView_Lessons.setPrefSize(380, 260);
		// Selecting a row shows that lesson in the details box (US-CH3)
		listView_Lessons.getSelectionModel().selectedIndexProperty().addListener(
				(_, _, _) -> {ControllerCuratorHome.performSelectionChanged(); });

		setupLabelUI(label_DetailsHeading, "Arial", 14, 365, Pos.BASELINE_LEFT, 415, 170);
		label_DetailsHeading.setFont(Font.font("Arial", FontWeight.BOLD, 14));

		text_Details.setLayoutX(415);
		text_Details.setLayoutY(192);
		text_Details.setPrefSize(365, 260);
		text_Details.setFont(Font.font("Arial", 13));
		text_Details.setWrapText(true);
		// Read-only: the box can be scrolled and copied from, but never typed into
		text_Details.setEditable(false);

		// These two labels are only as wide as the list above them, so they do not lie over
		// the Lock / Unlock button to their right and take its mouse clicks
		setupLabelUI(label_ListHint, "Arial", 12, 380, Pos.BASELINE_LEFT, 20, 457);
		label_ListHint.setTextFill(Color.GRAY);
		setupLabelUI(label_ListStatus, "Arial", 12, 380, Pos.BASELINE_LEFT, 20, 475);

		// The Curator actions for the selected lesson sit under its details (Epics E2 to E6)
		setupButtonUI(button_Lock, "Dialog", 14, 365, Pos.CENTER, 415, 460);
		button_Lock.setOnAction((_) -> {ControllerCuratorHome.performLock(); });
		button_Lock.setDisable(true);

		setupLabelUI(label_Status, "Arial", 14, 580, Pos.BASELINE_LEFT, 20, 497);

		// GUI Area 3
        setupButtonUI(button_Logout, "Dialog", 18, 250, Pos.CENTER, 20, 540);
        button_Logout.setOnAction((_) -> {ControllerCuratorHome.performLogout(); });

        setupButtonUI(button_Quit, "Dialog", 18, 250, Pos.CENTER, 300, 540);
        button_Quit.setOnAction((_) -> {ControllerCuratorHome.performQuit(); });

		// This is the end of the GUI initialization code

		// Place all of the widget items into the Root Pane's list of children
		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, button_UpdateThisUser, line_Separator1,
			label_ListHeading, text_Search, button_Search, button_ClearSearch, label_SearchError,
			listView_Lessons, label_DetailsHeading, text_Details, label_ListHint,
			label_ListStatus, label_Status, button_Lock,
	        line_Separator4, button_Logout, button_Quit);
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
	 * Private local method to initialize an error label: small, red, and wrapped, so a longer
	 * message drops to a second line instead of running off the page
	 *
	 * @param l		The Label object to be initialized
	 * @param w		The width of the Label
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupErrorLabelUI(Label l, double w, double x, double y){
		l.setFont(Font.font("Arial", 12));
		l.setTextFill(Color.RED);
		l.setMinWidth(w);
		l.setMaxWidth(w);
		l.setWrapText(true);
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


	/**********
	 * Private local method to initialize the standard fields for a text field
	 *
	 * @param t		The TextField object to be initialized
	 * @param w		The width of the TextField
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupTextFieldUI(TextField t, double w, double x, double y){
		t.setFont(Font.font("Arial", 14));
		t.setMinWidth(w);
		t.setMaxWidth(w);
		t.setLayoutX(x);
		t.setLayoutY(y);
	}
}
