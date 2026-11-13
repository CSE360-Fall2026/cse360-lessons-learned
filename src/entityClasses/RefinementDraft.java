package entityClasses;

import java.util.Objects;

/**
 * Immutable text and revision seen when a Curator opens a lesson (US-C-E3-04/06).
 * A suggestion never mutates this snapshot. Comparing it at save time prevents a
 * second editor from silently overwriting changes made during the review.
 * Lock flags are deliberately absent: E3 never writes E6's flags.
 * Tested by curatorRefinementTests.CuratorRefinementTests.
 *
 * @param lessonId selected lesson
 * @param title title when the review began
 * @param description description when the review began
 * @param updatedAt core revision timestamp
 * @param curatedAt curation timestamp when the review began
 * @param curatedBy previous Curator, possibly empty
 * @author Daniel Martinez Julio
 */
public record RefinementDraft(int lessonId, String title, String description,
        long updatedAt, long curatedAt, String curatedBy) {

    /**
     * Captures a stored lesson; tested by RF-01 and RF-15.
     * @param lesson stored lesson
     * @return its immutable review snapshot
     */
    public static RefinementDraft fromLesson(LessonsLearned lesson) {
        return new RefinementDraft(lesson.getId(), lesson.getTitle(), lesson.getDescription(),
                lesson.getUpdatedAt(), lesson.getCuratedAt(), lesson.getCuratedBy());
    }

    /**
     * Tests whether the stored text/revision still matches; RF-15/16/17.
     * @param lesson freshly loaded lesson, or null
     * @return whether this draft is current
     */
    public boolean matches(LessonsLearned lesson) {
        return lesson != null && lessonId == lesson.getId()
                && Objects.equals(title, lesson.getTitle())
                && Objects.equals(description, lesson.getDescription())
                && updatedAt == lesson.getUpdatedAt() && curatedAt == lesson.getCuratedAt()
                && Objects.equals(curatedBy, lesson.getCuratedBy());
    }
}
