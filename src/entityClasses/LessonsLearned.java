package entityClasses;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import inputValidation.LessonRecognizer;

/*******
 * <p> Title: LessonsLearned Class </p>
 *
 * <p> Description: This entity class is one persistent lesson learned.  It holds every piece of
 * information the system keeps about a lesson and provides the Create, Read, Update, and Delete
 * (CRUD) and input validation operations needed by the Contributor User Stories (Phase Two).  It
 * also holds the attributes that the Phase Three epics will need, so that TP3 can add the
 * Curator and AI features without changing the shape of this class or migrating the
 * database again.</p>
 *
 * <p> <b>Attributes.</b>  The attributes fall into five groups.  Each attribute's own Javadoc
 * gives its source and the rationale for including it.</p>
 * <ul>
 * <li> Core information, entered by the Contributor (US-C1): title, description, tags,
 * 		category.  Together with the id, owner, and timestamps these are the "core
 * 		information" of the V1 Contributor story.</li>
 * <li> Lock flags (V1 Contributor; US-R3, US-U1-V, US-D1-V; Epic E6): one per core field plus
 * 		one for the whole lesson.  Contributors can only read them.</li>
 * <li> Experience entries (US-U2, US-R3): loaded on demand, stored in their own table.</li>
 * <li> AI coaching fields (AICoaching.pdf; Epic E1): root cause, recommendation, and a flag
 * 		recording that AI assistance was used.</li>
 * <li> Curator fields (Epics E2 to E5): search terms, who curated the lesson and when, the
 * 		original text before curation, and the statistics summary and its visibility.</li>
 * </ul>
 *
 * <p> <b>Operations.</b>  The class offers instance and static methods for each CRUD action:
 * create() (US-C1), read() (US-R3), update() (US-U1), delete() (US-D1), plus
 * loadExperienceEntries() (US-R3), updateCuratorFields() and setLock() (Phase Three Curator,
 * used now by tests and demos), and the validation methods validate(), findInvalidField(),
 * and validateField() (US-C1-V, US-U1-V).</p>
 *
 * <p> <b>Design.</b>  Like entityClasses.User, this class contains no SQL.  Every CRUD method
 * delegates to the single shared Database instance in FoundationsMain, and every validation
 * method delegates to inputValidation.LessonRecognizer.  This keeps the Foundations layering
 * intact (SQL only in the database package, validation rules only in the inputValidation
 * package) while giving callers one object that knows how to save, load, and check itself.
 * Authorization (does the requesting user own this lesson?) is deliberately NOT done here; it
 * belongs to the GUI Model that knows who is logged in, which checks it before calling these
 * methods.</p>
 *
 * <p> <b>How the Contributor User Stories are enabled.</b></p>
 * <ul>
 * <li> US-C1 / US-C1-V: the five-argument constructor, findInvalidField() and validate(), then
 * 		create(), which refuses invalid data so nothing invalid is ever saved.</li>
 * <li> US-R1 / US-R4: ownerUserName, used by LessonsLearnedList.loadByOwner() so a
 * 		Contributor's list contains only that Contributor's lessons.</li>
 * <li> US-R3 / US-R3-V: read() returns null for a missing lesson; isFieldLocked() and the lock
 * 		getters tell a screen which fields to show as read-only; loadExperienceEntries().</li>
 * <li> US-U1 / US-U1-V: the setters, isFieldLocked(), validate(), and update().</li>
 * <li> US-U2 / US-U2-V: experience entries are created through ExperienceEntry and loaded here.</li>
 * <li> US-D1 / US-D1-V: isLocked() (a lesson with any locked field cannot be deleted) and
 * 		delete().</li>
 * </ul>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-09-29 Initial version for HW2 (as LessonLearned)
 * @version 2.00		2026-10-14 TP2: renamed LessonsLearned; CRUD and validation operations;
 * 							AI coaching and Curator attributes for Phase Three
 */

public class LessonsLearned {

	/*-*******************************************************************************************

	Field names

	These names are used by findInvalidField(), validateField(), setLock(), and isFieldLocked(),
	and by the GUI Model to place an error message beneath the right input.  Defining them once
	keeps a misspelled field name from silently meaning "no field".

	 */

