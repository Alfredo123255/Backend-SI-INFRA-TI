package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.dto.DatosAdministrativosActivo;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Activos.Modelo;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.ModeloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ActivoAdministrativoService {
    private final ActivoRepository repository;
    private final ModeloRepository modeloRepository;
    private final ClusterRepository clusterRepository;
    private final HistoricoEstadoRepository historico;

    public ActivoAdministrativoService(ActivoRepository repository, ModeloRepository modeloRepository,
            ClusterRepository clusterRepository, HistoricoEstadoRepository historico) {
        this.repository = repository;
        this.modeloRepository = modeloRepository;
        this.clusterRepository = clusterRepository;
        this.historico = historico;
    }

    @Transactional
    public DatosAdministrativosActivo actualizar(Long id, ActualizarAdministrativosActivoRequest solicitud) {
        Activo activo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Activo " + id + " no encontrado"));
        validarCamposEspecificos(activo, solicitud);
        String nuevoCluster = null;
        if (solicitud.cluster() != null) {
            nuevoCluster = solicitud.cluster().trim();
            if (nuevoCluster.isEmpty() || nuevoCluster.length() > 100) {
                throw new SolicitudInvalidaException("Selecciona un clúster válido.");
            }
            if (!clusterRepository.existsById(nuevoCluster)) {
                throw new RecursoNoEncontradoException("Clúster " + nuevoCluster + " no encontrado");
            }
        }
        Modelo modelo = activo.getModelo() == null ? null
                : modeloRepository.findById(activo.getModelo()).orElse(null);
        if (solicitud.fechaEol() != null) {
            if (modelo == null) {
                throw new SolicitudInvalidaException("El activo no tiene un modelo registrado para actualizar fechaEol.");
            }
            modelo.setFechaEol(solicitud.fechaEol());
        }

        if (nuevoCluster != null && !nuevoCluster.equals(activo.getCluster())) {
            String anterior = activo.getCluster();
            activo.setCluster(nuevoCluster);
            historico.save(HistoricoEstado.builder()
                    .activo(activo)
                    .campo("cluster")
                    .valorNuevo(nuevoCluster)
                    .descripcion("Clúster cambiado de " + (anterior == null ? "Sin clúster" : anterior)
                            + " a " + nuevoCluster)
                    .fechaCambio(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
        }

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
        return new DatosAdministrativosActivo(activo.getId(),
                activo.getTipo_activo() == null ? null : activo.getTipo_activo().name(),
                activo.getResponsable(), activo.getOrden_compra(), activo.getFecha_eos(),
                modelo == null ? null : modelo.getFechaEol(),
                activo instanceof Servidor servidor ? servidor.getFecha_soporte_so() : null,
                activo instanceof Switch equipo ? equipo.getTipoRED() : null,
                activo instanceof Switch equipo ? equipo.getModo_operacion() : null,
                activo instanceof Storage equipo ? equipo.getIops() : null,
                activo.getCluster());
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
        if (solicitud.iops() != null) {
            throw new SolicitudInvalidaException("IOPS es una métrica de monitoreo y no se puede editar manualmente.");
        }
    }
}
