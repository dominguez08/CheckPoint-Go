package com.mycompany.checkpoint.service;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

/**
 * Servicio encargado de generar, guardar y leer códigos QR utilizando la librería ZXing nativa.
 */
public class QRService {

    private int detectionCount = 0;

    /**
     * Genera una imagen QR nativa de JavaFX usando el NIE del estudiante y la guarda en el disco duro.
     */
    public static Image generarCodigoQR(String nie, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(nie, BarcodeFormat.QR_CODE, width, height);
        
        WritableImage wr = new WritableImage(width, height);
        PixelWriter pw = wr.getPixelWriter();
        
        // 1. Creamos en paralelo un BufferedImage nativo de Java AWT para guardarlo en disco
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Renderizar la matriz de bytes del QR a ambas imágenes
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean isBlack = bitMatrix.get(x, y);
                int color = isBlack ? 0xFF000000 : 0xFFFFFFFF;
                int awtColor = isBlack ? 0x000000 : 0xFFFFFF;

                pw.setArgb(x, y, color); // Para la UI de JavaFX
                bufferedImage.setRGB(x, y, awtColor); // Para guardar en disco
            }
        }

        // 2. Lógica de almacenamiento directo sin depender del módulo javafx.swing
        try {
            File carpetaDestino = new File("codigos_qr");
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            File archivoSalida = new File(carpetaDestino, "qr_" + nie + ".png");
            ImageIO.write(bufferedImage, "png", archivoSalida);
            
            System.out.println("💾 Código QR guardado exitosamente en: " + archivoSalida.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error al guardar el archivo QR en el disco: " + e.getMessage());
        }

        return wr;
    }

    /**
     * Procesa un fotograma de la cámara leyendo los píxeles directamente.
     */
    public boolean processFrame(Image fxImage, Consumer<String> onQrDetected) {
        if (fxImage == null) return false;

        try {
            int width = (int) fxImage.getWidth();
            int height = (int) fxImage.getHeight();
            
            if (width <= 0 || height <= 0) return false;

            int[] buffer = new int[width * height];
            PixelReader pr = fxImage.getPixelReader();
            pr.getPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), buffer, 0, width);

            LuminanceSource source = new RGBLuminanceSource(width, height, buffer);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            
            if (result != null && result.getText() != null) {
                detectionCount++;
                onQrDetected.accept(result.getText());
                return true;
            }
        } catch (NotFoundException e) {
            // No hay QR en este fotograma
        } catch (Exception e) {
            // Manejo de excepciones generales
        }
        return false;
    }

    /**
     * Devuelve el número total de detecciones exitosas realizadas.
     */
    public int getDetectionCount() {
        return this.detectionCount;
    }
}