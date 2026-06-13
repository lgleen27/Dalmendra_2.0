package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Configuracion;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.OrdenExistencia;
import com.frexal.dalmendra.app.model.Sucursal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AppState {

    private String nombrePrograma = "Dalmendra";

    private final List<Sucursal> sucursales = new ArrayList<>();
    private final List<Sucursal> sucursalesActivas = new ArrayList<>();
    private final List<Existencia> existencias = new ArrayList<>();
    private final List<Categoria> categorias = new ArrayList<>();
    private final List<OrdenExistencia> ordenExistencias = new ArrayList<>();
    private final List<String> erroresSincronizacion = new ArrayList<>();

    private Sucursal sucursalSeleccionada;

    private boolean primeraEjecucion;
    private boolean hayErrorSincronizacion;
    private boolean estaAbiertoFrmListado;
    private boolean estaAbiertoFrmCategorias;
    private boolean banActualizacion = true;

    private String idDbSelect = "1";
    private String timeSyncSucursal = "1";
    private String timeChangeSucursal = "15";
    private String firstReport = "PorCategorias";

    public String getNombrePrograma() {
        return nombrePrograma;
    }

    public void setNombrePrograma(String nombrePrograma) {
        this.nombrePrograma = nombrePrograma;
    }

    public List<Sucursal> getSucursales() {
        return Collections.unmodifiableList(sucursales);
    }

    public void setSucursales(List<Sucursal> sucursales) {
        this.sucursales.clear();

        if (sucursales != null) {
            this.sucursales.addAll(sucursales);
        }

        recalcularSucursalesActivas();
        validarSucursalSeleccionada();
    }

    public List<Sucursal> getSucursalesActivas() {
        return Collections.unmodifiableList(sucursalesActivas);
    }

    public void setSucursalesActivas(List<Sucursal> sucursalesActivas) {
        this.sucursalesActivas.clear();

        if (sucursalesActivas != null) {
            this.sucursalesActivas.addAll(sucursalesActivas);
        }

        validarSucursalSeleccionada();
    }

    public Sucursal getSucursalSeleccionada() {
        return sucursalSeleccionada;
    }

    public void setSucursalSeleccionada(Sucursal sucursalSeleccionada) {
        if (sucursalSeleccionada == null) {
            this.sucursalSeleccionada = null;
            return;
        }

        for (Sucursal sucursal : sucursalesActivas) {
            if (mismaSucursal(sucursal, sucursalSeleccionada)) {
                this.sucursalSeleccionada = sucursal;
                return;
            }
        }

        for (Sucursal sucursal : sucursales) {
            if (mismaSucursal(sucursal, sucursalSeleccionada)) {
                this.sucursalSeleccionada = sucursal;
                return;
            }
        }

        this.sucursalSeleccionada = null;
    }

    public List<Existencia> getExistencias() {
        return Collections.unmodifiableList(existencias);
    }

    public void setExistencias(List<Existencia> existencias) {
        this.existencias.clear();

        if (existencias != null) {
            this.existencias.addAll(existencias);
        }
    }

    public List<Categoria> getCategorias() {
        return Collections.unmodifiableList(categorias);
    }

    public void setCategorias(List<Categoria> categorias) {
        this.categorias.clear();

        if (categorias != null) {
            this.categorias.addAll(categorias);
        }
    }

    public List<OrdenExistencia> getOrdenExistencias() {
        return Collections.unmodifiableList(ordenExistencias);
    }

    public void setOrdenExistencias(List<OrdenExistencia> ordenExistencias) {
        this.ordenExistencias.clear();

        if (ordenExistencias != null) {
            this.ordenExistencias.addAll(ordenExistencias);
        }
    }

    public List<String> getErroresSincronizacion() {
        return Collections.unmodifiableList(erroresSincronizacion);
    }

    public void setErroresSincronizacion(List<String> erroresSincronizacion) {
        this.erroresSincronizacion.clear();

        if (erroresSincronizacion != null) {
            this.erroresSincronizacion.addAll(erroresSincronizacion);
        }

        this.hayErrorSincronizacion = !this.erroresSincronizacion.isEmpty();
    }

    public void addErrorSincronizacion(String error) {
        if (error != null && !error.trim().isEmpty()) {
            this.erroresSincronizacion.add(error.trim());
            this.hayErrorSincronizacion = true;
        }
    }

    public void clearErroresSincronizacion() {
        this.erroresSincronizacion.clear();
        this.hayErrorSincronizacion = false;
    }

    public boolean isPrimeraEjecucion() {
        return primeraEjecucion;
    }

    public void setPrimeraEjecucion(boolean primeraEjecucion) {
        this.primeraEjecucion = primeraEjecucion;
    }

    public boolean isHayErrorSincronizacion() {
        return hayErrorSincronizacion;
    }

    public void setHayErrorSincronizacion(boolean hayErrorSincronizacion) {
        this.hayErrorSincronizacion = hayErrorSincronizacion;
    }

    public boolean isEstaAbiertoFrmListado() {
        return estaAbiertoFrmListado;
    }

    public void setEstaAbiertoFrmListado(boolean estaAbiertoFrmListado) {
        this.estaAbiertoFrmListado = estaAbiertoFrmListado;
    }

    public boolean isEstaAbiertoFrmCategorias() {
        return estaAbiertoFrmCategorias;
    }

    public void setEstaAbiertoFrmCategorias(boolean estaAbiertoFrmCategorias) {
        this.estaAbiertoFrmCategorias = estaAbiertoFrmCategorias;
    }

    public boolean isBanActualizacion() {
        return banActualizacion;
    }

    public void setBanActualizacion(boolean banActualizacion) {
        this.banActualizacion = banActualizacion;
    }

    public String getIdDbSelect() {
        return idDbSelect;
    }

    public void setIdDbSelect(String idDbSelect) {
        this.idDbSelect = idDbSelect;
    }

    public String getTimeSyncSucursal() {
        return timeSyncSucursal;
    }

    public void setTimeSyncSucursal(String timeSyncSucursal) {
        this.timeSyncSucursal = timeSyncSucursal;
    }

    public String getTimeChangeSucursal() {
        return timeChangeSucursal;
    }

    public void setTimeChangeSucursal(String timeChangeSucursal) {
        this.timeChangeSucursal = timeChangeSucursal;
    }

    public String getFirstReport() {
        return firstReport;
    }

    public void setFirstReport(String firstReport) {
        this.firstReport = firstReport;
    }

    public Optional<Configuracion> getConfiguracion(String descripcion) {
        switch (descripcion) {
            case "IdDbSelect":
                return Optional.of(new Configuracion("IdDbSelect", idDbSelect));
            case "TimeSyncSucursal":
                return Optional.of(new Configuracion("TimeSyncSucursal", timeSyncSucursal));
            case "TimeChangeSucursal":
                return Optional.of(new Configuracion("TimeChangeSucursal", timeChangeSucursal));
            case "FirstReport":
                return Optional.of(new Configuracion("FirstReport", firstReport));
            default:
                return Optional.empty();
        }
    }

    private void recalcularSucursalesActivas() {
        sucursalesActivas.clear();

        for (Sucursal sucursal : sucursales) {
            if (sucursal != null && Boolean.TRUE.equals(sucursal.getActiva())) {
                sucursalesActivas.add(sucursal);
            }
        }
    }

    private void validarSucursalSeleccionada() {
        if (sucursalSeleccionada == null) {
            if (!sucursalesActivas.isEmpty()) {
                sucursalSeleccionada = sucursalesActivas.get(0);
            }
            return;
        }

        for (Sucursal sucursal : sucursalesActivas) {
            if (mismaSucursal(sucursal, sucursalSeleccionada)) {
                sucursalSeleccionada = sucursal;
                return;
            }
        }

        if (!sucursalesActivas.isEmpty()) {
            sucursalSeleccionada = sucursalesActivas.get(0);
        } else {
            sucursalSeleccionada = null;
        }
    }

    private boolean mismaSucursal(Sucursal a, Sucursal b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getId() == null || b.getId() == null) {
            return false;
        }

        return a.getId().equals(b.getId());
    }
}