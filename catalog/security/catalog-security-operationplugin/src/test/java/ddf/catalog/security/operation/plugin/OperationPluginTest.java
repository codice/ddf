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
package ddf.catalog.security.operation.plugin;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OperationPluginTest {

  OperationPlugin plugin;

  Subject subject;

  @Before
  public void setup() {
    plugin = new OperationPlugin();

    AuthorizingRealm realm = mock(AuthorizingRealm.class);

    when(realm.getName()).thenReturn("mockRealm");
    when(realm.isPermitted(any(PrincipalCollection.class), any(Permission.class)))
        .then(makeDecision());

    Collection<Realm> realms = new ArrayList<Realm>();
    realms.add(realm);

    DefaultSecurityManager manager = new DefaultSecurityManager();
    manager.setRealms(realms);

    SimplePrincipalCollection principalCollection =
        new SimplePrincipalCollection(
            new Principal() {
              @Override
              public String getName() {
                return "testuser";
              }
            },
            realm.getName());

    subject = new MockSubject(manager, principalCollection);
  }

  private Answer<Boolean> makeDecision() {

    Map<String, List<String>> testRoleMap = new HashMap<String, List<String>>();
    List<String> testRoles = new ArrayList<String>();
    testRoles.add("A");
    testRoles.add("B");
    testRoleMap.put("Roles", testRoles);

    final KeyValueCollectionPermission testUserPermission =
        new KeyValueCollectionPermission(CollectionPermission.READ_ACTION, testRoleMap);

    return new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        Permission incomingPermission = (Permission) args[1];
        return testUserPermission.implies(incomingPermission);
      }
    };
  }

  @Test
  public void allowedRequestTest() throws Exception {
    testPluginWithRole("A");
  }

  @Test(expected = StopProcessingException.class)
  public void rejectedRequestTest() throws Exception {
    testPluginWithRole("Z");
  }

  @Test(expected = StopProcessingException.class)
  public void noSubjectRequestTest() throws Exception {
    Map<String, Serializable> properties = new HashMap<>();

    HashMap<String, Set<String>> perms = new HashMap<>();
    Set<String> roles = new HashSet<>();
    roles.add("A");
    perms.put("Roles", roles);
    properties.put(PolicyPlugin.OPERATION_SECURITY, perms);
    CreateRequestImpl request = new CreateRequestImpl(new ArrayList<>(), properties);

    plugin.processPreCreate(request);
  }

  @Test
  public void noPropertiesRequestTest() throws Exception {
    CreateRequestImpl request = new CreateRequestImpl(new ArrayList<>(), null);

    plugin.processPreCreate(request);
  }

  @Test
  public void noPolicyRequestTest() throws Exception {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    CreateRequestImpl request = new CreateRequestImpl(new ArrayList<>(), properties);

    plugin.processPreCreate(request);
  }

  private void testPluginWithRole(String role) throws Exception {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);

    HashMap<String, Set<String>> perms = new HashMap<>();
    Set<String> roles = new HashSet<>();
    roles.add(role);
    perms.put("Roles", roles);
    properties.put(PolicyPlugin.OPERATION_SECURITY, perms);
    CreateRequestImpl request = new CreateRequestImpl(new ArrayList<>(), properties);
    QueryRequestImpl queryRequest = new QueryRequestImpl(mock(Query.class), properties);
    UpdateRequestImpl updateRequest = new UpdateRequestImpl(new ArrayList<>(), "", properties);
    DeleteRequestImpl deleteRequest = new DeleteRequestImpl(new String[] {""}, properties);
    ResourceRequestById resourceRequestById = new ResourceRequestById("", properties);

    plugin.processPreCreate(request);

    plugin.processPreQuery(queryRequest);

    plugin.processPreUpdate(updateRequest, new HashMap<>());

    plugin.processPreDelete(deleteRequest);

    plugin.processPreResource(resourceRequestById);
  }

  private class MockSubject extends DelegatingSubject implements Subject {

    public MockSubject(SecurityManager manager, PrincipalCollection principals) {
      super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
    }

    @Override
    public boolean isGuest() {
      return false;
    }

    @Override
    public String getName() {
      return "Mock Subject";
    }
  }
}
