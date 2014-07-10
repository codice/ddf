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
package ddf.catalog.source.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores external configuration properties to be used across POJOs.
 */
public class ConfigurationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStore.class);

    private static ConfigurationStore uniqueInstance;

    private String dataDirectoryPath;

    private boolean forceAutoCommit;

    private boolean disableTextPath;

    private Double nearestNeighborDistanceLimit;

    private ConfigurationStore() {
    }

    /**
     * @return a unique instance of {@link ConfigurationStore}
     */
    public static synchronized ConfigurationStore getInstance() {

        if (uniqueInstance == null) {
            LOGGER.debug("Creating new instance of {}", ConfigurationStore.class.getSimpleName());
            uniqueInstance = new ConfigurationStore();
        }

        return uniqueInstance;
    }

    /**
     * @return true, if text path indexing has been disabled
     */
    public boolean isDisableTextPath() {
        return disableTextPath;
    }

    /**
     * @param disableTextPath When set to true, this will turn off text path indexing for every subsequent
     *                        update or insert.
     */
    public void setDisableTextPath(boolean disableTextPath) {
        this.disableTextPath = disableTextPath;
    }

    public String getDataDirectoryPath() {
        return dataDirectoryPath;
    }

    public void setDataDirectoryPath(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
    }

    public Double getNearestNeighborDistanceLimit() {
        return nearestNeighborDistanceLimit;
    }

    public void setNearestNeighborDistanceLimit(Double nearestNeighborDistanceLimit) {
            if (nearestNeighborDistanceLimit >= 0) {
                this.nearestNeighborDistanceLimit = nearestNeighborDistanceLimit;
            } else {
                this.nearestNeighborDistanceLimit = nearestNeighborDistanceLimit * -1;
                LOGGER.info("{} was multiplied by -1 to get a positive number.", nearestNeighborDistanceLimit);
            }
    }

    /**
     * @return true, if forcing auto commit is turned on
     */
    public boolean isForceAutoCommit() {
        return forceAutoCommit;
    }

    /**
     * @param forceAutoCommit When set to true, this will force a soft commit upon every solr transaction such
     *                        as insert, delete,
     */
    public void setForceAutoCommit(boolean forceAutoCommit) {
        this.forceAutoCommit = forceAutoCommit;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
