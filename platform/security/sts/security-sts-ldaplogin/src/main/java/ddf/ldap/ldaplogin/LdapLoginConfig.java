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
package ddf.ldap.ldaplogin;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.karaf.jaas.config.impl.Module;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers LDAP as a JAAS realm.
 */
public class LdapLoginConfig {

    public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";

    public static final String LDAP_BIND_USER_PASS = "ldapBindUserPass";

    public static final String LDAP_URL = "ldapUrl";

    public static final String USER_BASE_DN = "userBaseDn";

    public static final String GROUP_BASE_DN = "groupBaseDn";

    public static final String KEY_ALIAS = "keyAlias";

    public static final String START_TLS = "startTls";

    private static final String SUFFICIENT_FLAG = "sufficient";

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapLoginConfig.class);

    private static final String LDAP_MODULE = ddf.ldap.ldaplogin.SslLdapLoginModule.class.getName();

    private String id = "LDAP:" + UUID.randomUUID()
            .toString();

    private Map<String, Object> ldapProperties = new HashMap<>();

    private LdapService ldapService;

    /**
     * Update method that receives new properties.
     *
     * @param props Map of properties.
     */
    public void update(Map<String, ?> props) {
        if (props != null) {
            LOGGER.debug("Received an updated set of configurations for the LDAP Login Config.");
            // create modules from the newly updated config
            Module ldapModule = createLdapModule(props);
            ldapService.update(ldapModule);
        }
    }

    /**
     * Creates a new module with the given properties.
     *
     * @param properties Map of properties.
     * @return newly created module.
     */
    private Module createLdapModule(Map<String, ?> properties) {
        Module ldapModule = new Module();
        ldapModule.setClassName(LDAP_MODULE);
        ldapModule.setFlags(SUFFICIENT_FLAG);
        ldapModule.setName(id);
        Properties props = new Properties();
        props.put("connection.username", properties.get(LDAP_BIND_USER_DN));
        props.put("connection.password", properties.get(LDAP_BIND_USER_PASS));
        props.put("connection.url",
                properties.get(LDAP_URL)
                        .toString());
        props.put("user.base.dn", properties.get(USER_BASE_DN));
        props.put("user.filter", "(uid=%u)");
        props.put("user.search.subtree", "true");
        props.put("role.base.dn", properties.get(GROUP_BASE_DN));
        props.put("role.filter", "(member=uid=%u," + properties.get(USER_BASE_DN) + ")");
        props.put("role.name.attribute", "cn");
        props.put("role.search.subtree", "true");
        props.put("authentication", "simple");
        props.put("ssl.protocol", "TLS");
        props.put("ssl.truststore", "ts");
        props.put("ssl.keystore", "ks");
        props.put("ssl.keyalias",
                properties.get(KEY_ALIAS)
                        .toString());
        props.put("ssl.algorithm", "SunX509");
        props.put("ssl.starttls", properties.get(START_TLS));
        ldapModule.setOptions(props);

        return ldapModule;
    }

    String getId() {
        return id;
    }

    public void setLdapBindUserDn(String ldapBindUserDn) {
        LOGGER.trace("setLdapBindUserDn called: {}", ldapBindUserDn);
        ldapProperties.put(LDAP_BIND_USER_DN, ldapBindUserDn);
    }

    public void setLdapBindUserPass(String bindUserPass) {
        LOGGER.trace("setLdapBindUserPass called: {}", bindUserPass);
        ldapProperties.put(LDAP_BIND_USER_PASS, bindUserPass);
    }

    public void setLdapUrl(String ldapUrl) {
        LOGGER.trace("setLdapUrl called: {}", ldapUrl);
        ldapProperties.put(LDAP_URL, new PropertyResolver(ldapUrl));
    }

    public void setUserBaseDn(String userBaseDn) {
        LOGGER.trace("setUserBaseDn called: {}", userBaseDn);
        ldapProperties.put(USER_BASE_DN, userBaseDn);
    }

    public void setGroupBaseDn(String groupBaseDn) {
        LOGGER.trace("setGroupBaseDn called: {}", groupBaseDn);
        ldapProperties.put(GROUP_BASE_DN, groupBaseDn);
    }

    public void setKeyAlias(String keyAlias) {
        LOGGER.trace("setKeyAlias called: {}", keyAlias);
        ldapProperties.put(KEY_ALIAS, new PropertyResolver(keyAlias));
    }

    public void setStartTls(boolean startTls) {
        LOGGER.trace("Setting startTls: {}", startTls);
        ldapProperties.put(START_TLS, String.valueOf(startTls));
    }

    public void setStartTls(String startTls) {
        LOGGER.trace("Setting startTls: {}", startTls);
        ldapProperties.put(START_TLS, startTls);
    }

    public void configure() {
        LOGGER.trace("configure called - calling update");
        update(ldapProperties);
    }

    public void destroy(int arg) {
        LOGGER.trace("configure called - calling delete");
        ldapService.delete(id);
    }

    public void setLdapService(LdapService ldapService) {
        this.ldapService = ldapService;
    }
}
