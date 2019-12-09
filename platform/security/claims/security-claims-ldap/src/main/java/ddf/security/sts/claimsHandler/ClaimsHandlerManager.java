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
package ddf.security.sts.claimsHandler;

import ddf.security.claims.ClaimsHandler;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.PropertyResolver;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPUrl;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Options;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates and registers LDAP and Role claims handlers. */
public class ClaimsHandlerManager {

  public static final String URL = "url";

  public static final String LOAD_BALANCING = "ldapLoadBalancing";

  public static final String FAILOVER = "failover";

  public static final String START_TLS = "startTls";

  public static final String OVERRIDE_CERT_DN = "overrideCertDn";

  public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";

  public static final String BIND_METHOD = "bindMethod";

  public static final String REALM = "realm";

  public static final String KDC_ADDRESS = "kdcAddress";

  public static final String PASSWORD = "password";

  public static final String LOGIN_USER_ATTRIBUTE = "loginUserAttribute";

  public static final String MEMBER_USER_ATTRIBUTE = "membershipUserAttribute";

  public static final String USER_BASE_DN = "userBaseDn";

  public static final String OBJECT_CLASS = "objectClass";

  public static final String MEMBER_NAME_ATTRIBUTE = "memberNameAttribute";

  public static final String GROUP_BASE_DN = "groupBaseDn";

