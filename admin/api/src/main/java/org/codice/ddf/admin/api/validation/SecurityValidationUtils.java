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
package org.codice.ddf.admin.api.validation;

import static org.codice.ddf.admin.api.config.context.ContextPolicyBin.AUTH_TYPES;
import static org.codice.ddf.admin.api.config.context.ContextPolicyBin.CONTEXT_PATHS;
import static org.codice.ddf.admin.api.config.context.ContextPolicyBin.REALM;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.context.ContextPolicyBin;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableList;

public class SecurityValidationUtils {

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

    public static final List<ConfigurationMessage> validateContextPolicyBins(List<ContextPolicyBin> bins, String configId){
        List<ConfigurationMessage> errors = new ArrayList<>();
        if(bins == null || bins.isEmpty()) {
            errors.add(createMissingRequiredFieldMsg(configId));
        } else {
            errors.addAll(bins.stream()
                    .map(cpb -> cpb.validate(Arrays.asList(REALM, CONTEXT_PATHS, AUTH_TYPES)))
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
            // TODO: tbatie - 1/16/17 - Check if the req attri fields has values, if so validate
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateRealm(String realm, String configId) {
        List<ConfigurationMessage> errors = ValidationUtils.validateString(realm, configId);
        if (errors.isEmpty() && !ALL_REALMS.contains(realm)) {
            errors.add(createInvalidFieldMsg("Unknown realm: " + realm + ". Realm must be one of " + ALL_REALMS.stream().collect(
                    Collectors.joining(",")), configId));
        }

        return errors;
    }

    public static final List<ConfigurationMessage> validateAuthTypes(List<String> authTypes, String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (authTypes == null || authTypes.isEmpty()) {
            errors.add(createMissingRequiredFieldMsg(configId));
        } else {
            for (String authType : authTypes) {
                if (!ALL_AUTH_TYPES.contains(authType)) {
                    errors.add(createInvalidFieldMsg("Unknown authentication type: " + authType + ". Authentication type must be one of: " + ALL_AUTH_TYPES.stream().collect(Collectors.joining(",")), configId));
                }
            }
        }

        return errors;
    }
}
