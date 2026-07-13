package com.mycompany.checkpoint.ui.controllers;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController extends BaseController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private BarChart<String, Number> chartAsistenciaSemanal;

    // 🔥 NUEVOS: Inyecciones FXML para las tarjetas contenedoras de los contadores
    @FXML private VBox cardEntradas;
    @FXML private VBox cardSalidas;
    @FXML private VBox cardPresentes;
    @FXML private VBox cardRetrasos;

    // Lista en caché para guardar los registros del día actual
    private List<Registro> listaRegistrosHoy = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (chartAsistenciaSemanal != null) {
            chartAsistenciaSemanal.setAnimated(false);
        }
        configurarEventosTarjetas();
    }

    /**
     * Guarda una copia local de los registros de hoy cuando Firebase los actualice
     * para que las tarjetas tengan acceso inmediato a los datos.
     */
    public void setRegistrosHoy(List<Registro> registros) {
        this.listaRegistrosHoy = registros != null ? registros : new ArrayList<>();
    }

    /**
     * Configura los listeners de clic para cada tarjeta del Dashboard
     */
    private void configurarEventosTarjetas() {
        if (cardEntradas != null) {
            cardEntradas.setStyle("-fx-cursor: hand;");
            cardEntradas.setOnMouseClicked(e -> mostrarDetalleMétrica("Entradas de Hoy", TipoMovimiento.ENTRADA, false));
        }
        if (cardSalidas != null) {
            cardSalidas.setStyle("-fx-cursor: hand;");
            cardSalidas.setOnMouseClicked(e -> mostrarDetalleMétrica("Salidas de Hoy", TipoMovimiento.SALIDA, false));
        }
        if (cardPresentes != null) {
            cardPresentes.setStyle("-fx-cursor: hand;");
            cardPresentes.setOnMouseClicked(e -> mostrarDetalleMétrica("Alumnos Presentes", TipoMovimiento.ENTRADA, true));
        }
        if (cardRetrasos != null) {
            cardRetrasos.setStyle("-fx-cursor: hand;");
            cardRetrasos.setOnMouseClicked(e -> mostrarDetalleMétrica("Retrasos de Hoy", null, false)); 
        }
    }

    /**
     * Filtra los registros y abre una ventana modal con la tabla detallada.
     */
    private void mostrarDetalleMétrica(String titulo, TipoMovimiento tipo, boolean soloPresentes) {
        List<Registro> filtrados;

        if (titulo.equals("Retrasos de Hoy")) {
            // Filtrar solo los que tengan la propiedad tardanza activa
            filtrados = listaRegistrosHoy.stream()
                    .filter(Registro::isTardanza)
                    .collect(Collectors.toList());
        } else if (soloPresentes) {
            // Alumnos que entraron pero aún no han registrado salida
            Set<String> conSalida = listaRegistrosHoy.stream()
                    .filter(r -> r.getTipo() == TipoMovimiento.SALIDA && r.getPersona() != null)
                    .map(r -> r.getPersona().getNie())
                    .collect(Collectors.toSet());

            filtrados = listaRegistrosHoy.stream()
                    .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA && r.getPersona() != null)
                    .filter(r -> !conSalida.contains(r.getPersona().getNie()))
                    .collect(Collectors.toList());
        } else {
            // Filtrado convencional por Tipo de movimiento (Entrada / Salida)
            filtrados = listaRegistrosHoy.stream()
                    .filter(r -> r.getTipo() == tipo)
                    .collect(Collectors.toList());
        }

        abrirModalDetalle(titulo, filtrados);
    }

    /**
     * Construye y despliega la ventana emergente con la lista y la opción PDF
     */
    private void abrirModalDetalle(String titulo, List<Registro> datos) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Detalle: " + titulo);
        modalStage.setMinWidth(650);
        modalStage.setMinHeight(450);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f8fafc;");

        Label lblTitulo = new Label(titulo + " (" + datos.size() + ")");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Configuración de la Tabla JavaFX generada al vuelo
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
        colHora.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaHoraFormateada()));
        colHora.setPrefWidth(150);

        TableColumn<Registro, String> colObs = new TableColumn<>("Observaciones");
        colObs.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getObservaciones() != null ? cell.getValue().getObservaciones() : ""));
        colObs.setPrefWidth(100);

        tabla.getColumns().addAll(colNie, colNombre, colHora, colObs);
        tabla.getItems().addAll(datos);

        // Botones de acción
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnCerrar.setOnAction(e -> modalStage.close());

        Button btnExportar = new Button("📄 Exportar a PDF");
        btnExportar.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        btnExportar.setOnAction(e -> generarReportePDF(titulo, datos, modalStage));

        HBox barraBotones = new HBox(10);
        barraBotones.setAlignment(Pos.CENTER_RIGHT);
        barraBotones.getChildren().addAll(btnCerrar, btnExportar);

        layout.getChildren().addAll(lblTitulo, tabla, barraBotones);

        Scene scene = new Scene(layout);
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    /**
     * Genera el documento PDF con formato institucional estructurado
     */
    private void generarReportePDF(String tipoReporte, List<Registro> registros, Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte de Asistencia");
        fileChooser.setInitialFileName("Reporte_" + tipoReporte.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(parentStage);
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Fuentes estandarizadas
            Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font fontSub = new Font(Font.HELVETICA, 11, Font.ITALIC);
            Font fontHeaderTabla = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font fontCelda = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // Encabezado principal del PDF
            Paragraph titulo = new Paragraph("CheckPoint Go - Control de Entrada y Salida", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Reporte Detallado: " + tipoReporte + "\nGenerado el: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontSub);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            document.add(subtitulo);

            // Estructura de Tabla PDF (4 columnas)
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20f, 45f, 20f, 15f});

            // Headers de la Tabla
            String[] headers = {"NIE", "Estudiante / Persona", "Fecha y Hora", "Retraso"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTabla));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Inyección de Filas
            for (Registro r : registros) {
                String nie = r.getPersona() != null ? r.getPersona().getNie() : "N/A";
                String nombre = r.getPersona() != null ? r.getPersona().getNombre() : "Desconocido";
                String fechaHora = r.getFechaHoraFormateada() != null ? r.getFechaHoraFormateada() : "N/A";
                String retraso = r.isTardanza() ? "SÍ" : "NO";

                table.addCell(new PdfPCell(new Phrase(nie, fontCelda)));
                table.addCell(new PdfPCell(new Phrase(nombre, fontCelda)));
                table.addCell(new PdfPCell(new Phrase(fechaHora, fontCelda)));
                
                PdfPCell cellRetraso = new PdfPCell(new Phrase(retraso, fontCelda));
                cellRetraso.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cellRetraso);
            }

            document.add(table);
            document.close();

            // Notificación visual de éxito al usuario
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportación Completada");
            alert.setHeaderText(null);
            alert.setContentText("¡El reporte PDF ha sido generado y guardado exitosamente!");
            alert.showAndWait();

        } catch (Exception ex) {
            logger.error("Error al construir el archivo de exportación PDF: {}", ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Exportación");
            alert.setHeaderText("No se pudo generar el PDF");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    public void actualizarGraficaSemanal(List<Registro> registros) {
        if (chartAsistenciaSemanal == null) return;

        Platform.runLater(() -> {
            try {
                chartAsistenciaSemanal.getData().clear();

                Map<String, Integer> conteoSemanal = new HashMap<>();
                conteoSemanal.put("Lunes", 0);
                conteoSemanal.put("Martes", 0);
                conteoSemanal.put("Miércoles", 0);
                conteoSemanal.put("Jueves", 0);
                conteoSemanal.put("Viernes", 0);

                SimpleDateFormat sdfLatino = new SimpleDateFormat("dd/MM/yyyy");

                for (Registro r : registros) {
                    if (r.getFechaHoraFormateada() != null && r.getTipo() == TipoMovimiento.ENTRADA) {
                        try {
                            String fechaDoc = r.getFechaHoraFormateada().substring(0, 10); 
                            Date date = sdfLatino.parse(fechaDoc);
                            
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            int numeroDia = cal.get(Calendar.DAY_OF_WEEK);

                            String diaSemana = "";
                            switch (numeroDia) {
                                case Calendar.MONDAY:    diaSemana = "Lunes"; break;
                                case Calendar.TUESDAY:   diaSemana = "Martes"; break;
                                case Calendar.WEDNESDAY: diaSemana = "Miércoles"; break;
                                case Calendar.THURSDAY:  diaSemana = "Jueves"; break;
                                case Calendar.FRIDAY:    diaSemana = "Viernes"; break;
                            }

                            if (conteoSemanal.containsKey(diaSemana)) {
                                conteoSemanal.put(diaSemana, conteoSemanal.get(diaSemana) + 1);
                            }
                        } catch (Exception e) {
                            logger.error("Error parseando fecha en gráfica semanal: {}", e.getMessage());
                        }
                    }
                }

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Alumnos Asistidos");
                
                series.getData().add(new XYChart.Data<>("Lunes", conteoSemanal.get("Lunes")));
                series.getData().add(new XYChart.Data<>("Martes", conteoSemanal.get("Martes")));
                series.getData().add(new XYChart.Data<>("Miércoles", conteoSemanal.get("Miércoles")));
                series.getData().add(new XYChart.Data<>("Jueves", conteoSemanal.get("Jueves")));
                series.getData().add(new XYChart.Data<>("Viernes", conteoSemanal.get("Viernes")));

                chartAsistenciaSemanal.getData().add(series);
                
            } catch (Exception ex) {
                logger.error("Error crítico actualizando la interfaz del gráfico: {}", ex.getMessage());
            }
        });
    }
}