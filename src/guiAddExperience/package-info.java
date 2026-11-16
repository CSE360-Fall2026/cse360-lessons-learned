/*******
 * <p> The Add Experience Page (TP2 Task 4): a Contributor records a new attempt to reuse a lesson:
 * what was done and how, the time/effort in hours (US-U2), and optionally each team member's
 * share of the effort.  Invalid values are refused with a specific message and nothing is saved
 * (US-U2-V); the team members' hours may not add up to more than the total, and at most 20
 * members may be listed (design decision D31). </p>
 *
 * <p> Model-View-Controller, as in the Foundations GUI packages: ViewAddExperience,
 * ControllerAddExperience, and ModelAddExperience, which saves the entry through
 * ExperienceEntry.create(). </p>
 */
package guiAddExperience;
