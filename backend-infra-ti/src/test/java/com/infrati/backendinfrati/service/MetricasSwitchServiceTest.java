package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MetricasSwitchServiceTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MetricasServidorService service;
    private static final LocalDate DIA=LocalDate.of(2026,10,15);
    private static final LocalDateTime PRIMERA=DIA.atTime(5,10), ULTIMA=DIA.atTime(5,20);

    @Test
    void cp077_082_083_084_091_sumaAntesDePromediarYUsaNombresDelInventario() {
        long id=crear(); prepararPuertos(id);
        lectura(id,"cpu_uso_ghz",null,"0.864",PRIMERA);
        lectura(id,"ram_uso_gb",null,"1.68",PRIMERA);
        lectura(id,"equipo_temperatura_c",null,"28",PRIMERA);
        lectura(id,"equipo_consumo_w",null,"210",PRIMERA);
        var r=service.consultar(id,"custom",DIA,DIA,List.of());var s=r.servidor();
        assertEquals("SWITCH",s.tipo());assertEquals(6,s.metricas().size());
        assertFalse(s.metricas().containsKey("ram_uso_gb"));
        igual("18",s.metricas().get("cpu_uso_ghz").porcentaje());
        igual("28",s.metricas().get("equipo_temperatura_c").actual());
        igual("210",s.metricas().get("equipo_consumo_w").actual());
        // Primera lectura: 100+300+20=420. Segunda: 200+20=220. Promedio: 320.
        igual("320",s.metricas().get("red_trafico_entrada_mbps").valores().getFirst());
        igual("220",s.metricas().get("red_trafico_entrada_mbps").actual());
        // Máximo por instante: 30 y 20; promedio del intervalo: 25.
        igual("25",s.metricas().get("red_utilizacion_pct").valores().getFirst());
        igual("20",s.metricas().get("red_utilizacion_pct").actual());
        assertEquals("1/1/1",s.puertoMasCargado().numero());
        assertEquals(4,s.puertosSwitch().size());
        igual("150",s.puertosSwitch().getFirst().entrada().getFirst());
        assertNull(s.metricas().get("red_trafico_entrada_mbps").valores().get(1));
    }

    @Test
    void cp088_099_100_noInventaVelocidadNiReutilizaTraficoDePuertosApagados() {
        long id=crear();prepararPuertos(id);
        var s=service.consultar(id,"custom",DIA,DIA,List.of()).servidor();
        var apagado=s.puertosSwitch().get(2);assertEquals("Apagado",apagado.estado());
        assertNull(apagado.entradaActual());assertNull(apagado.salidaActual());assertNull(apagado.utilizacionActual());
        var sinVelocidad=s.puertosSwitch().get(3);
        assertNull(sinVelocidad.anchoBandaGbps());assertNull(sinVelocidad.utilizacionActual());
        igual("20",sinVelocidad.entradaActual());
        // La última lectura del activo manda, aunque exista una medición con fecha posterior.
        lectura(id,"red_utilizacion_pct","1/1/2","99",ULTIMA.plusHours(1));
        lectura(id,"red_trafico_entrada_mbps","1/1/2","9999",ULTIMA.plusHours(1));
        s=service.consultar(id,"custom",DIA,DIA,List.of()).servidor();
        assertEquals("1/1/1",s.puertoMasCargado().numero());
        igual("220",s.metricas().get("red_trafico_entrada_mbps").actual());
        assertNull(s.puertosSwitch().get(1).entradaActual());
    }

    @Test
    void cp092_096_098_103_cpuSinCapacidadConexionCaidaComparacionesYPeriodoVacio() {
        long id=crear();prepararPuertos(id);
        lectura(id,"cpu_uso_ghz",null,"0.864",PRIMERA);
        jdbc.update("UPDATE cpu SET velocidad_ghz=NULL WHERE activo_id=?",id);
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Sin conexión' WHERE activo_id=?",id);
        long otro=crear(),tercero=crear(),cuarto=crear();
        var r=service.consultar(id,"custom",DIA,DIA,List.of(otro,tercero,cuarto));
        assertEquals(3,r.comparados().size());assertEquals("Sin conexión",r.servidor().estadoConexion());
        assertNotNull(r.servidor().ultimaMedicion());
        assertNull(r.servidor().metricas().get("cpu_uso_ghz").actual());
        assertTrue(r.servidor().metricas().get("cpu_uso_ghz").valores().stream().allMatch(v->v==null));
        for(var comparado:r.comparados()) assertEquals(24,comparado.metricas().get("red_utilizacion_pct").valores().size());
        assertThrows(SolicitudInvalidaException.class,()->service.consultar(id,"24h",null,null,List.of(otro,tercero,cuarto,id)));
        assertThrows(SolicitudInvalidaException.class,()->service.consultar(id,"24h",null,null,List.of(otro,otro)));
        var vacio=service.consultar(id,"custom",DIA.minusDays(2),DIA.minusDays(2),List.of());
        assertTrue(vacio.servidor().metricas().values().stream().allMatch(m->m.valores().stream().allMatch(v->v==null)));
    }

    private long crear() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        String nombre="SW-"+UUID.randomUUID();jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",nombre);
        long id=jdbc.queryForObject("""
                INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo,ultima_actualizacion)
                VALUES (?,?,?,'SWITCH','Encendido',?) RETURNING id
                """,Long.class,nombre,nombre,nombre,ULTIMA);
        jdbc.update("INSERT INTO switch(id) VALUES (?)",id);
        jdbc.update("INSERT INTO cpu(activo_id,velocidad_ghz,cantidad_nucleos,estado) VALUES (?,1.2,4,'Encendido')",id);
        jdbc.update("""
                INSERT INTO monitoreo_snmp(ip_gestion,usuario,clave,frecuencia_actualizacion,estado_conexion,activo_id)
                VALUES (?,'prueba','ficticia',60,'Activo',?)
                """,nombre,id);return id;
    }

    private void prepararPuertos(long id) {
        for(int n=1;n<=4;n++) jdbc.update("INSERT INTO puerto_switch(switch_id,numero_puerto,velocidad,estado) VALUES (?,?,?,?)",
                id,"1/1/"+n,n==4?null:"1000 Mbps",n==3?"Apagado":"Encendido");
        for(String puerto:List.of("1/1/1","1/1/2","1/1/3")) lectura(id,"red_ancho_banda_mbps",puerto,"1000",ULTIMA);
        for(LocalDateTime fecha:List.of(PRIMERA,ULTIMA)) {
            lectura(id,"red_trafico_entrada_mbps","1/1/1",fecha.equals(PRIMERA)?"100":"200",fecha);
            lectura(id,"red_trafico_salida_mbps","1/1/1","50",fecha);
            lectura(id,"red_utilizacion_pct","1/1/1",fecha.equals(PRIMERA)?"10":"20",fecha);
            lectura(id,"red_trafico_entrada_mbps","1/1/4","20",fecha);
            lectura(id,"red_trafico_salida_mbps","1/1/4","10",fecha);
            lectura(id,"red_trafico_entrada_mbps","mgmt","99999",fecha);
            lectura(id,"red_trafico_entrada_mbps","if:1","7777",fecha);
        }
        lectura(id,"red_trafico_entrada_mbps","1/1/2","300",PRIMERA);
        lectura(id,"red_utilizacion_pct","1/1/2","30",PRIMERA);
        lectura(id,"red_trafico_entrada_mbps","1/1/3","100",PRIMERA.minusDays(1));
    }
    private void lectura(long id,String nombre,String puerto,String valor,LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO metrica_historica(activo_id,nombre_metrica,componente_tipo,componente_serial,valor,fecha_medicion)
                VALUES (?,?,?,?,?,?)
                """,id,nombre,puerto==null?null:"puerto_switch",puerto,new BigDecimal(valor),fecha);
    }
    private static void igual(String valor,BigDecimal actual) { assertNotNull(actual);assertEquals(0,new BigDecimal(valor).compareTo(actual)); }
}
