package com.mycompany.checkpoint.ui.controllers;

import com.mycompany.checkpoint.firebase.FirebaseService;
import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import com.mycompany.checkpoint.ui.stages.CameraStage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador corregido para el registro de asistencias.
 * SOLUCIONADO: Conteo independiente de asistencias para alternar Entrada/Salida de forma infalible.
 */
public class RegistroController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(RegistroController.class);

    @FXML private TextField txtIdPersona;
    @FXML private ComboBox<TipoMovimiento> cmbTipo;
    @FXML private Button btnCamara;
    @FXML private Button btnRegistrar;
    @FXML private Label lblEstado;
    @FXML private Label lblSyncStatus;

    @FXML private TableView<Registro> tableRegistros;
    @FXML private TableColumn<Registro, String> colPersona;
    @FXML private TableColumn<Registro, String> colID;
    @FXML private TableColumn<Registro, String> colTipo;
    @FXML private TableColumn<Registro, String> colFechaHora;
    @FXML private TableColumn<Registro, Boolean> colTardanza;

    private FirebaseService firebaseService;
    private final List<Estudiante> personas = new ArrayList<>();
    private final ObservableList<Registro> registros = FXCollections.observableArrayList();
    
    // Lista de respaldo para asegurar el conteo de movimientos del día
    private final List<Registro> historialDelDia = new ArrayList<>();

    @FXML
    public void initialize() {
        logger.info("Inicializando RegistroController");

        this.firebaseService = FirebaseService.getInstance();

        // Configurar ComboBox
        cmbTipo.setItems(FXCollections.observableArrayList(TipoMovimiento.values()));
        cmbTipo.setValue(TipoMovimiento.ENTRADA);

        // Configurar columnas de la tabla (Sin observaciones)
        setupTableColumns();
        tableRegistros.setItems(registros);

        // Configurar eventos
        btnCamara.setOnAction(e -> openCamera());
        btnRegistrar.setOnAction(e -> registrarMovimiento());

        // Cargar alumnos e historial
        cargarDatos();
    }

    private void setupTableColumns() {
        colPersona.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPersona() != null ? cellData.getValue().getPersona().getNombre() : "Desconocido"));

        colID.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPersona() != null ? cellData.getValue().getPersona().getNie() : ""));

        colTipo.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTipo() != null ? cellData.getValue().getTipo().getDescripcion() : ""));

        colFechaHora.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFechaHoraFormateada()));

        colTardanza.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isTardanza()));
    }

    @FXML
    private void openCamera() {
        CameraStage.showAndWait(qrData -> {
            if (qrData != null && !qrData.trim().isEmpty()) {
                Platform.runLater(() -> {
                    String nieEscaneado = qrData.trim();
                    txtIdPersona.setText(nieEscaneado);
                    registrarMovimiento();
                });
            }
        });
    }

    @FXML
    private void registrarMovimiento() {
        try {
            String niePersona = txtIdPersona.getText().trim();

            if (niePersona.isEmpty()) {
                mostrarError("Por favor ingresa o escanea un NIE de estudiante");
                return;
            }

            Estudiante persona = personas.stream()
                    .filter(p -> p.getNie() != null && p.getNie().equalsIgnoreCase(niePersona))
                    .findFirst()
                    .orElse(null);

            if (persona == null) {
                mostrarError("Estudiante con NIE '" + niePersona + "' no encontrado");
                return;
            }

            if (persona.getEstado() != null && !persona.getEstado().equalsIgnoreCase("Activo")) {
                mostrarError("El estudiante " + persona.getNombre() + " está inactivo");
                return;
            }

            // --- CORRECCIÓN CRÍTICA: Conteo robusto usando el historial global del día ---
            long conteoEntradas = historialDelDia.stream()
                    .filter(r -> r.getPersona() != null && r.getPersona().getNie().equalsIgnoreCase(niePersona))
                    .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA)
                    .count();

            long conteoSalidas = historialDelDia.stream()
                    .filter(r -> r.getPersona() != null && r.getPersona().getNie().equalsIgnoreCase(niePersona))
                    .filter(r -> r.getTipo() == TipoMovimiento.SALIDA)
                    .count();

            // Si las entradas son mayores que las salidas, obligatoriamente le toca salir
            TipoMovimiento tipoAsignado = (conteoEntradas > conteoSalidas) ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA;
            cmbTipo.setValue(tipoAsignado);

            // Generar nuevo registro único (El constructor toma la hora exacta de este milisegundo)
            Registro registro = new Registro(System.identityHashCode(new Object()), persona, tipoAsignado);
            registro.setObservaciones(""); // Vacío por defecto

            // Validar Tardanza
            if (tipoAsignado == TipoMovimiento.ENTRADA && registro.getFechaHoraFormateada() != null) {
                try {
                    String fh = registro.getFechaHoraFormateada();
                    if (fh.length() >= 13) {
                        int hora = Integer.parseInt(fh.substring(11, 13));
                        if (hora >= 7) { 
                            registro.setTardanza(true);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error al evaluar tardanza.");
                }
            }

            // Actualizar ambas estructuras locales inmediatamente
            registros.add(0, registro);
            historialDelDia.add(registro);

            // Subir a Firebase
            if (firebaseService != null && firebaseService.isInitialized()) {
                firebaseService.addRegistro(registro);
            }

            txtIdPersona.clear();
            mostrarExito("✅ Registrado: " + persona.getNombre() + " (" + tipoAsignado.getDescripcion() + ")");

        } catch (Exception e) {
            logger.error("Error al registrar: {}", e.getMessage());
            mostrarError("Error: " + e.getMessage());
        }
    }

    private void cargarDatos() {
        personas.clear();
        registros.clear();
        historialDelDia.clear();
        
        if (firebaseService != null && firebaseService.isInitialized()) {
            lblSyncStatus.setText("🔄 Sincronizando base de datos...");
            
            firebaseService.getAllPersonas().thenAccept(listaEstudiantes -> {
                Platform.runLater(() -> {
                    if (listaEstudiantes != null) {
                        personas.addAll(listaEstudiantes);
                        lblSyncStatus.setText("🟢 Base de datos en línea (" + personas.size() + " alumnos)");
                        lblSyncStatus.setStyle("-fx-text-fill: #228B57; -fx-font-weight: bold;");
                    }
                });
            });

            // Escuchar cambios en tiempo real e introducirlos al historial de control
            firebaseService.listenToTodayRegistros(listaDeRegistros -> {
                Platform.runLater(() -> {
                    if (listaDeRegistros != null) {
                        registros.clear();
                        historialDelDia.clear();
                        
                        // Guardamos todo en nuestro registro estático de control
                        historialDelDia.addAll(listaDeRegistros);
                        
                        // Mostramos en la tabla ordenando del más nuevo al más antiguo
                        listaDeRegistros.stream()
                                .sorted((r1, r2) -> {
                                    String f1 = r1.getFechaHoraFormateada() != null ? r1.getFechaHoraFormateada() : "";
                                    String f2 = r2.getFechaHoraFormateada() != null ? r2.getFechaHoraFormateada() : "";
                                    return f2.compareTo(f1);
                                })
                                .forEach(registros::add);
                    }
                });
            });
        }
    }

    private void mostrarExito(String mensaje) {
        lblEstado.setText(mensaje);
        lblEstado.setStyle("-fx-text-fill: #228B57; -fx-font-weight: bold;");
    }

    private void mostrarError(String mensaje) {
        lblEstado.setText("❌ " + mensaje);
        lblEstado.setStyle("-fx-text-fill: #DC3C3C; -fx-font-weight: bold;");
    }

    public void actualizarSyncStatus(boolean online) {}
}