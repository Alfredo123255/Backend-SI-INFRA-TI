package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.MetricasServidorRespuesta;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.Metrica;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.Opcion;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.Servidor;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.PuertoSwitch;
import com.infrati.backendinfrati.dto.MetricasServidorRespuesta.PuertoMasCargado;
import com.infrati.backendinfrati.repository.MetricasServidorRepository.LecturaPuerto;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.repository.MetricasServidorRepository;
import com.infrati.backendinfrati.repository.MetricasServidorRepository.Ficha;
import com.infrati.backendinfrati.repository.MetricasServidorRepository.Lectura;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetricasServidorService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final MetricasServidorRepository repository;

    public MetricasServidorService(MetricasServidorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public MetricasServidorRespuesta consultar(long id, String periodo, LocalDate desde, LocalDate hasta,
                                                List<Long> comparar) {
        Ficha ficha = ficha(id);
        Rango rango = rango(periodo, desde, hasta, LocalDateTime.now(LIMA));
        List<Opcion> opciones = repository.disponibles(id, ficha.tipo());
        List<Long> ids = comparar == null ? List.of() : comparar;
        Set<Long> permitidos = opciones.stream().map(Opcion::id).collect(Collectors.toSet());
        if (ids.size() > 3 || new HashSet<>(ids).size() != ids.size() || !permitidos.containsAll(ids)) {
            throw new SolicitudInvalidaException(Set.of("RACKEABLE", "BLADE").contains(ficha.tipo())
                    ? "Selecciona hasta tres servidores distintos (rackeables o blade)."
                    : "Selecciona hasta tres equipos distintos del mismo tipo.");
        }
        var intervalos = rango.intervalos();
        return new MetricasServidorRespuesta(cargar(ficha, rango, intervalos),
                ids.stream().map(otro -> cargar(ficha(otro), rango, intervalos)).toList(),
                opciones, rango.agrupacion().nombre, rango.desde(), rango.hasta(), intervalos);
    }

    private Ficha ficha(long id) {
        return repository.ficha(id).orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipo."));
    }

    private Servidor cargar(Ficha ficha, Rango rango, List<LocalDateTime> intervalos) {
        boolean storage = "STORAGE".equals(ficha.tipo());
        boolean esSwitch = "SWITCH".equals(ficha.tipo());
        var nombres = "CHASIS".equals(ficha.tipo()) ? MetricasServidorRepository.METRICAS_CHASIS
                : storage ? MetricasServidorRepository.METRICAS_STORAGE
                : esSwitch ? MetricasServidorRepository.METRICAS_SWITCH : MetricasServidorRepository.METRICAS;
        var actuales = (esSwitch ? repository.actualesSwitch(ficha.id()) : repository.actuales(ficha.id(), nombres)).stream()
                .collect(Collectors.toMap(Lectura::nombre, Function.identity()));
        var historico = repository.historico(ficha.id(), rango.desde(), utc(rango.desde()), utc(rango.hasta()),
                        rango.agrupacion().intervalo, nombres, esSwitch).stream()
                .collect(Collectors.groupingBy(Lectura::nombre,
                        Collectors.toMap(Lectura::fecha, Lectura::valor)));
        var puertosSwitch = esSwitch ? cargarPuertosSwitch(ficha.id(),rango,intervalos) : List.<PuertoSwitch>of();
        var mayor = puertosSwitch.stream().filter(p -> p.utilizacionActual()!=null)
                .max(Comparator.comparing(PuertoSwitch::utilizacionActual));
        var masCargado = mayor.map(p -> new PuertoMasCargado(p.numero(),p.anchoBandaGbps(),p.utilizacionActual())).orElse(null);
        Map<String, Metrica> metricas = new LinkedHashMap<>();
        for (String nombre : nombres) {
            boolean requiereCapacidad = switch (nombre) {
                case "cpu_uso_ghz", "ram_uso_gb", "disco_uso_gb", "almacenamiento_uso_tb" -> true;
                default -> false;
            };
            BigDecimal capacidad = switch (nombre) {
                case "cpu_uso_ghz" -> ficha.cpu();
                case "ram_uso_gb" -> ficha.ram();
                case "disco_uso_gb" -> ficha.disco();
                case "almacenamiento_uso_tb" -> ficha.capacidadStorage();
                default -> null;
            };
            BigDecimal actual = actuales.containsKey(nombre) ? actuales.get(nombre).valor() : null;
            if (requiereCapacidad && capacidad == null && (nombre.equals("cpu_uso_ghz") || nombre.equals("ram_uso_gb"))) actual = null;
            if (nombre.equals("red_utilizacion_pct")) actual=masCargado==null ? null : masCargado.utilizacion();
            var valores = historico.getOrDefault(nombre, Map.of());
            List<BigDecimal> serie = intervalos.stream().map(bloque -> requiereCapacidad
                    ? porcentaje(valores.get(bloque), capacidad) : valores.get(bloque)).toList();
            metricas.put(nombre, new Metrica(actual, nombre.equals("red_utilizacion_pct") ? actual : porcentaje(actual, capacidad), capacidad, serie));
        }
        var ultima = actuales.values().stream().map(Lectura::fecha).max(Comparator.naturalOrder())
                .map(fecha -> fecha.atOffset(ZoneOffset.UTC)).orElse(null);
        var puertos = storage ? repository.puertosRed(ficha.id()) : List.<MetricasServidorRespuesta.PuertoRed>of();
        BigDecimal capacidadRed = storage ? repository.capacidadRedGbps(ficha.id()) : null;
        return new Servidor(ficha.id(), ficha.hostname(), ficha.tipo(), ficha.estadoConexion(),
                ultima, ficha.tieneDiscos(), metricas, capacidadRed, puertos, puertosSwitch, masCargado);
    }

    private List<PuertoSwitch> cargarPuertosSwitch(long id, Rango rango, List<LocalDateTime> intervalos) {
        var actuales=repository.actualesPuertosSwitch(id).stream().collect(Collectors.groupingBy(LecturaPuerto::numero,
                Collectors.toMap(LecturaPuerto::nombre,LecturaPuerto::valor)));
        var historico=repository.historicoPuertosSwitch(id,rango.desde(),utc(rango.desde()),utc(rango.hasta()),rango.agrupacion().intervalo)
                .stream().collect(Collectors.groupingBy(LecturaPuerto::numero,Collectors.groupingBy(LecturaPuerto::nombre,
                        Collectors.toMap(LecturaPuerto::fecha,LecturaPuerto::valor))));
        return repository.inventarioSwitch(id).stream().map(p -> {
            var valores=actuales.getOrDefault(p.numero(),Map.of());
            var mbps=valores.get("red_ancho_banda_mbps");
            var gbps=mbps==null || mbps.signum()<=0 ? null : mbps.movePointLeft(3);
            boolean apagado="Apagado".equals(p.estado());
            var entrada=apagado ? null : valores.get("red_trafico_entrada_mbps");
            var salida=apagado ? null : valores.get("red_trafico_salida_mbps");
            var utilizacion=apagado || gbps==null ? null : valores.get("red_utilizacion_pct");
            var porNombre=historico.getOrDefault(p.numero(),Map.of());
            return new PuertoSwitch(p.id(),p.numero(),p.estado(),gbps,entrada,salida,utilizacion,
                    seriePuerto(porNombre,"red_trafico_entrada_mbps",intervalos),
                    seriePuerto(porNombre,"red_trafico_salida_mbps",intervalos),
                    seriePuerto(porNombre,"red_utilizacion_pct",intervalos));
        }).toList();
    }

    private static List<BigDecimal> seriePuerto(Map<String,Map<LocalDateTime,BigDecimal>> historico,
                                               String nombre,List<LocalDateTime> intervalos) {
        var valores=historico.getOrDefault(nombre,Map.of());
        return intervalos.stream().map(valores::get).toList();
    }

    private static BigDecimal porcentaje(BigDecimal valor, BigDecimal capacidad) {
        return valor == null || capacidad == null || capacidad.signum() <= 0 ? null
                : valor.movePointRight(2).divide(capacidad, MathContext.DECIMAL64);
    }

    private static LocalDateTime utc(LocalDateTime lima) {
        return lima.atZone(LIMA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    static Rango rango(String periodo, LocalDate desde, LocalDate hasta, LocalDateTime ahora) {
        LocalDateTime dia = ahora.toLocalDate().atStartOfDay();
        return switch (periodo) {
            case "24h" -> new Rango(ahora.truncatedTo(ChronoUnit.HOURS).minusHours(23), ahora, Agrupacion.HORA);
            case "7d" -> new Rango(dia.minusDays(6), ahora, Agrupacion.DIA);
            case "30d" -> new Rango(dia.minusDays(29), ahora, Agrupacion.DIA);
            case "1y" -> new Rango(dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(51),
                    ahora, Agrupacion.SEMANA);
            case "custom" -> {
                if (desde == null || hasta == null || desde.isAfter(hasta) || hasta.isAfter(dia.toLocalDate())) {
                    throw new SolicitudInvalidaException("Indica un rango válido de fechas hasta hoy.");
                }
                // Entre fechas distintas, Hasta es exclusivo: 10 -> 11 son 24 horas.
                // La misma fecha sigue permitiendo consultar ese día calendario completo.
                long dias = Math.max(1, ChronoUnit.DAYS.between(desde, hasta));
                LocalDateTime fin = (desde.equals(hasta) ? hasta.plusDays(1) : hasta).atStartOfDay();
                yield new Rango(desde.atStartOfDay(), fin,
                        dias < 7 ? Agrupacion.HORA : dias <= 90 ? Agrupacion.DIA : Agrupacion.SEMANA);
            }
            default -> throw new SolicitudInvalidaException("El periodo solicitado no es válido.");
        };
    }

    enum Agrupacion {
        HORA("hora", "1 hour", Duration.ofHours(1)),
        DIA("dia", "1 day", Duration.ofDays(1)),
        SEMANA("semana", "7 days", Duration.ofDays(7));

        final String nombre;
        final String intervalo;
        final Duration paso;

        Agrupacion(String nombre, String intervalo, Duration paso) {
            this.nombre = nombre;
            this.intervalo = intervalo;
            this.paso = paso;
        }
    }

    record Rango(LocalDateTime desde, LocalDateTime hasta, Agrupacion agrupacion) {
        List<LocalDateTime> intervalos() {
            List<LocalDateTime> bloques = new ArrayList<>();
            for (LocalDateTime t = desde; t.isBefore(hasta); t = t.plus(agrupacion.paso)) bloques.add(t);
            return bloques;
        }
    }
}
