package com.frexal.dalmendra.app;

import com.frexal.dalmendra.app.config.MySqlSchemaInitializer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Clase principal de arranque de la aplicación Dalmendra.
 *
 * Su responsabilidad es:
 * 1. Iniciar JavaFX.
 * 2. Verificar que la base de datos MySQL local esté disponible.
 * 3. Inicializar el esquema de base de datos si es necesario.
 * 4. Cargar la vista principal del sistema.
 */
public class DalmendraApplication extends Application {

    /**
     * Método que JavaFX ejecuta al iniciar la aplicación.
     *
     * @param stage Ventana principal de la aplicación.
     */
    @Override
    public void start(Stage stage) {
        // ------------------------------------------------------------
        // 1. Validar conexión e inicialización de la base de datos
        // ------------------------------------------------------------
        // Aquí se intenta conectar al servidor MySQL local y crear
        // la base/tablas necesarias si todavía no existen.
        // Si algo falla en este punto, no se debe abrir el sistema.
        try {
            MySqlSchemaInitializer initializer = new MySqlSchemaInitializer();
            initializer.initialize();

        } catch (Exception e) {
            // --------------------------------------------------------
            // 2. Mostrar mensaje amigable si no se encuentra el servidor
            // --------------------------------------------------------
            // En lugar de dejar que la aplicación falle con un error
            // técnico poco claro, se muestra un mensaje entendible.
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de conexión");
            alert.setHeaderText(null);
            alert.setContentText("Servidor local no encontrado.");
            alert.showAndWait();

            // --------------------------------------------------------
            // 3. Cerrar completamente la aplicación
            // --------------------------------------------------------
            // Se termina JavaFX y luego la JVM para asegurar que el
            // programa no continúe ejecutándose en segundo plano.
            Platform.exit();
            System.exit(0);
            return;
        }

        // ------------------------------------------------------------
        // 4. Cargar interfaz principal del sistema
        // ------------------------------------------------------------
        // Si la base de datos fue validada correctamente,
        // entonces sí se carga la vista principal.
        try {
            FXMLLoader loader = new FXMLLoader(
                    DalmendraApplication.class.getResource("/com/frexal/dalmendra/app/ui/main/MainView.fxml")
            );

            Scene scene = new Scene(loader.load(), 1200, 750);

            // Título de la ventana
            stage.setTitle("Dalmendra");

            // Ícono principal de la aplicación
            stage.getIcons().add(new Image(
                    DalmendraApplication.class.getResourceAsStream("/icons/logo.png")
            ));

            // Asignar escena a la ventana principal
            stage.setScene(scene);

            // Abrir maximizada para aprovechar toda la pantalla
            stage.setMaximized(true);

            // Mostrar ventana
            stage.show();

            // --------------------------------------------------------
            // 5. Control de cierre de la aplicación
            // --------------------------------------------------------
            // Se fuerza el cierre total para evitar que queden hilos
            // activos, por ejemplo en tareas programadas o servicios.
            stage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception e) {
            // --------------------------------------------------------
            // 6. Manejo de error si falla la carga de la interfaz
            // --------------------------------------------------------
            // Este bloque cubre errores distintos al de la base
            // de datos, por ejemplo si falta un FXML o un recurso.
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de inicio");
            alert.setHeaderText(null);
            alert.setContentText("No fue posible iniciar la aplicación.");
            alert.showAndWait();

            Platform.exit();
            System.exit(0);
        }
    }

    /**
     * Punto de entrada principal del programa.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        launch(args);
    }
}