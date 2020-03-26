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
package ddf.security.pdp.realm.test;

import static org.mockito.Mockito.when;

import ddf.security.pdp.realm.AuthzRealm;
import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import ddf.security.policy.extension.PolicyExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import junit.framework.Assert;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/** User: tustisos Date: 3/20/13 Time: 9:35 AM */
public class AuthzRealmTest {
  AuthzRealm testRealm;

  List<Permission> permissionList;

  HashMap<String, List<String>> security;

  PrincipalCollection mockSubjectPrincipal;

  @Before
  public void setup() throws PdpException {
    String ruleClaim = "FineAccessControls";
    String countryClaim = "CountryOfAffiliation";

    // setup the subject permissions
    List<Permission> permissions = new ArrayList<>();
    KeyValuePermission rulePermission = new KeyValuePermissionImpl(ruleClaim);
    rulePermission.addValue("A");
    rulePermission.addValue("B");
    permissions.add(rulePermission);
    KeyValuePermission countryPermission = new KeyValuePermissionImpl(countryClaim);
    countryPermission.addValue("AUS");
    permissions.add(countryPermission);

    SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    authorizationInfo.addObjectPermission(rulePermission);
    authorizationInfo.addObjectPermission(countryPermission);
    authorizationInfo.addObjectPermission(
        new KeyValuePermissionImpl("role", Arrays.asList("admin")));
    authorizationInfo.addRole("admin");
    authorizationInfo.addStringPermission("wild");

    testRealm =
        new AuthzRealm("src/test/resources/policies", new XmlParser()) {
          @Override
          public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
            return authorizationInfo;
          }
        };

    mockSubjectPrincipal = Mockito.mock(PrincipalCollection.class);
    when(mockSubjectPrincipal.getPrimaryPrincipal()).thenReturn("user");

    // setup the resource permissions
    permissionList = new ArrayList<>();
    security = new HashMap<>();
    security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
    security.put("rule", Arrays.asList("A", "B"));
    testRealm.setMatchOneMappings(Arrays.asList("CountryOfAffiliation=country"));
    testRealm.setMatchAllMappings(Arrays.asList("FineAccessControls=rule"));
    testRealm.setRolePermissionResolver(
        roleString -> Arrays.asList(new KeyValuePermissionImpl("role", Arrays.asList(roleString))));
  }

  @Test
  public void testIsPermitted() {
    permissionList.clear();
    KeyValueCollectionPermission kvcp = new KeyValueCollectionPermissionImpl("action", security);
    permissionList.add(kvcp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsKvpPermitted() {
    permissionList.clear();
    KeyValuePermission kvp = new KeyValuePermissionImpl("role", Arrays.asList("admin"));
    permissionList.add(kvp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsWildcardPermitted() {
    permissionList.clear();
    WildcardPermission kvp = new WildcardPermission("role:admin");
    permissionList.add(kvp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsWildcardNotPermitted() {
    permissionList.clear();
    WildcardPermission kvp = new WildcardPermission("role:secretary");
    permissionList.add(kvp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(false, permitted);
    }
  }

  @Test
  public void testIsPermittedAllSingle() {
    permissionList.clear();
    KeyValuePermission kvp = new KeyValuePermissionImpl("rule", Arrays.asList("A", "B"));
    permissionList.add(kvp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsPermittedOneSingle() {
    permissionList.clear();
    KeyValuePermission kvp =
        new KeyValuePermissionImpl("country", Arrays.asList("AUS", "CAN", "GBR"));
    permissionList.add(kvp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsPermittedOneMultiple() throws PdpException {
    permissionList.clear();
    KeyValuePermission kvp =
        new KeyValuePermissionImpl("country", Arrays.asList("AUS", "CAN", "GBR"));
    permissionList.add(kvp);

    String ruleClaim = "FineAccessControls";
    String countryClaim = "CountryOfAffiliation";

    // create a new user here with multiple country permissions to test
    List<Permission> permissions = new ArrayList<Permission>();
    KeyValuePermission rulePermission = new KeyValuePermissionImpl(ruleClaim);
    rulePermission.addValue("A");
    rulePermission.addValue("B");
    permissions.add(rulePermission);
    KeyValuePermission countryPermission = new KeyValuePermissionImpl(countryClaim);
    countryPermission.addValue("USA");
    countryPermission.addValue("AUS");
    permissions.add(countryPermission);

    SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    authorizationInfo.addObjectPermission(rulePermission);
    authorizationInfo.addObjectPermission(countryPermission);
    authorizationInfo.addRole("admin");

    AuthzRealm testRealm =
        new AuthzRealm("src/test/resources/policies", new XmlParser()) {
          @Override
          public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
            return authorizationInfo;
          }
        };
    testRealm.setMatchOneMappings(Arrays.asList("CountryOfAffiliation=country"));
    testRealm.setMatchAllMappings(Arrays.asList("FineAccessControls=rule"));
    testRealm.setRolePermissionResolver(
        roleString -> Arrays.asList(new KeyValuePermissionImpl("role", Arrays.asList(roleString))));

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testIsNotPermitted() {

    HashMap<String, List<String>> security = new HashMap<String, List<String>>();
    security.put("country", Arrays.asList("AUS", "CAN", "GBR"));
    security.put("country2", Arrays.asList("CAN", "GBR"));
    security.put("rule", Arrays.asList("A", "B"));
    security.put("rule2", Arrays.asList("A", "B", "C"));
    KeyValueCollectionPermission kvcp = new KeyValueCollectionPermissionImpl("action", security);
    permissionList.clear();
    permissionList.add(kvcp);

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(false, permitted);
    }
  }

  @Test
  public void testBadPolicyExtension() {
    permissionList.clear();
    KeyValuePermission kvp =
        new KeyValuePermissionImpl("country", Arrays.asList("AUS", "CAN", "GBR"));
    permissionList.add(kvp);

    testRealm.addPolicyExtension(
        new PolicyExtension() {
          @Override
          public KeyValueCollectionPermission isPermittedMatchAll(
              CollectionPermission subjectAllCollection,
              KeyValueCollectionPermission matchAllCollection,
              KeyValueCollectionPermission allPermissionsCollection) {
            throw new NullPointerException();
          }

          @Override
          public KeyValueCollectionPermission isPermittedMatchOne(
              CollectionPermission subjectAllCollection,
              KeyValueCollectionPermission matchOneCollection,
              KeyValueCollectionPermission allPermissionsCollection) {
            throw new NullPointerException();
          }
        });

    boolean[] permittedArray = testRealm.isPermitted(mockSubjectPrincipal, permissionList);

    for (boolean permitted : permittedArray) {
      Assert.assertEquals(true, permitted);
    }
  }

  @Test
  public void testAddRemoveSetPolicyExtension() {
    PolicyExtension policyExtension =
        new PolicyExtension() {
          @Override
          public KeyValueCollectionPermission isPermittedMatchAll(
              CollectionPermission subjectAllCollection,
              KeyValueCollectionPermission matchAllCollection,
              KeyValueCollectionPermission allPermissionsCollection) {
            throw new NullPointerException();
          }

          @Override
          public KeyValueCollectionPermission isPermittedMatchOne(
              CollectionPermission subjectAllCollection,
              KeyValueCollectionPermission matchOneCollection,
              KeyValueCollectionPermission allPermissionsCollection) {
            throw new NullPointerException();
          }
        };
    testRealm.addPolicyExtension(policyExtension);

    testRealm.removePolicyExtension(policyExtension);

    testRealm.setPolicyExtensions(Arrays.asList(policyExtension));
  }
}
