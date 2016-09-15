package com.fsi.mps.geoserver.catalog;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.fsi.geomap.mps.wfsclient.GetFeature;
import com.fsi.geomap.mps.wfsclient.Query.TypeName;
import com.fsi.geomap.mps.wfsclient.WfsClient;


public class WfsCatalogClient implements WfsClient, ApplicationContextAware {
	
	private Catalog catalog = null;

	private static final Logger log = Logger.getLogger(WfsCatalogClient.class.getName());
	
	public Map<Name, FeatureCollection<? extends FeatureType, ? extends Feature>> getFeatures(GetFeature getFeature) {
		return getFeatures(getFeature, false);
		
	}

	public Map<Name, FeatureCollection<? extends FeatureType, ? extends Feature>> getFeatures(GetFeature getFeature, boolean supportPaging) {
		Map <Name, FeatureCollection<? extends FeatureType, ? extends Feature>> featureCollections = new HashMap<Name, FeatureCollection<? extends FeatureType, ? extends Feature>>();
		if (catalog != null) {
			for (com.fsi.geomap.mps.wfsclient.Query sourceQuery : getFeature.getQueries()) {
				Query query = new Query();
				query.setFilter(sourceQuery.getFilter());
				query.setVersion(sourceQuery.getFeatureVersion());
				if (!supportPaging && getFeature.getMaxFeatures() >= 0) {
					query.setMaxFeatures((int)getFeature.getMaxFeatures());
				}
				for(TypeName typeName : sourceQuery.getTypeName()) {
					FeatureTypeInfo fti = catalog.getFeatureTypeByName(typeName.getName());
					try {
						FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = fti.getFeatureSource(null, null).getFeatures();
						if (featureCollections.get(typeName.getName()) == null) {
							featureCollections.put(typeName.getName(), featureCollection);
						} else {
							
						}
					} catch (IOException e) {
						
					}
					
				}
			}
		} else {
			log.log(Level.WARNING, "GeoServer Catalog not found in WfsCatalogClinet.getFeatures()!");
		}
		return featureCollections;
	}


	public FeatureCollection<? extends FeatureType, ? extends Feature> getFeatures(FeatureType featureType, GetFeature getFeature) {
		return getFeatures(featureType, getFeature, false);
	}

	public FeatureCollection<? extends FeatureType, ? extends Feature> getFeatures(FeatureType featureType, GetFeature getFeature, boolean supportPaging) {
		FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = null;
		if (catalog != null) {
			Map <Name, FeatureCollection<? extends FeatureType, ? extends Feature>> featureCollections = new HashMap<Name, FeatureCollection<? extends FeatureType, ? extends Feature>>();
			List<com.fsi.geomap.mps.wfsclient.Query> sourceQueries = getFeature.getQueries();
			if (sourceQueries.size() == 1) {
				com.fsi.geomap.mps.wfsclient.Query sourceQuery = sourceQueries.get(0);
				Query query = new Query();
				query.setFilter(sourceQuery.getFilter());
				query.setVersion(sourceQuery.getFeatureVersion());
				if (!supportPaging && getFeature.getMaxFeatures() >= 0) {
					query.setMaxFeatures((int)getFeature.getMaxFeatures());
				}
				if (sourceQuery.getTypeName().size() == 1) {
					TypeName typeName = sourceQuery.getTypeName().get(0);
					FeatureTypeInfo fti = catalog.getFeatureTypeByName(typeName.getName());
					try {
						featureCollection = fti.getFeatureSource(null, null).getFeatures(query);
						if (featureCollections.get(typeName.getName()) == null) {
							featureCollections.put(typeName.getName(), featureCollection);
						} else {
							log.log(Level.WARNING, "Illegal type list length (" + sourceQuery.getTypeName().size() + ") in WfsCatalogClient.getFeatures()!");
						}
					} catch (IOException e) {
						log.log(Level.WARNING, "Error retrieving FeatureCollection!", e);
					}
				} else {
					log.log(Level.WARNING, "Illegal query size (" + sourceQueries.size() + ") in WfsCatalogClient.getFeatures()!");
				}
			}
		} else {
			log.log(Level.WARNING, "GeoServer Catalog not found in WfsCatalogClinet.getFeatures()!");
		}
		return featureCollection;
	}

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		this.catalog = context.getBean("catalog", Catalog.class);
	}

	
}
