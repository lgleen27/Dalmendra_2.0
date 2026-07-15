package com.frexal.dalmendra.app.dto.reporte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteCategoriasJsonDto {

    private LocalDateTime fechaGeneracion;
    private String origen;
    private Integer totalSucursales;
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