	/** Field name for the title. */
	public static final String FIELD_TITLE = "title";
	/** Field name for the description. */
	public static final String FIELD_DESCRIPTION = "description";
	/** Field name for the tags. */
	public static final String FIELD_TAGS = "tags";
	/** Field name for the category. */
	public static final String FIELD_CATEGORY = "category";
	/** Field name for the root cause (AI coaching field). */
	public static final String FIELD_ROOT_CAUSE = "rootCause";
	/** Field name for the recommendation (AI coaching field). */
	public static final String FIELD_RECOMMENDATION = "recommendation";
	/** Field name for the curator search terms. */
	public static final String FIELD_SEARCH_TERMS = "searchTerms";
	/** Lock scope that locks the whole lesson rather than one field. */
	public static final String LOCK_ENTIRE = "entire";


	/*-*******************************************************************************************

	Attributes: identity and core information (US-C1)

	 */

	/** Database primary key; 0 until the lesson has been saved.  Source: the Foundations pattern
	 * of an AUTO_INCREMENT id.  Rationale: a stable handle for selecting a lesson from a list
	 * (US-R3) that does not change when the title is edited. */
	private int id;

	/** Username of the Contributor who created the lesson.  Source: US-R1 ("lessons I have
	 * added") and US-R4 ("I can only see the lessons learned that I have added").  Rationale: the
	 * value every ownership check compares against; it is set once and has no setter. */
	private String ownerUserName;

	/** Short name of the lesson.  Source: US-C1 core information.  Rationale: what appears in
	 * the Contributor's list and what US-R2 searches. */
	private String title;

	/** Full explanation of the lesson.  Source: US-C1 core information. */
	private String description;

	/** Comma-separated keywords chosen by the Contributor.  Source: US-C1 core information.
	 * Rationale: lets lessons be grouped and found by topic. */
	private String tags;

	/** One of LessonRecognizer.VALID_CATEGORIES.  Source: US-C1 core information.  Rationale: a
	 * fixed list (rather than free text) keeps categories consistent across Contributors. */
	private String category;


	/*-*******************************************************************************************

	Attributes: lock flags (V1 Contributor story; US-R3, US-U1-V, US-D1-V; Epic E6)

	 */

	/** True if a Curator has locked the title.  Source: V1, "other roles may lock one or more
	 * fields"; US-U1-V.  Rationale: one flag per core field, because Epic E6 locks "specific
	 * fields of a lesson". */
	private boolean lockedTitle;

	/** True if a Curator has locked the description.  Source: as lockedTitle. */
	private boolean lockedDescription;

	/** True if a Curator has locked the tags.  Source: as lockedTitle. */
	private boolean lockedTags;

	/** True if a Curator has locked the category.  Source: as lockedTitle. */
	private boolean lockedCategory;

	/** True if a Curator has locked the whole lesson.  Source: Epic E6, "lock entire lessons";
	 * US-D1-V.  Rationale: separate from the field flags so that locking everything and locking
	 * every field can be told apart, and so a new field added later is covered automatically. */
	private boolean lockedEntire;


	/*-*******************************************************************************************

	Attributes: timestamps

	 */

	/** When the lesson was created, in epoch milliseconds.  Rationale: the Contributor's list is
	 * shown newest first, and the value supports auditing. */
	private long createdAt;

	/** When the core information was last changed, in epoch milliseconds.  Rationale: shows
	 * whether a lesson has been revised since it was written. */
	private long updatedAt;


	/*-*******************************************************************************************

	Attributes: experience (US-U2, US-R3)

	 */

	/** The experience entries for this lesson.  Source: US-U2, US-R3 ("view its full details,
	 * including its experience entries").  Rationale: not loaded with every lesson, so reading a
	 * list costs one query; loadExperienceEntries() fills it when one lesson is opened. */
	private List<ExperienceEntry> experienceEntries = new ArrayList<ExperienceEntry>();


	/*-*******************************************************************************************

	Attributes: AI coaching fields (AICoaching.pdf; Epic E1).  Stored now, shown in TP3.

	 */

	/** What went wrong and why.  Source: AICoaching.pdf, which selects Root Cause as an input
	 * field for AI coaching; Epic E1, "include critical information."  Rationale: a lesson that
	 * records only what happened, not why, is hard to reuse.  Optional; empty by default. */
	private String rootCause = "";

