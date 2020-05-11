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

import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_USERNAME_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_FILTER_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_FILTER_OPTIONS_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.audit.SecurityLogger;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.impl.Module;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Tests out LdapLoginConfig functionality. */
public class LdapLoginConfigTest {

  private BundleContext context;

  private ServiceRegistration<JaasRealm> jaasRealm;

  private static final String LDAP_URL_1 = "ldaps://ldap:1636";

  private static final String LDAP_URL_2 = "ldaps://ldap2:1636";

  /** Sets up a new context and JaasRealm before each test. */
  @Before
  public void setUp() {
    context = mock(BundleContext.class);
    jaasRealm = mock(ServiceRegistration.class);
    when(context.registerService(
            eq(JaasRealm.class), any(JaasRealm.class), ArgumentMatchers.<Dictionary<String, Object>>any()))
        .thenReturn(jaasRealm);
  }

  /**
   * Verifies that the JaasRealm is properly registered and that multiple ldap modules can be
   * created, updated and deleted.
   */
  @Test
  public void testLdapLoginConfig() {
    LdapService ldapService = new LdapService(context);

    LdapLoginConfig ldapConfigOne = createLdapConfig(ldapService, LDAP_URL_1);
    ldapConfigOne.configure();
    String configIdOne = ldapConfigOne.getId();

    // Verify the JaasRealm is registered.
    verify(context)
        .registerService(
            eq(JaasRealm.class), any(JaasRealm.class), ArgumentMatchers.<Dictionary<String, Object>>any());

    LdapLoginConfig ldapConfigTwo = createLdapConfig(ldapService, LDAP_URL_1, LDAP_URL_2);
    ldapConfigTwo.configure();
    String configIdTwo = ldapConfigTwo.getId();

    Map<String, String> ldapPropsOne = createLdapProperties("cn=user1");
    ldapConfigOne.update(ldapPropsOne);
    Map<String, String> ldapPropsTwo = createLdapProperties("cn=user2");
    ldapConfigTwo.update(ldapPropsTwo);

    List<Module> ldapModules = ldapService.getModules();
    for (Module module : ldapModules) {
      String moduleName = module.getName();
      String username = module.getOptions().getProperty(CONNECTION_USERNAME_OPTIONS_KEY);
      // Assert the ldap modules were updated.
      if (moduleName.equals(configIdOne)) {
        assertThat(username, is(equalTo("cn=user1")));
      } else if (moduleName.equals(configIdTwo)) {
        assertThat(username, is(equalTo("cn=user2")));
      } else {
        fail("The ldap modules did not update correctly.");
      }
    }

    // Verify the JaasRealm has only been registered once.
    verify(context, times(1))
        .registerService(
            eq(JaasRealm.class), any(JaasRealm.class), ArgumentMatchers.<Dictionary<String, Object>>any());

    // Destroy the first ldap module.
    ldapConfigOne.destroy(1);
    // Assert that the ldap module had already been removed.
    assertThat(ldapService.delete(configIdOne), is(equalTo(false)));

    // Assert the second ldap module is removed.
    assertThat(ldapService.delete(configIdTwo), is(equalTo(true)));
  }

  private LdapLoginConfig createLdapConfig(LdapService ldapService, String... ldapUrl) {
    LdapLoginConfig ldapConfig = new LdapLoginConfig(context);
    ldapConfig.setLdapService(ldapService);
    ldapConfig.setLdapBindUserDn("cn=admin");
    ldapConfig.setLdapBindUserPass("password");
    ldapConfig.setLdapUrl(ldapUrl);
    ldapConfig.setUserBaseDn("ou=users,dc=example,dc=com");
    ldapConfig.setGroupBaseDn("ou=groups,dc=example,dc=com");
    ldapConfig.setStartTls(false);
    ldapConfig.setLoginUserAttribute("uid");
    ldapConfig.setMembershipUserAttribute("uid");
    ldapConfig.setMemberNameAttribute("member");
    ldapConfig.setBindMethod("Simple");
    ldapConfig.setRealm("");
    ldapConfig.setKdcAddress("");
    ldapConfig.setSecurityLogger(mock(SecurityLogger.class));
    return ldapConfig;
  }

  private Map<String, String> createLdapProperties(String userDn) {
    Map<String, String> ldapProps = new HashMap<>();
    ldapProps.put(LdapLoginConfig.LDAP_BIND_USER_DN, userDn);
    ldapProps.put(LdapLoginConfig.LDAP_BIND_USER_PASS, "secret");
    ldapProps.put(LdapLoginConfig.LOGIN_USER_ATTRIBUTE, "uid");
    ldapProps.put(LdapLoginConfig.MEMBER_USER_ATTRIBUTE, "cn");
    ldapProps.put(LdapLoginConfig.MEMBER_NAME_ATTRIBUTE, "member");
    ldapProps.put(LdapLoginConfig.LDAP_URL, "ldaps://test-ldap:1636");
    ldapProps.put(LdapLoginConfig.USER_BASE_DN, "ou=users,dc=example,dc=com");
    ldapProps.put(LdapLoginConfig.GROUP_BASE_DN, "ou=groups,dc=example,dc=com");
    ldapProps.put(LdapLoginConfig.START_TLS, "false");
    ldapProps.put(LdapLoginConfig.BIND_METHOD, "Simple");
    ldapProps.put(LdapLoginConfig.KDC_ADDRESS, "");
    ldapProps.put(LdapLoginConfig.REALM, "");
    return ldapProps;
  }

  @Test
  public void testSetUserNameAttribute() {
    LdapService ldapService = new LdapService(context);
    LdapLoginConfig config = createLdapConfig(ldapService, LDAP_URL_1);
    config.setLoginUserAttribute("cn");

    config.configure();

    Properties properties = ldapService.getModules().get(0).getOptions();
    assertThat(properties.getProperty(USER_FILTER_OPTIONS_KEY), is("(cn=%u)"));
    assertThat(
        properties.getProperty(ROLE_FILTER_OPTIONS_KEY),
        is("(|(member=uid=%u,ou=users,dc=example,dc=com)(member=%fqdn)(member=cn=%u,%dn))"));
  }

  @Test
  public void testSetMembershipAttribute() {
    LdapService ldapService = new LdapService(context);
    LdapLoginConfig config = createLdapConfig(ldapService, LDAP_URL_1);
    config.setMembershipUserAttribute("cn");

    config.configure();

    Properties properties = ldapService.getModules().get(0).getOptions();
    assertThat(properties.getProperty(USER_FILTER_OPTIONS_KEY), is("(uid=%u)"));
    assertThat(
        properties.getProperty(ROLE_FILTER_OPTIONS_KEY),
        is("(|(member=cn=%u,ou=users,dc=example,dc=com)(member=%fqdn)(member=uid=%u,%dn))"));
  }

  @Test
  public void testSetMemberNameAttribute() {
    LdapService ldapService = new LdapService(context);
    LdapLoginConfig config = createLdapConfig(ldapService, LDAP_URL_1);
    config.setMemberNameAttribute("uniqueMember");

    config.configure();

    Properties properties = ldapService.getModules().get(0).getOptions();
    assertThat(properties.getProperty(USER_FILTER_OPTIONS_KEY), is("(uid=%u)"));
    assertThat(
        properties.getProperty(ROLE_FILTER_OPTIONS_KEY),
        is(
            "(|(uniqueMember=uid=%u,ou=users,dc=example,dc=com)(uniqueMember=%fqdn)(uniqueMember=uid=%u,%dn))"));
  }
}
