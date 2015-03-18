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
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.LdapException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and registers LDAP and Role claims handlers.
 *
 */
public class ClaimsHandlerManager implements ConfigurationWatcher {

    public static final String URL = "url";
    public static final String START_TLS = "startTls";
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

    private ServiceRegistration<ClaimsHandler> roleHandlerRegistration = null;

    private ServiceRegistration<ClaimsHandler> ldapHandlerRegistration = null;

    protected String keystoreLoc, keystorePass;

    protected String truststoreLoc, truststorePass;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimsHandlerManager.class);

    private Map<String, Object> ldapProperties = new HashMap<>();

    private Map<String, Object> props;

    /**
     * Creates a new instance of the ClaimsHandlerManager.
     *
     * @param encryptService Encryption service used to decrypt passwords from the configurations.
     */
    public ClaimsHandlerManager(EncryptionService encryptService) {
        this.encryptService = encryptService;
    }

    /**
     * Callback method that is called when configuration is updated. Also called by the
     * blueprint init-method when all properties have been set.
     *
     * @param props Map of properties.
     */
    public void update(Map<String, Object> props) {
        LOGGER.debug("Received an updated set of configurations for the LDAP/Role Claims Handlers.");
        String url = (String) props.get(ClaimsHandlerManager.URL);
        Boolean startTls;
        if (props.get(ClaimsHandlerManager.START_TLS) instanceof String) {
            startTls = Boolean.valueOf((String) props.get(ClaimsHandlerManager.START_TLS));
        } else {
            startTls = (Boolean) props.get(ClaimsHandlerManager.START_TLS);
        }
        String userDn = (String) props.get(ClaimsHandlerManager.LDAP_BIND_USER_DN);
        String password = (String) props.get(ClaimsHandlerManager.PASSWORD);
        String userBaseDn = (String) props.get(ClaimsHandlerManager.USER_BASE_DN);
        String objectClass = (String) props.get(ClaimsHandlerManager.OBJECT_CLASS);
        String memberNameAttribute = (String) props.get(ClaimsHandlerManager.MEMBER_NAME_ATTRIBUTE);
        String groupBaseDn = (String) props.get(ClaimsHandlerManager.GROUP_BASE_DN);
        String userNameAttribute = (String) props.get(ClaimsHandlerManager.USER_NAME_ATTRIBUTE);
        String propertyFileLocation = (String) props.get(ClaimsHandlerManager.PROPERTY_FILE_LOCATION);
        try {
            if (encryptService != null) {
                password = encryptService.decryptValue(password);
            }
            LDAPConnectionFactory connection1 = createLdapConnectionFactory(url, startTls);
            LDAPConnectionFactory connection2 = createLdapConnectionFactory(url, startTls);
            registerRoleClaimsHandler(connection1, propertyFileLocation, userBaseDn, userNameAttribute, objectClass, memberNameAttribute,
                    groupBaseDn, userDn, password);
            registerLdapClaimsHandler(connection2, propertyFileLocation, userBaseDn,
                    userNameAttribute, userDn, password);

        } catch (Exception e) {
            LOGGER.warn(
                    "Experienced error while configuring claims handlers. Handlers are NOT configured and claim retrieval will not work.",
                    e);
        }
        this.props = props;
    }

    public void destroy() {

    }

    protected LDAPConnectionFactory createLdapConnectionFactory(String url, Boolean startTls)
            throws LdapException {
        boolean useSsl = url.startsWith("ldaps");
        boolean useTls = !url.startsWith("ldaps") && startTls;

        LDAPOptions lo = new LDAPOptions();

        try {
            if (useSsl || useTls) {
                SSLContext sslContext = SSLContext.getInstance(CommonSSLFactory.PROTOCOL);
                if(keystoreLoc != null && truststoreLoc != null) {
                    sslContext.init(CommonSSLFactory.createKeyManagerFactory(keystoreLoc, keystorePass).getKeyManagers(),
                            CommonSSLFactory.createTrustManagerFactory(truststoreLoc, truststorePass).getTrustManagers(), new SecureRandom());
                }

                lo.setSSLContext(sslContext);
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error("Error encountered while configuring SSL. Secure connection will fail.", e);
        }

        lo.setUseStartTLS(useTls);
        lo.addEnabledCipherSuite("TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA");
        lo.addEnabledProtocol("TLSv1.1", "TLSv1.2");
        lo.setProviderClassLoader(ClaimsHandlerManager.class.getClassLoader());

        String host = url.substring(url.indexOf("://") + 3, url.lastIndexOf(":"));
        Integer port = Integer.valueOf(url.substring(url.lastIndexOf(":") + 1));

        return new LDAPConnectionFactory(host, port, lo);
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
    private void registerRoleClaimsHandler(LDAPConnectionFactory connection, String propertyFileLoc,
            String userBaseDn, String userNameAttr, String objectClass, String memberNameAttribute, String groupBaseDn, String userDn, String password) {
        RoleClaimsHandler roleHandler = new RoleClaimsHandler();
        roleHandler.setLdapConnectionFactory(connection);
        roleHandler.setPropertyFileLocation(propertyFileLoc);
        roleHandler.setUserBaseDn(userBaseDn);
        roleHandler.setUserNameAttribute(userNameAttr);
        roleHandler.setObjectClass(objectClass);
        roleHandler.setMemberNameAttribute(memberNameAttribute);
        roleHandler.setGroupBaseDn(groupBaseDn);
        roleHandler.setBindUserDN(userDn);
        roleHandler.setBindUserCredentials(password);
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
    private void registerLdapClaimsHandler(LDAPConnectionFactory connection, String propertyFileLoc,
            String userBaseDn, String userNameAttr, String userDn, String password) {
        LdapClaimsHandler ldapHandler = new LdapClaimsHandler();
        ldapHandler.setLdapConnectionFactory(connection);
        ldapHandler.setPropertyFileLocation(propertyFileLoc);
        ldapHandler.setUserBaseDN(userBaseDn);
        ldapHandler.setUserNameAttribute(userNameAttr);
        ldapHandler.setBindUserDN(userDn);
        ldapHandler.setBindUserCredentials(password);
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
        BundleContext context = getContext();
        if (registration != null) {
            ClaimsHandler oldClaimsHandler = context.getService(registration.getReference());
            if (oldClaimsHandler instanceof RoleClaimsHandler) {
                ((RoleClaimsHandler) oldClaimsHandler).disconnect();
            } else if (oldClaimsHandler instanceof LdapClaimsHandler) {
                ((LdapClaimsHandler) oldClaimsHandler).disconnect();
            }
            registration.unregister();
        }

        if (context != null) {
            return context.registerService(ClaimsHandler.class, handler, null);
        }
        return null;
    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(ClaimsHandlerManager.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }

    public void setUrl(String url) {
        LOGGER.trace("Setting url: {}", url);
        ldapProperties.put(URL, url);
    }

    public void setStartTls(boolean startTls) {
        LOGGER.trace("Setting startTls: {}", startTls);
        ldapProperties.put(START_TLS, startTls);
    }

    public void setStartTls(String startTls) {
        LOGGER.trace("Setting startTls: {}", startTls);
        ldapProperties.put(START_TLS, startTls);
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
