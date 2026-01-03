package com.hiku.trailImport.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.hiku.trailImport.db.dao.TrailImportDao;

//  perform the domain operation and orchestrate persistence
@ApplicationScoped
public class PeakImportService {

    @Inject
    TrailImportDao trailImportDao;

    @Transactional
    public Long importPeak(String name, String territory, Double latitude, Double longitude, Double elevationM) {
        return trailImportDao.insertPeakNative(name, territory, latitude, longitude, elevationM);
    }
}
