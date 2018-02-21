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
package org.codice.ddf.admin.application.service.impl;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the application interface. This class exposes a karaf-based repository
 * (identified inside of a feature) as a DDF application.
 */
public class ApplicationImpl implements Application, Comparable<Application> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationImpl.class);

  private String name;

  private String description;

  private List<String> bundleLocations;

  public BundleInfo bundleToBundleInfo(Bundle bundle) {
    return new BundleInfo() {
      @Override
      public String getLocation() {
        return bundle.getLocation();
      }

      @Override
      public int getStartLevel() {
        return -1;
      }

      @Override
      public boolean isStart() {
        return false;
      }

      @Override
      public boolean isDependency() {
        return false;
      }
    };
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Set<Feature> getFeatures() throws ApplicationServiceException {
    return Collections.emptySet();
  }

  public Set<Feature> getAutoInstallFeatures() {
    return Collections.emptySet();
  }

  @Override
  public Feature getMainFeature() {
    return null;
  }

  @Override
  public Set<BundleInfo> getBundles() throws ApplicationServiceException {
    Set<BundleInfo> bundles = new HashSet<>();
    Map<String, Bundle> bundleMap = bundlesByLocation();

    for (String loc : bundleLocations) {
      if (bundleMap.containsKey(loc)) {
        if (bundleMap.get(loc).getState() == Bundle.ACTIVE) {
          bundles.add(bundleToBundleInfo(bundleMap.get(loc)));
        } else {
          LOGGER.debug("Unable to find bundle {} of app {} in system.", loc, name);
        }
      }
    }

    return bundles;
  }

  private Map<String, Bundle> bundlesByLocation() {
    return Arrays.stream(
            FrameworkUtil.getBundle(ApplicationImpl.class).getBundleContext().getBundles())
        .collect(Collectors.toMap(Bundle::getLocation, Function.identity()));
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj == this) {
      return true;
    } else if (!(obj instanceof Application)) {
      return false;
    }

    Application otherApp = (Application) obj;
    return name.equals(otherApp.getName());
  }

  @Override
  public int compareTo(Application otherApp) {
    return name.compareTo(otherApp.getName());
  }

  @Override
  public URI getURI() {
    return null;
  }
}
