package curatorLockTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import applicationMain.FoundationsMain;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiAddExperience.ModelAddExperience;
import guiCreateLesson.ModelCreateLesson;
import guiCuratorLock.ModelCuratorLock;
import guiLessonDetails.ModelLessonDetails;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import guiUpdateLesson.ModelUpdateLesson;

/*******
 * <p> Title: CuratorLockTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the Curator lock action, the HW3 Task 5 prototype of TP3
 * Aspect 2 (Epic E6: "As a lessons-learned curator, I can lock entire lessons or specific fields
 * of a lesson so the contributor can't make changes to fields or delete the lesson").  Each test
 * method is one row of "TP3 Testing Details and Rationale.pdf" and its display name starts with
 * that row's id (LK-01 to LK-33), so the JUnit window lists the tests in the same order as the
 * document.</p>
 *
 * <p> <b>What is tested.</b>  ModelCuratorLock.changeLocks() (requirements TR-1 to TR-6 and
 * TR-8), the new Database.setLessonLocks() (TR-7), the unchanged TP2 Contributor Models
 * enforcing a Curator's locks (TR-9), and the TP2 test suites still passing (TR-10).</p>
 *
 * <p> <b>Test data.</b>  Before every test, a new lesson L is created by a Contributor ("the
 * owner") through the TP2 ModelCreateLesson, so it is a real, valid lesson with all five lock
 * flags false.  Every lesson a test creates is deleted after that test, even when the test
 * fails, so the tests never depend on each other or on data already in the database.</p>
 *
 * <p> <b>How to read the output.</b>  Every check prints the test id, the input, the expected
 * result, and the actual result, then asserts that the Expected and Actual lines are identical.
 * A result is written as "Success: message" or "Refused: message", and the five lock flags are
 * written in the order entire, title, description, tags, category, so "F,T,F,F,F" means only
 * the title is locked.  Each negative test also reads the lesson back from the database to prove
 * that nothing changed.</p>
 *
 * <p> FoundationsMain must not be running, because H2 allows one connection.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-01 HW3 Task 5: tests written before the prototype code
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class CuratorLockTests {

	/** The description of lesson L (longer than the 10-character minimum). */
	private static final String DESCRIPTION = "Use GitHub Actions to build and test every push.";

	/** The message for a user who is not a Curator (TR-1). */
	private static final String NOT_CURATOR = "Only a curator can lock or unlock lessons.";

	/** The message for an empty field list when locking (TR-3). */
	private static final String SELECT_LOCK =
			"Select the whole lesson or at least one field to lock.";

	/** The message for an empty field list when unlocking (TR-6). */
	private static final String SELECT_UNLOCK =
			"Select the whole lesson or at least one field to unlock.";

	/** The end of every unknown-field message (TR-3). */
	private static final String CHOOSE = ". Choose entire, title, description, tags, or category.";

	/** The flags of a lesson with nothing locked. */
	private static final String NONE_LOCKED = "F,F,F,F,F";

	/** A Curator only (the Role2 flag, ConOps assumption A1). */
	private static final User CURATOR = new User("hw3curator", "", "", "", "", "", "",
			false, false, true);

	/** An Admin only. */
	private static final User ADMIN = new User("hw3admin", "", "", "", "", "", "",
			true, false, false);

	/** A user with both the Contributor and the Curator roles. */
	private static final User BOTH = new User("hw3both", "", "", "", "", "", "",
			false, true, true);

	/** The Contributor who owns lesson L; unique for every run, like TP2's uniqueUser(). */
	private static final String OWNER_NAME = "hw3owner" + System.currentTimeMillis();

	/** The owner as a User, a Contributor only. */
	private static final User OWNER = new User(OWNER_NAME, "", "", "", "", "", "",
			false, true, false);

	/** Every lesson id created by the current test, deleted after the test. */
	private static final List<Integer> createdLessons = new ArrayList<Integer>();

	/** Lesson L, created fresh before each test. */
	private int lessonL;

	/**********
	 * Default constructor is not used; JUnit creates an instance for each test.
	 */
	public CuratorLockTests() {
	}


	/*-*******************************************************************************************

	Setup and cleanup

	 */

	/**********
	 * <p> Method: void connect() </p>
	 *
	 * <p> Description: Opens the application's database once, before the first test. </p>
	 *
	 * @throws SQLException if the database cannot be opened (for example, the application is
	 * 		still running)
	 */
	@BeforeAll
	static void connect() throws SQLException {
		// The tests use the real database through FoundationsMain.database, as the TP2 tests do,
		// so they exercise the same code the application runs
		FoundationsMain.database.connectToDatabase();
	}

	/**********
	 * <p> Method: void disconnect() </p>
	 *
	 * <p> Description: Closes the database after the last test. </p>
	 */
	@AfterAll
	static void disconnect() {
		// Closing releases H2's single connection, so the application can be started again
		FoundationsMain.database.closeConnection();
	}

	/**********
	 * <p> Method: void createLessonL() </p>
	 *
	 * <p> Description: Creates a fresh lesson L before each test, owned by the Contributor. </p>
	 */
	@BeforeEach
	void createLessonL() {
		// A new lesson for every test keeps the tests independent: no test can see locks left
		// behind by another test
		lessonL = makeLesson();
	}

	/**********
	 * <p> Method: void deleteCreatedLessons() </p>
	 *
	 * <p> Description: Deletes every lesson the test created, even if the test failed. </p>
	 */
	@AfterEach
	void deleteCreatedLessons() {
		// Delete through the Database, not the Contributor Model, because the Model correctly
		// refuses to delete a locked lesson and many tests leave L locked
		for (int id : createdLessons) FoundationsMain.database.deleteLesson(id);
		createdLessons.clear();
	}


	/*-*******************************************************************************************

	Helper methods

	 */

	/**********
	 * <p> Method: int makeLesson() </p>
	 *
	 * <p> Description: Creates a lesson exactly as a Contributor would, through the TP2
	 * ModelCreateLesson, and remembers it for cleanup. </p>
	 *
	 * @return the new lesson's id
	 */
	private static int makeLesson() {
		// Creating through the real Contributor Model guarantees L is a valid TP2 lesson
		LessonResult result = ModelCreateLesson.createLesson(OWNER_NAME, "Set up CI", DESCRIPTION,
				"ci, build", "Technical");
		if (!result.isSuccess())
			throw new IllegalStateException("Test setup failed: " + result.getMessage());
		createdLessons.add(result.getLessonId());
		return result.getLessonId();
	}

	/**********
	 * <p> Method: int missingLessonId() </p>
	 *
	 * <p> Description: Returns the id of a lesson that existed and has been deleted (lesson M in
	 * the test details). </p>
	 *
	 * @return an id that no lesson has any more
	 */
	private static int missingLessonId() {
		// An id that really existed is more realistic than one that never did
		int id = makeLesson();
		FoundationsMain.database.deleteLesson(id);
		return id;
	}

	/**********
	 * <p> Method: String flags(int lessonId) </p>
	 *
	 * <p> Description: Reads a lesson back from the database and returns its five lock flags,
	 * in the order entire, title, description, tags, category. </p>
	 *
	 * @param lessonId the lesson to read
	 *
	 * @return for example "F,T,F,F,F", or "no lesson" if it does not exist
	 */
	private static String flags(int lessonId) {
		// Reading from the database, not from an object in memory, proves what was really saved
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null) return "no lesson";
		return tf(lesson.isLockedEntire()) + "," + tf(lesson.isLockedTitle()) + ","
				+ tf(lesson.isLockedDescription()) + "," + tf(lesson.isLockedTags()) + ","
				+ tf(lesson.isLockedCategory());
	}

	/**********
	 * <p> Method: String tf(boolean value) </p>
	 *
	 * <p> Description: Writes a flag as T or F. </p>
	 *
	 * @param value the flag
	 *
	 * @return "T" for true, "F" for false
	 */
	private static String tf(boolean value) {
		// One letter per flag keeps the five flags readable on one line
		return value ? "T" : "F";
	}

	/**********
	 * <p> Method: String text(int lessonId) </p>
	 *
	 * <p> Description: Reads a lesson's four core fields from the database. </p>
	 *
	 * @param lessonId the lesson to read
	 *
	 * @return title | description | tags | category
	 */
	private static String text(int lessonId) {
		// Used to prove that locking changes only flags, never the Contributor's text
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		return lesson.getTitle() + " | " + lesson.getDescription() + " | " + lesson.getTags()
				+ " | " + lesson.getCategory();
	}

	/**********
	 * <p> Method: String outcome(LessonResult result) </p>
	 *
	 * <p> Description: Describes a result as "Success: message" or "Refused: message". </p>
	 *
	 * @param result the result to describe
	 *
	 * @return the description
	 */
	private static String outcome(LessonResult result) {
		// Including success or failure catches a result with the right message but the wrong
		// outcome, such as a refusal reported as a success
		return (result.isSuccess() ? "Success: " : "Refused: ") + result.getMessage();
	}

	/**********
	 * <p> Method: LessonResult lock(User user, int lessonId, List&lt;String&gt; fields,
	 * 		boolean lock, boolean confirmed) </p>
	 *
	 * <p> Description: Calls the prototype, ModelCuratorLock.changeLocks(). </p>
	 *
	 * @param user the acting user
	 *
	 * @param lessonId the lesson
	 *
	 * @param fields the field names
	 *
	 * @param lock true to lock, false to unlock
	 *
	 * @param confirmed true if the user answered Yes
	 *
	 * @return the prototype's result
	 */
	private static LessonResult lock(User user, int lessonId, List<String> fields, boolean lock,
			boolean confirmed) {
		// One place calls the prototype, so every test uses it the same way
		return ModelCuratorLock.changeLocks(user, lessonId, fields, lock, confirmed);
	}

	/**********
	 * <p> Method: void check(String id, String input, String expected, String actual) </p>
	 *
	 * <p> Description: Prints the Expected and Actual lines for one check and then asserts that
	 * they are identical. </p>
	 *
	 * @param id the test id, such as "LK-01"
	 *
	 * @param input a readable description of the input
	 *
	 * @param expected the required result
	 *
	 * @param actual the result the code produced
	 */
	private static void check(String id, String input, String expected, String actual) {
		// Printing before asserting means the screencast shows both lines even for a failure
		System.out.println(id + "  Input:    " + input);
		System.out.println(id + "  Expected: " + expected);
		System.out.println(id + "  Actual:   " + actual);
		System.out.println();
		assertEquals(expected, actual, id);
	}


	/*-*******************************************************************************************

	TR-1: only a Curator can lock or unlock

	 */

	/**********
	 * <p> Method: void lk01_curatorLocksTitle() </p>
	 *
	 * <p> Description: LK-01, positive.  A Curator locks the title of a lesson the Curator does
	 * not own (scenario S1; TR-1 and TR-2). </p>
	 */
	@Test
	@DisplayName("LK-01 A Curator locks the title (P)")
	void lk01_curatorLocksTitle() {
		// The Curator is not the owner, so success also proves ownership is not required (TR-2)
		LessonResult result = lock(CURATOR, lessonL, List.of("title"), true, true);
		check("LK-01", "changeLocks(curator, L, [title], lock, confirmed)",
				"Success: Locked: title. | flags F,T,F,F,F",
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk02_adminRefused() </p>
	 *
	 * <p> Description: LK-02, negative.  An Admin who is not a Curator is refused. </p>
	 */
	@Test
	@DisplayName("LK-02 An Admin who is not a Curator is refused (N)")
	void lk02_adminRefused() {
		// Managing roles does not include locking (ConOps section IV)
		LessonResult result = lock(ADMIN, lessonL, List.of("title"), true, true);
		check("LK-02", "changeLocks(admin, L, [title], lock, confirmed)",
				"Refused: " + NOT_CURATOR + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk03_ownerRefused() </p>
	 *
	 * <p> Description: LK-03, negative.  The lesson's own Contributor is refused. </p>
	 */
	@Test
	@DisplayName("LK-03 The lesson's own Contributor is refused (N)")
	void lk03_ownerRefused() {
		// The most important refusal: a Contributor must never change locks on their own lesson
		// (Top Defects R1, CWE-862)
		LessonResult result = lock(OWNER, lessonL, List.of("title"), true, true);
		check("LK-03", "changeLocks(owner, L, [title], lock, confirmed)",
				"Refused: " + NOT_CURATOR + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk04_noUserRefused() </p>
	 *
	 * <p> Description: LK-04, negative, boundary value.  No user at all (null) is refused without
	 * an exception. </p>
	 */
	@Test
	@DisplayName("LK-04 No user (null) is refused without an exception (N)")
	void lk04_noUserRefused() {
		// A null user must not crash the method (CWE-476), the defect found in Task 2's recognizers
		LessonResult result = lock(null, lessonL, List.of("title"), true, true);
		check("LK-04", "changeLocks(null, L, [title], lock, confirmed)",
				"Refused: " + NOT_CURATOR + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk05_userWithSeveralRoles() </p>
	 *
	 * <p> Description: LK-05, positive.  A user who is both a Contributor and a Curator may
	 * lock. </p>
	 */
	@Test
	@DisplayName("LK-05 A user with the Contributor and Curator roles may lock (P)")
	void lk05_userWithSeveralRoles() {
		// Holding the Curator role is enough, whatever other roles the user also has
		LessonResult result = lock(BOTH, lessonL, List.of("tags"), true, true);
		check("LK-05", "changeLocks(both, L, [tags], lock, confirmed)",
				"Success: Locked: tags. | flags F,F,F,T,F",
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk06_roleCheckedFirst() </p>
	 *
	 * <p> Description: LK-06, negative.  A non-Curator asking about a missing lesson gets the
	 * not-a-Curator message, not "Lesson not found.", which proves the role is checked first. </p>
	 */
	@Test
	@DisplayName("LK-06 The role is checked before the lesson (N)")
	void lk06_roleCheckedFirst() {
		// If the lesson were checked first, a non-Curator could learn which ids exist (CWE-200)
		LessonResult result = lock(ADMIN, missingLessonId(), List.of("title"), true, true);
		check("LK-06", "changeLocks(admin, M, [title], lock, confirmed)",
				"Refused: " + NOT_CURATOR, outcome(result));
	}


	/*-*******************************************************************************************

	TR-2: the lesson must exist

	 */

	/**********
	 * <p> Method: void lk07_deletedLesson() </p>
	 *
	 * <p> Description: LK-07, negative.  A lesson deleted after the Curator's list was shown
	 * (scenario S5). </p>
	 */
	@Test
	@DisplayName("LK-07 A deleted lesson gives Lesson not found. (N)")
	void lk07_deletedLesson() {
		// The same message the TP2 Contributor pages use, so the whole system speaks one language
		LessonResult result = lock(CURATOR, missingLessonId(), List.of("title"), true, true);
		check("LK-07", "changeLocks(curator, M, [title], lock, confirmed)",
				"Refused: " + LessonAccess.NOT_FOUND, outcome(result));
	}

	/**********
	 * <p> Method: void lk08_idZero() </p>
	 *
	 * <p> Description: LK-08, negative, boundary value.  Id 0, just below the first database
	 * id. </p>
	 */
	@Test
	@DisplayName("LK-08 Lesson id 0 gives Lesson not found. (N)")
	void lk08_idZero() {
		// Database ids start at 1, so 0 is the first invalid id below the range
		LessonResult result = lock(CURATOR, 0, List.of("title"), true, true);
		check("LK-08", "changeLocks(curator, 0, [title], lock, confirmed)",
				"Refused: " + LessonAccess.NOT_FOUND, outcome(result));
	}

	/**********
	 * <p> Method: void lk09_idMinusOne() </p>
	 *
	 * <p> Description: LK-09, negative, boundary value.  Id -1, the TP2 "no lesson" id. </p>
	 */
	@Test
	@DisplayName("LK-09 Lesson id -1 gives Lesson not found. (N)")
	void lk09_idMinusOne() {
		// LessonResult.getLessonId() returns -1 when there is no lesson, so a page could pass it
		LessonResult result = lock(CURATOR, -1, List.of("title"), true, true);
		check("LK-09", "changeLocks(curator, -1, [title], lock, confirmed)",
				"Refused: " + LessonAccess.NOT_FOUND, outcome(result));
	}


	/*-*******************************************************************************************

	TR-3: the field list must be valid

	 */

	/**********
	 * <p> Method: void lk10_emptyList() </p>
	 *
	 * <p> Description: LK-10, negative, boundary value.  An empty list (scenario S4: Lock
	 * pressed with nothing ticked). </p>
	 */
	@Test
	@DisplayName("LK-10 An empty field list is refused (N)")
	void lk10_emptyList() {
		// The smallest possible list; nothing may be saved
		LessonResult result = lock(CURATOR, lessonL, List.of(), true, true);
		check("LK-10", "changeLocks(curator, L, [], lock, confirmed)",
				"Refused: " + SELECT_LOCK + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk11_nullList() </p>
	 *
	 * <p> Description: LK-11, negative.  A missing (null) list is treated like an empty one. </p>
	 */
	@Test
	@DisplayName("LK-11 A null field list is refused without an exception (N)")
	void lk11_nullList() {
		// A missing list must not crash the method (CWE-476)
		LessonResult result = lock(CURATOR, lessonL, null, true, true);
		check("LK-11", "changeLocks(curator, L, null, lock, confirmed)",
				"Refused: " + SELECT_LOCK + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk12_unknownName() </p>
	 *
	 * <p> Description: LK-12, negative.  A name that is not one of the five field names. </p>
	 */
	@Test
	@DisplayName("LK-12 An unknown field name is refused (N)")
	void lk12_unknownName() {
		// The message lists the valid names so the user knows what to choose (CWE-20)
		LessonResult result = lock(CURATOR, lessonL, List.of("summary"), true, true);
		check("LK-12", "changeLocks(curator, L, [summary], lock, confirmed)",
				"Refused: Unknown field: summary" + CHOOSE + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk13_wrongCase() </p>
	 *
	 * <p> Description: LK-13, negative, boundary value.  "Title" differs from a valid name only
	 * by letter case. </p>
	 */
	@Test
	@DisplayName("LK-13 A name with the wrong letter case is refused (N)")
	void lk13_wrongCase() {
		// The TP2 lock methods are case-sensitive, so accepting "Title" would silently lock nothing
		LessonResult result = lock(CURATOR, lessonL, List.of("Title"), true, true);
		check("LK-13", "changeLocks(curator, L, [Title], lock, confirmed)",
				"Refused: Unknown field: Title" + CHOOSE + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk14_validAndUnknown() </p>
	 *
	 * <p> Description: LK-14, negative.  A valid name with an unknown one: nothing is saved, not
	 * even the valid name. </p>
	 */
	@Test
	@DisplayName("LK-14 A valid name mixed with an unknown one saves nothing (N)")
	void lk14_validAndUnknown() {
		// Proves the whole list is checked before anything is written (TR-3, TR-7)
		LessonResult result = lock(CURATOR, lessonL, List.of("title", "summary"), true, true);
		check("LK-14", "changeLocks(curator, L, [title, summary], lock, confirmed)",
				"Refused: Unknown field: summary" + CHOOSE + " | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk15_repeatedName() </p>
	 *
	 * <p> Description: LK-15, positive.  A repeated name is counted once. </p>
	 */
	@Test
	@DisplayName("LK-15 A repeated name is counted once (P)")
	void lk15_repeatedName() {
		// A page that sends the same box twice must not cause an error or a doubled message
		LessonResult result = lock(CURATOR, lessonL, List.of("tags", "tags"), true, true);
		check("LK-15", "changeLocks(curator, L, [tags, tags], lock, confirmed)",
				"Success: Locked: tags. | flags F,F,F,T,F",
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk16_allFiveNames() </p>
	 *
	 * <p> Description: LK-16, positive, boundary value.  The largest valid list: all five
	 * names. </p>
	 */
	@Test
	@DisplayName("LK-16 All five names together are accepted (P)")
	void lk16_allFiveNames() {
		// Every column is set by the same single write, and the message lists all five in order
		LessonResult result = lock(CURATOR, lessonL,
				List.of("entire", "title", "description", "tags", "category"), true, true);
		check("LK-16", "changeLocks(curator, L, [entire, title, description, tags, category], "
				+ "lock, confirmed)",
				"Success: Locked: entire lesson, title, description, tags, category. "
						+ "| flags T,T,T,T,T",
				outcome(result) + " | flags " + flags(lessonL));
	}


	/*-*******************************************************************************************

	TR-4: nothing changes without confirmation

	 */

	/**********
	 * <p> Method: void lk17_notConfirmed() </p>
	 *
	 * <p> Description: LK-17, negative.  A valid request that is not confirmed returns a refused
	 * result with an EMPTY message and saves nothing (scenario S4). </p>
	 */
	@Test
	@DisplayName("LK-17 Without confirmation nothing is saved (N)")
	void lk17_notConfirmed() {
		// The empty message follows the TP2 delete convention: "valid, waiting for Yes", so the
		// page knows to show its "Are you sure?" dialog
		LessonResult result = lock(CURATOR, lessonL, List.of("title"), true, false);
		check("LK-17", "changeLocks(curator, L, [title], lock, NOT confirmed)",
				"Refused:  | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk18_askThenConfirm() </p>
	 *
	 * <p> Description: LK-18, positive.  The two-step flow the Curator page will use: ask first,
	 * then save after Yes. </p>
	 */
	@Test
	@DisplayName("LK-18 Ask first, then save after Yes (P)")
	void lk18_askThenConfirm() {
		// First call: the page asks; second call: the Curator answered Yes
		LessonResult first = lock(CURATOR, lessonL, List.of("title"), true, false);
		LessonResult second = lock(CURATOR, lessonL, List.of("title"), true, true);
		check("LK-18", "the LK-17 call, then the same call confirmed",
				"Refused:  then Success: Locked: title. | flags F,T,F,F,F",
				outcome(first) + " then " + outcome(second) + " | flags " + flags(lessonL));
	}


	/*-*******************************************************************************************

	TR-5, TR-6, and TR-8: exactly the chosen flags change, and the message says which

	 */

	/**********
	 * <p> Method: void lk19_twoFieldsReverseOrder() </p>
	 *
	 * <p> Description: LK-19, positive.  Scenario S1: lock title and tags, given in reverse
	 * order. </p>
	 */
	@Test
	@DisplayName("LK-19 Two fields lock exactly those flags; message in fixed order (P)")
	void lk19_twoFieldsReverseOrder() {
		// Reverse order proves the message uses the fixed order (TR-8); the text check proves
		// that locking changes nothing but flags (TR-5)
		String before = text(lessonL);
		LessonResult result = lock(CURATOR, lessonL, List.of("tags", "title"), true, true);
		check("LK-19", "changeLocks(curator, L, [tags, title], lock, confirmed)",
				"Success: Locked: title, tags. | flags F,T,F,T,F | text unchanged: true",
				outcome(result) + " | flags " + flags(lessonL) + " | text unchanged: "
						+ before.equals(text(lessonL)));
	}

	/**********
	 * <p> Method: void lk20_wholeLesson() </p>
	 *
	 * <p> Description: LK-20, positive.  Scenario S2: a whole-lesson lock makes every field count
	 * as locked through the TP2 methods. </p>
	 */
	@Test
	@DisplayName("LK-20 A whole-lesson lock covers every field (P)")
	void lk20_wholeLesson() {
		// The whole-lesson flag is separate, but TP2's isFieldLocked() and isLocked() must
		// treat everything as locked
		LessonResult result = lock(CURATOR, lessonL, List.of("entire"), true, true);
		LessonsLearned stored = LessonsLearned.read(lessonL);
		boolean allFieldsLocked = stored.isFieldLocked(LessonsLearned.FIELD_TITLE)
				&& stored.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION)
				&& stored.isFieldLocked(LessonsLearned.FIELD_TAGS)
				&& stored.isFieldLocked(LessonsLearned.FIELD_CATEGORY);
		check("LK-20", "changeLocks(curator, L, [entire], lock, confirmed)",
				"Success: Locked: entire lesson. | flags T,F,F,F,F | every field locked: true"
						+ " | isLocked: true",
				outcome(result) + " | flags " + flags(lessonL) + " | every field locked: "
						+ allFieldsLocked + " | isLocked: " + stored.isLocked());
	}

	/**********
	 * <p> Method: void lk21_unlockOneOfThree() </p>
	 *
	 * <p> Description: LK-21, positive.  Scenario S3: unlocking one mistake leaves the other
	 * locks in place (TR-6). </p>
	 */
	@Test
	@DisplayName("LK-21 Unlocking one field leaves the other locks (P)")
	void lk21_unlockOneOfThree() {
		// Setup: three locks, then remove only the one locked by mistake
		lock(CURATOR, lessonL, List.of("title", "tags", "category"), true, true);
		LessonResult result = lock(CURATOR, lessonL, List.of("category"), false, true);
		check("LK-21", "lock [title, tags, category], then changeLocks(curator, L, [category], "
				+ "unlock, confirmed)",
				"Success: Unlocked: category. | flags F,T,F,T,F",
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk22_unlockWhatIsNotLocked() </p>
	 *
	 * <p> Description: LK-22, positive.  Unlocking a field that is not locked is harmless. </p>
	 */
	@Test
	@DisplayName("LK-22 Unlocking a field that is not locked is harmless (P)")
	void lk22_unlockWhatIsNotLocked() {
		// The Curator never has to know the current state before unlocking
		LessonResult result = lock(CURATOR, lessonL, List.of("description"), false, true);
		check("LK-22", "changeLocks(curator, L, [description], unlock, confirmed)",
				"Success: Unlocked: description. | flags " + NONE_LOCKED,
				outcome(result) + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk23_emptyListWhenUnlocking() </p>
	 *
	 * <p> Description: LK-23, negative.  The empty-list message uses the word for the action
	 * actually requested. </p>
	 */
	@Test
	@DisplayName("LK-23 An empty list when unlocking uses the unlock wording (N)")
	void lk23_emptyListWhenUnlocking() {
		// "to lock" would confuse a Curator who pressed Unlock
		LessonResult result = lock(CURATOR, lessonL, List.of(), false, true);
		check("LK-23", "changeLocks(curator, L, [], unlock, confirmed)",
				"Refused: " + SELECT_UNLOCK, outcome(result));
	}


	/*-*******************************************************************************************

	TR-7: one write, all or nothing (the new Database method, called directly)

	 */

	/**********
	 * <p> Method: void lk24_threeFlagsOneCall() </p>
	 *
	 * <p> Description: LK-24, positive.  Three flags set by one call to setLessonLocks(), with
	 * one name repeated. </p>
	 */
	@Test
	@DisplayName("LK-24 setLessonLocks sets three flags in one call, repeats ignored (P)")
	void lk24_threeFlagsOneCall() {
		// One call runs one UPDATE statement, so the three flags cannot be saved partly (OB-7).
		// "tags" is repeated on purpose: H2 rejects a column listed twice in SET, so the method
		// must remove repeats itself (a gap the mutation check found).
		boolean saved = FoundationsMain.database.setLessonLocks(lessonL,
				Arrays.asList("title", "description", "tags", "tags"), true);
		check("LK-24", "setLessonLocks(L, [title, description, tags, tags], true)",
				"returned true | flags F,T,T,T,F",
				"returned " + saved + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk25_missingRow() </p>
	 *
	 * <p> Description: LK-25, negative.  A write that changes no row is reported as a
	 * failure. </p>
	 */
	@Test
	@DisplayName("LK-25 setLessonLocks on a missing lesson returns false (N)")
	void lk25_missingRow() {
		// Reporting this as a failure is what lets changeLocks() tell the Curator it was not saved
		boolean saved = FoundationsMain.database.setLessonLocks(missingLessonId(),
				List.of("title"), true);
		check("LK-25", "setLessonLocks(M, [title], true)", "returned false",
				"returned " + saved);
	}

	/**********
	 * <p> Method: void lk26_emptyListAtDatabase() </p>
	 *
	 * <p> Description: LK-26, negative, boundary value.  An empty or missing (null) list must
	 * not run an empty or broken UPDATE. </p>
	 */
	@Test
	@DisplayName("LK-26 setLessonLocks with an empty or null list returns false (N)")
	void lk26_emptyListAtDatabase() {
		// "UPDATE ... SET WHERE" with no columns would be invalid SQL.  The null list was added
		// after the coverage run showed that branch untested.
		boolean savedEmpty = FoundationsMain.database.setLessonLocks(lessonL, List.of(), true);
		boolean savedNull = FoundationsMain.database.setLessonLocks(lessonL, null, true);
		check("LK-26", "setLessonLocks(L, [], true), then setLessonLocks(L, null, true)",
				"returned false, false | flags " + NONE_LOCKED,
				"returned " + savedEmpty + ", " + savedNull + " | flags " + flags(lessonL));
	}

	/**********
	 * <p> Method: void lk27_unknownNameAtDatabase() </p>
	 *
	 * <p> Description: LK-27, negative.  An unknown or null name is refused before the statement
	 * is built, so nothing is written. </p>
	 */
	@Test
	@DisplayName("LK-27 setLessonLocks with an unknown or null name writes nothing (N)")
	void lk27_unknownNameAtDatabase() {
		// Column names cannot be SQL parameters, so only the five known names may ever reach the
		// statement text (CWE-89).  A null name (added after the coverage run) must be refused
		// the same way, not crash.  Arrays.asList is used because List.of rejects null.
		boolean savedBogus = FoundationsMain.database.setLessonLocks(lessonL,
				List.of("title", "bogus"), true);
		boolean savedNull = FoundationsMain.database.setLessonLocks(lessonL,
				Arrays.asList("title", null), true);
		check("LK-27", "setLessonLocks(L, [title, bogus], true), then setLessonLocks(L, [title, "
				+ "null], true)",
				"returned false, false | flags " + NONE_LOCKED,
				"returned " + savedBogus + ", " + savedNull + " | flags " + flags(lessonL));
	}


	/*-*******************************************************************************************

	TR-9 and TR-10: the existing system enforces the locks and still works

	 */

	/**********
	 * <p> Method: void lk28_ownerCannotEditLockedTitle() </p>
	 *
	 * <p> Description: LK-28, negative.  After a Curator locks the title, the owner's change is
	 * refused by the unchanged TP2 ModelUpdateLesson (US-U1-V). </p>
	 */
	@Test
	@DisplayName("LK-28 The owner cannot edit a title the Curator locked (N)")
	void lk28_ownerCannotEditLockedTitle() {
		// The heart of Epic E6: a Curator's lock really stops the Contributor
		lock(CURATOR, lessonL, List.of("title"), true, true);
		LessonResult result = ModelUpdateLesson.updateLesson(OWNER_NAME, lessonL, "Set up CD",
				DESCRIPTION, "ci, build", "Technical");
		check("LK-28", "Curator locks [title]; owner changes the title to Set up CD",
				"Refused: " + LessonAccess.FIELD_LOCKED + " | field title | stored title Set up CI",
				outcome(result) + " | field " + result.getFailedField() + " | stored title "
						+ LessonsLearned.read(lessonL).getTitle());
	}

	/**********
	 * <p> Method: void lk29_ownerCanEditUnlockedField() </p>
	 *
	 * <p> Description: LK-29, positive.  Locking the title does not freeze the description. </p>
	 */
	@Test
	@DisplayName("LK-29 The owner can still edit a field that is not locked (P)")
	void lk29_ownerCanEditUnlockedField() {
		// The "can still edit the others" part of the Epic E6 scenario
		// The lock's own result is checked too, so this test cannot pass just because nothing
		// was locked (a weakness the first, test-first run exposed)
		String newDescription = "Use GitHub Actions to build, test, and deploy every push.";
		LessonResult locked = lock(CURATOR, lessonL, List.of("title"), true, true);
		LessonResult result = ModelUpdateLesson.updateLesson(OWNER_NAME, lessonL, "Set up CI",
				newDescription, "ci, build", "Technical");
		check("LK-29", "Curator locks [title]; owner changes only the description",
				"Success: Locked: title. then Success: Lesson updated | description saved: true",
				outcome(locked) + " then " + outcome(result) + " | description saved: "
						+ newDescription.equals(LessonsLearned.read(lessonL).getDescription()));
	}

	/**********
	 * <p> Method: void lk30_ownerCannotDeleteLockedLesson() </p>
	 *
	 * <p> Description: LK-30, negative.  Any locked field blocks deletion, even with an explicit
	 * Yes (US-D1-V). </p>
	 */
	@Test
	@DisplayName("LK-30 The owner cannot delete a lesson with a locked field (N)")
	void lk30_ownerCannotDeleteLockedLesson() {
		// confirmed = true is the strongest attempt: the owner has already answered Yes
		lock(CURATOR, lessonL, List.of("tags"), true, true);
		LessonResult result = ModelLessonDetails.deleteLesson(OWNER_NAME, lessonL, true);
		check("LK-30", "Curator locks [tags]; owner deletes L and answers Yes",
				"Refused: " + LessonAccess.LESSON_LOCKED + " | still exists: true",
				outcome(result) + " | still exists: " + (LessonsLearned.read(lessonL) != null));
	}

	/**********
	 * <p> Method: void lk31_experienceStillAllowed() </p>
	 *
	 * <p> Description: LK-31, positive.  A fully locked lesson still accepts experience (ConOps
	 * assumption A4). </p>
	 */
	@Test
	@DisplayName("LK-31 Experience can still be added to a fully locked lesson (P)")
	void lk31_experienceStillAllowed() {
		// Locks protect the lesson's text, not its record of reuse, which Epic E4 statistics need
		// The lock's own result is checked too, so this test cannot pass without a real lock
		LessonResult locked = lock(CURATOR, lessonL, List.of("entire"), true, true);
		LessonResult result = ModelAddExperience.addExperience(OWNER_NAME, lessonL, "2.5",
				"Reused the CI setup for a new repository.");
		check("LK-31", "Curator locks [entire]; owner adds 2.5 hours of experience",
				"Success: Locked: entire lesson. then Success: Experience added",
				outcome(locked) + " then " + outcome(result));
	}

	/**********
	 * <p> Method: void lk32_unlockGivesControlBack() </p>
	 *
	 * <p> Description: LK-32, positive.  After the Curator unlocks the title, the owner can
	 * change it again. </p>
	 */
	@Test
	@DisplayName("LK-32 After unlocking, the owner can edit the title again (P)")
	void lk32_unlockGivesControlBack() {
		// Lock, then unlock, then edit: unlocking must really return control to the Contributor
		// Both Curator results are checked, so the edit is proven to follow a real lock and unlock
		LessonResult locked = lock(CURATOR, lessonL, List.of("title"), true, true);
		LessonResult unlocked = lock(CURATOR, lessonL, List.of("title"), false, true);
		LessonResult result = ModelUpdateLesson.updateLesson(OWNER_NAME, lessonL, "Set up CD",
				DESCRIPTION, "ci, build", "Technical");
		check("LK-32", "Curator locks, then unlocks [title]; owner changes the title to Set up CD",
				"Success: Locked: title. then Success: Unlocked: title. then Success: Lesson "
						+ "updated | stored title Set up CD",
				outcome(locked) + " then " + outcome(unlocked) + " then " + outcome(result)
						+ " | stored title " + LessonsLearned.read(lessonL).getTitle());
	}

	/**********
	 * <p> Method: void lk33_tp2SuitesStillPass() </p>
	 *
	 * <p> Description: LK-33, positive, regression (TR-10).  Runs the 66 TP2 requirement tests
	 * and the 58 input validation tests, which throw an AssertionError if any test fails.  The
	 * third part of LK-33, my hw3Tests package (54 runs with the same 7 expected failures), is
	 * run separately in the JUnit window because its failures are intentional. </p>
	 *
	 * @throws SQLException if the database cannot be reopened afterward
	 */
	@Test
	@DisplayName("LK-33 The TP2 test suites still pass (P, regression)")
	void lk33_tp2SuitesStillPass() throws SQLException {
		// The TP2 suite opens and closes the shared database itself, so close ours first and
		// reopen it afterward for the cleanup that runs after this test
		FoundationsMain.database.closeConnection();
		String tp2Output;
		String validationOutput;
		try {
			tp2Output = captureOutput(() -> lessonTests.AllContributorTests.main(new String[0]));
			validationOutput = captureOutput(
					() -> inputValidation.LessonRecognizerTest.main(new String[0]));
		} finally {
			FoundationsMain.database.connectToDatabase();
		}
		// The printed totals are checked too, because the TP2 suite returns quietly without
		// running anything if it cannot connect, and that must not count as a pass
		check("LK-33", "AllContributorTests and LessonRecognizerTest",
				"66 passed, 0 failed | 58 passed, 0 failed",
				totals(tp2Output) + " | " + totals(validationOutput));
		assertTrue(tp2Output.contains("Number of tests passed: 66"), "LK-33 TP2 suite ran");
	}

	/**********
	 * <p> Method: String captureOutput(Runnable suite) </p>
	 *
	 * <p> Description: Runs a test suite and returns everything it printed. </p>
	 *
	 * @param suite the suite to run
	 *
	 * @return the printed output
	 */
	private static String captureOutput(Runnable suite) {
		// The suites print hundreds of lines; capturing them keeps the console readable and lets
		// the test read the totals.  finally restores the console even if a suite fails.
		PrintStream console = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured));
		try {
			suite.run();
		} finally {
			System.setOut(console);
		}
		return captured.toString();
	}

	/**********
	 * <p> Method: String totals(String output) </p>
	 *
	 * <p> Description: Finds the "Number of tests passed/failed" totals in a suite's output. </p>
	 *
	 * @param output the suite's printed output
	 *
	 * @return for example "66 passed, 0 failed", or "no totals printed"
	 */
	private static String totals(String output) {
		// Both TP2 suites end with the same two footer lines, copied from the Foundations testbed
		java.util.regex.Matcher passed =
				java.util.regex.Pattern.compile("Number of tests passed: (\\d+)").matcher(output);
		java.util.regex.Matcher failed =
				java.util.regex.Pattern.compile("Number of tests failed: (\\d+)").matcher(output);
		if (!passed.find() || !failed.find()) return "no totals printed";
		return passed.group(1) + " passed, " + failed.group(1) + " failed";
	}
}
