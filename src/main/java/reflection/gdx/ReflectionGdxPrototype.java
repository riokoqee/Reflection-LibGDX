package reflection.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

final class ReflectionGdxPrototype extends ApplicationAdapter {

    private static final float WORLD_WIDTH = 960f;
    private static final float WORLD_HEIGHT = 540f;
    private static final int TILE_SIZE = 48;
    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 32;
    private static final int FRAMES_PER_DIRECTION = 6;
    private static final int DIRECTION_RIGHT = 0;
    private static final int DIRECTION_UP = 1;
    private static final int DIRECTION_LEFT = 2;
    private static final int DIRECTION_DOWN = 3;
    private static final float PLAYER_DRAW_WIDTH = TILE_SIZE * 0.82f;
    private static final float PLAYER_DRAW_HEIGHT = TILE_SIZE * 1.64f;
    private static final float PLAYER_HITBOX_X = 12f;
    private static final float PLAYER_HITBOX_Y = 32f;
    private static final float PLAYER_HITBOX_WIDTH = 24f;
    private static final float PLAYER_HITBOX_HEIGHT = 12f;
    private static final float WALK_SPEED = 240f;
    private static final float SPRINT_SPEED = 420f;

    private SpriteBatch batch;
    private FitViewport viewport;
    private FitViewport uiViewport;
    private GdxTileCatalog tileCatalog;
    private GdxTextureStore textureStore;
    private GdxScene scene;
    private GdxInteractionOverlay overlay;
    private GdxStoryState story;
    private GdxMapData[] maps;
    private int currentMapIndex;
    private Texture heroIdleSheet;
    private Texture heroRunSheet;
    private TextureRegion[][] heroIdleFrames;
    private TextureRegion[][] heroRunFrames;
    private float playerX;
    private float playerY;
    private int direction = DIRECTION_DOWN;
    private float animationTime;
    private float titleUpdateTimer;
    private boolean tvOn;
    private boolean bedroomLampOn;
    private boolean phoneDresserOpen;
    private boolean hasLantern;
    private GdxSceneActor interactionTarget;
    private final Rectangle playerHitbox = new Rectangle();
    private final Rectangle interactionArea = new Rectangle();

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        uiViewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        tileCatalog = new GdxTileCatalog();
        textureStore = new GdxTextureStore();
        scene = GdxScene.create(textureStore);
        overlay = new GdxInteractionOverlay();
        story = new GdxStoryState();
        maps = new GdxMapData[] {
                GdxMapData.load("Apartment", "maps/apartment.txt", TILE_SIZE, 16, 12),
                GdxMapData.load("Forest of Doubts", "maps/forest_doubts.txt", TILE_SIZE, 23, 43),
                GdxMapData.load("Village", "maps/map02.txt", TILE_SIZE, 23, 23),
                GdxMapData.load("Mountain", "maps/map03.txt", TILE_SIZE, 24, 38),
                GdxMapData.load("Library", "maps/library.txt", TILE_SIZE, 24, 21)
        };
        heroIdleSheet = loadNearestTexture("player/new/Amelia_idle_anim_16x16.png");
        heroRunSheet = loadNearestTexture("player/new/Amelia_run_16x16.png");
        heroIdleFrames = sliceHeroSheet(heroIdleSheet);
        heroRunFrames = sliceHeroSheet(heroRunSheet);
        switchMap(0);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
        updateCamera();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        handleInput(delta);
        updateInteractionTarget();
        updateCamera();
        updateTitle(delta);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply(false);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        drawVisibleTiles();
        scene.drawFloorObjects(batch, currentMapIndex, currentMap().pixelHeight(TILE_SIZE));
        scene.drawSortedActors(batch, currentMapIndex, currentMap().pixelHeight(TILE_SIZE), playerY,
                () -> drawPlayer(!overlay.isBlocking() && isMovingInputActive()));
        batch.end();

