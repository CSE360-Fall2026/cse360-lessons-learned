package curatorSummaryTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import database.Database;
import entityClasses.ExperienceEntry;
import entityClasses.LessonSummary;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiContributorHome.ContributorLessonView;
import guiCuratorSummary.CuratorSummaryController;
import guiCuratorSummary.ModelCuratorSummary;
import guiLessonDetails.ModelLessonDetails;
import guiViewerHome.ModelViewerHome;

/*******
 * <p> Title: CuratorSummaryPageTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the pages of TP3 Aspects 3 and 4: the Model of the Curator
 * Merge Statistics page (guiCuratorSummary.ModelCuratorSummary), the merged effort summary panel
 * on the Contributor's Lesson Details page (guiLessonDetails.ModelLessonDetails), and the
 * Viewer's lesson details (guiViewerHome.ModelViewerHome).  The rules underneath, in Alex
 * Meyers' classes, are tested by CuratorSummaryTests; these tests check what the pages show and
 * do with them.  What can only be judged by looking at the screen is in the manual tests M-CS1
 * to M-CS8.</p>
 *
 * <p> <b>Requirements covered</b> (Staff User Stories.pdf, sections 9, 10, and 13):
 * US-C-E4-01, US-C-E4-02, US-C-E4-03, US-C-E5-01, US-C-E5-02, and US-V-04.  Each test's display
 * name gives its id, what it checks, its story, and P (positive) or N (negative).</p>
 *
 * <p> <b>Running the tests.</b>  Run As &gt; JUnit Test.  Every test gets its own new in-memory
 * H2 database, so the tests do not touch FoundationDatabase and can run while the application
 * is open.  Lesson A has experience records of 2 and 4 hours, lesson B one of 6 hours, and
 * lesson C none, as in CuratorSummaryTests.</p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-16 TP3 Aspects 3 and 4: tests for the Merge Statistics page and the
 * 							summary panels
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CuratorSummaryPageTests {

	/** The in-memory database of the current test. */
	private Database database;
	/** The page Model under test. */
	private ModelCuratorSummary model;
	/** A Curator. */
	private User curator;
	/** A Contributor who is not a Curator; owns every test lesson. */
	private User contributor;
	/** Lesson A: experience records of 2 and 4 hours. */
	private int lessonA;
	/** Lesson B: one experience record of 6 hours. */
	private int lessonB;
	/** Lesson C: no experience records. */
	private int lessonC;

	/**********
	 * Default constructor; JUnit creates one instance for each test.
	 */
	public CuratorSummaryPageTests() {
	}

	/**********
	 * Creates a new in-memory database with a Curator, a Contributor, and lessons A, B, and C.
	 *
	 * @throws Exception if the database cannot be set up
	 */
	@BeforeEach
	public void setUp() throws Exception {
		database = new Database("jdbc:h2:mem:tp3_summarypage_"
				+ UUID.randomUUID().toString().replace("-", ""));
		database.connectToDatabase();
		curator = new User("pageCurator", "Password123!", "Test", "", "Curator", "", "",
				false, false, true, false);
		contributor = new User("pageContributor", "Password123!", "Test", "", "Contributor",
				"", "", false, true, false, false);
		database.register(curator);
		database.register(contributor);
		lessonA = newLesson("Deployment pipeline lesson");
		lessonB = newLesson("Database migration lesson");
		lessonC = newLesson("Release checklist lesson");
		addExperience(lessonA, 2.0);
		addExperience(lessonA, 4.0);
		addExperience(lessonB, 6.0);
		model = new ModelCuratorSummary(database);
	}

	/**********
	 * Closes the test database, which also discards it.
	 */
	@AfterEach
	public void tearDown() {
		if (database != null) database.closeConnection();
	}


	/*-********************************************************************************************

	Opening the page

	 */

	/**********
	 * <p> SP-01: A Curator opens the target lesson and gets its title and no saved summary; a
	 * Contributor and no user are refused with the Curator message; a missing lesson gives
	 * "Lesson not found." (US-C-E5-02, US-C-E4-01). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.openLesson(), ModelCuratorSummary.getSummary(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("SP-01 Only a Curator can open the page, for a lesson that exists (US-C-E5-02, P/N)")
	public void sp01OpenLesson() {
		ModelCuratorSummary.Result opened = model.openLesson(curator, lessonC);
		assertTrue(opened.isSuccess());
		assertEquals("Release checklist lesson", opened.getLesson().getTitle());
		assertNull(opened.getSummary());

		for (User user : new User[] { contributor, null }) {
			ModelCuratorSummary.Result refused = model.openLesson(user, lessonC);
			assertFalse(refused.isSuccess());
			assertEquals(CuratorSummaryController.NOT_CURATOR, refused.getMessage());
			assertNull(refused.getLesson());
		}
		assertEquals("Lesson not found.", model.openLesson(curator, 99999).getMessage());
	}

	/**********
	 * <p> SP-02: The list of lessons to choose from has every lesson for a Curator, and nothing
	 * for anyone else (US-C-E4-01). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.sourceChoices(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("SP-02 A Curator can choose from every lesson; others get none (US-C-E4-01, P/N)")
	public void sp02SourceChoices() {
		List<Integer> ids = new ArrayList<Integer>();
		for (LessonsLearned lesson : model.sourceChoices(curator)) ids.add(lesson.getId());
		assertTrue(ids.containsAll(List.of(lessonA, lessonB, lessonC)));
		assertEquals(3, ids.size());
		assertTrue(model.sourceChoices(contributor).isEmpty());
		assertTrue(model.sourceChoices(null).isEmpty());
	}

	/**********
	 * <p> SP-03: Each row names the lesson, its owner, and how many experience records it has,
	 * in the singular for one, so the Curator can judge what is being merged (US-C-E4-02). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.choiceRow(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("SP-03 Each lesson row shows its owner and number of experience records (US-C-E4-02, P)")
	public void sp03ChoiceRow() {
		assertEquals("Deployment pipeline lesson   by pageContributor   (2 experience records)",
				model.choiceRow(database.getLessonById(lessonA)));
		assertEquals("Database migration lesson   by pageContributor   (1 experience record)",
				model.choiceRow(database.getLessonById(lessonB)));
		assertEquals("Release checklist lesson   by pageContributor   (0 experience records)",
				model.choiceRow(database.getLessonById(lessonC)));
	}


	/*-********************************************************************************************

	Preview, confirm, and cancel

	 */

	/**********
	 * <p> SP-04: Preview returns the summary and makes Confirm possible; a refusal returns the
	 * story's message and no summary, and leaves nothing to confirm (US-C-E4-01). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.preview(), ModelCuratorSummary.getSummary(),
	 * ModelCuratorSummary.hasPreview(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("SP-04 Preview gives a summary, or the refusal message and nothing to confirm (US-C-E4-01, P/N)")
	public void sp04Preview() {
		ModelCuratorSummary.Result refused = model.preview(curator, lessonC, List.of(lessonA));
		assertFalse(refused.isSuccess());
		assertEquals("Select at least two lessons", refused.getMessage());
		assertNull(refused.getSummary());
		assertFalse(model.hasPreview());

		ModelCuratorSummary.Result preview = model.preview(curator, lessonC,
				List.of(lessonA, lessonB));
		assertTrue(preview.isSuccess());
		assertEquals(3, preview.getSummary().getEntryCount());
		assertTrue(model.hasPreview());
		assertNull(database.getLessonSummary(lessonC), "A preview saves nothing");
	}

	/**********
	 * <p> SP-05: The table labels every statistic, names the source lessons, shows hours to two
	 * decimal places, and shows N/A for the standard deviation of one observation; a source
	 * lesson deleted since the merge is listed as deleted (US-C-E4-02, US-C-E4-03). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.preview(), ModelCuratorSummary.tableText(),
	 * ModelCuratorSummary.confirm(), ModelCuratorSummary.getSummary(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("SP-05 The table is labeled, names the sources, uses two decimals (US-C-E4-03, P)")
	public void sp05TableText() {
		LessonSummary preview = model.preview(curator, lessonC, List.of(lessonA, lessonB))
				.getSummary();
		String table = model.tableText("PREVIEW (not saved)", preview);
		assertTrue(table.startsWith("PREVIEW (not saved)\n\nSource lessons (2):\n"
				+ "  - Deployment pipeline lesson\n  - Database migration lesson\n"), table);
		assertTrue(table.contains("Experience records      3\n"), table);
		assertTrue(table.contains("Total hours             12.00\n"), table);
		assertTrue(table.contains("Mean hours              4.00\n"), table);
		assertTrue(table.contains("Median hours            4.00\n"), table);
		assertTrue(table.contains("Minimum hours           2.00\n"), table);
		assertTrue(table.contains("Maximum hours           6.00\n"), table);
		assertTrue(table.contains("Sample std. deviation   2.00\n"), table);
		assertTrue(table.contains("not a rating"), table);

		LessonSummary one = model.preview(curator, lessonC, List.of(lessonB, lessonC))
				.getSummary();
		assertTrue(model.tableText("PREVIEW", one).contains(
				"Sample std. deviation   N/A: fewer than two observations"));

		model.confirm(curator, lessonC, List.of(lessonB, lessonC));
		database.deleteLesson(lessonB);
		assertTrue(model.tableText("SAVED", database.getLessonSummary(lessonC))
				.contains("  - lesson " + lessonB + " (deleted)\n"));
	}

	/**********
	 * <p> SP-06: Confirm after a preview saves the summary with the SAVED message and leaves
	 * nothing to confirm; Confirm without a preview is refused (US-C-E4-01). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.confirm(), ModelCuratorSummary.preview(),
	 * ModelCuratorSummary.hasPreview(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("SP-06 Confirm saves after a preview and is refused without one (US-C-E4-01, P/N)")
	public void sp06Confirm() {
		ModelCuratorSummary.Result early = model.confirm(curator, lessonC,
				List.of(lessonA, lessonB));
		assertFalse(early.isSuccess());
		assertEquals(CuratorSummaryController.NO_PREVIEW, early.getMessage());

		model.preview(curator, lessonC, List.of(lessonA, lessonB));
		ModelCuratorSummary.Result saved = model.confirm(curator, lessonC,
				List.of(lessonA, lessonB));
		assertTrue(saved.isSuccess());
		assertEquals(ModelCuratorSummary.SAVED, saved.getMessage());
		assertFalse(model.hasPreview());
		assertNotNull(database.getLessonSummary(lessonC));
	}

	/**********
	 * <p> SP-07: If the data changed since the preview, Confirm saves nothing, says so, and
	 * gives the page the refreshed preview to show; the next Confirm saves it (US-C-E4-02). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.preview(), ModelCuratorSummary.confirm(),
	 * ModelCuratorSummary.getSummary(), ModelCuratorSummary.hasPreview(). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("SP-07 Changed data: the page gets the refreshed preview to show (US-C-E4-02, N)")
	public void sp07RefreshedPreview() {
		model.preview(curator, lessonC, List.of(lessonA, lessonB));
		addExperience(lessonA, 8.0);
		ModelCuratorSummary.Result changed = model.confirm(curator, lessonC,
				List.of(lessonA, lessonB));
		assertFalse(changed.isSuccess());
		assertEquals(CuratorSummaryController.DATA_CHANGED, changed.getMessage());
		assertEquals(4, changed.getSummary().getEntryCount());
		assertTrue(model.hasPreview());
		assertNull(database.getLessonSummary(lessonC));
		assertTrue(model.confirm(curator, lessonC, List.of(lessonA, lessonB)).isSuccess());
		assertEquals(4, database.getLessonSummary(lessonC).getEntryCount());
	}

	/**********
	 * <p> SP-08: Cancel discards the preview and saves nothing (US-C-E4-01 V sentence). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.preview(), ModelCuratorSummary.cancel(),
	 * ModelCuratorSummary.hasPreview(), ModelCuratorSummary.confirm(). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("SP-08 Cancel discards the preview and saves nothing (US-C-E4-01, N)")
	public void sp08Cancel() {
		model.preview(curator, lessonC, List.of(lessonA, lessonB));
		model.cancel();
		assertFalse(model.hasPreview());
		assertFalse(model.confirm(curator, lessonC, List.of(lessonA, lessonB)).isSuccess());
		assertNull(database.getLessonSummary(lessonC));
	}

	/**********
	 * <p> SP-09: A new Model, as made each time the page is displayed, has no preview, so a
	 * preview from an earlier visit can never be confirmed; openLesson also discards one. </p>
	 *
	 * <p> Validates: ModelCuratorSummary.preview(), ModelCuratorSummary.openLesson(),
	 * ModelCuratorSummary.hasPreview(). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("SP-09 A preview never carries over to another visit of the page (US-C-E4-01, N)")
	public void sp09NoCarryOver() {
		model.preview(curator, lessonC, List.of(lessonA, lessonB));
		model.openLesson(curator, lessonC);
		assertFalse(model.hasPreview());
		assertFalse(new ModelCuratorSummary(database).hasPreview());
	}


	/*-********************************************************************************************

	Visibility and the words on the Curator page

	 */

	/**********
	 * <p> SP-10: Show and Hide report what they did and return the summary as it is now; on a
	 * lesson with no summary the refusal is returned (US-C-E5-01). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.setVisibility(), ModelCuratorSummary.getSummary(). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("SP-10 Show and Hide report the new state; no summary is refused (US-C-E5-01, P/N)")
	public void sp10Visibility() {
		ModelCuratorSummary.Result none = model.setVisibility(curator, lessonC, true);
		assertFalse(none.isSuccess());
		assertEquals(CuratorSummaryController.NO_SUMMARY, none.getMessage());

		saveSummaryOnC();
		ModelCuratorSummary.Result shown = model.setVisibility(curator, lessonC, true);
		assertEquals(ModelCuratorSummary.NOW_VISIBLE, shown.getMessage());
		assertTrue(shown.getSummary().isVisibleToContributor());
		ModelCuratorSummary.Result hidden = model.setVisibility(curator, lessonC, false);
		assertEquals(ModelCuratorSummary.NOW_HIDDEN, hidden.getMessage());
		assertFalse(hidden.getSummary().isVisibleToContributor());
		assertFalse(model.setVisibility(contributor, lessonC, true).isSuccess());
	}

	/**********
	 * <p> SP-11: The saved line says whether there is a summary and who can see it, and the
	 * confirmation question says when a summary is replaced and that it starts hidden
	 * (US-C-E5-01, US-C-E4-02, US-C-E5-02). </p>
	 *
	 * <p> Validates: ModelCuratorSummary.savedLine(), ModelCuratorSummary.setVisibility(),
	 * ModelCuratorSummary.confirmationQuestion(). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("SP-11 Saved line and confirmation question say what will happen (US-C-E5-01, P)")
	public void sp11Words() {
		assertEquals("Saved summary: none yet.", ModelCuratorSummary.savedLine(null));
		LessonSummary saved = saveSummaryOnC();
		assertEquals("Saved summary: 3 experience records, HIDDEN from contributors.",
				ModelCuratorSummary.savedLine(saved));
		model.setVisibility(curator, lessonC, true);
		assertEquals("Saved summary: 3 experience records, VISIBLE to contributors.",
				ModelCuratorSummary.savedLine(database.getLessonSummary(lessonC)));

		assertEquals("Save this summary on 'Release checklist lesson'?  It will be hidden from "
				+ "contributors until you show it.",
				ModelCuratorSummary.confirmationQuestion("Release checklist lesson", false));
		assertEquals("Save this summary on 'Release checklist lesson'?  It replaces the lesson's "
				+ "current summary.  It will be hidden from contributors until you show it.",
				ModelCuratorSummary.confirmationQuestion("Release checklist lesson", true));
	}


	/*-********************************************************************************************

	The Contributor's panel and the Viewer's details

	 */

	/**********
	 * <p> SP-12: The exact panel text for the US-C-E4-03 example: one labeled statistic per
	 * line, in hours, to two decimal places. </p>
	 *
	 * <p> Validates: ModelLessonDetails.summaryPanelText(),
	 * ModelCuratorSummary.setVisibility(). </p>
	 */
	@Test
	@Order(12)
	@DisplayName("SP-12 The summary panel shows one labeled statistic per line (US-C-E4-03, P)")
	public void sp12PanelText() {
		saveSummaryOnC();
		model.setVisibility(curator, lessonC, true);
		assertEquals("Experience records: 3\nTotal: 12.00 hrs\nMean: 4.00 hrs\nMedian: 4.00 hrs\n"
				+ "Minimum: 2.00 hrs\nMaximum: 6.00 hrs\nStd dev: 2.00 hrs",
				ModelLessonDetails.summaryPanelText(lessonC, database));
	}

	/**********
	 * <p> SP-13: On the Contributor's Lesson Details page, a hidden summary and a missing one
	 * show the same sentence, so the Contributor cannot tell a hidden one exists, and neither
	 * shows numbers; a visible one shows its statistics, and hiding it again removes them on the
	 * next read (US-C-E5-02). </p>
	 *
	 * <p> Validates: ModelLessonDetails.summaryPanelText(),
	 * ModelCuratorSummary.setVisibility(). </p>
	 */
	@Test
	@Order(13)
	@DisplayName("SP-13 Contributor panel: hidden and missing look the same, no zeros (US-C-E5-02, N)")
	public void sp13ContributorPanel() {
		String missing = ModelLessonDetails.summaryPanelText(lessonC, database);
		assertEquals(ContributorLessonView.EMPTY_STATE, missing);
		saveSummaryOnC();
		assertEquals(missing, ModelLessonDetails.summaryPanelText(lessonC, database),
				"Hidden must look exactly like missing");
		assertFalse(missing.matches(".*\\d.*"), "No numbers at all");

		model.setVisibility(curator, lessonC, true);
		assertTrue(ModelLessonDetails.summaryPanelText(lessonC, database)
				.startsWith("Experience records: 3"));
		model.setVisibility(curator, lessonC, false);
		assertEquals(ContributorLessonView.EMPTY_STATE,
				ModelLessonDetails.summaryPanelText(lessonC, database));
	}

	/**********
	 * <p> SP-14: The Viewer's lesson details include a summary only when the Curator made it
	 * visible, and never a hidden one (US-V-04). </p>
	 *
	 * <p> Validates: ModelViewerHome.visibleSummaryText(),
	 * ModelCuratorSummary.setVisibility(). </p>
	 */
	@Test
	@Order(14)
	@DisplayName("SP-14 Viewer details show a summary only when it is visible (US-V-04, P/N)")
	public void sp14Viewer() {
		assertEquals("", ModelViewerHome.visibleSummaryText(lessonC, database));
		saveSummaryOnC();
		assertEquals("", ModelViewerHome.visibleSummaryText(lessonC, database));
		model.setVisibility(curator, lessonC, true);
		assertTrue(ModelViewerHome.visibleSummaryText(lessonC, database)
				.contains("Total: 12.00 hrs"));
		model.setVisibility(curator, lessonC, false);
		assertEquals("", ModelViewerHome.visibleSummaryText(lessonC, database));
	}

	/**********
	 * <p> SP-15: The numbers read "12.00", not "12,00", even on a computer whose language uses a
	 * decimal comma (here German); the default language is restored afterwards. </p>
	 *
	 * <p> Validates: ModelLessonDetails.summaryPanelText(),
	 * ModelCuratorSummary.setVisibility(), ModelCuratorSummary.tableText(). </p>
	 */
	@Test
	@Order(15)
	@DisplayName("SP-15 Numbers use a decimal point on every computer (US-C-E4-03, P)")
	public void sp15DecimalPoint() {
		saveSummaryOnC();
		model.setVisibility(curator, lessonC, true);
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertTrue(ModelLessonDetails.summaryPanelText(lessonC, database)
					.contains("Total: 12.00 hrs"));
			assertTrue(model.tableText("SAVED", database.getLessonSummary(lessonC))
					.contains("12.00"));
		} finally {
			Locale.setDefault(original);
		}
	}


	/*-********************************************************************************************

	Helpers

	 */

	/**********
	 * Creates a lesson owned by the test Contributor.
	 *
	 * @param title the lesson title
	 * @return the new lesson's id
	 */
	private int newLesson(String title) {
		try {
			int id = database.createLesson(new LessonsLearned(contributor.getUserName(), title,
					"A description that is long enough.", "", "Other"));
			assertTrue(id > 0, "The test lesson must be created");
			return id;
		} catch (Exception e) {
			throw new AssertionError("The test lesson could not be created", e);
		}
	}

	/**********
	 * Adds one experience record with the given hours to a lesson.
	 *
	 * @param lessonId	the lesson
	 * @param hours		the recorded time and effort hours
	 */
	private void addExperience(int lessonId, double hours) {
		assertTrue(database.createExperience(new ExperienceEntry(lessonId, hours,
				"Experience record of " + hours + " hours")) > 0);
	}

	/**********
	 * Previews and confirms a merge of lessons A and B on lesson C through the page Model.
	 *
	 * @return the saved summary
	 */
	private LessonSummary saveSummaryOnC() {
		model.preview(curator, lessonC, List.of(lessonA, lessonB));
		return model.confirm(curator, lessonC, List.of(lessonA, lessonB)).getSummary();
	}
}
