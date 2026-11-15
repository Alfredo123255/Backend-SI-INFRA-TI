package com.infrati.backendinfrati.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class SeparacionCapasTest {
    @Test
    void losServiciosNoImportanJdbcNiEjecutanSql() throws Exception {
        var prohibido = Pattern.compile("JdbcTemplate|ConnectionCallback|SELECT |UPDATE |pg_notify|createNativeQuery|createQuery|prepareStatement");
        try (var archivos = Files.walk(Path.of("src/main/java/com/infrati/backendinfrati/service"))) {
            for (var archivo : archivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(prohibido.matcher(Files.readString(archivo)).find(), archivo + ": acceso a datos fuera del repositorio");
            }
        }
    }
}
