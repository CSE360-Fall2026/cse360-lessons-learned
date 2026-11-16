package inputValidation;

import java.math.BigDecimal;

/*******
 * <p> Title: LessonRecognizer Class. </p>
 *
 * <p> Description: This class recognizes valid input for every value that can be stored in a
 * lesson learned: the core information (title, description, tags, category), the search text a
 * Contributor uses to find lessons, the experience information added after a lesson is reused
 * (time/effort, description, and the effort of each team member), and the attributes reserved
 * for Phase Three (root cause, recommendation, and curator search terms).</p>
 *
 * <p> The class follows the convention established by UserNameRecognizer: every method returns
 * an empty String when the input is valid and otherwise returns a specific message that can be
 * displayed directly beneath the related field.  The empty-String-means-valid convention keeps
 * the calling code simple, and the specific messages satisfy the "helpful error message" part of
 * User Stories US-C1-V, US-R2-V, US-U1-V, and US-U2-V.  The automated tests compare these
 * messages word for word, so a message must not be changed without updating the tests.</p>
 *
 * <p> Every method checks the inexpensive conditions first (blank, then length) and scans the
 * characters only after that.  An oversized input is therefore rejected immediately without
 * being walked through, which is the size-limit principle in the team's InputValidation.pdf:
 * all textual input is checked against a reasonable size limit before anything else is done
 * with it.</p>
 *
 * <p> The limits for Title (100) and Search text (254) reuse rules already established in
 * InputValidation.pdf.  The remaining limits were selected for this phase because TP1 had no
 * equivalent field; each is documented beside its constant.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-09-29 Initial version, modeled on UserNameRecognizer
 * @version 2.00		2026-10-14 TP2: rules for root cause, recommendation, search terms, and
 * 							per-member effort; plain-number helper for stored hour values
 *
 */
public class LessonRecognizer {

	/*****
	 * <p> Method: LessonRecognizer() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method in
	 * this class is static. </p>
	 */
	public LessonRecognizer() {

	}

	/** The categories a lesson may have.  The Lessons Learned System V1 stories do not define
	 * categories, so this list was adopted from the CATEGORIES list in the LessonShare sample
	 * application, which the team evaluated in HW2.  Matching is exact and case-sensitive. */
	public static final String[] VALID_CATEGORIES = { "General", "Technical", "Teamwork",
			"Project Management", "Communication", "Research", "Career" };

	/** Maximum title length.  Source: the free-text rule in InputValidation.pdf. */
	private static final int TITLE_MAX = 100;

	/** Minimum description length.  A few words cannot explain a lesson to another reader. */
	private static final int DESCRIPTION_MIN = 10;

	/** Maximum description length.  Large enough for several paragraphs. */
	private static final int DESCRIPTION_MAX = 5000;

	/** Maximum length of one tag (the text between two commas). */
	private static final int TAG_MAX = 30;

	/** Maximum length of the whole tags field. */
	private static final int TAGS_TOTAL_MAX = 200;

	/** Maximum search text length.  Source: the Users search rule in InputValidation.pdf. */
	private static final int SEARCH_MAX = 254;

	/** Maximum number of characters in a time/effort value.  This keeps an enormous string of
	 * digits from ever reaching Double.parseDouble. */
	private static final int EXPERIENCE_VALUE_SIZE_MAX = 20;

	/** Maximum time/effort, in hours, for one experience entry. */
	private static final double EXPERIENCE_VALUE_MAX = 10000;

	/** Minimum experience description length. */
	private static final int EXPERIENCE_DESCRIPTION_MIN = 5;

	/** Maximum experience description length. */
	private static final int EXPERIENCE_DESCRIPTION_MAX = 2000;

	/** Maximum length of the optional root cause and recommendation fields (TP2).  These are the
	 * AICoaching fields planned for Phase Three, sized like an experience description. */
	private static final int COACHED_TEXT_MAX = 2000;

	/** Maximum length of the whole curator search terms field (TP2, Epic E2). */
	private static final int SEARCH_TERMS_TOTAL_MAX = 500;

	/** Maximum length of one curator search term.  Longer than a tag, because a search term can
	 * be a short phrase such as "intermittent build failure". */
	private static final int SEARCH_TERM_MAX = 50;

	/** Maximum length of a team member's name on an experience entry (TP2).  Matches the
	 * 100-character limit used for names in TP1. */
	private static final int MEMBER_NAME_MAX = 100;


