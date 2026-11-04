package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarClusterRequest;
import com.infrati.backendinfrati.dto.ClusterRespuesta;
import com.infrati.backendinfrati.dto.CrearClusterRequest;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Cluster;
import com.infrati.backendinfrati.model.Enum.AmbienteEnum;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.DataCenterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class ClusterService {
    private final ClusterRepository clusters;
    private final DataCenterRepository datacenters;
    private final ActivoRepository activos;

    public ClusterService(ClusterRepository clusters, DataCenterRepository datacenters, ActivoRepository activos) {
        this.clusters = clusters;
        this.datacenters = datacenters;
        this.activos = activos;
    }

    @Transactional(readOnly = true)
    public List<ClusterRespuesta> listar() {
        return clusters.findAll(Sort.by("nombre")).stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public ClusterRespuesta crear(CrearClusterRequest solicitud) {
        if (solicitud == null) throw new SolicitudInvalidaException("Debe enviar los datos del clúster.");
        String nombre = ValidacionEntornos.requerido(solicitud.nombre(), "nombre", 100);
        AmbienteEnum ambiente = ambiente(solicitud.ambiente());
        String datacenter = datacenter(solicitud.datacenter());
        if (clusters.existsById(nombre)) throw new RecursoDuplicadoException("El clúster " + nombre + " ya existe.");
        try {
            return aRespuesta(clusters.saveAndFlush(Cluster.builder()
                    .nombre(nombre).ambiente(ambiente).datacenter(datacenter).build()));
        } catch (DataIntegrityViolationException error) {
            throw new RecursoDuplicadoException("No se pudo crear el clúster; verifica que el nombre sea único y el datacenter exista.");
        }
    }

    @Transactional
    public ClusterRespuesta actualizar(String nombre, ActualizarClusterRequest solicitud) {
        Cluster cluster = buscar(nombre);
        if (solicitud == null) throw new SolicitudInvalidaException("Debe enviar los datos del clúster.");
        AmbienteEnum ambiente = ambiente(solicitud.ambiente());
        String datacenter = datacenter(solicitud.datacenter());
        cluster.setAmbiente(ambiente);
        cluster.setDatacenter(datacenter);
        try {
            return aRespuesta(clusters.saveAndFlush(cluster));
        } catch (DataIntegrityViolationException error) {
            throw new RecursoDuplicadoException("No se pudo actualizar el clúster; verifica que el datacenter exista.");
        }
    }

    @Transactional
    public void eliminar(String nombre) {
        Cluster cluster = buscar(nombre);
        long equipos = activos.countByCluster(nombre);
        if (equipos > 0) throw new RecursoDuplicadoException("No se puede eliminar " + nombre + " porque tiene "
                + equipos + " equipo(s) asignado(s). Reasigna esos equipos a otro cluster antes de eliminarlo.");
        try {
            clusters.delete(cluster);
            clusters.flush();
        } catch (DataIntegrityViolationException error) {
            // La FK también protege frente a asignaciones concurrentes. Nunca se elimina en cascada.
            throw new RecursoDuplicadoException("No se puede eliminar el clúster porque tiene equipos asociados. Reasígnalos antes de eliminarlo.");
        }
    }

    private Cluster buscar(String nombre) {
        return clusters.findById(nombre).orElseThrow(() -> new RecursoNoEncontradoException("Clúster " + nombre + " no encontrado."));
    }

    private String datacenter(String valor) {
        String nombre = ValidacionEntornos.requerido(valor, "datacenter", 255);
        if (!datacenters.existsById(nombre)) throw new RecursoNoEncontradoException("Datacenter " + nombre + " no encontrado.");
        return nombre;
    }

    private AmbienteEnum ambiente(String valor) {
        String texto = ValidacionEntornos.requerido(valor, "ambiente", 32);
        try {
            return AmbienteEnum.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new SolicitudInvalidaException("El ambiente debe ser PRODUCCION, QA o DESARROLLO.");
        }
    }

    private ClusterRespuesta aRespuesta(Cluster cluster) {
        return new ClusterRespuesta(cluster.getNombre(), cluster.getAmbiente().name(),
                cluster.getDatacenter(), activos.countByCluster(cluster.getNombre()));
    }
}
