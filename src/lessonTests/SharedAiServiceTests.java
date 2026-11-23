package lessonTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import database.BrainstormService;
import database.SharedAiService;

/**
 * Unit tests for SharedAiService verifying untrusted AI content, consent,
 * and fail safe error handling for TP3 Aspect 8 (US-AI-D2-01, US-AI-D2-02).
 *
 * <p> The tests after testRequestConsentIfNeededSkipsDialogWhenAlreadyConsented check the
 * per-user consent policy and every request with a simulated provider (FakeBackend), so they
 * need no API key and no network.  The consent dialog itself is checked by hand in Manual
 * Tests.pdf. </p>
 *
 * @author Virgil Jones
 * @author Emilia Davey
 *
 * @version 1.00 2026-11-16 Virgil Jones: cleaning, consent, and blocked-request tests (TP3 B-10)
 * @version 1.01 2026-11-17 Emilia Davey: consent is per user; tests for the per-user policy,
 *          the request success and failure paths, lesson search terms, and brainstorming
 */
public class SharedAiServiceTests {

    /**
     * Default constructor; JUnit creates one instance for each test.
     */
    public SharedAiServiceTests() {
    }

    /** The service under test, with the default backend. */
    private SharedAiService service;

    /** The acting user in the tests below. */
    private static final String USER = "aiTestUser";
    /** A second user, who must not inherit USER's consent. */
    private static final String OTHER = "aiOtherUser";

