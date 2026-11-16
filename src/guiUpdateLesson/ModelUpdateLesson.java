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
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (update from HW2's ModelRole1Home)
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
	 * <p> Description: UPDATE.  Implements US-U1 and US-U1-V.  Order of checks:
	 * <br> 1. The lesson exists and belongs to requestingUser (US-R3-V).
	 * <br> 2. No locked field is CHANGED.  Submitting a locked field's stored value unchanged is
	 * allowed, because the form always submits every field even when only one was edited.  The
	 * lock check comes before validation on purpose, so a locked field is refused for being
	 * locked even if the new value was also invalid (Test Case "Update Lesson - Rejected Because
	 * Field Is Locked").
	 * <br> 3. Every unlocked field passes its rule, as typed and again after trimming.
	 * <br> 4. One UPDATE statement saves everything, or nothing is saved.</p>
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

		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V

		String newTitle = (title == null) ? "" : title.trim();
		String newDescription = (description == null) ? "" : description.trim();
		String newTags = (tags == null) ? "" : tags.trim();
		String newCategory = (category == null) ? "" : category;

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

		// Step 3a (US-U1-V): validate each unlocked field as typed.  A locked field was just
		// proven unchanged, so it holds the stored value, which was valid when it was saved.
		String[][] submitted = { { LessonsLearned.FIELD_TITLE, title },
				{ LessonsLearned.FIELD_DESCRIPTION, description },
				{ LessonsLearned.FIELD_TAGS, tags }, { LessonsLearned.FIELD_CATEGORY, category } };
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
		String field = lesson.findInvalidField();
		if (!field.isEmpty()) return LessonResult.failed(lesson.validate(), field);

		// Step 4 (US-U1): one statement saves every field
		lesson.setUpdatedAt(System.currentTimeMillis());
		if (!lesson.update()) return LessonResult.failed(LessonAccess.NOT_FOUND, "");
		return LessonResult.passed("Lesson updated", lessonId, lesson);
	}
}
