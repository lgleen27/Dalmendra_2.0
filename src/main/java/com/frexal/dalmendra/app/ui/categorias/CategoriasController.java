package com.frexal.dalmendra.app.ui.categorias;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.service.CategoriaService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

/**
 * Controlador de la vista modal de administración de Categorías ({@code CategoriasView.fxml}).
 *
 * Permite:
 * - Operaciones CRUD completas sobre las categorías (crear, editar, eliminar, activar/inactivar).
 * - Configurar la {@code palabraClave} para la clasificación dinámica de existencias.
 * - Reordenar visualmente las categorías (mover arriba, abajo, inicio y fin) para definir
 *   el orden en que aparecerán las columnas en el tablero principal.
 */
public class CategoriasController {

    @FXML private Label lblId;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPalabraClave;
    @FXML private CheckBox ckbEstado;

    @FXML private TableView<Categoria> tblCategorias;
    @FXML private TableColumn<Categoria, Long> colId;
    @FXML private TableColumn<Categoria, String> colDescripcion;
    @FXML private TableColumn<Categoria, String> colPalabraClave;
    @FXML private TableColumn<Categoria, String> colEstado;

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnGuardar;
    @FXML private Button btnEliminar;
    @FXML private Button btnCancelar;
    @FXML private Button btnOrdenar;
    @FXML private Button btnInicio;
    @FXML private Button btnSubir;
    @FXML private Button btnBajar;
    @FXML private Button btnFinal;

    private final CategoriaService categoriaService =
            new CategoriaService(new CategoriaRepository());

    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();

