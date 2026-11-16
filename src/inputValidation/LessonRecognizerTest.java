package inputValidation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/*******
 * <p> Title: LessonRecognizerTest Class. </p>
 *
 * <p> Description: The automated input validation tests for every field LessonRecognizer
 * checks.  Tests 1 to 45 are the "Automated Input Validation Tests" from HW2 Test Cases.pdf,
 * kept with the same numbers; Tests 46 to 58 were added in TP2 for the fields added in TP2: a
 * team member's name (requirement E7) and the root cause, recommendation, and Curator search
 * terms (requirement E6).</p>
 *
 * <p> <b>Requirements covered.</b>  Each test names the requirements it demonstrates.  The
 * field rules here are what US-C1-V, US-R2-V, US-U1-V, and US-U2-V mean by input that is
 * "missing, too short, too long, or contains invalid characters", and each test checks the exact
 * "specific, helpful error message" those stories require.  The requirement tests in the
 * lessonTests package then prove that each page's Model applies these rules before anything is
 * saved.</p>
 *
 * <p> <b>How the tests are organized.</b>  Each numbered test is one JUnit test method, named
 * after its number (test01NormalTitleIsValid to test58SearchTermsWithTabAreRejected), and the
 * tests run in number order.  Boundary tests for one field only make sense side by side (the
 * last valid length next to the first invalid length), so each field's tests come together,
 * right after that field's run method, whose Javadoc lists every test of the field with its
 * input and expected result.</p>
 *
 * <p> <b>Running the tests (TP3).</b>  These are JUnit 5 tests: run the class with Run As &gt;
 * JUnit Test.  Each test's display name is its TP2 number, the field, what it checks, its
 * requirements, and P (a valid input that must be accepted) or N (an invalid input that must be
 * rejected with one exact message), for example "Test 4 Title: 101-character title - over the
 * maximum (US-C1-V, US-U1-V, N)".  The same tests still run as a console program through
 * main(), with exactly the same output as in TP2, because curatorLockTests LK-33 runs it to show
 * the TP2 tests still pass.  These tests do not use the database.</p>
 *
 * <p> <b>How to read the output.</b>  Each test prints one line, "Test N: [input] - [what it
 * checks] - Pass.".  For a valid input the test passes only if the recognizer returns an
 * empty String (no error).  For an invalid input the test passes only if the recognizer
 * returns exactly the expected message; when a test fails, both the expected and the actual
 * message are printed under the line.  Under JUnit the test then asserts the same comparison, so
 * a failure fails that test and its message names the test number.  The console program runs
 * every test before reporting, and ends with an AssertionError if anything failed, so a failure
 * cannot be missed.</p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-09-29 Initial version for HW2
 * @version 1.01		2026-10-16 TP2 Task 6: Tests 46-58 for the member name, root cause,
 * 							recommendation, and search terms; requirement mapping in the Javadoc
 * @version 1.02		2026-11-16 TP3 B-20: converted to JUnit 5, one test method per numbered
 * 							test with the same input and expected result; the console runner is
 * 							kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LessonRecognizerTest {

	/** How many tests have run so far in the console program. */
	private static int numberOfTests = 0;
	/** How many of those tests failed. */
	private static int numberFailed = 0;
	/** True while main() runs the tests as a console program, false under JUnit. */
	private static boolean consoleMode = false;

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and main() creates one to
	 * run the same test methods as a console program.
	 */
	public LessonRecognizerTest() {
	}

	/**********
	 * Runs Tests 1 to 58 as a console program and prints the totals.  Ends with an AssertionError
	 * if any test failed.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		System.out.println("TP2 Automated Input Validation Tests - LessonRecognizer\n");
		System.out.println("A valid input passes when no error is returned; an invalid input "
				+ "passes only\nwhen the exact expected error message is returned.");
		// Count instead of asserting, so every test runs before the totals are printed
		numberOfTests = 0;
		numberFailed = 0;
		consoleMode = true;
		LessonRecognizerTest tests = new LessonRecognizerTest();
		try {
			runTitleTests(tests);
			runDescriptionTests(tests);
			runTagsTests(tests);
			runCategoryTests(tests);
			runSearchTextTests(tests);
			runExperienceValueTests(tests);
			runExperienceDescriptionTests(tests);
			runMemberNameTests(tests);
			runCoachedTextTests(tests);
			runSearchTermsTests(tests);
		} finally {
			consoleMode = false;
		}

		// Same footer wording as the testbed automation files (EmailAddressTestAutomation and
		// PasswordEvaluationTestingAutomation)
		System.out.println("____________________________________________________________________________");
		System.out.println();
		System.out.println("Number of tests passed: " + (numberOfTests - numberFailed));
		System.out.println("Number of tests failed: " + numberFailed);
		if (numberFailed > 0)
			throw new AssertionError(numberFailed + " LessonRecognizer test case(s) failed.");
	}

	/**********
	 * <p> Tests 1-7: lesson title. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1 and US-U1 (a valid title is accepted), US-C1-V and US-U1-V
	 * (a missing, too long, or invalid title gets a specific message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidTitle requires at least one
	 * non-blank character, at most 100 characters, and no control characters, and names the
	 * position of the first invalid character. </p>
	 *
	 * <p> Tests: 1 normal title (valid); 2 one character (valid, minimum); 3 100 characters
	 * (valid, maximum); 4 101 characters (too long); 5 only spaces (required); 6 contains a tab
	 * (invalid character at position 7); 7 empty (required). </p>
	 *
	 * <p> Runs Tests 1 to 7 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runTitleTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Title Recognizer Tests (US-C1, US-C1-V, US-U1-V) ---");
		tests.test01NormalTitleIsValid();
		tests.test02OneCharacterTitleIsValid();
		tests.test03HundredCharacterTitleIsValid();
		tests.test04TitleOver100IsRejected();
		tests.test05BlankTitleIsRequired();
		tests.test06TitleWithTabIsRejected();
		tests.test07EmptyTitleIsRequired();
	}

	/**********
	 * <p> Test 1 (Positive): title, "Fixed the deployment pipeline bug" - valid title. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(1)
	@DisplayName("Test 1 Title: \"Fixed the deployment pipeline bug\" - valid title (US-C1, US-U1, P)")
	void test01NormalTitleIsValid() {
		expectValid(1, LessonRecognizer.checkForValidTitle("Fixed the deployment pipeline bug"),
				"\"Fixed the deployment pipeline bug\" - valid title");
	}

	/**********
	 * <p> Test 2 (Positive): title, "X" (1 character) - meets the minimum, any non-blank value.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(2)
	@DisplayName("Test 2 Title: \"X\" (1 character) - meets the minimum, any non-blank value (US-C1, US-U1, P)")
	void test02OneCharacterTitleIsValid() {
		expectValid(2, LessonRecognizer.checkForValidTitle("X"),
				"\"X\" (1 character) - meets the minimum, any non-blank value");
	}

	/**********
	 * <p> Test 3 (Positive): title, 100-character title - meets the maximum boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(3)
	@DisplayName("Test 3 Title: 100-character title - meets the maximum boundary (US-C1, US-U1, P)")
	void test03HundredCharacterTitleIsValid() {
		expectValid(3, LessonRecognizer.checkForValidTitle(rep("A", 100)),
				"100-character title - meets the maximum boundary");
	}

	/**********
	 * <p> Test 4 (Negative): title, 101-character title - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Title must be 100 characters or fewer." </p>
	 */
	@Test
	@Order(4)
	@DisplayName("Test 4 Title: 101-character title - over the maximum (US-C1-V, US-U1-V, N)")
	void test04TitleOver100IsRejected() {
		expectError(4, LessonRecognizer.checkForValidTitle(rep("A", 101)),
				"Title must be 100 characters or fewer.", "101-character title - over the maximum");
	}

	/**********
	 * <p> Test 5 (Negative): title, whitespace-only title. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Title is required." </p>
	 */
	@Test
	@Order(5)
	@DisplayName("Test 5 Title: whitespace-only title (US-C1-V, US-U1-V, N)")
	void test05BlankTitleIsRequired() {
		expectError(5, LessonRecognizer.checkForValidTitle("   "), "Title is required.",
				"whitespace-only title");
	}

	/**********
	 * <p> Test 6 (Negative): title, "Sprint[TAB]Recap" - control character. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Title contains an invalid character at position 7." </p>
	 */
	@Test
	@Order(6)
	@DisplayName("Test 6 Title: \"Sprint[TAB]Recap\" - control character (US-C1-V, US-U1-V, N)")
	void test06TitleWithTabIsRejected() {
		expectError(6, LessonRecognizer.checkForValidTitle("Sprint\tRecap"),
				"Title contains an invalid character at position 7.",
				"\"Sprint[TAB]Recap\" - control character");
	}

	/**********
	 * <p> Test 7 (Negative): title, empty title. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Title is required." </p>
	 */
	@Test
	@Order(7)
	@DisplayName("Test 7 Title: empty title (US-C1-V, US-U1-V, N)")
	void test07EmptyTitleIsRequired() {
		expectError(7, LessonRecognizer.checkForValidTitle(""), "Title is required.", "empty title");
	}

	/**********
	 * <p> Tests 8-15: lesson description. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1 and US-U1 (a valid description is accepted), US-C1-V and
	 * US-U1-V (a missing, too short, too long, or invalid description gets a specific
	 * message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidDescription requires 10 to 5000
	 * characters with no control characters other than line breaks, and reports a blank value
	 * as required. </p>
	 *
	 * <p> Tests: 8 normal (valid); 9 10 characters (valid, minimum); 10 5000 characters (valid,
	 * maximum); 11 9 characters (range message); 12 5001 characters (range message); 13 only
	 * spaces (required); 14 contains a tab (invalid character at position 9); 15 empty
	 * (required). </p>
	 *
	 * <p> Runs Tests 8 to 15 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runDescriptionTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Description Recognizer Tests (US-C1, US-C1-V, US-U1-V) ---");
		tests.test08NormalDescriptionIsValid();
		tests.test09TenCharacterDescriptionIsValid();
		tests.test10MaxLengthDescriptionIsValid();
		tests.test11NineCharacterDescriptionIsRejected();
		tests.test12DescriptionOver5000IsRejected();
		tests.test13BlankDescriptionIsRequired();
		tests.test14DescriptionWithTabIsRejected();
		tests.test15EmptyDescriptionIsRequired();
	}

	/**********
	 * <p> Test 8 (Positive): description, normal description. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(8)
	@DisplayName("Test 8 Description: normal description (US-C1, US-U1, P)")
	void test08NormalDescriptionIsValid() {
		expectValid(8,
				LessonRecognizer.checkForValidDescription("We underestimated integration time and shipped two days late."),
				"normal description");
	}

	/**********
	 * <p> Test 9 (Positive): description, 10-character description - meets the minimum boundary.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(9)
	@DisplayName("Test 9 Description: 10-character description - meets the minimum boundary (US-C1, US-U1, P)")
	void test09TenCharacterDescriptionIsValid() {
		expectValid(9, LessonRecognizer.checkForValidDescription(rep("A", 10)),
				"10-character description - meets the minimum boundary");
	}

	/**********
	 * <p> Test 10 (Positive): description, 5000-character description - meets the maximum
	 * boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(10)
	@DisplayName("Test 10 Description: 5000-character description - meets the maximum boundary (US-C1, US-U1, P)")
	void test10MaxLengthDescriptionIsValid() {
		expectValid(10, LessonRecognizer.checkForValidDescription(rep("A", 5000)),
				"5000-character description - meets the maximum boundary");
	}

	/**********
	 * <p> Test 11 (Negative): description, 9-character description - under the minimum. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Description must be between 10 and 5000 characters." </p>
	 */
	@Test
	@Order(11)
	@DisplayName("Test 11 Description: 9-character description - under the minimum (US-C1-V, US-U1-V, N)")
	void test11NineCharacterDescriptionIsRejected() {
		expectError(11, LessonRecognizer.checkForValidDescription(rep("A", 9)),
				"Description must be between 10 and 5000 characters.",
				"9-character description - under the minimum");
	}

	/**********
	 * <p> Test 12 (Negative): description, 5001-character description - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Description must be between 10 and 5000 characters." </p>
	 */
	@Test
	@Order(12)
	@DisplayName("Test 12 Description: 5001-character description - over the maximum (US-C1-V, US-U1-V, N)")
	void test12DescriptionOver5000IsRejected() {
		expectError(12, LessonRecognizer.checkForValidDescription(rep("A", 5001)),
				"Description must be between 10 and 5000 characters.",
				"5001-character description - over the maximum");
	}

	/**********
	 * <p> Test 13 (Negative): description, whitespace-only description. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Description is required." </p>
	 */
	@Test
	@Order(13)
	@DisplayName("Test 13 Description: whitespace-only description (US-C1-V, US-U1-V, N)")
	void test13BlankDescriptionIsRequired() {
		expectError(13, LessonRecognizer.checkForValidDescription("     "),
				"Description is required.", "whitespace-only description");
	}

	/**********
	 * <p> Test 14 (Negative): description, description containing a tab (control character). </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Description contains an invalid character at position 9." </p>
	 */
	@Test
	@Order(14)
	@DisplayName("Test 14 Description: description containing a tab (control character) (US-C1-V, US-U1-V, N)")
	void test14DescriptionWithTabIsRejected() {
		expectError(14, LessonRecognizer.checkForValidDescription("Line one\tline two"),
				"Description contains an invalid character at position 9.",
				"description containing a tab (control character)");
	}

	/**********
	 * <p> Test 15 (Negative): description, empty description. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Description is required." </p>
	 */
	@Test
	@Order(15)
	@DisplayName("Test 15 Description: empty description (US-C1-V, US-U1-V, N)")
	void test15EmptyDescriptionIsRequired() {
		expectError(15, LessonRecognizer.checkForValidDescription(""), "Description is required.",
				"empty description");
	}

	/**********
	 * <p> Tests 16-20: tags. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1 and US-U1 (tags are optional core information), US-C1-V
	 * and US-U1-V (too long or invalid tags get a specific message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidTags accepts an empty value, limits
	 * the whole field to 200 characters and each comma-separated tag to 30, and rejects control
	 * characters. </p>
	 *
	 * <p> Tests: 16 normal tags (valid); 17 empty (valid, optional); 18 201 characters in total
	 * (too long); 19 one 31-character tag (too long); 20 contains a tab (invalid
	 * character). </p>
	 *
	 * <p> Runs Tests 16 to 20 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runTagsTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Tags Recognizer Tests (US-C1, US-C1-V, US-U1-V) ---");
		tests.test16NormalTagsAreValid();
		tests.test17EmptyTagsAreValid();
		tests.test18TagsOver200AreRejected();
		tests.test19TagOver30IsRejected();
		tests.test20TagsWithTabAreRejected();
	}

	/**********
	 * <p> Test 16 (Positive): tags, normal comma-separated tags. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(16)
	@DisplayName("Test 16 Tags: normal comma-separated tags (US-C1, US-U1, P)")
	void test16NormalTagsAreValid() {
		expectValid(16, LessonRecognizer.checkForValidTags("java, debugging, sprint-3"),
				"normal comma-separated tags");
	}

	/**********
	 * <p> Test 17 (Positive): tags, empty tags - accepted, the field is optional. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(17)
	@DisplayName("Test 17 Tags: empty tags - accepted, the field is optional (US-C1, US-U1, P)")
	void test17EmptyTagsAreValid() {
		expectValid(17, LessonRecognizer.checkForValidTags(""),
				"empty tags - accepted, the field is optional");
	}

	/**********
	 * <p> Test 18 (Negative): tags, tags field over 200 characters total. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Tags are too long." </p>
	 */
	@Test
	@Order(18)
	@DisplayName("Test 18 Tags: tags field over 200 characters total (US-C1-V, US-U1-V, N)")
	void test18TagsOver200AreRejected() {
		expectError(18, LessonRecognizer.checkForValidTags(rep("a", 201)), "Tags are too long.",
				"tags field over 200 characters total");
	}

	/**********
	 * <p> Test 19 (Negative): tags, a single 31-character tag - over the 30-character limit. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Tags are too long." </p>
	 */
	@Test
	@Order(19)
	@DisplayName("Test 19 Tags: a single 31-character tag - over the 30-character limit (US-C1-V, US-U1-V, N)")
	void test19TagOver30IsRejected() {
		expectError(19, LessonRecognizer.checkForValidTags(rep("a", 31)), "Tags are too long.",
				"a single 31-character tag - over the 30-character limit");
	}

	/**********
	 * <p> Test 20 (Negative): tags, tags containing a tab (control character). </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Tags contain an invalid character." </p>
	 */
	@Test
	@Order(20)
	@DisplayName("Test 20 Tags: tags containing a tab (control character) (US-C1-V, US-U1-V, N)")
	void test20TagsWithTabAreRejected() {
		expectError(20, LessonRecognizer.checkForValidTags("java, deb\tug"),
				"Tags contain an invalid character.", "tags containing a tab (control character)");
	}

	/**********
	 * <p> Tests 21-25: category. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1 and US-U1 (a listed category is accepted), US-C1-V and
	 * US-U1-V (any other value gets a specific message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidCategory accepts only an exact,
	 * case-sensitive match with one of LessonRecognizer.VALID_CATEGORIES, the values the
	 * Category drop-down offers. </p>
	 *
	 * <p> Tests: 21 "Technical" (valid); 22 "General" (valid); 23 "Marketing" (not in the
	 * list); 24 blank (not in the list); 25 "TECHNICAL" (wrong case). </p>
	 *
	 * <p> Runs Tests 21 to 25 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runCategoryTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Category Recognizer Tests (US-C1, US-C1-V, US-U1-V) ---");
		tests.test21TechnicalCategoryIsValid();
		tests.test22GeneralCategoryIsValid();
		tests.test23UnlistedCategoryIsRejected();
		tests.test24BlankCategoryIsRejected();
		tests.test25WrongCaseCategoryIsRejected();
	}

	/**********
	 * <p> Test 21 (Positive): category, "Technical". </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(21)
	@DisplayName("Test 21 Category: \"Technical\" (US-C1, US-U1, P)")
	void test21TechnicalCategoryIsValid() {
		expectValid(21, LessonRecognizer.checkForValidCategory("Technical"), "\"Technical\"");
	}

	/**********
	 * <p> Test 22 (Positive): category, "General". </p>
	 *
	 * <p> <b>Requirements:</b> US-C1, US-U1.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(22)
	@DisplayName("Test 22 Category: \"General\" (US-C1, US-U1, P)")
	void test22GeneralCategoryIsValid() {
		expectValid(22, LessonRecognizer.checkForValidCategory("General"), "\"General\"");
	}

	/**********
	 * <p> Test 23 (Negative): category, "Marketing" - not in the fixed list. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Please choose a valid category." </p>
	 */
	@Test
	@Order(23)
	@DisplayName("Test 23 Category: \"Marketing\" - not in the fixed list (US-C1-V, US-U1-V, N)")
	void test23UnlistedCategoryIsRejected() {
		expectError(23, LessonRecognizer.checkForValidCategory("Marketing"),
				"Please choose a valid category.", "\"Marketing\" - not in the fixed list");
	}

	/**********
	 * <p> Test 24 (Negative): category, blank category. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Please choose a valid category." </p>
	 */
	@Test
	@Order(24)
	@DisplayName("Test 24 Category: blank category (US-C1-V, US-U1-V, N)")
	void test24BlankCategoryIsRejected() {
		expectError(24, LessonRecognizer.checkForValidCategory(""),
				"Please choose a valid category.", "blank category");
	}

	/**********
	 * <p> Test 25 (Negative): category, "TECHNICAL" - matching is case-sensitive. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1-V, US-U1-V.  <b>Expected:</b> the input is rejected with
	 * exactly the message "Please choose a valid category." </p>
	 */
	@Test
	@Order(25)
	@DisplayName("Test 25 Category: \"TECHNICAL\" - matching is case-sensitive (US-C1-V, US-U1-V, N)")
	void test25WrongCaseCategoryIsRejected() {
		expectError(25, LessonRecognizer.checkForValidCategory("TECHNICAL"),
				"Please choose a valid category.", "\"TECHNICAL\" - matching is case-sensitive");
	}

	/**********
	 * <p> Tests 26-30: search and filter text. </p>
	 *
	 * <p> <b>Requirements:</b> US-R2 (valid search text is accepted) and US-R2-V (too long or
	 * invalid search text gets a specific message; empty text is not an error). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidSearchText accepts empty text,
	 * allows up to 254 characters, and rejects control characters. </p>
	 *
	 * <p> Tests: 26 normal (valid); 27 empty (valid, finds nothing); 28 254 characters (valid,
	 * maximum); 29 255 characters (too long); 30 contains a tab (invalid character). </p>
	 *
	 * <p> Runs Tests 26 to 30 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runSearchTextTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Search/Filter Text Recognizer Tests (US-R2, US-R2-V) ---");
		tests.test26NormalSearchTextIsValid();
		tests.test27EmptySearchTextIsValid();
		tests.test28MaxLengthSearchTextIsValid();
		tests.test29SearchTextOver254IsRejected();
		tests.test30SearchTextWithTabIsRejected();
	}

	/**********
	 * <p> Test 26 (Positive): search text, normal search text. </p>
	 *
	 * <p> <b>Requirements:</b> US-R2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(26)
	@DisplayName("Test 26 Search text: normal search text (US-R2, P)")
	void test26NormalSearchTextIsValid() {
		expectValid(26, LessonRecognizer.checkForValidSearchText("java debugging"),
				"normal search text");
	}

	/**********
	 * <p> Test 27 (Positive): search text, empty search text - valid input, it just finds nothing.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> US-R2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(27)
	@DisplayName("Test 27 Search text: empty search text - valid input, it just finds nothing (US-R2, P)")
	void test27EmptySearchTextIsValid() {
		expectValid(27, LessonRecognizer.checkForValidSearchText(""),
				"empty search text - valid input, it just finds nothing");
	}

	/**********
	 * <p> Test 28 (Positive): search text, 254-character search - meets the maximum boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-R2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(28)
	@DisplayName("Test 28 Search text: 254-character search - meets the maximum boundary (US-R2, P)")
	void test28MaxLengthSearchTextIsValid() {
		expectValid(28, LessonRecognizer.checkForValidSearchText(rep("a", 254)),
				"254-character search - meets the maximum boundary");
	}

	/**********
	 * <p> Test 29 (Negative): search text, 255-character search - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> US-R2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Search text is too long." </p>
	 */
	@Test
	@Order(29)
	@DisplayName("Test 29 Search text: 255-character search - over the maximum (US-R2-V, N)")
	void test29SearchTextOver254IsRejected() {
		expectError(29, LessonRecognizer.checkForValidSearchText(rep("a", 255)),
				"Search text is too long.", "255-character search - over the maximum");
	}

	/**********
	 * <p> Test 30 (Negative): search text, search text containing a tab (control character). </p>
	 *
	 * <p> <b>Requirements:</b> US-R2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Search text contains an invalid character." </p>
	 */
	@Test
	@Order(30)
	@DisplayName("Test 30 Search text: search text containing a tab (control character) (US-R2-V, N)")
	void test30SearchTextWithTabIsRejected() {
		expectError(30, LessonRecognizer.checkForValidSearchText("java\tdebug"),
				"Search text contains an invalid character.",
				"search text containing a tab (control character)");
	}

	/**********
	 * <p> Tests 31-38: experience time/effort value. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2 (a positive number of hours is accepted) and US-U2-V (a
	 * negative, zero, too large, or non-numeric value gets a specific message).  The same rule
	 * is used for each team member's hours (E7). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidExperienceValue checks that the text
	 * is a plain decimal number (digits and at most one point, starting and ending with a digit)
	 * before parsing it, so no input can cause an exception, and then requires more than 0 and
	 * at most 10000. </p>
	 *
	 * <p> Tests: 31 "4.5" (valid); 32 "0.1" (valid, smallest above zero); 33 "10000" (valid,
	 * maximum); 34 "0"; 35 "-3"; 36 "10000.01"; 37 "abc"; 38 empty.  Tests 34-38 all expect
	 * "Time/effort must be a positive number.". </p>
	 *
	 * <p> Runs Tests 31 to 38 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runExperienceValueTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Experience Value Recognizer Tests (US-U2, US-U2-V, E7) ---");
		tests.test31DecimalHoursAreValid();
		tests.test32SmallestHoursAreValid();
		tests.test33MaximumHoursAreValid();
		tests.test34ZeroHoursAreRejected();
		tests.test35NegativeHoursAreRejected();
		tests.test36HoursOverMaximumAreRejected();
		tests.test37NonNumericHoursAreRejected();
		tests.test38EmptyHoursAreRejected();
	}

	/**********
	 * <p> Test 31 (Positive): experience value, "4.5". </p>
	 *
	 * <p> <b>Requirements:</b> US-U2, E7.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(31)
	@DisplayName("Test 31 Experience value: \"4.5\" (US-U2, E7, P)")
	void test31DecimalHoursAreValid() {
		expectValid(31, LessonRecognizer.checkForValidExperienceValue("4.5"), "\"4.5\"");
	}

	/**********
	 * <p> Test 32 (Positive): experience value, "0.1" - smallest value above zero. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2, E7.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(32)
	@DisplayName("Test 32 Experience value: \"0.1\" - smallest value above zero (US-U2, E7, P)")
	void test32SmallestHoursAreValid() {
		expectValid(32, LessonRecognizer.checkForValidExperienceValue("0.1"),
				"\"0.1\" - smallest value above zero");
	}

	/**********
	 * <p> Test 33 (Positive): experience value, "10000" - meets the maximum boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2, E7.  <b>Expected:</b> the input is accepted, so the
	 * recognizer returns an empty String. </p>
	 */
	@Test
	@Order(33)
	@DisplayName("Test 33 Experience value: \"10000\" - meets the maximum boundary (US-U2, E7, P)")
	void test33MaximumHoursAreValid() {
		expectValid(33, LessonRecognizer.checkForValidExperienceValue("10000"),
				"\"10000\" - meets the maximum boundary");
	}

	/**********
	 * <p> Test 34 (Negative): experience value, "0" - must be greater than zero. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, E7.  <b>Expected:</b> the input is rejected with exactly
	 * the message "Time/effort must be a positive number." </p>
	 */
	@Test
	@Order(34)
	@DisplayName("Test 34 Experience value: \"0\" - must be greater than zero (US-U2-V, E7, N)")
	void test34ZeroHoursAreRejected() {
		expectError(34, LessonRecognizer.checkForValidExperienceValue("0"),
				"Time/effort must be a positive number.", "\"0\" - must be greater than zero");
	}

	/**********
	 * <p> Test 35 (Negative): experience value, "-3" - negative. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, E7.  <b>Expected:</b> the input is rejected with exactly
	 * the message "Time/effort must be a positive number." </p>
	 */
	@Test
	@Order(35)
	@DisplayName("Test 35 Experience value: \"-3\" - negative (US-U2-V, E7, N)")
	void test35NegativeHoursAreRejected() {
		expectError(35, LessonRecognizer.checkForValidExperienceValue("-3"),
				"Time/effort must be a positive number.", "\"-3\" - negative");
	}

	/**********
	 * <p> Test 36 (Negative): experience value, "10000.01" - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, E7.  <b>Expected:</b> the input is rejected with exactly
	 * the message "Time/effort must be a positive number." </p>
	 */
	@Test
	@Order(36)
	@DisplayName("Test 36 Experience value: \"10000.01\" - over the maximum (US-U2-V, E7, N)")
	void test36HoursOverMaximumAreRejected() {
		expectError(36, LessonRecognizer.checkForValidExperienceValue("10000.01"),
				"Time/effort must be a positive number.", "\"10000.01\" - over the maximum");
	}

	/**********
	 * <p> Test 37 (Negative): experience value, "abc" - not a number. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, E7.  <b>Expected:</b> the input is rejected with exactly
	 * the message "Time/effort must be a positive number." </p>
	 */
	@Test
	@Order(37)
	@DisplayName("Test 37 Experience value: \"abc\" - not a number (US-U2-V, E7, N)")
	void test37NonNumericHoursAreRejected() {
		expectError(37, LessonRecognizer.checkForValidExperienceValue("abc"),
				"Time/effort must be a positive number.", "\"abc\" - not a number");
	}

	/**********
	 * <p> Test 38 (Negative): experience value, empty input. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, E7.  <b>Expected:</b> the input is rejected with exactly
	 * the message "Time/effort must be a positive number." </p>
	 */
	@Test
	@Order(38)
	@DisplayName("Test 38 Experience value: empty input (US-U2-V, E7, N)")
	void test38EmptyHoursAreRejected() {
		expectError(38, LessonRecognizer.checkForValidExperienceValue(""),
				"Time/effort must be a positive number.", "empty input");
	}

	/**********
	 * <p> Tests 39-45: experience description. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2 (a description of what was done is accepted) and US-U2-V
	 * (a missing, too short, or too long description gets a specific message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> checkForValidExperienceDescription requires 5 to
	 * 2000 characters that are not all blank. </p>
	 *
	 * <p> Tests: 39 normal (valid); 40 5 characters (valid, minimum); 41 2000 characters
	 * (valid, maximum); 42 4 characters; 43 2001 characters; 44 only spaces; 45 empty.  Tests
	 * 42-45 all expect "Please describe what was done.". </p>
	 *
	 * <p> Runs Tests 39 to 45 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runExperienceDescriptionTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Experience Description Recognizer Tests (US-U2, US-U2-V) ---");
		tests.test39NormalExperienceDescriptionIsValid();
		tests.test40FiveCharacterExperienceDescriptionIsValid();
		tests.test41MaxLengthExperienceDescriptionIsValid();
		tests.test42FourCharacterExperienceDescriptionIsRejected();
		tests.test43ExperienceDescriptionOver2000IsRejected();
		tests.test44BlankExperienceDescriptionIsRejected();
		tests.test45EmptyExperienceDescriptionIsRejected();
	}

	/**********
	 * <p> Test 39 (Positive): experience description, normal description. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(39)
	@DisplayName("Test 39 Experience description: normal description (US-U2, P)")
	void test39NormalExperienceDescriptionIsValid() {
		expectValid(39,
				LessonRecognizer.checkForValidExperienceDescription("Paired with two teammates and reused the earlier fix."),
				"normal description");
	}

	/**********
	 * <p> Test 40 (Positive): experience description, 5-character description - meets the minimum
	 * boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(40)
	@DisplayName("Test 40 Experience description: 5-character description - meets the minimum boundary (US-U2, P)")
	void test40FiveCharacterExperienceDescriptionIsValid() {
		expectValid(40, LessonRecognizer.checkForValidExperienceDescription(rep("A", 5)),
				"5-character description - meets the minimum boundary");
	}

	/**********
	 * <p> Test 41 (Positive): experience description, 2000-character description - meets the
	 * maximum boundary. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(41)
	@DisplayName("Test 41 Experience description: 2000-character description - meets the maximum boundary (US-U2, P)")
	void test41MaxLengthExperienceDescriptionIsValid() {
		expectValid(41, LessonRecognizer.checkForValidExperienceDescription(rep("A", 2000)),
				"2000-character description - meets the maximum boundary");
	}

	/**********
	 * <p> Test 42 (Negative): experience description, "Fixd" (4 characters) - under the minimum.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Please describe what was done." </p>
	 */
	@Test
	@Order(42)
	@DisplayName("Test 42 Experience description: \"Fixd\" (4 characters) - under the minimum (US-U2-V, N)")
	void test42FourCharacterExperienceDescriptionIsRejected() {
		expectError(42, LessonRecognizer.checkForValidExperienceDescription("Fixd"),
				"Please describe what was done.", "\"Fixd\" (4 characters) - under the minimum");
	}

	/**********
	 * <p> Test 43 (Negative): experience description, 2001-character description - over the
	 * maximum. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Please describe what was done." </p>
	 */
	@Test
	@Order(43)
	@DisplayName("Test 43 Experience description: 2001-character description - over the maximum (US-U2-V, N)")
	void test43ExperienceDescriptionOver2000IsRejected() {
		expectError(43, LessonRecognizer.checkForValidExperienceDescription(rep("A", 2001)),
				"Please describe what was done.", "2001-character description - over the maximum");
	}

	/**********
	 * <p> Test 44 (Negative): experience description, whitespace-only description. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Please describe what was done." </p>
	 */
	@Test
	@Order(44)
	@DisplayName("Test 44 Experience description: whitespace-only description (US-U2-V, N)")
	void test44BlankExperienceDescriptionIsRejected() {
		expectError(44, LessonRecognizer.checkForValidExperienceDescription("   "),
				"Please describe what was done.", "whitespace-only description");
	}

	/**********
	 * <p> Test 45 (Negative): experience description, empty description. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Please describe what was done." </p>
	 */
	@Test
	@Order(45)
	@DisplayName("Test 45 Experience description: empty description (US-U2-V, N)")
	void test45EmptyExperienceDescriptionIsRejected() {
		expectError(45, LessonRecognizer.checkForValidExperienceDescription(""),
				"Please describe what was done.", "empty description");
	}

	/**********
	 * <p> Tests 46-49: team member name (added in TP2). </p>
	 *
	 * <p> <b>Requirement:</b> E7, each team member listed on an experience entry needs a valid
	 * name. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidMemberName requires a non-blank name of
	 * at most 100 characters with no control characters, and names the position of the first
	 * invalid character. </p>
	 *
	 * <p> Tests: 46 "Virgil Jones" (valid); 47 101 characters (too long); 48 only spaces
	 * (required); 49 contains a tab (invalid character at position 4). </p>
	 *
	 * <p> Runs Tests 46 to 49 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runMemberNameTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Team Member Name Recognizer Tests (E7) ---");
		tests.test46NormalMemberNameIsValid();
		tests.test47MemberNameOver100IsRejected();
		tests.test48BlankMemberNameIsRequired();
		tests.test49MemberNameWithTabIsRejected();
	}

	/**********
	 * <p> Test 46 (Positive): member name, "Virgil Jones" - valid name. </p>
	 *
	 * <p> <b>Requirements:</b> E7.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(46)
	@DisplayName("Test 46 Member name: \"Virgil Jones\" - valid name (E7, P)")
	void test46NormalMemberNameIsValid() {
		expectValid(46, LessonRecognizer.checkForValidMemberName("Virgil Jones"),
				"\"Virgil Jones\" - valid name");
	}

	/**********
	 * <p> Test 47 (Negative): member name, 101-character name - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> E7.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Team member name must be 100 characters or fewer." </p>
	 */
	@Test
	@Order(47)
	@DisplayName("Test 47 Member name: 101-character name - over the maximum (E7, N)")
	void test47MemberNameOver100IsRejected() {
		expectError(47, LessonRecognizer.checkForValidMemberName(rep("A", 101)),
				"Team member name must be 100 characters or fewer.",
				"101-character name - over the maximum");
	}

	/**********
	 * <p> Test 48 (Negative): member name, whitespace-only name. </p>
	 *
	 * <p> <b>Requirements:</b> E7.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Team member name is required." </p>
	 */
	@Test
	@Order(48)
	@DisplayName("Test 48 Member name: whitespace-only name (E7, N)")
	void test48BlankMemberNameIsRequired() {
		expectError(48, LessonRecognizer.checkForValidMemberName("   "),
				"Team member name is required.", "whitespace-only name");
	}

	/**********
	 * <p> Test 49 (Negative): member name, "Ana[TAB]B" - control character. </p>
	 *
	 * <p> <b>Requirements:</b> E7.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Team member name contains an invalid character at position 4." </p>
	 */
	@Test
	@Order(49)
	@DisplayName("Test 49 Member name: \"Ana[TAB]B\" - control character (E7, N)")
	void test49MemberNameWithTabIsRejected() {
		expectError(49, LessonRecognizer.checkForValidMemberName("Ana\tB"),
				"Team member name contains an invalid character at position 4.",
				"\"Ana[TAB]B\" - control character");
	}

	/**********
	 * <p> Tests 50-54: root cause and recommendation (added in TP2). </p>
	 *
	 * <p> <b>Requirement:</b> E6, the optional coached fields from AICoaching.pdf (Root Cause
	 * and Recommendation) accept an empty value or up to 2000 characters, and longer text gets a
	 * message that names the field. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidRootCause and
	 * checkForValidRecommendation share checkOptionalCoachedText, which accepts empty text,
	 * limits the length to 2000, and rejects control characters other than line breaks. </p>
	 *
	 * <p> Tests: 50 empty root cause (valid, optional); 51 2000-character root cause (valid,
	 * maximum); 52 2001-character root cause (too long); 53 normal recommendation (valid); 54
	 * 2001-character recommendation (too long). </p>
	 *
	 * <p> Runs Tests 50 to 54 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runCoachedTextTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Root Cause and Recommendation Recognizer Tests (E6) ---");
		tests.test50EmptyRootCauseIsValid();
		tests.test51MaxLengthRootCauseIsValid();
		tests.test52RootCauseOver2000IsRejected();
		tests.test53NormalRecommendationIsValid();
		tests.test54RecommendationOver2000IsRejected();
	}

	/**********
	 * <p> Test 50 (Positive): root cause, empty root cause - accepted, the field is optional. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(50)
	@DisplayName("Test 50 Root cause: empty root cause - accepted, the field is optional (E6, P)")
	void test50EmptyRootCauseIsValid() {
		expectValid(50, LessonRecognizer.checkForValidRootCause(""),
				"empty root cause - accepted, the field is optional");
	}

	/**********
	 * <p> Test 51 (Positive): root cause, 2000-character root cause - meets the maximum boundary.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(51)
	@DisplayName("Test 51 Root cause: 2000-character root cause - meets the maximum boundary (E6, P)")
	void test51MaxLengthRootCauseIsValid() {
		expectValid(51, LessonRecognizer.checkForValidRootCause(rep("R", 2000)),
				"2000-character root cause - meets the maximum boundary");
	}

	/**********
	 * <p> Test 52 (Negative): root cause, 2001-character root cause - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Root cause must be 2000 characters or fewer." </p>
	 */
	@Test
	@Order(52)
	@DisplayName("Test 52 Root cause: 2001-character root cause - over the maximum (E6, N)")
	void test52RootCauseOver2000IsRejected() {
		expectError(52, LessonRecognizer.checkForValidRootCause(rep("R", 2001)),
				"Root cause must be 2000 characters or fewer.",
				"2001-character root cause - over the maximum");
	}

	/**********
	 * <p> Test 53 (Positive): recommendation, normal recommendation. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(53)
	@DisplayName("Test 53 Recommendation: normal recommendation (E6, P)")
	void test53NormalRecommendationIsValid() {
		expectValid(53,
				LessonRecognizer.checkForValidRecommendation("Schedule integration testing in the first week."),
				"normal recommendation");
	}

	/**********
	 * <p> Test 54 (Negative): recommendation, 2001-character recommendation - over the maximum.
	 * </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Recommendation must be 2000 characters or fewer." </p>
	 */
	@Test
	@Order(54)
	@DisplayName("Test 54 Recommendation: 2001-character recommendation - over the maximum (E6, N)")
	void test54RecommendationOver2000IsRejected() {
		expectError(54, LessonRecognizer.checkForValidRecommendation(rep("R", 2001)),
				"Recommendation must be 2000 characters or fewer.",
				"2001-character recommendation - over the maximum");
	}

	/**********
	 * <p> Tests 55-58: Curator search terms (added in TP2). </p>
	 *
	 * <p> <b>Requirement:</b> E6, the search terms a Phase Three Curator adds are limited to 500
	 * characters in total and 50 per comma-separated term, with no control characters. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidSearchTerms accepts empty text, checks
	 * the total length and invalid characters, then checks each term after splitting on
	 * commas. </p>
	 *
	 * <p> Tests: 55 normal terms (valid); 56 501 characters in total (too long); 57 one
	 * 51-character term (too long); 58 contains a tab (invalid character). </p>
	 *
	 * <p> Runs Tests 55 to 58 in order for the console program.  Under JUnit each test
	 * below is its own test method. </p>
	 */
	private static void runSearchTermsTests(LessonRecognizerTest tests) {
		System.out.println("\n--- Curator Search Terms Recognizer Tests (E6) ---");
		tests.test55NormalSearchTermsAreValid();
		tests.test56SearchTermsOver500AreRejected();
		tests.test57SearchTermOver50IsRejected();
		tests.test58SearchTermsWithTabAreRejected();
	}

	/**********
	 * <p> Test 55 (Positive): search terms, normal comma-separated search terms. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is accepted, so the recognizer
	 * returns an empty String. </p>
	 */
	@Test
	@Order(55)
	@DisplayName("Test 55 Search terms: normal comma-separated search terms (E6, P)")
	void test55NormalSearchTermsAreValid() {
		expectValid(55, LessonRecognizer.checkForValidSearchTerms("ci, pipelines, release"),
				"normal comma-separated search terms");
	}

	/**********
	 * <p> Test 56 (Negative): search terms, 501 characters in total - over the maximum. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Search terms are too long." </p>
	 */
	@Test
	@Order(56)
	@DisplayName("Test 56 Search terms: 501 characters in total - over the maximum (E6, N)")
	void test56SearchTermsOver500AreRejected() {
		expectError(56, LessonRecognizer.checkForValidSearchTerms(rep("a", 501)),
				"Search terms are too long.", "501 characters in total - over the maximum");
	}

	/**********
	 * <p> Test 57 (Negative): search terms, a single 51-character term - over the 50 limit. </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Search terms are too long." </p>
	 */
	@Test
	@Order(57)
	@DisplayName("Test 57 Search terms: a single 51-character term - over the 50 limit (E6, N)")
	void test57SearchTermOver50IsRejected() {
		expectError(57, LessonRecognizer.checkForValidSearchTerms("ok, " + rep("a", 51)),
				"Search terms are too long.", "a single 51-character term - over the 50 limit");
	}

	/**********
	 * <p> Test 58 (Negative): search terms, search terms containing a tab (control character).
	 * </p>
	 *
	 * <p> <b>Requirements:</b> E6.  <b>Expected:</b> the input is rejected with exactly the
	 * message "Search terms contain an invalid character." </p>
	 */
	@Test
	@Order(58)
	@DisplayName("Test 58 Search terms: search terms containing a tab (control character) (E6, N)")
	void test58SearchTermsWithTabAreRejected() {
		expectError(58, LessonRecognizer.checkForValidSearchTerms("ci\tcd"),
				"Search terms contain an invalid character.",
				"search terms containing a tab (control character)");
	}



	/**********
	 * Prints one "Test N: ... - Pass." line for a test whose input should be VALID, which means
	 * the recognizer returns an empty String.  Under JUnit it then asserts that result.
	 *
	 * @param testNumber	the test number
	 * @param actualMessage	what the recognizer returned
	 * @param description	what the test checks
	 */
	private static void expectValid(int testNumber, String actualMessage, String description) {
		numberOfTests++;
		boolean passed = actualMessage.isEmpty();
		if (!passed) numberFailed++;
		System.out.println("Test " + testNumber + ": " + description + " - "
				+ (passed ? "Pass." : "FAIL."));
		if (!passed)
			System.out.println("        expected valid but got: '" + actualMessage + "'");
		// Under JUnit, fail this test with a message that names it; the console program counts
		// failures instead, so every test runs before the totals
		if (!consoleMode)
			assertEquals("", actualMessage, "Test " + testNumber + " expected a valid input");
	}

	/**********
	 * Prints one "Test N: ... - Pass." line for a test whose input should be REJECTED with one
	 * exact message.  Comparing the exact text is how these tests check the "specific, helpful
	 * error message" requirement.  Under JUnit it then asserts that comparison.
	 *
	 * @param testNumber		the test number
	 * @param actualMessage		what the recognizer returned
	 * @param expectedMessage	the exact message the test expects
	 * @param description		what the test checks
	 */
	private static void expectError(int testNumber, String actualMessage, String expectedMessage,
			String description) {
		numberOfTests++;
		boolean passed = expectedMessage.equals(actualMessage);
		if (!passed) numberFailed++;
		System.out.println("Test " + testNumber + ": " + description + " - "
				+ (passed ? "Pass." : "FAIL."));
		if (!passed)
			System.out.println("        expected: '" + expectedMessage + "'  actual: '"
					+ actualMessage + "'");
		if (!consoleMode)
			assertEquals(expectedMessage, actualMessage, "Test " + testNumber);
	}

	/**********
	 * Builds a String by repeating one piece, used for the exact-boundary-length inputs.
	 *
	 * @param piece	the text to repeat
	 * @param times	how many times to repeat it
	 * @return the repeated text
	 */
	private static String rep(String piece, int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++) sb.append(piece);
		return sb.toString();
	}
}
