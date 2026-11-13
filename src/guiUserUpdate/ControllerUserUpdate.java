package guiUserUpdate;

import entityClasses.User;
import javafx.stage.Stage;

/**********
 * <p> Title: ControllerUserUpdate Class</p>
 *
 * <p> Description: This static class supports the actions initiated by the ViewUserUpdate
 * class.</p>
 *
 * @version 1.01		2026-10-14 TP2: home page 2 is now the Contributor Home Page
 * @version 1.02		2026-11-10 TP3 Aspect 1: home page 3 is now the Curator Home Page
 */
public class ControllerUserUpdate {

	/**********
	 * Default constructor is not used; every method is static.
	 */
	public ControllerUserUpdate() {
	}
	/*-********************************************************************************************

	The Controller for ViewUserUpdate

	**********************************************************************************************/

	/**********
	 * <p> Title: ControllerUserUpdate Class</p>
	 *
	 * <p> Description: This static class supports the actions initiated by the ViewUserUpdate
	 * class. In this case, there is just one method, no constructors, and no attributes.</p>
	 *
	 * @version 1.01		2026-10-14 TP2: home page 2 is now the Contributor Home Page
	 * @version 1.02		2026-11-10 TP3 Aspect 1: home page 3 is now the Curator Home Page
	 */

	/*-********************************************************************************************

	The User Interface Actions for this page

	**********************************************************************************************/


	/**********
	 * <p> Method: public goToUserHomePage(Stage theStage, User theUser) </p>
	 *
	 * <p> Description: This method is called when the user has clicked on the button to
	 * proceed to the user's home page.
	 *
	 * @param theStage specifies the JavaFX Stage for next next GUI page and it's methods
	 *
	 * @param theUser specifies the user so we go to the right page and so the right information
	 */
	protected static void goToUserHomePage(Stage theStage, User theUser) {

		// Get the roles the user selected during login
		int theRole = applicationMain.FoundationsMain.activeHomePage;

		// Use that role to proceed to that role's home page
		switch (theRole) {
		case 1:
			guiAdminHome.ViewAdminHome.displayAdminHome(theStage, theUser);
			break;
		case 2:
			// TP2: home page 2 (the Role1 flag) is the Contributor Home Page
			guiContributorHome.ViewContributorHome.displayContributorHome(theStage, theUser);
			break;
		case 3:
			// TP3: home page 3 (the Role2 flag) is the Curator Home Page
			guiCuratorHome.ViewCuratorHome.displayCuratorHome(theStage, theUser);
			break;
		default:
			System.out.println("*** ERROR *** UserUpdate goToUserHome has an invalid role: " +
					theRole);
			System.exit(0);
		}
 	}
}
