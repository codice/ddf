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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.features.BundleInfo;
import org.codice.ddf.admin.application.service.Application;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the application interface. This class exposes a karaf-based repository
 * (identified inside of a feature) as a DDF application.
 */
public class ApplicationImpl implements Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationImpl.class);

  private String name;

  private String description;

  private List<String> bundleLocations;

  private Set<BundleInfo> bundles;

  public ApplicationImpl() {
    bundleLocations = new ArrayList<>();
    bundles = new HashSet<>();
  }

  public BundleInfo bundleToBundleInfo(Bundle bundle) {
    return new BundleInfo() {
      @Override
      public boolean isBlacklisted() {
        return false;
      }

      @Override
      public String getLocation() {
        return bundle.getLocation();
      }

      @Override
      public String getOriginalLocation() {
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

      @Override
      public BundleOverrideMode isOverriden() {
        return BundleOverrideMode.NONE;
      }
    };
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public Set<BundleInfo> getBundles() {
    return bundles;
  }

  public void loadBundles(Map<String, Bundle> bundlesByLocation) {
    for (String loc : bundleLocations) {
      if (bundlesByLocation.containsKey(loc)) {
        if (bundlesByLocation.get(loc).getState() == Bundle.ACTIVE) {
          bundles.add(bundleToBundleInfo(bundlesByLocation.get(loc)));
        } else {
          LOGGER.debug("Unable to find bundle {} of app {} in system.", loc, name);
        }
      }
    }
  }
}
