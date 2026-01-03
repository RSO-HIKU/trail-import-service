package com.hiku.trailImport.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import java.io.InputStream;

import com.hiku.trailImport.db.dao.TrailImportDao;
import com.hiku.trailImport.util.GpxToWkt;

@ApplicationScoped
public class GpxImportService {

    @Inject
    TrailImportDao trailImportDao;

    public static class ImportResult {
        public Long id;
        public String name;
        public String sourceFile;
    }

    @Transactional
    public ImportResult importGpx(InputStream body, HttpHeaders headers) throws Exception {
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

        ImportResult result = new ImportResult();
        result.id = id;
        result.name = res.name;
        result.sourceFile = fileName;
        return result;
    }
}
