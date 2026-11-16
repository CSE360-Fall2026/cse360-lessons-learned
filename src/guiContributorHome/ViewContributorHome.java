package guiContributorHome;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import database.Database;
import entityClasses.User;


/*******
 * <p> Title: ViewContributorHome Class. </p>
 *
 * <p> Description: The Java/FX-based Contributor Home Page.  This is the page a user playing the
 * Contributor role sees after logging in.  It lists every lesson learned the Contributor has
 * added (US-R1), lets the Contributor search their titles (US-R2, US-R2-V), and is the starting
 * point for every other Contributor action: creating a lesson (guiCreateLesson) and opening a
 * lesson to view, edit, add experience to, or delete it (guiLessonDetails).</p>
 *
 * <p> This page replaces the Foundations Role1 home page.  The Lessons Learned System V1 stories
 * say new roles will replace Role1 and Role2; this is the Contributor role.  The user's role is
 * still stored in the Foundations newRole1 database column (design decision D12), so existing
 * accounts and invitations keep working.</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * the widgets do lives in ControllerContributorHome, and the data work lives in
 * ModelContributorHome.  The page is a singleton: the widgets are created once, and each call to
 * displayContributorHome() refills the parts that depend on the user and the data.</p>
 *
 * <p> The layout follows User Experience.pdf: a clean, uncrowded page with few elements (the
 * "rule of 7"), one clearly emphasized primary action (New Lesson), and messages shown in place
 * rather than in pop-ups.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Lynn Robert Carter (Contributor home page by Emilia Davey)
 *
 * @version 1.00		2025-08-20 Initial version (as the Role1 home page)
 * @version 2.00		2026-10-14 TP2: the Contributor Home Page, split out of the HW2 Role1 page
 *
 */

public class ViewContributorHome {

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

	// GUI Area 2: the Contributor's list of lessons and the search box

	/** Heading for the list.  Rationale: says plainly what the list holds (US-R1). */
	protected static Label label_ListHeading = new Label("My Lessons Learned");
	/** The search text.  Source: US-R2, "I can search the titles of my lessons learned." */
	protected static TextField text_Search = new TextField();
	/** Runs the search (US-R2). */
	protected static Button button_Search = new Button("Search");
	/** Clears the search and shows every lesson again.  Rationale: an empty search shows
	 * nothing (design decision D6), so this button is how the full list comes back. */
	protected static Button button_ClearSearch = new Button("Clear");
	/** Shows a search-text error beneath the search box (US-R2-V; InputValidation.pdf). */
	protected static Label label_SearchError = new Label();
	/** The Contributor's lessons, one row each.  Source: US-R1, "a list of all the lessons
	 * learned I have added," and V1, "I can use that list to specify which lesson learned to
	 * examine more closely." */
	protected static ListView<String> listView_Lessons = new ListView<String>();
	/** Tells the user how to open a lesson.  Rationale: validation-as-guidance, User
	 * Experience.pdf; the user is told what to do before having to guess. */
	protected static Label label_ListHint =
			new Label("Double-click a lesson, or select it and press Open Lesson.");
	/** How many lessons the list is showing. */
	protected static Label label_ListStatus = new Label();
	/** The primary action: start a new lesson (US-C1). */
	protected static Button button_NewLesson = new Button("New Lesson");
	/** Opens the selected lesson's details (US-R3).  Disabled until a row is selected, so the
	 * button's state shows whether it can be used (User Experience.pdf). */
	protected static Button button_OpenLesson = new Button("Open Lesson");
	/** A confirmation or error carried over from another page, such as "Lesson deleted". */
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
	private static ViewContributorHome theView;

	/** Reference to the shared in-memory database so this package has access. */
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in user.  Every Model call is made with this user's name (US-R4). */
	protected static User theUser;

	/** The shared Scene each invocation populates. */
	private static Scene theContributorHomeScene;
	/** This page's home-page number, used by the account update page to return here.  The
	 * numbering is the Foundations one: Admin 1, Role1 (Contributor) 2, Role2 (Curator) 3. */
	protected static final int theRole = 2;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayContributorHome(Stage ps, User user) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the
	 * Contributor Home Page with no status message.  See the three-parameter version. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 */
	public static void displayContributorHome(Stage ps, User user) {
		displayContributorHome(ps, user, "");
	}


