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

import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_PASSWORD;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_USERNAME;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_BASE_DN;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_FILTER;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_NAME_ATTRIBUTE;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_SEARCH_SUBTREE;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_FILTER;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_SEARCH_SUBTREE;

import ddf.security.common.audit.SecurityLogger;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.jaas.config.impl.Module;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers LDAP as a JAAS realm. */
public class LdapLoginConfig {

  public static final String LDAP_BIND_USER_DN = "ldapBindUserDn";

  public static final String LDAP_BIND_USER_PASS = "ldapBindUserPass";

  public static final String LDAP_URL = "ldapUrl";

  public static final String BIND_METHOD = "bindMethod";

  public static final String USER_BASE_DN = "userBaseDn";

  public static final String GROUP_BASE_DN = "groupBaseDn";

  public static final String START_TLS = "startTls";

  public static final String REALM = "realm";

  public static final String KDC_ADDRESS = "kdcAddress";

  private static final String SUFFICIENT_FLAG = "sufficient";

  private static final String LOGIN_USER_ATTRIBUTE = "loginUserAtttribute";

  private static final String MEMBER_USER_ATTRIBUTE = "membershipUserAttribute";

  private static final Logger LOGGER = LoggerFactory.getLogger(LdapLoginConfig.class);

  private static final String LDAP_MODULE = ddf.ldap.ldaplogin.SslLdapLoginModule.class.getName();

  private String id = "LDAP" + UUID.randomUUID().toString();

  private Map<String, Object> ldapProperties = new HashMap<>();

  private LdapService ldapService;

  private LDAPConnectionPool ldapConnectionPool;

  private SSLContext sslContext;

  private final BundleContext context;

  ServiceRegistration connectionPoolServiceRegistration;

  public LdapLoginConfig(final BundleContext context) {
    this.context = context;
  }

  /**
   * Update method that receives new properties.
   *
   * @param props Map of properties.
   */
  public void update(Map<String, ?> props) {
    if (props != null) {
      LOGGER.debug("Received an updated set of configurations for the LDAP Login Config.");

      if (connectionPoolServiceRegistration != null) {
        connectionPoolServiceRegistration.unregister();
      }
      if (ldapConnectionPool != null) {
        ldapConnectionPool.close();
      }
      try {
        LDAPConnectionFactory ldapConnectionFactory =
            createLdapConnectionFactory(
                (String) ldapProperties.get(LDAP_URL),
                Boolean.parseBoolean((String) ldapProperties.get(START_TLS)));
        ldapConnectionPool = new LDAPConnectionPool(ldapConnectionFactory, id);
      } catch (LdapException e) {
        LOGGER.error("Error creating ldap connection factory", e);
      }

      Dictionary<String, String> serviceProps = new Hashtable<>();
      serviceProps.put("id", id);

      LOGGER.debug("Registering LdapConnectionPool");
      connectionPoolServiceRegistration =
          context.registerService(
              LDAPConnectionPool.class.getName(), ldapConnectionPool, serviceProps);
      // create modules from the newly updated config
      Module ldapModule = createLdapModule(props);
      ldapService.update(ldapModule);
    }
  }

