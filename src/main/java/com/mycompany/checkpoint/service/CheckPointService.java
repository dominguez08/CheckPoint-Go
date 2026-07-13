package com.mycompany.checkpoint.service;

import com.mycompany.checkpoint.model.Estudiante;
import com.mycompany.checkpoint.model.Registro;
import com.mycompany.checkpoint.model.TipoMovimiento;
import com.mycompany.checkpoint.model.TipoPersona;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio principal del sistema CheckPoint Go.
 * Gestiona el almacenamiento en memoria (prototipo) de personas y registros.
 *
 * En una versión de producción, este servicio se conectaría a una base de datos
 * como MySQL o PostgreSQL usando JDBC o JPA.
 */
public class CheckPointService {

    // Hora límite para considerar tardanza (8:00 AM)
    private static final LocalTime HORA_LIMITE_TARDANZA = LocalTime.of(8, 0);

    // Repositorios en memoria
    private final List<Estudiante> personas;
    private final List<Registro> registros;

    // Contador autoincremental para IDs de registros
    private int contadorRegistros = 1;

    /**
     * Constructor: inicializa las listas y carga datos de ejemplo.
     */
    public CheckPointService() {
        this.personas = new ArrayList<>();
        this.registros = new ArrayList<>();
        cargarDatosEjemplo();
    }

    // =========================================================
    //  GESTIÓN DE PERSONAS
    // =========================================================

