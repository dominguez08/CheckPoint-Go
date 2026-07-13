package com.mycompany.checkpoint;

import com.mycompany.checkpoint.firebase.FirebaseService;
import com.mycompany.checkpoint.util.SyncManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Aplicación JavaFX para CheckPoint Go.
 * Inicializa de forma segura el SDK de Firebase Admin y arranca desde la pantalla de Login.
 */
public class JavaFXApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(JavaFXApplication.class);
    
    private FirebaseService firebaseService;
    private SyncManager syncManager;

    @Override
    public void init() throws Exception {
        logger.info("Inicializando servicios en segundo plano...");
        try {
            // 1. Inicializar el SDK de Firebase si no se ha hecho antes
            if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                
                // Apuntar directamente a la carpeta config y nombre de tu archivo JSON
                File jsonClave = new File("config/firebase-config.json");
                
                // Intento de respaldo si se ejecuta desde otra ruta de compilación
                if (!jsonClave.exists()) {
                    jsonClave = new File("src/main/resources/config/firebase-config.json");
                }

                if (!jsonClave.exists()) {
                    throw new java.io.FileNotFoundException(
                        "❌ No se encontró el archivo firebase-config.json en la carpeta config.");
                }

                logger.info("🔥 Archivo de credenciales de Firebase encontrado en: " + jsonClave.getAbsolutePath());

                com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseOptions.builder()
                    .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(new FileInputStream(jsonClave)))
                    .build();

                com.google.firebase.FirebaseApp.initializeApp(options);
                logger.info("🔥 ¡Firebase SDK conectado con éxito globalmente!");
            }

            // 2. Crear las instancias ahora que el SDK ya está activo

            FirebaseService firebaseService = FirebaseService.getInstance();
            this.syncManager = new SyncManager(this.firebaseService);
            
            logger.info("SyncManager y FirebaseService preparados correctamente.");
        } catch (Exception e) {
            logger.error("⚠️ Error crítico al conectar a Firebase: {}. Entrando en Modo Local.", e.getMessage());
            this.firebaseService = null;
            this.syncManager = null;
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            logger.info("Iniciando CheckPointGo - Cargando pantalla de autenticación...");

            // Apuntamos al archivo LoginView.fxml que creamos en tu carpeta de recursos
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mycompany/checkpoint/fxml/LoginView.fxml.xml"));
            javafx.scene.Parent root = loader.load();

            // Una ventana un poco más compacta ideal para el cuadro de login
            Scene scene = new Scene(root, 450, 500);

            // Cargamos tus estilos CSS globales si los requiere el Login
            try {
                String css = getClass().getResource("/com/mycompany/checkpoint/styles/main.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception cssEx) {
                logger.warn("No se pudo cargar el archivo main.css para el login: {}", cssEx.getMessage());
            }

            primaryStage.setTitle("CheckPoint Go - Iniciar Sesión");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Evitamos que maximicen la pantalla de login
            primaryStage.centerOnScreen();

            primaryStage.show();
            logger.info("Pantalla de inicio de sesión desplegada.");

        } catch (Exception e) {
            logger.error("Error iniciando aplicación en el Login: {}", e.getMessage(), e);
            throw new RuntimeException("Error iniciando aplicación", e);
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Cerrando CheckPointGo y deteniendo hilos de sincronización...");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}