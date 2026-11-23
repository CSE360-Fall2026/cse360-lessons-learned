package lessonTests;

import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.makeLesson;
import static lessonTests.TestSupport.requirement;
import static lessonTests.TestSupport.run;
import static lessonTests.TestSupport.titles;
import static lessonTests.TestSupport.uniqueUser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;

/*******
 * <p> Title: LessonsLearnedListEntityTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the LessonsLearnedList entity class itself (TP2
 * Task 3).  The requirements are:</p>
 * <ul>
 * <li> E3: LessonsLearnedList implements CRUD for a collection of lessons: it loads lessons from
 * 		the database (all, or one owner's), and adds, finds, replaces, and removes lessons in the
 * 		list, refusing operations that make no sense. </li>
 * <li> E4: LessonsLearnedList produces subsets (by title text, by keywords, by category, and by
 * 		tag) as new lists, leaving the original list unchanged.  US-R2 uses the title subset, and
 * 		the keyword subset is ready for the Phase Three search epic. </li>
 * </ul>
 *
 * <p> The E4 tests and most E3 tests work on a list built in memory, because the subset and
 * collection operations do not touch the database; that makes their expected results
 * independent of anything else stored.  See TestSupport for how to read the output.</p>
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
 * @version 1.00		2026-10-16 TP2 Task 6: entity tests for LessonsLearnedList
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LessonsLearnedListEntityTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public LessonsLearnedListEntityTests() {
	}

	/**********
	 * Runs the LessonsLearnedList entity tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Entity Tests - LessonsLearnedList (E3, E4)",
				LessonsLearnedListEntityTests::runAll);
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
		LessonsLearnedListEntityTests tests = new LessonsLearnedListEntityTests();
		requirement("E3", "LessonsLearnedList loads, adds, finds, replaces, and removes lessons");
		run(tests::testLoadByOwnerAndLoadAll);
		run(tests::testAddFindReplaceAndRemove);
		run(tests::testInvalidCollectionOperationsRefused);

		requirement("E4", "LessonsLearnedList returns subsets as new lists, original unchanged");
		run(tests::testFilterByKeywordsRequiresEveryKeyword);
		run(tests::testFilterByCategoryAndTag);
		run(tests::testFiltersReturnEmptyListForBlankOrNoMatch);
	}


	/**********
	 * Builds an unsaved lesson with a given id, for the in-memory tests.
	 *
	 * @param id			the id to give it
	 * @param title			the title
	 * @param description	the description
	 * @param tags			the tags
	 * @param category		the category
	 * @return the lesson
	 */
	private static LessonsLearned lesson(int id, String title, String description, String tags,
			String category) {
		LessonsLearned lesson = new LessonsLearned("listTester", title, description, tags,
				category);
		lesson.setId(id);
		return lesson;
	}


	/**********
	 * Builds the three-lesson list the E4 tests filter.
	 *
	 * @return a list holding "Deploy pipeline fix", "Sprint retro notes", and "Release checklist"
	 */
	private static LessonsLearnedList sampleList() {
		LessonsLearnedList list = new LessonsLearnedList();
		list.add(lesson(1, "Deploy pipeline fix",
				"Rolled back the release after the smoke test failed.", "ci, release", "Technical"));
		list.add(lesson(2, "Sprint retro notes", "The team agreed to timebox standups.",
				"teamwork", "Teamwork"));
		list.add(lesson(3, "Release checklist", "A checklist for every release, with a smoke test.",
				"release", "Technical"));
		return list;
	}


	/*-*******************************************************************************************

	E3: the collection

	 */

	/**********
	 * <p> Test E3-P1 (Positive): loadByOwner and loadAll Read Lessons From the Database. </p>
	 *
	 * <p> <b>Requirement:</b> E3 (Read for a collection), the basis of US-R1 and US-R4. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> loadByOwner and loadAll call
	 * Database.getLessonsByOwner and Database.getAllLessons and wrap the rows in a new
	 * list. </p>
	 *
	 * <p> <b>Steps:</b> one owner creates two lessons; load that owner's list and the full
	 * list. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the owner's list holds exactly those two
	 * titles and the full list contains both of them. </p>
	 *
	 * <p> Validates: LessonsLearnedList.loadAll(), LessonsLearnedList.loadByOwner(),
	 * LessonsLearnedList.contains(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("E3-P1 loadByOwner and loadAll Read Lessons From the Database (E3, P)")
	void testLoadByOwnerAndLoadAll() {
		String owner = uniqueUser("listLoad");
		int first = makeLesson(owner, "Loaded lesson one", "General");
		int second = makeLesson(owner, "Loaded lesson two", "General");

		LessonsLearnedList all = LessonsLearnedList.loadAll();
		check("E3-P1", true, "loadByOwner and loadAll Read Lessons From the Database",
				"owner's list: 2 lesson(s): Loaded lesson one, Loaded lesson two; full list "
				+ "contains both: true",
				"owner's list: " + titles(LessonsLearnedList.loadByOwner(owner))
				+ "; full list contains both: " + (all.contains(first) && all.contains(second)));
	}


	/**********
	 * <p> Test E3-P2 (Positive): Add, Find, Replace, and Remove Work on the List. </p>
	 *
	 * <p> <b>Requirement:</b> E3 (Create, Read, Update, and Delete on the collection), used by
	 * the Home page to keep its displayed list in step after a change. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> add() appends, getById() finds by id, replace()
	 * swaps in the lesson with the same id, and removeById() removes it. </p>
	 *
	 * <p> <b>Steps:</b> add lessons 1 and 2; find 2; replace 2 with a new title; remove 1. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means each step reports the value shown and the
	 * list ends holding only the replaced lesson. </p>
	 *
	 * <p> Validates: LessonsLearnedList.add(), LessonsLearnedList.getById(),
	 * LessonsLearnedList.replace(), LessonsLearnedList.removeById(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("E3-P2 Add, Find, Replace, and Remove Work on the List (E3, P)")
	void testAddFindReplaceAndRemove() {
		LessonsLearnedList list = new LessonsLearnedList();
		boolean added = list.add(lesson(1, "One", "First description.", "", "General"))
				&& list.add(lesson(2, "Two", "Second description.", "", "General"));
		String found = list.getById(2).getTitle();
		boolean replaced = list.replace(lesson(2, "Two replaced", "New description.", "",
				"General"));
		boolean removed = list.removeById(1);

		check("E3-P2", true, "Add, Find, Replace, and Remove Work on the List",
				"added: true; found: Two; replaced: true; removed: true; list: 1 lesson(s): Two "
				+ "replaced",
				"added: " + added + "; found: " + found + "; replaced: " + replaced + "; removed: "
				+ removed + "; list: " + titles(list));
	}


	/**********
	 * <p> Test E3-N1 (Negative): Invalid Collection Operations Are Refused. </p>
	 *
	 * <p> <b>Requirement:</b> E3, operations that make no sense are refused safely (false or
	 * null) instead of corrupting the list or throwing an exception. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> add() refuses null and a second lesson with an id
	 * already in the list; get(), getById(), replace(), and removeById() report a missing
	 * lesson with null or false. </p>
	 *
	 * <p> <b>Steps:</b> on a one-lesson list, try each invalid operation, then check the
	 * size. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means every operation is refused as shown and the
	 * list still holds 1 lesson. </p>
	 *
	 * <p> Validates: LessonsLearnedList.add(), LessonsLearnedList.getById(),
	 * LessonsLearnedList.replace(), LessonsLearnedList.removeById(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("E3-N1 Invalid Collection Operations Are Refused (E3, N)")
	void testInvalidCollectionOperationsRefused() {
		LessonsLearnedList list = new LessonsLearnedList();
		list.add(lesson(1, "Only lesson", "The only description.", "", "General"));

		String actual = "add null: " + list.add(null)
				+ "; add duplicate id: " + list.add(lesson(1, "Copy", "Duplicate id.", "", "General"))
				+ "; get(5): " + list.get(5)
				+ "; getById(99): " + list.getById(99)
				+ "; replace id 99: " + list.replace(lesson(99, "Ghost", "Not in list.", "", "General"))
				+ "; removeById(99): " + list.removeById(99)
				+ "; size: " + list.size();
		check("E3-N1", false, "Invalid Collection Operations Are Refused",
				"add null: false; add duplicate id: false; get(5): null; getById(99): null; "
				+ "replace id 99: false; removeById(99): false; size: 1", actual);
	}


	/*-*******************************************************************************************

	E4: subsets

	 */

	/**********
	 * <p> Test E4-P1 (Positive): Keyword Subset Requires Every Keyword, Ignoring Case. </p>
	 *
	 * <p> <b>Requirement:</b> E4, the keyword subset (decision D5, for the Phase Three search
	 * epic) keeps only lessons whose title, description, or tags contain every keyword. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> filterByKeywords splits the text on spaces and
	 * commas, lower-cases everything, and keeps a lesson only if all words are found. </p>
	 *
	 * <p> <b>Steps:</b> on the three-lesson sample, filter by "RELEASE smoke". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the two lessons mentioning both words are
	 * returned, "Sprint retro notes" is not, and the original list still holds 3. </p>
	 *
	 * <p> Validates: LessonsLearnedList.filterByKeywords(), LessonsLearnedList.add(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("E4-P1 Keyword Subset Requires Every Keyword, Ignoring Case (E4, P)")
	void testFilterByKeywordsRequiresEveryKeyword() {
		LessonsLearnedList list = sampleList();
		LessonsLearnedList subset = list.filterByKeywords("RELEASE smoke");
		check("E4-P1", true, "Keyword Subset Requires Every Keyword, Ignoring Case",
				"subset: 2 lesson(s): Deploy pipeline fix, Release checklist; original still "
				+ "holds 3",
				"subset: " + titles(subset) + "; original still holds " + list.size());
	}


	/**********
	 * <p> Test E4-P2 (Positive): Category and Tag Subsets. </p>
	 *
	 * <p> <b>Requirement:</b> E4, a list can be narrowed by category and by tag (for the
	 * Phase Three browse and search epics). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> filterByCategory keeps exact category matches;
	 * filterByTag keeps lessons whose tags contain the tag, ignoring case. </p>
	 *
	 * <p> <b>Steps:</b> filter the sample by the category "Technical" and by the tag
	 * "TEAMWORK". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the category subset holds the two Technical
	 * lessons and the tag subset holds "Sprint retro notes". </p>
	 *
	 * <p> Validates: LessonsLearnedList.filterByCategory(), LessonsLearnedList.filterByTag(),
	 * LessonsLearnedList.add(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("E4-P2 Category and Tag Subsets (E4, P)")
	void testFilterByCategoryAndTag() {
		LessonsLearnedList list = sampleList();
		check("E4-P2", true, "Category and Tag Subsets",
				"Technical: 2 lesson(s): Deploy pipeline fix, Release checklist; tag TEAMWORK: "
				+ "1 lesson(s): Sprint retro notes",
				"Technical: " + titles(list.filterByCategory("Technical")) + "; tag TEAMWORK: "
				+ titles(list.filterByTag("TEAMWORK")));
	}


	/**********
	 * <p> Test E4-N1 (Negative): Subsets Are Empty for Blank Text or No Match. </p>
	 *
	 * <p> <b>Requirement:</b> E4, together with the US-R2-V rule: blank search text finds
	 * nothing (not everything), a search that matches nothing returns an empty list (not null),
	 * and categories must match exactly. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> each filter returns a new, possibly empty, list and
	 * treats blank text as matching nothing. </p>
	 *
	 * <p> <b>Steps:</b> filter the sample by blank keywords, by "release teamwork" (no lesson
	 * has both), by the category "technical" in lower case, and by blank title text. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means all four subsets are empty. </p>
	 *
	 * <p> Validates: LessonsLearnedList.filterByKeywords(),
	 * LessonsLearnedList.filterByCategory(), LessonsLearnedList.filterByTitleContains(),
	 * LessonsLearnedList.add(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("E4-N1 Subsets Are Empty for Blank Text or No Match (E4, N)")
	void testFiltersReturnEmptyListForBlankOrNoMatch() {
		LessonsLearnedList list = sampleList();
		check("E4-N1", false, "Subsets Are Empty for Blank Text or No Match",
				"blank keywords: 0 lesson(s); release teamwork: 0 lesson(s); category technical: "
				+ "0 lesson(s); blank title text: 0 lesson(s)",
				"blank keywords: " + titles(list.filterByKeywords("  "))
				+ "; release teamwork: " + titles(list.filterByKeywords("release teamwork"))
				+ "; category technical: " + titles(list.filterByCategory("technical"))
				+ "; blank title text: " + titles(list.filterByTitleContains("   ")));
	}
}
