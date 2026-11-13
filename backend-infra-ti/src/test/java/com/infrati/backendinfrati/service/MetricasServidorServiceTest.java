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
class MetricasServidorServiceTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MetricasServidorService service;

    @Test
    void cp081_cp084_cp092_cp096_cp097_usaLecturasYConservaIntervalosVacios() {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).contains("test"));
        String identificador = UUID.randomUUID().toString();
        String sede = "METRICAS-" + identificador;
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)", sede);
        Long id = jdbc.queryForObject("""
                INSERT INTO activo(numero_serie, hostname, ubicacion, tipo_activo, estado_operativo)
                VALUES (?, ?, ?, 'SERVIDOR', 'Encendido') RETURNING id
                """, Long.class, identificador, "prueba-metricas", sede);
        jdbc.update("INSERT INTO servidor(id, tipo) VALUES (?, 'BLADE')", id);
        jdbc.update("""
                INSERT INTO monitoreo_snmp(ip_gestion, usuario, clave, frecuencia_actualizacion, estado_conexion, activo_id)
                VALUES (?, 'prueba', 'prueba', 300, 'Sin conexión', ?)
                """, identificador, id);
        jdbc.update("INSERT INTO cpu(activo_id, velocidad_ghz, cantidad_nucleos, estado) VALUES (?, 2.5, 16, 'Encendido')", id);

        LocalDate dia = LocalDate.of(2026, 10, 15);
        LocalDateTime instante = dia.atTime(5, 10);
        lectura(id, "cpu_uso_ghz", null, null, 20, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:1", 30, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:2", 70, instante);
        lectura(id, "red_trafico_entrada_mbps", "puerto_tarjeta_red", "if:1", 50, instante.plusMinutes(20));

        var resultado = service.consultar(id, "custom", dia, dia, List.of());
        var servidor = resultado.servidor();
        assertFalse(servidor.tieneDiscos());
        assertEquals("Sin conexión", servidor.estadoConexion());
        assertNotNull(servidor.ultimaMedicion());
        assertEquals(24, resultado.intervalos().size());
        assertEquals(dia.atStartOfDay(), resultado.intervalos().getFirst());
        var trafico = servidor.metricas().get("red_trafico_entrada_mbps");
        assertEquals(0, BigDecimal.valueOf(75).compareTo(trafico.valores().getFirst()));
        assertNull(trafico.valores().get(1));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(servidor.metricas().get("cpu_uso_ghz").porcentaje()));
        assertTrue(servidor.metricas().get("ram_uso_gb").valores().stream().allMatch(v -> v == null));
    }

    @Test
    void cp108_cp110_validaFechasYAgrupacionesLimite() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 4, 13, 0);
        LocalDate hoy = ahora.toLocalDate();
        assertThrows(SolicitudInvalidaException.class,
                () -> MetricasServidorService.rango("custom", hoy, hoy.minusDays(1), ahora));
        assertEquals(24, MetricasServidorService.rango("custom", hoy, hoy, ahora).intervalos().size());
        var unDia=MetricasServidorService.rango("custom",hoy.minusDays(1),hoy,ahora);
        assertEquals(MetricasServidorService.Agrupacion.HORA,unDia.agrupacion());assertEquals(24,unDia.intervalos().size());
        assertEquals(hoy.atStartOfDay(),unDia.hasta());
        var dosDias=MetricasServidorService.rango("custom",hoy.minusDays(2),hoy,ahora);
        assertEquals(MetricasServidorService.Agrupacion.HORA,dosDias.agrupacion());assertEquals(48,dosDias.intervalos().size());
        var treinta=MetricasServidorService.rango("custom",hoy.minusDays(30),hoy,ahora);
        assertEquals(MetricasServidorService.Agrupacion.DIA,treinta.agrupacion());assertEquals(30,treinta.intervalos().size());
        var treintaYUno=MetricasServidorService.rango("custom",hoy.minusDays(31),hoy,ahora);
        assertEquals(MetricasServidorService.Agrupacion.DIA,treintaYUno.agrupacion());assertEquals(31,treintaYUno.intervalos().size());
        for(int dias:new int[]{3,6,7,8,90,91}) {
            var rango=MetricasServidorService.rango("custom",hoy.minusDays(dias),hoy,ahora);
            var agrupacion=dias<7?MetricasServidorService.Agrupacion.HORA:dias<=90?MetricasServidorService.Agrupacion.DIA:MetricasServidorService.Agrupacion.SEMANA;
            int puntos=dias<7?dias*24:dias<=90?dias:(int)Math.ceil(dias/7.0);
            assertEquals(agrupacion,rango.agrupacion());assertEquals(puntos,rango.intervalos().size());
        }
        var semana=MetricasServidorService.rango("7d",null,null,ahora);
        assertEquals(MetricasServidorService.Agrupacion.DIA,semana.agrupacion());assertEquals(7,semana.intervalos().size());
    }

    @Test void comparaRackeableYBladeEnAmbosSentidosConSuPropiaCapacidad() {
        Long rack=servidorComparacion("RACKEABLE",16), blade=servidorComparacion("BLADE",32);
        Long retirado=servidorComparacion("BLADE",16), otroTipo=chasis();
        jdbc.update("UPDATE activo SET estado_operativo='Baja' WHERE id=?",retirado);
        LocalDate dia=LocalDate.of(2026,10,15);
        lectura(rack,"cpu_uso_ghz",null,null,20,dia.atTime(5,10));
        lectura(blade,"cpu_uso_ghz",null,null,20,dia.atTime(5,10));
        for(Long principal:List.of(rack,blade)) {
            Long otro=principal.equals(rack)?blade:rack;
            var resultado=service.consultar(principal,"custom",dia,dia,List.of(otro));
            assertEquals(1,resultado.comparados().size());
            assertEquals(otro.longValue(),resultado.comparados().getFirst().id());
            assertTrue(resultado.disponibles().stream().anyMatch(o->o.id()==otro));
            assertTrue(resultado.disponibles().stream().noneMatch(o->o.id()==principal || o.id()==retirado || o.id()==otroTipo));
            var cpu=resultado.comparados().getFirst().metricas().get("cpu_uso_ghz");
            BigDecimal esperado=BigDecimal.valueOf(otro.equals(rack)?50:25);
            assertEquals(0,esperado.compareTo(cpu.porcentaje()));
            assertEquals(0,esperado.compareTo(cpu.valores().getFirst()));
            assertEquals(resultado.intervalos().size(),cpu.valores().size());
            assertThrows(SolicitudInvalidaException.class,()->service.consultar(principal,"custom",dia,dia,List.of(otroTipo)));
            assertThrows(SolicitudInvalidaException.class,()->service.consultar(principal,"custom",dia,dia,List.of(retirado)));
            assertThrows(SolicitudInvalidaException.class,()->service.consultar(principal,"custom",dia,dia,List.of(principal)));
            assertThrows(SolicitudInvalidaException.class,()->service.consultar(principal,"custom",dia,dia,List.of(otro,otro)));
        }
        assertThrows(SolicitudInvalidaException.class,()->service.consultar(otroTipo,"custom",dia,dia,List.of(rack)));
    }

    private Long servidorComparacion(String tipo,int nucleos) {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        String nombre="COMPARACION-"+UUID.randomUUID();
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",nombre);
        Long id=jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo) VALUES (?,?,?,'SERVIDOR','Encendido') RETURNING id",Long.class,nombre,nombre,nombre);
        jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,?)",id,tipo);
        jdbc.update("INSERT INTO cpu(activo_id,velocidad_ghz,cantidad_nucleos,estado) VALUES (?,2.5,?,'Encendido')",id,nucleos);
        return id;
    }

    private void lectura(Long id, String nombre, String tipo, String serial, int valor, LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO metrica_historica(activo_id, nombre_metrica, componente_tipo, componente_serial, valor, fecha_medicion)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, nombre, tipo, serial, valor, fecha);
    }

    @Test void chasisExponeTemperaturaYConsumoRealesSinInventarOtrasMetricas() {
        Long id=chasis();LocalDate dia=LocalDate.of(2026,10,15);
        lectura(id,"equipo_temperatura_c",null,null,27,dia.atTime(5,10));
        lectura(id,"equipo_consumo_w",null,null,1760,dia.atTime(5,10));
        var resultado=service.consultar(id,"custom",dia,dia,List.of());
        assertEquals("CHASIS",resultado.servidor().tipo());assertEquals(2,resultado.servidor().metricas().size());
        assertEquals(0,BigDecimal.valueOf(27).compareTo(resultado.servidor().metricas().get("equipo_temperatura_c").actual()));
        assertEquals(0,BigDecimal.valueOf(1760).compareTo(resultado.servidor().metricas().get("equipo_consumo_w").valores().getFirst()));
        assertNull(resultado.servidor().metricas().get("equipo_temperatura_c").valores().get(1));
        assertTrue(resultado.disponibles().stream().allMatch(o->jdbc.queryForObject("SELECT tipo_activo FROM activo WHERE id=?",String.class,o.id()).equals("CHASIS")));
    }
    @Test void chasisSinMedicionesDevuelveNulosYConservaTodosLosPeriodos() {
        Long id=chasis();
        for(String periodo:List.of("24h","7d","30d","1y")) {
            var resultado=service.consultar(id,periodo,null,null,List.of());
            assertNull(resultado.servidor().ultimaMedicion());
            assertTrue(resultado.servidor().metricas().values().stream().allMatch(m->m.actual()==null && m.valores().stream().allMatch(v->v==null)));
        }
    }
    private Long chasis() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        String nombre="CHASIS-"+UUID.randomUUID();jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",nombre);
        Long id=jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo) VALUES (?,?,?,'CHASIS','Encendido') RETURNING id",Long.class,nombre,nombre,nombre);
        jdbc.update("INSERT INTO chasis_blade(id,cantidad_slots) VALUES (?,2)",id);return id;
    }

    @Test void rangoDe24HorasAgrupaEnLimaYExcluyeElLimiteHasta() {
        Long id=chasis();LocalDate desde=LocalDate.of(2026,11,3),hasta=desde.plusDays(1);
        lectura(id,"equipo_temperatura_c",null,null,20,desde.atTime(5,0));
        lectura(id,"equipo_temperatura_c",null,null,32,desde.atTime(17,30));
        lectura(id,"equipo_temperatura_c",null,null,46,hasta.atTime(4,40));
        lectura(id,"equipo_temperatura_c",null,null,80,hasta.atTime(5,0));
        var resultado=service.consultar(id,"custom",desde,hasta,List.of());
        var valores=resultado.servidor().metricas().get("equipo_temperatura_c").valores();
        assertEquals("hora",resultado.agrupacion());assertEquals(24,valores.size());
        assertEquals(desde.atStartOfDay(),resultado.intervalos().getFirst());
        assertEquals(desde.atTime(23,0),resultado.intervalos().getLast());
        assertEquals(0,BigDecimal.valueOf(20).compareTo(valores.getFirst()));
        assertEquals(0,BigDecimal.valueOf(32).compareTo(valores.get(12)));
        assertEquals(0,BigDecimal.valueOf(46).compareTo(valores.getLast()));
        assertFalse(valores.contains(BigDecimal.valueOf(80)));
    }
}
