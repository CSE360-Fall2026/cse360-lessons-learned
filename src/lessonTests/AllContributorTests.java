package lessonTests;

/*******
 * <p> Title: AllContributorTests Class. </p>
 *
 * <p> Description: Runs every semi-automated test class in this package, in CRUD order, as one
 * suite with one set of totals: CreateLessonTests, ReadLessonTests, UpdateLessonTests,
 * AddExperienceTests, DeleteLessonTests, LessonsLearnedEntityTests, and
 * LessonsLearnedListEntityTests.  Together they cover the fourteen Contributor User Stories
 * and the entity-class requirements E1 to E7 with at least two tests each; the
 * Requirements-Test Traceability list names every test and the requirement it
 * demonstrates.</p>
 *
 * <p> The field-by-field input validation tests are run separately by
 * inputValidation.LessonRecognizerTest, following the Foundations pattern of one test class per
 * recognizer, and the GUI behaviors are checked by the manual tests in ManualGuiTests.</p>
 *
 * <p> FoundationsMain must not be running, because the H2 database file allows only one
 * connection at a time.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2 Task 6: one runner for every test class (replaces
 * 							ContributorFunctionalTest, whose 21 checks are kept, under the same
 * 							names, in the classes listed above)
 */
public class AllContributorTests {

	/**********
	 * Default constructor is not used; every member is static.
	 */
	public AllContributorTests() {
	}

	/**********
	 * Runs every test class as one suite and prints the combined totals.  Ends with an
	 * AssertionError if any test failed.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor and Entity Tests - all requirement groups",
				CreateLessonTests::runAll,
				ReadLessonTests::runAll,
				UpdateLessonTests::runAll,
				AddExperienceTests::runAll,
				DeleteLessonTests::runAll,
				LessonsLearnedEntityTests::runAll,
				LessonsLearnedListEntityTests::runAll);
	}
}