	/**********
	 * <p> Method: checkForValidTitle(String title) </p>
	 *
	 * <p> Description: This method checks a title for User Stories US-C1-V and US-U1-V.  A title
	 * must not be blank, must be 100 characters or fewer, and must not contain control
	 * characters.  There is no minimum beyond "not blank", which is the team's rule for free-text
	 * fields.  The position in the message is 1-based so that it reads naturally.</p>
	 *
	 * @param title specifies the title to be checked
	 *
	 * @return an empty String if the title is valid; otherwise, an error message
	 *
	 */
	public static String checkForValidTitle(String title) {
		if (title == null || title.trim().isEmpty())
			return "Title is required.";

		if (title.length() > TITLE_MAX)
			return "Title must be " + TITLE_MAX + " characters or fewer.";

		int badIndex = indexOfControlCharacter(title, false);
		if (badIndex >= 0)
			return "Title contains an invalid character at position " + (badIndex + 1) + ".";

		return "";
	}


	/**********
	 * <p> Method: checkForValidDescription(String description) </p>
	 *
	 * <p> Description: This method checks a description.  Unlike the title, it has a real
	 * minimum (10 characters).  A blank description is reported as "required" rather than "too
	 * short" so the message matches what the user actually did.  Line breaks are allowed because
	 * a TextArea produces them and a description may have several paragraphs.</p>
	 *
	 * @param description specifies the description to be checked
	 *
	 * @return an empty String if the description is valid; otherwise, an error message
	 *
	 */
	public static String checkForValidDescription(String description) {
		if (description == null || description.trim().isEmpty())
			return "Description is required.";

		if (description.length() < DESCRIPTION_MIN || description.length() > DESCRIPTION_MAX)
			return "Description must be between " + DESCRIPTION_MIN + " and " + DESCRIPTION_MAX
					+ " characters.";

		int badIndex = indexOfControlCharacter(description, true);
		if (badIndex >= 0)
			return "Description contains an invalid character at position " + (badIndex + 1) + ".";

		return "";
	}


	/**********
	 * <p> Method: checkForValidTags(String tags) </p>
	 *
	 * <p> Description: This method checks the tags field.  Tags are optional, so an empty String
	 * is valid.  The field may be at most 200 characters, each tag may be at most 30 characters,
	 * and control characters are rejected.  Empty pieces between commas (for example
	 * "java,,css") are skipped rather than reported, since an empty piece is not a tag.</p>
	 *
	 * @param tags specifies the comma-separated tags to be checked
	 *
	 * @return an empty String if the tags are valid; otherwise, an error message
	 *
	 */
	public static String checkForValidTags(String tags) {
		if (tags == null || tags.isEmpty())
			return "";

		if (tags.length() > TAGS_TOTAL_MAX)
			return "Tags are too long.";

		if (indexOfControlCharacter(tags, false) >= 0)
			return "Tags contain an invalid character.";

		// Each piece is trimmed so "java, css" is not penalized for the space after the comma
		for (String tag : tags.split(","))
			if (tag.trim().length() > TAG_MAX)
				return "Tags are too long.";

		return "";
	}


	/**********
	 * <p> Method: checkForValidCategory(String category) </p>
	 *
	 * <p> Description: This method checks a category.  It must be an exact, case-sensitive match
	 * for one of VALID_CATEGORIES.  This mirrors how InputValidation.pdf treats roles: accept
	 * only supported values.  No character scan is needed because this is only a lookup in a
	 * fixed list.</p>
	 *
	 * @param category specifies the category to be checked
	 *
	 * @return an empty String if the category is valid; otherwise, an error message
	 *
	 */
	public static String checkForValidCategory(String category) {
		if (category != null)
			for (String validCategory : VALID_CATEGORIES)
				if (validCategory.equals(category))
					return "";

		return "Please choose a valid category.";
	}


	/**********
	 * <p> Method: checkForValidSearchText(String searchText) </p>
	 *
	 * <p> Description: This method checks search text for User Story US-R2-V.  Empty text is
	 * VALID: following the team's Users search rule, an empty search is not a field error, it
	 * simply finds nothing (LessonsLearnedList.filterByTitleContains and filterByKeywords
	 * implement that).  Only text that is too long or contains a control character is
	 * rejected.</p>
	 *
	 * @param searchText specifies the search text to be checked
	 *
	 * @return an empty String if the search text is valid (even if empty); otherwise, an error
	 * message
	 *
	 */
	public static String checkForValidSearchText(String searchText) {
		if (searchText == null)
			return "";

		if (searchText.length() > SEARCH_MAX)
			return "Search text is too long.";

		if (indexOfControlCharacter(searchText, false) >= 0)
			return "Search text contains an invalid character.";

		return "";
	}


