package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.BajaActivoRespuesta;
import com.infrati.backendinfrati.dto.DarDeBajaRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class BajaActivoService {
    private final ActivoRepository activos;
    private final MonitoreoSnmpRepository conexiones;
    private final HistoricoEstadoRepository historico;
    private final EntityManager entityManager;

    public BajaActivoService(ActivoRepository activos, MonitoreoSnmpRepository conexiones,
            HistoricoEstadoRepository historico, EntityManager entityManager) {
        this.activos = activos;
        this.conexiones = conexiones;
        this.historico = historico;
        this.entityManager = entityManager;
    }

    @Transactional
    public BajaActivoRespuesta darDeBaja(Long id, DarDeBajaRequest solicitud) {
        if (solicitud == null) {
            throw new SolicitudInvalidaException("Se requiere responsable y motivo para dar de baja el activo.");
        }
        String responsable = requerido(solicitud.responsable(), "responsable");
        String motivo = requerido(solicitud.motivo(), "motivo");
        String detalle = "Responsable: " + responsable + ". Motivo: " + motivo;

        // El ETL bloquea primero la conexión y luego el activo. Conservamos ese orden
        // para que una lectura SNMP concurrente no deje el activo a medio dar de baja.
        entityManager.createNativeQuery("SELECT id FROM monitoreo_snmp WHERE activo_id=:id FOR UPDATE")
                .setParameter("id", id).getResultList();
        if (entityManager.createNativeQuery("SELECT id FROM activo WHERE id=:id FOR UPDATE")
                .setParameter("id", id).getResultList().isEmpty()) {
            throw new RecursoNoEncontradoException("Activo " + id + " no encontrado");
        }

        Activo activo = activos.findById(id).orElseThrow();
        var vinculadas = conexiones.findByActivoId(id);
        LocalDateTime fecha = LocalDateTime.now(ZoneOffset.UTC);
        int eventos = 0;
        int conexionesInactivadas = 0;

        if (activo.getEstado_operativo() != EstadoEnum.Baja) {
            String anterior = activo.getEstado_operativo() == null ? "sin estado previo"
                    : activo.getEstado_operativo().name();
            activo.setEstado_operativo(EstadoEnum.Baja);
            registrar(activo, "estado_operativo", "Baja",
                    "Activo dado de baja. Estado anterior: " + anterior + ". " + detalle, fecha);
            eventos++;
        }
        if (Boolean.TRUE.equals(activo.getMantenimientoActivo())) {
            activo.setMantenimientoActivo(false);
            registrar(activo, "mantenimiento_activo", "false",
                    "Mantenimiento finalizado por baja del activo. " + detalle, fecha);
            eventos++;
        }
        if (activo.getCluster() != null) {
            String anterior = activo.getCluster();
            activo.setCluster(null);
            registrar(activo, "cluster", "Sin clúster",
                    "Activo retirado del clúster " + anterior + ". " + detalle, fecha);
            eventos++;
        }
        for (var conexion : vinculadas) {
            if (!"Inactivo".equals(conexion.getEstadoConexion())) {
                String anterior = conexion.getEstadoConexion();
                conexion.setEstadoConexion("Inactivo");
                registrar(activo, "estado_conexion", "Inactivo",
                        "Conexión SNMP #" + conexion.getId() + " desactivada por baja del activo"
                                + " (antes: " + anterior + "). " + detalle, fecha);
                conexionesInactivadas++;
                eventos++;
            }
        }
        return new BajaActivoRespuesta(id, EstadoEnum.Baja.name(), activo.getCluster(),
                Boolean.TRUE.equals(activo.getMantenimientoActivo()), conexionesInactivadas,
                eventos, eventos > 0);
    }

    private void registrar(Activo activo, String campo, String valor, String descripcion,
            LocalDateTime fecha) {
        historico.save(HistoricoEstado.builder()
                .activo(activo)
                .campo(campo)
                .valorNuevo(valor)
                .descripcion(descripcion)
                .fechaCambio(fecha)
                .build());
    }

    private static String requerido(String valor, String nombre) {
        if (valor == null || valor.isBlank() || valor.length() > 255
                || valor.chars().anyMatch(Character::isISOControl)) {
            throw new SolicitudInvalidaException("El campo " + nombre + " es obligatorio y debe ser válido.");
        }
        return valor.trim();
    }
}
