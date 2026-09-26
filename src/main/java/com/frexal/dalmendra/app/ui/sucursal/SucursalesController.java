package com.frexal.dalmendra.app.ui.sucursales;

import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.service.AppState;
import com.frexal.dalmendra.app.service.SqlServerSucursalClient;
import com.frexal.dalmendra.app.service.SucursalService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de la vista modal de administración de Sucursales ({@code SucursalesView.fxml}).
 *
 * Funcionalidades:
 * - Altas, bajas y modificaciones de sucursales físicas.
 * - Captura y edición de credenciales de conexión remota a SQL Server.
 * - Asignación de color distintivo por sucursal mediante {@link javafx.scene.control.ColorPicker}.
 * - Prueba de conectividad directa en tiempo real mediante socket y JDBC.
 * - Reordenamiento de las sucursales para el ComboBox del tablero principal.
 */
public class SucursalesController {

    @FXML private Label lblId;
    @FXML private Label lblColorValue;

    @FXML private TextField txtNombreSucursal;
    @FXML private TextField txtServidor;
    @FXML private TextField txtDB;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;

    @FXML private CheckBox chkActiva;
    @FXML private ColorPicker colorPicker;
    @FXML private Rectangle rectColor;

    @FXML private TableView<Sucursal> tblSucursales;
    @FXML private TableColumn<Sucursal, Long> colId;
    @FXML private TableColumn<Sucursal, String> colNombreSucursal;
    @FXML private TableColumn<Sucursal, String> colServidor;
    @FXML private TableColumn<Sucursal, String> colDB;
    @FXML private TableColumn<Sucursal, String> colUsuario;
    @FXML private TableColumn<Sucursal, String> colFechaActualizacion;

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private Button btnCancelar;
    @FXML private Button btnTest;
    @FXML private Button btnOrdenar;
    @FXML private Button btnInicio;
    @FXML private Button btnSubir;
    @FXML private Button btnBajar;
    @FXML private Button btnFinal;
    @FXML private Button btnCerrar;
    @FXML private Button btnMysql;
    @FXML private Button btnColor;

    private SucursalService sucursalService;
    private AppState appState;

