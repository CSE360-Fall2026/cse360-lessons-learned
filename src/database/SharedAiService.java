package database;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Shared AI service for TP3 Aspect 8 (D2).
 * Enforces one time user consent (US-AI-D2-01), catches API failures safely,
 * and validates all AI responses as untrusted text (US-AI-D2-02).
 *
 * <p> <b>One consent policy for every AI tool.</b>  Every AI feature (Contributor coaching and
 * brainstorming, E1; Curator search terms, E2; Curator refinement, E3) asks for consent through
 * requestConsentIfNeeded and sends text only through this class's request methods.  The
 * acceptance is remembered per user name for the rest of the application run, so one user's
 * acceptance never covers another user who logs in later, and the same user is not asked again
 * after logging out and back in.  Each request method takes the acting user's name and refuses
 * to send anything for a user who has not accepted the notice (US-AI-D2-01). </p>
 *
 * <p> <b>Failures.</b>  A missing API key, a timeout, an HTTP error, or an unusable reply all
 * become an IllegalStateException with UNAVAILABLE_MESSAGE, so a page can show one clear message
 * and keep the user's draft.  The provider's own error text never reaches the page, because it
 * can contain the request or the credential (US-AI-D2-02). </p>
 *
 * <p> <b>Testing.</b>  The calls to the provider go through the AiBackend interface.  The
 * default backend is BrainstormService (Groq, with the key from the local config.properties);
 * JUnit tests pass a simulated backend to the constructor, so every path can be tested without
 * a key or a network. </p>
 *
 * @author Virgil Jones
 * @author Emilia Davey
 *
 * @version 1.00 2026-11-16 Virgil Jones: consent dialog, coaching and keyword requests, and
 *          cleaning of untrusted replies (TP3 B-10)
 * @version 1.01 2026-11-17 Emilia Davey: consent is remembered per user instead of once per run;
 *          every request takes the acting user's name; brainstorming, takeaway, and lesson
 *          search-term requests for E1 and E2; an injectable AiBackend for testing
 */
public class SharedAiService {

    /** Shown when the AI service cannot be used right now (US-AI-D2-02). */
    public static final String UNAVAILABLE_MESSAGE =
            "The coach is unavailable right now. You can keep writing.";
    /** Shown when the user declined, or has not accepted, the AI notice (US-AI-D2-01). */
    public static final String CONSENT_DECLINED_MESSAGE =
            "AI assistance was declined. You can continue writing manually.";

    /** The heading of the AI notice. */
    public static final String NOTICE_HEADER = "Send lesson text to an external AI service?";
    /** The text of the AI notice: what is sent, where, and what declining means
     * (US-AI-D2-01). */
    public static final String NOTICE_TEXT = "To give suggestions, the lesson text you are "
            + "working on is sent to Groq, an external AI provider.  Do not include passwords "
            + "or sensitive data.  If you choose Cancel, nothing is sent and you can keep "
            + "working without AI.  Your answer is remembered until the application closes.";

    /** The most search-term characters a lesson may hold (LessonRecognizer). */
    private static final int SEARCH_TERMS_TOTAL_MAX = 500;
    /** The most characters one search term may have (LessonRecognizer). */
    private static final int SEARCH_TERM_MAX = 50;

    /** The user names that accepted the notice during this run of the application. */
    private static final Set<String> consentedUsers = new HashSet<String>();

    /**
     * <p> The calls this service makes to the AI provider.  The default sends them to Groq
     * through BrainstormService; tests supply a simulated one. </p>
     */
    public interface AiBackend {
        /**
         * Is an API key configured?
         *
         * @return true if an API key is configured
         */
        boolean isConfigured();
        /**
         * Asks for writing coaching on a lesson draft.
         *
         * @param title the lesson title
         * @param description the lesson text
         * @return the raw coaching reply
         * @throws Exception if the request fails
         */
        String coach(String title, String description) throws Exception;
        /**
         * Asks for alternative keywords for a search.
         *
         * @param query the user's search words
         * @return the raw keyword suggestions
         * @throws Exception if the request fails
         */
        List<String> keywords(String query) throws Exception;
        /**
         * Asks for search terms that would help someone find a lesson.
         *
         * @param title the lesson title
         * @param description the lesson description
         * @param tags the lesson tags
         * @return the raw search-term suggestions for the lesson
         * @throws Exception if the request fails
         */
        List<String> lessonSearchTerms(String title, String description, String tags)
                throws Exception;
        /**
         * Asks for the next brainstorming question.
         *
         * @param history the brainstorming conversation so far
         * @return the raw next question or reply
         * @throws Exception if the request fails
         */
        String chat(List<BrainstormService.ChatMessage> history) throws Exception;
        /**
         * Asks for the lessons learned in a brainstorming conversation.
         *
         * @param history the brainstorming conversation
         * @return the raw takeaways
         * @throws Exception if the request fails
         */
        List<String> takeaways(List<BrainstormService.ChatMessage> history) throws Exception;
    }

