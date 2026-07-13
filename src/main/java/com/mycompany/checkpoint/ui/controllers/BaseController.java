package com.mycompany.checkpoint.ui.controllers;

import javafx.scene.Node;

/**
 * Clase base para todos los controladores de vistas.
 * Proporciona métodos comunes para gestión de vistas.
 */
public abstract class BaseController {
    protected Node view;

    /**
     * Establece la vista asociada a este controlador.
     */
    public void setView(Node view) {
        this.view = view;
    }

    /**
     * Obtiene la vista asociada a este controlador.
     */
    public Node getView() {
        return view;
    }

    /**
     * Llamado cuando esta vista se muestra/activa.
     * Las subclases pueden sobrescribir para actualizar datos, etc.
     */
    public void onViewShown() {
        // Implementar en subclases si es necesario
    }

    /**
     * Llamado cuando esta vista se oculta.
     * Las subclases pueden sobrescribir para limpiar recursos, etc.
     */
    public void onViewHidden() {
        // Implementar en subclases si es necesario
    }
}