    private final ObservableList<Sucursal> sucursales = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String status = "0";
    private int conteo = 0;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        inhabilitarCampos();
        limpiarCampos();
    }

    public void initData(SucursalService sucursalService, AppState appState) {
        this.sucursalService = sucursalService;
        this.appState = appState;
        cargarSucursales();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colNombreSucursal.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                safe(data.getValue().getNombreSucursal())
        ));
        colServidor.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                safe(data.getValue().getDataSource())
        ));
        colDB.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                safe(data.getValue().getCatalog())
        ));
        colUsuario.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                safe(data.getValue().getUserId())
        ));
        colFechaActualizacion.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getFechaHoraActualizacion() != null
                        ? data.getValue().getFechaHoraActualizacion().format(formatter)
                        : ""
        ));

        tblSucursales.setItems(sucursales);
        tblSucursales.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void configurarEventos() {
        tblSucursales.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                return;
            }

            if ("0".equals(status)) {
                cargarFormulario(selected);
                btnEditar.setDisable(false);
                btnEliminar.setDisable(false);
                btnCancelar.setDisable(false);
                btnTest.setDisable(false);
            } else if ("3".equals(status)) {
                validaFuncionesOrden();
            }
        });
    }

    private void cargarSucursales() {
        try {
            List<Sucursal> lista = sucursalService.cargarSucursales();
            lista.sort(Comparator.comparing(s -> s.getOrden() == null ? 0 : s.getOrden()));
            sucursales.setAll(lista);
            conteo = sucursales.size();

            if (!sucursales.isEmpty()) {
                tblSucursales.getSelectionModel().selectFirst();
            }
        } catch (SQLException ex) {
            mostrarError("Error", "No se pudieron cargar las sucursales: " + ex.getMessage());
        }
    }

    private void cargarFormulario(Sucursal sucursal) {
        lblId.setText(sucursal.getId() == null ? "" : String.valueOf(sucursal.getId()));
        txtNombreSucursal.setText(safe(sucursal.getNombreSucursal()));
        txtServidor.setText(safe(sucursal.getDataSource()));
        txtDB.setText(safe(sucursal.getCatalog()));
        txtUsuario.setText(safe(sucursal.getUserId()));
        txtContrasena.setText("");
        chkActiva.setSelected(Boolean.TRUE.equals(sucursal.getActiva()));

        Color color = parseColor(sucursal.getColor());
        colorPicker.setValue(color);
        rectColor.setFill(color);
        lblColorValue.setText(toHex(color));
    }

    private void habilitarCampos() {
        txtNombreSucursal.setDisable(false);
        txtServidor.setDisable(false);
        txtDB.setDisable(false);
        txtUsuario.setDisable(false);
        txtContrasena.setDisable(false);
        chkActiva.setDisable(false);
        colorPicker.setDisable(false);

        btnNuevo.setDisable(true);
        btnEditar.setDisable(true);
        btnGuardar.setDisable(false);
        btnEliminar.setDisable(true);
        btnCancelar.setDisable(false);
        btnTest.setDisable(false);
        btnOrdenar.setDisable(true);
        btnInicio.setDisable(true);
        btnSubir.setDisable(true);
        btnBajar.setDisable(true);
        btnFinal.setDisable(true);
        btnColor.setDisable(false);
    }

    private void inhabilitarCampos() {
        txtNombreSucursal.setDisable(true);
        txtServidor.setDisable(true);
        txtDB.setDisable(true);
        txtUsuario.setDisable(true);
        txtContrasena.setDisable(true);
        chkActiva.setDisable(true);
        colorPicker.setDisable(true);

        btnNuevo.setDisable(false);
        btnEditar.setDisable(true);
        btnGuardar.setDisable(true);
        btnEliminar.setDisable(true);
        btnCancelar.setDisable(true);
        btnTest.setDisable(true);
        btnOrdenar.setDisable(false);
        btnInicio.setDisable(true);
        btnSubir.setDisable(true);
        btnBajar.setDisable(true);
        btnFinal.setDisable(true);
        btnColor.setDisable(true);

        rectColor.setFill(Color.WHITE);
        colorPicker.setValue(Color.WHITE);
        lblColorValue.setText(toHex(Color.WHITE));
    }

    private void limpiarCampos() {
        lblId.setText("");
        txtNombreSucursal.clear();
        txtServidor.clear();
        txtDB.clear();
        txtUsuario.clear();
        txtContrasena.clear();
        chkActiva.setSelected(true);
        colorPicker.setValue(Color.WHITE);
        rectColor.setFill(Color.WHITE);
        lblColorValue.setText(toHex(Color.WHITE));
        status = "0";
    }

    private boolean validarCampos() {
        if (isBlank(txtNombreSucursal.getText())) {
            return false;
        }
        if (isBlank(txtServidor.getText())) {
            return false;
        }
        if (isBlank(txtDB.getText())) {
            return false;
        }
        if (isBlank(txtUsuario.getText())) {
            return false;
        }
        if (isBlank(txtContrasena.getText()) && "1".equals(status)) {
            return false;
        }
        return true;
    }

    private Sucursal construirSucursalDesdeFormulario() {
        Sucursal sucursal = new Sucursal();

        if (!lblId.getText().trim().isEmpty()) {
            sucursal.setId(Long.parseLong(lblId.getText().trim()));
        }

        sucursal.setNombreSucursal(txtNombreSucursal.getText().trim());
        sucursal.setDataSource(txtServidor.getText().trim());
        sucursal.setCatalog(txtDB.getText().trim());
        sucursal.setUserId(txtUsuario.getText().trim());
        sucursal.setPassword(txtContrasena.getText().trim());
        sucursal.setActiva(chkActiva.isSelected());
        sucursal.setColor(lblColorValue.getText());

        if (sucursal.getId() == null) {
            sucursal.setOrden(conteo + 1);
        } else {
            Sucursal seleccionada = tblSucursales.getSelectionModel().getSelectedItem();
            sucursal.setOrden(seleccionada != null ? seleccionada.getOrden() : 0);
            sucursal.setFechaHoraActualizacion(seleccionada != null ? seleccionada.getFechaHoraActualizacion() : null);
        }

        return sucursal;
    }

    @FXML
    private void onNuevo() {
        limpiarCampos();
        habilitarCampos();
        status = "1";
    }

    @FXML
    private void onEditar() {
        if (tblSucursales.getSelectionModel().getSelectedItem() == null) {
            mostrarInformacion("Sucursales", "Debes seleccionar una sucursal.");
            return;
        }
        habilitarCampos();
        status = "2";
    }

    @FXML
    private void onGuardar() {
        if (!validarCampos()) {
            mostrarError("Validación", "Debes capturar nombre, servidor, base de datos, usuario y contraseña.");
            return;
        }

        try {
            Sucursal sucursal = construirSucursalDesdeFormulario();

            if ("2".equals(status) && isBlank(txtContrasena.getText())) {
                sucursalService.updateSinPassword(sucursal);
            } else {
                sucursalService.save(sucursal);
            }

            mostrarInformacion("Sucursales", "La sucursal se guardó correctamente.");
            cargarSucursales();
            inhabilitarCampos();
            limpiarCampos();
        } catch (IllegalArgumentException ex) {
            mostrarError("Validación", ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo guardar la sucursal: " + ex.getMessage());
        }
    }

    @FXML
    private void onEliminar() {
        Sucursal seleccionada = tblSucursales.getSelectionModel().getSelectedItem();

        if (seleccionada == null || seleccionada.getId() == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sucursales");
        alert.setHeaderText(null);
        alert.setContentText("¿Deseas eliminar la sucursal \"" + seleccionada.getNombreSucursal() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sucursalService.deleteById(seleccionada.getId());
                mostrarInformacion("Sucursales", "La sucursal fue eliminada correctamente.");
                cargarSucursales();
                inhabilitarCampos();
                limpiarCampos();
            } catch (Exception ex) {
                mostrarError("Error", "No se pudo eliminar la sucursal: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onCancelar() {
        inhabilitarCampos();
        limpiarCampos();

        if (!sucursales.isEmpty()) {
            tblSucursales.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onCerrar() {
        Stage stage = (Stage) tblSucursales.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onOrdenar() {
        if (sucursales.size() <= 1) {
            mostrarInformacion("Sucursales", "Debe existir más de un registro para ordenar.");
            return;
        }

        inhabilitarCampos();
        limpiarCampos();

        btnBajar.setDisable(false);
        btnInicio.setDisable(false);
        btnSubir.setDisable(false);
        btnFinal.setDisable(false);
        btnOrdenar.setDisable(true);
        btnNuevo.setDisable(true);
        btnCancelar.setDisable(false);
        btnGuardar.setDisable(true);

        status = "3";
        validaFuncionesOrden();
    }

    private void validaFuncionesOrden() {
        int index = tblSucursales.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }

        btnInicio.setDisable(index == 0);
        btnSubir.setDisable(index == 0);
        btnFinal.setDisable(index == sucursales.size() - 1);
        btnBajar.setDisable(index == sucursales.size() - 1);
    }

    @FXML
    private void onInicio() {
        int index = tblSucursales.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            return;
        }

        Sucursal seleccionada = sucursales.remove(index);
        sucursales.add(0, seleccionada);
        persistirOrden();
        tblSucursales.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSubir() {
        int index = tblSucursales.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            return;
        }

        Sucursal actual = sucursales.get(index);
        sucursales.set(index, sucursales.get(index - 1));
        sucursales.set(index - 1, actual);

        persistirOrden();
        tblSucursales.getSelectionModel().select(index - 1);
    }

    @FXML
    private void onBajar() {
        int index = tblSucursales.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= sucursales.size() - 1) {
            return;
        }

        Sucursal actual = sucursales.get(index);
        sucursales.set(index, sucursales.get(index + 1));
        sucursales.set(index + 1, actual);

        persistirOrden();
        tblSucursales.getSelectionModel().select(index + 1);
    }

    @FXML
    private void onFinal() {
        int index = tblSucursales.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= sucursales.size() - 1) {
            return;
        }

        Sucursal seleccionada = sucursales.remove(index);
        sucursales.add(seleccionada);
        persistirOrden();
        tblSucursales.getSelectionModel().selectLast();
    }

    private void persistirOrden() {
        try {
            List<Sucursal> copia = new ArrayList<>(sucursales);
            for (int i = 0; i < copia.size(); i++) {
                Sucursal sucursal = copia.get(i);
                int nuevoOrden = i + 1;
                sucursal.setOrden(nuevoOrden);
                sucursalService.updateOrden(sucursal.getId(), nuevoOrden);
            }
            tblSucursales.refresh();
            validaFuncionesOrden();
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo actualizar el orden: " + ex.getMessage());
            cargarSucursales();
        }
    }

    @FXML
    private void onTest() {
        Sucursal sucursal;

        try {
            if ("0".equals(status)) {
                sucursal = tblSucursales.getSelectionModel().getSelectedItem();

                if (sucursal == null) {
                    mostrarError("Sucursales", "No se encontró la sucursal seleccionada.");
                    return;
                }
            } else {
                if (isBlank(txtServidor.getText())
                        || isBlank(txtDB.getText())
                        || isBlank(txtUsuario.getText())
                        || isBlank(txtContrasena.getText())) {
                    mostrarError(
                            "Probar conexión",
                            "Debes capturar servidor, base de datos, usuario y contraseña para probar la conexión."
                    );
                    return;
                }

                sucursal = construirSucursalDesdeFormulario();
            }

            btnTest.setDisable(true);

            Sucursal sucursalFinal = sucursal;

            Thread thread = new Thread(() -> {
                try {
                    SqlServerSucursalClient client = new SqlServerSucursalClient();
                    client.validarConexionOrThrow(sucursalFinal);

                    Platform.runLater(() -> {
                        btnTest.setDisable(false);
                        mostrarInformacion(
                                "Probar conexión",
                                "Conexión SQL Server correcta."
                                        + "\nSucursal: " + safe(sucursalFinal.getNombreSucursal())
                                        + "\nServidor: " + safe(sucursalFinal.getDataSource())
                                        + "\nBase de datos: " + safe(sucursalFinal.getCatalog())
                                        + "\nUsuario: " + safe(sucursalFinal.getUserId())
                        );
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnTest.setDisable(false);
                        mostrarError(
                                "Probar conexión",
                                "No se pudo conectar a SQL Server."
                                        + "\nSucursal: " + safe(sucursalFinal.getNombreSucursal())
                                        + "\nServidor: " + safe(sucursalFinal.getDataSource())
                                        + "\nBase de datos: " + safe(sucursalFinal.getCatalog())
                                        + "\nDetalle: " + ex.getMessage()
                        );
                    });
                }
            });

            thread.setDaemon(true);
            thread.start();

        } catch (Exception ex) {
            btnTest.setDisable(false);
            mostrarError("Probar conexión", "No se pudo preparar la prueba: " + ex.getMessage());
        }
    }

    @FXML
    private void onMysqlTest() {
        boolean ok = probarConexionBasica("127.0.0.1");
        if (ok) {
            mostrarInformacion("MySQL Local", "Se detectó respuesta básica del puerto MySQL en localhost.");
        } else {
            mostrarError("MySQL Local", "No hubo respuesta de MySQL en localhost:3306.");
        }
    }

    @FXML
    private void onColorSelected() {
        Color color = colorPicker.getValue() == null ? Color.WHITE : colorPicker.getValue();
        rectColor.setFill(color);
        lblColorValue.setText(toHex(color));
    }

    @FXML
    private void onAbrirColorPicker() {
        if (colorPicker.isDisable()) {
            return;
        }

        Platform.runLater(colorPicker::show);
    }

    private String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private boolean probarConexionBasica(String host) {
        if (isBlank(host)) {
            return false;
        }

        String cleanedHost = host.trim();
        if (cleanedHost.contains(";")) {
            cleanedHost = cleanedHost.split(";")[0].trim();
        }
        if (cleanedHost.contains("\\")) {
            cleanedHost = cleanedHost.split("\\\\")[0].trim();
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(cleanedHost, 3306), 2000);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Color parseColor(String value) {
        try {
            if (isBlank(value)) {
                return Color.WHITE;
            }

            String color = value.trim();

            if (color.startsWith("#")) {
                return Color.web(color);
            }

            String[] parts = color.split(",");
            if (parts.length == 4) {
                int a = Integer.parseInt(parts[0].trim());
                int r = Integer.parseInt(parts[1].trim());
                int g = Integer.parseInt(parts[2].trim());
                int b = Integer.parseInt(parts[3].trim());
                return Color.rgb(r, g, b, a / 255.0);
            }
        } catch (Exception ignored) {
        }

        return Color.WHITE;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}