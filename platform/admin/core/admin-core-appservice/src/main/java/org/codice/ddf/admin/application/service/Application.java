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
package org.codice.ddf.admin.application.service;

import java.net.URI;
import java.util.Set;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;

/**
 * This class defines an application within DDF. An application is a component
 * that can contain multiple features. Inside each one of those features can be
 * one or more bundles.
 *
 */
public interface Application {

    /**
     * Name describing the application.
     *
     * @return name
     */
    String getName();

    /**
     * Version of the application
     *
     * @return version
     */
    String getVersion();

    /**
     * Short description of the application.
     *
     * @return description
     */
    String getDescription();

    /**
     * Retrieves the URI for the underlying application. This is the location of
     * the application's descriptor files which are used to identify its
     * location.
     *
     * @return URI of the location
     */
    URI getURI();

    /**
     * Gets the features that this application contains. This includes
     * <b>all</b> features inside the application regardless if they are
     * auto-started, required or optional.
     *
     * @return Set of the features located within the application
     * @throws ApplicationServiceException
     */
    Set<Feature> getFeatures() throws ApplicationServiceException;

    /**
     * If applicable, returns the single feature in the application that is
     * auto-started. By definition, this feature is considered to be the main
     * one that, in-turn, starts the rest of the application in the correct
     * order.
     *
     * @return Main feature that is auto-started, null if no such feature
     *         exists.
     */
    Feature getMainFeature();

    /**
     * Gets all of the bundles that this application contains. These bundles are
     * defined inside of the features.
     *
     * @return Set of the bundles located within the application.
     * @throws ApplicationServiceException
     */
    Set<BundleInfo> getBundles() throws ApplicationServiceException;

    Set<Feature> getAutoInstallFeatures();

}
