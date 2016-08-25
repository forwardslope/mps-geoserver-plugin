package mil.navy.geomap.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import mil.navy.geomap.mps.api.MPS;

import org.apache.commons.io.IOUtils;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;

public class MPSResource extends AbstractResource {

	static final String BASE_PATH = "/mps";

	private MPS mps;
	
	public void setMps(MPS mps) {
		this.mps = mps;
	}

	@Override
	protected List<DataFormat> createSupportedFormats(Request request, Response response) {

		List<DataFormat> formats = new ArrayList();
		formats.add(new StringFormat( MediaType.TEXT_PLAIN ));
		formats.add(new StringFormat( MediaType.APPLICATION_JSON ));
		formats.add(new StringFormat( MediaType.APPLICATION_XML ));

		return formats;
	}
	
	@Override
	public boolean allowPost() {
		return true;
	}
	
	@Override
	public void handlePost() {
		Request request = this.getRequest();
		Logger logger = getContext().getLogger();
		Representation entity = request.getEntity();
		try {
			InputStream stream = entity.getStream();
			String string = IOUtils.toString(stream);
			logger.info("GOT IT!: " + string);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Bummer", e);
		}
	}

	@Override
	public void handleGet() {
		Logger logger = getContext().getLogger();
		Request request = this.getRequest();		

		Reference resourceRef = request.getResourceRef();
		String query = resourceRef.getQuery();
		String path = resourceRef.getPath();
		
		Map<String, String> queryParamMap = createQueryParamMap(query);
		logger.info("MPS Query Params: " + queryParamMap.toString());

		// TODO: get ssoidcookie

		String[] pathParamArray_IndexZeroIsEmpty = path.split("/");
		String operation = pathParamArray_IndexZeroIsEmpty[4];
		logger.info("MPS Operation Invoked: " + operation);
		List<String> pathParams = new ArrayList<String>();
		StringBuilder pathParamStringBuilder = new StringBuilder();
		if (pathParamArray_IndexZeroIsEmpty.length > 5) {
			for (int i = 5; i < pathParamArray_IndexZeroIsEmpty.length; i++) {
				pathParams.add(pathParamArray_IndexZeroIsEmpty[i]);
				pathParamStringBuilder.append("/").append(pathParamArray_IndexZeroIsEmpty[i]);
			}
		}
		
		logger.info("MPS Path Params: " + pathParamStringBuilder.toString());
		
		String operationEnum = operation.replace("-", "_").toUpperCase();


		String returnString = null;
		switch (MPSOperation.valueOf(operationEnum)) {
		case GET_BOOKMARKS:
			logger.info("Get bookmarks invoked");
			returnString = mps.getBookmarks(null, null, null);
			break;
		case GET_ROLE:
			logger.info("Get role invoked");
			returnString = mps.getRole(null, null);
			break;
		case LOAD_FEATURES:
			logger.info("Load features invoked");
			returnString = mps.loadFeatures(null, null, null, null, null, null, null);
			break;
		default:
			break;
		}
		
		DataFormat format = getFormatGet();
		getResponse().setEntity(format.toRepresentation(returnString));
	}

	private Map<String, String> createQueryParamMap(String query) {
		Map<String, String> queryParamMap = new LinkedHashMap<String, String>();
		if (query == null || query.isEmpty()) {
			return queryParamMap; 
		} else {
			String[] queryParamKVArray = query.split("&");
			for (String queryParamKV : queryParamKVArray) {
				String[] queryParamKVPair = queryParamKV.split("=");
				String queryParamK = queryParamKVPair[0];
				String queryParamV = queryParamKVPair[1];
				queryParamMap.put(queryParamK, queryParamV);
			}
		}
		
		return queryParamMap;
	}
}
