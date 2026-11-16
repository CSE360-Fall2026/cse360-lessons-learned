/*******
 * <p> The Contributor Home Page (TP2 Task 4): the page a Contributor sees after logging in.  It
 * lists the lessons the Contributor has added (US-R1), searches their titles (US-R2, US-R2-V),
 * shows only the Contributor's own lessons (US-R4), and starts every other Contributor action:
 * New Lesson opens guiCreateLesson, and Open Lesson (or a double-click) opens guiLessonDetails
 * (US-R3). </p>
 *
 * <p> Like every Foundations GUI package, it follows Model-View-Controller: ViewContributorHome
 * declares and places the widgets, ControllerContributorHome reacts to the user, and
 * ModelContributorHome does the data work through the entity classes.  The package replaces the
 * Foundations Role1 home page; the role is still stored as Role1 (design decision D12). </p>
 */
package guiContributorHome;
