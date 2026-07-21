package com.frexal.dalmendra.app;

import javafx.application.Application;
import com.frexal.dalmendra.app.config.MySqlSchemaInitializer;
import javafx.fxml.FXMLLoader;
import static javafx.application.Application.launch;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class DalmendraApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        MySqlSchemaInitializer initializer = new MySqlSchemaInitializer();
        initializer.initialize();

        FXMLLoader loader = new FXMLLoader(
                DalmendraApplication.class.getResource("/com/frexal/dalmendra/app/ui/main/MainView.fxml")
        );

        Scene scene = new Scene(loader.load(), 1200, 750);

        stage.setTitle("Dalmendra");
        stage.getIcons().add(new Image(
                DalmendraApplication.class.getResourceAsStream("/icons/logo.png")
        ));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        stage.setOnCloseRequest(event -> {
            // 1. Termina el ciclo de vida de JavaFX ordenadamente
            javafx.application.Platform.exit();

            // 2. Fuerza el cierre de la JVM y mata cualquier hilo rebelde
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}