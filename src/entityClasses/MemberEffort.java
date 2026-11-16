package entityClasses;

import inputValidation.LessonRecognizer;

/*******
 * <p> Title: MemberEffort Class </p>
 *
 * <p> Description: This entity class is one team member's share of the effort recorded on an
 * ExperienceEntry.  The Lessons Learned System V1 Contributor story states that experience
 * information provides "what was done, how it was done, how long it took, and the effort used by
 * the various team members."  An ExperienceEntry already records the total time and the
 * description; this class records the last part of that sentence, one row per team member.</p>
 *
 * <p> Recording effort per member, rather than only a total, also supports the Phase Three
 * Curator epic E4 (merge time and effort data into a statistically useful form), because the
 * individual values are available to be combined later.</p>
 *
 * <p> Following the pattern of the other entity classes, this class holds data and offers
 * validation, but contains no SQL.  It is saved and loaded together with its ExperienceEntry by
 * the Database class, inside the same transaction, so an entry is never stored without its
 * member rows (or the reverse).</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2
 */

public class MemberEffort {

	/** Database primary key; 0 until the row has been saved.  Source: the Foundations pattern of
	 * an AUTO_INCREMENT id on every table. */
	private int id;

	/** The id of the ExperienceEntry this share belongs to; 0 until that entry has been saved.
	 * Only the id is stored, not an object reference, so the two classes stay independent. */
	private int experienceId;

	/** The team member's name as typed by the Contributor.  Source: V1 Contributor story, "the
	 * effort used by the various team members."  Rationale: a name, not a username, because team
	 * members who contributed effort do not necessarily have accounts in this system. */
	private String memberName;

	/** The hours this member contributed.  Source: same V1 sentence.  Rationale: hours, the
	 * same unit as ExperienceEntry.timeEffortHours, so shares can be compared with the total. */
	private double hours;


	/*****
	 * <p> Method: MemberEffort() </p>
	 *
	 * <p> Description: This default constructor is not used in this system. </p>
	 */
	public MemberEffort() {

	}


	/*****
	 * <p> Method: MemberEffort(String memberName, double hours) </p>
	 *
	 * <p> Description: Builds a new share that has not been saved yet. </p>
	 *
	 * @param memberName the team member's name
	 *
	 * @param hours the hours that member contributed
	 */
	public MemberEffort(String memberName, double hours) {
		this.memberName = memberName;
		this.hours = hours;
	}


	/*****
	 * <p> Method: MemberEffort(int id, int experienceId, String memberName, double hours) </p>
	 *
	 * <p> Description: The full constructor the Database uses to rebuild a share from a row. </p>
	 *
	 * @param id the database primary key
	 * @param experienceId the ExperienceEntry this share belongs to
	 * @param memberName the team member's name
	 * @param hours the hours that member contributed
	 */
	public MemberEffort(int id, int experienceId, String memberName, double hours) {
		this.id = id;
		this.experienceId = experienceId;
		this.memberName = memberName;
		this.hours = hours;
	}


	/*****
	 * <p> Method: String validate() </p>
	 *
	 * <p> Description: Checks this share against the input validation rules: the name first,
	 * then the hours, using the same LessonRecognizer rules that apply to typed input. </p>
	 *
	 * @return an empty String if the share is valid; otherwise, the first error message
	 */
	public String validate() {
		String error = LessonRecognizer.checkForValidMemberName(memberName);
		if (!error.isEmpty()) return error;
		return LessonRecognizer.checkForValidExperienceValue(hours);
	}


	/*****
	 * <p> Method: String findInvalidField() </p>
	 *
	 * <p> Description: Names the field validate() would complain about, so a screen can place
	 * the message beneath the right input, as InputValidation.pdf requires. </p>
	 *
	 * @return "memberName", "memberHours", or an empty String if the share is valid
	 */
	public String findInvalidField() {
		if (!LessonRecognizer.checkForValidMemberName(memberName).isEmpty()) return "memberName";
		if (!LessonRecognizer.checkForValidExperienceValue(hours).isEmpty()) return "memberHours";
		return "";
	}


	/*****
	 * <p> Method: int getId() </p>
	 *
	 * <p> Description: Returns the database primary key (0 if not yet saved). </p>
	 *
	 * @return the id
	 */
	public int getId() { return id; }


	/*****
	 * <p> Method: int getExperienceId() </p>
	 *
	 * <p> Description: Returns the id of the ExperienceEntry this share belongs to. </p>
	 *
	 * @return the experience entry id
	 */
	public int getExperienceId() { return experienceId; }


	/*****
	 * <p> Method: void setExperienceId(int experienceId) </p>
	 *
	 * <p> Description: Records the owning entry's id once that entry has been saved.  Called by
	 * the Database inside the same transaction that saves the entry. </p>
	 *
	 * @param experienceId the id the database gave the ExperienceEntry
	 */
	public void setExperienceId(int experienceId) { this.experienceId = experienceId; }


	/*****
	 * <p> Method: String getMemberName() </p>
	 *
	 * <p> Description: Returns the team member's name. </p>
	 *
	 * @return the name
	 */
	public String getMemberName() { return memberName; }


	/*****
	 * <p> Method: double getHours() </p>
	 *
	 * <p> Description: Returns the hours this member contributed. </p>
	 *
	 * @return the hours
	 */
	public double getHours() { return hours; }
}
