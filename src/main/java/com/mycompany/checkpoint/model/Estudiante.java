package com.mycompany.checkpoint.model;

import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.PropertyName;

public class Estudiante {
    private String nie;
    private String nombre;
    private String correo;
    private String anio;       
    private String seccion;   
    private String estado;    
    private String sexo;      
    private String especialidad; 

    public Estudiante() {
        this.estado = "Activo";
    }

    public Estudiante(String nie, String nombre, String correo, String anio, String seccion, String estado, String sexo, String especialidad) {
        this.setNie(nie);
        this.setNombre(nombre);
        this.setCorreo(correo);
        this.setAnio(anio);
        this.setSeccion(seccion);
        this.setEstado(estado);
        this.setSexo(sexo);
        this.setEspecialidad(especialidad);
    }

    public Estudiante(String nie, String nombre, String correo, String anio, String seccion, String estado, String sexo) {
        this(nie, nombre, correo, anio, seccion, estado, sexo, "General");
    }

    public Estudiante(String nie, String nombre, String correo, Object tipoObjeto) {
        this(nie, nombre, correo, "1° Año", "A", "Activo", "Masculino", "General");
    }

    private String limpiarComillas(String texto) {
        if (texto == null) return null;
        String recortado = texto.trim();
        if (recortado.startsWith("\"") && recortado.endsWith("\"") && recortado.length() >= 2) {
            return recortado.substring(1, recortado.length() - 1);
        }
        return recortado;
    }

    @PropertyName("Nie")
    public String getNie() { return nie; }
    @PropertyName("Nie")
    public void setNie(String nie) { this.nie = limpiarComillas(nie); }

    @PropertyName("Nombre")
    public String getNombre() { return nombre; }
    @PropertyName("Nombre")
    public void setNombre(String nombre) { this.nombre = limpiarComillas(nombre); }

    @PropertyName("Correo")
    public String getCorreo() { return correo; }
    @PropertyName("Correo")
    public void setCorreo(String correo) { this.correo = limpiarComillas(correo); }

    // 🔥 Mapeo directo a la base de datos de Firebase
    @PropertyName("Año")
    public String getAnio() { 
        return anio; 
    }
    
    @PropertyName("Año")
    public void setAnio(String anio) { 
        this.anio = limpiarComillas(anio); 
    }
    @PropertyName("Seccion")
    public String getSeccion() { return seccion; }
    @PropertyName("Seccion")
    public void setSeccion(String seccion) { this.seccion = limpiarComillas(seccion); }

    @PropertyName("Estado")
    public String getEstado() { return estado; }
    @PropertyName("Estado")
    public void setEstado(String estado) { this.estado = limpiarComillas(estado); }

    @PropertyName("Sexo")
    public String getSexo() { return sexo; }
    @PropertyName("Sexo")
    public void setSexo(String sexo) { this.sexo = limpiarComillas(sexo); }

    @PropertyName("Especialidad")
    public String getEspecialidad() { return especialidad; }
    @PropertyName("Especialidad")
    public void setEspecialidad(String especialidad) { this.especialidad = limpiarComillas(especialidad); }

    // 🎯 Métodos puente excluidos de Firebase para llamadas manuales de UI
    @Exclude
    public String getAño() { 
        return getAnio(); 
    }
    
    @Exclude
    public void setAño(String ano) { 
        this.setAnio(ano); 
    }

    @Exclude
    public String getNombres() { return getNombre(); }
    @Exclude
    public String getApellidos() { return ""; } 
    @Exclude
    public String getId() { return nie; }
    public void setId(String id) { this.setNie(id); }
    @Exclude
    public String getEmail() { return correo; }
    @Exclude
    public boolean isActivo() { return estado != null && estado.equalsIgnoreCase("Activo"); }
    @Exclude
    public TipoPersonaEmu getTipo() { return new TipoPersonaEmu(); }
    public void setTipo(Object tipo) { }
    @Exclude
    public boolean needsSync() { return true; }
    @Exclude
    public long getLastSyncTime() { return 0; }
    public void setLastSyncTime(long lastSyncTime) { }

    public static class TipoPersonaEmu {
        public String getTarget() { return "Estudiante"; }
        public String getDescription() { return "Estudiante"; }
        public String getDescripcion() { return "Estudiante"; } 
    }

    @Exclude
    public static final Integer SyncState = 1;
}