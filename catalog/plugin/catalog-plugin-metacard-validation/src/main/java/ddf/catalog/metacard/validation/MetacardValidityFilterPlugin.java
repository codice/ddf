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
package ddf.catalog.metacard.validation;

import static ddf.catalog.data.impl.BasicTypes.VALIDATION_ERRORS;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;

public class MetacardValidityFilterPlugin implements PolicyPlugin {

    private static Map<String, List<String>> attributeMap = new HashMap<>();

    public Map<String, List<String>> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(List<String> attributeMappings) {
        if (CollectionUtils.isEmpty(attributeMappings) || (attributeMappings.size() == 1
                && attributeMappings.get(0)
                .isEmpty())) {
            attributeMap = new HashMap<>();
            return;
        }
        for (String attributeMapping : attributeMappings) {
            String[] keyValue = attributeMapping.split("=");
            attributeMap.put(keyValue[0].trim(),
                    Arrays.asList(keyValue[1].split(","))
                            .stream()
                            .map(String::trim)
                            .collect(Collectors.toList()));
        }
    }

    public void setAttributeMap(Map<String, List<String>> attributeMap) {
        MetacardValidityFilterPlugin.attributeMap = attributeMap;
    }

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {

        if (input == null || input.getMetacard() == null) {
            return new PolicyResponseImpl();
        }
        Metacard metacard = input.getMetacard();
        HashMap<String, Set<String>> securityMap = new HashMap<>();

        if ((metacard.getAttribute(VALIDATION_ERRORS) != null && metacard.getAttribute(
                VALIDATION_ERRORS)
                .getValues() != null) || (metacard.getAttribute(VALIDATION_WARNINGS) != null &&
                metacard.getAttribute(VALIDATION_WARNINGS)
                        .getValues() != null)) {
            for (Map.Entry<String, List<String>> attributeMapping : attributeMap.entrySet()) {
                securityMap.put(attributeMapping.getKey(),
                        new HashSet<>(attributeMapping.getValue()));
            }

        }

        return new PolicyResponseImpl(new HashMap<>(), securityMap);
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }
}
