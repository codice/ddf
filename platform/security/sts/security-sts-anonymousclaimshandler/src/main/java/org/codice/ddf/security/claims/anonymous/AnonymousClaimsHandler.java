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
package org.codice.ddf.security.claims.anonymous;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.principal.AnonymousPrincipal;

/**
 * Provides claims for an anonymous auth token.
 */
public class AnonymousClaimsHandler implements ClaimsHandler, RealmSupport {

    public static final String IP_ADDRESS_CLAIMS_KEY = "IpAddress";

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousClaimsHandler.class);

    private Map<URI, List<String>> claimsMap = new HashMap<URI, List<String>>();

    private List<String> supportedRealms;

    private String realm;

    public AnonymousClaimsHandler() {
        LOGGER.debug("Starting AnonymousClaimsHandler");
    }

    public void setAttributes(List<String> attributes) {
        if (attributes != null) {
            LOGGER.debug("Attribute value list was set.");
            List<String> attrs = new ArrayList<>(attributes.size());
            for (String attr : attributes) {
                attrs.add(attr);
            }
            initClaimsMap(attrs);
        } else {
            LOGGER.warn("Set attribute value list was null");
        }
    }

    public Map<URI, List<String>> getClaimsMap() {
        return Collections.unmodifiableMap(claimsMap);
    }

    private void initClaimsMap(List<String> attributes) {
        for (String attr : attributes) {
            String[] claimMapping = attr.split("=");
            if (claimMapping.length == 2) {
                try {
                    List<String> values = new ArrayList<String>();
                    if (claimMapping[1].contains("|")) {
                        String[] valsArr = claimMapping[1].split("\\|");
                        Collections.addAll(values, valsArr);
                    } else {
                        values.add(claimMapping[1]);
                    }
                    claimsMap.put(new URI(claimMapping[0]), values);
                } catch (URISyntaxException e) {
                    LOGGER.warn(
                            "Claims mapping cannot be converted to a URI. This claim will be excluded: {}",
                            attr, e);
                }
            } else {
                LOGGER.warn("Invalid claims mapping entered for anonymous user: {}", attr);
            }
        }
    }

    @Override
    public List<URI> getSupportedClaimTypes() {
        List<URI> uriList = new ArrayList<URI>();
        for (URI uri : claimsMap.keySet()) {
            uriList.add(uri);
        }

        return uriList;
    }

    @Override
    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claims,
            ClaimsParameters parameters) {
        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        Principal principal = parameters.getPrincipal();
        for (Claim claim : claims) {
            URI claimType = claim.getClaimType();
            List<String> value = claimsMap.get(claimType);
            if (value != null) {
                ProcessedClaim c = new ProcessedClaim();
                c.setClaimType(claimType);
                c.setPrincipal(principal);
                for (String val : value) {
                    c.addValue(val);
                }
                claimsColl.add(c);
            }
        }

        if (principal != null && principal instanceof AnonymousPrincipal) {
            String ipAddress = ((AnonymousPrincipal)principal).getAddress();
            if (ipAddress != null) {
                try {
                    ProcessedClaim ipClaim = new ProcessedClaim();
                    ipClaim.setClaimType(new URI(IP_ADDRESS_CLAIMS_KEY));
                    ipClaim.setPrincipal(principal);
                    ipClaim.addValue(ipAddress);
                    claimsColl.add(ipClaim);
                } catch (URISyntaxException e) {
                    LOGGER.warn(
                            "Claims mapping cannot be converted to a URI. Ip claim will be excluded",
                            e);
                }
            }
        }
        return claimsColl;
    }

    @Override
    public List<String> getSupportedRealms() {
        return supportedRealms;
    }

    public void setSupportedRealms(List<String> supportedRealms) {
        this.supportedRealms = supportedRealms;
    }

    @Override
    public String getHandlerRealm() {
        return realm;
    }

    public void setHandlerRealm(String realm) {
        this.realm = realm;
    }

}
