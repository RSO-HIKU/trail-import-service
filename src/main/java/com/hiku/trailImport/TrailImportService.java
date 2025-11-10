package com.hiku.trailImport;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import com.hiku.trailImport.controller.ImportGPX;

@ApplicationPath("/trail-import")
public class TrailImportService extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<>();
		// Register resource classes explicitly (since we changed package)
		classes.add(ImportGPX.class);
		// Register multipart feature so @FormDataParam works
		classes.add(MultiPartFeature.class);
		return classes;
	}
}