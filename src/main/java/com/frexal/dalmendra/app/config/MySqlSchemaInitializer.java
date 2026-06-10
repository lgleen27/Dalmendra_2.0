/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlSchemaInitializer {

    public void initialize() throws SQLException {
        createDatabaseIfNotExists();

        try (Connection connection = MySqlConnectionFactory.getConnection();
             Statement statement = connection.createStatement()) {

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
                + "existencia DECIMAL(18,4), "
                + "orden INT NOT NULL DEFAULT 0, "
                + "fecha_actualizacion DATETIME NULL, "
                + "CONSTRAINT fk_existencias_sucursal "
                + "FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) "
                + "ON DELETE CASCADE"
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

            seedConfiguracion(statement);
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

    private void seedConfiguracion(Statement statement) throws SQLException {
        statement.executeUpdate(
            "INSERT INTO configuracion (descripcion, valor) "
            + "VALUES ('IdDbSelect', '1') "
            + "ON DUPLICATE KEY UPDATE valor = valor"
        );

        statement.executeUpdate(
            "INSERT INTO configuracion (descripcion, valor) "
            + "VALUES ('TimeSyncSucursal', '1') "
            + "ON DUPLICATE KEY UPDATE valor = valor"
        );

        statement.executeUpdate(
            "INSERT INTO configuracion (descripcion, valor) "
            + "VALUES ('TimeChangeSucursal', '15') "
            + "ON DUPLICATE KEY UPDATE valor = valor"
        );

        statement.executeUpdate(
            "INSERT INTO configuracion (descripcion, valor) "
            + "VALUES ('FirstReport', 'PorCategorias') "
            + "ON DUPLICATE KEY UPDATE valor = valor"
        );
    }
}