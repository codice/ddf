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
package org.codice.ddf.security.policy.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import ddf.security.permission.CollectionPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.Permission;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;
import org.codice.ddf.security.policy.context.attributes.DefaultContextAttributeMapping;
import org.codice.ddf.security.policy.context.impl.Policy;
import org.codice.ddf.security.policy.context.impl.PolicyManager;
import org.junit.Before;
import org.junit.Test;

/** Test for PolicyManager */
public class PolicyManagerTest {

  private static final String WEB_AUTH_TYPES = "webAuthenticationTypes";

  private static final String ENDPOINT_AUTH_TYPES = "endpointAuthenticationTypes";

  private static final String REQ_ATTRS = "requiredAttributes";

  private static final String GUEST_ACCESS = "guestAccess";

  private static final String SESSION_ACCESS = "sessionAccess";

  private static final List<String> DEFAULT_AUTH_TYPES = Arrays.asList("SAML", "GUEST");

  private PolicyManager manager;

  private PolicyManager rollBackTestManager;

  private String[] rollBackReqAttrValues = {
    "/=",
    "/A={A=a}",
    "/testContext=",
    "/1/2/3/testContext2=",
    "/A/B/C/testContext3=",
    "/A/B/C/testContext4=",
    "/A/B/C/testContext8={AbcTestContext8=abcTestContext8}"
  };

  private final List<String> expectedRollBackWebAuthTypes = DEFAULT_AUTH_TYPES;

  private final List<String> expectedRollBackEndpointAuthTypes = Arrays.asList("PKI", "BASIC");

  private final Map<String, List<String>> expectedRollBackReqAttrs =
      new ImmutableMap.Builder<String, List<String>>()
          .put("/testContext5", Arrays.asList(new String[] {}))
          .put("/1/2/3/testContext6", Arrays.asList(new String[] {}))
          .put("/A/B/C/testContext7", Collections.singletonList("A"))
          .put("/A/B/C/testContext8", Collections.singletonList("AbcTestContext8"))
          .build();

  private final ContextAttributeMapping[] attributes = {
    new DefaultContextAttributeMapping(null, "a", "a"),
    new DefaultContextAttributeMapping(null, "b", "b"),
    new DefaultContextAttributeMapping(null, "c", "c")
  };

  private final Map<String, List<ContextAttributeMapping>> simpleAttributeMap =
      new ImmutableMap.Builder<String, List<ContextAttributeMapping>>()
          .put("/a", Collections.singletonList(attributes[0]))
          .put("/a/b", Collections.singletonList(attributes[1]))
          .put("/a/b/c", Collections.singletonList(attributes[2]))
          .build();

  private final Map<String, List<ContextAttributeMapping>> complexAttributeMap =
      new ImmutableMap.Builder<String, List<ContextAttributeMapping>>()
          .put("/x", Collections.singletonList(attributes[0]))
          .put("/x/y/z", Collections.singletonList(attributes[1]))
          .build();

  @Before
  public void setup() {
    manager = new PolicyManager();
    manager.setTraversalDepth(10);
    manager.setGuestAccess(true);
    manager.setSessionAccess(true);
    manager.setContextPolicy("/", new Policy("/", new ArrayList<>(), null));
    manager.setContextPolicy("/search", new Policy("/search", new ArrayList<>(), null));
    manager.setContextPolicy("/admin", new Policy("/admin", new ArrayList<>(), null));
    manager.setContextPolicy(
        "/search/standard", new Policy("/search/standard", new ArrayList<>(), null));
    manager.setContextPolicy(
        "/search/simple", new Policy("/search/simple", new ArrayList<>(), null));
    manager.setContextPolicy(
        "/services/aaaaaa", new Policy("/services/aaaaaa", new ArrayList<>(), null));
    manager.setContextPolicy("/aaa", new Policy("/aaa", new ArrayList<>(), null));
    manager.setContextPolicy("/aaa/aaa", new Policy("/aaa/aaa", new ArrayList<>(), null));
    manager.setContextPolicy("/foo/bar", new Policy("/foo/bar", new ArrayList<>(), null));
    manager.setContextPolicy("/1/2", new Policy("/1/2", new ArrayList<>(), null));
    manager.setContextPolicy(
        "/1/2/3/4/5/6/7/8/9/10/11/12/13/14",
        new Policy("/1/2/3/4/5/6/7/8/9/10/11/12/13/14", new ArrayList<>(), null));

    for (Map.Entry<String, List<ContextAttributeMapping>> entry : simpleAttributeMap.entrySet()) {
      manager.setContextPolicy(
          entry.getKey(), new Policy(entry.getKey(), new ArrayList<>(), entry.getValue()));
    }

    for (Map.Entry<String, List<ContextAttributeMapping>> entry : complexAttributeMap.entrySet()) {
      manager.setContextPolicy(
          entry.getKey(), new Policy(entry.getKey(), new ArrayList<>(), entry.getValue()));
    }

    // Can't use Collections.singletonList because the context policy manager must be able to change
    // the passed in list
    manager.setWhiteListContexts(Arrays.asList("/foo"));

    Map<String, Object> contextPolicies = new HashMap<>();
    contextPolicies.put(WEB_AUTH_TYPES, "SAML|GUEST");
    contextPolicies.put(ENDPOINT_AUTH_TYPES, "PKI|BASIC");
    contextPolicies.put(REQ_ATTRS, rollBackReqAttrValues);
    contextPolicies.put(GUEST_ACCESS, true);
    contextPolicies.put(SESSION_ACCESS, true);

    rollBackTestManager = new PolicyManager();
    rollBackTestManager.setPolicies(contextPolicies);
  }

