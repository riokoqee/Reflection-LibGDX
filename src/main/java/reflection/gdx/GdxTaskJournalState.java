package reflection.gdx;

final class GdxTaskJournalState {

    private static final String[] LABELS = {
            "Make the bed",
            "Clear the dishes",
            "Turn on the TV",
            "Rest on the sofa",
            "Find the phone",
            "Pick up the lantern"
    };

    private final boolean[] complete = new boolean[LABELS.length];

    void reset() {
        for (int i = 0; i < complete.length; i++) {
            complete[i] = false;
        }
    }

    void markBedMade() {
        complete[0] = true;
    }

    void markDishesCleared() {
        complete[1] = true;
    }

    void markTvTurnedOn() {
        complete[2] = true;
    }

    void markSofaRested() {
        complete[3] = true;
    }

    void markPhoneFound() {
        complete[4] = true;
    }

    void markLanternPicked() {
        complete[5] = true;
    }

    int count() {
        return LABELS.length;
    }

    String label(int index) {
        return LABELS[index];
    }

    boolean complete(int index) {
        return complete[index];
    }

    int completedCount() {
        int count = 0;
        for (boolean done : complete) {
            if (done) {
                count++;
            }
        }
        return count;
    }
}
