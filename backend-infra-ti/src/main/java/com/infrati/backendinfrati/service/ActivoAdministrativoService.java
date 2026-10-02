package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.dto.DatosAdministrativosActivo;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.repository.ActivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivoAdministrativoService {
    private final ActivoRepository repository;

    public ActivoAdministrativoService(ActivoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DatosAdministrativosActivo actualizar(Long id, ActualizarAdministrativosActivoRequest solicitud) {
        Activo activo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Activo " + id + " no encontrado"));
        validarCamposEspecificos(activo, solicitud);

        if (solicitud.responsable() != null) activo.setResponsable(solicitud.responsable());
        if (solicitud.ordenCompra() != null) activo.setOrden_compra(solicitud.ordenCompra());
        if (solicitud.fechaEos() != null) activo.setFecha_eos(solicitud.fechaEos());
        if (activo instanceof Servidor servidor && solicitud.fechaSoporteSo() != null) {
            servidor.setFecha_soporte_so(solicitud.fechaSoporteSo());
        }
        if (activo instanceof Switch equipo && solicitud.tipoRed() != null) {
            equipo.setTipoRED(solicitud.tipoRed());
        }
        if (activo instanceof Switch equipo && solicitud.modoOperacion() != null) {
            equipo.setModo_operacion(solicitud.modoOperacion());
        }
        if (activo instanceof Storage equipo && solicitud.iops() != null) {
            equipo.setIops(solicitud.iops());
        }
        return new DatosAdministrativosActivo(activo.getId(),
                activo.getTipo_activo() == null ? null : activo.getTipo_activo().name(),
                activo.getResponsable(), activo.getOrden_compra(), activo.getFecha_eos(),
                activo instanceof Servidor servidor ? servidor.getFecha_soporte_so() : null,
                activo instanceof Switch equipo ? equipo.getTipoRED() : null,
                activo instanceof Switch equipo ? equipo.getModo_operacion() : null,
                activo instanceof Storage equipo ? equipo.getIops() : null);
    }

    private static void validarCamposEspecificos(Activo activo,
            ActualizarAdministrativosActivoRequest solicitud) {
        if (solicitud.fechaSoporteSo() != null && !(activo instanceof Servidor)) {
            throw new SolicitudInvalidaException("fechaSoporteSo solo aplica a servidores.");
        }
        if ((solicitud.tipoRed() != null || solicitud.modoOperacion() != null)
                && !(activo instanceof Switch)) {
            throw new SolicitudInvalidaException("tipoRed y modoOperacion solo aplican a switches.");
        }
        if (solicitud.iops() != null && !(activo instanceof Storage)) {
            throw new SolicitudInvalidaException("iops solo aplica a storage.");
        }
        if (solicitud.iops() != null && solicitud.iops() < 0) {
            throw new SolicitudInvalidaException("iops no puede ser negativo.");
        }
    }
}
