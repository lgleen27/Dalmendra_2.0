package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Configuracion;
import com.frexal.dalmendra.app.repository.ConfiguracionRepository;

import java.sql.SQLException;
import java.util.List;

public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final AppState appState;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository, AppState appState) {
        this.configuracionRepository = configuracionRepository;
        this.appState = appState;
    }

    public void cargarConfiguracion() throws SQLException {
        List<Configuracion> configuraciones = configuracionRepository.findAll();

        for (Configuracion config : configuraciones) {
            actualizarEstado(config.getDescripcion(), config.getValor());
        }
    }

    public void actualizarConfiguracion(String descripcion, String valor) throws SQLException {
        configuracionRepository.upsert(descripcion, valor);
        actualizarEstado(descripcion, valor);
    }

    public int convertirMinutosAMilisegundos(int minutos) {
        return minutos * 60000;
    }

    public boolean convertirEnteroABoolean(int valor) {
        return valor == 1;
    }

    public int convertirBooleanAEntero(boolean valor) {
        return valor ? 1 : 0;
    }

    private void actualizarEstado(String descripcion, String valor) {
        switch (descripcion) {
            case "IdDbSelect":
                appState.setIdDbSelect(valor);
                break;
            case "TimeSyncSucursal":
                appState.setTimeSyncSucursal(valor);
                break;
            case "TimeChangeSucursal":
                appState.setTimeChangeSucursal(valor);
                break;
            case "FirstReport":
                appState.setFirstReport(valor);
                break;
            default:
                break;
        }
    }
}