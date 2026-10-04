package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.DataCenterRespuesta;
import com.infrati.backendinfrati.repository.DataCenterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataCenterService {
    private final DataCenterRepository repository;

    public DataCenterService(DataCenterRepository repository) {
        this.repository = repository;
    }

    public List<DataCenterRespuesta> listar() {
        return repository.findAll(Sort.by("nombre")).stream()
                .map(datacenter -> new DataCenterRespuesta(datacenter.getNombre()))
                .toList();
    }
}
