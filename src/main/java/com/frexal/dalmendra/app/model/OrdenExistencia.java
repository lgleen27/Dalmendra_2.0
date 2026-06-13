package com.frexal.dalmendra.app.model;

public class OrdenExistencia {

    private Long id;
    private Long sucursalId;
    private String codigo;
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