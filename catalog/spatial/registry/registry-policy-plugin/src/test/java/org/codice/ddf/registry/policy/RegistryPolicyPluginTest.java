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
package org.codice.ddf.registry.policy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.plugin.PolicyResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.junit.Test;

public class RegistryPolicyPluginTest {

  @Test
  public void testBlackListPostQuery() throws Exception {

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    RegistryPolicyPlugin rpp = createRegistryPlugin();

    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

    assertThat(response.operationPolicy().size(), is(0));
    assertThat(response.itemPolicy().size(), is(0));
    rpp.setRegistryEntryIds(Collections.singleton("validId"));

    response = rpp.processPostQuery(new ResultImpl(mcard), null);
    assertThat(response.itemPolicy(), equalTo(rpp.getBypassAccessPolicy()));
  }

  @Test
  public void testWhiteListPostQuery() throws Exception {

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setWhiteList(true);
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

    assertThat(response.operationPolicy().size(), is(0));
    assertThat(response.itemPolicy(), equalTo(rpp.getBypassAccessPolicy()));

    rpp.setRegistryEntryIds(Collections.singleton("validId"));
    response = rpp.processPostQuery(new ResultImpl(mcard), null);
    assertThat(response.itemPolicy().size(), is(0));
  }

  @Test
  public void testCudRegistryOperations() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    rpp.setCreateAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setUpdateAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setDeleteAccessPolicyStrings(Collections.singletonList("role=guest"));

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    PolicyResponse response = rpp.processPreCreate(mcard, null);
    assertThat(response.operationPolicy(), equalTo(rpp.getCreateAccessPolicy()));
    response = rpp.processPreUpdate(mcard, null);
    assertThat(response.operationPolicy(), equalTo(rpp.getUpdateAccessPolicy()));
    response = rpp.processPreDelete(Collections.singletonList(mcard), null);
    assertThat(response.operationPolicy(), equalTo(rpp.getDeleteAccessPolicy()));
  }

  @Test
  public void testReadRegistryOperations() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    rpp.setReadAccessPolicyStrings(Collections.singletonList("role=guest"));

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);
    assertThat(response.itemPolicy(), equalTo(rpp.getReadAccessPolicy()));
  }

  @Test
  public void testRemoteCudOperations() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    rpp.setCreateAccessPolicyStrings(Collections.singletonList("role=guest"));

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    HashMap<String, Serializable> props = new HashMap<>();
    props.put("local-destination", false);
    PolicyResponse response = rpp.processPreCreate(mcard, props);
    assertThat(response.operationPolicy().size(), is(0));
    response = rpp.processPreUpdate(mcard, props);
    assertThat(response.operationPolicy().size(), is(0));
    response = rpp.processPreDelete(Collections.singletonList(mcard), props);
    assertThat(response.operationPolicy().size(), is(0));
  }

  @Test
  public void testNonRegistryMcardTypes() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, "some.type"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);
    assertThat(response.itemPolicy().isEmpty(), is(true));
    response = rpp.processPreCreate(mcard, null);
    assertThat(response.operationPolicy().isEmpty(), is(true));
    response = rpp.processPreUpdate(mcard, null);
    assertThat(response.operationPolicy().isEmpty(), is(true));
    response = rpp.processPreDelete(Collections.singletonList(mcard), null);
    assertThat(response.operationPolicy().isEmpty(), is(true));
    Metacard mcard2 = new MetacardImpl();
    mcard2.setAttribute(new AttributeImpl(Metacard.ID, "abcdefghijklmnop1234567890"));

    response = rpp.processPostQuery(new ResultImpl(mcard2), null);
    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testDisabledRegistry() throws Exception {

    RegistryPolicyPlugin rpp = createRegistryPlugin();

    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    rpp.setRegistryDisabled(true);

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    PolicyResponse response = rpp.processPreCreate(mcard, null);
    assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
    response = rpp.processPreUpdate(mcard, null);
    assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
    response = rpp.processPreDelete(Collections.singletonList(mcard), null);
    assertThat(response.operationPolicy(), equalTo(rpp.getBypassAccessPolicy()));
  }

  @Test
  public void testNoRegistryBypassPermissions() throws Exception {
    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(null);
    PolicyResponse response = rpp.processPostQuery(new ResultImpl(mcard), null);

    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testSecurityValueSet() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();

    Map<String, Set<String>> expectedPolicy = new HashMap<>();
    Set<String> firstSet = new HashSet<>();
    Set<String> secondSet = new HashSet<>();
    firstSet.add("Charles");
    firstSet.add("Haller");
    secondSet.add("Nikolaevna");
    secondSet.add("Alexandria");
    secondSet.add("Rasputin");
    expectedPolicy.put("David", firstSet);
    expectedPolicy.put("Illyana", secondSet);

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));
    List<Serializable> securityValues = new ArrayList<>();
    securityValues.add("David=Charles,Haller");
    securityValues.add("Illyana=Nikolaevna, Alexandria, Rasputin");
    mcard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.SECURITY_LEVEL, securityValues));

    PolicyResponse response = rpp.processPreCreate(mcard, null);
    assertThat(response.operationPolicy().size(), is(0));
    assertThat(response.itemPolicy().size(), is(2));
    assertThat(response.itemPolicy(), equalTo(expectedPolicy));
  }

  @Test
  public void testSecurityValueInvalidStrings() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));
    List<Serializable> securityValues = new ArrayList<>();
    securityValues.add(" ");
    securityValues.add("");
    securityValues.add("badString");
    mcard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.SECURITY_LEVEL, securityValues));

    PolicyResponse response = rpp.processPreCreate(mcard, null);
    assertThat(response.operationPolicy().size(), is(0));
    assertThat(response.itemPolicy().size(), is(0));
  }

  @Test
  public void testUnusedMethods() throws Exception {
    RegistryPolicyPlugin rpp = createRegistryPlugin();
    rpp.setRegistryBypassPolicyStrings(Collections.singletonList("role=system-admin"));
    rpp.setCreateAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setUpdateAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setDeleteAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setReadAccessPolicyStrings(Collections.singletonList("role=guest"));
    rpp.setRegistryEntryIds(Collections.singleton("1234567890abcdefg987654321"));

    assertThat(rpp.isRegistryDisabled(), is(false));
    assertThat(rpp.getBypassAccessPolicy().get("role").iterator().next(), equalTo("system-admin"));
    assertThat(rpp.getCreateAccessPolicy().get("role").iterator().next(), equalTo("guest"));
    assertThat(rpp.getReadAccessPolicy().get("role").iterator().next(), equalTo("guest"));
    assertThat(rpp.getRegistryEntryIds().contains("1234567890abcdefg987654321"), is(true));

    Metacard mcard = new MetacardImpl();
    mcard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "validId"));
    mcard.setAttribute(new AttributeImpl(Metacard.ID, "1234567890abcdefg987654321"));

    assertThat(rpp.processPostDelete(mcard, null).itemPolicy().isEmpty(), is(true));
    assertThat(rpp.processPostDelete(mcard, null).operationPolicy().isEmpty(), is(true));

    assertThat(rpp.processPreQuery(null, null).itemPolicy().isEmpty(), is(true));
    assertThat(rpp.processPreQuery(null, null).operationPolicy().isEmpty(), is(true));

    assertThat(rpp.processPreResource(null).itemPolicy().isEmpty(), is(true));
    assertThat(rpp.processPreResource(null).operationPolicy().isEmpty(), is(true));

    assertThat(rpp.processPostResource(null, mcard).itemPolicy().isEmpty(), is(true));
    assertThat(rpp.processPostResource(null, mcard).operationPolicy().isEmpty(), is(true));

    assertThat(rpp.isWhiteList(), is(false));
  }

  private RegistryPolicyPlugin createRegistryPlugin() {
    RegistryPolicyPlugin rpp = new RegistryPolicyPlugin();

    return rpp;
  }
}
