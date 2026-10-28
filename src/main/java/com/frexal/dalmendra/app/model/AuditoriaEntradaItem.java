package com.frexal.dalmendra.app.model;

import java.math.BigDecimal;

/**
 * Representa un renglón detallado de entrada en el kardex de Soft Restaurant
 * para propósitos de auditoría y conciliación física.
 */
public class AuditoriaEntradaItem {

    private String hora;
    private String codigo;
    private String descripcion;
    private BigDecimal cantidad;
    private Integer idAlmacen;
    private String nombreAlmacen;
    private boolean esAlmacenProduccion;
    private String origen;
    private Long idCompra;

    public AuditoriaEntradaItem() {
    }

    public AuditoriaEntradaItem(String hora, String codigo, String descripcion, BigDecimal cantidad,
                                Integer idAlmacen, String nombreAlmacen, boolean esAlmacenProduccion,
                                String origen, Long idCompra) {
        this.hora = hora;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.idAlmacen = idAlmacen;
        this.nombreAlmacen = nombreAlmacen;
        this.esAlmacenProduccion = esAlmacenProduccion;
        this.origen = origen;
        this.idCompra = idCompra;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
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

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getIdAlmacen() {
        return idAlmacen;
    }

    public void setIdAlmacen(Integer idAlmacen) {
        this.idAlmacen = idAlmacen;
    }

    public String getNombreAlmacen() {
        return nombreAlmacen;
    }

    public void setNombreAlmacen(String nombreAlmacen) {
        this.nombreAlmacen = nombreAlmacen;
    }

    public boolean isEsAlmacenProduccion() {
        return esAlmacenProduccion;
    }

    public void setEsAlmacenProduccion(boolean esAlmacenProduccion) {
        this.esAlmacenProduccion = esAlmacenProduccion;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Long getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Long idCompra) {
        this.idCompra = idCompra;
    }
}
