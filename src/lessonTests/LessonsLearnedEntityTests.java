package lessonTests;

import static lessonTests.TestSupport.DESCRIPTION;
import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.remember;
import static lessonTests.TestSupport.rep;
import static lessonTests.TestSupport.requirement;
import static lessonTests.TestSupport.run;
import static lessonTests.TestSupport.stored;
import static lessonTests.TestSupport.uniqueUser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;

/*******
 * <p> Title: LessonsLearnedEntityTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the LessonsLearned entity class itself (TP2 Task
 * 3), called directly rather than through a page's Model.  The requirements are:</p>
 * <ul>
 * <li> E1: LessonsLearned fully implements CRUD against the database (create, read, update,
 * 		delete). </li>
 * <li> E2: LessonsLearned performs input validation and refuses to save invalid data, even when
 * 		a caller forgets to validate first. </li>
 * <li> E5: The lock flags a Curator sets are saved and reported per field and for the whole
 * 		lesson (needed by US-R3, US-U1-V, and US-D1-V). </li>
 * <li> E6: The attributes added for the TP3 epics and the AICoaching fields (root cause,
 * 		recommendation, AI-assisted flag, search terms, curator fields) are saved, read back, and
 * 		validated. </li>
 * </ul>
 *
 * <p> A failed save in these tests surfaces as an exception, because that is how the entity
 * reports it; each test catches the exception and prints its message so the output shows the
 * exact text.  See TestSupport for how to read the output.</p>
 *
 * <p> <b>Running the tests.</b>  These are JUnit 5 tests: run the class, or the whole
 * lessonTests package, with Run As &gt; JUnit Test.  Each test's display name is its TP2 id, its
 * name, its requirement, and P or N, for example "C1-P1 Create Lesson - Valid Input (US-C1, P)",
 * and the tests run in the order of the Requirements-Test Traceability list.  The same tests
 * still run as a console program through main() and AllContributorTests, with the same output
 * as in TP2.  FoundationsMain must not be running.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2 Task 6: entity tests for LessonsLearned
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LessonsLearnedEntityTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public LessonsLearnedEntityTests() {
	}

	/**********
	 * Runs the LessonsLearned entity tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Entity Tests - LessonsLearned (E1, E2, E5, E6)",
				LessonsLearnedEntityTests::runAll);
	}

	/**********
	 * <p> Method: void connectForJUnit() </p>
	 *
	 * <p> Description: Opens the database once before the first JUnit test of this class.
	 * FoundationsMain must not be running, because H2 allows one connection. </p>
	 */
	@BeforeAll
	static void connectForJUnit() {
		TestSupport.startJUnit();
	}

	/**********
	 * <p> Method: void cleanUpForJUnit() </p>
	 *
	 * <p> Description: After the last JUnit test of this class, deletes every lesson the tests
	 * created and closes the database. </p>
	 */
	@AfterAll
	static void cleanUpForJUnit() {
		TestSupport.finishJUnit();
	}

	/**********
	 * Runs every test in this class, grouped under the requirement each one demonstrates.
	 */
	public static void runAll() {
		LessonsLearnedEntityTests tests = new LessonsLearnedEntityTests();
		requirement("E1", "LessonsLearned implements Create, Read, Update, and Delete in the "
				+ "database");
		run(tests::testCreateAndReadLesson);
		run(tests::testUpdateAndDeleteLesson);
		run(tests::testReadOrDeleteMissingLesson);

		requirement("E2", "LessonsLearned validates its fields and refuses to store invalid data");
		run(tests::testValidationReportsFirstInvalidFieldInScreenOrder);
		run(tests::testCreateRefusesInvalidLesson);
		run(tests::testUpdateRefusesInvalidChange);

		requirement("E5", "Lock flags are saved and reported for each field and the whole lesson");
		run(tests::testFieldLockSavedAndReported);
		run(tests::testEntireLockCoversEveryFieldAndCanBeCleared);
		run(tests::testUnknownLockScopeRefused);

		requirement("E6", "TP3 and AICoaching attributes are saved, read back, and validated");
		run(tests::testFutureAttributesSavedAndReadBack);
		run(tests::testRootCauseTooLongRefused);
		run(tests::testSearchTermsTooLongRefused);
	}


	/**********
	 * Creates and saves a valid lesson directly through the entity, remembering it for clean-up.
	 *
	 * @param owner	the owner's username
	 * @param title	the title
	 * @return the saved lesson, with its new id
	 */
	private static LessonsLearned saveLesson(String owner, String title) {
		LessonsLearned lesson = new LessonsLearned(owner, title, DESCRIPTION, "java", "General");
		try {
			remember(lesson.create());
		} catch (SQLException e) {
			throw new IllegalStateException("Test setup failed: " + e.getMessage());
		}
		return lesson;
	}


	/*-*******************************************************************************************

	E1: CRUD

	 */

	/**********
	 * <p> Test E1-P1 (Positive): Create Stores the Lesson and Read Returns It. </p>
	 *
	 * <p> <b>Requirement:</b> E1 (Create and Read), the basis of US-C1 and US-R3. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> create() validates the lesson and inserts it with
	 * Database.createLesson, storing the generated id on the object; the static read(id) loads
	 * the row with Database.getLessonById. </p>
	 *
	 * <p> <b>Steps:</b> build a lesson, call create(), then read(id). </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means an id was assigned and the row read back
	 * holds the same title, category, tags, and owner. </p>
	 *
	 * <p> Validates: LessonsLearned.read(), LessonsLearned.getId(),
	 * LessonsLearned.getOwnerUserName(), LessonsLearned.create(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("E1-P1 Create Stores the Lesson and Read Returns It (E1, P)")
	void testCreateAndReadLesson() {
		String owner = uniqueUser("entityCreate");
		LessonsLearned lesson = saveLesson(owner, "Entity title");
		LessonsLearned readBack = LessonsLearned.read(lesson.getId());

		String actual = "id assigned: " + (lesson.getId() > 0 ? "yes" : "no") + "; read back: "
				+ stored(lesson.getId()) + "; owner "
				+ (readBack != null && owner.equals(readBack.getOwnerUserName()) ? "matches"
						: "does not match");
		check("E1-P1", true, "Create Stores the Lesson and Read Returns It",
				"id assigned: yes; read back: Entity title | General | tags: java; owner matches",
				actual);
	}


	/**********
	 * <p> Test E1-P2 (Positive): Update Saves Changes and Delete Removes the Row. </p>
	 *
	 * <p> <b>Requirement:</b> E1 (Update and Delete), the basis of US-U1 and US-D1. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> update() validates and writes every field with
	 * Database.updateLesson; delete() removes the row with Database.deleteLesson.  Both return
	 * whether a row was affected. </p>
	 *
	 * <p> <b>Steps:</b> change the title and call update(); read it back; call delete(); read
	 * again. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means update() returned true and the new title is
	 * stored, then delete() returned true and the lesson can no longer be read. </p>
	 *
	 * <p> Validates: LessonsLearned.setTitle(), LessonsLearned.update(),
	 * LessonsLearned.getId(), LessonsLearned.delete(), LessonsLearned.create(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("E1-P2 Update Saves Changes and Delete Removes the Row (E1, P)")
	void testUpdateAndDeleteLesson() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityUpdate"), "Before update");
		lesson.setTitle("After update");
		boolean updated = lesson.update();
		String afterUpdate = stored(lesson.getId());
		boolean deleted = lesson.delete();

		check("E1-P2", true, "Update Saves Changes and Delete Removes the Row",
				"update: true; stored: After update | General | tags: java; delete: true; "
				+ "stored: no lesson stored with that id",
				"update: " + updated + "; stored: " + afterUpdate + "; delete: " + deleted
				+ "; stored: " + stored(lesson.getId()));
	}


	/**********
	 * <p> Test E1-N1 (Negative): Reading or Deleting a Missing Lesson Fails Safely. </p>
	 *
	 * <p> <b>Requirement:</b> E1, with US-R3-V: asking for a lesson that does not exist must
	 * not throw an exception, so the Model can report "Lesson not found.". </p>
	 *
	 * <p> <b>How the code satisfies it:</b> read() returns null when no row matches, and
	 * delete() returns false when no row was removed. </p>
	 *
	 * <p> <b>Steps:</b> read id -999; delete a lesson object whose id is -999. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means read gives null and delete gives
	 * false. </p>
	 *
	 * <p> Validates: LessonsLearned.setId(), LessonsLearned.read(), LessonsLearned.delete(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("E1-N1 Reading or Deleting a Missing Lesson Fails Safely (E1, N)")
	void testReadOrDeleteMissingLesson() {
		LessonsLearned ghost = new LessonsLearned();
		ghost.setId(-999);
		check("E1-N1", false, "Reading or Deleting a Missing Lesson Fails Safely",
				"read: null; delete: false",
				"read: " + (LessonsLearned.read(-999) == null ? "null" : "a lesson")
				+ "; delete: " + ghost.delete());
	}


	/*-*******************************************************************************************

	E2: validation inside the entity

	 */

	/**********
	 * <p> Test E2-P1 (Positive): Validation Reports the First Invalid Field in Screen
	 * Order. </p>
	 *
	 * <p> <b>Requirement:</b> E2, the entity tells the caller which field is wrong and why, one
	 * field at a time from the top of the screen, and reports a fully valid lesson as
	 * valid. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> findInvalidField() checks title, description,
	 * tags, category, and then the TP3 fields in that order; validate() returns that field's
	 * message, or an empty String when everything is valid. </p>
	 *
	 * <p> <b>Steps:</b> start with an empty title and the category "Marketing"; fix the title;
	 * then fix the category. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the title is reported first, then the
	 * category, and finally the lesson is valid. </p>
	 *
	 * <p> Validates: LessonsLearned.findInvalidField(), LessonsLearned.validate(),
	 * LessonsLearned.setTitle(), LessonsLearned.setCategory(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("E2-P1 Validation Reports the First Invalid Field in Screen Order (E2, P)")
	void testValidationReportsFirstInvalidFieldInScreenOrder() {
		LessonsLearned lesson = new LessonsLearned("anyone", "", DESCRIPTION, "", "Marketing");
		String first = lesson.findInvalidField() + " \"" + lesson.validate() + "\"";
		lesson.setTitle("Fixed title");
		String second = lesson.findInvalidField() + " \"" + lesson.validate() + "\"";
		lesson.setCategory("General");
		String third = lesson.validate().isEmpty() ? "valid" : lesson.validate();

		check("E2-P1", true, "Validation Reports the First Invalid Field in Screen Order",
				"first: title \"Title is required.\"; after fixing title: category \"Please choose "
				+ "a valid category.\"; after fixing category: valid",
				"first: " + first + "; after fixing title: " + second + "; after fixing category: "
				+ third);
	}


	/**********
	 * <p> Test E2-N1 (Negative): Create Refuses an Invalid Lesson. </p>
	 *
	 * <p> <b>Requirement:</b> E2 and US-C1-V, invalid data can never be stored, even if a
	 * caller skips validation. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> create() calls validate() first and throws an
	 * IllegalArgumentException carrying the message instead of inserting. </p>
	 *
	 * <p> <b>Steps:</b> call create() on a lesson with an empty title; count the owner's
	 * lessons. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the exception carries "Title is required."
	 * and 0 lessons were stored. </p>
	 *
	 * <p> Validates: LessonsLearned.create(), LessonsLearnedList.loadByOwner(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("E2-N1 Create Refuses an Invalid Lesson (E2, N)")
	void testCreateRefusesInvalidLesson() {
		String owner = uniqueUser("entityBadCreate");
		LessonsLearned lesson = new LessonsLearned(owner, "", DESCRIPTION, "java", "General");
		String outcome;
		try {
			remember(lesson.create());
			outcome = "saved";
		} catch (IllegalArgumentException e) {
			outcome = "refused \"" + e.getMessage() + "\"";
		} catch (SQLException e) {
			outcome = "database error " + e.getMessage();
		}
		check("E2-N1", false, "Create Refuses an Invalid Lesson",
				"refused \"Title is required.\"; 0 lesson(s) stored for the owner",
				outcome + "; " + LessonsLearnedList.loadByOwner(owner).size()
				+ " lesson(s) stored for the owner");
	}


	/**********
	 * <p> Test E2-N2 (Negative): Update Refuses an Invalid Change. </p>
	 *
	 * <p> <b>Requirement:</b> E2 and US-U1-V, an invalid change is refused and the stored
	 * lesson is left unchanged. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> update() calls validate() first and throws instead
	 * of writing. </p>
	 *
	 * <p> <b>Steps:</b> set a 5-character description on a saved lesson and call update(); read
	 * the stored description. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the description-length message is in the
	 * exception and the stored description is still the original. </p>
	 *
	 * <p> Validates: LessonsLearned.setDescription(), LessonsLearned.update(),
	 * LessonsLearned.read(), LessonsLearned.getId(), LessonsLearned.getDescription(),
	 * LessonsLearned.create(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("E2-N2 Update Refuses an Invalid Change (E2, N)")
	void testUpdateRefusesInvalidChange() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityBadUpdate"), "Valid lesson");
		lesson.setDescription("short");
		String outcome;
		try {
			outcome = "update returned " + lesson.update();
		} catch (IllegalArgumentException e) {
			outcome = "refused \"" + e.getMessage() + "\"";
		}
		check("E2-N2", false, "Update Refuses an Invalid Change",
				"refused \"Description must be between 10 and 5000 characters.\"; stored "
				+ "description: " + DESCRIPTION,
				outcome + "; stored description: "
				+ LessonsLearned.read(lesson.getId()).getDescription());
	}


	/*-*******************************************************************************************

	E5: locks

	 */

	/**********
	 * <p> Test E5-P1 (Positive): A Field Lock Is Saved and Reported. </p>
	 *
	 * <p> <b>Requirement:</b> E5, a lock on one field is stored and reported for that field
	 * only, while the lesson as a whole counts as locked (US-R3, US-U1-V, US-D1-V). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> setLock(scope, locked) saves the flag through
	 * Database.setLessonLock and updates the object; isFieldLocked() and isLocked() report
	 * the flags. </p>
	 *
	 * <p> <b>Steps:</b> setLock("title", true), then read the lesson fresh from the
	 * database. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means setLock returned true and the stored lesson
	 * reports the title locked, the description unlocked, and the lesson locked. </p>
	 *
	 * <p> Validates: LessonsLearned.setLock(), LessonsLearned.read(), LessonsLearned.getId(),
	 * LessonsLearned.isFieldLocked(), LessonsLearned.isLocked(), LessonsLearned.create(). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("E5-P1 A Field Lock Is Saved and Reported (E5, P)")
	void testFieldLockSavedAndReported() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityLock"), "Lesson to lock");
		boolean set = lesson.setLock(LessonsLearned.FIELD_TITLE, true);
		LessonsLearned fresh = LessonsLearned.read(lesson.getId());
		check("E5-P1", true, "A Field Lock Is Saved and Reported",
				"setLock: true; title locked: true; description locked: false; lesson locked: true",
				"setLock: " + set + "; title locked: " + fresh.isFieldLocked("title")
				+ "; description locked: " + fresh.isFieldLocked("description")
				+ "; lesson locked: " + fresh.isLocked());
	}


	/**********
	 * <p> Test E5-P2 (Positive): The Whole-Lesson Lock Covers Every Field and Can Be
	 * Cleared. </p>
	 *
	 * <p> <b>Requirement:</b> E5, locking the entire lesson locks every field, and a lock can
	 * be removed again (as the Phase Three Curator will need). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> isFieldLocked() returns true for every field when
	 * lockedEntire is set; setLock(LOCK_ENTIRE, false) clears it. </p>
	 *
	 * <p> <b>Steps:</b> lock "entire", read fresh and check all four fields; unlock and read
	 * again. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means 4 of 4 fields are locked while the lock is
	 * set, and the lesson is unlocked after clearing it. </p>
	 *
	 * <p> Validates: LessonsLearned.setLock(), LessonsLearned.read(), LessonsLearned.getId(),
	 * LessonsLearned.isFieldLocked(), LessonsLearned.isLocked(), LessonsLearned.create(). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("E5-P2 The Whole-Lesson Lock Covers Every Field and Can Be Cleared (E5, P)")
	void testEntireLockCoversEveryFieldAndCanBeCleared() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityEntire"), "Lesson to lock fully");
		lesson.setLock(LessonsLearned.LOCK_ENTIRE, true);
		LessonsLearned locked = LessonsLearned.read(lesson.getId());
		int count = 0;
		for (String field : new String[] { "title", "description", "tags", "category" })
			if (locked.isFieldLocked(field)) count++;
		lesson.setLock(LessonsLearned.LOCK_ENTIRE, false);

		check("E5-P2", true, "The Whole-Lesson Lock Covers Every Field and Can Be Cleared",
				"while locked: 4 of 4 fields locked; after clearing: lesson locked: false",
				"while locked: " + count + " of 4 fields locked; after clearing: lesson locked: "
				+ LessonsLearned.read(lesson.getId()).isLocked());
	}


	/**********
	 * <p> Test E5-N1 (Negative): An Unknown Lock Scope Is Refused. </p>
	 *
	 * <p> <b>Requirement:</b> E5, only the four core fields and the whole lesson can be
	 * locked; anything else is refused without changing the lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> Database.setLessonLock maps the scope to a column
	 * with a fixed switch and returns false for anything else, so no unexpected text ever
	 * reaches the SQL statement. </p>
	 *
	 * <p> <b>Steps:</b> setLock("owner", true); read the lesson fresh. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means setLock returned false and the lesson is
	 * not locked. </p>
	 *
	 * <p> Validates: LessonsLearned.setLock(), LessonsLearned.read(), LessonsLearned.getId(),
	 * LessonsLearned.isLocked(), LessonsLearned.create(). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("E5-N1 An Unknown Lock Scope Is Refused (E5, N)")
	void testUnknownLockScopeRefused() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityBadScope"), "Lesson with bad scope");
		boolean set = lesson.setLock("owner", true);
		check("E5-N1", false, "An Unknown Lock Scope Is Refused",
				"setLock: false; lesson locked: false",
				"setLock: " + set + "; lesson locked: "
				+ LessonsLearned.read(lesson.getId()).isLocked());
	}


	/*-*******************************************************************************************

	E6: attributes for TP3 and the AICoaching fields

	 */

	/**********
	 * <p> Test E6-P1 (Positive): TP3 Attributes Are Saved and Read Back. </p>
	 *
	 * <p> <b>Requirement:</b> E6 (TP2 deliverable 3.3), the class holds the attributes the TP3
	 * epics and the AICoaching fields will need: root cause, recommendation, an AI-assisted flag,
	 * Curator search terms, and who curated the lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> create() inserts these columns along with the core
	 * fields, and updateCuratorFields() saves the Curator-only fields separately so that a
	 * Contributor's update can never overwrite them. </p>
	 *
	 * <p> <b>Steps:</b> create a lesson with a root cause, recommendation, AI flag, and search
	 * terms; read it back; then set new search terms and a curator and call
	 * updateCuratorFields(); read again. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means every value read back equals the value
	 * saved. </p>
	 *
	 * <p> Validates: LessonsLearned.setRootCause(), LessonsLearned.setRecommendation(),
	 * LessonsLearned.setAiAssisted(), LessonsLearned.setSearchTerms(),
	 * LessonsLearned.create(), LessonsLearned.read(), LessonsLearned.getId(),
	 * LessonsLearned.getRootCause(), LessonsLearned.getRecommendation(),
	 * LessonsLearned.isAiAssisted(), LessonsLearned.getSearchTerms(),
	 * LessonsLearned.setCuratedBy(), LessonsLearned.updateCuratorFields(),
	 * LessonsLearned.getCuratedBy(). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("E6-P1 TP3 Attributes Are Saved and Read Back (E6, P)")
	void testFutureAttributesSavedAndReadBack() {
		String owner = uniqueUser("entityFuture");
		LessonsLearned lesson = new LessonsLearned(owner, "Lesson with TP3 fields", DESCRIPTION,
				"java", "Technical");
		lesson.setRootCause("Integration was not planned.");
		lesson.setRecommendation("Schedule integration early.");
		lesson.setAiAssisted(true);
		lesson.setSearchTerms("integration, planning");
		try {
			remember(lesson.create());
		} catch (SQLException e) {
			throw new IllegalStateException("Test setup failed: " + e.getMessage());
		}
		LessonsLearned created = LessonsLearned.read(lesson.getId());
		String afterCreate = created.getRootCause() + " | " + created.getRecommendation() + " | AI: "
				+ created.isAiAssisted() + " | " + created.getSearchTerms();

		lesson.setSearchTerms("ci, release");
		lesson.setCuratedBy("curator1");
		lesson.updateCuratorFields();
		LessonsLearned curated = LessonsLearned.read(lesson.getId());

		check("E6-P1", true, "TP3 Attributes Are Saved and Read Back",
				"after create: Integration was not planned. | Schedule integration early. | AI: "
				+ "true | integration, planning; after curation: ci, release | curator1",
				"after create: " + afterCreate + "; after curation: " + curated.getSearchTerms()
				+ " | " + curated.getCuratedBy());
	}


	/**********
	 * <p> Test E6-N1 (Negative): A Root Cause Over 2000 Characters Is Refused. </p>
	 *
	 * <p> <b>Requirement:</b> E6, the optional coached fields are validated like the core
	 * fields: a root cause may be at most 2000 characters. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> findInvalidField() includes the root cause, using
	 * LessonRecognizer.checkForValidRootCause, so create() refuses the lesson. </p>
	 *
	 * <p> <b>Steps:</b> create a lesson whose root cause is 2001 characters; count the owner's
	 * lessons. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the root-cause message is shown and 0
	 * lessons were stored. </p>
	 *
	 * <p> Validates: LessonsLearned.setRootCause(), LessonsLearned.create(),
	 * LessonsLearnedList.loadByOwner(). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("E6-N1 A Root Cause Over 2000 Characters Is Refused (E6, N)")
	void testRootCauseTooLongRefused() {
		String owner = uniqueUser("entityLongRoot");
		LessonsLearned lesson = new LessonsLearned(owner, "Lesson with a long root cause",
				DESCRIPTION, "java", "General");
		lesson.setRootCause(rep("R", 2001));
		String outcome;
		try {
			remember(lesson.create());
			outcome = "saved";
		} catch (IllegalArgumentException e) {
			outcome = "refused \"" + e.getMessage() + "\"";
		} catch (SQLException e) {
			outcome = "database error " + e.getMessage();
		}
		check("E6-N1", false, "A Root Cause Over 2000 Characters Is Refused",
				"refused \"Root cause must be 2000 characters or fewer.\"; 0 lesson(s) stored for "
				+ "the owner",
				outcome + "; " + LessonsLearnedList.loadByOwner(owner).size()
				+ " lesson(s) stored for the owner");
	}


	/**********
	 * <p> Test E6-N2 (Negative): A Search Term Over 50 Characters Is Refused. </p>
	 *
	 * <p> <b>Requirement:</b> E6, Curator search terms are limited to 50 characters each (and
	 * 500 in total). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateCuratorFields() checks the terms with
	 * LessonRecognizer.checkForValidSearchTerms and throws instead of saving. </p>
	 *
	 * <p> <b>Steps:</b> on a saved lesson, set the terms "ok, " followed by a 51-character term
	 * and call updateCuratorFields(); read the stored terms. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the search-terms message is shown and the
	 * stored search terms are still empty. </p>
	 *
	 * <p> Validates: LessonsLearned.setSearchTerms(), LessonsLearned.updateCuratorFields(),
	 * LessonsLearned.read(), LessonsLearned.getId(), LessonsLearned.getSearchTerms(),
	 * LessonsLearned.create(). </p>
	 */
	@Test
	@Order(12)
	@DisplayName("E6-N2 A Search Term Over 50 Characters Is Refused (E6, N)")
	void testSearchTermsTooLongRefused() {
		LessonsLearned lesson = saveLesson(uniqueUser("entityLongTerm"), "Lesson with long terms");
		lesson.setSearchTerms("ok, " + rep("a", 51));
		String outcome;
		try {
			outcome = "update returned " + lesson.updateCuratorFields();
		} catch (IllegalArgumentException e) {
			outcome = "refused \"" + e.getMessage() + "\"";
		}
		String storedTerms = LessonsLearned.read(lesson.getId()).getSearchTerms();
		check("E6-N2", false, "A Search Term Over 50 Characters Is Refused",
				"refused \"Search terms are too long.\"; stored search terms: (empty)",
				outcome + "; stored search terms: " + (storedTerms.isEmpty() ? "(empty)"
						: storedTerms));
	}
}
