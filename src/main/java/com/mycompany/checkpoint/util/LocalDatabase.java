package com.mycompany.checkpoint.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.model.Registro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilidad para persistencia local de datos en JSON.
 * Actúa como cache offline para personas y registros.
 */
public class LocalDatabase {
    private static final Logger logger = LoggerFactory.getLogger(LocalDatabase.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String DATA_DIR = ".checkpoint_data";
    private static final String PERSONAS_FILE = "personas.json";
    private static final String REGISTROS_FILE = "registros.json";

    static {
        ensureDataDirectory();
    }

    /**
     * Asegura que el directorio de datos existe.
     */
    private static void ensureDataDirectory() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), DATA_DIR);
            Files.createDirectories(path);
        } catch (IOException e) {
            logger.error("Error creando directorio de datos: {}", e.getMessage());
        }
    }

    /**
     * Obtiene la ruta del archivo de personas.
     */
    private static File getPersonasFile() {
        return Paths.get(System.getProperty("user.home"), DATA_DIR, PERSONAS_FILE).toFile();
    }

    /**
     * Obtiene la ruta del archivo de registros.
     */
    private static File getRegistrosFile() {
        return Paths.get(System.getProperty("user.home"), DATA_DIR, REGISTROS_FILE).toFile();
    }

    /**
     * Carga todas las personas desde el cache local.
     */
    public static List<Estudiante> loadPersonas() {
        try {
            File file = getPersonasFile();
            if (!file.exists()) {
                logger.info("Archivo de personas no existe, retornando lista vacía");
                return new ArrayList<>();
            }

            Estudiante[] personas = mapper.readValue(file, Estudiante[].class);
            logger.info("Cargadas {} personas desde cache local", personas.length);
            return Arrays.asList(personas);
        } catch (IOException e) {
            logger.error("Error cargando personas: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda personas en el cache local.
     */
    public static void savePersonas(List<Estudiante> personas) {
        try {
            File file = getPersonasFile();
            mapper.writeValue(file, personas);
            logger.info("Guardadas {} personas en cache local", personas.size());
        } catch (IOException e) {
            logger.error("Error guardando personas: {}", e.getMessage());
        }
    }

    /**
     * Carga todos los registros desde el cache local.
     */
    public static List<Registro> loadRegistros() {
        try {
            File file = getRegistrosFile();
            if (!file.exists()) {
                logger.info("Archivo de registros no existe, retornando lista vacía");
                return new ArrayList<>();
            }

            Registro[] registros = mapper.readValue(file, Registro[].class);
            logger.info("Cargados {} registros desde cache local", registros.length);
            return Arrays.asList(registros);
        } catch (IOException e) {
            logger.error("Error cargando registros: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Guarda registros en el cache local.
     */
    public static void saveRegistros(List<Registro> registros) {
        try {
            File file = getRegistrosFile();
            mapper.writeValue(file, registros);
            logger.info("Guardados {} registros en cache local", registros.size());
        } catch (IOException e) {
            logger.error("Error guardando registros: {}", e.getMessage());
        }
    }

    /**
     * Limpia la base de datos local.
     */
    public static void clear() {
        try {
            getPersonasFile().delete();
            getRegistrosFile().delete();
            logger.info("Cache local limpiado");
        } catch (Exception e) {
            logger.error("Error limpiando cache: {}", e.getMessage());
        }
    }

    /**
     * Obtiene el tamaño total del cache en bytes.
     */
    public static long getCacheSize() {
        long size = 0;
        File personasFile = getPersonasFile();
        File registrosFile = getRegistrosFile();

        if (personasFile.exists()) size += personasFile.length();
        if (registrosFile.exists()) size += registrosFile.length();

        return size;
    }
}
