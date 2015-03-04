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
package ddf.security.sts.claimsHandler;

import ddf.security.common.util.CommonSSLFactory;
import ddf.security.encryption.EncryptionService;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and registers LDAP and Role claims handlers.
 *
 */
public class ClaimsHandlerManager implements ConfigurationWatcher {

    public static final String URL = "url";
    public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";
    public static final String PASSWORD = "password";
    public static final String USER_NAME_ATTRIBUTE = "userNameAttribute";
    public static final String USER_BASE_DN = "userBaseDn";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String MEMBER_NAME_ATTRIBUTE = "memberNameAttribute";
    public static final String GROUP_BASE_DN = "groupBaseDn";
    public static final String USER_DN = "userDn";
    public static final String PROPERTY_FILE_LOCATION = "propertyFileLocation";

    private EncryptionService encryptService;

    private BundleContext context;

    private ServiceRegistration<ClaimsHandler> roleHandlerRegistration = null;

    private ServiceRegistration<ClaimsHandler> ldapHandlerRegistration = null;

    private String keystoreLoc, keystorePass;

    private String truststoreLoc, truststorePass;

    private LdapConnection connection;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimsHandlerManager.class);

    private Map<String, String> ldapProperties = new HashMap<String, String>();

    private Map<String, String> props;

    /**
     * Creates a new instance of the ClaimsHandlerManager.
     *
     * @param encryptService Encryption service used to decrypt passwords from the configurations.
     * @param context BundleContext that should be used to register services.
     */
    public ClaimsHandlerManager(EncryptionService encryptService, BundleContext context) {
        this.encryptService = encryptService;
        this.context = context;
    }

    /**
     * Callback method that is called when configuration is updated. Also called by the
     * blueprint init-method when all properties have been set.
     *
     * @param props Map of properties.
     */
    public void update(Map<String, String> props) {
        LOGGER.debug("Received an updated set of configurations for the LDAP/Role Claims Handlers.");
        String url = props.get(ClaimsHandlerManager.URL);
        String userDn = props.get(ClaimsHandlerManager.USER_DN);
        String password = props.get(ClaimsHandlerManager.PASSWORD);
        String userBaseDn = props.get(ClaimsHandlerManager.USER_BASE_DN);
        String objectClass = props.get(ClaimsHandlerManager.OBJECT_CLASS);
        String memberNameAttribute = props.get(ClaimsHandlerManager.MEMBER_NAME_ATTRIBUTE);
        String groupBaseDn = props.get(ClaimsHandlerManager.GROUP_BASE_DN);
        String userNameAttribute = props.get(ClaimsHandlerManager.USER_NAME_ATTRIBUTE);
        String propertyFileLocation = props.get(ClaimsHandlerManager.PROPERTY_FILE_LOCATION);
        if((keystoreLoc != null && truststoreLoc != null) || !url.startsWith("ldaps")) {
            try {
                if (encryptService != null) {
                    password = encryptService.decryptValue(password);
                }
                connection = createLdapConnection(url, userDn, password);
                registerRoleClaimsHandler(connection, propertyFileLocation, userBaseDn,
                        userNameAttribute, objectClass, memberNameAttribute, groupBaseDn);
                registerLdapClaimsHandler(connection, propertyFileLocation, userBaseDn,
                        userNameAttribute);

            } catch (Exception e) {
                LOGGER.warn(
                        "Experienced error while configuring claims handlers. Handlers are NOT configured and claim retrieval will not work.",
                        e);
            }
        }
        this.props = props;
    }

    public void destroy() {

    }

    protected LdapConnection createLdapConnection(String url, String userDn, String password)
            throws LdapException {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setUseSsl(url.startsWith("ldaps"));
        config.setLdapHost(url.substring(url.indexOf("://")+3, url.lastIndexOf(":")));
        config.setName(userDn);
        config.setCredentials(password);
        try {
            config.setLdapPort(Integer.valueOf(url.substring(url.lastIndexOf(":") + 1)));
        } catch (NumberFormatException e) {
            LOGGER.warn("No lDAP port specified, default port will be used.", e);
        }
        if(config.isUseSsl()) {
            config.setSslProtocol(CommonSSLFactory.PROTOCOL);
            try {
                config.setKeyManagers(CommonSSLFactory.createKeyManagerFactory(keystoreLoc, keystorePass).getKeyManagers());
                config.setTrustManagers(CommonSSLFactory.createTrustManagerFactory(truststoreLoc, truststorePass).getTrustManagers());
            } catch (IOException e) {
                LOGGER.error("Error encountered while configuring SSL. Secure connection will fail.", e);
            }
        }
        LdapConnection connection = new LdapNetworkConnection(config);

        return connection;
    }

    /**
     * Registers a new Role-based ClaimsHandler.
     *
     * @param connection
     *            LdapTemplate used to query ldap for the roles.
     * @param propertyFileLoc
     *            File location of the property file.
     * @param userBaseDn
     *            Base DN to determine the roles.
     * @param userNameAttr
     *            Identifier that defines the user.
     * @param groupBaseDn
     *            Base DN of the group.
     */
    private void registerRoleClaimsHandler(LdapConnection connection, String propertyFileLoc,
            String userBaseDn, String userNameAttr, String objectClass, String memberNameAttribute, String groupBaseDn) {
        RoleClaimsHandler roleHandler = new RoleClaimsHandler();
        roleHandler.setLdapConnection(connection);
        roleHandler.setPropertyFileLocation(propertyFileLoc);
        roleHandler.setUserBaseDn(userBaseDn);
        roleHandler.setUserNameAttribute(userNameAttr);
        roleHandler.setObjectClass(objectClass);
        roleHandler.setMemberNameAttribute(memberNameAttribute);
        roleHandler.setGroupBaseDn(groupBaseDn);
        LOGGER.debug("Registering new role claims handler.");
        roleHandlerRegistration = registerClaimsHandler(roleHandler, roleHandlerRegistration);
    }

    /**
     * Registers a new Ldap-based Claims Handler.
     *
     * @param connection
     *            LdapTemplate used to query ldap for the roles.
     * @param propertyFileLoc
     *            File location of the property file.
     * @param userBaseDn
     *            Base DN to determine the roles.
     * @param userNameAttr
     *            Identifier that defines the user.
     */
    private void registerLdapClaimsHandler(LdapConnection connection, String propertyFileLoc,
            String userBaseDn, String userNameAttr) {
        LdapClaimsHandler ldapHandler = new LdapClaimsHandler();
        ldapHandler.setLdapConnection(connection);
        ldapHandler.setPropertyFileLocation(propertyFileLoc);
        ldapHandler.setUserBaseDN(userBaseDn);
        ldapHandler.setUserNameAttribute(userNameAttr);
        LOGGER.debug("Registering new ldap claims handler.");
        ldapHandlerRegistration = registerClaimsHandler(ldapHandler, ldapHandlerRegistration);
    }

    /**
     * Utility method that registers a ClaimsHandler and returns the service
     * registration.
     *
     * @param handler
     *            Handler that should be registered.
     * @param registration
     *            Previous registration, will be used to unregister if not null.
     * @return new registration for the service.
     */
    private ServiceRegistration<ClaimsHandler> registerClaimsHandler(ClaimsHandler handler,
            ServiceRegistration<ClaimsHandler> registration) {
        if (registration != null) {
            registration.unregister();
        }
        return context.registerService(ClaimsHandler.class, handler, null);
    }

    public void setUrl(String url) {
        LOGGER.trace("Setting url: {}", url);
        ldapProperties.put(URL, url);
    }

    public void setLdapBindUserDn(String bindUserDn) {
        LOGGER.trace("Setting bindUserDn: {}", bindUserDn);
        ldapProperties.put(LDAP_BIND_USER_DN, bindUserDn);
    }

    public void setPassword(String password) {
        LOGGER.trace("Setting password: {}", password);
        ldapProperties.put(PASSWORD, password);
    }

    public void setUserNameAttribute(String userNameAttribute) {
        LOGGER.trace("Setting userNameAttribute: {}", userNameAttribute);
        ldapProperties.put(USER_NAME_ATTRIBUTE, userNameAttribute);
    }

    public void setUserBaseDn(String userBaseDn) {
        LOGGER.trace("Setting userBaseDn: {}", userBaseDn);
        ldapProperties.put(USER_BASE_DN, userBaseDn);
    }

    public void setObjectClass(String objectClass) {
        LOGGER.trace("Setting objectClass: {}", objectClass);
        ldapProperties.put(OBJECT_CLASS, objectClass);
    }

    public void setMemberNameAttribute(String memberNameAttribute) {
        LOGGER.trace("Setting memberNameAttribute: {}", memberNameAttribute);
        ldapProperties.put(MEMBER_NAME_ATTRIBUTE, memberNameAttribute);
    }

    public void setGroupBaseDn(String groupBaseDn) {
        LOGGER.trace("Setting groupBaseDn: {}", groupBaseDn);
        ldapProperties.put(GROUP_BASE_DN, groupBaseDn);
    }

    public void setUserDn(String userDn) {
        LOGGER.trace("Setting userDn: {}", userDn);
        ldapProperties.put(USER_DN, userDn);
    }

    public void setPropertyFileLocation(String propertyFileLocation) {
        LOGGER.trace("Setting propertyFileLocation: {}", propertyFileLocation);
        ldapProperties.put(PROPERTY_FILE_LOCATION, propertyFileLocation);
    }

    public void configure() {
        LOGGER.trace("configure method called - calling update");
        update(ldapProperties);
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {
        String keystoreLocation = configuration.get(ConfigurationManager.KEY_STORE);
        String keystorePassword = encryptService.decryptValue(configuration
                .get(ConfigurationManager.KEY_STORE_PASSWORD));

        String truststoreLocation = configuration.get(ConfigurationManager.TRUST_STORE);
        String truststorePassword = encryptService.decryptValue(configuration
                .get(ConfigurationManager.TRUST_STORE_PASSWORD));

        if (StringUtils.isNotBlank(keystoreLocation)) {
            if (new File(keystoreLocation).exists()) {
                this.keystoreLoc = keystoreLocation;
                this.keystorePass = keystorePassword;
            } else {
                LOGGER.debug("Keystore file does not exist at location {}, not updating keystore values.");
            }
        }
        if (StringUtils.isNotBlank(truststoreLocation)) {
            if (new File(truststoreLocation).exists()) {
                this.truststoreLoc = truststoreLocation;
                this.truststorePass = truststorePassword;
            } else {
                LOGGER.debug("Truststore file does not exist at location {}, not updating truststore values.");
            }
        }
        update(props);
    }

}
