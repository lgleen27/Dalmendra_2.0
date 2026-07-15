package com.frexal.dalmendra.app.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ReporteCategoriasJsonService {

    private final ObjectMapper objectMapper;

    public ReporteCategoriasJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void guardarJson(Map<String, Object> datos, String rutaArchivo) throws IOException {
        File archivo = new File(rutaArchivo);

        File carpeta = archivo.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(archivo, datos);
    }

    public void guardarJsonDto(Object datos, String rutaArchivo) throws IOException {
        File archivo = new File(rutaArchivo);

        File carpeta = archivo.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(archivo, datos);
    }
}