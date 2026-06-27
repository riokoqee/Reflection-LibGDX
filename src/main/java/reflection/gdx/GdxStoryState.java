package reflection.gdx;

final class GdxStoryState {

    private static final int STAGE_SHADOW_FIRST = 0;
    private static final int STAGE_SHADOW_SECOND = 1;
    private static final int STAGE_CHILD = 2;
    private static final int STAGE_FOREST_SHADOW = 3;
    private static final int STAGE_FRIEND = 4;
    private static final int STAGE_ELDER = 5;
    private static final int STAGE_WARRIOR = 6;
    private static final int STAGE_DONE = 7;

    private int stage = STAGE_SHADOW_FIRST;
    private int growth = 35;
    private int calm = 35;
    private int empathy = 35;
    private int confidence = 35;
    private int responsibility = 35;
    private int avoidance = 35;
    private int selfWorth = 35;

    Prompt promptForActor(String actorName, int mapIndex, boolean hasLantern) {
        if ("Shadow".equals(actorName) && mapIndex == 0 && stage <= STAGE_SHADOW_SECOND) {
            return stage == STAGE_SHADOW_FIRST ? shadowFirst() : shadowSecond();
        }
        if ("Child".equals(actorName) && stage <= STAGE_CHILD) {
            return child();
        }
        if ("Shadow".equals(actorName) && mapIndex == 1 && stage <= STAGE_FOREST_SHADOW && hasLantern) {
            return forestShadow();
        }
        if ("Friend".equals(actorName) && stage <= STAGE_FRIEND) {
            return friend();
        }
        if ("Elder".equals(actorName) && stage <= STAGE_ELDER) {
            return elder();
        }
        if ("Warrior".equals(actorName) && stage <= STAGE_WARRIOR) {
            return warrior();
        }
        return null;
    }

    ChoiceOutcome choose(Prompt prompt, int choiceIndex) {
        if (prompt == null || prompt.choices.length == 0) {
            return ChoiceOutcome.message("", "");
        }
        int index = Math.max(0, Math.min(choiceIndex, prompt.choices.length - 1));
        Choice choice = prompt.choices[index];
        apply(choice);

        switch (prompt.id) {
            case "shadow_first":
                stage = STAGE_SHADOW_SECOND;
                return ChoiceOutcome.prompt(shadowSecond());
            case "shadow_second":
                stage = STAGE_CHILD;
                return ChoiceOutcome.destination("Shadow",
                        "The apartment door dissolves into a dark path. The Forest of Doubts breathes beyond it.",
                        1, 23, 43);
            case "child":
                stage = STAGE_FOREST_SHADOW;
                return ChoiceOutcome.message("Child",
                        "The swing creaks once. The path ahead becomes easier to see.");
            case "forest_shadow":
                stage = STAGE_FRIEND;
                return ChoiceOutcome.destination("Shadow",
                        "The forest opens. Warm lights appear between the trees.",
                        2, 23, 15);
            case "friend":
                stage = STAGE_ELDER;
                return ChoiceOutcome.message("Friend",
                        "The village square grows quieter. The library light is still on.");
            case "elder":
                stage = STAGE_WARRIOR;
                return ChoiceOutcome.destination("Elder",
                        "The answer is not in the book. The bridge beyond the library leads to the Mountain.",
                        3, 35, 31);
            case "warrior":
                stage = STAGE_DONE;
                return ChoiceOutcome.message("Warrior",
                        "The climb is complete. Every voice you met was part of the same inner road.\n\n" +
                                "Current profile: " + metricsLine());
            default:
                return ChoiceOutcome.message(prompt.speaker, choice.resultText);
        }
    }

    String metricsLine() {
        return "Growth " + growth +
                " | Calm " + calm +
                " | Empathy " + empathy +
                " | Confidence " + confidence +
                " | Responsibility " + responsibility +
                " | Avoidance " + avoidance +
                " | Self-worth " + selfWorth;
    }

