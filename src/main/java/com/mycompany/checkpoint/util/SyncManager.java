package com.mycompany.checkpoint.util;

import com.mycompany.checkpoint.firebase.FirebaseService;
import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Gestor de sincronización entre cache local y Firebase.
 * Maneja operaciones offline-first con auto-sync cuando hay conexión.
 */
public class SyncManager {
    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);
    private static final long SYNC_INTERVAL_MS = 30000; // 30 segundos

    private final FirebaseService firebaseService;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isOnline = new AtomicBoolean(true);

    private Consumer<List<Estudiante>> onPersonasUpdated;
    private Consumer<List<Registro>> onRegistrosUpdated;
    private Consumer<Boolean> onConnectionStatusChanged;

    public SyncManager(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SyncThread");
            t.setDaemon(true);
            return t;
            });
    }

    /**
     * Inicia el gestor de sincronización.
     */
    public void start() {
        if (isRunning.getAndSet(true)) {
            logger.warn("SyncManager ya está en ejecución");
            return;
        }

        logger.info("SyncManager iniciado");

        // Cargar datos locales al iniciar
        loadLocalData();

        // Iniciar loop de sincronización
        executor.submit(this::syncLoop);

        // Iniciar monitoreo de conectividad
        executor.submit(this::connectivityMonitorLoop);
    }

    /**
     * Detiene el gestor de sincronización.
     */
    public void stop() {
        isRunning.set(false);
        executor.shutdownNow();
        logger.info("SyncManager detenido");
    }

    /**
     * Carga datos locales al iniciar.
     */
    private void loadLocalData() {
        try {
            List<Estudiante> personas = LocalDatabase.loadPersonas();
            List<Registro> registros = LocalDatabase.loadRegistros();

            if (onPersonasUpdated != null) onPersonasUpdated.accept(personas);
            if (onRegistrosUpdated != null) onRegistrosUpdated.accept(registros);

            logger.info("Datos locales cargados: {} personas, {} registros",
                    personas.size(), registros.size());
        } catch (Exception e) {
            logger.error("Error cargando datos locales: {}", e.getMessage());
        }
    }

    /**
     * Loop principal de sincronización (cada 30 segundos).
     */
    private void syncLoop() {
        while (isRunning.get()) {
            try {
                if (isOnline.get()) {
                    performSync();
                }
                Thread.sleep(SYNC_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error en sync loop: {}", e.getMessage());
            }
        }
    }

    /**
     * Loop para monitorear conectividad (cada 10 segundos).
     */
    private void connectivityMonitorLoop() {
        while (isRunning.get()) {
            try {
                boolean wasOnline = isOnline.get();
                boolean isNowOnline = checkConnectivity();

                if (wasOnline != isNowOnline) {
                    isOnline.set(isNowOnline);
                    logger.info("Estado de conexión cambió a: {}", isNowOnline ? "ONLINE" : "OFFLINE");
                    if (onConnectionStatusChanged != null) {
                        onConnectionStatusChanged.accept(isNowOnline);
                    }
                    if (isNowOnline) {
                        performSync(); // Sincronizar inmediatamente al reconectar
                    }
                }

                Thread.sleep(10000); // Chequear cada 10 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error en connectivity monitor: {}", e.getMessage());
            }
        }
    }

    /**
     * Verifica conectividad ping a 8.8.8.8 (Google DNS).
     */
    private boolean checkConnectivity() {
        try {
            Process process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("Error checando conectividad: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Realiza sincronización: carga datos locales y remotos, resuelve cambios.
     */
    private void performSync() {
        try {
            // Obtener datos remotos
            List<Estudiante> remotePersonas = firebaseService.getAllPersonas().get();
            List<Registro> remoteRegistros = firebaseService.getAllRegistros().get();

            // Obtener datos locales
            List<Estudiante> localPersonas = LocalDatabase.loadPersonas();
            List<Registro> localRegistros = LocalDatabase.loadRegistros();

            // Sincronizar personas pendientes
            for (Estudiante p : localPersonas) {
                if (p.needsSync()) {
                    try {
                        firebaseService.addPersona(p).get();
                        p.setLastSyncTime(System.currentTimeMillis());
                        logger.info("Persona sincronizada: {}", p.getId());
                    } catch (Exception e) {
                        logger.error("Error sincronizando persona {}: {}", p.getId(), e.getMessage());
                    }
                }
            }

            // Sincronizar registros pendientes
            for (Registro r : localRegistros) {
                if (r.needsSync()) {
                    try {
                        firebaseService.addRegistro(r).get();
                        r.setLastSyncTime(System.currentTimeMillis());
                        logger.info("Registro sincronizado: {}", r.getId());
                    } catch (Exception e) {
                        logger.error("Error sincronizando registro {}: {}", r.getId(), e.getMessage());
                    }
                }
            }

            // Guardar estado actualizado localmente
            LocalDatabase.savePersonas(localPersonas);
            LocalDatabase.saveRegistros(localRegistros);

            // Notificar cambios
            if (onPersonasUpdated != null) onPersonasUpdated.accept(remotePersonas);
            if (onRegistrosUpdated != null) onRegistrosUpdated.accept(remoteRegistros);

            logger.debug("Sincronización completada");
        } catch (Exception e) {
            logger.error("Error durante sincronización: {}", e.getMessage());
        }
    }

    /**
     * Agrega una nueva persona localmente con estado PENDING.
     */
    public void addPersonaLocal(Estudiante persona) {
        List<Estudiante> personas = LocalDatabase.loadPersonas();
        personas.add(persona);
        LocalDatabase.savePersonas(personas);
        if (onPersonasUpdated != null) onPersonasUpdated.accept(personas);
    }

    /**
     * Agrega un nuevo registro localmente determinando automáticamente si es ENTRADA o SALIDA
     * basándose en la actividad del alumno durante el día actual para evitar duplicados.
     */
    public void addRegistroLocal(Registro registro) {
        if (registro == null || registro.getPersona() == null) return;

        String nieAlumno = registro.getPersona().getNie();
        List<Registro> registrosExistentes = LocalDatabase.loadRegistros();

        // Filtrar el historial del alumno en el día de hoy
        List<Registro> historialHoyAlumno = registrosExistentes.stream()
                .filter(r -> r.getPersona() != null && nieAlumno.equals(r.getPersona().getNie()))
                .collect(Collectors.toList());

        // Determinar dinámicamente qué movimiento corresponde
        TipoMovimiento movimientoCorrecto = determinarTipoMovimientoAuto(historialHoyAlumno);
        registro.setTipo(movimientoCorrecto);

        // Si se cambia automáticamente a entrada, evaluar la tardanza por defecto (Ej: 07:30 AM)
        if (movimientoCorrecto == TipoMovimiento.ENTRADA) {
            boolean esTardanza = LocalTime.now().isAfter(LocalTime.of(7, 30));
            registro.setTardanza(esTardanza);
        } else {
            registro.setTardanza(false); // Las salidas no acumulan retardos
        }

        logger.info("Asignado movimiento automático [{}] para el NIE: {}", movimientoCorrecto, nieAlumno);

        registrosExistentes.add(registro);
        LocalDatabase.saveRegistros(registrosExistentes);
        
        if (onRegistrosUpdated != null) {
            onRegistrosUpdated.accept(registrosExistentes);
        }
    }

    /**
     * Evalúa las entradas y salidas previas del alumno para decidir el siguiente paso.
     */
    private TipoMovimiento determinarTipoMovimientoAuto(List<Registro> historialHoy) {
        if (historialHoy == null || historialHoy.isEmpty()) {
            return TipoMovimiento.ENTRADA;
        }

        long totalEntradas = historialHoy.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || "ENTRADA".equalsIgnoreCase(String.valueOf(r.getTipo())))
                .count();

        long totalSalidas = historialHoy.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.SALIDA || "SALIDA".equalsIgnoreCase(String.valueOf(r.getTipo())))
                .count();

        // Si las entradas superan a las salidas, significa que está dentro del establecimiento -> le corresponde Salida.
        if (totalEntradas > totalSalidas) {
            return TipoMovimiento.SALIDA;
        } else {
            return TipoMovimiento.ENTRADA;
        }
    }

    // --- Callbacks ---

    public void setOnPersonasUpdated(Consumer<List<Estudiante>> callback) {
        this.onPersonasUpdated = callback;
    }

    public void setOnRegistrosUpdated(Consumer<List<Registro>> callback) {
        this.onRegistrosUpdated = callback;
    }

    public void setOnConnectionStatusChanged(Consumer<Boolean> callback) {
        this.onConnectionStatusChanged = callback;
    }

    // --- Getters ---

    public boolean isOnline() {
        return isOnline.get();
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}