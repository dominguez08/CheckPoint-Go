package com.mycompany.checkpoint.ui.controllers;

import com.mycompany.checkpoint.firebase.FirebaseService;
import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.service.ExcelImportService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class PersonasController extends BaseController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(PersonasController.class);

    @FXML private TextField txtIdPersona; 
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cmbAno;
    @FXML private ComboBox<String> cmbSeccion;
    @FXML private ComboBox<String> cmbSexo;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private ComboBox<String> cmbEspecialidad; 
    @FXML private Button btnAgregar;
    @FXML private Button btnImportarExcel; 
    @FXML private Label lblEstado;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Estudiante> tablePersonas;
    @FXML private TableColumn<Estudiante, String> colID; 
    @FXML private TableColumn<Estudiante, String> colNombre;
    @FXML private TableColumn<Estudiante, String> colEmail;
    @FXML private TableColumn<Estudiante, String> colAno;
    @FXML private TableColumn<Estudiante, String> colSeccion;
    @FXML private TableColumn<Estudiante, String> colSexo;
    @FXML private TableColumn<Estudiante, String> colEstado;
    @FXML private TableColumn<Estudiante, String> colEspecialidad; 

    private FirebaseService firebaseService;
    private final ExcelImportService excelImportService = new ExcelImportService();
    private final ObservableList<Estudiante> personasList = FXCollections.observableArrayList();
    private FilteredList<Estudiante> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Inicializando PersonasController");

        if (cmbAno != null) {
            cmbAno.setItems(FXCollections.observableArrayList("1° Año", "2° Año", "3° Año"));
            cmbAno.setValue("1° Año");
        }
        if (cmbSeccion != null) {
            cmbSeccion.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
            cmbSeccion.setValue("A");
        }
        if (cmbSexo != null) {
            cmbSexo.setItems(FXCollections.observableArrayList("Masculino", "Femenino"));
            cmbSexo.setValue("Masculino");
        }
        if (cmbEstado != null) {
            cmbEstado.setItems(FXCollections.observableArrayList("Activo", "Inactivo"));
            cmbEstado.setValue("Activo");
        }
        if (cmbEspecialidad != null) {
            cmbEspecialidad.setItems(FXCollections.observableArrayList(
                "Desarrollo de Software", 
                "Infraestructura Tecnológica", 
                "Servicios Informáticos", 
                "Mecánica Industrial", 
                "Mantenimiento Automotriz", 
                "Electrónica", 
                "Sistemas Eléctricos"
            ));
            cmbEspecialidad.setValue("Desarrollo de Software");
        }

        setupTableColumns();
        
        filteredData = new FilteredList<>(personasList, p -> true);
        tablePersonas.setItems(filteredData);
        
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(persona -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (persona.getNombre() != null && persona.getNombre().toLowerCase().contains(lowerCaseFilter)) return true;
                if (persona.getEspecialidad() != null && persona.getEspecialidad().toLowerCase().contains(lowerCaseFilter)) return true;
                return persona.getNie() != null && persona.getNie().toLowerCase().contains(lowerCaseFilter);
            });
        });

        btnAgregar.setOnAction(e -> agregarPersona());

        this.firebaseService = FirebaseService.getInstance();
        cargarDatos();
    }

    @Override
    public void onViewShown() {
        cargarDatos();
    }

    private void setupTableColumns() {
        if (colID != null) colID.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNie()));
        if (colNombre != null) colNombre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        if (colEmail != null) colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCorreo()));
        
        // 🎯 SOLUCIÓN INMUNE A FALLOS DE REFLEXIÓN: Extrae directamente el String evaluando todas las variantes posibles
        if (colAno != null) {
            colAno.setCellValueFactory(cellData -> {
                Estudiante est = cellData.getValue();
                String txtAno = "";
                if (est != null) {
                    if (est.getAnio() != null && !est.getAnio().isEmpty()) {
                        txtAno = est.getAnio();
                    } else if (est.getAño() != null && !est.getAño().isEmpty()) {
                        txtAno = est.getAño();
                    }
                }
                return new javafx.beans.property.SimpleStringProperty(txtAno);
            });
        }
        
        if (colSeccion != null) colSeccion.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSeccion()));
        if (colSexo != null) colSexo.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSexo()));
        if (colEstado != null) colEstado.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado()));
        if (colEspecialidad != null) colEspecialidad.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEspecialidad()));
    }

    @FXML
    private void handleImportarExcel() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar listado de alumnos (Excel)");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivos de Excel (*.xlsx)", "*.xlsx"));

        javafx.stage.Stage stage = (javafx.stage.Stage) btnImportarExcel.getScene().getWindow();
        File archivoExcel = fileChooser.showOpenDialog(stage);

        if (archivoExcel != null) {
            btnImportarExcel.setDisable(true);
            mostrarStatusLocal("Procesando Excel e importando alumnos...");

            new Thread(() -> {
                try {
                    excelImportService.importarAlumnosAFirestore(archivoExcel);
                    Platform.runLater(() -> {
                        btnImportarExcel.setDisable(false);
                        mostrarStatusLocal("✅ Listado importado correctamente.");
                        cargarDatos();
                    });
                } catch (Exception e) {
                    logger.error("Error al importar Excel: ", e);
                    Platform.runLater(() -> {
                        btnImportarExcel.setDisable(false);
                        mostrarStatusLocal("❌ Error: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void agregarPersona() {
        String nie = txtIdPersona.getText().trim(); 
        String nombre = txtNombre.getText().trim();
        String email = txtEmail.getText().trim();
        String ano = cmbAno.getValue();
        String seccion = cmbSeccion.getValue();
        String sexo = cmbSexo.getValue();
        String estado = cmbEstado.getValue();
        String especialidad = cmbEspecialidad != null ? cmbEspecialidad.getValue() : "General"; 

        if (nie.isEmpty() || nombre.isEmpty() || email.isEmpty()) {
            mostrarStatusLocal("❌ Completa todos los campos");
            return;
        }

        Estudiante nuevaPersona = new Estudiante(nie, nombre, email, ano, seccion, estado, sexo, especialidad);
        btnAgregar.setDisable(true);

        if (firebaseService == null || !firebaseService.isInitialized()) {
            generarYGuardarArchivoQR(nie);
            personasList.add(nuevaPersona);
            limpiarFormulario();
            btnAgregar.setDisable(false);
            mostrarStatusLocal("✅ Guardado local (Sin conexión)");
            return;
        }

        firebaseService.addPersona(nuevaPersona).thenAccept(v -> {
            Platform.runLater(() -> {
                generarYGuardarArchivoQR(nie);
                limpiarFormulario();
                btnAgregar.setDisable(false);
                mostrarStatusLocal("✅ Sincronizado en Firebase.");
                cargarDatos(); 
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                btnAgregar.setDisable(false);
                mostrarStatusLocal("❌ Error Firebase: " + e.getMessage());
            });
            return null;
        });
    }

    private void generarYGuardarArchivoQR(String nie) {
        try {
            File directorio = new File("qrcodes");
            if (!directorio.exists()) directorio.mkdirs();
            String rutaArchivo = "qrcodes" + File.separator + nie + ".png";
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(nie, BarcodeFormat.QR_CODE, 350, 350);
            Path path = FileSystems.getDefault().getPath(rutaArchivo);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (Exception ex) {
            logger.error("Error al exportar el QR: {}", ex.getMessage());
        }
    }

    private void cargarDatos() {
        if (firebaseService == null || !firebaseService.isInitialized()) {
            Platform.runLater(() -> {
                personasList.clear();
                personasList.add(new Estudiante("5060636", "Carlos Mendoza", "carlos.m@mail.com", "1° Año", "A", "Activo", "Masculino", "Desarrollo de Software"));
            });
            return;
        }

        firebaseService.getAllPersonas().thenAccept(personas -> {
            Platform.runLater(() -> {
                personasList.clear();
                if (personas != null && !personas.isEmpty()) {
                    personasList.addAll(personas);
                }
            });
        }).exceptionally(e -> {
            logger.error("Fallo al obtener documentos de Firestore: {}", e.getMessage());
            return null;
        });
    }

    private void limpiarFormulario() {
        txtIdPersona.clear();
        txtNombre.clear();
        txtEmail.clear();
        if (cmbEspecialidad != null) cmbEspecialidad.setValue("Desarrollo de Software");
    }

    private void mostrarStatusLocal(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
        }
    }
}