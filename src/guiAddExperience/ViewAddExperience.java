package guiAddExperience;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;


/*******
 * <p> Title: ViewAddExperience Class. </p>
 *
 * <p> Description: The Java/FX-based Add Experience Page, where a Contributor records a new
 * attempt to leverage a lesson: what was done and how, the time/effort it took in hours (US-U2),
 * and, optionally, the effort each team member used (Lessons Learned System V1).  Invalid values
 * are refused with a specific message beneath the related field, and nothing is saved
 * (US-U2-V).</p>
 *
 * <p> The team member area is a list the Contributor grows one row at a time with "Add Team
 * Member" (up to ExperienceEntry.MAX_MEMBERS rows); each row has a name, the hours, and a Remove
 * button.  The rows are created by ControllerAddExperience and placed into vbox_Members, which
 * sits inside a ScrollPane so the page keeps its fixed 800 by 600 layout however many rows are
 * added.</p>
 *
 * <p> The page applies the same User Experience.pdf patterns as the other Contributor forms:
 * each rule stated in the caption, a live counter, errors beneath the field when the user leaves
 * it, and an action button that stays disabled until the input is valid.</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * they do lives in ControllerAddExperience, and the data work in ModelAddExperience.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (from the HW2 add-experience dialog)
 *
 */

public class ViewAddExperience {

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

	// GUI Area 2: the experience form

	/** Form heading. */
	protected static Label label_FormHeading = new Label("New Experience Entry");
	/** Which lesson the experience is being added to.  Rationale: the user should be able to
	 * see that the entry will go on the right lesson. */
	protected static Label label_LessonName = new Label();

	/** Time/effort caption with its rule.  Source: US-U2, "the time/effort it took in hours";
	 * rule from LessonRecognizer.checkForValidExperienceValue(). */
	protected static Label label_HoursCaption =
			new Label("Total time / effort in hours (required, more than 0, up to 10000)");
	/** The total hours input (US-U2). */
	protected static TextField text_Hours = new TextField();
	/** Hours error, beneath the field (US-U2-V). */
	protected static Label label_HoursError = new Label();

	/** Description caption with its rule.  Source: US-U2, "a description of what was done and
	 * how"; rule from LessonRecognizer.checkForValidExperienceDescription(). */
	protected static Label label_DescriptionCaption =
			new Label("What was done and how (required, 5 to 2000 characters)");
	/** Live description length. */
	protected static Label label_DescriptionCounter = new Label();
	/** The description input (US-U2). */
	protected static TextArea text_Description = new TextArea();
	/** Description error, beneath the field (US-U2-V). */
	protected static Label label_DescriptionError = new Label();

	/** Team member caption.  Source: V1, "the effort used by the various team members." */
	protected static Label label_MembersCaption =
			new Label("Team members' effort (optional: name and hours for each person)");
	/** Holds one row per team member; the rows are built by the Controller. */
	protected static VBox vbox_Members = new VBox(6);
	/** Scrolls the team member rows so any number of rows fits the fixed layout. */
	protected static ScrollPane scroll_Members = new ScrollPane(vbox_Members);
	/** The name field of each team member row, in row order. */
	protected static List<TextField> memberNameFields = new ArrayList<TextField>();
	/** The hours field of each team member row, in row order. */
	protected static List<TextField> memberHoursFields = new ArrayList<TextField>();
	/** Team member error, beneath the list; the message names the row (US-U2-V). */
	protected static Label label_MembersError = new Label();

	/** Saves the entry (US-U2).  Disabled until every field is valid. */
	protected static Button button_AddExperience = new Button("Add Experience");
	/** Explains why Add Experience may be disabled. */
	protected static Label label_AddHint = new Label(
			"Add Experience becomes available when the hours, the description, and every team "
			+ "member row are filled in correctly.");
	/** Adds a blank team member row (up to ExperienceEntry.MAX_MEMBERS). */
	protected static Button button_AddMember = new Button("Add Team Member");
	/** Shows a failure that is not about one field, such as "Lesson not found." */
	protected static Label label_Status = new Label();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: return, logout, and quit

	/** Returns to the lesson's details without saving. */
	protected static Button button_Return = new Button("Return");
	/** Logs out.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewAddExperience theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in Contributor.  Every Model call is made with this user's name (US-R4). */
	protected static User theUser;
	/** The id of the lesson the experience is being added to. */
	protected static int theLessonId = -1;

