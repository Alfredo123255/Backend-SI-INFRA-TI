package com.infrati.backendinfrati.client;

import com.infrati.backendinfrati.dto.ActivoCreado;
import com.infrati.backendinfrati.dto.CrearActivoSolicitud;
import com.infrati.backendinfrati.dto.ProbarRespuesta;
import com.infrati.backendinfrati.dto.ProbarSolicitud;
import com.infrati.backendinfrati.exception.EtlException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EtlClient {
    private final RestClient http;
    private final String apiKey;

    public EtlClient(@Value("${app.etl.base-url}") String baseUrl,
                     @Value("${app.etl.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory transporte = new SimpleClientHttpRequestFactory();
        transporte.setConnectTimeout(Duration.ofSeconds(5));
        transporte.setReadTimeout(Duration.ofSeconds(45));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(transporte).build();
        this.apiKey = apiKey;
    }

    public ProbarRespuesta probar(ProbarSolicitud solicitud) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("ip_gestion", solicitud.ipGestion());
        cuerpo.put("usuario", solicitud.usuario());
        cuerpo.put("clave", solicitud.clave());
        cuerpo.put("clave_privacidad", solicitud.clavePrivacidad());
        Map<String, Object> respuesta = enviar("/api/etl/probar-conexion", cuerpo);
        return new ProbarRespuesta(Boolean.TRUE.equals(respuesta.get("ok")), texto(respuesta, "descripcion"),
                texto(respuesta, "sys_object_id"), texto(respuesta, "sys_name"),
                texto(respuesta, "fabricante"), texto(respuesta, "tipo_detectado"),
                entero(respuesta, "milisegundos"), texto(respuesta, "error_tipo"),
                texto(respuesta, "mensaje"));
    }

    public ActivoCreado crearActivo(CrearActivoSolicitud solicitud) {
        Map<String, Object> respuesta = enviar("/api/etl/crear-activo",
                Map.of("conexion_id", solicitud.conexionId(), "cluster_id", solicitud.clusterId()));
        Map<String, Integer> componentes = new LinkedHashMap<>();
        if (respuesta.get("componentes") instanceof Map<?, ?> filas) {
            filas.forEach((clave, valor) -> componentes.put(String.valueOf(clave), ((Number) valor).intValue()));
        }
        return new ActivoCreado(Boolean.TRUE.equals(respuesta.get("ok")),
                largo(respuesta, "activo_id"), largo(respuesta, "conexion_id"),
                texto(respuesta, "cluster_id"), texto(respuesta, "numero_serie"),
                texto(respuesta, "hostname"), texto(respuesta, "fabricante"),
                texto(respuesta, "tipo_activo"), texto(respuesta, "estado_operativo"),
                texto(respuesta, "modelo"), texto(respuesta, "ubicacion"), componentes,
                entero(respuesta, "metricas_guardadas"), entero(respuesta, "eventos_registrados"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enviar(String ruta, Object cuerpo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EtlException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar la clave de la API ETL.");
        }
        try {
            Map<String, Object> respuesta = http.post().uri(ruta).contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey).body(cuerpo).retrieve().body(Map.class);
            if (respuesta == null) {
                throw new EtlException(HttpStatus.BAD_GATEWAY, "El ETL devolvió una respuesta vacía.");
            }
            return respuesta;
        } catch (RestClientResponseException error) {
            throw convertirError(error);
        } catch (RestClientException error) {
            throw new EtlException(HttpStatus.BAD_GATEWAY, "No se pudo contactar la API ETL.");
        }
    }

    private EtlException convertirError(RestClientResponseException error) {
        int codigo = error.getStatusCode().value();
        return switch (codigo) {
            case 404 -> new EtlException(HttpStatus.NOT_FOUND, "El clúster o la conexión no existe en el ETL.");
            case 409 -> new EtlException(HttpStatus.CONFLICT, "La conexión ya está vinculada a un activo.");
            case 422 -> new EtlException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El ETL rechazó los datos o el perfil SNMP del equipo.");
            case 504 -> new EtlException(HttpStatus.GATEWAY_TIMEOUT, "El equipo no respondió por SNMP.");
            case 502 -> new EtlException(HttpStatus.BAD_GATEWAY, "Falló la consulta SNMP del equipo.");
            case 503 -> new EtlException(HttpStatus.SERVICE_UNAVAILABLE, "La base de datos del ETL no está disponible.");
            case 401 -> new EtlException(HttpStatus.BAD_GATEWAY, "La autenticación del backend con el ETL falló.");
            default -> new EtlException(HttpStatus.BAD_GATEWAY, "El ETL no pudo completar la solicitud.");
        };
    }

    private static String texto(Map<String, Object> nodo, String campo) {
        Object valor = nodo.get(campo);
        return valor == null ? null : String.valueOf(valor);
    }

    private static Integer entero(Map<String, Object> nodo, String campo) {
        Object valor = nodo.get(campo);
        return valor instanceof Number numero ? numero.intValue() : null;
    }

    private static Long largo(Map<String, Object> nodo, String campo) {
        Object valor = nodo.get(campo);
        return valor instanceof Number numero ? numero.longValue() : null;
    }
}
