package com.frexal.dalmendra.app.ui.categorias;

import com.frexal.dalmendra.app.model.Categoria;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class StockCategoriaController {

    @FXML private Label lblCategoria;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtStockDeseado;

    private Categoria categoria;

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
        lblCategoria.setText("Categoría: " + categoria.getDescripcion());

        if (categoria.getStockMinimo() != null) {
            txtStockMinimo.setText(String.valueOf(categoria.getStockMinimo()));
        }

        if (categoria.getStockDeseado() != null) {
            txtStockDeseado.setText(String.valueOf(categoria.getStockDeseado()));
        }
    }

    @FXML
    private void onGuardar() {
        try {
            int stockMinimo = Integer.parseInt(txtStockMinimo.getText().trim());
            int stockDeseado = Integer.parseInt(txtStockDeseado.getText().trim());

            if (stockMinimo < 0 || stockDeseado < 0) {
                mostrarError("Los valores no pueden ser negativos.");
                return;
            }

            if (stockDeseado < stockMinimo) {
                mostrarError("El stock deseado no puede ser menor al stock mínimo.");
                return;
            }

            categoria.setStockMinimo(stockMinimo);
            categoria.setStockDeseado(stockDeseado);

            // Aquí llamas a tu service/repository para persistir
            cerrar();
        } catch (NumberFormatException ex) {
            mostrarError("Debes capturar números válidos.");
        }
    }

    @FXML
    private void onCancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtStockMinimo.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Stock");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}