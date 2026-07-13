package com.mycompany.checkpoint.ui.stages;

import com.mycompany.checkpoint.ui.controllers.CameraController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Ventana independiente para captura de cámara y escaneo de QR.
 */
public class CameraStage extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(CameraStage.class);

    private CameraController controller;

    public CameraStage() throws IOException {
        initStyle(StageStyle.DECORATED);
        setTitle("Escanear Código QR");
        setWidth(900);
        setHeight(750);
        setResizable(false);

        // Cargar FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/mycompany/checkpoint/fxml/camera.fxml"));
        Scene scene = new Scene(loader.load());

        // Cargar CSS
        String css = getClass().getResource(
                "/com/mycompany/checkpoint/styles/main.css").toExternalForm();
        scene.getStylesheets().add(css);

        setScene(scene);

        this.controller = loader.getController();
        this.controller.setStage(this);

        logger.info("CameraStage creado");
    }

    /**
     * Establece el callback a ejecutar cuando se detecte un QR.
     */
    public void setOnQRDetected(Consumer<String> callback) {
        if (controller != null) {
            controller.setOnQRDetected(callback);
        }
    }

    /**
     * Abre la ventana de cámara de forma modal.
     */
    public static void showAndWait(Consumer<String> onQRDetected) {
        try {
            CameraStage stage = new CameraStage();
            stage.setOnQRDetected(onQRDetected);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Error abriendo CameraStage: {}", e.getMessage());
        }
    }
}
