package guiTools;

import entityClasses.User;

/*******
 * <p> Title: CuratorAccess Class </p>
 *
 * <p> Description: The single rule that says who is a Curator, shared by every Curator page.
 * TP3 Aspect 1 requires that "every Curator action checks the role first" (TP3 Aspects.pdf;
 * Initial ConOps objective OB-4), and the Curator epics E2 to E6 each begin with the action
 * "System checks the Curator role" (Epics.pdf).  Each Curator Model therefore calls
 * isCurator() as its first step.</p>
 *
 * <p> <b>Why one shared class.</b>  This follows LessonAccess, which holds the Contributor
 * ownership rule for the same reason: five Curator features written by different team members
 * need the rule, and writing it once means it cannot be forgotten, or written slightly
 * differently, in one of them.  If the way the role is stored ever changes, only this class
 * changes.</p>
 *
 * <p> <b>Why the rule reads the User object.</b>  A user is a Curator when the Foundations Role2
 * flag is set (User.getNewRole2()), because the Curator role replaces the unused Role2 stub
 * (Initial ConOps assumption A1; RoleLabels).  The flag is the one the login page loaded from
 * the database for this session, the same way the Foundations code decides who is an Admin.  A
 * role an Admin gives or removes therefore takes effect the next time that user logs in, and
 * Database.loginRole2() checks the stored flag again at that login.</p>
 *
 * <p> Each Curator Model keeps its own refusal message next to its own action (for example
 * ModelCuratorHome.NOT_CURATOR and ModelCuratorLock.NOT_CURATOR), so the message can name what
 * was refused.</p>
 *
 * <p> Tested by curatorHomeTests.CuratorHomeTests, CH-05 to CH-07 (JUnit), and indirectly by
 * every refusal test of the Curator Models (CH-18, CH-19, CH-28, CH-31, CH-35, and
 * curatorLockTests LK-02 to LK-04). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-10 Initial version for TP3 Aspect 1
 */
public class CuratorAccess {

	/*****
	 * <p> Method: CuratorAccess() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every member of
	 * this class is static. </p>
	 */
	public CuratorAccess() {
	}


	/*****
	 * <p> Method: boolean isCurator(User user) </p>
	 *
	 * <p> Description: Is this user a Curator?  This is step 1 of every Curator action
	 * (US-CH-V; OB-4). </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-05 (positive), CH-06, and CH-07 (negative). </p>
	 *
	 * @param user the logged-in user; may be null
	 *
	 * @return true only if there is a user and that user has the Curator role
	 */
	public static boolean isCurator(User user) {
		// Null is answered "no" rather than crashing (CWE-476).  An Admin is NOT a Curator
		// unless the Curator role was also given: the roles are separate flags, so an Admin
		// account cannot read every Contributor's lessons just by being an Admin (CH-06).
		return user != null && user.getNewRole2();
	}
}
