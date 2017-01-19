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
package org.codice.ddf.admin.api.config.services;

import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LDAPS;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LOGIN;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.TLS;
import static org.codice.ddf.admin.api.config.validation.ValidationUtils.FACTORY_PID_KEY;
import static org.codice.ddf.admin.api.config.validation.ValidationUtils.SERVICE_PID_KEY;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.configurator.ConfiguratorException;
import org.codice.ddf.configuration.PropertyResolver;

public class LdapLoginServiceProperties {

    // --- Ldap Login Service Properties
    public static final String LDAP_LOGIN_MANAGED_SERVICE_FACTORY_PID = "Ldap_Login_Config";
    public static final String LDAP_LOGIN_FEATURE = "security-sts-ldaplogin";

    public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";
    public static final String LDAP_BIND_USER_PASS = "ldapBindUserPass";
    public static final String BIND_METHOD = "bindMethod";
    public static final String KDC_ADDRESS = "kdcAddress";
    public static final String REALM = "realm";
    public static final String USER_NAME_ATTRIBUTE = "userNameAttribute";
    public static final String USER_BASE_DN = "userBaseDn";
    public static final String GROUP_BASE_DN = "groupBaseDn";
    public static final String LDAP_URL = "ldapUrl";
    public static final String START_TLS = "startTls";
    // ---

    public static final LdapConfiguration ldapLoginServiceToLdapConfiguration(Map<String, Object> props) {
        //The keys below are specific to the Ldap_Login_Config service and mapped to the general LDAP configuration class fields
        //This should eventually be cleaned up and structured data should be sent between the ldap login and claims services rather than map
        LdapConfiguration ldapConfiguration = new LdapConfiguration();
        ldapConfiguration.servicePid(
                props.get(SERVICE_PID_KEY) == null ? null : (String) props.get(SERVICE_PID_KEY));
        ldapConfiguration.factoryPid(props.get(FACTORY_PID_KEY) == null ? null : (String) props.get(FACTORY_PID_KEY));
        ldapConfiguration.bindUserDn((String) props.get(LDAP_BIND_USER_DN));
        ldapConfiguration.bindUserPassword((String) props.get(LDAP_BIND_USER_PASS));
        ldapConfiguration.bindUserMethod((String) props.get(BIND_METHOD));
        ldapConfiguration.bindKdcAddress((String) props.get(KDC_ADDRESS));
        ldapConfiguration.bindRealm((String) props.get(REALM));
        ldapConfiguration.userNameAttribute((String) props.get(USER_NAME_ATTRIBUTE));
        ldapConfiguration.baseUserDn((String) props.get(USER_BASE_DN));
        ldapConfiguration.baseGroupDn((String) props.get(GROUP_BASE_DN));
        URI ldapUri = getUriFromProperty((String) props.get(LDAP_URL));
        ldapConfiguration.encryptionMethod(ldapUri.getScheme());
        ldapConfiguration.hostName(ldapUri.getHost());
        ldapConfiguration.port(ldapUri.getPort());
        if ((Boolean) props.get(START_TLS)) {
            ldapConfiguration.encryptionMethod(TLS);
        }
        ldapConfiguration.ldapUseCase(LOGIN);
        return ldapConfiguration;
    }

    public static final Map<String, Object> ldapConfigurationToLdapLoginService(LdapConfiguration config) {
        Map<String, Object> ldapStsConfig = new HashMap<>();
        String ldapUrl = getLdapUrl(config);
        boolean startTls = isStartTls(config);

        ldapStsConfig.put(LDAP_URL, ldapUrl + config.hostName() + ":" + config.port());
        ldapStsConfig.put(START_TLS, Boolean.toString(startTls));
        ldapStsConfig.put(LDAP_BIND_USER_DN, config.bindUserDn());
        ldapStsConfig.put(LDAP_BIND_USER_PASS, config.bindUserPassword());
        ldapStsConfig.put(BIND_METHOD, config.bindUserMethod());
        ldapStsConfig.put(KDC_ADDRESS, config.bindKdcAddress());
        ldapStsConfig.put(REALM, config.bindRealm());

        ldapStsConfig.put(USER_NAME_ATTRIBUTE, config.userNameAttribute());
        ldapStsConfig.put(USER_BASE_DN, config.baseUserDn());
        ldapStsConfig.put(GROUP_BASE_DN, config.baseGroupDn());
        return ldapStsConfig;
    }

    public static boolean isStartTls(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(TLS);
    }

    public static String getLdapUrl(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(LDAPS) ? "ldaps://" : "ldap://";
    }


    public static final URI getUriFromProperty(String ldapUrl) {
        try {
            ldapUrl = PropertyResolver.resolveProperties(ldapUrl);
            if (!ldapUrl.matches("\\w*://.*")) {
                ldapUrl = "ldap://" + ldapUrl;
            }
        } catch (ConfiguratorException e) {
            return null;
        }
        return URI.create(ldapUrl);
    }
}
