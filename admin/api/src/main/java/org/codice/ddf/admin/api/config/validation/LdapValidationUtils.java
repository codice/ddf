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
package org.codice.ddf.admin.api.config.validation;

import static org.codice.ddf.admin.api.config.validation.ValidationUtils.validateString;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;

import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableList;

public class LdapValidationUtils {

    public static final String LDAPS = "ldaps";
    public static final String TLS = "tls";
    public static final String NONE = "none";
    public static final ImmutableList<String> LDAP_ENCRYPTION_METHODS = ImmutableList.of(LDAPS, TLS, NONE);

    public static final String SIMPLE = "Simple";
    public static final String SASL = "SASL";
    public static final String GSSAPI_SASL = "GSSAPI SASL";
    public static final String DIGEST_MD5_SASL = "Digest MD5 SASL";
    public static final List<String> BIND_METHODS = ImmutableList.of(SIMPLE, SASL, GSSAPI_SASL, DIGEST_MD5_SASL);

    // TODO: tbatie - 1/17/17 - Rename these constants to authenticationAndAttributeStore
    public static final String LOGIN = "login";
    public static final String CREDENTIAL_STORE = "credentialStore";
    public static final String LOGIN_AND_CREDENTIAL_STORE = "loginAndCredentialStore";
    public static final ImmutableList LDAP_USE_CASES = ImmutableList.of(LOGIN, CREDENTIAL_STORE, LOGIN_AND_CREDENTIAL_STORE);

    public static final List<ConfigurationMessage> validateEncryptionMethod(String encryptionMethod, String configId) {
        List<ConfigurationMessage> errors = validateString(encryptionMethod, configId);
        if (errors.isEmpty() && !LDAP_ENCRYPTION_METHODS.stream()
                .filter(e -> e.equalsIgnoreCase(encryptionMethod))
                .findFirst()
                .isPresent()) {
            errors.add(createInvalidFieldMsg("Unknown encryption method: " + encryptionMethod + ". Encryption method must be one of: " + LDAP_ENCRYPTION_METHODS.stream().collect(
                    Collectors.joining(",")), configId));
        }

        return errors;
    }

    public static final List<ConfigurationMessage> validateDn(String dn, String configId) {
        // TODO: tbatie - 1/16/17 - Validate the DN format
        return validateString(dn, configId);
    }

    public static final List<ConfigurationMessage> validateBindUserMethod(String bindMethod, String configId) {
        List<ConfigurationMessage> errors = validateString(bindMethod, configId);
        if(errors.isEmpty() && !BIND_METHODS.contains(bindMethod)) {
            errors.add(createInvalidFieldMsg("Unknown bind method: " + bindMethod + ". Bind method must be one of: " + BIND_METHODS.stream().collect(Collectors.joining(",")), configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateBindKdcAddress(String bindKdcAddress, String configId) {
        // TODO: tbatie - 1/16/17 - Need to do additional validation
        return validateString(bindKdcAddress, configId);
    }

    public static final List<ConfigurationMessage> validateBindRealm(String bindRealm, String configId) {
        // TODO: tbatie - 1/16/17 - Is there more validation we can do?
        return validateString(bindRealm, configId);
    }

    public static final List<ConfigurationMessage> validateLdapQuery(String query, String configId) {
        // TODO: tbatie - 1/16/17 - validate query
        return validateString(query, configId);
    }

    public static final List<ConfigurationMessage> validateLdapType(String ldapType, String configId) {
        // TODO: tbatie - 1/16/17 - not sure if there is any additional validation we should do here
        return validateString(ldapType, configId);
    }

    public static final List<ConfigurationMessage> validateLdapUseCase(String ldapUseCase, String configId) {
        List<ConfigurationMessage> errors = validateString(ldapUseCase, configId);
        if(errors.isEmpty() && !LDAP_USE_CASES.contains(ldapUseCase)) {
            errors.add(createInvalidFieldMsg("Unknown LDAP use case: " + ldapUseCase + ". LDAP use case must be one of: " + LDAP_USE_CASES.stream().collect(Collectors.joining(",")), configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateGroupObjectClass(String objectClass, String configId) {
        // TODO: tbatie - 1/16/17 - not sure if there is any additional validation we should do here at the syntax level
        return validateString(objectClass, configId);
    }


}
