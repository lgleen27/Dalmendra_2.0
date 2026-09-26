package com.frexal.dalmendra.app.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa una Sucursal física y sus credenciales de conexión
 * remota hacia el servidor SQL Server donde reside su sistema POS.
 */
public class Sucursal {

    /** Identificador único autoincrementable. */
    private Long id;

    /** Nombre comercial o descriptivo de la sucursal. */
    private String nombreSucursal;

    /** Servidor SQL Server (puede ser IP, Hostname, o formato Host\\Instancia[:Puerto]). */
    private String dataSource;

    /** Nombre de la base de datos en SQL Server (Catálogo). */
    private String catalog;

    /** Usuario de conexión a SQL Server. */
    private String userId;

    /** Contraseña de conexión a SQL Server. */
    private String password;

    /** Orden relativo en el selector y listados. */
    private Integer orden;

    /** Fecha y hora en la que se completó con éxito la última sincronización. */
    private LocalDateTime fechaHoraActualizacion;

    /** Código hexadecimal del color identificativo de la sucursal (ej. #FF5733). */
    private String color;

    /** Indica si la sucursal está activa para sincronización y consulta. */
    private Boolean activa;

    public Sucursal() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreSucursal() {
        return nombreSucursal;
    }

    public void setNombreSucursal(String nombreSucursal) {
        this.nombreSucursal = nombreSucursal;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public LocalDateTime getFechaHoraActualizacion() {
        return fechaHoraActualizacion;
    }

    public void setFechaHoraActualizacion(LocalDateTime fechaHoraActualizacion) {
        this.fechaHoraActualizacion = fechaHoraActualizacion;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return nombreSucursal == null ? "" : nombreSucursal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sucursal)) return false;
        Sucursal sucursal = (Sucursal) o;
        return id != null && sucursal.id != null && Objects.equals(id, sucursal.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}