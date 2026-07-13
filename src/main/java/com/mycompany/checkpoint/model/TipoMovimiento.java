package com.mycompany.checkpoint.model;

/**
 * Enumeración que define si un registro es una
 * entrada o una salida de las instalaciones.
 */
public enum TipoMovimiento {
    ENTRADA("Entrada"),
    SALIDA("Salida");

    private final String descripcion;

    TipoMovimiento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
