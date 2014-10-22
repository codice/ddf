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
import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

    private String header;

    private String footer;

    private String style;

    private String textColor;

    private String format;

    private List<String> imageryProviders = new ArrayList<String>();

    private List<String> proxiedImageryProviders = new ArrayList<String>();

    private String terrainProvider;

    private String proxiedTerrainProvider;

    private List<String> imageryEndpoints = new ArrayList<String>();

    private String terrainEndpoint;

    private Boolean isSignIn = false;

    private Boolean isTask = false;

    private Boolean isGazetteer = true;

    private BrandingPlugin branding;

    private static MimeType JSON_MIME_TYPE;
    
    private Integer timeout = 5000;
    
    private HttpProxyService httpProxy;

    private BundleContext bundleContext;
    
    private int incrementer=0;

    private Integer resultCount = 250;
    
    private String bundleName;

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
        JSONObject configObj = new JSONObject();
        configObj.put("header", header);
        configObj.put("footer", footer);
        configObj.put("style", style);
        configObj.put("textColor", textColor);
        configObj.put("branding", getProductName());
        configObj.put("version", getProductVersion());
        configObj.put("showWelcome", isSignIn);
        configObj.put("showTask", isTask);
        configObj.put("format", format);
        configObj.put("timeout", timeout);
        configObj.put("resultCount", resultCount);
        configObj.put("typeNameMapping", typeNameMapping);
        configObj.put("terrainProvider", proxiedTerrainProvider);
        configObj.put("imageryProviders", proxiedImageryProviders);
        configObj.put("gazetteer", isGazetteer);

        String configString = JSONValue.toJSONString(configObj);
        BinaryContent content = new BinaryContentImpl(new ByteArrayInputStream(configString.getBytes()),
                JSON_MIME_TYPE);
        response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();

        return response;
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

    public List<String> getProxiedImageryProviders() {
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

    public String getProxiedTerrainProvider() {
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
            String proxiedProvider = getProxiedProvider(provider, true);
            proxiedImageryProviders.add(proxiedProvider);
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

        String proxiedProvider = getProxiedProvider(terrainProvider, false);
        proxiedTerrainProvider = proxiedProvider;
    }

    private String getProxiedProvider(String provider, boolean imagery) {
        int firstPartIdx = provider.indexOf("{");
        int lastPartIdx = provider.indexOf("}");
        String innerPart = provider.substring(firstPartIdx, lastPartIdx);
        String[] providerParts = innerPart.split(";");
        StringBuilder proxiedProviderParts = new StringBuilder();
        for (String part : providerParts) {
            if (part.contains(URL)) {
                String url = part.substring(part.indexOf("=")+1);

                try {
                    bundleName = bundleContext.getBundle().getSymbolicName().toLowerCase() + incrementer;
                    incrementer++;
                    String endpointName = httpProxy.start(bundleName, url, timeout);
                    if (imagery) {
                        imageryEndpoints.add(SERVLET_PATH + "/" + endpointName);
                    } else {
                        terrainEndpoint = SERVLET_PATH + "/" + endpointName;
                    }
                    proxiedProviderParts.append(QUOTE + URL + QUOTE + ":" + QUOTE + SERVLET_PATH + "/" + endpointName + QUOTE + ",");
                } catch (Exception e) {
                    LOGGER.error("Unable to configure proxy for: {}", url, e);
                }
            } else {
                String[] parts = part.split("=");
                if(parts.length == 2) {
                    StringBuilder valuePart = new StringBuilder();
                    if (parts[1].contains("|")) {
                        String[] valueParts = parts[1].split("|");
                        valuePart.append("[");
                        for (String value : valueParts) {
                            valuePart.append(value + ",");
                        }
                        valuePart.deleteCharAt(valuePart.length() - 1);
                        valuePart.append("]");
                    } else {
                        valuePart.append(parts[1]);
                    }
                    proxiedProviderParts.append(QUOTE + parts[0] + QUOTE + ":" + QUOTE + valuePart + QUOTE + ",");
                } else {
                    proxiedProviderParts.append(part + ",");
                }
            }
        }

        String cleanProxiedProviders = proxiedProviderParts.deleteCharAt(proxiedProviderParts.lastIndexOf(",")).toString();
        return "{" + QUOTE + provider.substring(0, firstPartIdx-1).replace("=", "") + QUOTE + ":{" + cleanProxiedProviders + "}}";
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

    public Map<String, Set<String>> getTypeNameMapping() {
        return typeNameMapping;
    }

}
