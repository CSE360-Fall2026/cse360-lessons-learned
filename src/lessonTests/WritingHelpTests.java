package lessonTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import database.BrainstormService;
import database.Database;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.User;
import guiCuratorSearchTerms.ModelCuratorSearchTerms;
import guiWritingHelp.DraftTarget;
import guiWritingHelp.ModelWritingHelp;

/*******
 * <p> Title: WritingHelpTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for the Writing Help window's Model (guiWritingHelp,
 * TP3 Aspect 7) and for the Curator Search Terms page's connection to the shared AI service
 * (TP3 Aspect 5).  The AI provider is simulated, so the tests need no API key and no network,
 * and every test gets its own in-memory database, so they can run while the application is
 * open.  The window itself is tested by hand in M-WH1 to M-WH5 (Manual Tests.pdf).</p>
 *
 * <p> <b>Requirements covered</b> (Staff User Stories.pdf): US-P-E1-01 (brainstorming with
 * follow-up questions, choosing the final content), US-P-E1-02 (suggestions separate until
 * added, locks respected), US-P-E1-03 (examples only from the Contributor's own lessons),
 * US-AI-D2-01 (nothing sent without this user's consent), US-AI-D2-02 (a failure keeps the
 * work and says so), and US-C-E2-01 (search-term suggestions from the shared service).  Each
 * display name gives the test id, what it checks, its story, and P or N.</p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-17 Initial version for TP3 Aspects 5 and 7
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WritingHelpTests {

	/** The Contributor using the window. */
	private static final String CONTRIBUTOR = "helpContributor";
	/** Another Contributor, whose lessons must never be shown as examples. */
	private static final String OTHER = "helpOtherContributor";

	/** The in-memory database of the current test. */
	private Database database;
	/** The simulated AI provider of the current test. */
	private FakeBackend fake;
	/** The Model under test. */
	private ModelWritingHelp model;
	/** One of the Contributor's own lessons. */
	private int ownLesson;
	/** A lesson owned by the other Contributor, with the same words. */
	private int otherLesson;

	/**********
	 * A simulated AI provider: canned replies, or a failure on request; counts requests.
	 */
	private static class FakeBackend implements SharedAiService.AiBackend {
		/** Creates a provider that answers. */
		FakeBackend() {
		}
		/** True to make every request fail. */
		boolean fail = false;
		/** The reply to brainstorming requests. */
		String reply = "What did you check before deploying?";
		/** The reply to list requests. */
		List<String> list = new ArrayList<String>();
		/** How many requests reached this provider. */
		int requests = 0;

		/**
		 * Counts a request, and fails it if fail is set.
		 *
		 * @throws Exception when fail is set
		 */
		private void call() throws Exception {
			requests++;
			if (fail) throw new RuntimeException("timeout");
		}
		public boolean isConfigured() { return true; }
		public String coach(String t, String d) throws Exception { call(); return reply; }
		public List<String> keywords(String q) throws Exception { call(); return list; }
		public List<String> lessonSearchTerms(String t, String d, String g) throws Exception {
			call(); return list;
		}
		public String chat(List<BrainstormService.ChatMessage> h) throws Exception {
			call(); return reply;
		}
		public List<String> takeaways(List<BrainstormService.ChatMessage> h) throws Exception {
			call(); return list;
		}
		public String refine(String f, String t, String g) throws Exception {
			call(); return reply;
		}
	}

	/**********
	 * A lesson page as the window sees it: two fields, a lock per field, and a record of what
	 * the page was asked to do.
	 */
	private static class FakePage implements DraftTarget {
		/** Creates a page with empty, unlocked fields. */
		FakePage() {
		}
		/** The text of each field. */
		Map<String, String> text = new HashMap<String, String>();
		/** Which fields are locked. */
		Map<String, Boolean> locked = new HashMap<String, Boolean>();
		/** How many times AI-assisted text was put into the draft. */
		int aiUpdates = 0;

		public String getText(String field) { return text.getOrDefault(field, ""); }
		public boolean isLocked(String field) { return locked.getOrDefault(field, false); }
		public void setTextFromAi(String field, String t) { text.put(field, t); aiUpdates++; }
	}

	/**********
	 * Default constructor; JUnit creates one instance for each test.
	 */
	public WritingHelpTests() {
	}

	/**********
	 * Creates the database, one lesson for each Contributor, and a Model for the Contributor,
	 * with no consent given yet.
	 *
	 * @throws Exception if the database cannot be set up
	 */
	@BeforeEach
	public void setUp() throws Exception {
		SharedAiService.clearAllConsent();
		database = new Database("jdbc:h2:mem:tp3_writinghelp_"
				+ UUID.randomUUID().toString().replace("-", ""));
		database.connectToDatabase();
		LessonsLearned own = new LessonsLearned(CONTRIBUTOR, "Rollback after a bad deploy",
				"We rolled back the deploy after the health check failed.", "deploy", "Technical");
		own.setRootCause("The config was not checked.");
		own.setRecommendation("Check the config first.");
		ownLesson = database.createLesson(own);
		otherLesson = database.createLesson(new LessonsLearned(OTHER, "Deploy rollback notes",
				"Another person's deploy rollback lesson.", "deploy", "Technical"));
		fake = new FakeBackend();
		model = new ModelWritingHelp(database, new SharedAiService(fake), CONTRIBUTOR);
	}

	/**********
	 * Closes the database and forgets every consent.
	 */
	@AfterEach
	public void tearDown() {
		if (database != null) database.closeConnection();
		SharedAiService.clearAllConsent();
	}


	/**********
	 * <p> WH-01: The window starts with its own opening question, so nothing has been sent and
	 * there is no answer yet (US-P-E1-01). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("WH-01 Brainstorming starts with a question and sends nothing (US-P-E1-01, P)")
	public void wh01Opening() {
		assertEquals("AI: " + ModelWritingHelp.OPENING_QUESTION, model.transcript());
		assertEquals(0, model.answerCount());
		assertEquals(0, fake.requests);
	}

	/**********
	 * <p> WH-02: An empty answer and an answer over 2000 characters are refused before anything
	 * is sent. </p>
	 */
	@Test
	@Order(2)
	@DisplayName("WH-02 Empty and over-long answers are refused, nothing sent (US-P-E1-01, N)")
	public void wh02BadAnswers() {
		SharedAiService.setUserConsented(CONTRIBUTOR, true);
		assertEquals(ModelWritingHelp.EMPTY_ANSWER, model.send("   ").getMessage());
		assertEquals(ModelWritingHelp.ANSWER_TOO_LONG, model.send("a".repeat(2001)).getMessage());
		assertEquals(0, fake.requests);
		assertTrue(model.send("a".repeat(2000)).isSuccess(), "Exactly 2000 is allowed");
	}

	/**********
	 * <p> WH-03: Without this user's consent nothing is sent and the conversation is unchanged,
	 * even if another user has consented (US-AI-D2-01). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("WH-03 No consent: nothing sent, conversation unchanged (US-AI-D2-01, N)")
	public void wh03NoConsent() {
		SharedAiService.setUserConsented(OTHER, true);
		ModelWritingHelp.Result r = model.send("Our deploy failed.");
		assertFalse(r.isSuccess());
		assertEquals(SharedAiService.CONSENT_DECLINED_MESSAGE, r.getMessage());
		assertEquals(0, fake.requests);
		assertEquals(0, model.answerCount());
	}

	/**********
	 * <p> WH-04: With consent, the answer and the AI's next question are added to the
	 * conversation, each labeled (US-P-E1-01: "answer follow-up questions"). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("WH-04 An answer gets the AI's next question (US-P-E1-01, P)")
	public void wh04Send() {
		SharedAiService.setUserConsented(CONTRIBUTOR, true);
		assertTrue(model.send("  Our deploy failed.  ").isSuccess());
		assertEquals("AI: " + ModelWritingHelp.OPENING_QUESTION + "\n\nYou: Our deploy failed."
				+ "\n\nAI: What did you check before deploying?", model.transcript());
		assertEquals(1, model.answerCount());
	}

	/**********
	 * <p> WH-05: If the AI fails, the message says so and the conversation is left as it was,
	 * so the answer can be sent again (US-AI-D2-02). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("WH-05 A failure keeps the conversation and allows a retry (US-AI-D2-02, N)")
	public void wh05Failure() {
		SharedAiService.setUserConsented(CONTRIBUTOR, true);
		String before = model.transcript();
		fake.fail = true;
		ModelWritingHelp.Result r = model.send("Our deploy failed.");
		assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, r.getMessage());
		assertEquals(before, model.transcript());
		assertEquals(0, model.answerCount());
		fake.fail = false;
		assertTrue(model.send("Our deploy failed.").isSuccess());
	}

	/**********
	 * <p> WH-06: Takeaways need at least one answer; then the AI's takeaways are returned for
	 * the Contributor to choose from (US-P-E1-01: "choose the final content"). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("WH-06 Takeaways need an answer first, then are offered (US-P-E1-01, P/N)")
	public void wh06Takeaways() {
		SharedAiService.setUserConsented(CONTRIBUTOR, true);
		assertEquals(ModelWritingHelp.NO_CONVERSATION, model.takeaways().getMessage());
		assertEquals(0, fake.requests);
		model.send("Our deploy failed.");
		fake.list = Arrays.asList("Check the config first.", "Practice the rollback.");
		ModelWritingHelp.Result r = model.takeaways();
		assertTrue(r.isSuccess());
		assertEquals(ModelWritingHelp.CHOOSE_TAKEAWAYS, r.getMessage());
		assertEquals(Arrays.asList("Check the config first.", "Practice the rollback."),
				r.getItems());
	}

	/**********
	 * <p> WH-07: Chosen takeaways are added after the existing text: in the Description one
	 * per line after a blank line, in the one-line Recommendation as sentences after a space.
	 * The page is asked to set the text, which marks it as AI-assisted (US-P-E1-02). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("WH-07 Chosen takeaways are added after the draft's text (US-P-E1-02, P)")
	public void wh07AddToDraft() {
		FakePage page = new FakePage();
		page.text.put(LessonsLearned.FIELD_DESCRIPTION, "We rolled back.");
		page.text.put(LessonsLearned.FIELD_RECOMMENDATION, "Plan ahead.");
		List<String> chosen = Arrays.asList("Check the config first.", "Practice the rollback.");

		ModelWritingHelp.Result d = ModelWritingHelp.addToDraft(page,
				LessonsLearned.FIELD_DESCRIPTION, chosen);
		assertTrue(d.isSuccess());
		assertEquals(String.format(ModelWritingHelp.ADDED, "Description"), d.getMessage());
		assertEquals("We rolled back.\n\n- Check the config first.\n- Practice the rollback.",
				page.getText(LessonsLearned.FIELD_DESCRIPTION));

		ModelWritingHelp.addToDraft(page, LessonsLearned.FIELD_RECOMMENDATION, chosen);
		assertEquals("Plan ahead. Check the config first. Practice the rollback.",
				page.getText(LessonsLearned.FIELD_RECOMMENDATION));
		assertEquals(2, page.aiUpdates);

		assertEquals("- Only one.", ModelWritingHelp.appendItems("", List.of("Only one."), true));
	}

	/**********
	 * <p> WH-08: A locked field is refused with the lock message and left unchanged, and Add
	 * with nothing chosen is refused (US-P-E1-02 V sentence: a locked field gives "that field's
	 * specific message" and the draft is kept). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("WH-08 A locked field or no choice is refused, draft unchanged (US-P-E1-02, N)")
	public void wh08Refusals() {
		FakePage page = new FakePage();
		page.text.put(LessonsLearned.FIELD_DESCRIPTION, "We rolled back.");
		page.locked.put(LessonsLearned.FIELD_DESCRIPTION, true);
		ModelWritingHelp.Result r = ModelWritingHelp.addToDraft(page,
				LessonsLearned.FIELD_DESCRIPTION, List.of("Check the config first."));
		assertEquals("This field is locked and cannot be edited.", r.getMessage());
		assertEquals("We rolled back.", page.getText(LessonsLearned.FIELD_DESCRIPTION));
		assertEquals(ModelWritingHelp.NOTHING_SELECTED, ModelWritingHelp.addToDraft(page,
				LessonsLearned.FIELD_RECOMMENDATION, new ArrayList<String>()).getMessage());
		assertEquals(0, page.aiUpdates);
	}

	/**********
	 * <p> WH-09: The example search finds only the Contributor's own lessons, even when another
	 * Contributor's lesson has the same words (US-P-E1-03). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("WH-09 Examples come only from the Contributor's own lessons (US-P-E1-03, P)")
	public void wh09OwnExamplesOnly() {
		List<LessonsLearned> found = model.searchExamples("deploy rollback");
		assertEquals(1, found.size());
		assertEquals(ownLesson, found.get(0).getId());
		assertTrue(model.searchExamples("payroll").isEmpty());
		assertEquals(0, fake.requests, "Example search uses no AI");
	}

	/**********
	 * <p> WH-10: An empty search is explained instead of searched (US-P-E1-03 V sentence). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("WH-10 An empty search gets helpful feedback (US-P-E1-03, N)")
	public void wh10EmptySearch() {
		assertEquals("Type one or more words to search your lessons.",
				ModelWritingHelp.searchError("  "));
		assertEquals("", ModelWritingHelp.searchError("deploy"));
		assertTrue(model.searchExamples("").isEmpty());
	}

	/**********
	 * <p> WH-11: Opening an example checks again that it is the Contributor's own: their lesson
	 * shows its title, category, description, root cause, and recommendation; another
	 * Contributor's lesson, or one that does not exist, gives "Lesson not found."
	 * (US-P-E1-03). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("WH-11 Opening an example rechecks that it is the Contributor's own (US-P-E1-03, P/N)")
	public void wh11OpenExample() {
		assertEquals("Rollback after a bad deploy\nCategory: Technical\n\n"
				+ "We rolled back the deploy after the health check failed.\n\n"
				+ "Root cause:\nThe config was not checked.\n\n"
				+ "Recommendation:\nCheck the config first.", model.exampleText(ownLesson));
		assertEquals("Lesson not found.", model.exampleText(otherLesson));
		assertEquals("Lesson not found.", model.exampleText(99999));
	}

	/**********
	 * <p> WH-12: The Curator Search Terms page's Model, given the same provider the page now
	 * uses (the shared AI service), gets a valid suggestion it can show for review; without the
	 * Curator's consent nothing is sent (US-C-E2-01, US-AI-D2-01). </p>
	 *
	 * @throws Exception if the Curator cannot be registered
	 */
	@Test
	@Order(12)
	@DisplayName("WH-12 Search Terms page gets suggestions from the shared service (US-C-E2-01, P/N)")
	public void wh12SearchTermsProvider() throws Exception {
		User curator = new User("helpCurator", "Password123!", "", "", "", "", "",
				false, false, true, false);
		database.register(curator);
		SharedAiService service = new SharedAiService(fake);
		ModelCuratorSearchTerms terms = new ModelCuratorSearchTerms(database,
				(t, d, g) -> service.requestLessonSearchTerms(curator.getUserName(), t, d, g));
		ModelCuratorSearchTerms.Draft draft = terms.openLesson(curator, ownLesson).draft();
		fake.list = Arrays.asList("rollback", "failed release", "health check");

		ModelCuratorSearchTerms.Review refused = terms.requestSuggestion(curator, draft,
				SharedAiService.hasUserConsented(curator.getUserName()));
		assertFalse(refused.success());
		assertEquals(0, fake.requests, "Nothing is sent without consent");

		SharedAiService.setUserConsented(curator.getUserName(), true);
		ModelCuratorSearchTerms.Review review = terms.requestSuggestion(curator, draft,
				SharedAiService.hasUserConsented(curator.getUserName()));
		assertTrue(review.success(), review.message());
		assertEquals("rollback, failed release, health check", review.suggestion());
	}
}
