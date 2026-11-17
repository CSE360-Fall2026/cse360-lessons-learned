package database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Title: BrainstormService Class.
 * </p>
 *
 * <p>
 * Description: Handles external Groq API requests and JSON parsing for lesson coaching,
 * brainstorming, refinement, and keyword generation. Integrated into the shared AI
 * service (TP3 Aspect 8 / D2) using local config.properties key loading.
 * </p>
 *
 * <p>
 * Validated by: SharedAiServiceTests (JUnit).
 * </p>
 *
 * <p>
 * Copyright: Barrett Honors Contract Project © 2026
 * </p>
 *
 * @author Ariana Adrian
 * @author Virgil Jones and Team
 *
 * @version 2.03 2026-11-17 Emilia Davey: suggestLessonSearchTerms for Epic E2; Javadoc for every
 *          field and method
 * @version 2.02 2026-11-16 integrated into TP3 database package with config.properties key loading
 * @version 2.01 2026-06-27 retarget prompts to lessons-learned focus for AI training quality
 * @version 2.00 2026-05-04 swapped to Groq (OpenAI-compatible API)
 */
public class BrainstormService {

/** The Groq chat-completions endpoint (OpenAI-compatible). */
	private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
/** The Groq model used for every request. */
	private static final String GROQ_MODEL = "openai/gpt-oss-120b";

/** Instructions for brainstorming: ask one focused follow-up question per reply. */
	private static final String CHAT_SYSTEM_PROMPT = "You are a rigorous interviewer helping a user document a high-quality lesson "
			+ "learned from a real project experience. Your goal is to extract entries "
			+ "specific and honest enough to serve as training data for a future AI model. "
			+ "Ask exactly one focused question per reply. Push past surface-level answers: "
			+ "ask what specifically went wrong (not just that something did), what assumption "
			+ "turned out to be false, what the user would do differently and why, or what "
			+ "they now believe that they did not believe before. If an answer is vague, "
			+ "press for a concrete example or a specific moment. Never lecture, summarize, "
			+ "or offer advice. Keep replies to 2-3 sentences. Do not list takeaways unless " + "explicitly asked.";

/** Instructions for writing coaching: 3 or 4 bullet points of feedback. */
	private static final String COACH_SYSTEM_PROMPT = "You are a writing coach reviewing a 'lesson learned' post. "
			+ "Give 3-4 specific, actionable bullet points of feedback. "
			+ "Focus on: clarity of the lesson, specificity of details, "
			+ "what would make it more valuable to readers, and any missing context. "
			+ "Be direct and constructive. Start each bullet with a dash (-).";

/** Instructions for turning a brainstorming conversation into a JSON list of lessons. */
	private static final String EXTRACT_SYSTEM_PROMPT = "Based on the conversation that follows, extract between 3 and 7 distinct "
			+ "lessons learned. Requirements: each lesson must be specific to what was "
			+ "actually described (no generic advice that could apply to any project), "
			+ "written in the user's voice and first person where natural, and actionable "
			+ "enough that someone else could apply it. Prioritize lessons about false "
			+ "assumptions, specific decisions that backfired, and concrete things the user "
			+ "would do differently. Discard anything vague or obvious. "
			+ "Return ONLY a JSON array of strings, with no surrounding prose, no markdown "
			+ "fences, no keys. Example: [\"First lesson.\", \"Second lesson.\"]";

/**
 * Not used: every method is static, so this class is never instantiated.
 */
private BrainstormService() {
}
	// === Public API
	// ===========================================================================

/**
 * Reads the Groq API key from the local config.properties file, which is listed in .gitignore
 * so the key is never pushed (US-AI-D2-01).
 *
 * @return the key, or an empty String if the file or the key is missing
 */
	private static String loadApiKey() {
        try (java.io.FileInputStream fis = new java.io.FileInputStream("config.properties")) {
            java.util.Properties props = new java.util.Properties();
            props.load(fis);
            return props.getProperty("groq.api.key", "").trim();
        } catch (Exception e) {
            return "";
        }
    }

/**
 * Is an API key configured?  SharedAiService checks this before every request, so a missing
 * key gives the clear "unavailable" message instead of a failed request.
 *
 * @return true if config.properties holds a non-empty groq.api.key
 */
    public static boolean isConfigured() {
        return !loadApiKey().isEmpty();
    }

/**
 * Brainstorming: sends the conversation and returns the AI's next follow-up question.
 *
 * @param history the conversation so far, oldest first
 * @return the assistant's reply text
 * @throws Exception if the request fails or the provider reports an error
 */
	public static String chat(List<ChatMessage> history) throws Exception {
		String body = buildRequestBody(history, CHAT_SYSTEM_PROMPT);
		String response = postToGroq(body);
		return extractAssistantMessage(response);
	}

/**
 * Suggests three alternative keywords for a search query.
 *
 * @param query the user's search words
 * @return the suggested keywords (possibly empty if the reply could not be read)
 * @throws Exception if the request fails or the provider reports an error
 */
	public static List<String> suggestKeywords(String query) throws Exception {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage("user", query));
		String requestBody = buildRequestBody(msgs,
				"Suggest exactly 3 alternative search keywords or short phrases that would "
						+ "help find relevant lessons learned entries related to the user's query. "
						+ "Return ONLY a JSON array of 3 strings, no prose, no markdown fences. "
						+ "Example: [\"keyword one\", \"keyword two\", \"keyword three\"]");
		return parseJsonStringArray(extractAssistantMessage(postToGroq(requestBody)));
	}

	/**
	 * Suggests search terms that would help someone find this lesson, including words that do
	 * not appear in it (TP3 Aspect 5, Epic E2).  SharedAiService keeps each term within the
	 * 50-character limit and the whole list within 500 characters.
	 *
	 * @param title the lesson title
	 * @param description the lesson description
	 * @param tags the lesson tags
	 * @return the suggested terms (possibly empty if the reply could not be read)
	 * @throws Exception if the request fails or the provider reports an error
	 */
	public static List<String> suggestLessonSearchTerms(String title, String description, String tags)
			throws Exception {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage("user", "Title: " + title + "\n\nDescription:\n" + description
				+ "\n\nTags: " + (tags == null ? "" : tags)));
		String requestBody = buildRequestBody(msgs,
				"Suggest between 5 and 10 search terms that someone looking for this lesson learned "
						+ "might type, including synonyms and related words that do NOT appear in the "
						+ "lesson. Each term must be 1 to 4 words and at most 50 characters, with no "
						+ "commas. Return ONLY a JSON array of strings, no prose, no markdown fences. "
						+ "Example: [\"rollback\", \"deployment failure\", \"release checklist\"]");
		return parseJsonStringArray(extractAssistantMessage(postToGroq(requestBody)));
	}

