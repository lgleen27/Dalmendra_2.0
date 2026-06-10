package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.SucursalRepository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final AppState appState;

    public SucursalService(SucursalRepository sucursalRepository, AppState appState) {
        this.sucursalRepository = sucursalRepository;
        this.appState = appState;
    }

    public List<Sucursal> cargarSucursales() throws SQLException {
        List<Sucursal> sucursales = sucursalRepository.findAll();
        appState.setSucursales(sucursales);

        List<Sucursal> activas = sucursales.stream()
                .filter(s -> s.getActiva() == null || s.getActiva())
                .sorted(Comparator.comparing(Sucursal::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        appState.setSucursalesActivas(activas);
        return sucursales;
    }

    public Sucursal guardar(Sucursal sucursal) throws SQLException {
        if (sucursal.getOrden() == null) {
            int siguienteOrden = appState.getSucursales().stream()
                    .map(Sucursal::getOrden)
                    .filter(v -> v != null)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            sucursal.setOrden(siguienteOrden);
        }

        if (sucursal.getActiva() == null) {
            sucursal.setActiva(true);
        }

        Sucursal guardada = sucursalRepository.save(sucursal);
        cargarSucursales();
        return guardada;
    }

    public void eliminar(Long id) throws SQLException {
        sucursalRepository.deleteById(id);
        cargarSucursales();
    }

    public void actualizarFechaActualizacion(Long id, LocalDateTime fecha) throws SQLException {
        sucursalRepository.updateFechaActualizacion(id, Timestamp.valueOf(fecha));
        cargarSucursales();
    }

    public boolean haySucursalesActivas() {
        return !appState.getSucursalesActivas().isEmpty();
    }
}