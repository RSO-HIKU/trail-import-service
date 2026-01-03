package com.hiku.trailImport.controller;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.*;

import com.hiku.trailImport.service.GpxImportService;


@Path("/importGPX")
@Produces(MediaType.APPLICATION_JSON)
public class ImportGPX {
    
    @Inject
    private GpxImportService gpxImportService;

    // Raw body upload: filename from X-Filename header or Content-Disposition
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, "application/gpx+xml", MediaType.WILDCARD})
    public Response importGPXRaw(InputStream body, @Context HttpHeaders headers) {
        try {
            GpxImportService.ImportResult result = gpxImportService.importGpx(body, headers);
            String idStr = (result.id != null) ? String.valueOf(result.id) : "null";
            String safeName = result.name != null ? result.name.replace("\"", "'") : "";
            String safeFile = result.sourceFile != null ? result.sourceFile.replace("\"", "'") : "";
            return Response.ok()
                    .entity("{\"success\":true,\"trailId\":" + idStr + 
                           ",\"name\":\"" + safeName + "\",\"sourceFile\":\"" + safeFile + "\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}")
                    .build();
        }
    }

    // Maybe in future multipart approach can be implemnted over the raw + X-Filename header
    // Multipart form-data upload
    // @POST
    // @Consumes(MediaType.MULTIPART_FORM_DATA)
    // public Response importGPXMultipart(
    //         @FormDataParam("file") InputStream uploadedInputStream,
    //         @FormDataParam("file") FormDataContentDisposition fileDetail) {
        
    //     try {
    //         if (uploadedInputStream == null || fileDetail == null) {
    //             return Response.status(Response.Status.BAD_REQUEST)
    //                     .entity("{\"error\":\"Missing file parameter\"}")
    //                     .build();
    //         }
    
    //         String fileName = fileDetail.getFileName();
    //         if (fileName == null || !fileName.toLowerCase().endsWith(".gpx")) {
    //             return Response.status(Response.Status.BAD_REQUEST)
    //                     .entity("{\"error\":\"Only GPX files are supported\"}")
    //                     .build();
    //         }
    
    //         GpxToWkt.Result res = GpxToWkt.convert(uploadedInputStream, fileName);
    
    //         Long id = trailImportDao.insertTrailNative(
    //             res.name,
    //             res.lengthKm,
    //             res.wktLineString,
    //             fileName
    //         );
    
    //         return Response.ok()
    //                 .entity("{\"success\":true,\"trailId\":" + id + 
    //                        ",\"name\":\"" + res.name.replace("\"", "'") + "\"}")
    //                 .build();
    
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                 .entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}")
    //                 .build();
    //     }
    // }
}
