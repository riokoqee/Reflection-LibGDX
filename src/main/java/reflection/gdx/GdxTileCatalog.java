package reflection.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

final class GdxTileCatalog implements Disposable {

    private final Map<Integer, TileDef> tiles = new HashMap<>();
    private final Map<String, Texture> textureCache = new HashMap<>();

    GdxTileCatalog() {
        put(0, "grass00", false);
        put(1, "grass00", true);
        put(2, "grass00", true);
        put(3, "grass00", false);
        put(4, "grass00", true);
        put(5, "grass00", false);
        put(6, "grass00", false);
        put(7, "grass00", false);
        put(8, "grass00", false);
        put(9, "grass00", false);
        put(10, "grass00", false);
        put(11, "grass01", false);
        put(12, "water00", true);
        put(13, "water01", true);
        put(14, "water02", true);
        put(15, "water03", true);
        put(16, "water04", true);
        put(17, "water05", true);
        put(18, "water06", true);
        put(19, "water07", true);
        put(20, "water08", true);
        put(21, "water09", true);
        put(22, "water10", true);
        put(23, "water11", true);
        put(24, "water12", true);
        put(25, "water13", true);
        put(26, "road00", false);
        put(27, "road01", false);
        put(28, "road02", false);
        put(29, "road03", false);
        put(30, "road04", false);
        put(31, "road05", false);
        put(32, "road06", false);
        put(33, "road07", false);
        put(34, "road08", false);
        put(35, "road09", false);
        put(36, "road10", false);
        put(37, "road11", false);
        put(38, "road12", false);
        put(39, "earth", false);
        put(40, "wall", true);
        put(41, "tree", true);
        put(42, "hut", false);
        put(43, "floor01", false);
        put(44, "table01", true);
        put(45, "home_generated/void", true);
        put(46, "home_generated/floor_wood", false);
        put(47, "home_generated/wall_white", true);
        put(48, "home_generated/wall_top", true);
        put(49, "home_generated/floor_dark", false);
        put(50, "forest_generated/ground", false);
        put(51, "forest_generated/path", false);
        put(52, "forest_generated/edge", true);
        put(53, "forest_generated/flowers", false);
        put(54, "village_stone_road", false);
        put(55, "village_stone_road_alt", false);
        put(56, "village_stone_road_dark", false);
        put(57, "mountain_void", true);
        put(58, "mountain_ground", false);
        put(59, "mountain_path", false);
        put(60, "mountain_cliff", true);
        put(61, "mountain_snow", false);
        put(62, "mountain_rock", true);
        put(63, "mountain_stairs", false);
    }

    TileDef get(int tileId) {
        return tiles.get(tileId);
    }

    boolean isBlocked(int tileId) {
        TileDef tile = tiles.get(tileId);
        return tile == null || tile.collision;
    }

    @Override
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
        tiles.clear();
    }

    private void put(int tileId, String imageName, boolean collision) {
        String path = "tiles/" + imageName + ".png";
        Texture texture = textureCache.computeIfAbsent(path, this::loadNearestTexture);
        tiles.put(tileId, new TileDef(texture, collision));
    }

    private Texture loadNearestTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    static final class TileDef {
        final Texture texture;
        final boolean collision;

        private TileDef(Texture texture, boolean collision) {
            this.texture = texture;
            this.collision = collision;
        }
    }
}
