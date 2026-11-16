package entityClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ExperienceEntry Class </p>
 *
 * <p> Description: This entity class is one "attempt to leverage a lesson" from User Story
 * US-U2: after a lesson has been reused, the Contributor records what was done, how it was done,
 * how long it took, and the effort each team member used (Lessons Learned System V1, Contributor
 * Phase Two).  Each entry belongs to exactly one lesson.  Only that lesson's id is stored rather
 * than a reference to the LessonsLearned object, so neither class depends on the internals of
 * the other.</p>
 *
 * <p> CRUD.  Experience entries are a historical record, so they support Create and Read only:
 * create() saves an entry and readForLesson() loads every entry for a lesson.  US-U2 asks only
 * for adding experience, and Phase Three Epic E4 merges these values into statistics, which
 * would be undermined if past attempts could be rewritten.  An entry is deleted only when its
 * lesson is deleted, by the ON DELETE CASCADE rule on the experienceDB table.</p>
 *
 * <p> Like the other entity classes, this class contains no SQL.  Persistence is delegated to
 * the shared Database instance in FoundationsMain, and validation is delegated to
 * LessonRecognizer, so the rules for typed input and stored objects are the same rules.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-09-29 Initial version for HW2
 * @version 2.00		2026-10-14 TP2: per-member effort, validation, and Create/Read methods
 */

public class ExperienceEntry {

	/** The largest number of team members that may be listed on one entry.  Rationale: a limit
	 * on every list the user controls, in the spirit of InputValidation.pdf's size limits. */
	public static final int MAX_MEMBERS = 20;

	/** Database primary key; 0 until the entry has been saved.  Source: Foundations pattern. */
	private int id;

	/** The lesson this entry belongs to.  Source: US-U2, "add experience information to an
	 * existing lesson learned." */
	private int lessonId;

	/** Total hours for this attempt.  Source: US-U2, "the time/effort it took in hours"; V1,
	 * "how long it took."  Rationale: a single unit (hours) so entries can be merged (Epic E4). */
	private double timeEffortHours;

	/** What was done and how.  Source: US-U2, "a description of what was done and how"; V1,
	 * "what was done, how it was done." */
	private String description;

	/** When the entry was recorded, in epoch milliseconds.  Rationale: lists entries in the
	 * order the attempts happened. */
	private long createdAt;

	/** Each team member's share of the effort.  Source: V1, "the effort used by the various team
	 * members."  Optional: an empty list means the effort was not broken down by member. */
	private List<MemberEffort> memberEfforts = new ArrayList<MemberEffort>();


	/*****
	 * <p> Method: ExperienceEntry() </p>
	 *
	 * <p> Description: This default constructor is not used in this system. </p>
	 */
	public ExperienceEntry() {

	}


	/*****
	 * <p> Method: ExperienceEntry(int lessonId, double timeEffortHours, String description) </p>
	 *
	 * <p> Description: Builds a new entry, without a per-member breakdown, that has not been
	 * saved yet.  The constructor stores what it is given; validate() decides whether it is
	 * acceptable. </p>
	 *
	 * @param lessonId the lesson this entry belongs to
	 *
	 * @param timeEffortHours how many hours the attempt took
	 *
	 * @param description what was done and how
	 */
	public ExperienceEntry(int lessonId, double timeEffortHours, String description) {
		this.lessonId = lessonId;
		this.timeEffortHours = timeEffortHours;
		this.description = description;
		this.createdAt = System.currentTimeMillis();
	}


	/*****
	 * <p> Method: ExperienceEntry(int lessonId, double timeEffortHours, String description,
	 * 		List&lt;MemberEffort&gt; memberEfforts) </p>
	 *
	 * <p> Description: TP2.  Builds a new entry that includes each team member's share. </p>
	 *
	 * @param lessonId the lesson this entry belongs to
	 *
	 * @param timeEffortHours how many hours the attempt took in total
	 *
	 * @param description what was done and how
	 *
	 * @param memberEfforts each team member's share; null is treated as an empty list
	 */
	public ExperienceEntry(int lessonId, double timeEffortHours, String description,
			List<MemberEffort> memberEfforts) {
		this(lessonId, timeEffortHours, description);
		this.memberEfforts = copyWithoutNulls(memberEfforts);
	}


