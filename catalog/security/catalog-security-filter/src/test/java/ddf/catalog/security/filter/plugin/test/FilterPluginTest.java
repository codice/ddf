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
package ddf.catalog.security.filter.plugin.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.Resource;
import ddf.catalog.security.filter.plugin.FilterPlugin;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class FilterPluginTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterPluginTest.class);

  FilterPlugin plugin;

  QueryResponseImpl incomingResponse;

  ResourceResponseImpl resourceResponse;

  CreateRequestImpl createRequest;

  CreateRequestImpl badCreateRequest;

  UpdateRequestImpl updateRequest;

  DeleteResponse deleteResponse;

  DeleteResponse badDeleteResponse;

  @Before
  public void setup() {
    AuthorizingRealm realm = mock(AuthorizingRealm.class);

    when(realm.getName()).thenReturn("mockRealm");
    when(realm.isPermitted(any(PrincipalCollection.class), any(Permission.class)))
        .then(makeDecision());

    Collection<org.apache.shiro.realm.Realm> realms = new ArrayList<>();
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
    Subject systemSubject = new MockSubject(manager, principalCollection);

    plugin =
        new FilterPlugin() {
          @Override
          protected Subject getSystemSubject() {
            return systemSubject;
          }
        };
    QueryRequestImpl request = getSampleRequest();
    Map<String, Serializable> properties = new HashMap<>();

    Subject subject = new MockSubject(manager, principalCollection);
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    request.setProperties(properties);

    incomingResponse = new QueryResponseImpl(request);
    ResourceRequest resourceRequest = mock(ResourceRequest.class);
    when(resourceRequest.getProperties()).thenReturn(properties);
    resourceResponse = new ResourceResponseImpl(resourceRequest, mock(Resource.class));
    resourceResponse.setProperties(properties);

    DeleteRequest deleteRequest = mock(DeleteRequest.class);
    when(deleteRequest.getProperties()).thenReturn(properties);
    List<Metacard> deletedMetacards = new ArrayList<>();
    deletedMetacards.add(getExactRolesMetacard());
    deleteResponse = new DeleteResponseImpl(deleteRequest, properties, deletedMetacards);

    List<Metacard> badDeletedMetacards = new ArrayList<>();
    badDeletedMetacards.add(getMoreRolesMetacard());
    badDeleteResponse = new DeleteResponseImpl(deleteRequest, properties, badDeletedMetacards);

    createRequest = new CreateRequestImpl(getExactRolesMetacard());
    createRequest.setProperties(properties);

    badCreateRequest = new CreateRequestImpl(getMoreRolesMetacard());
    badCreateRequest.setProperties(properties);

    updateRequest = new UpdateRequestImpl(getExactRolesMetacard().getId(), getExactRolesMetacard());
    updateRequest.setProperties(properties);

    ResultImpl result1 = new ResultImpl(getMoreRolesMetacard());
    ResultImpl result2 = new ResultImpl(getMissingRolesMetacard());
    ResultImpl result3 = new ResultImpl(getExactRolesMetacard());
    ResultImpl result4 = new ResultImpl(getNoRolesMetacard());
    ResultImpl result5 = new ResultImpl(getNoSecurityAttributeMetacard());
    incomingResponse.addResult(result1, false);
    incomingResponse.addResult(result2, false);
    incomingResponse.addResult(result3, false);
    incomingResponse.addResult(result4, false);
    incomingResponse.addResult(result5, true);
  }

  public Answer<Boolean> makeDecision() {

    Map<String, List<String>> testRoleMap = new HashMap<>();
    List<String> testRoles = new ArrayList<>();
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
  public void testPluginFilter() {

    try {
      QueryResponse response = plugin.processPostQuery(incomingResponse);
      verifyFilterResponse(response);
    } catch (StopProcessingException e) {
      LOGGER.error("Stopped processing the redaction plugin", e);
    }
  }

  @Test
  public void testPluginFilterNoStrategies() {
    plugin = new FilterPlugin();
    try {
      QueryResponse response = plugin.processPostQuery(incomingResponse);
      verifyFilterResponse(response);
    } catch (StopProcessingException e) {
      LOGGER.error("Stopped processing the redaction plugin", e);
    }
  }

  @Test
  public void testPluginFilterResourceGood() throws StopProcessingException {
    plugin.processPostResource(resourceResponse, getExactRolesMetacard());
  }

  @Test
  public void testPluginFilterResourceNoStrategiesGood() throws StopProcessingException {
    plugin = new FilterPlugin();
    plugin.processPostResource(resourceResponse, getExactRolesMetacard());
  }

  @Test(expected = StopProcessingException.class)
  public void testPluginFilterResourceBad() throws StopProcessingException {
    plugin.processPostResource(resourceResponse, getMoreRolesMetacard());
  }

  @Test
  public void testPluginFilterDeleteBad() throws StopProcessingException {
    DeleteResponse response = plugin.processPostDelete(badDeleteResponse);
    assertThat(response.getDeletedMetacards().size(), is(0));
  }

  @Test(expected = StopProcessingException.class)
  public void testPluginFilterDeleteNoRequest() throws StopProcessingException {
    plugin.processPostDelete(mock(DeleteResponse.class));
  }

  @Test
  public void testPluginFilterDeleteGood() throws StopProcessingException {
    plugin.processPostDelete(deleteResponse);
  }

  @Test(expected = StopProcessingException.class)
  public void testPluginFilterResourceNoStrategiesBad() throws StopProcessingException {
    plugin = new FilterPlugin();
    plugin.processPostResource(resourceResponse, getMoreRolesMetacard());
  }

  @Test(expected = StopProcessingException.class)
  public void testNoSubject() throws Exception {
    QueryResponseImpl response = new QueryResponseImpl(getSampleRequest());
    plugin.processPostQuery(response);
    fail("Plugin should have thrown exception when no subject was sent in.");
  }

  @Test(expected = StopProcessingException.class)
  public void testNoSubjectResource() throws Exception {
    ResourceResponseImpl response = new ResourceResponseImpl(mock(Resource.class));
    plugin.processPostResource(response, mock(Metacard.class));
    fail("Plugin should have thrown exception when no subject was sent in.");
  }

  @Test(expected = StopProcessingException.class)
  public void testNoRequestSubject() throws Exception {
    QueryResponseImpl response = new QueryResponseImpl(null);
    plugin.processPostQuery(response);
    fail("Plugin should have thrown exception when no subject was sent in.");
  }

  @Test(expected = StopProcessingException.class)
  public void testNoRequestSubjectNoStrategies() throws Exception {
    QueryResponseImpl response = new QueryResponseImpl(null);
    plugin = new FilterPlugin();
    plugin.processPostQuery(response);
    fail("Plugin should have thrown exception when no subject was sent in.");
  }

  @Test(expected = StopProcessingException.class)
  public void testPreCreateNoSubject() throws Exception {
    plugin.processPreCreate(new CreateRequestImpl(mock(Metacard.class)));
  }

  @Test(expected = StopProcessingException.class)
  public void testPreCreateBadWithSubject() throws Exception {
    plugin.processPreCreate(badCreateRequest);
  }

  @Test(expected = StopProcessingException.class)
  public void testPreUpdateNoSubject() throws Exception {
    plugin.processPreUpdate(new UpdateRequestImpl("", mock(Metacard.class)), new HashMap<>());
  }

  @Test
  public void testPreCreate() throws Exception {
    plugin.processPreCreate(createRequest);
  }

  @Test
  public void testPreUpdate() throws Exception {
    Map<String, Metacard> metacardMap = new HashMap<>();
    Metacard metacard = getExactRolesMetacard();
    metacardMap.put(metacard.getId(), metacard);
    plugin.processPreUpdate(updateRequest, metacardMap);
  }

  @Test
  public void testUnusedMethods() throws StopProcessingException {
    plugin.processPreQuery(mock(QueryRequest.class));

    plugin.processPreDelete(mock(DeleteRequest.class));

    plugin.processPreResource(mock(ResourceRequest.class));
  }

  private void verifyFilterResponse(QueryResponse response) {
    LOGGER.info("Filtered with {}", response.getResults().size() + " out of 5 original.");
    LOGGER.info("Checking Results");
    assertEquals(4, response.getResults().size());
    LOGGER.info("Filtering succeeded.");
  }

  private Metacard getMoreRolesMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setResourceSize("100");
    try {
      metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
    } catch (URISyntaxException e) {
      LOGGER.error("", e);
    }
    metacard.setContentTypeName("Resource");
    metacard.setTitle("Metacard 1");
    metacard.setContentTypeVersion("1");
    metacard.setType(
        new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, new HashSet<>()));
    HashMap<String, List<String>> security = new HashMap<>();
    security.put("Roles", Arrays.asList("A", "B", "CR", "WS"));
    metacard.setSecurity(security);
    return metacard;
  }

  private Metacard getMissingRolesMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setResourceSize("100");
    try {
      metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
    } catch (URISyntaxException e) {
      LOGGER.error("", e);
    }
    metacard.setContentTypeName("Resource");
    metacard.setTitle("Metacard 1");
    metacard.setContentTypeVersion("1");
    metacard.setType(
        new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, new HashSet<>()));

    HashMap<String, List<String>> security = new HashMap<>();
    security.put("Roles", Arrays.asList("A"));
    metacard.setSecurity(security);
    return metacard;
  }

  private Metacard getExactRolesMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setResourceSize("100");
    try {
      metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
    } catch (URISyntaxException e) {
      LOGGER.error("", e);
    }
    metacard.setContentTypeName("Resource");
    metacard.setTitle("Metacard 1");
    metacard.setContentTypeVersion("1");
    metacard.setType(
        new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, new HashSet<>()));

    HashMap<String, List<String>> security = new HashMap<>();
    security.put("Roles", Arrays.asList("A", "B"));
    metacard.setSecurity(security);
    metacard.setId("exactroles");
    return metacard;
  }

  private Metacard getNoRolesMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setResourceSize("100");
    try {
      metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
    } catch (URISyntaxException e) {
      LOGGER.error("", e);
    }
    metacard.setContentTypeName("Resource");
    metacard.setTitle("Metacard 1");
    metacard.setContentTypeVersion("1");
    metacard.setType(
        new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, new HashSet<>()));

    HashMap<String, List<String>> security = new HashMap<>();
    metacard.setSecurity(security);
    return metacard;
  }

  private Metacard getNoSecurityAttributeMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setResourceSize("100");
    try {
      metacard.setResourceURI(new URI("http://some.fancy.uri/goes/here"));
    } catch (URISyntaxException e) {
      LOGGER.error("", e);
    }
    // Intentionally do not set the Metacard.SECURITY attribute
    metacard.setContentTypeName("Resource");
    metacard.setTitle("Metacard 1");
    metacard.setContentTypeVersion("1");
    metacard.setType(
        new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, new HashSet<>()));

    return metacard;
  }

  private QueryRequestImpl getSampleRequest() {
    return new QueryRequestImpl(
        new Query() {
          @Override
          public int getStartIndex() {
            return 0;
          }

          @Override
          public int getPageSize() {
            return 10;
          }

          @Override
          public SortBy getSortBy() {
            return null;
          }

          @Override
          public boolean requestsTotalResultsCount() {
            return false;
          }

          @Override
          public long getTimeoutMillis() {
            return 0;
          }

          @Override
          public boolean evaluate(Object o) {
            return true;
          }

          @Override
          public Object accept(FilterVisitor filterVisitor, Object o) {
            return null;
          }
        });
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
