package com.mycompany.checkpoint.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo que representa un registro de movimiento
 * (entrada o salida) de una persona en el sistema.
 */
public class Registro {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public enum SyncState {
        SYNCED, PENDING, ERROR, OFFLINE
    }

    private int id;                        // ID único del registro
    private Estudiante persona;               // Persona que realizó el movimiento
    private TipoMovimiento tipo;           // Entrada o Salida
    private LocalDateTime fechaHora;       // Fecha y hora del movimiento
    private boolean tardanza;              // Si llegó después de la hora límite
    private String observaciones;          // Notas opcionales
    private SyncState syncState;           // Estado de sincronización
    private long lastSyncTime;             // Timestamp de último sync

    /**
     * Constructor de un nuevo registro de movimiento.
     *
     * @param id      Identificador único del registro
     * @param persona Persona que realiza el movimiento
     * @param tipo    Tipo de movimiento (ENTRADA o SALIDA)
     */
    public Registro(int id, Estudiante persona, TipoMovimiento tipo) {
        this.id = id;
        this.persona = persona;
        this.tipo = tipo;
        this.fechaHora = LocalDateTime.now(); // Toma la hora actual automáticamente
        this.tardanza = false;
        this.observaciones = "";
        this.syncState = SyncState.PENDING;
        this.lastSyncTime = 0;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Estudiante getPersona() { return persona; }
    public void setPersona(Estudiante persona) { this.persona = persona; }

    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public boolean isTardanza() { return tardanza; }
    public void setTardanza(boolean tardanza) { this.tardanza = tardanza; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public SyncState getSyncState() { return syncState; }
    public void setSyncState(SyncState syncState) { this.syncState = syncState; }

    public long getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    public boolean needsSync() {
        return syncState == SyncState.PENDING || syncState == SyncState.ERROR;
    }

    /**
     * Retorna la fecha y hora formateada como cadena legible.
     */
    public String getFechaHoraFormateada() {
        return fechaHora.format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | %s%s",
                id,
                tipo.getDescripcion(),
                persona.getNombre(),
                getFechaHoraFormateada(),
                tardanza ? " [TARDANZA]" : "");
    }
}
