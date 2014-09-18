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
package org.codice.ddf.security.claims.anonymous;

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides claims for an anonymous auth token.
 */
public class AnonymousClaimsHandler implements ClaimsHandler, RealmSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousClaimsHandler.class);

    private Map<URI, List<String>> claimsMap = new HashMap<URI, List<String>>();

    private List<String> supportedRealms;

    private String realm;

    public void setAttributes(List<String> attributes) {
        List<String> attrs = new ArrayList<String>(attributes.size());
        for(String attr : attributes) {
            if(attr.contains(",")) {
                String[] tmpAttrs = attr.split(",");
                attrs.addAll(Arrays.asList(tmpAttrs));
            } else {
                attrs.add(attr);
            }
        }
        initClaimsMap(attrs);
    }

    public void setAttributes(String attributes) {
        String[] attrs = attributes.split(",");
        setAttributes(Arrays.asList(attrs));
    }

    public Map<URI, List<String>> getClaimsMap() {
        return Collections.unmodifiableMap(claimsMap);
    }

    private void initClaimsMap(List<String> attributes) {
        for(String attr : attributes) {
            String[] claimMapping = attr.split("=");
            if(claimMapping.length == 2) {
                try {
                    List<String> values = new ArrayList<String>();
                    if(claimMapping[1].contains("|")) {
                        String[] valsArr = claimMapping[1].split("\\|");
                        Collections.addAll(values, valsArr);
                    } else {
                        values.add(claimMapping[1]);
                    }
                    claimsMap.put(new URI(claimMapping[0]), values);
                } catch (URISyntaxException e) {
                    LOGGER.warn("Claims mapping cannot be converted to a URI. This claim will be excluded: {}", attr, e);
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
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
            ClaimsParameters parameters) {
        ClaimCollection claimsColl = new ClaimCollection();
        Principal principal = parameters.getPrincipal();
        for (RequestClaim claim : claims) {
            URI claimType = claim.getClaimType();
            List<String> value = claimsMap.get(claimType);
            if (value != null) {
                Claim c = new Claim();
                c.setClaimType(claimType);
                c.setPrincipal(principal);
                for(String val : value) {
                    c.addValue(val);
                }
                claimsColl.add(c);
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
