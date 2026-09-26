/**
 * Definición del módulo Java de Dalmendra.
 * Configura las dependencias requeridas (JavaFX, JDBC, Jackson, HttpClient)
 * y abre los paquetes necesarios por reflexión para FXMLLoader, TableView y ObjectMapper.
 */
module com.frexal.dalmendra.MainLauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;

    opens com.frexal.dalmendra.app to javafx.fxml;
    opens com.frexal.dalmendra.app.model to javafx.base;
    opens com.frexal.dalmendra.app.ui.main to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.config to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.categorias to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.sucursales to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.existencias to javafx.fxml;
    opens com.frexal.dalmendra.app.dto.reporte to com.fasterxml.jackson.databind;
    
    exports com.frexal.dalmendra.app;
    exports com.frexal.dalmendra.app.model;
    exports com.frexal.dalmendra.app.service;
    exports com.frexal.dalmendra.app.repository;
    exports com.frexal.dalmendra.app.config;
    exports com.frexal.dalmendra.app.ui.main;
    
}
