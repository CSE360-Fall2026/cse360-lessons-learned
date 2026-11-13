package guiCuratorLock;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;


/*******
 * <p> Title: ViewCuratorLock Class. </p>
 *
 * <p> Description: The Java/FX-based Curator Lock page (TP3 Aspect 2; Epic E6: "As a
 * lessons-learned curator, I can lock entire lessons or specific fields of a lesson so the
 * contributor can't make changes to fields or delete the lesson").  It is frame 2 of the
 * Initial ConOps storyboard.  The Curator reaches it from the Curator Home Page with a lesson
 * selected, ticks the whole lesson or any of its title, description, tags, and category, and
 * presses Lock (US-CL1) or Unlock (US-CL2).  Nothing is saved until the Curator answers Yes to
 * a confirmation (OB-6).</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * the widgets do lives in ControllerCuratorLock, and the data work, including the Curator role
 * check, lives in ModelCuratorLock.  The page is a singleton: the widgets are created once, and
 * each call to displayCuratorLock() refills the parts that depend on the lesson.</p>
 *
 * <p> The layout follows User Experience.pdf and the Contributor's pages: the lesson at the
 * top, messages shown in place, and Return, Logout, and Quit at the bottom in the same places
 * as on the Lesson Details page.</p>
 *
 * <p> Tested by the manual tests M-CL1 to M-CL6 in Manual Tests.pdf. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-13 Initial version for TP3 Aspect 2
 *
 */

public class ViewCuratorLock {

	/*-*******************************************************************************************

	Attributes

	 */

	// These are the application values required by the user interface

	/** The window width shared by every page.  Source: FoundationsMain. */
	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	/** The window height shared by every page.  Source: FoundationsMain. */
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;


	// These are the widget attributes for the GUI. There are 3 areas for this GUI.

	// GUI Area 1: the purpose of this page and whose account is being used

	/** The page title.  Source: the Foundations page pattern. */
	protected static Label label_PageTitle = new Label();
	/** Whose account is in use.  Source: the Foundations page pattern. */
	protected static Label label_UserDetails = new Label();

	/** Separator between GUI Areas 1 and 2. */
	protected static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2: the lesson, what is locked now, the five boxes, and the two actions

	/** The title of the lesson being changed, or the reason it could not be loaded.  Source:
	 * US-CL1, "I can choose a lesson from my Curator Home page". */
	protected static Label label_LessonTitle = new Label();
	/** The lesson's owner and category.  Rationale: two lessons can share a title, so the
	 * owner confirms this is the lesson the Curator meant. */
	protected static Label label_LessonInfo = new Label();
	/** What is locked now.  Source: OB-10, "both roles shall see which parts are locked". */
	protected static Label label_CurrentLocks = new Label();
	/** Tells the Curator what the boxes are for. */
	protected static Label label_Choose = new Label("Choose what to lock or unlock:");
	/** The whole lesson.  Source: Epic E6, "lock entire lessons". */
	protected static CheckBox checkBox_Entire = new CheckBox("Entire lesson");
	/** The title field.  Source: Epic E6, "or specific fields of a lesson". */
	protected static CheckBox checkBox_Title = new CheckBox("Title");
	/** The description field.  Source: Epic E6. */
	protected static CheckBox checkBox_Description = new CheckBox("Description");
	/** The tags field.  Source: Epic E6. */
	protected static CheckBox checkBox_Tags = new CheckBox("Tags");
	/** The category field.  Source: Epic E6. */
	protected static CheckBox checkBox_Category = new CheckBox("Category");
	/** Explains what a lock does.  Rationale: validation-as-guidance, User Experience.pdf; the
	 * Curator learns the effect before confirming, not after. */
	protected static Label label_Hint = new Label("Locks restrict contributors only.  A locked "
			+ "field cannot be edited, and a lesson with any lock cannot be deleted.  A "
			+ "contributor can still add experience to a locked lesson.");
	/** Locks the ticked parts.  Source: US-CL1. */
	protected static Button button_Lock = new Button("Lock");
	/** Unlocks the ticked parts.  Source: US-CL2. */
	protected static Button button_Unlock = new Button("Unlock");
	/** A refusal in red, or "No change was made." in green.  Source: US-CL-V, "I receive a
	 * specific message and nothing changes". */
	protected static Label label_Status = new Label();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: returning, logging out, and quitting

	/** Returns to the Curator Home Page without changing anything. */
	protected static Button button_Return = new Button("Return");
	/** Logs out and returns to the login page.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewCuratorLock theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in user.  Every Model call is made with this user, so the Model can check the
	 * Curator role (US-CL-V). */
	protected static User theUser;
	/** The id of the lesson being locked or unlocked; -1 until the page is first displayed. */
	protected static int theLessonId = -1;