  @Test
  public void testSimpleAttributeMappings() {

    for (Map.Entry<String, List<ContextAttributeMapping>> entry : simpleAttributeMap.entrySet()) {
      ContextPolicy policy = manager.getContextPolicy(entry.getKey());
      CollectionPermission permission = policy.getAllowedAttributePermissions();

      assertThat(permission.implies(entry.getValue().get(0).getAttributePermission()), is(true));
    }
  }

  @Test
  public void testComplexPaths() {
    CollectionPermission rootPermissions =
        manager.getContextPolicy("/x").getAllowedAttributePermissions();
    CollectionPermission noPermissions =
        manager.getContextPolicy("/x/y").getAllowedAttributePermissions();
    CollectionPermission lastPermission =
        manager.getContextPolicy("/x/y/z").getAllowedAttributePermissions();

    assertThat(noPermissions.implies(rootPermissions), is(true));
    assertThat(rootPermissions.implies(lastPermission), is(false));
    assertThat(lastPermission.implies(noPermissions), is(false));
  }

  @Test
  public void testBadTraversal() {
    // test that we can still resolve policies for paths larger than the limit
    ContextPolicy contextPolicy = manager.getContextPolicy("/1/2/3/4/5/6/7/8/9/10/11/12/13/14/15");
    assertThat("/1/2/3/4/5/6/7/8/9/10/11/12/13/14", is(contextPolicy.getContextPath()));

    // test that extra /s are removed from the end
    ContextPolicy contextPolicy1 =
        manager.getContextPolicy("/1/2/3/4/5/6/7/8/9/10/11/12/13/14////////////////");
    assertThat("/1/2/3/4/5/6/7/8/9/10/11/12/13/14", is(contextPolicy1.getContextPath()));

    // test that all slashes resolves to just /
    ContextPolicy contextPolicy2 =
        manager.getContextPolicy(
            "///////////////////////////////////////////////////////////////////////////");
    assertThat("/", is(contextPolicy2.getContextPath()));

    // test that we can remove slashes within paths and still resolve a policy
    ContextPolicy contextPolicy3 =
        manager.getContextPolicy(
            "/1/2/3/////////////////////////////////////4/5//6/7////////////////");
    assertThat("/1/2", is(contextPolicy3.getContextPath()));

    // test same as above but with a path that is too long so it resolves to /
    ContextPolicy contextPolicy4 =
        manager.getContextPolicy(
            "/1/2/3////////4/5//////////6/7/8//////////9/10//////////11/12/13/14////////////////");
    assertThat("/", is(contextPolicy4.getContextPath()));

    // test two slashes
    ContextPolicy contextPolicy5 = manager.getContextPolicy("//");
    assertThat("/", is(contextPolicy5.getContextPath()));

    // test one slash
    ContextPolicy contextPolicy6 = manager.getContextPolicy("/");
    assertThat("/", is(contextPolicy6.getContextPath()));
  }

