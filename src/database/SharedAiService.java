package database;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Shared AI service for TP3 Aspect 8 (D2).
 * Enforces one time user consent (US-AI-D2-01), catches API failures safely,
 * and validates all AI responses as untrusted text (US-AI-D2-02).
 *
 * @author Virgil Jones
 */
public class SharedAiService {

    public static final String UNAVAILABLE_MESSAGE =
            "The coach is unavailable right now. You can keep writing.";
    public static final String CONSENT_DECLINED_MESSAGE =
            "AI assistance was declined. You can continue writing manually.";

    private static boolean userConsented = false;

    public SharedAiService() {
    }

    public static boolean hasUserConsented() {
        return userConsented;
    }

    public static void setUserConsented(boolean consented) {
        userConsented = consented;
    }

    /**
     * Shows a one time consent dialog before sending lesson text to the AI service.
     */
    public boolean requestConsentIfNeeded() {
        if (userConsented) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("AI Assistance Notice");
        alert.setHeaderText("Send lesson text to external AI service?");
        alert.setContentText("Your lesson text will be sent to Groq to generate suggestions. "
                + "Do not include passwords or sensitive data.");

        ButtonType ok = new ButtonType("I understand, continue", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ok) {
            userConsented = true;
            return true;
        }
        return false;
    }

    /**
     * Requests writing coaching for a lesson and cleans the untrusted reply.
     */
    public String requestCoaching(String title, String description, int maxLength) {
        if (!userConsented) {
            throw new IllegalStateException(CONSENT_DECLINED_MESSAGE);
        }
        if (!BrainstormService.isConfigured()) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
        try {
            String raw = BrainstormService.coachEntry(title, description);
            return cleanAiSuggestion(raw, maxLength);
        } catch (Exception e) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
    }

    /**
     * Requests keyword suggestions for a lesson and cleans each returned term.
     */
    public List<String> requestSearchTerms(String query) {
        if (!userConsented) {
            throw new IllegalStateException(CONSENT_DECLINED_MESSAGE);
        }
        if (!BrainstormService.isConfigured()) {
            throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        }
        try {
            List<String> terms = BrainstormService.suggestKeywords(query);
            for (int i = 0; i < terms.size(); i++) {
                terms.set(i, cleanAiSuggestion(terms.get(i), 100));
            }
            return terms;
        } catch (Exception e) {
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