        uiViewport.apply(false);
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        overlay.draw(batch, promptText());
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (tileCatalog != null) {
            tileCatalog.dispose();
        }
        if (scene != null) {
            scene.dispose();
        }
        if (overlay != null) {
            overlay.dispose();
        }
        if (textureStore != null) {
            textureStore.dispose();
        }
        if (heroIdleSheet != null) {
            heroIdleSheet.dispose();
        }
        if (heroRunSheet != null) {
            heroRunSheet.dispose();
        }
    }

    private Texture loadNearestTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private TextureRegion[][] sliceHeroSheet(Texture sheet) {
        TextureRegion[][] frames = new TextureRegion[4][FRAMES_PER_DIRECTION];
        for (int directionIndex = 0; directionIndex < frames.length; directionIndex++) {
            for (int frameIndex = 0; frameIndex < FRAMES_PER_DIRECTION; frameIndex++) {
                int sourceColumn = directionIndex * FRAMES_PER_DIRECTION + frameIndex;
                frames[directionIndex][frameIndex] = new TextureRegion(
                        sheet,
                        sourceColumn * FRAME_WIDTH,
                        0,
                        FRAME_WIDTH,
                        FRAME_HEIGHT
                );
            }
        }
        return frames;
    }

    private void handleMapShortcuts() {
        if (overlay.isBlocking()) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            switchMap(0);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            switchMap(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            switchMap(2);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            switchMap(3);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            switchMap(4);
        }
    }

    private void switchMap(int index) {
        currentMapIndex = MathUtils.clamp(index, 0, maps.length - 1);
        GdxMapData map = currentMap();
        playerX = map.startX;
        playerY = map.startY;
        animationTime = 0f;
        updateCamera();
    }

    private void switchMap(int index, int col, int row) {
        currentMapIndex = MathUtils.clamp(index, 0, maps.length - 1);
        playerX = col * TILE_SIZE;
        playerY = row * TILE_SIZE;
        animationTime = 0f;
        updateCamera();
    }

    private void handleInput(float delta) {
        if (overlay.isChoiceOpen()) {
            handleChoiceInput();
            return;
        }
        if (overlay.isDialogueOpen()) {
            if (advancePressed()) {
                overlay.closeDialogue();
            }
            return;
        }

        handleMapShortcuts();
        if (interactPressed()) {
            interact();
            return;
        }
        updatePlayer(delta);
    }

    private void handleChoiceInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            overlay.moveChoice(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            overlay.moveChoice(1);
        }
        if (interactPressed()) {
            chooseStoryAnswer();
        }
    }

    private void chooseStoryAnswer() {
        GdxStoryState.ChoiceOutcome outcome = story.choose(overlay.activePrompt(), overlay.selectedChoiceIndex());
        overlay.closePrompt();
        if (outcome.nextPrompt != null) {
            overlay.showPrompt(outcome.nextPrompt);
            return;
        }
        Runnable closeAction = outcome.hasDestination()
                ? () -> switchMap(outcome.mapIndex, outcome.column, outcome.row)
                : null;
        overlay.showDialogue(outcome.speaker, outcome.text, closeAction);
    }

    private void updatePlayer(float delta) {
        float moveX = 0f;
        float moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveY -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveY += 1f;
        }

        if (moveX != 0f || moveY != 0f) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= length;
            moveY /= length;
            if (Math.abs(moveX) > Math.abs(moveY)) {
                direction = moveX < 0f ? DIRECTION_LEFT : DIRECTION_RIGHT;
            } else {
                direction = moveY < 0f ? DIRECTION_UP : DIRECTION_DOWN;
            }

            float speed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) ? SPRINT_SPEED : WALK_SPEED;
            moveHorizontally(moveX * speed * delta);
            moveVertically(moveY * speed * delta);
        }
        animationTime += delta;
    }

    private boolean interactPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
    }

    private boolean advancePressed() {
        return interactPressed() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    private boolean isMovingInputActive() {
        return Gdx.input.isKeyPressed(Input.Keys.A) ||
                Gdx.input.isKeyPressed(Input.Keys.D) ||
                Gdx.input.isKeyPressed(Input.Keys.W) ||
                Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.RIGHT) ||
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isKeyPressed(Input.Keys.DOWN);
    }

    private void moveHorizontally(float deltaX) {
        if (deltaX != 0f && !collides(playerX + deltaX, playerY)) {
            playerX += deltaX;
        }
    }

    private void moveVertically(float deltaY) {
        if (deltaY != 0f && !collides(playerX, playerY + deltaY)) {
            playerY += deltaY;
        }
    }

    private boolean collides(float nextX, float nextY) {
        GdxMapData map = currentMap();
        float left = nextX + PLAYER_HITBOX_X;
        float right = left + PLAYER_HITBOX_WIDTH;
        float top = nextY + PLAYER_HITBOX_Y;
        float bottom = top + PLAYER_HITBOX_HEIGHT;

        if (left < 0f || top < 0f || right >= map.pixelWidth(TILE_SIZE) || bottom >= map.pixelHeight(TILE_SIZE)) {
            return true;
        }

        playerHitbox.set(left, top, PLAYER_HITBOX_WIDTH, PLAYER_HITBOX_HEIGHT);
        if (scene.collides(currentMapIndex, playerHitbox)) {
            return true;
        }

        int leftColumn = (int) Math.floor(left / TILE_SIZE);
        int rightColumn = (int) Math.floor((right - 0.01f) / TILE_SIZE);
        int topRow = (int) Math.floor(top / TILE_SIZE);
        int bottomRow = (int) Math.floor((bottom - 0.01f) / TILE_SIZE);

        for (int row = topRow; row <= bottomRow; row++) {
            for (int column = leftColumn; column <= rightColumn; column++) {
                if (tileCatalog.isBlocked(map.tileAt(column, row))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateInteractionTarget() {
        if (overlay.isBlocking()) {
            interactionTarget = null;
            return;
        }
        interactionTarget = scene.findInteractionTarget(currentMapIndex, currentInteractionArea());
    }

    private Rectangle currentInteractionArea() {
        float x = playerX + PLAYER_HITBOX_X;
        float y = playerY + PLAYER_HITBOX_Y;
        float width = PLAYER_HITBOX_WIDTH;
        float height = PLAYER_HITBOX_HEIGHT;
        float reach = TILE_SIZE * 0.62f;

        switch (direction) {
            case DIRECTION_UP:
                y -= reach;
                break;
            case DIRECTION_DOWN:
                y += reach;
                break;
            case DIRECTION_LEFT:
                x -= reach;
                break;
            case DIRECTION_RIGHT:
                x += reach;
                break;
            default:
                break;
        }

        return interactionArea.set(x - 8f, y - 8f, width + 16f, height + 16f);
    }

    private void interact() {
        if (interactionTarget == null) {
            return;
        }

        switch (interactionTarget.name) {
            case "Door":
                overlay.showDialogue("Door", "The apartment door opens. The Forest of Doubts is waiting outside.",
                        () -> switchMap(1, 23, 43));
                break;
            case "Village Library Door":
                overlay.showDialogue("Library", "The door gives way to a quiet room filled with old shelves.",
                        () -> switchMap(4, 24, 21));
                break;
            case "Library Exit":
                overlay.showDialogue("Library", "You step back into the village square.",
                        () -> switchMap(2, 28, 10));
                break;
            case "TV":
                tvOn = !tvOn;
                scene.setTvOn(tvOn);
                overlay.showDialogue("TV", tvOn
                        ? "The screen wakes up with a soft glow. The room feels less empty."
                        : "The screen goes dark again.");
                break;
            case "Bedroom Lamp":
            case "Dresser":
                bedroomLampOn = !bedroomLampOn;
                overlay.showDialogue("Lamp", bedroomLampOn
                        ? "Warm light spreads over the bedroom."
                        : "The bedroom returns to a quiet dimness.");
                break;
            case "Phone Dresser":
                phoneDresserOpen = true;
                scene.setPhoneDresserOpen(true);
                overlay.showDialogue("Dresser", "The drawer slides open. A phone is inside, already lit by a message from mom.");
                break;
            case "Dirty Dishes":
            case "Kitchen Wall Sink":
                scene.hideDirtyDishes();
                overlay.showDialogue("Kitchen", "The dishes are cleared away. Running water cuts through the heavy silence.");
                break;
            case "Lantern":
                hasLantern = true;
                scene.pickupLantern();
                overlay.showDialogue("Lantern", "You pick up the lantern. Its light makes the forest path easier to read.");
                break;
            case "Bed":
                overlay.showDialogue("Bed", "The blanket is pulled straight. The room looks a little calmer.");
                break;
            case "Sofa":
                overlay.showDialogue("Sofa", tvOn
                        ? "You sit down for a moment. The TV noise fills the room."
                        : "It would feel easier to rest here if the TV were on first.");
                break;
            case "Old Photo":
                overlay.showDialogue("Photo", "The photo is worn at the edges, but the feeling inside it is still clear.");
                break;
            case "Mirror":
            case "Bathroom Mirror":
                overlay.showDialogue("Mirror", "For a second, the reflection seems slower than you are.");
                break;
            case "Lost Lantern":
                overlay.showDialogue("Lost Lantern", "A small lantern lies cold in the grass, as if someone left in a hurry.");
                break;
            case "Wounded Bird":
                overlay.showDialogue("Bird", "The bird watches you without moving. It needs gentleness, not noise.");
                break;
            case "Mountain Fork":
                overlay.showDialogue("Fork", "Two paths split ahead. Both feel honest. Neither feels easy.");
                break;
            case "Traveler Pack":
                overlay.showDialogue("Pack", "The pack is light, but it has clearly been carried for a long time.");
                break;
            case "Shadow":
                interactShadow();
                break;
            case "Child":
                openStoryPromptOrMessage("Child",
                        "The swing creaks softly. The child points toward the deeper path.");
                break;
            case "Friend":
                openStoryPromptOrMessage("Friend",
                        "The friend smiles carefully, like they are waiting for you to choose your words.");
                break;
            case "Elder":
                openStoryPromptOrMessage("Elder",
                        "The elder closes the book. The next answer is somewhere beyond the village.");
                break;
            case "Warrior":
                openStoryPromptOrMessage("Warrior",
                        "The warrior stands beside the fire. The final climb begins when you stop running from yourself.");
                break;
            case "Traveler":
                overlay.showDialogue("Traveler", "The traveler adjusts their pack. Some burdens only become lighter when named.");
                break;
            default:
                overlay.showDialogue(displayName(interactionTarget.name), "There is nothing more to do here yet.");
                break;
        }
    }

    private void interactShadow() {
        GdxStoryState.Prompt prompt = story.promptForActor("Shadow", currentMapIndex, hasLantern);
        if (prompt != null) {
            overlay.showPrompt(prompt);
            return;
        }
        overlay.showDialogue("Shadow", currentMapIndex == 1 && !hasLantern
                ? "Without a lantern, the voice is almost impossible to follow."
                : "The shadow says nothing, but the silence has shape.");
    }

    private void openStoryPromptOrMessage(String actorName, String fallbackText) {
        GdxStoryState.Prompt prompt = story.promptForActor(actorName, currentMapIndex, hasLantern);
        if (prompt != null) {
            overlay.showPrompt(prompt);
        } else {
            overlay.showDialogue(actorName, fallbackText);
        }
    }

    private void updateCamera() {
        if (viewport == null || maps == null) {
            return;
        }
        GdxMapData map = currentMap();
        Camera camera = viewport.getCamera();
        float mapWidth = map.pixelWidth(TILE_SIZE);
        float mapHeight = map.pixelHeight(TILE_SIZE);
        float halfWidth = viewport.getWorldWidth() * 0.5f;
        float halfHeight = viewport.getWorldHeight() * 0.5f;
        float targetX = playerX + TILE_SIZE * 0.5f;
        float targetY = mapHeight - playerY - TILE_SIZE * 0.5f;

        if (mapWidth <= viewport.getWorldWidth()) {
            targetX = mapWidth * 0.5f;
        } else {
            targetX = MathUtils.clamp(targetX, halfWidth, mapWidth - halfWidth);
        }

        if (mapHeight <= viewport.getWorldHeight()) {
            targetY = mapHeight * 0.5f;
        } else {
            targetY = MathUtils.clamp(targetY, halfHeight, mapHeight - halfHeight);
        }

        camera.position.set(targetX, targetY, 0f);
        camera.update();
    }

    private void drawVisibleTiles() {
        GdxMapData map = currentMap();
        Camera camera = viewport.getCamera();
        float mapHeight = map.pixelHeight(TILE_SIZE);
        float left = camera.position.x - viewport.getWorldWidth() * 0.5f;
        float right = camera.position.x + viewport.getWorldWidth() * 0.5f;
        float bottom = camera.position.y - viewport.getWorldHeight() * 0.5f;
        float top = camera.position.y + viewport.getWorldHeight() * 0.5f;

        int firstColumn = Math.max(0, (int) Math.floor(left / TILE_SIZE) - 1);
        int lastColumn = Math.min(map.columns - 1, (int) Math.floor(right / TILE_SIZE) + 1);
        int firstRow = Math.max(0, (int) Math.floor((mapHeight - top) / TILE_SIZE) - 1);
        int lastRow = Math.min(map.rows - 1, (int) Math.floor((mapHeight - bottom) / TILE_SIZE) + 1);

        for (int row = firstRow; row <= lastRow; row++) {
            float drawY = mapHeight - (row + 1) * TILE_SIZE;
            for (int column = firstColumn; column <= lastColumn; column++) {
                GdxTileCatalog.TileDef tile = tileCatalog.get(map.tileAt(column, row));
                if (tile != null) {
                    batch.draw(tile.texture, column * TILE_SIZE, drawY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawPlayer(boolean moving) {
        TextureRegion[][] frames = moving ? heroRunFrames : heroIdleFrames;
        float frameDuration = moving ? 0.09f : 0.18f;
        int frameIndex = ((int) (animationTime / frameDuration)) % FRAMES_PER_DIRECTION;
        TextureRegion frame = frames[direction][frameIndex];
        GdxMapData map = currentMap();
        float drawX = playerX - (PLAYER_DRAW_WIDTH - TILE_SIZE) * 0.5f;
        float drawY = map.pixelHeight(TILE_SIZE) - playerY - TILE_SIZE;
        batch.draw(frame, drawX, drawY, PLAYER_DRAW_WIDTH, PLAYER_DRAW_HEIGHT);
    }

    private void updateTitle(float delta) {
        titleUpdateTimer += delta;
        if (titleUpdateTimer >= 0.5f) {
            titleUpdateTimer = 0f;
            Gdx.graphics.setTitle("Reflection LibGDX - " + currentMap().name + " - " +
                    Gdx.graphics.getFramesPerSecond() + " FPS");
        }
    }

    private String promptText() {
        if (interactionTarget == null || overlay.isBlocking()) {
            return "";
        }
        return "E  " + displayName(interactionTarget.name);
    }

    private String displayName(String name) {
        if (name == null || name.isEmpty()) {
            return "Interact";
        }
        if (name.startsWith("village_house")) {
            return "House";
        }
        if (name.startsWith("tree_")) {
            return "Tree";
        }
        if (name.startsWith("decoration_") || name.startsWith("mountain_")) {
            return "Inspect";
        }
        if ("Village Library Door".equals(name)) {
            return "Library";
        }
        return name;
    }

    private GdxMapData currentMap() {
        return maps[currentMapIndex];
    }
}
