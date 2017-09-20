/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.searchui.standard.properties;

import static org.boon.Boon.toJson;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.BinaryContentImpl;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.proxy.http.HttpProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.bpsm.edn.EdnIOException;
import us.bpsm.edn.EdnSyntaxException;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

/**
 * Stores external configuration properties.
 *
 * @author ddf.isgs@lmco.com
 */
@Path("/")
public class ConfigurationStore {

  public static final String SERVLET_PATH = "/proxy";

  public static final String URL = "url";

  public static final String ENDPOINT_NAME = "standard";

  public static final Factory NEW_SET_FACTORY = TreeSet::new;

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

  private static MimeType jsonMimeType;

  static {
    MimeType mime = null;
    try {
      String jsonMimeType_STRING = "application/json";
      mime = new MimeType(jsonMimeType_STRING);
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failed to create json mimetype.");
    }
    jsonMimeType = mime;
  }

  private String format;

  private List<String> imageryProviders = new ArrayList<>();

  private List<Map<String, Object>> proxiedImageryProviders = new ArrayList<>();

  private List<Map<String, Object>> imageryProviderMaps = new ArrayList<>();

  private Map<String, String> urlToProxyMap = new HashMap<>();

  private String terrainProvider;

  private Map<String, Object> proxiedTerrainProvider;

  private List<String> imageryEndpoints = new ArrayList<>();

  private String terrainEndpoint;

  private Boolean isSignIn = true;

  private Boolean isTask = false;

  private Boolean isGazetteer = true;

  private Boolean isIngest = true;

  private Optional<BrandingRegistry> branding = Optional.empty();

  private Integer timeout = 15000;

  private HttpProxyService httpProxy;

  private int incrementer = 0;

  private Integer resultCount = 250;

  private String projection = "EPSG:4326";

  private String bingKey = "";

  private String helpUrl = "help.html";

  private Boolean isExternalAuthentication = false;

  private Map<String, Set<String>> typeNameMapping = new HashMap<>();

  public ConfigurationStore() {}

