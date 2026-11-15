package com.infrati.backendinfrati.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Comprueba que la aplicación responde sin exponer datos del sistema. */
@RestController
public class SaludController {
    @GetMapping("/healthz")
    public Map<String, String> salud() {
        return Map.of("status", "ok");
    }
}
