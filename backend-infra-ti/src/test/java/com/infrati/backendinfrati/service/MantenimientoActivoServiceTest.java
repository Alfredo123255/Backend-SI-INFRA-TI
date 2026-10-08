package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Transactional
class MantenimientoActivoServiceTest {
    @Autowired private MantenimientoActivoService service;
    @Autowired private ActivoRepository activos;
    @Autowired private MonitoreoSnmpRepository conexiones;
    @Autowired private EntityManager entityManager;

    @Test
    void cambiaBooleanoYRegistraUnSoloEventoPorTransicion() {
        Activo activo = activos.findAll().stream()
                .filter(a -> a.getEstado_operativo() != EstadoEnum.Baja)
                .findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene un activo de prueba");
        long id = activo.getId();
        boolean nuevo = !Boolean.TRUE.equals(activo.getMantenimientoActivo());
        long eventosAntes = contarEventos(id);
        var solicitud = new CambiarMantenimientoRequest(nuevo, "Operaciones", "Prueba transaccional");

        var primerResultado = service.cambiar(id, solicitud);
        entityManager.flush();
        assertEquals(nuevo, primerResultado.mantenimientoActivo());
        assertTrue(primerResultado.cambioRegistrado());
        assertEquals(eventosAntes + 1, contarEventos(id));
        var listado = service.listar().stream().filter(item -> item.activoId() == id).findFirst();
        assertEquals(nuevo, listado.isPresent());
        if (nuevo) {
            assertTrue(listado.orElseThrow().descripcion().contains("Prueba transaccional"));
            assertTrue(listado.orElseThrow().desde() != null);
        }

        var repetido = service.cambiar(id, solicitud);
        entityManager.flush();
        assertFalse(repetido.cambioRegistrado());
        assertEquals(eventosAntes + 1, contarEventos(id));
    }

    @Test
    void pausaLaConexionYLaRestauraSinAlterarLaUltimaLectura() {
        Activo activo = activos.findAll().stream()
                .filter(a -> a.getEstado_operativo() != EstadoEnum.Baja
                        && !Boolean.TRUE.equals(a.getMantenimientoActivo())
                        && conexiones.findOneByActivoId(a.getId()).isPresent())
                .findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene un activo con conexión");
        long id = activo.getId();
        var conexion = conexiones.findOneByActivoId(id).orElseThrow();
        var ultimaLectura = conexion.getFechaUltimaActualizacion();
        conexion.setEstadoConexion("Sin conexión");
        long eventosAntes = contarEventosConexion(id);

        service.cambiar(id, new CambiarMantenimientoRequest(true, "Operaciones", "Intervención"));
        entityManager.flush();
        assertEquals("Inactivo", conexion.getEstadoConexion());
        assertEquals(ultimaLectura, conexion.getFechaUltimaActualizacion());

        service.cambiar(id, new CambiarMantenimientoRequest(false, "Operaciones", "Terminada"));
        entityManager.flush();
        assertEquals("Sin conexión", conexion.getEstadoConexion());
        assertEquals(ultimaLectura, conexion.getFechaUltimaActualizacion());
        assertEquals(eventosAntes + 2, contarEventosConexion(id));
    }

    @Test
    void reactivaLaConexionQueEstabaActivaAntesDelMantenimiento() {
        Activo activo = activos.findAll().stream()
                .filter(a -> a.getEstado_operativo() != EstadoEnum.Baja
                        && !Boolean.TRUE.equals(a.getMantenimientoActivo())
                        && conexiones.findOneByActivoId(a.getId()).isPresent())
                .findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene un activo con conexión");
        long id = activo.getId();
        var conexion = conexiones.findOneByActivoId(id).orElseThrow();
        conexion.setEstadoConexion("Activo");
        long eventosAntes = contarEventosConexion(id);

        service.cambiar(id, new CambiarMantenimientoRequest(true, "Operaciones", "Intervención"));
        entityManager.flush();
        assertEquals("Inactivo", conexion.getEstadoConexion());

        service.cambiar(id, new CambiarMantenimientoRequest(false, "Operaciones", "Terminada"));
        entityManager.flush();
        assertEquals("Activo", conexion.getEstadoConexion());
        assertEquals(eventosAntes + 2, contarEventosConexion(id));
    }

    @Test
    void noReactivaUnaConexionQueYaEstabaInactivaAntesDelMantenimiento() {
        Activo activo = activos.findAll().stream()
                .filter(a -> a.getEstado_operativo() != EstadoEnum.Baja
                        && !Boolean.TRUE.equals(a.getMantenimientoActivo())
                        && conexiones.findOneByActivoId(a.getId()).isPresent())
                .findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene un activo con conexión");
        long id = activo.getId();
        var conexion = conexiones.findOneByActivoId(id).orElseThrow();
        conexion.setEstadoConexion("Inactivo");
        long eventosAntes = contarEventosConexion(id);

        service.cambiar(id, new CambiarMantenimientoRequest(true, "Operaciones", "Intervención"));
        service.cambiar(id, new CambiarMantenimientoRequest(false, "Operaciones", "Terminada"));
        entityManager.flush();

        assertEquals("Inactivo", conexion.getEstadoConexion());
        assertEquals(eventosAntes, contarEventosConexion(id));
    }

    private long contarEventos(long id) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM historico_estado WHERE activo_id=:id AND campo='mantenimiento_activo'")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private long contarEventosConexion(long id) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM historico_estado WHERE activo_id=:id AND campo='estado_conexion'")
                .setParameter("id", id).getSingleResult()).longValue();
    }
}
