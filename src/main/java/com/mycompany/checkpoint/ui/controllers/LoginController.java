package com.mycompany.checkpoint.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Button btnIngresar;
    @FXML private Label lblError;

    @FXML
    private void handleLogin() {
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText().trim();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            lblError.setText("Por favor, completa todos los campos.");
            return;
        }

        // Aquí puedes conectar luego con la autenticación de Firebase Auth si lo deseas.
        // Por ahora, validaremos con un usuario administrador por defecto:
        if (usuario.equals("admin") && contrasena.equals("12345")) {
            lblError.setText(""); // Limpiar error
            irAlPanelPrincipal();
        } else {
            lblError.setText("❌ Usuario o contraseña incorrectos.");
        }
    }

private void irAlPanelPrincipal() {
    try {
        // Cambiamos la ruta para que apunte a tu FXML principal real
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/checkpoint/fxml/main.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) btnIngresar.getScene().getWindow();
        
        // Creamos la escena manteniendo las dimensiones que ya usabas
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1200, 700);
        
        // Opcional: Si quieres cargarle los estilos CSS de una vez
        try {
            String css = getClass().getResource("/com/mycompany/checkpoint/styles/main.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("No se pudo cargar el CSS en el cambio de pantalla");
        }
        
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.setTitle("CheckPoint Go - Control de Entrada y Salida");
        stage.show();
        
    } catch (Exception e) {
        e.printStackTrace();
        lblError.setText("Error al cargar el panel principal: " + e.getMessage());
    }
}
}