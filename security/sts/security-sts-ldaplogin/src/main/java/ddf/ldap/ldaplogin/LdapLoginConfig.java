/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.ldap.ldaplogin;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Registers LDAP as a JAAS realm.
 *
 */
public class LdapLoginConfig {

    public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";
    public static final String LDAP_BIND_USER_PASS = "ldapBindUserPass";
    public static final String LDAP_URL = "ldapUrl";
    public static final String USER_BASE_DN = "userBaseDn";
    public static final String GROUP_BASE_DN = "groupBaseDn";
    public static final String KEY_ALIAS = "keyAlias";

    private static String LDAP_MODULE = ddf.ldap.ldaplogin.SslLdapLoginModule.class.getName();

    private static String PROPS_MODULE = org.apache.karaf.jaas.modules.properties.PropertiesLoginModule.class.getName();

    private static final String CONFIG_NAME = "ldap";

    private static final String SUFFICIENT_FLAG = "sufficient";

    private ServiceRegistration<JaasRealm> registration = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapLoginConfig.class);

    private Map<String, String> ldapProperties = new HashMap<String, String>();

    /**
     * Registers the passed-in modules under a new JaasRealm.
     *
     * @param modules
     *            Modules to add to the JaasRealm.
     */
    private void registerConfig(Module[] modules) {
        if (registration != null) {
            LOGGER.debug("Found previous registration, unregistering old service.");
            try {
                registration.unregister();
            } catch (IllegalStateException ise) {
                LOGGER.debug("Service was already unregistered, continuing on to register new service.");
            }
        }
        Config config = new Config();
        BundleContext context = getContext();
        config.setBundleContext(context);
        config.setName(CONFIG_NAME);
        config.setRank(2);
        config.setModules(modules);
        LOGGER.debug("Registering new service as a JaasRealm.");
        if (context != null) {
            registration = context.registerService(JaasRealm.class, config, null);
        }
    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(LdapLoginConfig.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }

    /**
     * Update method that receives new properties.
     *
     * @param props
     *            Map of properties.
     */
    public void update(Map<String, ?> props) {
        LOGGER.debug("Received an updated set of configurations for the LDAP Login Config.");
        // create modules from the newly updated config
        Module ldapModule = createLdapModule(props);
        Module propsModule = createPropertiesModule();
        registerConfig(new Module[] {propsModule, ldapModule});
    }

    /**
     * Creates a new module with the given properties.
     *
     * @param properties
     *            Map of properties.
     * @return newly created module.
     */
    private Module createLdapModule(Map<String, ?> properties) {
        Module ldapModule = new Module();
        ldapModule.setClassName(LDAP_MODULE);
        ldapModule.setFlags(SUFFICIENT_FLAG);
        ldapModule.setName("ldapModule");
        Properties props = new Properties();
        props.put("initial.context.factory", "com.sun.jndi.ldap.LdapCtxFactory");
        props.put("connection.username", properties.get(LDAP_BIND_USER_DN));
        props.put("connection.password", properties.get(LDAP_BIND_USER_PASS));
        props.put("connection.url", properties.get(LDAP_URL));
        props.put("user.base.dn", properties.get(USER_BASE_DN));
        props.put("user.filter", "(uid=%u)");
        props.put("user.search.subtree", "true");
        props.put("role.base.dn", properties.get(GROUP_BASE_DN));
        props.put("role.filter", "(member=uid=%u," + properties.get(USER_BASE_DN) + ")");
        props.put("role.name.attribute", "cn");
        props.put("role.search.subtree", "true");
        props.put("authentication", "simple");
        props.put("ssl.protocol", "SSL");
        props.put("ssl.truststore", "ts");
        props.put("ssl.keystore", "ks");
        props.put("ssl.keyalias", properties.get(KEY_ALIAS));
        props.put("ssl.algorithm", "SunX509");
        ldapModule.setOptions(props);

        return ldapModule;
    }

    private Module createPropertiesModule() {
        Module propsModule = new Module();
        propsModule.setClassName(PROPS_MODULE);
        propsModule.setFlags(SUFFICIENT_FLAG);
        propsModule.setName("propsModule");
        Properties props = new Properties();
        props.put("users", System.getProperty("ddf.home") + "/etc/users.properties");
        propsModule.setOptions(props);
        return propsModule;
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
        ldapProperties.put(LDAP_URL, ldapUrl);
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
        ldapProperties.put(KEY_ALIAS, keyAlias);
    }

    public void configure() {
        LOGGER.trace("configure called - calling update");
        update(ldapProperties);
    }
}