    /** The default backend: Groq through BrainstormService. */
    private static final AiBackend GROQ = new AiBackend() {
        public boolean isConfigured() { return BrainstormService.isConfigured(); }
        public String coach(String title, String description) throws Exception {
            return BrainstormService.coachEntry(title, description);
        }
        public List<String> keywords(String query) throws Exception {
            return BrainstormService.suggestKeywords(query);
        }
        public List<String> lessonSearchTerms(String title, String description, String tags)
                throws Exception {
            return BrainstormService.suggestLessonSearchTerms(title, description, tags);
        }
        public String chat(List<BrainstormService.ChatMessage> history) throws Exception {
            return BrainstormService.chat(history);
        }
        public List<String> takeaways(List<BrainstormService.ChatMessage> history)
                throws Exception {
            return BrainstormService.extractTakeaways(history);
        }
    };

    /** The backend this instance uses. */
    private final AiBackend backend;

    /**
     * Creates the service with the default backend (Groq through BrainstormService).
     */
    public SharedAiService() {
        this(GROQ);
    }

    /**
     * Creates the service with the given backend.  Used by JUnit tests with a simulated
     * provider.
     *
     * @param backend the backend that answers the requests
     */
    public SharedAiService(AiBackend backend) {
        this.backend = (backend == null) ? GROQ : backend;
    }

    /*-****************************************************************************************

    Consent (US-AI-D2-01)

     */

    /**
     * Has this user accepted the AI notice during this run of the application?
     *
     * @param userName the acting user's name
     * @return true if the user accepted the notice
     */
    public static synchronized boolean hasUserConsented(String userName) {
        return userName != null && consentedUsers.contains(userName);
    }

    /**
     * Records whether a user accepted the AI notice.  requestConsentIfNeeded calls this after
     * the dialog; tests call it directly.
     *
     * @param userName the acting user's name
     * @param consented true if the user accepted, false to withdraw the acceptance
     */
    public static synchronized void setUserConsented(String userName, boolean consented) {
        if (userName == null) return;
        if (consented) consentedUsers.add(userName);
        else consentedUsers.remove(userName);
    }

    /**
     * Forgets every acceptance.  Used by JUnit tests so each test starts with no consent.
     */
    public static synchronized void clearAllConsent() {
        consentedUsers.clear();
    }

    /**
     * Shows the AI notice to this user unless they already accepted it in this run, and
     * remembers an acceptance.  Must be called on the JavaFX thread.
     *
     * @param userName the acting user's name
     * @return true if the user has accepted the notice (now or earlier); false if they chose
     *         Cancel or closed the dialog, in which case nothing may be sent
     */
    public boolean requestConsentIfNeeded(String userName) {
        if (userName == null) return false;
        if (hasUserConsented(userName)) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("AI Assistance Notice");
        alert.setHeaderText(NOTICE_HEADER);
        alert.setContentText(NOTICE_TEXT);

        ButtonType ok = new ButtonType("I understand, continue", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ok) {
            setUserConsented(userName, true);
            return true;
        }
        return false;
    }

    /*-****************************************************************************************

    Requests.  Each one checks consent first, then the configuration, and turns every failure
    into UNAVAILABLE_MESSAGE.

     */

