package com.hiku.trailImport.controller;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.hiku.trailImport.service.PeakImportService;

@Path("/peak")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class ImportPeak {

    public static class PeakPayload {
        public String name;
        public String territory;
        public Double latitude;
        public Double longitude;
        public Double elevation_m;
        public String coordinates; // optional: "lat, lon" combined string
    }

    @Inject
    PeakImportService peakImportService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importPeak(PeakPayload payload) {
        try {
            if (payload == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Request body is required\"}")
                        .build();
            }

            // Allow combined coordinates string "lat, lon" if latitude/longitude are not provided separately
            if ((payload.latitude == null || payload.longitude == null) && payload.coordinates != null) {
                String[] parts = payload.coordinates.split(",");
                if (parts.length == 2) {
                    try {
                        double lat = Double.parseDouble(parts[0].trim());
                        double lon = Double.parseDouble(parts[1].trim());
                        payload.latitude = lat;
                        payload.longitude = lon;
                    } catch (NumberFormatException ignored) {
                        // fall through to validation error below
                    }
                }
            }
            if (payload.name == null || payload.name.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"name is required\"}")
                        .build();
            }
            if (payload.latitude == null || payload.longitude == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"latitude and longitude are required\"}")
                        .build();
            }

            Long id = peakImportService.importPeak(
                payload.name,
                payload.territory,
                payload.latitude,
                payload.longitude,
                payload.elevation_m
            );

            String idStr = (id != null) ? String.valueOf(id) : "null";
            return Response.ok()
                    .entity("{\"success\":true,\"peakId\":" + idStr +
                            ",\"name\":\"" + payload.name.replace("\"", "'") + "\"}")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}")
                    .build();
        }
    }
}
