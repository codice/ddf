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
package ddf.security.sts.client.configuration.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ddf.security.sts.client.configuration.STSClientConfiguration;

public class STSClientConfigurationImpl implements STSClientConfiguration {

    String address = null;

    String endpointName = null;

    String serviceName = null;

    String username = null;

    String password = null;

    String signatureUsername = null;

    String signatureProperties = null;

    String encryptionUsername = null;

    String encryptionProperties = null;

    String tokenUsername = null;

    String tokenProperties = null;

    List<String> claims = new ArrayList<String>();

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getEndpointName() {
        return endpointName;
    }

    @Override
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getSignatureUsername() {
        return signatureUsername;
    }

    @Override
    public void setSignatureUsername(String signatureUsername) {
        this.signatureUsername = signatureUsername;
    }

    @Override
    public String getSignatureProperties() {
        return signatureProperties;
    }

    @Override
    public void setSignatureProperties(String signatureProperties) {
        this.signatureProperties = signatureProperties;

    }

    @Override
    public String getEncryptionUsername() {
        return encryptionUsername;
    }

    @Override
    public void setEncryptionUsername(String encryptionUsername) {
        this.encryptionUsername = encryptionUsername;

    }

    @Override
    public String getEncryptionProperties() {
        return encryptionProperties;
    }

    @Override
    public void setEncryptionProperties(String encryptionProperties) {
        this.encryptionProperties = encryptionProperties;

    }

    @Override
    public String getTokenUsername() {
        return tokenUsername;
    }

    @Override
    public void setTokenUsername(String tokenUsername) {
        this.tokenUsername = tokenUsername;
    }

    @Override
    public String getTokenProperties() {
        return tokenProperties;
    }

    @Override
    public void setTokenProperties(String tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    @Override
    public List<String> getClaims() {
        return claims;
    }

    @Override
    public void setClaims(List<String> claims) {
        this.claims = new ArrayList<String>(claims);
    }

    @Override
    public void setClaims(String claimsListAsString) {
        List<String> setClaims = new ArrayList<String>();
        if (StringUtils.isNotBlank(claimsListAsString)) {
            for (String claim : claimsListAsString.split(",")) {
                claim = claim.trim();
                setClaims.add(claim);
            }
        }
        this.claims = setClaims;
    }

}
