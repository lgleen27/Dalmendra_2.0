package com.frexal.dalmendra.app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Existencia {

    private Long id;
    private Long sucursalId;
    private Long categoriaId;
    private String codigo;
    private String descripcion;
    private BigDecimal existencia;
    private Integer stockMinimo;
    private Integer stockDeseado;
    private Integer orden;
    private LocalDateTime fechaActualizacion;

    public Existencia() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
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

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public BigDecimal getExistenciaOrZero() {
        return existencia != null ? existencia : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return (codigo == null ? "" : codigo) + " - " + (descripcion == null ? "" : descripcion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Existencia)) return false;
        Existencia that = (Existencia) o;
        return id != null && that.id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}