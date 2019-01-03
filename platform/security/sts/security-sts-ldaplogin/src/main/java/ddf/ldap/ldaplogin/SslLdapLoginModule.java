/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.ldap.ldaplogin;

import com.google.common.collect.ImmutableSet;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.DigestMD5SASLBindRequest;
import org.forgerock.opendj.ldap.requests.GSSAPISASLBindRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslLdapLoginModule extends AbstractKarafLoginModule {

  public static final String CONNECTION_USERNAME_OPTIONS_KEY = "connection.username";

  @SuppressWarnings("squid:S2068")
  public static final String CONNECTION_PASSWORD_OPTIONS_KEY = "connection.password";

  public static final String USER_BASE_DN_OPTIONS_KEY = "user.base.dn";

  public static final String USER_FILTER_OPTIONS_KEY = "user.filter";

  public static final String USER_SEARCH_SUBTREE_OPTIONS_KEY = "user.search.subtree";

  public static final String ROLE_BASE_DN_OPTIONS_KEY = "role.base.dn";

  public static final String ROLE_FILTER_OPTIONS_KEY = "role.filter";

  public static final String ROLE_NAME_ATTRIBUTE_OPTIONS_KEY = "role.name.attribute";

  public static final String ROLE_SEARCH_SUBTREE_OPTIONS_KEY = "role.search.subtree";

  public static final String BIND_METHOD_OPTIONS_KEY = "bindMethod";

  public static final String REALM_OPTIONS_KEY = "realm";

  public static final String KDC_ADDRESS_OPTIONS_KEY = "kdcAddress";

  public static final String CONNECTION_POOL_ID_OPTIONS_KEY = "connectionPoolId";

  private static final Logger LOGGER = LoggerFactory.getLogger(SslLdapLoginModule.class);

  private static final String DEFAULT_AUTHENTICATION = "simple";

  private static final String LOGIN_ERROR_MESSAGE =
      "Username [%s] could not log in successfully using LDAP authentication due to an exception: \n\t";

  private String realm;

  private String kdcAddress;

  private String bindMethod = DEFAULT_AUTHENTICATION;

  private String connectionUsername;

  private char[] connectionPassword;

  private String userBaseDN;

  private String userFilter;

  private boolean userSearchSubtree = true;

  private String roleBaseDN;

  private EncryptionService encryptionService;

  private String roleFilter;

  private String roleNameAttribute;

  private boolean roleSearchSubtree = true;

  private GenericObjectPool<Connection> ldapConnectionPool;

  protected boolean doLogin() throws LoginException {

    // --------- EXTRACT USERNAME AND PASSWORD FOR LDAP LOOKUP -------------
    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("Username: ");
    callbacks[1] = new PasswordCallback("Password: ", false);

    try {
      callbackHandler.handle(callbacks);
    } catch (IOException ioException) {
      LOGGER.debug("Exception while handling login.", ioException);
      throw new LoginException(ioException.getMessage());
    } catch (UnsupportedCallbackException unsupportedCallbackException) {
      LOGGER.debug("Exception while handling login.", unsupportedCallbackException);
      throw new LoginException(
          unsupportedCallbackException.getMessage()
              + " not available to obtain information from user.");
    }

    user = ((NameCallback) callbacks[0]).getName();
    if (user == null) {
      return false;
    }
    user = user.trim();
    validateUsername(user);

    char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();

    // If either a username or password is specified don't allow authentication = "none".
    // This is to prevent someone from logging into Karaf as any user without providing a
    // valid password (because if authentication = none, the password could be any
    // value - it is ignored).
    // Username is not checked in this conditional because a null username immediately exits
    // this method.
    if ("none".equalsIgnoreCase(getBindMethod()) && (tmpPassword != null)) {
      LOGGER.debug(
          "Changing from authentication = none to simple since user or password was specified.");
      // default to simple so that the provided user/password will get checked
      setBindMethod(DEFAULT_AUTHENTICATION);
    }

    if (tmpPassword == null) {
      tmpPassword = new char[0];
    }

    // ---------------------------------------------------------------------
    // RESET OBJECT STATE AND DECLARE LOCAL VARS
    principals = new HashSet<>();
    Connection connection;
    String userDn;
    // ---------------------------------------------------------------------

    // ------------- CREATE CONNECTION #1 ----------------------------------
    try {
      connection = ldapConnectionPool.borrowObject();
    } catch (Exception e) {
      LOGGER.info("Unable to obtain ldap connection from pool", e);
      return false;
    }
    try {

      if (connection != null) {
        // ------------- BIND #1 (CONNECTION USERNAME & PASSWORD) --------------
        try {
          BindRequest request;
          switch (bindMethod) {
            case "Simple":
              request = Requests.newSimpleBindRequest(connectionUsername, connectionPassword);
              break;
            case "SASL":
              request = Requests.newPlainSASLBindRequest(connectionUsername, connectionPassword);
              break;
            case "GSSAPI SASL":
              request = Requests.newGSSAPISASLBindRequest(connectionUsername, connectionPassword);
              ((GSSAPISASLBindRequest) request).setRealm(realm);
              ((GSSAPISASLBindRequest) request).setKDCAddress(kdcAddress);
              break;
            case "Digest MD5 SASL":
              request =
                  Requests.newDigestMD5SASLBindRequest(connectionUsername, connectionPassword);
              ((DigestMD5SASLBindRequest) request).setCipher(DigestMD5SASLBindRequest.CIPHER_HIGH);
              ((DigestMD5SASLBindRequest) request).getQOPs().clear();
              ((DigestMD5SASLBindRequest) request)
                  .getQOPs()
                  .add(DigestMD5SASLBindRequest.QOP_AUTH_CONF);
              ((DigestMD5SASLBindRequest) request)
                  .getQOPs()
                  .add(DigestMD5SASLBindRequest.QOP_AUTH_INT);
              ((DigestMD5SASLBindRequest) request).getQOPs().add(DigestMD5SASLBindRequest.QOP_AUTH);
              if (StringUtils.isNotEmpty(realm)) {
                ((DigestMD5SASLBindRequest) request).setRealm(realm);
              }
              break;
            default:
              request = Requests.newSimpleBindRequest(connectionUsername, connectionPassword);
              break;
          }
          LOGGER.trace("Attempting LDAP bind for administrator: {}", connectionUsername);
          BindResult bindResult = connection.bind(request);

          if (!bindResult.isSuccess()) {
            LOGGER.debug("Bind failed");
            return false;
          }
        } catch (LdapException e) {
          LOGGER.debug("Unable to bind to LDAP server.", e);
          return false;
        }
        LOGGER.trace("LDAP bind successful for administrator: {}", connectionUsername);
        // --------- SEARCH #1, FIND USER DISTINGUISHED NAME -----------
        SearchScope scope;
        scope = userSearchSubtree ? SearchScope.WHOLE_SUBTREE : SearchScope.SINGLE_LEVEL;
        userFilter = userFilter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement(user));
        userFilter = userFilter.replace("\\", "\\\\");
        LOGGER.trace(
            "Performing LDAP query for user: {} at {} with filter {}",
            user,
            userBaseDN,
            userFilter);
        try (ConnectionEntryReader entryReader = connection.search(userBaseDN, scope, userFilter)) {
          while (entryReader.hasNext() && entryReader.isReference()) {
            LOGGER.debug("Referral ignored while searching for user {}", user);
            entryReader.readReference();
          }
          if (!entryReader.hasNext()) {
            LOGGER.info("User {} not found in LDAP.", user);
            return false;
          }
          SearchResultEntry searchResultEntry = entryReader.readEntry();

          userDn = searchResultEntry.getName().toString();

        } catch (LdapException | SearchResultReferenceIOException e) {
          LOGGER.info("Unable to read contents of LDAP user search.", e);
          return false;
        }

        // ----- BIND #2 (USER DISTINGUISHED NAME AND PASSWORD) ------------
        // Validate user's credentials.
        try {
          LOGGER.trace("Attempting LDAP bind for user: {}", userDn);
          BindResult bindResult = connection.bind(userDn, tmpPassword);

          if (!bindResult.isSuccess()) {
            LOGGER.info("Bind failed");
            return false;
          }
        } catch (Exception e) {
          LOGGER.info("Unable to bind user: {} to LDAP server.", userDn, e);
          return false;
        }

        LOGGER.trace("LDAP bind successful for user: {}", userDn);

        // ---------- ADD USER AS PRINCIPAL --------------------------------
        principals.add(new UserPrincipal(user));

        // ----- BIND #3 (CONNECTION USERNAME & PASSWORD) --------------
        try {
          LOGGER.trace("Attempting LDAP bind for administrator: {}", connectionUsername);
          BindResult bindResult = connection.bind(connectionUsername, connectionPassword);

          if (!bindResult.isSuccess()) {
            LOGGER.info("Bind failed");
            return false;
          }
        } catch (LdapException e) {
          LOGGER.info("Unable to bind to LDAP server.", e);
          return false;
        }
        LOGGER.trace("LDAP bind successful for administrator: {}", connectionUsername);

        // --------- SEARCH #3, GET ROLES ------------------------------
        scope = roleSearchSubtree ? SearchScope.WHOLE_SUBTREE : SearchScope.SINGLE_LEVEL;
        roleFilter = roleFilter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement(user));
        roleFilter =
            roleFilter.replaceAll(Pattern.quote("%dn"), Matcher.quoteReplacement(userBaseDN));
        roleFilter =
            roleFilter.replaceAll(Pattern.quote("%fqdn"), Matcher.quoteReplacement(userDn));
        roleFilter = roleFilter.replace("\\", "\\\\");
        LOGGER.trace(
            "Performing LDAP query for roles for user: {} at {} with filter {} for role attribute {}",
            user,
            roleBaseDN,
            roleFilter,
            roleNameAttribute);

        // ------------- ADD ROLES AS NEW PRINCIPALS -------------------
        try (ConnectionEntryReader entryReader =
            connection.search(roleBaseDN, scope, roleFilter, roleNameAttribute)) {
          SearchResultEntry entry;
          while (entryReader.hasNext()) {
            if (entryReader.isEntry()) {
              entry = entryReader.readEntry();
              Attribute attr = entry.getAttribute(roleNameAttribute);
              if (attr == null) {
                throw new LoginException(
                    "No attributes returned for [" + roleNameAttribute + " : " + roleBaseDN + "]");
              }
              for (ByteString role : attr) {
                principals.add(new RolePrincipal(role.toString()));
              }
            } else {
              // Got a continuation reference.
              final SearchResultReference ref = entryReader.readReference();
              LOGGER.debug("Skipping result reference: {}", ref.getURIs());
            }
          }
        } catch (Exception e) {
          LOGGER.debug("Exception while getting roles for [" + user + "].", e);
          throw new LoginException("Can't get roles for [" + user + "]: " + e.getMessage());
        }
      } else {
        LOGGER.trace("LDAP Connection was null could not authenticate user.");
        return false;
      }

      return true;
    } finally {
      ldapConnectionPool.returnObject(connection);
    }
  }

  @Override
  public boolean abort() throws LoginException {
    return true;
  }

  @Override
  public boolean logout() throws LoginException {
    subject.getPrincipals().removeAll(principals);
    principals.clear();
    return true;
  }

  protected BundleContext getContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(SslLdapLoginModule.class);
    if (cxfBundle != null) {
      return cxfBundle.getBundleContext();
    }
    return null;
  }

  @Override
  public void initialize(
      Subject subject,
      CallbackHandler callbackHandler,
      Map<String, ?> sharedState,
      Map<String, ?> options) {
    super.initialize(subject, callbackHandler, options);
    installEncryptionService();
    connectionUsername = (String) options.get(CONNECTION_USERNAME_OPTIONS_KEY);
    connectionPassword =
        getDecryptedPassword((String) options.get(CONNECTION_PASSWORD_OPTIONS_KEY));
    userBaseDN = (String) options.get(USER_BASE_DN_OPTIONS_KEY);
    userFilter = (String) options.get(USER_FILTER_OPTIONS_KEY);
    userSearchSubtree = Boolean.parseBoolean((String) options.get(USER_SEARCH_SUBTREE_OPTIONS_KEY));
    roleBaseDN = (String) options.get(ROLE_BASE_DN_OPTIONS_KEY);
    roleFilter = (String) options.get(ROLE_FILTER_OPTIONS_KEY);
    roleNameAttribute = (String) options.get(ROLE_NAME_ATTRIBUTE_OPTIONS_KEY);
    roleSearchSubtree = Boolean.parseBoolean((String) options.get(ROLE_SEARCH_SUBTREE_OPTIONS_KEY));
    bindMethod = (String) options.get(BIND_METHOD_OPTIONS_KEY);
    realm = (String) options.get(REALM_OPTIONS_KEY);
    kdcAddress = (String) options.get(KDC_ADDRESS_OPTIONS_KEY);
    String connectionPoolId = (String) options.get(CONNECTION_POOL_ID_OPTIONS_KEY);
    installLdapConnectionPool(connectionPoolId);
  }

  private void installLdapConnectionPool(String connectionPoolId) {
    BundleContext bundleContext = getContext();
    if (bundleContext != null) {
      try {
        Collection<ServiceReference<GenericObjectPool>> serviceReferences =
            bundleContext.getServiceReferences(
                GenericObjectPool.class, String.format("(id=%s)", connectionPoolId));
        ServiceReference<GenericObjectPool> serviceReference =
            serviceReferences
                .stream()
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No LDAPConnectionPool service found with id:" + connectionPoolId));
        ldapConnectionPool = bundleContext.getService(serviceReference);
      } catch (InvalidSyntaxException | IllegalStateException e) {
        LOGGER.error("Unable to get LDAP Connection pool. LDAP log in will not be possible.", e);
      }
    }
  }

  @Override
  public boolean login() throws LoginException {
    boolean isLoggedIn;
    try {
      isLoggedIn = doLogin();
      if (!isLoggedIn) {
        SecurityLogger.audit("Username [" + user + "] failed LDAP authentication.");
      }
      return isLoggedIn;
    } catch (InvalidCharactersException e) {
      SecurityLogger.audit(e.getMessage());
      throw new LoginException(String.format(LOGIN_ERROR_MESSAGE, user) + e.getMessage());
    } catch (LoginException e) {
      throw new LoginException(String.format(LOGIN_ERROR_MESSAGE, user) + e.getMessage());
    }
  }

  void validateUsername(String username) throws InvalidCharactersException {
    boolean hasBadCharacters = false;
    for (int i = 0; i < username.length(); i++) {
      char curChar = username.charAt(i);
      switch (curChar) {
        case '\\':
        case ',':
        case '+':
        case '"':
        case '<':
        case '>':
        case ';':
        case '#':
          hasBadCharacters = true;
          break;
      }
      if (hasBadCharacters) {
        throw new InvalidCharactersException(
            String.format("Username [%s] contains invalid LDAP characters", username));
      }
    }
  }

  Set<Principal> getPrincipals() {
    return ImmutableSet.copyOf(principals);
  }

  private void installEncryptionService() {

    BundleContext bundleContext = getContext();
    if (bundleContext != null) {
      ServiceReference serviceReference =
          bundleContext.getServiceReference(EncryptionService.class.getName());
      encryptionService = (EncryptionService) bundleContext.getService(serviceReference);
      bundleContext.ungetService(serviceReference);
    }
  }

  protected char[] getDecryptedPassword(String encryptedPassword) {

    char[] decryptedPassword = null;
    if (encryptionService != null) {
      try {
        decryptedPassword = encryptionService.decryptValue(encryptedPassword).toCharArray();
      } catch (SecurityException | IllegalStateException e) {
        LOGGER.info("Error decrypting connection password passed into LDAP configuration: ", e);
      }
    } else {
      LOGGER.info("Encryption service is not available.");
    }

    return decryptedPassword;
  }

  public EncryptionService getEncryptionService() {
    return encryptionService;
  }

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  String getBindMethod() {
    return bindMethod;
  }

  void setBindMethod(String bindMethod) {
    this.bindMethod = bindMethod;
  }

  // for testing
  public void setLdapConnectionPool(GenericObjectPool<Connection> ldapConnectionPool) {
    this.ldapConnectionPool = ldapConnectionPool;
  }

  private static class InvalidCharactersException extends LoginException {

    public InvalidCharactersException(String message) {
      super(message);
    }
  }
}