	/**********
	 * <p> Method: checkForValidExperienceValue(String timeEffort) </p>
	 *
	 * <p> Description: This method checks a time/effort value in hours for User Story US-U2-V.
	 * The same rule is applied to the total for an experience entry and to each team member's
	 * share.  The text must be a plain decimal number: digits with at most one decimal point,
	 * starting and ending with a digit.  That rules out a minus sign, letters, "5." and ".5".  The
	 * number must then be greater than 0 and no more than 10,000.  Every kind of failure uses the
	 * same message, because to the user all of them mean "type a positive number".</p>
	 *
	 * @param timeEffort specifies the text typed into a time/effort field
	 *
	 * @return an empty String if it is a valid positive number in range; otherwise, an error
	 * message
	 *
	 */
	public static String checkForValidExperienceValue(String timeEffort) {
		final String message = "Time/effort must be a positive number.";

		String text = (timeEffort == null) ? "" : timeEffort.trim();
		if (text.isEmpty() || text.length() > EXPERIENCE_VALUE_SIZE_MAX)
			return message;

		if (!isDigit(text.charAt(0)) || !isDigit(text.charAt(text.length() - 1)))
			return message;

		int decimalPoints = 0;
		for (int ndx = 0; ndx < text.length(); ndx++) {
			char currentCharacter = text.charAt(ndx);
			if (currentCharacter == '.')
				decimalPoints++;
			else if (!isDigit(currentCharacter))
				return message;
		}
		if (decimalPoints > 1)
			return message;

		// The loop above proved this is a plain number, so parsing it is safe
		double value = Double.parseDouble(text);
		if (value <= 0 || value > EXPERIENCE_VALUE_MAX)
			return message;

		return "";
	}


	/**********
	 * <p> Method: checkForValidExperienceValue(double hours) </p>
	 *
	 * <p> Description: TP2.  The entity classes store hours as a double, but the rule above is
	 * defined on the text a user types.  This overload converts the stored number back to plain
	 * decimal text and applies exactly the same rule, so a value is judged identically whether it
	 * comes from the keyboard or from an object.  BigDecimal.toPlainString() is used instead of
	 * String.valueOf() because String.valueOf(0.0001) produces "1.0E-4", which would be rejected
	 * as "not a plain number" even though the value itself is valid.</p>
	 *
	 * @param hours specifies the stored number of hours to be checked
	 *
	 * @return an empty String if the value is valid; otherwise, an error message
	 */
	public static String checkForValidExperienceValue(double hours) {
		// NaN and infinity cannot be expressed as plain text, so they are rejected first
		if (Double.isNaN(hours) || Double.isInfinite(hours))
			return checkForValidExperienceValue("");
		return checkForValidExperienceValue(BigDecimal.valueOf(hours).stripTrailingZeros()
				.toPlainString());
	}


	/**********
	 * <p> Method: checkForValidExperienceDescription(String description) </p>
	 *
	 * <p> Description: This method checks the "what was done and how" text on an experience
	 * entry for User Story US-U2-V.  It must be 5 to 2000 characters, not blank, and free of
	 * control characters (line breaks are allowed).  Every kind of failure uses the one message
	 * defined for this field.</p>
	 *
	 * @param description specifies the experience description to be checked
	 *
	 * @return an empty String if it is valid; otherwise, an error message
	 *
	 */
	public static String checkForValidExperienceDescription(String description) {
		final String message = "Please describe what was done.";

		if (description == null || description.trim().isEmpty())
			return message;

		if (description.length() < EXPERIENCE_DESCRIPTION_MIN
				|| description.length() > EXPERIENCE_DESCRIPTION_MAX)
			return message;

		if (indexOfControlCharacter(description, true) >= 0)
			return message;

		return "";
	}


	/**********
	 * <p> Method: checkForValidMemberName(String memberName) </p>
	 *
	 * <p> Description: TP2.  This method checks the name of a team member listed on an
	 * experience entry.  The Lessons Learned System V1 Contributor story says experience
	 * information includes "the effort used by the various team members", so each share of the
	 * effort is recorded with a name.  The name is required, at most 100 characters, and must not
	 * contain control characters.</p>
	 *
	 * <p> NameInputRecognizer is deliberately not reused here.  It accepts letters, spaces,
	 * hyphens, and apostrophes only, so it would reject names a team commonly writes such as
	 * "Alex M." or "Dev Team 2", and it prints a trace of its state machine on every call.</p>
	 *
	 * @param memberName specifies the team member name to be checked
	 *
	 * @return an empty String if the name is valid; otherwise, an error message
	 */
	public static String checkForValidMemberName(String memberName) {
		if (memberName == null || memberName.trim().isEmpty())
			return "Team member name is required.";

		if (memberName.length() > MEMBER_NAME_MAX)
			return "Team member name must be " + MEMBER_NAME_MAX + " characters or fewer.";

		int badIndex = indexOfControlCharacter(memberName, false);
		if (badIndex >= 0)
			return "Team member name contains an invalid character at position "
					+ (badIndex + 1) + ".";

		return "";
	}


