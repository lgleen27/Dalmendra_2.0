/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexiones JDBC para la base de datos local MySQL.
 * Proporciona métodos para obtener conexiones tanto al servidor genérico (para inicializar la base de datos)
 * como a la base de datos específica de la aplicación.
 */
public final class MySqlConnectionFactory {

    private MySqlConnectionFactory() {
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se encontró el driver JDBC de MySQL.", e);
        }
    }

    /**
     * Obtiene una conexión al servidor MySQL (sin seleccionar base de datos).
     * Utilizada principalmente durante el arranque para ejecutar scripts DDL de creación de la base.
     *
     * @return Conexión activa a nivel de servidor MySQL.
     * @throws SQLException Si ocurre un error de comunicación o credenciales.
     */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.JDBC_URL_SERVER,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
        );
    }

    /**
     * Obtiene una conexión a la base de datos 'dalmendra'.
     *
     * @return Conexión activa a la base de datos local.
     * @throws SQLException Si ocurre un error al conectar.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.JDBC_URL_DATABASE,
                DatabaseConfig.DB_USER,
                DatabaseConfig.DB_PASSWORD
        );
    }
}