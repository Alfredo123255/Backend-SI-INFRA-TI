package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlertaActivoServiceTest {
    @Autowired AlertaActivoService service;
    @Autowired JdbcTemplate jdbc;
    private final LocalDateTime ahora=LocalDateTime.now(ZoneOffset.UTC);

    @Test
    void cp117_122_variasAlertasPorEquipoYSeveridadAntesDeFecha() {
        long id=crear();
        insertar(id,"estado_operativo",null,null,"Crítica",ahora.minusHours(2));
        insertar(id,"estado_conexion",null,null,"Crítica",ahora.minusHours(1));
        insertar(id,"estado","disco","D-1","Media",ahora);
        insertar(id,"estado","fuente_poder","P-1","Media",ahora.minusMinutes(1));
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Sin conexión' WHERE activo_id=?",id);
        var filas=service.listar(id,null,null);
        assertEquals(4,filas.size());
        assertEquals(List.of("Crítica","Crítica","Media","Media"),filas.stream().map(a->a.severidad()).toList());
        assertEquals("estado_conexion",filas.getFirst().campo());
        assertEquals(4,filas.stream().map(a->a.alertaId()).distinct().count());
        // Un evento posterior de otro campo no sustituye ninguna condicion.
        jdbc.update("INSERT INTO historico_estado(activo_id,campo,valor_nuevo,descripcion,fecha_cambio) VALUES (?,'mantenimiento_activo','false','Otro evento',?)",id,ahora.plusMinutes(1));
        assertEquals(4,service.listar(id,null,null).size());
        assertEquals(2,service.listar(id,"Media","eventos").size());
    }

    @Test
    void recuperacionParcialYGetNoEscribeHistoricoNiAlertas() {
        long id=crear();
        insertar(id,"estado","disco","D-1","Media",ahora);
        insertar(id,"estado","fuente_poder","P-1","Media",ahora);
        var filas=service.listar(id,null,null);
        int antes=jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=?",Integer.class,id);
        service.listar(id,null,null);service.listar(id,null,null);
        assertEquals(antes,jdbc.queryForObject("SELECT count(*) FROM historico_estado WHERE activo_id=?",Integer.class,id));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM alerta WHERE activo_id=?",Integer.class,id));
        jdbc.update("UPDATE alerta SET fecha_resolucion=? WHERE id=?",ahora.plusSeconds(1),filas.getFirst().alertaId());
        assertEquals(1,service.listar(id,null,null).size());
    }

    @Test
    void cp118_excluyeMantenimientoYBajaSinCerrarAlertas() {
        long id=crear();insertar(id,"umbral_cpu_uso_ghz",null,null,"Crítica",ahora);
        jdbc.update("UPDATE activo SET mantenimiento_activo=true WHERE id=?",id);
        assertTrue(service.listar(id,null,null).isEmpty());
        jdbc.update("UPDATE activo SET mantenimiento_activo=false,estado_operativo='Baja' WHERE id=?",id);
        assertTrue(service.listar(id,null,null).isEmpty());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM alerta WHERE activo_id=? AND fecha_resolucion IS NULL",Integer.class,id));
    }

    @Test
    void cp121_eosIncluyeHostnameModeloFechaYDiasRestantes() {
        long id=crear();
        jdbc.update("INSERT INTO modelos(nombre_modelo) VALUES ('Modelo CP121') ON CONFLICT DO NOTHING");
        jdbc.update("UPDATE activo SET modelo='Modelo CP121',fecha_eos=(CURRENT_TIMESTAMP AT TIME ZONE 'America/Lima')::date+30 WHERE id=?",id);
        insertar(id,"umbral_dias_para_eos",null,null,"Media",ahora);
        var a=service.listar(id,null,"eos").getFirst();
        assertNotNull(a.hostname());assertEquals("Modelo CP121",a.modelo());assertNotNull(a.fechaEos());assertEquals(30,a.diasRestantes());
        assertTrue(service.listar(id,null,"metricas").isEmpty());
    }

    @Test
    void cp123_sinCondicionesDevuelveListaVaciaYValidaFiltros() {
        long id=crear();assertTrue(service.listar(id,null,null).isEmpty());
        assertThrows(SolicitudInvalidaException.class,()->service.listar(0L,null,null));
        assertThrows(SolicitudInvalidaException.class,()->service.listar(null,"Baja",null));
        assertThrows(SolicitudInvalidaException.class,()->service.listar(null,null,"otro"));
    }

    private long crear() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        String nombre="ALERTA-JAVA-"+UUID.randomUUID();jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",nombre);
        long id=jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo) VALUES (?,?,?,'SERVIDOR','Encendido') RETURNING id",Long.class,nombre,nombre,nombre);
        jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,'RACKEABLE')",id);
        jdbc.update("INSERT INTO monitoreo_snmp(ip_gestion,usuario,clave,frecuencia_actualizacion,estado_conexion,activo_id) VALUES ('127.0.0.1:1','test','test',120,'Activo',?)",id);
        return id;
    }
    private void insertar(long id,String campo,String tipo,String serial,String severidad,LocalDateTime inicio) {
        jdbc.update("INSERT INTO alerta(activo_id,campo,componente_tipo,componente_sn,severidad,descripcion,fecha_inicio,ultima_verificacion) VALUES (?,?,?,?,?,'Condicion de prueba',?,?)",id,campo,tipo,serial,severidad,inicio,inicio);
    }
}
