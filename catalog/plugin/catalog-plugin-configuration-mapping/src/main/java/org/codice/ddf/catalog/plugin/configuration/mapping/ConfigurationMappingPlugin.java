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
 */
package org.codice.ddf.catalog.plugin.configuration.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.expansion.Expansion;

public class ConfigurationMappingPlugin implements PreIngestPlugin {

    private Expansion expansionService;

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {

        expandAttributes(input.getMetacards());
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {

        List<Metacard> metacards = new ArrayList<>();

        for (Map.Entry<Serializable, Metacard> metacardEntry : input.getUpdates()) {
            metacards.add(metacardEntry.getValue());
        }

        expandAttributes(metacards);
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    private void expandAttributes(List<Metacard> metacards) {

        AttributeRuleSet attributeRuleSet = new AttributeRuleSet(expansionService);

        for (Metacard metacard : metacards) {
            attributeRuleSet.expandMetacard(metacard);
        }
    }

    private class AttributeRuleSet {

        private Map<String, Map<String, String>> attributeRuleSet;

        public AttributeRuleSet(Expansion expansion) {

            attributeRuleSet = new HashMap<>();

            for (Map.Entry<String, List<String[]>> attributeRules : expansion.getExpansionMap()
                    .entrySet()) {
                Map<String, String> attributeMap = toMap(attributeRules.getValue());

                attributeRuleSet.put(attributeRules.getKey(), attributeMap);
            }
        }

        public void expandMetacard(Metacard metacard) {

            Set<String> attributeSet = metacard.getMetacardType()
                    .getAttributeDescriptors()
                    .stream()
                    .map(des -> des.getName())
                    .collect(Collectors.toSet());

            Set<String> commonAttributes = new HashSet<>(CollectionUtils.intersection(attributeSet,
                    attributeRuleSet.keySet()));

            for (String attributeName : commonAttributes) {
                Attribute metacardAttribute = metacard.getAttribute(attributeName);

                if (metacardAttribute != null) {

                    // expandTo holds the value that the ruleset
                    String expandTo = getCorrespondingValue(attributeName,
                            metacardAttribute.getValue());

                    if (expandTo != null) {
                        Attribute attributeImpl = new AttributeImpl(attributeName, expandTo);
                        metacard.setAttribute(attributeImpl);
                    }
                }
            }
        }

        private String getCorrespondingValue(String attributeName, Serializable currentValue) {
            return attributeRuleSet.get(attributeName)
                    .get(currentValue);
        }

        private Map<String, String> toMap(List<String[]> list) {

            Map<String, String> map = new HashMap<>();

            for (String[] pair : list) {
                map.put(pair[0], pair[1]);
            }
            return map;
        }

    }

    public void setExpansionService(Expansion expansionService) {
        this.expansionService = expansionService;
    }
}