	/** What to do differently next time.  Source: AICoaching.pdf (Recommendation field); Epic E1.
	 * Rationale: makes a lesson actionable for the next reader.  Optional; empty by default. */
	private String recommendation = "";

	/** True if the Contributor used an AI tool while writing this lesson.  Source: Epic E1; the
	 * HW2 LessonShare evaluation, which recommended keeping LessonShare's "AI-assisted" label for
	 * transparency.  Rationale: readers can see that the text was AI-assisted. */
	private boolean aiAssisted;


	/*-*******************************************************************************************

	Attributes: Curator fields (Epics E2 to E5).  Never shown to or changed by a Contributor.

	 */

	/** Comma-separated search terms a Curator adds so the lesson can be found by words it does
	 * not contain.  Source: Epic E2.  Rationale: V1 says a Contributor cannot see information
	 * other roles add, so these live apart from the Contributor's own tags. */
	private String searchTerms = "";

	/** Username of the Curator who last edited the lesson; empty if never curated.  Source: Epic
	 * E3, which records "who edited it and when"; V1, "content that I have added after a curator
	 * has edited it."  Rationale: supports an "edited by curator" note. */
	private String curatedBy = "";

	/** When the lesson was last curated, in epoch milliseconds; 0 if never.  Source: Epic E3. */
	private long curatedAt;

	/** The Contributor's title as it was before the first curator edit; empty if never curated.
	 * Source: Epic E3 open decision "whether the original text is kept."  Rationale: keeping it
	 * lets a curated edit be compared with, or restored to, what the Contributor wrote. */
	private String originalTitle = "";

	/** The Contributor's description as it was before the first curator edit.  Source and
	 * rationale: as originalTitle. */
	private String originalDescription = "";

	/** The id of the merged time-and-effort statistics stored for this lesson; -1 if none.
	 * Source: Epic E4, which saves the summary "on the target lesson."  Rationale: a reference
	 * rather than copies of the numbers, so one summary has one home. */
	private int summaryId = -1;

	/** True if the Curator has made the statistics visible to the Contributor.  Source: Epic E5.
	 * Rationale: hidden by default (the Epics.pdf proposal) so the Curator reviews the numbers
	 * before a Contributor sees them. */
	private boolean statsVisibleToContributor;


	/*-*******************************************************************************************

	Constructors

	 */

	/*****
	 * <p> Method: LessonsLearned() </p>
	 *
	 * <p> Description: This default constructor is not used in this system. </p>
	 */
	public LessonsLearned() {

	}


	/*****
	 * <p> Method: LessonsLearned(String ownerUserName, String title, String description,
	 * 		String tags, String category) </p>
	 *
	 * <p> Description: Builds a brand new lesson that has never been saved (US-C1): there is no
	 * id yet, nothing is locked (only a Curator locks fields), and both timestamps are "now".
	 * The values are stored exactly as given; findInvalidField() and validate() decide whether
	 * they are acceptable, and create() refuses to save them if they are not. </p>
	 *
	 * @param ownerUserName the username of the Contributor who owns this lesson
	 *
	 * @param title the lesson's title
	 *
	 * @param description the lesson's description
	 *
	 * @param tags comma-separated tags (may be empty)
	 *
	 * @param category one of LessonRecognizer.VALID_CATEGORIES
	 */
	public LessonsLearned(String ownerUserName, String title, String description, String tags,
			String category) {
		this.ownerUserName = ownerUserName;
		this.title = title;
		this.description = description;
		this.tags = tags;
		this.category = category;
		long now = System.currentTimeMillis();
		this.createdAt = now;
		this.updatedAt = now;
	}


