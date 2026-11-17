package guiWritingHelp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import database.BrainstormService;
import database.Database;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiTools.LessonAccess;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelWritingHelp Class. </p>
 *
 * <p> Description: The Model of the Writing Help window (TP3 Aspect 7, Epic E1), opened from
 * the Create Lesson and Edit Lesson pages.  It holds everything the window does that is not
 * drawing widgets, so all of it can be tested with JUnit and no screen:</p>
 * <ul>
 * <li> <b>Brainstorming</b> (US-P-E1-01): the AI asks one follow-up question at a time, the
 * Contributor answers, and on request the AI lists the lessons learned in the conversation.
 * Every request goes through SharedAiService, so the shared consent notice and the same failure
 * message apply (US-AI-D2-01, US-AI-D2-02).  A failed request leaves the conversation as it
 * was, so the Contributor can retry or keep writing.</li>
 * <li> <b>Adding takeaways to the draft</b> (US-P-E1-02): the takeaways the Contributor selects
 * are added to the end of the Description or the Recommendation through the page's DraftTarget,
 * never to a locked field.  The Contributor can still edit them before saving.</li>
 * <li> <b>Finding examples</b> (US-P-E1-03): a keyword search of the Contributor's OWN lessons
 * only, and opening an example checks again that the lesson is theirs.  No AI is used.</li>
 * </ul>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-17 Initial version for TP3 Aspect 7 (US-P-E1-01, -02, -03)
 */
public class ModelWritingHelp {

	/** The first question, asked by the window itself, so the conversation can start without
	 * sending anything. */
	public static final String OPENING_QUESTION = "What happened?  Describe the experience "
			+ "this lesson comes from: what you were trying to do, what went wrong or right, and "
			+ "what you noticed.";
	/** Shown when Send is pressed with nothing typed. */
	public static final String EMPTY_ANSWER = "Type an answer before sending.";
	/** Shown when an answer is longer than ANSWER_MAX. */
	public static final String ANSWER_TOO_LONG = "An answer can have at most 2000 characters.";
	/** Shown when takeaways are requested before any answer. */
	public static final String NO_CONVERSATION = "Answer at least one question before asking "
			+ "for takeaways.";
	/** Shown when Add is pressed with no takeaway selected. */
	public static final String NOTHING_SELECTED = "Select at least one takeaway to add.";
	/** Shown after takeaways are added; %s is the field name. */
	public static final String ADDED = "Added to the %s.  You can edit it on the lesson page "
			+ "before saving.";
	/** Shown after a successful takeaway request. */
	public static final String CHOOSE_TAKEAWAYS = "Select the takeaways you want, then add them "
			+ "to the Description or the Recommendation.";
	/** Shown when an example search finds nothing. */
	public static final String NO_EXAMPLES = "None of your lessons match those words.  Try "
			+ "other words, or keep writing.";

	/** The longest answer the Contributor may send. */
	public static final int ANSWER_MAX = 2000;
	/** The longest AI reply that will be shown. */
	public static final int REPLY_MAX = 2000;
	/** The longest single takeaway that will be shown. */
	public static final int TAKEAWAY_MAX = 500;

	/*-*******************************************************************************************

	The result of an action

	 */

	/**********
	 * <p> Title: Result Class. </p>
	 *
	 * <p> Description: The outcome of one action: whether it worked, the message to show, and,
	 * for a takeaway request, the takeaways. </p>
	 */
	public static final class Result {
		/** True if the action worked. */
		private final boolean success;
		/** The message to show. */
		private final String message;
		/** The takeaways of a takeaway request. */
		private final List<String> items;

		/**
		 * Creates a result.
		 *
		 * @param success	true if the action worked
		 * @param message	the message to show
		 * @param items		the takeaways, or null
		 */
		private Result(boolean success, String message, List<String> items) {
			this.success = success;
			this.message = (message == null) ? "" : message;
			this.items = (items == null) ? Collections.<String>emptyList() : items;
		}

		/**
		 * Did the action work?
		 *
		 * @return true if the action worked
		 */
		public boolean isSuccess() { return success; }
		/**
		 * The message to show.
		 *
		 * @return the message; empty if there is nothing to say
		 */
		public String getMessage() { return message; }
		/**
		 * The takeaways of a takeaway request.
		 *
		 * @return the takeaways; otherwise an empty list
		 */
		public List<String> getItems() { return items; }
	}

	/*-*******************************************************************************************

	Attributes and constructor

	 */

