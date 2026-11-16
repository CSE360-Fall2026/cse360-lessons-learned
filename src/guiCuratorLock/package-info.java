/**
 * <p> The Curator Lock page (TP3 Aspect 2, Epic E6): a Curator locks or unlocks a whole lesson
 * or any of its title, description, tags, and category, after confirming.  It is frame 2 of
 * the Initial ConOps storyboard, and the Curator opens it for the lesson selected on the
 * Curator Home Page (package guiCuratorHome).</p>
 *
 * <p> Like every Foundations GUI package, it follows Model-View-Controller: ViewCuratorLock
 * declares and places the widgets, ControllerCuratorLock reacts to the user, and
 * ModelCuratorLock does the data work.  The Model was written first, as the HW3 Task 5
 * risk-reduction prototype; the View and Controller were added in TP3.  Every action checks
 * the Curator role first, using the shared rule in guiTools.CuratorAccess.</p>
 *
 * <p> The Model is tested by curatorLockTests.CuratorLockTests (JUnit, LK-01 to LK-33) and
 * curatorLockTests.CuratorLockPageTests (JUnit, LP-01 to LP-20).  The View and Controller are
 * tested by the manual tests M-CL1 to M-CL6 in Manual Tests.pdf.</p>
 */
package guiCuratorLock;