	/**********
	 * <p> Method: checkForValidRootCause(String rootCause) </p>
	 *
	 * <p> Description: TP2.  This method checks the optional root cause ("what went wrong and
	 * why") field, one of the input fields selected in AICoaching.pdf for AI coaching in Phase
	 * Three.  The field is optional, so an empty value is valid.</p>
	 *
	 * @param rootCause specifies the root cause text to be checked
	 *
	 * @return an empty String if it is valid; otherwise, an error message
	 */
	public static String checkForValidRootCause(String rootCause) {
		return checkOptionalCoachedText(rootCause, "Root cause");
	}


	/**********
	 * <p> Method: checkForValidRecommendation(String recommendation) </p>
	 *
	 * <p> Description: TP2.  This method checks the optional recommendation ("what to do
	 * differently next time") field, one of the input fields selected in AICoaching.pdf for AI
	 * coaching in Phase Three.  The field is optional, so an empty value is valid.</p>
	 *
	 * @param recommendation specifies the recommendation text to be checked
	 *
	 * @return an empty String if it is valid; otherwise, an error message
	 */
	public static String checkForValidRecommendation(String recommendation) {
		return checkOptionalCoachedText(recommendation, "Recommendation");
	}


	/**********
	 * <p> Method: checkForValidSearchTerms(String searchTerms) </p>
	 *
	 * <p> Description: TP2.  This method checks the comma-separated search terms a Curator
	 * attaches to a lesson so that viewers can find it even when their words do not appear in
	 * the lesson (Epic E2).  The rule follows the tags rule: optional, a limit on the whole
	 * field, a limit on each term, no control characters, and empty pieces between commas are
	 * skipped.</p>
	 *
	 * @param searchTerms specifies the search terms to be checked
	 *
	 * @return an empty String if they are valid; otherwise, an error message
	 */
	public static String checkForValidSearchTerms(String searchTerms) {
		if (searchTerms == null || searchTerms.isEmpty())
			return "";

		if (searchTerms.length() > SEARCH_TERMS_TOTAL_MAX)
			return "Search terms are too long.";

		if (indexOfControlCharacter(searchTerms, false) >= 0)
			return "Search terms contain an invalid character.";

		for (String term : searchTerms.split(","))
			if (term.trim().length() > SEARCH_TERM_MAX)
				return "Search terms are too long.";

		return "";
	}


	/**********
	 * <p> Method: checkOptionalCoachedText(String text, String fieldLabel) </p>
	 *
	 * <p> Description: TP2.  The shared rule behind the root cause and recommendation fields:
	 * optional, at most 2000 characters, and no control characters other than line breaks.  One
	 * private method keeps the two public rules from drifting apart.</p>
	 *
	 * @param text specifies the text to be checked
	 *
	 * @param fieldLabel specifies the field name used at the start of an error message
	 *
	 * @return an empty String if the text is valid; otherwise, an error message
	 */
	private static String checkOptionalCoachedText(String text, String fieldLabel) {
		if (text == null || text.isEmpty())
			return "";

		if (text.length() > COACHED_TEXT_MAX)
			return fieldLabel + " must be " + COACHED_TEXT_MAX + " characters or fewer.";

		int badIndex = indexOfControlCharacter(text, true);
		if (badIndex >= 0)
			return fieldLabel + " contains an invalid character at position "
					+ (badIndex + 1) + ".";

		return "";
	}


	/**********
	 * <p> Method: indexOfControlCharacter(String text, boolean allowLineBreaks) </p>
	 *
	 * <p> Description: This method finds the first control character (tabs and other
	 * non-printing characters) in a String.  Java counts a newline as a control character, so the
	 * multi-line fields pass allowLineBreaks = true to let newlines through.  Single-line fields
	 * (title, tags, search, names) reject them like any other control character.</p>
	 *
	 * @param text specifies the String to be scanned
	 *
	 * @param allowLineBreaks true if newline characters are acceptable in this field
	 *
	 * @return the 0-based index of the first control character, or -1 if there is none
	 *
	 */
	private static int indexOfControlCharacter(String text, boolean allowLineBreaks) {
		for (int ndx = 0; ndx < text.length(); ndx++) {
			char currentCharacter = text.charAt(ndx);
			boolean isLineBreak = (currentCharacter == '\n' || currentCharacter == '\r');
			if (Character.isISOControl(currentCharacter) && !(allowLineBreaks && isLineBreak))
				return ndx;
		}
		return -1;
	}


	/**********
	 * <p> Method: isDigit(char character) </p>
	 *
	 * <p> Description: This method determines whether a character is 0-9.  Character.isDigit()
	 * is not used because it also accepts digits from other writing systems, which
	 * Double.parseDouble() does not necessarily understand.</p>
	 *
	 * @param character specifies the character to be checked
	 *
	 * @return true if the character is 0-9; otherwise, false
	 *
	 */
	private static boolean isDigit(char character) {
		return character >= '0' && character <= '9';
	}
}