	/*****
	 * <p> Method: ExperienceEntry(int id, int lessonId, double timeEffortHours,
	 * 		String description, long createdAt) </p>
	 *
	 * <p> Description: The full constructor Database.getExperienceForLesson() uses to rebuild an
	 * entry from a row.  The member shares are attached afterwards with setMemberEfforts(). </p>
	 *
	 * @param id the database primary key
	 * @param lessonId the lesson this entry belongs to
	 * @param timeEffortHours how many hours the attempt took
	 * @param description what was done and how
	 * @param createdAt epoch milliseconds when it was recorded
	 */
	public ExperienceEntry(int id, int lessonId, double timeEffortHours, String description,
			long createdAt) {
		this.id = id;
		this.lessonId = lessonId;
		this.timeEffortHours = timeEffortHours;
		this.description = description;
		this.createdAt = createdAt;
	}


	/*-*******************************************************************************************

	Input validation (US-U2-V)

	 */

	/*****
	 * <p> Method: String findInvalidField() </p>
	 *
	 * <p> Description: Names the first field that fails validation, checking in the order the
	 * fields appear on the screen: total time/effort, description, then the team members.  The
	 * names match the failedField values used by the Contributor Model, so a screen can show the
	 * message beneath the right input (InputValidation.pdf). </p>
	 *
	 * @return "experienceValue", "experienceDescription", "memberName", "memberHours",
	 * 		"memberEfforts", or an empty String if the entry is valid
	 */
	public String findInvalidField() {
		if (!LessonRecognizer.checkForValidExperienceValue(timeEffortHours).isEmpty())
			return "experienceValue";											// US-U2-V
		if (!LessonRecognizer.checkForValidExperienceDescription(description).isEmpty())
			return "experienceDescription";										// US-U2-V
		if (memberEfforts.size() > MAX_MEMBERS) return "memberEfforts";
		for (MemberEffort share : memberEfforts) {
			String field = share.findInvalidField();
			if (!field.isEmpty()) return field;
		}
		if (memberHoursExceedTotal()) return "memberEfforts";
		return "";
	}


	/*****
	 * <p> Method: String validate() </p>
	 *
	 * <p> Description: Checks the whole entry and returns the message for the first problem
	 * found, in the same order as findInvalidField().  Beyond the per-field rules, two rules
	 * cover the member list as a whole: at most MAX_MEMBERS shares, and the shares may not add
	 * up to more than the total time/effort (they may add up to less, because a Contributor may
	 * choose to list only some members). </p>
	 *
	 * @return an empty String if the entry is valid; otherwise, a specific error message
	 */
	public String validate() {
		String field = findInvalidField();
		switch (field) {
			case "": return "";
			case "experienceValue":
				return LessonRecognizer.checkForValidExperienceValue(timeEffortHours);
			case "experienceDescription":
				return LessonRecognizer.checkForValidExperienceDescription(description);
			case "memberEfforts":
				if (memberEfforts.size() > MAX_MEMBERS)
					return "No more than " + MAX_MEMBERS + " team members can be listed.";
				return "Team member hours cannot add up to more than the total time/effort.";
			default:
				// memberName or memberHours: report the first share that is invalid
				for (MemberEffort share : memberEfforts) {
					String error = share.validate();
					if (!error.isEmpty()) return error;
				}
				return "";
		}
	}


	/*****
	 * <p> Method: boolean memberHoursExceedTotal() </p>
	 *
	 * <p> Description: True if the member shares add up to more than the total.  A tiny
	 * tolerance is allowed because adding decimal hours as doubles is not exact (0.1 + 0.2 is
	 * slightly more than 0.3), and the user should not be told that 0.1 + 0.2 exceeds 0.3. </p>
	 *
	 * @return true if the shares exceed the total time/effort
	 */
	private boolean memberHoursExceedTotal() {
		double sum = 0;
		for (MemberEffort share : memberEfforts) sum += share.getHours();
		return sum > timeEffortHours + 1e-9;
	}


	/*-*******************************************************************************************

	Persistence (Create and Read only, see the class description)

	 */

