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
package org.codice.ddf.catalog.plugin.metacard.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/** Validate all the helpers within the {@link MetacardServices} utility operations class. */
@RunWith(MockitoJUnitRunner.class)
public class MetacardServicesTest {

  private static final String METACARD_TITLE = "Metacard Title";

  private static final String METACARD_DESCRIPTION = "Hello from the metacard";

  private static final String DESCRIPTION_KEY = "description";

  private AttributeFactory attributeFactory;

  private MetacardServices metacardServices;

  @Before
  public void setup() throws Exception {
    attributeFactory = new AttributeFactory();
    metacardServices = new MetacardServices();
  }

  @Test
  public void testApplyNewAttributesOnEmptyList() throws Exception {
    List<Metacard> metacards = ImmutableList.of();
    Map<String, String> attributeMap = ImmutableMap.of();

    List<MetacardType> mockMetacardTypes = mock(List.class);
    metacardServices = new MetacardServices(mockMetacardTypes);
    AttributeFactory mockAttributeFactory = mock(AttributeFactory.class);

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, mockAttributeFactory);

    assertThat(newMetacards, hasSize(0));
    verifyZeroInteractions(mockMetacardTypes, mockAttributeFactory);
  }

  @Test
  public void testApplyNewAttributesWithEmptyMap() throws Exception {
    Metacard metacard = createMetacard();
    List<Metacard> metacards = ImmutableList.of(metacard);
    Map<String, String> attributeMap = ImmutableMap.of();

    List<MetacardType> mockMetacardTypes = mock(List.class);
    metacardServices = new MetacardServices(mockMetacardTypes);
    AttributeFactory mockAttributeFactory = mock(AttributeFactory.class);

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, mockAttributeFactory);

    assertThat(newMetacards, hasSize(1));
    verifyZeroInteractions(mockMetacardTypes, mockAttributeFactory);

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(0));
  }

  @Test
  public void testApplyNewAttributes() throws Exception {

    Metacard metacard = createMetacard();
    List<Metacard> metacards = ImmutableList.of(metacard);
    Map<String, String> attributeMap =
        ImmutableMap.of(
            "point-of-contact", "contact",
            "metadata", "<? xml version=1.0 ?><Root><Child/></Root>");

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, attributeFactory);

    assertThat(newMetacards, hasSize(1));

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(0));
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(0), attributeMap);
  }

  @Test
  public void testApplyNewAttributesToMultipleMetacards() throws Exception {

    Metacard metacard1 = createMetacard();
    Metacard metacard2 = createMetacard();
    List<Metacard> metacards = ImmutableList.of(metacard1, metacard2);
    Map<String, String> attributeMap =
        ImmutableMap.of(
            "point-of-contact", "contact",
            "metadata", "<? xml version=1.0 ?><Root><Child/></Root>");

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, attributeFactory);

    assertThat(newMetacards, hasSize(2));

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(0));
    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(1));
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(0), attributeMap);
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(1), attributeMap);
  }

  @Test
  public void testDoNotApplyAnyAttributesBecauseAllExist() throws Exception {

    Metacard metacard = createMetacard();
    List<Metacard> metacards = ImmutableList.of(metacard);
    Map<String, String> attributeMap =
        ImmutableMap.of(
            "title", "canned title",
            "description", "canned description");

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, attributeFactory);

    assertThat(newMetacards, hasSize(1));

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(0));
  }

  @Test
  public void testApplyNewAttributesWithoutOverwrite() throws Exception {

    Metacard metacard = createMetacard();
    List<Metacard> metacards = ImmutableList.of(metacard);

    Map<String, String> attributesThatShouldBeOnMetacard =
        ImmutableMap.of(
            "point-of-contact", "contact",
            "metadata", "<? xml version=1.0 ?><Root><Child/></Root>");

    Map<String, String> attributesThatShouldNOTBeOnMetacard =
        ImmutableMap.of(
            "title", "canned title",
            "description", "canned description");

    Map attributeMap =
        ImmutableMap.builder()
            .putAll(attributesThatShouldBeOnMetacard)
            .putAll(attributesThatShouldNOTBeOnMetacard)
            .build();

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, attributeFactory);

    assertThat(newMetacards, hasSize(1));

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(0));
    assertThatMetacardHasAttributesEqualToThoseInMap(
        newMetacards.get(0), attributesThatShouldBeOnMetacard);
  }

  @Test
  public void testApplyNewAttributesThatDifferPerMetacard() throws Exception {

    MetacardImpl metacardWithMetadataNoExpectedTitleOrDescription = new MetacardImpl();
    metacardWithMetadataNoExpectedTitleOrDescription.setMetadata("<html><body/></html>");

    MetacardImpl metacardWithPoc = createMetacard();
    metacardWithPoc.setPointOfContact("my name here");

    MetacardImpl metacardWithCreatedDate = createMetacard();
    metacardWithCreatedDate.setCreatedDate(
        DatatypeConverter.parseDateTime("2001-10-26T21:32:52").getTime());

    List<Metacard> metacards =
        ImmutableList.of(
            metacardWithMetadataNoExpectedTitleOrDescription,
            metacardWithPoc,
            metacardWithCreatedDate);

    Map<String, String> attributesTitleAndDescription =
        ImmutableMap.of(
            "title", "canned title",
            "description", "canned description");

    Map<String, String> attributeMetadata =
        ImmutableMap.of("metadata", "<? xml version=1.0 ?><Root><Child/></Root>");
    Map<String, String> attributePoc = ImmutableMap.of("point-of-contact", "contact");
    Map<String, String> attributeCreated = ImmutableMap.of("created", "2001-10-26T21:32:52");

    Map attributeMap =
        ImmutableMap.builder()
            .putAll(attributesTitleAndDescription)
            .putAll(attributeMetadata)
            .putAll(attributePoc)
            .putAll(attributeCreated)
            .build();

    List<Metacard> newMetacards =
        metacardServices.setAttributesIfAbsent(metacards, attributeMap, attributeFactory);

    assertThat(newMetacards, hasSize(3));

    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(1));
    assertThatMetacardHasExpectedTitleAndDescription(newMetacards.get(2));

    Map metadataResult =
        ImmutableMap.builder()
            .putAll(attributesTitleAndDescription)
            .putAll(attributePoc)
            .putAll(attributeCreated)
            .build();
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(0), metadataResult);

    Map pocResult =
        ImmutableMap.builder().putAll(attributeMetadata).putAll(attributeCreated).build();
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(1), pocResult);

    Map createdResult =
        ImmutableMap.builder().putAll(attributeMetadata).putAll(attributePoc).build();
    assertThatMetacardHasAttributesEqualToThoseInMap(newMetacards.get(2), createdResult);
  }

  private MetacardImpl createMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle(METACARD_TITLE);
    metacard.setDescription(METACARD_DESCRIPTION);
    return metacard;
  }

  private void assertThatMetacardHasExpectedTitleAndDescription(Metacard metacard) {
    assertThat(metacard.getTitle(), is(METACARD_TITLE));
    assertThat(metacard.getAttribute(DESCRIPTION_KEY).getValue(), is(METACARD_DESCRIPTION));
  }

  private void assertThatMetacardHasAttributesEqualToThoseInMap(
      Metacard metacard, Map<String, String> attributeMap) {
    attributeMap
        .entrySet()
        .stream()
        .filter(entry -> metacard.getAttribute(entry.getKey()) != null)
        .forEach(
            entry -> {
              Serializable serializable = metacard.getAttribute(entry.getKey()).getValue();
              if (serializable instanceof Date) {
                Date dateValue = Date.class.cast(serializable);
                assertThat(
                    dateValue, is(DatatypeConverter.parseDateTime(entry.getValue()).getTime()));
              } else {
                assertThat(serializable, is(entry.getValue()));
              }
            });
  }
}
