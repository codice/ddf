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
package org.codice.ddf.catalog.harvest;

import com.google.common.collect.ImmutableList;
import ddf.catalog.Constants;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.catalog.harvest.file.DirectoryHarvester;
import org.codice.ddf.catalog.harvest.listeners.PersistentListener;
import org.codice.ddf.catalog.harvest.webdav.WebDavHarvester;

public class SourceHarvester {

  private static final String IN_PLACE = "in_place";

  static final String DELETE = "delete";

  static final String MOVE = "move";

  private static final List<String> SUPPORTED_MECHANISMS = ImmutableList.of(IN_PLACE, DELETE, MOVE);

  private static final String HTTP = "http";

  private final CamelContext camelContext;

  private final StorageAdaptor metacardOnlyAdaptor;

  private Map<String, Serializable> attributeOverrides = new HashMap<>();

  private String processingMechanism = DELETE;

  private String monitoredLocation = "";

  private int numThreads = 1;

  private int readLockIntervalMilliseconds = 500;

  private DirectoryHarvester inPlaceDirectoryHarvester = null;

  private WebDavHarvester webDavHarvester = null;

  private ContentDirectoryMonitor contentDirectoryMonitor = null;

  public SourceHarvester(StorageAdaptor adaptor, CamelContext camelContext) {
    Validate.notNull(adaptor, "Argument adaptor may not be null");
    Validate.notNull(camelContext, "Argument camelContext may not be null");

    metacardOnlyAdaptor = adaptor;
    this.camelContext = camelContext;
  }

  public void init() {
    if (StringUtils.isEmpty(monitoredLocation)) {
      throw new IllegalArgumentException("Property {monitoredLocation} may not be empty or null");
    }

    monitoredLocation = stripEndingSlash(monitoredLocation);

    switch (processingMechanism) {
      case DELETE:
      case MOVE:
        contentDirectoryMonitor = new ContentDirectoryMonitor(camelContext);
        contentDirectoryMonitor.setMonitoredDirectoryPath(monitoredLocation);
        contentDirectoryMonitor.setProcessingMechanism(processingMechanism);
        contentDirectoryMonitor.setNumThreads(numThreads);
        contentDirectoryMonitor.setReadLockIntervalMilliseconds(readLockIntervalMilliseconds);
        contentDirectoryMonitor.setAttributeOverrides(getOverridesAsList());
        contentDirectoryMonitor.init();
        break;
      case IN_PLACE:
        createInPlaceHarvester(monitoredLocation);
        break;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Received invalid processing mechanism [%s], but expected [%s].",
                processingMechanism, SUPPORTED_MECHANISMS));
    }
  }

  private List<String> getOverridesAsList() {
    return attributeOverrides
        .entrySet()
        .stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.toList());
  }

  private void createInPlaceHarvester(String location) {
    if (monitoredLocation.startsWith(HTTP)) {
      webDavHarvester =
          new WebDavHarvester(
              location,
              Collections.singleton(new PersistentListener(metacardOnlyAdaptor, location)));
    } else {
      inPlaceDirectoryHarvester =
          new DirectoryHarvester(
              location,
              Collections.singleton(new PersistentListener(metacardOnlyAdaptor, location)));
    }
  }

  /**
   * Invoked when updates are made to the configuration of existing content monitors. This method is
   * invoked by the container as specified by the update-strategy and update-method attributes in
   * Blueprint XML file.
   *
   * @param properties - properties map for the configuration
   */
  public void updateCallback(Map<String, Object> properties) {
    if (MapUtils.isNotEmpty(properties)) {
      setProcessingMechanism(getPropertyAs(properties, "processingMechanism", String.class));
      setMonitoredLocation(getPropertyAs(properties, "monitoredLocation", String.class));
      setNumThreads(getPropertyAs(properties, "numThreads", Integer.class));
      setReadLockIntervalMilliseconds(
          getPropertyAs(properties, "readLockIntervalMilliseconds", Integer.class));

      Object o = properties.get(Constants.ATTRIBUTE_OVERRIDES_KEY);
      if (o instanceof String[]) {
        String[] incomingAttrOverrides = (String[]) o;
        setAttributeOverrides(Arrays.asList(incomingAttrOverrides));
      }

      destroy(0);
      init();
    }
  }

  @SuppressWarnings(
      "squid:S1172" /* The code parameter is required in blueprint-cm-1.0.7. See https://issues.apache.org/jira/browse/ARIES-1436. */)
  public void destroy(int code) {
    if (contentDirectoryMonitor != null) {
      contentDirectoryMonitor.destroy(0);
      contentDirectoryMonitor = null;
    }

    if (webDavHarvester != null) {
      webDavHarvester.destroy();
      webDavHarvester = null;
    }

    if (inPlaceDirectoryHarvester != null) {
      inPlaceDirectoryHarvester.destroy();
      inPlaceDirectoryHarvester = null;
    }
  }

  public void setProcessingMechanism(String processingMechanism) {
    this.processingMechanism = processingMechanism;
  }

  public void setMonitoredLocation(String monitoredLocation) {
    this.monitoredLocation = monitoredLocation;
  }

  public void setNumThreads(int numThreads) {
    this.numThreads = numThreads;
  }

  public void setReadLockIntervalMilliseconds(int readLockIntervalMilliseconds) {
    this.readLockIntervalMilliseconds = readLockIntervalMilliseconds;
  }

  public void setAttributeOverrides(List<String> incomingAttrOverrides) {
    attributeOverrides.clear();

    for (String keyValuePair : incomingAttrOverrides) {
      String[] parts = keyValuePair.split("=");

      if (parts.length != 2) {
        throw new IllegalArgumentException(
            String.format("Invalid attribute override key value pair of [%s].", keyValuePair));
      }

      attributeOverrides.put(parts[0], parts[1]);
    }
  }

  private <T> T getPropertyAs(Map<String, Object> properties, String key, Class<T> clazz) {
    Object property = properties.get(key);
    if (clazz.isInstance(property)) {
      return clazz.cast(property);
    }

    throw new IllegalArgumentException(
        String.format(
            "Received invalid configuration value of [%s] for property [%s]. Expected type of [%s]",
            property, key, clazz.getName()));
  }

  /**
   * Strips the trailing slash from the harvest location, if it exists. This will treat, for
   * example, "/foo/bar" and "/foo/bar/" the same from a persistence tracking standpoint.
   *
   * @param location harvest location
   * @return new string with strip slashed
   */
  private String stripEndingSlash(String location) {
    if (location.endsWith("/")) {
      return location.substring(0, location.length() - 1);
    }
    return location;
  }
}
