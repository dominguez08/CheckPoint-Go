package com.mycompany.checkpoint.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.mycompany.checkpoint.model.Estudiante;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

public class ExcelImportService {

    /**
     * Lee el archivo Excel, registra cada fila como un Estudiante en Firestore
     * y genera automáticamente su respectivo código QR nativo en el disco.
     */
    public void importarAlumnosAFirestore(File archivoExcel) throws Exception {
        // Obtener la instancia activa de Firestore
        Firestore db = FirestoreClient.getFirestore();

        try (FileInputStream fis = new FileInputStream(archivoExcel);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Tomamos la primera hoja del Excel
            Sheet sheet = workbook.getSheetAt(0);

            // Recorremos las filas (empezamos en 1 para saltarnos la fila de encabezados)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Leer y limpiar cada celda del Excel
                String nie          = obtenerValorCelda(row.getCell(0));
                String nombre       = obtenerValorCelda(row.getCell(1));
                String correo       = obtenerValorCelda(row.getCell(2));
                String ano          = obtenerValorCelda(row.getCell(3)); // El año (Ej: "1° Año")
                String seccion      = obtenerValorCelda(row.getCell(4)); // La sección (Ej: "A")
                String sexo         = obtenerValorCelda(row.getCell(5)); // Sexo (Ej: "Masculino")
                
                // 🎯 NUEVO: Leemos la columna de la Especialidad (Celda 6 del Excel)
                String especialidad = obtenerValorCelda(row.getCell(6)); 

                // Validación mínima: si no tiene NIE o Nombre, nos saltamos la fila
                if (nie.isEmpty() || nombre.isEmpty()) {
                    continue;
                }

                // Si la celda de especialidad viene en blanco en el Excel, le asignamos "General"
                if (especialidad.isEmpty()) {
                    especialidad = "General";
                }

                // 🎯 SOLUCIÓN: Usamos el constructor de 8 parámetros para incluir la especialidad real
                Estudiante nuevoEstudiante = new Estudiante(nie, nombre, correo, ano, seccion, "Activo", sexo, specialtyFix(especialidad));

                // Lo guardamos en la colección "estudiantes" usando el NIE como ID del documento
                db.collection("estudiantes")
                  .document(nie)
                  .set(nuevoEstudiante);

                // Generación automática y en caliente del código QR para este alumno
                try {
                    // Genera un QR estándar de 300x300 píxeles usando el NIE mapeado
                    QRService.generarCodigoQR(nie, 300, 300);
                    System.out.println("🚀 Alumno importado y QR creado con éxito: " + nombre + " (NIE: " + nie + ")");
                } catch (Exception qrEx) {
                    System.err.println("⚠️ Alumno guardado en base de datos, pero falló la creación de su QR para " + nombre + ": " + qrEx.getMessage());
                }
            }
        }
    }

    /**
     * Normaliza los textos de la especialidad para evitar que pequeños errores o espacios extra 
     * en el Excel generen duplicados extraños en tu base de datos.
     */
    private String specialtyFix(String texto) {
        if (texto == null || texto.trim().isEmpty()) return "General";
        String t = texto.trim();
        if (t.equalsIgnoreCase("Desarrollo de Software")) return "Desarrollo de Software";
        if (t.equalsIgnoreCase("Infraestructura Tecnológica") || t.equalsIgnoreCase("Infraestructura")) return "Infraestructura Tecnológica";
        if (t.equalsIgnoreCase("Servicios Informáticos")) return "Servicios Informáticos";
        if (t.equalsIgnoreCase("Mecánica Industrial")) return "Mecánica Industrial";
        if (t.equalsIgnoreCase("Mantenimiento Automotriz") || t.equalsIgnoreCase("Automotriz")) return "Mantenimiento Automotriz";
        if (t.equalsIgnoreCase("Electrónica")) return "Electrónica";
        if (t.equalsIgnoreCase("Sistemas Eléctricos")) return "Sistemas Eléctricos";
        return t;
    }

    /**
     * Método auxiliar para leer de forma segura celdas de texto o numéricas sin añadir decimales extraños (.0)
     */
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return "";
        
        if (cell.getCellType() == CellType.NUMERIC) {
            // Convierte números (como el NIE o el Año) a texto limpio sin decimales
            return String.format("%.0f", cell.getNumericCellValue());
        } else {
            return cell.getStringCellValue().trim();
        }
    }
}