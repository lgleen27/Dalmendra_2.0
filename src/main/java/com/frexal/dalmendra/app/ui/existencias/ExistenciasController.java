package com.frexal.dalmendra.app.ui.existencias;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;
import com.frexal.dalmendra.app.service.AppState;
import com.frexal.dalmendra.app.service.OrdenExistenciaService;
import com.frexal.dalmendra.app.service.SucursalService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExistenciasController {

    private static final Long CATEGORIA_TODAS_ID = -1L;

    @FXML
    private ComboBox<Sucursal> cmbSucursales;

    @FXML
    private ComboBox<Categoria> cmbCategorias;

    @FXML
    private Button btnConsultar;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnOrdenar;

    @FXML
    private Button btnInicio;

    @FXML
    private Button btnSubir;

    @FXML
    private Button btnBajar;

    @FXML
    private Button btnFinal;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCerrar;

    @FXML
    private Label lblEstado;

    @FXML
    private TableView<Existencia> tblExistencias;

    @FXML
    private TableColumn<Existencia, String> colCodigo;

    @FXML
    private TableColumn<Existencia, String> colDescripcion;

    @FXML
    private TableColumn<Existencia, BigDecimal> colExistencia;

    private final ExistenciaRepository existenciaRepository = new ExistenciaRepository();
    private final CategoriaRepository categoriaRepository = new CategoriaRepository();

    private SucursalService sucursalService;
    private OrdenExistenciaService ordenExistenciaService;
    private AppState appState;

    private final ObservableList<Existencia> existencias = FXCollections.observableArrayList();
    private boolean modoOrden = false;

    public void initData(SucursalService sucursalService,
                         OrdenExistenciaService ordenExistenciaService,
                         AppState appState) {
        this.sucursalService = sucursalService;
        this.ordenExistenciaService = ordenExistenciaService;
        this.appState = appState;

        cargarSucursales();
        cargarCategorias();
        limpiarPantalla();
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colExistencia.setCellValueFactory(new PropertyValueFactory<>("existencia"));

        configurarFormatoExistencia();
        tblExistencias.setItems(existencias);

        cmbSucursales.setConverter(new StringConverter<Sucursal>() {
            @Override
            public String toString(Sucursal sucursal) {
                return sucursal != null ? valor(sucursal.getNombreSucursal()) : "";
            }

            @Override
            public Sucursal fromString(String string) {
                return null;
            }
        });

        cmbCategorias.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria categoria) {
                return categoria != null ? valor(categoria.getDescripcion()) : "";
            }

            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });

        tblExistencias.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (modoOrden) {
                actualizarBotonesMovimiento();
            }
        });
    }

    private void configurarFormatoExistencia() {
        DecimalFormat formato = new DecimalFormat("#,##0.####");

        colExistencia.setCellFactory(col -> new TableCell<Existencia, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                BigDecimal valor = item != null ? item : BigDecimal.ZERO;
                setText(formato.format(valor));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void cargarSucursales() {
        cmbSucursales.getItems().clear();

        if (appState == null || appState.getSucursalesActivas() == null) {
            return;
        }

        cmbSucursales.getItems().addAll(appState.getSucursalesActivas());

        if (appState.getSucursalSeleccionada() != null) {
            cmbSucursales.getSelectionModel().select(appState.getSucursalSeleccionada());
        }

        if (cmbSucursales.getSelectionModel().getSelectedItem() == null && !cmbSucursales.getItems().isEmpty()) {
            cmbSucursales.getSelectionModel().selectFirst();
        }
    }

    private void cargarCategorias() {
        try {
            List<Categoria> categorias = categoriaRepository.findAll();
            List<Categoria> activas = new ArrayList<>();

            Categoria todas = new Categoria();
            todas.setId(CATEGORIA_TODAS_ID);
            todas.setDescripcion("Todas");
            todas.setOrden(-1);
            todas.setEstado(true);
            activas.add(todas);

            for (Categoria categoria : categorias) {
                if (categoria.getEstado() == null || categoria.getEstado()) {
                    activas.add(categoria);
                }
            }

            activas.sort((a, b) -> {
                if (CATEGORIA_TODAS_ID.equals(a.getId())) return -1;
                if (CATEGORIA_TODAS_ID.equals(b.getId())) return 1;
                return Comparator.comparing(Categoria::getOrden, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Categoria::getDescripcion, Comparator.nullsLast(String::compareToIgnoreCase))
                        .compare(a, b);
            });

            cmbCategorias.getItems().clear();
            cmbCategorias.getItems().addAll(activas);
            cmbCategorias.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            mostrarError("Existencias", "No se pudieron cargar las categorías: " + ex.getMessage());
        }
    }

    @FXML
    private void onConsultar() {
        try {
            Sucursal sucursal = cmbSucursales.getSelectionModel().getSelectedItem();
            Categoria categoria = cmbCategorias.getSelectionModel().getSelectedItem();

            if (sucursal == null) {
                mostrarInformacion("Existencias", "Debes seleccionar una sucursal.");
                return;
            }

            if (categoria == null) {
                mostrarInformacion("Existencias", "Debes seleccionar una categoría.");
                return;
            }

            List<Existencia> registros;

            if (esCategoriaTodas(categoria)) {
                registros = existenciaRepository.findBySucursalId(sucursal.getId());
            } else {
                registros = existenciaRepository.findBySucursalIdAndCategoriaId(
                        sucursal.getId(),
                        categoria.getId()
                );
            }

            existencias.setAll(registros);

            if (registros.isEmpty()) {
                inhabilitarModoOrden();
                lblEstado.setText("No existen registros para el filtro seleccionado.");
                return;
            }

            cmbSucursales.setDisable(true);
            cmbCategorias.setDisable(true);
            btnConsultar.setDisable(true);
            btnOrdenar.setDisable(registros.size() <= 1);
            btnGuardar.setDisable(true);

            lblEstado.setText("Registros encontrados: " + registros.size());
        } catch (Exception ex) {
            mostrarError("Existencias", "No se pudieron consultar las existencias: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        limpiarPantalla();
    }

    @FXML
    private void onOrdenar() {
        if (existencias.size() <= 1) {
            mostrarInformacion("Existencias", "Se necesitan al menos dos registros para ordenar.");
            return;
        }

        modoOrden = true;
        btnOrdenar.setDisable(true);
        btnGuardar.setDisable(false);

        if (!existencias.isEmpty()) {
            tblExistencias.getSelectionModel().selectFirst();
        }

        actualizarBotonesMovimiento();
        lblEstado.setText("Modo de orden activado.");
    }

    @FXML
    private void onInicio() {
        moverInicioFin(true);
    }

    @FXML
    private void onSubir() {
        moverSubirBajar(true);
    }

    @FXML
    private void onBajar() {
        moverSubirBajar(false);
    }

    @FXML
    private void onFinal() {
        moverInicioFin(false);
    }

    @FXML
    private void onGuardar() {
        try {
            Sucursal sucursal = cmbSucursales.getSelectionModel().getSelectedItem();

            if (sucursal == null) {
                mostrarInformacion("Existencias", "No hay sucursal seleccionada.");
                return;
            }

            reenumerarOrdenes();

            for (Existencia item : existencias) {
                existenciaRepository.updateOrden(item.getId(), item.getOrden());
                ordenExistenciaService.actualizarOrden(sucursal.getId(), item.getCodigo(), item.getOrden());
            }

            ordenExistenciaService.cargarOrdenes();

            modoOrden = false;
            btnGuardar.setDisable(true);
            btnOrdenar.setDisable(existencias.size() <= 1);
            actualizarBotonesMovimiento();
            lblEstado.setText("Orden guardado correctamente.");
        } catch (Exception ex) {
            mostrarError("Existencias", "No se pudo guardar el orden: " + ex.getMessage());
        }
    }

    @FXML
    private void onCerrar() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    private void moverSubirBajar(boolean subir) {
        int index = tblExistencias.getSelectionModel().getSelectedIndex();

        if (index < 0) {
            return;
        }

        int nuevoIndex = subir ? index - 1 : index + 1;

        if (nuevoIndex < 0 || nuevoIndex >= existencias.size()) {
            return;
        }

        Existencia actual = existencias.get(index);
        Existencia destino = existencias.get(nuevoIndex);

        existencias.set(index, destino);
        existencias.set(nuevoIndex, actual);

        reenumerarOrdenes();
        tblExistencias.getSelectionModel().select(nuevoIndex);
        tblExistencias.refresh();
        actualizarBotonesMovimiento();
    }

    private void moverInicioFin(boolean alInicio) {
        int index = tblExistencias.getSelectionModel().getSelectedIndex();

        if (index < 0) {
            return;
        }

        Existencia seleccionada = existencias.remove(index);

        if (alInicio) {
            existencias.add(0, seleccionada);
            tblExistencias.getSelectionModel().select(0);
        } else {
            existencias.add(seleccionada);
            tblExistencias.getSelectionModel().select(existencias.size() - 1);
        }

        reenumerarOrdenes();
        tblExistencias.refresh();
        actualizarBotonesMovimiento();
    }

    private void reenumerarOrdenes() {
        for (int i = 0; i < existencias.size(); i++) {
            existencias.get(i).setOrden(i + 1);
        }
        tblExistencias.refresh();
    }

    private void actualizarBotonesMovimiento() {
        if (!modoOrden) {
            btnInicio.setDisable(true);
            btnSubir.setDisable(true);
            btnBajar.setDisable(true);
            btnFinal.setDisable(true);
            return;
        }

        int index = tblExistencias.getSelectionModel().getSelectedIndex();
        int total = existencias.size();

        if (index < 0 || total == 0) {
            btnInicio.setDisable(true);
            btnSubir.setDisable(true);
            btnBajar.setDisable(true);
            btnFinal.setDisable(true);
            return;
        }

        btnInicio.setDisable(index == 0);
        btnSubir.setDisable(index == 0);
        btnBajar.setDisable(index == total - 1);
        btnFinal.setDisable(index == total - 1);
    }

    private void inhabilitarModoOrden() {
        modoOrden = false;
        btnOrdenar.setDisable(true);
        btnInicio.setDisable(true);
        btnSubir.setDisable(true);
        btnBajar.setDisable(true);
        btnFinal.setDisable(true);
        btnGuardar.setDisable(true);
    }

    private void limpiarPantalla() {
        existencias.clear();
        cmbSucursales.setDisable(false);
        cmbCategorias.setDisable(false);
        btnConsultar.setDisable(false);
        inhabilitarModoOrden();
        lblEstado.setText("Seleccione una sucursal y una categoría.");
    }

    private boolean esCategoriaTodas(Categoria categoria) {
        return categoria != null && CATEGORIA_TODAS_ID.equals(categoria.getId());
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
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