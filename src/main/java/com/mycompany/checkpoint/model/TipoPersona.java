package com.mycompany.checkpoint.model;

/**
 * Enumeración que define los tipos de personas
 * que pueden registrarse en el sistema.
 */
public enum TipoPersona {
    ESTUDIANTE("Estudiante"),
    PERSONAL_ADMINISTRATIVO("Personal Administrativo"),
    PERSONAL_SEGURIDAD("Personal de Seguridad del Instituto");

    private final String descripcion;

    TipoPersona(String descripcion) {
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
