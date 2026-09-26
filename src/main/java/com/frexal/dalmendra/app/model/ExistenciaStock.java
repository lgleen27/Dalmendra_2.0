package com.frexal.dalmendra.app.model;

import java.time.LocalDateTime;

/**
 * Entidad que persiste la configuración de límites de stock (mínimo y deseado)
 * fijada localmente para un insumo específico en una sucursal determinada.
 */
public class ExistenciaStock {

    /** Identificador único autoincrementable. */
    private Long id;

    /** ID de la sucursal donde aplican estos límites. */
    private Long sucursalId;

    /** Código del insumo/artículo. */
    private String codigo;

    /** Nivel de stock mínimo permitido antes de marcar en estado crítico. */
    private Integer stockMinimo;

    /** Nivel de stock deseado u óptimo antes de marcar en advertencia preventiva. */
    private Integer stockDeseado;

    /** Fecha y hora de la última modificación de los umbrales. */
    private LocalDateTime fechaActualizacion;

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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}