    private String status = "0";

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        inhabilitarCampos();
        cargarCategorias();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        colDescripcion.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDescripcion()));
        colPalabraClave.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPalabraClave()));
        colEstado.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                Boolean.TRUE.equals(data.getValue().getEstado()) ? "Activo" : "Inactivo"
        ));

        tblCategorias.setItems(categorias);
        tblCategorias.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void configurarEventos() {
        tblCategorias.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                return;
            }

            if ("0".equals(status)) {
                cargarFormulario(actual);
                btnEditar.setDisable(false);
                btnEliminar.setDisable(false);
                btnCancelar.setDisable(false);
            } else if ("3".equals(status)) {
                validaFuncionesOrden();
            }
        });
    }

    private void cargarCategorias() {
        try {
            List<Categoria> lista = categoriaService.findAll();
            lista.sort(Comparator.comparing(c -> c.getOrden() == null ? 0 : c.getOrden()));
            categorias.setAll(lista);

            if (!categorias.isEmpty()) {
                tblCategorias.getSelectionModel().selectFirst();
            }
        } catch (SQLException ex) {
            mostrarError("Error", "No se pudieron cargar las categorías: " + ex.getMessage());
        }
    }

    private void cargarFormulario(Categoria categoria) {
        lblId.setText(categoria.getId() == null ? "" : String.valueOf(categoria.getId()));
        txtDescripcion.setText(categoria.getDescripcion() == null ? "" : categoria.getDescripcion());
        txtPalabraClave.setText(categoria.getPalabraClave() == null ? "" : categoria.getPalabraClave());
        ckbEstado.setSelected(Boolean.TRUE.equals(categoria.getEstado()));
        
    }

    private void habilitarCampos() {
        txtDescripcion.setDisable(false);
        txtPalabraClave.setDisable(false);
        ckbEstado.setDisable(false);

        btnNuevo.setDisable(true);
        btnEditar.setDisable(true);
        btnGuardar.setDisable(false);
        btnEliminar.setDisable(true);
        btnCancelar.setDisable(false);
    }

    private void inhabilitarCampos() {
        txtDescripcion.setDisable(true);
        txtPalabraClave.setDisable(true);
        ckbEstado.setDisable(true);

        btnNuevo.setDisable(false);
        btnEditar.setDisable(true);
        btnGuardar.setDisable(true);
        btnEliminar.setDisable(true);
        btnCancelar.setDisable(true);

        btnInicio.setDisable(true);
        btnFinal.setDisable(true);
        btnSubir.setDisable(true);
        btnBajar.setDisable(true);
        btnOrdenar.setDisable(false);
    }

    private void limpiarCampos() {
        lblId.setText("");
        txtDescripcion.clear();
        txtPalabraClave.clear();
        ckbEstado.setSelected(true);
        status = "0";
    }

    private Categoria construirCategoriaDesdeFormulario() {
        Categoria categoria = new Categoria();

        if (!lblId.getText().trim().isEmpty()) {
            categoria.setId(Long.parseLong(lblId.getText().trim()));
        }

        categoria.setDescripcion(txtDescripcion.getText().trim());
        categoria.setPalabraClave(txtPalabraClave.getText().trim());
        categoria.setEstado(ckbEstado.isSelected());

        if (categoria.getId() == null) {
            int siguienteOrden = categorias.size() + 1;
            categoria.setOrden(siguienteOrden);
        } else {
            Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();
            categoria.setOrden(seleccionada != null ? seleccionada.getOrden() : 0);
        }

        return categoria;
    }

    @FXML
    private void onNuevo() {
        habilitarCampos();
        limpiarCampos();
        status = "1";
    }

    @FXML
    private void onEditar() {
        if (tblCategorias.getSelectionModel().getSelectedItem() == null) {
            mostrarInformacion("Categorías", "Debes seleccionar una categoría.");
            return;
        }
        habilitarCampos();
        status = "2";
    }

    @FXML
    private void onGuardar() {
        try {
            Categoria categoria = construirCategoriaDesdeFormulario();
            categoriaService.save(categoria);
            mostrarInformacion("Categorías", "La categoría se guardó correctamente.");
            cargarCategorias();
            inhabilitarCampos();
            limpiarCampos();
        } catch (IllegalArgumentException ex) {
            mostrarError("Validación", ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo guardar la categoría: " + ex.getMessage());
        }
    }

    @FXML
    private void onEliminar() {
        Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null || seleccionada.getId() == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Categorías");
        alert.setHeaderText(null);
        alert.setContentText("¿Deseas eliminar la categoría \"" + seleccionada.getDescripcion() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categoriaService.deleteById(seleccionada.getId());
                mostrarInformacion("Categorías", "La categoría fue eliminada correctamente.");
                cargarCategorias();
                inhabilitarCampos();
                limpiarCampos();
            } catch (Exception ex) {
                mostrarError("Error", "No se pudo eliminar la categoría: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onCancelar() {
        inhabilitarCampos();
        limpiarCampos();
        if (!categorias.isEmpty()) {
            tblCategorias.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onCerrar() {
        Stage stage = (Stage) tblCategorias.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onOrdenar() {
        if (categorias.size() <= 1) {
            mostrarInformacion("Categorías", "Debe existir más de un registro para ordenar.");
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

        status = "3";
        validaFuncionesOrden();
    }

    private void validaFuncionesOrden() {
        int index = tblCategorias.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }

        btnInicio.setDisable(index == 0);
        btnSubir.setDisable(index == 0);
        btnFinal.setDisable(index == categorias.size() - 1);
        btnBajar.setDisable(index == categorias.size() - 1);
    }

    @FXML
    private void onInicio() {
        int index = tblCategorias.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            return;
        }

        Categoria seleccionada = categorias.remove(index);
        categorias.add(0, seleccionada);
        persistirOrden();
        tblCategorias.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSubir() {
        int index = tblCategorias.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            return;
        }

        Categoria actual = categorias.get(index);
        categorias.set(index, categorias.get(index - 1));
        categorias.set(index - 1, actual);

        persistirOrden();
        tblCategorias.getSelectionModel().select(index - 1);
    }

    @FXML
    private void onBajar() {
        int index = tblCategorias.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= categorias.size() - 1) {
            return;
        }

        Categoria actual = categorias.get(index);
        categorias.set(index, categorias.get(index + 1));
        categorias.set(index + 1, actual);

        persistirOrden();
        tblCategorias.getSelectionModel().select(index + 1);
    }

    @FXML
    private void onFinal() {
        int index = tblCategorias.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= categorias.size() - 1) {
            return;
        }

        Categoria seleccionada = categorias.remove(index);
        categorias.add(seleccionada);
        persistirOrden();
        tblCategorias.getSelectionModel().selectLast();
    }

    private void persistirOrden() {
        try {
            List<Categoria> copia = new ArrayList<>(categorias);
            for (int i = 0; i < copia.size(); i++) {
                Categoria categoria = copia.get(i);
                int nuevoOrden = i + 1;
                categoria.setOrden(nuevoOrden);
                categoriaService.updateOrden(categoria.getId(), nuevoOrden);
            }
            tblCategorias.refresh();
            validaFuncionesOrden();
        } catch (Exception ex) {
            mostrarError("Error", "No se pudo actualizar el orden: " + ex.getMessage());
            cargarCategorias();
        }
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