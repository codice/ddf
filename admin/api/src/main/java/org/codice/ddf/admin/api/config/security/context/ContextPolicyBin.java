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

package org.codice.ddf.admin.api.config.security.context;

import static org.codice.ddf.admin.api.config.security.context.ContextPolicyUtils.validateContextPath;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableList;

public class ContextPolicyBin {

    // TODO: tbatie - 1/14/17 - Make sure all fields here match docs
    public static final String KARAF = "karaf";
    public static final String LDAP = "ldap";
    public static final String IDP = "IdP";
    public static final List<String> ALL_REALMS = ImmutableList.of(KARAF, LDAP, IDP);

    public static final String SAML = "SAML";
    public static final String BASIC = "basic";
    public static final String PKI = "PKI";
    public static final String CAS = "CAS";
    public static final String GUEST = "GUEST";
    public static final List<String> ALL_AUTH_TYPES = ImmutableList.of(SAML, BASIC, PKI, CAS, GUEST);

    public static final String REALM = "realm";
    public static final String CONTEXT_PATHS = "contextPaths";
    public static final String AUTH_TYPES = "authenticationTypes";
    public static final String REQ_ATTRIS = "requiredAttributes";

    private String realm;
    private Set<String> contextPaths;
    private List<String> authenticationTypes;
    private Map<String, String> requiredAttributes;

    public ContextPolicyBin() {
        authenticationTypes = new ArrayList<>();
        requiredAttributes = new HashMap<>();
        contextPaths = new HashSet<>();
    }

    public ContextPolicyBin realm(String realm) {
        this.realm = realm;
        return this;
    }

    public Set<String> contextPaths() {
        return contextPaths;
    }

    public List<String> authenticationTypes() {
        return authenticationTypes;
    }

    public Map<String, String> requiredAttributes() {
        return requiredAttributes;
    }

    public String realm() {
        return realm;
    }

    public ContextPolicyBin authenticationTypes(List<String> authenticationTypes) {
        this.authenticationTypes = authenticationTypes;
        return this;
    }

    public ContextPolicyBin requiredAttributes(Map<String, String> requiredAttributes) {
        this.requiredAttributes = requiredAttributes;
        return this;
    }

    public ContextPolicyBin contextPaths(Set<String> contextPaths) {
        this.contextPaths = contextPaths;
        return this;
    }

    public ContextPolicyBin contextPaths(String context) {
        contextPaths.add(context);
        return this;
    }

    public List<ConfigurationMessage> validate() {
        List<ConfigurationMessage> msgs = new ArrayList<>();

        if (realm() == null || StringUtils.isEmpty(realm())) {
            msgs.add(createMissingRequiredFieldMsg(REALM));
        }

        if (contextPaths() == null || contextPaths().isEmpty()) {
            msgs.add(createMissingRequiredFieldMsg(CONTEXT_PATHS));
        } else {
            msgs.addAll(contextPaths().stream()
                    .map(context -> validateContextPath(context))
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }

        if (authenticationTypes() == null || authenticationTypes().isEmpty()) {
            msgs.add(createMissingRequiredFieldMsg(AUTH_TYPES));
        } else {
            for (String authType : authenticationTypes()) {
                if (!ALL_AUTH_TYPES.contains(authType)) {
                    msgs.add(createInvalidFieldMsg("Unknown auth type: " + authType, AUTH_TYPES));
                }
            }
        }

        if (requiredAttributes() != null) {
            if (requiredAttributes().values()
                    .contains(null) || requiredAttributes().values()
                    .contains("")) {
                msgs.add(createMissingRequiredFieldMsg(REQ_ATTRIS));
            }
        }

        return msgs;
    }

    public boolean hasSameRequiredAttributes(Map<String, String> mappingsToCheck) {
        if (!(requiredAttributes.keySet()
                .containsAll(mappingsToCheck.keySet()) && mappingsToCheck.keySet()
                .containsAll(requiredAttributes.keySet()))) {
            return false;
        }

        return !requiredAttributes.entrySet()
                .stream()
                .filter(binMapping -> !mappingsToCheck.get(binMapping.getKey())
                        .equals(binMapping.getValue()))
                .findFirst()
                .isPresent();
    }
}
