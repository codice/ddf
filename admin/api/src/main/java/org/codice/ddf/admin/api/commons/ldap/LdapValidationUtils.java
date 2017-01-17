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
package org.codice.ddf.admin.api.commons.ldap;

import static org.codice.ddf.admin.api.commons.ValidationUtils.validateNonEmptyString;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_METHODS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_ENCRYPTION_METHODS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_USE_CASES;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;

import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class LdapValidationUtils {


    public static final List<ConfigurationMessage> validateEncryptionMethod(String encryptionMethod, String configId) {
        List<ConfigurationMessage> errors = validateNonEmptyString(encryptionMethod, configId);
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
        return validateNonEmptyString(dn, configId);
    }

    public static final List<ConfigurationMessage> validateBindUserMethod(String bindMethod, String configId) {
        List<ConfigurationMessage> errors = validateNonEmptyString(bindMethod, configId);
        if(errors.isEmpty() && !BIND_METHODS.contains(bindMethod)) {
            errors.add(createInvalidFieldMsg("Unknown bind method: " + bindMethod + ". Bind method must be one of: " + BIND_METHODS.stream().collect(Collectors.joining(",")), configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateBindKdcAddress(String bindKdcAddress, String configId) {
        // TODO: tbatie - 1/16/17 - Need to do additional validation
        return validateNonEmptyString(bindKdcAddress, configId);
    }

    public static final List<ConfigurationMessage> validateBindRealm(String bindRealm, String configId) {
        // TODO: tbatie - 1/16/17 - Is there more validation we can do?
        return validateNonEmptyString(bindRealm, configId);
    }

    public static final List<ConfigurationMessage> validateQuery(String query, String configId) {
        // TODO: tbatie - 1/16/17 - validate query
        return validateNonEmptyString(query, configId);
    }

    public static final List<ConfigurationMessage> validateLdapType(String ldapType, String configId) {
        // TODO: tbatie - 1/16/17 - not sure if there is any additional validation we should do here
        return validateNonEmptyString(ldapType, configId);
    }

    public static final List<ConfigurationMessage> validateLdapUseCase(String ldapUseCase, String configId) {
        List<ConfigurationMessage> errors = validateNonEmptyString(ldapUseCase, configId);
        if(errors.isEmpty() && !LDAP_USE_CASES.contains(ldapUseCase)) {
            errors.add(createInvalidFieldMsg("Unknown LDAP use case: " + ldapUseCase + ". LDAP use case must be one of: " + LDAP_USE_CASES.stream().collect(Collectors.joining(",")), configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateGroupObjectClass(String objectClass, String configId) {
        // TODO: tbatie - 1/16/17 - not sure if there is any additional validation we should do here
        return validateNonEmptyString(objectClass, configId);
    }


}