    /**
     * Requests writing coaching for a lesson and cleans the untrusted reply (US-P-E1-02).
     *
     * @param userName the acting user, who must have accepted the notice
     * @param title the lesson title
     * @param description the lesson text, with any root cause and recommendation
     * @param maxLength the longest reply that will be accepted
     * @return the cleaned coaching text
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE
     */
    public String requestCoaching(String userName, String title, String description,
            int maxLength) {
        checkReady(userName);
        try {
            return cleanAiSuggestion(backend.coach(title, description), maxLength);
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * Requests keyword suggestions for a search and cleans each returned term.
     *
     * @param userName the acting user, who must have accepted the notice
     * @param query the user's search words
     * @return the cleaned keywords
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE
     */
    public List<String> requestSearchTerms(String userName, String query) {
        checkReady(userName);
        try {
            List<String> terms = new ArrayList<String>(backend.keywords(query));
            for (int i = 0; i < terms.size(); i++) {
                terms.set(i, cleanAiSuggestion(terms.get(i), 100));
            }
            return terms;
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * Requests search terms that would help Viewers find a lesson (Epic E2, US-C-E2-01), and
     * returns them as one comma-separated String that fits the search-term rule: terms that
     * are blank, unusable, or longer than 50 characters are dropped, repeated terms are kept
     * once, and terms are added only while the total stays within 500 characters.  The Curator
     * still reviews and edits the result before anything is saved.
     *
     * @param userName the acting user, who must have accepted the notice
     * @param title the lesson title
     * @param description the lesson description
     * @param tags the lesson tags
     * @return the suggested terms, separated by ", "
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE,
     *         including when no usable term came back
     */
    public String requestLessonSearchTerms(String userName, String title, String description,
            String tags) {
        checkReady(userName);
        List<String> raw;
        try {
            raw = backend.lessonSearchTerms(title, description, tags);
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
        StringBuilder joined = new StringBuilder();
        Set<String> seen = new HashSet<String>();
        if (raw != null) {
            for (String term : raw) {
                String clean;
                try {
                    // A comma inside a term would split it into two terms when saved
                    clean = cleanAiSuggestion(term == null ? null : term.replace(',', ' '),
                            SEARCH_TERM_MAX).replaceAll("\\s+", " ");
                } catch (IllegalArgumentException unusable) {
                    continue;
                }
                if (!seen.add(clean.toLowerCase())) continue;
                int added = (joined.length() == 0 ? 0 : 2) + clean.length();
                if (joined.length() + added > SEARCH_TERMS_TOTAL_MAX) break;
                if (joined.length() > 0) joined.append(", ");
                joined.append(clean);
            }
        }
        if (joined.length() == 0) throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        return joined.toString();
    }

    /**
     * Sends the brainstorming conversation and returns the AI's next question (US-P-E1-01:
     * "the user can answer follow-up questions").  The reply is cleaned like any suggestion.
     *
     * @param userName the acting user, who must have accepted the notice
     * @param history the conversation so far, ending with the user's latest message
     * @param maxLength the longest reply that will be accepted
     * @return the cleaned reply
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE
     */
    public String requestBrainstormReply(String userName,
            List<BrainstormService.ChatMessage> history, int maxLength) {
        checkReady(userName);
        try {
            return cleanAiSuggestion(backend.chat(history), maxLength);
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * Asks for the lessons learned in a brainstorming conversation, so the Contributor can
     * choose which to use (US-P-E1-01).  Unusable items are dropped; if none is left, the
     * request counts as failed.
     *
     * @param userName the acting user, who must have accepted the notice
     * @param history the conversation
     * @param maxLength the longest takeaway that will be accepted
     * @return the cleaned takeaways
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE
     */
    public List<String> requestTakeaways(String userName,
            List<BrainstormService.ChatMessage> history, int maxLength) {
        checkReady(userName);
        List<String> raw;
        try {
            raw = backend.takeaways(history);
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
        List<String> clean = new ArrayList<String>();
        if (raw != null) {
            for (String item : raw) {
                try {
                    clean.add(cleanAiSuggestion(item, maxLength));
                } catch (IllegalArgumentException unusable) {
                    // An unusable item is dropped rather than shown
                }
            }
        }
        if (clean.isEmpty()) throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        return clean;
    }

    /**
     * Refuses a request from a user who has not accepted the notice, or when no API key is
     * configured.  Nothing is sent in either case.
     *
     * @param userName the acting user
     * @throws IllegalStateException with CONSENT_DECLINED_MESSAGE or UNAVAILABLE_MESSAGE
     */
    private void checkReady(String userName) {
        if (!hasUserConsented(userName)) {
            throw new IllegalStateException(CONSENT_DECLINED_MESSAGE);
        }
        if (!backend.isConfigured()) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * Clean and validates an AI suggestion string before it can be displayed
     * in the GUI or saved into the database.
     *
     * @param suggestion the raw text received from the AI service
     * @param maxLength the maximum allowed characters for the target field
     * @return the trimmed, cleaned suggestion string
     * @throws IllegalArgumentException if text is null, blank, too long, or has control characters
     */
    public String cleanAiSuggestion(String suggestion, int maxLength) {
        // Why: AI services can return null if the content sent is empty or broken.
        // We reject null immediately to prevent NullPointerExceptions.
        if (suggestion == null) {
            throw new IllegalArgumentException("AI suggestion cannot be null");
        }

        // Why: Trim surrounding whitespace.
        String trimmed = suggestion.trim();

        // Why: An empty reply or whitespace-only reply gives the user no value.
        // We reject empty text so the UI does not render empty suggestion cards.
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("AI suggestion cannot be empty");
        }

        // Why: Database columns have strict boundaries (e.g., VARCHAR(100) or VARCHAR(2000)).
        // Checking maxLength here prevents SQL exceptions.
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("AI suggestion exceeds maximum allowed length of " + maxLength);
        }

        // Why: Language models can contain raw control characters.
        // These can corrupt SQL databases or break UI rendering, so we reject them.
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            // Allow normal newlines (\n) and returns (\r), but reject other ASCII control characters.
            if (Character.isISOControl(c) && c != '\n' && c != '\r') {
                throw new IllegalArgumentException("AI suggestion contains invalid control character");
            }
        }

        return trimmed;
    }
}
