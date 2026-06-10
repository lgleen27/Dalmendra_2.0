package com.frexal.dalmendra.app.config;

public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "dalmendra";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";

    public static final String JDBC_PARAMETERS =
            "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=America/Mexico_City";

    public static final String JDBC_URL_SERVER =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + JDBC_PARAMETERS;

    public static final String JDBC_URL_DATABASE =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + JDBC_PARAMETERS;
}