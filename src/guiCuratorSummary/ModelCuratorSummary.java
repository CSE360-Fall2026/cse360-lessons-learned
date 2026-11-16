package guiCuratorSummary;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import database.Database;
import entityClasses.LessonSummary;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;

/*******
 * <p> Title: ModelCuratorSummary Class. </p>
 *
 * <p> Description: The Model of the Curator Merge Statistics page (TP3 Aspects 3 and 4; Epics E4
 * and E5).  It holds everything the page does that is not drawing widgets, so all of it can be
 * tested with JUnit and no screen: reading the target lesson and the lessons to choose from,
 * the preview, confirmation, cancel, and show or hide actions, and the words the page shows.</p>
 *
 * <p> The merge and visibility rules themselves are in Alex Meyers' CuratorSummaryController,
 * which this Model calls.  The Model turns the controller's exceptions into a Result with the
 * message to show, so the page never has to handle an exception and every refusal is shown in
 * the same place.</p>
 *
 * <p> The Model works on the Database it is given.  The page gives it FoundationsMain.database;
 * the JUnit tests give it an in-memory database (curatorSummaryTests.CuratorSummaryPageTests).</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-16 Initial version for TP3 Aspects 3 and 4
 */
public class ModelCuratorSummary {

	/** Shown after a summary is saved (US-C-E4-01, US-C-E5-02: new summaries start hidden). */
	public static final String SAVED = "Summary saved on this lesson.  It is hidden from "
			+ "contributors until you show it.";

	/** Shown after the summary is made visible (US-C-E5-01). */
	public static final String NOW_VISIBLE = "The summary is now visible to contributors.";

	/** Shown after the summary is hidden (US-C-E5-01). */
	public static final String NOW_HIDDEN = "The summary is now hidden from contributors.";

	/** Shown after Cancel (US-C-E4-01 V sentence: nothing is saved). */
	public static final String CANCELLED = "Preview cancelled.  Nothing was saved.";

	/** Shown when the Curator answers No to the confirmation. */
	public static final String NO_CHANGE = "No change was made.";

	/** Shown in the table area before anything has been previewed. */
	public static final String NOTHING_PREVIEWED = "Select at least two lessons on the left and "
			+ "press Preview to see their merged statistics here.  Nothing is saved until you "
			+ "confirm.";

	/** Shown when the lesson has no saved summary. */
	public static final String NO_SAVED_SUMMARY = "Saved summary: none yet.";

	/*-*******************************************************************************************

	The result of a page action

	 */

	/**********
	 * <p> Title: Result Class. </p>
	 *
	 * <p> Description: The outcome of one page action: whether it worked, the message to show,
	 * the summary to show (a preview, a saved summary, or a refreshed preview), and, for
	 * openLesson, the lesson. </p>
	 */
	public static final class Result {
		private final boolean success;
		private final String message;
		private final LessonSummary summary;
		private final LessonsLearned lesson;

		private Result(boolean success, String message, LessonSummary summary,
				LessonsLearned lesson) {
			this.success = success;
			this.message = (message == null) ? "" : message;
			this.summary = summary;
			this.lesson = lesson;
		}

		/** @return true if the action worked */
		public boolean isSuccess() { return success; }
		/** @return the message to show; empty if there is nothing to say */
		public String getMessage() { return message; }
		/** @return the summary to show, or null */
		public LessonSummary getSummary() { return summary; }
		/** @return the lesson that was opened, or null */
		public LessonsLearned getLesson() { return lesson; }
	}

	/*-*******************************************************************************************

	Attributes and constructor

	 */

	/** The database this Model reads and writes. */
	private final Database database;
	/** Alex Meyers' merge and visibility workflow, which holds the current preview. */
	private final CuratorSummaryController controller;

	/**********
	 * Creates the Model for one page, working on the given database.
	 *
	 * @param database the connected database
	 */
	public ModelCuratorSummary(Database database) {
		this.database = database;
		this.controller = new CuratorSummaryController(database);
	}

	/*-*******************************************************************************************

	Reading

	 */

	/**********
	 * <p> Method: Result openLesson(User curator, int lessonId) </p>
	 *
	 * <p> Description: Opens the target lesson: checks the Curator role and that the lesson
	 * exists, discards any preview left from an earlier visit, and returns the lesson with its
	 * saved summary (null if it has none). </p>
	 *
	 * @param curator	the logged-in user
	 * @param lessonId	the target lesson
	 * @return the lesson and saved summary, or the reason it cannot be opened
	 */
	public Result openLesson(User curator, int lessonId) {
		controller.clearPreview();
		if (!CuratorAccess.isCurator(curator))
			return new Result(false, CuratorSummaryController.NOT_CURATOR, null, null);
		LessonsLearned lesson = database.getLessonById(lessonId);
		if (lesson == null) return new Result(false, LessonAccess.NOT_FOUND, null, null);
		return new Result(true, "", database.getLessonSummary(lessonId), lesson);
	}

