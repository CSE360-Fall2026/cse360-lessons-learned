package lessonTests;

import static lessonTests.TestSupport.DESCRIPTION;
import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.describe;
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

import entityClasses.LessonsLearned;
import guiContributorHome.ModelContributorHome;
import guiCreateLesson.ModelCreateLesson;
import guiTools.LessonResult;

/*******
 * <p> Title: CreateLessonTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the Create user stories, US-C1 (a Contributor can
 * add a new lesson learned by entering its title, description, tags, and category) and US-C1-V
 * (invalid core information gets a specific, helpful message and nothing is saved).</p>
 *
 * <p> Every test calls ModelCreateLesson.createLesson, the same method the Create Lesson page's
 * Controller calls when the "Create Lesson" button is pressed, and then reads the database
 * directly to prove what was, or was not, stored.  See TestSupport for how to read the
 * output.  The character-by-character boundary checks of each field are in
 * inputValidation.LessonRecognizerTest; these tests prove that the Model applies those rules
 * before saving.</p>
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
 * @version 1.00		2026-10-16 TP2 Task 6: Create tests, one method per test
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreateLessonTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public CreateLessonTests() {
	}

	/**********
	 * Runs the Create tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor Tests - Create Lesson (US-C1, US-C1-V)",
				CreateLessonTests::runAll);
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
		CreateLessonTests tests = new CreateLessonTests();
		requirement("US-C1", "A Contributor can add a new lesson (title, description, tags, "
				+ "category) and it is saved");
		run(tests::testCreateLessonValidInput);
		run(tests::testCreateLessonTrimsTextAndAllowsEmptyTags);

		requirement("US-C1-V", "Invalid core information gets a specific message and nothing "
				+ "is saved");
		run(tests::testCreateLessonBoundaryValuesAccepted);
		run(tests::testCreateLessonInvalidCategoryBlocksSave);
		run(tests::testCreateLessonMissingTitleBlocksSave);
		run(tests::testCreateLessonDescriptionTooShortAfterTrimming);
	}


	/**********
	 * <p> Test C1-P1 (Positive): Create Lesson - Valid Input. </p>
	 *
	 * <p> <b>Requirement:</b> US-C1, a Contributor can add a new lesson by entering its core
	 * information, and it is saved so it can be retrieved later. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ModelCreateLesson.createLesson validates the four
	 * fields, trims the free text, and calls LessonsLearned.create(), which inserts one row in
	 * lessonsLearnedDB through Database.createLesson and returns the new id. </p>
	 *
	 * <p> <b>Steps:</b> a new Contributor creates "Valid title" (Technical, tags "java,
	 * testing"); the test then counts that Contributor's lessons and reads the stored row back by
	 * id. </p>
	 *
	 * <p> <b>How to read the output:</b> the Actual line shows the Model's message, how many
	 * lessons the Contributor now has, and the title, category, and tags read back from the
	 * database.  PASS means the lesson was really stored with exactly the values entered. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("C1-P1 Create Lesson - Valid Input (US-C1, P)")
	void testCreateLessonValidInput() {
		String user = uniqueUser("createValid");
		LessonResult result = ModelCreateLesson.createLesson(user, "Valid title", DESCRIPTION,
				"java, testing", "Technical");
		remember(result.getLessonId());

		String actual = describe(result) + "; my list holds "
				+ ModelContributorHome.viewMyLessons(user).size() + " lesson(s); stored: "
				+ stored(result.getLessonId());
		check("C1-P1", true, "Create Lesson - Valid Input",
				"success \"Lesson created\"; my list holds 1 lesson(s); stored: Valid title | "
				+ "Technical | tags: java, testing", actual);
	}


	/**********
	 * <p> Test C1-P2 (Positive): Create Lesson - Text Is Trimmed and Empty Tags Are
	 * Allowed. </p>
	 *
	 * <p> <b>Requirement:</b> US-C1 (tags are optional core information), and the
	 * InputValidation.pdf rule that free text is stored without leading and trailing spaces. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> LessonRecognizer.checkForValidTags accepts an empty
	 * value, and createLesson calls LessonsLearned.trimTextFields() before saving. </p>
	 *
	 * <p> <b>Steps:</b> create a lesson titled "  Padded title  " (spaces on both sides), with
	 * no tags, in the General category; read the stored row back. </p>
	 *
	 * <p> <b>How to read the output:</b> the stored title must be "Padded title" with no spaces
	 * around it, and the tags must be "(none)".  PASS means an optional field may be left empty
	 * and the stored text is clean. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("C1-P2 Create Lesson - Text Is Trimmed and Empty Tags Are Allowed (US-C1, P)")
	void testCreateLessonTrimsTextAndAllowsEmptyTags() {
		String user = uniqueUser("createTrim");
		LessonResult result = ModelCreateLesson.createLesson(user, "  Padded title  ",
				DESCRIPTION, "", "General");
		remember(result.getLessonId());

		check("C1-P2", true, "Create Lesson - Text Is Trimmed and Empty Tags Are Allowed",
				"success \"Lesson created\"; stored: Padded title | General | tags: (none)",
				describe(result) + "; stored: " + stored(result.getLessonId()));
	}


	/**********
	 * <p> Test C1V-P1 (Positive): Create Lesson - Boundary Values Accepted. </p>
	 *
	 * <p> <b>Requirement:</b> US-C1-V.  Rejecting input that is "too short or too long" also
	 * means accepting input that is exactly at the limit; otherwise the error message would be
	 * wrong. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> LessonRecognizer allows a title of up to 100
	 * characters and a description of 10 to 5000 characters, both limits included. </p>
	 *
	 * <p> <b>Steps:</b> create a lesson whose title is exactly 100 characters and whose
	 * description is exactly 10 characters. </p>
	 *
	 * <p> <b>How to read the output:</b> the Actual line shows the lengths read back from the
	 * database.  PASS means both limits are accepted and stored in full. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("C1V-P1 Create Lesson - Boundary Values Accepted (US-C1-V, P)")
	void testCreateLessonBoundaryValuesAccepted() {
		String user = uniqueUser("createBoundary");
		LessonResult result = ModelCreateLesson.createLesson(user, rep("T", 100), rep("D", 10),
				"", "Teamwork");
		remember(result.getLessonId());

		LessonsLearned saved = LessonsLearned.read(result.getLessonId());
		String lengths = (saved == null) ? "nothing stored"
				: "stored title length " + saved.getTitle().length() + ", description length "
						+ saved.getDescription().length();
		check("C1V-P1", true, "Create Lesson - Boundary Values Accepted",
				"success \"Lesson created\"; stored title length 100, description length 10",
				describe(result) + "; " + lengths);
	}


	/**********
	 * <p> Test C1V-N1 (Negative): Create Lesson - Invalid Input Blocks the Save. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, an invalid field gets a specific message and nothing
	 * gets saved, not even the valid fields; and US-C1, which saves only a valid lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> createLesson calls findInvalidField() on the whole
	 * lesson before LessonsLearned.create() is reached, and returns the message and the name
	 * of the field so the page shows it beneath that field. </p>
	 *
	 * <p> <b>Steps:</b> submit a valid title and description with the category "Marketing",
	 * which is not one of the fixed categories; count the Contributor's lessons. </p>
	 *
	 * <p> <b>How to read the output:</b> the Actual line must show the category message, the
	 * field "category", and 0 lessons.  PASS means the invalid input was refused and nothing
	 * was stored. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("C1V-N1 Create Lesson - Invalid Input Blocks the Save (US-C1-V, N)")
	void testCreateLessonInvalidCategoryBlocksSave() {
		String user = uniqueUser("createBadCategory");
		LessonResult result = ModelCreateLesson.createLesson(user, "Another valid title",
				DESCRIPTION, "java", "Marketing");
		remember(result.getLessonId());

		check("C1V-N1", false, "Create Lesson - Invalid Input Blocks the Save",
				"refused \"Please choose a valid category.\" (field: category); my list holds 0 "
				+ "lesson(s)",
				describe(result) + "; my list holds " + ModelContributorHome.viewMyLessons(user).size()
				+ " lesson(s)");
	}


	/**********
	 * <p> Test C1V-N2 (Negative): Create Lesson - Missing Title Blocks the Save. </p>
	 *
	 * <p> <b>Requirement:</b> US-C1-V, missing core information gets a specific message and
	 * nothing is saved. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> LessonRecognizer.checkForValidTitle returns "Title
	 * is required." for an empty title, and createLesson stops before saving. </p>
	 *
	 * <p> <b>Steps:</b> submit an empty title with otherwise valid fields; count the
	 * Contributor's lessons. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the message names the missing title, the
	 * field is "title", and 0 lessons were stored. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("C1V-N2 Create Lesson - Missing Title Blocks the Save (US-C1-V, N)")
	void testCreateLessonMissingTitleBlocksSave() {
		String user = uniqueUser("createNoTitle");
		LessonResult result = ModelCreateLesson.createLesson(user, "", DESCRIPTION, "java",
				"General");
		remember(result.getLessonId());

		check("C1V-N2", false, "Create Lesson - Missing Title Blocks the Save",
				"refused \"Title is required.\" (field: title); my list holds 0 lesson(s)",
				describe(result) + "; my list holds " + ModelContributorHome.viewMyLessons(user).size()
				+ " lesson(s)");
	}


	/**********
	 * <p> Test C1V-N3 (Negative): Create Lesson - Description Too Short After Trimming. </p>
	 *
	 * <p> <b>Requirement:</b> US-C1-V, a value that is too short is refused with a specific
	 * message and nothing is saved. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> the description "   short   " is 11 characters as
	 * typed, so it passes the first check; createLesson then trims it to "short" (5 characters)
	 * and validates again before saving.  This second check is why padding with spaces cannot
	 * sneak a too-short description into the database. </p>
	 *
	 * <p> <b>Steps:</b> submit the padded description; count the Contributor's lessons. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the length message is shown for the
	 * description and 0 lessons were stored. </p>
	 *
	 * <p> Validates: ModelCreateLesson.createLesson(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("C1V-N3 Create Lesson - Description Too Short After Trimming (US-C1-V, N)")
	void testCreateLessonDescriptionTooShortAfterTrimming() {
		String user = uniqueUser("createPadded");
		LessonResult result = ModelCreateLesson.createLesson(user, "Padded description",
				"   short   ", "", "General");
		remember(result.getLessonId());

		check("C1V-N3", false, "Create Lesson - Description Too Short After Trimming",
				"refused \"Description must be between 10 and 5000 characters.\" (field: "
				+ "description); my list holds 0 lesson(s)",
				describe(result) + "; my list holds " + ModelContributorHome.viewMyLessons(user).size()
				+ " lesson(s)");
	}
}
