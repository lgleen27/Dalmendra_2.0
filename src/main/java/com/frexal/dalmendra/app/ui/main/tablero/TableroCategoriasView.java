package com.frexal.dalmendra.app.ui.main.tablero;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente visual que dibuja el tablero "Por Categorías" 
 * utilizando TableroLayoutCalculator para calcular columnas y fuentes.
 */
public class TableroCategoriasView extends StackPane {

    private final VBox container;
    private final Label warningLabel;
    
    private List<Categoria> categorias = new ArrayList<>();
    private List<Existencia> existencias = new ArrayList<>();
    private boolean permitirDividir = false;

    private static final int MAX_COLUMNAS = 6;

    public TableroCategoriasView() {
        container = new VBox(0);
        container.setAlignment(Pos.TOP_LEFT);
        container.prefWidthProperty().bind(this.widthProperty());
        container.prefHeightProperty().bind(this.heightProperty());

        warningLabel = new Label();
        warningLabel.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px;");
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
        StackPane.setAlignment(warningLabel, Pos.BOTTOM_CENTER);

        this.getChildren().addAll(container, warningLabel);

        // Recalcular el layout cuando cambia el tamaño
        this.widthProperty().addListener((obs, oldV, newV) -> updateLayoutView());
        this.heightProperty().addListener((obs, oldV, newV) -> updateLayoutView());
    }

    public void updateData(List<Categoria> categorias, List<Existencia> existencias, boolean permitirDividir) {
        this.categorias = categorias != null ? categorias : new ArrayList<>();
        this.existencias = existencias != null ? existencias : new ArrayList<>();
        this.permitirDividir = permitirDividir;
        updateLayoutView();
    }

    private void updateLayoutView() {
        double width = this.getWidth();
        double height = this.getHeight();

        if (width <= 0 || height <= 0 || categorias.isEmpty() || existencias.isEmpty()) {
            container.getChildren().clear();
            warningLabel.setVisible(false);
            warningLabel.setManaged(false);
            return;
        }

        // Agrupar existencias por categoría
        List<TableroLayoutCalculator.Block> blocks = new ArrayList<>();
        Map<String, TableroLayoutCalculator.Block> blockMap = new HashMap<>();

        for (Categoria c : categorias) {
            String title = c.getDescripcion() != null ? c.getDescripcion().toUpperCase() : "";
            TableroLayoutCalculator.Block b = new TableroLayoutCalculator.Block(title);
            blocks.add(b);
            blockMap.put(title, b);
        }

        for (Existencia ex : existencias) {
            if (ex.getCategoriaId() != null) {
                Categoria cat = buscarCategoria(ex.getCategoriaId());
                if (cat != null) {
                    String title = cat.getDescripcion() != null ? cat.getDescripcion().toUpperCase() : "";
                    TableroLayoutCalculator.Block b = blockMap.get(title);
                    if (b != null) {
                        b.items.add(ex);
                    }
                }
            }
        }
        
        // Remover bloques vacíos
        blocks.removeIf(b -> b.items.isEmpty());

        TableroLayoutCalculator.LayoutResult layout = TableroLayoutCalculator.calculate(blocks, width, height, permitirDividir, MAX_COLUMNAS);

        container.getChildren().clear();
        
        HBox columnsContainer = new HBox(6);
        columnsContainer.setPadding(new Insets(3, 6, 3, 6));
        columnsContainer.setAlignment(Pos.TOP_LEFT);
        
        VBox[] columnBoxes = new VBox[layout.columns];
        for (int i = 0; i < layout.columns; i++) {
            columnBoxes[i] = new VBox();
            columnBoxes[i].setSpacing(0);
            columnBoxes[i].setAlignment(Pos.TOP_LEFT);
            columnBoxes[i].setPrefWidth((width - (layout.columns - 1) * 6.0 - 12.0) / layout.columns);
            columnsContainer.getChildren().add(columnBoxes[i]);
            HBox.setHgrow(columnBoxes[i], Priority.ALWAYS);
        }

        int currentColumn = 0;
        double currentY = 0;
        double rowHeight = layout.rowHeight;
        double titleHeight = rowHeight * 1.1;
        int drawnItemsCount = 0;
        int totalItemsCount = blocks.stream().mapToInt(b -> b.items.size()).sum();

        for (TableroLayoutCalculator.Block block : blocks) {
            if (currentColumn >= layout.columns) break;

            if (currentY + titleHeight > height) {
                currentColumn++;
                currentY = 0;
                if (currentColumn >= layout.columns) break;
            }

            if (!permitirDividir) {
                double blockTotalHeight = titleHeight + (block.items.size() * rowHeight);
                if (currentY + blockTotalHeight > height) {
                    currentColumn++;
                    currentY = 0;
                    if (currentColumn >= layout.columns) break;
                    if (blockTotalHeight > height) {
                        // El bloque no cabe ni completo en una columna, forzar split o truncar
                    }
                }
            }

            columnBoxes[currentColumn].getChildren().add(crearTitulo(block.title, layout.fontSize, titleHeight));
            currentY += titleHeight;

            for (Object itemObj : block.items) {
                Existencia ex = (Existencia) itemObj;

                if (currentColumn >= layout.columns) break;

                if (currentY + rowHeight > height) {
                    if (permitirDividir) {
                        currentColumn++;
                        currentY = 0;
                        if (currentColumn >= layout.columns) break;
                        columnBoxes[currentColumn].getChildren().add(crearTitulo(block.title + " (cont.)", layout.fontSize, titleHeight));
                        currentY += titleHeight;
                    } else {
                        break; // Se salta los demás ítems porque no caben y no se pueden dividir
                    }
                }

                columnBoxes[currentColumn].getChildren().add(crearFila(ex, block.title, layout.fontSize, rowHeight));
                currentY += rowHeight;
                drawnItemsCount++;
            }
        }

        container.getChildren().add(columnsContainer);

        if (drawnItemsCount < totalItemsCount) {
            int faltan = totalItemsCount - drawnItemsCount;
            warningLabel.setText("⚠️ Faltan " + faltan + " artículos. Ajuste el tamaño o aumente pantallas.");
            warningLabel.setVisible(true);
            warningLabel.setManaged(true);
        } else {
            warningLabel.setVisible(false);
            warningLabel.setManaged(false);
        }
    }

