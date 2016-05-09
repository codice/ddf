/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.webconsole.BrandingPlugin;
import org.boon.json.JsonFactory;
import org.codice.proxy.http.HttpProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class ConfigurationStore {

    public static final String SERVLET_PATH = "/proxy";

    public static final String URL = "url";

    public static final String ENDPOINT_NAME = "catalog";

    public static final Factory NEW_SET_FACTORY = new Factory() {
        public Object create() {
            return new TreeSet();
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

    private String format;

    private List imageryProviders = new ArrayList<>();

    private List<Map> proxiedImageryProviders = new ArrayList<>();

    private Map terrainProvider;

    private Map<String, Object> proxiedTerrainProvider;

    private List<String> imageryEndpoints = new ArrayList<>();

    private String terrainEndpoint;

    private Boolean isSignIn = true;

    private Boolean isTask = false;

    private Boolean isGazetteer = true;

    private Boolean isIngest = true;

    private BrandingPlugin branding;

    private Integer timeout = 15000;

    private HttpProxyService httpProxy;

    private int incrementer = 0;

    private Integer resultCount = 250;

    private String projection = "EPSG:3857";

    private String bingKey = "";

    private String helpUrl = "help.html";

    private Boolean isExternalAuthentication = false;

    private Map<String, Set<String>> typeNameMapping = new HashMap<String, Set<String>>();

    public List<String> getSummaryShow() {
        return summaryShow;
    }

    public List<String> getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(List<String> readOnly) {
        this.readOnly = readOnly;
    }

    public void setSummaryShow(List<String> summaryShow) {
        this.summaryShow = summaryShow;
    }

    private List<String> readOnly = Collections.emptyList();

    private List<String> summaryShow = Collections.emptyList();

    public ConfigurationStore() {

    }

    public void destroy() {
        if (imageryEndpoints.size() > 0) {
            for (String endpoint : imageryEndpoints) {
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
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getDocument(@Context UriInfo uriInfo,
            @Context HttpServletRequest httpRequest) {
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

        if (proxiedImageryProviders.isEmpty()) {
            config.put("imageryProviders",
                    Arrays.asList(ImmutableMap.of(
                            "type", "SI",
                            "url", "/search/catalog/images/natural_earth_50m.png",
                            "parameters", ImmutableMap.of(
                                    "imageSize", Arrays.asList(10800, 5400)),
                                    "alpha", 1)));
        } else {
            config.put("imageryProviders", proxiedImageryProviders);
        }

        config.put("gazetteer", isGazetteer);
        config.put("showIngest", isIngest);
        config.put("projection", projection);
        config.put("helpUrl", helpUrl);
        config.put("bingKey", bingKey);
        config.put("externalAuthentication", isExternalAuthentication);
        config.put("readOnly", readOnly);
        config.put("summaryShow", summaryShow);

        return config;
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

    public List<Map> getProxiedImageryProviders() {
        return proxiedImageryProviders;
    }

    public String getImageryProviders() {
        return JsonFactory.create()
                .writeValueAsString(imageryProviders);
    }

    public void setImageryProviders(String imageryProviders) {
        if (StringUtils.isEmpty(imageryProviders)) {
            this.imageryProviders = Collections.emptyList();
        } else {
            Object o = JsonFactory.create()
                    .readValue(imageryProviders, List.class);
            if (o != null) {
                this.imageryProviders = (List) o;
                setProxiesForImagery(this.imageryProviders);
            } else {
                LOGGER.warn("Could not parse imagery providers as JSON, {}", imageryProviders);
            }
        }
    }

    public Map<String, Object> getProxiedTerrainProvider() {
        return proxiedTerrainProvider;
    }

    public String getTerrainProvider() {
        return JsonFactory.create()
                .writeValueAsString(terrainProvider);
    }

    public void setTerrainProvider(String terrainProvider) {
        if (StringUtils.isEmpty(terrainProvider)) {
            this.terrainProvider = null;
        } else {
            Object o = JsonFactory.create()
                    .readValue(terrainProvider, Map.class);
            if (o != null) {
                this.terrainProvider = (Map) o;
                setProxyForTerrain(this.terrainProvider);
            } else {
                LOGGER.warn("Could not parse terrain providers as JSON, {}", terrainProvider);
            }
        }
    }

    private void setProxiesForImagery(List imageryProviders) {
        if (imageryEndpoints.size() > 0) {
            for (String endpoint : imageryEndpoints) {
                try {
                    httpProxy.stop(endpoint);
                } catch (Exception e) {
                    LOGGER.error("Unable to stop proxy endpoint.", e);
                }
            }
        }
        proxiedImageryProviders.clear();

        for (Object provider : imageryProviders) {
            Map proxiedProvider = getProxiedProvider((Map) provider, true);
            if (proxiedProvider != null) {
                proxiedImageryProviders.add(proxiedProvider);
            }
        }
    }

    private void setProxyForTerrain(Map terrainProvider) {
        if (terrainEndpoint != null) {
            try {
                httpProxy.stop(terrainEndpoint);
            } catch (Exception e) {
                LOGGER.error("Unable to stop proxy endpoint.", e);
            }
        }

        proxiedTerrainProvider = getProxiedProvider(terrainProvider, false);
    }

    private Map getProxiedProvider(Map config, boolean imagery) {
        if (config == null) {
            return null;
        }

        if (config.containsKey(URL)) {
            String url = config.get(URL)
                    .toString();

            try {
                String endpointName = ENDPOINT_NAME + incrementer;
                incrementer++;
                endpointName = httpProxy.start(endpointName, url, timeout);
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
                            typeNameMapping.get(displayName)
                                    .add(type);
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

    public Map<String, Set<String>> getTypeNameMapping() {
        return typeNameMapping;
    }

    public void setTypeNameMapping(String string) {
        if (string != null) {
            this.setTypeNameMapping(new String[] {string});
        }
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public String getBingKey() {
        return bingKey;
    }

    public void setBingKey(String bingKey) {
        this.bingKey = bingKey;
    }

    public String getHelpUrl() {
        return helpUrl;
    }

    public void setHelpUrl(String helpUrl) {
        this.helpUrl = helpUrl;
    }

    public Boolean getExternalAuthentication() {
        return isExternalAuthentication;
    }

    public void setExternalAuthentication(Boolean isExternalAuthentication) {
        this.isExternalAuthentication = isExternalAuthentication;
    }
}