/**
 * Summarizes a lesson learned in one sentence.
 *
 * @param title the lesson title
 * @param body the lesson text
 * @return the one-sentence summary
 * @throws Exception if the request fails or the provider reports an error
 */
	public static String summarizeLesson(String title, String body) throws Exception {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage("user", "Title: " + title + "\n\nBody: " + body));
		String requestBody = buildRequestBody(msgs,
				"Summarize this lesson learned in exactly one sentence. "
						+ "Be specific to what this lesson actually says — no generic advice. "
						+ "Return only the sentence, no preamble, no punctuation beyond the period.");
		return extractAssistantMessage(postToGroq(requestBody)).trim();
	}

/**
 * Writing coaching: returns 3 or 4 bullet points of feedback on a lesson draft.
 *
 * @param title the lesson title
 * @param body the lesson text
 * @return the coaching text
 * @throws Exception if the request fails or the provider reports an error
 */
	public static String coachEntry(String title, String body) throws Exception {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage("user", "Title: " + title + "\n\nLesson:\n" + body));
		String requestBody = buildRequestBody(msgs, COACH_SYSTEM_PROMPT);
		String response = postToGroq(requestBody);
		return extractAssistantMessage(response);
	}

/**
 * Turns a brainstorming conversation into 3 to 7 specific lessons learned.
 *
 * @param history the conversation, oldest first
 * @return the lessons (possibly empty if the reply could not be read)
 * @throws Exception if the request fails or the provider reports an error
 */
	public static List<String> extractTakeaways(List<ChatMessage> history) throws Exception {
		String body = buildRequestBody(history, EXTRACT_SYSTEM_PROMPT);
		String response = postToGroq(body);
		String text = extractAssistantMessage(response);
		return parseJsonStringArray(text);
	}

	// === HTTP plumbing
	// ========================================================================

/**
 * Sends one request to Groq with a 15-second connect and request timeout.
 *
 * @param body the JSON request body
 * @return the JSON response body
 * @throws Exception if the request fails, times out, or the status is not 200
 */
	private static String postToGroq(String body) throws Exception {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GROQ_URL)).timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + loadApiKey())
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
		if (resp.statusCode() != 200) {
			throw new RuntimeException("Groq API error " + resp.statusCode() + ": " + truncate(resp.body(), 300));
		}
		return resp.body();
	}

/**
 * Shortens text for an error message.
 *
 * @param s the text
 * @param n the most characters to keep
 * @return the text, cut to n characters with "..." if it was longer
 */
	private static String truncate(String s, int n) {
		if (s == null)
			return "";
		return s.length() <= n ? s : s.substring(0, n) + "...";
	}

	// === Request body construction
	// ============================================================

