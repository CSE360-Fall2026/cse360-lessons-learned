package guiUpdateLesson;

import entityClasses.LessonsLearned;
import guiTools.LessonAccess;
import guiTools.LessonResult;

/*******
 * <p> Title: ModelUpdateLesson Class. </p>
 *
 * <p> Description: The Update Lesson Page Model.  It performs the data work behind changing the
 * core information of a lesson the Contributor has added, as long as the field being changed is
 * not locked (US-U1), and refusing locked or invalid changes with a clear message while leaving
 * the lesson unchanged (US-U1-V).</p>
 *
 * <p> The View disables locked fields, but this Model does not rely on that: it compares every
 * submitted value with the stored one and refuses any change to a locked field.  A disabled
 * widget is a convenience for the user, not a security check.</p>
 *
 * <p> The methods are public so the automated tests can call them without starting JavaFX.</p>
 *
 * <p> Tested by lessonTests.UpdateLessonTests and ContributorAiToolsTests (JUnit); the refusal
 * of a change to a field a Curator locked also by curatorLockTests.CuratorLockTests and
 * curatorRefinementTests.CuratorRefinementTests RF-14; on screen by the TP2 semi-automated
 * tests M4 and M5 in lessonTests.ManualGuiTests. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 * @author Virgil Jones
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (update from HW2's ModelRole1Home)
 * @version 1.01		2026-11-16 TP3 Aspect 7: support rootCause, recommendation, and aiAssisted
 */

public class ModelUpdateLesson {

	/*****
	 * <p> Method: ModelUpdateLesson() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelUpdateLesson() {

	}


	/**********
	 * <p> Method: LessonResult loadLessonForEdit(String requestingUser, int lessonId) </p>
	 *
	 * <p> Description: READ.  Loads the lesson the edit form will be filled with, applying the
	 * same ownership rule as every other Contributor operation (US-R3-V, US-R4). </p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson to edit
	 * @return					on success the lesson; otherwise "Lesson not found."
	 */
	public static LessonResult loadLessonForEdit(String requestingUser, int lessonId) {
		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V
		return LessonResult.passed("", lessonId, lesson);
	}
	
	/**********
	 * <p> Method: LessonResult updateLesson(String requestingUser, int lessonId, String title,
	 * 		String description, String tags, String category) </p>
	 *
	 * <p> Description: UPDATE (US-U1, US-U1-V).  Delegates to the full overload keeping the
	 * lesson's stored rootCause, recommendation, and aiAssisted flag unchanged. </p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson to change
	 * @param title				the new title, exactly as typed
	 * @param description		the new description, exactly as typed
	 * @param tags				the new tags, exactly as typed
	 * @param category			the new category
	 * @return					on success "Lesson updated"; otherwise the specific message and the
	 * 							field that caused it
	 */
	public static LessonResult updateLesson(String requestingUser, int lessonId, String title,
			String description, String tags, String category) {
		LessonsLearned existing = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		String existingRootCause = (existing == null) ? "" : existing.getRootCause();
		String existingRecommendation = (existing == null) ? "" : existing.getRecommendation();
		boolean existingAiAssisted = (existing != null) && existing.isAiAssisted();
		return updateLesson(requestingUser, lessonId, title, description, tags, category,
				existingRootCause, existingRecommendation, existingAiAssisted);
	}


