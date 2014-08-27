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

import java.util.Map;

import org.apache.cxf.sts.claims.ClaimsHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;

import ddf.security.encryption.EncryptionService;

/**
 * Creates and registers LDAP and Role claims handlers.
 *
 */
public class ClaimsHandlerManager {

    private EncryptionService encryptService;

    private BundleContext context;

    private ServiceRegistration<ClaimsHandler> roleHandlerRegistration = null;

    private ServiceRegistration<ClaimsHandler> ldapHandlerRegistration = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimsHandlerManager.class);

    /**
     * Creates a new instance of the ClaimsHandlerManager.
     *
     * @param encryptService
     *            Encryption service used to decrypt passwords from the
     *            configurations.
     * @param context
     *            BundleContext that should be used to register services.
     * @param defaults
     *            Default properties to use on initial registration.
     */
    public ClaimsHandlerManager(EncryptionService encryptService, BundleContext context,
            Map<String, String> defaults) {
        this.encryptService = encryptService;
        this.context = context;
        update(defaults);
    }

    /**
     * Callback method that is called when configuration is updated.
     *
     * @param props
     *            Map of properties.
     */
    public void update(Map<String, String> props) {
        LOGGER.debug("Received an updated set of configurations for the LDAP/Role Claims Handlers.");
        String url = props.get("url");
        String userDn = props.get("userDn");
        String password = props.get("password");
        String userBaseDn = props.get("userBaseDn");
        String objectClass = props.get("objectClass");
        String memberNameAttribute = props.get("memberNameAttribute");
        String groupBaseDn = props.get("groupBaseDn");
        String userNameAttribute = props.get("userNameAttribute");
        String propertyFileLocation = props.get("propertyFileLocation");
        try {
            LdapTemplate template = createLdapTemplate(url, userDn, password);
            registerRoleClaimsHandler(template, propertyFileLocation, userBaseDn,
                    userNameAttribute, objectClass, memberNameAttribute, groupBaseDn);
            registerLdapClaimsHandler(template, propertyFileLocation, userBaseDn, userNameAttribute);

        } catch (Exception e) {
            LOGGER.warn("Experienced error while configuring claims handlers. Handlers are NOT configured and claim retrieval will not work.");
        }

    }

    /**
     * Creates a new LdapTemplate from the incoming properties.
     *
     * @param url
     *            URL to LDAP.
     * @param userDn
     *            DN of the user that should be used to query LDAP.
     * @param password
     *            Password of the LDAP user used to query LDAP.
     * @return New LdapTemplate
     * @throws Exception
     *             If the LDAP configurations are incorrect and the system
     *             cannot connect to the LDAP.
     */
    private LdapTemplate createLdapTemplate(String url, String userDn, String password)
            throws Exception {
        ContextSourceDecrypt contextSource = new ContextSourceDecrypt();
        contextSource.setEncryptionService(encryptService);
        contextSource.setUrl(url);
        contextSource.setUserDn(userDn);
        contextSource.setPassword(password);
        try {
            contextSource.afterPropertiesSet();
            return new LdapTemplate(contextSource);
        } catch (Exception e) {
            LOGGER.warn(
                    "Error thrown while trying to configure ldap context. Cannot connect to LDAP for determining claims.",
                    e);
            throw e;
        }
    }

    /**
     * Registers a new Role-based ClaimsHandler.
     *
     * @param template
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
    private void registerRoleClaimsHandler(LdapTemplate template, String propertyFileLoc,
            String userBaseDn, String userNameAttr, String objectClass, String memberNameAttribute, String groupBaseDn) {
        RoleClaimsHandler roleHandler = new RoleClaimsHandler();
        roleHandler.setLdapTemplate(template);
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
     * @param template
     *            LdapTemplate used to query ldap for the roles.
     * @param propertyFileLoc
     *            File location of the property file.
     * @param userBaseDn
     *            Base DN to determine the roles.
     * @param userNameAttr
     *            Identifier that defines the user.
     */
    private void registerLdapClaimsHandler(LdapTemplate template, String propertyFileLoc,
            String userBaseDn, String userNameAttr) {
        LdapClaimsHandler ldapHandler = new LdapClaimsHandler();
        ldapHandler.setLdapTemplate(template);
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

}
