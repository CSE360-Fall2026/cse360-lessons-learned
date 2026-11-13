package guiCuratorLock;

import java.util.ArrayList;
import java.util.List;

import applicationMain.FoundationsMain;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;
import guiTools.LessonResult;

/*******
 * <p> Title: ModelCuratorLock Class. </p>
 *
 * <p> Description: The Model for the Curator's Lock/Unlock action, the HW3 Task 5
 * risk-reduction prototype of TP3 Aspect 2, locking lessons and fields (Epic E6: "As a
 * lessons-learned curator, I can lock entire lessons or specific fields of a lesson so the
 * contributor can't make changes to fields or delete the lesson").  The future Curator Lock page
 * (frame 2 of the Initial ConOps storyboard) will call changeLocks(), exactly as each TP2
 * Contributor page calls its own Model.</p>
 *
 * <p> <b>What it adds to TP2.</b>  TP2 already stores the five lock flags and the Contributor
 * Models already refuse to edit a locked field or delete a locked lesson.  What did not exist
 * was a way for a Curator to set the flags: a Curator role check, a check of the chosen fields,
 * a confirmation step, and a single database write for all the chosen flags.  This class adds
 * those, and nothing in the TP2 Contributor code had to change.</p>
 *
 * <p> <b>What TP3 adds for the page.</b>  The Curator Lock page (ViewCuratorLock and
 * ControllerCuratorLock) needs three more things that are worth testing without a screen, so
 * they live here and not in the Controller: lessonToLock() reads the lesson the page is opened
 * for, chosenFields() turns the ticked boxes into the field names, and confirmationQuestion()
 * words the "Are you sure?" question.</p>
 *
 * <p> <b>The Curator role.</b>  A user is a Curator when the Role2 flag is set, because TP3
 * Aspects.pdf replaces the unused Role2 stub with the Curator role (Initial ConOps, assumption
 * A1).  The rule itself lives in guiTools.CuratorAccess, which every Curator Model shares.</p>
 *
 * <p> <b>Results.</b>  Like the TP2 Models, every outcome is a guiTools.LessonResult, so the
 * page can show getMessage() directly.  The messages are the ones listed in TP3 Testing
 * Requirements.pdf, section III.</p>
 *
 * <p> Tested by curatorLockTests.CuratorLockTests (JUnit, LK-01 to LK-33) for changeLocks(),
 * and by curatorLockTests.CuratorLockPageTests (JUnit, LP-01 to LP-20) for lessonToLock(),
 * chosenFields(), confirmationQuestion(), and the order in which the page calls this
 * Model.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-01 HW3 Task 5: prototype written to pass CuratorLockTests
 * @version 1.01		2026-11-10 TP3 Aspect 1: the Curator check uses the shared CuratorAccess
 * @version 1.02		2026-11-13 TP3 Aspect 2: lessonToLock(), chosenFields(),
 * 							confirmationQuestion(), and NO_CHANGE added for the Curator Lock page
 */
public class ModelCuratorLock {

	/** Shown when the acting user is not a Curator (requirement TR-1). */
	public static final String NOT_CURATOR = "Only a curator can lock or unlock lessons.";

	/** Shown when the database write fails (requirement TR-7; ConOps scenario S5). */
	public static final String NOT_SAVED = "The lock could not be saved. Please try again.";

	/** Shown when the Curator answers No to the confirmation (US-CL-V; ConOps scenario S4). */
	public static final String NO_CHANGE = "No change was made.";

	/** The five lockable names, in the fixed order used in every message (requirement TR-8). */
	private static final String[] FIELD_ORDER = { LessonsLearned.LOCK_ENTIRE,
			LessonsLearned.FIELD_TITLE, LessonsLearned.FIELD_DESCRIPTION,
			LessonsLearned.FIELD_TAGS, LessonsLearned.FIELD_CATEGORY };

	/**********
	 * <p> Method: ModelCuratorLock() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every member of
	 * this class is static. </p>
	 */
	public ModelCuratorLock() {
	}


