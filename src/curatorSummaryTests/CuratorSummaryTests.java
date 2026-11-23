package curatorSummaryTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import inputValidation.TimeEffortStatisticsCalculator;

/*******
 * <p> Title: CuratorSummaryTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for TP3 Aspects 3 and 4 (Epics E4 and E5): merging the time and
 * effort data of several lessons into a summary, and the Curator's control of whether
 * Contributors can see it.  The code under test is Alex Meyers' TimeEffortStatisticsCalculator,
 * LessonSummary, CuratorSummaryController, and ContributorLessonView, with the Database methods
 * saveLessonSummary, getLessonSummary, and setStatsVisibleToContributor.</p>
 *
 * <p> <b>Requirements covered</b> (Staff User Stories.pdf, sections 9 and 10):
 * US-C-E4-01 (preview, explicit confirmation, cancel), US-C-E4-02 (which observations are
 * merged, and saving), US-C-E4-03 (the statistics and how they are shown), US-C-E5-01 (the two
 * acceptance checks Alex added: the toggle persists, and it changes nothing else), and
 * US-C-E5-02 (hidden by default, only a Curator, filtered at the read boundary).  Each test's
 * display name gives its id, what it checks, its story, and P (positive: valid use that must
 * work) or N (negative: something that must be refused).</p>
 *
 * <p> <b>Running the tests.</b>  Run As &gt; JUnit Test.  Every test gets its own new in-memory
 * H2 database, so the tests do not touch FoundationDatabase, do not depend on each other, and
 * can run while the application is open.</p>
 *
 * <p> <b>Test data.</b>  Lesson A has experience records of 2 and 4 hours and lesson B has one
 * of 6 hours, so merging A and B gives the observations 2, 4, and 6 used in the US-C-E4-03
 * example.  Lesson C has no experience records.</p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-16 TP3 Aspects 3 and 4: tests for Alex Meyers' code after it
 * 							was moved into src
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CuratorSummaryTests {

	/** The database URL of the current test, so a second connection can reopen it. */
	private String databaseUrl;
	/** The in-memory database of the current test. */
	private Database database;
	/** The controller under test. */
	private CuratorSummaryController controller;
	/** A Curator (Role2 flag set). */
	private User curator;
	/** A Contributor who is not a Curator. */
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
	public CuratorSummaryTests() {
	}

	/**********
	 * Creates a new in-memory database with a Curator, a Contributor, and lessons A, B, and C.
	 *
	 * @throws Exception if the database cannot be set up
	 */
	@BeforeEach
	public void setUp() throws Exception {
		databaseUrl = "jdbc:h2:mem:tp3_summary_" + UUID.randomUUID().toString().replace("-", "");
		database = new Database(databaseUrl);
		database.connectToDatabase();

		curator = new User("summaryCurator", "Password123!", "Test", "", "Curator", "", "",
				false, false, true, false);
		contributor = new User("summaryContributor", "Password123!", "Test", "", "Contributor",
				"", "", false, true, false, false);
		database.register(curator);
		database.register(contributor);

		lessonA = newLesson("Deployment pipeline lesson");
		lessonB = newLesson("Database migration lesson");
		lessonC = newLesson("Lesson with no experience");
		addExperience(lessonA, 2.0);
		addExperience(lessonA, 4.0);
		addExperience(lessonB, 6.0);

		controller = new CuratorSummaryController(database);
	}

	/**********
	 * Closes the test database, which also discards it.
	 */
	@AfterEach
	public void tearDown() {
		if (database != null) database.closeConnection();
	}


	/*-********************************************************************************************

	US-C-E4-03: the statistics, computed by TimeEffortStatisticsCalculator

	 */

	/**********
	 * <p> SU-01: The worked example in US-C-E4-03.  For observations of 2, 4, and 6 hours the
	 * summary must be count 3, total 12, mean 4, median 4, minimum 2, maximum 6, and sample
	 * standard deviation 2 (denominator n - 1). </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("SU-01 Hours 2, 4, 6 give the story's example statistics (US-C-E4-03, P)")
	public void su01StoryExample() {
		LessonSummary s = TimeEffortStatisticsCalculator.computeStatistics(lessonA,
				Set.of(lessonA, lessonB), List.of(2.0, 4.0, 6.0));
		assertEquals(3, s.getEntryCount());
		assertEquals(12.0, s.getTotalHours(), 1e-9);
		assertEquals(4.0, s.getMeanHours(), 1e-9);
		assertEquals(4.0, s.getMedianHours(), 1e-9);
		assertEquals(2.0, s.getMinHours(), 1e-9);
		assertEquals(6.0, s.getMaxHours(), 1e-9);
		assertEquals(2.0, s.getStandardDeviationHours(), 1e-9);
		assertFalse(s.isVisibleToContributor(), "A new summary starts hidden (US-C-E5-02)");
	}

	/**********
	 * <p> SU-02: With an even number of observations the median is the mean of the two middle
	 * values, and the order the hours arrive in does not matter: 10, 1, 3, 2 has median 2.5. </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("SU-02 Even count: median is the mean of the two middle values (US-C-E4-03, P)")
	public void su02EvenMedian() {
		LessonSummary s = TimeEffortStatisticsCalculator.computeStatistics(lessonA,
				Set.of(lessonA, lessonB), List.of(10.0, 1.0, 3.0, 2.0));
		assertEquals(2.5, s.getMedianHours(), 1e-9);
		assertEquals(1.0, s.getMinHours(), 1e-9);
		assertEquals(10.0, s.getMaxHours(), 1e-9);
	}

	/**********
	 * <p> SU-03: With one observation the standard deviation is shown as "N/A: fewer than two
	 * observations" instead of a misleading number, while the other measures are still
	 * reported (US-C-E4-03 V sentence). </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("SU-03 One observation: standard deviation is N/A, others reported (US-C-E4-03, P)")
	public void su03OneObservation() {
		LessonSummary s = TimeEffortStatisticsCalculator.computeStatistics(lessonA,
				Set.of(lessonA, lessonC), List.of(5.0));
		assertEquals(1, s.getEntryCount());
		assertEquals(5.0, s.getMeanHours(), 1e-9);
		assertEquals(5.0, s.getMedianHours(), 1e-9);
		assertTrue(Double.isNaN(s.getStandardDeviationHours()));
		assertEquals("N/A: fewer than two observations", s.getFormattedStandardDeviation());
	}

	/**********
	 * <p> SU-04: Values are kept at full precision but shown to two decimal places, labeled in
	 * hours, and the count is labeled as experience records (US-C-E4-03). </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(),
	 * ContributorLessonView.renderSummaryPanelText(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("SU-04 Display: two decimals, hours, count labeled as experience records (US-C-E4-03, P)")
	public void su04DisplayPrecision() {
		LessonSummary s = TimeEffortStatisticsCalculator.computeStatistics(lessonA,
				Set.of(lessonA, lessonB), List.of(1.0, 2.0, 2.5));
		assertEquals(5.5 / 3.0, s.getMeanHours(), 1e-12, "Precision is kept in the value");
		s.setVisibleToContributor(true);
		String text = ContributorLessonView.renderSummaryPanelText(s);
		assertTrue(text.contains("(3 experience records)"), text);
		assertTrue(text.contains("Mean: 1.83 hrs"), text);
		assertTrue(text.contains("Std Dev: 0.76 hrs"), text);
	}


	/*-********************************************************************************************

	US-C-E4-01 and US-C-E4-02: which observations are merged

	 */

	/**********
	 * <p> SU-05: Missing, negative, and non-finite hours are left out rather than treated as
	 * zero, and the count is the number of valid observations (US-C-E4-02). </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("SU-05 Null, negative, NaN, and infinite hours are left out, not zero (US-C-E4-02, N)")
	public void su05InvalidHoursLeftOut() {
		List<Double> hours = new ArrayList<>(Arrays.asList(2.0, null, -1.0, Double.NaN,
				Double.POSITIVE_INFINITY, 4.0, 6.0));
		LessonSummary s = TimeEffortStatisticsCalculator.computeStatistics(lessonA,
				Set.of(lessonA, lessonB), hours);
		assertEquals(3, s.getEntryCount());
		assertEquals(12.0, s.getTotalHours(), 1e-9);
		assertEquals(2.0, s.getMinHours(), 1e-9, "-1 must not become the minimum");
	}

	/**********
	 * <p> SU-06: If there are no valid observations, the message is "No experience data to
	 * merge." (US-C-E4-02 V sentence), whether the list is empty, null, or all invalid. </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("SU-06 No valid hours gives \"No experience data to merge.\" (US-C-E4-02, N)")
	public void su06NoData() {
		for (List<Double> hours : Arrays.asList(null, Collections.<Double>emptyList(),
				Arrays.asList(-2.0, Double.NaN))) {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
					TimeEffortStatisticsCalculator.computeStatistics(lessonA,
							Set.of(lessonA, lessonB), hours));
			assertEquals("No experience data to merge.", e.getMessage());
		}
	}

	/**********
	 * <p> SU-07: A merge with more than MAX_ENTRIES_LIMIT (5000) records is refused with a clear
	 * message, instead of computing statistics on only part of the data (CWE-770). </p>
	 *
	 * <p> Validates: TimeEffortStatisticsCalculator.computeStatistics(). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("SU-07 More than 5000 experience records is refused, not truncated (US-C-E4-02, N)")
	public void su07TooManyEntries() {
		List<Double> hours = new ArrayList<>(Collections.nCopies(
				TimeEffortStatisticsCalculator.MAX_ENTRIES_LIMIT + 1, 1.0));
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
				TimeEffortStatisticsCalculator.computeStatistics(lessonA, Set.of(lessonA, lessonB),
						hours));
		assertEquals(TimeEffortStatisticsCalculator.TOO_MANY_ENTRIES, e.getMessage());
		// Exactly at the limit is still accepted
		hours.remove(0);
		assertEquals(TimeEffortStatisticsCalculator.MAX_ENTRIES_LIMIT,
				TimeEffortStatisticsCalculator.computeStatistics(lessonA, Set.of(lessonA, lessonB),
						hours).getEntryCount());
	}

	/**********
	 * <p> SU-08: Fewer than two distinct lessons gives "Select at least two lessons"
	 * (US-C-E4-01 V sentence): no lessons, one lesson, and one lesson selected twice. </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.getPendingPreview(). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("SU-08 Fewer than two distinct lessons gives \"Select at least two lessons\" (US-C-E4-01, N)")
	public void su08TooFewLessons() {
		for (List<Integer> ids : Arrays.asList(null, Collections.<Integer>emptyList(),
				List.of(lessonA), List.of(lessonA, lessonA))) {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
					controller.previewMergedSummary(curator, lessonA, ids));
			assertEquals("Select at least two lessons", e.getMessage());
		}
		assertNull(controller.getPendingPreview());
	}

	/**********
	 * <p> SU-09: Selecting a lesson again does not count it again: A, B, A gives the three
	 * observations 2, 4, 6, not five (US-C-E4-02).  Ids of zero or less are ignored. </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("SU-09 A lesson selected twice is counted once (US-C-E4-02, P)")
	public void su09DuplicatesCountedOnce() {
		LessonSummary s = controller.previewMergedSummary(curator, lessonC,
				Arrays.asList(lessonA, lessonB, lessonA, 0, -3));
		assertEquals(3, s.getEntryCount());
		assertEquals(12.0, s.getTotalHours(), 1e-9);
		assertEquals(new LinkedHashSet<>(List.of(lessonA, lessonB)), s.getSourceLessonIds());
	}

	/**********
	 * <p> SU-10: The target may also be a source (US-C-E4-02): target A with sources A and B. </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("SU-10 The target lesson may also be a source (US-C-E4-02, P)")
	public void su10TargetMayBeSource() {
		LessonSummary s = controller.previewMergedSummary(curator, lessonA, List.of(lessonA, lessonB));
		assertEquals(lessonA, s.getTargetLessonId());
		assertEquals(3, s.getEntryCount());
	}

	/**********
	 * <p> SU-11: A missing target, a missing source, and sources with no experience records are
	 * refused with their messages, and nothing is saved (US-C-E4-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("SU-11 Missing target, missing source, or no data is refused, nothing saved (US-C-E4-02, N)")
	public void su11InvalidLessons() {
		IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
				controller.previewMergedSummary(curator, 99999, List.of(lessonA, lessonB)));
		assertEquals(CuratorSummaryController.INVALID_TARGET, e1.getMessage());
		IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
				controller.previewMergedSummary(curator, lessonA, List.of(lessonA, 99999)));
		assertEquals(CuratorSummaryController.MISSING_SOURCE, e2.getMessage());
		int lessonD = newLesson("Another lesson with no experience");
		IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
				controller.previewMergedSummary(curator, lessonA, List.of(lessonC, lessonD)));
		assertEquals("No experience data to merge.", e3.getMessage());
		assertNull(database.getLessonSummary(lessonA));
	}


	/*-********************************************************************************************

	US-C-E4-01 and US-C-E4-02: preview, confirm, cancel, and save

	 */

	/**********
	 * <p> SU-12: A preview computes the summary from the database but saves nothing: the target
	 * has no saved summary and its summaryId is still -1 (US-C-E4-01). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(). </p>
	 */
	@Test
	@Order(12)
	@DisplayName("SU-12 Preview reads the database and saves nothing (US-C-E4-01, P)")
	public void su12PreviewSavesNothing() {
		LessonSummary preview = controller.previewMergedSummary(curator, lessonC,
				List.of(lessonA, lessonB));
		assertEquals(2.0, preview.getStandardDeviationHours(), 1e-9);
		assertNull(database.getLessonSummary(lessonC));
		assertEquals(-1, database.getLessonById(lessonC).getSummaryId());
	}

	/**********
	 * <p> SU-13: Confirming saves the summary on the target.  Reopening the target, through a
	 * second connection to the same database, gives the same statistics and source references,
	 * and the summary is hidden by default (US-C-E4-01, US-C-E5-02). </p>
	 *
	 * @throws Exception if the second connection cannot be opened
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(),
	 * CuratorSummaryController.getPendingPreview(). </p>
	 */
	@Test
	@Order(13)
	@DisplayName("SU-13 Confirm saves; reopening keeps the summary and sources; hidden (US-C-E4-01, P)")
	public void su13ConfirmSaves() throws Exception {
		controller.previewMergedSummary(curator, lessonC, List.of(lessonA, lessonB));
		LessonSummary saved = controller.confirmAndSaveSummary(curator, lessonC,
				List.of(lessonB, lessonA));
		assertTrue(saved.getSummaryId() > 0);
		assertNull(controller.getPendingPreview(), "The preview is used up by the confirmation");

		Database reopened = new Database(databaseUrl);
		reopened.connectToDatabase();
		try {
			LessonSummary read = reopened.getLessonSummary(lessonC);
			assertNotNull(read);
			assertEquals(saved.getSummaryId(), read.getSummaryId());
			assertEquals(saved.getSummaryId(), reopened.getLessonById(lessonC).getSummaryId());
			assertEquals(3, read.getEntryCount());
			assertEquals(12.0, read.getTotalHours(), 1e-9);
			assertEquals(2.0, read.getStandardDeviationHours(), 1e-9);
			assertEquals(new LinkedHashSet<>(List.of(lessonA, lessonB)), read.getSourceLessonIds());
			assertFalse(read.isVisibleToContributor());
		} finally {
			reopened.closeConnection();
		}
	}

	/**********
	 * <p> SU-14: Confirming without a preview is refused with "Preview summary statistics before
	 * confirming." and nothing is saved (US-C-E4-01). </p>
	 *
	 * <p> Validates: CuratorSummaryController.confirmAndSaveSummary(). </p>
	 */
	@Test
	@Order(14)
	@DisplayName("SU-14 Confirm without a preview is refused (US-C-E4-01, N)")
	public void su14ConfirmWithoutPreview() {
		IllegalStateException e = assertThrows(IllegalStateException.class, () ->
				controller.confirmAndSaveSummary(curator, lessonC, List.of(lessonA, lessonB)));
		assertEquals(CuratorSummaryController.NO_PREVIEW, e.getMessage());
		assertNull(database.getLessonSummary(lessonC));
	}

	/**********
	 * <p> SU-15: Cancelling a new preview leaves the existing summary unchanged, and a later
	 * Confirm is refused because there is no preview (US-C-E4-01 V sentence). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.clearPreview(),
	 * CuratorSummaryController.confirmAndSaveSummary(). </p>
	 */
	@Test
	@Order(15)
	@DisplayName("SU-15 Cancel saves nothing and keeps the existing summary (US-C-E4-01, N)")
	public void su15CancelKeepsExisting() {
		saveSummaryOnC();
		LessonSummary before = database.getLessonSummary(lessonC);

		controller.previewMergedSummary(curator, lessonC, List.of(lessonA, lessonC));
		controller.clearPreview();
		assertThrows(IllegalStateException.class, () ->
				controller.confirmAndSaveSummary(curator, lessonC, List.of(lessonA, lessonC)));

		LessonSummary after = database.getLessonSummary(lessonC);
		assertEquals(before.getSummaryId(), after.getSummaryId());
		assertEquals(3, after.getEntryCount());
	}

	/**********
	 * <p> SU-16: If the selection changes between preview and Confirm, the preview is discarded
	 * with "Selected lessons changed since preview. ..." and nothing is saved (US-C-E4-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(),
	 * CuratorSummaryController.getPendingPreview(). </p>
	 */
	@Test
	@Order(16)
	@DisplayName("SU-16 Changed selection discards the preview, nothing saved (US-C-E4-02, N)")
	public void su16SelectionChanged() {
		controller.previewMergedSummary(curator, lessonC, List.of(lessonA, lessonB));
		IllegalStateException e = assertThrows(IllegalStateException.class, () ->
				controller.confirmAndSaveSummary(curator, lessonC, List.of(lessonA, lessonC)));
		assertEquals(CuratorSummaryController.SELECTION_CHANGED, e.getMessage());
		assertNull(controller.getPendingPreview());
		assertNull(database.getLessonSummary(lessonC));
	}

	/**********
	 * <p> SU-17: If an experience record is added after the preview, Confirm refreshes the
	 * preview and asks for a new confirmation, saving nothing; the next Confirm saves the
	 * refreshed summary (US-C-E4-02: "refresh the preview and require a new confirmation"). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(),
	 * CuratorSummaryController.getPendingPreview(). </p>
	 */
	@Test
	@Order(17)
	@DisplayName("SU-17 Changed data refreshes the preview and needs a new Confirm (US-C-E4-02, N)")
	public void su17DataChanged() {
		controller.previewMergedSummary(curator, lessonC, List.of(lessonA, lessonB));
		addExperience(lessonB, 8.0);

		IllegalStateException e = assertThrows(IllegalStateException.class, () ->
				controller.confirmAndSaveSummary(curator, lessonC, List.of(lessonA, lessonB)));
		assertEquals(CuratorSummaryController.DATA_CHANGED, e.getMessage());
		assertNull(database.getLessonSummary(lessonC), "Nothing is saved");
		assertEquals(4, controller.getPendingPreview().getEntryCount(), "The preview is refreshed");

		LessonSummary saved = controller.confirmAndSaveSummary(curator, lessonC,
				List.of(lessonA, lessonB));
		assertEquals(20.0, saved.getTotalHours(), 1e-9);
		assertEquals(20.0, database.getLessonSummary(lessonC).getTotalHours(), 1e-9);
	}

	/**********
	 * <p> SU-18: Repeating a merge replaces the summary instead of adding the same observations
	 * to the old total: two merges of A and B still give count 3 and total 12, with a new
	 * summary id (US-C-E4-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(). </p>
	 */
	@Test
	@Order(18)
	@DisplayName("SU-18 Repeating a merge replaces the summary, no double totals (US-C-E4-02, P)")
	public void su18RepeatReplaces() {
		LessonSummary first = saveSummaryOnC();
		LessonSummary second = saveSummaryOnC();
		assertTrue(second.getSummaryId() != first.getSummaryId());
		LessonSummary read = database.getLessonSummary(lessonC);
		assertEquals(second.getSummaryId(), read.getSummaryId());
		assertEquals(3, read.getEntryCount());
		assertEquals(12.0, read.getTotalHours(), 1e-9);
	}

	/**********
	 * <p> SU-19: Merging leaves the source lessons and their raw experience records unchanged
	 * (US-C-E4-01, US-C-E4-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(). </p>
	 */
	@Test
	@Order(19)
	@DisplayName("SU-19 Merging leaves the source lessons and raw records unchanged (US-C-E4-02, P)")
	public void su19SourcesUnchanged() {
		String beforeA = describe(lessonA);
		String beforeB = describe(lessonB);
		saveSummaryOnC();
		assertEquals(beforeA, describe(lessonA));
		assertEquals(beforeB, describe(lessonB));
	}


	/*-********************************************************************************************

	US-C-E5-01 and US-C-E5-02: Contributor visibility

	 */

	/**********
	 * <p> SU-20: Alex's acceptance check 1 for US-C-E5-01: "A Curator can toggle a saved merged
	 * summary between visible and hidden on a target lesson, and the updated state persists in
	 * the database."  Each state is read back through a second connection. </p>
	 *
	 * @throws Exception if the second connection cannot be opened
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(). </p>
	 */
	@Test
	@Order(20)
	@DisplayName("SU-20 A Curator's visible/hidden toggle persists in the database (US-C-E5-01, P)")
	public void su20TogglePersists() throws Exception {
		saveSummaryOnC();
		Database reopened = new Database(databaseUrl);
		reopened.connectToDatabase();
		try {
			controller.updateStatisticsVisibility(curator, lessonC, true);
			assertTrue(reopened.getLessonSummary(lessonC).isVisibleToContributor());
			assertTrue(reopened.getLessonById(lessonC).isStatsVisibleToContributor());
			controller.updateStatisticsVisibility(curator, lessonC, false);
			assertFalse(reopened.getLessonSummary(lessonC).isVisibleToContributor());
			assertFalse(reopened.getLessonById(lessonC).isStatsVisibleToContributor());
		} finally {
			reopened.closeConnection();
		}
	}

	/**********
	 * <p> SU-21: Alex's acceptance check 2 for US-C-E5-01: "Toggling visibility updates the
	 * access state immediately without altering the underlying computed statistical metrics or
	 * raw experience records." </p>
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(),
	 * ContributorLessonView.readSummaryForContributor(). </p>
	 */
	@Test
	@Order(21)
	@DisplayName("SU-21 Toggling changes access at once, not the statistics or records (US-C-E5-01, P)")
	public void su21ToggleChangesNothingElse() {
		LessonSummary saved = saveSummaryOnC();
		String statsBefore = statistics(database.getLessonSummary(lessonC));
		String recordsBefore = describe(lessonA) + describe(lessonB);

		controller.updateStatisticsVisibility(curator, lessonC, true);
		assertNotNull(ContributorLessonView.readSummaryForContributor(database, lessonC),
				"Visible at once");
		controller.updateStatisticsVisibility(curator, lessonC, false);
		assertNull(ContributorLessonView.readSummaryForContributor(database, lessonC),
				"Hidden at once");

		LessonSummary after = database.getLessonSummary(lessonC);
		assertEquals(saved.getSummaryId(), after.getSummaryId());
		assertEquals(statsBefore, statistics(after));
		assertEquals(recordsBefore, describe(lessonA) + describe(lessonB));
	}

	/**********
	 * <p> SU-22: Showing or hiding is refused for a lesson that has no saved summary and for a
	 * lesson that does not exist. </p>
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(). </p>
	 */
	@Test
	@Order(22)
	@DisplayName("SU-22 Toggling a lesson with no summary, or no lesson, is refused (US-C-E5-01, N)")
	public void su22ToggleWithoutSummary() {
		IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
				controller.updateStatisticsVisibility(curator, lessonA, true));
		assertEquals(CuratorSummaryController.NO_SUMMARY, e1.getMessage());
		assertFalse(database.getLessonById(lessonA).isStatsVisibleToContributor());
		IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
				controller.updateStatisticsVisibility(curator, 99999, true));
		assertEquals(CuratorSummaryController.INVALID_TARGET, e2.getMessage());
	}

	/**********
	 * <p> SU-23: Only a Curator can preview, confirm, read the saved summary, or change its
	 * visibility.  A Contributor, an Admin without the Curator role, and no user are all refused
	 * with the same message, and nothing changes (US-C-E5-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.previewMergedSummary(),
	 * CuratorSummaryController.confirmAndSaveSummary(),
	 * CuratorSummaryController.getSavedSummary(),
	 * CuratorSummaryController.updateStatisticsVisibility(). </p>
	 */
	@Test
	@Order(23)
	@DisplayName("SU-23 Only a Curator can merge or change visibility (US-C-E5-02, N)")
	public void su23OnlyCurator() {
		saveSummaryOnC();
		User admin = new User("summaryAdmin", "Password123!", "Test", "", "Admin", "", "",
				true, false, false, false);
		for (User user : Arrays.asList(contributor, admin, null)) {
			SecurityException e = assertThrows(SecurityException.class, () ->
					controller.previewMergedSummary(user, lessonC, List.of(lessonA, lessonB)));
			assertEquals(CuratorSummaryController.NOT_CURATOR, e.getMessage());
			assertThrows(SecurityException.class, () ->
					controller.confirmAndSaveSummary(user, lessonC, List.of(lessonA, lessonB)));
			assertThrows(SecurityException.class, () ->
					controller.getSavedSummary(user, lessonC));
			assertThrows(SecurityException.class, () ->
					controller.updateStatisticsVisibility(user, lessonC, true));
		}
		assertFalse(database.getLessonSummary(lessonC).isVisibleToContributor());
	}

	/**********
	 * <p> SU-24: A new summary is hidden by default: the Contributor read returns nothing and
	 * the panel text is empty (US-C-E5-02). </p>
	 *
	 * <p> Validates: ContributorLessonView.readSummaryForContributor(),
	 * ContributorLessonView.renderSummaryPanelText(). </p>
	 */
	@Test
	@Order(24)
	@DisplayName("SU-24 A new summary is hidden from Contributors by default (US-C-E5-02, P)")
	public void su24HiddenByDefault() {
		saveSummaryOnC();
		assertNull(ContributorLessonView.readSummaryForContributor(database, lessonC));
		assertEquals("", ContributorLessonView.renderSummaryPanelText(
				ContributorLessonView.readSummaryForContributor(database, lessonC)));
	}

	/**********
	 * <p> SU-25: Once the Curator makes it visible, the Contributor read returns the summary and
	 * the panel shows its statistics (US-C-E5-01, US-C-E5-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(),
	 * ContributorLessonView.readSummaryForContributor(),
	 * ContributorLessonView.renderSummaryPanelText(). </p>
	 */
	@Test
	@Order(25)
	@DisplayName("SU-25 A visible summary is shown to Contributors (US-C-E5-02, P)")
	public void su25VisibleIsShown() {
		saveSummaryOnC();
		controller.updateStatisticsVisibility(curator, lessonC, true);
		LessonSummary shown = ContributorLessonView.readSummaryForContributor(database, lessonC);
		assertNotNull(shown);
		String text = ContributorLessonView.renderSummaryPanelText(shown);
		assertTrue(text.startsWith("Merged Effort Summary (3 experience records)"), text);
		assertTrue(text.contains("Total Hours: 12.00 hrs"), text);
		assertTrue(text.contains("Std Dev: 2.00 hrs"), text);
	}

	/**********
	 * <p> SU-26: Hiding a summary again removes it on the next read: the flag is read from the
	 * database each time, so nothing cached on a page can keep showing it (US-C-E5-01 V
	 * sentence, US-C-E5-02: "hiding again removes access on the next read"). </p>
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(),
	 * ContributorLessonView.readSummaryForContributor(). </p>
	 */
	@Test
	@Order(26)
	@DisplayName("SU-26 Hiding again removes the summary on the next read (US-C-E5-02, N)")
	public void su26HideAgain() {
		saveSummaryOnC();
		controller.updateStatisticsVisibility(curator, lessonC, true);
		assertNotNull(ContributorLessonView.readSummaryForContributor(database, lessonC));
		controller.updateStatisticsVisibility(curator, lessonC, false);
		assertNull(ContributorLessonView.readSummaryForContributor(database, lessonC));
	}

	/**********
	 * <p> SU-27: A lesson with no summary shows no statistics and no made-up zeros: the
	 * Contributor read returns nothing and the panel text is empty (US-C-E5-02 V sentence). </p>
	 *
	 * <p> Validates: ContributorLessonView.readSummaryForContributor(),
	 * ContributorLessonView.renderSummaryPanelText(). </p>
	 */
	@Test
	@Order(27)
	@DisplayName("SU-27 No summary: no statistics and no made-up zeros (US-C-E5-02, N)")
	public void su27NoSummaryNoZeros() {
		assertNull(ContributorLessonView.readSummaryForContributor(database, lessonA));
		assertEquals("", ContributorLessonView.renderSummaryPanelText(null));
		assertNull(ContributorLessonView.readSummaryForContributor(null, lessonA));
	}

	/**********
	 * <p> SU-28: Re-merging a visible summary saves a new summary, which starts hidden again
	 * ("new summaries default to hidden", US-C-E5-02). </p>
	 *
	 * <p> Validates: CuratorSummaryController.updateStatisticsVisibility(),
	 * ContributorLessonView.readSummaryForContributor(). </p>
	 */
	@Test
	@Order(28)
	@DisplayName("SU-28 A re-merged summary starts hidden again (US-C-E5-02, P)")
	public void su28ReMergeIsHidden() {
		saveSummaryOnC();
		controller.updateStatisticsVisibility(curator, lessonC, true);
		saveSummaryOnC();
		assertFalse(database.getLessonSummary(lessonC).isVisibleToContributor());
		assertNull(ContributorLessonView.readSummaryForContributor(database, lessonC));
	}

	/**********
	 * <p> SU-29: Deleting the target lesson deletes its summary, so no orphan summary is left
	 * (ON DELETE CASCADE). </p>
	 *
	 * <p> Validates: Database.deleteLesson(). </p>
	 */
	@Test
	@Order(29)
	@DisplayName("SU-29 Deleting the target lesson deletes its summary (US-C-E4-01, P)")
	public void su29DeleteTarget() {
		saveSummaryOnC();
		assertTrue(database.deleteLesson(lessonC));
		assertNull(database.getLessonSummary(lessonC));
		assertNotNull(database.getLessonById(lessonA), "The source lessons are not deleted");
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
	 * Previews and confirms a merge of lessons A and B on lesson C.
	 *
	 * @return the saved summary
	 */
	private LessonSummary saveSummaryOnC() {
		controller.previewMergedSummary(curator, lessonC, List.of(lessonA, lessonB));
		return controller.confirmAndSaveSummary(curator, lessonC, List.of(lessonA, lessonB));
	}

	/**********
	 * Describes a lesson's title, description, and every experience record, so two
	 * descriptions are equal only if nothing in them changed.
	 *
	 * @param lessonId the lesson
	 * @return the description
	 */
	private String describe(int lessonId) {
		LessonsLearned lesson = database.getLessonById(lessonId);
		StringBuilder sb = new StringBuilder(lesson.getTitle() + "|" + lesson.getDescription());
		for (ExperienceEntry entry : database.getExperienceForLesson(lessonId))
			sb.append("|").append(entry.getId()).append(":").append(entry.getTimeEffortHours())
					.append(":").append(entry.getDescription());
		return sb.toString();
	}

	/**********
	 * Lists every statistic of a summary, so two lists are equal only if no number changed.
	 *
	 * @param s the summary
	 * @return the statistics as text
	 */
	private static String statistics(LessonSummary s) {
		return s.getEntryCount() + "|" + s.getTotalHours() + "|" + s.getMeanHours() + "|"
				+ s.getMedianHours() + "|" + s.getMinHours() + "|" + s.getMaxHours() + "|"
				+ s.getStandardDeviationHours() + "|" + s.getSourceLessonIds();
	}
}
