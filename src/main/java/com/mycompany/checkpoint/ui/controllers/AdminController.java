package com.mycompany.checkpoint.ui.controllers;

import com.mycompany.checkpoint.service.ExcelImportService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AdminController {

    @FXML private Button btnImportarExcel; // Debe coincidir con el fx:id de tu FXML
    
    private final ExcelImportService excelService = new ExcelImportService();

    @FXML
    private void handleImportarExcel() {
        // 1. Configurar el buscador de archivos
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Archivo de Excel con Alumnos");
        
        // Filtro para que solo permita elegir archivos de Excel (.xlsx)
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos de Excel (*.xlsx)", "*.xlsx")
        );

        // 2. Abrir la ventana de diálogo
        Stage stage = (Stage) btnImportarExcel.getScene().getWindow();
        File archivoSeleccionado = fileChooser.showOpenDialog(stage);

        if (archivoSeleccionado != null) {
            try {
                // 3. Ejecutar la importación
                excelService.importarAlumnosAFirestore(archivoSeleccionado);
                
                mostrarAlerta("Éxito", "El listado de alumnos se ha subido correctamente a Firebase.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlerta("Error", "Ocurrió un problema al leer el archivo Excel: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}