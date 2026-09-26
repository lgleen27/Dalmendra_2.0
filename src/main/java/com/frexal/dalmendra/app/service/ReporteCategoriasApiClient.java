package com.frexal.dalmendra.app.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Cliente HTTP REST basado en el {@link HttpClient} nativo de Java.
 * Se encarga de transmitir el archivo JSON de reporte generado hacia una API web
 * externa (ej. backend en Laravel) utilizando autenticación mediante Bearer Token.
 */
public class ReporteCategoriasApiClient {

    private final HttpClient httpClient;

    public ReporteCategoriasApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Lee el archivo JSON generado localmente y lo envía vía HTTP POST al endpoint especificado.
     *
     * @param url Endpoint REST de destino.
     * @param token Bearer Token de autorización.
     * @param rutaArchivoJson Ruta del archivo JSON a enviar.
     * @return Cuerpo de la respuesta del servidor web.
     * @throws IOException Si ocurre un error de lectura o la respuesta HTTP no es 2xx.
     * @throws InterruptedException Si la operación de red es interrumpida.
     */
    public String enviarJson(String url, String token, String rutaArchivoJson) throws IOException, InterruptedException {
        String contenidoJson = Files.readString(Path.of(rutaArchivoJson), StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(contenidoJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Error HTTP " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }
}