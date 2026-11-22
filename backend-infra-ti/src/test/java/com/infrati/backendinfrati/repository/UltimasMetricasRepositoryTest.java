package com.infrati.backendinfrati.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UltimasMetricasRepositoryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired InventarioResumenRepository inventario;
    @Autowired MetricasServidorRepository metricas;
    private String sede;
    private static final LocalDateTime FECHA=LocalDateTime.of(2026,10,15,5,20);

    @BeforeEach void preparar() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        sede="ULTIMAS-"+UUID.randomUUID();
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",sede);
    }

    @Test void conservaTodosLosTiposSinMetricasYConMetricasParciales() {
        for (String tipo:List.of("SERVIDOR","STORAGE","SWITCH","CHASIS")) {
            long sin=activo(tipo),cpu=activo(tipo),ram=activo(tipo);
            lectura(cpu,"cpu_uso_ghz",null,null,"10",FECHA);
            lectura(ram,"ram_uso_gb",null,null,"32",FECHA);
            var filas=inventario.servidores(List.of(sin,cpu,ram));
            assertEquals(Set.of(sin,cpu,ram),filas.keySet());
            assertNull(filas.get(sin).usoCpu());assertNull(filas.get(sin).usoRam());
            assertEquals(10.0,filas.get(cpu).usoCpu());assertNull(filas.get(cpu).usoRam());
            assertNull(filas.get(ram).usoCpu());assertEquals(32.0,filas.get(ram).usoRam());
        }
        assertTrue(inventario.servidores(List.of()).isEmpty());
    }

    @Test void conservaCapacidadesUltimaFechaPorMetricaYDesempatePorId() {
        long id=activo("SERVIDOR");
        jdbc.update("INSERT INTO cpu(activo_id,velocidad_ghz,cantidad_nucleos,estado) VALUES (?,2.5,16,'Encendido')",id);
        jdbc.update("INSERT INTO ram(activo_id,capacidad_gb,estado) VALUES (?,128,'Encendido')",id);
        lectura(id,"cpu_uso_ghz",null,null,"35",FECHA.minusMinutes(5));
        lectura(id,"cpu_uso_ghz",null,null,"10",FECHA);
        lectura(id,"cpu_uso_ghz",null,null,"20",FECHA);
        lectura(id,"ram_uso_gb",null,null,"32",FECHA.minusDays(1));
        var fila=inventario.servidores(List.of(id)).get(id);
        assertEquals(40.0,fila.cpu());assertEquals(128.0,fila.ram());
        assertEquals(20.0,fila.usoCpu());assertEquals(32.0,fila.usoRam());
    }

    @Test void switchFiltraAntesDeElegirFechaYSumaSoloLaLecturaElegible() {
        long id=activo("SWITCH");
        for (int n=1;n<=3;n++) jdbc.update("INSERT INTO puerto_switch(switch_id,numero_puerto,estado) VALUES (?,?,?)",id,"1/1/"+n,n==3?"Apagado":"Encendido");
        lectura(id,"equipo_temperatura_c",null,null,"20",FECHA.minusDays(1));
        lectura(id,"equipo_temperatura_c",null,null,"30",FECHA);
        lectura(id,"equipo_temperatura_c",null,null,"5",FECHA);
        // Una fila posterior de un componente no vÃ¡lido no debe ocultar el dato del equipo.
        lectura(id,"equipo_temperatura_c","puerto_switch","1/1/1","999",FECHA.plusDays(1));
        lectura(id,"equipo_temperatura_c",null,"invalido","999",FECHA.plusDays(1));
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/1","100",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/1","50",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/2","200",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/3","900",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","mgmt","900",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_tarjeta_red","1/1/1","900",FECHA);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/1","900",FECHA.plusDays(1));
        lectura(id,"red_trafico_salida_mbps","puerto_switch","1/1/1","900",FECHA.minusDays(1));
        lectura(id,"red_utilizacion_pct","puerto_switch","1/1/1","30",FECHA);
        lectura(id,"red_utilizacion_pct","puerto_switch","1/1/2","20",FECHA);
        lectura(id,"red_utilizacion_pct","puerto_switch","1/1/3","99",FECHA);
        var filas=metricas.actualesSwitch(id);
        assertEquals(List.of("equipo_temperatura_c","red_trafico_entrada_mbps","red_utilizacion_pct"),filas.stream().map(MetricasServidorRepository.Lectura::nombre).toList());
        valor(filas,"equipo_temperatura_c","35");valor(filas,"red_trafico_entrada_mbps","350");valor(filas,"red_utilizacion_pct","30");
        jdbc.update("UPDATE puerto_switch SET estado='Apagado' WHERE switch_id=?",id);
        assertEquals(List.of("equipo_temperatura_c"),metricas.actualesSwitch(id).stream().map(MetricasServidorRepository.Lectura::nombre).toList());
    }

    @Test void switchSinMetricasOFechaDeActualizacionNoInventaLecturas() {
        long id=activo("SWITCH");assertTrue(metricas.actualesSwitch(id).isEmpty());
        jdbc.update("INSERT INTO puerto_switch(switch_id,numero_puerto,estado) VALUES (?,'1/1/1','Encendido')",id);
        lectura(id,"red_trafico_entrada_mbps","puerto_switch","1/1/1","100",FECHA);
        lectura(id,"cpu_uso_ghz",null,null,"2",FECHA.minusDays(1));
        jdbc.update("UPDATE activo SET ultima_actualizacion=NULL WHERE id=?",id);
        var filas=metricas.actualesSwitch(id);
        assertEquals(1,filas.size());valor(filas,"cpu_uso_ghz","2");
    }

    private long activo(String tipo) {
        String nombre="ULT-"+UUID.randomUUID();
        long id=jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo,ultima_actualizacion) VALUES (?,?,?,?,'Encendido',?) RETURNING id",Long.class,nombre,nombre,sede,tipo,FECHA);
        switch (tipo) {
            case "SERVIDOR" -> jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,'RACKEABLE')",id);
            case "STORAGE" -> jdbc.update("INSERT INTO storage(id) VALUES (?)",id);
            case "SWITCH" -> jdbc.update("INSERT INTO switch(id) VALUES (?)",id);
            case "CHASIS" -> jdbc.update("INSERT INTO chasis_blade(id,cantidad_slots) VALUES (?,0)",id);
            default -> throw new IllegalArgumentException(tipo);
        }
        return id;
    }
    private void lectura(long id,String nombre,String tipo,String serial,String valor,LocalDateTime fecha) {
        jdbc.update("INSERT INTO metrica_historica(activo_id,nombre_metrica,componente_tipo,componente_serial,valor,fecha_medicion) VALUES (?,?,?,?,?,?)",id,nombre,tipo,serial,new BigDecimal(valor),fecha);
    }
    private static void valor(List<MetricasServidorRepository.Lectura> filas,String nombre,String esperado) {
        var fila=filas.stream().filter(f->f.nombre().equals(nombre)).findFirst().orElseThrow();
        assertEquals(0,new BigDecimal(esperado).compareTo(fila.valor()));
    }
}
