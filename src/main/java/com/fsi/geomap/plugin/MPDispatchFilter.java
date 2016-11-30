package com.fsi.geomap.plugin;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.geoserver.platform.AdvancedDispatchFilter;

public class MPDispatchFilter extends AdvancedDispatchFilter {
	
	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
        	String path = ((HttpServletRequest) request).getPathInfo();
        	if (path != null && path.indexOf("/mps") == 0) { // if this is an MPS request, bypass GeoServer filtering
                chain.doFilter(request, response);
        	} else {
                super.doFilter(request, response, chain); // perform GeoServer filtering
        	}
        } else {
            super.doFilter(request, response, chain); // perform GeoServer filtering
        }
    }

}
