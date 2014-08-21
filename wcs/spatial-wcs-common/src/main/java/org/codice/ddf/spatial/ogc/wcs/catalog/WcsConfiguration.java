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
package org.codice.ddf.spatial.ogc.wcs.catalog;

/**
 * Domain object to encapsulate the configuration of an instance of a {@link WcsResourceReader}.
 * 
 * @author rodgersh
 * 
 */
public class WcsConfiguration {

    private String wcsUrl;

    private String id;

    private String username;

    private String password;

    private Integer connectionTimeout;

    private Integer receiveTimeout;

    private boolean disableCnCheck;

    public String getWcsUrl() {
        return wcsUrl;
    }

    public void setWcsUrl(String wcsUrl) {
        this.wcsUrl = wcsUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDisableCnCheck(boolean disableCnCheck) {
        this.disableCnCheck = disableCnCheck;
    }

    public boolean getDisableCnCheck() {
        return disableCnCheck;

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
    }

    public Integer getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Integer timeout) {
        this.receiveTimeout = timeout;
    }

}
