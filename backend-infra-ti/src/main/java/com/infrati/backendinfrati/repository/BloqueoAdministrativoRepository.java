package com.infrati.backendinfrati.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/** Bloqueos dentro de la transacción administrativa, con el mismo orden usado por el ETL. */
@Repository
public class BloqueoAdministrativoRepository {
    private final EntityManager entityManager;

    public BloqueoAdministrativoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void bloquearCambiosAcceso() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(841904004)").getSingleResult();
    }

    public boolean bloquearActivoParaBaja(Long id) {
        entityManager.createNativeQuery("SELECT id FROM monitoreo_snmp WHERE activo_id=:id FOR UPDATE")
                .setParameter("id", id).getResultList();
        return !entityManager.createNativeQuery("SELECT id FROM activo WHERE id=:id FOR UPDATE")
                .setParameter("id", id).getResultList().isEmpty();
    }
}
