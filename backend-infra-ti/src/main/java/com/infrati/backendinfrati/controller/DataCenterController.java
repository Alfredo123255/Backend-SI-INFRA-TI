package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.DataCenterRespuesta;
import com.infrati.backendinfrati.service.DataCenterService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/datacenters")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class DataCenterController {
    private final DataCenterService service;

    public DataCenterController(DataCenterService service) {
        this.service = service;
    }

    @GetMapping
    public List<DataCenterRespuesta> listar() {
        return service.listar();
    }
}
