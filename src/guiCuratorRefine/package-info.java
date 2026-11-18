/**
 * The Refine with AI page (TP3 Aspect 6, Epic E3: "As a lessons-learned curator, I can use an
 * AI tool to help me edit and refine lessons-learned details so others can better understand
 * the lesson"). A Curator opens it from the Curator Home Page with a lesson selected, asks the
 * AI to rewrite the title or the description, compares the original and the suggestion side by
 * side, edits or discards the suggestion, and saves the final wording after an explicit Yes.
 *
 * Model-View-Controller, as in the Foundations GUI packages: ViewCuratorRefine,
 * ControllerCuratorRefine, and Daniel Martinez Julio's ModelCuratorRefine, which checks the
 * Curator role, the lesson's revision, and the consent before every request and save, and
 * keeps the original contributor's text and the Curator's name and time. CuratorRefinementDemo
 * is Daniel's console demonstration of the Model with a fixed reply; docs/aspect6-prototype.md
 * describes the prototype and its test commands.
 */
package guiCuratorRefine;