    private void apply(Choice choice) {
        growth = clamp(growth + choice.growth);
        calm = clamp(calm + choice.calm);
        empathy = clamp(empathy + choice.empathy);
        confidence = clamp(confidence + choice.confidence);
        responsibility = clamp(responsibility + choice.responsibility);
        avoidance = clamp(avoidance + choice.avoidance);
        selfWorth = clamp(selfWorth + choice.selfWorth);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private Prompt shadowFirst() {
        return prompt("shadow_first", "Shadow",
                "You finally look at me... I have been here for a long time. Your eyes kept turning away.",
                choice("What are you?", 0, 0, 0, 0),
                choice("Is this a dream?", 0, 0, 0, 0),
                choice("I am just tired...", 0, 0, 0, 0),
                choice("Leave me alone.", 0, 0, 0, 0));
    }

    private Prompt shadowSecond() {
        return prompt("shadow_second", "Shadow",
                "I am what you keep running from. Do you want to come with me?",
                choice("Let's go.", 15, 8, 0, 0),
                choice("Tell me who you are first.", 0, 0, 12, 10),
                choice("I do not want to go anywhere.", -15, 10, 0, 0),
                choice("None of this is real.", 0, -10, 0, 8));
    }

    private Prompt child() {
        if (empathy >= 40) {
            return prompt("child", "Child",
                    "I knew you would come... Did you forget me completely?",
                    choice("Sorry, I was busy.", 0, -8, 18, 0),
                    choice("I did not abandon you.", 15, 0, 0, 10),
                    choice("Let's sit together.", 10, 15, 22, 0));
        }
        return prompt("child", "Child",
                "You always say that... Did you forget me completely?",
                choice("Sorry, I was busy.", 0, -12, 8, 0),
                choice("Who are you? I do not know you.", 0, 10, -20, 0),
                choice("I did not abandon you.", 5, 0, -10, 0));
    }

    private Prompt forestShadow() {
        return prompt("forest_shadow", "Shadow",
                "See? Even that part of you is disappointed. How long will you keep hiding?",
                choice("I am not hiding.", 0, -8, 0, 12),
                choice("What should I do?", 18, 0, 12, 0),
                choice("I accept you.", 20, 25, 15, 0),
                choice("Just disappear.", 0, -25, -15, 18));
    }

    private Prompt friend() {
        if (empathy >= 55) {
            return prompt("friend", "Friend",
                    "You are finally here... I am glad to see you. You look like you have not slept in years.",
                    choice("Everything is fine.", 0, 8, -10, 0),
                    choice("Honestly? I feel awful.", 0, 12, 22, 0),
                    choice("Sorry I disappeared.", 15, 0, 25, 0));
        }
        return prompt("friend", "Friend",
                "You always do this... distant, even when you are standing nearby.",
                choice("Everything is fine.", 0, 10, -15, 0),
                choice("Honestly? I feel awful.", 0, 8, 15, 0),
                choice("Tell me about yourself instead.", 0, 0, 18, -8),
                choice("Sorry I disappeared.", 12, 0, 10, 0));
    }

    private Prompt elder() {
        return prompt("elder", "Elder",
                "Many come here, but few stay. What are you looking for in this place?",
                choice("Strength.", 0, 0, -10, 25),
                choice("Peace.", 8, 25, 0, 0),
                choice("Answers.", 18, 0, 12, 0),
                choice("I am just moving forward.", 10, 10, 10, 10));
    }

    private Prompt warrior() {
        if (growth + confidence >= 130) {
            return prompt("warrior", "Warrior",
                    "The path is complete. Few make it this far.",
                    choice("I can keep going.", 20, 0, 0, 25),
                    choice("I need rest.", -10, 18, 0, 0),
                    choice("I will take everyone with me.", 18, 0, 22, 0));
        }
        return prompt("warrior", "Warrior",
                "The path is almost complete, but something inside still wants to stop halfway.",
                choice("I can keep going.", 15, 0, 0, 18),
                choice("I need rest.", -15, 25, 0, 0),
                choice("Something in me has changed.", 20, 20, 20, 20));
    }

    private Prompt prompt(String id, String speaker, String text, Choice... choices) {
        return new Prompt(id, speaker, text, choices);
    }

    private Choice choice(String text, int growth, int calm, int empathy, int confidence) {
        return new Choice(text, growth, calm, empathy, confidence, 0, 0, 0, "");
    }

    static final class Prompt {
        final String id;
        final String speaker;
        final String text;
        final Choice[] choices;

        private Prompt(String id, String speaker, String text, Choice[] choices) {
            this.id = id;
            this.speaker = speaker;
            this.text = text;
            this.choices = choices;
        }
    }

    static final class Choice {
        final String text;
        final int growth;
        final int calm;
        final int empathy;
        final int confidence;
        final int responsibility;
        final int avoidance;
        final int selfWorth;
        final String resultText;

        private Choice(String text, int growth, int calm, int empathy, int confidence,
                       int responsibility, int avoidance, int selfWorth, String resultText) {
            this.text = text;
            this.growth = growth;
            this.calm = calm;
            this.empathy = empathy;
            this.confidence = confidence;
            this.responsibility = responsibility;
            this.avoidance = avoidance;
            this.selfWorth = selfWorth;
            this.resultText = resultText;
        }
    }

    static final class ChoiceOutcome {
        final Prompt nextPrompt;
        final String speaker;
        final String text;
        final int mapIndex;
        final int column;
        final int row;

        private ChoiceOutcome(Prompt nextPrompt, String speaker, String text, int mapIndex, int column, int row) {
            this.nextPrompt = nextPrompt;
            this.speaker = speaker;
            this.text = text;
            this.mapIndex = mapIndex;
            this.column = column;
            this.row = row;
        }

        static ChoiceOutcome prompt(Prompt prompt) {
            return new ChoiceOutcome(prompt, "", "", -1, 0, 0);
        }

        static ChoiceOutcome message(String speaker, String text) {
            return new ChoiceOutcome(null, speaker, text, -1, 0, 0);
        }

        static ChoiceOutcome destination(String speaker, String text, int mapIndex, int column, int row) {
            return new ChoiceOutcome(null, speaker, text, mapIndex, column, row);
        }

        boolean hasDestination() {
            return mapIndex >= 0;
        }
    }
}
