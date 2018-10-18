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
package org.codice.ddf.resourcemanagement.usage;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ResourceUsagePluginTest {

  private ResourceUsagePlugin plugin;

  private AttributesStore attributeStore;

  private static final String TEST_USER = "testuser";

  private static final String RESOURCE_SIZE = "100";

  private static final String RESOURCE_SIZE_LARGE = "200";

  private static final long DATA_LIMIT = 200L;

  private Subject subject;

  @Before
  public void setUp() throws PersistenceException {
    attributeStore = mock(AttributesStore.class);
    doReturn(DATA_LIMIT).when(attributeStore).getDataLimitByUser(anyString());
    plugin = new ResourceUsagePlugin(attributeStore);
    plugin.setMonitorLocalSources(true);
  }

  @Test
  public void getMonitorLocalSources() {
    assertThat(plugin.getMonitorLocalSources(), is(true));
  }

  @Test
  public void testNullInput() throws StopProcessingException, PluginExecutionException {
    assertThat(plugin.process((ResourceRequest) null), is(nullValue()));
    assertThat(plugin.process((ResourceResponse) null), is(nullValue()));
  }

  @Test
  public void testPreResourceValidSize()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ArgumentCaptor<String> usernameArg = ArgumentCaptor.forClass(String.class);

    ResourceRequest originalRequest = getMockLocalResourceRequest(RESOURCE_SIZE, TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);

    assertThat(request, notNullValue());
    assertThat(request, is(originalRequest));

    verify(attributeStore).getDataLimitByUser(usernameArg.capture());

    assertThat(usernameArg.getValue(), is(TEST_USER));
  }

  @Test(expected = DataUsageLimitExceededException.class)
  public void testPreResourceSizeExceedsDataLimit()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    doReturn(DATA_LIMIT).when(attributeStore).getCurrentDataUsageByUser(anyString());
    ResourceRequest originalRequest = getMockLocalResourceRequest(RESOURCE_SIZE_LARGE, TEST_USER);
    plugin.process(originalRequest);
  }

  @Test
  public void testPreResourceLocalSourceSizeNotFound()
      throws PersistenceException, PluginExecutionException, StopProcessingException {
    ResourceRequest originalRequest = getMockLocalResourceRequest(null, TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, is(notNullValue()));
    assertThat(request, is(originalRequest));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPreResourceLocalSourceInvalidSize()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ResourceRequest originalRequest = getMockLocalResourceRequest("47 bytes", TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, is(notNullValue()));
    assertThat(request, is(originalRequest));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPreResourceLocalSourceNoLocalMonitoring()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    plugin.setMonitorLocalSources(false);
    ResourceRequest originalRequest = getMockLocalResourceRequest(RESOURCE_SIZE, TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, is(notNullValue()));
    assertThat(request, is(originalRequest));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPostResourceLocalSourceNoLocalMonitoring()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    plugin.setMonitorLocalSources(false);
    ResourceResponse originalResponse = getMockLocalResourceResponse(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, is(notNullValue()));
    assertThat(response, is(originalResponse));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPostResourceRemoteSourceNoRemoteDestinationKey() throws Exception {
    plugin.setMonitorLocalSources(false);
    ResourceResponse originalResponse =
        getMockRemoteResourceResponseNoRemoteDestinationKey(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, is(notNullValue()));
    assertThat(response, is(originalResponse));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPostResourceRemoteSourceFalseRemoteDestinationKey() throws Exception {
    plugin.setMonitorLocalSources(false);
    ResourceResponse originalResponse =
        getMockRemoteResourceResponseFalseRemoteDestinationKey(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, is(notNullValue()));
    assertThat(response, is(originalResponse));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPreResourcePersistenceException()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    doThrow(PersistenceException.class).when(attributeStore).getCurrentDataUsageByUser(anyString());
    ResourceRequest originalRequest = getMockLocalResourceRequest(RESOURCE_SIZE, TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, notNullValue());
    assertThat(request, is(originalRequest));
  }

  @Test
  public void testPostResourcePersistenceException()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    doThrow(PersistenceException.class)
        .when(attributeStore)
        .updateUserDataUsage(anyString(), anyLong());
    ResourceResponse resourceResponse = getMockLocalResourceResponse(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(resourceResponse);

    assertThat(response, notNullValue());
    assertThat(response, is(resourceResponse));
  }

  @Test
  public void testPreResourceNoSubject()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ResourceRequest originalRequest = getMockResourceRequestNoSubject(RESOURCE_SIZE);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, is(notNullValue()));
    assertThat(request, is(originalRequest));
    verify(attributeStore, never()).getDataLimitByUser(anyString());
    verify(attributeStore, never()).getCurrentDataUsageByUser(anyString());
  }

  @Test
  public void testPostResourceNoSubject()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ResourceResponse originalResponse = getMockResourceResponseNoSubject(RESOURCE_SIZE);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, is(notNullValue()));
    assertThat(response, is(originalResponse));
    verify(attributeStore, never()).updateUserDataUsage(anyString(), anyLong());
  }

  @Test
  public void testPostResourceLocalSourceValidSize()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ArgumentCaptor<String> usernameArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> dataUsage = ArgumentCaptor.forClass(Long.class);
    ResourceResponse originalResponse = getMockLocalResourceResponse(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, notNullValue());
    assertThat(response, is(originalResponse));
    verify(attributeStore).updateUserDataUsage(usernameArg.capture(), dataUsage.capture());
    assertThat(usernameArg.getValue(), is(TEST_USER));
    assertThat(dataUsage.getValue(), is(Long.valueOf(RESOURCE_SIZE)));
  }

  @Test
  public void testPostResourceLocalSourceSizeNotFound()
      throws PersistenceException, PluginExecutionException, StopProcessingException {
    ResourceResponse resourceResponse = getMockLocalResourceResponse(null, TEST_USER);
    ResourceResponse request = plugin.process(resourceResponse);
    assertThat(request, is(notNullValue()));
    assertThat(request, is(resourceResponse));
    verify(attributeStore, never()).updateUserDataUsage(anyString(), anyLong());
  }

  @Test
  public void testPostResourceInvalidSize()
      throws StopProcessingException, PluginExecutionException, PersistenceException {
    ResourceResponse originalResponse = getMockLocalResourceResponse("47 bytes", TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, is(notNullValue()));
    assertThat(response, is(originalResponse));
    verify(attributeStore, never()).updateUserDataUsage(anyString(), anyLong());
  }

  @Test
  public void testPreResourceRemoteSourceValidSize() throws Exception {
    plugin.setMonitorLocalSources(false);
    ArgumentCaptor<String> usernameArg = ArgumentCaptor.forClass(String.class);
    ResourceRequest originalRequest = getMockRemoteResourceRequest(RESOURCE_SIZE, TEST_USER);
    ResourceRequest request = plugin.process(originalRequest);
    assertThat(request, notNullValue());
    assertThat(request, is(originalRequest));
    verify(attributeStore).getDataLimitByUser(usernameArg.capture());
    assertThat(usernameArg.getValue(), is(TEST_USER));
  }

  @Test
  public void testPostResourceRemoteSourceValidSize() throws Exception {
    plugin.setMonitorLocalSources(false);
    ArgumentCaptor<String> usernameArg = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> dataUsage = ArgumentCaptor.forClass(Long.class);
    ResourceResponse originalResponse = getMockRemoteResourceResponse(RESOURCE_SIZE, TEST_USER);
    ResourceResponse response = plugin.process(originalResponse);
    assertThat(response, notNullValue());
    assertThat(response, is(originalResponse));
    verify(attributeStore).updateUserDataUsage(usernameArg.capture(), dataUsage.capture());
    assertThat(usernameArg.getValue(), is(TEST_USER));
    assertThat(dataUsage.getValue(), is(Long.valueOf(RESOURCE_SIZE)));
  }

  private ResourceResponse getMockResourceResponse(String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceResponse resourceResponse = mock(ResourceResponse.class);
    Map<String, Serializable> responseProperties = new HashMap<>();
    responseProperties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    responseProperties.put(Metacard.RESOURCE_SIZE, resourceSize);
    when(resourceResponse.getPropertyNames()).thenReturn(responseProperties.keySet());
    when(resourceResponse.getPropertyValue(SecurityConstants.SECURITY_SUBJECT)).thenReturn(subject);
    when(resourceResponse.getPropertyValue(Metacard.RESOURCE_SIZE)).thenReturn(resourceSize);
    return resourceResponse;
  }

  private ResourceRequest getMockResourceRequest(String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceRequest resourceRequest = mock(ResourceRequest.class);
    Map<String, Serializable> requestProperties = new HashMap<>();
    requestProperties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    requestProperties.put(Metacard.RESOURCE_SIZE, resourceSize);
    when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());
    when(resourceRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT)).thenReturn(subject);
    when(resourceRequest.getPropertyValue(Metacard.RESOURCE_SIZE)).thenReturn(resourceSize);
    return resourceRequest;
  }

  private ResourceResponse getMockLocalResourceResponse(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    return getMockResourceResponse(resourceSize, expectedUsername);
  }

  private ResourceRequest getMockLocalResourceRequest(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    return getMockResourceRequest(resourceSize, expectedUsername);
  }

  private ResourceRequest getMockRemoteResourceRequest(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceRequest resourceRequest = getMockResourceRequest(resourceSize, expectedUsername);
    when(resourceRequest.getPropertyValue(Constants.REMOTE_DESTINATION_KEY)).thenReturn(true);
    when(resourceRequest.hasProperties()).thenReturn(true);
    return resourceRequest;
  }

  private ResourceResponse getMockRemoteResourceResponse(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceResponse resourceResponse = getMockResourceResponse(resourceSize, expectedUsername);
    when(resourceResponse.getPropertyValue(Constants.REMOTE_DESTINATION_KEY)).thenReturn(true);
    when(resourceResponse.hasProperties()).thenReturn(true);
    return resourceResponse;
  }

  private ResourceResponse getMockRemoteResourceResponseNoRemoteDestinationKey(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceResponse resourceResponse = getMockResourceResponse(resourceSize, expectedUsername);
    when(resourceResponse.hasProperties()).thenReturn(true);
    return resourceResponse;
  }

  private ResourceResponse getMockRemoteResourceResponseFalseRemoteDestinationKey(
      String resourceSize, String expectedUsername) {
    setSubject(expectedUsername);
    ResourceResponse resourceResponse = getMockResourceResponse(resourceSize, expectedUsername);
    when(resourceResponse.getPropertyValue(Constants.REMOTE_DESTINATION_KEY)).thenReturn(false);
    when(resourceResponse.hasProperties()).thenReturn(true);
    return resourceResponse;
  }

  private ResourceRequest getMockResourceRequestNoSubject(String resourceSize) {
    ResourceRequest resourceRequest = getMockResourceRequest(resourceSize, null);
    when(resourceRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT)).thenReturn(null);
    return resourceRequest;
  }

  private ResourceResponse getMockResourceResponseNoSubject(String resourceSize) {
    ResourceResponse resourceResponse = getMockResourceResponse(resourceSize, null);
    when(resourceResponse.getPropertyValue(SecurityConstants.SECURITY_SUBJECT)).thenReturn(null);
    return resourceResponse;
  }

  private void setSubject(String expectedUsername) {
    AuthorizingRealm realm = mock(AuthorizingRealm.class);
    when(realm.getName()).thenReturn("mockRealm");
    when(realm.isPermitted(any(PrincipalCollection.class), any(Permission.class))).thenReturn(true);
    Collection<Realm> realms = new ArrayList<>();
    realms.add(realm);
    DefaultSecurityManager manager = new DefaultSecurityManager();
    manager.setRealms(realms);
    SimplePrincipalCollection principalCollection =
        new SimplePrincipalCollection(
            new Principal() {
              @Override
              public String getName() {
                return expectedUsername;
              }

              @Override
              public String toString() {
                return expectedUsername;
              }
            },
            realm.getName());

    subject = new MockSubject(manager, principalCollection);
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
