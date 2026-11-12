package com.infrati.backendinfrati.controller;
import com.infrati.backendinfrati.dto.LimiteMetricaRespuesta;
import com.infrati.backendinfrati.service.UmbralService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/metricas/limites")
@CrossOrigin(origins="${app.cors.origen-front}")
public class LimitesMetricasController {
    private final UmbralService service;
    public LimitesMetricasController(UmbralService service) { this.service=service; }
    @GetMapping public List<LimiteMetricaRespuesta> listar() { return service.limitesMetricas(); }
}
