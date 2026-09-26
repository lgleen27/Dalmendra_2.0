package com.frexal.dalmendra.app.dto.reporte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO raíz para la serialización del reporte global de existencias y categorías.
 * Representa la carga útil completa que se almacena en disco y se envía a la API REST.
 */
public class ReporteCategoriasJsonDto {

    /** Fecha y hora exacta de creación del reporte. */
    private LocalDateTime fechaGeneracion;

    /** Identificador del origen del reporte (ej. "reporte_categorias"). */
    private String origen;

    /** Cantidad total de sucursales activas incluidas. */
    private Integer totalSucursales;

    /** Lista detallada de sucursales con sus respectivas categorías y productos. */
    private List<SucursalReporteJsonDto> sucursales = new ArrayList<>();

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Integer getTotalSucursales() {
        return totalSucursales;
    }

    public void setTotalSucursales(Integer totalSucursales) {
        this.totalSucursales = totalSucursales;
    }

    public List<SucursalReporteJsonDto> getSucursales() {
        return sucursales;
    }

    public void setSucursales(List<SucursalReporteJsonDto> sucursales) {
        this.sucursales = sucursales;
    }
}