    private Label crearTitulo(String texto, double fontSize, double titleHeight) {
        Label titulo = new Label(texto);
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setMinHeight(titleHeight);
        titulo.setPrefHeight(titleHeight);
        titulo.setStyle("-fx-font-size: " + (fontSize + 1) + "px; -fx-font-weight: bold; -fx-text-fill: #2c2c2c; -fx-padding: 0 0 1 0;");
        return titulo;
    }

    private HBox crearFila(Existencia ex, String categoriaTitulo, double fontSize, double rowHeight) {
        HBox fila = new HBox();
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setMaxWidth(Double.MAX_VALUE);
        fila.setMinHeight(rowHeight);
        fila.setPrefHeight(rowHeight);
        fila.setPadding(new Insets(0, 5, 0, 5));

        String descripcion = ex.getDescripcion();
        if (descripcion != null) {
            String cat = categoriaTitulo.replace(" (cont.)", "");
            if (descripcion.toUpperCase().startsWith(cat.toUpperCase())) {
                descripcion = descripcion.substring(cat.length()).trim();
            }
        } else {
            descripcion = "";
        }

        Label lblDesc = new Label(descripcion);
        lblDesc.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #333333;");
        HBox.setHgrow(lblDesc, Priority.ALWAYS);

        BigDecimal dExistencia = ex.getExistenciaOrZero();
        String txtExistencia = (dExistencia.doubleValue() == dExistencia.intValue()) ? 
                               String.valueOf(dExistencia.intValue()) : 
                               String.format("%.1f", dExistencia.doubleValue());
        
        Label lblExist = new Label(txtExistencia);
        lblExist.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        lblExist.setMinWidth(40);
        lblExist.setAlignment(Pos.CENTER_RIGHT);

        String colorSemaforo = getColorSemaforo(dExistencia.doubleValue(), 
                                                ex.getStockMinimo() != null ? ex.getStockMinimo().doubleValue() : null, 
                                                ex.getStockDeseado() != null ? ex.getStockDeseado().doubleValue() : null);

        fila.setStyle("-fx-background-color: " + colorSemaforo + "; -fx-border-color: transparent transparent #e0e0e0 transparent; -fx-border-width: 0 0 1 0;");

        fila.getChildren().addAll(lblDesc, lblExist);
        return fila;
    }

    private String getColorSemaforo(double existenciaActual, Double stockMinimo, Double stockDeseado) {
        if (stockMinimo == null || stockDeseado == null) {
            return "transparent";
        }
        if (existenciaActual <= stockMinimo) {
            return "#ffb3b3";
        } else if (existenciaActual <= stockDeseado) {
            return "#fff3a3";
        } else {
            return "transparent";
        }
    }

    private Categoria buscarCategoria(Long id) {
        for (Categoria c : categorias) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }
}
