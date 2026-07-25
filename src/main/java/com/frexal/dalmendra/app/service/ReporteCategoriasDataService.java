package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.dto.reporte.CategoriaReporteJsonDto;
import com.frexal.dalmendra.app.dto.reporte.ProductoReporteJsonDto;
import com.frexal.dalmendra.app.dto.reporte.ReporteCategoriasJsonDto;
import com.frexal.dalmendra.app.dto.reporte.SucursalReporteJsonDto;
import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.ExistenciaStock;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;
import com.frexal.dalmendra.app.repository.ExistenciaStockRepository;
import com.frexal.dalmendra.app.repository.SucursalRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteCategoriasDataService {

    private final SucursalRepository sucursalRepository;
    private final CategoriaRepository categoriaRepository;
    private final ExistenciaRepository existenciaRepository;
    private final ExistenciaStockRepository existenciaStockRepository;

    public ReporteCategoriasDataService(SucursalRepository sucursalRepository,
                                        CategoriaRepository categoriaRepository,
                                        ExistenciaRepository existenciaRepository,
                                        ExistenciaStockRepository existenciaStockRepository) {
        this.sucursalRepository = sucursalRepository;
        this.categoriaRepository = categoriaRepository;
        this.existenciaRepository = existenciaRepository;
        this.existenciaStockRepository = existenciaStockRepository;
    }

    public ReporteCategoriasJsonDto construirReporteCompleto() throws SQLException {
        ReporteCategoriasJsonDto reporte = new ReporteCategoriasJsonDto();
        reporte.setFechaGeneracion(LocalDateTime.now());
        reporte.setOrigen("reporte_categorias");

        List<Sucursal> sucursalesActivas = obtenerSucursalesActivas();
        List<SucursalReporteJsonDto> sucursalesJson = new ArrayList<>();

        for (Sucursal sucursal : sucursalesActivas) {
            SucursalReporteJsonDto sucursalDto = construirSucursal(sucursal);
            sucursalesJson.add(sucursalDto);
        }

        reporte.setSucursales(sucursalesJson);
        reporte.setTotalSucursales(sucursalesJson.size());

        return reporte;
    }

    private SucursalReporteJsonDto construirSucursal(Sucursal sucursal) throws SQLException {
        SucursalReporteJsonDto dto = new SucursalReporteJsonDto();
        dto.setSucursalId(sucursal.getId());
        dto.setSucursalNombre(sucursal.getNombreSucursal());
        dto.setActiva(sucursal.getActiva());
        dto.setUltimaSincronizacionCorrecta(LocalDateTime.now());

        List<Categoria> categoriasActivas = obtenerCategoriasActivas();
        List<Existencia> existenciasSucursal = obtenerExistenciasPorSucursal(sucursal.getId());
        aplicarStockLocal(existenciasSucursal, sucursal.getId());

        List<CategoriaReporteJsonDto> categoriasJson = new ArrayList<>();
        int totalProductos = 0;

        for (Categoria categoria : categoriasActivas) {
            List<ProductoReporteJsonDto> productos = filtrarProductosPorCategoria(categoria, existenciasSucursal);

            if (productos.isEmpty()) {
                continue;
            }

            CategoriaReporteJsonDto categoriaDto = new CategoriaReporteJsonDto();
            categoriaDto.setCategoriaId(categoria.getId());
            categoriaDto.setNombre(categoria.getDescripcion());
            categoriaDto.setPalabraClave(categoria.getPalabraClave());
            categoriaDto.setProductos(productos);
            categoriaDto.setTotalProductos(productos.size());

            categoriasJson.add(categoriaDto);
            totalProductos += productos.size();
        }

        dto.setCategorias(categoriasJson);
        dto.setTotalCategorias(categoriasJson.size());
        dto.setTotalProductos(totalProductos);

        return dto;
    }

    private void aplicarStockLocal(List<Existencia> existencias, Long sucursalId) throws SQLException {
        if (existencias == null || existencias.isEmpty() || sucursalId == null) {
            return;
        }

        List<ExistenciaStock> stocks = existenciaStockRepository.findBySucursalId(sucursalId);
        Map<String, ExistenciaStock> stockPorCodigo = new HashMap<>();

        for (ExistenciaStock stock : stocks) {
            if (stock.getCodigo() != null) {
                stockPorCodigo.put(stock.getCodigo().trim(), stock);
            }
        }

        for (Existencia existencia : existencias) {
            if (existencia.getCodigo() == null) {
                continue;
            }

            ExistenciaStock stockLocal = stockPorCodigo.get(existencia.getCodigo().trim());

            if (stockLocal != null) {
                existencia.setStockMinimo(stockLocal.getStockMinimo());
                existencia.setStockDeseado(stockLocal.getStockDeseado());
            } else {
                existencia.setStockMinimo(null);
                existencia.setStockDeseado(null);
            }
        }
    }

    private List<ProductoReporteJsonDto> filtrarProductosPorCategoria(Categoria categoria,
                                                                      List<Existencia> existenciasSucursal) {
        List<ProductoReporteJsonDto> productos = new ArrayList<>();

        for (Existencia existencia : existenciasSucursal) {
            if (!coincideCategoria(categoria, existencia)) {
                continue;
            }

            ProductoReporteJsonDto producto = new ProductoReporteJsonDto();
            producto.setExistenciaId(existencia.getId());
            producto.setCodigo(existencia.getCodigo());
            producto.setDescripcion(existencia.getDescripcion());
            producto.setExistencia(existencia.getExistencia());
            producto.setStockMinimo(existencia.getStockMinimo());
            producto.setStockDeseado(existencia.getStockDeseado());

            String estadoStock = calcularEstadoStock(
                    existencia.getExistencia(),
                    existencia.getStockMinimo(),
                    existencia.getStockDeseado()
            );

            producto.setEstadoStock(estadoStock);
            productos.add(producto);
        }

        return productos;
    }

    private boolean coincideCategoria(Categoria categoria, Existencia existencia) {
        if (categoria == null || existencia == null) {
            return false;
        }

        String palabraClave = normalizarTexto(categoria.getPalabraClave());
        String descripcion = normalizarTexto(existencia.getDescripcion());

        if (palabraClave.isEmpty() || descripcion.isEmpty()) {
            return false;
        }

        return descripcion.startsWith(palabraClave);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ");
    }

    private String calcularEstadoStock(BigDecimal existencia, Integer stockMinimo, Integer stockDeseado) {
        if (stockMinimo == null || stockDeseado == null) {
            return "SIN_CONFIGURAR";
        }

        BigDecimal valorExistencia = existencia != null ? existencia : BigDecimal.ZERO;
        BigDecimal minimo = BigDecimal.valueOf(stockMinimo);
        BigDecimal deseado = BigDecimal.valueOf(stockDeseado);

        if (valorExistencia.compareTo(minimo) <= 0) {
            return "MINIMO_CRITICO";
        }

        if (valorExistencia.compareTo(deseado) <= 0) {
            return "BAJO_DESEADO";
        }

        return "NORMAL";
    }

    private List<Sucursal> obtenerSucursalesActivas() throws SQLException {
        return sucursalRepository.findAll()
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getActiva()))
                .collect(Collectors.toList());
    }

    private List<Categoria> obtenerCategoriasActivas() throws SQLException {
        return categoriaRepository.findAll()
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getEstado()))
                .sorted((a, b) -> Integer.compare(
                        a.getOrden() != null ? a.getOrden() : Integer.MAX_VALUE,
                        b.getOrden() != null ? b.getOrden() : Integer.MAX_VALUE
                ))
                .collect(Collectors.toList());
    }

    private List<Existencia> obtenerExistenciasPorSucursal(Long sucursalId) throws SQLException {
        return existenciaRepository.findBySucursalId(sucursalId);
    }
}