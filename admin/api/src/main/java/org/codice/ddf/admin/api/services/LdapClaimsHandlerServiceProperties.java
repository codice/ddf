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
package org.codice.ddf.admin.api.services;

import static org.codice.ddf.admin.api.services.LdapLoginServiceProperties.getLdapUrl;
import static org.codice.ddf.admin.api.services.LdapLoginServiceProperties.getUriFromProperty;
import static org.codice.ddf.admin.api.services.LdapLoginServiceProperties.isStartTls;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.TLS;
import static org.codice.ddf.admin.api.validation.ValidationUtils.SERVICE_PID_KEY;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;

public class LdapClaimsHandlerServiceProperties {

    // --- Ldap Claims Handler Service Properties
    public static final String LDAP_CLAIMS_HANDLER_MANAGED_SERVICE_FACTORY_PID = "Claims_Handler_Manager";
    public static final String LDAP_CLAIMS_HANDLER_FEATURE = "security-sts-ldapclaimshandler";

    public static final String URL = "url";
    public static final String START_TLS = "startTls";
    public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";
    public static final String PASSWORD = "password";
    public static final String BIND_METHOD = "bindMethod";
    public static final String LOGIN_USER_ATTRIBUTE = "loginUserAttribute";
    public static final String USER_BASE_DN = "userBaseDn";
    public static final String GROUP_BASE_DN = "groupBaseDn";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String MEMBERSHIP_USER_ATTRIBUTE = "membershipUserAttribute";
    public static final String MEMBER_NAME_ATTRIBUTE = "memberNameAttribute";
    public static final String PROPERTY_FILE_LOCATION = "propertyFileLocation";
    // ---

    public static final LdapConfiguration ldapClaimsHandlerServiceToLdapConfig(Map<String, Object> props) {
        LdapConfiguration config = new LdapConfiguration();
        config.servicePid(props.get(SERVICE_PID_KEY) == null ? null : (String) props.get(SERVICE_PID_KEY));
        URI ldapUri = getUriFromProperty((String) props.get(URL));
        config.encryptionMethod(ldapUri.getScheme());
        config.hostName(ldapUri.getHost());
        config.port(ldapUri.getPort());
        if ((Boolean) props.get(START_TLS)) {
            config.encryptionMethod(TLS);
        }
        config.bindUserDn((String) props.get(LDAP_BIND_USER_DN));
        config.bindUserPassword((String) props.get(PASSWORD));
        config.bindUserMethod((String) props.get(BIND_METHOD));
        config.userNameAttribute((String) props.get(LOGIN_USER_ATTRIBUTE));
        config.baseUserDn((String) props.get(USER_BASE_DN));
        config.baseGroupDn((String) props.get(GROUP_BASE_DN));
        config.groupObjectClass((String) props.get(OBJECT_CLASS));
        config.membershipAttribute((String) props.get(MEMBERSHIP_USER_ATTRIBUTE));
        config.memberNameAttribute((String) props.get(MEMBER_NAME_ATTRIBUTE));

        String attributeMappingsPath = (String) props.get(PROPERTY_FILE_LOCATION);
        config.attributeMappingsPath(attributeMappingsPath);
        Map<String, String> attributeMappings =
                new HashMap(new Configurator().getProperties(Paths.get(attributeMappingsPath)));
        config.attributeMappings(attributeMappings);
        config.ldapUseCase(CREDENTIAL_STORE);
        return config;
    }

    public static final Map<String, Object> ldapConfigToLdapClaimsHandlerService(LdapConfiguration config) {
        Map<String, Object> props = new HashMap();
        String ldapUrl = getLdapUrl(config);
        boolean startTls = isStartTls(config);
        props.put(URL, ldapUrl + config.hostName() + ":" + config.port());
        props.put(START_TLS, startTls);
        props.put(LDAP_BIND_USER_DN, config.bindUserDn());
        props.put(PASSWORD, config.bindUserPassword());
        props.put(BIND_METHOD, config.bindUserMethod());
        props.put(LOGIN_USER_ATTRIBUTE, config.userNameAttribute());
        props.put(USER_BASE_DN, config.baseUserDn());
        props.put(GROUP_BASE_DN, config.baseGroupDn());
        props.put(OBJECT_CLASS, config.groupObjectClass());
        props.put(MEMBERSHIP_USER_ATTRIBUTE, config.membershipAttribute());

        // TODO: tbatie - 1/15/17 - memberNameAttribute is not implemented in UI
        props.put(MEMBER_NAME_ATTRIBUTE, config.membershipAttribute());
        props.put(PROPERTY_FILE_LOCATION, config.attributeMappingsPath());
        return props;
    }
}
