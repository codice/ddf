/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transform.xml;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.xml.adapter.AdaptedMetacard;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class TestAdaptedMetacard {
  private static final Attribute ID = new AttributeImpl(Core.ID, "metacard-id");

  private static final Attribute DOWNLOAD_URL =
      new AttributeImpl(Core.RESOURCE_DOWNLOAD_URL, "https://localhost:8993/download");

  private static final Attribute DESCRIPTION = new AttributeImpl(Core.DESCRIPTION, "Description");

  private static final Attribute DESCRIPTION_ALT =
      new AttributeImpl(Core.DESCRIPTION, "Alternative description");

  private AdaptedMetacardUnderTest metacard = new AdaptedMetacardUnderTest();

  private int expectedCollectionSize;

  private int expectedDescriptorCount;

  @Before
  public void setup() {
    metacard.setAttribute(ID);
    metacard.setAttribute(DOWNLOAD_URL);
    metacard.setAttribute(DESCRIPTION);

    expectedCollectionSize = metacard.getInternalCount();
    expectedDescriptorCount = metacard.getDescriptorCount();

    assertThat(metacard.getAttributesThroughDescriptors(), hasSize(expectedDescriptorCount));
    assertThat(metacard.getAttributesInternal(), hasSize(expectedCollectionSize));
  }

  @Test
  public void testCannotAddNullAttribute() {
    metacard.setAttribute(null);
    assertThat(metacard.getAttributesThroughDescriptors(), hasSize(expectedDescriptorCount));
    assertThat(metacard.getAttributesInternal(), hasSize(expectedCollectionSize));
  }

  @Test
  public void testCannotDuplicateAttributes() {
    metacard.setAttribute(DESCRIPTION_ALT);
    assertThat(metacard.getAttributesThroughDescriptors(), hasSize(expectedDescriptorCount));
    assertThat(metacard.getAttributesInternal(), hasSize(expectedCollectionSize));
  }

  @Test
  public void testGetAttributeNullName() {
    assertThat(metacard.getAttribute(null), is(nullValue()));
  }

  @Test
  public void testGetAttributeEmptyName() {
    assertThat(metacard.getAttribute(""), is(nullValue()));
  }

  @Test
  public void testGetAttributeId() {
    assertThat(metacard.getAttribute(Core.ID), is(new AttributeImpl(Core.ID, "metacard-id")));
  }

  @Test
  public void testGetValidAttribute() {
    assertThat(
        metacard.getAttribute(Core.DESCRIPTION),
        is(new AttributeImpl(Core.DESCRIPTION, "Description")));
  }

  @Test
  public void testGetNonExistentAttribute() {
    assertThat(metacard.getAttribute("nonexistent"), is(nullValue()));
  }

  static class AdaptedMetacardUnderTest extends AdaptedMetacard {
    Set<Attribute> getAttributesInternal() {
      return getAttributes();
    }

    int getInternalCount() {
      return getAttributes().size();
    }

    List<Attribute> getAttributesThroughDescriptors() {
      return getMetacardType().getAttributeDescriptors().stream()
          .map(AttributeDescriptor::getName)
          .map(this::getAttribute)
          .collect(Collectors.toList());
    }

    int getDescriptorCount() {
      return getMetacardType().getAttributeDescriptors().size();
    }
  }
}