  protected LDAPConnectionFactory createLdapConnectionFactory(String url, Boolean startTls)
      throws LdapException {
    boolean useSsl = url.startsWith("ldaps");
    boolean useTls = !url.startsWith("ldaps") && startTls;

    Options lo = Options.defaultOptions();

    try {
      if (useSsl || useTls) {
        LOGGER.trace("Setting up secure LDAP connection.");
        initializeSslContext();
        lo.set(LDAPConnectionFactory.SSL_CONTEXT, getSslContext());
      } else {
        LOGGER.trace("Setting up insecure LDAP connection.");
      }
    } catch (GeneralSecurityException e) {
      LOGGER.info("Error encountered while configuring SSL. Secure connection will fail.", e);
    }

    lo.set(LDAPConnectionFactory.HEARTBEAT_TIMEOUT, new Duration(30L, TimeUnit.SECONDS));
    lo.set(LDAPConnectionFactory.HEARTBEAT_INTERVAL, new Duration(60L, TimeUnit.SECONDS));
    lo.set(LDAPConnectionFactory.CONNECT_TIMEOUT, new Duration(30L, TimeUnit.SECONDS));

    lo.set(LDAPConnectionFactory.SSL_USE_STARTTLS, useTls);
    lo.set(
        LDAPConnectionFactory.SSL_ENABLED_CIPHER_SUITES,
        Arrays.asList(System.getProperty("https.cipherSuites", "").split(",")));
    lo.set(
        LDAPConnectionFactory.SSL_ENABLED_PROTOCOLS,
        Arrays.asList(System.getProperty("https.protocols", "").split(",")));
    lo.set(
        LDAPConnectionFactory.TRANSPORT_PROVIDER_CLASS_LOADER,
        SslLdapLoginModule.class.getClassLoader());

    String host = url.substring(url.indexOf("://") + 3, url.lastIndexOf(':'));
    Integer port = useSsl ? 636 : 389;
    try {
      port = Integer.valueOf(url.substring(url.lastIndexOf(':') + 1));
    } catch (NumberFormatException ignore) {
      // ignore this error will try with default
    }

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

  private void initializeSslContext() throws NoSuchAlgorithmException {
    // Only set if null so tests can inject a context.
    if (getSslContext() == null) {
      setSslContext(SSLContext.getDefault());
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
    props.put(CONNECTION_USERNAME, properties.get(LDAP_BIND_USER_DN));
    props.put(CONNECTION_PASSWORD, properties.get(LDAP_BIND_USER_PASS));

    final Object userBaseDn = properties.get(USER_BASE_DN);
    props.put(SslLdapLoginModule.USER_BASE_DN, userBaseDn);

    final Object loginUserAttribute = properties.get(LOGIN_USER_ATTRIBUTE);
    final Object membershipUserAttribute = properties.get(MEMBER_USER_ATTRIBUTE);
    props.put(USER_FILTER, String.format("(%s=%%u)", loginUserAttribute));
    props.put(USER_SEARCH_SUBTREE, "true");
    props.put(ROLE_BASE_DN, properties.get(GROUP_BASE_DN));
    props.put(
        ROLE_FILTER, String.format("(member=%s=%%u,%s)", membershipUserAttribute, userBaseDn));
    props.put(ROLE_NAME_ATTRIBUTE, "cn");
    props.put(ROLE_SEARCH_SUBTREE, "true");
    props.put("authentication", "simple");
    props.put("ssl.protocol", "TLS");
    props.put("ssl.algorithm", "SunX509");
    props.put(BIND_METHOD, properties.get(BIND_METHOD));
    props.put(REALM, (properties.get(REALM) != null) ? properties.get(REALM) : "");
    props.put(
        KDC_ADDRESS, (properties.get(KDC_ADDRESS) != null) ? properties.get(KDC_ADDRESS) : "");
    props.put("connectionPoolId", id);
    if ("GSSAPI SASL".equals(properties.get(BIND_METHOD))
        && (StringUtils.isEmpty((String) properties.get(REALM))
            || StringUtils.isEmpty((String) properties.get(KDC_ADDRESS)))) {
      LOGGER.warn(
          "LDAP connection will fail. GSSAPI SASL connection requires Kerberos Realm and KDC Address.");
    }
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

  public void setStartTls(boolean startTls) {
    LOGGER.trace("Setting startTls: {}", startTls);
    ldapProperties.put(START_TLS, String.valueOf(startTls));
  }

  public void setStartTls(String startTls) {
    LOGGER.trace("Setting startTls: {}", startTls);
    ldapProperties.put(START_TLS, startTls);
  }

  public void setLoginUserAttribute(String loginUserAttribute) {
    LOGGER.trace("setLoginUserAttribute called: {}", loginUserAttribute);
    ldapProperties.put(LOGIN_USER_ATTRIBUTE, loginUserAttribute);
  }

  public void setMembershipUserAttribute(String membershipUserAttribute) {
    LOGGER.trace("setMemberUserAttribute called: {}", membershipUserAttribute);
    ldapProperties.put(MEMBER_USER_ATTRIBUTE, membershipUserAttribute);
  }

  public void setBindMethod(String bindMethod) {
    LOGGER.trace("setBindMethod: {}", bindMethod);
    ldapProperties.put(BIND_METHOD, bindMethod);
  }

  public void setRealm(String realm) {
    LOGGER.trace("setRealm: {}", realm);
    ldapProperties.put(REALM, realm);
  }

  public void setKdcAddress(String kdcAddress) {
    LOGGER.trace("setKdcAddress: {}", kdcAddress);
    ldapProperties.put(KDC_ADDRESS, kdcAddress);
  }

  public void configure() {
    LOGGER.trace("configure called - calling update");
    update(ldapProperties);
  }

  public void setSslContext(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  public SSLContext getSslContext() {
    return sslContext;
  }

  public void destroy(int arg) {
    LOGGER.trace("configure called - calling delete");
    ldapService.delete(id);
    if (ldapConnectionPool != null) {
      ldapConnectionPool.close();
    }
    if (connectionPoolServiceRegistration != null) {
      connectionPoolServiceRegistration.unregister();
    }
  }

  public void setLdapService(LdapService ldapService) {
    this.ldapService = ldapService;
  }
}
