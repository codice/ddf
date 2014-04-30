/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package org.codice.ddf.ui.searchui.standard.properties;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.BinaryContentImpl;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.webconsole.BrandingPlugin;
import org.codice.proxy.http.HttpProxyService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class ConfigurationStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

    public static final String SERVLET_PATH = "/proxy";
    
    private static ConfigurationStore uniqueInstance;

    private String header = "";

    private String footer = "";

    private String style = "";

    private String textColor = "";

    private String wmsServer = "";
    
    private String targetUrl = "";

    private String layers = "";

    private String format = "";

    private Boolean isSignIn = false;

    private BrandingPlugin branding;

    private static String JSON_MIME_TYPE_STRING = "application/json";

    private static MimeType JSON_MIME_TYPE = null;
    
    private Integer timeout = 5000;

    private Boolean isSyncQuery = false;
    
    private HttpProxyService httpProxy = null;
    
    private String endpointName = null;
    
    private BundleContext bundleContext = null;
    
    private int incrementer=0;

    private Integer resultCount = 250;

    static {
        MimeType mime = null;
        try {
            mime = new MimeType(JSON_MIME_TYPE_STRING);
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to create json mimetype.");
        }
        JSON_MIME_TYPE = mime;
    }

    private ConfigurationStore() {
        header = "";
        footer = "";
        style = "";
        textColor = "";
        layers = "";
        format = "";
        timeout = 5000;
    }

    @GET
    @Path("/config")
    public Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
        Response response;
        JSONObject configObj = new JSONObject();
        configObj.put("header", header);
        configObj.put("footer", footer);
        configObj.put("style", style);
        configObj.put("textColor", textColor);
        configObj.put("branding", getProductName());
        configObj.put("version", getProductVersion());
        configObj.put("showWelcome", isSignIn);
        configObj.put("wmsServer", wmsServer);
        configObj.put("layers", layers);
        configObj.put("format", format);
        configObj.put("timeout", timeout);
        configObj.put("sync", isSyncQuery);
        configObj.put("targetUrl", targetUrl);
        configObj.put("resultCount", resultCount);

        String configString = JSONValue.toJSONString(configObj);
        BinaryContent content = new BinaryContentImpl(new ByteArrayInputStream(configString.getBytes()),
                JSON_MIME_TYPE);
        response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();

        return response;
    }

    /**
     * @return a unique instance of {@link ConfigurationStore}
     */
    public static synchronized ConfigurationStore getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new ConfigurationStore();
        }

        return uniqueInstance;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getProductName() {
        if (branding != null) {
            // Remove the version number
            return StringUtils.substringBeforeLast(branding.getProductName(), " ");
        } else {
            return "";
        }
    }

    public String getProductVersion() {
        if (branding != null) {
            // Remove the version number
            return StringUtils.substringAfterLast(branding.getProductName(), " ");
        } else {
            return "";
        }
    }

    public BrandingPlugin getBranding() {
        return branding;
    }

    public void setBranding(BrandingPlugin branding) {
        this.branding = branding;
    }

    public String getWmsServer() {
        return this.wmsServer;
    }

    public void setWmsServer(String wmsServer) {
        this.wmsServer = wmsServer;
    }
    
    public String getLayers() { return layers; }

    public void setLayers(String layers) {
        this.layers = layers;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
    
    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Boolean getSyncQuery() {
        return this.isSyncQuery;
    }

    public void setSyncQuery(Boolean sync) {
        this.isSyncQuery = sync;
    }

    public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    public void update(Map<String, Object> properties) {
    	if (properties != null) {
    		setHeader((String) properties.get("header"));
    		setFooter((String) properties.get("footer"));
    		setStyle((String) properties.get("style"));
    		setTextColor((String) properties.get("textColor"));
    		setWmsServer((String) properties.get("wmsServer"));
    		setLayers((String) properties.get("layers"));
    		setFormat((String) properties.get("format"));
    		setTimeout((Integer) properties.get("timeout"));
    		setSyncQuery((Boolean) properties.get("syncQuery"));
            setResultCount((Integer) properties.get("resultCount"));
            setSignIn((Boolean) properties.get("signIn"));
    		
    		//Fetch the DDF HTTP Proxy
            if(StringUtils.isNotBlank(wmsServer)) {
                startProxy();
            } else {
                LOGGER.debug("No WMS Server was provided in the Standard UI configuration. " +
                		"If you are attempting to connect to a WMS Server, please provide " +
                		"the location of the WMS Server in the Standard UI configuration.");
            }
    		
            LOGGER.debug(
                    "Updated properties: header={}, footer={}, style={}, textColor={},"
                            + "wmsServer={}, layers={}, format={}, timeout={}, syncQuery={}, resultCount{}",
                    header, footer, style, textColor, wmsServer, layers, format, timeout,
                    isSyncQuery, resultCount);
  
    	} else{
    		LOGGER.debug("Properties are empty");
    		//Stop proxy
    		try {
				httpProxy.stop(endpointName);
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
    	}
    }
    
    public void init(){
    	if ((StringUtils.isNotBlank(wmsServer))){
    		startProxy();
    	} else {
    		LOGGER.debug("Cannot instantiate proxy connection.");
    	}
    }
    
    private void startProxy(){		
		try {
			String bundleName = bundleContext.getBundle().getSymbolicName().toLowerCase() + incrementer;
			incrementer++;

            endpointName = httpProxy.start(bundleName, wmsServer, timeout);

			targetUrl = SERVLET_PATH + "/" + endpointName;
			LOGGER.debug("Target URL: " + targetUrl);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
    }

	public HttpProxyService getHttpProxy() {
		return httpProxy;
	}

	public void setHttpProxy(HttpProxyService httpProxy) {
		this.httpProxy = httpProxy;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Boolean getSignIn() {
        return isSignIn;
    }

    public void setSignIn(Boolean isSignIn) {
        this.isSignIn = isSignIn;
    }
}
