package com.hiku.trailImport.util;

import org.locationtech.jts.geom.Coordinate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GpxToWkt {

    public static class Result {
        public final String name;
        public final String wktLineString;
        public final double lengthKm;

        public Result(String name, String wktLineString, double lengthKm) {
            this.name = name;
            this.wktLineString = wktLineString;
            this.lengthKm = lengthKm;
        }
    }

    public static Result convert(InputStream gpxInputStream, String fileName) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(gpxInputStream);
            doc.getDocumentElement().normalize();

            String trailName = extractTrailName(doc, fileName);

            List<Coordinate> coords = new ArrayList<>();
            NodeList trkpts = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < trkpts.getLength(); i++) {
                Element trkpt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                coords.add(new Coordinate(lon, lat));
            }

            if (coords.isEmpty()) {
                NodeList rtepts = doc.getElementsByTagName("rtept");
                for (int i = 0; i < rtepts.getLength(); i++) {
                    Element rtept = (Element) rtepts.item(i);
                    double lat = Double.parseDouble(rtept.getAttribute("lat"));
                    double lon = Double.parseDouble(rtept.getAttribute("lon"));
                    coords.add(new Coordinate(lon, lat));
                }
            }

            if (coords.isEmpty()) {
                throw new IllegalArgumentException("GPX file contains no track or route points");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("LINESTRING (");
            for (int i = 0; i < coords.size(); i++) {
                Coordinate c = coords.get(i);
                sb.append(c.x).append(' ').append(c.y);
                if (i < coords.size() - 1) sb.append(',').append(' ');
            }
            sb.append(')');

            double lengthKm = calculateLength(coords);

            return new Result(trailName, sb.toString(), lengthKm);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GPX: " + e.getMessage(), e);
        }
    }

    private static String extractTrailName(Document doc, String fallback) {
        NodeList metaNames = doc.getElementsByTagName("name");
        if (metaNames.getLength() > 0) {
            String name = metaNames.item(0).getTextContent().trim();
            if (!name.isEmpty()) return name;
        }
        return fallback.replaceAll("\\.gpx$", "");
    }

    private static double calculateLength(List<Coordinate> coords) {
        double totalKm = 0.0;
        for (int i = 1; i < coords.size(); i++) {
            totalKm += haversine(coords.get(i - 1), coords.get(i));
        }
        return Math.round(totalKm * 100.0) / 100.0;
    }

    private static double haversine(Coordinate c1, Coordinate c2) {
        final double R = 6371.0;
        double lat1 = Math.toRadians(c1.y);
        double lat2 = Math.toRadians(c2.y);
        double dLat = Math.toRadians(c2.y - c1.y);
        double dLon = Math.toRadians(c2.x - c1.x);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