	/**********
	 * <p> Method: LessonResult changeLocks(User actingUser, int lessonId,
	 * 		List&lt;String&gt; fields, boolean lock, boolean confirmed) </p>
	 *
	 * <p> Description: Locks or unlocks the chosen parts of one lesson for a Curator.  Order of
	 * checks:
	 * <br> 1. The acting user is a Curator (TR-1).  This comes FIRST, so someone who is not a
	 * Curator cannot even learn whether a lesson exists.
	 * <br> 2. The lesson exists (TR-2).  A Curator may lock any Contributor's lesson.
	 * <br> 3. The field list is not empty and every name is one of the five lockable names
	 * (TR-3).  The whole list is checked before anything is saved.
	 * <br> 4. The Curator confirmed (TR-4).
	 * <br> 5. All the chosen flags are saved in one database write (TR-7).</p>
	 *
	 * <p> The page relies on the same convention as the TP2 delete action
	 * (ModelLessonDetails.deleteLesson).  It first calls with confirmed = false.  A non-empty
	 * message means the request is refused and the message is shown.  An EMPTY message means
	 * "this request is valid, waiting for Yes", so the page shows its "Are you sure?" dialog
	 * and, only on an explicit Yes, calls again with confirmed = true.</p>
	 *
	 * @param actingUser	the logged-in user; may be null
	 *
	 * @param lessonId		the lesson to change
	 *
	 * @param fields		any of "entire", "title", "description", "tags", and "category";
	 * 						repeated names count once; may be null
	 *
	 * @param lock			true to lock the chosen parts, false to unlock them
	 *
	 * @param confirmed		true only if the Curator answered "Yes"
	 *
	 * @return				on success "Locked: ..." or "Unlocked: ..." naming the changed parts
	 * 						in a fixed order.  If confirmed is false and the request is valid, a
	 * 						failed result with an EMPTY message.  Otherwise the specific refusal
	 * 						message.
	 */
	public static LessonResult changeLocks(User actingUser, int lessonId, List<String> fields,
			boolean lock, boolean confirmed) {

		// Step 1 (TR-1): only a Curator.  Checked before the lesson so a non-Curator learns
		// nothing about which lesson ids exist (CWE-200), and null-safe (CWE-476).
		if (!CuratorAccess.isCurator(actingUser))
			return LessonResult.failed(NOT_CURATOR, "");

		// Step 2 (TR-2): the lesson exists.  No ownership check: ownership is a Contributor
		// rule, and a Curator reviews every Contributor's lessons.
		if (LessonsLearned.read(lessonId) == null)
			return LessonResult.failed(LessonAccess.NOT_FOUND, "");

		// Step 3 (TR-3): a non-empty list of known names.  Every name is checked before
		// anything is written, so one bad name means nothing is saved.
		String action = lock ? "lock" : "unlock";
		if (fields == null || fields.isEmpty())
			return LessonResult.failed(
					"Select the whole lesson or at least one field to " + action + ".", "");
		for (String field : fields)
			if (!isLockableName(field))
				return LessonResult.failed("Unknown field: " + field
						+ ". Choose entire, title, description, tags, or category.",
						String.valueOf(field));

		// Step 4 (TR-4): nothing changes until the Curator answers Yes.  The empty message
		// tells the page to ask, following the TP2 delete convention.
		if (!confirmed) return LessonResult.failed("", "");

		// Step 5 (TR-7): one write for every chosen flag, so the lesson is never half-locked
		List<String> chosen = inFixedOrder(fields);
		if (!FoundationsMain.database.setLessonLocks(lessonId, chosen, lock))
			return LessonResult.failed(NOT_SAVED, "");

		// TR-8: tell the Curator exactly what changed.  The lesson is read back so the page
		// can show its stored state.
		return LessonResult.passed((lock ? "Locked: " : "Unlocked: ") + describe(chosen) + ".",
				lessonId, LessonsLearned.read(lessonId));
	}


