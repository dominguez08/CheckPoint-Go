package com.mycompany.checkpoint.camera;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Servicio para captura de cámara usando OpenCV.
 * Proporciona frames en tiempo real a través de callbacks.
 * Si OpenCV no está disponible, usa modo simulado.
 */
public class CameraService {
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);
    private static final int FPS = 30;
    private static final int FRAME_DELAY_MS = 1000 / FPS;

    private static boolean OPENCV_AVAILABLE = false;

    static {
        try {
            nu.pattern.OpenCV.loadShared();
            OPENCV_AVAILABLE = true;
            logger.info("✅ OpenCV (OpenPnP) cargado exitosamente");
        } catch (Exception | UnsatisfiedLinkError e) {
            logger.warn("⚠️ OpenCV no disponible: {}", e.getMessage());
            logger.info("ℹ️ Usando modo simulado para testing");
        }
    }

    private Object camera; // VideoCapture (si OpenCV está disponible)
    private ExecutorService executor;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean useSimulatedCamera;

    /**
     * Inicia la captura de cámara e invoca callback para cada frame.
     * Los frames se procesan en thread separado para no bloquear UI.
     *
     * @param frameCallback Consumer que recibe cada frame como Image JavaFX
     * @throws RuntimeException si la cámara no puede abrirse y OpenCV no está disponible
     */
    public void startCamera(Consumer<Image> frameCallback) {
        if (isRunning.getAndSet(true)) {
            logger.warn("La cámara ya está en ejecución");
            return;
        }

        if (OPENCV_AVAILABLE) {
            startRealCamera(frameCallback);
        } else {
            useSimulatedCamera = true;
            startSimulatedCamera(frameCallback);
        }
    }

    /**
     * Inicia captura real con OpenCV.
     */
    private void startRealCamera(Consumer<Image> frameCallback) {
        try {
            // Usar reflexión para evitar compilación dependiente de OpenCV
            Class<?> videoCaptureClass = Class.forName("org.opencv.videoio.VideoCapture");
            camera = videoCaptureClass.getConstructor(int.class).newInstance(0);

            boolean isOpened = (boolean) videoCaptureClass.getMethod("isOpened").invoke(camera);
            if (!isOpened) {
                isRunning.set(false);
                throw new RuntimeException("No se pudo abrir la cámara. Verifica permisos.");
            }

            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CameraThread");
                t.setDaemon(true);
                return t;
            });

            executor.submit(() -> captureFramesReal(frameCallback));
            logger.info("✅ Cámara real iniciada");

        } catch (Exception e) {
            isRunning.set(false);
            logger.error("Error iniciando cámara real: {}", e.getMessage());
            throw new RuntimeException("Error al abrir cámara: " + e.getMessage(), e);
        }
    }

    /**
     * Loop de captura real con OpenCV.
     */
    private void captureFramesReal(Consumer<Image> frameCallback) {
        try {
            Class<?> matClass = Class.forName("org.opencv.core.Mat");
            Object frame = matClass.getConstructor().newInstance();

            Class<?> videoCaptureClass = Class.forName("org.opencv.videoio.VideoCapture");

            while (isRunning.get()) {
                try {
                    boolean hasFrame = (boolean) videoCaptureClass.getMethod("read", matClass).invoke(camera, frame);

                    if (hasFrame) {
                        Image image = matToImage(frame);
                        Platform.runLater(() -> frameCallback.accept(image));
                    }

                    Thread.sleep(FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error procesando frame: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error en captura real: {}", e.getMessage());
        }
    }

    /**
     * Inicia captura simulada (para testing sin OpenCV/cámara).
     */
    private void startSimulatedCamera(Consumer<Image> frameCallback) {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SimulatedCameraThread");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> {
            int frameCount = 0;
            while (isRunning.get()) {
                try {
                    Image frame = generateTestFrame(frameCount++);
                    Platform.runLater(() -> frameCallback.accept(frame));
                    Thread.sleep(FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        logger.info("📷 Cámara simulada iniciada (modo testing)");
    }

    /**
     * Genera un frame de prueba con patrón.
     */
    private Image generateTestFrame(int frameCount) {
        WritableImage image = new WritableImage(640, 480);
        var pixelWriter = image.getPixelWriter();

        for (int y = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                // Patrón de prueba: gradiente + animación
                int r = (x + frameCount) % 256;
                int g = (y + frameCount) % 256;
                int b = ((x + y) / 2) % 256;

                Color color = Color.color(r / 255.0, g / 255.0, b / 255.0);
                pixelWriter.setColor(x, y, color);
            }
        }

        return image;
    }

    /**
     * Convierte una matriz OpenCV Mat a una imagen JavaFX (usando reflexión).
     */
    private Image matToImage(Object mat) {
        try {
            // Obtener ancho y alto
            int width = (int) mat.getClass().getMethod("width").invoke(mat);
            int height = (int) mat.getClass().getMethod("height").invoke(mat);
            int channels = (int) mat.getClass().getMethod("channels").invoke(mat);

            WritableImage image = new WritableImage(width, height);

            if (channels == 3) {
                return colorMatToImage(mat, width, height);
            } else {
                return new WritableImage(width, height);
            }
        } catch (Exception e) {
            logger.warn("Error convirtiendo Mat a Image: {}", e.getMessage());
            return new WritableImage(640, 480);
        }
    }

    /**
     * Convierte imagen OpenCV a color (BGR).
     */
    private Image colorMatToImage(Object mat, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        try {
            byte[] data = new byte[width * height * 3];
            mat.getClass().getMethod("get", int.class, int.class, byte[].class)
                    .invoke(mat, 0, 0, data);

            var pixelWriter = image.getPixelWriter();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = (y * width + x) * 3;
                    int b = data[idx] & 0xFF;
                    int g = data[idx + 1] & 0xFF;
                    int r = data[idx + 2] & 0xFF;
                    int argb = (255 << 24) | (r << 16) | (g << 8) | b;
                    pixelWriter.setArgb(x, y, argb);
                }
            }
        } catch (Exception e) {
            logger.warn("Error en conversión de color: {}", e.getMessage());
        }
        return image;
    }

    /**
     * Detiene la captura de cámara y libera recursos.
     */
    public void stopCamera() {
        isRunning.set(false);

        if (camera != null && OPENCV_AVAILABLE) {
            try {
                camera.getClass().getMethod("release").invoke(camera);
                logger.info("✅ Cámara liberada");
            } catch (Exception e) {
                logger.warn("Error liberando cámara: {}", e.getMessage());
            }
        }

        if (executor != null) {
            executor.shutdownNow();
            logger.info("Thread de cámara detenido");
        }
    }

    /**
     * Verifica si la cámara está activa.
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Verifica si se está usando modo simulado.
     */
    public boolean isSimulated() {
        return useSimulatedCamera;
    }
}

