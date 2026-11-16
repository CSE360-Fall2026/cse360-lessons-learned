package guiCreateLesson;

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
 * <p> Title: ViewCreateLesson Class. </p>
 *
 * <p> Description: The Java/FX-based Create Lesson Page, where a Contributor adds a new lesson
 * learned by entering its core information: title, description, tags, and category (US-C1).
 * Invalid input is refused with a specific message shown directly beneath the field that caused
 * it, and nothing is saved (US-C1-V).</p>
 *
 * <p> The form follows the patterns the team chose in User Experience.pdf:</p>
 * <ul>
 * <li> Validation as guidance, not only rejection (Khan Academy): each caption states the
 * 		field's rule before the user types.</li>
 * <li> Proactive length feedback (Khan Academy's "160 characters left"): a live counter beside
 * 		each text field, which turns red when the limit is exceeded.</li>
 * <li> Errors beneath the field as soon as the user leaves it (GitHub, Discord).</li>
 * <li> A disabled action button until the input meets the requirements (Khan Academy), so the
 * 		state of the form is visible before anything is submitted.</li>
 * <li> "(required)" written in words rather than an asterisk, which GitHub's page showed is
 * 		better for accessibility.</li>
 * </ul>
 *
 * <p> As in every Foundations View, this class only declares and positions the widgets.  What
 * they do lives in ControllerCreateLesson, and the data work in ModelCreateLesson.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (create form from the HW2 Role1 page)
 *
 */

public class ViewCreateLesson {

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

	// GUI Area 2: the form.  Each field has a caption stating its rule, a live counter, the input
	// itself, and an error label directly beneath it (InputValidation.pdf).

	/** Form heading. */
	protected static Label label_FormHeading = new Label("New Lesson");

	/** Title caption with its rule.  Source: US-C1 core information; rule from
	 * LessonRecognizer.checkForValidTitle(). */
	protected static Label label_TitleCaption = new Label("Title (required, up to 100 characters)");
	/** Live title length, for example "12 / 100". */
	protected static Label label_TitleCounter = new Label();
	/** The title input (US-C1). */
	protected static TextField text_Title = new TextField();
	/** Title error, beneath the field (US-C1-V). */
	protected static Label label_TitleError = new Label();

	/** Description caption with its rule.  Source: US-C1; rule from
	 * LessonRecognizer.checkForValidDescription(). */
	protected static Label label_DescriptionCaption =
			new Label("Description (required, 10 to 5000 characters)");
	/** Live description length. */
	protected static Label label_DescriptionCounter = new Label();
	/** The description input (US-C1).  A TextArea because a description may be several
	 * paragraphs. */
	protected static TextArea text_Description = new TextArea();
	/** Description error, beneath the field (US-C1-V). */
	protected static Label label_DescriptionError = new Label();

	/** Tags caption with its rule.  Source: US-C1; rule from LessonRecognizer.checkForValidTags(). */
	protected static Label label_TagsCaption =
			new Label("Tags (optional, separated by commas, each up to 30 characters)");
	/** Live tags length. */
	protected static Label label_TagsCounter = new Label();
	/** The tags input (US-C1). */
	protected static TextField text_Tags = new TextField();
	/** Tags error, beneath the field (US-C1-V). */
	protected static Label label_TagsError = new Label();

	/** Category caption.  Source: US-C1. */
	protected static Label label_CategoryCaption = new Label("Category (required)");
	/** The category choice.  A fixed list rather than free text, so categories stay consistent;
	 * the first item is a "&lt;Select one&gt;" prompt, as in the other Foundations ComboBoxes. */
	protected static ComboBox <String> combobox_Category = new ComboBox <String>();
	/** Category error, beneath the field (US-C1-V). */
	protected static Label label_CategoryError = new Label();

	/** Saves the lesson (US-C1).  Disabled until every field is valid (User Experience.pdf). */
	protected static Button button_CreateLesson = new Button("Create Lesson");
	/** Explains why Create Lesson may be disabled.  Rationale: a disabled button with no
	 * explanation leaves the user guessing. */
	protected static Label label_CreateHint = new Label(
			"Create Lesson becomes available when every required field is filled in correctly.");
	/** Shows a failure that is not about one field, such as a database error. */
	protected static Label label_Status = new Label();

	/** Separator between GUI Areas 2 and 3. */
	protected static Line line_Separator4 = new Line(20, 525, width-20,525);

	// GUI Area 3: return, logout, and quit, as on the Foundations Add/Remove Roles page

	/** Returns to the Contributor Home Page without saving. */
	protected static Button button_Return = new Button("Return");
	/** Logs out.  Source: the Foundations page pattern. */
	protected static Button button_Logout = new Button("Logout");
	/** Ends the application.  Source: the Foundations page pattern. */
	protected static Button button_Quit = new Button("Quit");

	// This is the end of the GUI objects for the page.

	/** The singleton instance; null until the page is first displayed. */
	private static ViewCreateLesson theView;

	/** The Stage that JavaFX has established for this application. */
	protected static Stage theStage;
	/** The Pane that holds all the GUI widgets. */
	protected static Pane theRootPane;
	/** The logged-in Contributor, who will own the new lesson (US-R4). */
	protected static User theUser;

	/** The shared Scene each invocation populates. */
	private static Scene theCreateLessonScene;


	/*-*******************************************************************************************

	Constructors

	 */

	/**********
	 * <p> Method: displayCreateLesson(Stage ps, User user) </p>
	 *
	 * <p> Description: The single entry point from outside this package to display the Create
	 * Lesson Page.  It instantiates the singleton if needed, then resets the form to blank so a
	 * previous visit's text never carries over into a new lesson, and shows the page. </p>
	 *
	 * @param ps specifies the JavaFX Stage to be used for this GUI and its methods
	 *
	 * @param user specifies the User for this GUI and its methods
	 */
	public static void displayCreateLesson(Stage ps, User user) {
		theStage = ps;
		theUser = user;
		if (theView == null) theView = new ViewCreateLesson();

		label_UserDetails.setText("User: " + theUser.getUserName());
		ControllerCreateLesson.resetForm();

		theStage.setTitle("CSE 360 Foundations: Create Lesson");
		theStage.setScene(theCreateLessonScene);
		theStage.show();
		text_Title.requestFocus();
	}


	/**********
	 * <p> Method: ViewCreateLesson() </p>
	 *
	 * <p> Description: Initializes every element of the graphical user interface: location,
	 * size, font, color, and event handlers.  This is a singleton, so it runs only once. </p>
	 */
	private ViewCreateLesson() {

		theRootPane = new Pane();
		theCreateLessonScene = new Scene(theRootPane, width, height);

		// GUI Area 1
		label_PageTitle.setText("Create Lesson");
		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		// GUI Area 2: the form
		setupLabelUI(label_FormHeading, "Arial", 18, 300, Pos.BASELINE_LEFT, 20, 103);
		label_FormHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

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

		// The action column on the right, with the primary action emphasized
		setupButtonUI(button_CreateLesson, "Dialog", 16, 180, Pos.CENTER, 600, 153);
		button_CreateLesson.setStyle("-fx-base: #1e5aa8; -fx-text-fill: white;");
		button_CreateLesson.setOnAction((_) -> {ControllerCreateLesson.performCreate(); });
		setupHintLabelUI(label_CreateHint, 180, 600, 200);

		setupErrorLabelUI(label_Status, 560, 20, 470);

		// Live feedback: every change updates the counters and the button state, and leaving a
		// field shows its error (or clears it once the field is fixed)
		text_Title.textProperty().addListener((_, _, _) -> {ControllerCreateLesson.updateFormState(); });
		text_Description.textProperty().addListener((_, _, _) -> {ControllerCreateLesson.updateFormState(); });
		text_Tags.textProperty().addListener((_, _, _) -> {ControllerCreateLesson.updateFormState(); });
		combobox_Category.valueProperty().addListener((_, _, _) -> {ControllerCreateLesson.updateFormState(); });
		text_Title.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerCreateLesson.checkField("title"); });
		text_Description.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerCreateLesson.checkField("description"); });
		text_Tags.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerCreateLesson.checkField("tags"); });
		combobox_Category.focusedProperty().addListener((_, _, focused) ->
				{ if (!focused) ControllerCreateLesson.checkField("category"); });

		// GUI Area 3
		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> {ControllerCreateLesson.performReturn(); });
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 300, 540);
		button_Logout.setOnAction((_) -> {ControllerCreateLesson.performLogout(); });
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> {ControllerCreateLesson.performQuit(); });

		theRootPane.getChildren().addAll(
			label_PageTitle, label_UserDetails, line_Separator1, label_FormHeading,
			label_TitleCaption, label_TitleCounter, text_Title, label_TitleError,
			label_DescriptionCaption, label_DescriptionCounter, text_Description,
			label_DescriptionError, label_TagsCaption, label_TagsCounter, text_Tags,
			label_TagsError, label_CategoryCaption, combobox_Category, label_CategoryError,
			button_CreateLesson, label_CreateHint, label_Status,
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
