package com.frexal.dalmendra.app.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SyncSchedulerService {

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> tareaProgramada;

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