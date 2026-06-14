module com.frexal.dalmendra.DalmendraApplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.frexal.dalmendra.app to javafx.fxml;
    opens com.frexal.dalmendra.app.model to javafx.base;
    opens com.frexal.dalmendra.app.ui.main to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.config to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.categorias to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.sucursales to javafx.fxml;
    opens com.frexal.dalmendra.app.ui.existencias to javafx.fxml;
    
    exports com.frexal.dalmendra.app;
    exports com.frexal.dalmendra.app.model;
    exports com.frexal.dalmendra.app.service;
    exports com.frexal.dalmendra.app.repository;
    exports com.frexal.dalmendra.app.config;
    exports com.frexal.dalmendra.app.ui.main;
}
