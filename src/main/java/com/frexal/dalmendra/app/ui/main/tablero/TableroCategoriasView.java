package com.frexal.dalmendra.app.ui.main.tablero;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente visual que dibuja el tablero "Por Categorías"
 * utilizando TableroLayoutCalculator para calcular columnas y tamaño de letra.
 *
 * Estilo visual:
 * - Cada columna es una "tabla" blanca con borde.
 * - Cada categoría tiene un encabezado con fondo oscuro para distinguirla.
 * - Cada artículo es un renglón con línea divisoria y la cantidad en su propia celda.
 */
public class TableroCategoriasView extends StackPane {

    // ===== Estilos =====
    private static final String COLOR_BORDE_TABLA = "#7d8f94";
    private static final String COLOR_LINEA_RENGLON = "#cfd8dc";
    private static final String COLOR_FONDO_TABLA = "white";
    private static final String COLOR_FONDO_TITULO = "#30505b";
    private static final String COLOR_TEXTO_TITULO = "white";
    private static final String COLOR_TEXTO = "#1f1f1f";
    private static final String COLOR_MINIMO = "#ffb3b3";
    private static final String COLOR_DESEADO = "#fff3a3";

    /** Caracteres mínimos del nombre que deben poder leerse (se ajusta según los datos). */
    private static final int MIN_CHARS_LEGIBLES = 8;
    private static final int MAX_CHARS_OBJETIVO = 16;

    /** Margen vertical total (padding externo + bordes de tabla). */
    private static final double MARGEN_VERTICAL = 10.0;

    private static final int MAX_COLUMNAS = 6;

    private final VBox container;
    private final Label warningLabel;
    private final Label entradasLabel;

    private List<Categoria> categorias = new ArrayList<>();
    private List<Existencia> existencias = new ArrayList<>();
    private boolean permitirDividir = false;
    private com.frexal.dalmendra.app.model.EntradasDelDia entradasDelDia;

