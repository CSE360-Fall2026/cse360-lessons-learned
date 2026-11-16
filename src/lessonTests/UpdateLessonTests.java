package lessonTests;

import static lessonTests.TestSupport.DESCRIPTION;
import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.describe;
import static lessonTests.TestSupport.lock;
import static lessonTests.TestSupport.makeLesson;
import static lessonTests.TestSupport.rep;
import static lessonTests.TestSupport.requirement;
import static lessonTests.TestSupport.run;
import static lessonTests.TestSupport.uniqueUser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import entityClasses.LessonsLearned;
import guiTools.LessonResult;
import guiUpdateLesson.ModelUpdateLesson;

/*******
 * <p> Title: UpdateLessonTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for US-U1 (a Contributor can update the title,
 * description, tags, and category of their lesson as long as the field is not locked) and
 * US-U1-V (a change to a locked field, or invalid new information, is refused with a clear
 * message and the lesson is left unchanged).</p>
 *
 * <p> Every test calls ModelUpdateLesson.updateLesson, the method the Edit Lesson page's
 * Controller calls for "Save Changes", and then reads the lesson back from the database with
 * LessonsLearned.read to prove what is really stored.  Lock flags are set directly, the way
 * the Phase Three Curator will set them, because no Contributor action can lock a field.  See
 * TestSupport for how to read the output.</p>
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
 * @version 1.00		2026-10-16 TP2 Task 6: Update tests, one method per test
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpdateLessonTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public UpdateLessonTests() {
	}

	/**********
	 * Runs the Update tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor Tests - Update Lesson (US-U1, US-U1-V)",
				UpdateLessonTests::runAll);
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
		UpdateLessonTests tests = new UpdateLessonTests();
		requirement("US-U1", "A Contributor can update the core information of their lesson "
				+ "when the field is not locked");
		run(tests::testUpdateValidChangeToUnlockedField);
		run(tests::testUpdateAllFourCoreFields);
		run(tests::testOtherFieldsEditableWhileOneIsLocked);

		requirement("US-U1-V", "A locked field or invalid new value is refused with a clear "
				+ "message; the lesson is unchanged");
		run(tests::testUpdateRejectedBecauseFieldIsLocked);
		run(tests::testUpdateRejectedBecauseNewValueIsInvalid);
		run(tests::testUpdateRejectedBecauseEntireLessonIsLocked);
	}


	/**********
	 * Describes the stored lesson in one line, read fresh from the database.
	 *
	 * @param lessonId	the lesson to read
	 * @return "stored: title | description | tags | category"
	 */
	private static String storedAll(int lessonId) {
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null) return "stored: nothing";
		return "stored: " + lesson.getTitle() + " | " + lesson.getDescription() + " | "
				+ lesson.getTags() + " | " + lesson.getCategory();
	}


	/**********
	 * <p> Test U1-P1 (Positive): Update Lesson - Valid Change to an Unlocked Field. </p>
	 *
	 * <p> <b>Requirement:</b> US-U1, the owner can change an unlocked field, and the change is
	 * saved. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateLesson checks ownership, checks that no
	 * locked field is being changed, validates the new values, and saves them with
	 * LessonsLearned.update() (one UPDATE statement in Database.updateLesson). </p>
	 *
	 * <p> <b>Steps:</b> change the title "Original title" to "Updated title"; read the lesson
	 * back. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means "Lesson updated" was returned and the
	 * database now holds the new title with the other fields unchanged. </p>
	 */
	@Test
	@Order(1)
	@DisplayName("U1-P1 Update Lesson - Valid Change to an Unlocked Field (US-U1, P)")
	void testUpdateValidChangeToUnlockedField() {
		String user = uniqueUser("updateValid");
		int id = makeLesson(user, "Original title", "General");

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "Updated title",
				DESCRIPTION, "java", "General");
		check("U1-P1", true, "Update Lesson - Valid Change to an Unlocked Field",
				"success \"Lesson updated\"; stored: Updated title | " + DESCRIPTION
				+ " | java | General",
				describe(result) + "; " + storedAll(id));
	}


	/**********
	 * <p> Test U1-P2 (Positive): Update Lesson - All Four Core Fields Changed. </p>
	 *
	 * <p> <b>Requirement:</b> US-U1 names all four core fields (title, description, tags, and
	 * category) as updatable. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateLesson sets all four values on the lesson
	 * and Database.updateLesson writes all of them in one statement. </p>
	 *
	 * <p> <b>Steps:</b> change every core field to a new valid value. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means every new value is stored. </p>
	 */
	@Test
	@Order(2)
	@DisplayName("U1-P2 Update Lesson - All Four Core Fields Changed (US-U1, P)")
	void testUpdateAllFourCoreFields() {
		String user = uniqueUser("updateAll");
		int id = makeLesson(user, "Old title", "General");

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "New title",
				"A completely new description.", "release, ci", "Project Management");
		check("U1-P2", true, "Update Lesson - All Four Core Fields Changed",
				"success \"Lesson updated\"; stored: New title | A completely new description. | "
				+ "release, ci | Project Management",
				describe(result) + "; " + storedAll(id));
	}


	/**********
	 * <p> Test U1-P3 (Positive): Update Lesson - Other Fields Stay Editable While One Is
	 * Locked. </p>
	 *
	 * <p> <b>Requirements:</b> US-U1, "as long as the field is not locked": a lock on one field
	 * does not stop the Contributor from changing the others; and US-U1-V, which refuses only a
	 * change to the locked field itself. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateLesson refuses only when a locked field's
	 * value differs from the stored value; an unchanged locked field is allowed through. </p>
	 *
	 * <p> <b>Steps:</b> lock the title; submit the same title with a new description. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the update succeeded, the description
	 * changed, and the locked title is the same as before. </p>
	 */
	@Test
	@Order(3)
	@DisplayName("U1-P3 Update Lesson - Other Fields Stay Editable While One Is Locked (US-U1, P)")
	void testOtherFieldsEditableWhileOneIsLocked() {
		String user = uniqueUser("updatePartLock");
		int id = makeLesson(user, "Locked title", "General");
		lock(id, "title", true);

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "Locked title",
				"The description may still change.", "java", "General");
		check("U1-P3", true, "Update Lesson - Other Fields Stay Editable While One Is Locked",
				"success \"Lesson updated\"; stored: Locked title | The description may still "
				+ "change. | java | General",
				describe(result) + "; " + storedAll(id));
	}


	/**********
	 * <p> Test U1V-N1 (Negative): Update Lesson - Rejected Because Field Is Locked. </p>
	 *
	 * <p> <b>Requirements:</b> US-U1-V, changing a locked field is refused with a clear message
	 * and the lesson is left unchanged; and US-U1, which allows a change only "as long as the
	 * field is not locked". </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateLesson compares each locked field's new value
	 * with the stored one before any validation or saving, and returns
	 * LessonAccess.FIELD_LOCKED with the field's name.  The Edit page also disables locked
	 * inputs, so this Model check is the safety net behind the screen. </p>
	 *
	 * <p> <b>Steps:</b> lock the title, then try to change it. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the locked-field message is returned for
	 * the title and the stored lesson is exactly as it was. </p>
	 */
	@Test
	@Order(4)
	@DisplayName("U1V-N1 Update Lesson - Rejected Because Field Is Locked (US-U1-V, N)")
	void testUpdateRejectedBecauseFieldIsLocked() {
		String user = uniqueUser("updateLocked");
		int id = makeLesson(user, "Locked title", "General");
		lock(id, "title", true);

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "Sneaky new title",
				DESCRIPTION, "java", "General");
		check("U1V-N1", false, "Update Lesson - Rejected Because Field Is Locked",
				"refused \"This field is locked and cannot be edited.\" (field: title); stored: "
				+ "Locked title | " + DESCRIPTION + " | java | General",
				describe(result) + "; " + storedAll(id));
	}


	/**********
	 * <p> Test U1V-N2 (Negative): Update Lesson - Rejected Because New Value Is Invalid. </p>
	 *
	 * <p> <b>Requirement:</b> US-U1-V, invalid updated information is refused with a clear
	 * message and the lesson is left unchanged. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> updateLesson validates every unlocked field with
	 * LessonsLearned.validateField before anything is saved. </p>
	 *
	 * <p> <b>Steps:</b> submit a 5001-character description for an unlocked lesson. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the description-length message is returned
	 * for the description and the stored description is still the original one. </p>
	 */
	@Test
	@Order(5)
	@DisplayName("U1V-N2 Update Lesson - Rejected Because New Value Is Invalid (US-U1-V, N)")
	void testUpdateRejectedBecauseNewValueIsInvalid() {
		String user = uniqueUser("updateInvalid");
		int id = makeLesson(user, "Valid title", "General");

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "Valid title",
				rep("A", 5001), "java", "General");
		check("U1V-N2", false, "Update Lesson - Rejected Because New Value Is Invalid",
				"refused \"Description must be between 10 and 5000 characters.\" (field: "
				+ "description); stored: Valid title | " + DESCRIPTION + " | java | General",
				describe(result) + "; " + storedAll(id));
	}


	/**********
	 * <p> Test U1V-N3 (Negative): Update Lesson - Rejected Because the Whole Lesson Is
	 * Locked. </p>
	 *
	 * <p> <b>Requirement:</b> US-U1-V, when the whole lesson is locked, every field counts as
	 * locked, so any change is refused. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> LessonsLearned.isFieldLocked returns true for every
	 * field when the whole-lesson lock is set. </p>
	 *
	 * <p> <b>Steps:</b> lock the entire lesson, then try to change only the category. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the locked-field message names the category
	 * and the stored category is still General. </p>
	 */
	@Test
	@Order(6)
	@DisplayName("U1V-N3 Update Lesson - Rejected Because the Whole Lesson Is Locked (US-U1-V, N)")
	void testUpdateRejectedBecauseEntireLessonIsLocked() {
		String user = uniqueUser("updateEntireLock");
		int id = makeLesson(user, "Fully locked lesson", "General");
		lock(id, "entire", true);

		LessonResult result = ModelUpdateLesson.updateLesson(user, id, "Fully locked lesson",
				DESCRIPTION, "java", "Technical");
		check("U1V-N3", false, "Update Lesson - Rejected Because the Whole Lesson Is Locked",
				"refused \"This field is locked and cannot be edited.\" (field: category); stored: "
				+ "Fully locked lesson | " + DESCRIPTION + " | java | General",
				describe(result) + "; " + storedAll(id));
	}
}
