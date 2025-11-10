package com.hiku.trailImport.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal GPX -> GeoJSON converter.
 * Extracts all <trkpt lat=".." lon=".."/> points and outputs a LineString GeoJSON.
 * If no track points are found, tries <rtept> route points.
 * Output CRS is WGS84 as per GPX spec.
 */
public class GpxToGeoJsonConverter {

    public static String convert(String gpxPath) {
        try {
            File f = new File(gpxPath);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            doc.getDocumentElement().normalize();

            List<double[]> coords = new ArrayList<>();
            NodeList trkpts = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < trkpts.getLength(); i++) {
                String lat = trkpts.item(i).getAttributes().getNamedItem("lat").getTextContent();
                String lon = trkpts.item(i).getAttributes().getNamedItem("lon").getTextContent();
                coords.add(new double[]{Double.parseDouble(lon), Double.parseDouble(lat)}); // GeoJSON order: lon, lat
            }
            if (coords.isEmpty()) {
                NodeList rtepts = doc.getElementsByTagName("rtept");
                for (int i = 0; i < rtepts.getLength(); i++) {
                    String lat = rtepts.item(i).getAttributes().getNamedItem("lat").getTextContent();
                    String lon = rtepts.item(i).getAttributes().getNamedItem("lon").getTextContent();
                    coords.add(new double[]{Double.parseDouble(lon), Double.parseDouble(lat)});
                }
            }

            if (coords.isEmpty()) {
                // Return empty GeoJSON FeatureCollection
                return "{\"type\":\"FeatureCollection\",\"features\":[]}";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[");
            for (int i = 0; i < coords.size(); i++) {
                double[] c = coords.get(i);
                if (i > 0) sb.append(',');
                sb.append('[').append(c[0]).append(',').append(c[1]).append(']');
            }
            sb.append("]},\"properties\":{}} ");
            return sb.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to parse GPX: " + escape(e.getMessage()) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}
