package com.hiku.trailImport.controller;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.*;

import com.hiku.trailImport.db.dao.TrailImportDao;
import com.hiku.trailImport.util.GpxToWkt;


@Path("/importGPX")
@Produces(MediaType.APPLICATION_JSON)
public class ImportGPX {
    
    @Inject
    private TrailImportDao trailImportDao;

    // Raw body upload: filename from X-Filename header or Content-Disposition
    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM, "application/gpx+xml", MediaType.WILDCARD})
    public Response importGPXRaw(InputStream body, @Context HttpHeaders headers) {
        try {
            String fileName = headers.getHeaderString("X-Filename");
            if (fileName == null || fileName.isBlank()) {
                fileName = headers.getHeaderString("Content-Disposition");
                if (fileName != null && fileName.contains("filename=")) {
                    fileName = fileName.substring(fileName.indexOf("filename=") + 9)
                                       .replaceAll("\"", "")
                                       .trim();
                }
            }
            
            if (fileName == null || fileName.isBlank()) {
                fileName = "uploaded.gpx";
            } else if (!fileName.toLowerCase().endsWith(".gpx")) {
                fileName = fileName + ".gpx";
            }

            GpxToWkt.Result res = GpxToWkt.convert(body, fileName);

            Long id = trailImportDao.insertTrailNative(
                res.name,
                res.lengthKm,
                res.wktLineString,
                fileName
            );

            String idStr = (id != null) ? String.valueOf(id) : "null";
            return Response.ok()
                    .entity("{\"success\":true,\"trailId\":" + idStr + 
                           ",\"name\":\"" + res.name.replace("\"", "'") + "\",\"sourceFile\":\"" + fileName + "\"}")
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
