package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Sucursal;
import com.frexal.dalmendra.app.repository.SucursalRepository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para la administración y validación de sucursales físicas.
 * Mantiene la coherencia de datos, autoincremento de orden y sincronización con el {@link AppState}.
 */
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final AppState appState;

    public SucursalService(SucursalRepository sucursalRepository, AppState appState) {
        this.sucursalRepository = sucursalRepository;
        this.appState = appState;
    }

    /**
     * Consulta todas las sucursales en base de datos, las ordena y actualiza la lista en {@link AppState}.
     *
     * @return Lista ordenada de sucursales.
     * @throws SQLException Si ocurre un error al consultar la base de datos.
     */
    public List<Sucursal> cargarSucursales() throws SQLException {
        List<Sucursal> sucursales = sucursalRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Sucursal::getOrden,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .collect(Collectors.toList());

        appState.setSucursales(sucursales);
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