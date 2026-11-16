package database;

/**
 * Service to clean and validate untrusted output from external AI services.
 * Satisfies TP3 Aspect 8 Requirement 4 (CR5).
 * 
 * @author Virgil Jones
 */
public class SharedAiService {

    /**
     * Default constructor.
     */
    public SharedAiService() {
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