	/*****
	 * <p> Method: LessonsLearned(int id, String ownerUserName, String title, String description,
	 * 		String tags, String category, boolean lockedTitle, boolean lockedDescription,
	 * 		boolean lockedTags, boolean lockedCategory, boolean lockedEntire, long createdAt,
	 * 		long updatedAt) </p>
	 *
	 * <p> Description: The constructor the Database uses to rebuild a lesson's identity, core
	 * information, locks, and timestamps from a row.  The Phase Three attributes are then filled
	 * in with their setters, which keeps this constructor's parameter list manageable. </p>
	 *
	 * @param id the database primary key
	 * @param ownerUserName the owner's username
	 * @param title the title
	 * @param description the description
	 * @param tags the tags
	 * @param category the category
	 * @param lockedTitle is the title locked
	 * @param lockedDescription is the description locked
	 * @param lockedTags are the tags locked
	 * @param lockedCategory is the category locked
	 * @param lockedEntire is the whole lesson locked
	 * @param createdAt epoch milliseconds when it was created
	 * @param updatedAt epoch milliseconds when it was last updated
	 */
	public LessonsLearned(int id, String ownerUserName, String title, String description,
			String tags, String category, boolean lockedTitle, boolean lockedDescription,
			boolean lockedTags, boolean lockedCategory, boolean lockedEntire, long createdAt,
			long updatedAt) {
		this.id = id;
		this.ownerUserName = ownerUserName;
		this.title = title;
		this.description = description;
		this.tags = tags;
		this.category = category;
		this.lockedTitle = lockedTitle;
		this.lockedDescription = lockedDescription;
		this.lockedTags = lockedTags;
		this.lockedCategory = lockedCategory;
		this.lockedEntire = lockedEntire;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}


	/*-*******************************************************************************************

	Input validation (US-C1-V, US-U1-V)

	 */

	/*****
	 * <p> Method: String validateField(String fieldName, String value) </p>
	 *
	 * <p> Description: Checks one value against the rule for one field, by delegating to
	 * LessonRecognizer.  A screen can use this to check a field as the user types or leaves it,
	 * which User Experience.pdf calls "validation-as-guidance". </p>
	 *
	 * @param fieldName one of the FIELD_ constants
	 *
	 * @param value the value to check, exactly as typed
	 *
	 * @return an empty String if the value is valid; otherwise, a specific error message
	 *
	 * @throws IllegalArgumentException if fieldName is not one of the FIELD_ constants, because
	 * 		that is a programming error, not a user error
	 */
	public static String validateField(String fieldName, String value) {
		switch (fieldName) {
			case FIELD_TITLE: return LessonRecognizer.checkForValidTitle(value);
			case FIELD_DESCRIPTION: return LessonRecognizer.checkForValidDescription(value);
			case FIELD_TAGS: return LessonRecognizer.checkForValidTags(value);
			case FIELD_CATEGORY: return LessonRecognizer.checkForValidCategory(value);
			case FIELD_ROOT_CAUSE: return LessonRecognizer.checkForValidRootCause(value);
			case FIELD_RECOMMENDATION: return LessonRecognizer.checkForValidRecommendation(value);
			case FIELD_SEARCH_TERMS: return LessonRecognizer.checkForValidSearchTerms(value);
			default: throw new IllegalArgumentException("Unknown lesson field: " + fieldName);
		}
	}


	/*****
	 * <p> Method: String findInvalidField() </p>
	 *
	 * <p> Description: Names the first field holding an invalid value, checked in screen order:
	 * title, description, tags, category, then the Phase Three fields.  The order matters
	 * because only one message is shown at a time, and the user should fix fields from the top
	 * (Test Case "Create Lesson - Invalid Input Blocks the Save"). </p>
	 *
	 * @return one of the FIELD_ constants, or an empty String if every field is valid
	 */
	public String findInvalidField() {
		String[] order = { FIELD_TITLE, FIELD_DESCRIPTION, FIELD_TAGS, FIELD_CATEGORY,
				FIELD_ROOT_CAUSE, FIELD_RECOMMENDATION, FIELD_SEARCH_TERMS };
		for (String field : order)
			if (!validateField(field, getFieldValue(field)).isEmpty()) return field;
		return "";
	}


	/*****
	 * <p> Method: String validate() </p>
	 *
	 * <p> Description: Checks the whole lesson and returns the message for the first invalid
	 * field (see findInvalidField() for the order). </p>
	 *
	 * @return an empty String if the lesson is valid; otherwise, a specific error message
	 */
	public String validate() {
		String field = findInvalidField();
		return field.isEmpty() ? "" : validateField(field, getFieldValue(field));
	}


