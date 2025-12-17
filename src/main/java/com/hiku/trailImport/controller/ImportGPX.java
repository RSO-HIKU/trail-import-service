package com.hiku.trailImport.controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import com.hiku.trailImport.util.GpxToGeoJsonConverter;
import com.hiku.trailImport.repository.TrailImportRepo;
import com.hiku.shared.geoDataModels.Trail;

@Path("/importGPX")
public class ImportGPX {
    
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importGPX(
        String gpxContent,
        @HeaderParam("Filename") String headerFileName,
        @QueryParam("fileName") String queryFileName) throws IOException {
    
        // Create repo manually (no CDI)
        TrailImportRepo trailImportRepo = new TrailImportRepo();
        
        if (gpxContent == null || gpxContent.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"GPX content is required\"}")
                    .build();
        }
        
        // Prefer header, then query param, else default
        String fileName = headerFileName != null && !headerFileName.isEmpty()
            ? headerFileName
            : (queryFileName != null && !queryFileName.isEmpty() ? queryFileName : "uploaded.gpx");
        String filePath = "/tmp/" + fileName;
        
        // Write GPX content to temporary file
        Files.write(Paths.get(filePath), gpxContent.getBytes(StandardCharsets.UTF_8));

        try {
            // Convert GPX to Trail object with JTS LineString geometry
            Trail trail = GpxToGeoJsonConverter.convertToTrail(filePath, fileName);

            if (trail == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Failed to parse GPX file\"}")
                        .build();
            }

            // Insert trail into database
            Trail importedTrail = trailImportRepo.insertTrailObj(trail);

            if (importedTrail == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Failed to save trail to database\"}")
                        .build();
            }

            // Return success response with trail details
            String response = String.format(
                    "{\"status\":\"success\",\"message\":\"Trail imported successfully\",\"trail\":{\"id\":%d,\"name\":\"%s\",\"lengthKm\":%.2f,\"sourceFile\":\"%s\"}}",
                    importedTrail.getId(), importedTrail.getName(), importedTrail.getLengthKm(), importedTrail.getSourceFile()
            );

            return Response.ok(response).build();

        } finally {
            // Clean up temporary file
            Files.deleteIfExists(Paths.get(filePath));
        }
    }
}