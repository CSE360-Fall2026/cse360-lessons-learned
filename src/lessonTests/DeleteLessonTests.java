package lessonTests;

import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.describe;
import static lessonTests.TestSupport.lock;
import static lessonTests.TestSupport.makeLesson;
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

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import guiAddExperience.ModelAddExperience;
import guiContributorHome.ModelContributorHome;
import guiLessonDetails.ModelLessonDetails;
import guiTools.LessonResult;

/*******
 * <p> Title: DeleteLessonTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for US-D1 (a Contributor can delete their lesson after
 * confirming an "Are you sure?" prompt with an explicit "Yes") and US-D1-V (a locked lesson, or
 * a lesson with any locked field, cannot be deleted, and answering anything but "Yes" cancels
 * the deletion).</p>
 *
 * <p> Every test calls ModelLessonDetails.deleteLesson(user, id, confirmed), the method the
 * Lesson Details page's Controller calls after its confirmation dialog closes.  The
 * <code>confirmed</code> argument is the dialog's answer: true for "Yes", false for "No" or
 * closing the dialog.  The dialog itself is shown in the manual test M7 (ManualGuiTests).  See
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
 * @version 1.00		2026-10-16 TP2 Task 6: Delete tests, one method per test
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeleteLessonTests {

	/** The message the Model returns when a locked lesson cannot be deleted. */
	private static final String LOCKED = "refused \"This lesson is locked and cannot be deleted.\"";

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public DeleteLessonTests() {
	}

	/**********
	 * Runs the Delete tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor Tests - Delete Lesson (US-D1, US-D1-V)",
				DeleteLessonTests::runAll);
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
		DeleteLessonTests tests = new DeleteLessonTests();
		requirement("US-D1", "A Contributor can delete their lesson by answering \"Yes\" to "
				+ "\"Are you sure?\"");
		run(tests::testConfirmedDeletionOfUnlockedLesson);
		run(tests::testDeletionAlsoRemovesExperienceEntries);
		run(tests::testDeclinedConfirmationCancelsDeletion);

		requirement("US-D1-V", "A locked lesson, or one with any locked field, cannot be "
				+ "deleted");
		run(tests::testDeleteRejectedBecauseLessonIsLocked);
		run(tests::testDeleteRejectedBecauseSingleFieldIsLocked);
		run(tests::testDeleteAllowedAfterLockIsRemoved);
	}


	/**********
	 * Reports whether a lesson still exists, read directly from the database.
	 *
	 * @param lessonId	the lesson to look for
	 * @return "lesson still stored" or "lesson no longer stored"
	 */
	private static String existence(int lessonId) {
		return (LessonsLearned.read(lessonId) == null) ? "lesson no longer stored"
				: "lesson still stored";
	}


	/**********
	 * <p> Test D1-P1 (Positive): Delete Lesson - Confirmed Deletion of an Unlocked Lesson. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1, answering "Yes" deletes the lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> deleteLesson checks ownership and locks, and with
	 * confirmed == true calls LessonsLearned.delete(), which removes the row. </p>
	 *
	 * <p> <b>Steps:</b> delete an unlocked lesson with confirmed = true; then look for it in the
	 * Contributor's list and in the database. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means "Lesson deleted" was returned, the list is
	 * empty, and the row is gone. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("D1-P1 Delete Lesson - Confirmed Deletion of an Unlocked Lesson (US-D1, P)")
	void testConfirmedDeletionOfUnlockedLesson() {
		String user = uniqueUser("deleteYes");
		int id = makeLesson(user, "Lesson to delete", "General");

		LessonResult result = ModelLessonDetails.deleteLesson(user, id, true);
		check("D1-P1", true, "Delete Lesson - Confirmed Deletion of an Unlocked Lesson",
				"success \"Lesson deleted\"; my list holds 0 lesson(s); lesson no longer stored",
				describe(result) + "; my list holds " + ModelContributorHome.viewMyLessons(user).size()
				+ " lesson(s); " + existence(id));
	}


	/**********
	 * <p> Test D1-P2 (Positive): Delete Lesson - Its Experience Entries Are Removed Too. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1, deleting a lesson removes it entirely; orphaned experience
	 * rows would be data that no screen can reach or remove. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> experienceDB and experienceMemberEffortDB are declared with
	 * ON DELETE CASCADE, so deleting the lesson row deletes its entries and their member
	 * shares. </p>
	 *
	 * <p> <b>Steps:</b> add one experience entry, count the entries, delete the lesson, count
	 * again. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means 1 entry before and 0 after. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("D1-P2 Delete Lesson - Its Experience Entries Are Removed Too (US-D1, P)")
	void testDeletionAlsoRemovesExperienceEntries() {
		String user = uniqueUser("deleteCascade");
		int id = makeLesson(user, "Lesson with experience", "General");
		ModelAddExperience.addExperience(user, id, "1", "Reused once before deleting.");

		int before = ExperienceEntry.readForLesson(id).size();
		LessonResult result = ModelLessonDetails.deleteLesson(user, id, true);
		int after = ExperienceEntry.readForLesson(id).size();
		check("D1-P2", true, "Delete Lesson - Its Experience Entries Are Removed Too",
				"entries before: 1; success \"Lesson deleted\"; entries after: 0",
				"entries before: " + before + "; " + describe(result) + "; entries after: " + after);
	}


	/**********
	 * <p> Test D1-N1 (Negative): Delete Lesson - Declined Confirmation Cancels the
	 * Deletion. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1 and US-D1-V, without an explicit "Yes" the deletion is
	 * cancelled and nothing changes. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> with confirmed == false, deleteLesson returns
	 * before calling delete().  The result carries no message, because backing out is a normal
	 * choice, not an error (Test Cases.pdf). </p>
	 *
	 * <p> <b>Steps:</b> delete an unlocked lesson with confirmed = false. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the action is refused with no message and
	 * the lesson is still stored. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("D1-N1 Delete Lesson - Declined Confirmation Cancels the Deletion (US-D1, N)")
	void testDeclinedConfirmationCancelsDeletion() {
		String user = uniqueUser("deleteNo");
		int id = makeLesson(user, "Lesson to keep", "General");

		LessonResult result = ModelLessonDetails.deleteLesson(user, id, false);
		check("D1-N1", false, "Delete Lesson - Declined Confirmation Cancels the Deletion",
				"refused with no message; lesson still stored",
				describe(result) + "; " + existence(id));
	}


	/**********
	 * <p> Test D1V-N1 (Negative): Delete Lesson - Rejected Because Lesson Is Locked. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1-V, a locked lesson cannot be deleted, and the Contributor is
	 * told why. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> deleteLesson checks LessonsLearned.isLocked() before
	 * it looks at the confirmation, so even a "Yes" cannot delete a locked lesson. </p>
	 *
	 * <p> <b>Steps:</b> lock the whole lesson; delete it with confirmed = true. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the locked-lesson message is returned and
	 * the lesson is still stored. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("D1V-N1 Delete Lesson - Rejected Because Lesson Is Locked (US-D1-V, N)")
	void testDeleteRejectedBecauseLessonIsLocked() {
		String user = uniqueUser("deleteLocked");
		int id = makeLesson(user, "Locked lesson", "General");
		lock(id, "entire", true);

		LessonResult result = ModelLessonDetails.deleteLesson(user, id, true);
		check("D1V-N1", false, "Delete Lesson - Rejected Because Lesson Is Locked",
				LOCKED + "; lesson still stored", describe(result) + "; " + existence(id));
	}


	/**********
	 * <p> Test D1V-N2 (Negative): Delete Lesson - Rejected Because a Single Field Is
	 * Locked. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1-V, "if the lesson ... (or any of its fields) has been
	 * locked", the deletion is refused. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> isLocked() is true when the whole-lesson flag or any
	 * one field flag is set. </p>
	 *
	 * <p> <b>Steps:</b> lock only the tags; delete with confirmed = true. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the locked-lesson message is returned and
	 * the lesson is still stored. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("D1V-N2 Delete Lesson - Rejected Because a Single Field Is Locked (US-D1-V, N)")
	void testDeleteRejectedBecauseSingleFieldIsLocked() {
		String user = uniqueUser("deleteFieldLocked");
		int id = makeLesson(user, "Lesson with one locked field", "General");
		lock(id, "tags", true);

		LessonResult result = ModelLessonDetails.deleteLesson(user, id, true);
		check("D1V-N2", false, "Delete Lesson - Rejected Because a Single Field Is Locked",
				LOCKED + "; lesson still stored", describe(result) + "; " + existence(id));
	}


	/**********
	 * <p> Test D1V-P1 (Positive): Delete Lesson - Allowed Again After the Lock Is Removed. </p>
	 *
	 * <p> <b>Requirement:</b> US-D1-V, the refusal is caused by the lock and only by the lock:
	 * once a Curator removes it, the owner can delete the lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> deleteLesson reads the current lock flags from the
	 * database every time, so a cleared flag takes effect immediately. </p>
	 *
	 * <p> <b>Steps:</b> lock the category and try to delete; unlock it and try again. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the first attempt is refused with the
	 * locked message and the second returns "Lesson deleted". </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("D1V-P1 Delete Lesson - Allowed Again After the Lock Is Removed (US-D1-V, P)")
	void testDeleteAllowedAfterLockIsRemoved() {
		String user = uniqueUser("deleteUnlocked");
		int id = makeLesson(user, "Lesson unlocked later", "General");
		lock(id, "category", true);
		LessonResult whileLocked = ModelLessonDetails.deleteLesson(user, id, true);
		lock(id, "category", false);
		LessonResult afterUnlock = ModelLessonDetails.deleteLesson(user, id, true);

		check("D1V-P1", true, "Delete Lesson - Allowed Again After the Lock Is Removed",
				"while locked: " + LOCKED + "; after unlock: success \"Lesson deleted\"",
				"while locked: " + describe(whileLocked) + "; after unlock: "
				+ describe(afterUnlock));
	}
}
