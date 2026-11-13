package guiAdminHome;

import database.Database;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import java.util.Optional;
import java.util.ArrayList;
import guiTools.RoleLabels;
import inputValidation.EmailAddressRecognizer;

/*******
 * <p> Title: GUIAdminHomePage Class. </p>
 *
 * <p> Description: The Java/FX-based Admin Home Page.  This class provides the controller actions
 * basic on the user's use of the JavaFX GUI widgets defined by the View class.
 *
 * This page contains a number of buttons that have not yet been implemented.  WHen those buttons
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
 * @version 1.01		2025-09-16 Update Javadoc documentation *
 * @version 2.00		2026-09-18 Shows list of use; email validation from EmailAddressRecognizer; delete a user; one-time password
 * @version 2.01		2026-10-16 TP2: Role1 is shown as "Contributor" in the invitation message
 * 							and the user list, and stored as "Role1" (design decision D12;
 * 							see guiTools.RoleLabels)
 */

public class ControllerAdminHome {

	/*-*******************************************************************************************

	User Interface Actions for this page

	This controller is not a class that gets instantiated.  Rather, it is a collection of protected
	static methods that can be called by the View (which is a singleton instantiated object) and
	the Model is often just a stub, or will be a singleton instantiated object.

	*/

	/**
	 * Default constructor is not used.
	 */
	public ControllerAdminHome() {
	}

	/** Reference for the in-memory database so this package has access. */
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/**********
	 * <p>
	 *
	 * Title: performInvitation () Method. </p>
	 *
	 * <p> Description: Protected method to send an email inviting a potential user to establish
	 * an account and a specific role.  The role list shows "Contributor" for Role1 (D12) and
	 * "Curator" for Role2 (TP3 Aspect 1); the chosen label is turned back into the stored role
	 * name before the invitation is saved, because ControllerNewAccount expects "Role1" or
	 * "Role2" when the account is created. </p>
	 */
	protected static void performInvitation () {
		// Verify that the email address is valid - If not alert the user and return
		String emailAddress = ViewAdminHome.text_InvitationEmailAddress.getText();
		if (invalidEmailAddress(emailAddress)) {
			return;
		}

		// Check to ensure that we are not sending a second message with a new invitation code to
		// the same email address.
		if (theDatabase.emailaddressHasBeenUsed(emailAddress)) {
			ViewAdminHome.alertEmailError.setContentText(
					"An invitation has already been sent to this email address.");
			ViewAdminHome.alertEmailError.showAndWait();
			return;
		}

		// Inform the user that the invitation has been sent and display the invitation code
		String theSelectedRole = (String) ViewAdminHome.combobox_SelectRole.getValue();
		// TP2 (D12): store "Role1" even though the Admin chose "Contributor"; TP3 (Aspect 1):
		// store "Role2" even though the Admin chose "Curator"
		String invitationCode = theDatabase.generateInvitationCode(emailAddress,
				RoleLabels.toStoredName(theSelectedRole));
		String msg = "Code: " + invitationCode + " for role " + theSelectedRole +
				" was sent to: " + emailAddress;
		System.out.println(msg);
		ViewAdminHome.alertEmailSent.setContentText(msg);
		ViewAdminHome.alertEmailSent.showAndWait();

		// Update the Admin Home pages status
		ViewAdminHome.text_InvitationEmailAddress.setText("");
		ViewAdminHome.label_NumberOfInvitations.setText("Number of outstanding invitations: " +
				theDatabase.getNumberOfInvitations());
	}

	/**********
	 * <p>
	 *
	 * Title: manageInvitations () Method. </p>
	 *
	 * <p> Description: Protected method that is currently a stub informing the user that
	 * this function has not yet been implemented. </p>
	 */
	protected static void manageInvitations () {
		System.out.println("\n*** WARNING ***: Manage Invitations Not Yet Implemented");
		ViewAdminHome.alertNotImplemented.setTitle("*** WARNING ***");
		ViewAdminHome.alertNotImplemented.setHeaderText("Manage Invitations Issue");
		ViewAdminHome.alertNotImplemented.setContentText("Manage Invitations Not Yet Implemented");
		ViewAdminHome.alertNotImplemented.showAndWait();
	}

