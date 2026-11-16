
package guiTools;

import entityClasses.User;

/*******
 * <p> Title: ViewerAccess Class </p>
 *
 * <p> Description: Provides a shared authorization check for
 * the Viewer role in TP3 Aspect 9. Viewer operations must verify
 * that the user has the Viewer role before displaying lessons
 * or performing searches (US-V-01 through US-V-04). </p>
 *
 * <p> This class follows the same authorization pattern as
 * CuratorAccess. Keeping authorization in one location helps
 * prevent inconsistent role checks across Viewer operations. </p>
 *
 * <p> The Viewer role uses the Role3 flag stored in the User
 * object. This flag is loaded from the database during login. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-15 Initial version for TP3 Aspect 9
 */
public class ViewerAccess {

    /*****
     * <p> Method: ViewerAccess() </p>
     *
     * <p> Description: Default constructor. This class provides
     * static authorization methods and does not require instances. </p>
     */
    public ViewerAccess() {
    }

    /*****
     * <p> Method: boolean isViewer(User user) </p>
     *
     * <p> Description: Checks whether the provided user has the
     * Viewer role. A null user or a user without the Viewer role
     * is denied access. This supports the Viewer authorization
     * requirements of US-V-01 through US-V-04. </p>
     *
     * @param user the currently logged-in user; may be null
     *
     * @return true if the user has the Viewer role, false otherwise
     */
    public static boolean isViewer(User user) {

        // A null user cannot be authorized. Viewer access is
        // separate from Admin, Contributor, and Curator roles.
        return user != null && user.getNewRole3();
    }
}
