package lessonTests;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import guiContributorHome.ModelContributorHome;
import guiCreateLesson.ModelCreateLesson;
import guiTools.LessonResult;
import guiUpdateLesson.ModelUpdateLesson;

/*******
 * <p> Title: ContributorAiToolsTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for TP3 Aspect 7 (B-16): AI-assisted flag tracking,
 * rootCause and recommendation validation, Curator lock protection, and Contributor-only
 * keyword example search. </p>
 *
 * @author Virgil Jones
 * 
 * @version 1.00		2026-11-16 Initial TP3 Aspect 7 JUnit tests
 */
public class ContributorAiToolsTests {

	private Database db;

	@BeforeEach
	public void setUp() throws SQLException {
		// Create a fresh in-memory database before each test
		db = new Database("jdbc:h2:mem:aspect7test;DB_CLOSE_DELAY=-1");
		db.connectToDatabase();
		FoundationsMain.database = db;
	}

	@AfterEach
	public void tearDown() {
		if (db != null) db.closeConnection();
	}

	@Test
	public void testCreateWithAiApplied() {
		LessonResult result = ModelCreateLesson.createLesson(
				"virgil",
				"Rollback Lesson",
				"We deployed without a rollback script and had downtime.",
				"release, deploy",
				"Technical",
				"No rollback script was tested.",
				"Always test rollback steps first.",
				true);

		assertTrue(result.isSuccess());
		LessonsLearned saved = LessonsLearned.read(result.getLessonId());
		assertTrue(saved.isAiAssisted());
		assertEquals("No rollback script was tested.", saved.getRootCause());
		assertEquals("Always test rollback steps first.", saved.getRecommendation());
	}

	@Test
	public void testCreateWithoutAiApplied() {
		LessonResult result = ModelCreateLesson.createLesson(
				"virgil",
				"Manual Lesson",
				"Written completely by hand without AI coaching.",
				"manual",
				"General",
				"Manual cause",
				"Manual recommendation",
				false);

		assertTrue(result.isSuccess());
		LessonsLearned saved = LessonsLearned.read(result.getLessonId());
		assertFalse(saved.isAiAssisted());
	}

	@Test
	public void testRejectsOverLimitRootCause() {
		String tooLong = "a".repeat(2001);
		LessonResult result = ModelCreateLesson.createLesson(
				"virgil",
				"Valid Title",
				"Valid description with enough characters.",
				"tags",
				"Technical",
				tooLong,
				"Valid recommendation",
				true);

		assertFalse(result.isSuccess());
		assertEquals(LessonsLearned.FIELD_ROOT_CAUSE, result.getFailedField());
	}

	@Test
	public void testUpdateWithAiAndLockProtection() {
		LessonResult created = ModelCreateLesson.createLesson(
				"virgil", "Title", "Original description text.", "tag", "Technical");
		int id = created.getLessonId();

		// Update with AI applied -> aiAssisted becomes true
		LessonResult updated = ModelUpdateLesson.updateLesson(
				"virgil", id, "Title", "Original description text.", "tag", "Technical",
				"Missing unit test", "Add JUnit test coverage", true);
		assertTrue(updated.isSuccess());

		LessonsLearned lesson = LessonsLearned.read(id);
		assertTrue(lesson.isAiAssisted());

		// Lock description and confirm update refuses to overwrite it
		lesson.setLock(LessonsLearned.FIELD_DESCRIPTION, true);
		LessonResult blocked = ModelUpdateLesson.updateLesson(
				"virgil", id, "Title", "New AI text", "tag", "Technical",
				"Missing unit test", "Add JUnit test coverage", true);
		assertFalse(blocked.isSuccess());
		assertEquals(LessonsLearned.FIELD_DESCRIPTION, blocked.getFailedField());
	}

	@Test
	public void testExampleSearchOnlyShowsOwnLessons() {
		ModelCreateLesson.createLesson("virgil", "H2 Guide",
				"How to alter H2 columns safely.", "database", "Technical");
		ModelCreateLesson.createLesson("bob", "Bob H2 Secret",
				"Bob also wrote about H2 database.", "database", "Technical");

		LessonResult search = ModelContributorHome.searchMyLessonsByKeyword("virgil", "database");
		assertTrue(search.isSuccess());
		assertEquals(1, search.getList().size());
		assertEquals("H2 Guide", search.getList().get(0).getTitle());
	}
}