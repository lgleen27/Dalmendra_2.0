package com.frexal.dalmendra.app.ui.main.tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculador de layout para el tablero de categorías.
 * Encuentra el tamaño de fuente óptimo para que todos los elementos encajen en el espacio disponible.
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

    private static final double MIN_FONT_SIZE = 16.0;
    private static final double MAX_FONT_SIZE = 36.0;
    private static final double ROW_HEIGHT_MULTIPLIER = 1.35; // Factor para evitar que se encimen

    public static LayoutResult calculate(List<Block> blocks, double width, double height, boolean allowSplit, int maxColumns) {
        if (width <= 0 || height <= 0 || blocks.isEmpty()) {
            return new LayoutResult(1, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER, 0);
        }

        LayoutResult bestResult = null;

        for (int cols = 1; cols <= maxColumns; cols++) {
            LayoutResult result = binarySearchFontSize(blocks, width, height, cols, allowSplit);
            if (bestResult == null || (result.missedItems < bestResult.missedItems) || 
                (result.missedItems == bestResult.missedItems && result.fontSize > bestResult.fontSize)) {
                bestResult = result;
            }
        }

        if (bestResult == null) {
            bestResult = new LayoutResult(1, MIN_FONT_SIZE, MIN_FONT_SIZE * ROW_HEIGHT_MULTIPLIER, countTotalItems(blocks));
        }

        return bestResult;
    }

    private static int countTotalItems(List<Block> blocks) {
        return blocks.stream().mapToInt(b -> b.items.size()).sum();
    }

    private static LayoutResult binarySearchFontSize(List<Block> blocks, double width, double height, int columns, boolean allowSplit) {
        double low = MIN_FONT_SIZE;
        double high = MAX_FONT_SIZE;
        double bestFitFont = MIN_FONT_SIZE;
        int minMissed = Integer.MAX_VALUE;

        // Búsqueda binaria
        for (int i = 0; i < 15; i++) {
            double mid = (low + high) / 2.0;
            int missed = simulateLayout(blocks, width, height, columns, mid, allowSplit);

            if (missed == 0) {
                bestFitFont = mid;
                minMissed = 0;
                low = mid + 0.1; // Buscar si cabe con una más grande
            } else {
                if (missed < minMissed) {
                    minMissed = missed;
                }
                high = mid - 0.1; // Buscar una más chica
            }
        }

        // Si con el mínimo (16px) hay elementos fuera, calcular cuántos faltan usando el mínimo
        if (minMissed > 0) {
            bestFitFont = MIN_FONT_SIZE;
            minMissed = simulateLayout(blocks, width, height, columns, MIN_FONT_SIZE, allowSplit);
        }

        return new LayoutResult(columns, bestFitFont, bestFitFont * ROW_HEIGHT_MULTIPLIER, minMissed);
    }

    private static int simulateLayout(List<Block> blocks, double width, double height, int columns, double fontSize, boolean allowSplit) {
        double rowHeight = fontSize * ROW_HEIGHT_MULTIPLIER;
        double titleHeight = rowHeight * 1.1; // Título ligeramente más grande

        int currentColumn = 1;
        double currentY = 0;
        int missedItems = 0;

        for (Block block : blocks) {
            if (currentColumn > columns) {
                missedItems += block.items.size();
                continue;
            }

            // ¿Cabe el título?
            if (currentY + titleHeight > height) {
                currentColumn++;
                currentY = 0;
                if (currentColumn > columns) {
                    missedItems += block.items.size();
                    continue;
                }
            }
            
            // Si no allowSplit, validar si el bloque COMPLETO cabe en la columna actual
            if (!allowSplit) {
                double blockTotalHeight = titleHeight + (block.items.size() * rowHeight);
                if (currentY + blockTotalHeight > height) {
                    currentColumn++;
                    currentY = 0;
                    if (currentColumn > columns) {
                        missedItems += block.items.size();
                        continue;
                    }
                    if (blockTotalHeight > height) {
                        // El bloque ni siquiera cabe en una columna completa
                        // Lo que no quepa se pierde
                    }
                }
            }

            currentY += titleHeight;

            int itemsLeft = block.items.size();

            while (itemsLeft > 0) {
                if (currentColumn > columns) {
                    missedItems += itemsLeft;
                    break;
                }

                if (currentY + rowHeight > height) {
                    // No cabe más en esta columna
                    if (allowSplit) {
                        currentColumn++;
                        currentY = 0;
                        if (currentColumn <= columns) {
                            // Agregar título repetido
                            currentY += titleHeight;
                        }
                    } else {
                        // En no-split, lo que no cupo se pierde (por bloques extremadamente largos)
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
