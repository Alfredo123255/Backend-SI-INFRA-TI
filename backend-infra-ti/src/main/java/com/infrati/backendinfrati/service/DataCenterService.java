package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarDataCenterRequest;
import com.infrati.backendinfrati.dto.DataCenterRequest;
import com.infrati.backendinfrati.dto.DataCenterRespuesta;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.DataCenter;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.DataCenterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DataCenterService {
    private final DataCenterRepository repository;
    private final ClusterRepository clusters;
    private final ActivoRepository activos;

    public DataCenterService(DataCenterRepository repository, ClusterRepository clusters, ActivoRepository activos) {
        this.repository = repository;
        this.clusters = clusters;
        this.activos = activos;
    }

    @Transactional(readOnly = true)
    public List<DataCenterRespuesta> listar() {
        return repository.findAll(Sort.by("nombre")).stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public DataCenterRespuesta crear(DataCenterRequest solicitud) {
        if (solicitud == null) throw new SolicitudInvalidaException("Debe enviar los datos del datacenter.");
        String nombre = ValidacionEntornos.requerido(solicitud.nombre(), "nombre", 255);
        String latitud = ValidacionEntornos.coordenada(solicitud.latitud(), "latitud", 90);
        String longitud = ValidacionEntornos.coordenada(solicitud.longitud(), "longitud", 180);
        if (repository.existsById(nombre)) throw new RecursoDuplicadoException("El datacenter " + nombre + " ya existe.");
        try {
            return aRespuesta(repository.saveAndFlush(DataCenter.builder().nombre(nombre).latitud(latitud).longitud(longitud).build()));
        } catch (DataIntegrityViolationException error) {
            throw new RecursoDuplicadoException("No se pudo crear el datacenter; verifica que el nombre sea único.");
        }
    }

    @Transactional
    public DataCenterRespuesta actualizar(String nombre, ActualizarDataCenterRequest solicitud) {
        DataCenter datacenter = buscar(nombre);
        if (solicitud == null) throw new SolicitudInvalidaException("Debe enviar los datos del datacenter.");
        String latitud = ValidacionEntornos.coordenada(solicitud.latitud(), "latitud", 90);
        String longitud = ValidacionEntornos.coordenada(solicitud.longitud(), "longitud", 180);
        datacenter.setLatitud(latitud);
        datacenter.setLongitud(longitud);
        return aRespuesta(repository.saveAndFlush(datacenter));
    }

    @Transactional
    public void eliminar(String nombre) {
        DataCenter datacenter = buscar(nombre);
        long agrupaciones = clusters.countByDatacenter(nombre);
        long equipos = activos.countByUbicacion(nombre);
        if (agrupaciones > 0 || equipos > 0) throw new RecursoDuplicadoException("No se puede eliminar " + nombre + " porque tiene "
                + agrupaciones + " cluster(s) y " + equipos + " equipo(s) asociados. Reasígnalos antes de eliminarlo.");
        try {
            repository.delete(datacenter);
            repository.flush();
        } catch (DataIntegrityViolationException error) {
            throw new RecursoDuplicadoException("No se puede eliminar el datacenter porque tiene clusters o equipos asociados. Reasígnalos antes de eliminarlo.");
        }
    }

    private DataCenter buscar(String nombre) {
        return repository.findById(nombre).orElseThrow(() -> new RecursoNoEncontradoException("Datacenter " + nombre + " no encontrado."));
    }

    private DataCenterRespuesta aRespuesta(DataCenter datacenter) {
        return new DataCenterRespuesta(datacenter.getNombre(), datacenter.getLatitud(), datacenter.getLongitud(),
                clusters.countByDatacenter(datacenter.getNombre()), activos.countByUbicacion(datacenter.getNombre()));
    }
}