	/**********
	 * <p> Method: displayContributorHome(Stage ps, User user, String statusMessage) </p>
	 *
	 * <p> Description: Displays the Contributor Home Page and shows a message carried over from
	 * the page the user just left (for example "Lesson deleted").
	 *
	 * It first sets up the shared attributes so parameters do not have to be passed around.  It
	 * then instantiates the singleton if needed, which positions and wires every widget.  After
	 * that it fills in the parts that change with the user and the data (the user name and this
	 * Contributor's current lessons), sets the Scene onto the Stage, and shows it.</p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param statusMessage a confirmation to show in green, or an empty String for none
	 */
	public static void displayContributorHome(Stage ps, User user, String statusMessage) {

		// Establish the references to the GUI and the current user
		theStage = ps;
		theUser = user;

		// If not yet established, populate the static aspects of the GUI
		if (theView == null) theView = new ViewContributorHome();

		// Populate the dynamic aspects of the GUI with the data from the user and the current
		// state of the system.
		theDatabase.getUserAccountDetails(user.getUserName());
		applicationMain.FoundationsMain.activeHomePage = theRole;
		label_UserDetails.setText("User: " + theUser.getUserName());

		// This page is reused for every login, so this user's lessons are reloaded every time
		// rather than showing whoever used the page last (US-R4)
		ControllerContributorHome.showAllMyLessons();
		ControllerContributorHome.showStatus(statusMessage, true);

		// Set the title for the window, display the page, and wait for the user to do something
		theStage.setTitle("CSE 360 Foundations: Contributor Home Page");
		theStage.setScene(theContributorHomeScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewContributorHome() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once;
	 * displayContributorHome() fills in the changeable fields on every later use.</p>
	 */
	private ViewContributorHome() {

		// Create the Pane for the list of widgets and the Scene for the window
		theRootPane = new Pane();
		theContributorHomeScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Contributor Home Page");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);

		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		setupButtonUI(button_UpdateThisUser, "Dialog", 18, 170, Pos.CENTER, 610, 45);
		button_UpdateThisUser.setOnAction((_) -> {ControllerContributorHome.performUpdate(); });

		// GUI Area 2: heading, search row, list, and the actions beside the list
		setupLabelUI(label_ListHeading, "Arial", 18, 300, Pos.BASELINE_LEFT, 20, 105);
		label_ListHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

		setupTextFieldUI(text_Search, 400, 20, 138);
		text_Search.setPromptText("Search my lessons by title");
		text_Search.setOnAction((_) -> {ControllerContributorHome.performSearch(); });

		setupButtonUI(button_Search, "Dialog", 14, 80, Pos.CENTER, 430, 138);
		button_Search.setOnAction((_) -> {ControllerContributorHome.performSearch(); });

		setupButtonUI(button_ClearSearch, "Dialog", 14, 80, Pos.CENTER, 520, 138);
		button_ClearSearch.setOnAction((_) -> {ControllerContributorHome.performClearSearch(); });

		setupErrorLabelUI(label_SearchError, 580, 20, 170);

		listView_Lessons.setLayoutX(20);
		listView_Lessons.setLayoutY(192);
		listView_Lessons.setPrefSize(580, 260);
		// Selecting a row enables Open Lesson; double-clicking a row opens it (US-R3)
		listView_Lessons.getSelectionModel().selectedIndexProperty().addListener(
				(_, _, _) -> {ControllerContributorHome.performSelectionChanged(); });
		listView_Lessons.setOnMouseClicked((event) -> {
			if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
				ControllerContributorHome.performOpenLesson();
		});

		setupLabelUI(label_ListHint, "Arial", 12, 580, Pos.BASELINE_LEFT, 20, 457);
		label_ListHint.setTextFill(Color.GRAY);
		setupLabelUI(label_ListStatus, "Arial", 12, 580, Pos.BASELINE_LEFT, 20, 475);

		// The primary action is emphasized; Open Lesson is a normal button
		setupButtonUI(button_NewLesson, "Dialog", 16, 160, Pos.CENTER, 620, 192);
		button_NewLesson.setStyle("-fx-base: #1e5aa8; -fx-text-fill: white;");
		button_NewLesson.setOnAction((_) -> {ControllerContributorHome.performNewLesson(); });

		setupButtonUI(button_OpenLesson, "Dialog", 16, 160, Pos.CENTER, 620, 242);
		button_OpenLesson.setOnAction((_) -> {ControllerContributorHome.performOpenLesson(); });

		setupLabelUI(label_Status, "Arial", 14, 580, Pos.BASELINE_LEFT, 20, 497);

		// GUI Area 3
        setupButtonUI(button_Logout, "Dialog", 18, 250, Pos.CENTER, 20, 540);
        button_Logout.setOnAction((_) -> {ControllerContributorHome.performLogout(); });

        setupButtonUI(button_Quit, "Dialog", 18, 250, Pos.CENTER, 300, 540);
        button_Quit.setOnAction((_) -> {ControllerContributorHome.performQuit(); });

		// This is the end of the GUI initialization code

		// Place all of the widget items into the Root Pane's list of children
		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, button_UpdateThisUser, line_Separator1,
			label_ListHeading, text_Search, button_Search, button_ClearSearch, label_SearchError,
			listView_Lessons, label_ListHint, label_ListStatus, button_NewLesson,
			button_OpenLesson, label_Status,
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
