package lessonTests;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import applicationMain.FoundationsMain;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiCreateLesson.ModelCreateLesson;
import guiTools.LessonResult;

/*******
 * <p> Title: TestSupport Class. </p>
 *
 * <p> Description: The shared machinery used by every test class in this package, whether the
 * tests are run by JUnit or as a console program.  It connects to the Foundations Database, prints the requirement headers and the
 * result of each test in one fixed format, remembers every lesson the tests create so they can
 * all be removed at the end, and prints the totals.  Keeping this in one place means every test
 * class reports its results the same way, so the output of one class reads like the output of
 * any other.</p>
 *
 * <p> <b>How to read the output.</b>  Every test prints four lines:</p>
 * <pre>
 *   C1-P1  Positive  Create Lesson - Valid Input
 *          Expected: success "Lesson created"; my list holds 1 lesson(s); ...
 *          Actual:   success "Lesson created"; my list holds 1 lesson(s); ...
 *          Result:   PASS
 * </pre>
 * <p> The first line gives the test id (requirement, then P for a positive test or N for a
 * negative test, then a number), the kind of test, and its name.  The Expected line is written
 * by hand in the test method from the requirement.  The Actual line is built from what the code
 * under test really returned and what is really stored in the database afterwards.  A test
 * passes only when the two lines are exactly the same, so the reader can confirm the result by
 * comparing them.  "Positive" means the input is acceptable and the action must succeed;
 * "Negative" means the input or situation is not acceptable and the action must be refused with
 * the specific message shown, leaving the stored data unchanged.</p>
 *
 * <p> The suite ends with "Number of tests passed" and "Number of tests failed", the same footer
 * the Foundations testbed classes (PasswordEvaluationTestingAutomation) print, and throws an
 * AssertionError if any test failed, so a failure cannot be missed.</p>
 *
 * <p> <b>Under JUnit (TP3).</b>  Each test class calls startJUnit() before its first test and
 * finishJUnit() after its last.  check() prints the same four lines and then asserts that the
 * Expected and Actual lines are equal, so a failure fails that JUnit test and its message names
 * the test id.  The console runner, runSuite(), is kept because curatorLockTests LK-33 uses it to
 * show the TP2 tests still pass.</p>
 *
 * <p> The tests use the real H2 database file.  H2 allows only one connection to that file, so
 * FoundationsMain must not be running while the tests run.  Each test uses its own brand-new
 * username, so lessons left from any earlier run, or created by real users, cannot change the
 * counts a test expects.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2 Task 6: shared support for the requirement-based test
 * 							classes (replaces the helpers in ContributorFunctionalTest)
 * @version 1.01		2026-11-14 TP3 Task 5.4: startJUnit(), finishJUnit(), and a JUnit
 * 							assertion in check()
 */
public class TestSupport {

	/** How many tests have run in the current suite. */
	private static int numberOfTests = 0;

	/** How many of those tests failed. */
	private static int numberFailed = 0;

	/** Whether this run has opened the database connection. */
	private static boolean connected = false;

	/** True while runSuite is running the tests as a console program, false under JUnit. */
	private static boolean consoleMode = false;

	/** Every lesson the tests create, remembered so cleanUp() can delete it at the end. */
	private static List<Integer> createdLessonIds = new ArrayList<Integer>();

	/** A description that is valid (10 to 5000 characters) and used whenever the value does not
	 *  matter to the test. */
	public static final String DESCRIPTION = "This description is definitely long enough.";

	/** The interface a test class's runAll method satisfies, so runSuite can run any class. */
	public interface TestGroup {
		/** Runs every test in the group. */
		void run();
	}

	/**********
	 * Default constructor is not used; every member is static.
	 */
	public TestSupport() {
	}


	/*-*******************************************************************************************

	Running a suite

	 */

	/**********
	 * Runs one or more test groups as a suite: connects to the database, prints the suite
	 * title, runs the groups, deletes every lesson the tests created, closes the database, and
	 * prints the totals.  Each test class's main method calls this with its own group, and
	 * AllContributorTests calls it with every group.
	 *
	 * @param title		the title printed at the top of the output
	 * @param groups	the test groups to run, in order
	 *
	 * @throws AssertionError if any test failed, so a failing run cannot be mistaken for a pass
	 */
	public static void runSuite(String title, TestGroup... groups) {
		try {
			FoundationsMain.database.connectToDatabase();
			connected = true;
		} catch (SQLException e) {
			System.out.println("*** ERROR *** Could not connect to the database.  Is FoundationsMain "
					+ "still running?  " + e.getMessage());
			return;
		}

		numberOfTests = 0;
		numberFailed = 0;
		consoleMode = true;
		System.out.println(title);
		System.out.println("Each test prints what the requirement expects and what the code actually "
				+ "did.\nA test passes only when the Expected and Actual lines are identical.");

		try {
			for (TestGroup group : groups) group.run();
		} finally {
			// Clean up even if a test threw an unexpected exception, so no test data is left
			cleanUp();
			FoundationsMain.database.closeConnection();
			connected = false;
			consoleMode = false;
		}

		// Same footer wording as the testbed automation files (PasswordEvaluationTestingAutomation)
		System.out.println("____________________________________________________________________________");
		System.out.println();
		System.out.println("Number of tests passed: " + (numberOfTests - numberFailed));
		System.out.println("Number of tests failed: " + numberFailed);
		if (numberFailed > 0)
			throw new AssertionError(numberFailed + " test(s) failed in: " + title);
	}


