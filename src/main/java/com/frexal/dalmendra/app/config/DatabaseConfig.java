package com.frexal.dalmendra.app.config;

/**
 * Configuración centralizada de los parámetros de conexión JDBC para
 * la base de datos local MySQL de la aplicación Dalmendra.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    /** Servidor de base de datos local. */
    public static final String DB_HOST = "localhost";

    /** Puerto estándar de MySQL. */
    public static final int DB_PORT = 3306;

    /** Nombre de la base de datos de la aplicación. */
    public static final String DB_NAME = "dalmendra";

    /** Usuario de conexión a MySQL. */
    public static final String DB_USER = "root";

    /** Contraseña de conexión a MySQL. */
    public static final String DB_PASSWORD = "";

    /** Parámetros JDBC adicionales para compatibilidad y zona horaria. */
    public static final String JDBC_PARAMETERS =
            "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=America/Mexico_City";

    /** URL JDBC para conectarse al servidor MySQL (sin especificar base de datos). */
    public static final String JDBC_URL_SERVER =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + JDBC_PARAMETERS;

    /** URL JDBC para conectarse directamente a la base de datos de Dalmendra. */
    public static final String JDBC_URL_DATABASE =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + JDBC_PARAMETERS;
}