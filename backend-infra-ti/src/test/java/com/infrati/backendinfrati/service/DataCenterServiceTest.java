package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.*;
import com.infrati.backendinfrati.exception.*;
import com.infrati.backendinfrati.model.DataCenter;
import com.infrati.backendinfrati.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DataCenterServiceTest {
    private final DataCenterRepository repository = mock(DataCenterRepository.class);
    private final ClusterRepository clusters = mock(ClusterRepository.class);
    private final ActivoRepository activos = mock(ActivoRepository.class);
    private final DataCenterService service = new DataCenterService(repository, clusters, activos);

    private DataCenter existente() {
        var dc = DataCenter.builder().nombre("DC").latitud("-12").longitud("-77").build();
        when(repository.findById("DC")).thenReturn(Optional.of(dc));
        when(repository.saveAndFlush(any(DataCenter.class))).thenAnswer(i -> i.getArgument(0));
        return dc;
    }

    @Test
    void creaConCoordenadasYNombreNormalizado() {
        existente();
        var creado = service.crear(new DataCenterRequest(" Nuevo & DC ", " -12.0464 ", "-77.0428"));
        assertEquals("Nuevo & DC", creado.nombre());
        assertEquals("-12.0464", creado.latitud());
        assertEquals("-77.0428", creado.longitud());
    }

    @Test
    void coordenadasOpcionalesYExtremosValidos() {
        existente();
        var vacio = service.crear(new DataCenterRequest("Nuevo", "", null));
        assertNull(vacio.latitud());
        assertNull(vacio.longitud());
        assertEquals("90", service.crear(new DataCenterRequest("Polo", "90", "-180")).latitud());
        assertEquals("-90", service.crear(new DataCenterRequest("Polo", "-90", "180")).latitud());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-120", "90.01", "NaN", "Infinity", "abc"})
    void rechazaLatitudFueraDeRangoONoNumerica(String valor) {
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(new DataCenterRequest("DC", valor, "0")));
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-181", "180.01", "NaN", "Infinity", "abc"})
    void rechazaLongitudFueraDeRangoONoNumerica(String valor) {
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(new DataCenterRequest("DC", "0", valor)));
    }

    @Test
    void rechazaNombreVacioDuplicadosYCreacionConcurrente() {
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(new DataCenterRequest(" ", null, null)));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(null));
        when(repository.existsById("DC")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.crear(new DataCenterRequest("DC", null, null)));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("PK"));
        assertThrows(RecursoDuplicadoException.class, () -> service.crear(new DataCenterRequest("OTRO", null, null)));
    }

    @Test
    void listaOrdenadaConConteosIndependientesYCoordenadas() {
        var dc = existente();
        when(repository.findAll(Sort.by("nombre"))).thenReturn(List.of(dc));
        when(clusters.countByDatacenter("DC")).thenReturn(2L);
        when(activos.countByUbicacion("DC")).thenReturn(4L);
        var respuesta = service.listar().getFirst();
        assertEquals("-12", respuesta.latitud());
        assertEquals(2L, respuesta.clusters());
        assertEquals(4L, respuesta.equipos());
    }

    @Test
    void actualizaYPermiteVaciarCoordenadasSinRenombrar() {
        existente();
        var actualizado = service.actualizar("DC", new ActualizarDataCenterRequest("-13", "-78"));
        assertEquals("DC", actualizado.nombre());
        assertEquals("-13", actualizado.latitud());
        assertEquals("-78", actualizado.longitud());
        assertNull(service.actualizar("DC", new ActualizarDataCenterRequest(null, "")).latitud());
    }

    @Test
    void rechazaActualizacionInvalidaSinModificarEntidad() {
        var dc = existente();
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar("DC", new ActualizarDataCenterRequest("-120", "0")));
        assertEquals("-12", dc.getLatitud());
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar("NO", new ActualizarDataCenterRequest(null, null)));
    }

    @Test
    void eliminaDatacenterVacioSinCascada() {
        var dc = existente();
        service.eliminar("DC");
        verify(repository).delete(dc);
        verify(repository).flush();
        verify(clusters, never()).delete(any());
        verify(activos, never()).delete(any());
    }

    @Test
    void bloqueaDatacenterConClustersAunqueNoTengaEquipos() {
        existente();
        when(clusters.countByDatacenter("DC")).thenReturn(1L);
        assertThrows(RecursoDuplicadoException.class, () -> service.eliminar("DC"));
        verify(repository, never()).delete(any());
    }

    @Test
    void bloqueaDatacenterConEquiposAunqueNoTengaClusters() {
        existente();
        when(activos.countByUbicacion("DC")).thenReturn(1L);
        assertThrows(RecursoDuplicadoException.class, () -> service.eliminar("DC"));
        verify(repository, never()).delete(any());
    }

    @Test
    void eliminacionInexistenteYAsignacionConcurrente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar("NO"));
        existente();
        doThrow(new DataIntegrityViolationException("FK")).when(repository).flush();
        assertThrows(RecursoDuplicadoException.class, () -> service.eliminar("DC"));
    }
}
