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
package org.codice.ddf.admin.application.service.impl;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the application interface. This class exposes a karaf-based
 * repository (identified inside of a feature) as a DDF application.
 */
public class ApplicationImpl implements Application, Comparable<Application> {

    private Logger logger = LoggerFactory.getLogger(ApplicationImpl.class);

    private Set<Feature> features;

    private Set<Feature> autoInstallFeatures;

    private Feature mainFeature;

    private String name;

    private String version;

    private String description;

    private URI location;

    /**
     * Creates a new instance of application.
     *
     * @param repo Creates the application from a Karaf Feature Repository
     *             object.
     */
    public ApplicationImpl(Repository repo) {
        location = repo.getURI();
        try {
            String[] parts = repo.getName().split("-[0-9]");
            if (parts.length != 0){
                name = parts[0];
                version = repo.getName().substring(name.length()+1);
            } else {
                name = repo.getName();
                version = "0.0.0";
            }
            features = new HashSet<>(Arrays.asList(repo.getFeatures()));
        } catch (Exception e) {
            logger.warn(
                    "Error occured while trying to parse information for application. Application created but may have missing information.");
            features = new HashSet<>();
        }
        autoInstallFeatures = new HashSet<>();
        if (features.size() == 1) {
            autoInstallFeatures.add(features.iterator()
                    .next());
        } else {
            autoInstallFeatures.addAll(features.stream()
                    .filter(curFeature -> StringUtils.equalsIgnoreCase(Feature.DEFAULT_INSTALL_MODE,
                            curFeature.getInstall()))
                    .collect(Collectors.toList()));
        }
        // Determine mainFeature
        if (autoInstallFeatures.size() == 1) {
            mainFeature = autoInstallFeatures.iterator()
                    .next();
            name = mainFeature.getName();
            version = mainFeature.getVersion();
            description = mainFeature.getDescription();
        } else {
            Optional<Feature> first = autoInstallFeatures.stream()
                    .filter(f -> name.equals(f.getName()))
                    .findFirst();
            if (first.isPresent()) {
                mainFeature = first.get();
                name = mainFeature.getName();
                version = mainFeature.getVersion();
                description = mainFeature.getDescription();
            }
        }

        if (mainFeature == null) {
            logger.debug(
                    "Could not determine main feature in {}, using defaults. Each application "
                            + "should have 1 feature with the same name as the repository or 1 auto"
                            + " install feature. This Application will take no action when started"
                            + " or stopped.",
                    name);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Set<Feature> getFeatures() throws ApplicationServiceException {
        if (features != null) {
            return Collections.unmodifiableSet(features);
        } else {
            throw new ApplicationServiceException("No features found in application " + name
                    + " check the feature definition and log for errors.");
        }
    }

    public Set<Feature> getAutoInstallFeatures() {
        return autoInstallFeatures;
    }

    @Override
    public Feature getMainFeature() {
        return mainFeature;
    }

    @Override
    public Set<BundleInfo> getBundles() throws ApplicationServiceException {
        Set<BundleInfo> bundles = new TreeSet<BundleInfo>(new BundleInfoComparator());
        for (Feature curFeature : getFeatures()) {
            bundles.addAll(curFeature.getBundles());
        }

        return bundles;
    }

    @Override
    public String toString() {
        return name + " - " + version;
    }

    @Override
    public int hashCode() {
        return name.concat(version)
                .hashCode();
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
        return name.equals(otherApp.getName()) && version.equals(otherApp.getVersion());
    }

    @Override
    public int compareTo(Application otherApp) {
        int nameCompare = name.compareTo(otherApp.getName());
        if (nameCompare == 0) {
            return version.compareTo(otherApp.getVersion());
        } else {
            return nameCompare;
        }
    }

    @Override
    public URI getURI() {
        return location;
    }

    private static class BundleInfoComparator implements Comparator<BundleInfo>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(BundleInfo bundle1, BundleInfo bundle2) {
            return bundle1.getLocation()
                    .compareTo(bundle2.getLocation());
        }

    }
}
