package entityClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*******
 * <p>
 * Title: LessonsLearnedList Class
 * </p>
 *
 * <p>
 * Description: This class holds a list of lessons learned and provides the
 * Create, Read, Update, and Delete (CRUD) operations for lists. It is used for
 * two kinds of list:
 * </p>
 * <ul>
 * <li>Persistent lists loaded from the database: loadAll() returns every lesson
 * in the system, and loadByOwner() returns the lessons one Contributor has
 * added (US-R1, US-R4).</li>
 * <li>Subsets of a list, such as the results of a search. A subset may be
 * empty, may hold one lesson, or may hold many with no upper limit (US-R2; Test
 * Case "Subset List - Returns Correct Sizes").</li>
 * </ul>
 *
 * <p>
 * <b>Operations.</b> Create: add(). Read: size(), isEmpty(), get(), getById(),
 * contains(), getLessons(), and the static loaders. Update: replace(). Delete:
 * removeById(). Subsets: filterByTitleContains() (the Contributor search,
 * US-R2), filterByKeywords() (keyword search across title, description, tags,
 * and Curator-approved search terms for the Phase Three Viewer story "I can use
 * a keyword search for lessons and receive a list of matches"),
 * filterByCategory(), and filterByTag().
 * </p>
 *
 * <p>
 * <b>Design.</b> The list operations change only this in-memory list; saving or
 * deleting a lesson in the database is done by the LessonsLearned object itself
 * (create(), delete()). Keeping the two separate means filtering or trimming a
 * list can never delete data. For the same reason, every filter returns a NEW
 * list and never changes the list it is called on, so filtering "all my
 * lessons" cannot accidentally throw lessons away.
 * </p>
 *
 * <p>
 * <b>Privacy.</b> loadAll() exists at the class level for the Viewer and
 * Curator roles in Phase Three and for administration and testing. The
 * Contributor screens use only loadByOwner(), because the V1 story says a
 * Contributor is "unable to see submissions from other contributors" (US-R4).
 * </p>
 *
 * <p>
 * Copyright: Lynn Robert Carter © 2026
 * </p>
 *
 * @author David Nevarez and Emilia Davey
 *
 * @version 1.00 2026-09-29 Initial version for HW2 (as LessonCollection)
 * @version 2.00 2026-10-14 TP2: renamed LessonsLearnedList; persistent
 *          all-lessons and per-owner lists; list CRUD; keyword search
 * @version 2.01 2026-11-09 HW3: extended keyword search to include
 *          Curator-approved search terms
 */

public class LessonsLearnedList {

	/** The lessons in this list, in the order they were loaded or added.  Source: HW2 and TP2
	 * instructions, "a list class to support the CRUD actions for lists of lessons learned."
	 * Rationale: an ArrayList because a subset has no upper size limit and the order (newest
	 * first, as loaded) must be kept.  The list is private and handed out read-only, so it can
	 * only change through this class's own operations. */
	private final List<LessonsLearned> lessons;


	/*-*******************************************************************************************

	Constructors

	 */

	/*****
	 * <p> Method: LessonsLearnedList() </p>
	 *
	 * <p> Description: Builds an empty list.  This is what a Contributor with no lessons (US-R1)
	 * or a search that matched nothing (US-R2) receives: an empty list, never null. </p>
	 */
	public LessonsLearnedList() {
		lessons = new ArrayList<LessonsLearned>();
	}


	/*****
	 * <p> Method: LessonsLearnedList(List&lt;LessonsLearned&gt; lessons) </p>
	 *
	 * <p> Description: Builds a list holding the given lessons, for example right after the
	 * Database reads a batch of rows.  The lessons are copied into a new list, so later changes
	 * to the caller's list cannot change this one.  Null elements are skipped. </p>
	 *
	 * @param lessons the lessons to hold; null is treated as an empty list so nothing downstream
	 * 		has to check this class for null
	 */
	public LessonsLearnedList(List<LessonsLearned> lessons) {
		this.lessons = new ArrayList<LessonsLearned>();
		if (lessons != null)
			for (LessonsLearned lesson : lessons)
				if (lesson != null) this.lessons.add(lesson);
	}


	/*-*******************************************************************************************

	Persistent lists

	 */

	/*****
	 * <p> Method: LessonsLearnedList loadAll() </p>
	 *
	 * <p> Description: READ.  Loads every lesson learned in the system, newest first.  This is
	 * the persistent list of all lessons that the TP2 instructions require.  It must not be shown
	 * on a Contributor screen (US-R4); see the class description. </p>
	 *
	 * @return every lesson; empty (never null) if there are none
	 */
	public static LessonsLearnedList loadAll() {
		return applicationMain.FoundationsMain.database.getAllLessons();
	}


	/*****
	 * <p> Method: LessonsLearnedList loadByOwner(String ownerUserName) </p>
	 *
	 * <p> Description: READ (US-R1, US-R4).  Loads the lessons one Contributor has added, newest
	 * first.  The database query itself is restricted to the owner, so another Contributor's
	 * lesson can never be part of the result. </p>
	 *
	 * @param ownerUserName the Contributor whose lessons should be loaded
	 *
	 * @return that Contributor's lessons; empty (never null) if there are none
	 */
	public static LessonsLearnedList loadByOwner(String ownerUserName) {
		return applicationMain.FoundationsMain.database.getLessonsByOwner(ownerUserName);
	}


	/*-*******************************************************************************************

	List CRUD: these change only this in-memory list (see the class description)

	 */

	/*****
	 * <p> Method: boolean add(LessonsLearned lesson) </p>
	 *
	 * <p> Description: CREATE.  Adds a lesson to the end of this list.  A lesson whose id is
	 * already in the list is not added a second time, because the same lesson appearing twice
	 * would make a list's size misleading.  Unsaved lessons (id 0) are always added. </p>
	 *
	 * @param lesson the lesson to add
	 *
	 * @return true if it was added; false if it was null or already in the list
	 */
	public boolean add(LessonsLearned lesson) {
		if (lesson == null) return false;
		if (lesson.getId() != 0 && contains(lesson.getId())) return false;
		lessons.add(lesson);
		return true;
	}


	/*****
	 * <p> Method: int size() </p>
	 *
	 * <p> Description: READ.  How many lessons are in this list. </p>
	 *
	 * @return the number of lessons held
	 */
	public int size() { return lessons.size(); }


	/*****
	 * <p> Method: boolean isEmpty() </p>
	 *
	 * <p> Description: READ.  Does this list hold no lessons? </p>
	 *
	 * @return true if it is empty
	 */
	public boolean isEmpty() { return lessons.isEmpty(); }


	/*****
	 * <p> Method: LessonsLearned get(int index) </p>
	 *
	 * <p> Description: READ.  The lesson at a position in the list, for example the row a user
	 * selected on a screen. </p>
	 *
	 * @param index the 0-based position
	 *
	 * @return the lesson at that position, or null if the position is outside the list
	 */
	public LessonsLearned get(int index) {
		if (index < 0 || index >= lessons.size()) return null;
		return lessons.get(index);
	}


	/*****
	 * <p> Method: LessonsLearned getById(int lessonId) </p>
	 *
	 * <p> Description: READ.  The lesson in this list with the given id. </p>
	 *
	 * @param lessonId the id to look for
	 *
	 * @return the lesson, or null if no lesson in this list has that id
	 */
	public LessonsLearned getById(int lessonId) {
		for (LessonsLearned lesson : lessons)
			if (lesson.getId() == lessonId) return lesson;
		return null;
	}


	/*****
	 * <p> Method: boolean contains(int lessonId) </p>
	 *
	 * <p> Description: READ.  Is a lesson with the given id in this list? </p>
	 *
	 * @param lessonId the id to look for
	 *
	 * @return true if it is in the list
	 */
	public boolean contains(int lessonId) {
		return getById(lessonId) != null;
	}


	/*****
	 * <p> Method: List&lt;LessonsLearned&gt; getLessons() </p>
	 *
	 * <p> Description: READ.  The lessons in this list, in order.  The returned list is
	 * read-only, so the only way to change this list is through add(), replace(), and
	 * removeById().  It is never null, even for an empty subset. </p>
	 *
	 * @return a read-only view of the lessons
	 */
	public List<LessonsLearned> getLessons() { return Collections.unmodifiableList(lessons); }


	/*****
	 * <p> Method: boolean replace(LessonsLearned lesson) </p>
	 *
	 * <p> Description: UPDATE.  Replaces the lesson in this list that has the same id with the
	 * given (for example, freshly reloaded) lesson, keeping its position.  A screen uses this to
	 * refresh one row after an edit without reloading the whole list. </p>
	 *
	 * @param lesson the new version of a lesson already in the list
	 *
	 * @return true if a lesson was replaced; false if the lesson was null or its id is not here
	 */
	public boolean replace(LessonsLearned lesson) {
		if (lesson == null) return false;
		for (int ndx = 0; ndx < lessons.size(); ndx++)
			if (lessons.get(ndx).getId() == lesson.getId()) {
				lessons.set(ndx, lesson);
				return true;
			}
		return false;
	}


	/*****
	 * <p> Method: boolean removeById(int lessonId) </p>
	 *
	 * <p> Description: DELETE.  Removes the lesson with the given id from this list only (the
	 * database is not touched).  A screen uses this after LessonsLearned.delete() succeeds. </p>
	 *
	 * @param lessonId the id of the lesson to remove
	 *
	 * @return true if a lesson was removed; false if no lesson in this list has that id
	 */
	public boolean removeById(int lessonId) {
		for (int ndx = 0; ndx < lessons.size(); ndx++)
			if (lessons.get(ndx).getId() == lessonId) {
				lessons.remove(ndx);
				return true;
			}
		return false;
	}


	/*-*******************************************************************************************

	Subsets.  Each returns a NEW list and leaves this one unchanged.

	 */

	/*****
	 * <p> Method: LessonsLearnedList filterByTitleContains(String text) </p>
	 *
	 * <p> Description: The subset whose titles contain the text, ignoring case.  This is the
	 * search the Contributor screen uses (US-R2), so it is the filter behind the Search/Filter
	 * Text tests and "Subset List - Returns Correct Sizes".</p>
	 *
	 * <p> An EMPTY (or all-space) search returns an EMPTY list, not every lesson.  Java's
	 * String.contains("") is true for every String, so without this check an empty search would
	 * match everything.  The team's InputValidation.pdf says the Users search box should "return
	 * no results on an empty or non-matching search rather than showing a field error"; the same
	 * rule is applied to lessons (design decision D6).  The screen's Clear button is how a user
	 * gets the full list back. </p>
	 *
	 * @param text the text to look for in each title
	 *
	 * @return a new list with the matches; empty if the text is empty or nothing matched
	 */
	public LessonsLearnedList filterByTitleContains(String text) {
		List<LessonsLearned> matches = new ArrayList<LessonsLearned>();
		if (text == null || text.trim().isEmpty()) return new LessonsLearnedList(matches);
		String needle = text.trim().toLowerCase();
		for (LessonsLearned lesson : lessons)
			if (lesson.getTitle() != null && lesson.getTitle().toLowerCase().contains(needle))
				matches.add(lesson);
		return new LessonsLearnedList(matches);
	}


    /*****
     * <p>
     * Method: LessonsLearnedList filterByKeywords(String keywords)
     * </p>
     *
     * <p>
     * Description: TP2, extended for TP3. Returns a subset of lessons matching a
     * keyword search (design decision D5). The search text is split into keywords
     * at spaces and commas. A lesson matches only if EVERY keyword appears,
     * ignoring case, somewhere in its title, description, tags, or saved search
     * terms. Adding another keyword narrows the results.
     * </p>
     *
     * <p>
     * For TP3, the search now includes Curator-approved search terms. This allows
     * Viewers to find lessons using terms that may not appear in the lesson's
     * title, description, or tags. For example, a lesson about database connection
     * failures could also be found by searching for "connectivity" if that term was
     * saved by a Curator.
     * </p>
     *
     * <p>
     * As with filterByTitleContains(), an empty search returns an empty list
     * instead of every lesson. Searches with no matches also return an empty list.
     * The method creates a new list without changing the original list or any of
     * its lessons.
     * </p>
     *
     * <p>
     * The Contributor screen continues using the title search required by US-R2.
     * This method supports the TP2 keyword search functionality and the TP3 Viewer
     * keyword search, including Curator-approved terms.
     * </p>
     *
     * @param keywords one or more keywords separated by spaces or commas
     *
     * @return a new list containing lessons that match every keyword; empty if no
     *         keywords are provided or nothing matches
     */

	public LessonsLearnedList filterByKeywords(String keywords) {
		List<LessonsLearned> matches = new ArrayList<LessonsLearned>();
		if (keywords == null || keywords.trim().isEmpty()) return new LessonsLearnedList(matches);

		// Splits the search into keywords separated by spaces or commas.
		String[] words = keywords.trim().toLowerCase().split("[\\s,]+");

		for (LessonsLearned lesson : lessons) {
			// Includes Curator-approved search terms so Viewers can find
			// lessons even when those terms aren't in the title, description, or tags.
            String searchable = (nullToEmpty(lesson.getTitle()) + " " + nullToEmpty(lesson.getDescription()) + " "
                    + nullToEmpty(lesson.getTags()) + " " + nullToEmpty(lesson.getSearchTerms())).toLowerCase();
			boolean allFound = true;
			for (String word : words)
				if (!word.isEmpty() && !searchable.contains(word)) {
					allFound = false;
					break;
				}
			if (allFound) matches.add(lesson);
		}
		return new LessonsLearnedList(matches);
	}


	/*****
	 * <p> Method: LessonsLearnedList filterByCategory(String category) </p>
	 *
	 * <p> Description: The subset whose category matches exactly.  A general-purpose filter for
	 * grouping lessons by category; the Contributor search box does not use it. </p>
	 *
	 * @param category the category to keep
	 *
	 * @return a new list (possibly empty)
	 */
	public LessonsLearnedList filterByCategory(String category) {
		List<LessonsLearned> matches = new ArrayList<LessonsLearned>();
		for (LessonsLearned lesson : lessons)
			if (lesson.getCategory() != null && lesson.getCategory().equals(category))
				matches.add(lesson);
		return new LessonsLearnedList(matches);
	}


	/*****
	 * <p> Method: LessonsLearnedList filterByTag(String tag) </p>
	 *
	 * <p> Description: The subset whose tags contain the given text, ignoring case.  A
	 * general-purpose filter; empty text returns an empty list, like the searches above. </p>
	 *
	 * @param tag the tag text to look for
	 *
	 * @return a new list (possibly empty)
	 */
	public LessonsLearnedList filterByTag(String tag) {
		List<LessonsLearned> matches = new ArrayList<LessonsLearned>();
		if (tag == null || tag.trim().isEmpty()) return new LessonsLearnedList(matches);
		String needle = tag.trim().toLowerCase();
		for (LessonsLearned lesson : lessons)
			if (lesson.getTags() != null && lesson.getTags().toLowerCase().contains(needle))
				matches.add(lesson);
		return new LessonsLearnedList(matches);
	}


	/*****
	 * <p> Method: String nullToEmpty(String text) </p>
	 *
	 * <p> Description: Treats a missing value as empty text so the keyword search never fails on
	 * a lesson with a null field. </p>
	 *
	 * @param text the text, possibly null
	 *
	 * @return the text, or an empty String for null
	 */
	private static String nullToEmpty(String text) {
		return (text == null) ? "" : text;
	}
}
