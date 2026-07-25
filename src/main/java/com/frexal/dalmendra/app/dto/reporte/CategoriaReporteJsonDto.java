package com.frexal.dalmendra.app.dto.reporte;

import java.util.ArrayList;
import java.util.List;

public class CategoriaReporteJsonDto {

    private Long categoriaId;
    private String nombre;
    private String palabraClave;
    private Integer totalProductos;
    private List<ProductoReporteJsonDto> productos = new ArrayList<>();

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPalabraClave() {
        return palabraClave;
    }

    public void setPalabraClave(String palabraClave) {
        this.palabraClave = palabraClave;
    }

    public Integer getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(Integer totalProductos) {
        this.totalProductos = totalProductos;
    }

    public List<ProductoReporteJsonDto> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoReporteJsonDto> productos) {
        this.productos = productos;
    }
}