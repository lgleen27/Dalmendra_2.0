package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.OrdenExistencia;
import com.frexal.dalmendra.app.repository.OrdenExistenciaRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio encargado de gestionar y autocalcular el orden visual de los artículos por sucursal.
 */
public class OrdenExistenciaService {

    private final OrdenExistenciaRepository ordenRepository;
    private final AppState appState;

    public OrdenExistenciaService(OrdenExistenciaRepository ordenRepository, AppState appState) {
        this.ordenRepository = ordenRepository;
        this.appState = appState;
    }

    /**
     * Carga todos los órdenes registrados y actualiza la lista en memoria en {@link AppState}.
     *
     * @throws SQLException Si ocurre un error al consultar la base de datos.
     */
    public void cargarOrdenes() throws SQLException {
        List<OrdenExistencia> ordenes = ordenRepository.findAll();
        appState.setOrdenExistencias(ordenes);
    }

    public int obtenerOrden(Long sucursalId, String codigo) throws SQLException {
        validarDatos(sucursalId, codigo);

        OrdenExistencia existente = ordenRepository.findBySucursalIdAndCodigo(sucursalId, codigo);

        if (existente != null && existente.getOrden() != null) {
            return existente.getOrden();
        }

        int siguienteOrden = ordenRepository.getSiguienteOrden(sucursalId);
        ordenRepository.saveOrUpdate(new OrdenExistencia(sucursalId, codigo, siguienteOrden));
        cargarOrdenes();
        return siguienteOrden;
    }

    public void actualizarOrden(Long sucursalId, String codigo, int orden) throws SQLException {
        validarDatos(sucursalId, codigo);

        ordenRepository.saveOrUpdate(new OrdenExistencia(sucursalId, codigo, orden));
        cargarOrdenes();
    }

    private void validarDatos(Long sucursalId, String codigo) {
        if (sucursalId == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria.");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código es obligatorio.");
        }
    }
}