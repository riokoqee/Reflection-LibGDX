package reflection.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

final class GdxSceneActor {

    private static final int TILE_SIZE = 48;

    final String name;
    Texture texture;
    final float x;
    final float y;
    final float width;
    final float height;
    final boolean collision;
    final boolean floorLayer;
    final boolean npc;
    final Rectangle solidArea = new Rectangle();
    boolean visible = true;
    boolean interactable = true;
    boolean showName;
    String label;
    float labelCenterX;
    float labelBaselineY;
    boolean customLabel;
    Float renderSortY;

    private GdxSceneActor(String name, Texture texture, float x, float y, float width, float height,
                          boolean collision, boolean floorLayer, boolean npc) {
        this.name = name;
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.collision = collision;
        this.floorLayer = floorLayer;
        this.npc = npc;
        if (collision) {
            setSolidArea(0f, Math.max(0f, height - TILE_SIZE), width, TILE_SIZE);
        } else {
            setSolidArea(0f, 0f, width, height);
        }
    }

    static GdxSceneActor object(String name, Texture texture, float x, float y, float width, float height,
                                boolean collision, boolean floorLayer) {
        return new GdxSceneActor(name, texture, x, y, width, height, collision, floorLayer, false);
    }

    static GdxSceneActor npc(String name, Texture texture, float x, float y, float drawSize, boolean showName) {
        GdxSceneActor actor = new GdxSceneActor(name, texture, x, y, drawSize, drawSize, true, false, true);
        actor.showName = showName;
        actor.label = name;
        actor.setSolidArea(8f, 16f, 32f, 32f);
        return actor;
    }

    GdxSceneActor setSolidArea(float x, float y, float width, float height) {
        solidArea.set(x, y, width, height);
        return this;
    }

    GdxSceneActor setRenderSortY(float renderSortY) {
        this.renderSortY = renderSortY;
        return this;
    }

    GdxSceneActor setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    GdxSceneActor setInteractable(boolean interactable) {
        this.interactable = interactable;
        return this;
    }

    GdxSceneActor setTexture(Texture texture) {
        this.texture = texture;
        return this;
    }

    GdxSceneActor setWorldLabel(String label, float centerX, float baselineY) {
        this.label = label;
        this.labelCenterX = centerX;
        this.labelBaselineY = baselineY;
        this.customLabel = true;
        return this;
    }

    float sortY() {
        return renderSortY != null ? renderSortY : y;
    }

    boolean hasLabel() {
        return visible && ((showName && label != null && !label.isEmpty()) || customLabel);
    }

    float drawX() {
        return npc ? x - (width - TILE_SIZE) * 0.5f : x;
    }

    float drawTopY() {
        return npc ? y - (height - TILE_SIZE) : y;
    }

    Rectangle worldSolidArea(Rectangle target) {
        return target.set(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }

    void draw(SpriteBatch batch, float mapHeight) {
        if (!visible) {
            return;
        }
        batch.draw(texture, drawX(), mapHeight - drawTopY() - height, width, height);
    }
}
