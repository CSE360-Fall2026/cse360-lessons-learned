package curatorRefinementTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import database.BrainstormService;
import database.Database;
import database.SharedAiService;
import entityClasses.LessonsLearned;
import entityClasses.RefinementDraft;
import entityClasses.User;
import guiCuratorHome.ModelCuratorHome;
import guiCuratorRefine.ModelCuratorRefine;
import guiCuratorRefine.ModelCuratorRefine.Review;
import guiTools.CurationNote;
import guiTools.LessonResult;

/*******
 * <p> Title: RefinePageTests Class </p>
 *
 * <p> Description: JUnit tests for what the Refine with AI page adds to Daniel Martinez
 * Julio's ModelCuratorRefine (TP3 Aspect 6, Epic E3): the connection to the team's shared AI
 * service and its per-user consent (sharedProvider), the "Edited by curator" note
 * (guiTools.CurationNote), and the note and original text in the Curator Home details.  The
 * Model's own rules are tested by CuratorRefinementTests RF-01 to RF-42.</p>
 *
 * <p> Each test uses a new in-memory database and a simulated AI provider, so no lesson text
 * leaves the computer and the tests can run while the application is open.  P marks a
 * positive test and N a negative one.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-18 Initial version for TP3 Aspect 6
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class RefinePageTests {

	/** The lesson's stored title. */
	private static final String TITLE = "Deployment recovery";
	/** The lesson's stored description. */
	private static final String DESCRIPTION = "The worker failed. We restored its configuration "
			+ "and restarted it.";
	/** A fixed save time: 2023-11-14 22:13:20 UTC. */
	private static final long SAVE_TIME = 1_700_000_000_000L;

	/** The test database. */
	private Database database;
	/** A Curator. */
	private User curator;
	/** A second Curator, who must not inherit the first one's consent. */
	private User otherCurator;
	/** The lesson being refined. */
	private int lessonId;
	/** The simulated AI provider. */
	private FakeBackend fake;
	/** The Model as the page creates it, with the shared-service provider for curator. */
	private ModelCuratorRefine model;

	/**********
	 * Creates the test class; JUnit makes a new instance for each test.
	 */
	public RefinePageTests() {
	}

	/**********
	 * A simulated AI provider: a canned rewrite, or a failure on request; records what it was
	 * asked.
	 */
	private static class FakeBackend implements SharedAiService.AiBackend {
		/** Creates a provider that answers. */
		FakeBackend() {
		}
		/** True to make every request fail. */
		boolean fail = false;
		/** The rewrite returned. */
		String reply = "Restore the worker's configuration, check it, then restart the worker.";
		/** The field, text, and goal of each refinement request, in order. */
		List<String> asked = new ArrayList<String>();
		public boolean isConfigured() { return true; }
		public String coach(String t, String d) { return ""; }
		public List<String> keywords(String q) { return new ArrayList<String>(); }
		public List<String> lessonSearchTerms(String t, String d, String g) {
			return new ArrayList<String>();
		}
		public String chat(List<BrainstormService.ChatMessage> h) { return ""; }
		public List<String> takeaways(List<BrainstormService.ChatMessage> h) {
			return new ArrayList<String>();
		}
		public String refine(String f, String t, String g) throws Exception {
			asked.add(f + "|" + t + "|" + g);
			if (fail) throw new RuntimeException("Groq API error 429: key=SECRET");
			return reply;
		}
	}

	/**********
	 * Creates a new in-memory database with two Curators, a Contributor, and one lesson, and a
	 * Model whose provider is the shared service for the first Curator.
	 *
	 * @throws Exception if the database cannot be set up
	 */
	@BeforeEach
	public void setUp() throws Exception {
		database = new Database("jdbc:h2:mem:tp3_refinepage_"
				+ UUID.randomUUID().toString().replace("-", ""));
		database.connectToDatabase();
		curator = new User("refineCurator", "Password123!", "", "", "", "", "",
				false, false, true, false);
		otherCurator = new User("refineOther", "Password123!", "", "", "", "", "",
				false, false, true, false);
		User contributor = new User("refineOwner", "Password123!", "", "", "", "", "",
				false, true, false, false);
		database.register(curator);
		database.register(otherCurator);
		database.register(contributor);
		lessonId = database.createLesson(new LessonsLearned(contributor.getUserName(), TITLE,
				DESCRIPTION, "deploy", "Technical"));
		assertTrue(lessonId > 0, "The test lesson must be created");
		fake = new FakeBackend();
		SharedAiService.clearAllConsent();
		model = new ModelCuratorRefine(database, ModelCuratorRefine.sharedProvider(
				new SharedAiService(fake), curator.getUserName()));
	}

	/**********
	 * Forgets every acceptance and closes the test database, which also discards it.
	 */
	@AfterEach
	public void tearDown() {
		SharedAiService.clearAllConsent();
		database.closeConnection();
	}


	/**********
	 * RP-01 (N): even if the page passed "consented", the shared service sends nothing for a
	 * Curator who has not accepted the AI notice, and the request reports the AI as
	 * unavailable without changing the lesson (US-C-E3-03).
	 */
	@Test
	@DisplayName("RP-01 No AI request without this Curator's acceptance of the notice (US-C-E3-03, N)")
	public void rp01_serviceChecksConsent() {
		String before = state();
		Review review = model.requestSuggestion(curator, open(), "description", "", true);
		assertFalse(review.success());
		assertEquals(ModelCuratorRefine.UNAVAILABLE, review.message());
		assertEquals(0, fake.asked.size(), "Nothing may be sent");
		assertEquals(before, state());
	}

	/**********
	 * RP-02 (N): another Curator's acceptance does not count for this Curator (US-AI-D2-01).
	 */
	@Test
	@DisplayName("RP-02 Another user's acceptance does not let this Curator's request through (US-AI-D2-01, N)")
	public void rp02_consentIsPerUser() {
		SharedAiService.setUserConsented(otherCurator.getUserName(), true);
		Review review = model.requestSuggestion(curator, open(), "title", "", true);
		assertFalse(review.success());
		assertEquals(0, fake.asked.size());
	}

	/**********
	 * RP-03 (P): with consent, the chosen field, its stored text, and the goal reach the shared
	 * service; the cleaned suggestion and the original come back side by side, and the stored
	 * lesson is unchanged (US-C-E3-03, US-C-E3-04).
	 */
	@Test
	@DisplayName("RP-03 A suggestion comes back beside the original and changes nothing (US-C-E3-03/04, P)")
	public void rp03_suggestionThroughSharedService() {
		SharedAiService.setUserConsented(curator.getUserName(), true);
		fake.reply = "  \"Restore, check, then restart the worker.\"  ";
		String before = state();
		Review review = model.requestSuggestion(curator, open(), "description", "shorter", true);
		assertTrue(review.success(), review.message());
		assertEquals(DESCRIPTION, review.original());
		assertEquals("Restore, check, then restart the worker.", review.suggestion());
		assertEquals(List.of("description|" + DESCRIPTION + "|shorter"), fake.asked);
		assertEquals(before, state(), "A suggestion alone never changes the stored lesson");
	}

	/**********
	 * RP-04 (P): a title suggestion is made one line, as the lesson form needs (US-C-E3-05).
	 */
	@Test
	@DisplayName("RP-04 A title suggestion is one line (US-C-E3-05, P)")
	public void rp04_titleIsOneLine() {
		SharedAiService.setUserConsented(curator.getUserName(), true);
		fake.reply = "Recovering a failed\nworker";
		Review review = model.requestSuggestion(curator, open(), "title", "", true);
		assertTrue(review.success(), review.message());
		assertEquals("Recovering a failed worker", review.suggestion());
		assertEquals(TITLE, review.original());
	}

	/**********
	 * RP-05 (N, then P): a provider failure gives the fixed message without the provider's
	 * error or key, keeps the review so the Curator can continue, and a manual refinement can
	 * still be saved (US-C-E3-08).
	 */
	@Test
	@DisplayName("RP-05 A failure keeps the review; manual wording can still be saved (US-C-E3-08, N/P)")
	public void rp05_failureThenManualSave() {
		SharedAiService.setUserConsented(curator.getUserName(), true);
		fake.fail = true;
		RefinementDraft draft = open();
		Review review = model.requestSuggestion(curator, draft, "description", "", true);
		assertFalse(review.success());
		assertEquals(ModelCuratorRefine.UNAVAILABLE, review.message());
		assertFalse(review.message().contains("SECRET"));
		assertNotNull(review.draft(), "The Curator keeps the review after a failure");

		LessonResult saved = model.saveRefinement(curator, review.draft(),
				Map.of("description", "We restored the worker's configuration, then restarted it."),
				true);
		assertTrue(saved.isSuccess(), saved.getMessage());
		assertEquals("We restored the worker's configuration, then restarted it.",
				database.getLessonById(lessonId).getDescription());
	}

	/**********
	 * RP-06 (N): a title suggestion longer than 100 characters is never shown; the request
	 * reports the AI as unavailable and the lesson is unchanged (US-C-E3-08, "an unusable
	 * response is not applied or saved").
	 */
	@Test
	@DisplayName("RP-06 An unusable suggestion is not shown or saved (US-C-E3-08, N)")
	public void rp06_unusableSuggestion() {
		SharedAiService.setUserConsented(curator.getUserName(), true);
		fake.reply = "T".repeat(101);
		String before = state();
		Review review = model.requestSuggestion(curator, open(), "title", "", true);
		assertFalse(review.success());
		assertEquals("", review.suggestion());
		assertEquals(before, state());
	}

	/**********
	 * RP-07 (P): the whole page flow: a suggestion, edited by the Curator, saved; the lesson
	 * then has the Curator's wording, the contributor's original text, and the Curator's name,
	 * and the review snapshot taken after the save matches the stored lesson (US-C-E3-01,
	 * US-C-E3-06).
	 */
	@Test
	@DisplayName("RP-07 Suggest, edit, and save; the original and the Curator are recorded (US-C-E3-01/06, P)")
	public void rp07_suggestEditSave() {
		SharedAiService.setUserConsented(curator.getUserName(), true);
		RefinementDraft draft = open();
		Review review = model.requestSuggestion(curator, draft, "description", "", true);
		assertTrue(review.success(), review.message());
		String edited = review.suggestion() + " It took ten minutes.";
		LessonResult saved = model.saveRefinement(curator, draft, Map.of("description", edited),
				true);
		assertTrue(saved.isSuccess(), saved.getMessage());

		LessonsLearned stored = database.getLessonById(lessonId);
		assertEquals(edited, stored.getDescription());
		assertEquals(TITLE, stored.getTitle(), "A field the Curator did not change is not written");
		assertEquals(DESCRIPTION, stored.getOriginalDescription());
		assertEquals(TITLE, stored.getOriginalTitle());
		assertEquals(curator.getUserName(), stored.getCuratedBy());
		assertTrue(stored.getCuratedAt() > 0);
		assertTrue(open().matches(stored), "A new review after the save starts from the saved text");
	}

	/**********
	 * RP-08 (P): the "Edited by curator" note names the Curator and the date and time in
	 * English (US-C-E3-07).
	 */
	@Test
	@DisplayName("RP-08 The Edited by curator note names the Curator and the time (US-C-E3-07, P)")
	public void rp08_noteText() {
		LessonsLearned lesson = new LessonsLearned("owner", TITLE, DESCRIPTION, "", "Technical");
		lesson.setCuratedBy("refineCurator");
		lesson.setCuratedAt(SAVE_TIME);
		assertEquals("Edited by curator: refineCurator on Nov 14, 2023 at 10:13 PM",
				CurationNote.text(lesson, ZoneOffset.UTC));
	}

	/**********
	 * RP-09 (N): no note for a missing lesson, a lesson never refined, or incomplete
	 * attribution, so no false "Edited by curator" note appears (US-C-E3-07 V sentence).
	 */
	@Test
	@DisplayName("RP-09 No note without a complete refinement (US-C-E3-07, N)")
	public void rp09_noFalseNote() {
		assertEquals("", CurationNote.text(null));
		LessonsLearned lesson = new LessonsLearned("owner", TITLE, DESCRIPTION, "", "Technical");
		assertEquals("", CurationNote.text(lesson));
		lesson.setCuratedBy("refineCurator");
		assertEquals("", CurationNote.text(lesson), "A name without a time");
		lesson.setCuratedBy("");
		lesson.setCuratedAt(SAVE_TIME);
		assertEquals("", CurationNote.text(lesson), "A time without a name");
	}

	/**********
	 * RP-10 (P/N): the Curator Home details show the note after the locks and the original text
	 * at the end for a refined lesson, and are unchanged for a lesson never refined
	 * (US-C-E3-01, US-C-E3-07).
	 */
	@Test
	@DisplayName("RP-10 Curator Home details show the note and the original text (US-C-E3-01/07, P/N)")
	public void rp10_curatorHomeDetails() {
		LessonsLearned plain = new LessonsLearned("owner", TITLE, DESCRIPTION, "", "Technical");
		String plainText = ModelCuratorHome.detailsText(plain);
		assertFalse(plainText.contains("Edited by curator"));
		assertFalse(plainText.contains("Original text"));

		LessonsLearned refined = new LessonsLearned("owner", "Recovering a failed worker",
				"Restore, check, then restart.", "", "Technical");
		refined.setCuratedBy("refineCurator");
		refined.setCuratedAt(SAVE_TIME);
		refined.setOriginalTitle(TITLE);
		refined.setOriginalDescription(DESCRIPTION);
		String text = ModelCuratorHome.detailsText(refined);
		assertTrue(text.contains("Locked: nothing\n" + CurationNote.text(refined) + "\n\n"
				+ "Description:\nRestore, check, then restart."), text);
		assertTrue(text.endsWith("Original text, before any curator edits:\nTitle: " + TITLE
				+ "\nDescription:\n" + DESCRIPTION), text);
	}


	/*-*******************************************************************************************

	Helpers

	 */

	/**********
	 * Opens the lesson for the Curator, as the page does.
	 *
	 * @return the review snapshot
	 */
	private RefinementDraft open() {
		Review opened = model.openLesson(curator, lessonId);
		assertTrue(opened.success(), opened.message());
		return opened.draft();
	}

	/**********
	 * Everything about the lesson that a refinement could change, as one line.
	 *
	 * @return the stored title, description, originals, and attribution
	 */
	private String state() {
		LessonsLearned l = database.getLessonById(lessonId);
		return l.getTitle() + "|" + l.getDescription() + "|" + l.getOriginalTitle() + "|"
				+ l.getOriginalDescription() + "|" + l.getCuratedBy() + "|" + l.getCuratedAt()
				+ "|" + l.getUpdatedAt();
	}
}
