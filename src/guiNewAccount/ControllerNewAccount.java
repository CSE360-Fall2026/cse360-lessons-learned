package guiNewAccount;

import java.sql.SQLException;

import database.Database;
import entityClasses.User;
import javafx.scene.paint.Color;
import inputValidation.UserNameRecognizer;
import inputValidation.PasswordRecognizer;

/*******
 * <p> Title: ControllerNewAccount Class. </p>
 * 
 * <p> Description: The Java/FX-based New Account Page.  This class provides the controller actions
 * to allow the user to establish a new account after responding to an invitation and the use of a
 * one time code.
 * 
 * The controller deals with the user pressing the "User Step" button widget being click.  If also
 * supports the user click on the "Quit" button widget.
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
 * @version 2.01		2026-11-10 TP3 Aspect 1: a Role2 invitation creates a Curator account
 *  
 */

public class ControllerNewAccount {
	
	/*-********************************************************************************************

	The User Interface Actions for this page
	
	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and 
	the Model is often just a stub, or will be a singleton instantiated object.
	
	*/

	/**
	 * Default constructor is not used.
	 */
	public ControllerNewAccount() {
	}
	
	
	// Reference for the in-memory database so this package has access
	/** Reference for the in-memory database so this package has access. */
	private static Database theDatabase = applicationMain.FoundationsMain.database;
	
	/**********
	 * <p> Method: setNewAccountPassword() </p>
	 * 
	 * <p> Description: This method is called when the user adds text to the password 1 field in
	 * the View.</p>
	 * 
	 */
	protected static void setNewAccountPassword() {
		String pass1 = ViewNewAccount.text_Password1.getText();
		ViewNewAccount.label_PasswordsDoNotMatch.setText("");
		
		ViewNewAccount.resetAssessments();
		
		// If input is empty, leave them red and return
		if (pass1.isEmpty()) {
			return;
		}

		// Evaluate the password to populate the flags
		PasswordRecognizer.evaluatePassword(pass1);

		// Check flags - turn satisfied items green
		if (PasswordRecognizer.foundUpperCase) {
			ViewNewAccount.label_UpperCase.setText("At least one upper case letter - Satisfied");
			ViewNewAccount.label_UpperCase.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundLowerCase) {
			ViewNewAccount.label_LowerCase.setText("At least one lower case letter - Satisfied");
			ViewNewAccount.label_LowerCase.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundNumericDigit) {
			ViewNewAccount.label_NumericDigit.setText("At least one numeric digit - Satisfied");
			ViewNewAccount.label_NumericDigit.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundSpecialChar) {
			ViewNewAccount.label_SpecialChar.setText("At least one special character - Satisfied");
			ViewNewAccount.label_SpecialChar.setTextFill(Color.GREEN);
		}

		if (PasswordRecognizer.foundLongEnough) {
			ViewNewAccount.label_LongEnough.setText("At least eight characters - Satisfied");
			ViewNewAccount.label_LongEnough.setTextFill(Color.GREEN);
		}
	}
	
	/**********
	 * <p> Method: public doCreateUser() </p>
	 * 
	 * <p> Description: This method is called when the user has clicked on the User Setup
	 * button.  This method checks the input fields to see that they are valid.  If so, it then
	 * creates the account by adding information to the database.
	 * 
	 * The method reaches batch to the view page and to fetch the information needed rather than
	 * passing that information as parameters.
	 * 
	 */	
	protected static void doCreateUser() {
		
		// Fetch the username and password. (We use the first of the two here, but we will validate
		// that the two password fields are the same before we do anything with it.)
		String username = ViewNewAccount.text_Username.getText();
		String password = ViewNewAccount.text_Password1.getText();
		
		// Display key information to the log
		System.out.println("** Account for Username: " + username + "; theInvitationCode: "+
				ViewNewAccount.theInvitationCode + "; email address: " + 
				ViewNewAccount.emailAddress + "; Role: " + ViewNewAccount.theRole);
		
		// Initialize local variables that will be created during this process
		int roleCode = 0;
		User user = null;
		
		// Check that username is valid
		String userErr = UserNameRecognizer.checkForValidUserName(username);
		if (!userErr.isEmpty()) {
			ViewNewAccount.label_PasswordsDoNotMatch.setText(userErr);
			return; // Error occurred with username, stop from continuing
		}

		// Check that password is valid
		String passErr = PasswordRecognizer.evaluatePassword(password);
		if (!passErr.isEmpty()) {
			ViewNewAccount.label_PasswordsDoNotMatch.setText(passErr);
			return; // Error occurred with password, stop from continuing
		}

		// Make sure the two passwords are the same.	
		if (ViewNewAccount.text_Password1.getText().
				compareTo(ViewNewAccount.text_Password2.getText()) == 0) {
			
			// The passwords match so we will set up the role and the User object base on the 
			// information provided in the invitation
			if (ViewNewAccount.theRole.compareTo("Admin") == 0) {
				roleCode = 1;
				user = new User(username, password, "", "", "", "", "", true, false, false);
			} else if (ViewNewAccount.theRole.compareTo("Role1") == 0 || ViewNewAccount.theRole.compareTo("Contributor") == 0) {
				roleCode = 2;
				user = new User(username, password, "", "", "", "", "", false, true, false);
			} else if (ViewNewAccount.theRole.compareTo("Role2") == 0 || ViewNewAccount.theRole.compareTo("Curator") == 0) {
				// TP3 Aspect 1: Role2 is the Curator role.  Invitations store "Role2"; "Curator"
				// is accepted as well, the same way "Contributor" is accepted for Role1 above.
				roleCode = 3;
				user = new User(username, password, "", "", "", "", "", false, false, true);
			} else {
				System.out.println(
						"**** Trying to create a New Account for a role that does not exist!");
				System.exit(0);
			}
			
			// Unlike the FirstAdmin, we know the email address, so set that into the user as well.
        	user.setEmailAddress(ViewNewAccount.emailAddress);

        	// Inform the system about which role will be played
			applicationMain.FoundationsMain.activeHomePage = roleCode;
			
        	// Create the account based on user and proceed to the user account update page
            try {
            	// Create a new User object with the pre-set role and register in the database
            	theDatabase.register(user);
            } catch (SQLException e) {
                System.err.println("*** ERROR *** Database error: " + e.getMessage());
                e.printStackTrace();
                System.exit(0);
            }
            
            // The account has been set, so remove the invitation from the system
            theDatabase.removeInvitationAfterUse(
            		ViewNewAccount.text_Invitation.getText());
            
            // Set the database so it has this user and the current user
            theDatabase.getUserAccountDetails(username);

            // Navigate to the Welcome Login Page
            guiUserUpdate.ViewUserUpdate.displayUserUpdate(ViewNewAccount.theStage, user);
		}
		else {
			// The two passwords are NOT the same, so clear the passwords, explain the passwords
			// must be the same, and clear the message as soon as the first character is typed.
			ViewNewAccount.text_Password1.setText("");
			ViewNewAccount.text_Password2.setText("");
			ViewNewAccount.label_PasswordsDoNotMatch.setText(
					"The two passwords must match. Please try again!");
		}
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
