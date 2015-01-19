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

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.webconsole.BrandingPlugin;
import org.codice.proxy.http.HttpProxyService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.bpsm.edn.EdnIOException;
import us.bpsm.edn.EdnSyntaxException;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.boon.Boon.toJson;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class ConfigurationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

    public static final String SERVLET_PATH = "/proxy";

    public static final String URL = "url";

    public static final String QUOTE = "\"";

    private String format;

    private List<String> imageryProviders = new ArrayList<>();

    private List<Map<String, Object>> proxiedImageryProviders = new ArrayList<>();

    private String terrainProvider;

    private Map<String, Object> proxiedTerrainProvider;

    private List<String> imageryEndpoints = new ArrayList<>();

    private String terrainEndpoint;

    private Boolean isSignIn = true;

    private Boolean isTask = false;

    private Boolean isGazetteer = true;

    private Boolean isIngest = true;

    private BrandingPlugin branding;

    private static MimeType JSON_MIME_TYPE;
    
    private Integer timeout = 15000;
    
    private HttpProxyService httpProxy;

    private BundleContext bundleContext;
    
    private int incrementer=0;

    private Integer resultCount = 250;
    
    private String bundleName;

    private String projection = "EPSG:3857";

    private Map<String, Set<String>> typeNameMapping = new HashMap<String, Set<String>>();

    public static final Factory NEW_SET_FACTORY = new Factory() {
        public Object create() {
            return new TreeSet();
        }
    };

    static {
        MimeType mime = null;
        try {
            String JSON_MIME_TYPE_STRING = "application/json";
            mime = new MimeType(JSON_MIME_TYPE_STRING);
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to create json mimetype.");
        }
        JSON_MIME_TYPE = mime;
    }

    public ConfigurationStore() {

    }

    public void destroy() {
        if (imageryEndpoints.size() > 0) {
            for(String endpoint : imageryEndpoints) {
                try {
                    httpProxy.stop(endpoint);
                } catch (Exception e) {
                    LOGGER.error("Unable to stop proxy endpoint.", e);
                }
            }
        }
        if (terrainEndpoint != null) {
            try {
                httpProxy.stop(terrainEndpoint);
            } catch (Exception e) {
                LOGGER.error("Unable to stop proxy endpoint.", e);
            }
        }
    }

    @GET
    @Path("/config")
    public Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
        Response response;
        Map<String, Object> config = new HashMap<>();

        config.put("branding", getProductName());
        config.put("version", getProductVersion());
        config.put("showWelcome", isSignIn);
        config.put("showTask", isTask);
        config.put("format", format);
        config.put("timeout", timeout);
        config.put("resultCount", resultCount);
        config.put("typeNameMapping", typeNameMapping);
        config.put("terrainProvider", proxiedTerrainProvider);
        config.put("imageryProviders", proxiedImageryProviders);
        config.put("gazetteer", isGazetteer);
        config.put("showIngest", isIngest);
        config.put("projection", projection);

        String configJson = toJson(config);
        BinaryContent content = new BinaryContentImpl(new ByteArrayInputStream(configJson.getBytes()),
                JSON_MIME_TYPE);
        response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();

        return response;
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

    public List<Map<String, Object>> getProxiedImageryProviders() {
        return proxiedImageryProviders;
    }

    public List<String> getImageryProviders() {
        return imageryProviders;
    }

    public void setImageryProviders(String imageryProviders) {
        setImageryProviders(Arrays.asList(imageryProviders.split(",")));
    }

    public void setImageryProviders(List<String> imageryProviders) {
        List<String> itemList = new ArrayList<String>();
        for (String item : imageryProviders) {
            if (item.contains(",")) {
                String[] items = item.split(",");
                itemList.addAll(Arrays.asList(items));
            } else {
                itemList.add(item);
            }
        }
        this.imageryProviders = itemList;
        setProxiesForImagery(itemList);
    }

    public Map<String, Object> getProxiedTerrainProvider() {
        return proxiedTerrainProvider;
    }

    public String getTerrainProvider() {
        return terrainProvider;
    }

    public void setTerrainProvider(String terrainProvider) {
        this.terrainProvider = terrainProvider;
        setProxyForTerrain(terrainProvider);
    }

    private void setProxiesForImagery(List<String> imageryProviders) {
        if (imageryEndpoints.size() > 0) {
            for(String endpoint : imageryEndpoints) {
                try {
                    httpProxy.stop(endpoint);
                } catch (Exception e) {
                    LOGGER.error("Unable to stop proxy endpoint.", e);
                }
            }
        }
        proxiedImageryProviders.clear();

        for(String provider : imageryProviders) {
            Map<String, Object> proxiedProvider = getProxiedProvider(provider, true);
            if (proxiedProvider != null) {
                proxiedImageryProviders.add(proxiedProvider);
            }
        }
    }

    private void setProxyForTerrain(String terrainProvider) {
        if (terrainEndpoint != null) {
            try {
                httpProxy.stop(terrainEndpoint);
            } catch (Exception e) {
                LOGGER.error("Unable to stop proxy endpoint.", e);
            }
        }

        proxiedTerrainProvider = getProxiedProvider(terrainProvider, false);
    }

    private Map<String, Object> getProxiedProvider(String provider, boolean imagery) {
        if (StringUtils.isBlank(provider)) {
            return null;
        }

        Parser parser = Parsers.newParser(defaultConfiguration());
        Map<String, Object> config;
        try {
            Object value = parser.nextValue(Parsers.newParseable(provider));
            if (value instanceof Map) {
                config = new HashMap<String, Object>((Map) value);
            } else {
                LOGGER.warn("Expected a map for provider configuration but got {} instead: {}",
                        value.getClass().getName(), provider);
                return null;
            }
        } catch (EdnSyntaxException|EdnIOException e) {
            LOGGER.warn("Unable to parse provider configuration: " + provider, e);
            return null;
        }

        if (config.containsKey(URL)) {
            String url = config.get(URL).toString();

            try {
                bundleName = bundleContext.getBundle().getSymbolicName().toLowerCase() + incrementer;
                incrementer++;
                String endpointName = httpProxy.start(bundleName, url, timeout);
                if (imagery) {
                    imageryEndpoints.add(endpointName);
                } else {
                    terrainEndpoint = endpointName;
                }
                config.put(URL, SERVLET_PATH + "/" + endpointName);
            } catch (Exception e) {
                LOGGER.error("Unable to configure proxy for: {}", url, e);
            }
        }

        return config;
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

    public Boolean getTask() {
        return isTask;
    }

    public void setTask(Boolean isTask) {
        this.isTask = isTask;
    }

    public Boolean getGazetteer() {
        return isGazetteer;
    }

    public void setGazetteer(Boolean isGazetteer) {
        this.isGazetteer = isGazetteer;
    }

    public Boolean getIngest() {
        return this.isIngest;
    }

    public void setIngest(Boolean isIngest) {
        this.isIngest = isIngest;
    }

    public void setTypeNameMapping(String[] mappings) {
        if (mappings != null) {
            typeNameMapping = MapUtils.lazyMap(new TreeMap(), NEW_SET_FACTORY);

            for (String mappingValue : mappings) {
                // workaround for KARAF-1701
                for (String mapping : StringUtils.split(mappingValue, ",")) {
                    String[] nameAndType = StringUtils.split(mapping, "=");
                    if (nameAndType.length == 2) {
                        String displayName = StringUtils.strip(nameAndType[0]);
                        String type = StringUtils.strip(nameAndType[1]);
                        if (StringUtils.isNotBlank(displayName) && StringUtils.isNotBlank(type)) {
                            typeNameMapping.get(displayName).add(type);
                        }
                    } else {
                        LOGGER.info("Invalid type display name mapping format {}", mapping);
                    }
                }
            }
        }
    }

    public void setTypeNameMapping(List<String> mappings) {
        if (mappings != null) {
            this.setTypeNameMapping(mappings.toArray(new String[mappings.size()]));
        }
    }

    public void setTypeNameMapping(String string) {
        if (string != null) {
            this.setTypeNameMapping(new String[]{string});
        }
    }

    public Map<String, Set<String>> getTypeNameMapping() {
        return typeNameMapping;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

}
