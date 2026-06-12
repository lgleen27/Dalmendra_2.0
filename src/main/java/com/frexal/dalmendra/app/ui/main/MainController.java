package com.frexal.dalmendra.app.ui.main;

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
import com.frexal.dalmendra.app.ui.sucursales.SucursalesController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

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

            if (!appState.getSucursales().isEmpty()) {
                actualizarExistencias();
            }

            if (sucursalService.haySucursalesActivas()) {
                abrirReporteInicial();
            } else {
                lblVistaActual.setText("No hay sucursales activas. Abra el catálogo de sucursales.");
                mostrarInformacion(
                        "Dalmendra",
                        "No existe ninguna conexión con las sucursales, revisa las conexiones existentes o genera una nueva."
                );
            }

            lblEstado.setText("Sistema listo");
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
                    return;
                }
            }
        }

        if (!cmbSucursales.getItems().isEmpty()) {
            cmbSucursales.getSelectionModel().selectFirst();
            appState.setSucursalSeleccionada(cmbSucursales.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    private void onSucursalSeleccionada() {
        Sucursal sucursal = cmbSucursales.getSelectionModel().getSelectedItem();
        appState.setSucursalSeleccionada(sucursal);
        aplicarColorSucursalActual();

        if (sucursal != null) {
            lblEstado.setText("Sucursal activa: " + sucursal.getNombreSucursal());
        } else {
            lblEstado.setText("Sin sucursal seleccionada");
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
        lblSync.setText("Sincronizando...");
        lblEstado.setText("Actualizando existencias...");

        Thread thread = new Thread(() -> {
            inventarioSyncService.sincronizarTodas();

            Platform.runLater(() -> {
                if (appState.isHayErrorSincronizacion()) {
                    lblSync.setText("Con errores");
                    mostrarError(
                            "Errores de sincronización",
                            String.join("\n", appState.getErroresSincronizacion())
                    );
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
        try {
            int minutos = Integer.parseInt(appState.getTimeSyncSucursal());

            if (minutos > 0) {
                syncSchedulerService.programarSincronizacion(
                        () -> Platform.runLater(this::actualizarExistencias),
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
            lblEstado.setText("Catálogo de sucursales cerrado");
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
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el catálogo de categorías: " + ex.getMessage());
        }
    }

    @FXML
    private void onArticulos() {
        lblVistaActual.setText("Vista: Orden de Articulos");
        lblEstado.setText("Abriendo artículos");
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
            ex.printStackTrace();
            mostrarError("Error", "No se pudo abrir la configuración: " + ex.getMessage());
        }
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