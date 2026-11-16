package guiFirstAdmin;

import java.sql.SQLException;
import database.Database;
import entityClasses.User;
import javafx.stage.Stage;
import inputValidation.UserNameRecognizer;
import inputValidation.PasswordRecognizer;
import javafx.scene.paint.Color;

/*******
 * <p> Title: ControllerFirstAdmin Class. </p>
 * 
 * <p> Description: ControllerFirstAdmin class provides the controller actions based on the user's
 *  use of the JavaFX GUI widgets defined by the View class.
 * 
 * This page contains a number of buttons that have not yet been implemented.  WHhen those buttons
 * are pressed, an alert pops up to tell the user that the function associated with the button has
 * not been implemented. Also, be aware that What has been implemented may not work the way the
 * final product requires and there maybe defects in this code.
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
 * @version 2.00		2026-09-16 Updated to implement username and password validation with dynamic updates
 *  
 */

public class ControllerFirstAdmin {
	/*-********************************************************************************************

	The controller attributes for this page
	
	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and 
	the Model is often just a stub, or will be a singleton instantiated object.
	
	*/
	
	/** The admin username as last typed on the page. */
	private static String adminUsername = "";
	/** The first copy of the admin password as last typed. */
	private static String adminPassword1 = "";
	/** The second copy of the admin password, which must match the first. */
	private static String adminPassword2 = "";		
	/** Reference for the in-memory database so this package has access. */
	protected static Database theDatabase = applicationMain.FoundationsMain.database;		

	/*-********************************************************************************************

	The User Interface Actions for this page
	
	*/
	
	/**
	 * Default constructor is not used.
	 */
	public ControllerFirstAdmin() {
	}

	/**********
	 * <p> Method: setAdminUsername() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the username field in the
	 * View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminUsername() {
		adminUsername = ViewFirstAdmin.text_AdminUsername.getText();
	}
	
	
	/**********
	 * <p> Method: setAdminPassword1() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the password 1 field in
	 * the View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminPassword1() {
		adminPassword1 = ViewFirstAdmin.text_AdminPassword1.getText();
		ViewFirstAdmin.label_PasswordsDoNotMatch.setText("");
		
		ViewFirstAdmin.resetAssessments();
		
		// If input is empty, leave them red and return
		if (adminPassword1.isEmpty()) {
			return;
		}

		// Evaluate the password to populate the flags
		PasswordRecognizer.evaluatePassword(adminPassword1);

		// Check flags - turn satisfied items green
		if (PasswordRecognizer.foundUpperCase) {
			ViewFirstAdmin.label_UpperCase.setText("At least one upper case letter - Satisfied");
			ViewFirstAdmin.label_UpperCase.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundLowerCase) {
			ViewFirstAdmin.label_LowerCase.setText("At least one lower case letter - Satisfied");
			ViewFirstAdmin.label_LowerCase.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundNumericDigit) {
			ViewFirstAdmin.label_NumericDigit.setText("At least one numeric digit - Satisfied");
			ViewFirstAdmin.label_NumericDigit.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundSpecialChar) {
			ViewFirstAdmin.label_SpecialChar.setText("At least one special character - Satisfied");
			ViewFirstAdmin.label_SpecialChar.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundLongEnough) {
			ViewFirstAdmin.label_LongEnough.setText("At least eight characters - Satisfied");
			ViewFirstAdmin.label_LongEnough.setTextFill(Color.GREEN);
		}
	}
	
	
	/**********
	 * <p> Method: setAdminPassword2() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the password 2 field in
	 * the View.  A private local copy of what was last entered is kept here.</p>
	 * 
	 */
	protected static void setAdminPassword2() {
		adminPassword2 = ViewFirstAdmin.text_AdminPassword2.getText();		
		ViewFirstAdmin.label_PasswordsDoNotMatch.setText("");
	}
	
	
	/**********
	 * <p> Method: doSetupAdmin() </p>
	 * 
	 * <p> Description: This method is called when the user presses the button to set up the Admin
	 * account.  It start by trying to establish a new user and placing that user into the
	 * database.  If that is successful, we proceed to the UserUpdate page.</p>
	 * 
	 * @param ps the JavaFX Stage the pages are shown on
	 * 
	 * @param r the role code of the account being set up (1 is Admin)
	 */
	protected static void doSetupAdmin(Stage ps, int r) {
		
		// Check that username is valid
		String userErr = UserNameRecognizer.checkForValidUserName(adminUsername);
		if (!userErr.isEmpty()) {
			ViewFirstAdmin.label_PasswordsDoNotMatch.setText(userErr);
			return; // Error occurred with username, stop from continuing
		}

		// Check that password is valid
		String passErr = PasswordRecognizer.evaluatePassword(adminPassword1);
		if (!passErr.isEmpty()) {
			ViewFirstAdmin.label_PasswordsDoNotMatch.setText(passErr);
			return; // Error occurred with password, stop from continuing
		}
		
		
		// Make sure the two passwords are the same
		if (adminPassword1.compareTo(adminPassword2) == 0) {
        	// Create the passwords and proceed to the user home page
        	User user = new User(adminUsername, adminPassword1, "", "", "", "", "", true, false, 
        			false);
            try {
            	// Create a new User object with admin role and register in the database
            	theDatabase.register(user);
            	}
            catch (SQLException e) {
                System.err.println("*** ERROR *** Database error trying to register a user: " + 
                		e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }
            
            // User was established in the database, so navigate to the User Update Page
        	guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewFirstAdmin.theStage, user);
		}
		else {
			// The two passwords are NOT the same, so clear the passwords, explain the passwords
			// must be the same, and clear the message as soon as the first character is typed.
			ViewFirstAdmin.text_AdminPassword1.setText("");
			ViewFirstAdmin.text_AdminPassword2.setText("");
			ViewFirstAdmin.label_PasswordsDoNotMatch.setText(
					"The two passwords must match. Please try again!");
		}
	}
	
	
	/**********
	 * <p> Method: performQuit() </p>
	 * 
	 * <p> Description: This method terminates the execution of the program.  It leaves the
	 * database in a state where the normal login page will be displayed when the application is
	 * restarted.</p>
	 * 
	 */
	protected static void performQuit() {
		System.out.println("Perform Quit");
		System.exit(0);
	}	
}

