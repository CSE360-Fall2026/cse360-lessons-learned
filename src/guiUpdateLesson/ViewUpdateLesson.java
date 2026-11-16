package guiUpdateLesson;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import entityClasses.User;
import inputValidation.LessonRecognizer;


/*******
 * <p> Title: ViewUpdateLesson Class. </p>
 *
 * <p> Description: The Java/FX-based Update Lesson Page, where a Contributor changes the core
 * information of a lesson they have added (US-U1).  Fields a curator has locked are shown but
 * cannot be edited, and each is marked "(locked)" with a note explaining why (US-U1-V; V1,
 * "Other roles may lock one or more fields, so I am unable to alter them").</p>
 *
 * <p> The form is laid out exactly like the Create Lesson Page so the two feel like one tool,
 * and it applies the same User Experience.pdf patterns: each field's rule stated in its caption,
 * live counters, errors beneath the field when the user leaves it, and a Save button that stays
 * disabled until there is a valid change to save (Khan Academy's "Save changes" button,
 * "disabled by default").</p>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * they do lives in ControllerUpdateLesson, and the data work in ModelUpdateLesson.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (edit form from the HW2 Role1 page)
 *
 */

public class ViewUpdateLesson {

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

	// GUI Area 2: the form, laid out like the Create Lesson Page

	/** Form heading. */
	protected static Label label_FormHeading = new Label("Edit Lesson");
	/** Names the locked fields and explains that they cannot be changed (US-U1-V). */
	protected static Label label_LockNote = new Label();

	/** Title caption with its rule; "(locked)" is appended when the title is locked. */
	protected static Label label_TitleCaption = new Label();
	/** Live title length. */
	protected static Label label_TitleCounter = new Label();
	/** The title input (US-U1); disabled when locked. */
	protected static TextField text_Title = new TextField();
	/** Title error, beneath the field (US-U1-V). */
	protected static Label label_TitleError = new Label();

	/** Description caption with its rule; "(locked)" is appended when it is locked. */
	protected static Label label_DescriptionCaption = new Label();
	/** Live description length. */
	protected static Label label_DescriptionCounter = new Label();
	/** The description input (US-U1); disabled when locked. */
	protected static TextArea text_Description = new TextArea();
	/** Description error, beneath the field (US-U1-V). */
	protected static Label label_DescriptionError = new Label();

	/** Tags caption with its rule; "(locked)" is appended when they are locked. */
	protected static Label label_TagsCaption = new Label();
	/** Live tags length. */
	protected static Label label_TagsCounter = new Label();
	/** The tags input (US-U1); disabled when locked. */
	protected static TextField text_Tags = new TextField();
	/** Tags error, beneath the field (US-U1-V). */
	protected static Label label_TagsError = new Label();

	/** Category caption; "(locked)" is appended when it is locked. */
	protected static Label label_CategoryCaption = new Label();
	/** The category choice (US-U1); disabled when locked. */
	protected static ComboBox <String> combobox_Category = new ComboBox <String>();
	/** Category error, beneath the field (US-U1-V). */
	protected static Label label_CategoryError = new Label();

	/** Saves the changes (US-U1).  Disabled until a field has changed and every editable field is
	 * valid. */
	protected static Button button_SaveChanges = new Button("Save Changes");
	/** Explains why Save Changes may be disabled. */
	protected static Label label_SaveHint = new Label(
			"Save Changes becomes available after you change a field and every field is valid.");
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
	private static ViewUpdateLesson theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in Contributor.  Every Model call is made with this user's name (US-R4). */
	protected static User theUser;
	/** The id of the lesson being edited. */
	protected static int theLessonId = -1;

	/** The shared Scene each invocation populates. */
	private static Scene theUpdateLessonScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayUpdateLesson(Stage ps, User user, int lessonId) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Update
	 * Lesson Page.  It loads the lesson fresh from the database and fills the form with it, so
	 * the user always edits what is actually stored. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 *
	 * @param lessonId specifies the lesson to edit
	 */
	public static void displayUpdateLesson(Stage ps, User user, int lessonId) {
		theStage = ps;
		theUser = user;
		theLessonId = lessonId;
		if (theView == null) theView = new ViewUpdateLesson();

		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerUpdateLesson.loadForm();

		theStage.setTitle("CSE 360 Foundations: Edit Lesson");
		theStage.setScene(theUpdateLessonScene);
		theStage.show();
	}


