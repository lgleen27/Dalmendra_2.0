/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.model;

/**
 * Entidad que almacena pares clave-valor de configuración global del sistema
 * (por ejemplo tiempos de sincronización, reporte inicial, URLs de API).
 */
public class Configuracion {

    /** Identificador único del parámetro. */
    private Long id;

    /** Clave o nombre identificador de la configuración (único). */
    private String descripcion;

    /** Valor asignado al parámetro. */
    private String valor;

    public Configuracion() {
    }

    public Configuracion(String descripcion, String valor) {
        this.descripcion = descripcion;
        this.valor = valor;
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

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}