package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.model.Configuracion;
import com.frexal.dalmendra.app.model.Existencia;
import com.frexal.dalmendra.app.model.OrdenExistencia;
import com.frexal.dalmendra.app.model.Sucursal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppState {

    private String nombrePrograma = "Dalmendra";

    private List<Sucursal> sucursales = new ArrayList<>();
    private List<Sucursal> sucursalesActivas = new ArrayList<>();
    private List<Existencia> existencias = new ArrayList<>();
    private List<Categoria> categorias = new ArrayList<>();
    private List<OrdenExistencia> ordenExistencias = new ArrayList<>();
    private List<String> erroresSincronizacion = new ArrayList<>();

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
        return sucursales;
    }

    public void setSucursales(List<Sucursal> sucursales) {
        this.sucursales = sucursales;
    }

    public List<Sucursal> getSucursalesActivas() {
        return sucursalesActivas;
    }

    public void setSucursalesActivas(List<Sucursal> sucursalesActivas) {
        this.sucursalesActivas = sucursalesActivas;
    }

    public Sucursal getSucursalSeleccionada() {
        return sucursalSeleccionada;
    }

    public void setSucursalSeleccionada(Sucursal sucursalSeleccionada) {
        this.sucursalSeleccionada = sucursalSeleccionada;
    }

    public List<Existencia> getExistencias() {
        return existencias;
    }

    public void setExistencias(List<Existencia> existencias) {
        this.existencias = existencias;
    }

    public List<Categoria> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<Categoria> categorias) {
        this.categorias = categorias;
    }

    public List<OrdenExistencia> getOrdenExistencias() {
        return ordenExistencias;
    }

    public void setOrdenExistencias(List<OrdenExistencia> ordenExistencias) {
        this.ordenExistencias = ordenExistencias;
    }

    public List<String> getErroresSincronizacion() {
        return erroresSincronizacion;
    }

    public void setErroresSincronizacion(List<String> erroresSincronizacion) {
        this.erroresSincronizacion = erroresSincronizacion;
    }

    public void addErrorSincronizacion(String error) {
        this.erroresSincronizacion.add(error);
        this.hayErrorSincronizacion = true;
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
}