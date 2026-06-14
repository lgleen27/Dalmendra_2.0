package com.frexal.dalmendra.app.ui.main;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.repository.ConfiguracionRepository;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;
import com.frexal.dalmendra.app.repository.OrdenExistenciaRepository;
import com.frexal.dalmendra.app.repository.SucursalRepository;
import com.frexal.dalmendra.app.service.AppState;
import com.frexal.dalmendra.app.service.ConfiguracionService;
import com.frexal.dalmendra.app.service.InventarioSyncService;
import com.frexal.dalmendra.app.service.OrdenExistenciaService;
import com.frexal.dalmendra.app.service.SqlServerSucursalClient;
import com.frexal.dalmendra.app.service.SucursalService;
import com.frexal.dalmendra.app.service.SyncSchedulerService;
import com.frexal.dalmendra.app.ui.config.ConfiguracionController;
import com.frexal.dalmendra.app.ui.existencias.ExistenciasController;
import com.frexal.dalmendra.app.ui.sucursales.SucursalesController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class MainController {

    @FXML
    private Label lblVistaActual;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblSync;

    @FXML
    private StackPane pnlCentro;

    @FXML
    private ComboBox<Sucursal> cmbSucursales;

    private final AppState appState = new AppState();

    private final ConfiguracionRepository configuracionRepository = new ConfiguracionRepository();
    private final SucursalRepository sucursalRepository = new SucursalRepository();
    private final ExistenciaRepository existenciaRepository = new ExistenciaRepository();
    private final OrdenExistenciaRepository ordenExistenciaRepository = new OrdenExistenciaRepository();
    private final CategoriaRepository categoriaRepository = new CategoriaRepository();

    private final ConfiguracionService configuracionService =
            new ConfiguracionService(configuracionRepository, appState);

    private final SucursalService sucursalService =
            new SucursalService(sucursalRepository, appState);

    private final OrdenExistenciaService ordenExistenciaService =
            new OrdenExistenciaService(ordenExistenciaRepository, appState);

    private final InventarioSyncService inventarioSyncService =
            new InventarioSyncService(
                    new SqlServerSucursalClient(),
                    existenciaRepository,
                    categoriaRepository,
                    sucursalService,
                    ordenExistenciaService,
                    appState
            );

    private final SyncSchedulerService syncSchedulerService = new SyncSchedulerService();

    @FXML
    public void initialize() {
        try {
            lblEstado.setText("Cargando configuración...");
            configuracionService.cargarConfiguracion();
            sucursalService.cargarSucursales();
            ordenExistenciaService.cargarOrdenes();

            configurarComboSucursales();
            cargarComboSucursales();
            aplicarColorSucursalActual();
            definirTimerSync();

            if (sucursalService.haySucursalesActivas()) {
                actualizarExistencias();
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                lblSync.setText("Sin sucursales activas");
                setContenidoCentral(new Label("No hay sucursales activas."));
                mostrarInformacion(
                        "Dalmendra",
                        "No existe ninguna conexión con las sucursales, revisa las conexiones existentes o genera una nueva."
                );
            }

            if (appState.getSucursalSeleccionada() != null) {
                lblEstado.setText("Sistema listo - " + appState.getSucursalSeleccionada().getNombreSucursal());
            } else {
                lblEstado.setText("Sistema listo");
            }

        } catch (Exception ex) {
            lblEstado.setText("Error al iniciar");
            mostrarError("Error de inicio", ex.getMessage());
        }
    }

    private void configurarComboSucursales() {
        cmbSucursales.setConverter(new StringConverter<Sucursal>() {
            @Override
            public String toString(Sucursal sucursal) {
                return sucursal != null ? sucursal.getNombreSucursal() : "";
            }

            @Override
            public Sucursal fromString(String string) {
                return null;
            }
        });
    }

    private void cargarComboSucursales() {
        cmbSucursales.getItems().clear();
        cmbSucursales.getItems().addAll(appState.getSucursalesActivas());

        Sucursal seleccionada = appState.getSucursalSeleccionada();

        if (seleccionada != null && seleccionada.getId() != null) {
            for (Sucursal sucursal : cmbSucursales.getItems()) {
                if (sucursal.getId() != null && sucursal.getId().equals(seleccionada.getId())) {
                    cmbSucursales.getSelectionModel().select(sucursal);
                    appState.setSucursalSeleccionada(sucursal);
                    aplicarColorSucursal(sucursal);
                    return;
                }
            }
        }

        if (!cmbSucursales.getItems().isEmpty()) {
            cmbSucursales.getSelectionModel().selectFirst();
            appState.setSucursalSeleccionada(cmbSucursales.getSelectionModel().getSelectedItem());
            aplicarColorSucursalActual();
        } else {
            appState.setSucursalSeleccionada(null);
            aplicarColorSucursal(null);
        }
    }

    @FXML
    private void onSucursalSeleccionada() {
        Sucursal sucursal = cmbSucursales.getSelectionModel().getSelectedItem();
        appState.setSucursalSeleccionada(sucursal);
        aplicarColorSucursalActual();

        if (sucursal != null) {
            lblEstado.setText("Sucursal activa: " + sucursal.getNombreSucursal());
            abrirReporteInicial();
        } else {
            lblEstado.setText("Sin sucursal seleccionada");
            pnlCentro.getChildren().clear();
        }
    }

    private void abrirReporteInicial() {
        String reporte = appState.getFirstReport();

        if (reporte == null || reporte.trim().isEmpty()) {
            abrirVistaPorCategorias();
            return;
        }

        switch (reporte) {
            case "PorCategorias":
                abrirVistaPorCategorias();
                break;
            case "ListadoConCodigo":
                abrirVistaListadoConCodigo();
                break;
            case "ListadoSinCodigo":
                abrirVistaListadoSinCodigo();
                break;
            default:
                abrirVistaPorCategorias();
                break;
        }
    }

    private void actualizarExistencias() {
        if (!sucursalService.haySucursalesActivas()) {
            lblSync.setText("Sin sucursales activas");
            lblEstado.setText("No hay sucursales activas para sincronizar");
            return;
        }

        lblSync.setText("Sincronizando...");
        lblEstado.setText("Actualizando existencias...");

        Thread thread = new Thread(() -> {
            inventarioSyncService.sincronizarTodas();

            Platform.runLater(() -> {
                if (appState.isHayErrorSincronizacion()) {
                    lblSync.setText("Con errores");
                    mostrarErroresSincronizacion(appState.getErroresSincronizacion());
                } else {
                    lblSync.setText("Correcta");
                }

                abrirReporteInicial();

                if (appState.getSucursalSeleccionada() != null) {
                    lblEstado.setText("Existencias actualizadas - " + appState.getSucursalSeleccionada().getNombreSucursal());
                } else {
                    lblEstado.setText("Existencias actualizadas");
                }
            });
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void definirTimerSync() {
        try {
            int minutos = Integer.parseInt(appState.getTimeSyncSucursal());

            if (minutos > 0) {
                syncSchedulerService.programarSincronizacion(
                        this::actualizarExistencias,
                        minutos
                );
                lblSync.setText("Programada cada " + minutos + " min");
            } else {
                lblSync.setText("Desactivada");
            }
        } catch (Exception ex) {
            lblSync.setText("Configuración inválida");
        }
    }

    private void abrirVistaPorCategorias() {
        lblVistaActual.setText("Vista: Reporte por Categorias");
        cargarReportePorCategoriasEnPanel();

        if (appState.getSucursalSeleccionada() != null) {
            lblEstado.setText("Mostrando reporte por categorías - " + appState.getSucursalSeleccionada().getNombreSucursal());
        } else {
            lblEstado.setText("Mostrando reporte por categorías");
        }
    }

    private void abrirVistaListadoConCodigo() {
        lblVistaActual.setText("Vista: Listado con Codigo");
        cargarListadoGeneralEnPanel(true);

        if (appState.getSucursalSeleccionada() != null) {
            lblEstado.setText("Mostrando listado con código - " + appState.getSucursalSeleccionada().getNombreSucursal());
        } else {
            lblEstado.setText("Mostrando listado con código");
        }
    }

    private void abrirVistaListadoSinCodigo() {
        lblVistaActual.setText("Vista: Listado sin Codigo");
        cargarListadoGeneralEnPanel(false);

        if (appState.getSucursalSeleccionada() != null) {
            lblEstado.setText("Mostrando listado sin código - " + appState.getSucursalSeleccionada().getNombreSucursal());
        } else {
            lblEstado.setText("Mostrando listado sin código");
        }
    }

    @FXML
    private void onReportePorCategorias() {
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaPorCategorias();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onListadoConCodigo() {
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaListadoConCodigo();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onListadoSinCodigo() {
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaListadoSinCodigo();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onSucursales() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/frexal/dalmendra/app/ui/sucursales/SucursalesView.fxml")
            );

            Parent view = loader.load();

            SucursalesController controller = loader.getController();
            controller.initData(sucursalService, appState);

            Stage stage = new Stage();
            stage.setTitle("Sucursales");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(lblEstado.getScene().getWindow());
            stage.setResizable(false);
            stage.setScene(new Scene(view));
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

            sucursalService.cargarSucursales();
            cargarComboSucursales();
            aplicarColorSucursalActual();

            if (sucursalService.haySucursalesActivas()) {
                lblEstado.setText("Catálogo de sucursales cerrado");
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                lblSync.setText("Sin sucursales activas");
                lblEstado.setText("Catálogo de sucursales cerrado");
                setContenidoCentral(new Label("No hay sucursales activas."));
            }

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el catálogo de sucursales: " + ex.getMessage());
        }
    }

    @FXML
    private void onCategorias() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/frexal/dalmendra/app/ui/categorias/CategoriasView.fxml")
            );

            Parent view = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Categorías");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(lblEstado.getScene().getWindow());
            stage.setResizable(false);
            stage.setScene(new Scene(view));
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

            lblEstado.setText("Catálogo de categorías cerrado");
            abrirReporteInicial();

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el catálogo de categorías: " + ex.getMessage());
        }
    }

    @FXML
    private void onArticulos() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/frexal/dalmendra/app/ui/existencias/ExistenciasView.fxml")
            );

            Parent view = loader.load();

            ExistenciasController controller = loader.getController();
            controller.initData(sucursalService, ordenExistenciaService, appState);

            Stage stage = new Stage();
            stage.setTitle("Existencias");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(lblEstado.getScene().getWindow());
            stage.setResizable(false);
            stage.setScene(new Scene(view));
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

            lblEstado.setText("Catálogo de existencias cerrado");
            abrirReporteInicial();

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir existencias: " + ex.getMessage());
        }
    }

    @FXML
    private void onConfiguracion() {
        abrirVistaConfiguracion();
    }

    private void abrirVistaConfiguracion() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/frexal/dalmendra/app/ui/config/ConfiguracionView.fxml")
            );

            Parent view = loader.load();

            ConfiguracionController controller = loader.getController();
            controller.initData(configuracionService, appState, () -> {
                definirTimerSync();
                lblEstado.setText("Configuración actualizada");
                lblSync.setText("Configuración guardada");
            });

            Stage stage = new Stage();
            stage.setTitle("Configuración");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(lblEstado.getScene().getWindow());
            stage.setResizable(false);

            Scene scene = new Scene(view);
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.showAndWait();

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir la configuración: " + ex.getMessage());
        }
    }

    private void setContenidoCentral(Node node) {
        pnlCentro.getChildren().clear();
        if (node != null) {
            pnlCentro.getChildren().add(node);
        }
    }

    private ScrollPane crearContenedorScrollable(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private TableView<Existencia> crearTablaListado(boolean mostrarCodigo) {
        TableView<Existencia> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No hay registros para mostrar."));
        table.setStyle("-fx-font-size: 16px;");

        if (mostrarCodigo) {
            TableColumn<Existencia, String> colCodigo = new TableColumn<>("Código");
            colCodigo.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(valor(data.getValue().getCodigo())));
            table.getColumns().add(colCodigo);
        }

        TableColumn<Existencia, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(valor(data.getValue().getDescripcion())));

        TableColumn<Existencia, BigDecimal> colExistencia = new TableColumn<>("Existencia");
        colExistencia.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getExistenciaOrZero()));

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.####");
        colExistencia.setCellFactory(col -> new TableCell<Existencia, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item));
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        table.getColumns().add(colDescripcion);
        table.getColumns().add(colExistencia);

        return table;
    }

    private VBox crearBloqueCategoria(Categoria categoria, List<Existencia> existencias) {
        VBox box = new VBox(4);
        box.setPrefWidth(440);
        box.setMaxWidth(440);
        box.getStyleClass().add("reporte-categoria-box");

        Label titulo = new Label(valor(categoria.getDescripcion()).toUpperCase());
        titulo.getStyleClass().add("reporte-categoria-titulo");

        TableView<Existencia> tabla = crearTablaListado(false);
        tabla.setFixedCellSize(28);
        tabla.prefHeightProperty().bind(
                Bindings.size(tabla.getItems()).multiply(tabla.getFixedCellSize()).add(6)
        );
        tabla.setMinHeight(50);
        tabla.setMaxWidth(Double.MAX_VALUE);
        tabla.getStyleClass().add("reporte-categoria-table");

        tabla.getItems().setAll(existencias);

        String css = getClass()
                .getResource("/com/frexal/dalmendra/app/ui/main/main-reportes.css")
                .toExternalForm();

        if (!tabla.getStylesheets().contains(css)) {
            tabla.getStylesheets().add(css);
        }

        box.getChildren().addAll(titulo, tabla);
        return box;
    }

    private void cargarReportePorCategoriasEnPanel() {
        try {
            Sucursal sucursal = appState.getSucursalSeleccionada();

            if (sucursal == null || sucursal.getId() == null) {
                setContenidoCentral(new Label("No hay sucursal seleccionada."));
                return;
            }

            List<Categoria> categorias = categoriaRepository.findActivas();

            FlowPane flow = new FlowPane();
            flow.setPadding(new Insets(14));
            flow.setHgap(18);
            flow.setVgap(18);
            flow.setPrefWrapLength(980);
            flow.setStyle("-fx-background-color: transparent;");

            for (Categoria categoria : categorias) {
                if (categoria.getId() == null) {
                    continue;
                }

                List<Existencia> registros =
                        existenciaRepository.findBySucursalIdAndCategoriaIdOrderByOrden(
                                sucursal.getId(),
                                categoria.getId()
                        );

                if (registros == null || registros.isEmpty()) {
                    continue;
                }

                flow.getChildren().add(crearBloqueCategoria(categoria, registros));
            }

            if (flow.getChildren().isEmpty()) {
                Label lbl = new Label("No hay existencias para mostrar en la sucursal seleccionada.");
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #30505b;");
                flow.getChildren().add(lbl);
            }

            ScrollPane scrollPane = crearContenedorScrollable(flow);

            String css = getClass()
                    .getResource("/com/frexal/dalmendra/app/ui/main/main-reportes.css")
                    .toExternalForm();

            if (!scrollPane.getStylesheets().contains(css)) {
                scrollPane.getStylesheets().add(css);
            }

            setContenidoCentral(scrollPane);

        } catch (Exception ex) {
            mostrarError("Reporte por categorías", "No se pudo cargar el reporte: " + ex.getMessage());
        }
    }

    private void cargarListadoGeneralEnPanel(boolean mostrarCodigo) {
        try {
            Sucursal sucursal = appState.getSucursalSeleccionada();

            if (sucursal == null || sucursal.getId() == null) {
                setContenidoCentral(new Label("No hay sucursal seleccionada."));
                return;
            }

            List<Existencia> registros = existenciaRepository.findBySucursalId(sucursal.getId());
            List<FilaListadoTriple> filas = construirFilasTriples(registros);

            TableView<FilaListadoTriple> table = crearTablaListadoTriple(mostrarCodigo);
            table.getItems().setAll(filas);

            VBox contenedor = new VBox(table);
            contenedor.setPadding(new Insets(12));
            contenedor.setStyle("-fx-background-color: transparent;");
            VBox.setVgrow(table, Priority.ALWAYS);

            setContenidoCentral(contenedor);

        } catch (Exception ex) {
            mostrarError("Listado de existencias", "No se pudo cargar el listado: " + ex.getMessage());
        }
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private void aplicarColorSucursalActual() {
        aplicarColorSucursal(appState.getSucursalSeleccionada());
    }

    private void aplicarColorSucursal(Sucursal sucursal) {
        Color colorBase = Color.WHITE;

        if (sucursal != null && sucursal.getColor() != null && !sucursal.getColor().trim().isEmpty()) {
            try {
                colorBase = Color.web(normalizarColorSucursal(sucursal.getColor().trim()));
            } catch (Exception ex) {
                colorBase = Color.WHITE;
            }
        }

        Color colorSuave = colorBase.deriveColor(0, 1, 1, 0.28);

        pnlCentro.setBackground(new Background(
                new BackgroundFill(colorSuave, CornerRadii.EMPTY, Insets.EMPTY)
        ));
    }

    private String normalizarColorSucursal(String color) {
        String value = color.trim();

        if (value.startsWith("#")) {
            return value;
        }

        if (value.matches("[0-9A-Fa-f]{6}")) {
            return "#" + value;
        }

        return "#FFFFFF";
    }

    private void mostrarErroresSincronizacion(java.util.List<String> errores) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errores de sincronización");
        alert.setHeaderText("Se encontraron errores al sincronizar sucursales.");
        alert.setContentText("Revise el detalle expandible.");

        TextArea textArea = new TextArea(String.join("\n", errores));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(content);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    private List<FilaListadoTriple> construirFilasTriples(List<Existencia> registros) {
        List<FilaListadoTriple> filas = new ArrayList<>();

        for (int i = 0; i < registros.size(); i += 3) {
            Existencia item1 = registros.get(i);
            Existencia item2 = (i + 1 < registros.size()) ? registros.get(i + 1) : null;
            Existencia item3 = (i + 2 < registros.size()) ? registros.get(i + 2) : null;

            filas.add(new FilaListadoTriple(item1, item2, item3));
        }

        return filas;
    }
    
    private TableColumn<FilaListadoTriple, String> crearColumnaSeparador() {
        TableColumn<FilaListadoTriple, String> col = new TableColumn<>("");
        col.setSortable(false);
        col.setReorderable(false);
        col.setResizable(false);
        col.setPrefWidth(16);
        col.setMinWidth(16);
        col.setMaxWidth(16);
        col.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        return col;
    }
    
    private Existencia getExistenciaDeFila(FilaListadoTriple fila, int index) {
        if (fila == null) {
            return null;
        }

        switch (index) {
            case 1:
                return fila.getItem1();
            case 2:
                return fila.getItem2();
            case 3:
                return fila.getItem3();
            default:
                return null;
        }
    }

    private String textoExistencia(FilaListadoTriple fila, int index, java.util.function.Function<Existencia, String> mapper) {
        Existencia e = getExistenciaDeFila(fila, index);
        return e == null ? "" : valor(mapper.apply(e));
    }
    
    private TableView<FilaListadoTriple> crearTablaListadoTriple(boolean mostrarCodigo) {
        TableView<FilaListadoTriple> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No hay registros para mostrar."));
        table.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-background-color: white;" +
                "-fx-border-color: #c9d2d8;" +
                "-fx-border-width: 1;" +
                "-fx-table-cell-border-color: #d9e1e5;"
        );

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.####");

        for (int grupo = 1; grupo <= 3; grupo++) {
            final int grupoActual = grupo;

            if (mostrarCodigo) {
                TableColumn<FilaListadoTriple, String> colCodigo = new TableColumn<>("Código");
                colCodigo.setPrefWidth(90);
                colCodigo.setMinWidth(90);
                colCodigo.setCellValueFactory(data ->
                        new ReadOnlyStringWrapper(
                                textoExistencia(data.getValue(), grupoActual, Existencia::getCodigo)
                        )
                );
                table.getColumns().add(colCodigo);
            }

            TableColumn<FilaListadoTriple, String> colDescripcion = new TableColumn<>("Descripción");
            colDescripcion.setPrefWidth(220);
            colDescripcion.setMinWidth(220);
            colDescripcion.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(
                            textoExistencia(data.getValue(), grupoActual, Existencia::getDescripcion)
                    )
            );

            TableColumn<FilaListadoTriple, BigDecimal> colExistencia = new TableColumn<>("Existencia");
            colExistencia.setPrefWidth(90);
            colExistencia.setMinWidth(90);
            colExistencia.setCellValueFactory(data -> {
                Existencia e = getExistenciaDeFila(data.getValue(), grupoActual);
                return new ReadOnlyObjectWrapper<>(e != null ? e.getExistenciaOrZero() : null);
            });

            colExistencia.setCellFactory(col -> new TableCell<FilaListadoTriple, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(decimalFormat.format(item));
                    }
                    setAlignment(Pos.CENTER_RIGHT);
                }
            });

            table.getColumns().add(colDescripcion);
            table.getColumns().add(colExistencia);

            if (grupoActual < 3) {
                table.getColumns().add(crearColumnaSeparador());
            }
        }

        return table;
    }
}