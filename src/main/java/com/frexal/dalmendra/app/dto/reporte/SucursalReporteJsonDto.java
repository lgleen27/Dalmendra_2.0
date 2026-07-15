package com.frexal.dalmendra.app.dto.reporte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SucursalReporteJsonDto {

    private Long sucursalId;
    private String sucursalNombre;
    private Boolean activa;
    private LocalDateTime ultimaSincronizacionCorrecta;
    private Integer totalCategorias;
    private Integer totalProductos;
    private List<CategoriaReporteJsonDto> categorias = new ArrayList<>();

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getSucursalNombre() {
        return sucursalNombre;
    }

    public void setSucursalNombre(String sucursalNombre) {
        this.sucursalNombre = sucursalNombre;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getUltimaSincronizacionCorrecta() {
        return ultimaSincronizacionCorrecta;
    }

    public void setUltimaSincronizacionCorrecta(LocalDateTime ultimaSincronizacionCorrecta) {
        this.ultimaSincronizacionCorrecta = ultimaSincronizacionCorrecta;
    }

    public Integer getTotalCategorias() {
        return totalCategorias;
    }

    public void setTotalCategorias(Integer totalCategorias) {
        this.totalCategorias = totalCategorias;
    }

    public Integer getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(Integer totalProductos) {
        this.totalProductos = totalProductos;
    }

    public List<CategoriaReporteJsonDto> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<CategoriaReporteJsonDto> categorias) {
        this.categorias = categorias;
    }
}