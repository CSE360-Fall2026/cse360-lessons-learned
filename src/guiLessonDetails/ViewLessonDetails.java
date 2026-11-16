package guiLessonDetails;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;


/*******
 * <p> Title: ViewLessonDetails Class. </p>
 *
 * <p> Description: The Java/FX-based Lesson Details Page.  It shows one lesson learned in full:
 * its core information, which fields are locked and therefore unavailable to edit, and its
 * experience entries with each team member's share of the effort (US-R3; V1, "When I examine a
 * lesson more closely, I can see the fields I can alter, and which are locked").  From here the
 * Contributor can edit the lesson (guiUpdateLesson), record a new experience (guiAddExperience),
 * or delete it after an explicit "Yes" (US-D1, US-D1-V).</p>
 *
 * <p> The page applies the patterns chosen in User Experience.pdf:</p>
 * <ul>
 * <li> A clear pencil icon as the entry point for editing (iHerb).  It is drawn as a vector
 * 		shape rather than a font character, so it appears correctly whatever fonts the
 * 		computer has.</li>
 * <li> Distinct action buttons (Discord) in one column beside the content.</li>
 * <li> The destructive action isolated in its own area with a red button (Khan Academy, "Delete
 * 		your account sits in its own card ... and uses a red button to signal
 * 		irreversibility").</li>
 * <li> Locks explained in words, so the user is never left wondering why a field cannot be
 * 		changed.</li>
 * </ul>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * they do lives in ControllerLessonDetails, and the data work in ModelLessonDetails.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (details from the HW2 Role1 page)
 *
 */

public class ViewLessonDetails {

	/*-*******************************************************************************************

	Attributes

	 */

	/** The window width shared by every page.  Source: FoundationsMain. */
	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	/** The window height shared by every page.  Source: FoundationsMain. */
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	/** The pencil drawn on the Edit button.  Source: the "edit" icon of Google's Material Icons
	 * (Apache License 2.0), reduced to its outline path. */
	private static final String PENCIL_PATH = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z"
			+ "M20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 "
			+ "1.83-1.83z";

	// GUI Area 1: the purpose of this page and whose account is being used

	/** The page title.  Source: the Foundations page pattern. */
	protected static Label label_PageTitle = new Label();
	/** Whose account is in use.  Source: the Foundations page pattern. */
	protected static Label label_UserDetails = new Label();

	/** Separator between GUI Areas 1 and 2. */
	protected static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2, left: the lesson itself (read-only)

	/** The lesson's title, shown as the heading.  Source: US-R3, "view its full details." */
	protected static Label label_LessonTitle = new Label();
	/** Explains which fields are locked and what that means.  Source: US-R3, "which fields are
	 * locked and unavailable for me to edit"; US-D1-V. */
	protected static Label label_LockBanner = new Label();
	/** The category, with "(locked)" when it is locked. */
	protected static Label label_Category = new Label();
	/** The tags, with "(locked)" when they are locked. */
	protected static Label label_Tags = new Label();
	/** Holds the category and tags side by side, 30 pixels apart, so they never overlap. */
	protected static HBox hbox_CategoryTags = new HBox(30);
	/** Caption above the description, with "(locked)" when it is locked. */
	protected static Label label_DescriptionCaption = new Label();
	/** The description, read-only.  A TextArea so a long description can be scrolled. */
	protected static TextArea text_Description = new TextArea();
	/** When the lesson was created and last updated. */
	protected static Label label_Dates = new Label();
	/** Heading above the experience log, with the number of entries. */
	protected static Label label_ExperienceHeading = new Label();
	/** The experience log, read-only: each attempt's date, hours, description, and team
	 * members' shares.  Source: US-R3, "including its experience entries"; V1, "the effort used
	 * by the various team members." */
	protected static TextArea text_Experience = new TextArea();
	/** A confirmation (for example "Lesson updated") or an error for this page. */
	protected static Label label_Status = new Label();

