package guiCuratorRefine;

import java.util.List;
import java.util.Map;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.RefinementDraft;
import entityClasses.User;
import guiCuratorRefine.ModelCuratorRefine.Review;
import guiTools.LessonResult;

/**
 * Runnable, deterministic E3 prototype demonstration. The response is explicitly
 * a fixture, not a live AI result. Run through Test-Aspect6.ps1 -RunDemo to use a
 * separate disposable database. Tests cover the same model; this only demonstrates
 * the original/suggestion/review/save sequence for the team's integration review.
 * @author Daniel Martinez Julio
 */
public final class CuratorRefinementDemo {
    private CuratorRefinementDemo() { }

    /**
     * Shows the complete prototype sequence in a terminal without starting JavaFX.
     * @param args unused
     * @throws Exception if demonstration setup fails
     */
    public static void main(String[] args) throws Exception {
        Database database = FoundationsMain.database;
        String token = Long.toUnsignedString(System.nanoTime());
        String owner = "demoOwner" + token;
        User curator = new User("demoCurator" + token, "Example1!", "", "", "", "", "",
                false, false, true);
        int id = -1;
        try {
            database.connectToDatabase();
            database.register(curator);
            LessonsLearned lesson = new LessonsLearned(owner, "Deployment recovery",
                    "The worker failed. We restored its configuration and restarted it.", "deploy", "Technical");
            id = database.createLesson(lesson);
            database.setLessonLocks(id, List.of("entire", "title", "description"), true);
            ModelCuratorRefine model = new ModelCuratorRefine(database,
                    (field, original, goal) -> "Restore the worker configuration, validate it, then restart the worker.");
            RefinementDraft draft = model.openLesson(curator, id).draft();
            Review review = model.requestSuggestion(curator, draft, "description", "clearer", true);
            if (!review.success()) throw new IllegalStateException(review.message());
            System.out.println("ASPECT 6 / EPIC E3 PROTOTYPE");
            System.out.println("Demo adapter response only: no external AI/network call.");
            System.out.println("\nORIGINAL                          | SUGGESTION");
            String[] left = wrap(review.original()).split("\n");
            String[] right = wrap(review.suggestion()).split("\n");
            for (int line = 0; line < Math.max(left.length, right.length); line++)
                System.out.printf("%-34s| %s%n", line < left.length ? left[line] : "", line < right.length ? right[line] : "");
            System.out.println("\nRequest alone saved nothing: " + database.getLessonById(id).getCuratedBy().isEmpty());
            String reviewed = "Restore and validate the configuration before restarting the failed worker.";
            LessonResult saved = model.saveRefinement(curator, draft, Map.of("description", reviewed), true);
            if (!saved.isSuccess()) throw new IllegalStateException(saved.getMessage());
            LessonsLearned actual = database.getLessonById(id);
            System.out.println("CURATOR'S REVIEWED FINAL TEXT: " + actual.getDescription());
            System.out.println(ModelCuratorRefine.curationNote(actual));
            System.out.println("First original preserved: " + actual.getOriginalDescription().equals(draft.description()));
            System.out.println("Contributor locks still enabled: "
                    + (actual.isLockedEntire() && actual.isLockedTitle() && actual.isLockedDescription()));
        } finally {
            if (id > 0) database.deleteLesson(id);
            database.deleteUser(curator.getUserName());
            database.closeConnection();
        }
    }

    private static String wrap(String text) {
        StringBuilder lines = new StringBuilder();
        int length = 0;
        for (String word : text.split("\\s+")) {
            if (length > 0 && length + word.length() + 1 > 32) { lines.append('\n'); length = 0; }
            if (length > 0) { lines.append(' '); length++; }
            lines.append(word);
            length += word.length();
        }
        return lines.toString();
    }
}
