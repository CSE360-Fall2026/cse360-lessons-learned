package guiLessonDetails;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import guiContributorHome.ContributorLessonView;
import guiTools.LessonAccess;
import guiTools.LessonResult;

/*******
 * <p> Title: ModelLessonDetails Class. </p>
 *
 * <p> Description: The Lesson Details Page Model.  It performs the data work behind examining
 * one lesson (US-R3, US-R3-V) and deleting it (US-D1, US-D1-V), using the LessonsLearned entity
 * class and the shared ownership rule in guiTools.LessonAccess.</p>
 *
 * <p> Every operation runs its checks in the order of the validation stages in the team's
 * InputValidation.pdf: first authorization (does the lesson exist, and does it belong to the
 * requesting Contributor?), then the operation's own rules, and only then the write.</p>
 *
 * <p> The methods are public so the automated tests can call them without starting JavaFX.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (view and delete from HW2's
 * 							ModelRole1Home)
 * @version 1.01		2026-11-16 TP3 Aspect 4: summaryPanelText() for the merged effort summary
 */

public class ModelLessonDetails {

	/*****
	 * <p> Method: ModelLessonDetails() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelLessonDetails() {

	}


	/**********
	 * <p> Method: LessonResult viewLesson(String requestingUser, int lessonId) </p>
	 *
	 * <p> Description: READ (one).  Implements US-R3, US-R3-V, and US-R4.  On success the lesson
	 * comes back with its experience entries (and their members' shares) loaded, and its lock
	 * flags tell the View which fields to mark as locked.  A missing lesson and another
	 * Contributor's lesson both give "Lesson not found." (see guiTools.LessonAccess for
	 * why).</p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson to examine
	 * @return					on success the lesson with its experience loaded; otherwise
	 * 							"Lesson not found."
	 */
	public static LessonResult viewLesson(String requestingUser, int lessonId) {
		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V

		lesson.loadExperienceEntries();											// US-R3
		return LessonResult.passed("", lessonId, lesson);
	}


	/**********
	 * <p> Method: LessonResult deleteLesson(String requestingUser, int lessonId,
	 * 		boolean confirmed) </p>
	 *
	 * <p> Description: DELETE.  Implements US-D1 and US-D1-V.  Order of checks:
	 * <br> 1. The lesson exists and belongs to requestingUser (US-R3-V).
	 * <br> 2. Neither the lesson nor any of its fields is locked (US-D1-V).  This comes BEFORE
	 * the confirmation on purpose, so a locked lesson never reaches an "Are you sure?" prompt
	 * that could not lead anywhere (Test Case "Delete Lesson - Rejected Because Lesson Is
	 * Locked").
	 * <br> 3. The Contributor answered "Yes" (US-D1).</p>
	 *
	 * <p> The Controller relies on this order.  It first calls with confirmed = false.  A
	 * non-empty message means the deletion is refused and the message is shown.  An EMPTY
	 * message means "this lesson can be deleted, waiting for Yes", so the Controller shows its
	 * "Are you sure?" dialog and, only on an explicit Yes, calls again with confirmed = true.</p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson to delete
	 * @param confirmed			true only if the Contributor answered "Yes"
	 * @return					on success "Lesson deleted".  If confirmed is false and the lesson
	 * 							can be deleted, a failed result with an EMPTY message (backing out
	 * 							is not an error).  Otherwise the specific refusal message.
	 */
	public static LessonResult deleteLesson(String requestingUser, int lessonId,
			boolean confirmed) {

		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V

		if (lesson.isLocked())
			return LessonResult.failed(LessonAccess.LESSON_LOCKED, "");			// US-D1-V

		if (!confirmed) return LessonResult.failed("", "");						// US-D1-V

		if (!lesson.delete()) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-D1
		return LessonResult.passed("Lesson deleted", lessonId, null);
	}


	/**********
	 * <p> Method: String summaryPanelText(int lessonId) </p>
	 *
	 * <p> Description: TP3 Aspect 4.  The text of the merged effort summary panel for one of the
	 * Contributor's own lessons: the Curator's summary, one statistic per line, if the Curator
	 * made it visible; otherwise ContributorLessonView.EMPTY_STATE.  The summary is read through
	 * ContributorLessonView.readSummaryForContributor(), which drops a hidden summary at the
	 * read boundary, and the visibility is read from the database each time, so hiding it again
	 * takes effect the next time the page is shown (US-C-E5-02).  The caller shows this only
	 * after viewLesson() has confirmed the lesson is the user's own. </p>
	 *
	 * @param lessonId	the lesson being shown
	 *
	 * @return the panel text
	 */
	public static String summaryPanelText(int lessonId) {
		return summaryPanelText(lessonId, FoundationsMain.database);
	}


	/**********
	 * <p> Method: String summaryPanelText(int lessonId, Database database) </p>
	 *
	 * <p> Description: The same as summaryPanelText(int), on the given database, so the JUnit
	 * tests can use an in-memory database. </p>
	 *
	 * @param lessonId	the lesson being shown
	 *
	 * @param database	the database to read
	 *
	 * @return the panel text
	 */
	public static String summaryPanelText(int lessonId, Database database) {
		return ContributorLessonView.renderSummaryPanelLines(
				ContributorLessonView.readSummaryForContributor(database, lessonId));
	}
}
