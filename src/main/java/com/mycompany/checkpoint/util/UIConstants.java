package com.mycompany.checkpoint.util;

import java.awt.Color;
import java.awt.Font;

/**
 * Constantes de diseño visual para la interfaz de CheckPoint Go.
 * Centraliza colores y fuentes para mantener un estilo consistente.
 */
public class UIConstants {

    // --- Colores principales ---
    public static final Color COLOR_PRIMARIO      = new Color(30, 80, 162);   // Azul institucional
    public static final Color COLOR_SECUNDARIO     = new Color(245, 247, 252); // Fondo claro
    public static final Color COLOR_SIDEBAR        = new Color(22, 58, 120);   // Azul oscuro sidebar
    public static final Color COLOR_SIDEBAR_TEXTO  = Color.WHITE;
    public static final Color COLOR_BOTON_ACTIVO   = new Color(255, 255, 255, 40); // Blanco semitransparente
    public static final Color COLOR_TEXTO_PRIMARIO = new Color(20, 30, 60);
    public static final Color COLOR_TEXTO_GRIS     = new Color(100, 110, 130);
    public static final Color COLOR_FONDO          = new Color(238, 242, 250);
    public static final Color COLOR_BLANCO         = Color.WHITE;
    public static final Color COLOR_EXITO          = new Color(34, 139, 87);   // Verde
    public static final Color COLOR_ALERTA         = new Color(220, 60, 60);   // Rojo
    public static final Color COLOR_TARDANZA       = new Color(230, 120, 20);  // Naranja

    // --- Fuentes ---
    public static final Font FUENTE_TITULO    = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FUENTE_SUBTITULO = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FUENTE_NORMAL    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_PEQUEÑA   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FUENTE_NUMERO    = new Font("Segoe UI", Font.BOLD, 36);
    public static final Font FUENTE_BOTON     = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_SIDEBAR   = new Font("Segoe UI", Font.PLAIN, 14);

    // Evitar instanciación
    private UIConstants() {}
}
