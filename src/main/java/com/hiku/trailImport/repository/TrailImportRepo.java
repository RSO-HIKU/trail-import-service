package com.hiku.trailImport.repository;

import com.hiku.shared.geoDataModels.Peak;
import com.hiku.shared.geoDataModels.Trail;
import org.locationtech.jts.geom.LineString;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TrailImportRepo {

    private final EntityManagerFactory emf;

    public TrailImportRepo() {
        // Create EntityManagerFactory for this microservice's persistence unit
        this.emf = Persistence.createEntityManagerFactory("trailImportPU");
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    // ===== GPX Import Methods =====
    
    /**
     * Inserts a Trail object into the database.
     * The Trail object should have JTS LineString geometry set.
     * @param trail Trail object ready for insertion
     * @return The created Trail with generated ID, or null if insertion fails
     */
    public Trail insertTrailObj(Trail trail) {
        if (trail == null || trail.getGeometry() == null) {
            System.err.println("Cannot import trail: trail or geometry is null");
            return null;
        }
    
        LineString geom = trail.getGeometry();
        System.out.println("[TrailImportRepo] Geometry debug: type=" + geom.getGeometryType()
            + ", srid=" + geom.getSRID()
            + ", points=" + geom.getNumPoints());
        System.out.println("[TrailImportRepo] Geometry WKT: " + geom.toText());

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(trail);
            em.getTransaction().commit();
            System.out.println("Trail imported successfully: " + trail.getName() + " (ID: " + trail.getId() + ")");
            return trail;
        } catch (Exception e) {
            em.getTransaction().rollback();
            System.err.println("Failed to import trail: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

}