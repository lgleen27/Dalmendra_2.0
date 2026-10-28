package com.frexal.dalmendra.app.ui.entradas;

import com.frexal.dalmendra.app.model.AuditoriaEntradaItem;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.service.SqlServerSucursalClient;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador de la ventana modal de Auditoría de Entradas del Día.
 * Permite desglosar y conciliar las compras y movimientos de inventario registrados en Soft Restaurant,
 * diferenciando las que ingresaron al Almacén 3 (Producción) vs otros almacenes,
 * con soporte para conmutar entre diferentes sucursales activas en tiempo real.
 */
public class AuditoriaEntradasController {

    @FXML
    private Label lblSubtitulo;

    @FXML
    private ComboBox<Sucursal> cmbSucursales;

    @FXML
    private Label lblFecha;

    @FXML
    private TableView<AuditoriaEntradaItem> tblEntradas;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colHora;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colCodigo;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colDescripcion;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colCantidad;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colAlmacen;

    @FXML
    private TableColumn<AuditoriaEntradaItem, String> colOrigen;

    @FXML
    private Label lblTotalCompras;

    @FXML
    private Label lblTotalMovimientos;

    @FXML
    private Label lblTotalOtros;

    @FXML
    private Label lblGranTotal;

    private Sucursal sucursal;
    private SqlServerSucursalClient sqlClient;
    private com.frexal.dalmendra.app.service.AppState appState;
    private final ObservableList<AuditoriaEntradaItem> items = FXCollections.observableArrayList();
    private final DecimalFormat formatoCantidad = new DecimalFormat("#,##0.####");

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarEstiloFilas();
        tblEntradas.setItems(items);
        configurarComboConverter();
    }

    private void configurarComboConverter() {
        if (cmbSucursales != null) {
            cmbSucursales.setConverter(new StringConverter<Sucursal>() {
                @Override
                public String toString(Sucursal s) {
                    return s != null ? s.getNombreSucursal() : "";
                }

                @Override
                public Sucursal fromString(String string) {
                    return null;
                }
            });
        }
    }

    public void initData(Sucursal sucursalActual, List<Sucursal> sucursalesActivas, SqlServerSucursalClient sqlClient, com.frexal.dalmendra.app.service.AppState appState) {
        this.appState = appState;
        this.sqlClient = sqlClient != null ? sqlClient : new SqlServerSucursalClient();

        if (cmbSucursales != null && sucursalesActivas != null) {
            cmbSucursales.getItems().setAll(sucursalesActivas);
            if (sucursalActual != null) {
                for (Sucursal s : sucursalesActivas) {
                    if (s.getId() != null && s.getId().equals(sucursalActual.getId())) {
                        cmbSucursales.getSelectionModel().select(s);
                        this.sucursal = s;
                        break;
                    }
                }
            }
            if (this.sucursal == null && !sucursalesActivas.isEmpty()) {
                cmbSucursales.getSelectionModel().selectFirst();
                this.sucursal = cmbSucursales.getSelectionModel().getSelectedItem();
            }
        } else {
            this.sucursal = sucursalActual;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblFecha.setText("Fecha: " + LocalDate.now().format(dtf));

        cargarDatos();
    }

    public void initData(Sucursal sucursalActual, List<Sucursal> sucursalesActivas, SqlServerSucursalClient sqlClient) {
        initData(sucursalActual, sucursalesActivas, sqlClient, null);
    }

    public void initData(Sucursal sucursalActual, SqlServerSucursalClient sqlClient) {
        initData(sucursalActual, sucursalActual != null ? List.of(sucursalActual) : List.of(), sqlClient, null);
    }

    private void configurarColumnas() {
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colAlmacen.setCellValueFactory(new PropertyValueFactory<>("nombreAlmacen"));
        colOrigen.setCellValueFactory(new PropertyValueFactory<>("origen"));

        colCantidad.setCellValueFactory(cellData -> {
            BigDecimal cant = cellData.getValue().getCantidad();
            return new SimpleStringProperty(cant != null ? formatoCantidad.format(cant) : "0");
        });

        // Estilizar columna de cantidad para resaltar bajas/ajustes negativos en rojo
        colCantidad.setCellFactory(column -> new TableCell<AuditoriaEntradaItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    AuditoriaEntradaItem rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowItem != null && rowItem.getCantidad() != null && rowItem.getCantidad().compareTo(BigDecimal.ZERO) < 0) {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Estilizar columna de almacén para marcar visualmente almacenes distintos al 3
        colAlmacen.setCellFactory(column -> new TableCell<AuditoriaEntradaItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    AuditoriaEntradaItem rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowItem != null && !rowItem.isEsAlmacenProduccion()) {
                        setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #2e7d32;");
                    }
                }
            }
        });

        // Estilizar columna de origen para resaltar bajas/mermas
        colOrigen.setCellFactory(column -> new TableCell<AuditoriaEntradaItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Merma") || item.contains("(-)")) {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else if (item.contains("Compra")) {
                        setStyle("-fx-text-fill: #1565c0;");
                    } else {
                        setStyle("-fx-text-fill: #2e7d32;");
                    }
                }
            }
        });
    }

    private void configurarEstiloFilas() {
        // Resaltar renglones de otros almacenes o bajas
        tblEntradas.setRowFactory(tv -> new TableRow<AuditoriaEntradaItem>() {
            @Override
            protected void updateItem(AuditoriaEntradaItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (!item.isEsAlmacenProduccion()) {
                    setStyle("-fx-background-color: #fff8e1; -fx-border-color: #ffe082 transparent #ffe082 transparent;");
                } else if (item.getCantidad() != null && item.getCantidad().compareTo(BigDecimal.ZERO) < 0) {
                    setStyle("-fx-background-color: #ffebee; -fx-border-color: #ffcdd2 transparent #ffcdd2 transparent;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void cargarDatos() {
        if (sucursal == null) return;

        lblSubtitulo.setText("Consultando registros en vivo desde Soft Restaurant (" + sucursal.getNombreSucursal() + ")...");

        Thread thread = new Thread(() -> {
            String prefijo = appState != null ? appState.getPrefijoOmitirInsumos() : null;
            List<AuditoriaEntradaItem> detalle = sqlClient.consultarDetalleEntradasHoy(sucursal, prefijo);

            Platform.runLater(() -> {
                items.setAll(detalle);
                calcularTotales(detalle);
                lblSubtitulo.setText("Se encontraron " + detalle.size() + " movimientos de inventario el día de hoy.");
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void calcularTotales(List<AuditoriaEntradaItem> detalle) {
        BigDecimal totalCompras = BigDecimal.ZERO;
        BigDecimal totalMovimientos = BigDecimal.ZERO;
        BigDecimal totalOtros = BigDecimal.ZERO;

        for (AuditoriaEntradaItem item : detalle) {
            BigDecimal cant = item.getCantidad() != null ? item.getCantidad() : BigDecimal.ZERO;
            if (item.isEsAlmacenProduccion()) {
                if (item.getIdCompra() != null && item.getIdCompra() > 0) {
                    totalCompras = totalCompras.add(cant);
                } else {
                    totalMovimientos = totalMovimientos.add(cant);
                }
            } else {
                totalOtros = totalOtros.add(cant);
            }
        }

        BigDecimal granTotal = totalCompras.add(totalMovimientos).add(totalOtros);

        if (lblTotalCompras != null) {
            lblTotalCompras.setText(formatearCantidad(totalCompras));
        }
        if (lblTotalMovimientos != null) {
            lblTotalMovimientos.setText(formatearCantidad(totalMovimientos));
        }
        if (lblTotalOtros != null) {
            lblTotalOtros.setText(formatearCantidad(totalOtros));
        }
        if (lblGranTotal != null) {
            lblGranTotal.setText(formatearCantidad(granTotal));
        }
    }

    private String formatearCantidad(BigDecimal valor) {
        if (valor == null) return "0 pzas";
        if (valor.stripTrailingZeros().scale() <= 0) {
            return valor.intValue() + " pzas";
        }
        return formatoCantidad.format(valor) + " pzas";
    }

    @FXML
    private void onCambiarSucursal() {
        if (cmbSucursales != null) {
            Sucursal sel = cmbSucursales.getSelectionModel().getSelectedItem();
            if (sel != null && (this.sucursal == null || !sel.getId().equals(this.sucursal.getId()))) {
                this.sucursal = sel;
                cargarDatos();
            }
        }
    }

    @FXML
    private void onActualizar() {
        cargarDatos();
    }

    @FXML
    private void onCerrar() {
        Stage stage = (Stage) tblEntradas.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