	/*****
	 * <p> Method: void trimTextFields() </p>
	 *
	 * <p> Description: Removes leading and trailing spaces from the free-text fields and turns
	 * null into an empty String, as InputValidation.pdf specifies for free text.  The category is
	 * not trimmed because it must exactly match a fixed value.  Callers validate the text as
	 * typed (so lengths are measured as typed), call this, and then validate again: trimming can
	 * make a padded description shorter than its minimum, and validate() catches that before
	 * anything is saved. </p>
	 */
	public void trimTextFields() {
		title = trimmed(title);
		description = trimmed(description);
		tags = trimmed(tags);
		rootCause = trimmed(rootCause);
		recommendation = trimmed(recommendation);
		searchTerms = trimmed(searchTerms);
	}


	/*****
	 * <p> Method: String getFieldValue(String fieldName) </p>
	 *
	 * <p> Description: Returns the current value of a validated text field, so the validation
	 * loop above can treat every field the same way. </p>
	 *
	 * @param fieldName one of the FIELD_ constants
	 *
	 * @return the field's current value
	 */
	private String getFieldValue(String fieldName) {
		switch (fieldName) {
			case FIELD_TITLE: return title;
			case FIELD_DESCRIPTION: return description;
			case FIELD_TAGS: return tags;
			case FIELD_CATEGORY: return category;
			case FIELD_ROOT_CAUSE: return rootCause;
			case FIELD_RECOMMENDATION: return recommendation;
			case FIELD_SEARCH_TERMS: return searchTerms;
			default: throw new IllegalArgumentException("Unknown lesson field: " + fieldName);
		}
	}


	/*****
	 * <p> Method: String trimmed(String text) </p>
	 *
	 * <p> Description: Null-safe trim used by trimTextFields(). </p>
	 *
	 * @param text the text to trim, possibly null
	 *
	 * @return the trimmed text, or an empty String for null
	 */
	private static String trimmed(String text) {
		return (text == null) ? "" : text.trim();
	}


	/*-*******************************************************************************************

	Persistence: Create, Read, Update, Delete

	Each method delegates the SQL to the shared Database instance.  The Database is reached
	through FoundationsMain each time instead of being cached in a static field, so that a test
	that connects the database after this class is loaded still works.

	 */

	/*****
	 * <p> Method: int create() throws SQLException </p>
	 *
	 * <p> Description: CREATE (US-C1).  Saves this new lesson and records the id the database
	 * gave it.  The lesson is validated first and refused if invalid, so invalid data can never
	 * be stored even if a caller forgets to validate (US-C1-V: "nothing gets saved"). </p>
	 *
	 * @return the id of the new lesson
	 *
	 * @throws IllegalArgumentException if the lesson is invalid; the message says why
	 *
	 * @throws SQLException if the database cannot save the lesson, following the convention of
	 * 		Database.register(), so the caller decides what a failed save means
	 */
	public int create() throws SQLException {
		String error = validate();												// US-C1-V
		if (!error.isEmpty()) throw new IllegalArgumentException(error);
		id = applicationMain.FoundationsMain.database.createLesson(this);		// US-C1
		return id;
	}


	/*****
	 * <p> Method: LessonsLearned read(int lessonId) </p>
	 *
	 * <p> Description: READ (US-R3).  Loads one lesson by id, without its experience entries
	 * (call loadExperienceEntries() for those).  A missing lesson comes back as null, which the
	 * Model reports as "Lesson not found." (US-R3-V). </p>
	 *
	 * @param lessonId the id of the lesson to load
	 *
	 * @return the lesson, or null if there is no such lesson
	 */
	public static LessonsLearned read(int lessonId) {
		return applicationMain.FoundationsMain.database.getLessonById(lessonId);
	}


	/*****
	 * <p> Method: List&lt;ExperienceEntry&gt; loadExperienceEntries() </p>
	 *
	 * <p> Description: READ (US-R3).  Loads this lesson's experience entries, keeps them on this
	 * object, and returns them. </p>
	 *
	 * @return the experience entries, oldest first; empty (never null) if there are none
	 */
	public List<ExperienceEntry> loadExperienceEntries() {
		experienceEntries = ExperienceEntry.readForLesson(id);
		return experienceEntries;
	}