	/**********
	 * <p> Method: List&lt;LessonsLearned&gt; sourceChoices(User curator) </p>
	 *
	 * <p> Description: Every lesson, for the Curator to choose sources from.  A Curator can see
	 * every Contributor's lessons (US-CH); anyone else gets an empty list. </p>
	 *
	 * @param curator	the logged-in user
	 * @return the lessons, or an empty list
	 */
	public List<LessonsLearned> sourceChoices(User curator) {
		if (!CuratorAccess.isCurator(curator)) return new ArrayList<LessonsLearned>();
		return new ArrayList<LessonsLearned>(database.getAllLessons().getLessons());
	}

	/**********
	 * <p> Method: String choiceRow(LessonsLearned lesson) </p>
	 *
	 * <p> Description: One row of the list of lessons to choose from: the title, the owner, and
	 * how many experience records the lesson has, so the Curator can judge which lessons are
	 * comparable and which have data to merge (US-C-E4-02). </p>
	 *
	 * @param lesson	the lesson
	 * @return the row text
	 */
	public String choiceRow(LessonsLearned lesson) {
		int n = database.getExperienceForLesson(lesson.getId()).size();
		return lesson.getTitle() + "   by " + lesson.getOwnerUserName() + "   (" + n
				+ (n == 1 ? " experience record)" : " experience records)");
	}

	/*-*******************************************************************************************

	Actions

	 */

	/**********
	 * <p> Method: Result preview(User curator, int targetId, List&lt;Integer&gt; selected) </p>
	 *
	 * <p> Description: Computes the merged summary of the selected lessons without saving it
	 * (US-C-E4-01).  A refusal ("Select at least two lessons", "No experience data to merge.",
	 * and the rest) comes back as the message, with no summary. </p>
	 *
	 * @param curator	the logged-in user
	 * @param targetId	the target lesson
	 * @param selected	the ids of the selected source lessons
	 * @return the preview, or the reason there is none
	 */
	public Result preview(User curator, int targetId, List<Integer> selected) {
		try {
			return new Result(true, "", controller.previewMergedSummary(curator, targetId,
					selected), null);
		} catch (RuntimeException e) {
			return new Result(false, e.getMessage(), null, null);
		}
	}

	/**********
	 * <p> Method: Result confirm(User curator, int targetId, List&lt;Integer&gt; selected) </p>
	 *
	 * <p> Description: Saves the previewed summary on the target lesson, after the Curator has
	 * answered Yes to confirmationQuestion() (US-C-E4-01).  If the experience data changed since
	 * the preview, nothing is saved and the result carries the refreshed preview, so the page
	 * can show the new numbers before the Curator confirms again (US-C-E4-02). </p>
	 *
	 * @param curator	the logged-in user
	 * @param targetId	the target lesson
	 * @param selected	the ids of the selected source lessons, which must match the preview
	 * @return the saved summary and SAVED, or the reason nothing was saved
	 */
	public Result confirm(User curator, int targetId, List<Integer> selected) {
		try {
			LessonSummary saved = controller.confirmAndSaveSummary(curator, targetId, selected);
			return new Result(true, SAVED, saved, null);
		} catch (RuntimeException e) {
			// After DATA_CHANGED this is the refreshed preview; otherwise it is null
			return new Result(false, e.getMessage(), controller.getPendingPreview(), null);
		}
	}

	/**********
	 * <p> Method: void cancel() </p>
	 *
	 * <p> Description: Discards the preview without saving anything (US-C-E4-01 V sentence). </p>
	 */
	public void cancel() {
		controller.clearPreview();
	}

	/**********
	 * <p> Method: boolean hasPreview() </p>
	 *
	 * <p> Description: Is there a preview that Confirm could save?  The page enables Confirm and
	 * Cancel only while there is one. </p>
	 *
	 * @return true if a preview is waiting for confirmation
	 */
	public boolean hasPreview() {
		return controller.getPendingPreview() != null;
	}

