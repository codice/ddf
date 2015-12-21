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

import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_ERRORS;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;

public class MetacardValidityFilterPlugin implements PostQueryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardValidityFilterPlugin.class);

    private static Map<String, List<String>> attributeMap = new HashMap<>();

    @Override
    public QueryResponse process(QueryResponse input)
            throws PluginExecutionException, StopProcessingException {
        List<Result> results = input.getResults();
        if (results == null) {
            return input;
        }

        // process security
        for (Result result : results) {
            Metacard metacard = result.getMetacard();
            if (metacard == null) {
                continue;
            }
            Attribute securityAttribute = metacard.getAttribute(Metacard.SECURITY);
            HashMap<String, List<String>> securityMap = new HashMap<>();
            if (securityAttribute != null && securityAttribute.getValue() != null
                    && securityAttribute.getValue() instanceof Map) {
                try {
                    securityMap = (HashMap<String, List<String>>) securityAttribute.getValue();
                } catch (ClassCastException e) {
                    LOGGER.debug("Metacard Security attribute not an instance of HashMap, changing it to an empty HashMap", e);
                }
            }
            if ((metacard.getAttribute(VALIDATION_ERRORS) != null
                    && metacard.getAttribute(VALIDATION_ERRORS).getValues() != null) || (
                    metacard.getAttribute(VALIDATION_WARNINGS) != null
                            && metacard.getAttribute(VALIDATION_WARNINGS).getValues() != null)) {
                for (Map.Entry<String, List<String>> attributeMapping : attributeMap.entrySet()) {
                    securityMap.put(attributeMapping.getKey(), attributeMapping.getValue());
                }

            }
            if (!securityMap.isEmpty()) {
                metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, securityMap));
            }

        }
        return input;
    }

    public Map<String, List<String>> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(List<String> attributeMappings) {
        for (String attributeMapping : attributeMappings) {
            String[] keyValue = attributeMapping.split("=");
            attributeMap.put(keyValue[0].trim(),
                    Arrays.asList(keyValue[1].split(",")).stream().map(String::trim)
                            .collect(Collectors.toList()));
        }
    }

    public void setAttributeMap(Map<String, List<String>> attributeMap) {
        MetacardValidityFilterPlugin.attributeMap = attributeMap;
    }
}
