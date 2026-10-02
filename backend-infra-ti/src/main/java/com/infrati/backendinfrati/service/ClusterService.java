package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ClusterRespuesta;
import com.infrati.backendinfrati.dto.CrearClusterRequest;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Cluster;
import com.infrati.backendinfrati.model.Enum.AmbienteEnum;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.DataCenterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ClusterService {
    private final ClusterRepository clusters;
    private final DataCenterRepository datacenters;

    public ClusterService(ClusterRepository clusters, DataCenterRepository datacenters) {
        this.clusters = clusters;
        this.datacenters = datacenters;
    }

    public List<ClusterRespuesta> listar() {
        return clusters.findAll(Sort.by("nombre")).stream().map(ClusterService::aRespuesta).toList();
    }

    public ClusterRespuesta crear(CrearClusterRequest solicitud) {
        if (solicitud == null) {
            throw new SolicitudInvalidaException("Debe enviar los datos del clúster.");
        }
        String nombre = requerido(solicitud.nombre(), "nombre", 100);
        String datacenter = requerido(solicitud.datacenter(), "datacenter", 255);
        String ambienteTexto = requerido(solicitud.ambiente(), "ambiente", 32);
        AmbienteEnum ambiente;
        try {
            ambiente = AmbienteEnum.valueOf(ambienteTexto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new SolicitudInvalidaException("El ambiente debe ser PRODUCCION, QA o DESARROLLO.");
        }
        if (!datacenters.existsById(datacenter)) {
            throw new RecursoNoEncontradoException("Datacenter " + datacenter + " no encontrado");
        }
        if (clusters.existsById(nombre)) {
            throw new RecursoDuplicadoException("El clúster " + nombre + " ya existe");
        }
        try {
            return aRespuesta(clusters.saveAndFlush(Cluster.builder()
                    .nombre(nombre).ambiente(ambiente).datacenter(datacenter).build()));
        } catch (DataIntegrityViolationException error) {
            throw new RecursoDuplicadoException(
                    "No se pudo crear el clúster; verifica que el nombre sea único y el datacenter exista.");
        }
    }

    private static String requerido(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo
                || valor.chars().anyMatch(Character::isISOControl)) {
            throw new SolicitudInvalidaException("El campo " + campo + " es obligatorio y debe ser válido.");
        }
        return valor.trim();
    }

    private static ClusterRespuesta aRespuesta(Cluster cluster) {
        return new ClusterRespuesta(cluster.getNombre(), cluster.getAmbiente().name(),
                cluster.getDatacenter());
    }
}
