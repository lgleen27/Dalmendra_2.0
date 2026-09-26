package com.frexal.dalmendra.app.ui.config;

import com.frexal.dalmendra.app.service.AppState;
import com.frexal.dalmendra.app.service.ConfiguracionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador de la vista modal de Configuración del Sistema ({@code ConfiguracionView.fxml}).
 *
 * Permite gestionar:
 * - Tipo de reporte inicial al arrancar la aplicación.
 * - Intervalo en minutos para la sincronización periódica de inventarios.
 * - Tiempo de rotación entre sucursales.
 * - Parámetros de conexión a la API REST externa (URL y Token Bearer).
 */
public class ConfiguracionController {

    @FXML
    private ComboBox<String> cmbReporte;

    @FXML
    private Spinner<Integer> spnSync;

    @FXML
    private Spinner<Integer> spnCambio;

    @FXML
    private TextField txtApiUrl;

    @FXML
    private PasswordField txtApiToken;

    @FXML
    private CheckBox chkEditarApi;

    private ConfiguracionService configuracionService;
    private AppState appState;
    private Runnable onConfiguracionGuardada;

    public void initData(ConfiguracionService configuracionService,
                         AppState appState,
                         Runnable onConfiguracionGuardada) {
        this.configuracionService = configuracionService;
        this.appState = appState;
        this.onConfiguracionGuardada = onConfiguracionGuardada;
        cargarDatos();
        aplicarBloqueoApi();
    }

    @FXML
    private void initialize() {
        cmbReporte.setItems(FXCollections.observableArrayList(
                "PorCategorias",
                "ListadoConCodigo",
                "ListadoSinCodigo"
        ));

        spnSync.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 1));
        spnCambio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 15));

        spnSync.setEditable(true);
        spnCambio.setEditable(true);

        if (chkEditarApi != null) {
            chkEditarApi.setSelected(false);
        }

        aplicarBloqueoApi();
    }

    private void cargarDatos() {
        if (appState == null || configuracionService == null) {
            return;
        }

        String firstReport = appState.getFirstReport();
        if (firstReport != null && !firstReport.trim().isEmpty()) {
            cmbReporte.getSelectionModel().select(firstReport);
        } else {
            cmbReporte.getSelectionModel().select("PorCategorias");
        }

        spnSync.getValueFactory().setValue(parseEntero(appState.getTimeSyncSucursal(), 1));
        spnCambio.getValueFactory().setValue(parseEntero(appState.getTimeChangeSucursal(), 15));

        try {
            String apiUrl = configuracionService.getValorConfiguracion("ApiUrlReporteCategorias");
            String apiToken = configuracionService.getValorConfiguracion("ApiTokenReporteCategorias");

            txtApiUrl.setText(apiUrl == null ? "" : apiUrl);
            txtApiToken.setText(apiToken == null ? "" : apiToken);
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cargar la configuración de la API: " + ex.getMessage());
        }
    }

    @FXML
    private void onToggleEditarApi() {
        aplicarBloqueoApi();
    }

    private void aplicarBloqueoApi() {
        boolean habilitar = chkEditarApi != null && chkEditarApi.isSelected();

        if (txtApiUrl != null) {
            txtApiUrl.setDisable(!habilitar);
            txtApiUrl.setEditable(habilitar);
        }

        if (txtApiToken != null) {
            txtApiToken.setDisable(!habilitar);
            txtApiToken.setEditable(habilitar);
        }
    }

    @FXML
    private void onGuardar() {
        try {
            String reporte = cmbReporte.getSelectionModel().getSelectedItem();

            if (reporte == null || reporte.trim().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Debes seleccionar un reporte inicial.");
                return;
            }

            configuracionService.actualizarConfiguracion("FirstReport", reporte);
            configuracionService.actualizarConfiguracion("TimeSyncSucursal", String.valueOf(spnSync.getValue()));
            configuracionService.actualizarConfiguracion("TimeChangeSucursal", String.valueOf(spnCambio.getValue()));

            if (chkEditarApi != null && chkEditarApi.isSelected()) {
                String apiUrl = txtApiUrl.getText() != null ? txtApiUrl.getText().trim() : "";
                String apiToken = txtApiToken.getText() != null ? txtApiToken.getText().trim() : "";

                if (apiUrl.isEmpty()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Debes capturar la URL de la API.");
                    return;
                }

                if (apiToken.isEmpty()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Debes capturar el token de la API.");
                    return;
                }

                configuracionService.actualizarConfiguracion("ApiUrlReporteCategorias", apiUrl);
                configuracionService.actualizarConfiguracion("ApiTokenReporteCategorias", apiToken);
            }

            if (onConfiguracionGuardada != null) {
                onConfiguracionGuardada.run();
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "Configuración", "La configuración se guardó correctamente.");
            cerrarVentana();

        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar la configuración: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) cmbReporte.getScene().getWindow();
        stage.close();
    }

    private int parseEntero(String valor, int porDefecto) {
        try {
            return Integer.parseInt(valor);
        } catch (Exception ex) {
            return porDefecto;
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}