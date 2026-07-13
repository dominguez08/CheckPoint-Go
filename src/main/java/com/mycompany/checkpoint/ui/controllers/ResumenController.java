package com.mycompany.checkpoint.ui.controllers;

import com.google.cloud.firestore.ListenerRegistration;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.checkpoint.firebase.FirebaseService;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ResumenController extends BaseController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ResumenController.class);

    @FXML private Label lblEntradasNum;
    @FXML private Label lblSalidasNum;
    @FXML private Label lblPresentesNum;
    @FXML private Label lblTardanzasNum;
    @FXML private Label lblSyncStatus;
    @FXML private Label lblLastSync; 
    @FXML private ListView<String> listHistorialVivo;

    @FXML private VBox cardEntradas;
    @FXML private VBox cardSalidas;
    @FXML private VBox cardPresentes;
    @FXML private VBox cardRetrasos;

    @FXML private DashboardController dashboardController; 

    private FirebaseService firebaseService;
    private ListenerRegistration firestoreListener;
    private Timeline relojTimeline;
    private final ObservableList<String> logsEnVivo = FXCollections.observableArrayList();
    
    private List<Registro> listaRegistrosHoy = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logger.info("Inicializando estructura de ResumenController.");
        if (listHistorialVivo != null) {
            listHistorialVivo.setItems(logsEnVivo);
        }
        this.firebaseService = FirebaseService.getInstance();
        iniciarRelojEnVivo();
    }

    @Override
    public void onViewShown() {
        logger.info("Vista Resumen activa: Conectando Listener en Tiempo Real.");
        if (listHistorialVivo != null && listHistorialVivo.getItems() != logsEnVivo) {
            listHistorialVivo.setItems(logsEnVivo);
        }
        conectarListenerTiempoReal();
    }

    @Override
    public void onViewHidden() {
        logger.info("Vista Resumen oculta: Pausando Listener.");
        destruirListener();
    }

    public void setRegistrosHoy(List<Registro> registros) {
        this.listaRegistrosHoy = registros != null ? registros : new ArrayList<>();
    }

    private void iniciarRelojEnVivo() {
        DateTimeFormatter formateador = DateTimeFormatter.ofPattern("HH:mm:ss");
        relojTimeline = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            String horaActual = LocalTime.now().format(formateador);
            if (lblSyncStatus != null && lblSyncStatus.getText() != null && lblSyncStatus.getText().contains("🟢")) {
                lblSyncStatus.setText("🟢 Monitoreo en Vivo (" + horaActual + ")");
            }
        }));
        relojTimeline.setCycleCount(Animation.INDEFINITE);
        relojTimeline.play();
    }

    private void conectarListenerTiempoReal() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        if (firebaseService == null || !firebaseService.isInitialized()) {
            arrancarSimuladorPasivo();
            return;
        }
        firestoreListener = firebaseService.listenToTodayRegistros(listaDeRegistros -> {
            Platform.runLater(() -> procesarYRefrescarTodo(listaDeRegistros));
        });
    }

    private void procesarYRefrescarTodo(List<Registro> registros) {
        if (registros == null) return;

        logger.info("Procesando " + registros.size() + " registros recibidos de Firebase.");
        setRegistrosHoy(registros);

        // Agrupación por alumno para resolver duplicados y desajustes visuales
        Map<String, List<Registro>> porAlumno = registros.stream()
                .filter(r -> r.getPersona() != null && r.getPersona().getNie() != null)
                .collect(Collectors.groupingBy(r -> r.getPersona().getNie()));

        long entradas = registros.stream().filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || 
            (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString()))).count();
            
        long salidas = registros.stream().filter(r -> r.getTipo() == TipoMovimiento.SALIDA || 
            (r.getTipo() != null && "SALIDA".equalsIgnoreCase(r.getTipo().toString()))).count();
            
        long tardanzas = registros.stream().filter(Registro::isTardanza).count();

        // Cálculo exacto de Presentes evaluando el estado del alumno (Entradas > Salidas)
        long presentesReales = porAlumno.values().stream()
                .filter(list -> {
                    long ent = list.stream().filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString()))).count();
                    long sal = list.stream().filter(r -> r.getTipo() == TipoMovimiento.SALIDA || (r.getTipo() != null && "SALIDA".equalsIgnoreCase(r.getTipo().toString()))).count();
                    return ent > sal;
                }).count();

        Platform.runLater(() -> {
            if (lblEntradasNum != null) lblEntradasNum.setText(String.valueOf(entradas));
            if (lblSalidasNum != null) lblSalidasNum.setText(String.valueOf(salidas));
            if (lblPresentesNum != null) lblPresentesNum.setText(String.valueOf(presentesReales));
            if (lblTardanzasNum != null) lblTardanzasNum.setText(String.valueOf(tardanzas));
            
            if (dashboardController != null) {
                dashboardController.actualizarGraficaSemanal(registros);
            }

            if (listHistorialVivo != null) {
                logsEnVivo.clear();
                registros.stream()
                    .sorted((r1, r2) -> {
                        String fh1 = r1.getFechaHoraFormateada() != null ? r1.getFechaHoraFormateada() : "";
                        String fh2 = r2.getFechaHoraFormateada() != null ? r2.getFechaHoraFormateada() : "";
                        return fh2.compareTo(fh1); 
                    })
                    .limit(12)
                    .forEach(r -> {
                        String hora = "--:--";
                        if (r.getFechaHoraFormateada() != null && r.getFechaHoraFormateada().length() >= 16) {
                            hora = r.getFechaHoraFormateada().substring(11, 16);
                        }
                        boolean esEntrada = r.getTipo() == TipoMovimiento.ENTRADA || 
                                           (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString()));
                        String icono = esEntrada ? "🔹" : "🔸";
                        String tagTardanza = r.isTardanza() ? " [RETRASO]" : "";
                        String nombreEstudiante = r.getPersona() != null ? r.getPersona().getNombre() : "Usuario Desconocido";
                        logsEnVivo.add(hora + " " + icono + " " + nombreEstudiante + tagTardanza);
                    });
            }
            String horaChange = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (lblLastSync != null) lblLastSync.setText("Último evento detectado: " + horaChange);
        });
    }

    // ==========================================
    // CAPTURA DE EVENTOS Y CRUCE DE DATOS
    // ==========================================

    @FXML
    private void onCardEntradasClick() {
        List<Registro> entradas = listaRegistrosHoy.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || 
                        (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString())))
                .collect(Collectors.toList());

        Map<String, Registro> mapaSalidas = listaRegistrosHoy.stream()
                .filter(r -> (r.getTipo() == TipoMovimiento.SALIDA || 
                        (r.getTipo() != null && "SALIDA".equalsIgnoreCase(r.getTipo().toString()))) && r.getPersona() != null)
                .collect(Collectors.toMap(
                        r -> r.getPersona().getNie(),
                        r -> r,
                        (existente, reemplazo) -> existente
                ));

        abrirModalEntradasConSalida("Entradas de Hoy", entradas, mapaSalidas);
    }

    @FXML
    private void onCardSalidasClick() {
        List<Registro> filtrados = listaRegistrosHoy.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.SALIDA || 
                        (r.getTipo() != null && "SALIDA".equalsIgnoreCase(r.getTipo().toString())))
                .collect(Collectors.toList());
        abrirModalDetalle("Salidas de Hoy", filtrados);
    }

    @FXML
    private void onCardPresentesClick() {
        Map<String, List<Registro>> porAlumno = listaRegistrosHoy.stream()
                .filter(r -> r.getPersona() != null && r.getPersona().getNie() != null)
                .collect(Collectors.groupingBy(r -> r.getPersona().getNie()));

        List<Registro> listaPresentes = new ArrayList<>();

        for (List<Registro> historial : porAlumno.values()) {
            long ent = historial.stream().filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString()))).count();
            long sal = historial.stream().filter(r -> r.getTipo() == TipoMovimiento.SALIDA || (r.getTipo() != null && "SALIDA".equalsIgnoreCase(r.getTipo().toString()))).count();

            if (ent > sal) {
                historial.stream()
                        .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA || (r.getTipo() != null && "ENTRADA".equalsIgnoreCase(r.getTipo().toString())))
                        .max((r1, r2) -> {
                            String f1 = r1.getFechaHoraFormateada() != null ? r1.getFechaHoraFormateada() : "";
                            String f2 = r2.getFechaHoraFormateada() != null ? r2.getFechaHoraFormateada() : "";
                            return f1.compareTo(f2);
                        })
                        .ifPresent(listaPresentes::add);
            }
        }
                
        abrirModalDetalle("Alumnos Presentes en el Centro", listaPresentes);
    }

    @FXML
    private void onCardRetrasosClick() {
        List<Registro> filtrados = listaRegistrosHoy.stream()
                .filter(Registro::isTardanza)
                .collect(Collectors.toList());
        abrirModalDetalle("Retrasos de Hoy", filtrados);
    }

    // ==========================================
    // INTERFACES MODALES DINÁMICAS Y REPORTES
    // ==========================================

    private void abrirModalEntradasConSalida(String titulo, List<Registro> entradas, Map<String, Registro> mapaSalidas) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Detalle - " + titulo);
        modalStage.setMinWidth(750);
        modalStage.setMinHeight(450);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label lblTitulo = new Label(titulo + " (" + entradas.size() + ")");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TableView<Registro> tabla = new TableView<>();
        tabla.setPrefHeight(300);

        TableColumn<Registro, String> colNie = new TableColumn<>("NIE");
        colNie.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPersona() != null ? cell.getValue().getPersona().getNie() : "N/A"));
        colNie.setPrefWidth(100);

        TableColumn<Registro, String> colNombre = new TableColumn<>("Alumno / Persona");
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPersona() != null ? cell.getValue().getPersona().getNombre() : "Desconocido"));
        colNombre.setPrefWidth(220);

        TableColumn<Registro, String> colHoraEntrada = new TableColumn<>("Hora Entrada");
        colHoraEntrada.setCellValueFactory(cell -> {
            String fh = cell.getValue().getFechaHoraFormateada();
            String hora = (fh != null && fh.length() >= 19) ? fh.substring(11, 19) : fh;
            return new SimpleStringProperty(hora != null ? hora : "N/A");
        });
        colHoraEntrada.setPrefWidth(120);

        TableColumn<Registro, String> colHoraSalida = new TableColumn<>("Hora Salida");
        colHoraSalida.setCellValueFactory(cell -> {
            if (cell.getValue().getPersona() != null) {
                String nie = cell.getValue().getPersona().getNie();
                Registro registroSalida = mapaSalidas.get(nie);
                if (registroSalida != null) {
                    String fh = registroSalida.getFechaHoraFormateada();
                    return new SimpleStringProperty((fh != null && fh.length() >= 19) ? fh.substring(11, 19) : fh);
                }
            }
            return new SimpleStringProperty(""); 
        });
        colHoraSalida.setPrefWidth(120);

        TableColumn<Registro, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isTardanza() ? "⚠️ Retraso" : "🟢 A tiempo"));
        colEstado.setPrefWidth(100);

        tabla.getColumns().addAll(colNie, colNombre, colHoraEntrada, colHoraSalida, colEstado);
        tabla.getItems().addAll(entradas);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnCerrar.setOnAction(e -> modalStage.close());

        Button btnExportar = new Button("📄 Exportar PDF");
        btnExportar.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnExportar.setOnAction(e -> generarReportePDFCruzado(titulo, entradas, mapaSalidas, modalStage));

        HBox barraBotones = new HBox(10);
        barraBotones.setAlignment(Pos.CENTER_RIGHT);
        barraBotones.getChildren().addAll(btnCerrar, btnExportar);

        layout.getChildren().addAll(lblTitulo, tabla, barraBotones);

        Scene scene = new Scene(layout);
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    private void abrirModalDetalle(String titulo, List<Registro> datos) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Detalle - " + titulo);
        modalStage.setMinWidth(650);
        modalStage.setMinHeight(450);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label lblTitulo = new Label(titulo + " (" + datos.size() + ")");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TableView<Registro> tabla = new TableView<>();
        tabla.setPrefHeight(300);

        TableColumn<Registro, String> colNie = new TableColumn<>("NIE");
        colNie.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPersona() != null ? cell.getValue().getPersona().getNie() : "N/A"));
        colNie.setPrefWidth(120);

        TableColumn<Registro, String> colNombre = new TableColumn<>("Alumno / Persona");
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPersona() != null ? cell.getValue().getPersona().getNombre() : "Desconocido"));
        colNombre.setPrefWidth(250);

        TableColumn<Registro, String> colHora = new TableColumn<>("Hora Registrada");
        colHora.setCellValueFactory(cell -> {
            String fh = cell.getValue().getFechaHoraFormateada();
            return new SimpleStringProperty((fh != null && fh.length() >= 19) ? fh.substring(11, 19) : fh);
        });
        colHora.setPrefWidth(180);

        tabla.getColumns().addAll(colNie, colNombre, colHora);
        tabla.getItems().addAll(datos);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnCerrar.setOnAction(e -> modalStage.close());

        HBox barraBotones = new HBox(10);
        barraBotones.setAlignment(Pos.CENTER_RIGHT);
        barraBotones.getChildren().addAll(btnCerrar);

        layout.getChildren().addAll(lblTitulo, tabla, barraBotones);

        Scene scene = new Scene(layout);
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    private void generarReportePDFCruzado(String tipoReporte, List<Registro> entradas, Map<String, Registro> mapaSalidas, Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.setInitialFileName("Reporte_" + tipoReporte.replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(parentStage);
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font fontSub = new Font(Font.HELVETICA, 11, Font.ITALIC);
            Font fontHeader = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font fontCelda = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph titulo = new Paragraph("CheckPoint Go - Control de Asistencia", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Reporte Detallado: " + tipoReporte + "\nFecha: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontSub);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            document.add(subtitulo);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{15f, 45f, 20f, 20f});

            String[] headers = {"NIE", "Estudiante / Persona", "Hora Entrada", "Hora Salida"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (Registro r : entradas) {
                String nie = r.getPersona() != null ? r.getPersona().getNie() : "N/A";
                String nombre = r.getPersona() != null ? r.getPersona().getNombre() : "Desconocido";
                
                String fhEntrada = r.getFechaHoraFormateada();
                String hEntrada = (fhEntrada != null && fhEntrada.length() >= 19) ? fhEntrada.substring(11, 19) : fhEntrada;
                
                String hSalida = "";
                if (r.getPersona() != null && mapaSalidas.containsKey(r.getPersona().getNie())) {
                    String fhSalida = mapaSalidas.get(r.getPersona().getNie()).getFechaHoraFormateada();
                    hSalida = (fhSalida != null && fhSalida.length() >= 19) ? fhSalida.substring(11, 19) : fhSalida;
                }

                table.addCell(new PdfPCell(new Phrase(nie, fontCelda)));
                table.addCell(new PdfPCell(new Phrase(nombre, fontCelda)));
                table.addCell(new PdfPCell(new Phrase(hEntrada, fontCelda)));
                table.addCell(new PdfPCell(new Phrase(hSalida, fontCelda)));
            }

            document.add(table);
            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportación Completada");
            alert.setHeaderText(null);
            alert.setContentText("¡El reporte de asistencias combinadas ha sido guardado exitosamente!");
            alert.showAndWait();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo generar el PDF");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void arrancarSimuladorPasivo() {
        if (lblSyncStatus != null) lblSyncStatus.setText("❌ Modo Local de Prueba");
        if (lblEntradasNum != null) lblEntradasNum.setText("18");
        if (lblSalidasNum != null) lblSalidasNum.setText("4");
        if (lblPresentesNum != null) lblPresentesNum.setText("14");
        if (lblTardanzasNum != null) lblTardanzasNum.setText("2");
    }

    public void destruirListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }
}