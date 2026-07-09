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
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador principal de la ventana principal de Dalmendra.
 *
 * Este controlador se encarga de:
 * - cargar configuración y catálogos básicos,
 * - administrar la sucursal seleccionada,
 * - lanzar la sincronización automática o manual,
 * - mostrar reportes y listados en el panel central,
 * - reflejar en pantalla el estado general del sistema.
 */
public class MainController {

    // ======================================================
    // Componentes visuales definidos en MainView.fxml
    // ======================================================

    @FXML
    private Label lblVistaActual;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblSync;

    @FXML
    private Label lblUltimaSync;

    @FXML
    private StackPane pnlCentro;

    @FXML
    private ComboBox<Sucursal> cmbSucursales;

    // ======================================================
    // Estado general y acceso a datos/servicios
    // ======================================================

    /**
     * Estado compartido de la aplicación.
     * Aquí se conserva la sucursal seleccionada, configuración en memoria
     * y banderas auxiliares de sincronización.
     */
    private final AppState appState = new AppState();

    /**
     * Repositorios usados por la ventana principal.
     * Permiten leer configuración, sucursales, existencias, categorías y ordenamientos.
     */
    private final ConfiguracionRepository configuracionRepository = new ConfiguracionRepository();
    private final SucursalRepository sucursalRepository = new SucursalRepository();
    private final ExistenciaRepository existenciaRepository = new ExistenciaRepository();
    private final OrdenExistenciaRepository ordenExistenciaRepository = new OrdenExistenciaRepository();
    private final CategoriaRepository categoriaRepository = new CategoriaRepository();

    /**
     * Bandera simple para evitar que se dispare más de una sincronización al mismo tiempo.
     */
    private volatile boolean sincronizando = false;

