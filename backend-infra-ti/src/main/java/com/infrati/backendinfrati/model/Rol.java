package com.infrati.backendinfrati.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.*;
@Entity @Table(name="rol") @Getter @Setter @NoArgsConstructor
public class Rol {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(nullable=false,unique=true) private String nombre;
    @Column(name="ver_inventario", nullable=false)
    private boolean ver_inventario;
    @Column(name="gestionar_conexiones", nullable=false)
    private boolean gestionar_conexiones;
    @Column(name="editar_ciclo_vida", nullable=false)
    private boolean editar_ciclo_vida;
    @Column(name="gestionar_mantenimiento", nullable=false)
    private boolean gestionar_mantenimiento;
    @Column(name="dar_baja", nullable=false)
    private boolean dar_baja;
    @Column(name="configurar_umbrales", nullable=false)
    private boolean configurar_umbrales;
    @Column(name="gestionar_ubicaciones", nullable=false)
    private boolean gestionar_ubicaciones;
    @Column(name="exportar", nullable=false)
    private boolean exportar;
    @Column(name="administrar_usuarios", nullable=false)
    private boolean administrar_usuarios;
    public Map<String,Boolean> permisos() {
        var valores=new LinkedHashMap<String,Boolean>();
        valores.put("ver_inventario",ver_inventario);
        valores.put("gestionar_conexiones",gestionar_conexiones);
        valores.put("editar_ciclo_vida",editar_ciclo_vida);
        valores.put("gestionar_mantenimiento",gestionar_mantenimiento);
        valores.put("dar_baja",dar_baja);
        valores.put("configurar_umbrales",configurar_umbrales);
        valores.put("gestionar_ubicaciones",gestionar_ubicaciones);
        valores.put("exportar",exportar);
        valores.put("administrar_usuarios",administrar_usuarios);
        return valores;
    }
    public void aplicarPermisos(Map<String,Boolean> valores) {
        ver_inventario=Boolean.TRUE.equals(valores.get("ver_inventario"));
        gestionar_conexiones=Boolean.TRUE.equals(valores.get("gestionar_conexiones"));
        editar_ciclo_vida=Boolean.TRUE.equals(valores.get("editar_ciclo_vida"));
        gestionar_mantenimiento=Boolean.TRUE.equals(valores.get("gestionar_mantenimiento"));
        dar_baja=Boolean.TRUE.equals(valores.get("dar_baja"));
        configurar_umbrales=Boolean.TRUE.equals(valores.get("configurar_umbrales"));
        gestionar_ubicaciones=Boolean.TRUE.equals(valores.get("gestionar_ubicaciones"));
        exportar=Boolean.TRUE.equals(valores.get("exportar"));
        administrar_usuarios=Boolean.TRUE.equals(valores.get("administrar_usuarios"));
    }
}
