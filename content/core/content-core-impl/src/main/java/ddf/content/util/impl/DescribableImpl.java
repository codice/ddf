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
package ddf.content.util.impl;

import org.apache.log4j.Logger;

import ddf.content.util.Describable;

/**
 * Default implementation of the Describable interface, providing basic setter/getter methods for a
 * describable item's ID, title, version, organization, and description.
 * 
 * @version 0.1.0
 * @since 2.1.0
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public abstract class DescribableImpl implements Describable {
    private static Logger logger = Logger.getLogger(DescribableImpl.class);

    private String version = null;

    private String id = null;

    private String title = null;

    private String description = null;

    private String organization = null;

    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the describable item.
     * 
     * @param version
     */
    public void setVersion(String version) {
        logger.debug("Setting version = " + version);
        this.version = version;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the describable item.
     * 
     * @param id
     */
    public void setId(String id) {
        logger.debug("ENTERING: setId - id = " + id);
        this.id = id;
        logger.debug("EXITING: setId");
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the describable item.
     * 
     * @param title
     */
    public void setTitle(String title) {
        logger.debug("ENTERING: setTitle");
        this.title = title;
        logger.debug("EXITING: setTitle");
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the describable item.
     * 
     * @param description
     */
    public void setDescription(String description) {
        logger.debug("ENTERING: setDescription");
        this.description = description;
        logger.debug("EXITING: setDescription");
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    /**
     * Sets the organization of the describable item.
     * 
     * @param organization
     */
    public void setOrganization(String organization) {
        logger.debug("ENTERING: setOrganization");
        logger.debug("Setting organization = " + organization);
        this.organization = organization;
        logger.debug("EXITING: setOrganization");
    }

}
