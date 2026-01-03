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

        return id;
    }

    public Long insertPeakNative(String name, String territory, double latitude, double longitude, Double elevationM) {
        try {
            em.getTransaction().begin();

            Query q = em.createNativeQuery(
                "INSERT INTO peaks_hikes_service.peaks (name, territory, latitude, longitude, elevation_m) " +
                "VALUES (:name, :territory, :lat, :lon, :elev) RETURNING id"
            );
            q.setParameter("name", name);
            q.setParameter("territory", territory);
            q.setParameter("lat", latitude);
            q.setParameter("lon", longitude);
            q.setParameter("elev", elevationM);

            Long id = null;
            try {
                Object idObj = q.getSingleResult();
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                } else {
                    id = Long.parseLong(String.valueOf(idObj));
                }
            } catch (Exception idEx) {
                System.err.println("Warning: Could not retrieve peak ID after insert: " + idEx.getMessage());
            }

            em.getTransaction().commit();
            return id;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Peak native insert failed: " + e.getMessage(), e);
        }
    }
}
