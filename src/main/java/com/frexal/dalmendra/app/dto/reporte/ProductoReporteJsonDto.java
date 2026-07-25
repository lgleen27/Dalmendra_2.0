package com.frexal.dalmendra.app.dto.reporte;

import java.math.BigDecimal;

public class ProductoReporteJsonDto {

    private Long existenciaId;
    private String codigo;
    private String descripcion;
    private BigDecimal existencia;
    private Integer stockMinimo;
    private Integer stockDeseado;
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