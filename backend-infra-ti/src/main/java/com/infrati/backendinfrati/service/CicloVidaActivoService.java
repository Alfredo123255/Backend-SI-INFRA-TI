package com.infrati.backendinfrati.service;
import com.infrati.backendinfrati.dto.EquipoCicloVidaRespuesta;
import com.infrati.backendinfrati.repository.CicloVidaActivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class CicloVidaActivoService {
    private final CicloVidaActivoRepository repository;
    public CicloVidaActivoService(CicloVidaActivoRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public List<EquipoCicloVidaRespuesta> listar() { return repository.listar(); }
}