	/** The database the example search reads. */
	private final Database database;
	/** The shared AI service: consent, the provider, and cleaning of replies. */
	private final SharedAiService ai;
	/** The Contributor using the window. */
	private final String userName;
	/** The brainstorming conversation, oldest first; it starts with OPENING_QUESTION. */
	private final List<BrainstormService.ChatMessage> history =
			new ArrayList<BrainstormService.ChatMessage>();

	/**********
	 * Creates the Model for one opening of the window.
	 *
	 * @param database	the connected database
	 * @param ai		the shared AI service
	 * @param userName	the logged-in Contributor
	 */
	public ModelWritingHelp(Database database, SharedAiService ai, String userName) {
		this.database = database;
		this.ai = ai;
		this.userName = userName;
		history.add(new BrainstormService.ChatMessage("model", OPENING_QUESTION));
	}

	/*-*******************************************************************************************

	Brainstorming (US-P-E1-01)

	 */

	/**********
	 * <p> Method: Result send(String answer) </p>
	 *
	 * <p> Description: Sends the Contributor's answer and adds the AI's next question to the
	 * conversation.  If the request is refused or fails, the conversation is left exactly as it
	 * was and the message says why, so the Contributor can retry or continue without AI
	 * (US-AI-D2-02).  The caller must already have asked for consent. </p>
	 *
	 * @param answer	what the Contributor typed
	 * @return success, or the reason nothing was added
	 */
	public Result send(String answer) {
		String text = (answer == null) ? "" : answer.trim();
		if (text.isEmpty()) return new Result(false, EMPTY_ANSWER, null);
		if (text.length() > ANSWER_MAX) return new Result(false, ANSWER_TOO_LONG, null);

		history.add(new BrainstormService.ChatMessage("user", text));
		try {
			String reply = ai.requestBrainstormReply(userName,
					new ArrayList<BrainstormService.ChatMessage>(history), REPLY_MAX);
			history.add(new BrainstormService.ChatMessage("model", reply));
			return new Result(true, "", null);
		} catch (RuntimeException e) {
			history.remove(history.size() - 1);
			return new Result(false, e.getMessage(), null);
		}
	}

	/**********
	 * <p> Method: Result takeaways() </p>
	 *
	 * <p> Description: Asks the AI for the lessons learned in the conversation, for the
	 * Contributor to choose from.  At least one answer is needed first. </p>
	 *
	 * @return the takeaways, or the reason there are none
	 */
	public Result takeaways() {
		if (answerCount() == 0) return new Result(false, NO_CONVERSATION, null);
		try {
			return new Result(true, CHOOSE_TAKEAWAYS, ai.requestTakeaways(userName,
					new ArrayList<BrainstormService.ChatMessage>(history), TAKEAWAY_MAX));
		} catch (RuntimeException e) {
			return new Result(false, e.getMessage(), null);
		}
	}

	/**********
	 * <p> Method: int answerCount() </p>
	 *
	 * @return how many answers the Contributor has sent
	 */
	public int answerCount() {
		int n = 0;
		for (BrainstormService.ChatMessage m : history) if ("user".equals(m.role)) n++;
		return n;
	}

	/**********
	 * <p> Method: String transcript() </p>
	 *
	 * <p> Description: The conversation as text, each turn labeled "AI:" or "You:" and separated
	 * by a blank line. </p>
	 *
	 * @return the transcript
	 */
	public String transcript() {
		StringBuilder sb = new StringBuilder();
		for (BrainstormService.ChatMessage m : history) {
			if (sb.length() > 0) sb.append("\n\n");
			sb.append("user".equals(m.role) ? "You: " : "AI: ").append(m.text);
		}
		return sb.toString();
	}

	/*-*******************************************************************************************

	Adding takeaways to the draft (US-P-E1-02)

	 */

	/**********
	 * <p> Method: Result addToDraft(DraftTarget target, String field, List&lt;String&gt; chosen)
	 * </p>
	 *
	 * <p> Description: Adds the chosen takeaways to the end of one field of the draft (see
	 * appendItems for the format of each field).  A locked field
	 * is refused with the lock message and left unchanged.  The page re-checks the field, so a
	 * field that becomes too long shows its own error while the draft is kept (US-P-E1-02 V
	 * sentence). </p>
	 *
	 * @param target	the lesson page
	 * @param field		LessonsLearned.FIELD_DESCRIPTION or LessonsLearned.FIELD_RECOMMENDATION
	 * @param chosen	the takeaways the Contributor selected
	 * @return success with the ADDED message, or the reason nothing was added
	 */
	public static Result addToDraft(DraftTarget target, String field, List<String> chosen) {
		if (chosen == null || chosen.isEmpty()) return new Result(false, NOTHING_SELECTED, null);
		if (target.isLocked(field)) return new Result(false, LessonAccess.FIELD_LOCKED, null);
		boolean multiLine = LessonsLearned.FIELD_DESCRIPTION.equals(field);
		target.setTextFromAi(field, appendItems(target.getText(field), chosen, multiLine));
		String name = LessonsLearned.FIELD_DESCRIPTION.equals(field) ? "Description"
				: "Recommendation";
		return new Result(true, String.format(ADDED, name), null);
	}

