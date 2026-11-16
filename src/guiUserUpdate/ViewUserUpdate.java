package guiUserUpdate;

import java.util.Optional;

import database.Database;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import entityClasses.User;
import inputValidation.NameInputRecognizer;
import inputValidation.EmailAddressRecognizer;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import inputValidation.PasswordRecognizer;

/*******
 * <p> Title: ViewUserUpdate Class. </p>
 * 
 * <p> Description: The Java/FX-based User Update Page.  This page enables the user to update the
 * attributes about the user held by the system.  Currently, this page does not provide a mechanism
 * to change the Username and not all of the functions on this page are implemented.
 * 
 * Currently the following attributes can be updated:
 * 		- First Name
 * 		- Middle Name
 * 		- Last Name
 * 		- Preferred First Name
 * 		- Email Address
 * The page uses dialog boxes for updating these items.</p>
 * 
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 * 
 * @author Lynn Robert Carter
 * @author Virgil Jones &amp; Team Fall 2026
 * 
 * @version 1.01		2025-08-19 Initial version plus new internal documentation
 * @version 2.00		2026-09-17 User input information has validation, and Preferred Name changed to Display Name, 
 * 									Email update validation, Password change and validation
 *  
 */

public class ViewUserUpdate {

	/*-********************************************************************************************

	Attributes

	 */

	// These are the application values required by the user interface
	
	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	
	// These are the widget attributes for the GUI. There are 3 areas for this GUI.
	
	// Unlike may of the other pages, the GUI on this page is not organized into areas and the user
	// is not able to logout, return, or quit from this page
	
	// These widgets display the purpose of the page and guide the user.
	private static Label label_ApplicationTitle = new Label("Update a User's Account Details");
    private static Label label_Purpose = 
    		new Label(" Use this page to define or update your account information."); 
    
    // These are static output labels and do not change during execution
	private static Label label_Username = new Label("Username:");
	private static Label label_Password = new Label("Password:");
	private static Label label_FirstName = new Label("First Name:");
	private static Label label_MiddleName = new Label("Middle Name:");
	private static Label label_LastName = new Label("Last Name:");
	private static Label label_PreferredFirstName = new Label("Display Name:");
	private static Label label_EmailAddress = new Label("Email Address:");
	
	// These are dynamic labels and they change based on the user and user interactions.
	private static Label label_CurrentUsername = new Label();
	private static Label label_CurrentPassword = new Label();
	private static Label label_CurrentFirstName = new Label();
	private static Label label_CurrentMiddleName = new Label();
	private static Label label_CurrentLastName = new Label();
	private static Label label_CurrentPreferredFirstName = new Label();
	private static Label label_CurrentEmailAddress = new Label();
	
	// These buttons enable the user to edit the various dynamic fields.  The username and the
	// passwords for a user are currently not editable.
	private static Button button_UpdateUsername = new Button("Update Username");
	private static Button button_UpdatePassword = new Button("Update Password");
	private static Button button_UpdateFirstName = new Button("Update First Name");
	private static Button button_UpdateMiddleName = new Button("Update Middle Name");
	private static Button button_UpdateLastName = new Button("Update Last Name");
	// private static Button button_UpdatePreferredFirstName = new Button("Update Display Name");
	private static Button button_UpdateEmailAddress = new Button("Update Email Address");

	// This button enables the user to finish working on this page and proceed to the user's home
	// page determined by the user's role at the time of log in.
	private static Button button_ProceedToUserHomePage = new Button("Proceed to the User Home Page");
	
	// This is the end of the GUI widgets for this page.
	
	// These are the set of pop-up dialog boxes that are used to enable the user to change the
	// the values of the various account detail items.
	private static TextInputDialog dialogUpdateFirstName;
	private static TextInputDialog dialogUpdateMiddleName;
	private static TextInputDialog dialogUpdateLastName;
	private static TextInputDialog dialogUpdatePreferredFirstName;
	private static TextInputDialog dialogUpdateEmailAddresss;
	
	// These attributes are used to configure the page and populate it with this user's information
	private static ViewUserUpdate theView;	// Used to determine if instantiation of the class
											// is needed