	/**********
	 * <p> Method: LessonResult updateLesson(String requestingUser, int lessonId, String title,
	 * 		String description, String tags, String category, String rootCause,
	 * 		String recommendation, boolean aiAssisted) </p>
	 *
	 * <p> Description: UPDATE (US-U1, US-U1-V, TP3 Aspect 7 / Epic E1).  Checks ownership and
	 * Curator locks, validates unlocked core fields plus rootCause and recommendation as typed
	 * and after trimming, and persists everything in one update. </p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson to change
	 * @param title				the new title, exactly as typed
	 * @param description		the new description, exactly as typed
	 * @param tags				the new tags, exactly as typed
	 * @param category			the new category
	 * @param rootCause			the new root cause, exactly as typed
	 * @param recommendation	the new recommendation, exactly as typed
	 * @param aiAssisted		true if AI coaching was applied during this edit (or earlier)
	 * @return					on success "Lesson updated"; otherwise the specific message and the
	 * 							field that caused it
	 */
	public static LessonResult updateLesson(String requestingUser, int lessonId, String title,
			String description, String tags, String category, String rootCause,
			String recommendation, boolean aiAssisted) {

		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V

		String newTitle = (title == null) ? "" : title.trim();
		String newDescription = (description == null) ? "" : description.trim();
		String newTags = (tags == null) ? "" : tags.trim();
		String newCategory = (category == null) ? "" : category;
		String newRootCause = (rootCause == null) ? "" : rootCause.trim();
		String newRecommendation = (recommendation == null) ? "" : recommendation.trim();

		// Step 2 (US-U1-V): a locked field may not be changed; checked before validation
		if (lesson.isFieldLocked(LessonsLearned.FIELD_TITLE)
				&& !newTitle.equals(lesson.getTitle()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED, LessonsLearned.FIELD_TITLE);
		if (lesson.isFieldLocked(LessonsLearned.FIELD_DESCRIPTION)
				&& !newDescription.equals(lesson.getDescription()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED,
					LessonsLearned.FIELD_DESCRIPTION);
		if (lesson.isFieldLocked(LessonsLearned.FIELD_TAGS)
				&& !newTags.equals(lesson.getTags()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED, LessonsLearned.FIELD_TAGS);
		if (lesson.isFieldLocked(LessonsLearned.FIELD_CATEGORY)
				&& !newCategory.equals(lesson.getCategory()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED, LessonsLearned.FIELD_CATEGORY);
		if (lesson.isLockedEntire() && !newRootCause.equals(lesson.getRootCause()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED, LessonsLearned.FIELD_ROOT_CAUSE);
		if (lesson.isLockedEntire() && !newRecommendation.equals(lesson.getRecommendation()))
			return LessonResult.failed(LessonAccess.FIELD_LOCKED,
					LessonsLearned.FIELD_RECOMMENDATION);

		// Step 3a (US-U1-V): validate each unlocked field as typed
		String[][] submitted = {
				{ LessonsLearned.FIELD_TITLE, title },
				{ LessonsLearned.FIELD_DESCRIPTION, description },
				{ LessonsLearned.FIELD_TAGS, tags },
				{ LessonsLearned.FIELD_CATEGORY, category },
				{ LessonsLearned.FIELD_ROOT_CAUSE, rootCause == null ? "" : rootCause },
				{ LessonsLearned.FIELD_RECOMMENDATION, recommendation == null ? "" : recommendation }
		};
		for (String[] pair : submitted) {
			if (lesson.isFieldLocked(pair[0])) continue;
			String error = LessonsLearned.validateField(pair[0], pair[1]);
			if (!error.isEmpty()) return LessonResult.failed(error, pair[0]);
		}

		// Step 3b: apply the trimmed values and validate the lesson that will actually be stored
		lesson.setTitle(newTitle);
		lesson.setDescription(newDescription);
		lesson.setTags(newTags);
		lesson.setCategory(newCategory);
		lesson.setRootCause(newRootCause);
		lesson.setRecommendation(newRecommendation);
		lesson.setAiAssisted(lesson.isAiAssisted() || aiAssisted);
		String field = lesson.findInvalidField();
		if (!field.isEmpty()) return LessonResult.failed(lesson.validate(), field);

		// Step 4 (US-U1): one statement saves every field
		lesson.setUpdatedAt(System.currentTimeMillis());
		if (!lesson.update()) return LessonResult.failed(LessonAccess.NOT_FOUND, "");
		return LessonResult.passed("Lesson updated", lessonId, lesson);
	}
}
