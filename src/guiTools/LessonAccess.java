package guiTools;

import entityClasses.LessonsLearned;

/*******
 * <p> Title: LessonAccess Class </p>
 *
 * <p> Description: The single authorization rule shared by every Contributor page: a
 * Contributor may open, change, add experience to, or delete a lesson only if the lesson exists
 * AND that Contributor created it.  It also holds the messages for failures that are not about
 * one input field, so every page reports them with exactly the same words.</p>
 *
 * <p> <b>Why one shared class.</b>  The rule is needed by four of the five Contributor Models
 * (guiLessonDetails, guiUpdateLesson, guiAddExperience, and the delete action), and US-R3-V says
 * "this same check applies before any update, experience entry, or delete action on a selected
 * lesson."  Writing the check once means it cannot be forgotten, or written slightly
 * differently, in one of them.</p>
 *
 * <p> <b>Why "not found" for someone else's lesson.</b>  A missing lesson and another
 * Contributor's lesson produce the same result on purpose.  If they gave different messages, a
 * Contributor trying ids could learn which ids other people are using, which would break the
 * privacy promise of US-R4 (Test Case "Cannot View Another Contributor's Lesson").</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (from HW2's ModelRole1Home)
 */

public class LessonAccess {

	/** Shown when a lesson does not exist or belongs to another Contributor (US-R3-V, US-R4). */
	public static final String NOT_FOUND = "Lesson not found.";

	/** Shown when a Contributor tries to change a locked field (US-U1-V). */
	public static final String FIELD_LOCKED = "This field is locked and cannot be edited.";

	/** Shown when a Contributor tries to delete a lesson that is locked or has a locked field
	 * (US-D1-V). */
	public static final String LESSON_LOCKED = "This lesson is locked and cannot be deleted.";


	/*****
	 * <p> Method: LessonAccess() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every member of
	 * this class is static. </p>
	 */
	public LessonAccess() {

	}


	/*****
	 * <p> Method: LessonsLearned findOwnedLesson(String requestingUser, int lessonId) </p>
	 *
	 * <p> Description: Returns the lesson only if it exists AND belongs to requestingUser;
	 * otherwise returns null, which every caller reports as NOT_FOUND.  This is step 1 of every
	 * Contributor operation on a single lesson (US-R3-V, US-R4). </p>
	 *
	 * @param requestingUser the username of the logged-in Contributor
	 *
	 * @param lessonId the lesson being asked for
	 *
	 * @return the lesson, or null if it is missing or belongs to someone else
	 */
	public static LessonsLearned findOwnedLesson(String requestingUser, int lessonId) {
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null || requestingUser == null
				|| !lesson.getOwnerUserName().equals(requestingUser))
			return null;															// US-R4
		return lesson;
	}
}
