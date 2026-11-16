
package guiViewerHome;

import java.util.ArrayList;
import java.util.List;

import entityClasses.User;
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

/*******
 * <p> Title: ViewViewerHome Class </p>
 *
 * <p> Description: Provides the Viewer Home Page for TP3 Aspect 9.
 * A Viewer can list all lessons, search by keyword, and select
 * a lesson to read its details. The page does not provide
 * controls for creating, editing, deleting, or locking lessons.
 * </p>
 *
 * <p> This class implements the graphical interface for
 * US-V-01, US-V-02, US-V-03, and US-V-04. Lesson retrieval,
 * authorization, and search validation are handled by
 * ModelViewerHome through ControllerViewerHome. </p>
 *
 * <p> The layout follows the existing Curator Home Page,
 * maintaining consistency with the team's JavaFX interface
 * while removing Curator-specific actions. </p>
 *
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 9
 */
public class ViewViewerHome {

    /** Width of the application window. */
    private static final double width =
            applicationMain.FoundationsMain.WINDOW_WIDTH;

    /** Height of the application window. */
    private static final double height =
            applicationMain.FoundationsMain.WINDOW_HEIGHT;

    // GUI Area 1: Page heading and current user.

    /** Title of the Viewer Home Page. */
    protected static Label label_PageTitle = new Label();

    /** Username of the currently logged-in Viewer. */
    protected static Label label_UserDetails = new Label();

	/** Opens the account update page for the Viewer. */
	protected static Button button_UpdateThisUser = new Button("Account Update");

    /** Separator between the heading and lesson content. */
    protected static Line line_Separator1 =
            new Line(20, 95, width - 20, 95);

    // GUI Area 2: Lesson listing, search, and details.

    /** Heading for the collection of lessons (US-V-01). */
    protected static Label label_ListHeading =
            new Label("All Lessons Learned");

    /** Keyword search input (US-V-02). */
    protected static TextField text_Search = new TextField();

    /** Executes the keyword search. */
    protected static Button button_Search =
            new Button("Search");

    /** Clears the search and restores the full lesson list. */
    protected static Button button_ClearSearch =
            new Button("Clear");

    /** Displays search validation messages. */
    protected static Label label_SearchError = new Label();

    /** Displays the available lessons (US-V-01). */
    protected static ListView<String> listView_Lessons =
            new ListView<String>();

    /** Heading for the selected lesson details. */
    protected static Label label_DetailsHeading =
            new Label("Selected lesson");

    /** Displays the selected lesson without editing (US-V-03). */
    protected static TextArea text_Details =
            new TextArea();

    /** Provides instructions for selecting a lesson. */
    protected static Label label_ListHint =
            new Label("Select a lesson to view its details.");

    /** Displays the number of lessons currently shown. */
    protected static Label label_ListStatus = new Label();

    /** Displays operation status and error messages. */
    protected static Label label_Status = new Label();

    /**
     * Stores lesson identifiers in the same order as the
     * displayed list rows, allowing the Controller to retrieve
     * the selected lesson without relying on its display text.
     */
    protected static List<Integer> listedLessonIds =
            new ArrayList<Integer>();

    /** Separator between lesson content and navigation. */
    protected static Line line_Separator4 =
            new Line(20, 525, width - 20, 525);

    // GUI Area 3: Navigation.

    /** Returns the user to the login page. */
    protected static Button button_Logout =
            new Button("Logout");

    /** Exits the application. */
    protected static Button button_Quit =
            new Button("Quit");

    /** Singleton instance of the Viewer Home Page. */
    private static ViewViewerHome theView;

    /** JavaFX Stage used by the application. */
    protected static Stage theStage;

    /** Root pane containing the interface components. */
    protected static Pane theRootPane;

    /** Currently logged-in user. */
    protected static User theUser;

    /** Scene used to display the Viewer Home Page. */
    private static Scene theViewerHomeScene;

    /**********
     * <p> Method: displayViewerHome(Stage ps, User user) </p>
     *
     * <p> Description: Displays the Viewer Home Page for
     * the specified user. This method provides the entry
     * point used by the application's role-dispatch system.
     * </p>
     *
     * @param ps the JavaFX Stage used by the application
     *
     * @param user the currently logged-in user
     */
    public static void displayViewerHome(Stage ps, User user) {

        displayViewerHome(ps, user, "");
    }

    /**********
     * <p> Method: displayViewerHome(Stage ps, User user,
     * String statusMessage) </p>
     *
     * <p> Description: Initializes the Viewer Home Page and
     * reloads the available lessons. A status message may
     * be displayed when returning from another page.
     * </p>
     *
     * @param ps the JavaFX Stage used by the application
     *
     * @param user the currently logged-in user
     *
     * @param statusMessage an optional status message
     */
    public static void displayViewerHome(
            Stage ps, User user, String statusMessage) {

        theStage = ps;
        theUser = user;

		// Home page 4 corresponds to the Viewer role.
		applicationMain.FoundationsMain.activeHomePage = 4;

        if (theView == null) {
            theView = new ViewViewerHome();
        }

        label_UserDetails.setText(
                "User: " + theUser.getUserName());

        ControllerViewerHome.showStatus("", true);
        ControllerViewerHome.showAllLessons();

        if (statusMessage != null
                && !statusMessage.isEmpty()) {

            ControllerViewerHome.showStatus(
                    statusMessage, true);
        }

        theStage.setTitle(
                "CSE 360 Foundations: Viewer Home Page");

        theStage.setScene(theViewerHomeScene);
        theStage.show();
    }

