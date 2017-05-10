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
package ddf.catalog.impl.operations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.SecurityUtils;

import ddf.security.SecurityConstants;
import ddf.security.Subject;

/**
 * Support class for dealing with security for the {@code CatalogFrameworkImpl}.
 *
 * Specifically, this class exists to isolate the logic required to build a security policy map.
 */
public class OperationsSecuritySupport {
    void buildPolicyMap(HashMap<String, Set<String>> policyMap,
            Set<Map.Entry<String, Set<String>>> policy) {
        if (policy != null) {
            for (Map.Entry<String, Set<String>> entry : policy) {
                if (policyMap.containsKey(entry.getKey())) {
                    policyMap.get(entry.getKey())
                            .addAll(entry.getValue());
                } else {
                    policyMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }
        }
    }

    /**
     * Creates and returns a map of properties containing a security subject. If the correct subject
     * type can not be obtained from the passed in subject or from the thread context no subject will
     * be added to the properties.
     * If the passed in subject is not compatible the method will fallback to trying to pull it from
     * the thread context.
     * @param subject The subject to be added. Can be {@code null}
     */
    Map<String, Serializable> getPropertiesWithSubject(Object subject) {
        Map<String, Serializable> properties = new HashMap<>();
        if(subject != null && subject instanceof Subject){
            properties.put(SecurityConstants.SECURITY_SUBJECT, (Subject)subject);
        } else {
            try {
                Object subjectFromContxt = SecurityUtils.getSubject();
                if (subjectFromContxt instanceof Subject) {
                    properties.put(SecurityConstants.SECURITY_SUBJECT, (Subject) subjectFromContxt);
                }
            } catch(Exception e){
                //Error thrown if no subject/security manager found for thread context
                //Ignore
            }
        }
        return properties;
    }
}
