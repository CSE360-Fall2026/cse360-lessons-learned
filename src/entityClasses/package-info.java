/*******
 * <p> The entity classes of the application: plain data objects with the operations on that
 * data.  As in the Foundations code, the SQL itself lives in database.Database; these classes
 * delegate to it. </p>
 *
 * <p> <b>TP2 Lessons Learned classes</b> (TP2 Task 3): </p>
 * <ul>
 * <li> LessonsLearned: one lesson learned.  Holds the core information from the Contributor User
 * 		Stories (title, description, tags, category), the lock flags, the attributes reserved for
 * 		the TP3 epics and the AICoaching fields, and full Create, Read, Update, and Delete with
 * 		input validation. </li>
 * <li> LessonsLearnedList: the persistent list of lessons.  Loads all lessons or one
 * 		Contributor's lessons, adds, finds, replaces, and removes lessons, and returns subsets
 * 		(by title, keywords, category, and tag) as new lists. </li>
 * <li> ExperienceEntry: one new attempt to reuse a lesson: what was done, the time/effort in
 * 		hours, and the effort of each team member (US-U2). </li>
 * <li> MemberEffort: one team member's share of an experience entry. </li>
 * </ul>
 *
 * <p> User is the Foundations account entity, unchanged except for its documentation. </p>
 */
package entityClasses;
