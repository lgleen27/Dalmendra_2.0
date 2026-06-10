package com.frexal.dalmendra.app.ui.main;

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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Label lblVistaActual;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblSync;

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

            definirTimerSync();

            if (!appState.getSucursales().isEmpty()) {
                actualizarExistencias();
            }

            if (sucursalService.haySucursalesActivas()) {
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales, revisa las conexiones existentes o genera una nueva.");
            }

            lblEstado.setText("Sistema listo");
        } catch (Exception ex) {
            lblEstado.setText("Error al iniciar");
            mostrarError("Error de inicio", ex.getMessage());
        }
    }

    private void abrirReporteInicial() {
        switch (appState.getFirstReport()) {
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
        lblSync.setText("Sincronizando...");
        lblEstado.setText("Actualizando existencias...");

        Thread thread = new Thread(() -> {
            inventarioSyncService.sincronizarTodas();

            Platform.runLater(() -> {
                if (appState.isHayErrorSincronizacion()) {
                    lblSync.setText("Con errores");
                    mostrarError("Errores de sincronización",
                            String.join("\n", appState.getErroresSincronizacion()));
                } else {
                    lblSync.setText("Correcta");
                }
                lblEstado.setText("Existencias actualizadas");
            });
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void definirTimerSync() {
        int minutos = Integer.parseInt(appState.getTimeSyncSucursal());

        if (minutos > 0) {
            syncSchedulerService.programarSincronizacion(() -> Platform.runLater(this::actualizarExistencias), minutos);
            lblSync.setText("Programada cada " + minutos + " min");
        } else {
            lblSync.setText("Desactivada");
        }
    }

    private void abrirVistaPorCategorias() {
        lblVistaActual.setText("Vista: Reporte por Categorias");
        lblEstado.setText("Mostrando reporte por categorias");
    }

    private void abrirVistaListadoConCodigo() {
        lblVistaActual.setText("Vista: Listado con Codigo");
        lblEstado.setText("Mostrando listado con código");
    }

    private void abrirVistaListadoSinCodigo() {
        lblVistaActual.setText("Vista: Listado sin Codigo");
        lblEstado.setText("Mostrando listado sin código");
    }

    @FXML
    private void onReportePorCategorias() {
        actualizarExistencias();
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaPorCategorias();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onListadoConCodigo() {
        actualizarExistencias();
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaListadoConCodigo();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onListadoSinCodigo() {
        actualizarExistencias();
        if (sucursalService.haySucursalesActivas()) {
            abrirVistaListadoSinCodigo();
        } else {
            mostrarInformacion("Dalmendra", "No existe ninguna conexión con las sucursales.");
        }
    }

    @FXML
    private void onSucursales() {
        lblVistaActual.setText("Vista: Catálogo de Sucursales");
        lblEstado.setText("Abriendo sucursales");
    }

    @FXML
    private void onCategorias() {
        lblVistaActual.setText("Vista: Catálogo de Categorias");
        lblEstado.setText("Abriendo categorias");
    }

    @FXML
    private void onArticulos() {
        lblVistaActual.setText("Vista: Orden de Articulos");
        lblEstado.setText("Abriendo artículos");
    }

    @FXML
    private void onConfiguracion() {
        lblVistaActual.setText("Vista: Configuración");
        lblEstado.setText("Abriendo configuración");
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
}