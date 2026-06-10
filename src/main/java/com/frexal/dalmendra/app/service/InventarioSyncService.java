package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.ExistenciaRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventarioSyncService {

    private final SqlServerSucursalClient sqlServerSucursalClient;
    private final ExistenciaRepository existenciaRepository;
    private final SucursalService sucursalService;
    private final OrdenExistenciaService ordenExistenciaService;
    private final AppState appState;

    public InventarioSyncService(SqlServerSucursalClient sqlServerSucursalClient,
                                 ExistenciaRepository existenciaRepository,
                                 SucursalService sucursalService,
                                 OrdenExistenciaService ordenExistenciaService,
                                 AppState appState) {
        this.sqlServerSucursalClient = sqlServerSucursalClient;
        this.existenciaRepository = existenciaRepository;
        this.sucursalService = sucursalService;
        this.ordenExistenciaService = ordenExistenciaService;
        this.appState = appState;
    }

    public void sincronizarTodas() {
        appState.clearErroresSincronizacion();

        for (Sucursal sucursal : appState.getSucursales()) {
            if (sucursal.getPassword() == null || sucursal.getPassword().isBlank()) {
                continue;
            }

            try {
                if (!sqlServerSucursalClient.validarConexion(sucursal)) {
                    appState.addErrorSincronizacion("Sin conexión con sucursal: " + sucursal.getNombreSucursal());
                    continue;
                }

                sincronizarSucursal(sucursal);
            } catch (Exception ex) {
                appState.addErrorSincronizacion("Error al sincronizar sucursal "
                        + sucursal.getNombreSucursal() + ": " + ex.getMessage());
            }
        }
    }

    public void sincronizarSucursal(Sucursal sucursal) throws SQLException {
        List<SqlServerSucursalClient.InventarioRemotoRow> inventario = sqlServerSucursalClient.consultarInventario(sucursal);
        List<SqlServerSucursalClient.VentaRemotaRow> ventas = sqlServerSucursalClient.consultarVentas(sucursal);

        Map<String, ExistenciaTemporal> mapa = new LinkedHashMap<String, ExistenciaTemporal>();

        for (SqlServerSucursalClient.InventarioRemotoRow row : inventario) {
            mapa.put(row.getCodigo(), new ExistenciaTemporal(
                    row.getCodigo(),
                    row.getDescripcion(),
                    row.getExistencia() == null ? BigDecimal.ZERO : row.getExistencia()
            ));
        }

        for (SqlServerSucursalClient.VentaRemotaRow venta : ventas) {
            ExistenciaTemporal actual = mapa.get(venta.getCodigo());

            if (actual != null) {
                BigDecimal vendida = venta.getExistenciaVendida() == null
                        ? BigDecimal.ZERO
                        : venta.getExistenciaVendida();
                actual.existencia = actual.existencia.subtract(vendida);
            } else {
                BigDecimal vendida = venta.getExistenciaVendida() == null
                        ? BigDecimal.ZERO
                        : venta.getExistenciaVendida();
                mapa.put(venta.getCodigo(), new ExistenciaTemporal(
                        venta.getCodigo(),
                        venta.getDescripcion(),
                        vendida.negate()
                ));
            }
        }

        existenciaRepository.deleteBySucursalId(sucursal.getId());

        LocalDateTime ahora = LocalDateTime.now();

        for (ExistenciaTemporal item : mapa.values()) {
            Existencia existencia = new Existencia();
            existencia.setSucursalId(sucursal.getId());
            existencia.setCodigo(item.codigo);
            existencia.setDescripcion(item.descripcion);
            existencia.setExistencia(item.existencia);
            existencia.setOrden(ordenExistenciaService.obtenerOrden(sucursal.getId(), item.codigo));
            existencia.setFechaActualizacion(ahora);

            existenciaRepository.insert(existencia);
        }

        sucursalService.actualizarFechaActualizacion(sucursal.getId(), ahora);
    }

    private static class ExistenciaTemporal {
        private final String codigo;
        private final String descripcion;
        private BigDecimal existencia;

        private ExistenciaTemporal(String codigo, String descripcion, BigDecimal existencia) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.existencia = existencia;
        }
    }
}