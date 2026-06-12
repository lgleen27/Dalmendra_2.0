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
        actualizarEstado(sucursales);
        return sucursales;
    }

    public List<Sucursal> findAll() throws SQLException {
        return cargarSucursales();
    }

    public Sucursal guardar(Sucursal sucursal) throws SQLException {
        validar(sucursal);

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

    public Sucursal save(Sucursal sucursal) throws SQLException {
        return guardar(sucursal);
    }

    public Sucursal updateSinPassword(Sucursal sucursal) throws SQLException {
        validarSinPassword(sucursal);

        if (sucursal.getOrden() == null) {
            sucursal.setOrden(0);
        }

        if (sucursal.getActiva() == null) {
            sucursal.setActiva(true);
        }

        Sucursal actualizada = sucursalRepository.updateSinPassword(sucursal);
        cargarSucursales();
        return actualizada;
    }

    public void eliminar(Long id) throws SQLException {
        if (id != null) {
            sucursalRepository.deleteById(id);
            cargarSucursales();
        }
    }

    public void deleteById(Long id) throws SQLException {
        eliminar(id);
    }

    public void updateOrden(Long id, Integer orden) throws SQLException {
        if (id != null && orden != null) {
            sucursalRepository.updateOrden(id, orden);
            cargarSucursales();
        }
    }

    public void actualizarFechaActualizacion(Long id, LocalDateTime fecha) throws SQLException {
        if (id != null && fecha != null) {
            sucursalRepository.updateFechaActualizacion(id, Timestamp.valueOf(fecha));
            cargarSucursales();
        }
    }

    public void updateFechaActualizacion(Long id, Timestamp fecha) throws SQLException {
        if (id != null && fecha != null) {
            sucursalRepository.updateFechaActualizacion(id, fecha);
            cargarSucursales();
        }
    }

    public boolean haySucursalesActivas() {
        return !appState.getSucursalesActivas().isEmpty();
    }

    public List<Sucursal> getSucursalesActivas() {
        return appState.getSucursalesActivas();
    }

    public List<Sucursal> getSucursales() {
        return appState.getSucursales();
    }

    private void actualizarEstado(List<Sucursal> sucursales) {
        appState.setSucursales(sucursales);

        List<Sucursal> activas = sucursales.stream()
                .filter(s -> s.getActiva() == null || s.getActiva())
                .sorted(Comparator.comparing(Sucursal::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        appState.setSucursalesActivas(activas);

        if (appState.getSucursalSeleccionada() != null && appState.getSucursalSeleccionada().getId() != null) {
            Long idSeleccionado = appState.getSucursalSeleccionada().getId();

            for (Sucursal sucursal : activas) {
                if (sucursal.getId() != null && sucursal.getId().equals(idSeleccionado)) {
                    appState.setSucursalSeleccionada(sucursal);
                    return;
                }
            }

            for (Sucursal sucursal : sucursales) {
                if (sucursal.getId() != null && sucursal.getId().equals(idSeleccionado)) {
                    appState.setSucursalSeleccionada(sucursal);
                    return;
                }
            }
        }

        if (!activas.isEmpty()) {
            appState.setSucursalSeleccionada(activas.get(0));
        } else if (!sucursales.isEmpty()) {
            appState.setSucursalSeleccionada(sucursales.get(0));
        } else {
            appState.setSucursalSeleccionada(null);
        }
    }

    private void validar(Sucursal sucursal) {
        if (sucursal == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria.");
        }
        if (isBlank(sucursal.getNombreSucursal())) {
            throw new IllegalArgumentException("El nombre de la sucursal es obligatorio.");
        }
        if (isBlank(sucursal.getDataSource())) {
            throw new IllegalArgumentException("El servidor es obligatorio.");
        }
        if (isBlank(sucursal.getCatalog())) {
            throw new IllegalArgumentException("La base de datos es obligatoria.");
        }
        if (isBlank(sucursal.getUserId())) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        if (isBlank(sucursal.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
    }

    private void validarSinPassword(Sucursal sucursal) {
        if (sucursal == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria.");
        }
        if (isBlank(sucursal.getNombreSucursal())) {
            throw new IllegalArgumentException("El nombre de la sucursal es obligatorio.");
        }
        if (isBlank(sucursal.getDataSource())) {
            throw new IllegalArgumentException("El servidor es obligatorio.");
        }
        if (isBlank(sucursal.getCatalog())) {
            throw new IllegalArgumentException("La base de datos es obligatoria.");
        }
        if (isBlank(sucursal.getUserId())) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}