    /**
     * Agrega una nueva persona al sistema.
     *
     * @param persona La persona a registrar
     * @throws IllegalArgumentException si ya existe una persona con el mismo ID
     */
    public void agregarPersona(Estudiante persona) {
        if (buscarPersonaPorId(persona.getId()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una persona con el ID: " + persona.getId());
        }
        personas.add(persona);
    }

    /**
     * Busca una persona por su ID único.
     *
     * @param id El ID a buscar
     * @return Optional con la persona encontrada, o vacío si no existe
     */
    public Optional<Estudiante> buscarPersonaPorId(String id) {
        return personas.stream()
                .filter(p -> p.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    /**
     * Retorna la lista completa de personas registradas.
     */
    public List<Estudiante> obtenerTodasLasPersonas() {
        return new ArrayList<>(personas);
    }

    /**
     * Filtra personas por tipo.
     *
     * @param tipo El tipo de persona a filtrar
     */
    public List<Estudiante> obtenerPersonasPorTipo(TipoPersona tipo) {
        return personas.stream()
                // CORRECCIÓN: Compara el texto de la descripción simulada con el nombre del Enum
                .filter(p -> p.getTipo().getDescripcion().equalsIgnoreCase(tipo.name()))
                .collect(Collectors.toList());
    }

    /**
     * Elimina una persona del sistema por ID.
     *
     * @param id ID de la persona a eliminar
     * @return true si fue eliminada, false si no se encontró
     */
    public boolean eliminarPersona(String id) {
        return personas.removeIf(p -> p.getId().equalsIgnoreCase(id));
    }

    // =========================================================
    //  GESTIÓN DE REGISTROS (ENTRADAS/SALIDAS)
    // =========================================================

    /**
     * Registra una entrada o salida para una persona.
     * Detecta automáticamente si hay tardanza en entradas.
     *
     * @param idPersona ID de la persona
     * @param tipo      ENTRADA o SALIDA
     * @return El registro creado
     * @throws IllegalArgumentException si la persona no existe
     */
    public Registro registrarMovimiento(String idPersona, TipoMovimiento tipo) {
        Estudiante persona = buscarPersonaPorId(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Persona con ID '" + idPersona + "' no encontrada."));

        Registro registro = new Registro(contadorRegistros++, persona, tipo);

        // Detectar tardanza solo en entradas
        if (tipo == TipoMovimiento.ENTRADA) {
            LocalTime horaActual = registro.getFechaHora().toLocalTime();
            if (horaActual.isAfter(HORA_LIMITE_TARDANZA)) {
                registro.setTardanza(true);
            }
        }

        registros.add(registro);
        return registro;
    }

    /**
     * Retorna todos los registros del sistema.
     */
    public List<Registro> obtenerTodosLosRegistros() {
        return new ArrayList<>(registros);
    }

    /**
     * Filtra registros por tipo de movimiento.
     */
    public List<Registro> obtenerRegistrosPorTipo(TipoMovimiento tipo) {
        return registros.stream()
                .filter(r -> r.getTipo() == tipo)
                .collect(Collectors.toList());
    }

    // =========================================================
    //  ESTADÍSTICAS DEL DÍA (para el panel Resumen)
    // =========================================================

    /**
     * Cuenta el total de entradas registradas hoy.
     */
    public long contarEntradasHoy() {
        LocalDate hoy = LocalDate.now();
        return registros.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.ENTRADA)
                .filter(r -> r.getFechaHora().toLocalDate().equals(hoy))
                .count();
    }

    /**
     * Cuenta el total de salidas registradas hoy.
     */
    public long contarSalidasHoy() {
        LocalDate hoy = LocalDate.now();
        return registros.stream()
                .filter(r -> r.getTipo() == TipoMovimiento.SALIDA)
                .filter(r -> r.getFechaHora().toLocalDate().equals(hoy))
                .count();
    }

    /**
     * Calcula cuántas personas están actualmente dentro del instituto.
     * Presentes = Entradas de hoy - Salidas de hoy
     */
    public long contarPresentes() {
        long entradas = contarEntradasHoy();
        long salidas = contarSalidasHoy();
        return Math.max(0, entradas - salidas);
    }

    /**
     * Cuenta cuántas personas llegaron tarde hoy.
     */
    public long contarTardanzasHoy() {
        LocalDate hoy = LocalDate.now();
        return registros.stream()
                .filter(Registro::isTardanza)
                .filter(r -> r.getFechaHora().toLocalDate().equals(hoy))
                .count();
    }

    // =========================================================
    //  DATOS DE EJEMPLO (Prototipo)
    // =========================================================

    /**
     * Carga datos de prueba para demostrar el funcionamiento del sistema.
     * En producción, estos datos vendrían de la base de datos.
     */
    private void cargarDatosEjemplo() {
        // Estudiantes
        personas.add(new Estudiante("EST001", "María González López", "mgonzalez@instituto.edu.sv", TipoPersona.ESTUDIANTE));
        personas.add(new Estudiante("EST002", "Carlos Hernández Rivas", "chernandez@instituto.edu.sv", TipoPersona.ESTUDIANTE));
        personas.add(new Estudiante("EST003", "Ana Martínez Flores", "amartinez@instituto.edu.sv", TipoPersona.ESTUDIANTE));
        personas.add(new Estudiante("EST004", "Luis García Pérez", "lgarcia@instituto.edu.sv", TipoPersona.ESTUDIANTE));
        personas.add(new Estudiante("EST005", "Sofía Ramírez Díaz", "sramirez@instituto.edu.sv", TipoPersona.ESTUDIANTE));

        // Personal Administrativo
        personas.add(new Estudiante("ADM001", "Roberto Chávez Molina", "rchavez@instituto.edu.sv", TipoPersona.PERSONAL_ADMINISTRATIVO));
        personas.add(new Estudiante("ADM002", "Patricia Vásquez Cruz", "pvasquez@instituto.edu.sv", TipoPersona.PERSONAL_ADMINISTRATIVO));

        // Personal de Seguridad
        personas.add(new Estudiante("SEG001", "Juan Morales Aguilar", "jmorales@instituto.edu.sv", TipoPersona.PERSONAL_SEGURIDAD));
        personas.add(new Estudiante("SEG002", "Rosa Portillo Mejía", "rportillo@instituto.edu.sv", TipoPersona.PERSONAL_SEGURIDAD));
    }
}