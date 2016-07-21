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
package ddf.catalog.metacard.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.AttributeStatement;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.DefaultMetacardAttributePlugin;
import ddf.security.assertion.SecurityAssertion;

public class DefaultSecurityAttributeValuesPlugin implements DefaultMetacardAttributePlugin {

    private static Multimap<String, String> sysHighToMetacardAttributeMapping =
            HashMultimap.create();

    ddf.security.Subject getSystemSubject() {
        return org.codice.ddf.security.common.Security.getInstance()
                .getSystemSubject();
    }

    /**
     * Retrieves the system high attributes and the mapping of the system attribute names to
     * metacard security markings. Returns a hash map of the metacard markings
     * to the value of its corresponding system attribute.
     *
     * @return Map of the metacard security markings to the value of their corresponding system high
     * attribute.
     */
    private Map<String, Attribute> getHighwaterSecurityMarkings() {
        Map<String, Attribute> securityMarkings = new HashMap<>();
        ddf.security.Subject system =
                org.codice.ddf.security.common.Security.runAsAdmin(() -> getSystemSubject());
        SecurityAssertion assertion = system.getPrincipals()
                .oneByType(SecurityAssertion.class);
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        for (AttributeStatement curStatement : attributeStatements) {
            for (org.opensaml.saml.saml2.core.Attribute attribute : curStatement.getAttributes()) {
                Collection<String> attributeNames =
                        sysHighToMetacardAttributeMapping.get(attribute.getName());
                if (!attributeNames.isEmpty()) {
                    for (String attributeName : attributeNames) {
                        HashSet<Serializable> values = attribute.getAttributeValues()
                                .stream()
                                .filter(curValue -> curValue instanceof XSString)
                                .map(XSString.class::cast)
                                .map(XSString::getValue)
                                .collect(Collectors.toCollection(HashSet::new));
                        if (securityMarkings.containsKey(attributeName)) {
                            values.addAll(securityMarkings.get(attributeName)
                                    .getValues());
                        }
                        securityMarkings.put(attributeName,
                                new AttributeImpl(attributeName,
                                        (List<Serializable>) ImmutableList.<Serializable>copyOf(
                                                values)));
                    }
                }
            }
        }
        return securityMarkings;
    }

    @Override
    public Metacard addDefaults(Metacard metacard) {
        boolean hasSecurityAttr = sysHighToMetacardAttributeMapping.values()
                .stream()
                .map(metacard::getAttribute)
                .anyMatch(Objects::nonNull);

        if (!hasSecurityAttr) {
            Map<String, Attribute> securityMarkings = getHighwaterSecurityMarkings();
            securityMarkings.keySet()
                    .stream()
                    .filter(securityMarking -> metacard.getMetacardType()
                            .getAttributeDescriptor(securityMarking) != null)
                    .forEach(securityMarking -> {
                        metacard.setAttribute(securityMarkings.get(securityMarking));
                    });
        }
        return metacard;
    }

    public void setMetacardMarkingMappings(List<String> metacardMarkingMappings) {
        Multimap<String, String> attributeMapping = HashMultimap.create();
        Splitter splitter = Splitter.on("=")
                .trimResults();
        metacardMarkingMappings.forEach(mapping -> {
            List<String> map = splitter.splitToList(mapping);
            if (map.size() == 2) {
                attributeMapping.put(map.get(0), map.get(1));
            }
        });
        sysHighToMetacardAttributeMapping = attributeMapping;
    }
}
