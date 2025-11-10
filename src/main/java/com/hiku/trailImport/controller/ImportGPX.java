package com.hiku.trailImport.controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.glassfish.jersey.media.multipart.FormDataParam;
import com.hiku.trailImport.util.GpxToGeoJsonConverter;


@Path("/importGPX")
public class ImportGPX {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String importGPX(@FormDataParam("file") InputStream uploadedInputStream) throws IOException {

        // Save uploaded GPX temporarily
        String filePath = "/tmp/uploaded.gpx";
        Files.copy(uploadedInputStream, Paths.get(filePath));

        // Convert to GeoJSON
        String geojson = GpxToGeoJsonConverter.convert(filePath);

        return geojson;
    }
}