	/**********
	 * <p> Method: void startJUnit() </p>
	 *
	 * <p> Description: Opens the database for a test class run by JUnit.  Each test class calls
	 * this from its @BeforeAll method, so every class can be run on its own, or the whole
	 * lessonTests package at once. </p>
	 *
	 * @throws IllegalStateException if the database cannot be opened, for example because
	 * 		FoundationsMain is still running
	 */
	public static void startJUnit() {
		// The same database and the same unique test users as the console runs, so the JUnit
		// results are directly comparable with the TP2 results
		try {
			FoundationsMain.database.connectToDatabase();
			connected = true;
		} catch (SQLException e) {
			throw new IllegalStateException("Could not connect to the database.  Is FoundationsMain "
					+ "still running?  " + e.getMessage());
		}
	}


	/**********
	 * <p> Method: void finishJUnit() </p>
	 *
	 * <p> Description: Deletes every lesson the class's tests created and closes the database.
	 * Each test class calls this from its @AfterAll method, which JUnit runs even when a test
	 * failed, so no test data is left behind. </p>
	 */
	public static void finishJUnit() {
		cleanUp();
		FoundationsMain.database.closeConnection();
		connected = false;
	}


	/**********
	 * Prints the header for one requirement, so every test below it in the output is clearly
	 * tied to that requirement.
	 *
	 * @param requirementId	the requirement id, for example "US-C1" or "E3"
	 * @param statement		a short statement of the requirement
	 */
	public static void requirement(String requirementId, String statement) {
		System.out.println();
		System.out.println("=== " + requirementId + ": " + statement);
	}


	/**********
	 * Runs one test method.  If the code under test throws an exception the test did not
	 * expect, the exception is counted as a failure and printed, and the remaining tests still
	 * run, so one broken behavior cannot hide the results of all the others.
	 *
	 * @param test	the test method to run
	 */
	public static void run(Runnable test) {
		try {
			test.run();
		} catch (RuntimeException e) {
			numberOfTests++;
			numberFailed++;
			System.out.println();
			System.out.println("  *** FAIL *** the test stopped with an unexpected exception: " + e);
		}
	}


	/**********
	 * Records and prints one test.  The test passes only when the expected and actual
	 * descriptions are exactly equal; the comparison is done on text so that the printed output
	 * is itself the evidence for the result.  Run by JUnit, a failed check also fails the JUnit
	 * test, with the test id and name in the message; run as a console program, it is counted
	 * and the remaining tests still run.
	 *
	 * @param testId	the test id, for example "C1-P1"
	 * @param positive	true for a positive test (the action must succeed), false for a negative
	 * 					test (the action must be refused)
	 * @param name		the descriptive test name, matching the Requirements-Test Traceability list
	 * @param expected	what the requirement says should happen, written by hand in the test
	 * @param actual	what the code under test actually did, built from its real results
	 */
	public static void check(String testId, boolean positive, String name, String expected,
			String actual) {
		numberOfTests++;
		boolean passed = expected.equals(actual);
		if (!passed) numberFailed++;
		System.out.println();
		System.out.println(String.format("  %-7s %-9s %s", testId, positive ? "Positive" : "Negative",
				name));
		System.out.println("          Expected: " + expected);
		System.out.println("          Actual:   " + actual);
		System.out.println("          Result:   " + (passed ? "PASS" : "*** FAIL ***"));
		// Printed first, then asserted, so the Expected and Actual lines show in the JUnit
		// console even for a failure
		if (!consoleMode) assertEquals(expected, actual, testId + " " + name);
	}


	/*-*******************************************************************************************

	Describing results as text

	 */

	/**********
	 * Turns a Model result into the short text used on the Expected and Actual lines: whether
	 * the action succeeded or was refused, the exact message shown to the user, and (for a
	 * refusal) which field the message belongs beneath.
	 *
	 * @param result	the result returned by a Contributor Model
	 * @return for example: success "Lesson created", or refused "Title is required." (field: title)
	 */
	public static String describe(LessonResult result) {
		if (result == null) return "no result (null)";
		String message = result.getMessage();
		if (result.isSuccess())
			return "success" + (message.isEmpty() ? "" : " \"" + message + "\"");
		String text = message.isEmpty() ? "refused with no message" : "refused \"" + message + "\"";
		String field = result.getFailedField();
		return (field == null || field.isEmpty()) ? text : text + " (field: " + field + ")";
	}