	// This enables access to the application's database
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	private static Stage theStage;				// The Stage that JavaFX has established for us	
	private static Pane theRootPane;			// The Pane that holds all the GUI widgets
	private static User theUser;				// The current user of the application

	public static Scene theUserUpdateScene = null;	// The Scene each invocation populates

	private static Optional<String> result;		// The result from a pop-up dialog

	/*-********************************************************************************************

	Constructors
	
	 */


	/**********
	 * <p> Method: displayUserUpdate(Stage ps, User user) </p>
	 * 
	 * <p> Description: This method is the single entry point from outside this package to cause
	 * the UserUpdate page to be displayed.
	 * 
	 * It first sets up very shared attributes so we don't have to pass parameters.
	 * 
	 * It then checks to see if the page has been setup.  If not, it instantiates the class, 
	 * initializes all the static aspects of the GUI widgets (e.g., location on the page, font,
	 * size, and any methods to be performed).
	 * 
	 * After the instantiation, the code then populates the elements that change based on the user
	 * and the system's current state.  It then sets the Scene onto the stage, and makes it visible
	 * to the user.
	 * 
	 * @param ps specifies the JavaFX Stage to be used for this GUI and it's methods
	 * 
	 * @param user specifies the User whose roles will be updated
	 *
	 */
	public static void displayUserUpdate(Stage ps, User user) {
		
		// Establish the references to the GUI and the current user
		theUser = user;
		theStage = ps;
		
		// If not yet established, populate the static aspects of the GUI by creating the 
		// singleton instance of this class
		if (theView == null) theView = new ViewUserUpdate();
		
		// Set the widget values that change from use of page to another use of the page.
		String s = "";
		
		// Set the dynamic aspects of the window based on the user logged in and the current state
		// of the various account elements.
		s = theUser.getUserName();
		System.out.println("*** Fetching account data for user: " + s);
    	if (s == null || s.length() < 1)label_CurrentUsername.setText("<none>");
    	else label_CurrentUsername.setText(s);
		
		s = theUser.getPassword();
    	if (s == null || s.length() < 1)label_CurrentPassword.setText("<none>");
    	else label_CurrentPassword.setText(s);
    	
		s = theUser.getFirstName();
    	if (s == null || s.length() < 1)label_CurrentFirstName.setText("<none>");
    	else label_CurrentFirstName.setText(s);
       
        s = theUser.getMiddleName();
    	if (s == null || s.length() < 1)label_CurrentMiddleName.setText("<none>");
    	else label_CurrentMiddleName.setText(s);
        
        s = theUser.getLastName();
    	if (s == null || s.length() < 1)label_CurrentLastName.setText("<none>");
    	else label_CurrentLastName.setText(s);
        
    	// Set the display name to default First + Last name, or the preferred name, or <none>
		refreshDisplayName();
        
		s = theUser.getEmailAddress();
    	if (s == null || s.length() < 1)label_CurrentEmailAddress.setText("<none>");
    	else label_CurrentEmailAddress.setText(s);

		// Set the title for the window, display the page, and wait for the Admin to do something
    	theStage.setTitle("CSE 360 Foundation Code: Update User Account Details");
        theStage.setScene(theUserUpdateScene);
		theStage.show();
	}

	
	/**********
	 * <p> Method: ViewUserUpdate() </p>
	 * 
	 * <p> Description: This method initializes all the elements of the graphical user interface.
	 * This method determines the location, size, font, color, and change and event handlers for
	 * each GUI object.</p>
	 * 
	 * <p> This is a singleton and is only performed once.  Subsequent uses fill in the changeable
	 * fields using the displayUserUpdate method.</p>
	 * 
	 */
	
