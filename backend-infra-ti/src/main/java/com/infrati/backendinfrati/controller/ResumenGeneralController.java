package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ResumenInfraestructura.*;
import com.infrati.backendinfrati.service.ResumenInfraestructuraService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resumen-general")
@CrossOrigin(origins="${app.cors.origen-front}")
public class ResumenGeneralController {
    private final ResumenInfraestructuraService service;
    public ResumenGeneralController(ResumenInfraestructuraService service) { this.service=service; }
    @GetMapping public Inicio consultar() { return service.inicio(); }
    @GetMapping("/equipos") public List<Equipo> equipos(@RequestParam String filtro) { return service.equiposInicio(filtro); }
}