    /**********
     * <p> Method: ViewViewerHome() </p>
     *
     * <p> Description: Initializes the graphical components
     * of the Viewer Home Page, including their positions,
     * formatting, and event handlers. The page follows
     * the existing Curator Home Page layout.
     * </p>
     */
    private ViewViewerHome() {

        theRootPane = new Pane();

        theViewerHomeScene =
                new Scene(theRootPane, width, height);

        // GUI Area 1.

        label_PageTitle.setText("Viewer Home Page");

        setupLabelUI(label_PageTitle, 28,
                width, Pos.CENTER, 0, 5);

        setupLabelUI(label_UserDetails, 20,
                width, Pos.BASELINE_LEFT, 20, 55);

		setupButtonUI(button_UpdateThisUser, 18, 170, 610, 45);

		button_UpdateThisUser.setOnAction(_ -> guiUserUpdate.ViewUserUpdate.displayUserUpdate(theStage, theUser));

        // GUI Area 2.

        setupLabelUI(label_ListHeading, 18,
                300, Pos.BASELINE_LEFT, 20, 105);

        label_ListHeading.setFont(
                Font.font("Arial", FontWeight.BOLD, 18));

        text_Search.setLayoutX(20);
        text_Search.setLayoutY(138);
        text_Search.setPrefWidth(400);

        text_Search.setPromptText(
                "Search all lessons by keyword");

        text_Search.setOnAction(event ->
                ControllerViewerHome.performSearch());

        setupButtonUI(button_Search, 14,
                80, 430, 138);

        button_Search.setOnAction(event ->
                ControllerViewerHome.performSearch());

        setupButtonUI(button_ClearSearch, 14,
                80, 520, 138);

        button_ClearSearch.setOnAction(event ->
                ControllerViewerHome.performClearSearch());

        setupLabelUI(label_SearchError, 12,
                380, Pos.BASELINE_LEFT, 20, 170);

        label_SearchError.setTextFill(Color.RED);

        listView_Lessons.setLayoutX(20);
        listView_Lessons.setLayoutY(192);
        listView_Lessons.setPrefSize(380, 260);

        listView_Lessons.getSelectionModel()
                .selectedIndexProperty()
                .addListener((observable, oldValue, newValue) ->
                        ControllerViewerHome.performSelectionChanged());

        setupLabelUI(label_DetailsHeading, 14,
                365, Pos.BASELINE_LEFT, 415, 170);

        label_DetailsHeading.setFont(
                Font.font("Arial", FontWeight.BOLD, 14));

        text_Details.setLayoutX(415);
        text_Details.setLayoutY(192);
        text_Details.setPrefSize(365, 260);

        text_Details.setFont(Font.font("Arial", 13));
        text_Details.setWrapText(true);

        // Viewer details must not be editable (US-V-04).
        text_Details.setEditable(false);

        setupLabelUI(label_ListHint, 12,
                380, Pos.BASELINE_LEFT, 20, 457);

        label_ListHint.setTextFill(Color.GRAY);

        setupLabelUI(label_ListStatus, 12,
                380, Pos.BASELINE_LEFT, 20, 475);

        setupLabelUI(label_Status, 14,
                580, Pos.BASELINE_LEFT, 20, 497);

        // GUI Area 3.

        setupButtonUI(button_Logout, 18,
                250, 20, 540);

		button_Logout.setOnAction(_ -> ControllerViewerHome.performLogout());

        setupButtonUI(button_Quit, 18,
                250, 300, 540);

		button_Quit.setOnAction(_ -> ControllerViewerHome.performQuit());

        theRootPane.getChildren().addAll(
                label_PageTitle,
                label_UserDetails,
                line_Separator1,
                label_ListHeading,
                text_Search,
                button_Search,
                button_ClearSearch,
				button_UpdateThisUser,
                label_SearchError,
                listView_Lessons,
                label_DetailsHeading,
                text_Details,
                label_ListHint,
                label_ListStatus,
                label_Status,
                line_Separator4,
                button_Logout,
                button_Quit
        );
    }

    /**********
     * <p> Method: setupLabelUI(Label label, double fontSize,
     * double labelWidth, Pos position, double x, double y) </p>
     *
     * <p> Description: Applies consistent font, alignment,
     * size, and positioning to a label.
     * </p>
     *
     * @param label the label being configured
     * @param fontSize the label's font size
     * @param labelWidth the width of the label
     * @param position the alignment of the label text
     * @param x the horizontal position
     * @param y the vertical position
     */
    private static void setupLabelUI(
            Label label, double fontSize, double labelWidth,
            Pos position, double x, double y) {

        label.setFont(Font.font("Arial", fontSize));
        label.setPrefWidth(labelWidth);
        label.setAlignment(position);
        label.setLayoutX(x);
        label.setLayoutY(y);
    }

    /**********
     * <p> Method: setupButtonUI(Button button,
     * double fontSize, double buttonWidth,
     * double x, double y) </p>
     *
     * <p> Description: Applies consistent dimensions,
     * font, and positioning to a button.
     * </p>
     *
     * @param button the button being configured
     * @param fontSize the button's font size
     * @param buttonWidth the width of the button
     * @param x the horizontal position
     * @param y the vertical position
     */
    private static void setupButtonUI(
            Button button, double fontSize,
            double buttonWidth, double x, double y) {

        button.setFont(Font.font("Dialog", fontSize));
        button.setPrefWidth(buttonWidth);
        button.setLayoutX(x);
        button.setLayoutY(y);
    }
}
