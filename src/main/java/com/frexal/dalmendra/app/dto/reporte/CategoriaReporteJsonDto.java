package com.frexal.dalmendra.app.dto.reporte;

import java.util.ArrayList;
import java.util.List;

public class CategoriaReporteJsonDto {

    private Long categoriaId;
    private String nombre;
    private String palabraClave;
    private Integer stockMinimo;
    private Integer stockDeseado;
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