package com.hiku.trailImport.util;

import com.hiku.shared.geoDataModels.Trail;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GPX -> Trail converter.
 * Extracts track data and creates Trail objects with JTS LineString geometry.
 * Compatible with PostGIS for spatial queries and proximity searches.
 */
public class GpxToGeoJsonConverter {

    private static final int SRID = 4326;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Converts a GPX file to a Trail object ready for database insertion.
     * @param gpxPath Path to the GPX file
     * @param sourceFileName The GPX filename (e.g., "Vešter-Lubnik.gpx")
     * @return Trail object with JTS LineString geometry, or null if parsing fails
     */
    public static Trail convertToTrail(String gpxPath, String sourceFileName) {
        try {
            File f = new File(gpxPath);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            doc.getDocumentElement().normalize();

            // Extract trail name from GPX metadata or filename
            String trailName = extractTrailName(doc);
            if (trailName == null || trailName.isEmpty()) {
                trailName = sourceFileName.replace(".gpx", "").replace(".GPX", "");
            }

            // Extract coordinates (track points or route points)
            List<Coordinate> coordinates = extractCoordinates(doc);
            if (coordinates.isEmpty()) {
                throw new Exception("No track or route points found in GPX file");
            }

            // Create JTS LineString (PostGIS compatible)
            LineString lineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[0]));
            lineString.setSRID(SRID);

            // Calculate length in km
            double lengthKm = calculateDistance(coordinates);

            // Create and populate Trail object
            Trail trail = new Trail();
            trail.setName(trailName);
            trail.setGeometry(lineString);
            trail.setLengthKm(lengthKm);
            trail.setSourceFile(sourceFileName);
            // createdAt will be set automatically by @PrePersist
            // region and difficulty can be set to null or defaults if not in GPX

            return trail;
        } catch (Exception e) {
            System.err.println("Failed to parse GPX: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts track name from GPX metadata (trk/name or rtept/name)
     */
    private static String extractTrailName(Document doc) {
        // Try to get from track name
        NodeList trkNames = doc.getElementsByTagName("trk");
        if (trkNames.getLength() > 0) {
            NodeList names = ((org.w3c.dom.Element) trkNames.item(0)).getElementsByTagName("name");
            if (names.getLength() > 0) {
                return names.item(0).getTextContent();
            }
        }

        // Try to get from route name
        NodeList rtNames = doc.getElementsByTagName("rte");
        if (rtNames.getLength() > 0) {
            NodeList names = ((org.w3c.dom.Element) rtNames.item(0)).getElementsByTagName("name");
            if (names.getLength() > 0) {
                return names.item(0).getTextContent();
            }
        }

        return null;
    }

    /**
     * Extracts coordinates from GPX (track points or route points)
     * Returns coordinates in WGS84 (SRID 4326) suitable for PostGIS
     */
    private static List<Coordinate> extractCoordinates(Document doc) {
        List<Coordinate> coordinates = new ArrayList<>();

        // Try track points first
        NodeList trkpts = doc.getElementsByTagName("trkpt");
        for (int i = 0; i < trkpts.getLength(); i++) {
            Node node = trkpts.item(i);
            double lat = Double.parseDouble(node.getAttributes().getNamedItem("lat").getTextContent());
            double lon = Double.parseDouble(node.getAttributes().getNamedItem("lon").getTextContent());
            // JTS uses (lon, lat) order for WGS84
            coordinates.add(new Coordinate(lon, lat));
        }

        // Fall back to route points if no track points
        if (coordinates.isEmpty()) {
            NodeList rtepts = doc.getElementsByTagName("rtept");
            for (int i = 0; i < rtepts.getLength(); i++) {
                Node node = rtepts.item(i);
                double lat = Double.parseDouble(node.getAttributes().getNamedItem("lat").getTextContent());
                double lon = Double.parseDouble(node.getAttributes().getNamedItem("lon").getTextContent());
                coordinates.add(new Coordinate(lon, lat));
            }
        }

        return coordinates;
    }

    /**
     * Calculates the total distance in kilometers using Haversine formula
     */
    private static double calculateDistance(List<Coordinate> coordinates) {
        if (coordinates.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < coordinates.size() - 1; i++) {
            totalDistance += haversineDistance(
                    coordinates.get(i).getY(), coordinates.get(i).getX(),
                    coordinates.get(i + 1).getY(), coordinates.get(i + 1).getX()
            );
        }

        return Math.round(totalDistance * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Haversine formula to calculate distance between two points in km
     */
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
