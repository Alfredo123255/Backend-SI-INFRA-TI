package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.repository.ActivoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivoAdministrativoServiceTest {
    private final ActivoRepository repository = mock(ActivoRepository.class);
    private final ActivoAdministrativoService service = new ActivoAdministrativoService(repository);

    private void encontrado(Activo activo) {
        when(repository.findById(activo.getId())).thenReturn(Optional.of(activo));
    }

    private static ActualizarAdministrativosActivoRequest solicitud(String responsable,
            String orden, LocalDate eos, LocalDate soporte, String red, String modo, Integer iops) {
        return new ActualizarAdministrativosActivoRequest(responsable, orden, eos, soporte, red, modo, iops);
    }

    @Test
    void camposNulosConservanDatosYNoCambianSnmpEnServidor() {
        Servidor servidor = Servidor.builder().id(1L).hostname("servidor-01")
                .responsable("Anterior").orden_compra("OC-1")
                .fecha_eos(LocalDate.of(2028, 1, 1))
                .fecha_soporte_so(LocalDate.of(2027, 1, 1)).build();
        encontrado(servidor);

        var respuesta = service.actualizar(1L, solicitud("Nuevo", null, null,
                LocalDate.of(2029, 12, 31), null, null, null));

        assertEquals("Nuevo", respuesta.responsable());
        assertEquals("OC-1", respuesta.ordenCompra());
        assertEquals(LocalDate.of(2028, 1, 1), respuesta.fechaEos());
        assertEquals(LocalDate.of(2029, 12, 31), respuesta.fechaSoporteSo());
        assertEquals("servidor-01", servidor.getHostname());
    }

    @Test
    void actualizaCamposDelSwitch() {
        Switch equipo = Switch.builder().id(2L).tipoRED("LAN").modo_operacion("L2").build();
        encontrado(equipo);
        var respuesta = service.actualizar(2L, solicitud("Redes", null, null, null,
                "Campus", null, null));
        assertEquals("Redes", respuesta.responsable());
        assertEquals("Campus", respuesta.tipoRed());
        assertEquals("L2", respuesta.modoOperacion());
    }

    @Test
    void actualizaCamposDeStorageYChasis() {
        Storage storage = Storage.builder().id(3L).iops(500).build();
        encontrado(storage);
        assertEquals(1200, service.actualizar(3L, solicitud(null, null, null, null,
                null, null, 1200)).iops());

        ChasisBlade chasis = ChasisBlade.builder().id(4L).orden_compra("OC-1").build();
        encontrado(chasis);
        assertEquals("OC-2", service.actualizar(4L, solicitud(null, "OC-2", null, null,
                null, null, null)).ordenCompra());
    }

    @Test
    void rechazaCamposIncompatiblesSinModificarElActivo() {
        Switch equipo = Switch.builder().id(5L).responsable("Anterior").build();
        encontrado(equipo);
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizar(5L,
                solicitud("Nuevo", null, null, LocalDate.of(2030, 1, 1), null, null, null)));
        assertEquals("Anterior", equipo.getResponsable());
    }

    @Test
    void devuelveNoEncontrado() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(999L,
                solicitud(null, null, null, null, null, null, null)));
    }
}
