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

import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_PASSWORD_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_URL_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_USERNAME_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_BASE_DN_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_FILTER_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_NAME_ATTRIBUTE_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_SEARCH_SUBTREE_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.SSL_STARTTLS_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_FILTER_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_SEARCH_SUBTREE_OPTIONS_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.jaas.config.impl.Module;
import org.codice.ddf.configuration.PropertyResolver;
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

  private String id = "LDAP:" + UUID.randomUUID().toString();

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
    props.put(CONNECTION_USERNAME_OPTIONS_KEY, properties.get(LDAP_BIND_USER_DN));
    props.put(CONNECTION_PASSWORD_OPTIONS_KEY, properties.get(LDAP_BIND_USER_PASS));
    props.put(
        CONNECTION_URL_OPTIONS_KEY,
        new PropertyResolver((String) properties.get(LDAP_URL)).toString());

    final Object userBaseDn = properties.get(USER_BASE_DN);
    props.put(SslLdapLoginModule.USER_BASE_DN_OPTIONS_KEY, userBaseDn);

    final Object loginUserAttribute = properties.get(LOGIN_USER_ATTRIBUTE);
    final Object membershipUserAttribute = properties.get(MEMBER_USER_ATTRIBUTE);
    props.put(USER_FILTER_OPTIONS_KEY, String.format("(%s=%%u)", loginUserAttribute));
    props.put(USER_SEARCH_SUBTREE_OPTIONS_KEY, "true");
    props.put(ROLE_BASE_DN_OPTIONS_KEY, properties.get(GROUP_BASE_DN));
    props.put(
        ROLE_FILTER_OPTIONS_KEY,
        String.format("(member=%s=%%u,%s)", membershipUserAttribute, userBaseDn));
    props.put(ROLE_NAME_ATTRIBUTE_OPTIONS_KEY, "cn");
    props.put(ROLE_SEARCH_SUBTREE_OPTIONS_KEY, "true");
    props.put("authentication", "simple");
    props.put("ssl.protocol", "TLS");
    props.put("ssl.algorithm", "SunX509");
    props.put(SSL_STARTTLS_OPTIONS_KEY, properties.get(START_TLS));
    props.put(BIND_METHOD, properties.get(BIND_METHOD));
    props.put(REALM, (properties.get(REALM) != null) ? properties.get(REALM) : "");
    props.put(
        KDC_ADDRESS, (properties.get(KDC_ADDRESS) != null) ? properties.get(KDC_ADDRESS) : "");
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

  public void destroy(int arg) {
    LOGGER.trace("configure called - calling delete");
    ldapService.delete(id);
  }

  public void setLdapService(LdapService ldapService) {
    this.ldapService = ldapService;
  }
}
