package reflection.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

final class GdxInteractionOverlay implements Disposable {

    private static final float WORLD_WIDTH = 960f;
    private static final float WORLD_HEIGHT = 540f;

    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont bodyFont = new BitmapFont();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Texture pixel;
    private String title = "";
    private String text = "";
    private Runnable closeAction;
    private boolean dialogueOpen;
    private GdxStoryState.Prompt activePrompt;
    private int selectedChoice;

    GdxInteractionOverlay() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        titleFont.setUseIntegerPositions(false);
        bodyFont.setUseIntegerPositions(false);
        titleFont.getData().setScale(1.35f);
        bodyFont.getData().setScale(1.18f);
    }

    void showDialogue(String title, String text) {
        showDialogue(title, text, null);
    }

    void showDialogue(String title, String text, Runnable closeAction) {
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
        this.closeAction = closeAction;
        activePrompt = null;
        selectedChoice = 0;
        dialogueOpen = true;
    }

    boolean isDialogueOpen() {
        return dialogueOpen;
    }

    boolean isBlocking() {
        return dialogueOpen || activePrompt != null;
    }

    boolean isChoiceOpen() {
        return activePrompt != null;
    }

    void showPrompt(GdxStoryState.Prompt prompt) {
        activePrompt = prompt;
        selectedChoice = 0;
        dialogueOpen = false;
        closeAction = null;
    }

    GdxStoryState.Prompt activePrompt() {
        return activePrompt;
    }

    int selectedChoiceIndex() {
        return selectedChoice;
    }

    void moveChoice(int amount) {
        if (activePrompt == null || activePrompt.choices.length == 0) {
            return;
        }
        selectedChoice += amount;
        if (selectedChoice < 0) {
            selectedChoice = activePrompt.choices.length - 1;
        }
        if (selectedChoice >= activePrompt.choices.length) {
            selectedChoice = 0;
        }
    }

    void closePrompt() {
        activePrompt = null;
        selectedChoice = 0;
    }

    void clear() {
        title = "";
        text = "";
        closeAction = null;
        dialogueOpen = false;
        activePrompt = null;
        selectedChoice = 0;
    }

    void closeDialogue() {
        if (!dialogueOpen) {
            return;
        }
        dialogueOpen = false;
        Runnable action = closeAction;
        closeAction = null;
        if (action != null) {
            action.run();
        }
    }

    void draw(SpriteBatch batch, String promptText) {
        if (activePrompt != null) {
            drawChoicePrompt(batch);
        } else if (dialogueOpen) {
            drawDialogue(batch);
        } else if (promptText != null && !promptText.isEmpty()) {
            drawPrompt(batch, promptText);
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        bodyFont.dispose();
        pixel.dispose();
    }

    private void drawDialogue(SpriteBatch batch) {
        float boxX = 64f;
        float boxY = 36f;
        float boxWidth = WORLD_WIDTH - boxX * 2f;
        float boxHeight = 150f;

        batch.setColor(0f, 0f, 0f, 0.78f);
        batch.draw(pixel, boxX, boxY, boxWidth, boxHeight);
        batch.setColor(0.68f, 0.79f, 0.74f, 0.95f);
        batch.draw(pixel, boxX, boxY + boxHeight - 3f, boxWidth, 3f);
        batch.draw(pixel, boxX, boxY, boxWidth, 3f);

        titleFont.setColor(0.86f, 0.94f, 0.88f, 1f);
        titleFont.draw(batch, title, boxX + 24f, boxY + boxHeight - 24f);

        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(batch, text, boxX + 24f, boxY + boxHeight - 58f,
                boxWidth - 48f, Align.left, true);

        String hint = "E / Enter / Space";
        glyphLayout.setText(bodyFont, hint);
        bodyFont.setColor(0.78f, 0.86f, 0.82f, 0.92f);
        bodyFont.draw(batch, hint, boxX + boxWidth - glyphLayout.width - 24f, boxY + 22f);
    }

    private void drawPrompt(SpriteBatch batch, String promptText) {
        glyphLayout.setText(bodyFont, promptText);
        float width = glyphLayout.width + 36f;
        float height = 34f;
        float x = WORLD_WIDTH * 0.5f - width * 0.5f;
        float y = 76f;

        batch.setColor(0f, 0f, 0f, 0.66f);
        batch.draw(pixel, x, y, width, height);
        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(batch, promptText, x + 18f, y + 23f);
    }

    private void drawChoicePrompt(SpriteBatch batch) {
        float boxX = 64f;
        float boxY = 30f;
        float boxWidth = WORLD_WIDTH - boxX * 2f;
        float boxHeight = 276f;

        batch.setColor(0f, 0f, 0f, 0.82f);
        batch.draw(pixel, boxX, boxY, boxWidth, boxHeight);
        batch.setColor(0.68f, 0.79f, 0.74f, 0.95f);
        batch.draw(pixel, boxX, boxY + boxHeight - 3f, boxWidth, 3f);
        batch.draw(pixel, boxX, boxY, boxWidth, 3f);

        titleFont.setColor(0.86f, 0.94f, 0.88f, 1f);
        titleFont.draw(batch, activePrompt.speaker, boxX + 24f, boxY + boxHeight - 24f);

        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(batch, activePrompt.text, boxX + 24f, boxY + boxHeight - 58f,
                boxWidth - 48f, Align.left, true);

        float choiceY = boxY + boxHeight - 132f;
        for (int i = 0; i < activePrompt.choices.length; i++) {
            boolean selected = i == selectedChoice;
            float rowY = choiceY - i * 34f;
            if (selected) {
                batch.setColor(0.55f, 0.75f, 0.66f, 0.28f);
                batch.draw(pixel, boxX + 20f, rowY - 22f, boxWidth - 40f, 28f);
            }
            if (selected) {
                bodyFont.setColor(0.86f, 1f, 0.92f, 1f);
            } else {
                bodyFont.setColor(Color.WHITE);
            }
            String marker = selected ? "> " : "  ";
            bodyFont.draw(batch, marker + activePrompt.choices[i].text, boxX + 30f, rowY);
        }

        String hint = "W/S or arrows - choose    E / Enter - confirm";
        glyphLayout.setText(bodyFont, hint);
        bodyFont.setColor(0.78f, 0.86f, 0.82f, 0.92f);
        bodyFont.draw(batch, hint, boxX + boxWidth - glyphLayout.width - 24f, boxY + 22f);
    }
}