	/**********
	 * <p>
	 *
	 * Title: setOnetimePassword () Method. </p>
	 *
	 * <p> Description: Protected method that is currently a stub informing the user that
	 * this function has not yet been implemented. </p>
	 */
	protected static void setOnetimePassword () {
		// Fetch list from database
		List<String> userList = theDatabase.getUserList();

				if (userList == null || userList.size() <= 1) {
					ViewAdminHome.alertNotImplemented.setHeaderText("No Users");
					ViewAdminHome.alertNotImplemented.setContentText("There are no registered users in the database.");
					ViewAdminHome.alertNotImplemented.showAndWait();
					return;
				}

				// Create new list without the "<Select a User>"
				List<String> users = new ArrayList<String>();
				for (int i = 1; i < userList.size(); i++) {
					users.add(userList.get(i));
				}

				ChoiceDialog<String> selectUser = new ChoiceDialog<String>("", users);
				selectUser.setTitle("One-Time Password");
				selectUser.setHeaderText("Generate One-Time Password");
				selectUser.setContentText("Select a user:");

				Optional<String> chosenUser = selectUser.showAndWait();

				if (chosenUser.isPresent()) {
					String selectedUser = chosenUser.get();
					String otp = theDatabase.generateOneTimePassword(selectedUser);

					// Message to user
					String msg = "One-Time Password: " + otp + " was generated for user: " + selectedUser + ".";
					System.out.println(msg);

					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("One-Time Password Generated");
					alert.setHeaderText("One-Time Password for " + selectedUser);
					alert.setContentText(msg);
					alert.showAndWait();
				}
			}

