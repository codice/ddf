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
package org.codice.ddf.platform.status.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.codice.ddf.platform.status.Application;
import org.codice.ddf.platform.status.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the application interface. This class exposes a karaf-based
 * repository (identified inside of a feature) as a DDF application.
 *
 */
public class ApplicationImpl implements Application, Comparable<Application> {

    private Logger logger = LoggerFactory.getLogger(ApplicationImpl.class);

    private Repository internalRepo;

    public ApplicationImpl(Repository repo) {
        internalRepo = repo;
    }

    @Override
    public String getName() {
        return internalRepo.getName();
    }

    @Override
    public Set<Feature> getFeatures() throws ApplicationServiceException {
        Set<Feature> featureSet = new HashSet<Feature>();
        try {
            featureSet.addAll(Arrays.asList(internalRepo.getFeatures()));
        } catch (Exception e) {
            logger.warn("Could not obtain list of features from internal repository.");
            throw new ApplicationServiceException("Exception while retrieving feature set.", e);
        }
        return featureSet;
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
    public int hashCode() {
        return internalRepo.getName().hashCode();
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
        return internalRepo.getName().equals(otherApp.getName());
    }

    @Override
    public int compareTo(Application otherApp) {
        return internalRepo.getName().compareTo(otherApp.getName());
    }

    private class BundleInfoComparator implements Comparator<BundleInfo> {

        @Override
        public int compare(BundleInfo bundle1, BundleInfo bundle2) {
            return bundle1.getLocation().compareTo(bundle2.getLocation());
        }

    }

}
