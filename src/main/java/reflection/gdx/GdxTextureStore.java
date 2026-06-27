package reflection.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

final class GdxTextureStore implements Disposable {

    private final Map<String, Texture> textures = new HashMap<>();

    Texture get(String resourcePath) {
        String normalizedPath = normalize(resourcePath);
        return textures.computeIfAbsent(normalizedPath, this::load);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }

    private Texture load(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private String normalize(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return path.endsWith(".png") ? path : path + ".png";
    }
}
