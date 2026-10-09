package com.frexal.dalmendra.app.ui.main.tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculador de layout para el tablero de categorías.
 *
 * Busca la combinación (número de columnas, tamaño de letra) más grande posible que:
 * - Quepa a lo ALTO (todos los renglones visibles).
 * - Quepa a lo ANCHO (que se lean al menos N caracteres del nombre más la cantidad).
 */
public class TableroLayoutCalculator {

    public static class LayoutResult {
        public int columns;
        public double fontSize;
        public double rowHeight;
        public int missedItems;

        public LayoutResult(int columns, double fontSize, double rowHeight, int missedItems) {
            this.columns = columns;
            this.fontSize = fontSize;
            this.rowHeight = rowHeight;
            this.missedItems = missedItems;
        }
    }

    public static class Block {
        public String title;
        public List<Object> items;

        public Block(String title) {
            this.title = title;
            this.items = new ArrayList<>();
        }
    }

    public static final double MIN_FONT_SIZE = 14.0;
    public static final double MAX_FONT_SIZE = 40.0;

    /** Alto del renglón respecto a la letra (holgura para que no se encimen). */
    public static final double ROW_HEIGHT_MULTIPLIER = 1.40;
    /** Alto del encabezado de categoría respecto al renglón. */
    public static final double TITLE_HEIGHT_FACTOR = 1.15;

    /** Ancho aproximado de un carácter en mayúsculas, como fracción del tamaño de letra. */
    public static final double CHAR_WIDTH_FACTOR = 0.64;
    /** Ancho reservado para la columna de cantidad, como múltiplo del tamaño de letra. */
    public static final double QTY_WIDTH_FACTOR = 2.6;

    /** Separación horizontal entre columnas y padding externo (deben coincidir con la vista). */
    public static final double COLUMN_GAP = 6.0;
    public static final double OUTER_PADDING_H = 12.0;
    /** Bordes + paddings internos de cada renglón. */
    public static final double ROW_INNER_PADDING = 18.0;

    public static double columnWidth(double width, int columns) {
        return (width - OUTER_PADDING_H - (columns - 1) * COLUMN_GAP) / columns;
    }

    public static double qtyWidth(double fontSize) {
        return fontSize * QTY_WIDTH_FACTOR;
    }

    /** Compatibilidad: mínimo de 14 caracteres legibles. */
    public static LayoutResult calculate(List<Block> blocks, double width, double height, boolean allowSplit, int maxColumns) {
        return calculate(blocks, width, height, allowSplit, maxColumns, 14);
    }

    public static LayoutResult calculate(List<Block> blocks, double width, double height,
                                         boolean allowSplit, int maxColumns, int minCharsDescripcion) {
        int total = countTotalItems(blocks);
        if (width <= 0 || height <= 0 || blocks.isEmpty()) {
            return new LayoutResult(1, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER, 0);
        }

        LayoutResult bestResult = null;

        for (int cols = 1; cols <= maxColumns; cols++) {
            // Tamaño máximo de letra que permite leer el nombre en esta cantidad de columnas
            double colW = columnWidth(width, cols);
            double maxFontByWidth = (colW - ROW_INNER_PADDING)
                    / (CHAR_WIDTH_FACTOR * minCharsDescripcion + QTY_WIDTH_FACTOR);

            LayoutResult result;
            if (maxFontByWidth < MIN_FONT_SIZE) {
                // Con estas columnas los nombres serían ilegibles: solo se usa si nada más funciona
                result = new LayoutResult(cols, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER,
                        total + simulateLayout(blocks, height, cols, MIN_FONT_SIZE, allowSplit) + 1);
            } else {
                double high = Math.min(MAX_FONT_SIZE, maxFontByWidth);
                result = binarySearchFontSize(blocks, height, cols, allowSplit, high);
            }

            if (bestResult == null
                    || result.missedItems < bestResult.missedItems
                    || (result.missedItems == bestResult.missedItems && result.fontSize > bestResult.fontSize + 0.01)) {
                bestResult = result;
            }
        }

        if (bestResult.missedItems > total) {
            // Ninguna opción era legible a lo ancho: usar 1 columna con letra mínima
            int missed = simulateLayout(blocks, height, 1, MIN_FONT_SIZE, allowSplit);
            bestResult = new LayoutResult(1, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER, missed);
        }

        return bestResult;
    }

    private static int countTotalItems(List<Block> blocks) {
        return blocks.stream().mapToInt(b -> b.items.size()).sum();
    }

    private static LayoutResult binarySearchFontSize(List<Block> blocks, double height, int columns,
                                                     boolean allowSplit, double maxFont) {
        double low = MIN_FONT_SIZE;
        double high = maxFont;
        double bestFitFont = -1;

        for (int i = 0; i < 18 && low <= high; i++) {
            double mid = (low + high) / 2.0;
            int missed = simulateLayout(blocks, height, columns, mid, allowSplit);
            if (missed == 0) {
                bestFitFont = mid;
                low = mid + 0.1;
            } else {
                high = mid - 0.1;
            }
        }

        if (bestFitFont < 0) {
            int missed = simulateLayout(blocks, height, columns, MIN_FONT_SIZE, allowSplit);
            return new LayoutResult(columns, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER, missed);
        }

        double font = Math.floor(bestFitFont * 10) / 10.0;
        return new LayoutResult(columns, font, font * ROW_HEIGHT_MULTIPLIER, 0);
    }

    /**
     * Simula el acomodo y regresa cuántos artículos NO cabrían.
     * Debe reflejar exactamente la misma lógica que usa la vista al dibujar.
     */
    public static int simulateLayout(List<Block> blocks, double height, int columns, double fontSize, boolean allowSplit) {
        double rowHeight = fontSize * ROW_HEIGHT_MULTIPLIER;
        double titleHeight = rowHeight * TITLE_HEIGHT_FACTOR;

        int currentColumn = 1;
        double currentY = 0;
        int missedItems = 0;

        for (Block block : blocks) {
            if (currentColumn > columns) {
                missedItems += block.items.size();
                continue;
            }

            if (!allowSplit) {
                double blockTotalHeight = titleHeight + (block.items.size() * rowHeight);
                if (currentY > 0 && currentY + blockTotalHeight > height) {
                    currentColumn++;
                    currentY = 0;
                    if (currentColumn > columns) {
                        missedItems += block.items.size();
                        continue;
                    }
                }
            } else if (currentY + titleHeight + rowHeight > height) {
                // No dejar un encabezado "huérfano" al final de la columna
                currentColumn++;
                currentY = 0;
                if (currentColumn > columns) {
                    missedItems += block.items.size();
                    continue;
                }
            }

            currentY += titleHeight;
            int itemsLeft = block.items.size();

            while (itemsLeft > 0) {
                if (currentY + rowHeight > height) {
                    if (allowSplit) {
                        currentColumn++;
                        currentY = 0;
                        if (currentColumn > columns) {
                            missedItems += itemsLeft;
                            break;
                        }
                        currentY += titleHeight; // encabezado "(cont.)"
                    } else {
                        missedItems += itemsLeft;
                        break;
                    }
                } else {
                    currentY += rowHeight;
                    itemsLeft--;
                }
            }
        }

        return missedItems;
    }
}