	/**********
	 * <p>
	 *
	 * Title: deleteUser () Method. </p>
	 *
	 * <p> Description: Protected method that is currently a stub informing the user that
	 * this function has not yet been implemented. </p>
	 */
	protected static void deleteUser() {
		// Fetch list from database
		List<String> userList = theDatabase.getUserList();
		ViewAdminHome.alertNotImplemented.setTitle("Delete User");

				// Check if userList is null or empty (this starts at 1 due to "<Select a User>")
				if (userList == null || userList.size() <= 1) {
					ViewAdminHome.alertNotImplemented.setHeaderText("No Users Available");
					ViewAdminHome.alertNotImplemented.setContentText("There are no user accounts in the database.");
					ViewAdminHome.alertNotImplemented.showAndWait();
					return;
				}

				// DO NOT SHOW CURRENT ADMIN USER IN LIST
				List<String> safeUsers = new ArrayList<String>();	// create a new list
				// Add users that are not the current admin user to the list
				for (int i = 1; i < userList.size(); i++) {
					String user = userList.get(i);
					if (!user.equals(ViewAdminHome.theUser.getUserName())) {
						safeUsers.add(user);
					}
				}

				// Check if new list created from database (without admin) is empty
				if (safeUsers.isEmpty()) {
					ViewAdminHome.alertNotImplemented.setHeaderText("No Users Available");
					ViewAdminHome.alertNotImplemented.setContentText("There are no user accounts in the database.");
					ViewAdminHome.alertNotImplemented.showAndWait();
					return;
				}

				// Show dropdown selection for users
				ChoiceDialog<String> selectUser = new ChoiceDialog<String>("", safeUsers);
				selectUser.setHeaderText("Select a user to permanently delete");
				selectUser.setContentText("User:");

				Optional<String> chosenUser = selectUser.showAndWait();

				// Confirmation to delete user
				if (chosenUser.isPresent()) {
					String targetUser = chosenUser.get();	// Selected user to delete

					Alert confirmDelete = new Alert(Alert.AlertType.CONFIRMATION);
					confirmDelete.setTitle("Confirm Deletion");
					confirmDelete.setHeaderText("Delete user: " + targetUser + "?");
					confirmDelete.setContentText("Are you sure you want to permanently delete this user? This cannot be undone.");

					Optional<ButtonType> confirm = confirmDelete.showAndWait();

					if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
						theDatabase.deleteUser(targetUser);

						// Update user count on the admin home page
						ViewAdminHome.label_NumberOfUsers.setText("Number of users: " + theDatabase.getNumberOfUsers());

						// Success message
						ViewAdminHome.alertNotImplemented.setTitle("Delete User");
						ViewAdminHome.alertNotImplemented.setHeaderText("User Successfully Deleted");
						ViewAdminHome.alertNotImplemented.setContentText("User '" + targetUser + "' has been removed from the database.");
						ViewAdminHome.alertNotImplemented.showAndWait();
					}
				}
			}

	/**********
	 * <p>
	 *
	 * Title: listUsers () Method. </p>
	 *
	 * <p> Description: Protected method that is currently a stub informing the user that
	 * this function has not yet been implemented. </p>
	 */
	protected static void listUsers() {
		// Fetch list from database
		List<String> userList = theDatabase.getUserList();
		String infoText = "";

		if (userList != null) {
			// Skip index 0 because it starts as "<Select a User>"
			for (int i = 1; i < userList.size(); i++) {
				String username = userList.get(i);
				theDatabase.getUserAccountDetails(username);

				// Name set up
				String first = theDatabase.getCurrentFirstName();
				String last = theDatabase.getCurrentLastName();
				String fullName = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
				if (fullName.isEmpty()) {
					fullName = "<none>";
				}

				// Email set up
				String email = theDatabase.getCurrentEmailAddress();
				if (email == null || email.isEmpty()) {
					email = "<none>";
				}

				// Roles set up (TP2, D12: the Role1 flag is shown as Contributor; TP3 Aspect 1:
				// the Role2 flag is shown as Curator)
				String roles = "";
				if (theDatabase.getCurrentAdminRole()) roles += "Admin, ";
				if (theDatabase.getCurrentNewRole1()) roles += RoleLabels.ROLE1_LABEL + ", ";
				if (theDatabase.getCurrentNewRole2()) roles += RoleLabels.ROLE2_LABEL + ", ";

				if (roles.endsWith(", ")) {
					roles = roles.substring(0, roles.length() - 2);
				}
				if (roles.isEmpty()) {
					roles = "<none>";
				}

				// Output to the modal
				infoText += i + ") Username: " + username + "  |  Name: " + fullName + "  |  Email: " + email + "  |  Roles: " + roles + "\n";
			}
		}

		if (infoText.isEmpty()) {
			infoText = "No users found in the system.";
		}

		ViewAdminHome.alertNotImplemented.setTitle("User Directory");
		ViewAdminHome.alertNotImplemented.setHeaderText("Total List of Registered Users: (" + theDatabase.getNumberOfUsers() + ")");
		ViewAdminHome.alertNotImplemented.setContentText(infoText.trim());

		// Pop-up modal resize to fit content
		ViewAdminHome.alertNotImplemented.setResizable(true);
		ViewAdminHome.alertNotImplemented.getDialogPane().setPrefSize(580, javafx.scene.layout.Region.USE_COMPUTED_SIZE);

		ViewAdminHome.alertNotImplemented.showAndWait();
	}

	/**********
	 * <p>
	 *
	 * Title: addRemoveRoles () Method. </p>
	 *
	 * <p> Description: Protected method that allows an admin to add and remove roles for any of
	 * the users currently in the system.  This is done by invoking the AddRemoveRoles Page. There
	 * is no need to specify the home page for the return as this can only be initiated by and
	 * Admin.</p>
	 */
	protected static void addRemoveRoles() {
		guiAddRemoveRoles.ViewAddRemoveRoles.displayAddRemoveRoles(ViewAdminHome.theStage,
				ViewAdminHome.theUser);
	}

	/**********
	 * <p>
	 *
	 * Title: invalidEmailAddress () Method. </p>
	 *
	 * <p> Description: Protected method that is intended to check an email address before it is
	 * used to reduce errors.  The code currently only checks to see that the email address is not
	 * empty.  In the future, a syntactic check must be performed and maybe there is a way to check
	 * if a properly email address is active.</p>
	 *
	 * @param emailAddress	This String holds what is expected to be an email address
	 * 
	 * @return true if the address is invalid (an alert has been shown), false if it is valid
	 */
	protected static boolean invalidEmailAddress(String emailAddress) {
		// Check that the email address is valid
		String err = EmailAddressRecognizer.checkEmailAddress(emailAddress.trim());
		if (!err.isEmpty()) {
			ViewAdminHome.alertEmailError.setTitle("Email Error");
			ViewAdminHome.alertEmailError.setHeaderText("Invalid Email Address");
			ViewAdminHome.alertEmailError.setContentText(err);
			ViewAdminHome.alertEmailError.showAndWait();
			return true;
		}
		return false;
	}

	/**********
	 * <p>
	 *
	 * Title: performLogout () Method. </p>
	 *
	 * <p> Description: Protected method that logs this user out of the system and returns to the
	 * login page for future use.</p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewAdminHome.theStage);
	}

	/**********
	 * <p>
	 *
	 * Title: performQuit () Method. </p>
	 *
	 * <p> Description: Protected method that gracefully terminates the execution of the program.
	 * </p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}
}
