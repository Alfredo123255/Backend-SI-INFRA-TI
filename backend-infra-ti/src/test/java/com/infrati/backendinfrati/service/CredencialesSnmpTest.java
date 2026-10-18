package com.infrati.backendinfrati.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CredencialesSnmpTest {
    private final CredencialesSnmp servicio = new CredencialesSnmp("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "");

    @Test void leeVectorCreadoPorPythonYExportaVectorJava() throws Exception {
        assertEquals("Contraseña prueba Ñ-123", servicio.descifrar("enc:v1:AAECAwQFBgcICQoLy6RdJnKc_JwByTPFjXGraTYNiFkusL7E_Of1cpX-AIXMOVgPx2_kvwY", "clave"));
        Path archivo = Path.of("target/credencial-java-prueba.txt");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, servicio.cifrar("Contraseña prueba Ñ-123", "clave_privacidad"));
    }

    @Test void nonceDistintoYRechazaManipulacionOCambioDeCampo() {
        String primero = servicio.cifrar("Enterprise", "clave");
        assertNotEquals(primero, servicio.cifrar("Enterprise", "clave"));
        assertEquals("Enterprise", servicio.descifrar(primero, "clave"));
        assertThrows(IllegalStateException.class, () -> servicio.descifrar(primero, "clave_privacidad"));
        String alterado = primero.substring(0, primero.length()-2) + "AA";
        assertThrows(IllegalStateException.class, () -> servicio.descifrar(alterado, "clave"));
        var otra = new CredencialesSnmp("AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=", "");
        assertThrows(IllegalStateException.class, () -> otra.descifrar(primero, "clave"));
    }

    @Test void validaLlaveYLongitudAntesDeGuardar() {
        assertThrows(IllegalStateException.class, () -> new CredencialesSnmp("", ""));
        assertTrue(servicio.cifrar("x".repeat(158), "clave_privacidad").length() <= 255);
        assertThrows(com.infrati.backendinfrati.exception.SolicitudInvalidaException.class,
                () -> servicio.cifrar("x".repeat(159), "clave_privacidad"));
        assertNull(servicio.descifrar(null, "clave"));
        assertEquals("anterior", servicio.descifrar("anterior", "clave"));
    }
}
