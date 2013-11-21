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
package ddf.catalog.util.impl;

import org.apache.log4j.Logger;

import ddf.catalog.util.Describable;
import ddf.catalog.util.DescribableImpl;

/**
 * Default implementation of the Describable interface, providing basic setter/getter methods for a
 * describable item's ID, title, version, organization, and description.
 * 
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

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.util.Describable#getVersion()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.util.Describable#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @deprecated
     * @param shortname
     */
    public void setShortname(String shortname) {
        this.id = shortname;
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

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.util.Describable#getTitle()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.util.Describable#getDescription()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.util.Describable#getOrganization()
     */
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
