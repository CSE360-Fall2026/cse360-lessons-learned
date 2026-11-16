package lessonTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import database.SharedAiService;

/**
 * Unit tests for SharedAiService verifying untrusted AI content.
 * Tests Boundary Value Analysis and error handling for TP3 Requirement 4 (CR5).
 * 
 * @author Virgil Jones
 */
public class SharedAiServiceTests {

    private SharedAiService service;

    /**
     * Sets up a clean SharedAiService instance before each test.
     */
    @BeforeEach
    public void setUp() {
        service = new SharedAiService();
    }

    /**
     * Tests that clean, valid AI suggestions within the boundary return successfully.
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
}