	/**********
	 * Lists the titles of the lessons in a list, sorted alphabetically so that the text does not
	 * depend on the order the database returns them in.
	 *
	 * @param list	the lessons, possibly null
	 * @return for example: 2 lesson(s): Deploy pipeline fix, Sprint retro notes
	 */
	public static String titles(LessonsLearnedList list) {
		if (list == null) return "null list";
		List<String> names = new ArrayList<String>();
		for (LessonsLearned lesson : list.getLessons()) names.add(lesson.getTitle());
		Collections.sort(names);
		return list.size() + " lesson(s)" + (names.isEmpty() ? "" : ": " + String.join(", ", names));
	}


	/**********
	 * Describes the stored core information of one lesson, read fresh from the database, so a
	 * test can prove what was really saved rather than what the Model claims was saved.
	 *
	 * @param lessonId	the lesson to read
	 * @return "title | category | tags: ..." or "no lesson stored with that id"
	 */
	public static String stored(int lessonId) {
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null) return "no lesson stored with that id";
		String tags = lesson.getTags().isEmpty() ? "(none)" : lesson.getTags();
		return lesson.getTitle() + " | " + lesson.getCategory() + " | tags: " + tags;
	}


	/*-*******************************************************************************************

	Test data

	 */

	/**********
	 * Makes a username that no earlier run can have used.  The H2 database is a real file that
	 * survives between runs, so a fixed username could still own lessons from the last run and
	 * throw the counts off.
	 *
	 * @param base	a readable name for the test, so stray rows can be traced to their test
	 * @return a brand-new username, for example "tp2test_createTester_123456789"
	 */
	public static String uniqueUser(String base) {
		return "tp2test_" + base + "_" + System.nanoTime();
	}


	/**********
	 * Creates a valid lesson through the Create Lesson Model (the same path the GUI uses) and
	 * remembers it for cleanUp().  Used to set up a test, not as the action being tested.
	 *
	 * @param user		the Contributor who owns the lesson
	 * @param title		the lesson title
	 * @param category	the lesson category
	 * @return the new lesson's id
	 *
	 * @throws IllegalStateException if the lesson could not be created, because every test that
	 * 		calls this depends on the lesson existing
	 */
	public static int makeLesson(String user, String title, String category) {
		LessonResult result = ModelCreateLesson.createLesson(user, title, DESCRIPTION, "java",
				category);
		if (!result.isSuccess())
			throw new IllegalStateException("Test setup failed: " + result.getMessage());
		remember(result.getLessonId());
		return result.getLessonId();
	}


	/**********
	 * Remembers a lesson id so cleanUp() deletes it.  Tests that create lessons directly (not
	 * through makeLesson) call this.
	 *
	 * @param lessonId	the id of a lesson a test created; ids of 0 or less are ignored
	 */
	public static void remember(int lessonId) {
		if (lessonId > 0) createdLessonIds.add(lessonId);
	}


	/**********
	 * Sets a lock flag the way the Phase Three Curator will.  No Contributor action can set a
	 * lock, so the tests set it directly to create the locked situations the requirements
	 * describe.
	 *
	 * @param lessonId	the lesson to lock
	 * @param scope		"title", "description", "tags", "category", or "entire"
	 * @param locked	true to lock, false to unlock
	 */
	public static void lock(int lessonId, String scope, boolean locked) {
		if (!FoundationsMain.database.setLessonLock(lessonId, scope, locked))
			throw new IllegalStateException("Test setup failed: could not set the " + scope + " lock");
	}


	/**********
	 * Deletes every lesson these tests created (their experience entries and team-member shares
	 * go with them through the database's ON DELETE CASCADE rules).  This goes straight to the
	 * Database so it also removes locked lessons, which the Model correctly refuses to delete.
	 */
	private static void cleanUp() {
		if (!connected) return;
		int removed = 0;
		for (int id : createdLessonIds)
			if (FoundationsMain.database.deleteLesson(id)) removed++;
		System.out.println("\nCleaned up " + removed + " test lesson(s).");
		createdLessonIds.clear();
	}


	/**********
	 * Builds a String by repeating one piece, used for exact-boundary-length inputs.
	 *
	 * @param piece	the text to repeat
	 * @param times	how many times to repeat it
	 * @return the repeated text
	 */
	public static String rep(String piece, int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++) sb.append(piece);
		return sb.toString();
	}
}
