package com.frexal.dalmendra.app.ui.main;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.model.ExistenciaStock;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.repository.ConfiguracionRepository;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;
import com.frexal.dalmendra.app.repository.OrdenExistenciaRepository;
import com.frexal.dalmendra.app.repository.SucursalRepository;
import com.frexal.dalmendra.app.repository.ExistenciaStockRepository;
import com.frexal.dalmendra.app.service.AppState;
import com.frexal.dalmendra.app.service.ConfiguracionService;
import com.frexal.dalmendra.app.service.InventarioSyncService;
import com.frexal.dalmendra.app.service.OrdenExistenciaService;
import com.frexal.dalmendra.app.service.SqlServerSucursalClient;
import com.frexal.dalmendra.app.service.SucursalService;
import com.frexal.dalmendra.app.service.SyncSchedulerService;
import com.frexal.dalmendra.app.service.ReporteCategoriasDataService;
import com.frexal.dalmendra.app.service.ReporteCategoriasJsonService;
import com.frexal.dalmendra.app.ui.config.ConfiguracionController;
import com.frexal.dalmendra.app.ui.existencias.ExistenciasController;
import com.frexal.dalmendra.app.ui.sucursales.SucursalesController;
import com.frexal.dalmendra.app.dto.reporte.ReporteCategoriasJsonDto;
import com.frexal.dalmendra.app.service.ReporteCategoriasApiClient;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import com.frexal.dalmendra.app.service.ReporteCategoriasJsonService;

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

    /** Etiqueta superior que indica el nombre del reporte o listado que se está visualizando. */
    @FXML
    private Label lblVistaActual;

    /** Etiqueta de la barra de estado inferior que muestra la actividad actual del sistema. */
    @FXML
    private Label lblEstado;

    /** Etiqueta que informa el estado de la sincronización (ej. "Correcta", "Sincronizando...", "Con errores"). */
    @FXML
    private Label lblSync;

    /** Etiqueta que muestra la fecha y hora de la última sincronización correcta de la sucursal activa. */
    @FXML
    private Label lblUltimaSync;

    /** Contenedor principal donde se renderizan dinámicamente los tableros de reportes y tablas. */
    @FXML
    private StackPane pnlCentro;

    /** Selector desplegable con la lista de sucursales activas. */
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
    private final ExistenciaStockRepository existenciaStockRepository = new ExistenciaStockRepository();

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
    //private Alert alertErroresSync;

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

    /**
     * Manejador de evento al pulsar el botón de "Reporte por Categorías".
     * Muestra las existencias agrupadas y distribuidas en columnas con semáforos de stock.
     */
    @FXML
    private void onReportePorCategorias() {
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaPorCategorias();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    /**
     * Manejador de evento al pulsar el botón de "Listado con Código".
     * Muestra la tabla triple incluyendo la columna de código de cada insumo.
     */
    @FXML
    private void onListadoConCodigo() {
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaListadoConCodigo();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    /**
     * Manejador de evento al pulsar el botón de "Listado sin Código".
     * Muestra la tabla triple ocultando la columna de código para una vista más limpia.
     */
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
        if (!appState.isBanActualizacion()) {
            return;
        }

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
                        //mostrarErroresSincronizacionNoBloqueante(appState.getErroresSincronizacion());
                    } else {
                        lblSync.setText("Correcta");
                        abrirReporteInicial();

                        if (appState.getSucursalSeleccionada() != null) {
                            lblEstado.setText("Existencias actualizadas - "
                                    + appState.getSucursalSeleccionada().getNombreSucursal());
                            probarGeneracionJsonReal();
                        } else {
                            lblEstado.setText("Existencias actualizadas");
                            probarGeneracionJsonReal();
                        }
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

    /**
     * Abre la ventana modal para la gestión de Sucursales ({@code SucursalesView.fxml}).
     * Pausa la sincronización automática mientras la ventana está visible y refresca
     * las sucursales al cerrarse.
     */
    @FXML
    private void onSucursales() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frexal/dalmendra/app/ui/sucursales/SucursalesView.fxml"));
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

            configurarPausaSincronizacionEnVentana(stage);

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

    /**
     * Abre la ventana modal para la gestión de Categorías ({@code CategoriasView.fxml}).
     * Permite editar palabras clave y reordenar columnas del tablero.
     */
    @FXML
    private void onCategorias() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frexal/dalmendra/app/ui/categorias/CategoriasView.fxml"));
            Parent view = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Categorías");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(lblEstado.getScene().getWindow());
            stage.setResizable(false);
            stage.setScene(new Scene(view));
            stage.sizeToScene();
            stage.centerOnScreen();

            configurarPausaSincronizacionEnVentana(stage);

            stage.showAndWait();

            lblEstado.setText("Catálogo de categorías cerrado");
            abrirReporteInicial();

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el catálogo de categorías: " + ex.getMessage());
        }
    }

    /**
     * Abre la ventana modal para la consulta y ajuste de Existencias ({@code ExistenciasView.fxml}).
     * Permite fijar stock mínimo y deseado, y reordenar artículos por sucursal.
     */
    @FXML
    private void onArticulos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frexal/dalmendra/app/ui/existencias/ExistenciasView.fxml"));
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

            configurarPausaSincronizacionEnVentana(stage);

            stage.showAndWait();

            lblEstado.setText("Catálogo de existencias cerrado");
            abrirReporteInicial();

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir existencias: " + ex.getMessage());
        }
    }

    /**
     * Manejador de evento al pulsar el botón de "Configuración".
     * Delega la apertura de la ventana de configuración del sistema.
     */
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frexal/dalmendra/app/ui/config/ConfiguracionView.fxml"));
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

            configurarPausaSincronizacionEnVentana(stage);

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
        VBox columna = new VBox(0);
        columna.setAlignment(Pos.TOP_LEFT);
        columna.setFillWidth(true);
        columna.setMaxWidth(Double.MAX_VALUE);
        columna.setMinWidth(0);
        columna.setPrefWidth(0);

        HBox.setHgrow(columna, Priority.ALWAYS);

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
        VBox contenedor = new VBox();
        contenedor.setSpacing(0);
        contenedor.setAlignment(Pos.TOP_LEFT);
        contenedor.setFillWidth(true);
        contenedor.setMaxWidth(Double.MAX_VALUE);

        Label titulo = new Label(valor(categoria.getDescripcion()).toUpperCase());
        titulo.setMaxWidth(Double.MAX_VALUE);
        titulo.setMinHeight(33);
        titulo.setPrefHeight(33);

        titulo.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #2c2c2c;" +
                "-fx-background-color: transparent;" +
                "-fx-padding: 0 0 1 0;"
        );

        VBox tabla = new VBox();
        tabla.setSpacing(0);
        tabla.setFillWidth(true);
        tabla.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < existencias.size(); i++) {
            tabla.getChildren().add(
                    crearFilaCategoria(
                            existencias.get(i),
                            categoria,
                            i == existencias.size() - 1
                    )
            );
        }

        contenedor.getChildren().addAll(titulo, tabla);

        return contenedor;
    }
    
    /////////////////////////////////////
    /**
    * Crea una fila individual dentro del bloque de categoría.
    * La descripción puede limpiarse quitando la palabra clave de la categoría.
    */
    private Node crearFilaCategoria(
            Existencia existencia,
            Categoria categoria,
            boolean ultimaFila
    ) {
        GridPane fila = new GridPane();

        fila.setHgap(2);
        fila.setVgap(0);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setMaxWidth(Double.MAX_VALUE);
        fila.setMinHeight(25);
        fila.setPrefHeight(25);
        fila.setPadding(new Insets(0, 3, 0, 3));

        fila.setStyle(
                "-fx-border-color: #c0c0c0;" +
                "-fx-border-width: 1 1 0 1;" +
                "-fx-background-color: #f4f4f4;"
        );

        if (ultimaFila) {
            fila.setStyle(
                    "-fx-border-color: #c0c0c0;" +
                    "-fx-border-width: 1;" +
                    "-fx-background-color: #f4f4f4;"
            );
        }

        Label lblDescripcion = new Label(
                limpiarDescripcionParaCategoria(existencia, categoria)
        );

        lblDescripcion.setWrapText(false);
        lblDescripcion.setMaxWidth(Double.MAX_VALUE);
        lblDescripcion.setMinWidth(0);

        Label lblExistencia = new Label(
                formatearExistencia(existencia.getExistenciaOrZero())
        );

        lblExistencia.setAlignment(Pos.CENTER_RIGHT);
        lblExistencia.setMinWidth(40);
        lblExistencia.setPrefWidth(40);
        lblExistencia.setMaxWidth(40);

        aplicarColorStock(
                lblDescripcion,
                lblExistencia,
                existencia,
                categoria
        );

        GridPane.setHgrow(lblDescripcion, Priority.ALWAYS);

        fila.add(lblDescripcion, 0, 0);
        fila.add(lblExistencia, 1, 0);

        return fila;
    }

    /**
     * Evalúa los niveles de stock de un artículo y aplica el semáforo de color correspondiente:
     * - Fondo rojo pastel (#ffb3b3) si la existencia es menor o igual al Stock Mínimo (Crítico).
     * - Fondo amarillo pastel (#fff3a3) si la existencia es menor o igual al Stock Deseado (Alerta preventiva).
     * - Sin color de fondo si el stock supera el nivel deseado o no tiene umbrales fijados.
     *
     * @param lblDescripcion Etiqueta visual de la descripción del insumo.
     * @param lblExistencia Etiqueta visual del valor numérico de la existencia.
     * @param existencia Objeto de dominio con el stock actual y umbrales.
     * @param categoria Categoría a la que pertenece el insumo.
     */
    private void aplicarColorStock(Label lblDescripcion, Label lblExistencia, Existencia existencia, Categoria categoria) {
       int existenciaActual = existencia.getExistenciaOrZero() != null
               ? existencia.getExistenciaOrZero().intValue()
               : 0;

       Integer stockMinimo = existencia.getStockMinimo();
       Integer stockDeseado = existencia.getStockDeseado();

       String estiloDescripcion =
               "-fx-font-size: 24px; " +
               "-fx-font-weight: 600; " +
               "-fx-text-fill: #222222; " +
               "-fx-padding: 0 0 0 0;";
       

       String estiloExistencia =
               "-fx-font-size: 23px; " +
               "-fx-font-weight: bold; " +
               "-fx-text-fill: #222222; " +
               "-fx-padding: 0 0 0 0;";

       if (stockMinimo == null || stockDeseado == null) {
           lblDescripcion.setStyle(estiloDescripcion);
           lblExistencia.setStyle(estiloExistencia);
           return;
       }

       if (existenciaActual <= stockMinimo) {
           lblDescripcion.setStyle(estiloDescripcion + "-fx-background-color: #ffb3b3;");
           lblExistencia.setStyle(estiloExistencia + "-fx-background-color: #ffb3b3;");
       } else if (existenciaActual <= stockDeseado) {
           lblDescripcion.setStyle(estiloDescripcion + "-fx-background-color: #fff3a3;");
           lblExistencia.setStyle(estiloExistencia + "-fx-background-color: #fff3a3;");
       } else {
           lblDescripcion.setStyle(estiloDescripcion);
           lblExistencia.setStyle(estiloExistencia);
       }
   }

   /**
    * Construye y muestra el reporte por categorías en el panel central.
    *
    * Estrategia:
    * - Respeta el orden original de las categorías.
    * - Distribuye en 4 columnas.
    * - Usa un máximo de celdas visibles por columna.
    * - Cuando un bloque ya no cabe, pasa a la siguiente columna.
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
           aplicarStockLocal(existencias);
           List<Existencia> pendientes = new ArrayList<>(existencias);

           HBox layoutColumnas = new HBox(6);
           layoutColumnas.setPadding(new Insets(3, 6, 3, 6));
           layoutColumnas.setAlignment(Pos.TOP_LEFT);
           layoutColumnas.setStyle("-fx-background-color: transparent;");

           VBox columna1 = crearColumnaReporte();
           VBox columna2 = crearColumnaReporte();
           VBox columna3 = crearColumnaReporte();
           VBox columna4 = crearColumnaReporte();
           VBox columna5 = crearColumnaReporte();

           List<VBox> columnas = List.of(columna1, columna2, columna3, columna4, columna5);

           int[] cargas = new int[]{0, 0, 0, 0};
           int maxCeldasPorColumna = 33;
           int indiceColumnaActual = 0;

           for (Categoria categoria : categorias) {
               List<Existencia> registrosCategoria = filtrarExistenciasPorCategoria(pendientes, categoria);

               if (registrosCategoria.isEmpty()) {
                   continue;
               }

               VBox bloque = crearBloqueCategoria(categoria, registrosCategoria);
               int celdasBloque = calcularCeldasBloque(registrosCategoria.size());

               if (indiceColumnaActual < columnas.size() - 1
                        && cargas[indiceColumnaActual] > 0
                        && cargas[indiceColumnaActual] + celdasBloque > maxCeldasPorColumna) {

                    indiceColumnaActual++;
                }

               columnas.get(indiceColumnaActual).getChildren().add(bloque);
               cargas[indiceColumnaActual] += celdasBloque;

               pendientes.removeAll(registrosCategoria);
           }

           if (columna1.getChildren().isEmpty()
                   && columna2.getChildren().isEmpty()
                   && columna3.getChildren().isEmpty()
                   && columna4.getChildren().isEmpty()
                   && columna5.getChildren().isEmpty()) {

               Label lbl = new Label("No hay existencias para mostrar en la sucursal seleccionada.");
               lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #30505b;");
               setContenidoCentral(lbl);
               return;
           }

           layoutColumnas.getChildren().addAll(columna1, columna2, columna3, columna4, columna5);

           HBox.setHgrow(columna1, Priority.ALWAYS);
           HBox.setHgrow(columna2, Priority.ALWAYS);
           HBox.setHgrow(columna3, Priority.ALWAYS);
           HBox.setHgrow(columna4, Priority.ALWAYS);
           HBox.setHgrow(columna5, Priority.ALWAYS);

           ScrollPane scrollPane = crearContenedorScrollable(layoutColumnas);
           setContenidoCentral(scrollPane);

       } catch (Exception ex) {
           mostrarError("Reporte por categorías", "No se pudo cargar el reporte: " + ex.getMessage());
       }
   }
   
   /**
    * Calcula cuántas celdas visuales ocupa un bloque.
    *
    * Regla:
    * - 1 celda para el título.
    * - 1 celda por cada fila de existencia.
    */
   private int calcularCeldasBloque(int cantidadFilas) {
       return 1 + cantidadFilas;
   }
   
   /**
    * Formatea la existencia numérica para presentarla en pantalla.
    */
   private String formatearExistencia(BigDecimal valor) {
       DecimalFormat decimalFormat = new DecimalFormat("#,##0.####");
       return decimalFormat.format(valor != null ? valor : BigDecimal.ZERO);
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
            aplicarStockLocal(registros);
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
//    private void mostrarErroresSincronizacionNoBloqueante(List<String> errores) {
//        String detalle = (errores == null || errores.isEmpty())
//                ? "Se detectaron errores de sincronización."
//                : String.join("\n", errores);
//
//        if (alertErroresSync == null) {
//            alertErroresSync = new Alert(Alert.AlertType.ERROR);
//            alertErroresSync.setTitle("Errores de sincronización");
//            alertErroresSync.setHeaderText("Se encontraron errores al sincronizar sucursales.");
//            alertErroresSync.setContentText("La ventana se cerrará automáticamente en 30 segundos.");
//
//            TextArea textArea = new TextArea();
//            textArea.setEditable(false);
//            textArea.setWrapText(true);
//            textArea.setMaxWidth(Double.MAX_VALUE);
//            textArea.setMaxHeight(Double.MAX_VALUE);
//
//            GridPane.setVgrow(textArea, Priority.ALWAYS);
//            GridPane.setHgrow(textArea, Priority.ALWAYS);
//
//            GridPane content = new GridPane();
//            content.setMaxWidth(Double.MAX_VALUE);
//            content.add(textArea, 0, 0);
//
//            alertErroresSync.getDialogPane().setExpandableContent(content);
//            alertErroresSync.getDialogPane().setExpanded(true);
//        }
//
//        TextArea textArea = (TextArea) ((GridPane) alertErroresSync.getDialogPane().getExpandableContent())
//                .getChildren().get(0);
//
//        textArea.setText(detalle);
//
//        if (autoCloseErroresSync == null) {
//            autoCloseErroresSync = new PauseTransition(Duration.seconds(30));
//            autoCloseErroresSync.setOnFinished(event -> {
//                if (alertErroresSync != null) {
//                    alertErroresSync.hide();
//                }
//            });
//        }
//
//        autoCloseErroresSync.stop();
//
//        if (!alertErroresSync.isShowing()) {
//            alertErroresSync.show();
//        }
//
//        autoCloseErroresSync.playFromStart();
//    }

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
    
    /**
     * Construye el reporte completo DTO de todas las sucursales y categorías activas,
     * serializa la estructura a JSON en el disco local y dispara el envío HTTP hacia la API.
     */
    private void probarGeneracionJsonReal() {
        try {
            ReporteCategoriasDataService dataService = new ReporteCategoriasDataService(
                    sucursalRepository,
                    categoriaRepository,
                    existenciaRepository,
                    existenciaStockRepository
            );

            ReporteCategoriasJsonDto reporte = dataService.construirReporteCompleto();
            Path archivoJson = obtenerRutaJsonReporteCategorias();

            ReporteCategoriasJsonService jsonService = new ReporteCategoriasJsonService();
            jsonService.guardarJsonDto(reporte, archivoJson.toString());

            System.out.println("JSON real generado correctamente en: " + archivoJson.toAbsolutePath());
            probarEnvioJsonALaravelLocal();

        } catch (Exception e) {
            System.out.println("No se genero el JSON");
            e.printStackTrace();
        }
    }

    /**
     * Transmite el archivo JSON de existencias hacia la API web externa (ej. Laravel).
     * Lee la URL y el Bearer Token configurados en la base de datos local.
     */
    private void probarEnvioJsonALaravelLocal() {
        try {
            String apiUrl = configuracionService.getValorConfiguracion("ApiUrlReporteCategorias");
            String apiToken = configuracionService.getValorConfiguracion("ApiTokenReporteCategorias");

            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                //mostrarError("Configuración", "No se ha configurado la URL de la API.");
                return;
            }

            if (apiToken == null || apiToken.trim().isEmpty()) {
                //mostrarError("Configuración", "No se ha configurado el token de la API.");
                return;
            }

            Path rutaJson = obtenerRutaJsonReporteCategorias();

            ReporteCategoriasApiClient apiClient = new ReporteCategoriasApiClient();

            String respuesta = apiClient.enviarJson(
                    apiUrl,
                    apiToken,
                    rutaJson.toString()
            );

            System.out.println("Respuesta Laravel: " + respuesta);
        } catch (Exception e) {
            e.printStackTrace();
            //mostrarError("Error API", "No fue posible enviar el JSON: " + e.getMessage());
        }
    }

    /**
     * Resuelve la ruta del sistema de archivos donde se almacenará el reporte JSON.
     * En Windows utiliza {@code %APPDATA%/Dalmendra/reportes/reporte_categorias.json},
     * o el directorio del usuario en otros sistemas operativos.
     *
     * @return Path absoluto del archivo JSON.
     * @throws Exception Si ocurre un error al crear los directorios.
     */
    private Path obtenerRutaJsonReporteCategorias() throws Exception {
        String appData = System.getenv("APPDATA");
        Path carpetaBase;

        if (appData != null && !appData.isBlank()) {
            carpetaBase = Paths.get(appData, "Dalmendra", "reportes");
        } else {
            carpetaBase = Paths.get(System.getProperty("user.home"), "Dalmendra", "reportes");
        }

        Files.createDirectories(carpetaBase);
        return carpetaBase.resolve("reporte_categorias.json");
    }

    /**
     * Configura oyentes de eventos en una ventana modal (Stage) para pausar la
     * sincronización en segundo plano mientras el modal está abierto y reactivarla
     * al cerrarse.
     *
     * @param stage Ventana modal que suspenderá temporalmente la sincronización.
     */
    private void configurarPausaSincronizacionEnVentana(Stage stage) {
        stage.setOnShown(event -> appState.setBanActualizacion(false));
        stage.setOnHidden(event -> appState.setBanActualizacion(true));
    }

    /**
     * Asocia los umbrales de stock mínimo y deseado (almacenados en la tabla {@code existencias_stock})
     * a cada objeto {@link Existencia} de la lista proporcionada.
     *
     * @param existencias Lista de existencias de la sucursal activa.
     */
    private void aplicarStockLocal(List<Existencia> existencias) {
        if (existencias == null || existencias.isEmpty()) {
            return;
        }

        Long sucursalId = existencias.get(0).getSucursalId();
        if (sucursalId == null) {
            return;
        }

        try {
            List<ExistenciaStock> stocks = existenciaStockRepository.findBySucursalId(sucursalId);
            java.util.Map<String, ExistenciaStock> stockPorCodigo = new java.util.HashMap<>();

            for (ExistenciaStock stock : stocks) {
                if (stock.getCodigo() != null) {
                    stockPorCodigo.put(stock.getCodigo().trim(), stock);
                }
            }

            for (Existencia existencia : existencias) {
                if (existencia.getCodigo() == null) {
                    existencia.setStockMinimo(null);
                    existencia.setStockDeseado(null);
                    continue;
                }

                ExistenciaStock stockLocal = stockPorCodigo.get(existencia.getCodigo().trim());

                if (stockLocal != null) {
                    existencia.setStockMinimo(stockLocal.getStockMinimo());
                    existencia.setStockDeseado(stockLocal.getStockDeseado());
                } else {
                    existencia.setStockMinimo(null);
                    existencia.setStockDeseado(null);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}