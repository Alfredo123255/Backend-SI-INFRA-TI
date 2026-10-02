package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CrearClusterRequest;
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
    private final ClusterService service = new ClusterService(clusters, datacenters);

    @Test
    void creaClusterConAmbienteNormalizadoYDatacenterExistente() {
        when(datacenters.existsById("DC-LAB-LOCAL")).thenReturn(true);
        when(clusters.saveAndFlush(any(Cluster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var creado = service.crear(new CrearClusterRequest(" CLUSTER-NUEVO ", "qa", " DC-LAB-LOCAL "));

        assertEquals("CLUSTER-NUEVO", creado.nombre());
        assertEquals("QA", creado.ambiente());
        assertEquals("DC-LAB-LOCAL", creado.datacenter());
    }

    @Test
    void rechazaDuplicadosYReferenciasInvalidas() {
        when(datacenters.existsById("DC-LAB-LOCAL")).thenReturn(true);
        when(clusters.existsById("CLUSTER-NUEVO")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.crear(
                new CrearClusterRequest("CLUSTER-NUEVO", "QA", "DC-LAB-LOCAL")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(
                new CrearClusterRequest("OTRO", "QA", "DC-INEXISTENTE")));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(
                new CrearClusterRequest("OTRO", "INVALIDO", "DC-LAB-LOCAL")));
    }

    @Test
    void listaLosClustersOrdenados() {
        when(clusters.findAll(Sort.by("nombre"))).thenReturn(List.of(
                Cluster.builder().nombre("A").ambiente(AmbienteEnum.PRODUCCION).datacenter("DC-1").build(),
                Cluster.builder().nombre("B").ambiente(AmbienteEnum.QA).datacenter("DC-2").build()));

        var listado = service.listar();

        assertEquals(List.of("A", "B"), listado.stream().map(c -> c.nombre()).toList());
    }
}
