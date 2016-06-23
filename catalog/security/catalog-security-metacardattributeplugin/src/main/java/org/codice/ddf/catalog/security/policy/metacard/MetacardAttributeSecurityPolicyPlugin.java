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
package org.codice.ddf.catalog.security.policy.metacard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;

/**
 * Plugin that parses Metacard attributes for security policy information
 */
public class MetacardAttributeSecurityPolicyPlugin implements PolicyPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MetacardAttributeSecurityPolicyPlugin.class);

    private List<String> metacardAttributes = new ArrayList<>();

    private Map<String, String> mcAttribsLookup = new HashMap<>();

    public synchronized List<String> getMetacardAttributes() {
        return metacardAttributes;
    }

    public synchronized void setMetacardAttributes(List<String> metacardAttributes) {
        List<String> badAttributes = metacardAttributes.stream()
                .filter(s -> !s.contains("="))
                .collect(Collectors.toList());

        if (!badAttributes.isEmpty()) {
            LOGGER.debug(
                    "These metacard attributes not formatted as key=val and will be added as val=val: [{}]",
                    badAttributes);
        }

        mcAttribsLookup = metacardAttributes.stream()
                .map(s -> s.split("="))
                .collect(Collectors.toMap(sArr -> sArr[0],
                        sArr -> sArr.length == 1 ? sArr[0] : sArr[1]));
        this.metacardAttributes = metacardAttributes;
    }

    private synchronized Map<String, Set<String>> buildSecurityMap(Metacard metacard) {
        Map<String, Set<String>> securityMap = new HashMap<>();
        if (metacard != null) {
            for (Map.Entry<String, String> row : mcAttribsLookup.entrySet()) {
                Attribute attribute = metacard.getAttribute(row.getKey());
                if (attribute != null) {
                    securityMap.put(row.getValue(),
                            new HashSet<>(listAsStrings(attribute.getValues())));
                }
            }
        }
        return securityMap;
    }

    private List<String> listAsStrings(List list) {
        return list;
    }

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl(null, buildSecurityMap(input));
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard newMetacard,
            Map<String, Serializable> properties) throws StopProcessingException {
        return new PolicyResponseImpl(null, buildSecurityMap(newMetacard));
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        Map<String, Set<String>> response = new HashMap<>();
        for (Metacard metacard : metacards) {
            Map<String, Set<String>> parseSecurityMetadata = buildSecurityMap(metacard);
            for (Map.Entry<String, Set<String>> entry : parseSecurityMetadata.entrySet()) {
                if (response.containsKey(entry.getKey())) {
                    response.get(entry.getKey())
                            .addAll(entry.getValue());
                } else {
                    response.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return new PolicyResponseImpl(response, null);
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl(null, buildSecurityMap(input));
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl(null, buildSecurityMap(input.getMetacard()));
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl(null, buildSecurityMap(metacard));
    }
}