	/**********
	 * <p> Method: String appendItems(String current, List&lt;String&gt; items, boolean
	 * multiLine) </p>
	 *
	 * <p> Description: The field's text with the items added at the end.  In a multi-line field
	 * (the Description) each item is on its own line starting with "- ", after a blank line if
	 * the field already has text.  In a one-line field (the Recommendation, a text field that
	 * cannot hold line breaks) the items are added as sentences separated by spaces. </p>
	 *
	 * @param current	the field's current text
	 * @param items		the items to add
	 * @param multiLine	true for the Description, false for the Recommendation
	 * @return the new text
	 */
	public static String appendItems(String current, List<String> items, boolean multiLine) {
		StringBuilder added = new StringBuilder();
		for (String item : items) {
			if (added.length() > 0) added.append(multiLine ? "\n" : " ");
			added.append(multiLine ? "- " : "").append(item.trim().replaceAll("\\s+", " "));
		}
		String existing = (current == null) ? "" : current.trim();
		if (existing.isEmpty()) return added.toString();
		// A blank line (or, in a one-line field, a space) separates the added items from text
		// that was already there
		return existing + (multiLine ? "\n\n" : " ") + added;
	}

	/*-*******************************************************************************************

	Finding examples (US-P-E1-03)

	 */

	/**********
	 * <p> Method: List&lt;LessonsLearned&gt; searchExamples(String keywords) </p>
	 *
	 * <p> Description: The Contributor's OWN lessons that match the keywords, newest first.
	 * Other Contributors' lessons are never searched (US-P-E1-03). </p>
	 *
	 * @param keywords	one or more words separated by spaces or commas
	 * @return the matching lessons (empty if none match or the words are not valid)
	 */
	public List<LessonsLearned> searchExamples(String keywords) {
		if (!searchError(keywords).isEmpty()) return new ArrayList<LessonsLearned>();
		LessonsLearnedList own = database.getLessonsByOwner(userName);
		return new ArrayList<LessonsLearned>(own.filterByKeywords(keywords).getLessons());
	}

	/**********
	 * <p> Method: String searchError(String keywords) </p>
	 *
	 * <p> Description: Why the keywords cannot be searched, or an empty String if they can:
	 * empty, or failing the search-text rule (US-P-E1-03 V sentence). </p>
	 *
	 * @param keywords	the words typed
	 * @return the message to show, or an empty String
	 */
	public static String searchError(String keywords) {
		if (keywords == null || keywords.trim().isEmpty())
			return "Type one or more words to search your lessons.";
		return LessonRecognizer.checkForValidSearchText(keywords);
	}

	/**********
	 * <p> Method: String exampleText(int lessonId) </p>
	 *
	 * <p> Description: One example lesson to read: its title, category, description, root
	 * cause, and recommendation.  Opening it checks again that the lesson exists and belongs to
	 * this Contributor (US-P-E1-03); otherwise the result is "Lesson not found." </p>
	 *
	 * @param lessonId	the lesson to open
	 * @return the lesson text, or "Lesson not found."
	 */
	public String exampleText(int lessonId) {
		LessonsLearned lesson = database.getLessonById(lessonId);
		if (lesson == null || userName == null || !userName.equals(lesson.getOwnerUserName()))
			return LessonAccess.NOT_FOUND;
		StringBuilder sb = new StringBuilder(lesson.getTitle()).append("\n");
		sb.append("Category: ").append(lesson.getCategory()).append("\n\n");
		sb.append(lesson.getDescription());
		if (!blank(lesson.getRootCause()))
			sb.append("\n\nRoot cause:\n").append(lesson.getRootCause());
		if (!blank(lesson.getRecommendation()))
			sb.append("\n\nRecommendation:\n").append(lesson.getRecommendation());
		return sb.toString();
	}

	/**********
	 * Is the text null or only spaces?
	 *
	 * @param s	the text
	 * @return true if there is nothing to show
	 */
	private static boolean blank(String s) {
		return s == null || s.trim().isEmpty();
	}
}