	/** The shared Scene each invocation populates. */
	private static Scene theCuratorLockScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayCuratorLock(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Curator
	 * Lock page for one lesson.
	 *
	 * It first sets up the shared attributes so parameters do not have to be passed around.  It
	 * then instantiates the singleton if needed, which positions and wires every widget.  After
	 * that it fills in the parts that change with the user and the lesson, sets the Scene onto
	 * the Stage, and shows it.</p>
	 *
	 * <p> FoundationsMain.activeHomePage is left as the Curator Home Page set it, because this
	 * page is reached only from there and has no Account Update button of its own.</p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson to lock or unlock
	 */
	public static void displayCuratorLock(Stage ps, User user, int lessonId) {

		// Establish the references to the GUI, the current user, and the lesson
		theStage = ps;
		theUser = user;
		theLessonId = lessonId;

		// If not yet established, populate the static aspects of the GUI
		if (theView == null) theView = new ViewCuratorLock();

		// Populate the dynamic aspects of the GUI.  The page is reused for every lesson, so the
		// lesson is read again each time rather than showing the last one.
		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerCuratorLock.loadLesson();

		// Set the title for the window, display the page, and wait for the user to do something
		theStage.setTitle("CSE 360 Foundations: Lock or Unlock a Lesson");
		theStage.setScene(theCuratorLockScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewCuratorLock() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once;
	 * displayCuratorLock() fills in the changeable fields on every later use.</p>
	 */
	private ViewCuratorLock() {

		// Create the Pane for the list of widgets and the Scene for the window
		theRootPane = new Pane();
		theCuratorLockScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Lock or Unlock a Lesson");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the lesson and its current locks
		setupLabelUI(label_LessonTitle, "Arial", 20, 760, Pos.BASELINE_LEFT, 20, 103);
		label_LessonTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
		setupLabelUI(label_LessonInfo, "Arial", 14, 760, Pos.BASELINE_LEFT, 20, 135);
		setupLabelUI(label_CurrentLocks, "Arial", 14, 760, Pos.BASELINE_LEFT, 20, 160);

		// The five boxes, in the fixed order every lock message uses
		setupLabelUI(label_Choose, "Arial", 14, 400, Pos.BASELINE_LEFT, 20, 198);
		label_Choose.setFont(Font.font("Arial", FontWeight.BOLD, 14));
		setupCheckBoxUI(checkBox_Entire, 40, 228);
		setupCheckBoxUI(checkBox_Title, 40, 258);
		setupCheckBoxUI(checkBox_Description, 40, 288);
		setupCheckBoxUI(checkBox_Tags, 40, 318);
		setupCheckBoxUI(checkBox_Category, 40, 348);

		setupLabelUI(label_Hint, "Arial", 12, 760, Pos.BASELINE_LEFT, 20, 386);
		label_Hint.setMaxWidth(760);
		label_Hint.setWrapText(true);
		label_Hint.setTextFill(Color.GRAY);

		setupButtonUI(button_Lock, "Dialog", 16, 180, Pos.CENTER, 20, 432);
		button_Lock.setOnAction((_) -> {ControllerCuratorLock.performLock(); });

		setupButtonUI(button_Unlock, "Dialog", 16, 180, Pos.CENTER, 220, 432);
		button_Unlock.setOnAction((_) -> {ControllerCuratorLock.performUnlock(); });

		setupLabelUI(label_Status, "Arial", 14, 760, Pos.BASELINE_LEFT, 20, 482);
		label_Status.setMaxWidth(760);
		label_Status.setWrapText(true);

		// GUI Area 3, in the same places as on the Lesson Details page
        setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
        button_Return.setOnAction((_) -> {ControllerCuratorLock.performReturn(); });

        setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
        button_Logout.setOnAction((_) -> {ControllerCuratorLock.performLogout(); });

        setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
        button_Quit.setOnAction((_) -> {ControllerCuratorLock.performQuit(); });

		// This is the end of the GUI initialization code

		// Place all of the widget items into the Root Pane's list of children
		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1,
			label_LessonTitle, label_LessonInfo, label_CurrentLocks, label_Choose,
			checkBox_Entire, checkBox_Title, checkBox_Description, checkBox_Tags,
			checkBox_Category, label_Hint, button_Lock, button_Unlock, label_Status,
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


	/**********
	 * Private local method to initialize the standard fields for a check box
	 *
	 * @param c		The CheckBox object to be initialized
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupCheckBoxUI(CheckBox c, double x, double y){
		c.setFont(Font.font("Arial", 15));
		c.setLayoutX(x);
		c.setLayoutY(y);
	}
}
