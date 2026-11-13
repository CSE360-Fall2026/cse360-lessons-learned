package curatorLockTests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiCreateLesson.ModelCreateLesson;
import guiCuratorHome.ModelCuratorHome;
import guiCuratorLock.ModelCuratorLock;
import guiTools.LessonAccess;
import guiTools.LessonResult;

/*******
 * <p> Title: CuratorLockPageTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the Curator Lock page of TP3 Aspect 2, locking lessons and
 * fields (Epic E6).  The lock action itself, ModelCuratorLock.changeLocks(), was tested in HW3
 * by CuratorLockTests (LK-01 to LK-33).  These tests cover what the page adds on top of it: how
 * the ticked boxes become field names, how the confirmation question is worded, and the order
 * in which the page calls the Model.  Each test's display name starts with its id (LP-01 to
 * LP-20) and ends with (P) for a positive test or (N) for a negative test.</p>
 *
 * <p> <b>Requirements tested.</b></p>
 * <ul>
 * <li> LR-1 (US-CL1, US-CL-V): the boxes the Curator ticks become the field names, once each,
 * 		in the fixed order entire, title, description, tags, category; no box ticked is an
 * 		empty list, which the lock action refuses.  Tests LP-01 to LP-03,
 * 		ModelCuratorLock.chosenFields(). </li>
 * <li> LR-2 (US-CL1, US-CL2; Initial ConOps objective OB-6): the confirmation question names
 * 		the action, the chosen parts, and the lesson, for example "Lock title and tags of 'Set
 * 		up CI'?".  Tests LP-04 to LP-11, ModelCuratorLock.confirmationQuestion(). </li>
 * <li> LR-3 (US-CL1, US-CL2; scenarios S1, S2, S3): the page asks first and saves only after
 * 		Yes, and afterwards the Curator Home Page shows the lesson's new lock state.  Tests
 * 		LP-12, LP-13, and LP-17. </li>
 * <li> LR-4 (US-CL-V; scenarios S4, S5): answering No saves nothing; a lesson that is gone and
 * 		a user who is not a Curator are refused before any question is asked.  Tests LP-14 to
 * 		LP-16. </li>
 * <li> LR-5 (US-CL1, US-CL-V): the page opens only for a Curator and only for a lesson that
 * 		exists, and shows that lesson's stored locks.  Tests LP-18 to LP-20,
 * 		ModelCuratorLock.lessonToLock(). </li>
 * </ul>
 *
 * <p> <b>How the page flow is tested without the page.</b>  The page's Controller does three
 * things in order: it calls changeLocks() with confirmed = false; if the answer has a message
 * it shows the message and stops; otherwise it asks confirmationQuestion() and, only on Yes,
 * calls changeLocks() again with confirmed = true.  The flow tests make the same calls in the
 * same order, so they prove the Model supports the page.  That the buttons and the dialog
 * really make those calls can only be seen on the screen, and is covered by the manual tests
 * M-CL1 to M-CL6 in Manual Tests.pdf.</p>
 *
 * <p> <b>Test data.</b>  The tests use the application's real database.  Every lesson a test
 * creates is removed after that test, even when the test fails.  FoundationsMain must not be
 * running, because H2 allows one connection.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-13 TP3 Aspect 2: tests written before the lock page code
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class CuratorLockPageTests {

	/** A description longer than the 10-character minimum. */
	private static final String DESCRIPTION = "Use GitHub Actions to build and test every push.";

	/** The lesson title used in the confirmation questions (Initial ConOps scenario S1). */
	private static final String TITLE = "Set up CI";

	/** A Curator only (the Role2 flag). */
	private static final User CURATOR = new User("tp3lockcurator", "", "", "", "", "", "",
			false, false, true);

	/** The Contributor who owns the test lessons; unique for every run. */
	private static final String OWNER_NAME = "tp3lockowner" + System.currentTimeMillis();

	/** The owner as a User: a Contributor who is not a Curator. */
	private static final User OWNER = new User(OWNER_NAME, "", "", "", "", "", "",
			false, true, false);

	/** Every lesson id created by the current test, deleted after the test. */
	private static final List<Integer> createdLessons = new ArrayList<Integer>();

	/** The application's database. */
	private static final Database db = FoundationsMain.database;

	/**********
	 * Default constructor is not used; JUnit creates an instance for each test.
	 */
	public CuratorLockPageTests() {
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
		// The same real database the application uses, as in CuratorLockTests
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
	 * <p> Method: void deleteCreatedLessons() </p>
	 *
	 * <p> Description: Deletes every lesson the test created, even if the test failed. </p>
	 */
	@AfterEach
	void deleteCreatedLessons() {
		// Deleted through the Database, because the Contributor Model correctly refuses to
		// delete a locked lesson and most of these tests leave one locked
		for (int id : createdLessons) db.deleteLesson(id);
		createdLessons.clear();
	}


	/*-*******************************************************************************************

	Helper methods

	 */

	/**********
	 * <p> Method: int makeLesson() </p>
	 *
	 * <p> Description: Creates the lesson "Set up CI" exactly as a Contributor would, with
	 * nothing locked, and remembers it for cleanup. </p>
	 *
	 * @return the new lesson's id
	 */
	private static int makeLesson() {
		// Creating through the real Contributor Model guarantees a valid TP2 lesson
		LessonResult result = ModelCreateLesson.createLesson(OWNER_NAME, TITLE, DESCRIPTION,
				"ci", "Technical");
		if (!result.isSuccess())
			throw new IllegalStateException("Test setup failed: " + result.getMessage());
		createdLessons.add(result.getLessonId());
		return result.getLessonId();
	}

	/**********
	 * <p> Method: String outcome(LessonResult result) </p>
	 *
	 * <p> Description: Describes a result as "Success: message" or "Refused: message".  A
	 * refusal with an empty message, which means "valid, waiting for Yes", is written as
	 * "Ask". </p>
	 *
	 * @param result the result to describe
	 *
	 * @return the description
	 */
	private static String outcome(LessonResult result) {
		// "Ask" makes the two-step convention visible in the printed output
		if (!result.isSuccess() && result.getMessage().isEmpty()) return "Ask";
		return (result.isSuccess() ? "Success: " : "Refused: ") + result.getMessage();
	}

	/**********
	 * <p> Method: String locks(int lessonId) </p>
	 *
	 * <p> Description: What is locked on a lesson as stored in the database, in the words the
	 * Curator Home Page shows. </p>
	 *
	 * @param lessonId the lesson to read
	 *
	 * @return for example "nothing" or "title, tags"
	 */
	private static String locks(int lessonId) {
		// Read back from the database, so the test proves what was really saved
		return ModelCuratorHome.lockSummary(LessonsLearned.read(lessonId));
	}

	/**********
	 * <p> Method: void check(String id, String input, String expected, String actual) </p>
	 *
	 * <p> Description: Prints the Expected and Actual lines for one check and then asserts that
	 * they are identical. </p>
	 *
	 * @param id the test id, such as "LP-01"
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

	LR-1: the ticked boxes become field names

	 */

	/**********
	 * <p> Method: void lp01_twoBoxesTicked() </p>
	 *
	 * <p> Description: LP-01, positive.  Ticking Title and Tags gives exactly those two names
	 * (scenario S1; LR-1). </p>
	 */
	@Test
	@DisplayName("LP-01 Title and Tags ticked give title and tags (P)")
	void lp01_twoBoxesTicked() {
		check("LP-01", "chosenFields(entire F, title T, description F, tags T, category F)",
				"[title, tags]",
				ModelCuratorLock.chosenFields(false, true, false, true, false).toString());
	}

	/**********
	 * <p> Method: void lp02_allBoxesTicked() </p>
	 *
	 * <p> Description: LP-02, boundary.  All five boxes give all five names in the fixed order
	 * (LR-1). </p>
	 */
	@Test
	@DisplayName("LP-02 All five boxes give all five names in the fixed order (P)")
	void lp02_allBoxesTicked() {
		check("LP-02", "chosenFields(T, T, T, T, T)",
				"[entire, title, description, tags, category]",
				ModelCuratorLock.chosenFields(true, true, true, true, true).toString());
	}

	/**********
	 * <p> Method: void lp03_noBoxTicked() </p>
	 *
	 * <p> Description: LP-03, negative.  No box ticked gives an empty list, and the lock action
	 * refuses an empty list with its specific message, so pressing Lock with nothing ticked
	 * changes nothing (scenario S4; LR-1). </p>
	 */
	@Test
	@DisplayName("LP-03 No box ticked gives an empty list that the lock action refuses (N)")
	void lp03_noBoxTicked() {
		int id = makeLesson();
		List<String> fields = ModelCuratorLock.chosenFields(false, false, false, false, false);
		LessonResult result = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, false);
		check("LP-03", "chosenFields(F, F, F, F, F), then Lock",
				"[] | Refused: Select the whole lesson or at least one field to lock. "
				+ "| locked: nothing",
				fields + " | " + outcome(result) + " | locked: " + locks(id));
	}


	/*-*******************************************************************************************

	LR-2: the wording of the confirmation question

	 */

	/**********
	 * <p> Method: void lp04_questionForOneField() </p>
	 *
	 * <p> Description: LP-04, positive.  One field (LR-2). </p>
	 */
	@Test
	@DisplayName("LP-04 The question for one field (P)")
	void lp04_questionForOneField() {
		check("LP-04", "confirmationQuestion(\"Set up CI\", [title], lock)",
				"Lock title of 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("title"), true));
	}

	/**********
	 * <p> Method: void lp05_questionForTwoFields() </p>
	 *
	 * <p> Description: LP-05, positive.  Two fields are joined with "and".  This is the exact
	 * question in step 3 of Initial ConOps scenario S1 (LR-2). </p>
	 */
	@Test
	@DisplayName("LP-05 The question for two fields is the ConOps S1 question (P)")
	void lp05_questionForTwoFields() {
		check("LP-05", "confirmationQuestion(\"Set up CI\", [title, tags], lock)",
				"Lock title and tags of 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("title", "tags"),
						true));
	}

	/**********
	 * <p> Method: void lp06_questionForSeveralFields() </p>
	 *
	 * <p> Description: LP-06, positive.  Three or more fields are listed with commas and a
	 * final "and" (LR-2). </p>
	 */
	@Test
	@DisplayName("LP-06 The question for four fields (P)")
	void lp06_questionForSeveralFields() {
		check("LP-06", "confirmationQuestion(\"Set up CI\", [title, description, tags, "
				+ "category], lock)",
				"Lock title, description, tags, and category of 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE,
						Arrays.asList("title", "description", "tags", "category"), true));
	}

	/**********
	 * <p> Method: void lp07_questionForTheWholeLesson() </p>
	 *
	 * <p> Description: LP-07, positive.  The whole lesson alone reads as a sentence about the
	 * lesson, not about a field "of" it (scenario S2; LR-2). </p>
	 */
	@Test
	@DisplayName("LP-07 The question for the whole lesson (P)")
	void lp07_questionForTheWholeLesson() {
		check("LP-07", "confirmationQuestion(\"Set up CI\", [entire], lock)",
				"Lock the entire lesson 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("entire"), true));
	}

	/**********
	 * <p> Method: void lp08_questionForTheWholeLessonAndAField() </p>
	 *
	 * <p> Description: LP-08, positive.  The whole lesson together with a field names both, the
	 * whole lesson first (LR-2). </p>
	 */
	@Test
	@DisplayName("LP-08 The question for the whole lesson and a field (P)")
	void lp08_questionForTheWholeLessonAndAField() {
		check("LP-08", "confirmationQuestion(\"Set up CI\", [entire, title], lock)",
				"Lock the entire lesson and title of 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("entire", "title"),
						true));
	}

	/**********
	 * <p> Method: void lp09_questionForUnlock() </p>
	 *
	 * <p> Description: LP-09, positive.  Unlocking uses the word "Unlock" (scenario S3;
	 * LR-2). </p>
	 */
	@Test
	@DisplayName("LP-09 The question for unlocking (P)")
	void lp09_questionForUnlock() {
		check("LP-09", "confirmationQuestion(\"Set up CI\", [category], unlock) / "
				+ "([entire], unlock)",
				"Unlock category of 'Set up CI'? / Unlock the entire lesson 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("category"), false)
				+ " / " + ModelCuratorLock.confirmationQuestion(TITLE, Arrays.asList("entire"),
						false));
	}

	/**********
	 * <p> Method: void lp10_questionUsesTheFixedOrder() </p>
	 *
	 * <p> Description: LP-10, positive.  The question names each part once, in the fixed order,
	 * however the list was built, so it always matches the "Locked: ..." message that follows
	 * (LR-2). </p>
	 */
	@Test
	@DisplayName("LP-10 The question names each part once, in the fixed order (P)")
	void lp10_questionUsesTheFixedOrder() {
		check("LP-10", "confirmationQuestion(\"Set up CI\", [tags, title, tags], lock)",
				"Lock title and tags of 'Set up CI'?",
				ModelCuratorLock.confirmationQuestion(TITLE,
						Arrays.asList("tags", "title", "tags"), true));
	}

	/**********
	 * <p> Method: void lp11_noQuestionWithoutAChoice() </p>
	 *
	 * <p> Description: LP-11, negative.  With nothing to ask about there is no question: an
	 * empty list, a missing list, and a list holding only an unknown name each give an empty
	 * String, and none of them crashes (CWE-476; LR-2). </p>
	 */
	@Test
	@DisplayName("LP-11 An empty, missing, or unknown choice gives no question (N)")
	void lp11_noQuestionWithoutAChoice() {
		check("LP-11", "confirmationQuestion with [] / null / [summary]", "'' / '' / ''",
				"'" + ModelCuratorLock.confirmationQuestion(TITLE, new ArrayList<String>(), true)
				+ "' / '" + ModelCuratorLock.confirmationQuestion(TITLE, null, true)
				+ "' / '" + ModelCuratorLock.confirmationQuestion(TITLE,
						Arrays.asList("summary"), true) + "'");
	}


	/*-*******************************************************************************************

	LR-3: the page asks first and saves only after Yes

	 */

	/**********
	 * <p> Method: void lp12_lockFlowOfScenarioS1() </p>
	 *
	 * <p> Description: LP-12, positive.  Initial ConOps scenario S1 from the page's side: Title
	 * and Tags are ticked, the first call answers "ask" and saves nothing, the question is the
	 * S1 question, the call after Yes saves both flags, and the Curator Home Page then shows
	 * the lesson as locked (US-CL1; LR-3). </p>
	 */
	@Test
	@DisplayName("LP-12 Scenario S1: tick, ask, Yes, saved, shown as locked (P)")
	void lp12_lockFlowOfScenarioS1() {
		int id = makeLesson();
		List<String> fields = ModelCuratorLock.chosenFields(false, true, false, true, false);
		LessonResult ask = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, false);
		String afterAsk = locks(id);
		String question = ModelCuratorLock.confirmationQuestion(TITLE, fields, true);
		LessonResult yes = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, true);
		String row = ModelCuratorHome.rowText(LessonsLearned.read(id));
		check("LP-12", "Title and Tags ticked, Lock, Yes",
				"Ask | locked: nothing | Lock title and tags of 'Set up CI'? | "
				+ "Success: Locked: title, tags. | locked: title, tags | row ends (locked) true",
				outcome(ask) + " | locked: " + afterAsk + " | " + question + " | "
				+ outcome(yes) + " | locked: " + locks(id) + " | row ends (locked) "
				+ row.endsWith("(locked)"));
	}

	/**********
	 * <p> Method: void lp13_unlockFlowOfScenarioS3() </p>
	 *
	 * <p> Description: LP-13, positive.  Scenario S3 from the page's side: with the title and
	 * category locked, ticking Category and choosing Unlock clears that one flag and leaves the
	 * title locked (US-CL2; LR-3). </p>
	 */
	@Test
	@DisplayName("LP-13 Scenario S3: unlock one field, the other stays locked (P)")
	void lp13_unlockFlowOfScenarioS3() {
		int id = makeLesson();
		db.setLessonLocks(id, Arrays.asList("title", "category"), true);
		List<String> fields = ModelCuratorLock.chosenFields(false, false, false, false, true);
		LessonResult ask = ModelCuratorLock.changeLocks(CURATOR, id, fields, false, false);
		String question = ModelCuratorLock.confirmationQuestion(TITLE, fields, false);
		LessonResult yes = ModelCuratorLock.changeLocks(CURATOR, id, fields, false, true);
		check("LP-13", "title and category locked; Category ticked, Unlock, Yes",
				"Ask | Unlock category of 'Set up CI'? | Success: Unlocked: category. "
				+ "| locked: title",
				outcome(ask) + " | " + question + " | " + outcome(yes) + " | locked: "
				+ locks(id));
	}

	/**********
	 * <p> Method: void lp17_lockFlowOfScenarioS2() </p>
	 *
	 * <p> Description: LP-17, positive.  Scenario S2 from the page's side: Entire lesson is
	 * ticked and confirmed, and the message is "Locked: entire lesson." (US-CL1; LR-3). </p>
	 */
	@Test
	@DisplayName("LP-17 Scenario S2: lock the whole lesson (P)")
	void lp17_lockFlowOfScenarioS2() {
		int id = makeLesson();
		List<String> fields = ModelCuratorLock.chosenFields(true, false, false, false, false);
		LessonResult ask = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, false);
		String question = ModelCuratorLock.confirmationQuestion(TITLE, fields, true);
		LessonResult yes = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, true);
		check("LP-17", "Entire lesson ticked, Lock, Yes",
				"Ask | Lock the entire lesson 'Set up CI'? | Success: Locked: entire lesson. "
				+ "| locked: entire lesson",
				outcome(ask) + " | " + question + " | " + outcome(yes) + " | locked: "
				+ locks(id));
	}


	/*-*******************************************************************************************

	LR-4: No saves nothing, and refusals come before any question

	 */

	/**********
	 * <p> Method: void lp14_answeringNoSavesNothing() </p>
	 *
	 * <p> Description: LP-14, negative.  Scenario S4: the Curator ticks a box, is asked, and
	 * answers No, so the page never makes the confirmed call.  Nothing is locked, and the page
	 * has a message to say so (US-CL-V; LR-4). </p>
	 */
	@Test
	@DisplayName("LP-14 Scenario S4: answering No saves nothing (N)")
	void lp14_answeringNoSavesNothing() {
		int id = makeLesson();
		List<String> fields = ModelCuratorLock.chosenFields(false, true, false, false, false);
		LessonResult ask = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, false);
		// "No": the page stops here and shows ModelCuratorLock.NO_CHANGE
		check("LP-14", "Title ticked, Lock, No",
				"Ask | locked: nothing | No change was made.",
				outcome(ask) + " | locked: " + locks(id) + " | " + ModelCuratorLock.NO_CHANGE);
	}

	/**********
	 * <p> Method: void lp15_lessonGoneIsRefusedBeforeAsking() </p>
	 *
	 * <p> Description: LP-15, negative.  Scenario S5: the lesson was deleted after the page
	 * opened.  The first call answers "Lesson not found.", so the page shows that and never
	 * asks "Are you sure?" about something that cannot happen (US-CL-V; LR-4). </p>
	 */
	@Test
	@DisplayName("LP-15 Scenario S5: a deleted lesson is refused before any question (N)")
	void lp15_lessonGoneIsRefusedBeforeAsking() {
		int id = makeLesson();
		db.deleteLesson(id);
		List<String> fields = ModelCuratorLock.chosenFields(false, true, false, false, false);
		LessonResult ask = ModelCuratorLock.changeLocks(CURATOR, id, fields, true, false);
		check("LP-15", "lesson deleted; Title ticked, Lock",
				"Refused: " + LessonAccess.NOT_FOUND, outcome(ask));
	}

	/**********
	 * <p> Method: void lp16_nonCuratorIsRefusedBeforeAsking() </p>
	 *
	 * <p> Description: LP-16, negative.  Scenario S5: a user who is not a Curator reaches the
	 * lock action.  The first call refuses, nothing is asked, and nothing is saved (US-CL-V;
	 * LR-4). </p>
	 */
	@Test
	@DisplayName("LP-16 Scenario S5: a non-Curator is refused before any question (N)")
	void lp16_nonCuratorIsRefusedBeforeAsking() {
		int id = makeLesson();
		List<String> fields = ModelCuratorLock.chosenFields(false, true, false, false, false);
		LessonResult ask = ModelCuratorLock.changeLocks(OWNER, id, fields, true, false);
		check("LP-16", "the lesson's own Contributor; Title ticked, Lock",
				"Refused: " + ModelCuratorLock.NOT_CURATOR + " | locked: nothing",
				outcome(ask) + " | locked: " + locks(id));
	}


	/*-*******************************************************************************************

	LR-5: opening the page for one lesson

	 */

	/**********
	 * <p> Method: void lp18_pageOpensForACurator() </p>
	 *
	 * <p> Description: LP-18, positive.  A Curator opens the page for a lesson another user
	 * owns and receives that lesson with its stored locks, so the page can show what is locked
	 * now (US-CL1; LR-5). </p>
	 */
	@Test
	@DisplayName("LP-18 The page opens for a Curator and carries the stored locks (P)")
	void lp18_pageOpensForACurator() {
		int id = makeLesson();
		db.setLessonLocks(id, Arrays.asList("description"), true);
		LessonResult result = ModelCuratorLock.lessonToLock(CURATOR, id);
		LessonsLearned lesson = result.getLesson();
		check("LP-18", "Curator; a Contributor's lesson with the description locked",
				"Success | title Set up CI | owner " + OWNER_NAME + " | locked: description",
				(result.isSuccess() ? "Success" : "Refused") + " | title " + lesson.getTitle()
				+ " | owner " + lesson.getOwnerUserName() + " | locked: "
				+ ModelCuratorHome.lockSummary(lesson));
	}

	/**********
	 * <p> Method: void lp19_pageRefusesANonCurator() </p>
	 *
	 * <p> Description: LP-19, negative.  A user who is not a Curator, and no user at all, are
	 * refused with the lock message and receive no lesson.  The same answer is given for a
	 * lesson that does not exist, so the refusal reveals nothing (CWE-200, CWE-476; US-CL-V;
	 * LR-5). </p>
	 */
	@Test
	@DisplayName("LP-19 The page refuses a non-Curator before looking up the lesson (N)")
	void lp19_pageRefusesANonCurator() {
		int id = makeLesson();
		int gone = makeLesson();
		db.deleteLesson(gone);
		LessonResult owner = ModelCuratorLock.lessonToLock(OWNER, id);
		LessonResult ownerGone = ModelCuratorLock.lessonToLock(OWNER, gone);
		LessonResult nobody = ModelCuratorLock.lessonToLock(null, id);
		String refused = "Refused: " + ModelCuratorLock.NOT_CURATOR + " | lesson null";
		check("LP-19", "Contributor, own lesson / Contributor, deleted lesson / null user",
				refused + " / " + refused + " / " + refused,
				outcome(owner) + " | lesson " + owner.getLesson() + " / " + outcome(ownerGone)
				+ " | lesson " + ownerGone.getLesson() + " / " + outcome(nobody) + " | lesson "
				+ nobody.getLesson());
	}

	/**********
	 * <p> Method: void lp20_pageRefusesAMissingLesson() </p>
	 *
	 * <p> Description: LP-20, negative.  A Curator opening the page for a lesson that has been
	 * deleted receives "Lesson not found." and no lesson (scenario S5; US-CL-V; LR-5). </p>
	 */
	@Test
	@DisplayName("LP-20 The page refuses a deleted lesson (N)")
	void lp20_pageRefusesAMissingLesson() {
		int id = makeLesson();
		db.deleteLesson(id);
		LessonResult result = ModelCuratorLock.lessonToLock(CURATOR, id);
		check("LP-20", "Curator; a lesson that was deleted",
				"Refused: " + LessonAccess.NOT_FOUND + " | lesson null",
				outcome(result) + " | lesson " + result.getLesson());
	}
}
