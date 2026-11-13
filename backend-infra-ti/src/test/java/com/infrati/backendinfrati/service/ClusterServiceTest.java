package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CrearClusterRequest;
import com.infrati.backendinfrati.dto.ActualizarClusterRequest;
import com.infrati.backendinfrati.repository.ActivoRepository;
import java.util.Optional;
import static org.mockito.Mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Cluster;
import com.infrati.backendinfrati.model.Enum.AmbienteEnum;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.DataCenterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterServiceTest {
    private final ClusterRepository clusters = mock(ClusterRepository.class);
    private final DataCenterRepository datacenters = mock(DataCenterRepository.class);
    private final ActivoRepository activos = mock(ActivoRepository.class);
    private final ClusterService service = new ClusterService(clusters, datacenters, activos);

    @Test
    void creaClusterConAmbienteNormalizadoYDatacenterExistente() {
        when(datacenters.existsById("DC-LIMA")).thenReturn(true);
        when(clusters.saveAndFlush(any(Cluster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var creado = service.crear(new CrearClusterRequest(" CLUSTER-NUEVO ", "qa", " DC-LIMA "));

        assertEquals("CLUSTER-NUEVO", creado.nombre());
        assertEquals("QA", creado.ambiente());
        assertEquals("DC-LIMA", creado.datacenter());
    }

    @Test
    void rechazaDuplicadosYReferenciasInvalidas() {
        when(datacenters.existsById("DC-LIMA")).thenReturn(true);
        when(clusters.existsById("CLUSTER-NUEVO")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.crear(
                new CrearClusterRequest("CLUSTER-NUEVO", "QA", "DC-LIMA")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(
                new CrearClusterRequest("OTRO", "QA", "DC-INEXISTENTE")));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(
                new CrearClusterRequest("OTRO", "INVALIDO", "DC-LIMA")));
    }

    @Test
    void listaLosClustersOrdenados() {
        when(clusters.findAll(Sort.by("nombre"))).thenReturn(List.of(
                Cluster.builder().nombre("A").ambiente(AmbienteEnum.PRODUCCION).datacenter("DC-1").build(),
                Cluster.builder().nombre("B").ambiente(AmbienteEnum.QA).datacenter("DC-2").build()));

        var listado = service.listar();

        assertEquals(List.of("A", "B"), listado.stream().map(c -> c.nombre()).toList());
    }

    private Cluster existente() {
        var cluster = Cluster.builder().nombre("CL").ambiente(AmbienteEnum.QA).datacenter("DC").build();
        when(clusters.findById("CL")).thenReturn(Optional.of(cluster));
        return cluster;
    }

    @Test
    void incluyeConteoDeActivos() {
        var cluster = existente();
        when(clusters.findAll(any(Sort.class))).thenReturn(List.of(cluster));
        when(activos.countByCluster("CL")).thenReturn(3L);
        assertEquals(3L, service.listar().getFirst().activos());
    }

    @Test
    void actualizaSoloAmbienteYDatacenterConservandoLaClave() {
        existente();
        when(datacenters.existsById("DC-2")).thenReturn(true);
        when(clusters.saveAndFlush(any(Cluster.class))).thenAnswer(i -> i.getArgument(0));
        var respuesta = service.actualizar("CL", new ActualizarClusterRequest("DESARROLLO", "DC-2"));
        assertEquals("CL", respuesta.nombre());
        assertEquals("DESARROLLO", respuesta.ambiente());
        assertEquals("DC-2", respuesta.datacenter());
    }

    @Test
    void rechazaActualizacionInvalidaOInexistente() {
        existente();
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar("CL", new ActualizarClusterRequest("OTRO", "DC")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar("CL", new ActualizarClusterRequest("QA", "NO")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar("NO", new ActualizarClusterRequest("QA", "DC")));
    }

    @Test
    void eliminaClusterVacioSinCascada() {
        var cluster = existente();
        service.eliminar("CL");
        verify(clusters).delete(cluster);
        verify(clusters).flush();
        verify(activos, never()).delete(any());
    }

    @Test
    void bloqueaEliminacionConActivosInclusoDadosDeBaja() {
        existente();
        when(activos.countByCluster("CL")).thenReturn(1L);
        assertThrows(RecursoDuplicadoException.class, () -> service.eliminar("CL"));
        verify(clusters, never()).delete(any());
    }

    @Test
    void eliminacionInexistenteYAsignacionConcurrente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar("NO"));
        existente();
        doThrow(new DataIntegrityViolationException("FK")).when(clusters).flush();
        assertThrows(RecursoDuplicadoException.class, () -> service.eliminar("CL"));
    }

    @Test
    void rechazaNombreYCamposVacios() {
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(new CrearClusterRequest(" ", "QA", "DC")));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(new CrearClusterRequest("CL", "", "DC")));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(null));
    }
}
