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
package org.codice.ddf.registry.common.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.registry.common.RegistryConstants;
import org.junit.Before;
import org.junit.Test;

public class RegistryUtilityTest {

  private static final String VALID_REGISTRY_ID = "urn:uuid:922145e890bb458696526a7550e3c0b0";

  private Metacard metacard;

  private ArrayList<String> tags = new ArrayList<>();

  private Attribute tagsAttribute;

  private Attribute blankRegistryIdAttribute =
      new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "");

  private Attribute registryIdAttribute =
      new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, VALID_REGISTRY_ID);

  private Attribute identityAttribute =
      new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);

  private Attribute localAttribute =
      new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);

  @Before
  public void setup() {
    metacard = new MetacardImpl();
    tags.add(RegistryConstants.REGISTRY_TAG);
    tagsAttribute = new AttributeImpl(Metacard.TAGS, tags);

    metacard.setAttribute(tagsAttribute);
  }

  @Test
  public void testValidRegistryId() {
    assertThat(RegistryUtility.validRegistryId(VALID_REGISTRY_ID), is(true));
    assertThat(
        RegistryUtility.validRegistryId("aaaaaaaaa922145e890bb458696526a7550e3c0b0"), is(false));
    assertThat(
        RegistryUtility.validRegistryId("urn:uuid:922145e890bb458696526a7550e3c0b"), is(false));
    assertThat(
        RegistryUtility.validRegistryId("urn:uuid:922145e890bb458696526a7550e3c0b000"), is(false));
    assertThat(
        RegistryUtility.validRegistryId("urn:uuid:922145e890bb45869652:$%550e3c0b0"), is(false));
  }

  @Test
  public void testMetacardHasNoRegistryTag() {
    metacard = new MetacardImpl();
    assertThat(RegistryUtility.isRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testMetacardHasRegistryTagNoRegistryId() {
    assertThat(RegistryUtility.isRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testMetacardHasEmptyRegistryId() {
    metacard.setAttribute(blankRegistryIdAttribute);
    assertThat(RegistryUtility.isRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testInternalMetacardHasNoRegistryTag() {
    metacard = new MetacardImpl();
    assertThat(RegistryUtility.isInternalRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testInternalMetacardHasRegistryTagNoRegistryId() {
    tags.clear();
    tags.add(RegistryConstants.REGISTRY_TAG_INTERNAL);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));
    assertThat(RegistryUtility.isInternalRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testInternalMetacardHasEmptyRegistryId() {
    metacard.setAttribute(blankRegistryIdAttribute);
    tags.clear();
    tags.add(RegistryConstants.REGISTRY_TAG_INTERNAL);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));
    assertThat(RegistryUtility.isInternalRegistryMetacard(metacard), is(false));
  }

  @Test
  public void testInternalMetacardHasValidRegistryId() {
    metacard.setAttribute(registryIdAttribute);
    tags.clear();
    tags.add(RegistryConstants.REGISTRY_TAG_INTERNAL);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));
    assertThat(RegistryUtility.isInternalRegistryMetacard(metacard), is(true));
  }

  @Test
  public void testMetacardHasValidRegistryId() {
    metacard.setAttribute(registryIdAttribute);
    assertThat(RegistryUtility.isRegistryMetacard(metacard), is(true));
  }

  @Test
  public void testGetRegistryId() {
    metacard.setAttribute(registryIdAttribute);
    assertThat(RegistryUtility.getRegistryId(metacard), is(VALID_REGISTRY_ID));
  }

  @Test
  public void testIsNotIdentityNode() {
    assertThat(RegistryUtility.isIdentityNode(metacard), is(false));
  }

  @Test
  public void testIsIdentityNode() {
    metacard.setAttribute(identityAttribute);
    assertThat(RegistryUtility.isIdentityNode(metacard), is(true));
  }

  @Test
  public void testIsNotLocalNode() {
    assertThat(RegistryUtility.isLocalNode(metacard), is(false));
  }

  @Test
  public void testIsLocalNode() {
    metacard.setAttribute(localAttribute);
    assertThat(RegistryUtility.isLocalNode(metacard), is(true));
  }

  @Test
  public void testNullStringAttribute() {
    assertThat(RegistryUtility.getStringAttribute(metacard, "MissingAttribute", null), nullValue());
  }

  @Test
  public void testStringAttribute() {
    metacard.setAttribute(registryIdAttribute);
    assertThat(
        RegistryUtility.getStringAttribute(metacard, RegistryObjectMetacardType.REGISTRY_ID, null),
        is(VALID_REGISTRY_ID));
  }

  @Test
  public void testNullListAttribute() {
    assertThat(
        RegistryUtility.getListOfStringAttribute(metacard, "MissingAttribute").size(), is(0));
  }

  @Test
  public void testListAttribute() {
    List<String> values = RegistryUtility.getListOfStringAttribute(metacard, Metacard.TAGS);
    assertThat(values.size(), is(1));
    assertThat(values, contains(RegistryConstants.REGISTRY_TAG));
  }

  @Test
  public void testHasAttribute() {
    assertThat(RegistryUtility.hasAttribute(metacard, Metacard.TAGS), is(true));
    assertThat(RegistryUtility.hasAttribute(metacard, "BadAttribute"), is(false));
  }
}
