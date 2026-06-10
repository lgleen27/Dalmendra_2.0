package com.frexal.dalmendra.app.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncSchedulerService {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public void programarSincronizacion(Runnable tarea, int minutos) {
        if (minutos <= 0) {
            return;
        }

        executorService.scheduleAtFixedRate(tarea, 0, minutos, TimeUnit.MINUTES);
    }

    public void detener() {
        executorService.shutdown();
    }
}