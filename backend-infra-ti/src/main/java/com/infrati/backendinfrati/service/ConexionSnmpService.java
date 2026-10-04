package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.client.EtlClient;
import com.infrati.backendinfrati.dto.ActivoCreado;
import com.infrati.backendinfrati.dto.CrearActivoSolicitud;
import com.infrati.backendinfrati.dto.ProbarRespuesta;
import com.infrati.backendinfrati.dto.ProbarSolicitud;
import com.infrati.backendinfrati.dto.RegistroConexionRequest;
import com.infrati.backendinfrati.exception.EtlException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.MonitoreoSnmp;
import com.infrati.backendinfrati.repository.ClusterRepository;
import com.infrati.backendinfrati.repository.MonitoreoSnmpRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConexionSnmpService {
    private final EtlClient etlClient;
    private final MonitoreoSnmpRepository conexiones;
    private final ClusterRepository clusters;
    private final Map<String, String> clavesPrivacidad;

    public ConexionSnmpService(EtlClient etlClient, MonitoreoSnmpRepository conexiones,
                               ClusterRepository clusters,
                               @Value("${SNMP_PRIVACY_KEYS_JSON:}") String clavesPrivacidadJson) {
        this.etlClient = etlClient;
        this.conexiones = conexiones;
        this.clusters = clusters;
        try {
            this.clavesPrivacidad = clavesPrivacidadJson == null || clavesPrivacidadJson.isBlank()
                    ? Map.of()
                    : new ObjectMapper().readValue(clavesPrivacidadJson, new TypeReference<>() {});
        } catch (JacksonException error) {
            throw new IllegalArgumentException("SNMP_PRIVACY_KEYS_JSON debe ser un objeto JSON válido.", error);
        }
    }

    public ProbarRespuesta probar(ProbarSolicitud solicitud) {
        validarConexion(solicitud.ipGestion(), solicitud.usuario(), solicitud.clave(), solicitud.clavePrivacidad());
        return etlClient.probar(new ProbarSolicitud(solicitud.ipGestion(), solicitud.usuario(),
                solicitud.clave(), resolverClavePrivacidad(solicitud.ipGestion(), solicitud.usuario(),
                        solicitud.clavePrivacidad())));
    }

    public ActivoCreado registrarYCrear(RegistroConexionRequest solicitud) {
        validarConexion(solicitud.ipGestion(), solicitud.usuario(), solicitud.clave(), solicitud.clavePrivacidad());
        if (solicitud.frecuenciaActualizacion() == null || solicitud.frecuenciaActualizacion() <= 0) {
            throw new SolicitudInvalidaException("La frecuencia debe ser un número positivo de segundos.");
        }
        if (solicitud.clusterId() == null || solicitud.clusterId().isBlank()) {
            throw new SolicitudInvalidaException("Debe indicar el clúster del activo.");
        }
        String clusterId = solicitud.clusterId().trim();
        if (!clusters.existsById(clusterId)) {
            throw new RecursoNoEncontradoException("Clúster " + clusterId + " no encontrado");
        }
        // saveAndFlush termina su propia transacción: el ETL consulta esta fila desde otra conexión.
        MonitoreoSnmp conexion = conexiones.saveAndFlush(MonitoreoSnmp.builder()
                .ipGestion(solicitud.ipGestion().trim())
                .usuario(solicitud.usuario().trim())
                .clave(solicitud.clave())
                .clavePrivacidad(resolverClavePrivacidad(solicitud.ipGestion(), solicitud.usuario(),
                        solicitud.clavePrivacidad()))
                .frecuenciaActualizacion(solicitud.frecuenciaActualizacion())
                .estadoConexion("Inactivo")
                .build());
        try {
            return etlClient.crearActivo(new CrearActivoSolicitud(conexion.getId(), clusterId));
        } catch (EtlException error) {
            // Si el ETL llegó a vincular el activo antes de perderse la respuesta, la fila se conserva.
            int eliminadas = conexiones.eliminarSiNoVinculada(conexion.getId());
            if (eliminadas == 0) {
                throw new EtlException(error.getEstado(),
                        "No se confirmó la creación. Comprueba la conexión " + conexion.getId() + " antes de reintentar.");
            }
            throw error;
        }
    }

    private static void validarConexion(String ip, String usuario, String clave, String clavePrivacidad) {
        if (ip == null || ip.isBlank() || ip.length() > 255) {
            throw new SolicitudInvalidaException("La IP de gestión es obligatoria y debe ser válida.");
        }
        if (usuario == null || usuario.isBlank() || usuario.length() > 32) {
            throw new SolicitudInvalidaException("El usuario SNMPv3 es obligatorio.");
        }
        if (clave == null || clave.isBlank() || (clavePrivacidad != null && clavePrivacidad.isBlank())) {
            throw new SolicitudInvalidaException("Las credenciales SNMPv3 son obligatorias.");
        }
    }

    private String resolverClavePrivacidad(String ip, String usuario, String claveSolicitada) {
        if (claveSolicitada != null) {
            return claveSolicitada;
        }
        return clavesPrivacidad.get(ip.trim() + "|" + usuario.trim());
    }
}
