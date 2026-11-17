/*******
 * <p> The Writing Help window (TP3 Aspect 7, Epic E1): opened from the Create Lesson and Edit
 * Lesson pages, it lets a Contributor brainstorm with the AI, which asks one follow-up question
 * at a time, add the takeaways they choose to the Description or Recommendation (US-P-E1-01,
 * US-P-E1-02), and search their own lessons for an example (US-P-E1-03). </p>
 *
 * <p> Model-View-Controller, as in the Foundations GUI packages: ViewWritingHelp,
 * ControllerWritingHelp, and ModelWritingHelp, which talks to the AI only through
 * database.SharedAiService, so the same per-user AI notice and consent apply.  DraftTarget is
 * how the window reads and changes the lesson page's draft without depending on either page. </p>
 */
package guiWritingHelp;
