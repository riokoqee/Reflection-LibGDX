package reflection.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GdxScene implements Disposable {

    private static final int TILE_SIZE = 48;
    private static final int APARTMENT = 0;
    private static final int FOREST_DOUBTS = 1;
    private static final int VILLAGE = 2;
    private static final int MOUNTAIN = 3;
    private static final int LIBRARY = 4;
    private static final int MAP_COUNT = 5;
    private static final float VILLAGE_HOUSE_SCALE = 1.5f;

    private final GdxTextureStore textureStore;
    private final List<GdxSceneActor>[] objects;
    private final List<GdxSceneActor>[] npcs;
    private final List<DrawEntry> drawEntries = new ArrayList<>();
    private final Rectangle collisionBounds = new Rectangle();
    private final Rectangle interactionBounds = new Rectangle();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Texture pixel;
    private GdxSceneActor tv;
    private GdxSceneActor phoneDresser;
    private GdxSceneActor dirtyDishes;
    private GdxSceneActor lantern;

    private GdxScene(GdxTextureStore textureStore) {
        this.textureStore = textureStore;
        objects = createLayers();
        npcs = createLayers();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        font.setUseIntegerPositions(false);
        font.getData().setScale(1.12f);
    }

    static GdxScene create(GdxTextureStore textureStore) {
        GdxScene scene = new GdxScene(textureStore);
        scene.placeApartmentObjects();
        scene.placeForestObjects();
        scene.placeVillageObjects();
        scene.placeMountainObjects();
        scene.placeLibraryObjects();
        scene.placeNpcs();
        return scene;
    }

    void drawFloorObjects(SpriteBatch batch, int mapIndex, float mapHeight) {
        for (GdxSceneActor actor : objects[mapIndex]) {
            if (actor.floorLayer) {
                actor.draw(batch, mapHeight);
            }
        }
    }

    void drawSortedActors(SpriteBatch batch, int mapIndex, float mapHeight, float playerSortY,
                          Runnable playerRenderer) {
        drawEntries.clear();
        for (GdxSceneActor actor : objects[mapIndex]) {
            if (!actor.floorLayer && actor.visible) {
                drawEntries.add(DrawEntry.actor(actor));
            }
        }
        for (GdxSceneActor actor : npcs[mapIndex]) {
            if (actor.visible) {
                drawEntries.add(DrawEntry.actor(actor));
            }
        }
        drawEntries.add(DrawEntry.player(playerSortY, playerRenderer));
        drawEntries.sort(Comparator.comparingDouble(DrawEntry::sortY));

        for (DrawEntry entry : drawEntries) {
            if (entry.playerRenderer != null) {
                entry.playerRenderer.run();
            } else {
                entry.actor.draw(batch, mapHeight);
                drawLabel(batch, entry.actor, mapHeight);
            }
        }
    }

    boolean collides(int mapIndex, Rectangle playerHitbox) {
        return collidesWithLayer(objects[mapIndex], playerHitbox) || collidesWithLayer(npcs[mapIndex], playerHitbox);
    }

    GdxSceneActor findInteractionTarget(int mapIndex, Rectangle area) {
        GdxSceneActor npc = findTarget(npcs[mapIndex], area);
        if (npc != null) {
            return npc;
        }
        return findTarget(objects[mapIndex], area);
    }

    void setTvOn(boolean on) {
        if (tv != null) {
            tv.setTexture(textureStore.get(on ? "/objects/home/tv_plasma_on" : "/objects/home/tv_plasma_off"));
        }
    }

    void setPhoneDresserOpen(boolean open) {
        if (phoneDresser != null) {
            phoneDresser.setTexture(textureStore.get(open ? "/objects/home/dresser_open_phone" : "/objects/home/dresser"));
        }
    }

    void hideDirtyDishes() {
        if (dirtyDishes != null) {
            dirtyDishes.setVisible(false).setInteractable(false);
        }
    }

    void pickupLantern() {
        if (lantern != null) {
            lantern.setVisible(false).setInteractable(false);
        }
    }

    @Override
    public void dispose() {
        font.dispose();
        pixel.dispose();
    }

    private boolean collidesWithLayer(List<GdxSceneActor> layer, Rectangle playerHitbox) {
        for (GdxSceneActor actor : layer) {
            if (!actor.visible || !actor.collision || actor.floorLayer) {
                continue;
            }
            actor.worldSolidArea(collisionBounds);
            if (collisionBounds.width > 0f && collisionBounds.height > 0f && collisionBounds.overlaps(playerHitbox)) {
                return true;
            }
        }
        return false;
    }

    private GdxSceneActor findTarget(List<GdxSceneActor> layer, Rectangle area) {
        GdxSceneActor fallback = null;
        for (GdxSceneActor actor : layer) {
            if (!actor.interactable || actor.floorLayer) {
                continue;
            }
            actor.worldSolidArea(interactionBounds);
            if (interactionBounds.width <= 0f || interactionBounds.height <= 0f || !interactionBounds.overlaps(area)) {
                continue;
            }
            if (!actor.collision) {
                return actor;
            }
            if (fallback == null) {
                fallback = actor;
            }
        }
        return fallback;
    }

    private void drawLabel(SpriteBatch batch, GdxSceneActor actor, float mapHeight) {
        if (!actor.hasLabel()) {
            return;
        }
        glyphLayout.setText(font, actor.label);
        float centerX = actor.customLabel ? actor.x + actor.labelCenterX : actor.drawX() + actor.width * 0.5f;
        float baselineTopY = actor.customLabel ? actor.y + actor.labelBaselineY : actor.drawTopY() - 10f;
        float baselineY = mapHeight - baselineTopY;
        float textX = centerX - glyphLayout.width * 0.5f;
        float plateX = textX - 7f;
        float plateY = baselineY - glyphLayout.height - 5f;
        float plateWidth = glyphLayout.width + 14f;
        float plateHeight = glyphLayout.height + 8f;

        batch.setColor(0f, 0f, 0f, 0.68f);
        batch.draw(pixel, plateX, plateY, plateWidth, plateHeight);
        batch.setColor(Color.WHITE);
        font.draw(batch, glyphLayout, textX, baselineY);
    }

    private void placeApartmentObjects() {
        float bedX = TILE_SIZE * 22f - drawSize(2.8f);
        float bedY = TILE_SIZE * 7f;
        addObjectAtPixel(APARTMENT, "Bed", "/objects/home/bed", bedX, bedY, 2.8f, 2.8f, true, false)
                .setSolidArea(TILE_SIZE / 2f, TILE_SIZE / 5f, TILE_SIZE * 7f / 4f, TILE_SIZE * 2f);

        float dresserWidth = drawSize(1.25f);
        float dresserX = bedX - dresserWidth - TILE_SIZE / 8f;
        float dresserY = TILE_SIZE * 7f;

        float mirrorWidth = drawSize(1.6f);
        float mirrorX = dresserX - mirrorWidth - TILE_SIZE / 4f;
        float mirrorY = TILE_SIZE * 6f + TILE_SIZE / 4f;
        addObjectAtPixel(APARTMENT, "Mirror", "/objects/home/mirrors/mirror_floor_wood_brown",
                mirrorX, mirrorY, 1.6f, 2.4f, true, false)
                .setSolidArea(TILE_SIZE / 3f, TILE_SIZE / 3f, TILE_SIZE, TILE_SIZE * 13f / 8f);

        float lampWidth = drawSize(0.65f);
        addObjectAtPixel(APARTMENT, "Bedroom Lamp", "/objects/home/decor/bedroom_lamp_gold",
                dresserX + (dresserWidth - lampWidth) / 2f, dresserY - TILE_SIZE / 3f,
                0.65f, 0.75f, false, false)
                .setSolidArea(TILE_SIZE / 8f, TILE_SIZE / 8f, TILE_SIZE * 2f / 5f, TILE_SIZE / 2f)
                .setRenderSortY(dresserY + TILE_SIZE);

        addObjectAtPixel(APARTMENT, "Dresser", "/objects/home/dresser", dresserX, dresserY,
                1.25f, 1.25f, true, false);

        addObjectAtPixel(APARTMENT, "Bedroom Plant", "/objects/home/decor/plant_tall_green",
                TILE_SIZE * 22f - drawSize(0.85f), TILE_SIZE * 15f - drawSize(1.25f),
                0.85f, 1.25f, true, false)
                .setSolidArea(TILE_SIZE / 6f, TILE_SIZE * 5f / 6f, TILE_SIZE / 2f, TILE_SIZE / 3f);

        float tableX = TILE_SIZE * 14f + TILE_SIZE / 4f;
        float tableY = TILE_SIZE * 13f;
        float tableWidth = drawSize(1.45f);
        float tableHeight = drawSize(0.8f);
        float photoWidth = drawSize(0.68f);
        float photoHeight = drawSize(0.5f);
        addObjectAtPixel(APARTMENT, "Old Photo", "/objects/story/old_photo",
                tableX + (tableWidth - photoWidth) / 2f, tableY + (tableHeight - photoHeight) / 2f,
                0.68f, 0.5f, false, false)
                .setRenderSortY(tableY + 1f);
        addObjectAtPixel(APARTMENT, "Photo Table", "/objects/home/interiors/if_living_coffee_table",
                tableX, tableY, 1.45f, 0.8f, true, false)
                .setSolidArea(TILE_SIZE / 12f, TILE_SIZE / 12f,
                        tableWidth - TILE_SIZE / 6f, tableHeight - TILE_SIZE / 6f);

        phoneDresser = addObjectAtPixel(APARTMENT, "Phone Dresser", "/objects/home/dresser",
                TILE_SIZE * 24f - TILE_SIZE / 8f, TILE_SIZE * 7f, 1.35f, 1.2f, true, false)
                .setSolidArea(TILE_SIZE / 12f, TILE_SIZE / 8f, TILE_SIZE * 6f / 5f, TILE_SIZE);

        dirtyDishes = addObjectAtPixel(APARTMENT, "Dirty Dishes", "/objects/home/decor/dirty_dishes",
                TILE_SIZE * 20f + TILE_SIZE / 4f, TILE_SIZE * 16f + TILE_SIZE / 12f,
                0.7f, 0.42f, false, false)
                .setSolidArea(0f, 0f, TILE_SIZE * 7f / 10f, TILE_SIZE * 4f / 3f)
                .setRenderSortY(TILE_SIZE * 16f + 3f);

        addObject(APARTMENT, "Living Carpet", "/objects/home/carpet", 31, 10, 2.7f, 2.0f, false, true);

        GdxSceneActor sofa = addObject(APARTMENT, "Sofa", "/objects/home/interiors/if_living_sofa_gray",
                32, 12, 2.8f, 1.75f, true, false);
        sofa.setSolidArea(0f, 0f, drawSize(2.8f), drawSize(1.75f)).setRenderSortY(TILE_SIZE * 13f);

        float tvWidth = drawSize(2.45f);
        float sofaCenterX = TILE_SIZE * 32f + drawSize(2.8f) / 2f;
        tv = addObjectAtPixel(APARTMENT, "TV", "/objects/home/tv_plasma_off",
                sofaCenterX - tvWidth / 2f, TILE_SIZE * 8f, 2.45f, 1.35f, true, false)
                .setSolidArea(TILE_SIZE / 8f, TILE_SIZE / 8f,
                        tvWidth - TILE_SIZE / 4f, drawSize(1.35f) - TILE_SIZE / 4f);

        addObjectAtPixel(APARTMENT, "Bathroom Mirror", "/objects/home/mirror_sink",
                TILE_SIZE * 28f, TILE_SIZE * 16f + TILE_SIZE / 4f, 1.45f, 1.6f, true, false)
                .setSolidArea(TILE_SIZE / 5f, TILE_SIZE * 3f / 4f, TILE_SIZE, TILE_SIZE * 3f / 4f);

        float doorWidth = drawSize(1.45f);
        float doorHeight = drawSize(2.0f);
        float bottomWallY = TILE_SIZE * 24f;
        float doorY = bottomWallY - doorHeight + TILE_SIZE / 2f;
        float doorX = TILE_SIZE * 23f + TILE_SIZE / 2f - doorWidth / 2f;
        addObjectAtPixel(APARTMENT, "Door", "/objects/home/door", doorX, doorY, 1.45f, 2.0f, true, false)
                .setSolidArea(0f, bottomWallY - doorY, doorWidth, doorHeight - (bottomWallY - doorY))
                .setRenderSortY(bottomWallY + 1f);

        placeHomeDecoration("Bedroom Rug", "carpet_striped", 1.55f, 1.15f, 19, 10, false);
        placeKitchenDecoration("Kitchen Counter Left", "kitchen_counter_left", 1.15f, 1.1f, 20, 16, true);
        placeKitchenDecoration("Kitchen Counter Right", "kitchen_counter_right", 1.15f, 1.1f, 19, 16, true);
        placeKitchenDecoration("Kitchen Stove", "kitchen_stove", 1.15f, 1.1f, 18, 16, true);
        placeKitchenDecoration("Kitchen Wall Sink", "kitchen_sink_wall", 0.8f, 0.75f, 20, 16, false);
        placeHomeDecoration("Kitchen Fridge", "kitchen_fridge", 1.0f, 2.3f, 21, 16, true);
        placeHomeDecoration("Kitchen Rug", "carpet_green", 1.6f, 1.25f, 17, 21, false);
        addObject(APARTMENT, "Kitchen Dining Set", "/objects/home/interiors/if_dining_table_chairs",
                14, 22, 2.25f, 1.25f, true, false);
        placeHomeDecoration("Bathroom Toilet", "bathroom_toilet", 0.8f, 1.35f, 31, 17, true);
        placeHomeDecoration("Bathroom Tub", "bathroom_tub", 2.0f, 1.0f, 31, 22, true);
    }

    private void placeForestObjects() {
        int[][] tree05 = {
                {5, 4}, {15, 4}, {24, 4}, {37, 4}, {7, 12}, {6, 20}, {8, 30}, {7, 41},
                {43, 10}, {43, 17}, {43, 26}, {43, 35}, {17, 40}, {31, 40}, {43, 42},
                {10, 8}, {18, 12}, {13, 18}, {36, 9}, {37, 17}, {8, 26}, {10, 32},
                {38, 29}, {8, 38}, {28, 36}
        };
        int[][] tree11 = {
                {10, 5}, {20, 6}, {36, 5}, {42, 5}, {4, 9}, {4, 16}, {4, 25}, {5, 35},
                {39, 13}, {40, 22}, {39, 31}, {40, 41}, {12, 42}, {26, 42}, {36, 42},
                {14, 10}, {8, 15}, {25, 9}, {35, 12}, {9, 23}, {18, 24}, {35, 25},
                {32, 30}, {10, 36}, {34, 38}
        };
        for (int[] position : tree05) {
            placeTree("tree_05", 2.9f, 3.2f, position[0], position[1]);
        }
        for (int[] position : tree11) {
            placeTree("tree_11", 2.4f, 3.0f, position[0], position[1]);
        }

        forestDecoration("decoration_10_flowers_pink", 0.9f, 0.9f, 20, 41);
        forestDecoration("decoration_14_flowers_purple", 0.9f, 0.9f, 26, 38);
        forestDecoration("decoration_08_mushroom_orange", 0.9f, 0.9f, 18, 34);
        forestDecoration("decoration_12_mushroom_red", 0.8f, 0.8f, 14, 32);
        forestDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 12, 28);
        forestDecoration("decoration_09_berries_red", 0.9f, 0.9f, 21, 27);
        forestDecoration("decoration_05_mushroom_brown", 0.8f, 0.8f, 29, 27);
        forestDecoration("decoration_11_leaf_curled", 0.9f, 0.9f, 33, 24);
        forestDecoration("decoration_07_mushroom_blue", 0.8f, 0.8f, 29, 20);
        forestDecoration("decoration_13_berries_green", 0.9f, 0.9f, 35, 19);
        forestDecoration("decoration_02_sprout", 0.8f, 0.9f, 27, 15);
        forestDecoration("decoration_01_mushroom_gold", 0.9f, 0.9f, 33, 13);
        forestDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 27, 8);
        forestDecoration("decoration_04_blue_bulb", 1.0f, 1.0f, 35, 8);
        forestDecoration("decoration_03_mushroom_purple", 0.9f, 0.9f, 40, 16);
        forestDecoration("decoration_06_cactus", 0.9f, 0.9f, 41, 25);
        forestDecoration("decoration_10_flowers_pink", 0.9f, 0.9f, 36, 32);
        forestDecoration("decoration_14_flowers_purple", 0.9f, 0.9f, 30, 33);
        forestDecoration("decoration_09_berries_red", 0.9f, 0.9f, 6, 14);
        forestDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 7, 22);
        forestDecoration("decoration_08_mushroom_orange", 0.9f, 0.9f, 6, 33);
        forestDecoration("decoration_11_leaf_curled", 0.9f, 0.9f, 12, 39);
        forestDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 39, 39);
        forestDecoration("decoration_04_blue_bulb", 1.0f, 1.0f, 44, 33);
        forestDecoration("decoration_13_berries_green", 0.9f, 0.9f, 16, 11);
        forestDecoration("decoration_12_mushroom_red", 0.8f, 0.8f, 22, 13);
        forestDecoration("decoration_02_sprout", 0.8f, 0.9f, 12, 7);
        forestDecoration("decoration_11_leaf_curled", 0.9f, 0.9f, 17, 8);
        forestDecoration("decoration_10_flowers_pink", 0.9f, 0.9f, 22, 8);
        forestDecoration("decoration_14_flowers_purple", 0.9f, 0.9f, 39, 8);
        forestDecoration("decoration_05_mushroom_brown", 0.8f, 0.8f, 42, 12);
        forestDecoration("decoration_13_berries_green", 0.9f, 0.9f, 5, 13);
        forestDecoration("decoration_08_mushroom_orange", 0.9f, 0.9f, 23, 16);
        forestDecoration("decoration_04_blue_bulb", 1.0f, 1.0f, 38, 18);
        forestDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 15, 20);
        forestDecoration("decoration_03_mushroom_purple", 0.9f, 0.9f, 6, 28);
        forestDecoration("decoration_09_berries_red", 0.9f, 0.9f, 20, 31);
        forestDecoration("decoration_07_mushroom_blue", 0.8f, 0.8f, 35, 30);
        forestDecoration("decoration_01_mushroom_gold", 0.9f, 0.9f, 40, 36);
        forestDecoration("decoration_12_mushroom_red", 0.8f, 0.8f, 15, 37);
        forestDecoration("decoration_06_cactus", 0.9f, 0.9f, 5, 40);
        forestDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 31, 42);
        forestDecoration("decoration_11_leaf_curled", 0.9f, 0.9f, 44, 22);
        forestDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 24, 34);
        forestDecoration("decoration_02_sprout", 0.8f, 0.9f, 14, 14);
        forestDecoration("decoration_05_mushroom_brown", 0.8f, 0.8f, 33, 10);
        forestDecoration("decoration_10_flowers_pink", 0.9f, 0.9f, 28, 12);
        forestDecoration("decoration_14_flowers_purple", 0.9f, 0.9f, 36, 15);
        forestDecoration("decoration_09_berries_red", 0.9f, 0.9f, 11, 27);
        forestDecoration("decoration_13_berries_green", 0.9f, 0.9f, 40, 29);

        addObject(FOREST_DOUBTS, "Lost Lantern", "/objects/story/lost_lantern", 18, 35, 0.8f, 0.9f, false, false)
                .setRenderSortY(TILE_SIZE * 35f - 1f);
        addObject(FOREST_DOUBTS, "Wounded Bird", "/objects/story/wounded_bird", 28, 28, 0.75f, 0.55f, false, false)
                .setRenderSortY(TILE_SIZE * 28f - 1f);
        lantern = addObject(FOREST_DOUBTS, "Lantern", "/objects/lantern", 23, 41, 1.7f, 1.7f, false, false)
                .setSolidArea(TILE_SIZE / 2f, TILE_SIZE / 4f, TILE_SIZE * 3f / 4f, TILE_SIZE * 5f / 4f)
                .setRenderSortY(TILE_SIZE * 41f - 1f);
    }

    private void placeVillageObjects() {
        villageHouse("village_house_friend", "building_020_x956_y563_73x93", 3.0f, 3.8f, 12, 3);
        villageHouse("village_house_north_west", "building_014_x13_y464_86x80", 4.0f, 3.75f, 5, 3);
        villageHouse("village_house_north_center", "building_022_x13_y568_86x80", 4.0f, 3.75f, 17, 3);
        villageHouse("village_house_north_east", "building_029_x13_y668_86x80", 4.0f, 3.75f, 25, 3)
                .setWorldLabel("Library", drawSize(4.0f * VILLAGE_HOUSE_SCALE) / 2f,
                        drawSize(3.75f * VILLAGE_HOUSE_SCALE) - TILE_SIZE * 1.55f);
        villageHouse("village_house_west_upper", "building_038_x13_y758_86x80", 4.0f, 3.75f, 1, 15);
        villageHouse("village_house_west_lower", "building_050_x13_y941_86x80", 4.0f, 3.75f, 1, 26);
        villageHouse("village_house_center_west", "building_018_x791_y563_73x93", 3.0f, 3.8f, 15, 16);
        villageHouse("village_house_center_east", "building_019_x871_y563_73x93", 3.0f, 3.8f, 32, 16);
        villageHouse("village_house_east_upper", "building_029_x13_y668_86x80", 4.0f, 3.75f, 42, 15);
        villageHouse("village_house_east_lower", "building_038_x13_y758_86x80", 4.0f, 3.75f, 42, 42);
        villageHouse("village_house_south_west", "building_050_x13_y941_86x80", 4.0f, 3.75f, 12, 36);
        villageHouse("village_house_south_center", "building_014_x13_y464_86x80", 4.0f, 3.75f, 25, 36);
        villageHouse("village_house_south_east", "building_022_x13_y568_86x80", 4.0f, 3.75f, 38, 36);
        placeVillageLibraryDoorTrigger();
    }

    private void placeMountainObjects() {
        mountainDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 33, 12);
        mountainDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 38, 16);
        mountainDecoration("decoration_04_blue_bulb", 1.0f, 1.0f, 30, 18);
        mountainDecoration("decoration_04_blue_bulb", 1.0f, 1.0f, 24, 25);
        mountainDecoration("decoration_01_mushroom_gold", 0.9f, 0.9f, 18, 36);
        mountainDecoration("decoration_07_mushroom_blue", 0.8f, 0.8f, 27, 33);
        mountainDecoration("decoration_03_mushroom_purple", 0.9f, 0.9f, 14, 42);
        mountainDecoration("decoration_08_mushroom_orange", 0.9f, 0.9f, 38, 33);
        mountainDecoration("decoration_02_sprout", 0.8f, 0.9f, 23, 22);
        mountainDecoration("decoration_02_sprout", 0.8f, 0.9f, 32, 27);
        mountainDecoration("decoration_11_leaf_curled", 0.9f, 0.9f, 21, 28);
        mountainDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 29, 36);
        mountainDecoration("decoration_10_flowers_pink", 0.9f, 0.9f, 16, 39);
        mountainDecoration("decoration_14_flowers_purple", 0.9f, 0.9f, 25, 37);
        mountainDecoration("decoration_09_berries_red", 0.9f, 0.9f, 37, 30);
        mountainDecoration("decoration_13_berries_green", 0.9f, 0.9f, 40, 35);
        mountainDecoration("decoration_12_mushroom_red", 0.8f, 0.8f, 12, 43);
        mountainDecoration("decoration_05_mushroom_brown", 0.8f, 0.8f, 30, 40);
        mountainDecoration("decoration_00_crystal_blue", 1.0f, 1.0f, 36, 14);
        mountainDecoration("decoration_15_leaves_green", 0.9f, 0.9f, 33, 24);
        addObject(MOUNTAIN, "Mountain Fork", "/objects/story/mountain_fork", 31, 33, 0.9f, 0.9f, false, false);
        addObject(MOUNTAIN, "Traveler Pack", "/objects/story/traveler_pack", 29, 34, 0.75f, 0.75f, false, false);
    }

    private void placeLibraryObjects() {
        float doorWidth = drawSize(1.45f);
        float doorHeight = drawSize(2.0f);
        float bottomWallY = TILE_SIZE * 23f;
        float doorY = bottomWallY - doorHeight + TILE_SIZE / 2f;
        addObjectAtPixel(LIBRARY, "Library Exit", "/objects/home/door",
                TILE_SIZE * 24f + TILE_SIZE / 2f - doorWidth / 2f, doorY, 1.45f, 2.0f, true, false)
                .setSolidArea(0f, bottomWallY - doorY, doorWidth, doorHeight - (bottomWallY - doorY))
                .setRenderSortY(bottomWallY + 1f);

        libraryShelf("Library Shelf Left", 17, 13);
        libraryShelf("Library Shelf Mid Left", 20, 13);
        libraryShelf("Library Shelf Mid Right", 28, 13);
        libraryShelf("Library Shelf Right", 31, 13);
        addObject(LIBRARY, "Library Reading Table", "/objects/home/interiors/if_living_coffee_table",
                23, 19, 1.9f, 0.95f, true, false)
                .setSolidArea(TILE_SIZE / 12f, TILE_SIZE / 10f,
                        drawSize(1.9f) - TILE_SIZE / 6f, drawSize(0.95f) - TILE_SIZE / 5f);
        addObject(LIBRARY, "Library Lamp", "/objects/home/decor/bedroom_lamp_gold",
                27, 18, 0.65f, 0.75f, true, false)
                .setSolidArea(TILE_SIZE / 8f, TILE_SIZE / 8f, TILE_SIZE * 2f / 5f, TILE_SIZE / 2f);
        addObject(LIBRARY, "Library Plant", "/objects/home/decor/plant_tall_green",
                16, 21, 0.85f, 1.25f, true, false)
                .setSolidArea(TILE_SIZE / 6f, TILE_SIZE * 5f / 6f, TILE_SIZE / 2f, TILE_SIZE / 3f);
    }

    private void placeNpcs() {
        float mirrorX = TILE_SIZE * 22f - drawSize(2.8f) - drawSize(1.25f) - TILE_SIZE / 8f
                - drawSize(1.6f) - TILE_SIZE / 4f;
        float mirrorY = TILE_SIZE * 6f + TILE_SIZE / 4f;
        addNpc(APARTMENT, "Shadow", "/player/characters/shadow", mirrorX + TILE_SIZE / 5f,
                mirrorY + TILE_SIZE / 2f, 0.62f, false);
        addNpc(FOREST_DOUBTS, "Child", "/player/characters/child", 30, 8, 1.15f, true);
        addNpc(FOREST_DOUBTS, "Shadow", "/player/characters/shadow", 31, 21, 1.15f, true);
        addNpc(VILLAGE, "Friend", "/player/characters/friend", 13, 10, 1.5f, true);
        addNpc(LIBRARY, "Elder", "/player/characters/elder", 24, 16, 1.38f, true);
        addNpc(MOUNTAIN, "Warrior", "/player/characters/warrior_knight", 35, 13, 1.15f, true);
        addNpc(MOUNTAIN, "Traveler", "/player/characters/friend", 26, 34, 1.15f, true);
    }

    private void placeKitchenDecoration(String name, String imageName, float widthTiles, float heightTiles,
                                        int col, int row, boolean collision) {
        float x = TILE_SIZE * col;
        float y = TILE_SIZE * row;
        if ("Kitchen Wall Sink".equals(name)) {
            x += TILE_SIZE / 6f;
            y -= TILE_SIZE / 6f;
            addObjectAtPixel(APARTMENT, name, "/objects/home/decor/" + imageName, x, y,
                    widthTiles, heightTiles, collision, false).setRenderSortY(TILE_SIZE * row + 1f);
        } else {
            if ("Kitchen Counter Right".equals(name)) {
                x -= TILE_SIZE / 3f;
            } else if ("Kitchen Counter Left".equals(name)) {
                x -= TILE_SIZE / 6f;
            } else if ("Kitchen Stove".equals(name)) {
                x -= TILE_SIZE / 2f;
            }
            addObjectAtPixel(APARTMENT, name, "/objects/home/decor/" + imageName, x, y,
                    widthTiles, heightTiles, collision, false);
        }
    }

    private void placeHomeDecoration(String name, String imageName, float widthTiles, float heightTiles,
                                     int col, int row, boolean collision) {
        float y = TILE_SIZE * row;
        if ("Bathroom Toilet".equals(name)) {
            y -= TILE_SIZE / 4f;
        }
        addObjectAtPixel(APARTMENT, name, "/objects/home/decor/" + imageName,
                TILE_SIZE * col, y, widthTiles, heightTiles, collision, isFloorPlacement(name));
    }

    private void placeTree(String imageName, float widthTiles, float heightTiles, int col, int row) {
        GdxSceneActor tree = addObject(FOREST_DOUBTS, imageName, "/trees/sliced/" + imageName,
                col, row, widthTiles, heightTiles, true, false);
        tree.setSolidArea(TILE_SIZE / 6f, TILE_SIZE / 6f,
                Math.max(TILE_SIZE, drawSize(widthTiles) - TILE_SIZE / 3f),
                Math.max(TILE_SIZE, drawSize(heightTiles) - TILE_SIZE / 3f));
    }

    private void forestDecoration(String imageName, float widthTiles, float heightTiles, int col, int row) {
        addObject(FOREST_DOUBTS, imageName, "/objects/forest_decorations/" + imageName,
                col, row, widthTiles, heightTiles, false, false);
    }

    private void mountainDecoration(String imageName, float widthTiles, float heightTiles, int col, int row) {
        addObject(MOUNTAIN, imageName, "/objects/forest_decorations/" + imageName,
                col, row, widthTiles, heightTiles, false, false);
    }

    private GdxSceneActor villageHouse(String name, String imageName, float widthTiles, float heightTiles,
                                       int col, int row) {
        float finalWidthTiles = widthTiles * VILLAGE_HOUSE_SCALE;
        float finalHeightTiles = heightTiles * VILLAGE_HOUSE_SCALE;
        GdxSceneActor house = addObject(VILLAGE, name, "/tiles/sliced/Buildings/" + imageName,
                col, row, finalWidthTiles, finalHeightTiles, true, false);
        house.setSolidArea(0f, 0f, drawSize(finalWidthTiles), drawSize(finalHeightTiles));
        return house;
    }

    private void placeVillageLibraryDoorTrigger() {
        float widthTiles = 1.35f;
        float heightTiles = 2.8f;
        float doorWidth = drawSize(widthTiles);
        float doorHeight = drawSize(heightTiles);
        float houseX = TILE_SIZE * 25f;
        float houseY = TILE_SIZE * 3f;
        float houseWidth = drawSize(4.0f * VILLAGE_HOUSE_SCALE);
        float houseHeight = drawSize(3.75f * VILLAGE_HOUSE_SCALE);
        float doorX = houseX + houseWidth / 2f - doorWidth / 2f;
        float doorY = houseY + houseHeight - TILE_SIZE / 2f;

        addObjectAtPixel(VILLAGE, "Village Library Door", "/objects/home/door",
                doorX, doorY, widthTiles, heightTiles, false, false)
                .setSolidArea(0f, 0f, doorWidth, doorHeight)
                .setVisible(false);
    }

    private void libraryShelf(String name, int col, int row) {
        addObject(LIBRARY, name, "/objects/home/dresser", col, row, 1.25f, 1.6f, true, false)
                .setSolidArea(TILE_SIZE / 12f, TILE_SIZE / 8f,
                        drawSize(1.25f) - TILE_SIZE / 6f, drawSize(1.6f) - TILE_SIZE / 6f);
    }

    private GdxSceneActor addNpc(int map, String name, String imagePath, int col, int row,
                                 float drawScale, boolean showName) {
        return addNpc(map, name, imagePath, (float) TILE_SIZE * col, (float) TILE_SIZE * row, drawScale, showName);
    }

    private GdxSceneActor addNpc(int map, String name, String imagePath, float x, float y,
                                 float drawScale, boolean showName) {
        GdxSceneActor actor = GdxSceneActor.npc(name, textureStore.get(imagePath), x, y,
                drawSize(drawScale), showName);
        npcs[map].add(actor);
        return actor;
    }

    private GdxSceneActor addObject(int map, String name, String imagePath, int col, int row,
                                    float widthTiles, float heightTiles, boolean collision, boolean floorLayer) {
        return addObjectAtPixel(map, name, imagePath, TILE_SIZE * col, TILE_SIZE * row,
                widthTiles, heightTiles, collision, floorLayer);
    }

    private GdxSceneActor addObjectAtPixel(int map, String name, String imagePath, float x, float y,
                                           float widthTiles, float heightTiles,
                                           boolean collision, boolean floorLayer) {
        GdxSceneActor actor = GdxSceneActor.object(name, textureStore.get(imagePath), x, y,
                drawSize(widthTiles), drawSize(heightTiles), collision, floorLayer);
        objects[map].add(actor);
        return actor;
    }

    private boolean isFloorPlacement(String name) {
        return name.contains("Rug") || name.contains("Carpet");
    }

    private float drawSize(float tiles) {
        return Math.round(TILE_SIZE * tiles);
    }

    @SuppressWarnings("unchecked")
    private static List<GdxSceneActor>[] createLayers() {
        List<GdxSceneActor>[] layers = new ArrayList[MAP_COUNT];
        for (int i = 0; i < layers.length; i++) {
            layers[i] = new ArrayList<>();
        }
        return layers;
    }

    private static final class DrawEntry {
        final GdxSceneActor actor;
        final Runnable playerRenderer;
        final float playerSortY;

        private DrawEntry(GdxSceneActor actor, Runnable playerRenderer, float playerSortY) {
            this.actor = actor;
            this.playerRenderer = playerRenderer;
            this.playerSortY = playerSortY;
        }

        static DrawEntry actor(GdxSceneActor actor) {
            return new DrawEntry(actor, null, 0f);
        }

        static DrawEntry player(float playerSortY, Runnable playerRenderer) {
            return new DrawEntry(null, playerRenderer, playerSortY);
        }

        float sortY() {
            return actor != null ? actor.sortY() : playerSortY;
        }
    }
}
