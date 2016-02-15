/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.security.sts.claims.property;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeFileClaimsHandler implements ClaimsHandler, RealmSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFileClaimsHandler.class);

    private static final ObjectMapper MAPPER = JsonFactory.create();

    private String attributeFileLocation;

    private List<URI> supportedClaimTypes = new ArrayList<>();

    private Map<String, Object> json;

    private List<String> supportedRealms;

    private String realm;

    public void init() {
        if (attributeFileLocation != null) {
            Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
            Path path = Paths.get(attributeFileLocation);
            if (!path.isAbsolute()) {
                path = Paths.get(ddfHomePath.toString(), path.toString());
            }
            Set<String> claims = new HashSet<>();
            try (InputStream stream = Files.newInputStream(path)) {
                String jsonString = IOUtils.toString(stream);
                json = MAPPER.parser().parseMap(jsonString);
                Set<Map.Entry<String, Object>> entries = json.entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    Object value = entry.getValue();
                    if (value instanceof Map) {
                        Set keySet = ((Map) value).keySet();
                        for (Object key : keySet) {
                            claims.add((String) key);
                        }
                    }
                }
                supportedClaimTypes.clear();
                supportedClaimTypes.addAll(claims.stream()
                        .map(URI::create)
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                LOGGER.error("Unable to read attribute file for system users.", e);
            }
        }
    }

    @Override
    public List<URI> getSupportedClaimTypes() {
        return supportedClaimTypes;
    }

    @Override
    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claimCollection,
            ClaimsParameters claimsParameters) {
        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        Principal principal = claimsParameters.getPrincipal();
        Object user = json.get(principal.getName());
        Map userMap = null;
        if (user != null) {
            if (user instanceof Map) {
                userMap = (Map) user;
            }
        } else {
            Set<Map.Entry<String, Object>> entries = json.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                String key = entry.getKey();
                Pattern pattern = Pattern.compile(key);
                Matcher matcher = pattern.matcher(principal.getName());
                if (matcher.matches()) {
                    userMap = (Map) entry.getValue();
                    break;
                }
            }
        }
        if (userMap == null) {
            return claimsColl;
        }
        for (Claim claim : claimCollection) {
            Object attributeValue = userMap.get(claim.getClaimType()
                    .toString());
            ProcessedClaim c = new ProcessedClaim();
            c.setClaimType(claim.getClaimType());
            c.setPrincipal(principal);
            if (attributeValue instanceof List) {
                ((List) attributeValue).forEach(c::addValue);
                claimsColl.add(c);
            } else if (attributeValue instanceof String) {
                c.addValue(attributeValue);
                claimsColl.add(c);
            }
        }
        return claimsColl;
    }

    @Override
    public List<String> getSupportedRealms() {
        return supportedRealms;
    }

    @Override
    public String getHandlerRealm() {
        return realm;
    }

    public void setAttributeFileLocation(String attributeFileLocation) {
        this.attributeFileLocation = attributeFileLocation;
    }
}
