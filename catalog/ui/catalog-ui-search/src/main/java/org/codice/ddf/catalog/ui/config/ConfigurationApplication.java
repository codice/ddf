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
package org.codice.ddf.catalog.ui.config;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static spark.Spark.exception;
import static spark.Spark.get;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.configuration.HistorianConfiguration;
import ddf.platform.resource.bundle.locator.ResourceBundleLocator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.collections.Factory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.codice.proxy.http.HttpProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class ConfigurationApplication implements SparkApplication {

  public static final String SERVLET_PATH = "./proxy";

  public static final String URL = "url";

  public static final String PROXY_ENABLED = "proxyEnabled";

  public static final String ENDPOINT_NAME = "catalog";

  public static final Factory NEW_SET_FACTORY = TreeSet::new;

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationApplication.class);

  private final UuidGenerator uuidGenerator;

  private String format;

  private List imageryProviders = new ArrayList<>();

  private List defaultLayout = new ArrayList<>();

  private List visualizations = new ArrayList<>();

  private List<Map> imageryProviderUrlMaps = new ArrayList<>();

  private List<Map<String, Object>> imageryProviderMaps = new ArrayList<>();

  private Map<String, String> urlToProxyMap = new HashMap<>();

  private Map terrainProvider;

  private Map<String, Object> proxiedTerrainProvider;

  private List<String> imageryEndpoints = new ArrayList<>();

  private String terrainEndpoint;

  private Boolean editingEnabled = true;

  private Boolean signInEnabled = true;

  private Boolean taskEnabled = false;

  private Boolean gazetteerEnabled = true;

  private Boolean onlineGazetteerEnabled = true;

  private Boolean ingestEnabled = true;

  private Boolean cacheEnabled = true;

  private Boolean unknownErrorBoxEnabled = true;

  private Boolean externalAuthenticationEnabled = false;

  private Boolean experimentalEnabled = false;

  private Boolean webSocketsEnabled = false;

  private Boolean localCatalogEnabled = true;

  private Boolean queryFeedbackEnabled = false;

  private Boolean relevanceScoresEnabled = false;

  private Boolean logoEnabled = false;

  private Boolean historicalSearchEnabled = true;

  private Boolean archiveSearchEnabled = true;

  private Boolean metacardPreviewEnabled = true;

  private Boolean customTextNotationEnabled = false;

  private String customTextNotationAttribute = "title";

  private Boolean spellcheckEnabled = false;

  private Boolean phoneticsEnabled = false;

  private BrandingPlugin branding;

  private Integer timeout = 300000;

  private Integer zoomPercentage = 100;

  private String spacingMode = "comfortable";

  private HttpProxyService httpProxy;

  private int incrementer = 0;

  private Integer resultCount = 250;

  private Integer exportResultLimit = 1000;

  private String projection = "EPSG:4326";

  private String bingKey = "";

  private List<Long> scheduleFrequencyList;

  private Map<String, Set<String>> typeNameMapping = new HashMap<>();

  private Integer autoMergeTime = 1000;

  private String mapHome = "";

  private String queryFeedbackEmailSubjectTemplate;

  private String queryFeedbackEmailBodyTemplate;

  private String queryFeedbackEmailDestination;

  private int maximumUploadSize = 1_048_576;

  private List<String> readOnly =
      ImmutableList.of(
          "checksum",
          "checksum-algorithm",
          "id",
          "metadata",
          "source-id",
          "^metacard\\.",
          "^version\\.",
          "^validation\\.");

  private List<String> summaryShow = Collections.emptyList();

  private List<String> resultShow = Collections.emptyList();

  private Map<String, String> attributeAliases = Collections.emptyMap();

  private List<String> hiddenAttributes = Collections.emptyList();

  private List<String> attributeSuggestionList = Collections.emptyList();

  private Map<String, String> attributeDescriptions = Collections.emptyMap();

  private List<String> listTemplates = Collections.emptyList();

  private int sourcePollInterval = 60000;

  private String uiName;

  private Integer relevancePrecision = 5;

  /** The current historian configuration. */
  private HistorianConfiguration historianConfiguration;

  private String theme;

  private String customPrimaryColor;

  private String customPositiveColor;

  private String customNegativeColor;

  private String customWarningColor;

  private String customFavoriteColor;

  private String customBackgroundNavigation;

  private String customBackgroundAccentContent;

  private String customBackgroundDropdown;

  private String customBackgroundContent;

  private String customBackgroundModal;

  private String customBackgroundSlideout;

  private List<String> basicSearchTemporalSelectionDefault;

  private String basicSearchMatchType;

  private Set<String> editorAttributes = Collections.emptySet();
  private Set<String> requiredAttributes = Collections.emptySet();
  private Map<String, Set<String>> attributeEnumMap = Collections.emptyMap();

  private static final String INTRIGUE_BASE_NAME = "IntrigueBundle";

  private volatile Map<String, String> i18n = Collections.emptyMap();

  public void setI18n(ResourceBundleLocator resourceBundleLocator) {
    try {
      ResourceBundle resourceBundle = resourceBundleLocator.getBundle(INTRIGUE_BASE_NAME);

      if (resourceBundle != null) {
        Enumeration bundleKeys = resourceBundle.getKeys();

        Map<String, String> keywords = new HashMap<>();

        while (bundleKeys.hasMoreElements()) {
          String key = (String) bundleKeys.nextElement();
          String value = resourceBundle.getString(key);

          keywords.put(key, value);
        }

        i18n = keywords;
      }
    } catch (IOException e) {
      LOGGER.debug(
          "An error occurred while creating class loader to URL for ResourceBundle: {}, {}",
          INTRIGUE_BASE_NAME,
          Locale.getDefault(),
          e);
    }
  }

  @VisibleForTesting
  void setI18n(Map<String, String> i18n) {
    this.i18n = i18n;
  }

  @VisibleForTesting
  Map<String, String> getI18n() {
    return this.i18n;
  }

  public Set<String> getEditorAttributes() {
    return editorAttributes;
  }

  public void setEditorAttributes(Set<String> editorAttributes) {
    this.editorAttributes = editorAttributes;
  }

  public Set<String> getRequiredAttributes() {
    return requiredAttributes;
  }

  public void setRequiredAttributes(List<String> requiredAttributes) {
    this.requiredAttributes = new LinkedHashSet<>();
    for (String entry : requiredAttributes) {
      if (StringUtils.isNotBlank(entry)) {
        this.requiredAttributes.add(entry);
      }
    }
  }

  public Map<String, Set<String>> getAttributeEnumMap() {
    return attributeEnumMap;
  }

  public void setAttributeEnumMap(Map<String, Set<String>> attributeEnumMap) {
    this.attributeEnumMap = attributeEnumMap;
  }

  public Set<String> extractValues(String valueString) {
    return new LinkedHashSet<>(Splitter.on(',').trimResults().splitToList(valueString));
  }

  public void setAttributeEnumMap(List<String> entries) {
    Map<String, Set<String>> mergedEntryMap = new LinkedHashMap<>();

    for (String entry : entries) {
      if (StringUtils.isBlank(entry)) {
        continue;
      }

      String[] kvPair = entry.split("=", 2);
      String attribute = kvPair[0].trim();
      if (!attribute.isEmpty()) {
        Set<String> values;
        if (mergedEntryMap.containsKey(attribute)) {
          values = mergedEntryMap.get(attribute);
        } else {
          values = new LinkedHashSet<>();
          mergedEntryMap.put(attribute, values);
        }

        if (kvPair.length == 2) {
          values.addAll(Splitter.on(',').trimResults().omitEmptyStrings().splitToList(kvPair[1]));
        }
      }
    }

    Set<String> attributeSet = new LinkedHashSet<>(mergedEntryMap.keySet());
    setEditorAttributes(attributeSet);
    mergedEntryMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    setAttributeEnumMap(mergedEntryMap);
  }

  public ConfigurationApplication(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  public List<Long> getScheduleFrequencyList() {
    return scheduleFrequencyList;
  }

  public List<String> getSummaryShow() {
    return summaryShow;
  }

  public List<String> getResultShow() {
    return resultShow;
  }

  public List<String> getReadOnly() {
    return readOnly;
  }

  public List<String> getAttributeAliases() {
    return attributeAliases
        .entrySet()
        .stream()
        .map(pair -> String.format("%s=%s", pair.getKey(), pair.getValue()))
        .collect(Collectors.toList());
  }

  public List<String> getHiddenAttributes() {
    return hiddenAttributes;
  }

  public List<String> getListTemplates() {
    return listTemplates;
  }

  public List<String> getAttributeDescriptions() {
    return attributeDescriptions
        .entrySet()
        .stream()
        .map(pair -> String.format("%s=%s", pair.getKey(), pair.getValue()))
        .collect(Collectors.toList());
  }

  public void setScheduleFrequencyList(List<Long> scheduleFrequencyList) {
    this.scheduleFrequencyList = scheduleFrequencyList;
  }

  public void setReadOnly(List<String> readOnly) {
    this.readOnly = readOnly;
  }

  public void setSummaryShow(List<String> summaryShow) {
    this.summaryShow = summaryShow;
  }

  public void setResultShow(List<String> resultShow) {
    this.resultShow = resultShow;
  }

  public void setMaximumUploadSize(int size) {
    this.maximumUploadSize = size;
  }

  public int getMaximumUploadSize() {
    return maximumUploadSize;
  }

  public void setAttributeAliases(List<String> attributeAliases) {
    this.attributeAliases = parseAttributeAndValuePairs(attributeAliases);
  }

  public void setHiddenAttributes(List<String> hiddenAttributes) {
    this.hiddenAttributes = hiddenAttributes;
  }

  public void setAttributeSuggestionList(List<String> list) {
    this.attributeSuggestionList = list;
  }

  public void setListTemplates(List<String> listTemplates) {
    this.listTemplates = listTemplates;
  }

  public void setAttributeDescriptions(List<String> attributeDescriptions) {
    this.attributeDescriptions = parseAttributeAndValuePairs(attributeDescriptions);
  }

  public void setSourcePollInterval(int sourcePollInterval) {
    this.sourcePollInterval = sourcePollInterval;
  }

  public int getSourcePollInterval() {
    return sourcePollInterval;
  }

  public void destroy() {
    stopImageryEndpoints(imageryEndpoints);
    if (terrainEndpoint != null) {
      try {
        httpProxy.stop(terrainEndpoint);
      } catch (Exception e) {
        LOGGER.error("Unable to stop proxy endpoint.", e);
      }
    }
  }

  private List<Map> getConfigImageryProviders() {
    if (imageryProviderUrlMaps.isEmpty()) {
      // @formatter:off
      return Collections.singletonList(
          ImmutableMap.builder()
              .put("type", "SI")
              .put("url", "./images/natural_earth_50m.png")
              .put("parameters", ImmutableMap.of("imageSize", Arrays.asList(10800, 5400)))
              .put("alpha", 1)
              .put("name", "Default Layer")
              .put("show", true)
              .put("proxyEnabled", true)
              .put("order", 0)
              .build());
      // @formatter:on
    } else {
      return imageryProviderUrlMaps;
    }
  }

  private List<Map> getDefaultLayoutConfig() {
    if (defaultLayout == null || defaultLayout.isEmpty()) {
      // @formatter:off
      return Collections.singletonList(
          ImmutableMap.of(
              "type",
              "stack",
              "content",
              Arrays.asList(
                  ImmutableMap.of(
                      "type", "component",
                      "component", "cesium",
                      "componentName", "cesium",
                      "title", "3D Map"),
                  ImmutableMap.of(
                      "type", "component",
                      "component", "inspector",
                      "componentName", "inspector",
                      "title", "Inspector"))));
      // @formatter:on
    } else {
      return defaultLayout;
    }
  }

  private List<Map> getVisualizationsConfig() {
    if (visualizations == null || visualizations.isEmpty()) {
      // @formatter:off
      return Arrays.asList(
          ImmutableMap.of("name", "openlayers", "title", "2D Map", "icon", "map"),
          ImmutableMap.of("name", "cesium", "title", "3D Map", "icon", "globe"),
          ImmutableMap.of("name", "inspector", "title", "Inspector", "icon", "info"),
          ImmutableMap.of("name", "histogram", "title", "Histogram", "icon", "bar-chart"),
          ImmutableMap.of("name", "table", "title", "Table", "icon", "table"));
      // @formatter:on
    } else {
      return defaultLayout;
    }
  }

  public Map<String, Object> getConfig() {
    Map<String, Object> config = new HashMap<>();

    config.put("branding", getProductName());
    config.put("version", getProductVersion());
    config.put("showWelcome", signInEnabled);
    config.put("showTask", taskEnabled);
    config.put("format", format);
    config.put("timeout", timeout);
    config.put("resultCount", resultCount);
    config.put("exportResultLimit", exportResultLimit);
    config.put("typeNameMapping", typeNameMapping);
    config.put("terrainProvider", proxiedTerrainProvider);
    config.put("imageryProviders", getConfigImageryProviders());
    config.put("gazetteer", gazetteerEnabled);
    config.put("onlineGazetteer", onlineGazetteerEnabled);
    config.put("showIngest", ingestEnabled);
    config.put("projection", projection);
    config.put("bingKey", bingKey);
    config.put("externalAuthentication", externalAuthenticationEnabled);
    config.put("readOnly", readOnly);
    config.put("summaryShow", summaryShow);
    config.put("resultShow", resultShow);
    config.put("hiddenAttributes", hiddenAttributes);
    config.put("listTemplates", listTemplates);
    config.put("attributeDescriptions", attributeDescriptions);
    config.put("attributeAliases", attributeAliases);
    config.put("sourcePollInterval", sourcePollInterval);
    config.put("scheduleFrequencyList", scheduleFrequencyList);
    config.put("isEditingAllowed", editingEnabled);
    config.put("isCacheDisabled", !cacheEnabled);
    config.put("disableLocalCatalog", !localCatalogEnabled);
    config.put("queryFeedbackEnabled", queryFeedbackEnabled);
    config.put("queryFeedbackEmailSubjectTemplate", queryFeedbackEmailSubjectTemplate);
    config.put("queryFeedbackEmailBodyTemplate", queryFeedbackEmailBodyTemplate);
    config.put("queryFeedbackEmailDestination", queryFeedbackEmailDestination);
    config.put("zoomPercentage", zoomPercentage);
    config.put("spacingMode", spacingMode);
    config.put("defaultLayout", getDefaultLayoutConfig());
    config.put("visualizations", getVisualizationsConfig());
    config.put("isExperimental", experimentalEnabled);
    config.put("autoMergeTime", autoMergeTime);
    config.put("webSocketsEnabled", webSocketsEnabled);
    config.put("mapHome", mapHome);
    config.put("product", uiName);
    config.put("showRelevanceScores", relevanceScoresEnabled);
    config.put("relevancePrecision", relevancePrecision);
    config.put("showLogo", logoEnabled);
    config.put("isHistoricalSearchDisabled", !historicalSearchEnabled);
    config.put("isArchiveSearchDisabled", !archiveSearchEnabled);
    config.put("isMetacardPreviewDisabled", !metacardPreviewEnabled);
    config.put("isCustomTextNotationEnabled", customTextNotationEnabled);
    config.put("customTextNotationAttribute", customTextNotationAttribute);
    config.put("isSpellcheckEnabled", spellcheckEnabled);
    config.put("isPhoneticsEnabled", phoneticsEnabled);
    config.put(
        "isVersioningEnabled",
        historianConfiguration != null && historianConfiguration.isHistoryEnabled());
    config.put("theme", theme);
    config.put("customPrimaryColor", customPrimaryColor);
    config.put("customPositiveColor", customPositiveColor);
    config.put("customNegativeColor", customNegativeColor);
    config.put("customWarningColor", customWarningColor);
    config.put("customFavoriteColor", customFavoriteColor);
    config.put("customBackgroundNavigation", customBackgroundNavigation);
    config.put("customBackgroundAccentContent", customBackgroundAccentContent);
    config.put("customBackgroundDropdown", customBackgroundDropdown);
    config.put("customBackgroundContent", customBackgroundContent);
    config.put("customBackgroundModal", customBackgroundModal);
    config.put("customBackgroundSlideout", customBackgroundSlideout);
    config.put("disableUnknownErrorBox", !unknownErrorBoxEnabled);
    config.put("editorAttributes", getEditorAttributes());
    config.put("requiredAttributes", getRequiredAttributes());
    config.put("enums", getAttributeEnumMap());
    config.put("basicSearchTemporalSelectionDefault", basicSearchTemporalSelectionDefault);
    config.put("basicSearchMatchType", basicSearchMatchType);
    config.put("useHyphensInUuid", uuidGenerator.useHyphens());
    config.put("i18n", i18n);
    config.put("attributeSuggestionList", attributeSuggestionList);
    return config;
  }

  @Override
  public void init() {
    get("/config", (req, res) -> this.getConfig(), GSON::toJson);

    exception(
        Exception.class,
        (ex, req, res) -> {
          res.status(500);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          LOGGER.warn("Failed to serve request.", ex);
          res.body(GSON.toJson(ImmutableMap.of("message", ex.getMessage())));
        });
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

  public List<Map> getImageryProviderUrlMaps() {
    return imageryProviderUrlMaps;
  }

  public String getImageryProviders() {
    return GSON.toJson(imageryProviders);
  }

  public void setImageryProviders(String imageryProviders) {
    if (StringUtils.isEmpty(imageryProviders)) {
      this.imageryProviders = Collections.emptyList();
    } else {
      try {
        Object o = GSON.fromJson(imageryProviders, List.class);
        if (o != null) {
          this.imageryProviders = (List) o;
          setProxiesForImagery(this.imageryProviders);
        } else {
          this.imageryProviders = Collections.emptyList();
          LOGGER.warn("Could not parse imagery providers as JSON, {}", imageryProviders);
        }
      } catch (ClassCastException e) {
        this.imageryProviders = Collections.emptyList();
        LOGGER.error("Unable to parse terrain provider {} into map.", imageryProviders, e);
      }
    }
  }

  public String getDefaultLayout() {
    return GSON.toJson(defaultLayout);
  }

  public void setDefaultLayout(String defaultLayout) {
    if (StringUtils.isEmpty(defaultLayout)) {
      this.defaultLayout = Collections.emptyList();
    } else {
      try {
        Object o = GSON.fromJson(defaultLayout, List.class);
        if (o != null) {
          this.defaultLayout = (List) o;
        } else {
          this.defaultLayout = Collections.emptyList();
          LOGGER.warn("Could not parse default layout config as JSON, {}", defaultLayout);
        }
      } catch (ClassCastException e) {
        this.defaultLayout = Collections.emptyList();
        LOGGER.error("Unable to parse default layout config {} into map.", defaultLayout, e);
      }
    }
  }

  public Map<String, Object> getProxiedTerrainProvider() {
    return proxiedTerrainProvider;
  }

  public String getTerrainProvider() {
    return GSON.toJson(terrainProvider);
  }

  public void setTerrainProvider(String terrainProvider) {
    if (StringUtils.isEmpty(terrainProvider)) {
      this.terrainProvider = null;
    } else {
      try {
        Object o = GSON.fromJson(terrainProvider, Map.class);
        if (o != null) {
          this.terrainProvider = (Map) o;
        } else {
          this.terrainProvider = null;
          LOGGER.warn("Could not parse terrain providers as JSON, {}", terrainProvider);
        }
      } catch (ClassCastException e) {
        this.terrainProvider = null;
        LOGGER.error("Unable to parse terrain provider {} into map.", terrainProvider, e);
      }
    }
    setProxyForTerrain(this.terrainProvider);
  }

  private void setProxiesForImagery(List<Map<String, Object>> newImageryProviders) {
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
    imageryProviderUrlMaps.clear();
    for (Map<String, Object> newImageryProvider : newImageryProviders) {
      HashMap<String, Object> map = new HashMap<>(newImageryProvider);
      String imageryProviderUrl = newImageryProvider.get(URL).toString();
      boolean proxyEnabled = true;
      Object proxyEnabledProp = newImageryProvider.get(PROXY_ENABLED);
      if (proxyEnabledProp instanceof Boolean) {
        proxyEnabled = (Boolean) proxyEnabledProp;
      }

      if (proxyEnabled) {
        map.put(URL, SERVLET_PATH + "/" + urlToProxyMap.get(imageryProviderUrl));
      } else {
        map.put(URL, imageryProviderUrl);
      }
      imageryProviderUrlMaps.add(map);
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
    for (Iterator<String> iterator = imageryEndpointsToStop.iterator(); iterator.hasNext(); ) {
      String endpoint = iterator.next();
      try {
        httpProxy.stop(endpoint);
        iterator.remove();
      } catch (Exception e) {
        LOGGER.error("Unable to stop proxy endpoint: {}", endpoint, e);
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
        LOGGER.error("Unable to configure proxy for: {}", url, e);
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

    proxiedTerrainProvider = startTerrainEndpoint(terrainProvider);
  }

  private Map<String, Object> startTerrainEndpoint(Map<String, Object> config) {
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
        LOGGER.error("Unable to configure proxy for: {}", url, e);
      }
    }

    return config;
  }

  private Map<String, String> parseAttributeAndValuePairs(List<String> pairs) {
    return pairs
        .stream()
        .map(str -> str.split("=", 2))
        .filter(
            (list) -> {
              if (list.length <= 1) {
                LOGGER.debug("Filtered out invalid attribute/value pair: {}", list[0]);
                return false;
              }
              return true;
            })
        .collect(Collectors.toMap(list -> list[0].trim(), list -> list[1].trim()));
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

  public Integer getExportResultLimit() {
    return exportResultLimit;
  }

  public void setExportResultLimit(Integer exportResultLimit) {
    this.exportResultLimit = exportResultLimit;
  }

  public Boolean getSignInEnabled() {
    return signInEnabled;
  }

  public void setSignInEnabled(Boolean signInEnabled) {
    this.signInEnabled = signInEnabled;
  }

  public Boolean getTaskEnabled() {
    return taskEnabled;
  }

  public void setTaskEnabled(Boolean taskEnabled) {
    this.taskEnabled = taskEnabled;
  }

  public Boolean getGazetteerEnabled() {
    return gazetteerEnabled;
  }

  public void setGazetteerEnabled(Boolean gazetteerEnabled) {
    this.gazetteerEnabled = gazetteerEnabled;
  }

  public Boolean getOnlineGazetteerEnabled() {
    return onlineGazetteerEnabled;
  }

  public void setOnlineGazetteerEnabled(Boolean onlineGazetteerEnabled) {
    this.onlineGazetteerEnabled = onlineGazetteerEnabled;
  }

  public Boolean getIngestEnabled() {
    return this.ingestEnabled;
  }

  public void setIngestEnabled(Boolean ingestEnabled) {
    this.ingestEnabled = ingestEnabled;
  }

  public void setCacheEnabled(Boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public Boolean getEditingEnabled() {
    return this.editingEnabled;
  }

  public void setEditingEnabled(Boolean editingEnabled) {
    this.editingEnabled = editingEnabled;
  }

  public void setUnknownErrorBoxEnabled(Boolean unknownErrorBoxEnabled) {
    this.unknownErrorBoxEnabled = unknownErrorBoxEnabled;
  }

  public Boolean getUnknownErrorBoxEnabled() {
    return unknownErrorBoxEnabled;
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

  public Boolean getExternalAuthenticationEnabled() {
    return externalAuthenticationEnabled;
  }

  public void setExternalAuthenticationEnabled(Boolean externalAuthenticationEnabled) {
    this.externalAuthenticationEnabled = externalAuthenticationEnabled;
  }

  public Boolean getLocalCatalogEnabled() {
    return localCatalogEnabled;
  }

  public void setLocalCatalogEnabled(Boolean localCatalogEnabled) {
    this.localCatalogEnabled = localCatalogEnabled;
  }

  public void setQueryFeedbackEnabled(Boolean queryFeedbackEnabled) {
    this.queryFeedbackEnabled = queryFeedbackEnabled;
  }

  public String getQueryFeedbackEmailSubjectTemplate() {
    return queryFeedbackEmailSubjectTemplate;
  }

  public void setQueryFeedbackEmailSubjectTemplate(String queryFeedbackEmailSubjectTemplate) {
    this.queryFeedbackEmailSubjectTemplate = queryFeedbackEmailSubjectTemplate;
  }

  public String getQueryFeedbackEmailBodyTemplate() {
    return queryFeedbackEmailBodyTemplate;
  }

  public void setQueryFeedbackEmailBodyTemplate(String queryFeedbackEmailBodyTemplate) {
    this.queryFeedbackEmailBodyTemplate = queryFeedbackEmailBodyTemplate;
  }

  public String getQueryFeedbackEmailDestination() {
    return queryFeedbackEmailDestination;
  }

  public void setQueryFeedbackEmailDestination(String queryFeedbackEmailDestination) {
    this.queryFeedbackEmailDestination = queryFeedbackEmailDestination;
  }

  public String getUiName() {
    return uiName;
  }

  public void setUiName(String uiName) {
    this.uiName = uiName;
  }

  public Boolean getRelevanceScoresEnabled() {
    return this.relevanceScoresEnabled;
  }

  public void setRelevanceScoresEnabled(Boolean relevanceScoresEnabled) {
    this.relevanceScoresEnabled = relevanceScoresEnabled;
  }

  public Integer getRelevancePrecision() {
    return this.relevancePrecision;
  }

  public void setRelevancePrecision(Integer relevancePrecision) {
    if (relevancePrecision > 0) {
      this.relevancePrecision = relevancePrecision;
    }
  }

  public Boolean getLogoEnabled() {
    return logoEnabled;
  }

  public void setLogoEnabled(Boolean logoEnabled) {
    this.logoEnabled = logoEnabled;
  }

  public String getSpacingMode() {
    return spacingMode;
  }

  public void setSpacingMode(String spacingMode) {
    this.spacingMode = spacingMode;
  }

  public Integer getZoomPercentage() {
    return zoomPercentage;
  }

  public void setZoomPercentage(Integer zoomPercentage) {
    this.zoomPercentage = zoomPercentage;
  }

  public Integer getAutoMergeTime() {
    return autoMergeTime;
  }

  public void setAutoMergeTime(Integer autoMergeTime) {
    this.autoMergeTime = autoMergeTime;
  }

  public Boolean getWebSocketsEnabled() {
    return webSocketsEnabled;
  }

  public void setWebSocketsEnabled(Boolean webSocketsEnabled) {
    this.webSocketsEnabled = webSocketsEnabled;
  }

  public Boolean getExperimentalEnabled() {
    return experimentalEnabled;
  }

  public void setExperimentalEnabled(Boolean experimentalEnabled) {
    this.experimentalEnabled = experimentalEnabled;
  }

  public String getMapHome() {
    return mapHome;
  }

  public void setMapHome(String mapHome) {
    this.mapHome = mapHome;
  }

  public Boolean getHistoricalSearchEnabled() {
    return historicalSearchEnabled;
  }

  public void setHistoricalSearchEnabled(Boolean historicalSearchEnabled) {
    this.historicalSearchEnabled = historicalSearchEnabled;
  }

  public Boolean getArchiveSearchEnabled() {
    return archiveSearchEnabled;
  }

  public void setArchiveSearchEnabled(Boolean archiveSearchEnabled) {
    this.archiveSearchEnabled = archiveSearchEnabled;
  }

  public Boolean getMetacardPreviewEnabled() {
    return metacardPreviewEnabled;
  }

  public void setMetacardPreviewEnabled(Boolean metacardPreviewEnabled) {
    this.metacardPreviewEnabled = metacardPreviewEnabled;
  }

  public Boolean getCustomTextNotationEnabled() {
    return customTextNotationEnabled;
  }

  public void setCustomTextNotationEnabled(Boolean customTextNotationEnabled) {
    this.customTextNotationEnabled = customTextNotationEnabled;
  }

  public String getCustomTextNotationAttribute() {
    return customTextNotationAttribute;
  }

  public void setCustomTextNotationAttribute(String customTextNotationAttribute) {
    this.customTextNotationAttribute = customTextNotationAttribute;
  }

  public Boolean getSpellcheckEnabled() {
    return spellcheckEnabled;
  }

  public void setSpellcheckEnabled(Boolean spellcheckEnabled) {
    this.spellcheckEnabled = spellcheckEnabled;
  }

  public Boolean getPhoneticsEnabled() {
    return phoneticsEnabled;
  }

  public void setPhoneticsEnabled(Boolean phoneticsEnabled) {
    this.phoneticsEnabled = phoneticsEnabled;
  }

  public void setHistorianConfiguration(HistorianConfiguration historian) {
    if (historian != null) {
      LOGGER.trace("Historian provided, enabled = {}", historian.isHistoryEnabled());
      this.historianConfiguration = historian;
      return;
    }
    LOGGER.trace("Historian was null, enabled = false");
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getCustomPrimaryColor() {
    return customPrimaryColor;
  }

  public void setCustomPrimaryColor(String customPrimaryColor) {
    this.customPrimaryColor = customPrimaryColor;
  }

  public String getCustomPositiveColor() {
    return customPositiveColor;
  }

  public void setCustomPositiveColor(String customPositiveColor) {
    this.customPositiveColor = customPositiveColor;
  }

  public String getCustomNegativeColor() {
    return customNegativeColor;
  }

  public void setCustomNegativeColor(String customNegativeColor) {
    this.customNegativeColor = customNegativeColor;
  }

  public String getCustomWarningColor() {
    return customWarningColor;
  }

  public void setCustomWarningColor(String customWarningColor) {
    this.customWarningColor = customWarningColor;
  }

  public String getCustomFavoriteColor() {
    return customFavoriteColor;
  }

  public void setCustomFavoriteColor(String customFavoriteColor) {
    this.customFavoriteColor = customFavoriteColor;
  }

  public String getCustomBackgroundNavigation() {
    return customBackgroundNavigation;
  }

  public void setCustomBackgroundNavigation(String customBackgroundNavigation) {
    this.customBackgroundNavigation = customBackgroundNavigation;
  }

  public String getCustomBackgroundAccentContent() {
    return customBackgroundAccentContent;
  }

  public void setCustomBackgroundAccentContent(String customBackgroundAccentContent) {
    this.customBackgroundAccentContent = customBackgroundAccentContent;
  }

  public String getCustomBackgroundDropdown() {
    return customBackgroundDropdown;
  }

  public void setCustomBackgroundDropdown(String customBackgroundDropdown) {
    this.customBackgroundDropdown = customBackgroundDropdown;
  }

  public String getCustomBackgroundContent() {
    return customBackgroundContent;
  }

  public void setCustomBackgroundContent(String customBackgroundContent) {
    this.customBackgroundContent = customBackgroundContent;
  }

  public String getCustomBackgroundModal() {
    return customBackgroundModal;
  }

  public void setCustomBackgroundModal(String customBackgroundModal) {
    this.customBackgroundModal = customBackgroundModal;
  }

  public String getCustomBackgroundSlideout() {
    return customBackgroundSlideout;
  }

  public void setCustomBackgroundSlideout(String customBackgroundSlideout) {
    this.customBackgroundSlideout = customBackgroundSlideout;
  }

  public void setBasicSearchTemporalSelectionDefault(
      List<String> basicSearchTemporalSelectionDefault) {
    this.basicSearchTemporalSelectionDefault = basicSearchTemporalSelectionDefault;
  }

  public List<String> getBasicSearchTemporalSelectionDefault() {
    return basicSearchTemporalSelectionDefault;
  }

  public String getVisualizations() {
    return GSON.toJson(visualizations);
  }

  public void setVisualizations(String visualizations) {
    if (StringUtils.isEmpty(visualizations)) {
      this.visualizations = Collections.emptyList();
    } else {
      try {
        Object o = GSON.fromJson(visualizations, List.class);
        if (o != null) {
          this.visualizations = (List) o;
        } else {
          this.visualizations = Collections.emptyList();
          LOGGER.warn("Could not parse visualizations config as JSON, {}", visualizations);
        }
      } catch (ClassCastException e) {
        this.visualizations = Collections.emptyList();
        LOGGER.error("Unable to parse visualizations config {} into map.", visualizations, e);
      }
    }
  }

  public String getBasicSearchMatchType() {
    return basicSearchMatchType;
  }

  public void setBasicSearchMatchType(String basicSearchMatchType) {
    this.basicSearchMatchType = basicSearchMatchType;
  }
}