	/*****
	 * <p> Method: int create() </p>
	 *
	 * <p> Description: CREATE.  Saves this entry and all of its member shares in one database
	 * transaction (US-U2), then records the new id on this object.  The entry is validated first
	 * and refused if invalid, so an invalid entry can never reach the database even if a caller
	 * forgets to validate (US-U2-V: "nothing is saved"). </p>
	 *
	 * @return the id the database gave the new entry, or -1 if the save failed
	 *
	 * @throws IllegalArgumentException if the entry is invalid; the message says why
	 */
	public int create() {
		String error = validate();
		if (!error.isEmpty()) throw new IllegalArgumentException(error);
		int newId = applicationMain.FoundationsMain.database.createExperience(this);
		if (newId > 0) id = newId;
		return newId;
	}


	/*****
	 * <p> Method: List&lt;ExperienceEntry&gt; readForLesson(int lessonId) </p>
	 *
	 * <p> Description: READ.  Loads every experience entry for one lesson, oldest first, each
	 * with its member shares (US-R3: viewing a lesson shows its experience entries).  Checking
	 * that the requesting user owns the lesson is the Model's job and happens before this is
	 * called. </p>
	 *
	 * @param lessonId the lesson whose entries should be loaded
	 *
	 * @return the entries, oldest first; an empty list (never null) if there are none
	 */
	public static List<ExperienceEntry> readForLesson(int lessonId) {
		return applicationMain.FoundationsMain.database.getExperienceForLesson(lessonId);
	}


	/*-*******************************************************************************************

	Getters and setters

	 */

	/*****
	 * <p> Method: int getId() </p>
	 *
	 * <p> Description: Returns the database primary key (0 if not yet saved). </p>
	 *
	 * @return the id
	 */
	public int getId() { return id; }


	/*****
	 * <p> Method: int getLessonId() </p>
	 *
	 * <p> Description: Returns the id of the lesson this entry belongs to. </p>
	 *
	 * @return the lesson id
	 */
	public int getLessonId() { return lessonId; }


	/*****
	 * <p> Method: double getTimeEffortHours() </p>
	 *
	 * <p> Description: Returns the total hours for this attempt. </p>
	 *
	 * @return the hours
	 */
	public double getTimeEffortHours() { return timeEffortHours; }


	/*****
	 * <p> Method: String getDescription() </p>
	 *
	 * <p> Description: Returns what was done and how. </p>
	 *
	 * @return the description
	 */
	public String getDescription() { return description; }


	/*****
	 * <p> Method: long getCreatedAt() </p>
	 *
	 * <p> Description: Returns when the entry was recorded (epoch milliseconds). </p>
	 *
	 * @return the creation time
	 */
	public long getCreatedAt() { return createdAt; }


	/*****
	 * <p> Method: List&lt;MemberEffort&gt; getMemberEfforts() </p>
	 *
	 * <p> Description: Returns the team members' shares.  The list is read-only so the shares
	 * cannot be changed behind validate()'s back; use setMemberEfforts() to replace them. </p>
	 *
	 * @return the shares, never null
	 */
	public List<MemberEffort> getMemberEfforts() {
		return Collections.unmodifiableList(memberEfforts);
	}


	/*****
	 * <p> Method: void setMemberEfforts(List&lt;MemberEffort&gt; memberEfforts) </p>
	 *
	 * <p> Description: Replaces the team members' shares.  A copy is kept, so later changes to
	 * the caller's list do not change this entry. </p>
	 *
	 * @param memberEfforts the shares; null is treated as an empty list
	 */
	public void setMemberEfforts(List<MemberEffort> memberEfforts) {
		this.memberEfforts = copyWithoutNulls(memberEfforts);
	}


	/*****
	 * <p> Method: List&lt;MemberEffort&gt; copyWithoutNulls(List&lt;MemberEffort&gt; shares) </p>
	 *
	 * <p> Description: Makes this entry's private copy of a list of shares.  Null elements are
	 * skipped, so validate() never has to guard against them, and a null list becomes an empty
	 * one.  A static helper (rather than calling setMemberEfforts() from the constructor) keeps
	 * the constructor from calling a method a subclass could override. </p>
	 *
	 * @param shares the shares to copy, possibly null
	 *
	 * @return a new list holding the non-null shares
	 */
	private static List<MemberEffort> copyWithoutNulls(List<MemberEffort> shares) {
		List<MemberEffort> copy = new ArrayList<MemberEffort>();
		if (shares != null)
			for (MemberEffort share : shares)
				if (share != null) copy.add(share);
		return copy;
	}
}