	// GUI Area 2, right: the actions

	/** Opens the edit page (US-U1).  Disabled when every core field is locked, because there
	 * would be nothing left to edit. */
	protected static Button button_Edit = new Button("Edit Lesson");
	/** Opens the add-experience page (US-U2).  Locks never block this: they protect the core
	 * fields, not the recording of a new attempt to use the lesson. */
	protected static Button button_AddExperience = new Button("Add Experience");
	/** Heading of the isolated area for the destructive action. */
	protected static Label label_DangerZone = new Label("Danger zone");
	/** Deletes the lesson after an explicit "Yes" (US-D1).  Red and set apart (Khan Academy). */
	protected static Button button_Delete = new Button("Delete Lesson");
	/** Explains, when the lesson is locked, why deleting it will be refused (US-D1-V). */
	protected static Label label_DeleteHint = new Label();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: return, logout, and quit

	/** Returns to the Contributor Home Page. */
	protected static Button button_Return = new Button("Return");
	/** Logs out.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewLessonDetails theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in Contributor.  Every Model call is made with this user's name (US-R4). */
	protected static User theUser;
	/** The id of the lesson being shown.  Rationale: the Controller's actions all act on it. */
	protected static int theLessonId = -1;

	/** The shared Scene each invocation populates. */
	private static Scene theLessonDetailsScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayLessonDetails(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The entry point from outside this package to display one lesson with no
	 * status message.  See the four-parameter version. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson to show
	 */
	public static void displayLessonDetails(Stage ps, User user, int lessonId) {
		displayLessonDetails(ps, user, lessonId, "");
	}


	/**********
	 * <p> Method: displayLessonDetails(Stage ps, User user, int lessonId, String statusMessage)
	 * </p>
	 *
	 * <p> Description: Displays one lesson and shows a confirmation carried over from the page
	 * the user just left (for example "Lesson created" or "Experience added").  The lesson is
	 * always reloaded from the database, so the page shows exactly what is stored.  If the
	 * lesson does not exist or belongs to someone else, the page shows "Lesson not found." and
	 * disables every action (US-R3-V). </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson to show
	 *
	 * @param statusMessage a confirmation to show in green, or an empty String for none
	 */
	public static void displayLessonDetails(Stage ps, User user, int lessonId,
			String statusMessage) {
		theStage = ps;
		theUser = user;
		theLessonId = lessonId;
		if (theView == null) theView = new ViewLessonDetails();

		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerLessonDetails.loadLesson();
		if (statusMessage != null && !statusMessage.isEmpty())
			ControllerLessonDetails.showStatus(statusMessage, true);

		theStage.setTitle("CSE 360 Foundations: Lesson Details");
		theStage.setScene(theLessonDetailsScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewLessonDetails() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once. </p>
	 */
	private ViewLessonDetails() {

		theRootPane = new Pane();
		theLessonDetailsScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Lesson Details");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2, left: the lesson (all read-only)
		setupLabelUI(label_LessonTitle, "Arial", 20, 560, Pos.BASELINE_LEFT, 20, 103);
		label_LessonTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
		label_LessonTitle.setMaxWidth(560);

		setupWrappedLabelUI(label_LockBanner, 560, 20, 133);
		label_LockBanner.setTextFill(Color.web("#b26a00"));

		// Category and tags sit in one row that sizes itself to the text, so a long category
		// with "(locked)" pushes the tags to the right instead of drawing over them
		label_Category.setFont(Font.font("Arial", 14));
		label_Category.setMinWidth(Region.USE_PREF_SIZE);
		label_Tags.setFont(Font.font("Arial", 14));
		hbox_CategoryTags.getChildren().addAll(label_Category, label_Tags);
		hbox_CategoryTags.setLayoutX(20);
		hbox_CategoryTags.setLayoutY(172);
		hbox_CategoryTags.setMaxWidth(560);
		hbox_CategoryTags.setPrefWidth(560);

		setupLabelUI(label_DescriptionCaption, "Arial", 14, 300, Pos.BASELINE_LEFT, 20, 197);
		setupReadOnlyAreaUI(text_Description, 20, 217, 560, 105);

		setupLabelUI(label_Dates, "Arial", 12, 560, Pos.BASELINE_LEFT, 20, 326);
		label_Dates.setTextFill(Color.GRAY);

		setupLabelUI(label_ExperienceHeading, "Arial", 14, 400, Pos.BASELINE_LEFT, 20, 348);
		label_ExperienceHeading.setFont(Font.font("Arial", FontWeight.BOLD, 14));
		setupReadOnlyAreaUI(text_Experience, 20, 368, 560, 100);

		setupWrappedLabelUI(label_Status, 560, 20, 475);
		label_Status.setFont(Font.font("Arial", 14));

		// GUI Area 2, right: edit and add experience at the top ...
		SVGPath pencil = new SVGPath();
		pencil.setContent(PENCIL_PATH);
		pencil.setScaleX(0.75);
		pencil.setScaleY(0.75);
		button_Edit.setGraphic(pencil);
		button_Edit.setGraphicTextGap(8);
		setupButtonUI(button_Edit, "Dialog", 16, 180, Pos.CENTER, 600, 103);
		button_Edit.setOnAction((_) -> {ControllerLessonDetails.performEdit(); });

		setupButtonUI(button_AddExperience, "Dialog", 16, 180, Pos.CENTER, 600, 153);
		button_AddExperience.setOnAction((_) -> {ControllerLessonDetails.performAddExperience(); });

		// ... and the destructive action set apart at the bottom, in red
		setupLabelUI(label_DangerZone, "Arial", 13, 180, Pos.BASELINE_LEFT, 600, 368);
		label_DangerZone.setFont(Font.font("Arial", FontWeight.BOLD, 13));
		label_DangerZone.setTextFill(Color.web("#c62828"));
		setupButtonUI(button_Delete, "Dialog", 16, 180, Pos.CENTER, 600, 390);
		button_Delete.setStyle("-fx-base: #c62828; -fx-text-fill: white;");
		button_Delete.setOnAction((_) -> {ControllerLessonDetails.performDelete(); });
		setupWrappedLabelUI(label_DeleteHint, 180, 600, 432);
		label_DeleteHint.setFont(Font.font("Arial", 12));
		label_DeleteHint.setTextFill(Color.GRAY);

		// GUI Area 3
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerLessonDetails.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerLessonDetails.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerLessonDetails.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1,
			label_LessonTitle, label_LockBanner, hbox_CategoryTags,
			label_DescriptionCaption, text_Description, label_Dates, label_ExperienceHeading,
			text_Experience, label_Status, button_Edit, button_AddExperience,
			label_DangerZone, button_Delete, label_DeleteHint,
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
	 * Private local method to initialize a label whose text may need more than one line
	 *
	 * @param l		The Label object to be initialized
	 * @param w		The width of the Label
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupWrappedLabelUI(Label l, double w, double x, double y){
		l.setFont(Font.font("Arial", 13));
		l.setMinWidth(w);
		l.setMaxWidth(w);
		l.setWrapText(true);
		l.setLayoutX(x);
		l.setLayoutY(y);
	}


	/**********
	 * Private local method to initialize a read-only, wrapped, scrollable text area
	 *
	 * @param t		The TextArea object to be initialized
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 * @param w		The width of the TextArea
	 * @param h		The height of the TextArea
	 */
	private static void setupReadOnlyAreaUI(TextArea t, double x, double y, double w, double h){
		t.setFont(Font.font("Arial", 13));
		t.setEditable(false);
		t.setWrapText(true);
		t.setFocusTraversable(false);
		t.setLayoutX(x);
		t.setLayoutY(y);
		t.setPrefSize(w, h);
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