	/** The shared Scene each invocation populates. */
	private static Scene theAddExperienceScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayAddExperience(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Add
	 * Experience Page for one lesson.  The form starts blank with no team member rows, so a
	 * previous entry never carries over. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson the experience is for
	 */
	public static void displayAddExperience(Stage ps, User user, int lessonId) {
		theStage = ps;
		theUser = user;
		theLessonId = lessonId;
		if (theView == null) theView = new ViewAddExperience();

		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerAddExperience.resetForm();

		theStage.setTitle("CSE 360 Foundations: Add Experience");
		theStage.setScene(theAddExperienceScene);
		theStage.show();
		text_Hours.requestFocus();
	}


	/**********
	 * <p> Method: ViewAddExperience() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once. </p>
	 */
	private ViewAddExperience() {

		theRootPane = new Pane();
		theAddExperienceScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Add Experience");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the form
		setupLabelUI(label_FormHeading, "Arial", 18, 300, Pos.BASELINE_LEFT, 20, 103);
		label_FormHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
		setupLabelUI(label_LessonName, "Arial", 14, 560, Pos.BASELINE_LEFT, 20, 128);
		label_LessonName.setMaxWidth(560);

		setupLabelUI(label_HoursCaption, "Arial", 13, 560, Pos.BASELINE_LEFT, 20, 155);
		setupTextFieldUI(text_Hours, 160, 20, 173);
		text_Hours.setPromptText("e.g. 3.5");
		setupErrorLabelUI(label_HoursError, 380, 200, 178);

		setupLabelUI(label_DescriptionCaption, "Arial", 13, 440, Pos.BASELINE_LEFT, 20, 208);
		setupCounterUI(label_DescriptionCounter, 480, 208);
		text_Description.setFont(Font.font("Arial", 14));
		text_Description.setLayoutX(20);
		text_Description.setLayoutY(226);
		text_Description.setPrefSize(560, 80);
		text_Description.setWrapText(true);
		text_Description.setPromptText("What did you do, and how did reusing this lesson go?");
		setupErrorLabelUI(label_DescriptionError, 560, 20, 309);

		setupLabelUI(label_MembersCaption, "Arial", 13, 560, Pos.BASELINE_LEFT, 20, 330);
		vbox_Members.setPadding(new Insets(6));
		scroll_Members.setLayoutX(20);
		scroll_Members.setLayoutY(350);
		scroll_Members.setPrefSize(560, 115);
		scroll_Members.setFitToWidth(true);
		setupErrorLabelUI(label_MembersError, 560, 20, 468);

		setupButtonUI(button_AddExperience, "Dialog", 16, 180, Pos.CENTER, 600, 173);
		button_AddExperience.setStyle("-fx-base: #1e5aa8; -fx-text-fill: white;");
		button_AddExperience.setOnAction((_) -> {ControllerAddExperience.performAdd(); });
		setupHintLabelUI(label_AddHint, 180, 600, 218);

		setupButtonUI(button_AddMember, "Dialog", 14, 180, Pos.CENTER, 600, 350);
		button_AddMember.setOnAction((_) -> {ControllerAddExperience.performAddMemberRow(); });

		setupErrorLabelUI(label_Status, 560, 20, 492);

		// Live feedback for the fixed fields; the member rows get theirs when they are created
		text_Hours.textProperty().addListener((_, _, _) -> {ControllerAddExperience.updateFormState(); });
		text_Description.textProperty().addListener((_, _, _) -> {ControllerAddExperience.updateFormState(); });
		text_Hours.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerAddExperience.checkField("experienceValue"); });
		text_Description.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerAddExperience.checkField("experienceDescription"); });

		// GUI Area 3
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerAddExperience.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerAddExperience.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerAddExperience.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1, label_FormHeading,
			label_LessonName, label_HoursCaption, text_Hours, label_HoursError,
			label_DescriptionCaption, label_DescriptionCounter, text_Description,
			label_DescriptionError, label_MembersCaption, scroll_Members, label_MembersError,
			button_AddExperience, label_AddHint, button_AddMember, label_Status,
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
	 * Private local method to initialize a live character counter: small, gray, and
	 * right-aligned in a 100-pixel box ending at the right edge of its field
	 *
	 * @param l		The Label object to be initialized
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupCounterUI(Label l, double x, double y){
		l.setFont(Font.font("Arial", 12));
		l.setTextFill(Color.GRAY);
		l.setMinWidth(100);
		l.setMaxWidth(100);
		l.setAlignment(Pos.BASELINE_RIGHT);
		l.setLayoutX(x);
		l.setLayoutY(y);
	}


	/**********
	 * Private local method to initialize a hint: small, gray, and wrapped
	 *
	 * @param l		The Label object to be initialized
	 * @param w		The width of the Label
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupHintLabelUI(Label l, double w, double x, double y){
		l.setFont(Font.font("Arial", 12));
		l.setTextFill(Color.GRAY);
		l.setMinWidth(w);
		l.setMaxWidth(w);
		l.setWrapText(true);
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
