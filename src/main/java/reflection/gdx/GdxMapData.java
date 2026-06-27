package reflection.gdx;

import com.badlogic.gdx.Gdx;

final class GdxMapData {

    final String name;
    final String resourcePath;
    final int[][] tileIds;
    final int columns;
    final int rows;
    final float startX;
    final float startY;

    private GdxMapData(String name, String resourcePath, int[][] tileIds, float startX, float startY) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.tileIds = tileIds;
        this.columns = tileIds.length;
        this.rows = tileIds[0].length;
        this.startX = startX;
        this.startY = startY;
    }

    static GdxMapData load(String name, String resourcePath, int tileSize, int startColumn, int startRow) {
        String text = Gdx.files.internal(resourcePath).readString("UTF-8").replace("\r", "").trim();
        String[] lines = text.split("\n");
        if (lines.length == 0) {
            throw new IllegalStateException("Map has no rows: " + resourcePath);
        }

        String[] firstRow = splitRow(lines[0]);
        int columns = firstRow.length;
        int rows = lines.length;
        int[][] tileIds = new int[columns][rows];

        for (int row = 0; row < rows; row++) {
            String[] values = splitRow(lines[row]);
            if (values.length != columns) {
                throw new IllegalStateException(
                        "Map row has " + values.length + " columns instead of " + columns +
                                ": " + resourcePath + " row " + row
                );
            }
            for (int column = 0; column < columns; column++) {
                tileIds[column][row] = Integer.parseInt(values[column]);
            }
        }

        return new GdxMapData(name, resourcePath, tileIds, startColumn * tileSize, startRow * tileSize);
    }

    int tileAt(int column, int row) {
        if (column < 0 || row < 0 || column >= columns || row >= rows) {
            return -1;
        }
        return tileIds[column][row];
    }

    float pixelWidth(int tileSize) {
        return columns * tileSize;
    }

    float pixelHeight(int tileSize) {
        return rows * tileSize;
    }

    private static String[] splitRow(String row) {
        return row.trim().split("\\s+");
    }
}
