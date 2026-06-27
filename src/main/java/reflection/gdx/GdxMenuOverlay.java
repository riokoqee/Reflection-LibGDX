package reflection.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

final class GdxMenuOverlay implements Disposable {

    private static final float WORLD_WIDTH = 960f;
    private static final float WORLD_HEIGHT = 540f;

    private final Texture background;
    private final Texture pixel;
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont menuFont = new BitmapFont();
    private final BitmapFont hintFont = new BitmapFont();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    GdxMenuOverlay(Texture background) {
        this.background = background;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        titleFont.setUseIntegerPositions(false);
        menuFont.setUseIntegerPositions(false);
        hintFont.setUseIntegerPositions(false);
        titleFont.getData().setScale(3.0f);
        menuFont.getData().setScale(1.55f);
        hintFont.getData().setScale(1.0f);
    }

    void drawTitle(SpriteBatch batch, String[] items, int selectedIndex) {
        batch.setColor(Color.WHITE);
        batch.draw(background, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        batch.setColor(0f, 0f, 0f, 0.34f);
        batch.draw(pixel, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);

        titleFont.setColor(0.9f, 0.96f, 0.92f, 1f);
        titleFont.draw(batch, "Reflection", 86f, 390f);

        hintFont.setColor(0.72f, 0.8f, 0.76f, 1f);
        hintFont.draw(batch, "LibGDX engine prototype", 92f, 352f);

        drawMenuItems(batch, items, selectedIndex, 104f, 276f);
        drawFooter(batch, "W/S or arrows - select    E / Enter - confirm");
    }

    void drawPause(SpriteBatch batch, String[] items, int selectedIndex) {
        batch.setColor(0f, 0f, 0f, 0.62f);
        batch.draw(pixel, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        batch.setColor(0.04f, 0.07f, 0.07f, 0.88f);
        batch.draw(pixel, 296f, 86f, 368f, 354f);
        batch.setColor(0.62f, 0.76f, 0.7f, 0.95f);
        batch.draw(pixel, 296f, 436f, 368f, 3f);
        batch.draw(pixel, 296f, 86f, 368f, 3f);

        titleFont.setColor(0.9f, 0.96f, 0.92f, 1f);
        glyphLayout.setText(titleFont, "Pause");
        titleFont.draw(batch, "Pause", WORLD_WIDTH * 0.5f - glyphLayout.width * 0.5f, 386f);

        drawMenuItems(batch, items, selectedIndex, 350f, 300f);
        drawFooter(batch, "Esc - resume    W/S or arrows - select    E / Enter - confirm");
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        menuFont.dispose();
        hintFont.dispose();
        pixel.dispose();
    }

    private void drawMenuItems(SpriteBatch batch, String[] items, int selectedIndex, float x, float startY) {
        for (int i = 0; i < items.length; i++) {
            boolean selected = i == selectedIndex;
            float y = startY - i * 48f;
            if (selected) {
                batch.setColor(0.58f, 0.78f, 0.68f, 0.22f);
                batch.draw(pixel, x - 16f, y - 31f, 262f, 40f);
            }
            if (selected) {
                menuFont.setColor(0.86f, 1f, 0.92f, 1f);
            } else {
                menuFont.setColor(Color.WHITE);
            }
            menuFont.draw(batch, (selected ? "> " : "  ") + items[i], x, y);
        }
    }

    private void drawFooter(SpriteBatch batch, String text) {
        glyphLayout.setText(hintFont, text);
        hintFont.setColor(0.7f, 0.78f, 0.74f, 1f);
        hintFont.draw(batch, text, WORLD_WIDTH * 0.5f - glyphLayout.width * 0.5f, 34f);
    }
}
