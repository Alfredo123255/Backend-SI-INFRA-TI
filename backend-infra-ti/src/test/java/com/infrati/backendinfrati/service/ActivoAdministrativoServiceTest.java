package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.model.Activos.Modelo;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.model.HistoricoEstado;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import com.infrati.backendinfrati.repository.ModeloRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class ActivoAdministrativoServiceTest {
    private final ActivoRepository repository = mock(ActivoRepository.class);
    private final ModeloRepository modeloRepository = mock(ModeloRepository.class);
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);
    private final HistoricoEstadoRepository historico = mock(HistoricoEstadoRepository.class);
    private final ActivoAdministrativoService service = new ActivoAdministrativoService(
            repository, modeloRepository, clusterRepository, historico);

    private void encontrado(Activo activo) {
        when(repository.findById(activo.getId())).thenReturn(Optional.of(activo));
    }

    private static ActualizarAdministrativosActivoRequest solicitud(String responsable,
            String orden, LocalDate eos, LocalDate eol, LocalDate soporte, String red, String modo, Integer iops) {
        return new ActualizarAdministrativosActivoRequest(responsable, orden, eos, eol, soporte, red, modo, iops, null);
    }

    @Test
    void camposNulosConservanDatosYNoCambianSnmpEnServidor() {
        Servidor servidor = Servidor.builder().id(1L).hostname("servidor-01").cluster("ORIGINAL")
                .responsable("Anterior").orden_compra("OC-1")
                .fecha_eos(LocalDate.of(2028, 1, 1))
                .fecha_soporte_so(LocalDate.of(2027, 1, 1)).build();
        encontrado(servidor);

        var respuesta = service.actualizar(1L, solicitud("Nuevo", null, null, null,
                LocalDate.of(2029, 12, 31), null, null, null));

        assertEquals("Nuevo", respuesta.responsable());
        assertEquals("OC-1", respuesta.ordenCompra());
        assertEquals(LocalDate.of(2028, 1, 1), respuesta.fechaEos());
        assertEquals(LocalDate.of(2029, 12, 31), respuesta.fechaSoporteSo());
        assertEquals("servidor-01", servidor.getHostname());
        assertEquals("ORIGINAL", respuesta.cluster());
    }

    @Test
    void actualizaCamposDelSwitch() {
        Switch equipo = Switch.builder().id(2L).tipoRED("LAN").modo_operacion("L2").build();
        encontrado(equipo);
        var respuesta = service.actualizar(2L, solicitud("Redes", null, null, null, null,
                "Campus", null, null));
        assertEquals("Redes", respuesta.responsable());
        assertEquals("Campus", respuesta.tipoRed());
        assertEquals("L2", respuesta.modoOperacion());
    }

    @Test
    void actualizaCamposDeStorageYChasis() {
        Storage storage = Storage.builder().id(3L).iops(500).build();
        encontrado(storage);
        assertEquals(500, service.actualizar(3L, solicitud("Operaciones", null, null, null, null,
                null, null, null)).iops());
        assertEquals("Operaciones", storage.getResponsable());

        ChasisBlade chasis = ChasisBlade.builder().id(4L).orden_compra("OC-1").build();
        encontrado(chasis);
        assertEquals("OC-2", service.actualizar(4L, solicitud(null, "OC-2", null, null, null,
                null, null, null)).ordenCompra());
    }

    @Test
    void rechazaModificacionManualDeIopsSinCambiarStorage() {
        Storage storage = Storage.builder().id(8L).iops(500).responsable("Anterior").build();
        encontrado(storage);

        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar(8L,
                solicitud("Nuevo", null, null, null, null, null, null, 1200)));

        assertEquals(500, storage.getIops());
        assertEquals("Anterior", storage.getResponsable());
    }

    @Test
    void rechazaCamposIncompatiblesSinModificarElActivo() {
        Switch equipo = Switch.builder().id(5L).responsable("Anterior").build();
        encontrado(equipo);
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar(5L,
                solicitud("Nuevo", null, null, null, LocalDate.of(2030, 1, 1), null, null, null)));
        assertEquals("Anterior", equipo.getResponsable());
    }

    @Test
    void devuelveNoEncontrado() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(999L,
                solicitud(null, null, null, null, null, null, null, null)));
    }

    @Test
    void actualizaEolDelModeloCompartido() {
        Switch equipo = Switch.builder().id(6L).modelo("CX-6300").build();
        Modelo modelo = Modelo.builder().nombreModelo("CX-6300")
                .fechaEol(LocalDate.of(2029, 1, 1)).build();
        encontrado(equipo);
        when(modeloRepository.findById("CX-6300")).thenReturn(Optional.of(modelo));

        var respuesta = service.actualizar(6L, solicitud(null, null, null,
                LocalDate.of(2030, 12, 31), null, null, null, null));

        assertEquals(LocalDate.of(2030, 12, 31), modelo.getFechaEol());
        assertEquals(modelo.getFechaEol(), respuesta.fechaEol());
        service.actualizar(6L, solicitud(null, null, null, null, null, null, null, null));
        assertEquals(LocalDate.of(2030, 12, 31), modelo.getFechaEol());
    }

    @Test
    void rechazaEolSinModeloRegistrado() {
        Storage equipo = Storage.builder().id(7L).modelo("DESCONOCIDO").build();
        encontrado(equipo);
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar(7L,
                solicitud(null, null, null, LocalDate.of(2030, 1, 1), null, null, null, null)));
    }

    @Test
    void cambiaClusterExistenteYRegistraEventoSoloSiCambio() {
        Servidor servidor = Servidor.builder().id(9L).cluster("CLUSTER-ANTERIOR").build();
        encontrado(servidor);
        when(clusterRepository.existsById("CLUSTER-NUEVO")).thenReturn(true);
        var solicitud = new ActualizarAdministrativosActivoRequest(null, null, null, null,
                null, null, null, null, " CLUSTER-NUEVO ");

        assertEquals("CLUSTER-NUEVO", service.actualizar(9L, solicitud).cluster());
        ArgumentCaptor<HistoricoEstado> evento = ArgumentCaptor.forClass(HistoricoEstado.class);
        verify(historico).save(evento.capture());
        assertEquals("cluster", evento.getValue().getCampo());
        assertEquals("CLUSTER-NUEVO", evento.getValue().getValorNuevo());
        assertEquals("Clúster cambiado de CLUSTER-ANTERIOR a CLUSTER-NUEVO",
                evento.getValue().getDescripcion());

        service.actualizar(9L, solicitud);
        verify(historico).save(any(HistoricoEstado.class));
    }

    @Test
    void rechazaClusterInexistenteSinAlterarActivo() {
        Servidor servidor = Servidor.builder().id(10L).cluster("CLUSTER-ANTERIOR").build();
        encontrado(servidor);
        var solicitud = new ActualizarAdministrativosActivoRequest("Nuevo", null, null, null,
                null, null, null, null, "NO-EXISTE");

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(10L, solicitud));
        assertEquals("CLUSTER-ANTERIOR", servidor.getCluster());
        verify(historico, never()).save(any(HistoricoEstado.class));
    }
}