	/**********
	 * <p> Method: ViewUpdateLesson() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once. </p>
	 */
	private ViewUpdateLesson() {

		theRootPane = new Pane();
		theUpdateLessonScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Edit Lesson");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the form (same positions as the Create Lesson Page)
		setupLabelUI(label_FormHeading, "Arial", 18, 140, Pos.BASELINE_LEFT, 20, 103);
		label_FormHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));
		setupHintLabelUI(label_LockNote, 420, 160, 106);
		label_LockNote.setTextFill(Color.web("#b26a00"));

		setupLabelUI(label_TitleCaption, "Arial", 13, 440, Pos.BASELINE_LEFT, 20, 135);
		setupCounterUI(label_TitleCounter, 480, 135);
		setupTextFieldUI(text_Title, 560, 20, 153);
		setupErrorLabelUI(label_TitleError, 560, 20, 184);

		setupLabelUI(label_DescriptionCaption, "Arial", 13, 440, Pos.BASELINE_LEFT, 20, 204);
		setupCounterUI(label_DescriptionCounter, 480, 204);
		text_Description.setFont(Font.font("Arial", 14));
		text_Description.setLayoutX(20);
		text_Description.setLayoutY(222);
		text_Description.setPrefSize(560, 120);
		text_Description.setWrapText(true);
		setupErrorLabelUI(label_DescriptionError, 560, 20, 345);

		setupLabelUI(label_TagsCaption, "Arial", 13, 360, Pos.BASELINE_LEFT, 20, 368);
		setupCounterUI(label_TagsCounter, 280, 386);
		setupTextFieldUI(text_Tags, 360, 20, 404);
		setupErrorLabelUI(label_TagsError, 360, 20, 435);

		setupLabelUI(label_CategoryCaption, "Arial", 13, 180, Pos.BASELINE_LEFT, 400, 386);
		List<String> categories = new ArrayList<String>();
		categories.add("<Select one>");
		for (String c : LessonRecognizer.VALID_CATEGORIES) categories.add(c);
		combobox_Category.setItems(FXCollections.observableArrayList(categories));
		setupComboBoxUI(combobox_Category, "Dialog", 14, 180, 400, 404);
		setupErrorLabelUI(label_CategoryError, 180, 400, 435);

		setupButtonUI(button_SaveChanges, "Dialog", 16, 180, Pos.CENTER, 600, 153);
		button_SaveChanges.setStyle("-fx-base: #1e5aa8; -fx-text-fill: white;");
		button_SaveChanges.setOnAction((_) -> {ControllerUpdateLesson.performSave(); });
		setupHintLabelUI(label_SaveHint, 180, 600, 200);

		setupErrorLabelUI(label_Status, 560, 20, 470);

		// Live feedback, as on the Create Lesson Page
		text_Title.textProperty().addListener((_, _, _) -> {ControllerUpdateLesson.updateFormState(); });
		text_Description.textProperty().addListener((_, _, _) -> {ControllerUpdateLesson.updateFormState(); });
		text_Tags.textProperty().addListener((_, _, _) -> {ControllerUpdateLesson.updateFormState(); });
		combobox_Category.valueProperty().addListener((_, _, _) -> {ControllerUpdateLesson.updateFormState(); });
		text_Title.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerUpdateLesson.checkField("title"); });
		text_Description.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerUpdateLesson.checkField("description"); });
		text_Tags.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerUpdateLesson.checkField("tags"); });
		combobox_Category.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerUpdateLesson.checkField("category"); });

		// GUI Area 3
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerUpdateLesson.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerUpdateLesson.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerUpdateLesson.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1, label_FormHeading, label_LockNote,
			label_TitleCaption, label_TitleCounter, text_Title, label_TitleError,
			label_DescriptionCaption, label_DescriptionCounter, text_Description,
			label_DescriptionError, label_TagsCaption, label_TagsCounter, text_Tags,
			label_TagsError, label_CategoryCaption, combobox_Category, label_CategoryError,
			button_SaveChanges, label_SaveHint, label_Status,
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


	/**********
	 * Private local method to initialize the standard fields for a ComboBox.  The width is
	 * capped as well as set, otherwise the ComboBox grows past its column.
	 *
	 * @param c		The ComboBox object to be initialized
	 * @param ff	The font to be used
	 * @param f		The size of the font to be used
	 * @param w		The width of the ComboBox
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupComboBoxUI(ComboBox <String> c, String ff, double f, double w,
			double x, double y){
		c.setStyle("-fx-font: " + f + " " + ff + ";");
		c.setMinWidth(w);
		c.setPrefWidth(w);
		c.setMaxWidth(w);
		c.setLayoutX(x);
		c.setLayoutY(y);
	}
}