  /**
   * Tests context rollbacks to the specified authorization types / <- /testContext - single
   * rollback to default / <- /1/2/3/testContext2 - several rollbacks to default /A <-
   * /A/B/C/testContext3 - several rollback to parent /A/B/C/testContext4 <- /A/B/C/testContext4 -
   * no rollback
   */
  @Test
  public void testAuthTypesRollBack() {
    for (ContextPolicy policy : rollBackTestManager.getPolicyStore().values()) {
      for (String authType : policy.getAuthenticationMethods()) {
        if (policy.getContextPath().startsWith("/services")) {
          assertThat(expectedRollBackEndpointAuthTypes.contains(authType), is(true));
        } else {
          assertThat(expectedRollBackWebAuthTypes.contains(authType), is(true));
        }
      }
    }
  }

  /**
   * Tests context rollbacks to the specified required attributes / <- /testContext5 - single
   * rollback to default / <- /1/2/3/testContext6 - several rollbacks to default /A <-
   * /A/B/C/testContext7 - several rollback to parent /A/B/C/testContext8 <- /A/B/C/testContext8 -
   * no rollback
   */
  @Test
  public void testReqAttrRollBack() {
    for (String contextPath : expectedRollBackReqAttrs.keySet()) {
      for (String authType :
          rollBackTestManager.getContextPolicy(contextPath).getAllowedAttributeNames()) {
        assertThat(expectedRollBackReqAttrs.get(contextPath).contains(authType), is(true));
      }
    }
  }

  @Test
  public void testInvalidEntry() {
    assertThat(
        rollBackTestManager.getContextPolicy("invalidContextPathEntry").getAllowedAttributeNames(),
        is(Arrays.asList(new String[] {})));
    assertThat(
        rollBackTestManager.getContextPolicy("invalidContextPathEntry").getAuthenticationMethods(),
        is(DEFAULT_AUTH_TYPES));
  }

