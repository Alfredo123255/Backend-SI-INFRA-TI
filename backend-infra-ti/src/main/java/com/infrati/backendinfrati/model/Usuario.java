package com.infrati.backendinfrati.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
@Entity @Table(name="usuario") @Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(nullable=false,unique=true) private String correo;
    @JsonIgnore @Column(name="contrasena_hash",nullable=false) private String contrasenaHash;
    @Column(nullable=false) private String nombre;
    @Column(nullable=false) private boolean activo=true;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="rol_id",nullable=false) private Rol rol;
    @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
}
