package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.dto.DarDeBajaRequest;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CierreAlertasFlujoTest {
    @Autowired MantenimientoActivoService mantenimiento;
    @Autowired BajaActivoService baja;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    private final LocalDateTime lectura=LocalDateTime.now(ZoneOffset.UTC).minusHours(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

    @Test
    void cp118_mantenimientoCierraTodasLasCondicionesSinDeclararRecuperacion() {
        long id=crear();
        var antes=jdbc.queryForList("SELECT descripcion FROM alerta WHERE activo_id=? ORDER BY id",String.class,id);
        mantenimiento.cambiar(id,new CambiarMantenimientoRequest(true,"Operaciones","Revisión programada"));
        em.flush();
        assertEquals(0,abiertas(id));
        assertEquals(4,cierres(id));
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM alerta WHERE activo_id=? AND fecha_resolucion IS NOT NULL AND ultima_verificacion=?",Integer.class,id,lectura));
        var despues=jdbc.queryForList("SELECT descripcion FROM alerta WHERE activo_id=? ORDER BY id",String.class,id);
        for(int i=0;i<antes.size();i++) {
            assertTrue(despues.get(i).startsWith(antes.get(i)));
            assertTrue(despues.get(i).contains("Cerrada por inicio de mantenimiento"));
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND valor_nuevo IN ('Normal','Encendido')",Integer.class,id));
        assertEquals(lectura,jdbc.queryForObject("SELECT ultima_actualizacion FROM activo WHERE id=?",LocalDateTime.class,id));
        assertEquals("Degradado",jdbc.queryForObject("SELECT estado_operativo FROM activo WHERE id=?",String.class,id));
        assertEquals("Inactivo",jdbc.queryForObject("SELECT estado_conexion FROM monitoreo_snmp WHERE activo_id=?",String.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND componente_tipo='disco' AND componente_sn='DISCO-MANT' AND campo='cierre_alerta_estado'",Integer.class,id));
    }

    @Test
    void cp118_repetirMantenimientoNoDuplicaCierresYFinalizarNoReabreDatosAntiguos() {
        long id=crear();
        mantenimiento.cambiar(id,new CambiarMantenimientoRequest(true,"Operaciones","Intervención"));
        em.flush();
        assertFalse(mantenimiento.cambiar(id,new CambiarMantenimientoRequest(true,"Operaciones","Reintento")).cambioRegistrado());
        assertTrue(mantenimiento.cambiar(id,new CambiarMantenimientoRequest(false,"Operaciones","Finalizada")).cambioRegistrado());
        em.flush();
        assertEquals(4,cierres(id));
        assertEquals(0,abiertas(id));
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM alerta WHERE activo_id=?",Integer.class,id));
        assertEquals("Activo",jdbc.queryForObject("SELECT estado_conexion FROM monitoreo_snmp WHERE activo_id=?",String.class,id));
        assertEquals(lectura,jdbc.queryForObject("SELECT ultima_actualizacion FROM activo WHERE id=?",LocalDateTime.class,id));
    }

    @Test
    void cp118_bajaCierraAlertasConSuMotivoYConservaLosEstadosDeComponentes() {
        long id=crear();
        var resultado=baja.darDeBaja(id,new DarDeBajaRequest("Operaciones","Retiro definitivo"));
        em.flush();
        assertEquals(0,abiertas(id));
        assertEquals(4,cierres(id));
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND valor_nuevo='Cerrada por baja'",Integer.class,id));
        assertEquals("Baja",jdbc.queryForObject("SELECT estado_operativo FROM activo WHERE id=?",String.class,id));
        assertEquals("Inactivo",jdbc.queryForObject("SELECT estado_conexion FROM monitoreo_snmp WHERE activo_id=?",String.class,id));
        assertEquals("Degradado",jdbc.queryForObject("SELECT estado FROM disco WHERE activo_id=?",String.class,id));
        assertEquals(resultado.eventosRegistrados(),jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=?",Integer.class,id));
        var repetida=baja.darDeBaja(id,new DarDeBajaRequest("Operaciones","Reintento"));
        em.flush();
        assertEquals(0,repetida.eventosRegistrados());
        assertEquals(4,cierres(id));
    }

    @Test
    void unActivoDeBajaNoEntraEnMantenimientoNiProduceCierresPorAccionInvalida() {
        long id=crear();
        jdbc.update("UPDATE activo SET estado_operativo='Baja' WHERE id=?",id);
        assertThrows(SolicitudInvalidaException.class,()->mantenimiento.cambiar(id,new CambiarMantenimientoRequest(true,"Operaciones","Inválida")));
        assertEquals(4,abiertas(id));
        assertEquals(0,cierres(id));
    }

    private int abiertas(long id) {
        return jdbc.queryForObject("SELECT count(*) FROM alerta WHERE activo_id=? AND fecha_resolucion IS NULL",Integer.class,id);
    }
    private int cierres(long id) {
        return jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=? AND campo LIKE 'cierre_alerta_%'",Integer.class,id);
    }
    private long crear() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        String nombre="CIERRE-TEST-"+UUID.randomUUID();
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",nombre);
        long id=jdbc.queryForObject("""
                INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo,ultima_actualizacion)
                VALUES (?,?,?,'SERVIDOR','Degradado',?) RETURNING id
                """,Long.class,nombre,nombre,nombre,lectura);
        jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,'RACKEABLE')",id);
        jdbc.update("INSERT INTO disco(activo_id,numero_serial,estado) VALUES (?,'DISCO-MANT','Degradado')",id);
        jdbc.update("INSERT INTO monitoreo_snmp(ip_gestion,usuario,clave,frecuencia_actualizacion,estado_conexion,activo_id) VALUES ('127.0.0.1:1','test','test',120,'Activo',?)",id);
        for (String campo:new String[]{"estado_operativo","umbral_equipo_temperatura_c","umbral_dias_para_eos","estado"}) {
            boolean componente=campo.equals("estado");
            jdbc.update("""
                    INSERT INTO alerta(activo_id,campo,componente_tipo,componente_sn,severidad,descripcion,fecha_inicio,ultima_verificacion)
                    VALUES (?,?,?,?,?,?,?,?)
                    """,id,campo,componente?"disco":null,componente?"DISCO-MANT":null,
                    componente?"Media":"Crítica","Condición original: "+campo,lectura,lectura);
        }
        return id;
    }
}