  public static final String PROPERTY_FILE_LOCATION = "propertyFileLocation";

  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimsHandlerManager.class);

  private static final String PROTOCOL = "TLS";

  private EncryptionService encryptService;

  private ServiceRegistration<ClaimsHandler> roleHandlerRegistration = null;

  private ServiceRegistration<ClaimsHandler> ldapHandlerRegistration = null;

  private Map<String, Object> ldapProperties = new HashMap<>();

  /**
   * Creates a new instance of the ClaimsHandlerManager.
   *
   * @param encryptService Encryption service used to decrypt passwords from the configurations.
   */
  public ClaimsHandlerManager(EncryptionService encryptService) {
    this.encryptService = encryptService;
  }

  /**
   * Callback method that is called when configuration is updated. Also called by the blueprint
   * init-method when all properties have been set.
   *
   * @param props Map of properties.
   */
  public void update(Map<String, Object> props) {
    if (props == null) {
      return;
    }
    LOGGER.debug("Received an updated set of configurations for the LDAP/Role Claims Handlers.");
    List<String> urls = getUrls(props, ClaimsHandlerManager.URL);
    String loadBalancingAlgorithm = (String) props.get(ClaimsHandlerManager.LOAD_BALANCING);
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
    String loginUserAttribute = (String) props.get(ClaimsHandlerManager.LOGIN_USER_ATTRIBUTE);
    String membershipUserAttribute = (String) props.get(ClaimsHandlerManager.MEMBER_USER_ATTRIBUTE);
    String propertyFileLocation = (String) props.get(ClaimsHandlerManager.PROPERTY_FILE_LOCATION);
    String bindMethod = (String) props.get(ClaimsHandlerManager.BIND_METHOD);
    String realm =
        (props.get(ClaimsHandlerManager.REALM) != null)
            ? (String) props.get(ClaimsHandlerManager.REALM)
            : "";
    String kdcAddress =
        (props.get(ClaimsHandlerManager.KDC_ADDRESS) != null)
            ? (String) props.get(ClaimsHandlerManager.KDC_ADDRESS)
            : "";
    if ("GSSAPI SASL".equals(bindMethod)
        && (StringUtils.isEmpty(realm) || StringUtils.isEmpty(kdcAddress))) {
      LOGGER.warn(
          "LDAP connection will fail. GSSAPI SASL connection requires Kerberos Realm and KDC Address.");
    }
    Boolean overrideCertDn;
    if (props.get(ClaimsHandlerManager.OVERRIDE_CERT_DN) instanceof String) {
      overrideCertDn = Boolean.valueOf((String) props.get(ClaimsHandlerManager.OVERRIDE_CERT_DN));
    } else {
      overrideCertDn = (Boolean) props.get(ClaimsHandlerManager.OVERRIDE_CERT_DN);
    }
    if (startTls == null) {
      startTls = false;
    }
    if (overrideCertDn == null) {
      overrideCertDn = false;
    }
    try {
      if (encryptService != null) {
        password = encryptService.decryptValue(password);
      }
      ConnectionFactory connection1 =
          createConnectionFactory(urls, startTls, loadBalancingAlgorithm);
      ConnectionFactory connection2 =
          createConnectionFactory(urls, startTls, loadBalancingAlgorithm);
      registerRoleClaimsHandler(
          connection1,
          propertyFileLocation,
          userBaseDn,
          loginUserAttribute,
          membershipUserAttribute,
          objectClass,
          memberNameAttribute,
          groupBaseDn,
          userDn,
          password,
          overrideCertDn,
          bindMethod,
          realm,
          kdcAddress);
      registerLdapClaimsHandler(
          connection2,
          propertyFileLocation,
          userBaseDn,
          loginUserAttribute,
          userDn,
          password,
          overrideCertDn,
          bindMethod,
          realm,
          kdcAddress);

    } catch (Exception e) {
      LOGGER.warn(
          "Experienced error while configuring claims handlers. Handlers are NOT configured and claim retrieval will not work. Check LDAP configuration.",
          e);
    }
  }

  private List<String> getUrls(Map<String, Object> props, String key) {
    List<String> urls = new ArrayList<>();
    Object urlProperty = props.get(key);
    if (urlProperty instanceof String[]) {
      urls.addAll(Arrays.asList((String[]) urlProperty));
    } else {
      urls.add(urlProperty.toString());
    }

    return urls;
  }

  public void destroy() {}

  protected ConnectionFactory createConnectionFactory(
      List<String> urls, Boolean startTls, String loadBalancingAlgorithm) throws LdapException {
    List<ConnectionFactory> connectionFactories = new ArrayList<>();

    for (String singleUrl : urls) {
      connectionFactories.add(
          createLdapConnectionFactory(new PropertyResolver(singleUrl).toString(), startTls));
    }

    Options options = Options.defaultOptions();
    if (FAILOVER.equalsIgnoreCase(loadBalancingAlgorithm)) {
      return Connections.newFailoverLoadBalancer(connectionFactories, options);
    } else {
      return Connections.newRoundRobinLoadBalancer(connectionFactories, options);
    }
  }

  protected LDAPConnectionFactory createLdapConnectionFactory(String url, Boolean startTls)
      throws LdapException {
    boolean useSsl = url.startsWith("ldaps");
    boolean useTls = !url.startsWith("ldaps") && startTls;

    Options lo = Options.defaultOptions();
    try {
      if (useSsl || useTls) {
        lo.set(LDAPConnectionFactory.SSL_CONTEXT, SSLContext.getDefault());
      }
    } catch (GeneralSecurityException e) {
      LOGGER.info("Error encountered while configuring SSL. Secure connection will fail.", e);
    }

    lo.set(LDAPConnectionFactory.SSL_USE_STARTTLS, useTls);
    lo.set(
        LDAPConnectionFactory.SSL_ENABLED_CIPHER_SUITES,
        Arrays.asList(System.getProperty("https.cipherSuites").split(",")));
    lo.set(
        LDAPConnectionFactory.SSL_ENABLED_PROTOCOLS,
        Arrays.asList(System.getProperty("https.protocols").split(",")));
    lo.set(
        LDAPConnectionFactory.TRANSPORT_PROVIDER_CLASS_LOADER,
        ClaimsHandlerManager.class.getClassLoader());

    LDAPUrl parsedUrl = LDAPUrl.valueOf(url);
    String host = parsedUrl.getHost();
    Integer port = parsedUrl.getPort();

    auditRemoteConnection(host);

    return new LDAPConnectionFactory(host, port, lo);
  }

  private void auditRemoteConnection(String host) {
    try {
      InetAddress inetAddress = InetAddress.getByName(host);
      SecurityLogger.audit(
          "Setting up remote connection to LDAP [{}].", inetAddress.getHostAddress());
    } catch (Exception e) {
      LOGGER.debug(
          "Unhandled exception while attempting to determine the IP address for an LDAP, might be a DNS issue.",
          e);
      SecurityLogger.audit(
          "Unable to determine the IP address for an LDAP [{}], might be a DNS issue.", host);
    }
  }

  /**
   * Registers a new Role-based ClaimsHandler.
   *
   * @param connection LdapTemplate used to query ldap for the roles.
   * @param propertyFileLoc File location of the property file.
   * @param userBaseDn Base DN to determine the roles.
   * @param loginUserAttribute Identifier that defines the user.
   * @param groupBaseDn Base DN of the group.
   */
  private void registerRoleClaimsHandler(
      ConnectionFactory connection,
      String propertyFileLoc,
      String userBaseDn,
      String loginUserAttribute,
      String membershipUserAttribute,
      String objectClass,
      String memberNameAttribute,
      String groupBaseDn,
      String userDn,
      String password,
      boolean overrideCertDn,
      String bindMethod,
      String realm,
      String kdcAddress) {
    RoleClaimsHandler roleHandler = new RoleClaimsHandler(new AttributeMapLoader());
    roleHandler.setLdapConnectionFactory(connection);
    roleHandler.setPropertyFileLocation(propertyFileLoc);
    roleHandler.setUserBaseDn(userBaseDn);
    roleHandler.setLoginUserAttribute(loginUserAttribute);
    roleHandler.setMembershipUserAttribute(membershipUserAttribute);
    roleHandler.setObjectClass(objectClass);
    roleHandler.setMemberNameAttribute(memberNameAttribute);
    roleHandler.setGroupBaseDn(groupBaseDn);
    roleHandler.setBindUserDN(userDn);
    roleHandler.setBindUserCredentials(password);
    roleHandler.setOverrideCertDn(overrideCertDn);
    roleHandler.setBindMethod(bindMethod);
    roleHandler.setKerberosRealm(realm);
    roleHandler.setKdcAddress(kdcAddress);
    LOGGER.debug("Registering new role claims handler.");
    roleHandlerRegistration = registerClaimsHandler(roleHandler, roleHandlerRegistration);
  }

  /**
   * Registers a new Ldap-based Claims Handler.
   *
   * @param connection LdapTemplate used to query ldap for the roles.
   * @param propertyFileLoc File location of the property file.
   * @param userBaseDn Base DN to determine the roles.
   * @param userNameAttr Identifier that defines the user.
   */
  private void registerLdapClaimsHandler(
      ConnectionFactory connection,
      String propertyFileLoc,
      String userBaseDn,
      String userNameAttr,
      String userDn,
      String password,
      boolean overrideCertDn,
      String bindMethod,
      String realm,
      String kdcAddress) {
    LdapClaimsHandler ldapHandler = new LdapClaimsHandler(new AttributeMapLoader());
    ldapHandler.setLdapConnectionFactory(connection);
    ldapHandler.setPropertyFileLocation(propertyFileLoc);
    ldapHandler.setUserBaseDN(userBaseDn);
    ldapHandler.setUserNameAttribute(userNameAttr);
    ldapHandler.setBindUserDN(userDn);
    ldapHandler.setBindUserCredentials(password);
    ldapHandler.setOverrideCertDn(overrideCertDn);
    ldapHandler.setBindMethod(bindMethod);
    ldapHandler.setKerberosRealm(realm);
    ldapHandler.setKdcAddress(kdcAddress);
    LOGGER.debug("Registering new ldap claims handler.");
    ldapHandlerRegistration = registerClaimsHandler(ldapHandler, ldapHandlerRegistration);
  }

  /**
   * Utility method that registers a ClaimsHandler and returns the service registration.
   *
   * @param handler Handler that should be registered.
   * @param registration Previous registration, will be used to unregister if not null.
   * @return new registration for the service.
   */
  private ServiceRegistration<ClaimsHandler> registerClaimsHandler(
      ClaimsHandler handler, ServiceRegistration<ClaimsHandler> registration) {
    BundleContext context = getContext();
    if (null != context) {
      if (registration != null) {
        ClaimsHandler oldClaimsHandler = context.getService(registration.getReference());
        if (oldClaimsHandler instanceof RoleClaimsHandler) {
          ((RoleClaimsHandler) oldClaimsHandler).disconnect();
        } else if (oldClaimsHandler instanceof LdapClaimsHandler) {
          ((LdapClaimsHandler) oldClaimsHandler).disconnect();
        }
        registration.unregister();
      }

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

  public void setUrl(String... url) {
    LOGGER.trace("Setting url: {}", url);
    ldapProperties.put(URL, url);
  }

  public void setLoadBalancing(String loadBalancing) {
    LOGGER.trace("Setting loadBalancing: {}", loadBalancing);
    ldapProperties.put(LOAD_BALANCING, loadBalancing);
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
    LOGGER.trace("Setting password: [HIDDEN]");
    ldapProperties.put(PASSWORD, password);
  }

  public void setLoginUserAttribute(String loginUserAttribute) {
    LOGGER.trace("Setting userNameAttribute: {}", loginUserAttribute);
    ldapProperties.put(LOGIN_USER_ATTRIBUTE, loginUserAttribute);
  }

  public void setMembershipUserAttribute(String membershipUserAttribute) {
    LOGGER.trace("Setting userNameAttribute: {}", membershipUserAttribute);
    ldapProperties.put(MEMBER_USER_ATTRIBUTE, membershipUserAttribute);
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

  public void setBindMethod(String bindMethod) {
    LOGGER.trace("Setting bindMethod: {}", bindMethod);
    ldapProperties.put(BIND_METHOD, bindMethod);
  }

  public void setRealm(String realm) {
    LOGGER.trace("Setting realm: {}", realm);
    ldapProperties.put(REALM, realm);
  }

  public void setKdcAddress(String kdcAddress) {
    LOGGER.trace("Setting kdcAddress: {}", kdcAddress);
    ldapProperties.put(KDC_ADDRESS, kdcAddress);
  }

  public void setOverrideCertDn(boolean overrideCertDn) {
    LOGGER.trace("Setting propertyFileLocation: {}", overrideCertDn);
    ldapProperties.put(OVERRIDE_CERT_DN, overrideCertDn);
  }

  public void configure() {
    LOGGER.trace("configure method called - calling update");
    update(ldapProperties);
  }

  public static KeyManagerFactory createKeyManagerFactory(String keyStoreLoc, String keyStorePass)
      throws IOException {
    KeyManagerFactory kmf;
    try {
      // keystore stuff
      KeyStore keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));
      LOGGER.debug("keyStoreLoc = {}", keyStoreLoc);
      FileInputStream keyFIS = new FileInputStream(keyStoreLoc);
      try {
        LOGGER.debug("Loading keyStore");
        keyStore.load(keyFIS, keyStorePass.toCharArray());
      } catch (CertificateException e) {
        throw new IOException("Unable to load certificates from keystore. " + keyStoreLoc, e);
      } finally {
        IOUtils.closeQuietly(keyFIS);
      }
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, keyStorePass.toCharArray());
      LOGGER.debug("key manager factory initialized");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(
          "Problems creating SSL socket. Usually this is "
              + "referring to the certificate sent by the server not being trusted by the client.",
          e);
    } catch (UnrecoverableKeyException e) {
      throw new IOException("Unable to load keystore. " + keyStoreLoc, e);
    } catch (KeyStoreException e) {
      throw new IOException("Unable to read keystore. " + keyStoreLoc, e);
    }

    return kmf;
  }

  public static TrustManagerFactory createTrustManagerFactory(
      String trustStoreLoc, String trustStorePass) throws IOException {
    TrustManagerFactory tmf;
    try {
      // truststore stuff
      KeyStore trustStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));
      LOGGER.debug("trustStoreLoc = {}", trustStoreLoc);
      FileInputStream trustFIS = new FileInputStream(trustStoreLoc);
      try {
        LOGGER.debug("Loading trustStore");
        trustStore.load(trustFIS, trustStorePass.toCharArray());
      } catch (CertificateException e) {
        throw new IOException("Unable to load certificates from truststore. " + trustStoreLoc, e);
      } finally {
        IOUtils.closeQuietly(trustFIS);
      }

      tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(trustStore);
      LOGGER.debug("trust manager factory initialized");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(
          "Problems creating SSL socket. Usually this is "
              + "referring to the certificate sent by the server not being trusted by the client.",
          e);
    } catch (KeyStoreException e) {
      throw new IOException("Unable to read keystore. " + trustStoreLoc, e);
    }
    return tmf;
  }
}
