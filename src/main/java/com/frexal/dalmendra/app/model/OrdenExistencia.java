package com.frexal.dalmendra.app.model;

/**
 * Entidad que almacena la prioridad u orden secuencial asignado a un artículo
 * dentro de una sucursal para ordenar las filas en pantalla.
 */
public class OrdenExistencia {

    /** Identificador único autoincrementable. */
    private Long id;

    /** ID de la sucursal donde aplica el orden. */
    private Long sucursalId;

    /** Código del insumo/artículo. */
    private String codigo;

    /** Número de posición u orden ascendente. */
    private Integer orden;

    public OrdenExistencia() {
    }

    public OrdenExistencia(Long sucursalId, String codigo, Integer orden) {
        this.sucursalId = sucursalId;
        this.codigo = codigo;
        this.orden = orden;
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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    @Override
    public String toString() {
        return "OrdenExistencia{id=" + id
                + ", sucursalId=" + sucursalId
                + ", codigo='" + codigo + '\''
                + ", orden=" + orden
                + '}';
    }
}