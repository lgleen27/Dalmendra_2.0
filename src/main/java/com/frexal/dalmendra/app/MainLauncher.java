package com.frexal.dalmendra.app;

/**
 * Lanzador estático de la aplicación.
 *
 * Esta clase no hereda de {@link javafx.application.Application}, lo cual es un patrón estándar
 * necesario para ejecutar aplicaciones JavaFX empaquetadas en un Fat-JAR (uber-jar) sin que
 * la JVM requiera los módulos JavaFX en el module-path en tiempo de arranque.
 */
public class MainLauncher {
    public static void main(String[] args) {
        DalmendraApplication.main(args);
    }
}