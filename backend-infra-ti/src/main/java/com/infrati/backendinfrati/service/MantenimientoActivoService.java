package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.dto.MantenimientoActivoRespuesta;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class MantenimientoActivoService {
    private final ActivoRepository activos;
    private final HistoricoEstadoRepository historico;
    private final EntityManager entityManager;

    public MantenimientoActivoService(ActivoRepository activos,
            HistoricoEstadoRepository historico, EntityManager entityManager) {
        this.activos = activos;
        this.historico = historico;
        this.entityManager = entityManager;
    }

    @Transactional
    public MantenimientoActivoRespuesta cambiar(Long id, CambiarMantenimientoRequest solicitud) {
        if (solicitud == null || solicitud.mantenimientoActivo() == null) {
            throw new SolicitudInvalidaException("mantenimientoActivo debe ser true o false.");
        }
        String responsable = detalle(solicitud.responsable(), "responsable");
        String motivo = detalle(solicitud.motivo(), "motivo");
        boolean activar = solicitud.mantenimientoActivo();
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
            historico.save(HistoricoEstado.builder()
                    .activo(activo)
                    .campo("mantenimiento_activo")
                    .valorNuevo(Boolean.toString(activar))
                    .descripcion(descripcion(activar, responsable, motivo))
                    .fechaCambio(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        return new MantenimientoActivoRespuesta(id,
                Boolean.TRUE.equals(activo.getMantenimientoActivo()), cambios == 1);
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
