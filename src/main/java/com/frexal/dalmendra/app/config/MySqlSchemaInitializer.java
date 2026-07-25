package com.frexal.dalmendra.app.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlSchemaInitializer {

    public void initialize() throws SQLException {
        createDatabaseIfNotExists();

        try (Connection connection = MySqlConnectionFactory.getConnection();
             Statement statement = connection.createStatement()) {

            createTables(statement);
            updateSchemaIfNeeded(connection, statement);
            //seedConfiguracion(statement);
        }
    }

    private void createTables(Statement statement) throws SQLException {
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS sucursales ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "nombre_sucursal VARCHAR(200), "
            + "data_source VARCHAR(200) NOT NULL, "
            + "catalog VARCHAR(200) NOT NULL, "
            + "user_id VARCHAR(200) NOT NULL, "
            + "password VARCHAR(255) NOT NULL, "
            + "orden INT NOT NULL DEFAULT 0, "
            + "fecha_hora_actualizacion DATETIME NULL, "
            + "color VARCHAR(32) NULL, "
            + "activa TINYINT NOT NULL DEFAULT 1"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS categorias ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "descripcion VARCHAR(50), "
            + "palabra_clave VARCHAR(20), "
            + "orden INT NOT NULL DEFAULT 0, "
            + "estado TINYINT NOT NULL DEFAULT 1"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS existencias ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "sucursal_id BIGINT NOT NULL, "
            + "codigo VARCHAR(20), "
            + "descripcion VARCHAR(250), "
            + "categoria_id BIGINT NULL, "
            + "existencia DECIMAL(18,4), "
            + "orden INT NOT NULL DEFAULT 0, "
            + "fecha_actualizacion DATETIME NULL, "
            + "CONSTRAINT fk_existencias_sucursal "
            + "FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) "
            + "ON DELETE CASCADE, "
            + "CONSTRAINT fk_existencias_categoria "
            + "FOREIGN KEY (categoria_id) REFERENCES categorias(id)"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS existencias_stock ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "sucursal_id BIGINT NOT NULL, "
            + "codigo VARCHAR(20) NOT NULL, "
            + "stock_minimo INT NULL, "
            + "stock_deseado INT NULL, "
            + "fecha_actualizacion DATETIME NULL, "
            + "CONSTRAINT fk_existencias_stock_sucursal "
            + "FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) "
            + "ON DELETE CASCADE, "
            + "CONSTRAINT uk_existencias_stock_sucursal_codigo UNIQUE (sucursal_id, codigo)"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS orden_existencias ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "sucursal_id BIGINT NOT NULL, "
            + "codigo VARCHAR(20) NOT NULL, "
            + "orden INT NOT NULL DEFAULT 0, "
            + "CONSTRAINT fk_orden_sucursal "
            + "FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) "
            + "ON DELETE CASCADE, "
            + "CONSTRAINT uk_orden_sucursal_codigo UNIQUE (sucursal_id, codigo)"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS configuracion ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "descripcion VARCHAR(250) NOT NULL, "
            + "valor VARCHAR(250) NULL, "
            + "CONSTRAINT uk_configuracion_descripcion UNIQUE (descripcion)"
            + ")"
        );

        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS sync_log ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "sucursal_id BIGINT NULL, "
            + "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "tipo VARCHAR(50) NOT NULL, "
            + "mensaje TEXT NOT NULL"
            + ")"
        );
    }

    private void updateSchemaIfNeeded(Connection connection, Statement statement) throws SQLException {
        if (!columnExists(connection, "categorias", "stock_minimo")) {
            statement.executeUpdate("ALTER TABLE categorias ADD COLUMN stock_minimo INT NULL");
        }

        if (!columnExists(connection, "categorias", "stock_deseado")) {
            statement.executeUpdate("ALTER TABLE categorias ADD COLUMN stock_deseado INT NULL");
        }

        if (!columnExists(connection, "existencias", "categoria_id")) {
            statement.executeUpdate("ALTER TABLE existencias ADD COLUMN categoria_id BIGINT NULL AFTER descripcion");
        }

        if (!foreignKeyExists(connection, "existencias", "fk_existencias_categoria")) {
            statement.executeUpdate(
                "ALTER TABLE existencias "
                + "ADD CONSTRAINT fk_existencias_categoria "
                + "FOREIGN KEY (categoria_id) REFERENCES categorias(id)"
            );
        }

        //asegurarTablaExistenciasStock(connection, statement);
    }

    private void asegurarTablaExistenciasStock(Connection connection, Statement statement) throws SQLException {
        if (!tableExists(connection, "existencias_stock")) {
            statement.executeUpdate(
                "CREATE TABLE existencias_stock ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "sucursal_id BIGINT NOT NULL, "
                + "codigo VARCHAR(20) NOT NULL, "
                + "stock_minimo INT NULL, "
                + "stock_deseado INT NULL, "
                + "fecha_actualizacion DATETIME NULL, "
                + "CONSTRAINT fk_existencias_stock_sucursal "
                + "FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) "
                + "ON DELETE CASCADE, "
                + "CONSTRAINT uk_existencias_stock_sucursal_codigo UNIQUE (sucursal_id, codigo)"
                + ")"
            );
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.TABLES "
                   + "WHERE TABLE_SCHEMA = DATABASE() "
                   + "AND TABLE_NAME = '" + tableName + "'";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.COLUMNS "
                   + "WHERE TABLE_SCHEMA = DATABASE() "
                   + "AND TABLE_NAME = '" + tableName + "' "
                   + "AND COLUMN_NAME = '" + columnName + "'";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String constraintName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.TABLE_CONSTRAINTS "
                   + "WHERE TABLE_SCHEMA = DATABASE() "
                   + "AND TABLE_NAME = '" + tableName + "' "
                   + "AND CONSTRAINT_NAME = '" + constraintName + "' "
                   + "AND CONSTRAINT_TYPE = 'FOREIGN KEY'";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }

    private void createDatabaseIfNotExists() throws SQLException {
        try (Connection connection = MySqlConnectionFactory.getServerConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                "CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.DB_NAME
                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            );
        }
    }

//    private void seedConfiguracion(Statement statement) throws SQLException {
//        statement.executeUpdate(
//            "INSERT INTO configuracion (descripcion, valor) "
//            + "VALUES ('IdDbSelect', '1') "
//            + "ON DUPLICATE KEY UPDATE valor = valor"
//        );
//
//        statement.executeUpdate(
//            "INSERT INTO configuracion (descripcion, valor) "
//            + "VALUES ('TimeSyncSucursal', '1') "
//            + "ON DUPLICATE KEY UPDATE valor = valor"
//        );
//
//        statement.executeUpdate(
//            "INSERT INTO configuracion (descripcion, valor) "
//            + "VALUES ('TimeChangeSucursal', '15') "
//            + "ON DUPLICATE KEY UPDATE valor = valor"
//        );
//
//        statement.executeUpdate(
//            "INSERT INTO configuracion (descripcion, valor) "
//            + "VALUES ('FirstReport', 'PorCategorias') "
//            + "ON DUPLICATE KEY UPDATE valor = valor"
//        );
//    }
}