	/*****
	 * <p> Method: boolean update() </p>
	 *
	 * <p> Description: UPDATE (US-U1).  Saves the fields a Contributor may change (the core
	 * information, root cause, recommendation, and the AI-assisted flag) in a single UPDATE
	 * statement, so the lesson is never half-updated.  Lock flags and Curator fields are not
	 * written here; they have their own methods so a Contributor's save can never change them.
	 * Refusing to change a LOCKED field is the Model's job (US-U1-V), because only the Model can
	 * compare the new values with the stored ones. </p>
	 *
	 * @return true if the lesson was saved; false if no such lesson exists
	 *
	 * @throws IllegalArgumentException if the lesson is invalid; the message says why
	 */
	public boolean update() {
		String error = validate();												// US-U1-V
		if (!error.isEmpty()) throw new IllegalArgumentException(error);
		return applicationMain.FoundationsMain.database.updateLesson(this);	// US-U1
	}


	/*****
	 * <p> Method: boolean updateCuratorFields() </p>
	 *
	 * <p> Description: UPDATE (Phase Three Curator, Epics E2 to E5).  Saves the Curator-only
	 * attributes: search terms, curated-by and curated-at, original title and description, the
	 * statistics summary id, and its visibility.  No Contributor screen calls this; it exists so
	 * the attributes are fully persistent now and TP3 only needs to add the Curator screens. </p>
	 *
	 * @return true if the lesson was saved; false if no such lesson exists
	 *
	 * @throws IllegalArgumentException if the search terms are invalid
	 */
	public boolean updateCuratorFields() {
		String error = LessonRecognizer.checkForValidSearchTerms(searchTerms);
		if (!error.isEmpty()) throw new IllegalArgumentException(error);
		return applicationMain.FoundationsMain.database.updateLessonCuratorFields(this);
	}


	/*****
	 * <p> Method: boolean setLock(String scope, boolean locked) </p>
	 *
	 * <p> Description: UPDATE (Epic E6).  Locks or unlocks one field, or the whole lesson, and
	 * saves the change at once.  In TP2 there is no Curator screen; this is used by the tests and
	 * the demonstration data to create the locked lessons that US-R3, US-U1-V, and US-D1-V
	 * need, and TP3's Curator page will call it. </p>
	 *
	 * @param scope FIELD_TITLE, FIELD_DESCRIPTION, FIELD_TAGS, FIELD_CATEGORY, or LOCK_ENTIRE
	 *
	 * @param locked true to lock, false to unlock
	 *
	 * @return true if the change was saved; false for an unknown scope or a missing lesson
	 */
	public boolean setLock(String scope, boolean locked) {
		if (!applicationMain.FoundationsMain.database.setLessonLock(id, scope, locked))
			return false;
		// Keep this object in step with the row that was just changed
		switch (scope) {
			case FIELD_TITLE: lockedTitle = locked; break;
			case FIELD_DESCRIPTION: lockedDescription = locked; break;
			case FIELD_TAGS: lockedTags = locked; break;
			case FIELD_CATEGORY: lockedCategory = locked; break;
			case LOCK_ENTIRE: lockedEntire = locked; break;
			default: break;			// unreachable: the Database refused unknown scopes above
		}
		return true;
	}


	/*****
	 * <p> Method: boolean delete() </p>
	 *
	 * <p> Description: DELETE (US-D1).  Removes this lesson; its experience entries and their
	 * member shares are removed with it by the database's ON DELETE CASCADE rules.  Checking the
	 * locks and the "Are you sure?" answer (US-D1-V) is the Model's job and happens first. </p>
	 *
	 * @return true if the lesson was deleted; false if no such lesson exists
	 */
	public boolean delete() {
		return applicationMain.FoundationsMain.database.deleteLesson(id);		// US-D1
	}


	/*-*******************************************************************************************

	Lock queries (US-R3, US-U1-V, US-D1-V)

	 */

	/*****
	 * <p> Method: boolean isFieldLocked(String field) </p>
	 *
	 * <p> Description: Can this one field be edited right now?  A field counts as locked if its
	 * own flag is set OR the whole lesson is locked.  Both are checked here so that no caller has
	 * to remember to check both (Test Case "View Single Lesson - Locked Field Displayed as
	 * Read-Only"). </p>
	 *
	 * @param field FIELD_TITLE, FIELD_DESCRIPTION, FIELD_TAGS, or FIELD_CATEGORY (case-sensitive).
	 * 		Any other name returns false, because only these four fields can be locked.
	 *
	 * @return true if the field cannot be edited right now
	 */
	public boolean isFieldLocked(String field) {
		if (lockedEntire) return true;
		switch (field) {
			case FIELD_TITLE: return lockedTitle;
			case FIELD_DESCRIPTION: return lockedDescription;
			case FIELD_TAGS: return lockedTags;
			case FIELD_CATEGORY: return lockedCategory;
			default: return false;
		}
	}