	private ViewUserUpdate() {

		// Create the Pane for the list of widgets and the Scene for the window
		theRootPane = new Pane();
		theUserUpdateScene = new Scene(theRootPane, width, height);
		theUserUpdateScene.getStylesheets().add(
			    getClass().getResource("/applicationMain/application.css").toExternalForm()
			);

		// Initialize the pop-up dialogs to an empty text filed.
		dialogUpdateFirstName = new TextInputDialog("");
		dialogUpdateMiddleName = new TextInputDialog("");
		dialogUpdateLastName = new TextInputDialog("");
		dialogUpdatePreferredFirstName = new TextInputDialog("");
		dialogUpdateEmailAddresss = new TextInputDialog("");

		// Establish the label for each of the dialogs.
		dialogUpdateFirstName.setTitle("Update First Name");
		dialogUpdateFirstName.setHeaderText("Update your First Name");
		
		dialogUpdateMiddleName.setTitle("Update Middle Name");
		dialogUpdateMiddleName.setHeaderText("Update your Middle Name");
		
		dialogUpdateLastName.setTitle("Update Last Name");
		dialogUpdateLastName.setHeaderText("Update your Last Name");
		
		dialogUpdatePreferredFirstName.setTitle("Update Display Name");	// updated to be display name
		dialogUpdatePreferredFirstName.setHeaderText("Update your Display Name");	// updated to be display name
		
		dialogUpdateEmailAddresss.setTitle("Update Email Address");
		dialogUpdateEmailAddresss.setHeaderText("Update your Email Address");

		// Label theScene with the name of the startup screen, centered at the top of the pane
		setupLabelUI(label_ApplicationTitle, "Arial", 28, width, Pos.CENTER, 0, 5);

        // Label to display the welcome message for the first theUser
        setupLabelUI(label_Purpose, "Arial", 20, width, Pos.CENTER, 0, 50);
        
        // Display the titles, values, and update buttons for the various admin account attributes.
        // If the attributes is null or empty, display "<none>".
        
        // USername
        setupLabelUI(label_Username, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 100);
        setupLabelUI(label_CurrentUsername, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 100);
        setupButtonUI(button_UpdateUsername, "Dialog", 18, 275, Pos.CENTER, 500, 93);
       
        // password
        setupLabelUI(label_Password, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 150);
        setupLabelUI(label_CurrentPassword, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 150);
        setupButtonUI(button_UpdatePassword, "Dialog", 18, 275, Pos.CENTER, 500, 143);
        // Password updater (same function as the password reset on login page)
        button_UpdatePassword.setOnAction((_) -> {
        	// Create modal
            Stage resetModal = new Stage();
            resetModal.initModality(Modality.APPLICATION_MODAL);
            resetModal.initOwner(theStage);
            resetModal.setTitle("Update Password");

         	// Set layout width and padding
            VBox layout = new VBox(10);
            layout.setPadding(new Insets(20));
            layout.setPrefWidth(420);

            Label titleLabel = new Label("Update Password for: " + theUser.getUserName());
            titleLabel.setFont(Font.font("Arial", 16));

            PasswordField passField = new PasswordField();
            passField.setPromptText("New Password");

            PasswordField confirmField = new PasswordField();
            confirmField.setPromptText("Confirm New Password");

            Label errorLabel = new Label();
            errorLabel.setTextFill(Color.RED);

            // Password checklist requirement labels
            Label req = new Label("Password requirements:");
            Label upper = new Label("At least one upper case letter");
            Label lower = new Label("At least one lower case letter");
            Label digit = new Label("At least one numeric digit");
            Label special = new Label("At least one special character");
            Label length = new Label("At least eight characters");

            upper.setTextFill(Color.RED);
            lower.setTextFill(Color.RED);
            digit.setTextFill(Color.RED);
            special.setTextFill(Color.RED);
            length.setTextFill(Color.RED);

            // Dynamic listener
            passField.textProperty().addListener((_, _, newVal) -> {
                PasswordRecognizer.evaluatePassword(newVal);

                upper.setTextFill(PasswordRecognizer.foundUpperCase ? Color.GREEN : Color.RED);
                lower.setTextFill(PasswordRecognizer.foundLowerCase ? Color.GREEN : Color.RED);
                digit.setTextFill(PasswordRecognizer.foundNumericDigit ? Color.GREEN : Color.RED);
                special.setTextFill(PasswordRecognizer.foundSpecialChar ? Color.GREEN : Color.RED);
                length.setTextFill(PasswordRecognizer.foundLongEnough ? Color.GREEN : Color.RED);
            });

            Button saveBtn = new Button("Save New Password");
            saveBtn.setOnAction((_) -> {
                String newPass = passField.getText();
                String confirmPass = confirmField.getText();

                String passErr = PasswordRecognizer.evaluatePassword(newPass);
                if (!passErr.isEmpty()) {
                    errorLabel.setText(passErr);
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    errorLabel.setText("Passwords do not match. Try again!");
                    return;
                }

                // Update database
                theDatabase.updatePassword(theUser.getUserName(), newPass);
                theUser.setPassword(newPass);
                label_CurrentPassword.setText(newPass);

                resetModal.close();

                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Password Updated");
                alert.setHeaderText("Password Successfully Saved");
                alert.setContentText("Your password has been updated.");
                alert.showAndWait();
            });

            layout.getChildren().addAll(titleLabel, passField, confirmField, errorLabel, req, upper, lower, digit, special, length, saveBtn);

            resetModal.setScene(new Scene(layout));
            resetModal.showAndWait();
        });       
        
        // First Name
        setupLabelUI(label_FirstName, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 200);
        setupLabelUI(label_CurrentFirstName, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 200);
        setupButtonUI(button_UpdateFirstName, "Dialog", 18, 275, Pos.CENTER, 500, 193);
        button_UpdateFirstName.setOnAction((_) -> {
            dialogUpdateFirstName.setHeaderText("Update your First Name");
            
            // Clear input and fill with current users name
            String currentUser = (theUser.getFirstName() == null) ? "" : theUser.getFirstName();
            dialogUpdateFirstName.getEditor().setText(currentUser);
            
            result = dialogUpdateFirstName.showAndWait();
            
            // Keep dialog open while being validated
            while (result.isPresent()) {
                String newFirstName = result.get().trim();
                String err = NameInputRecognizer.checkForValidName(newFirstName, "First Name", false);
                
                // If error, update header with error message
                if (!err.isEmpty()) {
                    dialogUpdateFirstName.setHeaderText(err);
                    result = dialogUpdateFirstName.showAndWait(); // reopen pop-uup with error
                } else {
                    // Valid input: update database
                    theDatabase.updateFirstName(theUser.getUserName(), newFirstName);
                    theDatabase.getUserAccountDetails(theUser.getUserName());
                    String newName = theDatabase.getCurrentFirstName();
                    theUser.setFirstName(newName);
                    if (newName == null || newName.length() < 1) label_CurrentFirstName.setText("<none>");
                    else label_CurrentFirstName.setText(newName);
                    refreshDisplayName();
                    break;
                }
            }
        });
               
        // Middle Name
        setupLabelUI(label_MiddleName, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 250);
        setupLabelUI(label_CurrentMiddleName, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 250);
        setupButtonUI(button_UpdateMiddleName, "Dialog", 18, 275, Pos.CENTER, 500, 243);
        button_UpdateMiddleName.setOnAction((_) -> {
	        dialogUpdateMiddleName.setHeaderText("Update your Middle Name");
	        
	        // Clear input and fill with current users name
            String currentUser = (theUser.getMiddleName() == null) ? "" : theUser.getMiddleName();
            dialogUpdateMiddleName.getEditor().setText(currentUser);
            
	        result = dialogUpdateMiddleName.showAndWait();
	        
	        // Keep dialog open while being validated
	        while (result.isPresent()) {
	            String newMiddleName = result.get().trim();
	            String err = NameInputRecognizer.checkForValidName(newMiddleName, "Middle Name", false);
	            
	            // If error, update header with error message
	            if (!err.isEmpty()) {
	            	dialogUpdateMiddleName.setHeaderText(err);
	                result = dialogUpdateMiddleName.showAndWait(); // reopen pop-uup with error
	            } else {
	                // Valid input: update database
	                theDatabase.updateMiddleName(theUser.getUserName(), newMiddleName);
	                theDatabase.getUserAccountDetails(theUser.getUserName());
	                String newName = theDatabase.getCurrentMiddleName();
	                theUser.setMiddleName(newName);
	                if (newName == null || newName.length() < 1) label_CurrentMiddleName.setText("<none>");
	                else label_CurrentMiddleName.setText(newName);
	                break;
	            }
	        }
	    });
        
        // Last Name
        setupLabelUI(label_LastName, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 300);
        setupLabelUI(label_CurrentLastName, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 300);
        setupButtonUI(button_UpdateLastName, "Dialog", 18, 275, Pos.CENTER, 500, 293);
        button_UpdateLastName.setOnAction((_) -> {
	        dialogUpdateLastName.setHeaderText("Update your Last Name");
	        
	        // Clear input and fill with current users name
            String currentUser = (theUser.getLastName() == null) ? "" : theUser.getLastName();
            dialogUpdateLastName.getEditor().setText(currentUser);
            
	        result = dialogUpdateLastName.showAndWait();
	        
	        // Keep dialog open while being validated
	        while (result.isPresent()) {
	            String newLastName = result.get().trim();
	            String err = NameInputRecognizer.checkForValidName(newLastName, "Last Name", false);
	            
	            // If error, update header with error message
	            if (!err.isEmpty()) {
	            	dialogUpdateLastName.setHeaderText(err);
	                result = dialogUpdateLastName.showAndWait(); // reopen pop-uup with error
	            } else {
	                // Valid input: update database
	                theDatabase.updateLastName(theUser.getUserName(), newLastName);
	                theDatabase.getUserAccountDetails(theUser.getUserName());
	                String newName = theDatabase.getCurrentLastName();
	                theUser.setLastName(newName);
	                if (newName == null || newName.length() < 1) label_CurrentLastName.setText("<none>");
	                else label_CurrentLastName.setText(newName);
	                refreshDisplayName();
	                break;
	            }
	        }
	    });
        
        // Display Name - Used to be Preferred First Name
        setupLabelUI(label_PreferredFirstName, "Arial", 18, 190, Pos.BASELINE_RIGHT, 
        		5, 350);
        setupLabelUI(label_CurrentPreferredFirstName, "Arial", 18, 260, Pos.BASELINE_LEFT, 
        		200, 350);
        /*
         * Disabled display name validation or user input change. Should default to first and last name.
         * 
        setupButtonUI(button_UpdatePreferredFirstName, "Dialog", 18, 275, Pos.CENTER, 500, 343);
        button_UpdatePreferredFirstName.setOnAction((_) -> {
        	dialogUpdatePreferredFirstName.setHeaderText("Update your Display Name");
            result = dialogUpdatePreferredFirstName.showAndWait();
            // Keep dialog open while being validated
            while (result.isPresent()) {
                String newPrefferedName = result.get().trim();
                String err = NameInputRecognizer.checkForValidName(newPrefferedName, "Display Name", false);
                
                // If error, update header with error message
                if (!err.isEmpty()) {
                	dialogUpdatePreferredFirstName.setHeaderText(err);
                    result = dialogUpdatePreferredFirstName.showAndWait(); // reopen pop-uup with error
                } else {
                    // Valid input: update database
                    theDatabase.updatePreferredFirstName(theUser.getUserName(), newPrefferedName);
                    theDatabase.getUserAccountDetails(theUser.getUserName());
                    String newName = theDatabase.getCurrentPreferredFirstName();
                    theUser.setPreferredFirstName(newName);
                    String firstName = (theUser.getFirstName() == null) ? "" : theUser.getFirstName();
                    String lastName = (theUser.getLastName() == null) ? "" : theUser.getLastName();
                    String defaultDisplayName = (firstName + " " + lastName).trim();
                    if(newName != null && newName.length() > 0) {
                    	label_CurrentPreferredFirstName.setText(newName);
                    } else if (!defaultDisplayName.isEmpty()) {
                    	label_CurrentPreferredFirstName.setText(defaultDisplayName);
                    } else {
                    	label_CurrentPreferredFirstName.setText("<none>");
                    }
                    break;
                }
            }
        });
        */
        
        
        
        // Email Address
        setupLabelUI(label_EmailAddress, "Arial", 18, 190, Pos.BASELINE_RIGHT, 5, 400);
        setupLabelUI(label_CurrentEmailAddress, "Arial", 18, 260, Pos.BASELINE_LEFT, 200, 400);
        setupButtonUI(button_UpdateEmailAddress, "Dialog", 18, 275, Pos.CENTER, 500, 393);
        button_UpdateEmailAddress.setOnAction((_) -> {
        	dialogUpdateEmailAddresss.setHeaderText("Update your Email Address");
        	
        	// Clear input and fill with current users name
        	String currentEmail = (theUser.getEmailAddress() == null) ? "" : theUser.getEmailAddress();
            dialogUpdateEmailAddresss.getEditor().setText(currentEmail);
            
	        result = dialogUpdateEmailAddresss.showAndWait();
	        
	        // Keep dialog open while being validated
	        while (result.isPresent()) {
	            String newEmail = result.get().trim();
	            String err = EmailAddressRecognizer.checkEmailAddress(newEmail);
	            
	            // If error, update header with error message
	            if (!err.isEmpty()) {
	            	dialogUpdateEmailAddresss.setHeaderText(err);
	                result = dialogUpdateEmailAddresss.showAndWait(); // reopen pop-uup with error
	            } else {
	                // Valid input: update database
	                theDatabase.updateEmailAddress(theUser.getUserName(), newEmail);
	                theDatabase.getUserAccountDetails(theUser.getUserName());
	                String newName = theDatabase.getCurrentEmailAddress();
	                theUser.setEmailAddress(newName);
	                if (newName == null || newName.length() < 1) label_CurrentEmailAddress.setText("<none>");
	                else label_CurrentEmailAddress.setText(newName);
	                break;
	            }
	        }
	    });
        
        // Set up the button to proceed to this user's home page
        setupButtonUI(button_ProceedToUserHomePage, "Dialog", 18, 300, 
        		Pos.CENTER, width/2-150, 500);
        button_ProceedToUserHomePage.setOnAction((_) -> 
        	{ControllerUserUpdate.goToUserHomePage(theStage, theUser);});
        button_ProceedToUserHomePage.getStyleClass().add("primary-button");
    	
        // Populate the Pane's list of children widgets
        // Removed "button_UpdatePreferredFirstName" - No longer setting a preferred name or display name
        theRootPane.getChildren().addAll(
        		label_ApplicationTitle, label_Purpose, label_Username,
        		label_CurrentUsername, 
        		label_Password, label_CurrentPassword, 
        		button_UpdatePassword, 
        		label_FirstName, label_CurrentFirstName, button_UpdateFirstName,
        		label_MiddleName, label_CurrentMiddleName, button_UpdateMiddleName,
        		label_LastName, label_CurrentLastName, button_UpdateLastName,
        		label_PreferredFirstName, label_CurrentPreferredFirstName,
        		button_UpdateEmailAddress,
        		label_EmailAddress, label_CurrentEmailAddress, 
        		button_ProceedToUserHomePage);
	}
	
