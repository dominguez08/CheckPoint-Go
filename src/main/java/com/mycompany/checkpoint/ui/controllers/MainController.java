package com.mycompany.checkpoint.ui.controllers;

import com.mycompany.checkpoint.firebase.FirebaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private Button btnResumen;
    @FXML private Button btnRegistro;
    @FXML private Button btnPersonas;
    @FXML private Button btnLogout;
    @FXML private Label lblUsuario;
    
    // 🔥 CONECTADO: Descomentado y listo para reflejar el estado global en el menú lateral
    @FXML private Label lblEstado; 

    private ResumenController resumenController;
    private RegistroController registroController;
    private PersonasController personasController;
    private FirebaseService firebaseService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Inicializando MainController");

        try {
            loadViews();
            setupNavigationHandlers();
            switchToResumen();
            
            // 🔥 SOLUCIÓN: Usamos la instancia central del Singleton y refrescamos la UI
            this.firebaseService = FirebaseService.getInstance();
            actualizarEstadoSincronizacion();

        } catch (IOException e) {
            logger.error("Error inicializando MainController: {}", e.getMessage(), e);
        }
    }

    /**
     * Evalúa el estado real del Singleton de Firebase y actualiza el texto de 
     * sincronización ubicado exclusivamente en la barra lateral.
     */
    private void actualizarEstadoSincronizacion() {
        if (lblEstado == null) return;

        if (firebaseService != null && firebaseService.isInitialized()) {
            logger.info("Firebase detectado: Sincronizado");
            lblEstado.setText("🟢 Sincronizado");
            lblEstado.setStyle("-fx-text-fill: #228B57; -fx-font-weight: bold;");
        } else {
            logger.warn("Firebase no inicializado: Modo local activo");
            lblEstado.setText("🟠 Modo Local");
            lblEstado.setStyle("-fx-text-fill: #E67814; -fx-font-weight: bold;");
        }
    }

    /**
     * Carga todas las vistas FXML.
     */
    private void loadViews() throws IOException {
        resumenController = loadFXML("/com/mycompany/checkpoint/fxml/resumen.fxml");
        registroController = loadFXML("/com/mycompany/checkpoint/fxml/registro.fxml");
        personasController = loadFXML("/com/mycompany/checkpoint/fxml/personas.fxml");
        logger.info("Todas las vistas cargadas");
    }

    /**
     * Carga un archivo FXML y retorna su controller.
     */
    private <T> T loadFXML(String path) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        Node view = loader.load();
        T controller = loader.getController();
        
        if (controller instanceof ResumenController) {
            ((ResumenController) controller).setView(view);
        } else if (controller instanceof RegistroController) {
            ((RegistroController) controller).setView(view);
        } else if (controller instanceof PersonasController) {
            ((PersonasController) controller).setView(view);
        }
        return controller;
    }

    private void setupNavigationHandlers() {
        btnResumen.setOnAction(e -> switchToResumen());
        btnRegistro.setOnAction(e -> switchToRegistro());
        btnPersonas.setOnAction(e -> switchToPersonas());
        btnLogout.setOnAction(e -> handleLogout());
    }

    @FXML
    private void switchToResumen() {
        logger.info("Cambiando a Resumen");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(resumenController.getView());
        resumenController.onViewShown();
        updateSelectedButton(btnResumen);
    }

    @FXML
    private void switchToRegistro() {
        logger.info("Cambiando a Registro");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(registroController.getView());
        registroController.onViewShown();
        updateSelectedButton(btnRegistro);
    }

    @FXML
    private void switchToPersonas() {
        logger.info("Cambiando a Personas");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(personasController.getView());
        personasController.onViewShown();
        updateSelectedButton(btnPersonas);
    }

    @FXML
    private void handleLogout() {
        logger.info("Cerrando sesión");
        System.exit(0);
    }

    private void updateSelectedButton(Button selected) {
        btnResumen.setStyle(selected == btnResumen ? "-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.15);" : "");
        btnRegistro.setStyle(selected == btnRegistro ? "-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.15);" : "");
        btnPersonas.setStyle(selected == btnPersonas ? "-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.15);" : "");
    }

    public ResumenController getResumenController() { return resumenController; }
    public RegistroController getRegistroController() { return registroController; }
    public PersonasController getPersonasController() { return personasController; }
}