	/**********
	 * <p> Method: Result setVisibility(User curator, int targetId, boolean visible) </p>
	 *
	 * <p> Description: Shows or hides the lesson's saved summary for Contributors (US-C-E5-01).
	 * Only the visibility changes, never the numbers (US-C-E5-02). </p>
	 *
	 * @param curator	the logged-in user
	 * @param targetId	the target lesson
	 * @param visible	true to show the summary, false to hide it
	 * @return the saved summary as it is now, or the reason nothing changed
	 */
	public Result setVisibility(User curator, int targetId, boolean visible) {
		try {
			controller.updateStatisticsVisibility(curator, targetId, visible);
			return new Result(true, visible ? NOW_VISIBLE : NOW_HIDDEN,
					database.getLessonSummary(targetId), null);
		} catch (RuntimeException e) {
			return new Result(false, e.getMessage(), null, null);
		}
	}

	/*-*******************************************************************************************

	Words the page shows

	 */

	/**********
	 * <p> Method: String confirmationQuestion(String targetTitle, boolean replacing) </p>
	 *
	 * <p> Description: The question asked before a summary is saved.  It says that an existing
	 * summary will be replaced and that the new one starts hidden, so the Curator knows the
	 * effect before answering (US-C-E4-02, US-C-E5-02). </p>
	 *
	 * @param targetTitle	the target lesson's title
	 * @param replacing		true if the lesson already has a saved summary
	 * @return the question
	 */
	public static String confirmationQuestion(String targetTitle, boolean replacing) {
		return "Save this summary on '" + targetTitle + "'?"
				+ (replacing ? "  It replaces the lesson's current summary." : "")
				+ "  It will be hidden from contributors until you show it.";
	}

	/**********
	 * <p> Method: String savedLine(LessonSummary saved) </p>
	 *
	 * <p> Description: One line saying whether the lesson has a saved summary and whether
	 * Contributors can see it (US-C-E5-01). </p>
	 *
	 * @param saved	the lesson's saved summary, or null
	 * @return the line
	 */
	public static String savedLine(LessonSummary saved) {
		if (saved == null) return NO_SAVED_SUMMARY;
		return "Saved summary: " + saved.getEntryCount()
				+ (saved.getEntryCount() == 1 ? " experience record, " : " experience records, ")
				+ (saved.isVisibleToContributor() ? "VISIBLE to contributors."
						: "HIDDEN from contributors.");
	}

	/**********
	 * <p> Method: String tableText(String heading, LessonSummary summary) </p>
	 *
	 * <p> Description: The labeled table of a summary (US-C-E4-03): the source lessons by title,
	 * then the count of experience records and the total, mean, median, minimum, maximum, and
	 * sample standard deviation in hours, to two decimal places.  With one observation the
	 * standard deviation reads "N/A: fewer than two observations".  A source lesson deleted
	 * since the merge is listed by its number and "(deleted)". </p>
	 *
	 * @param heading	the first line, such as "PREVIEW (not saved)"
	 * @param summary	the summary to show
	 * @return the table text
	 */
	public String tableText(String heading, LessonSummary summary) {
		StringBuilder sb = new StringBuilder(heading).append("\n\n");
		sb.append("Source lessons (").append(summary.getSourceLessonIds().size()).append("):\n");
		for (int id : summary.getSourceLessonIds()) {
			LessonsLearned source = database.getLessonById(id);
			sb.append("  - ").append(source == null ? "lesson " + id + " (deleted)"
					: source.getTitle()).append("\n");
		}
		sb.append("\n");
		sb.append(row("Experience records", String.valueOf(summary.getEntryCount())));
		sb.append(row("Total hours", hours(summary.getTotalHours())));
		sb.append(row("Mean hours", hours(summary.getMeanHours())));
		sb.append(row("Median hours", hours(summary.getMedianHours())));
		sb.append(row("Minimum hours", hours(summary.getMinHours())));
		sb.append(row("Maximum hours", hours(summary.getMaxHours())));
		String sd = (summary.getEntryCount() < 2 || Double.isNaN(summary.getStandardDeviationHours()))
				? "N/A: fewer than two observations" : hours(summary.getStandardDeviationHours());
		sb.append(row("Sample std. deviation", sd));
		sb.append("\nThese are descriptive statistics of the recorded hours, not a rating of "
				+ "the lessons.");
		return sb.toString();
	}

	/**********
	 * One table row: the label padded to a fixed width, then the value.
	 *
	 * @param label	the row label
	 * @param value	the value
	 * @return the row, ending in a newline
	 */
	private static String row(String label, String value) {
		return String.format(Locale.ROOT, "%-24s%s\n", label, value);
	}

	/**********
	 * Hours to two decimal places (US-C-E4-03).
	 *
	 * @param value	the hours
	 * @return the formatted value
	 */
	private static String hours(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
