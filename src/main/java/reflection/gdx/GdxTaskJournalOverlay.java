package reflection.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

final class GdxTaskJournalOverlay implements Disposable {

    private static final float WORLD_WIDTH = 960f;
    private static final float PANEL_WIDTH = 286f;
    private static final float PANEL_HEIGHT = 396f;
    private static final float PANEL_TOP = 468f;

    private final Texture pixel;
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont bodyFont = new BitmapFont();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    GdxTaskJournalOverlay() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        titleFont.setUseIntegerPositions(false);
        bodyFont.setUseIntegerPositions(false);
        titleFont.getData().setScale(1.45f);
        bodyFont.getData().setScale(1.05f);
    }

    void draw(SpriteBatch batch, GdxTaskJournalState tasks, float progress) {
        if (progress <= 0.01f) {
            return;
        }

        float eased = 1f - (1f - progress) * (1f - progress);
        float x = WORLD_WIDTH - PANEL_WIDTH * eased;
        float y = PANEL_TOP - PANEL_HEIGHT;

        batch.setColor(0.04f, 0.05f, 0.04f, 0.82f);
        batch.draw(pixel, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        batch.setColor(0.78f, 0.69f, 0.52f, 0.96f);
        batch.draw(pixel, x + 10f, y + PANEL_HEIGHT - 5f, PANEL_WIDTH - 20f, 3f);
        batch.draw(pixel, x + 10f, y + 13f, PANEL_WIDTH - 20f, 3f);

        titleFont.setColor(0.93f, 0.91f, 0.82f, 1f);
        titleFont.draw(batch, "Plan", x + 28f, y + PANEL_HEIGHT - 28f);

        String progressText = tasks.completedCount() + " / " + tasks.count();
        glyphLayout.setText(bodyFont, progressText);
        bodyFont.setColor(0.77f, 0.82f, 0.73f, 1f);
        bodyFont.draw(batch, progressText, x + PANEL_WIDTH - glyphLayout.width - 28f, y + PANEL_HEIGHT - 29f);

        float rowY = y + PANEL_HEIGHT - 78f;
        for (int i = 0; i < tasks.count(); i++) {
            drawTask(batch, tasks.label(i), tasks.complete(i), x + 28f, rowY - i * 42f);
        }

        bodyFont.setColor(0.62f, 0.67f, 0.61f, 1f);
        bodyFont.draw(batch, "I - close", x + 28f, y + 39f);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        bodyFont.dispose();
        pixel.dispose();
    }

    private void drawTask(SpriteBatch batch, String label, boolean complete, float x, float baselineY) {
        batch.setColor(complete ? 0.55f : 0.18f, complete ? 0.74f : 0.22f, complete ? 0.54f : 0.2f, 0.84f);
        batch.draw(pixel, x, baselineY - 20f, 24f, 24f);
        batch.setColor(0.04f, 0.05f, 0.04f, 0.92f);
        batch.draw(pixel, x + 4f, baselineY - 16f, 16f, 16f);

        if (complete) {
            bodyFont.setColor(0.75f, 0.95f, 0.72f, 1f);
            bodyFont.draw(batch, "x", x + 8f, baselineY);
        }

        bodyFont.setColor(complete ? 0.82f : 0.94f, complete ? 0.88f : 0.92f, complete ? 0.78f : 0.84f, 1f);
        bodyFont.draw(batch, label, x + 38f, baselineY, PANEL_WIDTH - 70f, Align.left, true);
    }
}
