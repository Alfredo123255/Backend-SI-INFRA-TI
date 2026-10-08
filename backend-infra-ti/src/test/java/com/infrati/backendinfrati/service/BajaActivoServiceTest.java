package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.DarDeBajaRequest;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Transactional
class BajaActivoServiceTest {
    @Autowired private BajaActivoService service;
    @Autowired private ActivoRepository activos;
    @Autowired private MonitoreoSnmpRepository conexiones;
    @Autowired private EntityManager entityManager;

    @Test
    void daDeBajaEnUnaTransaccionYNoDuplicaEventos() {
        var activo = activos.findAll().stream()
                .filter(a -> a.getEstado_operativo() != EstadoEnum.Baja && a.getCluster() != null)
                .filter(a -> conexiones.findByActivoId(a.getId()).stream()
                        .anyMatch(c -> !"Inactivo".equals(c.getEstadoConexion())))
                .findFirst().orElse(null);
        assumeTrue(activo != null, "La BD de integración no tiene un activo vinculado de prueba");
        long id = activo.getId();
        long eventosAntes = contarEventos(id);

        var respuesta = service.darDeBaja(id, new DarDeBajaRequest("Operaciones", "Retiro de prueba"));
        entityManager.flush();
        entityManager.clear();

        var actualizado = activos.findById(id).orElseThrow();
        assertEquals(EstadoEnum.Baja, actualizado.getEstado_operativo());
        assertNull(actualizado.getCluster());
        assertFalse(Boolean.TRUE.equals(actualizado.getMantenimientoActivo()));
        assertTrue(conexiones.findByActivoId(id).stream()
                .allMatch(c -> "Inactivo".equals(c.getEstadoConexion())));
        assertEquals(1, respuesta.conexionesInactivadas());
        assertTrue(respuesta.cambioRegistrado());
        assertEquals(eventosAntes + respuesta.eventosRegistrados(), contarEventos(id));

        var repetida = service.darDeBaja(id, new DarDeBajaRequest("Operaciones", "Reintento"));
        entityManager.flush();
        assertFalse(repetida.cambioRegistrado());
        assertEquals(0, repetida.eventosRegistrados());
        assertEquals(eventosAntes + respuesta.eventosRegistrados(), contarEventos(id));
    }

    private long contarEventos(long id) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM historico_estado WHERE activo_id=:id")
                .setParameter("id", id).getSingleResult()).longValue();
    }
}
