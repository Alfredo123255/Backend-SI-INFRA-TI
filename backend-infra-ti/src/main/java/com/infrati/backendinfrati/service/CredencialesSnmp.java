package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** AES-256-GCM: nonce de 12 bytes, texto cifrado y tag de 16 bytes. */
@Service
public class CredencialesSnmp {
    private static final String PREFIJO = "enc:v1:";
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final SecretKeySpec llave;

    public CredencialesSnmp(@Value("${SNMP_CREDENTIAL_KEY:}") String valor,
                            @Value("${SNMP_CREDENTIAL_KEY_FILE:}") String archivo) {
        try {
            String texto = valor == null ? "" : valor.trim();
            if (texto.isEmpty() && archivo != null && !archivo.isBlank()) {
                texto = Files.readString(Path.of(archivo), StandardCharsets.US_ASCII).trim();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(texto);
            if (bytes.length != 32) throw new IllegalArgumentException();
            this.llave = new SecretKeySpec(bytes, "AES");
            Arrays.fill(bytes, (byte) 0);
        } catch (Exception error) {
            throw new IllegalStateException("Configura una llave SNMP de 32 bytes mediante SNMP_CREDENTIAL_KEY_FILE o SNMP_CREDENTIAL_KEY.");
        }
    }

    private static byte[] contexto(String campo) {
        if (!"clave".equals(campo) && !"clave_privacidad".equals(campo)) {
            throw new IllegalArgumentException("Campo de credencial no admitido");
        }
        return ("infra-ti:monitoreo_snmp:" + campo + ":v1").getBytes(StandardCharsets.UTF_8);
    }

    public String cifrar(String valor, String campo) {
        if (valor == null) return null;
        if (valor.getBytes(StandardCharsets.UTF_8).length > 158) {
            throw new SolicitudInvalidaException("La clave SNMP admite hasta 158 bytes UTF-8 al almacenarse cifrada.");
        }
        try {
            byte[] nonce = new byte[12];
            ALEATORIO.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(contexto(campo));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] sobre = Arrays.copyOf(nonce, nonce.length + cifrado.length);
            System.arraycopy(cifrado, 0, sobre, nonce.length, cifrado.length);
            return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(sobre);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo cifrar la credencial SNMP.");
        }
    }

    public String descifrar(String valor, String campo) {
        // Compatibilidad transitoria con filas anteriores a la conversión.
        if (valor == null || !valor.startsWith("enc:")) return valor;
        try {
            if (!valor.startsWith(PREFIJO)) throw new IllegalArgumentException();
            byte[] sobre = Base64.getUrlDecoder().decode(valor.substring(PREFIJO.length()));
            if (sobre.length < 28) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(128, Arrays.copyOf(sobre, 12)));
            cipher.updateAAD(contexto(campo));
            return new String(cipher.doFinal(sobre, 12, sobre.length - 12), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo descifrar la credencial SNMP; comprueba la llave externa.");
        }
    }
}
