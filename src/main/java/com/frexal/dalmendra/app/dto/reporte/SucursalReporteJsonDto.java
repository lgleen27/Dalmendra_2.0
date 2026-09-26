package com.frexal.dalmendra.app.dto.reporte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que representa una Sucursal dentro del reporte JSON, conteniendo
 * sus metadatos y la lista de categorías con sus respectivos productos.
 */
public class SucursalReporteJsonDto {

    /** ID de la sucursal. */
    private Long sucursalId;

    /** Nombre descriptivo de la sucursal. */
    private String sucursalNombre;

    /** Estado activo de la sucursal. */
    private Boolean activa;

    /** Timestamp de la última sincronización sin errores. */
    private LocalDateTime ultimaSincronizacionCorrecta;

    /** Total de categorías con productos encontradas. */
    private Integer totalCategorias;

    /** Total general de productos clasificados en la sucursal. */
    private Integer totalProductos;

    /** Lista de categorías con sus productos correspondientes. */
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