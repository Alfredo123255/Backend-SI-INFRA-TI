package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ResumenInfraestructura.*;
import com.infrati.backendinfrati.service.ResumenInfraestructuraService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vistas-consolidadas")
@CrossOrigin(origins="${app.cors.origen-front}")
public class VistasConsolidadasController {
    private final ResumenInfraestructuraService service;
    public VistasConsolidadasController(ResumenInfraestructuraService service) { this.service=service; }
    @GetMapping("/datacenters") public List<Grupo> datacenters() { return service.datacenters(); }
    @GetMapping("/datacenters/{datacenter}/clusters") public List<Grupo> clusters(@PathVariable String datacenter) { return service.clusters(datacenter); }
    @GetMapping("/datacenters/{datacenter}/clusters/{cluster}/equipos")
    public List<Equipo> equiposCluster(@PathVariable String datacenter,@PathVariable String cluster) { return service.equiposCluster(datacenter,cluster); }
    @GetMapping("/tipos") public List<Grupo> tipos(@RequestParam(required=false) String datacenter,@RequestParam(required=false) String cluster) { return service.tipos(datacenter,cluster); }
    @GetMapping("/tipos/{tipo}/equipos")
    public List<Equipo> equiposTipo(@PathVariable String tipo,@RequestParam(required=false) String datacenter,@RequestParam(required=false) String cluster) { return service.equiposTipo(tipo,datacenter,cluster); }
}
