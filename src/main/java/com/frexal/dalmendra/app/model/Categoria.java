package com.frexal.dalmendra.app.model;

/**
 * Entidad que representa una Categoría de productos/insumos en Dalmendra.
 *
 * La categorización funciona mediante una {@code palabraClave}. Cualquier existencia
 * cuya descripción inicie con dicha palabra clave se asocia dinámicamente a esta categoría.
 */
public class Categoria {

    /** Identificador único autoincrementable. */
    private Long id;

    /** Nombre o descripción visible de la categoría. */
    private String descripcion;

    /** Prefijo o palabra clave con la que deben iniciar las descripciones de los artículos. */
    private String palabraClave;

    /** Posición u orden relativo en el que se mostrará la categoría en los reportes. */
    private Integer orden;

    /** Estado de la categoría: {@code true} si está activa, {@code false} si está inactiva. */
    private Boolean estado;

    public Categoria() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPalabraClave() {
        return palabraClave;
    }

    public void setPalabraClave(String palabraClave) {
        this.palabraClave = palabraClave;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}