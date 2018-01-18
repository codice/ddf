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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.junit.Before;
import org.junit.Test;

public class TestMetacardMapperImpl {

  // Feature Constants
  private static final String EXAMPLE_FEATURE_TYPE = "{http://www.example.com}featureType1";

  private static final String NON_EXISTENT_FEATURE = "feature.prop.nonexistent";

  private static final String FEATURE_NAME = "featureName";

  private static final String FEATURE_MODIFIED = "featureModified";

  private static final String FEATURE_CALLSIGN = "featureCallSign";

  private static final String FEATURE_MISSIONID = "featureMissionId";

  private static final String FEATURE_CONSTANT_VALUE = "ConstantFoo";

  // Attribute Constants
  private static final String METACARD_TITLE = "title";

  private static final String METACARD_MODIFIED = "modified";

  private static final String METACARD_CONTENTSTORE = "ext.content-store-name";

  private static final String METACARD_KEYWORD_1 = "topic.keyword1";

  private static final String METACARD_KEYWORD_2 = "topic.keyword2";

  // Feature Values
  private static final String FEATURE_NAME_VALUE = "myFeature";

  private static final Date FEATURE_MODIFIED_VALUE = new Date();

  private static final String FEATURE_CONTENTSTORE_VALUE = null;

  private static final String FEATURE_CALLSIGN_VALUE = "Test Callsign";

  private static final String FEATURE_MISSIONID_VALUE = "M101";

  private static final Serializable[][] MAPPINGS = {
    {METACARD_TITLE, FEATURE_NAME, "{{" + FEATURE_NAME + "}}", FEATURE_NAME_VALUE},
    {
      METACARD_MODIFIED,
      FEATURE_MODIFIED,
      "{{dateFormat " + FEATURE_MODIFIED + " format=\"yyyy-MM-dd\"}}",
      FEATURE_MODIFIED_VALUE
    },
    {METACARD_CONTENTSTORE, "", FEATURE_CONSTANT_VALUE, FEATURE_CONTENTSTORE_VALUE},
    {
      METACARD_KEYWORD_1,
      FEATURE_CALLSIGN,
      "CallSign = {{" + FEATURE_CALLSIGN + "}}",
      FEATURE_CALLSIGN_VALUE
    },
    {
      METACARD_KEYWORD_2,
      FEATURE_MISSIONID,
      "MissionID = {{" + FEATURE_MISSIONID + "}}",
      FEATURE_MISSIONID_VALUE
    }
  };

  private MetacardMapperImpl metacardMapper;

  private Map<String, Serializable> mappingContext = new HashMap<>();

  @Before
  public void setup() {
    this.metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);

    for (Serializable[] mapping : MAPPINGS) {
      String attributeName = mapping[0].toString();
      String featureName = mapping[1].toString();
      String templateText = mapping[2].toString();
      Serializable value = mapping[3];

      metacardMapper.addAttributeMapping(attributeName, featureName, templateText);
      mappingContext.put(featureName, value);
    }
  }

  @Test
  public void testGetFeatureProperty() {
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_TITLE))
            .get()
            .getFeatureProperty(),
        is(FEATURE_NAME));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_MODIFIED))
            .get()
            .getFeatureProperty(),
        is(FEATURE_MODIFIED));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_KEYWORD_1))
            .get()
            .getFeatureProperty(),
        is(FEATURE_CALLSIGN));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_KEYWORD_2))
            .get()
            .getFeatureProperty(),
        is(FEATURE_MISSIONID));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_CONTENTSTORE))
            .get()
            .getFeatureProperty(),
        is(""));
  }

  @Test
  public void testGetMetacardAttribute() {
    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_NAME))
            .get()
            .getAttributeName(),
        is(METACARD_TITLE));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_MODIFIED))
            .get()
            .getAttributeName(),
        is(METACARD_MODIFIED));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_CALLSIGN))
            .get()
            .getAttributeName(),
        is(METACARD_KEYWORD_1));

    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_MISSIONID))
            .get()
            .getAttributeName(),
        is(METACARD_KEYWORD_2));
  }

  @Test
  public void testGetEntry() {
    Optional optionalEntry =
        metacardMapper.getEntry(e -> e.getFeatureProperty().equals(FEATURE_NAME));
    assertThat(optionalEntry.isPresent(), is(true));

    optionalEntry =
        metacardMapper.getEntry(e -> e.getFeatureProperty().equals(NON_EXISTENT_FEATURE));
    assertThat(optionalEntry.isPresent(), is(false));

    optionalEntry = metacardMapper.getEntry(null);
    assertThat(optionalEntry, is(Optional.empty()));
  }

  @Test
  public void testTemplate() {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    Serializable[][] results = {
      {METACARD_TITLE, FEATURE_NAME_VALUE},
      {METACARD_MODIFIED, format.format(FEATURE_MODIFIED_VALUE)},
      {METACARD_CONTENTSTORE, FEATURE_CONSTANT_VALUE},
      {METACARD_KEYWORD_1, "CallSign = " + FEATURE_CALLSIGN_VALUE},
      {METACARD_KEYWORD_2, "MissionID = " + FEATURE_MISSIONID_VALUE}
    };

    for (Serializable[] result : results) {
      validateMapping(result[0].toString(), result[1]);
    }
  }

  private void validateMapping(String attributeName, Serializable expectedValue) {
    String value =
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(attributeName))
            .get()
            .getMappingFunction()
            .apply(mappingContext);

    assertThat(value, is(expectedValue));
  }

  @Test
  public void testStream() {
    assertThat(metacardMapper.stream().count(), is((long) MAPPINGS.length));
  }

  @Test
  public void testSetAttributeMappingsList() {
    metacardMapper.setAttributeMappings(
        Collections.singletonList(
            "{\"attributeName\": \"topic.keyword\", \"featureName\": \"MissionId\", \"template\": \"{{myFeature.missionid}}\"}"));
    List<MetacardMapper.Entry> mappingList = metacardMapper.getMappingEntryList();
    assertThat(mappingList, hasSize(1));
    assertThat(mappingList.get(0), is(instanceOf(FeatureAttributeEntry.class)));
    FeatureAttributeEntry featureAttributeEntry = (FeatureAttributeEntry) mappingList.get(0);
    assertThat(featureAttributeEntry.getAttributeName(), is("topic.keyword"));
    assertThat(featureAttributeEntry.getFeatureProperty(), is("MissionId"));
    assertThat(featureAttributeEntry.getTemplateText(), is("{{myFeature.missionid}}"));
  }

  @Test
  public void testSetAttributeMappingsListWithBadJson() {
    metacardMapper.setAttributeMappings(
        Arrays.asList(
            "{\"attributeName\": \"topic.keyword\", \"featureName\": \"MissionId\", \"template\": \"{{myFeature.missionid}}\"}",
            "{\"attributeName\": \"topic.other\", \"featureName\": \"Other\", \"template\" \"{{myFeature.other}}\""));
    List<MetacardMapper.Entry> mappingList = metacardMapper.getMappingEntryList();
    assertThat(mappingList, hasSize(1));
    assertThat(mappingList.get(0), is(instanceOf(FeatureAttributeEntry.class)));
    FeatureAttributeEntry featureAttributeEntry = (FeatureAttributeEntry) mappingList.get(0);
    assertThat(featureAttributeEntry.getAttributeName(), is("topic.keyword"));
    assertThat(featureAttributeEntry.getFeatureProperty(), is("MissionId"));
    assertThat(featureAttributeEntry.getTemplateText(), is("{{myFeature.missionid}}"));
  }
}
