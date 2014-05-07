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
package ddf.catalog.pubsub;

import java.util.Set;

import javax.security.auth.Subject;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.federation.Federatable;

public class MockFilter implements Filter, Federatable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockFilter.class);

    /** The is enterprise. */
    private boolean isEnterprise = false;

    /** The site names. */
    private Set<String> siteNames = null;

    private Subject user;

    protected MockFilter(Subject user) throws IllegalArgumentException {
        LOGGER.debug("Creating a MockFilter");

        this.user = user;
    }

    // @Override
    public Subject getUser() {
        return user;
    }

    // @Override
    public Set<String> getSiteIds() {
        return siteNames;
    }

    public void setSiteIds(Set<String> siteNames) {
        this.siteNames = siteNames;
    }

    // @Override
    public boolean isEnterprise() {
        return isEnterprise;
    }

    public void setIsEnterprise(boolean isEnterprise) {
        this.isEnterprise = isEnterprise;
    }

    @Override
    public Object accept(FilterVisitor arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean evaluate(Object arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<String> getSourceIds() {
        // TODO Auto-generated method stub
        return null;
    }

}