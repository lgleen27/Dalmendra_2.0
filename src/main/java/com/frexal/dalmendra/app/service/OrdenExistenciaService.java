package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.OrdenExistencia;
import com.frexal.dalmendra.app.repository.OrdenExistenciaRepository;

import java.sql.SQLException;
import java.util.List;

public class OrdenExistenciaService {

    private final OrdenExistenciaRepository ordenRepository;
    private final AppState appState;

    public OrdenExistenciaService(OrdenExistenciaRepository ordenRepository, AppState appState) {
        this.ordenRepository = ordenRepository;
        this.appState = appState;
    }

    public void cargarOrdenes() throws SQLException {
        List<OrdenExistencia> ordenes = ordenRepository.findAll();
        appState.setOrdenExistencias(ordenes);
    }

    public int obtenerOrden(Long sucursalId, String codigo) throws SQLException {
        OrdenExistencia existente = ordenRepository.findBySucursalIdAndCodigo(sucursalId, codigo);

        if (existente != null) {
            return existente.getOrden();
        }

        int siguienteOrden = ordenRepository.getSiguienteOrden(sucursalId);
        ordenRepository.saveOrUpdate(new OrdenExistencia(sucursalId, codigo, siguienteOrden));
        cargarOrdenes();
        return siguienteOrden;
    }

    public void actualizarOrden(Long sucursalId, String codigo, int orden) throws SQLException {
        ordenRepository.saveOrUpdate(new OrdenExistencia(sucursalId, codigo, orden));
        cargarOrdenes();
    }
}