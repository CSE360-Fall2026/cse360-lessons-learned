package guiWritingHelp;

/*******
 * <p> Title: DraftTarget Interface. </p>
 *
 * <p> Description: The lesson page that opened the Writing Help window: the Create Lesson page
 * or the Edit Lesson page.  The window reads the draft and adds chosen text to it only through
 * these methods, so the page's own rules still apply to everything added: the page re-checks the
 * field with its live validation, marks the draft as AI-assisted, and refuses a field a Curator
 * has locked (US-P-E1-02).  Nothing is saved until the Contributor saves the lesson on that
 * page (US-P-E1-01). </p>
 *
 * <p> Tested by lessonTests.WritingHelpTests WH-07 and WH-08 (JUnit: takeaways added to the
 * draft, a locked field refused), and on screen by M-WH3 and M-WH4 (Manual Tests.pdf). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-17 Initial version for TP3 Aspect 7 (US-P-E1-01, US-P-E1-02)
 */
public interface DraftTarget {

	/**********
	 * The current text of one field of the draft.
	 *
	 * @param field	LessonsLearned.FIELD_DESCRIPTION or LessonsLearned.FIELD_RECOMMENDATION
	 * @return the field's text, never null
	 */
	String getText(String field);

	/**********
	 * Can the Contributor change this field?  On the Edit Lesson page a Curator may have locked
	 * it (Epic E6); on the Create Lesson page nothing is locked.
	 *
	 * @param field	LessonsLearned.FIELD_DESCRIPTION or LessonsLearned.FIELD_RECOMMENDATION
	 * @return true if the field is locked
	 */
	boolean isLocked(String field);

	/**********
	 * Puts AI-assisted text into one field of the draft: the page sets the text, re-checks the
	 * field so any error is shown at once, and marks the draft as AI-assisted.
	 *
	 * @param field	LessonsLearned.FIELD_DESCRIPTION or LessonsLearned.FIELD_RECOMMENDATION
	 * @param text	the field's new text
	 */
	void setTextFromAi(String field, String text);
}