	/**********
	 * <p> Method: LessonResult lessonToLock(User actingUser, int lessonId) </p>
	 *
	 * <p> Description: Reads the lesson the Curator Lock page is opened for, so the page can
	 * show its title, its owner, and what is locked now (US-CL1).  The checks are the first
	 * two checks of changeLocks(), in the same order: the acting user is a Curator, then the
	 * lesson exists. </p>
	 *
	 * <p> Tested by CuratorLockPageTests LP-18 (positive), LP-19, and LP-20 (negative). </p>
	 *
	 * @param actingUser	the logged-in user; may be null
	 *
	 * @param lessonId		the lesson the page is opened for
	 *
	 * @return				on success the lesson with its stored lock flags; otherwise
	 * 						NOT_CURATOR or "Lesson not found.", with no lesson
	 */
	public static LessonResult lessonToLock(User actingUser, int lessonId) {
		// The page has its own read, with this Model's own refusal message, rather than
		// borrowing the Curator Home Page's "Only a curator can view all lessons.", which
		// would be the wrong words on a page about locking.  Role first, as in changeLocks(),
		// so a refusal says nothing about which lessons exist (CWE-200).
		if (!CuratorAccess.isCurator(actingUser))
			return LessonResult.failed(NOT_CURATOR, "");

		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");

		return LessonResult.passed("", lessonId, lesson);
	}


	/**********
	 * <p> Method: List&lt;String&gt; chosenFields(boolean entire, boolean title,
	 * 		boolean description, boolean tags, boolean category) </p>
	 *
	 * <p> Description: Turns the five check boxes of the Curator Lock page into the list of
	 * field names changeLocks() expects (US-CL1). </p>
	 *
	 * <p> Tested by CuratorLockPageTests LP-01, LP-02 (positive), and LP-03 (negative). </p>
	 *
	 * @param entire		true if "Entire lesson" is ticked
	 *
	 * @param title			true if "Title" is ticked
	 *
	 * @param description	true if "Description" is ticked
	 *
	 * @param tags			true if "Tags" is ticked
	 *
	 * @param category		true if "Category" is ticked
	 *
	 * @return				the ticked names in the fixed order entire, title, description,
	 * 						tags, category; an empty list, never null, if nothing is ticked
	 */
	public static List<String> chosenFields(boolean entire, boolean title, boolean description,
			boolean tags, boolean category) {
		// The page passes five booleans rather than building this list itself, so the names
		// and their order are decided in one tested place.  A Controller that typed "Title"
		// for one box would otherwise only fail on the screen.
		boolean[] ticked = { entire, title, description, tags, category };
		List<String> chosen = new ArrayList<String>();
		for (int i = 0; i < FIELD_ORDER.length; i++)
			if (ticked[i]) chosen.add(FIELD_ORDER[i]);
		return chosen;
	}


	/**********
	 * <p> Method: String confirmationQuestion(String lessonTitle, List&lt;String&gt; fields,
	 * 		boolean lock) </p>
	 *
	 * <p> Description: Words the question the page asks before anything is saved (Initial
	 * ConOps objective OB-6), for example "Lock title and tags of 'Set up CI'?", which is the
	 * question in step 3 of scenario S1.  The question names the action, each chosen part once
	 * in the fixed order, and the lesson, so the Curator confirms exactly what will change and
	 * the "Locked: ..." message that follows names the same parts. </p>
	 *
	 * <p> Tested by CuratorLockPageTests LP-04 to LP-10 (positive), LP-11 (negative), and in
	 * the flow tests LP-12, LP-13, and LP-17. </p>
	 *
	 * @param lessonTitle	the title of the lesson being changed
	 *
	 * @param fields		the chosen names, in any order; may be null
	 *
	 * @param lock			true for locking, false for unlocking
	 *
	 * @return				the question, or an empty String if no lockable name was chosen,
	 * 						because then there is nothing to ask about
	 */
	public static String confirmationQuestion(String lessonTitle, List<String> fields,
			boolean lock) {
		// Null is treated as "nothing chosen" rather than crashing (CWE-476).  Unknown names
		// are dropped here; changeLocks() is what refuses them, with its own message.
		List<String> chosen = (fields == null) ? new ArrayList<String>() : inFixedOrder(fields);
		if (chosen.isEmpty()) return "";

		String action = lock ? "Lock" : "Unlock";
		String lesson = "'" + lessonTitle + "'";

		// The whole lesson alone is a statement about the lesson itself; "the entire lesson of
		// 'Set up CI'" would read as if the lesson belonged to something else
		if (chosen.size() == 1 && chosen.get(0).equals(LessonsLearned.LOCK_ENTIRE))
			return action + " the entire lesson " + lesson + "?";

		List<String> words = new ArrayList<String>();
		for (String name : chosen)
			words.add(name.equals(LessonsLearned.LOCK_ENTIRE) ? "the entire lesson" : name);
		return action + " " + joinWithAnd(words) + " of " + lesson + "?";
	}


