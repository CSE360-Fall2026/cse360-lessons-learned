package guiUserLogin;

import database.Database;
import entityClasses.User;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import inputValidation.PasswordRecognizer;


/*******
 * <p> Title: ControllerUserLogin Class. </p>
 *
 * <p> Description: The Java/FX-based User Login Page.  This class provides the controller
 * actions basic on the user's use of the JavaFX GUI widgets defined by the View class.
 *
 * This controller determines if the log in is valid.  If so set up the link to the database,
 * determines how many roles this user is authorized to play, and the calls one the of the array of
 * role home pages if there is only one role.  If there are more than one role, it setup up and
 * calls the multiple roles dispatch page for the user to determine which role the user wants to
 * play.
 *
 * The class has been written assuming that the View or the Model are the only class methods that
 * can invoke these methods.  This is why each has been declared at "protected".  Do not change any
 * of these methods to public.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author Lynn Robert Carter
 * @author Virgil Jones &amp; Team Fall 2026
 *
 * @version 1.00		2025-08-17 Initial version
 * @version 1.01		2025-09-16 Update Javadoc documentation *
 * @version 2.00		2026-09-18 Add one-time password check with a required password reset before log in
 * @version 2.01		2026-10-14 TP2: a Role1 user is taken to the Contributor Home Page
 * @version 2.02		2026-11-10 TP3 Aspect 1: a Role2 user is taken to the Curator Home Page
 */

public class ControllerUserLogin {

	/*-********************************************************************************************

	The User Interface Actions for this page

	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and
	the Model is often just a stub, or will be a singleton instantiated object.

	*/

	/**
	 * Default constructor is not used.
	 */
	public ControllerUserLogin() {
	}

	/** Reference for the in-memory database so this package has access. */
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/** The Stage that JavaFX has established for this application. */
	private static Stage theStage;

	/**********
	 * <p> Method: public doLogin() </p>
	 *
	 * <p> Description: This method is called when the user has clicked on the Login button. This
	 * method checks the username and password to see if they are valid.  If so, it then logs that
	 * user in my determining which role to use.
	 *
	 * The method reaches batch to the view page and to fetch the information needed rather than
	 * passing that information as parameters.
	 *
	 * @param ts the JavaFX Stage to show the next page on
	 */
	protected static void doLogin(Stage ts) {
		theStage = ts;
		String username = ViewUserLogin.text_Username.getText();
		String password = ViewUserLogin.text_Password.getText();
    	boolean loginResult = false;

    	// Check if the user is logging in using an active one-time password
		if (theDatabase.isOneTimePasswordValid(username, password)) {
			theDatabase.clearOneTimePassword(password);

			// Create modal
			Stage resetModal = new Stage();
			resetModal.initModality(Modality.APPLICATION_MODAL);
			resetModal.initOwner(theStage);
			resetModal.setTitle("Reset Password");

			// Set layout width and padding
			VBox layout = new VBox(10);
			layout.setPadding(new Insets(20));
			layout.setPrefWidth(420);

			Label titleLabel = new Label("Enter a new password for: " + username);
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

				theDatabase.updatePassword(username, newPass);
				resetModal.close();

				ViewUserLogin.text_Password.setText("");
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Password Reset");
				alert.setHeaderText("Password Successfully Updated");
				alert.setContentText("Your password has been updated.\n Please log in using your new password.");
				alert.showAndWait();
			});

			layout.getChildren().addAll(titleLabel, passField, confirmField, errorLabel, req, upper, lower, digit, special, length, saveBtn);

			resetModal.setScene(new Scene(layout));
			resetModal.showAndWait();
			return;
		}

		// Fetch the user and verify the username
     	if (theDatabase.getUserAccountDetails(username) == false) {
     		// Don't provide too much information.  Don't say the username is invalid or the
     		// password is invalid.  Just say the pair is invalid.
    		ViewUserLogin.alertUsernamePasswordError.setContentText(
    				"Incorrect username/password. Try again!");
    		ViewUserLogin.alertUsernamePasswordError.showAndWait();
    		return;
    	}
		// System.out.println("*** Username is valid");

		// Check to see that the login password matches the account password
    	String actualPassword = theDatabase.getCurrentPassword();

    	if (password.compareTo(actualPassword) != 0) {
    		ViewUserLogin.alertUsernamePasswordError.setContentText(
    				"Incorrect username/password. Try again!");
    		ViewUserLogin.alertUsernamePasswordError.showAndWait();
    		return;
    	}
		// System.out.println("*** Password is valid for this user");

		// Establish this user's details
    	User user = new User(username, password, theDatabase.getCurrentFirstName(),
    			theDatabase.getCurrentMiddleName(), theDatabase.getCurrentLastName(),
    			theDatabase.getCurrentPreferredFirstName(), theDatabase.getCurrentEmailAddress(),
    			theDatabase.getCurrentAdminRole(),
    			theDatabase.getCurrentNewRole1(), theDatabase.getCurrentNewRole2());

    	// See which home page dispatch to use
		int numberOfRoles = theDatabase.getNumberOfRoles(user);
		// System.out.println("*** The number of roles: "+ numberOfRoles);
		if (numberOfRoles == 1) {
			// Single Account Home Page - The user has no choice here

			// Admin role
			if (user.getAdminRole()) {
				loginResult = theDatabase.loginAdmin(user);
				if (loginResult) {
					guiAdminHome.ViewAdminHome.displayAdminHome(theStage, user);
				}
			} else if (user.getNewRole1()) {
				loginResult = theDatabase.loginRole1(user);
				if (loginResult) {
					// TP2: the Role1 flag is the Contributor role (design decision D12)
					guiContributorHome.ViewContributorHome.displayContributorHome(theStage, user);
				}
			} else if (user.getNewRole2()) {
				loginResult = theDatabase.loginRole2(user);
				if (loginResult) {
					// TP3: the Role2 flag is the Curator role (Aspect 1; ConOps assumption A1)
					guiCuratorHome.ViewCuratorHome.displayCuratorHome(theStage, user);
				}
				// Other roles
			} else {
				System.out.println("***** UserLogin goToUserHome request has an invalid role");
			}
		} else if (numberOfRoles > 1) {
			// Multiple Account Home Page - The user chooses which role to play
			// System.out.println("*** Going to displayMultipleRoleDispatch");
			guiMultipleRoleDispatch.ViewMultipleRoleDispatch.
				displayMultipleRoleDispatch(theStage, user);
		}
	}


	/**********
	 * <p> Method: setup() </p>
	 *
	 * <p> Description: This method is called to reset the page and then populate it with new
	 * content for the new user.</p>
	 *
	 * @param theStage			the JavaFX Stage to show the new account page on
	 * @param invitationCode	the invitation code the new user entered
	 */
	protected static void doSetupAccount(Stage theStage, String invitationCode) {
		guiNewAccount.ViewNewAccount.displayNewAccount(theStage, invitationCode);
	}


	/**********
	 * <p> Method: public performQuit() </p>
	 *
	 * <p> Description: This method is called when the user has clicked on the Quit button.  Doing
	 * this terminates the execution of the application.  All important data must be stored in the
	 * database, so there is no cleanup required.  (This is important so we can minimize the impact
	 * of crashed.)
	 *
	 */
	protected static void performQuit() {
		System.out.println("Perform Quit");
		System.exit(0);
	}

}
