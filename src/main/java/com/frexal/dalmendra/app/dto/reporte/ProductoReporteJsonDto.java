package com.frexal.dalmendra.app.dto.reporte;

import java.math.BigDecimal;

/**
 * DTO que representa un producto/insumo individual con sus existencias,
 * umbrales de stock y estado calculado de semaforización en el reporte JSON.
 */
public class ProductoReporteJsonDto {

    /** ID de la existencia en la base de datos local. */
    private Long existenciaId;

    /** Código del insumo en el sistema remoto. */
    private String codigo;

    /** Descripción del insumo. */
    private String descripcion;

    /** Cantidad neta en inventario. */
    private BigDecimal existencia;

    /** Umbral mínimo de stock. */
    private Integer stockMinimo;

    /** Umbral deseado de stock. */
    private Integer stockDeseado;

    /** Estado semaforizado: MINIMO_CRITICO, BAJO_DESEADO, NORMAL o SIN_CONFIGURAR. */
    private String estadoStock;

    public Long getExistenciaId() {
        return existenciaId;
    }

    public void setExistenciaId(Long existenciaId) {
        this.existenciaId = existenciaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getExistencia() {
        return existencia;
    }

    public void setExistencia(BigDecimal existencia) {
        this.existencia = existencia;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Integer getStockDeseado() {
        return stockDeseado;
    }

    public void setStockDeseado(Integer stockDeseado) {
        this.stockDeseado = stockDeseado;
    }

    public String getEstadoStock() {
        return estadoStock;
    }

    public void setEstadoStock(String estadoStock) {
        this.estadoStock = estadoStock;
    }
}