	/**********
	 * <p> Method: String joinWithAnd(List&lt;String&gt; words) </p>
	 *
	 * <p> Description: Joins words the way a sentence lists them: "title", "title and tags",
	 * or "title, description, and tags". </p>
	 *
	 * @param words	one or more words
	 *
	 * @return		the words as one phrase
	 */
	private static String joinWithAnd(List<String> words) {
		// A question is read as a sentence, so "title, tags" (the form used after "Locked:")
		// would look like a list that was cut short
		int n = words.size();
		if (n == 1) return words.get(0);
		if (n == 2) return words.get(0) + " and " + words.get(1);
		return String.join(", ", words.subList(0, n - 1)) + ", and " + words.get(n - 1);
	}


	/**********
	 * <p> Method: boolean isLockableName(String name) </p>
	 *
	 * <p> Description: Is this one of the five lockable names? </p>
	 *
	 * @param name	the name to check; may be null
	 *
	 * @return		true only for "entire", "title", "description", "tags", or "category"
	 */
	private static boolean isLockableName(String name) {
		// An exact, case-sensitive match, because the TP2 lock methods are case-sensitive:
		// accepting "Title" here would lock nothing (test LK-13)
		for (String lockable : FIELD_ORDER)
			if (lockable.equals(name)) return true;
		return false;
	}


	/**********
	 * <p> Method: List&lt;String&gt; inFixedOrder(List&lt;String&gt; fields) </p>
	 *
	 * <p> Description: Returns the chosen names once each, in the fixed order entire, title,
	 * description, tags, category. </p>
	 *
	 * @param fields	the names as the Curator chose them, possibly repeated or out of order
	 *
	 * @return			the same names, each once, in the fixed order
	 */
	private static List<String> inFixedOrder(List<String> fields) {
		// Walking the fixed list (not the caller's list) removes repeats and fixes the order in
		// one pass, so the message reads the same however the boxes were ticked (TR-8)
		List<String> ordered = new ArrayList<String>();
		for (String lockable : FIELD_ORDER)
			if (fields.contains(lockable)) ordered.add(lockable);
		return ordered;
	}


	/**********
	 * <p> Method: String describe(List&lt;String&gt; names) </p>
	 *
	 * <p> Description: Turns the chosen names into the words the Curator sees, for example
	 * "entire lesson, title". </p>
	 *
	 * @param names	the chosen names, already in the fixed order
	 *
	 * @return		the names joined with ", ", with "entire" shown as "entire lesson"
	 */
	private static String describe(List<String> names) {
		// "Locked: entire." would be unclear, so the whole-lesson name is spelled out, matching
		// the TP2 banner "A curator has locked this whole lesson."
		List<String> words = new ArrayList<String>();
		for (String name : names)
			words.add(name.equals(LessonsLearned.LOCK_ENTIRE) ? "entire lesson" : name);
		return String.join(", ", words);
	}
}
