package com.mycompany.checkpoint.ui.controllers;

import com.mycompany.checkpoint.camera.CameraService;
import com.mycompany.checkpoint.service.QRService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Controlador para la captura de cámara y escaneo de QR continuo.
 * CORREGIDO: Ya no se cierra la ventana tras la primera lectura y limpia las comillas.
 */
public class CameraController {
    private static final Logger logger = LoggerFactory.getLogger(CameraController.class);

    @FXML private ImageView cameraPreview;
    @FXML private Rectangle detectionRect;
    @FXML private Label lblQRData;
    @FXML private Label lblDetectionStatus;
    @FXML private Label lblDebug;
    @FXML private ProgressBar progressDetection;
    @FXML private Button btnCancel;

    private CameraService cameraService;
    private QRService qrService;
    private Consumer<String> onQRDetected;
    private Stage stage;

    // 🔥 Bandera de control para dar un respiro entre lecturas consecutivas (2.5 segundos)
    private long tiempoBloqueoEscaneo = 0;
    private static final long TIME_COOLDOWN_MS = 2500; 

    @FXML
    public void initialize() {
        logger.info("📷 Inicializando CameraController en Modo Continuo");

        this.cameraService = new CameraService();
        this.qrService = new QRService();

        btnCancel.setOnAction(e -> closeCamera());

        try {
            startCamera();
        } catch (Exception e) {
            logger.error("❌ Error iniciando cámara: {}", e.getMessage());
            lblQRData.setText("❌ Error: " + e.getMessage());
            lblDetectionStatus.setStyle("-fx-text-fill: #DC3C3C;");
            lblDetectionStatus.setText("Error");
            lblDebug.setText("Intenta usar modo testing o instala OpenCV");
        }
    }

    /**
     * Inicia la captura de cámara continua.
     */
    private void startCamera() {
        try {
            cameraService.startCamera(frame -> {
                if (frame == null) return;

                Platform.runLater(() -> {
                    cameraPreview.setImage(frame);

                    // Validar si estamos esperando a que se cumpla el cooldown del alumno anterior
                    if (System.currentTimeMillis() < tiempoBloqueoEscaneo) {
                        return; 
                    }

                    // Procesar frame para detectar QR
                    boolean detected = qrService.processFrame(frame, qrData -> {
                        // 🔥 Activamos el cooldown inmediatamente para no re-escanear el mismo frame en bucle
                        tiempoBloqueoEscaneo = System.currentTimeMillis() + TIME_COOLDOWN_MS;

                        // 🧼 Limpieza preventiva: Si el QR leyó comillas accidentales, se las removemos
                        String nieLimpio = qrData != null ? qrData.replace("\"", "").trim() : "";

                        logger.info("✅ QR detectado y procesado: {}", nieLimpio);
                        lblQRData.setText("✅ Buscando NIE: " + nieLimpio);
                        lblDetectionStatus.setStyle("-fx-text-fill: #228B57;");
                        lblDetectionStatus.setText("Procesando...");

                        // Invocar callback de asistencia de forma asíncrona
                        if (onQRDetected != null) {
                            onQRDetected.accept(nieLimpio);
                        }

                        // 🔥 PLAN DE RESTABLECIMIENTO AUTOMÁTICO:
                        // En lugar de llamar a closeCamera(), disparamos un temporizador para volver a dejar la cámara lista
                        new Thread(() -> {
                            try {
                                Thread.sleep(TIME_COOLDOWN_MS);
                                Platform.runLater(() -> {
                                    lblQRData.setText("📷 Esperando siguiente código QR...");
                                    lblDetectionStatus.setText("Listo");
                                    lblDetectionStatus.setStyle("-fx-text-fill: #228B57;");
                                    progressDetection.setProgress(0.0);
                                });
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });

                    // Actualizar UI con estado de detección (mientras no esté bloqueado por cooldown)
                    if (System.currentTimeMillis() >= tiempoBloqueoEscaneo) {
                        int detectionCount = qrService.getDetectionCount();
                        if (detectionCount > 0) {
                            progressDetection.setProgress(Math.min((double) detectionCount / 3, 1.0));
                            lblDebug.setText("Detecciones: " + detectionCount + "/3");

                            if (detectionCount == 1) {
                                lblDetectionStatus.setStyle("-fx-text-fill: #E67814;");
                                lblDetectionStatus.setText("Detectando...");
                            } else if (detectionCount == 2) {
                                lblDetectionStatus.setStyle("-fx-text-fill: #E67814;");
                                lblDetectionStatus.setText("Confirmando...");
                            }
                        } else {
                            progressDetection.setProgress(0.0);
                            lblDetectionStatus.setText("Listo");
                            lblDetectionStatus.setStyle("-fx-text-fill: #228B57;");
                        }
                    }

                    if (cameraService.isSimulated()) {
                        lblDebug.setText("(Modo Testing) Ejecutando simulación continua");
                    }
                });
            });

            if (cameraService.isSimulated()) {
                lblQRData.setText("📷 MODO TESTING ACTIVO");
            } else {
                lblQRData.setText("📷 Buscando códigos QR...");
            }

        } catch (RuntimeException e) {
            logger.error("❌ Error: {}", e.getMessage());
            lblQRData.setText("❌ " + e.getMessage());
            lblDetectionStatus.setStyle("-fx-text-fill: #DC3C3C;");
        }
    }

    /**
     * Cierra la cámara y la ventana (Se invoca solo de forma manual al presionar 'Cancelar').
     */
    private void closeCamera() {
        logger.info("🔌 Cerrando módulo de cámara");
        cameraService.stopCamera();

        if (stage != null) {
            stage.close();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        // 🔥 Aseguramos que si cierran la ventana desde la "X", la cámara se apague y no quede el proceso zombi
        this.stage.setOnCloseRequest(e -> closeCamera());
    }

    public void setOnQRDetected(Consumer<String> callback) {
        this.onQRDetected = callback;
    }
}