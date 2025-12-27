package com.hiku.trailImport.db.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@ApplicationScoped
public class TrailImportDao {

    @Inject
    private EntityManager em;

    public Long insertTrailNative(String name, double lengthKm, String wktLineString, String sourceFile) {
        try {
            em.getTransaction().begin();
            Query q = em.createNativeQuery(
                "INSERT INTO peaks_hikes_service.trails (name, length_km, geometry, source_file) " +
                "VALUES (:name, :length_km, ST_SetSRID(ST_GeomFromText(:wkt), 4326), :source_file) RETURNING id"
            );
            q.setParameter("name", name);
            q.setParameter("length_km", lengthKm);
            q.setParameter("wkt", wktLineString);
            q.setParameter("source_file", sourceFile);

            Long id = null;
            try {
                Object idObj = q.getSingleResult();
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                } else {
                    id = Long.parseLong(String.valueOf(idObj));
                }
            } catch (Exception idEx) {
                System.err.println("Warning: Could not retrieve ID after insert: " + idEx.getMessage());
            }
            
            em.getTransaction().commit();
            return id;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Native insert failed: " + e.getMessage(), e);
        }
    }
}
