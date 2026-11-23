
package viewerTests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import entityClasses.User;
import guiTools.ViewerAccess;

/*******
 * <p> Title: ViewerAccessTests Class </p>
 *
 * <p> Description: Tests Viewer role authorization for TP3
 * Aspect 9. Verifies that only users assigned the Viewer role
 * are recognized as Viewers. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-16 Initial Viewer authorization tests
 */
public class ViewerAccessTests {

    /**********
     * Default constructor.  JUnit creates an instance for each test.
     */
    public ViewerAccessTests() {
    }

    /**********
     * <p> Method: testViewerIsAuthorized() </p>
     *
     * <p> Description: Verifies that a user with the Viewer
     * role is authorized. </p>
     *
     * <p> Validates: ViewerAccess.isViewer(). </p>
     */
    @Test
    public void testViewerIsAuthorized() {

        User user = new User(
                "viewer", "password", "", "", "", "", "",
                false, false, false, true);

        assertTrue(ViewerAccess.isViewer(user));
    }

    /**********
     * <p> Method: testContributorIsNotViewer() </p>
     *
     * <p> Description: Verifies that the Contributor role
     * does not automatically provide Viewer access. </p>
     *
     * <p> Validates: ViewerAccess.isViewer(). </p>
     */
    @Test
    public void testContributorIsNotViewer() {

        User user = new User(
                "contributor", "password", "", "", "", "", "",
                false, true, false, false);

        assertFalse(ViewerAccess.isViewer(user));
    }

    /**********
     * <p> Method: testCuratorIsNotViewer() </p>
     *
     * <p> Description: Verifies that the Curator role
     * does not automatically provide Viewer access. </p>
     *
     * <p> Validates: ViewerAccess.isViewer(). </p>
     */
    @Test
    public void testCuratorIsNotViewer() {

        User user = new User(
                "curator", "password", "", "", "", "", "",
                false, false, true, false);

        assertFalse(ViewerAccess.isViewer(user));
    }

    /**********
     * <p> Method: testNullUserIsDenied() </p>
     *
     * <p> Description: Verifies that a null user cannot
     * obtain Viewer access. </p>
     *
     * <p> Validates: ViewerAccess.isViewer(). </p>
     */
    @Test
    public void testNullUserIsDenied() {

        assertFalse(ViewerAccess.isViewer(null));
    }

    /**********
     * <p> Method: testViewerRoleCanBeRemoved() </p>
     *
     * <p> Description: Verifies that removing the Viewer
     * flag from a user denies Viewer authorization. </p>
     *
     * <p> Validates: ViewerAccess.isViewer(). </p>
     */
    @Test
    public void testViewerRoleCanBeRemoved() {

        User user = new User(
                "viewer", "password", "", "", "", "", "",
                false, false, false, true);

        assertTrue(ViewerAccess.isViewer(user));

        user.setRole3User(false);

        assertFalse(ViewerAccess.isViewer(user));
    }
}