    /**
     * Servicios principales usados por la ventana.
     */
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
                    sucursalService,
                    ordenExistenciaService,
                    appState
            );

    private final SyncSchedulerService syncSchedulerService = new SyncSchedulerService();

    // ======================================================
    // Utilidades de formato y alertas de sincronización
    // ======================================================

    /**
     * Formato visual usado para mostrar la fecha/hora de la última sincronización correcta.
     */
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Alerta reutilizable para errores de sincronización.
     * Se mantiene una sola instancia para evitar que se acumulen varias ventanas.
     */
    private Alert alertErroresSync;

    /**
     * Temporizador que cierra automáticamente la alerta de errores de sincronización
     * después de 30 segundos.
     */
    private PauseTransition autoCloseErroresSync;

    // ======================================================
    // Inicialización principal
    // ======================================================

    /**
     * Inicializa la ventana principal.
     *
     * Flujo principal:
     * 1. Carga configuración general.
     * 2. Carga sucursales y órdenes.
     * 3. Prepara el combo de sucursales.
     * 4. Programa el temporizador de sincronización automática.
     * 5. Si existen sucursales activas, sincroniza y abre el reporte inicial.
     */
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
            actualizarTextoUltimaSincronizacion();
            definirTimerSync();

            if (sucursalService.haySucursalesActivas()) {
                actualizarExistencias();
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                lblSync.setText("Sin sucursales activas");
                lblUltimaSync.setText("Sin registros");
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

    // ======================================================
    // Manejo del combo de sucursales
    // ======================================================

    /**
     * Configura cómo se mostrará cada sucursal dentro del ComboBox.
     */
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

    /**
     * Carga las sucursales activas en el combo y restaura la selección actual si existe.
     */
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
                    actualizarTextoUltimaSincronizacion();
                    return;
                }
            }
        }

        if (!cmbSucursales.getItems().isEmpty()) {
            cmbSucursales.getSelectionModel().selectFirst();
            appState.setSucursalSeleccionada(cmbSucursales.getSelectionModel().getSelectedItem());
            aplicarColorSucursalActual();
            actualizarTextoUltimaSincronizacion();
        } else {
            appState.setSucursalSeleccionada(null);
            aplicarColorSucursal(null);
            actualizarTextoUltimaSincronizacion();
        }
    }

    /**
     * Se ejecuta cuando el usuario cambia la sucursal activa.
     * Actualiza el estado visual, color de fondo y contenido del reporte.
     */
    @FXML
    private void onSucursalSeleccionada() {
        Sucursal sucursal = cmbSucursales.getSelectionModel().getSelectedItem();
        appState.setSucursalSeleccionada(sucursal);
        aplicarColorSucursalActual();
        actualizarTextoUltimaSincronizacion();

        if (sucursal != null) {
            lblEstado.setText("Sucursal activa: " + sucursal.getNombreSucursal());
            abrirReporteInicial();
        } else {
            lblEstado.setText("Sin sucursal seleccionada");
            pnlCentro.getChildren().clear();
        }
    }

    /**
     * Refresca las sucursales desde base de datos y trata de conservar
     * la sucursal que el usuario tenía seleccionada.
     *
     * Esto permite traer la fecha más reciente de sincronización correcta
     * después de ejecutar una sincronización.
     */
    private void refrescarSucursalSeleccionadaDesdeCombo() {
        Sucursal seleccionadaAnterior = appState.getSucursalSeleccionada();

        try {
            sucursalService.cargarSucursales();
        } catch (Exception ex) {
            actualizarTextoUltimaSincronizacion();
            return;
        }

        cargarComboSucursales();

        if (seleccionadaAnterior != null && seleccionadaAnterior.getId() != null) {
            for (Sucursal sucursal : cmbSucursales.getItems()) {
                if (sucursal.getId() != null && sucursal.getId().equals(seleccionadaAnterior.getId())) {
                    cmbSucursales.getSelectionModel().select(sucursal);
                    appState.setSucursalSeleccionada(sucursal);
                    aplicarColorSucursal(sucursal);
                    break;
                }
            }
        }

        actualizarTextoUltimaSincronizacion();
    }

    // ======================================================
    // Apertura de reportes principales
    // ======================================================

    /**
     * Abre la vista inicial configurada en la aplicación.
     * Si no existe configuración, por defecto abre el reporte por categorías.
     */
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

    /**
     * Abre el reporte agrupado por categorías.
     */
    private void abrirVistaPorCategorias() {
        lblVistaActual.setText("Vista: Reporte por Categorias");
        cargarReportePorCategoriasEnPanel();

        if (appState.getSucursalSeleccionada() != null) {
            lblEstado.setText("Mostrando reporte por categorías - " + appState.getSucursalSeleccionada().getNombreSucursal());
        } else {
            lblEstado.setText("Mostrando reporte por categorías");
        }
    }

    /**
     * Abre el listado general mostrando el código del artículo.
     */
    private void abrirVistaListadoConCodigo() {
        lblVistaActual.setText("Vista: Listado con Codigo");
        cargarListadoGeneralEnPanel(true);

        if (appState.getSucursalSeleccionada() != null) {
            lblEstado.setText("Mostrando listado con código - " + appState.getSucursalSeleccionada().getNombreSucursal());
        } else {
            lblEstado.setText("Mostrando listado con código");
        }
    }

    /**
     * Abre el listado general ocultando el código del artículo.
     */
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

    // ======================================================
    // Sincronización automática y estado visible
    // ======================================================

    /**
     * Ejecuta la sincronización de existencias en un hilo separado
     * para no congelar la interfaz gráfica.
     *
     * Comportamiento:
     * - Evita lanzar dos sincronizaciones simultáneas.
     * - Cambia el texto de estado mientras sincroniza.
     * - Si hubo errores, muestra una sola alerta reutilizable.
     * - Si la sincronización fue correcta, actualiza la última fecha visible.
     */
    private void actualizarExistencias() {
        if (sincronizando) {
            return;
        }

        if (!sucursalService.haySucursalesActivas()) {
            lblSync.setText("Sin sucursales activas");
            lblEstado.setText("No hay sucursales activas para sincronizar");
            actualizarTextoUltimaSincronizacion();
            return;
        }

        sincronizando = true;
        lblSync.setText("Sincronizando...");
        lblEstado.setText("Actualizando existencias...");

        Thread thread = new Thread(() -> {
            try {
                inventarioSyncService.sincronizarTodas();

                Platform.runLater(() -> {
                    refrescarSucursalSeleccionadaDesdeCombo();

                    if (appState.isHayErrorSincronizacion()) {
                        lblSync.setText("Con errores");
                        mostrarErroresSincronizacionNoBloqueante(appState.getErroresSincronizacion());
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

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblSync.setText("Error");
                    lblEstado.setText("Fallo durante la sincronización");
                    mostrarError("Error de sincronización", ex.getMessage());
                });
            } finally {
                sincronizando = false;
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Configura la sincronización automática usando el número de minutos
     * almacenado en la configuración.
     */
    private void definirTimerSync() {
        try {
            int minutos = Integer.parseInt(appState.getTimeSyncSucursal());

            if (minutos > 0) {
                syncSchedulerService.detener();
                syncSchedulerService.programarSincronizacion(
                        () -> Platform.runLater(this::actualizarExistencias),
                        minutos
                );
                lblSync.setText("Programada cada " + minutos + " min");
            } else {
                syncSchedulerService.detener();
                lblSync.setText("Desactivada");
            }
        } catch (Exception ex) {
            lblSync.setText("Configuración inválida");
        }
    }

    /**
     * Actualiza el label de la última sincronización correcta
     * usando la fecha almacenada en la sucursal seleccionada.
     */
    private void actualizarTextoUltimaSincronizacion() {
        if (lblUltimaSync == null) {
            return;
        }

        Sucursal sucursal = appState.getSucursalSeleccionada();

        if (sucursal == null) {
            lblUltimaSync.setText("Sin sucursal");
            return;
        }

        LocalDateTime fecha = sucursal.getFechaHoraActualizacion();

        if (fecha == null) {
            lblUltimaSync.setText("Sin registros");
        } else {
            lblUltimaSync.setText(fecha.format(dateTimeFormatter));
        }
    }

    // ======================================================
    // Apertura de catálogos y configuración
    // ======================================================

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
            actualizarTextoUltimaSincronizacion();

            if (sucursalService.haySucursalesActivas()) {
                lblEstado.setText("Catálogo de sucursales cerrado");
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                lblSync.setText("Sin sucursales activas");
                lblUltimaSync.setText("Sin registros");
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

    /**
     * Abre la ventana de configuración.
     * Al guardar, vuelve a definir el temporizador de sincronización.
     */
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
                actualizarTextoUltimaSincronizacion();
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

    // ======================================================
    // Utilidades del panel central
    // ======================================================

    /**
     * Reemplaza completamente el contenido del panel central.
     */
    private void setContenidoCentral(Node node) {
        pnlCentro.getChildren().clear();
        if (node != null) {
            pnlCentro.getChildren().add(node);
        }
    }

    /**
     * Envuelve un nodo dentro de un ScrollPane para permitir desplazamiento vertical.
     */
    private ScrollPane crearContenedorScrollable(Node contenido) {
        ScrollPane scrollPane = new ScrollPane(contenido);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    /**
     * Crea una columna visual usada en el reporte por categorías.
     */
    private VBox crearColumnaReporte() {
        VBox columna = new VBox(10);
        columna.setAlignment(Pos.TOP_LEFT);
        columna.setFillWidth(true);
        columna.setMaxWidth(Double.MAX_VALUE);
        columna.setPrefWidth(420);
        return columna;
    }

    // ======================================================
    // Reporte por categorías
    // ======================================================

    /**
     * Crea el bloque visual de una categoría con su encabezado
     * y la lista de existencias pertenecientes a esa categoría.
     */
    private VBox crearBloqueCategoria(Categoria categoria, List<Existencia> existencias) {
        VBox box = new VBox();
        box.setSpacing(0);
        box.setAlignment(Pos.TOP_LEFT);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
                "-fx-background-color: #f4f4f4;" +
                "-fx-border-color: #a9a9a9;" +
                "-fx-border-width: 1;"
        );

        Label titulo = new Label(valor(categoria.getDescripcion()).toUpperCase());
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #2c2c2c;" +
                "-fx-padding: 2 6 4 6;" +
                "-fx-background-color: transparent;"
        );

        VBox filas = new VBox();
        filas.setFillWidth(true);

        for (int i = 0; i < existencias.size(); i++) {
            filas.getChildren().add(crearFilaCategoria(existencias.get(i), categoria, i == existencias.size() - 1));
        }

        box.getChildren().addAll(titulo, filas);
        return box;
    }

    /**
     * Crea una fila individual dentro del bloque de categoría.
     * La descripción puede limpiarse quitando la palabra clave de la categoría.
     */
    private Node crearFilaCategoria(Existencia existencia, Categoria categoria, boolean ultimaFila) {
        GridPane fila = new GridPane();
        fila.setHgap(8);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setMaxWidth(Double.MAX_VALUE);
        fila.setPadding(new Insets(0, 6, 0, 6));

        String bordeInferior = ultimaFila ? "0" : "1";
        fila.setStyle("-fx-border-color: #c0c0c0; -fx-border-width: 1 0 " 
                + bordeInferior + " 0; -fx-background-color: #f4f4f4;");

        Label lblDescripcion = new Label(limpiarDescripcionParaCategoria(existencia, categoria));
        lblDescripcion.setWrapText(false);
        lblDescripcion.setMaxWidth(Double.MAX_VALUE);
        lblDescripcion.setStyle("-fx-font-size: 15px; -fx-text-fill: #222222; -fx-padding: 2 0 2 0;");

        Label lblExistencia = new Label(formatearExistencia(existencia.getExistenciaOrZero()));
        lblExistencia.setAlignment(Pos.CENTER_RIGHT);
        lblExistencia.setMinWidth(46);
        lblExistencia.setPrefWidth(46);
        lblExistencia.setMaxWidth(46);

        aplicarColorStock(lblDescripcion, lblExistencia, existencia, categoria);

        GridPane.setHgrow(lblDescripcion, Priority.ALWAYS);

        fila.add(lblDescripcion, 0, 0);
        fila.add(lblExistencia, 1, 0);

        return fila;
}
    
    private void aplicarColorStock(Label lblExistencia, Label lblDescripcion, Existencia existencia, Categoria categoria) {
        int existenciaActual = existencia.getExistenciaOrZero() != null
                ? existencia.getExistenciaOrZero().intValue()
                : 0;

        Integer stockMinimo = categoria.getStockMinimo();
        Integer stockDeseado = categoria.getStockDeseado();

        String estiloBase = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #222222; -fx-padding: 2 6 2 6;";

        if (stockMinimo == null || stockDeseado == null) {
            lblExistencia.setStyle(estiloBase);
            lblDescripcion.setStyle(estiloBase);
            return;
        }

        if (existenciaActual < stockMinimo) {
            lblExistencia.setStyle(estiloBase + "-fx-background-color: #ffb3b3;");
            lblDescripcion.setStyle(estiloBase + "-fx-background-color: #ffb3b3;");
        } else if (existenciaActual < stockDeseado) {
            lblExistencia.setStyle(estiloBase + "-fx-background-color: #fff3a3;");
            lblDescripcion.setStyle(estiloBase + "-fx-background-color: #fff3a3;");
        } else {
            lblExistencia.setStyle(estiloBase);
            lblDescripcion.setStyle(estiloBase);
        }
    }
    
    /**
     * Formatea la existencia numérica para presentarla en pantalla.
     */
    private String formatearExistencia(BigDecimal valor) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.####");
        return decimalFormat.format(valor != null ? valor : BigDecimal.ZERO);
    }

    /**
     * Construye y muestra el reporte por categorías en el panel central.
     * Distribuye las categorías en tres columnas visuales.
     */
    private void cargarReportePorCategoriasEnPanel() {
        try {
            Sucursal sucursal = appState.getSucursalSeleccionada();

            if (sucursal == null || sucursal.getId() == null) {
                setContenidoCentral(new Label("No hay sucursal seleccionada."));
                return;
            }

            List<Categoria> categorias = categoriaRepository.findActivas();
            List<Existencia> existencias = existenciaRepository.findBySucursalId(sucursal.getId());
            List<Existencia> pendientes = new ArrayList<>(existencias);

            HBox layoutColumnas = new HBox(12);
            layoutColumnas.setPadding(new Insets(8, 10, 8, 10));
            layoutColumnas.setAlignment(Pos.TOP_LEFT);
            layoutColumnas.setStyle("-fx-background-color: transparent;");

            VBox columna1 = crearColumnaReporte();
            VBox columna2 = crearColumnaReporte();
            VBox columna3 = crearColumnaReporte();

            List<VBox> columnas = List.of(columna1, columna2, columna3);
            int indiceColumna = 0;

            for (Categoria categoria : categorias) {
                List<Existencia> registrosCategoria = filtrarExistenciasPorCategoria(pendientes, categoria);

                if (!registrosCategoria.isEmpty()) {
                    VBox bloque = crearBloqueCategoria(categoria, registrosCategoria);
                    columnas.get(indiceColumna).getChildren().add(bloque);
                    pendientes.removeAll(registrosCategoria);
                    indiceColumna = (indiceColumna + 1) % columnas.size();
                }
            }

            layoutColumnas.getChildren().addAll(columna1, columna2, columna3);

            HBox.setHgrow(columna1, Priority.ALWAYS);
            HBox.setHgrow(columna2, Priority.ALWAYS);
            HBox.setHgrow(columna3, Priority.ALWAYS);

            if (columna1.getChildren().isEmpty() && columna2.getChildren().isEmpty() && columna3.getChildren().isEmpty()) {
                Label lbl = new Label("No hay existencias para mostrar en la sucursal seleccionada.");
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #30505b;");
                setContenidoCentral(lbl);
                return;
            }

            ScrollPane scrollPane = crearContenedorScrollable(layoutColumnas);
            setContenidoCentral(scrollPane);

        } catch (Exception ex) {
            mostrarError("Reporte por categorías", "No se pudo cargar el reporte: " + ex.getMessage());
        }
    }

    /**
     * Valida si una existencia pertenece a una categoría con base
     * en la palabra clave configurada para esa categoría.
     */
    private boolean coincideCategoria(Existencia existencia, Categoria categoria) {
        if (existencia == null || categoria == null) {
            return false;
        }

        String palabraClave = normalizarTexto(categoria.getPalabraClave());
        if (palabraClave.isEmpty()) {
            return false;
        }

        String descripcion = normalizarTexto(existencia.getDescripcion());
        return descripcion.startsWith(palabraClave);
    }

    /**
     * Normaliza texto para comparaciones internas.
     */
    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ");
    }

    /**
     * Quita de la descripción la palabra clave de la categoría
     * para que el reporte visual sea más limpio.
     */
    private String limpiarDescripcionParaCategoria(Existencia existencia, Categoria categoria) {
        String descripcion = valor(existencia != null ? existencia.getDescripcion() : "");
        String palabraClave = valor(categoria != null ? categoria.getPalabraClave() : "");

        if (descripcion.isEmpty() || palabraClave.isEmpty()) {
            return descripcion;
        }

        String descripcionNormalizada = normalizarTexto(descripcion);
        String palabraClaveNormalizada = normalizarTexto(palabraClave);

        if (!descripcionNormalizada.startsWith(palabraClaveNormalizada)) {
            return descripcion;
        }

        String restante = descripcion.trim().substring(palabraClave.trim().length()).trim();
        restante = restante.replaceFirst("^[\\.-]+\\s*", "").trim();

        return restante.isEmpty() ? descripcion : restante;
    }

    /**
     * Obtiene las existencias que pertenecen a una categoría específica.
     */
    private List<Existencia> filtrarExistenciasPorCategoria(List<Existencia> origen, Categoria categoria) {
        List<Existencia> resultado = new ArrayList<>();

        for (Existencia existencia : origen) {
            if (coincideCategoria(existencia, categoria)) {
                resultado.add(existencia);
            }
        }

        return resultado;
    }

    // ======================================================
    // Listado general
    // ======================================================

    /**
     * Carga el listado general en formato de tabla triple.
     * Puede mostrar o no la columna de código.
     */
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

    /**
     * Convierte nulos a cadena vacía para evitar errores visuales.
     */
    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    // ======================================================
    // Color visual según sucursal
    // ======================================================

    /**
     * Aplica el color de la sucursal actual al panel central.
     */
    private void aplicarColorSucursalActual() {
        aplicarColorSucursal(appState.getSucursalSeleccionada());
    }

    /**
     * Toma el color configurado para la sucursal y aplica
     * una versión suave como fondo del panel principal.
     */
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

    /**
     * Normaliza el color de sucursal para garantizar un valor hexadecimal válido.
     */
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

    // ======================================================
    // Alertas y mensajes al usuario
    // ======================================================

    /**
     * Muestra una alerta de errores de sincronización no bloqueante.
     *
     * Características:
     * - reutiliza la misma ventana,
     * - actualiza el detalle si llegan nuevos errores,
     * - evita que se acumulen varias alertas,
     * - se cierra sola después de 30 segundos.
     */
    private void mostrarErroresSincronizacionNoBloqueante(List<String> errores) {
        String detalle = (errores == null || errores.isEmpty())
                ? "Se detectaron errores de sincronización."
                : String.join("\n", errores);

        if (alertErroresSync == null) {
            alertErroresSync = new Alert(Alert.AlertType.ERROR);
            alertErroresSync.setTitle("Errores de sincronización");
            alertErroresSync.setHeaderText("Se encontraron errores al sincronizar sucursales.");
            alertErroresSync.setContentText("La ventana se cerrará automáticamente en 30 segundos.");

            TextArea textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane content = new GridPane();
            content.setMaxWidth(Double.MAX_VALUE);
            content.add(textArea, 0, 0);

            alertErroresSync.getDialogPane().setExpandableContent(content);
            alertErroresSync.getDialogPane().setExpanded(true);
        }

        TextArea textArea = (TextArea) ((GridPane) alertErroresSync.getDialogPane().getExpandableContent())
                .getChildren().get(0);

        textArea.setText(detalle);

        if (autoCloseErroresSync == null) {
            autoCloseErroresSync = new PauseTransition(Duration.seconds(30));
            autoCloseErroresSync.setOnFinished(event -> {
                if (alertErroresSync != null) {
                    alertErroresSync.hide();
                }
            });
        }

        autoCloseErroresSync.stop();

        if (!alertErroresSync.isShowing()) {
            alertErroresSync.show();
        }

        autoCloseErroresSync.playFromStart();
    }

    /**
     * Muestra una alerta informativa modal.
     */
    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra una alerta de error modal.
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ======================================================
    // Construcción del listado triple
    // ======================================================

    /**
     * Agrupa existencias de tres en tres para formar una fila
     * del listado general tipo triple.
     */
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

    /**
     * Crea una columna separadora visual entre grupos del listado triple.
     */
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

    /**
     * Obtiene la existencia correspondiente a una de las tres posiciones
     * dentro de la fila del listado triple.
     */
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

    /**
     * Obtiene un texto de una existencia específica de la fila,
     * usando una función que indica qué propiedad extraer.
     */
    private String textoExistencia(FilaListadoTriple fila, int index, java.util.function.Function<Existencia, String> mapper) {
        Existencia e = getExistenciaDeFila(fila, index);
        return e == null ? "" : valor(mapper.apply(e));
    }

    /**
     * Crea la tabla visual del listado triple.
     * Cada fila puede contener hasta tres artículos.
     */
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