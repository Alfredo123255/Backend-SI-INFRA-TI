package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.client.EtlClient;
import com.infrati.backendinfrati.dto.ActivoCreado;
import com.infrati.backendinfrati.dto.RegistroConexionRequest;
import com.infrati.backendinfrati.exception.EtlException;
import com.infrati.backendinfrati.model.MonitoreoSnmp;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConexionSnmpServiceTest {
    private final EtlClient cliente = mock(EtlClient.class);
    private final MonitoreoSnmpRepository conexiones = mock(MonitoreoSnmpRepository.class);
    private final ClusterRepository clusters = mock(ClusterRepository.class);
    private final ConexionSnmpService servicio = new ConexionSnmpService(cliente, conexiones, clusters);

    private static RegistroConexionRequest solicitud() {
        return new RegistroConexionRequest("127.0.0.20:16600", "monitor", "clave-auth",
                "clave-priv", 300, "CLUSTER-LAB-LOCAL");
    }

    @Test
    void guardaConexionAntesDePedirAltaAlEtl() {
        when(clusters.existsById("CLUSTER-LAB-LOCAL")).thenReturn(true);
        when(conexiones.saveAndFlush(any())).thenAnswer(invocacion -> {
            MonitoreoSnmp fila = invocacion.getArgument(0);
            assertEquals("Inactivo", fila.getEstadoConexion());
            assertEquals("clave-priv", fila.getClavePrivacidad());
            fila.setId(12L);
            return fila;
        });
        ActivoCreado creado = new ActivoCreado(true, 44L, 12L, "CLUSTER-LAB-LOCAL", "SW-44",
                "sw01", "HPE Aruba Networking", "SWITCH", "Encendido", "CX 6300M",
                "DC-LAB-LOCAL", Map.of(), 4, 2);
        when(cliente.crearActivo(any())).thenReturn(creado);
        assertEquals(44L, servicio.registrarYCrear(solicitud()).activoId());
        InOrder orden = inOrder(conexiones, cliente);
        orden.verify(conexiones).saveAndFlush(any());
        orden.verify(cliente).crearActivo(argThat(req -> req.conexionId() == 12L
                && req.clusterId().equals("CLUSTER-LAB-LOCAL")));
        verify(conexiones, never()).eliminarSiNoVinculada(any());
    }

    @Test
    void limpiaSoloConexionNoVinculadaSiFallaElEtl() {
        when(clusters.existsById("CLUSTER-LAB-LOCAL")).thenReturn(true);
        when(conexiones.saveAndFlush(any())).thenAnswer(invocacion -> {
            MonitoreoSnmp fila = invocacion.getArgument(0);
            fila.setId(12L);
            return fila;
        });
        when(cliente.crearActivo(any())).thenThrow(new EtlException(HttpStatus.GATEWAY_TIMEOUT, "Sin respuesta SNMP"));
        when(conexiones.eliminarSiNoVinculada(12L)).thenReturn(1);
        assertThrows(EtlException.class, () -> servicio.registrarYCrear(solicitud()));
        verify(conexiones).eliminarSiNoVinculada(12L);
    }
}
