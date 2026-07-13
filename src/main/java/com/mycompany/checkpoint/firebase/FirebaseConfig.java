package com.mycompany.checkpoint.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuración e inicialización de Firebase optimizada para Firestore.
 * Incluye un sistema de respaldo para la lectura del archivo JSON de credenciales.
 */
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String CONFIG_PATH = "config/firebase-config.json";
    private static boolean initialized = false;

    /**
     * Inicializa Firebase con credenciales desde el archivo de configuración.
     */
    public static synchronized void initialize() throws IOException {
        if (initialized) {
            logger.info("Firebase ya ha sido inicializado");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("FirebaseApp ya existe en el contexto");
            initialized = true;
            return;
        }

        try {
            InputStream serviceAccount = loadConfigFile();
            
            // 🔥 ESTRATEGIA DE RESPALDO: Si el ClassLoader falla, buscamos físicamente en la raíz del proyecto
            if (serviceAccount == null) {
                logger.warn("⚠️ No se encontró el JSON en resources. Buscando en la raíz del proyecto...");
                File archivoRaiz = new File("firebase-config.json");
                if (archivoRaiz.exists()) {
                    serviceAccount = new FileInputStream(archivoRaiz);
                    logger.info("📂 Archivo 'firebase-config.json' detectado y cargado desde la raíz del proyecto.");
                }
            }

            if (serviceAccount == null) {
                throw new IllegalStateException("❌ Error Fatal: El archivo 'firebase-config.json' no se encuentra.\n" +
                        "Asegúrate de colocarlo en 'src/main/resources/config/firebase-config.json' o directamente en la raíz del proyecto.");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            // CORRECCIÓN CLAVE: El SDK Admin de Java a veces requiere explícitamente el DatabaseUrl
            // para inicializar los canales gRPC correctamente si la región no es la estándar.
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            logger.info("🟢 Firebase SDK Admin inicializado correctamente");

        } catch (IOException e) {
            logger.error("❌ Error inicializando Firebase: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Carga el archivo de configuración desde las carpetas de recursos usando dos contextos.
     */
    private static InputStream loadConfigFile() {
        // Intento 1: A través del ClassLoader del hilo de ejecución actual
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_PATH);
        if (stream == null) {
            // Intento 2: Ruta absoluta en el árbol de recursos del paquete compilado
            stream = FirebaseConfig.class.getResourceAsStream("/" + CONFIG_PATH);
        }
        return stream;
    }

    /**
     * Obtiene la referencia a la base de datos de Cloud Firestore.
     * Asegura que Firebase esté inicializado primero.
     */
    public static Firestore getDatabase() throws IOException {
        if (!initialized) {
            initialize();
        }
        return FirestoreClient.getFirestore();
    }

    /**
     * Verifica si Firebase está inicializado.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}