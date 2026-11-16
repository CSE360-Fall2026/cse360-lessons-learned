package guiLessonDetails;

import entityClasses.LessonsLearned;
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
}
