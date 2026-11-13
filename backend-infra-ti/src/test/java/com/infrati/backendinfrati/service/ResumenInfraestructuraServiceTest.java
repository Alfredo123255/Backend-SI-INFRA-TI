package com.infrati.backendinfrati.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumenInfraestructuraServiceTest {
    @Autowired ResumenInfraestructuraService service;
    @Autowired AlertaActivoService alertas;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private String dc,cluster;
    private final LocalDateTime lectura=LocalDateTime.of(2026,1,2,12,0);
    @BeforeEach void preparar() {
        assertTrue(jdbc.queryForObject("SELECT current_database()",String.class).contains("test"));
        // Aislamiento transaccional: ningún equipo previo afecta a los indicadores globales.
        jdbc.update("UPDATE activo SET estado_operativo='Baja',mantenimiento_activo=false");
        dc="RESUMEN-"+UUID.randomUUID();cluster=dc+"-CL";
        jdbc.update("INSERT INTO datacenters(nombre,latitud,longitud) VALUES (?,'-12.05','-77.04')",dc);
        jdbc.update("INSERT INTO clusters(nombre,ambiente,datacenter) VALUES (?,'QA',?)",cluster,dc);
        jdbc.update("UPDATE umbral SET advertencia=30 WHERE nombre='dias_para_eos'");
    }
    @Test void cp130_ponderaPorCapacidadNoPromediaPorcentajesNiLecturasAntiguas() {
        long a=servidor("RACKEABLE",40,100),b=servidor("BLADE",30,100);
        metrica(a,"cpu_uso_ghz",999,lectura.minusMinutes(1));
        var g=service.clusters(dc).getFirst();
        igual(35,g.cpu());assertEquals(2,g.total());
        // Distintas capacidades: (40+30)/(100+200), no (40%+15%)/2.
        jdbc.update("UPDATE cpu SET cantidad_nucleos=200 WHERE activo_id=?",b);
        igual(70.0/3,service.clusters(dc).getFirst().cpu());
    }
    @Test void cp132_mantenimientoYBajaSeExcluyenEnTodosLosCalculos() {
        long normal=servidor("RACKEABLE",40,100),mant=servidor("BLADE",100,100),baja=servidor("RACKEABLE",100,100);
        metrica(normal,"equipo_temperatura_c",25,lectura);metrica(mant,"equipo_temperatura_c",100,lectura);
        jdbc.update("UPDATE activo SET mantenimiento_activo=true WHERE id=?",mant);
        jdbc.update("UPDATE activo SET estado_operativo='Baja' WHERE id=?",baja);
        var r=service.inicio();assertEquals(1,r.total());assertEquals(1,r.encendidos());
        igual(100,r.disponibilidad());igual(60,r.capacidad().getFirst().disponible());igual(25,r.temperatura());
        assertEquals(1,r.mantenimiento());assertEquals(1,service.equiposInicio("mantenimiento").size());
        assertEquals(1,service.equiposCluster(dc,cluster).size());
    }
    @Test void rf111_encendidoSinConexionNoEstaDisponiblePeroConservaSusUltimosDatos() {
        long id=servidor("RACKEABLE",40,100);
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Sin conexión' WHERE activo_id=?",id);
        igual(0,service.inicio().disponibilidad());assertEquals(1,service.inicio().sinConexion());
        assertEquals(lectura.atOffset(ZoneOffset.UTC),service.equiposInicio("sin-conexion").getFirst().ultimaActualizacion());
        igual(40,service.clusters(dc).getFirst().cpu());
    }
    @Test void cp133_almacenamientoSoloPorDatacenterIncluyeSinCluster() {
        long id=activo("STORAGE",null);
        jdbc.update("INSERT INTO storage(id,capacidad_total_tb) VALUES (?,100)",id);
        metrica(id,"almacenamiento_uso_tb",25,lectura);
        igual(25,service.datacenters().stream().filter(g->g.id().equals(dc)).findFirst().orElseThrow().almacenamiento());
        var sin=service.clusters(dc).stream().filter(g->g.id().equals("sin-cluster")).findFirst().orElseThrow();
        assertEquals(1,sin.total());assertNull(sin.almacenamiento());
        assertTrue(service.tipos(null,null).stream().allMatch(g->g.almacenamiento()==null));
        igual(25,service.equiposCluster(dc,"sin-cluster").getFirst().almacenamiento());
    }
    @Test void cp125_cp127_datacenterVacioYEquiposSinCluster() {
        var vacio=service.datacenters().stream().filter(g->g.id().equals(dc)).findFirst().orElseThrow();
        assertEquals(0,vacio.total());assertNull(vacio.cpu());assertNull(vacio.memoria());
        long id=servidor("RACKEABLE",30,100);jdbc.update("UPDATE activo SET cluster=NULL WHERE id=?",id);
        assertEquals(id,service.equiposCluster(dc,"sin-cluster").getFirst().id());
        assertEquals(1,service.clusters(dc).stream().filter(g->g.id().equals("sin-cluster")).findFirst().orElseThrow().total());
    }
    @Test void cp131_losConteosSonLosDelServicioDeAlertasActuales() {
        long a=servidor("RACKEABLE",40,100),b=servidor("BLADE",30,100);
        alerta(a,"Crítica","estado_operativo");alerta(a,"Media","estado");alerta(b,"Crítica","umbral_equipo_temperatura_c");
        alerta(b,"Crítica","estado_conexion"); // Activa: el servicio vigente excluye esta alerta obsoleta.
        var g=service.clusters(dc).getFirst();assertEquals(2,g.criticas());assertEquals(1,g.medias());
        assertEquals(alertas.listar().size(),g.criticas()+g.medias());
        assertEquals(2,service.equiposInicio("criticas").size());
        jdbc.update("UPDATE activo SET mantenimiento_activo=true WHERE id=?",a);
        assertEquals(1,service.clusters(dc).getFirst().criticas());assertEquals(0,service.clusters(dc).getFirst().medias());
    }
    @Test void eos_respetaLosLimites31_30_0_menos1EnHoraLima() {
        LocalDate hoy=LocalDate.now(ZoneId.of("America/Lima"));
        for(int dias:new int[]{31,30,0,-1}) {
            long id=servidor("RACKEABLE",10,100);jdbc.update("UPDATE activo SET fecha_eos=? WHERE id=?",hoy.plusDays(dias),id);
        }
        var r=service.inicio();assertEquals(1,r.eosVencidos());assertEquals(2,r.eosPorVencer());
        assertEquals(2,service.equiposInicio("eos-por-vencer").size());
        assertEquals(-1,service.equiposInicio("eos-vencidos").getFirst().diasRestantes());
    }
    @Test void capacidad_sinAdvertenciaSinMetaYNoUsaUnaLecturaAnterior() {
        long a=servidor("RACKEABLE",40,100);
        jdbc.update("UPDATE umbral SET advertencia=NULL,critico=NULL WHERE nombre='cpu_uso_ghz'");
        assertNull(service.inicio().capacidad().getFirst().meta());
        jdbc.update("UPDATE activo SET ultima_actualizacion=? WHERE id=?",lectura.plusSeconds(1),a);
        assertNull(service.inicio().capacidad().getFirst().disponible());
        assertNull(service.inicio().temperatura());
    }
    @Test void unaCapacidadIncompletaExcluyeSoloEseEquipoYRecurso() {
        long a=servidor("RACKEABLE",40,100),b=servidor("RACKEABLE",100,100);
        jdbc.update("INSERT INTO cpu(activo_id,estado) VALUES (?,'Encendido')",b);
        igual(40,service.clusters(dc).getFirst().cpu());
        jdbc.update("UPDATE cpu SET velocidad_ghz=NULL WHERE activo_id=?",a);
        assertNull(service.clusters(dc).getFirst().cpu());
        assertNotNull(service.clusters(dc).getFirst().memoria());
    }
    @Test void mantenimientoResponsableMotivoSeObtienenDelUltimoInicio() {
        long id=servidor("RACKEABLE",10,100);jdbc.update("UPDATE activo SET mantenimiento_activo=true WHERE id=?",id);
        jdbc.update("INSERT INTO historico_estado(activo_id,campo,valor_nuevo,descripcion,fecha_cambio) VALUES (?,'mantenimiento_activo','true','Mantenimiento iniciado. Responsable: Ana. Motivo: Firmware',?)",id,lectura);
        var e=service.equiposInicio("mantenimiento").getFirst();assertEquals("Ana",e.responsable());assertEquals("Firmware",e.motivo());
        assertTrue(e.ruta().endsWith("/servidores/rackeable/"+id));
    }
    @Test void nuevosGetExigenVerInventarioYValidanElAlcance() throws Exception {
        for(String path:new String[]{"/api/resumen-general","/api/resumen-general/equipos?filtro=criticas","/api/vistas-consolidadas/datacenters","/api/vistas-consolidadas/tipos"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("sin-permiso").authorities(()->"exportar"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("lector").authorities(()->"ver_inventario"))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/resumen-general/equipos?filtro=incorrecto").with(user("lector").authorities(()->"ver_inventario"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vistas-consolidadas/datacenters/no-existe/clusters").with(user("lector").authorities(()->"ver_inventario"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/vistas-consolidadas/tipos?cluster=x").with(user("lector").authorities(()->"ver_inventario"))).andExpect(status().isBadRequest());
    }
    private long activo(String tipo,String cl) {
        String n=UUID.randomUUID().toString();
        return jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo,cluster,ultima_actualizacion) VALUES (?,?,?,?,'Encendido',?,?) RETURNING id",Long.class,n,n,dc,tipo,cl,lectura);
    }
    private long servidor(String tipo,double uso,int capacidad) {
        long id=activo("SERVIDOR",cluster);
        jdbc.update("INSERT INTO servidor(id,tipo) VALUES (?,?)",id,tipo);
        jdbc.update("INSERT INTO cpu(activo_id,velocidad_ghz,cantidad_nucleos,estado) VALUES (?,1,?,'Encendido')",id,capacidad);
        jdbc.update("INSERT INTO ram(activo_id,capacidad_gb,estado) VALUES (?,100,'Encendido')",id);
        jdbc.update("INSERT INTO monitoreo_snmp(ip_gestion,usuario,clave,frecuencia_actualizacion,estado_conexion,activo_id) VALUES (?,'test','test',120,'Activo',?)","127.0.0.1:"+(id+1),id);
        metrica(id,"cpu_uso_ghz",uso,lectura);metrica(id,"ram_uso_gb",20,lectura);return id;
    }
    private void metrica(long id,String nombre,double valor,LocalDateTime fecha) {
        jdbc.update("INSERT INTO metrica_historica(activo_id,nombre_metrica,valor,fecha_medicion) VALUES (?,?,?,?)",id,nombre,valor,fecha);
    }
    private void alerta(long id,String severidad,String campo) {
        jdbc.update("INSERT INTO alerta(activo_id,campo,severidad,descripcion,fecha_inicio,ultima_verificacion) VALUES (?,?,?,'Prueba',?,?)",id,campo,severidad,lectura,lectura);
    }
    private static void igual(double esperado,BigDecimal valor) { assertNotNull(valor);assertEquals(esperado,valor.doubleValue(),1e-8); }

    @Test void ind01_soloEncendidoConConexionActivaEstaDisponible() {
        long activo=servidor("RACKEABLE",10,100),inactivo=servidor("RACKEABLE",10,100),
            sinConexion=servidor("BLADE",10,100),sinRegistro=servidor("BLADE",10,100);
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Inactivo' WHERE activo_id=?",inactivo);
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Sin conexión' WHERE activo_id=?",sinConexion);
        jdbc.update("DELETE FROM monitoreo_snmp WHERE activo_id=?",sinRegistro);
        var resultado=service.inicio();assertEquals(4,resultado.total());igual(25,resultado.disponibilidad());
        jdbc.update("UPDATE monitoreo_snmp SET estado_conexion='Inactivo' WHERE activo_id=?",activo);
        assertEquals(4,service.inicio().total());igual(0,service.inicio().disponibilidad());
    }
}