	// Display name auto update
	// Default to first and last name
    protected static void refreshDisplayName() {
    	String firstName = (theUser.getFirstName() == null) ? "" : theUser.getFirstName();
        String lastName = (theUser.getLastName() == null) ? "" : theUser.getLastName();
        String defaultDisplayName = (firstName + " " + lastName).trim();
        
        if(defaultDisplayName.isEmpty()) {
        	label_CurrentPreferredFirstName.setText("<none>");
        } else {
        	label_CurrentPreferredFirstName.setText(defaultDisplayName);
        }
      		
		// Update database with updated display name
		theUser.setPreferredFirstName(defaultDisplayName);
		theDatabase.updatePreferredFirstName(theUser.getUserName(), defaultDisplayName);
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
	 * @param w		The width of the Button
	 * @param p		The alignment (e.g. left, centered, or right)
	 * @param x		The location from the left edge (x axis)
	 * @param y		The location from the top (y axis)
	 */
	private static void setupLabelUI(Label l, String ff, double f, double w, Pos p, double x, double y){
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
	private static void setupButtonUI(Button b, String ff, double f, double w, Pos p, double x, double y){
		b.setFont(Font.font(ff, f));
		b.setMinWidth(w);
		b.setAlignment(p);
		b.setLayoutX(x);
		b.setLayoutY(y);		
	}
}
