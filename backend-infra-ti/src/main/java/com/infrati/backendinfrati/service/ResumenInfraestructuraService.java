package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ResumenInfraestructura.*;
import com.infrati.backendinfrati.dto.AlertaActivo;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Una misma instantánea y reglas de agregación para Inicio y todos los niveles consolidados. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ResumenInfraestructuraService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final Map<String,String> TIPOS = Map.of("rackeable","Servidor rackeable",
            "blade","Servidor blade","storage","Storage","switch","Switch","chasis","Chasis blade");
    private final JdbcTemplate jdbc;
    private final AlertaActivoService alertas;
    public ResumenInfraestructuraService(JdbcTemplate jdbc, AlertaActivoService alertas) {
        this.jdbc=jdbc; this.alertas=alertas;
    }

    private record Centro(String nombre, BigDecimal latitud, BigDecimal longitud) {}
    private record Cluster(String nombre, String datacenter) {}
    private record Activo(long id, String hostname, String datacenter, String cluster, String tipo,
            String estado, boolean mantenimiento, String conexion, LocalDateTime actualizacion,
            String modelo, LocalDate eos, String inicioMantenimiento, BigDecimal cpuUso,
            BigDecimal cpuCapacidad, BigDecimal ramUso, BigDecimal ramCapacidad,
            BigDecimal almacenamientoUso, BigDecimal almacenamientoCapacidad, BigDecimal temperatura) {
        boolean considerado() { return !mantenimiento && !"Baja".equals(estado); }
        boolean servidor() { return "rackeable".equals(tipo) || "blade".equals(tipo); }
    }
    private record Datos(List<Activo> todos, List<Activo> considerados, List<Centro> centros,
            List<Cluster> clusters, Map<Long,List<AlertaActivo>> alertas, Map<String,BigDecimal> avisos) {}

    private Datos datos() {
        // Las capacidades son inválidas si falta el dato de cualquier módulo, igual que en el ETL.
        // DISTINCT ON evita contar dos veces una medición repetida; nunca busca una fecha anterior.
        var activos=jdbc.query("""
            WITH capacidades_cpu AS (
                SELECT activo_id, CASE WHEN bool_and(velocidad_ghz IS NOT NULL AND velocidad_ghz>0
                    AND cantidad_nucleos IS NOT NULL AND cantidad_nucleos>0)
                    THEN sum(velocidad_ghz*cantidad_nucleos) END capacidad FROM cpu GROUP BY activo_id
            ), capacidades_ram AS (
                SELECT activo_id, CASE WHEN bool_and(capacidad_gb IS NOT NULL AND capacidad_gb>0)
                    THEN sum(capacidad_gb) END capacidad FROM ram GROUP BY activo_id
            )
            SELECT a.*, s.tipo tipo_servidor, c.capacidad cpu_capacidad, r.capacidad ram_capacidad,
                   st.capacidad_total_tb, sn.estado_conexion, h.descripcion inicio_mantenimiento,
                   m.cpu_uso, m.ram_uso, m.almacenamiento_uso, m.temperatura_actual
            FROM activo a LEFT JOIN servidor s ON s.id=a.id
            LEFT JOIN capacidades_cpu c ON c.activo_id=a.id
            LEFT JOIN capacidades_ram r ON r.activo_id=a.id
            LEFT JOIN storage st ON st.id=a.id
            LEFT JOIN LATERAL (SELECT estado_conexion FROM monitoreo_snmp
                WHERE activo_id=a.id ORDER BY id DESC LIMIT 1) sn ON true
            LEFT JOIN LATERAL (SELECT descripcion FROM historico_estado WHERE activo_id=a.id
                AND campo='mantenimiento_activo' AND valor_nuevo='true'
                ORDER BY fecha_cambio DESC,id DESC LIMIT 1) h ON true
            LEFT JOIN LATERAL (
                SELECT max(valor) FILTER (WHERE nombre_metrica='cpu_uso_ghz') cpu_uso,
                       max(valor) FILTER (WHERE nombre_metrica='ram_uso_gb') ram_uso,
                       max(valor) FILTER (WHERE nombre_metrica='almacenamiento_uso_tb') almacenamiento_uso,
                       max(valor) FILTER (WHERE nombre_metrica='equipo_temperatura_c') temperatura_actual
                FROM (SELECT DISTINCT ON (nombre_metrica,componente_tipo,componente_serial)
                    nombre_metrica,valor FROM metrica_historica
                    WHERE activo_id=a.id AND fecha_medicion=a.ultima_actualizacion
                      AND nombre_metrica IN ('cpu_uso_ghz','ram_uso_gb','almacenamiento_uso_tb','equipo_temperatura_c')
                      AND componente_tipo IS NULL AND componente_serial IS NULL
                    ORDER BY nombre_metrica,componente_tipo,componente_serial,id DESC) lectura
            ) m ON true ORDER BY a.hostname,a.id
            """,(rs,n)->new Activo(rs.getLong("id"),rs.getString("hostname"),rs.getString("ubicacion"),
                rs.getString("cluster"),"SERVIDOR".equals(rs.getString("tipo_activo"))
                    ? Optional.ofNullable(rs.getString("tipo_servidor")).orElse("RACKEABLE").toLowerCase(Locale.ROOT)
                    : rs.getString("tipo_activo").toLowerCase(Locale.ROOT),
                rs.getString("estado_operativo"),rs.getBoolean("mantenimiento_activo"),rs.getString("estado_conexion"),
                rs.getObject("ultima_actualizacion",LocalDateTime.class),rs.getString("modelo"),
                rs.getObject("fecha_eos",LocalDate.class),rs.getString("inicio_mantenimiento"),
                rs.getBigDecimal("cpu_uso"),rs.getBigDecimal("cpu_capacidad"),rs.getBigDecimal("ram_uso"),
                rs.getBigDecimal("ram_capacidad"),rs.getBigDecimal("almacenamiento_uso"),
                rs.getBigDecimal("capacidad_total_tb"),rs.getBigDecimal("temperatura_actual")));
        var centros=jdbc.query("SELECT * FROM datacenters ORDER BY nombre",(rs,n)->
                new Centro(rs.getString("nombre"),coordenada(rs.getString("latitud")),coordenada(rs.getString("longitud"))));
        var clusters=jdbc.query("SELECT nombre,datacenter FROM clusters ORDER BY nombre",(rs,n)->
                new Cluster(rs.getString("nombre"),rs.getString("datacenter")));
        Map<String,BigDecimal> avisos=new HashMap<>();
        jdbc.query("SELECT nombre,advertencia FROM umbral",rs->{avisos.put(rs.getString("nombre"),rs.getBigDecimal("advertencia"));});
        var actuales=alertas.listar().stream().collect(Collectors.groupingBy(AlertaActivo::activoId));
        return new Datos(activos,activos.stream().filter(Activo::considerado).toList(),centros,clusters,actuales,avisos);
    }

    public Inicio inicio() {
        var d=datos(); var a=d.considerados(); var grupo=grupo(d,"todos","Todos los datacenters",a,true,null);
        var fecha=a.stream().map(Activo::actualizacion).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        long disponibles=a.stream().filter(e->"Encendido".equals(e.estado()) && !"Sin conexión".equals(e.conexion())).count();
        var hoy=LocalDate.now(LIMA); var dias=d.avisos().get("dias_para_eos");
        return new Inicio(utc(fecha),a.size(),a.isEmpty()?null:porcentaje(BigDecimal.valueOf(disponibles),BigDecimal.valueOf(a.size())),
            grupo.criticas(),grupo.medias(),List.of(capacidad("CPU",grupo.cpu(),d.avisos().get("cpu_uso_ghz")),
                capacidad("Memoria",grupo.memoria(),d.avisos().get("ram_uso_gb")),
                capacidad("Almacenamiento",grupo.almacenamiento(),d.avisos().get("almacenamiento_uso_tb"))),
            a.stream().filter(e->e.eos()!=null && e.eos().isBefore(hoy)).count(),
            a.stream().filter(e->porVencer(e,hoy,dias)).count(),grupo.encendidos(),grupo.degradados(),grupo.apagados(),
            promedio(a,Activo::temperatura),d.todos().stream().filter(e->e.mantenimiento() && !"Baja".equals(e.estado())).count(),
            a.stream().filter(e->"Sin conexión".equals(e.conexion())).count(),datacenters(d),grupo);
    }

    public List<Grupo> datacenters() { return datacenters(datos()); }
    private List<Grupo> datacenters(Datos d) {
        return d.centros().stream().map(c->grupo(d,c.nombre(),c.nombre(),filtrar(d,c.nombre(),null,null),true,c)).toList();
    }
    public List<Grupo> clusters(String datacenter) {
        var d=datos(); centro(d,datacenter);
        var nombres=new TreeSet<String>();
        d.clusters().stream().filter(c->datacenter.equals(c.datacenter())).forEach(c->nombres.add(c.nombre()));
        filtrar(d,datacenter,null,null).stream().map(Activo::cluster).filter(Objects::nonNull).forEach(nombres::add);
        var resultado=new ArrayList<Grupo>();
        nombres.forEach(c->resultado.add(grupo(d,c,c,filtrar(d,datacenter,c,null),false,null)));
        var sin=filtrar(d,datacenter,"sin-cluster",null);
        if (!sin.isEmpty()) resultado.add(grupo(d,"sin-cluster","Sin cluster",sin,false,null));
        return resultado;
    }
    public List<Equipo> equiposCluster(String datacenter,String cluster) {
        var d=datos(); centro(d,datacenter); validarCluster(d,datacenter,cluster);
        return filtrar(d,datacenter,cluster,null).stream().map(a->equipo(d,a,null)).toList();
    }
    public List<Grupo> tipos(String datacenter,String cluster) {
        var d=datos(); validarAlcance(d,datacenter,cluster);
        return List.of("rackeable","blade","storage","switch","chasis").stream()
            .map(t->grupo(d,t,TIPOS.get(t),filtrar(d,datacenter,cluster,t),false,null)).toList();
    }
    public List<Equipo> equiposTipo(String tipo,String datacenter,String cluster) {
        var t=tipo.toLowerCase(Locale.ROOT);
        if (!TIPOS.containsKey(t)) throw new SolicitudInvalidaException("El tipo de equipo no es válido.");
        var d=datos(); validarAlcance(d,datacenter,cluster);
        return filtrar(d,datacenter,cluster,t).stream().map(a->equipo(d,a,null)).toList();
    }
    public List<Equipo> equiposInicio(String filtro) {
        var d=datos(); var hoy=LocalDate.now(LIMA);
        if ("criticas".equals(filtro) || "medias".equals(filtro)) {
            var severidad="criticas".equals(filtro)?"Crítica":"Media";
            return d.considerados().stream().flatMap(a->d.alertas().getOrDefault(a.id(),List.of()).stream()
                .filter(al->severidad.equals(al.severidad())).map(al->equipo(d,a,al))).toList();
        }
        Predicate<Activo> seleccion=switch(filtro) {
            case "eos-vencidos" -> a->a.eos()!=null && a.eos().isBefore(hoy);
            case "eos-por-vencer" -> a->porVencer(a,hoy,d.avisos().get("dias_para_eos"));
            case "mantenimiento" -> a->a.mantenimiento() && !"Baja".equals(a.estado());
            case "sin-conexion" -> a->"Sin conexión".equals(a.conexion());
            // Los botones existentes de estados conservan su navegación sin recalcular en el front.
            case "encendidos" -> a->"Encendido".equals(a.estado());
            case "degradados" -> a->"Degradado".equals(a.estado());
            case "apagados" -> a->"Apagado".equals(a.estado());
            default -> throw new SolicitudInvalidaException("El filtro de equipos no es válido.");
        };
        return ("mantenimiento".equals(filtro)?d.todos():d.considerados()).stream().filter(seleccion)
            .map(a->equipo(d,a,null)).toList();
    }

    private Grupo grupo(Datos d,String id,String nombre,List<Activo> activos,boolean almacenamiento,Centro centro) {
        long criticas=activos.stream().mapToLong(a->contar(d,a,"Crítica")).sum();
        long medias=activos.stream().mapToLong(a->contar(d,a,"Media")).sum();
        return new Grupo(id,nombre,activos.size(),estado(activos,"Encendido"),estado(activos,"Apagado"),estado(activos,"Degradado"),
            uso(activos.stream().filter(Activo::servidor).toList(),Activo::cpuUso,Activo::cpuCapacidad),
            uso(activos.stream().filter(Activo::servidor).toList(),Activo::ramUso,Activo::ramCapacidad),
            almacenamiento?uso(activos.stream().filter(a->"storage".equals(a.tipo())).toList(),Activo::almacenamientoUso,Activo::almacenamientoCapacidad):null,
            criticas,medias,centro==null?d.clusters().size():d.clusters().stream().filter(c->centro.nombre().equals(c.datacenter())).count(),
            centro==null?null:centro.latitud(),centro==null?null:centro.longitud(),criticas>0?"critical":medias>0?"warning":"good");
    }
    private Equipo equipo(Datos d,Activo a,AlertaActivo alerta) {
        var descripcion=a.inicioMantenimiento();
        String responsable=null,motivo=null;
        if (a.mantenimiento() && descripcion!=null) {
            int r=descripcion.indexOf("Responsable: "),m=descripcion.indexOf(". Motivo: ");
            if (r>=0) responsable=descripcion.substring(r+13,m>r?m:descripcion.length());
            if (m>=0) motivo=descripcion.substring(m+10);
        }
        return new Equipo(a.id(),a.hostname(),a.datacenter(),a.cluster(),a.tipo().toUpperCase(Locale.ROOT),TIPOS.get(a.tipo()),a.estado(),
            a.servidor()?porcentaje(a.cpuUso(),a.cpuCapacidad()):null,a.servidor()?porcentaje(a.ramUso(),a.ramCapacidad()):null,
            "storage".equals(a.tipo())?porcentaje(a.almacenamientoUso(),a.almacenamientoCapacidad()):null,
            contar(d,a,"Crítica"),contar(d,a,"Media"),ruta(a),alerta==null?null:alerta.descripcion(),alerta==null?null:alerta.severidad(),
            a.modelo(),a.eos(),a.eos()==null?null:ChronoUnit.DAYS.between(LocalDate.now(LIMA),a.eos()),responsable,motivo,utc(a.actualizacion()));
    }
    private static String ruta(Activo a) {
        return "/inventario/"+switch(a.tipo()) {
            case "rackeable","blade" -> "servidores/"+a.tipo();
            case "switch" -> "switches"; case "chasis" -> "chasis-blades"; default -> "storage";
        }+"/"+a.id();
    }
    private static long contar(Datos d,Activo a,String severidad) {
        return d.alertas().getOrDefault(a.id(),List.of()).stream().filter(al->severidad.equals(al.severidad())).count();
    }
    private static long estado(List<Activo> a,String e) { return a.stream().filter(x->e.equals(x.estado())).count(); }
    private static List<Activo> filtrar(Datos d,String dc,String cluster,String tipo) {
        return d.considerados().stream().filter(a->dc==null || dc.equals(a.datacenter()))
            .filter(a->cluster==null || ("sin-cluster".equals(cluster)?a.cluster()==null:cluster.equals(a.cluster())))
            .filter(a->tipo==null || tipo.equals(a.tipo())).toList();
    }
    private static void centro(Datos d,String dc) {
        if (d.centros().stream().noneMatch(c->c.nombre().equals(dc))) throw new RecursoNoEncontradoException("No se encontró el datacenter.");
    }
    private static void validarCluster(Datos d,String dc,String cluster) {
        if (!"sin-cluster".equals(cluster) && d.clusters().stream().noneMatch(c->c.nombre().equals(cluster) && dc.equals(c.datacenter()))
                && filtrar(d,dc,cluster,null).isEmpty()) throw new RecursoNoEncontradoException("No se encontró el cluster en este datacenter.");
    }
    private static void validarAlcance(Datos d,String dc,String cluster) {
        if (dc!=null) centro(d,dc);
        if (cluster!=null && dc==null) throw new SolicitudInvalidaException("Selecciona el datacenter del cluster.");
        if (cluster!=null) validarCluster(d,dc,cluster);
    }
    private static boolean porVencer(Activo a,LocalDate hoy,BigDecimal aviso) {
        if (a.eos()==null || aviso==null) return false;
        long dias=ChronoUnit.DAYS.between(hoy,a.eos());
        return dias>=0 && BigDecimal.valueOf(dias).compareTo(aviso)<=0;
    }
    private static Capacidad capacidad(String nombre,BigDecimal uso,BigDecimal aviso) {
        return new Capacidad(nombre,uso==null?null:CIEN.subtract(uso),aviso==null?null:CIEN.subtract(aviso));
    }
    private static BigDecimal uso(List<Activo> a,Function<Activo,BigDecimal> lectura,Function<Activo,BigDecimal> capacidad) {
        BigDecimal suma=BigDecimal.ZERO,total=BigDecimal.ZERO;
        for (var e:a) if (lectura.apply(e)!=null && lectura.apply(e).signum()>=0 && capacidad.apply(e)!=null && capacidad.apply(e).signum()>0) {
            suma=suma.add(lectura.apply(e));total=total.add(capacidad.apply(e));
        }
        return porcentaje(suma,total);
    }
    private static BigDecimal porcentaje(BigDecimal v,BigDecimal c) {
        return v==null || v.signum()<0 || c==null || c.signum()<=0?null:v.multiply(CIEN).divide(c,MathContext.DECIMAL64);
    }
    private static BigDecimal promedio(List<Activo> a,Function<Activo,BigDecimal> lectura) {
        var valores=a.stream().map(lectura).filter(Objects::nonNull).toList();
        return valores.isEmpty()?null:valores.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(valores.size()),MathContext.DECIMAL64);
    }
    private static OffsetDateTime utc(LocalDateTime fecha) { return fecha==null?null:fecha.atOffset(ZoneOffset.UTC); }
    private static BigDecimal coordenada(String v) {
        try {return v==null?null:new BigDecimal(v.trim());} catch(NumberFormatException ex) {return null;}
    }
}