    /**
     * A simulated AI provider: returns the canned replies set by a test, or throws when told to
     * fail, and counts the requests it received.
     */
    private static class FakeBackend implements SharedAiService.AiBackend {
        /** Creates a provider that is configured and answers. */
        FakeBackend() {
        }
        /** False to simulate a missing API key. */
        boolean configured = true;
        /** True to make every request fail. */
        boolean fail = false;
        /** The reply to coaching and brainstorming requests. */
        String reply = "Reply.";
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
            if (fail) throw new RuntimeException("Groq API error 401: key=SECRET");
        }
        public boolean isConfigured() { return configured; }
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
        /** The field, text, and goal of the last refinement request. */
        String lastRefine = "";
        public String refine(String f, String t, String g) throws Exception {
            call(); lastRefine = f + "|" + t + "|" + g; return reply;
        }
    }

    /** The simulated provider of the current test. */
    private FakeBackend fake;

    /**
     * Sets up a clean SharedAiService instance and resets consent state before each test.
     */
    @BeforeEach
    public void setUp() {
        service = new SharedAiService();
        fake = new FakeBackend();
        SharedAiService.clearAllConsent();
    }

    /**
     * Forgets every acceptance, so no consent leaks into other test classes.
     */
    @AfterEach
    public void tearDown() {
        SharedAiService.clearAllConsent();
    }

    /**
     * Tests that clean, valid AI suggestions within the boundary return successfully.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testValidSuggestion() {
        // Why: Valid normal text should be accepted and trimmed properly.
        String input = "  This is a helpful suggestion.  ";
        String result = service.cleanAiSuggestion(input, 100);
        assertEquals("This is a helpful suggestion.", result);
    }

    /**
     * Tests that null AI responses are rejected.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testNullSuggestionThrowsException() {
        // Why: Null checks prevent unexpected crashes when an API fails to send body content.
        try {
            service.cleanAiSuggestion(null, 100);
            fail("*** Error *** Null suggestion was not recognized as an error");
        } catch (IllegalArgumentException e) {
            assertEquals("AI suggestion cannot be null", e.getMessage());
        }
    }

    /**
     * Tests that empty or whitespace only AI responses are rejected.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testEmptySuggestionThrowsException() {
        // Why: Blank suggestions should be caught so empty cards are not shown to users.
        try {
            service.cleanAiSuggestion("    ", 100);
            fail("*** Error *** Empty suggestion was not recognized as an error");
        } catch (IllegalArgumentException e) {
            assertEquals("AI suggestion cannot be empty", e.getMessage());
        }
    }

    /**
     * Tests the upper boundary length of an allowed suggestion.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testMaxLengthBoundaryValid() {
        // Why: Tests the exact x_max boundary to ensure no off-by-one errors at the limit.
        String input = "A".repeat(100);
        String result = service.cleanAiSuggestion(input, 100);
        assertEquals(input, result);
    }

    /**
     * Tests that suggestions exceeding the maximum length boundary are rejected.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testExceedsMaxLengthThrowsException() {
        // Why: Tests out-of-bounds (x_max+) test to stop data that would break database column sizes.
        String input = "A".repeat(101);
        try {
            service.cleanAiSuggestion(input, 100);
            fail("*** Error *** Overlength suggestion was not recognized as an error");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("exceeds maximum allowed length"));
        }
    }

    /**
     * Tests that suggestions containing invalid ASCII control characters are rejected.
     *
     * <p> Validates: SharedAiService.cleanAiSuggestion(). </p>
     */
    @Test
    public void testControlCharacterThrowsException() {
        // Why: Protects the database and UI from ASCII control characters.
        String input = "Bad text\u0000here";
        try {
            service.cleanAiSuggestion(input, 100);
            fail("*** Error *** Control character was not recognized as an error");
        } catch (IllegalArgumentException e) {
            assertEquals("AI suggestion contains invalid control character", e.getMessage());
        }
    }

    /**
     * Tests that the session consent updates properly (US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.hasUserConsented(), SharedAiService.setUserConsented(). </p>
     */
    @Test
    public void testConsentStateToggle() {
        // Why: Verifies that user consent defaults to false and updates when granted.
        assertFalse(SharedAiService.hasUserConsented(USER));
        SharedAiService.setUserConsented(USER, true);
        assertTrue(SharedAiService.hasUserConsented(USER));
    }

    /**
     * Tests that coaching requests are blocked if user consent has not been granted (US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.requestCoaching(). </p>
     */
    @Test
    public void testCoachingBlockedWithoutConsent() {
        // Why: Ensures no lesson text is transmitted before the user accepts the AI notice.
        try {
            service.requestCoaching(USER, "Test Title", "Test Description", 500);
            fail("*** Error *** Coaching request succeeded without user consent");
        } catch (IllegalStateException e) {
            assertEquals(SharedAiService.CONSENT_DECLINED_MESSAGE, e.getMessage());
        }
    }

    /**
     * Tests that search term requests are blocked if user consent has not been granted (US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.requestSearchTerms(). </p>
     */
    @Test
    public void testSearchTermsBlockedWithoutConsent() {
        // Why: Ensures shared consent policy blocks E2 requests until accepted.
        try {
            service.requestSearchTerms(USER, "deployment rollback");
            fail("*** Error *** Search term request succeeded without user consent");
        } catch (IllegalStateException e) {
            assertEquals(SharedAiService.CONSENT_DECLINED_MESSAGE, e.getMessage());
        }
    }

    /**
     * Tests that once consent is granted, requestConsentIfNeeded returns true immediately
     * without re-prompting (US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.requestConsentIfNeeded(). </p>
     */
    @Test
    public void testRequestConsentIfNeededSkipsDialogWhenAlreadyConsented() {
        // Why: Confirms the one time notice is remembered across calls in the same session.
        SharedAiService.setUserConsented(USER, true);
        assertTrue(service.requestConsentIfNeeded(USER));
    }

    /*-****************************************************************************************

    Per-user consent and the request paths (added 2026-11-17)

     */

    /**
     * One user's acceptance does not cover a different user who logs in later, and the same
     * user stays accepted after logging out and back in (US-AI-D2-01).  No user (null) never
     * counts as accepted.
     *
     * <p> Validates: SharedAiService.requestConsentIfNeeded(). </p>
     */
    @Test
    public void testConsentIsPerUser() {
        SharedAiService.setUserConsented(USER, true);
        assertFalse(SharedAiService.hasUserConsented(OTHER), "Another user must be asked again");
        assertTrue(SharedAiService.hasUserConsented(USER), "The same user is remembered");
        assertFalse(SharedAiService.hasUserConsented(null));
        assertFalse(service.requestConsentIfNeeded(null), "No user can never consent");

        SharedAiService.setUserConsented(USER, false);
        assertFalse(SharedAiService.hasUserConsented(USER), "Consent can be withdrawn");
    }

    /**
     * A user without consent is refused for every request, and nothing reaches the provider,
     * even when another user has consented (US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.requestCoaching(),
     * SharedAiService.requestLessonSearchTerms(), SharedAiService.requestBrainstormReply(),
     * SharedAiService.requestTakeaways(). </p>
     */
    @Test
    public void testNothingIsSentWithoutThisUsersConsent() {
        SharedAiService.setUserConsented(OTHER, true);
        SharedAiService s = new SharedAiService(fake);
        List<BrainstormService.ChatMessage> history = new ArrayList<BrainstormService.ChatMessage>();
        assertThrows(IllegalStateException.class, () -> s.requestCoaching(USER, "T", "D", 500));
        assertThrows(IllegalStateException.class, () -> s.requestLessonSearchTerms(USER, "T", "D", ""));
        assertThrows(IllegalStateException.class, () -> s.requestBrainstormReply(USER, history, 500));
        assertThrows(IllegalStateException.class, () -> s.requestTakeaways(USER, history, 500));
        assertEquals(0, fake.requests, "No request may reach the provider");
    }

    /**
     * With consent and a key, coaching returns the cleaned reply (US-P-E1-02).
     *
     * <p> Validates: SharedAiService.requestCoaching(). </p>
     */
    @Test
    public void testCoachingSucceedsWithConsent() {
        SharedAiService.setUserConsented(USER, true);
        fake.reply = "  - Say what went wrong.\n- Add the date.  ";
        assertEquals("- Say what went wrong.\n- Add the date.",
                new SharedAiService(fake).requestCoaching(USER, "T", "D", 500));
        assertEquals(1, fake.requests);
    }

    /**
     * A missing API key gives the unavailable message without sending anything, and a provider
     * failure gives the same message without the provider's own error text, which can contain
     * the request or the credential (US-AI-D2-02).
     *
     * <p> Validates: SharedAiService.requestCoaching(). </p>
     */
    @Test
    public void testMissingKeyAndFailureGiveTheUnavailableMessage() {
        SharedAiService.setUserConsented(USER, true);
        fake.configured = false;
        IllegalStateException noKey = assertThrows(IllegalStateException.class,
                () -> new SharedAiService(fake).requestCoaching(USER, "T", "D", 500));
        assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, noKey.getMessage());
        assertEquals(0, fake.requests, "Nothing is sent without a key");

        fake.configured = true;
        fake.fail = true;
        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> new SharedAiService(fake).requestCoaching(USER, "T", "D", 500));
        assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, failed.getMessage());
        assertFalse(failed.getMessage().contains("SECRET"));
    }

    /**
     * Lesson search terms fit the search-term rule: commas inside a term are removed, blank,
     * over-50-character, and repeated terms are dropped, and the total stays within 500
     * characters (US-C-E2-01).  If nothing usable is left, the request counts as failed.
     *
     * <p> Validates: SharedAiService.requestLessonSearchTerms(). </p>
     */
    @Test
    public void testLessonSearchTermsFitTheRule() {
        SharedAiService.setUserConsented(USER, true);
        SharedAiService s = new SharedAiService(fake);
        fake.list = Arrays.asList(" rollback ", "deploy, failure", "", null, "A".repeat(51),
                "Rollback", "release   checklist");
        assertEquals("rollback, deploy failure, release checklist",
                s.requestLessonSearchTerms(USER, "T", "D", "g"));

        List<String> many = new ArrayList<String>();
        for (int i = 0; i < 40; i++) many.add("term number " + i + " " + "x".repeat(20));
        fake.list = many;
        String joined = s.requestLessonSearchTerms(USER, "T", "D", "g");
        assertTrue(joined.length() <= 500, "Total length " + joined.length());
        assertEquals("", inputValidation.LessonRecognizer.checkForValidSearchTerms(joined));

        fake.list = Arrays.asList("", "A".repeat(60));
        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> s.requestLessonSearchTerms(USER, "T", "D", "g"));
        assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, none.getMessage());
    }

    /**
     * Brainstorming returns the next question, and takeaways drop unusable items; with no
     * usable takeaway the request counts as failed (US-P-E1-01, US-AI-D2-02).
     *
     * <p> Validates: SharedAiService.requestBrainstormReply(),
     * SharedAiService.requestTakeaways(). </p>
     */
    @Test
    public void testBrainstormReplyAndTakeaways() {
        SharedAiService.setUserConsented(USER, true);
        SharedAiService s = new SharedAiService(fake);
        List<BrainstormService.ChatMessage> history = new ArrayList<BrainstormService.ChatMessage>();
        history.add(new BrainstormService.ChatMessage("user", "Our deploy failed."));
        fake.reply = "What did you check before deploying?";
        assertEquals("What did you check before deploying?", s.requestBrainstormReply(USER, history, 1000));

        fake.list = Arrays.asList("Check the config first.", " ", "Bad\u0000item", "Test rollback.");
        assertEquals(Arrays.asList("Check the config first.", "Test rollback."),
                s.requestTakeaways(USER, history, 500));

        fake.list = Arrays.asList(" ");
        assertThrows(IllegalStateException.class, () -> s.requestTakeaways(USER, history, 500));
    }

    /**
     * A refinement request sends nothing until this Curator accepted the notice; another
     * user's acceptance does not count (US-C-E3-03, US-AI-D2-01).
     *
     * <p> Validates: SharedAiService.requestRefinement(). </p>
     */
    @Test
    public void testRefinementNeedsThisUsersConsent() {
        SharedAiService s = new SharedAiService(fake);
        SharedAiService.setUserConsented(OTHER, true);
        IllegalStateException declined = assertThrows(IllegalStateException.class,
                () -> s.requestRefinement(USER, "title", "Fixed CI", ""));
        assertEquals(SharedAiService.CONSENT_DECLINED_MESSAGE, declined.getMessage());
        assertEquals(0, fake.requests, "Nothing may be sent without this user's consent");
    }

    /**
     * A refinement is cleaned for its field: surrounding quotes go, a title becomes one line,
     * a description keeps its line breaks, and the field, text, and goal reach the provider
     * (US-C-E3-03).
     *
     * <p> Validates: SharedAiService.requestRefinement(). </p>
     */
    @Test
    public void testRefinementIsCleanedForItsField() {
        SharedAiService.setUserConsented(USER, true);
        SharedAiService s = new SharedAiService(fake);
        fake.reply = "  \"Fixed the flaky\n  CI pipeline\"  ";
        assertEquals("Fixed the flaky CI pipeline", s.requestRefinement(USER, "title", "Fixed CI", "clearer"));
        assertEquals("title|Fixed CI|clearer", fake.lastRefine);

        fake.reply = "First line.\nSecond line.";
        assertEquals("First line.\nSecond line.", s.requestRefinement(USER, "description", "D", null));
        assertEquals("description|D|", fake.lastRefine, "A missing goal is sent as empty");
    }

    /**
     * A reply that does not fit the field, an empty reply, a failure, or a missing key all give
     * the one unavailable message, which never shows the provider's error or the key
     * (US-C-E3-08, US-AI-D2-02).
     *
     * <p> Validates: SharedAiService.requestRefinement(). </p>
     */
    @Test
    public void testRefinementProblemsGiveTheUnavailableMessage() {
        SharedAiService.setUserConsented(USER, true);
        SharedAiService s = new SharedAiService(fake);
        fake.reply = "T".repeat(101);
        assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, assertThrows(IllegalStateException.class,
                () -> s.requestRefinement(USER, "title", "x", "")).getMessage());
        fake.reply = "D".repeat(5000);
        assertEquals(5000, s.requestRefinement(USER, "description", "x", "").length());
        fake.reply = "  \"\"  ";
        assertThrows(IllegalStateException.class, () -> s.requestRefinement(USER, "title", "x", ""));
        fake.reply = null;
        assertThrows(IllegalStateException.class, () -> s.requestRefinement(USER, "title", "x", ""));

        fake.fail = true;
        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> s.requestRefinement(USER, "description", "x", ""));
        assertEquals(SharedAiService.UNAVAILABLE_MESSAGE, failed.getMessage());
        assertFalse(failed.getMessage().contains("SECRET"));

        fake.fail = false;
        fake.configured = false;
        int before = fake.requests;
        assertThrows(IllegalStateException.class, () -> s.requestRefinement(USER, "title", "x", ""));
        assertEquals(before, fake.requests, "Nothing is sent without a key");
    }
}
