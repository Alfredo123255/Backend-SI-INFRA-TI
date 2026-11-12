package com.infrati.backendinfrati.repository;
import com.infrati.backendinfrati.dto.EquipoCicloVidaRespuesta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class CicloVidaActivoRepository {
    private final JdbcTemplate jdbc;
    public CicloVidaActivoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<EquipoCicloVidaRespuesta> listar() {
        return jdbc.query("SELECT id,hostname,tipo_activo,estado_operativo FROM activo ORDER BY hostname,id",
            (rs,n)->new EquipoCicloVidaRespuesta(rs.getLong("id"),rs.getString("hostname"),rs.getString("tipo_activo"),rs.getString("estado_operativo")));
    }
}