/**
 * Builds the JSON request: the model, the system prompt, then the conversation.  Every text is
 * escaped, so lesson text cannot change the structure of the request.
 *
 * @param history the conversation, oldest first
 * @param systemPrompt the instructions for this kind of request
 * @return the JSON request body
 */
	private static String buildRequestBody(List<ChatMessage> history, String systemPrompt) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"model\":\"").append(GROQ_MODEL).append("\",");
		sb.append("\"temperature\":0.7,");
		sb.append("\"max_tokens\":1024,");
		sb.append("\"messages\":[");
		// System prompt first
		sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"}");
		// Then conversation. Note: Groq uses "assistant" not "model"
		for (ChatMessage m : history) {
			String role = m.role.equals("model") ? "assistant" : m.role;
			sb.append(",{\"role\":\"").append(role).append("\",").append("\"content\":\"").append(escapeJson(m.text))
					.append("\"}");
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}

	// === Response parsing
	// =====================================================================

	// Pulls the assistant's content out of a Groq/OpenAI response without needing a
	// JSON lib.
	// Looks for the first "content" field after a "message" field.
/**
 * Reads the assistant's text out of a Groq response without a JSON library.
 *
 * @param json the JSON response body
 * @return the assistant's text, or an empty String if none was found
 */
	private static String extractAssistantMessage(String json) {
		int msgIdx = json.indexOf("\"message\"");
		int searchFrom = msgIdx >= 0 ? msgIdx : 0;
		int contentIdx = json.indexOf("\"content\"", searchFrom);
		if (contentIdx < 0)
			return "";
		int colon = json.indexOf(':', contentIdx);
		if (colon < 0)
			return "";
		int start = json.indexOf('"', colon + 1);
		if (start < 0)
			return "";
		StringBuilder out = new StringBuilder();
		boolean escape = false;
		for (int i = start + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (escape) {
				switch (c) {
				case 'n':
					out.append('\n');
					break;
				case 't':
					out.append('\t');
					break;
				case 'r':
					out.append('\r');
					break;
				case '"':
					out.append('"');
					break;
				case '\\':
					out.append('\\');
					break;
				case '/':
					out.append('/');
					break;
				case 'u':
					if (i + 4 < json.length()) {
						try {
							int code = Integer.parseInt(json.substring(i + 1, i + 5), 16);
							out.append((char) code);
							i += 4;
						} catch (NumberFormatException e) {
							out.append(c);
						}
					}
					break;
				default:
					out.append(c);
				}
				escape = false;
			} else if (c == '\\') {
				escape = true;
			} else if (c == '"') {
				return out.toString();
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

/**
 * Reads a JSON array of strings, even when the model wrapped it in a markdown fence.
 *
 * @param text the reply text
 * @return the strings, or an empty list if no array was found
 */
	private static List<String> parseJsonStringArray(String text) {
		List<String> out = new ArrayList<>();
		if (text == null)
			return out;
		String s = text.trim();
		if (s.startsWith("```")) {
			int firstNewline = s.indexOf('\n');
			if (firstNewline >= 0)
				s = s.substring(firstNewline + 1);
			if (s.endsWith("```"))
				s = s.substring(0, s.length() - 3);
			s = s.trim();
		}
		int open = s.indexOf('[');
		int close = s.lastIndexOf(']');
		if (open < 0 || close < 0 || close <= open)
			return out;
		s = s.substring(open + 1, close);
		StringBuilder current = new StringBuilder();
		boolean inString = false;
		boolean escape = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (escape) {
				switch (c) {
				case 'n':
					current.append('\n');
					break;
				case 't':
					current.append('\t');
					break;
				case '"':
					current.append('"');
					break;
				case '\\':
					current.append('\\');
					break;
				default:
					current.append(c);
				}
				escape = false;
			} else if (c == '\\') {
				escape = true;
			} else if (c == '"') {
				if (inString) {
					out.add(current.toString());
					current.setLength(0);
					inString = false;
				} else {
					inString = true;
				}
			} else if (inString) {
				current.append(c);
			}
		}
		return out;
	}

	// === JSON escaping
	// ========================================================================

/**
 * Escapes text for a JSON string value.
 *
 * @param s the text
 * @return the escaped text; an empty String for null
 */
	private static String escapeJson(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			default:
				if (c < 0x20)
					sb.append(String.format("\\u%04x", (int) c));
				else
					sb.append(c);
			}
		}
		return sb.toString();
	}

	// === Chat history type
	// ====================================================================

/**
 * One message of a brainstorming conversation.
 */
	public static class ChatMessage {
/** Who wrote it: "user" or "model" ("model" is sent to Groq as "assistant"). */
		public String role; // "user" or "model" (translated to "assistant" before sending)
/** The message text. */
		public String text;

/**
 * Creates one message.
 *
 * @param role "user" or "model"
 * @param text the message text
 */
		public ChatMessage(String role, String text) {
			this.role = role;
			this.text = text;
		}
	}
}