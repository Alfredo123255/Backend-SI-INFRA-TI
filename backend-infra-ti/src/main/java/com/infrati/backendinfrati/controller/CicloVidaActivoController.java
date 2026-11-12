package com.infrati.backendinfrati.controller;
import com.infrati.backendinfrati.dto.EquipoCicloVidaRespuesta;
import com.infrati.backendinfrati.service.CicloVidaActivoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/activos/ciclo-vida")
@CrossOrigin(origins="${app.cors.origen-front}")
public class CicloVidaActivoController {
    private final CicloVidaActivoService service;
    public CicloVidaActivoController(CicloVidaActivoService service) { this.service=service; }
    @GetMapping public List<EquipoCicloVidaRespuesta> listar() { return service.listar(); }
}
