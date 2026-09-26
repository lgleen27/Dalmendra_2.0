package com.frexal.dalmendra.app.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servicio encargado de la ejecución periódica y concurrente de tareas en segundo plano.
 * Utiliza un {@link ScheduledExecutorService} de un solo hilo para disparar la sincronización
 * automática de inventarios a intervalos regulares de tiempo.
 */
public class SyncSchedulerService {

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> tareaProgramada;

    /**
     * Cancela cualquier tarea previa y programa la ejecución periódica de una acción.
     *
     * @param tarea Tarea ejecutable (Runnable).
     * @param minutos Frecuencia de repetición en minutos.
     */
    public synchronized void programarSincronizacion(Runnable tarea, int minutos) {
        if (minutos <= 0 || tarea == null) {
            return;
        }

        detener();

        executorService = Executors.newSingleThreadScheduledExecutor();
        tareaProgramada = executorService.scheduleAtFixedRate(
                tarea,
                0,
                minutos,
                TimeUnit.MINUTES
        );
    }

    public synchronized void detener() {
        if (tareaProgramada != null) {
            tareaProgramada.cancel(false);
            tareaProgramada = null;
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            executorService = null;
        }
    }
}