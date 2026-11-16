/*******
 * <p> The Curator Home Page (TP3 Aspect 1; Epics.pdf dependency D1): the page a Curator sees
 * after logging in.  It lists every lesson learned from every Contributor (US-CH1), searches
 * them by keyword (US-CH2, US-CH2-V), shows the selected lesson in full (US-CH3), and refuses
 * anyone who is not a Curator (US-CH-V).  It is the starting point for the Curator actions of
 * Epics E2 to E6, each of which works on the lesson selected here. </p>
 *
 * <p> Like every Foundations GUI package, it follows Model-View-Controller: ViewCuratorHome
 * declares and places the widgets, ControllerCuratorHome reacts to the user, and
 * ModelCuratorHome does the data work through the entity classes.  The package replaces the
 * Foundations Role2 stub (package guiRole2); the role is still stored as Role2 and shown as
 * "Curator" (guiTools.RoleLabels). </p>
 *
 * <p> The Model is tested by curatorHomeTests.CuratorHomeTests (JUnit, CH-16 to CH-36).  The
 * View and Controller are tested by the manual tests M-CH1 to M-CH6 in Manual Tests.pdf,
 * because what a JavaFX page shows can only be judged by looking at it. </p>
 */
package guiCuratorHome;
