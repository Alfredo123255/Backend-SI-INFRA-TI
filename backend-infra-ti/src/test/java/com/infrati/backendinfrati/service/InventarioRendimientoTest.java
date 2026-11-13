package com.infrati.backendinfrati.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.sql.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Import(InventarioRendimientoTest.ContadorConfig.class)
@Transactional
class InventarioRendimientoTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ServidorService servidores;
    private String prefijo;
    private static final ThreadLocal<Integer> CONSULTAS=new ThreadLocal<>();
    @BeforeEach void preparar() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        prefijo="PERF-"+UUID.randomUUID();
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",prefijo);
        for(String tipo:List.of("SERVIDOR","STORAGE","SWITCH","CHASIS")) {
            jdbc.update("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo,fabricante) SELECT ?||'-'||?||'-'||n,?||'-'||?||'-'||CASE WHEN n<=100 THEN 'small-' ELSE '' END||n,?,?,'Encendido','Prueba' FROM generate_series(1,1000) n",prefijo,tipo,prefijo,tipo,prefijo,tipo);
            String tabla=switch(tipo) { case "SERVIDOR"->"servidor";case "STORAGE"->"storage";case "SWITCH"->"switch";default->"chasis_blade"; };
            String extra=tipo.equals("SERVIDOR")?",tipo":tipo.equals("CHASIS")?",cantidad_slots":"";
            String valor=tipo.equals("SERVIDOR")?",'RACKEABLE'":tipo.equals("CHASIS")?",0":"";
            jdbc.update("INSERT INTO "+tabla+"(id"+extra+") SELECT id"+valor+" FROM activo WHERE ubicacion=? AND tipo_activo=?",prefijo,tipo);
        }
        jdbc.update("INSERT INTO cpu(activo_id,velocidad_ghz,cantidad_nucleos,estado) SELECT id,2.5,16,'Encendido' FROM activo WHERE ubicacion=? AND tipo_activo='SERVIDOR'",prefijo);
        jdbc.update("INSERT INTO ram(activo_id,capacidad_gb,estado) SELECT id,128,'Encendido' FROM activo WHERE ubicacion=? AND tipo_activo='SERVIDOR'",prefijo);
        jdbc.update("INSERT INTO disco(activo_id,capacidad_gb,estado) SELECT id,1000,'Encendido' FROM activo WHERE ubicacion=? AND tipo_activo='SERVIDOR'",prefijo);
        jdbc.update("INSERT INTO metrica_historica(activo_id,nombre_metrica,valor,fecha_medicion) SELECT id,m.nombre,m.valor,TIMESTAMP '2026-01-01' FROM activo CROSS JOIN (VALUES ('cpu_uso_ghz',20),('ram_uso_gb',32)) m(nombre,valor) WHERE ubicacion=? AND tipo_activo='SERVIDOR'",prefijo);
        jdbc.update("INSERT INTO puerto_switch(switch_id,numero_puerto,velocidad,estado) SELECT id,'1/1/1','1 Gbps','Encendido' FROM activo WHERE ubicacion=? AND tipo_activo='SWITCH'",prefijo);
        em.clear();
    }
    @Test void milEquiposPorCategoriaSinConsultasPorEquipo() throws Exception {
        boolean baseline=Boolean.getBoolean("inventario.baseline");
        String[][] rutas={{"SERVIDOR","/api/servidores"},{"STORAGE","/api/storage"},{"SWITCH","/api/switches"},{"CHASIS","/api/chasis-blade"}};
        for(String[] ruta:rutas) {
            int consultas=0;double maximo=0;
            for(int intento=0;intento<3;intento++) {
                em.clear();CONSULTAS.set(0);long inicio=System.nanoTime();
                var respuesta=mvc.perform(get(ruta[1]).param("q",prefijo+"-"+ruta[0]+"-").with(user("lector").authorities(()->"ver_inventario")))
                    .andExpect(status().isOk()).andReturn().getResponse();
                double ms=(System.nanoTime()-inicio)/1_000_000.0;consultas=CONSULTAS.get();CONSULTAS.remove();maximo=Math.max(maximo,ms);
                assertEquals(1000,respuesta.getContentAsString().split("\\\"id\\\":").length-1);
                registrar(ruta[1],intento,consultas,ms);
            }
            if(!baseline) { assertTrue(consultas<=10,ruta[1]+": "+consultas+" consultas");assertTrue(maximo<3000,ruta[1]+": "+maximo+" ms"); }
        }
        em.clear();CONSULTAS.set(0);
        var cien=servidores.listar(null,null,null,null,prefijo+"-SERVIDOR-small-");
        int consultasCien=CONSULTAS.get();CONSULTAS.remove();assertEquals(100,cien.size());
        em.clear();CONSULTAS.set(0);var mil=servidores.listar(null,null,null,null,prefijo+"-SERVIDOR-");
        int consultasMil=CONSULTAS.get();CONSULTAS.remove();assertEquals(1000,mil.size());
        assertEquals(50.0,mil.getFirst().getUsoCpuPct());assertEquals(25.0,mil.getFirst().getUsoRamPct());
        if(!baseline) assertEquals(consultasCien,consultasMil,"El número de consultas no debe crecer por equipo");
    }
    private static void registrar(String ruta,int intento,int consultas,double ms) throws Exception {
        String linea=String.format(Locale.ROOT,"%s,%d,%d,%.3f%n",ruta,intento+1,consultas,ms);
        System.out.print("INVENTARIO_PERF "+linea);
        String archivo=System.getProperty("inventario.reporte");
        if(archivo!=null)Files.writeString(Path.of(archivo),linea,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    @AfterEach void limpiarContador() { CONSULTAS.remove(); }
    private static Object llamar(Object objetivo,Method metodo,Object[] args) throws Throwable {
        try { return metodo.invoke(objetivo,args); }catch(InvocationTargetException e){throw e.getCause();}
    }
    private static Connection envolver(Connection conexion) {
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            Object resultado=llamar(conexion,m,a);
            if(resultado instanceof Statement sentencia) {
                Class<?> clase=sentencia instanceof CallableStatement?CallableStatement.class:sentencia instanceof PreparedStatement?PreparedStatement.class:Statement.class;
                return Proxy.newProxyInstance(clase.getClassLoader(),new Class<?>[]{clase},(s,metodo,args)->{
                    if(metodo.getName().startsWith("execute") && CONSULTAS.get()!=null)CONSULTAS.set(CONSULTAS.get()+1);
                    return llamar(sentencia,metodo,args);
                });
            }
            return resultado;
        });
    }
    @TestConfiguration static class ContadorConfig {
        @Bean static BeanPostProcessor contarSqlJdbcYHibernate() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean,String nombre) {
                    if(!(bean instanceof DataSource ds))return bean;
                    return Proxy.newProxyInstance(DataSource.class.getClassLoader(),new Class<?>[]{DataSource.class},(p,m,a)->{
                        Object resultado=llamar(ds,m,a);return resultado instanceof Connection c?envolver(c):resultado;
                    });
                }
            };
        }
    }

    @Test void conservaPorcentajesNulosRedondeoFiltrosYUltimaLecturaPorMetrica() {
        long id=jdbc.queryForObject("SELECT min(id) FROM activo WHERE ubicacion=? AND tipo_activo='SERVIDOR'",Long.class,prefijo);
        jdbc.update("INSERT INTO cpu(activo_id,estado) VALUES (?,'Encendido')",id);
        jdbc.update("INSERT INTO ram(activo_id,capacidad_gb,estado) VALUES (?,NULL,'Encendido')",id);
        jdbc.update("INSERT INTO metrica_historica(activo_id,nombre_metrica,valor,fecha_medicion) VALUES (?,'cpu_uso_ghz',10,TIMESTAMP '2026-01-02')",id);
        String hostname=jdbc.queryForObject("SELECT hostname FROM activo WHERE id=?",String.class,id);
        em.clear();var fila=servidores.listar("encendido",prefijo,null,"prueba",hostname).stream().filter(s->s.getId()==id).findFirst().orElseThrow();
        assertEquals(25.0,fila.getUsoCpuPct());assertEquals(25.0,fila.getUsoRamPct());
        jdbc.update("DELETE FROM cpu WHERE activo_id=?",id);jdbc.update("DELETE FROM ram WHERE activo_id=?",id);
        em.clear();fila=servidores.listar(null,null,null,null,hostname).stream().filter(s->s.getId()==id).findFirst().orElseThrow();
        assertNull(fila.getUsoCpuPct());assertNull(fila.getUsoRamPct());
        assertTrue(servidores.listar("no-existe",null,null,null,prefijo).isEmpty());
    }
}
