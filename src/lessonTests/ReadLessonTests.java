package lessonTests;

import static lessonTests.TestSupport.DESCRIPTION;
import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.describe;
import static lessonTests.TestSupport.lock;
import static lessonTests.TestSupport.makeLesson;
import static lessonTests.TestSupport.rep;
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
import guiAddExperience.ModelAddExperience;
import guiContributorHome.ModelContributorHome;
import guiLessonDetails.ModelLessonDetails;
import guiTools.LessonResult;
import guiUpdateLesson.ModelUpdateLesson;

/*******
 * <p> Title: ReadLessonTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the Read user stories: US-R1 (view my list),
 * US-R2 (search my titles and view the subset), US-R2-V (search input validation), US-R3 (open
 * one lesson and see its details, experience entries, and locks), US-R3-V (a lesson that does
 * not exist or is not mine fails safely), and US-R4 (Contributors cannot see each other's
 * lessons).</p>
 *
 * <p> The tests call the Models used by the Contributor Home page (ModelContributorHome) and
 * the Lesson Details page (ModelLessonDetails), plus the "load" checks of the Edit Lesson and
 * Add Experience pages for US-R3-V, because that story says the same check applies before every
 * update, experience entry, and delete.  See TestSupport for how to read the output.</p>
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
 * @version 1.00		2026-10-16 TP2 Task 6: Read tests, one method per test
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadLessonTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public ReadLessonTests() {
	}

	/**********
	 * Runs the Read tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor Tests - Read Lessons (US-R1 to US-R4)",
				ReadLessonTests::runAll);
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
		ReadLessonTests tests = new ReadLessonTests();
		requirement("US-R1", "A Contributor can view a list of all the lessons they have added");
		run(tests::testViewMyLessonsPopulatedList);
		run(tests::testViewMyLessonsEmptyList);
		run(tests::testViewMyLessonsUnknownUser);

		requirement("US-R2", "A Contributor can search their titles and view the matching subset");
		run(tests::testSubsetListOneMatch);
		run(tests::testSubsetListMultipleMatchesIgnoringCase);
		run(tests::testSubsetListZeroMatches);

		requirement("US-R2-V", "Invalid search text gets a specific message; empty text finds "
				+ "nothing");
		run(tests::testEmptySearchTextFindsNothing);
		run(tests::testSearchTextAtMaximumAccepted);
		run(tests::testSearchTextTooLongRejected);
		run(tests::testSearchTextInvalidCharacterRejected);

		requirement("US-R3", "A Contributor can open one lesson and see its details, experience "
				+ "entries, and locks");
		run(tests::testViewSingleLessonFullDetails);
		run(tests::testLockedFieldDisplayedAsReadOnly);
		run(tests::testOpenLessonSelectedFromMyList);

		requirement("US-R3-V", "A lesson that does not exist or is not mine gives \"Lesson not "
				+ "found.\"");
		run(tests::testOwnerPassesTheOwnershipCheck);
		run(tests::testSelectNonexistentLessonId);
		run(tests::testCannotViewAnotherContributorsLesson);
		run(tests::testAnotherContributorCannotChangeOrDeleteMyLesson);

		requirement("US-R4", "A Contributor sees only their own lessons");
		run(tests::testMyListShowsOnlyMyLessons);
		run(tests::testSearchNeverReturnsAnotherContributorsLessons);
	}


	/*-*******************************************************************************************

	US-R1: view my list

	 */

	/**********
	 * <p> Test R1-P1 (Positive): View My Lessons - Populated List. </p>
	 *
	 * <p> <b>Requirement:</b> US-R1, a Contributor can view a list of all the lessons they have
	 * added. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ModelContributorHome.viewMyLessons calls
	 * LessonsLearnedList.loadByOwner, which selects every lesson whose owner is the requesting
	 * user. </p>
	 *
	 * <p> <b>Steps:</b> a new Contributor creates two lessons; the test asks for their list. </p>
	 *
	 * <p> <b>How to read the output:</b> the Actual line lists the titles returned (sorted).
	 * PASS means both lessons, and only those, are in the list. </p>
	 *
	 * <p> Validates: ModelContributorHome.viewMyLessons(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("R1-P1 View My Lessons - Populated List (US-R1, P)")
	void testViewMyLessonsPopulatedList() {
		String user = uniqueUser("listPopulated");
		makeLesson(user, "Sprint retro notes", "Teamwork");
		makeLesson(user, "Deploy pipeline fix", "Technical");

		check("R1-P1", true, "View My Lessons - Populated List",
				"2 lesson(s): Deploy pipeline fix, Sprint retro notes",
				titles(ModelContributorHome.viewMyLessons(user)));
	}


	/**********
	 * <p> Test R1-P2 (Positive): View My Lessons - Empty List. </p>
	 *
	 * <p> <b>Requirement:</b> US-R1, for a Contributor who has not added anything yet, the list
	 * is empty, which the Home page explains with guidance instead of an error. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> loadByOwner always returns a list object, empty
	 * when there are no rows, never null. </p>
	 *
	 * <p> <b>Steps:</b> ask for the list of a brand-new Contributor. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the result is an empty list, not null. </p>
	 *
	 * <p> Validates: ModelContributorHome.viewMyLessons(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("R1-P2 View My Lessons - Empty List (US-R1, P)")
	void testViewMyLessonsEmptyList() {
		LessonsLearnedList list = ModelContributorHome.viewMyLessons(uniqueUser("listEmpty"));
		String actual = (list == null) ? "null" : (list.isEmpty() ? "empty list (not null)"
				: titles(list));
		check("R1-P2", true, "View My Lessons - Empty List", "empty list (not null)", actual);
	}


	/**********
	 * <p> Test R1-N1 (Negative): View My Lessons - Blank or Unknown Username. </p>
	 *
	 * <p> <b>Requirement:</b> US-R1 together with US-R4: a request that does not name a real
	 * Contributor must not return anyone's lessons. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> loadByOwner matches the owner exactly, so a blank
	 * or unknown name matches no rows. </p>
	 *
	 * <p> <b>Steps:</b> ask for the list of "" and of a username that has never existed. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means both requests return an empty list. </p>
	 *
	 * <p> Validates: ModelContributorHome.viewMyLessons(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("R1-N1 View My Lessons - Blank or Unknown Username (US-R1, N)")
	void testViewMyLessonsUnknownUser() {
		String actual = "blank username: " + titles(ModelContributorHome.viewMyLessons(""))
				+ "; unknown username: "
				+ titles(ModelContributorHome.viewMyLessons(uniqueUser("neverRegistered")));
		check("R1-N1", false, "View My Lessons - Blank or Unknown Username",
				"blank username: 0 lesson(s); unknown username: 0 lesson(s)", actual);
	}


	/*-*******************************************************************************************

	US-R2: search my titles

	 */

	/**********
	 * Creates the three lessons the US-R2 tests search through.
	 *
	 * @param user	the Contributor who owns them
	 */
	private static void makeSearchLessons(String user) {
		makeLesson(user, "Sprint retro notes", "Teamwork");
		makeLesson(user, "Sprint planning mistakes", "Teamwork");
		makeLesson(user, "Deploy pipeline fix", "Technical");
	}

	/**********
	 * <p> Test R2-P1 (Positive): Subset List - One Match. </p>
	 *
	 * <p> <b>Requirement:</b> US-R2, searching titles returns the matching subset (here, a
	 * single match). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ModelContributorHome.searchMyLessons validates the
	 * text, loads only the Contributor's lessons, and keeps those whose title contains the text
	 * (LessonsLearnedList.filterByTitleContains). </p>
	 *
	 * <p> <b>Steps:</b> with three lessons stored, search for "Deploy". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means exactly one lesson, "Deploy pipeline fix",
	 * is returned. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("R2-P1 Subset List - One Match (US-R2, P)")
	void testSubsetListOneMatch() {
		String user = uniqueUser("searchOne");
		makeSearchLessons(user);
		LessonResult result = ModelContributorHome.searchMyLessons(user, "Deploy");
		check("R2-P1", true, "Subset List - One Match",
				"success \"Search complete\"; 1 lesson(s): Deploy pipeline fix",
				describe(result) + "; " + titles(result.getList()));
	}


	/**********
	 * <p> Test R2-P2 (Positive): Subset List - Multiple Matches, Ignoring Case. </p>
	 *
	 * <p> <b>Requirement:</b> US-R2, a search can return many matches, and a Contributor should
	 * not need to remember the capitalization of their titles. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> filterByTitleContains lower-cases both the title
	 * and the search text before comparing. </p>
	 *
	 * <p> <b>Steps:</b> with three lessons stored, search for "SPRINT" in capitals. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the two "Sprint ..." lessons are returned
	 * and "Deploy pipeline fix" is not. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("R2-P2 Subset List - Multiple Matches, Ignoring Case (US-R2, P)")
	void testSubsetListMultipleMatchesIgnoringCase() {
		String user = uniqueUser("searchMany");
		makeSearchLessons(user);
		LessonResult result = ModelContributorHome.searchMyLessons(user, "SPRINT");
		check("R2-P2", true, "Subset List - Multiple Matches, Ignoring Case",
				"success \"Search complete\"; 2 lesson(s): Sprint planning mistakes, Sprint retro notes",
				describe(result) + "; " + titles(result.getList()));
	}


	/**********
	 * <p> Test R2-N1 (Negative): Subset List - Zero Matches (Titles Only). </p>
	 *
	 * <p> <b>Requirement:</b> US-R2, a search that matches nothing returns an empty subset, not
	 * an error; and the search covers titles, as the story says. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> filterByTitleContains looks only at titles and
	 * always returns a list object. </p>
	 *
	 * <p> <b>Steps:</b> search for "definitely", a word that appears in every lesson's
	 * description but in no title. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the search succeeds with 0 lessons. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("R2-N1 Subset List - Zero Matches (Titles Only) (US-R2, N)")
	void testSubsetListZeroMatches() {
		String user = uniqueUser("searchZero");
		makeSearchLessons(user);
		LessonResult result = ModelContributorHome.searchMyLessons(user, "definitely");
		check("R2-N1", false, "Subset List - Zero Matches (Titles Only)",
				"success \"Search complete\"; 0 lesson(s)",
				describe(result) + "; " + titles(result.getList()));
	}


	/*-*******************************************************************************************

	US-R2-V: search input validation

	 */

	/**********
	 * <p> Test R2V-P1 (Positive): Subset List - Empty Search Text Finds Nothing. </p>
	 *
	 * <p> <b>Requirement:</b> US-R2-V, empty search text shows an empty result instead of an
	 * error (the team's Users-search rule; the Clear button restores the full list). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidSearchText accepts empty text, and
	 * filterByTitleContains returns an empty list for blank text. </p>
	 *
	 * <p> <b>Steps:</b> with one lesson stored, search for "". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means success with 0 lessons. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("R2V-P1 Subset List - Empty Search Text Finds Nothing (US-R2-V, P)")
	void testEmptySearchTextFindsNothing() {
		String user = uniqueUser("searchEmpty");
		makeLesson(user, "Sprint retro notes", "Teamwork");
		LessonResult result = ModelContributorHome.searchMyLessons(user, "");
		check("R2V-P1", true, "Subset List - Empty Search Text Finds Nothing",
				"success \"Search complete\"; 0 lesson(s)",
				describe(result) + "; " + titles(result.getList()));
	}


	/**********
	 * <p> Test R2V-P2 (Positive): Search Input - 254 Characters Accepted. </p>
	 *
	 * <p> <b>Requirement:</b> US-R2-V, only text longer than the maximum is rejected, so text
	 * exactly at the 254-character maximum must be accepted. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidSearchText rejects only lengths over
	 * 254. </p>
	 *
	 * <p> <b>Steps:</b> search for 254 letters "a". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the search runs (success) and finds 0
	 * lessons. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("R2V-P2 Search Input - 254 Characters Accepted (US-R2-V, P)")
	void testSearchTextAtMaximumAccepted() {
		String user = uniqueUser("searchMax");
		makeLesson(user, "Sprint retro notes", "Teamwork");
		LessonResult result = ModelContributorHome.searchMyLessons(user, rep("a", 254));
		check("R2V-P2", true, "Search Input - 254 Characters Accepted",
				"success \"Search complete\"; 0 lesson(s)",
				describe(result) + "; " + titles(result.getList()));
	}


	/**********
	 * <p> Test R2V-N1 (Negative): Search Input - Invalid Text Rejected (Too Long). </p>
	 *
	 * <p> <b>Requirement:</b> US-R2-V, search text longer than the maximum gets a specific
	 * error message. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> searchMyLessons returns the recognizer's message
	 * before any lessons are loaded, so no search is run. </p>
	 *
	 * <p> <b>Steps:</b> search for 255 letters "a". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the search is refused with "Search text is
	 * too long." for the search field, and no list is returned. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("R2V-N1 Search Input - Invalid Text Rejected (Too Long) (US-R2-V, N)")
	void testSearchTextTooLongRejected() {
		String user = uniqueUser("searchLong");
		LessonResult result = ModelContributorHome.searchMyLessons(user, rep("a", 255));
		check("R2V-N1", false, "Search Input - Invalid Text Rejected (Too Long)",
				"refused \"Search text is too long.\" (field: searchText); no list returned",
				describe(result) + "; " + (result.getList() == null ? "no list returned"
						: "a list was returned"));
	}


	/**********
	 * <p> Test R2V-N2 (Negative): Search Input - Invalid Character Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> US-R2-V, search text containing invalid characters gets a
	 * specific error message. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidSearchText rejects control characters
	 * such as a tab. </p>
	 *
	 * <p> <b>Steps:</b> search for "java[TAB]debug". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the invalid-character message is returned
	 * for the search field. </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("R2V-N2 Search Input - Invalid Character Rejected (US-R2-V, N)")
	void testSearchTextInvalidCharacterRejected() {
		String user = uniqueUser("searchTab");
		LessonResult result = ModelContributorHome.searchMyLessons(user, "java\tdebug");
		check("R2V-N2", false, "Search Input - Invalid Character Rejected",
				"refused \"Search text contains an invalid character.\" (field: searchText)",
				describe(result));
	}


	/*-*******************************************************************************************

	US-R3: view one lesson

	 */

	/**********
	 * <p> Test R3-P1 (Positive): View Single Lesson - Full Details and Experience Entries. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3, opening a lesson shows its full details, including its
	 * experience entries. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ModelLessonDetails.viewLesson loads the owned lesson
	 * and then calls loadExperienceEntries(), which reads the entry rows for that lesson. </p>
	 *
	 * <p> <b>Steps:</b> create a lesson with tags and a Research category, add one 3.5-hour
	 * experience entry, then open the lesson. </p>
	 *
	 * <p> <b>How to read the output:</b> the Actual line shows the title, description, tags,
	 * category, and experience entries of the opened lesson.  PASS means every part matches
	 * what was stored. </p>
	 *
	 * <p> Validates: ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("R3-P1 View Single Lesson - Full Details and Experience Entries (US-R3, P)")
	void testViewSingleLessonFullDetails() {
		String user = uniqueUser("viewFull");
		LessonResult created = guiCreateLesson.ModelCreateLesson.createLesson(user,
				"Reusable checklist", DESCRIPTION, "java, testing", "Research");
		TestSupport.remember(created.getLessonId());
		ModelAddExperience.addExperience(user, created.getLessonId(), "3.5",
				"Reused the checklist and it saved us time.");

		LessonResult result = ModelLessonDetails.viewLesson(user, created.getLessonId());
		String actual = describe(result);
		LessonsLearned lesson = result.getLesson();
		if (lesson != null)
			actual += "; " + lesson.getTitle() + " | " + lesson.getDescription() + " | "
					+ lesson.getTags() + " | " + lesson.getCategory() + " | "
					+ lesson.getExperienceEntries().size() + " experience entry ("
					+ lesson.getExperienceEntries().get(0).getTimeEffortHours() + " h)";
		check("R3-P1", true, "View Single Lesson - Full Details and Experience Entries",
				"success; Reusable checklist | " + DESCRIPTION + " | java, testing | Research | "
				+ "1 experience entry (3.5 h)", actual);
	}


	/**********
	 * <p> Test R3-P2 (Positive): View Single Lesson - Locked Field Displayed as Read-Only. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3, the Contributor can see which fields are locked and
	 * unavailable to edit. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> the lesson returned by viewLesson carries its lock
	 * flags, and isFieldLocked() reports each field; the Details page uses this to mark locked
	 * fields and the Edit page disables those inputs. </p>
	 *
	 * <p> <b>Steps:</b> lock only the title (as a Curator would), then open the lesson. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means only the title reports locked and the other
	 * three fields remain editable. </p>
	 *
	 * <p> Validates: ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(12)
	@DisplayName("R3-P2 View Single Lesson - Locked Field Displayed as Read-Only (US-R3, P)")
	void testLockedFieldDisplayedAsReadOnly() {
		String user = uniqueUser("viewLocked");
		int id = makeLesson(user, "Lesson with a locked title", "General");
		lock(id, "title", true);

		LessonsLearned lesson = ModelLessonDetails.viewLesson(user, id).getLesson();
		String actual = (lesson == null) ? "lesson not returned"
				: "title locked: " + lesson.isFieldLocked("title") + "; description locked: "
						+ lesson.isFieldLocked("description") + "; tags locked: "
						+ lesson.isFieldLocked("tags") + "; category locked: "
						+ lesson.isFieldLocked("category");
		check("R3-P2", true, "View Single Lesson - Locked Field Displayed as Read-Only",
				"title locked: true; description locked: false; tags locked: false; "
				+ "category locked: false", actual);
	}


	/**********
	 * <p> Test R3-P3 (Positive): Open Lesson - The Lesson Selected From My List Opens. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3, the Contributor selects a specific lesson from their list
	 * to view it. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> each item in the Home page list is a LessonsLearned
	 * object carrying its id; the "Open Lesson" button (or a double-click) passes that id to
	 * ModelLessonDetails.viewLesson. </p>
	 *
	 * <p> <b>Steps:</b> create two lessons, load the list as the Home page does, pick "Second
	 * lesson" from it, and open that lesson by the id taken from the list. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the opened lesson has the selected id and
	 * the title "Second lesson", not the other lesson. </p>
	 *
	 * <p> Validates: ModelContributorHome.viewMyLessons(), ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(13)
	@DisplayName("R3-P3 Open Lesson - The Lesson Selected From My List Opens (US-R3, P)")
	void testOpenLessonSelectedFromMyList() {
		String user = uniqueUser("openFromList");
		makeLesson(user, "First lesson", "General");
		makeLesson(user, "Second lesson", "General");

		LessonsLearned selected = null;
		for (LessonsLearned lesson : ModelContributorHome.viewMyLessons(user).getLessons())
			if (lesson.getTitle().equals("Second lesson")) selected = lesson;

		String actual = "\"Second lesson\" not in the list";
		if (selected != null) {
			LessonResult opened = ModelLessonDetails.viewLesson(user, selected.getId());
			actual = describe(opened) + "; opened "
					+ (opened.getLesson() != null && opened.getLesson().getId() == selected.getId()
						? "the selected id" : "a different id")
					+ "; title: " + (opened.getLesson() == null ? "none" : opened.getLesson().getTitle());
		}
		check("R3-P3", true, "Open Lesson - The Lesson Selected From My List Opens",
				"success; opened the selected id; title: Second lesson", actual);
	}


	/*-*******************************************************************************************

	US-R3-V: a lesson that does not exist or is not mine

	 */

	/**********
	 * <p> Test R3V-P1 (Positive): Ownership Check - The Owner Passes Before Every Action. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3-V, the check that runs before viewing, updating, adding
	 * experience, and deleting must let the owner through. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> every Contributor Model starts with
	 * LessonAccess.findOwnedLesson, which returns the lesson when the requesting user owns
	 * it. </p>
	 *
	 * <p> <b>Steps:</b> as the owner, call the view, edit-load, and add-experience-load
	 * methods (the delete path is covered by the US-D1 tests). </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means all three succeed. </p>
	 *
	 * <p> Validates: ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(14)
	@DisplayName("R3V-P1 Ownership Check - The Owner Passes Before Every Action (US-R3-V, P)")
	void testOwnerPassesTheOwnershipCheck() {
		String user = uniqueUser("ownerCheck");
		int id = makeLesson(user, "My own lesson", "General");
		String actual = "view: " + describe(ModelLessonDetails.viewLesson(user, id))
				+ "; edit: " + describe(ModelUpdateLesson.loadLessonForEdit(user, id))
				+ "; add experience: " + describe(ModelAddExperience.loadLesson(user, id));
		check("R3V-P1", true, "Ownership Check - The Owner Passes Before Every Action",
				"view: success; edit: success; add experience: success", actual);
	}


	/**********
	 * <p> Test R3V-N1 (Negative): Select Nonexistent Lesson ID. </p>
	 *
	 * <p> <b>Requirements:</b> US-R3-V, opening, updating, adding experience to, or deleting a
	 * lesson that does not exist gives "Lesson not found." and no error page; and US-R3, which
	 * opens only a lesson that exists. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> findOwnedLesson returns null for a missing id, and
	 * each Model then returns LessonAccess.NOT_FOUND without touching the database. </p>
	 *
	 * <p> <b>Steps:</b> call view, update, add experience, and delete (confirmed) with the id
	 * -999. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means all four are refused with "Lesson not
	 * found." and nothing throws an exception. </p>
	 *
	 * <p> Validates: ModelLessonDetails.viewLesson(), ModelLessonDetails.deleteLesson(). </p>
	 */
	@Test
	@Order(15)
	@DisplayName("R3V-N1 Select Nonexistent Lesson ID (US-R3-V, N)")
	void testSelectNonexistentLessonId() {
		String user = uniqueUser("missingId");
		String actual = "view: " + describe(ModelLessonDetails.viewLesson(user, -999))
				+ "; update: " + describe(ModelUpdateLesson.updateLesson(user, -999, "Title",
						DESCRIPTION, "", "General"))
				+ "; add experience: " + describe(ModelAddExperience.addExperience(user, -999,
						"1", "Tried it again."))
				+ "; delete: " + describe(ModelLessonDetails.deleteLesson(user, -999, true));
		String notFound = "refused \"Lesson not found.\"";
		check("R3V-N1", false, "Select Nonexistent Lesson ID",
				"view: " + notFound + "; update: " + notFound + "; add experience: " + notFound
				+ "; delete: " + notFound, actual);
	}


	/**********
	 * <p> Test R3V-N2 (Negative): Cannot View Another Contributor's Lesson. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3-V (and US-R4), opening a lesson that is not mine gives
	 * "Lesson not found." rather than another Contributor's data. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> findOwnedLesson compares the lesson's owner with
	 * the requesting user and treats a mismatch exactly like a missing lesson, so the reply does
	 * not even reveal that the id exists. </p>
	 *
	 * <p> <b>Steps:</b> Contributor A creates a lesson; Contributor B opens it by id. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means B is refused and no lesson content is
	 * returned. </p>
	 *
	 * <p> Validates: ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(16)
	@DisplayName("R3V-N2 Cannot View Another Contributor's Lesson (US-R3-V, N)")
	void testCannotViewAnotherContributorsLesson() {
		String userA = uniqueUser("privacyOwnerA");
		String userB = uniqueUser("privacyOtherB");
		int id = makeLesson(userA, "Private lesson of A", "General");

		LessonResult result = ModelLessonDetails.viewLesson(userB, id);
		check("R3V-N2", false, "Cannot View Another Contributor's Lesson",
				"refused \"Lesson not found.\"; content returned: none",
				describe(result) + "; content returned: "
				+ (result.getLesson() == null ? "none" : result.getLesson().getTitle()));
	}


	/**********
	 * <p> Test R3V-N3 (Negative): Another Contributor Cannot Change or Delete My Lesson. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3-V, "this same check applies before any update, experience
	 * entry, or delete action". </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ModelUpdateLesson, ModelAddExperience, and
	 * ModelLessonDetails.deleteLesson all call findOwnedLesson before doing anything. </p>
	 *
	 * <p> <b>Steps:</b> Contributor B tries to update, add experience to, and delete A's
	 * lesson; the test then reads A's lesson back. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means all three are refused with "Lesson not
	 * found." and A's lesson still exists with its original title and no experience
	 * entries. </p>
	 *
	 * <p> Validates: ModelLessonDetails.deleteLesson(), ModelLessonDetails.viewLesson(). </p>
	 */
	@Test
	@Order(17)
	@DisplayName("R3V-N3 Another Contributor Cannot Change or Delete My Lesson (US-R3-V, N)")
	void testAnotherContributorCannotChangeOrDeleteMyLesson() {
		String userA = uniqueUser("privacyOwnerA");
		String userB = uniqueUser("privacyOtherB");
		int id = makeLesson(userA, "Original title of A", "General");

		String actual = "update: " + describe(ModelUpdateLesson.updateLesson(userB, id,
				"Changed by B", DESCRIPTION, "", "General"))
				+ "; add experience: " + describe(ModelAddExperience.addExperience(userB, id, "1",
						"B tried to add this."))
				+ "; delete: " + describe(ModelLessonDetails.deleteLesson(userB, id, true));
		LessonsLearned lessonOfA = ModelLessonDetails.viewLesson(userA, id).getLesson();
		actual += "; A's lesson: " + (lessonOfA == null ? "gone" : lessonOfA.getTitle() + ", "
				+ lessonOfA.getExperienceEntries().size() + " experience entries");

		String notFound = "refused \"Lesson not found.\"";
		check("R3V-N3", false, "Another Contributor Cannot Change or Delete My Lesson",
				"update: " + notFound + "; add experience: " + notFound + "; delete: " + notFound
				+ "; A's lesson: Original title of A, 0 experience entries", actual);
	}


	/*-*******************************************************************************************

	US-R4: privacy between Contributors

	 */

	/**********
	 * <p> Test R4-P1 (Positive): My List Shows Only My Lessons. </p>
	 *
	 * <p> <b>Requirement:</b> US-R4, a Contributor can only see the lessons they have
	 * added. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> viewMyLessons always loads by the requesting
	 * user's name (loadByOwner), never the whole table. </p>
	 *
	 * <p> <b>Steps:</b> Contributor A creates two lessons and Contributor B creates one; each
	 * asks for their list. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means A sees only A's two titles and B sees only
	 * B's one title. </p>
	 *
	 * <p> Validates: ModelContributorHome.viewMyLessons(). </p>
	 */
	@Test
	@Order(18)
	@DisplayName("R4-P1 My List Shows Only My Lessons (US-R4, P)")
	void testMyListShowsOnlyMyLessons() {
		String userA = uniqueUser("ownListA");
		String userB = uniqueUser("ownListB");
		makeLesson(userA, "Lesson A1", "General");
		makeLesson(userA, "Lesson A2", "General");
		makeLesson(userB, "Lesson B1", "General");

		check("R4-P1", true, "My List Shows Only My Lessons",
				"A sees 2 lesson(s): Lesson A1, Lesson A2; B sees 1 lesson(s): Lesson B1",
				"A sees " + titles(ModelContributorHome.viewMyLessons(userA)) + "; B sees "
				+ titles(ModelContributorHome.viewMyLessons(userB)));
	}


	/**********
	 * <p> Test R4-N1 (Negative): Search Never Returns Another Contributor's Lessons. </p>
	 *
	 * <p> <b>Requirement:</b> US-R4, searching must not expose another Contributor's lesson,
	 * even when its title matches. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> searchMyLessons filters only the list returned by
	 * loadByOwner for the requesting user. </p>
	 *
	 * <p> <b>Steps:</b> A creates "Deploy checklist", B creates "Deploy rollback"; A searches
	 * for "Deploy". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means A gets only "Deploy checklist". </p>
	 *
	 * <p> Validates: ModelContributorHome.searchMyLessons(). </p>
	 */
	@Test
	@Order(19)
	@DisplayName("R4-N1 Search Never Returns Another Contributor's Lessons (US-R4, N)")
	void testSearchNeverReturnsAnotherContributorsLessons() {
		String userA = uniqueUser("searchPrivA");
		String userB = uniqueUser("searchPrivB");
		makeLesson(userA, "Deploy checklist", "Technical");
		makeLesson(userB, "Deploy rollback", "Technical");

		LessonResult result = ModelContributorHome.searchMyLessons(userA, "Deploy");
		check("R4-N1", false, "Search Never Returns Another Contributor's Lessons",
				"success \"Search complete\"; 1 lesson(s): Deploy checklist",
				describe(result) + "; " + titles(result.getList()));
	}
}