  @Test
  public void testFindContextPaths() {
    ContextPolicy policy = manager.getContextPolicy("/search/standard/user");

    assertThat("/search/standard", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/search/standard");

    assertThat("/search/standard", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/search/endpoint");

    assertThat("/search", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/random/other/endpoint");

    assertThat("/", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/aaaaab");

    assertThat("/", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/aaa/aab");

    assertThat("/aaa", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/");

    assertThat("/", is(policy.getContextPath()));

    policy = manager.getContextPolicy("blah");

    assertThat("/", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/foo/bar");

    assertThat("/foo/bar", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/foo/bar/foobar");

    assertThat("/foo/bar", is(policy.getContextPath()));

    policy = manager.getContextPolicy("/foo");

    assertThat(policy, is(nullValue()));

    assertThat(manager.isWhiteListed("/foo"), is(true));

    assertThat(manager.isWhiteListed("/foo/bar"), is(false));
  }

  @Test
  public void testConfiguration() {
    Map<String, Object> properties = new HashMap<>();

    String webAuthTypes = "SAML|BASIC";
    String endAuthTypes = "PKI";
    String[] requiredAttributes =
        new String[] {
          "/={}", "/blah=", "/search={role=user;control=foo;control=bar}", "/admin={role=admin}"
        };

    properties.put(WEB_AUTH_TYPES, webAuthTypes);
    properties.put(ENDPOINT_AUTH_TYPES, endAuthTypes);
    properties.put("requiredAttributes", requiredAttributes);
    properties.put(GUEST_ACCESS, true);
    properties.put(SESSION_ACCESS, true);
    manager.setPolicies(properties);

    testAllPolicies();
  }

  @Test
  public void testSetPropertiesIgnoresNullMap() {
    String webAuthTypes = "SAML|BASIC";
    String endAuthTypes = "PKI";

    String[] requiredAttributes =
        new String[] {
          "/={}", "/search={role=user;control=foo;control=bar}", "/admin={role=admin|supervisor}"
        };
    manager.setWebAuthenticationTypes(webAuthTypes);
    manager.setEndpointAuthenticationTypes(endAuthTypes);
    manager.setRequiredAttributes(Arrays.asList(requiredAttributes));
    manager.setGuestAccess(true);
    manager.setSessionAccess(true);
    manager.configure();
    manager.setPolicies(null);
    testAllPolicies();
  }

  @Test
  public void testWhiteListWithProperties() {
    System.setProperty("org.codice.security.policy.context.test.bar", "/baz");
    manager.setWhiteListContexts(
        Arrays.asList("/foo", "${org.codice.security.policy.context.test.bar}"));
    assertThat(manager.getWhiteListContexts().contains("/baz"), is(true));
  }

  @Test
  public void testFileBasedConfig() {
    manager.setPolicyFilePath("web-context-policy-config.properties");

    Map<String, Object> properties = new HashMap<>();

    String authTypes = "OIDC";
    String[] requiredAttributes =
        new String[] {
          "/={}", "/blah=", "/search={role=user;control=foo;control=bar}", "/admin={role=admin}"
        };

    properties.put(WEB_AUTH_TYPES, authTypes);
    properties.put(ENDPOINT_AUTH_TYPES, authTypes);
    properties.put("requiredAttributes", requiredAttributes);
    properties.put(GUEST_ACCESS, true);
    properties.put(SESSION_ACCESS, true);
    manager.setPolicies(properties);

    testAllPolicies();
    testAdditionalPolicies();

    // test OIDC auth type is ignored
    assertTrue(
        manager
            .getAllContextPolicies()
            .stream()
            .flatMap(contextPolicy -> contextPolicy.getAuthenticationMethods().stream())
            .noneMatch(auth -> auth.equals("OIDC")));
  }

  private void testAllPolicies() {
    // check search policy
    ContextPolicy policy = manager.getContextPolicy("/search");
    assertThat("/search", is(policy.getContextPath()));
    Iterator<String> authIter = policy.getAuthenticationMethods().iterator();
    int i = 0;
    while (authIter.hasNext()) {
      if (i == 0) {
        assertThat("SAML", is(authIter.next()));
      } else if (i == 1) {
        assertThat("BASIC", is(authIter.next()));
      } else if (i == 2) {
        assertThat("GUEST", is(authIter.next()));
      }

      i++;
    }

    List<Permission> permissionList = policy.getAllowedAttributePermissions().getPermissionList();
    assertThat("role : user", is(permissionList.get(0).toString()));
    assertThat("control : foo", is(permissionList.get(1).toString()));
    assertThat("control : bar", is(permissionList.get(2).toString()));

    // check admin policy
    policy = manager.getContextPolicy("/admin");
    assertThat("/admin", is(policy.getContextPath()));
    authIter = policy.getAuthenticationMethods().iterator();
    i = 0;
    while (authIter.hasNext()) {
      if (i == 0) {
        assertThat("SAML", is(authIter.next()));
      } else if (i == 1) {
        assertThat("BASIC", is(authIter.next()));
      }

      i++;
    }

    // check foo policy
    policy = manager.getContextPolicy("/services");
    assertThat("/services", is(policy.getContextPath()));
    authIter = policy.getAuthenticationMethods().iterator();
    i = 0;
    while (authIter.hasNext()) {
      if (i == 0) {
        assertThat("PKI", is(authIter.next()));
      }

      i++;
    }

    // make sure some random context points to /
    policy = manager.getContextPolicy("/random");
    assertThat("/", is(policy.getContextPath()));
    authIter = policy.getAuthenticationMethods().iterator();
    i = 0;
    while (authIter.hasNext()) {
      if (i == 0) {
        assertThat("SAML", is(authIter.next()));
      } else if (i == 1) {
        assertThat("BASIC", is(authIter.next()));
      }

      i++;
    }
  }

  private void testAdditionalPolicies() {
    // check foo policy
    ContextPolicy policy = manager.getContextPolicy("/foo");
    assertThat("/foo", is(policy.getContextPath()));
    Iterator authIter = policy.getAuthenticationMethods().iterator();
    int i = 0;
    while (authIter.hasNext()) {
      if (i == 0) {
        assertThat("BASIC", is(authIter.next()));
      }

      i++;
    }

    // check unprotected contexts
    policy = manager.getContextPolicy("/unprotected");
    assertThat("/unprotected", is(policy.getContextPath()));
    authIter = policy.getAuthenticationMethods().iterator();
    assertThat(false, is(authIter.hasNext()));

    policy = manager.getContextPolicy("/unprotected2");
    assertThat("/unprotected2", is(policy.getContextPath()));
    authIter = policy.getAuthenticationMethods().iterator();
    assertThat(authIter.hasNext(), is(false));
  }
}
