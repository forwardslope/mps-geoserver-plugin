package com.fsi.geomap.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.locationtech.geomesa.kafka.KafkaFeatureEvent;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

import com.fsi.geomap.mps.wfsclient.WfsNotifier;

@Singleton

public class MpsFeatureListenerImpl implements MpsFeatureListener {
	private static final Logger logger = Logger.getLogger(MpsFeatureListenerImpl.class.getName());

	private Map<Name, FeatureListener> featureListenerMap = new ConcurrentHashMap<Name, FeatureListener>();
	private static Catalog catalog = null;
	private static ApplicationContext context = null;
	
	public MpsFeatureListenerImpl() {
	}

	// prints out attribute values for a SimpleFeature
	private void printFeature(Feature f) {
		System.out.print("fid:" + f.getIdentifier());
		for (Property prop : f.getProperties()) {
			if (prop.getValue() != null) {
				logger.log(Level.FINEST, " | " + prop.getName() + ":" + prop.getValue());
			}
		}
	}

	// private void registerListeners(DataStore consumerDS) throws IOException {
	// for (String typename : consumerDS.getTypeNames()) {
	// try {
	// registerListenerForFeature(consumerDS, typename);
	// } catch (KafkaListenerException e) {
	// logger.log(Level.WARNING, "Error registering Kafka listener for type " +
	// typename + "!", e);
	// }
	// }
	// }

	// the live consumer must be created before the producer writes features
	// in order to read streaming data.
	// i.e. the live consumer will only read data written after its
	// instantiation

	public FeatureListener registerListenerForFeature(final Name name, final WfsNotifier notifier)
			throws IOException, FeatureListenerException {
		try {

			FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
			if (fti == null) {
				logger.log(Level.WARNING, "FeatureType " + name + " does not exist in GeoServer catalog, assuming external WFS!");
				return null;
			}
			FeatureSource<? extends FeatureType, ? extends Feature> consumerFS = fti.getFeatureSource(null, null);
			FeatureListener consumerFL = null;
			logger.log(Level.INFO, "Registering a feature listener for feature type " + name.getURI() + ".");

			consumerFS.addFeatureListener(consumerFL = new FeatureListener() {
				public void changed(FeatureEvent featureEvent) {
					logger.log(Level.INFO, "Received FeatureEvent from layer " + name.getURI() + " of Type: "
							+ featureEvent.getType());
					if (featureEvent.getType() == FeatureEvent.Type.CHANGED) {
						if (featureEvent instanceof KafkaFeatureEvent) {
		                	SimpleFeature feature = ((KafkaFeatureEvent) featureEvent).feature();
		                	if (logger.isLoggable(Level.FINEST)) {
		                		printFeature(feature);
		                	}
		                    Name name = new NameImpl(feature.getFeatureType().getName().getNamespaceURI(), feature.getFeatureType().getName().getLocalPart());
							try {
								logger.log(Level.FINER,
										"Received Kafka CHANGED notification event for " + name.getURI() + "!");
								notifier.processNotification(name, feature);
								logger.log(Level.FINEST, "Processed CHANGED Kafka notification event for " + name + "!");
							} catch (Exception e) {
								logger.log(Level.WARNING, "Error processing Kafka CHANGED Notification! "
										+ (e.getMessage() != null ? e.getMessage() : ""), e);
							}
						} else {
							FeatureSource<? extends FeatureType, ? extends Feature> featureSource = featureEvent.getFeatureSource();
							FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection = null;
							try {
								featureCollection = featureSource.getFeatures(featureEvent.getFilter());
								FeatureIterator<? extends Feature> featureIterator = featureCollection.features();
								while (featureIterator.hasNext()) {
									Feature feature = featureIterator.next();
									if (logger.isLoggable(Level.FINEST)) {
										printFeature(feature);
									}
									try {
										if (name != null) {
											logger.log(Level.FINER,
													"Received non-Kafka CHANGED notification event for " + name.getURI() + "!");
											notifier.processNotification(name, feature);
											logger.log(Level.FINEST, "Processed CHANGED non-Kafka notification event for " + name + "!");
										}
									} catch (Exception e) {
										logger.log(Level.WARNING, "Error processing non-Kafka CHANGED Notification! "
												+ (e.getMessage() != null ? e.getMessage() : ""), e);
									}
								}
		
								if (featureEvent.getType() == FeatureEvent.Type.REMOVED) {
									logger.log(Level.FINE,
											"Received DELETE notification for filter: " + featureEvent.getFilter());
									if (name != null) {
										notifier.processDeleteNotification(name, featureEvent.getFilter());
										logger.log(Level.FINEST, "Processed DELETE notification event!");
									}
								}
							} catch (IOException e) {
								logger.log(Level.WARNING, "Error processing notification", e);
								
							}
						}
					}
				}
			});
			featureListenerMap.put(name, consumerFL);
			return consumerFL; 
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Error registering FeatureListener!", t);
			return null;
		}
	}

	public FeatureListener removeListenerForFeature(Name name) {
		if (catalog != null) {
			try {
				FeatureListener listener = featureListenerMap.remove(name);
				if (listener != null) {
					FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
					FeatureSource<? extends FeatureType, ? extends Feature> consumerFS = fti.getFeatureSource(null, null);
					consumerFS.removeFeatureListener(listener);
					return listener;
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Error retrieving typeNames while finalizing MpsFeatureListenerImpl", e);
			}
		}
		return null;
	}

    @Inject
	public void setContext(ApplicationContext newContext) {
    	context = newContext;
		catalog = context.getBean("catalog", Catalog.class);
    }
		
	@PreDestroy
	public void dispose() {
		if (catalog != null) {
			for (Name name : featureListenerMap.keySet()) {
				removeListenerForFeature(name);
			}
		}
	}
}
