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

import org.apache.log4j.Logger;

/**
 * Stores external configuration properties to be used across POJOs.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class ConfigurationStore {

    private String dataDirectoryPath;

    private boolean forceAutoCommit;

    private static final Logger LOGGER = Logger.getLogger(ConfigurationStore.class);

    public String getDataDirectoryPath() {
        return dataDirectoryPath;
    }

    public void setDataDirectoryPath(String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
    }

    public boolean isForceAutoCommit() {
        return forceAutoCommit;
    }

    public void setForceAutoCommit(boolean forceAutoCommit) {
        this.forceAutoCommit = forceAutoCommit;
    }

    private static ConfigurationStore uniqueInstance;

    private ConfigurationStore() {
    }

    /**
     * 
     * @return a unique instance of {@link ConfigurationStore}
     */
    public static synchronized ConfigurationStore getInstance() {

        if (uniqueInstance == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating new instance of " + ConfigurationStore.class.getSimpleName());
            }
            uniqueInstance = new ConfigurationStore();
        }

        return uniqueInstance;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
