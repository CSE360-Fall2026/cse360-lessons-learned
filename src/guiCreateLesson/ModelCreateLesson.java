package guiCreateLesson;

import java.sql.SQLException;

import entityClasses.LessonsLearned;
import guiTools.LessonResult;

/*******
 * <p> Title: ModelCreateLesson Class. </p>
 *
 * <p> Description: The Create Lesson Page Model.  It performs the data work behind creating a
 * lesson learned (US-C1) and refusing invalid core information with a specific message
 * (US-C1-V), using the LessonsLearned entity class for both validation and saving.</p>
 *
 * <p> Order of work, matching the validation stages in the team's InputValidation.pdf:
 * <br> 1. Field validation, as typed, in screen order (title, description, tags, category).
 * <br> 2. Normalization: outer spaces are trimmed from the free-text fields.
 * <br> 3. Validation again after trimming, because trimming can make a padded description
 * shorter than its minimum, and such a value must never be saved.
 * <br> 4. A single insert saves the whole lesson, or nothing is saved.</p>
 *
 * <p> Because nothing is written until every field has passed, a valid title can never be
 * saved on its own when the category beside it was invalid (Test Case "Create Lesson - Invalid
 * Input Blocks the Save").</p>
 *
 * <p> The methods are public so the automated tests can call them without starting JavaFX.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 * @author Virgil Jones
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (create from HW2's ModelRole1Home)
 * @version 1.01		2026-11-16 TP3 Aspect 7: support rootCause, recommendation, and aiAssisted
 */

public class ModelCreateLesson {

	/*****
	 * <p> Method: ModelCreateLesson() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelCreateLesson() {

	}
	
	/**********
	 * <p> Method: LessonResult createLesson(String requestingUser, String title,
	 * 		String description, String tags, String category) </p>
	 *
	 * <p> Description: CREATE (US-C1, US-C1-V).  Delegates to the full overload with empty
	 * rootCause/recommendation and aiAssisted = false so existing callers and tests stay
	 * compatible. </p>
	 *
	 * @param requestingUser	the username of the Contributor creating the lesson
	 * @param title				the title, exactly as typed
	 * @param description		the description, exactly as typed
	 * @param tags				comma-separated tags, exactly as typed (may be empty)
	 * @param category			one of LessonRecognizer.VALID_CATEGORIES
	 * @return					on success the new lesson, its id, and "Lesson created"; otherwise
	 * 							the specific error message and the field that caused it
	 */
	public static LessonResult createLesson(String requestingUser, String title,
			String description, String tags, String category) {
		return createLesson(requestingUser, title, description, tags, category, "", "", false);
	}


	/**********
	 * <p> Method: LessonResult createLesson(String requestingUser, String title,
	 * 		String description, String tags, String category, String rootCause,
	 * 		String recommendation, boolean aiAssisted) </p>
	 *
	 * <p> Description: CREATE (US-C1, US-C1-V, TP3 Aspect 7 / Epic E1).  Validates core and AI
	 * coaching fields as typed, trims free text, validates again, and saves the lesson with the
	 * aiAssisted flag. </p>
	 *
	 * @param requestingUser	the username of the Contributor creating the lesson
	 * @param title				the title, exactly as typed
	 * @param description		the description, exactly as typed
	 * @param tags				comma-separated tags, exactly as typed (may be empty)
	 * @param category			one of LessonRecognizer.VALID_CATEGORIES
	 * @param rootCause			optional root cause text (may be empty)
	 * @param recommendation	optional recommendation text (may be empty)
	 * @param aiAssisted		true if the Contributor applied an AI coaching suggestion
	 * @return					on success the new lesson, its id, and "Lesson created"; otherwise
	 * 							the specific error message and the field that caused it
	 */
	public static LessonResult createLesson(String requestingUser, String title,
			String description, String tags, String category, String rootCause,
			String recommendation, boolean aiAssisted) {

		// Stage 1 (US-C1-V): validate every field as typed, so lengths are measured as typed
		LessonsLearned lesson = new LessonsLearned(requestingUser, title, description, tags,
				category);
		lesson.setRootCause(rootCause == null ? "" : rootCause);
		lesson.setRecommendation(recommendation == null ? "" : recommendation);
		lesson.setAiAssisted(aiAssisted);

		String field = lesson.findInvalidField();
		if (!field.isEmpty())
			return LessonResult.failed(lesson.validate(), field);

		// Stages 2 and 3: trim the free text, as InputValidation.pdf specifies, then validate the
		// values that will actually be stored
		lesson.trimTextFields();
		field = lesson.findInvalidField();
		if (!field.isEmpty())
			return LessonResult.failed(lesson.validate(), field);

		// Stage 4 (US-C1): one insert saves the whole lesson
		try {
			int newId = lesson.create();
			return LessonResult.passed("Lesson created", newId, lesson);
		} catch (SQLException e) {
			// ControllerFirstAdmin prints the error and calls System.exit(0) when register()
			// fails.  One failed lesson save should not close the whole application, so a normal
			// failure is returned and the Contributor can try again.
			System.err.println("*** ERROR *** Could not save a lesson: " + e.getMessage());
			return LessonResult.failed("Could not save the lesson due to a database error.", "");
		}
	}
}