  public void destroy() {
    stopImageryEndpoints(imageryEndpoints);
    if (terrainEndpoint != null) {
      try {
        httpProxy.stop(terrainEndpoint);
      } catch (Exception e) {
        LOGGER.info("Unable to stop proxy endpoint.", e);
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
    config.put("imageryProviders", getProxiedImageryProviders());
    config.put("gazetteer", isGazetteer);
    config.put("showIngest", isIngest);
    config.put("projection", projection);
    config.put("helpUrl", helpUrl);
    config.put("bingKey", bingKey);
    config.put("externalAuthentication", isExternalAuthentication);

    String configJson = toJson(config);
    BinaryContent content =
        new BinaryContentImpl(
            new ByteArrayInputStream(configJson.getBytes(StandardCharsets.UTF_8)), jsonMimeType);
    response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();

    return response;
  }

  public String getProductName() {
    return branding.map(BrandingRegistry::getProductName).orElse("");
  }

  public String getProductVersion() {
    return branding.map(BrandingRegistry::getProductVersion).orElse("");
  }

  public BrandingRegistry getBranding() {
    return branding.orElse(null);
  }

  public void setBranding(BrandingRegistry branding) {
    this.branding = Optional.ofNullable(branding);
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
    if (proxiedImageryProviders.isEmpty()) {
      return Collections.singletonList(
          ImmutableMap.of(
              "type",
              "SI",
              "url",
              "/search/standard/images/natural_earth_50m.png",
              "parameters",
              ImmutableMap.of("imageSize", Arrays.asList(10800, 5400)),
              "alpha",
              1));
    } else {
      return proxiedImageryProviders;
    }
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

  private void setProxiesForImagery(List<String> proxiesForImagery) {
    List<Map<String, Object>> newImageryProviders =
        proxiesForImagery
            .stream()
            .map(this::getProviderConfigMap)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    List<Map<String, Object>> imageryProvidersToStop = new ArrayList<>();
    List<Map<String, Object>> imageryProvidersToStart = new ArrayList<>();

    findDifferences(imageryProviderMaps, newImageryProviders, imageryProvidersToStart);
    findDifferences(newImageryProviders, imageryProviderMaps, imageryProvidersToStop);

    List<String> proxiesToStop =
        imageryProvidersToStop
            .stream()
            .map(provider -> urlToProxyMap.get(provider.get(URL).toString()))
            .collect(Collectors.toList());

    stopImageryEndpoints(proxiesToStop);
    for (Map<String, Object> providerToStop : imageryProvidersToStop) {
      urlToProxyMap.remove(providerToStop.get(URL).toString());
    }
    startImageryEndpoints(imageryProvidersToStart);
    proxiedImageryProviders.clear();
    for (Map<String, Object> newImageryProvider : newImageryProviders) {
      HashMap<String, Object> map = new HashMap<>(newImageryProvider);
      map.put(URL, SERVLET_PATH + "/" + urlToProxyMap.get(newImageryProvider.get(URL).toString()));
      proxiedImageryProviders.add(map);
    }
    imageryProviderMaps = newImageryProviders;
  }

  private void findDifferences(
      List<Map<String, Object>> innerList,
      List<Map<String, Object>> outerList,
      List<Map<String, Object>> differences) {
    differences.addAll(outerList);
    differences.removeIf(innerList::contains);
  }

  private void stopImageryEndpoints(List<String> imageryEndpointsToStop) {
    for (String endpoint : imageryEndpointsToStop) {
      try {
        httpProxy.stop(endpoint);
        imageryEndpoints.remove(endpoint);
      } catch (Exception e) {
        LOGGER.info("Unable to stop proxy endpoint: {}", endpoint, e);
      }
    }
  }

  private void startImageryEndpoints(List<Map<String, Object>> imageryProvidersToStart) {
    for (Map<String, Object> provider : imageryProvidersToStart) {
      String url = provider.get(URL).toString();
      try {
        String endpointName = ENDPOINT_NAME + incrementer++;
        endpointName = httpProxy.start(endpointName, url, timeout);
        urlToProxyMap.put(url, endpointName);
        imageryEndpoints.add(endpointName);
      } catch (Exception e) {
        LOGGER.info("Unable to configure proxy for: {}", url, e);
      }
    }
  }

  private void setProxyForTerrain(String terrainProvider) {
    if (terrainEndpoint != null) {
      try {
        httpProxy.stop(terrainEndpoint);
      } catch (Exception e) {
        LOGGER.info("Unable to stop proxy endpoint.", e);
      }
    }

    proxiedTerrainProvider = startTerrainEndpoint(terrainProvider);
  }

  private Map<String, Object> startTerrainEndpoint(String provider) {
    if (StringUtils.isBlank(provider)) {
      return null;
    }

    Map<String, Object> config = getProviderConfigMap(provider);
    if (config == null) {
      return null;
    }

    if (config.containsKey(URL)) {
      String url = config.get(URL).toString();

      try {
        String endpointName = ENDPOINT_NAME + incrementer++;
        endpointName = httpProxy.start(endpointName, url, timeout);
        terrainEndpoint = endpointName;
        config.put(URL, SERVLET_PATH + "/" + endpointName);
      } catch (Exception e) {
        LOGGER.info("Unable to configure proxy for: {}", url, e);
      }
    }

    return config;
  }

  private Map<String, Object> getProviderConfigMap(String provider) {
    Parser parser = Parsers.newParser(defaultConfiguration());
    Map<String, Object> config;
    try {
      Object value = parser.nextValue(Parsers.newParseable(provider));
      if (value instanceof Map) {
        config = new HashMap<String, Object>((Map) value);
      } else {
        LOGGER.warn(
            "Expected a map for provider configuration but got {} instead: {}",
            value.getClass().getName(),
            provider);
        return null;
      }
    } catch (EdnSyntaxException | EdnIOException e) {
      LOGGER.warn("Unable to parse provider configuration: {}", provider, e);
      return null;
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