    private final Map<String, String> palabraClavePorTitulo = new HashMap<>();

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return texto.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    public TableroCategoriasView() {
        setMinSize(0, 0);

        container = new VBox(0);
        container.setAlignment(Pos.TOP_LEFT);
        container.setMinSize(0, 0);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        warningLabel = new Label();
        warningLabel.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px;");
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
        StackPane.setAlignment(warningLabel, Pos.BOTTOM_CENTER);

        entradasLabel = new Label();
        entradasLabel.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #c8e6c9; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 6px; -fx-text-alignment: right;");
        entradasLabel.setVisible(false);
        entradasLabel.setManaged(false);
        StackPane.setAlignment(entradasLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(entradasLabel, new Insets(10));

        this.getChildren().addAll(container, warningLabel, entradasLabel);

        // Recalcular el layout de forma segura cuando cambia el tamaño
        this.widthProperty().addListener((obs, oldV, newV) -> Platform.runLater(this::updateLayoutView));
        this.heightProperty().addListener((obs, oldV, newV) -> Platform.runLater(this::updateLayoutView));
    }

    public void setEntradasDelDia(com.frexal.dalmendra.app.model.EntradasDelDia entradas) {
        this.entradasDelDia = entradas;
        actualizarEtiquetaEntradas();
    }

    private void actualizarEtiquetaEntradas() {
        if (entradasDelDia != null && entradasDelDia.getTotal() > 0) {
            entradasLabel.setText(String.format("Entradas hoy:\nCompras: %d\nMovimientos: %d\nTOTAL: %d",
                    entradasDelDia.getCompras(),
                    entradasDelDia.getMovimientos(),
                    entradasDelDia.getTotal()));
            entradasLabel.setVisible(true);
        } else {
            entradasLabel.setVisible(false);
        }
    }

    public void updateData(List<Categoria> categorias, List<Existencia> existencias, boolean permitirDividir) {
        this.categorias = categorias != null ? categorias : new ArrayList<>();
        this.existencias = existencias != null ? existencias : new ArrayList<>();
        this.permitirDividir = permitirDividir;
        Platform.runLater(this::updateLayoutView);
    }

    private void updateLayoutView() {
        try {
            double width = this.getWidth();
            double height = this.getHeight();

            if (width <= 0 || height <= 0 || categorias.isEmpty() || existencias.isEmpty()) {
                container.getChildren().clear();
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
                return;
            }

            // ===== 1. Agrupar artículos por categoría (regla: la descripción empieza con la palabra clave) =====
            List<TableroLayoutCalculator.Block> blocks = new ArrayList<>();
            Map<String, TableroLayoutCalculator.Block> blockMap = new HashMap<>();
            palabraClavePorTitulo.clear();
            List<Categoria> catsOrdenadas = new ArrayList<>();

            for (Categoria c : categorias) {
                String title = c.getDescripcion() != null ? c.getDescripcion().toUpperCase() : "";
                if (blockMap.containsKey(title)) {
                    continue;
                }
                TableroLayoutCalculator.Block b = new TableroLayoutCalculator.Block(title);
                blocks.add(b);
                blockMap.put(title, b);
                palabraClavePorTitulo.put(title, c.getPalabraClave());
                catsOrdenadas.add(c);
            }

            for (Existencia ex : existencias) {
                String desc = normalizar(ex.getDescripcion());
                for (Categoria c : catsOrdenadas) {
                    String clave = normalizar(c.getPalabraClave());
                    if (!clave.isEmpty() && desc.startsWith(clave)) {
                        String title = c.getDescripcion() != null ? c.getDescripcion().toUpperCase() : "";
                        TableroLayoutCalculator.Block b = blockMap.get(title);
                        if (b != null && !b.items.contains(ex)) {
                            b.items.add(ex);
                        }
                    }
                }
            }

            blocks.removeIf(b -> b.items.isEmpty());

            if (blocks.isEmpty()) {
                container.getChildren().clear();
                return;
            }

            // ===== 2. Cuántos caracteres queremos que se lean del nombre =====
            int maxLen = 0;
            for (TableroLayoutCalculator.Block b : blocks) {
                for (Object o : b.items) {
                    maxLen = Math.max(maxLen, limpiarDescripcion((Existencia) o, b.title).length());
                }
            }
            int minChars = Math.max(MIN_CHARS_LEGIBLES, Math.min(MAX_CHARS_OBJETIVO, maxLen));

            // ===== 3. Calcular columnas y tamaño de letra =====
            double usableHeight = height - MARGEN_VERTICAL;
            TableroLayoutCalculator.LayoutResult layout = TableroLayoutCalculator.calculate(
                    blocks, width, usableHeight, permitirDividir, MAX_COLUMNAS, minChars);

            double fontSize = layout.fontSize;
            double rowHeight = layout.rowHeight;
            double titleHeight = rowHeight * TableroLayoutCalculator.TITLE_HEIGHT_FACTOR;
            double colWidth = TableroLayoutCalculator.columnWidth(width, layout.columns);
            double qtyWidth = TableroLayoutCalculator.qtyWidth(fontSize);

            // ===== 4. Dibujar =====
            container.getChildren().clear();

            HBox columnsContainer = new HBox(TableroLayoutCalculator.COLUMN_GAP);
            columnsContainer.setPadding(new Insets(4, TableroLayoutCalculator.OUTER_PADDING_H / 2,
                    4, TableroLayoutCalculator.OUTER_PADDING_H / 2));
            columnsContainer.setAlignment(Pos.TOP_LEFT);
            columnsContainer.setFillHeight(false);
            columnsContainer.setMinSize(0, 0);

            VBox[] columnBoxes = new VBox[layout.columns];
            for (int i = 0; i < layout.columns; i++) {
                VBox col = new VBox(0);
                col.setAlignment(Pos.TOP_LEFT);
                col.setMinWidth(colWidth);
                col.setPrefWidth(colWidth);
                col.setMaxWidth(colWidth);
                col.setMaxHeight(Region.USE_PREF_SIZE);
                col.setStyle("-fx-background-color: " + COLOR_FONDO_TABLA + ";"
                        + "-fx-border-color: " + COLOR_BORDE_TABLA + ";"
                        + "-fx-border-width: 1;");
                columnBoxes[i] = col;
            }

            int currentColumn = 0;
            double currentY = 0;
            int drawnItemsCount = 0;
            int totalItemsCount = blocks.stream().mapToInt(b -> b.items.size()).sum();

            for (TableroLayoutCalculator.Block block : blocks) {
                if (currentColumn >= layout.columns) break;

                if (!permitirDividir) {
                    double blockTotalHeight = titleHeight + (block.items.size() * rowHeight);
                    if (currentY > 0 && currentY + blockTotalHeight > usableHeight) {
                        currentColumn++;
                        currentY = 0;
                        if (currentColumn >= layout.columns) break;
                    }
                } else if (currentY + titleHeight + rowHeight > usableHeight) {
                    currentColumn++;
                    currentY = 0;
                    if (currentColumn >= layout.columns) break;
                }

                columnBoxes[currentColumn].getChildren().add(crearTitulo(block.title, fontSize, titleHeight));
                currentY += titleHeight;

                for (Object itemObj : block.items) {
                    Existencia ex = (Existencia) itemObj;

                    if (currentY + rowHeight > usableHeight) {
                        if (permitirDividir) {
                            currentColumn++;
                            currentY = 0;
                            if (currentColumn >= layout.columns) break;
                            columnBoxes[currentColumn].getChildren().add(
                                    crearTitulo(block.title + " (cont.)", fontSize, titleHeight));
                            currentY += titleHeight;
                        } else {
                            break; // No cabe y no se permite dividir
                        }
                    }

                    columnBoxes[currentColumn].getChildren().add(
                            crearFila(ex, block.title, fontSize, rowHeight, qtyWidth));
                    currentY += rowHeight;
                    drawnItemsCount++;
                }
            }

            // Solo agregar las columnas que tienen contenido
            for (VBox col : columnBoxes) {
                if (!col.getChildren().isEmpty()) {
                    columnsContainer.getChildren().add(col);
                }
            }

            container.getChildren().add(columnsContainer);

            if (drawnItemsCount < totalItemsCount) {
                int faltan = totalItemsCount - drawnItemsCount;
                warningLabel.setText("¡Ojo! Faltan " + faltan + " artículos. Ajuste el tamaño o aumente pantallas.");
                warningLabel.setVisible(true);
                warningLabel.setManaged(true);
            } else {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Label lblErr = new Label("Error de layout: " + ex.getMessage());
            lblErr.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            container.getChildren().clear();
            container.getChildren().add(lblErr);
        }
    }

    // ======================================================
    // Construcción de celdas
    // ======================================================

    private Label crearTitulo(String texto, double fontSize, double titleHeight) {
        Label titulo = new Label(texto);
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setMinWidth(0);
        titulo.setMinHeight(titleHeight);
        titulo.setPrefHeight(titleHeight);
        titulo.setMaxHeight(titleHeight);
        titulo.setTextOverrun(OverrunStyle.ELLIPSIS);
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.setStyle("-fx-font-size: " + fmt(fontSize) + "px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + COLOR_TEXTO_TITULO + ";"
                + "-fx-background-color: " + COLOR_FONDO_TITULO + ";"
                + "-fx-padding: 0 6 0 6;");
        return titulo;
    }

    private HBox crearFila(Existencia ex, String categoriaTitulo, double fontSize, double rowHeight, double qtyWidth) {
        HBox fila = new HBox(0);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setMaxWidth(Double.MAX_VALUE);
        fila.setMinWidth(0);
        fila.setMinHeight(rowHeight);
        fila.setPrefHeight(rowHeight);
        fila.setMaxHeight(rowHeight);

        // --- Nombre ---
        Label lblDesc = new Label(limpiarDescripcion(ex, categoriaTitulo));
        lblDesc.setMinWidth(0);
        lblDesc.setMaxWidth(Double.MAX_VALUE);
        lblDesc.setMaxHeight(rowHeight);
        lblDesc.setTextOverrun(OverrunStyle.ELLIPSIS);
        lblDesc.setStyle("-fx-font-size: " + fmt(fontSize) + "px; -fx-text-fill: " + COLOR_TEXTO + "; -fx-padding: 0 4 0 6;");
        HBox.setHgrow(lblDesc, Priority.ALWAYS);

        // --- Cantidad (celda propia, nunca se recorta) ---
        BigDecimal dExistencia = ex.getExistenciaOrZero();
        String txtExistencia = (dExistencia.stripTrailingZeros().scale() <= 0)
                ? String.valueOf(dExistencia.intValue())
                : String.format("%.1f", dExistencia.doubleValue());

        Label lblExist = new Label(txtExistencia);
        lblExist.setMinWidth(qtyWidth);
        lblExist.setPrefWidth(qtyWidth);
        lblExist.setMaxWidth(qtyWidth);
        lblExist.setMaxHeight(Double.MAX_VALUE);
        lblExist.setAlignment(Pos.CENTER_RIGHT);
        lblExist.setTextOverrun(OverrunStyle.CLIP);
        lblExist.setStyle("-fx-font-size: " + fmt(fontSize) + "px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXTO + ";"
                + "-fx-padding: 0 6 0 2;"
                + "-fx-border-color: transparent transparent transparent " + COLOR_LINEA_RENGLON + ";"
                + "-fx-border-width: 0 0 0 1;");

        String colorSemaforo = getColorSemaforo(dExistencia.doubleValue(),
                ex.getStockMinimo() != null ? ex.getStockMinimo().doubleValue() : null,
                ex.getStockDeseado() != null ? ex.getStockDeseado().doubleValue() : null);

        fila.setStyle("-fx-background-color: " + colorSemaforo + ";"
                + "-fx-border-color: transparent transparent " + COLOR_LINEA_RENGLON + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        fila.getChildren().addAll(lblDesc, lblExist);
        return fila;
    }

    /** Quita la palabra clave del inicio del nombre (igual que el reporte original). */
    private String limpiarDescripcion(Existencia ex, String categoriaTitulo) {
        String descripcion = ex.getDescripcion() != null ? ex.getDescripcion().trim() : "";
        String tituloBase = categoriaTitulo.replace(" (cont.)", "");
        String clave = palabraClavePorTitulo.get(tituloBase);
        if (clave != null && !clave.trim().isEmpty()
                && normalizar(descripcion).startsWith(normalizar(clave))
                && descripcion.length() >= clave.trim().length()) {
            String restante = descripcion.substring(clave.trim().length()).trim();
            restante = restante.replaceFirst("^[\\.-]+\\s*", "").trim();
            if (!restante.isEmpty()) {
                descripcion = restante;
            }
        }
        return descripcion;
    }

    private String getColorSemaforo(double existenciaActual, Double stockMinimo, Double stockDeseado) {
        if (stockMinimo == null || stockDeseado == null) {
            return COLOR_FONDO_TABLA;
        }
        if (existenciaActual <= stockMinimo) {
            return COLOR_MINIMO;
        } else if (existenciaActual <= stockDeseado) {
            return COLOR_DESEADO;
        }
        return COLOR_FONDO_TABLA;
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