	/*****
	 * <p> Method: boolean isLocked() </p>
	 *
	 * <p> Description: Can this lesson be deleted right now?  US-D1-V says it can NOT if the
	 * whole lesson is locked or if any single field is locked; the V1 story repeats this ("When a
	 * lesson I created is locked or has a locked field, I am unable to delete that lesson"). </p>
	 *
	 * @return true if the lesson is protected from deletion
	 */
	public boolean isLocked() {
		return lockedEntire || lockedTitle || lockedDescription || lockedTags || lockedCategory;
	}


	/*-*******************************************************************************************

	Getters and setters.  Following User.java, these are one-line accessors.  There is no setter
	for ownerUserName or createdAt (fixed when the lesson is created) or for the lock flags (they
	change only through setLock(), which also saves them).

	 */

	/** This getter returns the <code>id</code> attribute.
	 * @return the database id (0 if not yet saved) */
	public int getId() { return id; }
	/** This setter defines the <code>id</code> attribute.
	 * @param i the database id; used by the Database after an insert */
	public void setId(int i) { id = i; }
	/** This getter returns the <code>ownerUserName</code> attribute.
	 * @return the owning Contributor's username */
	public String getOwnerUserName() { return ownerUserName; }
	/** This getter returns the <code>title</code> attribute.
	 * @return the title */
	public String getTitle() { return title; }
	/** This setter defines the <code>title</code> attribute.
	 * @param s the new title (validated when the lesson is saved) */
	public void setTitle(String s) { title = s; }
	/** This getter returns the <code>description</code> attribute.
	 * @return the description */
	public String getDescription() { return description; }
	/** This setter defines the <code>description</code> attribute.
	 * @param s the new description (validated when the lesson is saved) */
	public void setDescription(String s) { description = s; }
	/** This getter returns the <code>tags</code> attribute.
	 * @return the comma-separated tags */
	public String getTags() { return tags; }
	/** This setter defines the <code>tags</code> attribute.
	 * @param s the new tags (validated when the lesson is saved) */
	public void setTags(String s) { tags = s; }
	/** This getter returns the <code>category</code> attribute.
	 * @return the category */
	public String getCategory() { return category; }
	/** This setter defines the <code>category</code> attribute.
	 * @param s the new category (validated when the lesson is saved) */
	public void setCategory(String s) { category = s; }
	/** This getter returns the <code>lockedTitle</code> attribute.
	 * @return true if the title is locked by its own flag */
	public boolean isLockedTitle() { return lockedTitle; }
	/** This getter returns the <code>lockedDescription</code> attribute.
	 * @return true if the description is locked by its own flag */
	public boolean isLockedDescription() { return lockedDescription; }
	/** This getter returns the <code>lockedTags</code> attribute.
	 * @return true if the tags are locked by their own flag */
	public boolean isLockedTags() { return lockedTags; }
	/** This getter returns the <code>lockedCategory</code> attribute.
	 * @return true if the category is locked by its own flag */
	public boolean isLockedCategory() { return lockedCategory; }
	/** This getter returns the <code>lockedEntire</code> attribute.
	 * @return true if the whole lesson is locked */
	public boolean isLockedEntire() { return lockedEntire; }
	/** This getter returns the <code>createdAt</code> attribute.
	 * @return the creation time in epoch milliseconds */
	public long getCreatedAt() { return createdAt; }
	/** This getter returns the <code>updatedAt</code> attribute.
	 * @return the last update time in epoch milliseconds */
	public long getUpdatedAt() { return updatedAt; }
	/** This setter defines the <code>updatedAt</code> attribute.
	 * @param t the last update time in epoch milliseconds */
	public void setUpdatedAt(long t) { updatedAt = t; }
	/** This getter returns the <code>experienceEntries</code> attribute.
	 * @return the experience entries loaded so far (empty until loadExperienceEntries()) */
	public List<ExperienceEntry> getExperienceEntries() { return experienceEntries; }
	/** This setter defines the <code>experienceEntries</code> attribute.
	 * @param e the experience entries; null is treated as an empty list */
	public void setExperienceEntries(List<ExperienceEntry> e) {
		experienceEntries = (e == null) ? new ArrayList<ExperienceEntry>() : e;
	}
	/** This getter returns the <code>rootCause</code> attribute.
	 * @return the root cause (AI coaching field; may be empty) */
	public String getRootCause() { return rootCause; }
	/** This setter defines the <code>rootCause</code> attribute.
	 * @param s the root cause (validated when the lesson is saved) */
	public void setRootCause(String s) { rootCause = s; }
	/** This getter returns the <code>recommendation</code> attribute.
	 * @return the recommendation (AI coaching field; may be empty) */
	public String getRecommendation() { return recommendation; }
	/** This setter defines the <code>recommendation</code> attribute.
	 * @param s the recommendation (validated when the lesson is saved) */
	public void setRecommendation(String s) { recommendation = s; }
	/** This getter returns the <code>aiAssisted</code> attribute.
	 * @return true if the Contributor used AI assistance */
	public boolean isAiAssisted() { return aiAssisted; }
	/** This setter defines the <code>aiAssisted</code> attribute.
	 * @param b true if the Contributor used AI assistance */
	public void setAiAssisted(boolean b) { aiAssisted = b; }
	/** This getter returns the <code>searchTerms</code> attribute.
	 * @return the Curator's search terms (may be empty) */
	public String getSearchTerms() { return searchTerms; }
	/** This setter defines the <code>searchTerms</code> attribute.
	 * @param s the Curator's search terms (validated when the Curator fields are saved) */
	public void setSearchTerms(String s) { searchTerms = s; }
	/** This getter returns the <code>curatedBy</code> attribute.
	 * @return the username of the Curator who last edited the lesson (empty if never) */
	public String getCuratedBy() { return curatedBy; }
	/** This setter defines the <code>curatedBy</code> attribute.
	 * @param s the username of the Curator who last edited the lesson */
	public void setCuratedBy(String s) { curatedBy = (s == null) ? "" : s; }
	/** This getter returns the <code>curatedAt</code> attribute.
	 * @return when the lesson was last curated in epoch milliseconds (0 if never) */
	public long getCuratedAt() { return curatedAt; }
	/** This setter defines the <code>curatedAt</code> attribute.
	 * @param t when the lesson was last curated in epoch milliseconds */
	public void setCuratedAt(long t) { curatedAt = t; }
	/** This getter returns the <code>originalTitle</code> attribute.
	 * @return the title as the Contributor wrote it before curation (empty if never curated) */
	public String getOriginalTitle() { return originalTitle; }
	/** This setter defines the <code>originalTitle</code> attribute.
	 * @param s the title as the Contributor wrote it before curation */
	public void setOriginalTitle(String s) { originalTitle = (s == null) ? "" : s; }
	/** This getter returns the <code>originalDescription</code> attribute.
	 * @return the description as written before curation (empty if never curated) */
	public String getOriginalDescription() { return originalDescription; }
	/** This setter defines the <code>originalDescription</code> attribute.
	 * @param s the description as the Contributor wrote it before curation */
	public void setOriginalDescription(String s) { originalDescription = (s == null) ? "" : s; }
	/** This getter returns the <code>summaryId</code> attribute.
	 * @return the id of the statistics summary for this lesson (-1 if none) */
	public int getSummaryId() { return summaryId; }
	/** This setter defines the <code>summaryId</code> attribute.
	 * @param i the id of the statistics summary for this lesson (-1 for none) */
	public void setSummaryId(int i) { summaryId = i; }
	/** This getter returns the <code>statsVisibleToContributor</code> attribute.
	 * @return true if the Curator has made the statistics visible to the Contributor */
	public boolean isStatsVisibleToContributor() { return statsVisibleToContributor; }
	/** This setter defines the <code>statsVisibleToContributor</code> attribute.
	 * @param b true to make the statistics visible to the Contributor */
	public void setStatsVisibleToContributor(boolean b) { statsVisibleToContributor = b; }
}
