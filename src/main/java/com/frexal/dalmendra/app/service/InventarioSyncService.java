package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.CategoriaRepository;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InventarioSyncService {

    private final SqlServerSucursalClient sqlServerSucursalClient;
    private final ExistenciaRepository existenciaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SucursalService sucursalService;
    private final OrdenExistenciaService ordenExistenciaService;
    private final AppState appState;

    public InventarioSyncService(SqlServerSucursalClient sqlServerSucursalClient,
                                 ExistenciaRepository existenciaRepository,
                                 CategoriaRepository categoriaRepository,
                                 SucursalService sucursalService,
                                 OrdenExistenciaService ordenExistenciaService,
                                 AppState appState) {
        this.sqlServerSucursalClient = sqlServerSucursalClient;
        this.existenciaRepository = existenciaRepository;
        this.categoriaRepository = categoriaRepository;
        this.sucursalService = sucursalService;
        this.ordenExistenciaService = ordenExistenciaService;
        this.appState = appState;
    }

    public void sincronizarTodas() {
        appState.clearErroresSincronizacion();

        for (Sucursal sucursal : appState.getSucursales()) {
            if (sucursal == null) {
                continue;
            }

            if (Boolean.FALSE.equals(sucursal.getActiva())) {
                continue;
            }

            if (sucursal.getPassword() == null || sucursal.getPassword().isBlank()) {
                appState.addErrorSincronizacion(
                        "Sucursal " + safe(sucursal.getNombreSucursal()) + ": no tiene contraseña configurada."
                );
                continue;
            }

            try {
                sqlServerSucursalClient.validarConexionOrThrow(sucursal);
                sincronizarSucursal(sucursal);
            } catch (Exception ex) {
                appState.addErrorSincronizacion(
                        "Sucursal " + safe(sucursal.getNombreSucursal()) + ": " + construirMensajeError(ex)
                );
            }
        }
    }

    public void sincronizarSucursal(Sucursal sucursal) throws SQLException {
        if (sucursal == null || sucursal.getId() == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria.");
        }

        List<Categoria> categoriasActivas = cargarCategoriasActivas();

        List<SqlServerSucursalClient.InventarioRemotoRow> inventario =
                sqlServerSucursalClient.consultarInventario(sucursal);

        List<SqlServerSucursalClient.VentaRemotaRow> ventas =
                sqlServerSucursalClient.consultarVentas(sucursal);

        Map<String, ExistenciaTemporal> mapa = new LinkedHashMap<>();

        for (SqlServerSucursalClient.InventarioRemotoRow row : inventario) {
            if (isBlank(row.getCodigo())) {
                continue;
            }

            String codigo = row.getCodigo().trim();

            mapa.put(codigo, new ExistenciaTemporal(
                    codigo,
                    safe(row.getDescripcion()),
                    row.getExistencia() == null ? BigDecimal.ZERO : row.getExistencia()
            ));
        }

        for (SqlServerSucursalClient.VentaRemotaRow venta : ventas) {
            if (isBlank(venta.getCodigo())) {
                continue;
            }

            String codigo = venta.getCodigo().trim();
            BigDecimal vendida = venta.getExistenciaVendida() == null
                    ? BigDecimal.ZERO
                    : venta.getExistenciaVendida();

            ExistenciaTemporal actual = mapa.get(codigo);

            if (actual != null) {
                actual.existencia = actual.existencia.subtract(vendida);

                if (isBlank(actual.descripcion) && !isBlank(venta.getDescripcion())) {
                    actual.descripcion = venta.getDescripcion().trim();
                }
            } else {
                mapa.put(codigo, new ExistenciaTemporal(
                        codigo,
                        safe(venta.getDescripcion()),
                        vendida.negate()
                ));
            }
        }

        existenciaRepository.deleteBySucursalId(sucursal.getId());

        LocalDateTime ahora = LocalDateTime.now();

        for (ExistenciaTemporal item : mapa.values()) {
            if (isBlank(item.codigo)) {
                continue;
            }

            Existencia existencia = new Existencia();
            existencia.setSucursalId(sucursal.getId());
            existencia.setCategoriaId(resolverCategoriaId(item.descripcion, categoriasActivas));
            existencia.setCodigo(item.codigo);
            existencia.setDescripcion(safe(item.descripcion));
            existencia.setExistencia(item.existencia == null ? BigDecimal.ZERO : item.existencia);
            existencia.setOrden(ordenExistenciaService.obtenerOrden(sucursal.getId(), item.codigo));
            existencia.setFechaActualizacion(ahora);

            existenciaRepository.insert(existencia);
        }

        sucursalService.actualizarFechaActualizacion(sucursal.getId(), ahora);
    }

    private List<Categoria> cargarCategoriasActivas() throws SQLException {
        List<Categoria> categorias = categoriaRepository.findAll();
        List<Categoria> activas = new ArrayList<>();

        for (Categoria categoria : categorias) {
            if (categoria == null) {
                continue;
            }
            if (Boolean.FALSE.equals(categoria.getEstado())) {
                continue;
            }
            if (categoria.getId() == null) {
                continue;
            }

            activas.add(categoria);
        }

        activas.sort(
                Comparator.comparing(Categoria::getOrden, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(c -> normalizarTexto(c.getDescripcion()))
        );

        return activas;
    }

    private Long resolverCategoriaId(String descripcion, List<Categoria> categoriasActivas) {
        if (categoriasActivas == null || categoriasActivas.isEmpty()) {
            return null;
        }

        String texto = normalizarTexto(descripcion);
        if (texto.isEmpty()) {
            return null;
        }

        Categoria mejorCoincidencia = null;
        int mejorLongitudPalabra = -1;

        for (Categoria categoria : categoriasActivas) {
            String palabraClave = normalizarTexto(categoria.getPalabraClave());

            if (palabraClave.isEmpty()) {
                continue;
            }

            if (texto.contains(palabraClave) && palabraClave.length() > mejorLongitudPalabra) {
                mejorCoincidencia = categoria;
                mejorLongitudPalabra = palabraClave.length();
            }
        }

        return mejorCoincidencia != null ? mejorCoincidencia.getId() : null;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private String construirMensajeError(Exception ex) {
        if (ex == null) {
            return "Error desconocido.";
        }

        if (ex instanceof SQLException) {
            return construirMensajeSql((SQLException) ex);
        }

        Throwable root = obtenerCausaRaiz(ex);
        String mensaje = root.getMessage();

        if (mensaje == null || mensaje.trim().isEmpty()) {
            mensaje = ex.getMessage();
        }
        if (mensaje == null || mensaje.trim().isEmpty()) {
            mensaje = root.getClass().getSimpleName();
        }

        return mensaje;
    }

    private String construirMensajeSql(SQLException ex) {
        StringBuilder sb = new StringBuilder();
        SQLException actual = ex;
        boolean primero = true;

        while (actual != null) {
            if (!primero) {
                sb.append(" | ");
            }

            String mensaje = actual.getMessage();
            if (mensaje == null || mensaje.trim().isEmpty()) {
                mensaje = "Error SQL sin detalle";
            }

            sb.append(mensaje);

            if (actual.getSQLState() != null && !actual.getSQLState().trim().isEmpty()) {
                sb.append(" [SQLState=").append(actual.getSQLState()).append("]");
            }

            if (actual.getErrorCode() != 0) {
                sb.append(" [Code=").append(actual.getErrorCode()).append("]");
            }

            actual = actual.getNextException();
            primero = false;
        }

        return sb.toString();
    }

    private Throwable obtenerCausaRaiz(Throwable ex) {
        Throwable actual = ex;

        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }

        return actual;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ExistenciaTemporal {
        private final String codigo;
        private String descripcion;
        private BigDecimal existencia;

        private ExistenciaTemporal(String codigo, String descripcion, BigDecimal existencia) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.existencia = existencia;
        }
    }
}