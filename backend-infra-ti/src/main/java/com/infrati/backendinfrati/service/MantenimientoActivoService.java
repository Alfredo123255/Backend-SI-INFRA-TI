package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.dto.MantenimientoActivoRespuesta;
import com.infrati.backendinfrati.dto.MantenimientoActivoDetalle;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.model.MonitoreoSnmp;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MantenimientoActivoService {
    private static final String PAUSA_CONEXION = "Pausada por mantenimiento. Estado previo: ";
    private final ActivoRepository activos;
    private final HistoricoEstadoRepository historico;
    private final MonitoreoSnmpRepository conexiones;
    private final EntityManager entityManager;

    public MantenimientoActivoService(ActivoRepository activos,
            HistoricoEstadoRepository historico, MonitoreoSnmpRepository conexiones,
            EntityManager entityManager) {
        this.activos = activos;
        this.historico = historico;
        this.conexiones = conexiones;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<MantenimientoActivoDetalle> listar() {
        return activos.findByMantenimientoActivoTrue().stream().map(activo -> {
            var evento = historico.findFirstByActivo_IdAndCampoAndValorNuevoOrderByFechaCambioDescIdDesc(
                    activo.getId(), "mantenimiento_activo", "true").orElse(null);
            return new MantenimientoActivoDetalle(activo.getId(), activo.getHostname(),
                    activo.getTipo_activo() == null ? null : activo.getTipo_activo().name(),
                    true, evento == null ? null : evento.getDescripcion(),
                    evento == null || evento.getFechaCambio() == null ? null
                            : evento.getFechaCambio().atOffset(ZoneOffset.UTC));
        }).toList();
    }

    @Transactional
    public MantenimientoActivoRespuesta cambiar(Long id, CambiarMantenimientoRequest solicitud) {
        if (solicitud == null || solicitud.mantenimientoActivo() == null) {
            throw new SolicitudInvalidaException("mantenimientoActivo debe ser true o false.");
        }
        String responsable = detalle(solicitud.responsable(), "responsable");
        String motivo = detalle(solicitud.motivo(), "motivo");
        boolean activar = solicitud.mantenimientoActivo();
        // El ETL bloquea primero la conexión y después el activo. Respetar ese
        // orden evita que una extracción concurrente escriba tras la pausa.
        MonitoreoSnmp conexion = conexiones.findOneByActivoId(id).orElse(null);
        Activo activo = activos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Activo " + id + " no encontrado"));
        if (activar && activo.getEstado_operativo() == EstadoEnum.Baja) {
            throw new SolicitudInvalidaException("Un activo dado de baja no puede entrar en mantenimiento.");
        }

        int cambios = activos.cambiarMantenimientoSiCorresponde(id, activar);
        entityManager.refresh(activo);
        if (activar && activo.getEstado_operativo() == EstadoEnum.Baja) {
            throw new SolicitudInvalidaException("Un activo dado de baja no puede entrar en mantenimiento.");
        }
        if (cambios == 1) {
            LocalDateTime fecha = LocalDateTime.now(ZoneOffset.UTC);
            historico.save(HistoricoEstado.builder()
                    .activo(activo)
                    .campo("mantenimiento_activo")
                    .valorNuevo(Boolean.toString(activar))
                    .descripcion(descripcion(activar, responsable, motivo))
                    .fechaCambio(fecha)
                    .build());
            if (conexion != null) {
                if (activar && !"Inactivo".equals(conexion.getEstadoConexion())) {
                    String anterior = conexion.getEstadoConexion();
                    conexion.setEstadoConexion("Inactivo");
                    registrarConexion(activo, "Inactivo",
                            PAUSA_CONEXION + anterior, fecha);
                } else if (!activar && activo.getEstado_operativo() != EstadoEnum.Baja
                        && "Inactivo".equals(conexion.getEstadoConexion())) {
                    var inicio = historico.findFirstByActivo_IdAndCampoAndValorNuevoOrderByFechaCambioDescIdDesc(
                            id, "mantenimiento_activo", "true");
                    var pausa = historico.findFirstByActivo_IdAndCampoAndValorNuevoOrderByFechaCambioDescIdDesc(
                            id, "estado_conexion", "Inactivo");
                    if (inicio.isPresent() && pausa.isPresent()
                            && !pausa.get().getFechaCambio().isBefore(inicio.get().getFechaCambio())
                            && pausa.get().getDescripcion().startsWith(PAUSA_CONEXION)) {
                        String anterior = pausa.get().getDescripcion().substring(PAUSA_CONEXION.length());
                        if ("Activo".equals(anterior) || "Sin conexión".equals(anterior)) {
                            conexion.setEstadoConexion(anterior);
                            registrarConexion(activo, anterior,
                                    "Monitoreo SNMP reanudado al finalizar mantenimiento", fecha);
                        }
                    }
                }
            }
        }
        return new MantenimientoActivoRespuesta(id,
                Boolean.TRUE.equals(activo.getMantenimientoActivo()), cambios == 1);
    }

    private void registrarConexion(Activo activo, String estado, String descripcion, LocalDateTime fecha) {
        historico.save(HistoricoEstado.builder()
                .activo(activo)
                .campo("estado_conexion")
                .valorNuevo(estado)
                .descripcion(descripcion)
                .fechaCambio(fecha)
                .build());
    }

    private static String detalle(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        if (valor.length() > 255 || valor.chars().anyMatch(Character::isISOControl)) {
            throw new SolicitudInvalidaException("El campo " + campo + " debe ser válido.");
        }
        return valor.trim();
    }

    private static String descripcion(boolean activar, String responsable, String motivo) {
        StringBuilder texto = new StringBuilder(activar ? "Mantenimiento iniciado" : "Mantenimiento finalizado");
        if (responsable != null) texto.append(". Responsable: ").append(responsable);
        if (motivo != null) texto.append(". Motivo: ").append(motivo);
        return texto.toString();
    }
}
