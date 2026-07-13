package com.mycompany.checkpoint.firebase;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de conexión con Firebase Firestore unificado para el control de estudiantes y QR.
 * SOLUCIONADO: Eliminado método inexistente setFechaHoraFormateada.
 */
public class FirebaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    private static FirebaseService instance;
    private boolean initialized = false;
    private final Firestore db;

    private FirebaseService() {
        Firestore tempDb = null;
        try {
            FirebaseConfig.initialize();
            tempDb = FirebaseConfig.getDatabase();
            this.initialized = (tempDb != null);
            
            if (this.initialized) {
                logger.info("🟢 Servicio de Firebase centralizado e inicializado correctamente.");
            }
        } catch (Exception e) {
            this.initialized = false;
            logger.error("❌ Error crítico al inicializar FirebaseService: {}", e.getMessage());
        }
        this.db = tempDb;
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    public boolean isInitialized() {
        return this.initialized && this.db != null;
    }

    /**
     * Mapea un documento de Firestore a un objeto Registro garantizando el tipo de movimiento y su hora real.
     */
    @SuppressWarnings("unchecked")
    private Registro mapearDocumentoARegistro(QueryDocumentSnapshot doc) {
        int id = doc.contains("id") && doc.getLong("id") != null ? doc.getLong("id").intValue() : 0;
        
        TipoMovimiento tipoAsignado = TipoMovimiento.ENTRADA;
        Object tipoObj = doc.get("tipo");
        if (tipoObj != null) {
            try {
                String tipoStr = tipoObj.toString().trim().toUpperCase();
                tipoAsignado = TipoMovimiento.valueOf(tipoStr);
            } catch (Exception ex) {
                logger.warn("No se pudo parsear el tipo de movimiento [{}], usando ENTRADA por defecto.", tipoObj);
            }
        }

        Estudiante est = new Estudiante();
        if (doc.contains("persona")) {
            Map<String, Object> perMap = (Map<String, Object>) doc.get("persona");
            if (perMap != null) {
                String nombre = (String) perMap.getOrDefault("nombre", perMap.get("Nombre"));
                String nie = (String) perMap.getOrDefault("nie", perMap.get("Nie"));
                if (nombre != null) est.setNombre(nombre);
                if (nie != null) est.setNie(nie);
            }
        }

        Registro registro = new Registro(id, est, tipoAsignado);

        // CORRECCIÓN CRÍTICA: Mapear la hora original de Firestore usando setFechaHora()
        String fhStr = doc.getString("fechaHoraFormateada");
        if (fhStr != null && !fhStr.trim().isEmpty()) {
            try {
                // Parsear directo usando el patrón estándar "dd/MM/yyyy HH:mm:ss"
                LocalDateTime fechaReal = LocalDateTime.parse(fhStr.trim(), FORMATTER);
                registro.setFechaHora(fechaReal);
            } catch (Exception ex) {
                logger.error("Error al mapear fechaHoraFormateada con formato estándar: {}. Reintentando fallback.", ex.getMessage());
                try {
                    String formateada = fhStr.replace(" ", "T");
                    registro.setFechaHora(LocalDateTime.parse(formateada));
                } catch (Exception fallbackEx) {
                    logger.error("Fallo definitivo al recuperar hora real del documento.");
                }
            }
        }

        if (doc.contains("tardanza") && doc.getBoolean("tardanza") != null) {
            registro.setTardanza(doc.getBoolean("tardanza"));
        } else if (doc.contains("Tardanza") && doc.getBoolean("Tardanza") != null) {
            registro.setTardanza(doc.getBoolean("Tardanza"));
        }
        
        registro.setObservaciones(""); 
        return registro;
    }

    /**
     * Escucha en tiempo real las asistencias del día actual.
     */
    public ListenerRegistration listenToTodayRegistros(Consumer<List<Registro>> callback) {
        if (!isInitialized()) {
            logger.warn("⚠️ Intentando colgar un Listener pero Firebase no está inicializado.");
            return null;
        }

        String hoy = LocalDate.now().toString(); 
        logger.info("📡 Iniciando Snapshot Listener en tiempo real para las asistencias de la fecha: [{}]", hoy);

        return db.collection("asistencias")
                 .whereEqualTo("fecha", hoy)
                 .addSnapshotListener((snapshots, e) -> {
                     if (e != null) {
                         logger.error("❌ Error en el Listener de asistencias de hoy: {}", e.getMessage());
                         return;
                     }

                     List<Registro> listaActualizada = new ArrayList<>();
                     if (snapshots != null) {
                         for (QueryDocumentSnapshot doc : snapshots) {
                             try {
                                 Registro registro = mapearDocumentoARegistro(doc);
                                 listaActualizada.add(registro);
                             } catch (Exception ex) {
                                 logger.error("Error crítico al mapear de forma segura en el Listener: {}", ex.getMessage());
                             }
                         }
                     }
                     callback.accept(listaActualizada);
                 });
    }

    public CompletableFuture<List<Estudiante>> getAllPersonas() {
        CompletableFuture<List<Estudiante>> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.completeExceptionally(new IllegalStateException("Firebase no está inicializado"));
            return future;
        }
        try {
            CollectionReference estudiantesRef = db.collection("estudiantes");
            com.google.api.core.ApiFuture<QuerySnapshot> querySnapshotFuture = estudiantesRef.get();
            
            querySnapshotFuture.addListener(() -> {
                try {
                    List<Estudiante> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshotFuture.get().getDocuments()) {
                        Estudiante e = document.toObject(Estudiante.class);
                        e.setId(document.getId());
                        lista.add(e);
                    }
                    logger.info("🔥 Cantidad de documentos descargados de Firestore: {}", lista.size());
                    future.complete(lista);
                } catch (Exception ex) {
                    logger.error("❌ Error al deserializar estudiantes de Firestore: {}", ex.getMessage());
                    future.completeExceptionally(ex);
                }
            }, Runnable::run);
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    public CompletableFuture<Estudiante> buscarPorNie(String nieScanned) {
        CompletableFuture<Estudiante> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.completeExceptionally(new IllegalStateException("Firebase no está inicializado"));
            return future;
        }
        
        String nieLimpio = nieScanned != null ? nieScanned.replaceAll("[\\\"']", "").trim() : "";
        logger.info("🔍 Iniciando búsqueda robusta en Firestore para el código QR: [{}]", nieLimpio);

        try {
            CollectionReference estudiantesRef = db.collection("estudiantes");
            com.google.api.core.ApiFuture<QuerySnapshot> future1 = estudiantesRef.whereEqualTo("Nie", nieLimpio).limit(1).get();
            
            future1.addListener(() -> {
                try {
                    List<QueryDocumentSnapshot> docs1 = future1.get().getDocuments();
                    if (!docs1.isEmpty()) {
                        completarEstudiante(docs1.get(0), future);
                        return;
                    }
                    
                    com.google.api.core.ApiFuture<QuerySnapshot> futureMin = estudiantesRef.whereEqualTo("nie", nieLimpio).limit(1).get();
                    List<QueryDocumentSnapshot> docsMin = futureMin.get().getDocuments();
                    if (!docsMin.isEmpty()) {
                        completarEstudiante(docsMin.get(0), future);
                        return;
                    }

                    try {
                        long nieNumerico = Long.parseLong(nieLimpio);
                        
                        com.google.api.core.ApiFuture<QuerySnapshot> future3 = estudiantesRef.whereEqualTo("Nie", nieNumerico).limit(1).get();
                        List<QueryDocumentSnapshot> docs3 = future3.get().getDocuments();
                        if (!docs3.isEmpty()) {
                            completarEstudiante(docs3.get(0), future);
                            return;
                        }
                        
                        com.google.api.core.ApiFuture<QuerySnapshot> future3Min = estudiantesRef.whereEqualTo("nie", nieNumerico).limit(1).get();
                        List<QueryDocumentSnapshot> docs3Min = future3Min.get().getDocuments();
                        if (!docs3Min.isEmpty()) {
                            completarEstudiante(docs3Min.get(0), future);
                            return;
                        }
                    } catch (NumberFormatException nfe) {
                        // Omitir si no es numérico
                    }

                    com.google.api.core.ApiFuture<DocumentSnapshot> futureDocId = estudiantesRef.document(nieLimpio).get();
                    DocumentSnapshot docIdSnap = futureDocId.get();
                    if (docIdSnap.exists()) {
                        Estudiante e = docIdSnap.toObject(Estudiante.class);
                        if (e != null) {
                            e.setId(docIdSnap.getId());
                            if (e.getNie() == null || e.getNie().isEmpty()) {
                                e.setNie(docIdSnap.getId());
                            }
                            logger.info("🎯 ¡Estudiante encontrado usando el NIE como ID del documento! ID: {}", docIdSnap.getId());
                            future.complete(e);
                            return;
                        }
                    }

                    logger.warn("⚠️ Código [{}] no coincide con ningún estudiante registrado en el sistema.", nieLimpio);
                    future.complete(null);

                } catch (Exception ex) {
                    logger.error("❌ Error procesando intentos de búsqueda por NIE: {}", ex.getMessage());
                    future.completeExceptionally(ex);
                }
            }, Runnable::run);

        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void completarEstudiante(QueryDocumentSnapshot doc, CompletableFuture<Estudiante> future) {
        Estudiante e = doc.toObject(Estudiante.class);
        e.setId(doc.getId());
        logger.info("🎯 ¡Estudiante encontrado con éxito! ID de documento: {}", doc.getId());
        future.complete(e);
    }

    public CompletableFuture<Void> addPersona(Estudiante e) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.completeExceptionally(new IllegalStateException("Firebase no está inicializado"));
            return future;
        }
        try {
            db.collection("estudiantes")
              .document(e.getNie())
              .set(e)
              .addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception ex) {
            logger.error("Error al intentar guardar estudiante en Firestore: {}", ex.getMessage());
            future.completeExceptionally(ex);
        }
        return future;
    }

    public CompletableFuture<Void> addRegistro(Registro r) { 
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.completeExceptionally(new IllegalStateException("Firebase no está inicializado"));
            return future;
        }
        try {
            Map<String, Object> datosAsistencia = new HashMap<>();
            datosAsistencia.put("id", r.getId());
            datosAsistencia.put("persona", r.getPersona());
            datosAsistencia.put("tipo", r.getTipo().name()); 
            datosAsistencia.put("observaciones", "");
            datosAsistencia.put("tardanza", r.isTardanza());
            datosAsistencia.put("fechaHoraFormateada", r.getFechaHoraFormateada());
            
            String fechaHoy = LocalDate.now().toString();
            datosAsistencia.put("fecha", fechaHoy);

            db.collection("asistencias")
              .add(datosAsistencia)
              .addListener(() -> future.complete(null), Runnable::run);
        } catch (Exception ex) {
            logger.error("❌ Error al guardar asistencia en Firestore: {}", ex.getMessage());
            future.completeExceptionally(ex);
        }
        return future;
    }

    public CompletableFuture<List<Registro>> getTodayRegistros() { 
        CompletableFuture<List<Registro>> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.complete(new ArrayList<>());
            return future;
        }
        String hoy = LocalDate.now().toString();
        com.google.api.core.ApiFuture<QuerySnapshot> apiFuture = db.collection("asistencias").whereEqualTo("fecha", hoy).get();
        
        apiFuture.addListener(() -> {
            try {
                List<Registro> lista = new ArrayList<>();
                for (QueryDocumentSnapshot doc : apiFuture.get().getDocuments()) {
                    lista.add(mapearDocumentoARegistro(doc));
                }
                future.complete(lista);
            } catch(Exception ex) {
                logger.error("❌ Error en getTodayRegistros asíncrono: {}", ex.getMessage());
                future.completeExceptionally(ex);
            }
        }, Runnable::run);
        return future; 
    }
    
    public CompletableFuture<List<Registro>> getAllRegistros() { 
        CompletableFuture<List<Registro>> future = new CompletableFuture<>();
        if (!isInitialized()) {
            future.complete(new ArrayList<>());
            return future;
        }
        com.google.api.core.ApiFuture<QuerySnapshot> apiFuture = db.collection("asistencias").get();
        
        apiFuture.addListener(() -> {
            try {
                List<Registro> lista = new ArrayList<>();
                for (QueryDocumentSnapshot doc : apiFuture.get().getDocuments()) {
                    lista.add(mapearDocumentoARegistro(doc));
                }
                future.complete(lista);
            } catch(Exception ex) {
                logger.error("❌ Error en getAllRegistros asíncrono: {}", ex.getMessage());
                future.completeExceptionally(ex);
            }
        }, Runnable::run);
        return future;
    }
}