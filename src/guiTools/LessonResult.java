package guiTools;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;

/*******
 * <p> Title: LessonResult Class </p>
 *
 * <p> Description: The outcome of one Contributor operation.  Every Model method in the
 * Contributor GUI packages (guiContributorHome, guiCreateLesson, guiLessonDetails,
 * guiUpdateLesson, and guiAddExperience) returns one of these instead of a bare true or false,
 * because the Contributor User Stories require a specific, helpful message for each failure
 * (US-C1-V, US-R2-V, US-R3-V, US-U1-V, US-U2-V, US-D1-V) and a boolean cannot carry one.  A
 * result bundles four things:</p>
 * <ul>
 * <li> whether the operation succeeded,</li>
 * <li> the message to show the user (a confirmation or a specific error),</li>
 * <li> the name of the field that caused a failure, so a View can show the message directly
 * 		beneath that field, as the team's InputValidation.pdf requires, and</li>
 * <li> any data the caller asked for: one lesson, a list of lessons, or the id involved.</li>
 * </ul>
 *
 * <p> The class lives in guiTools, beside GUISingleRoleDispatch, because it is shared by all
 * five Contributor packages rather than belonging to any one of them.  The constructor is
 * private: callers use passed(), passedList(), or failed(), so the meaning of a result is always
 * obvious where it is created.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-09-29 Initial version for HW2 (in package guiRole1)
 * @version 2.00		2026-10-14 TP2: moved to guiTools for the five Contributor packages
 */

public class LessonResult {

	/** True if the operation succeeded.  Rationale: the first thing every caller checks. */
	private boolean success;

	/** The text to show the user: a confirmation such as "Lesson created", or one of the exact
	 * error messages the tests compare against.  An empty message on a failure has a special
	 * meaning: the user backed out of a deletion, which is not an error (US-D1-V). */
	private String message = "";

	/** The field that caused a failure ("title", "description", and so on), or an empty String
	 * if the failure is not about one field (for example "Lesson not found.").  Source:
	 * InputValidation.pdf, "show the message beneath the related field." */
	private String failedField = "";

	/** The id of the lesson the operation involved, or -1 if there is none. */
	private int lessonId = -1;

	/** The lesson the caller asked for, or null. */
	private LessonsLearned lesson = null;

	/** The list of lessons the caller asked for (search results), or null. */
	private LessonsLearnedList list = null;


	/*****
	 * <p> Method: LessonResult() </p>
	 *
	 * <p> Description: Private; use passed(), passedList(), or failed(). </p>
	 */
	private LessonResult() {

	}


	/*****
	 * <p> Method: LessonResult passed(String message, int lessonId, LessonsLearned lesson) </p>
	 *
	 * <p> Description: A successful result about one lesson. </p>
	 *
	 * @param message the confirmation text for the user (for example "Lesson created")
	 * @param lessonId the id of the lesson involved
	 * @param lesson the lesson itself, or null if there is none to hand back
	 * @return a result whose isSuccess() is true
	 */
	public static LessonResult passed(String message, int lessonId, LessonsLearned lesson) {
		LessonResult r = new LessonResult();
		r.success = true;
		r.message = message;
		r.lessonId = lessonId;
		r.lesson = lesson;
		return r;
	}


	/*****
	 * <p> Method: LessonResult passedList(String message, LessonsLearnedList list) </p>
	 *
	 * <p> Description: A successful result that carries a list of lessons (used by search). </p>
	 *
	 * @param message the text for the user
	 * @param list the list to hand back
	 * @return a result whose isSuccess() is true
	 */
	public static LessonResult passedList(String message, LessonsLearnedList list) {
		LessonResult r = new LessonResult();
		r.success = true;
		r.message = message;
		r.list = list;
		return r;
	}


	/*****
	 * <p> Method: LessonResult failed(String message, String failedField) </p>
	 *
	 * <p> Description: A failed result.  The message should be one of the exact messages the
	 * tests expect.  An empty message is used deliberately for a declined delete confirmation,
	 * since backing out is not an error. </p>
	 *
	 * @param message the specific error message
	 * @param failedField "title", "description", "tags", "category", "searchText",
	 * 		"experienceValue", "experienceDescription", "memberName", "memberHours", or
	 * 		"memberEfforts"; an empty String if the failure is not about one field
	 * @return a result whose isSuccess() is false
	 */
	public static LessonResult failed(String message, String failedField) {
		LessonResult r = new LessonResult();
		r.success = false;
		r.message = message;
		r.failedField = failedField;
		return r;
	}


	/** This getter returns the <code>success</code> attribute.
	 * @return true if the operation succeeded */
	public boolean isSuccess() { return success; }
	/** This getter returns the <code>message</code> attribute.
	 * @return the message for the user (may be empty) */
	public String getMessage() { return message; }
	/** This getter returns the <code>failedField</code> attribute.
	 * @return the field that caused a failure, or an empty String */
	public String getFailedField() { return failedField; }
	/** This getter returns the <code>lessonId</code> attribute.
	 * @return the id of the lesson involved, or -1 */
	public int getLessonId() { return lessonId; }
	/** This getter returns the <code>lesson</code> attribute.
	 * @return the lesson, or null */
	public LessonsLearned getLesson() { return lesson; }
	/** This getter returns the <code>list</code> attribute.
	 * @return the list of lessons, or null */
	public LessonsLearnedList getList() { return list; }
}
