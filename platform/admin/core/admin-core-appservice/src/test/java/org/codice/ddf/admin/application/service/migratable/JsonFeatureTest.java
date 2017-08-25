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
package org.codice.ddf.admin.application.service.migratable;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonFeatureTest {

  private static final String NAME = "test.name";

  private static final String ID = "test.id";

  private static final String VERSION = "1.2";

  private static final String DESCRIPTION = "test.description";

  private static final FeatureState STATE = FeatureState.Installed;

  private static final String REGION = "test.region";

  private static final String REPOSITORY = "test.repo";

  private static final int START = 55;

  private final JsonFeature jfeature =
      new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.equalTo(VERSION));
    Assert.assertThat(jfeature.getDescription(), Matchers.equalTo(DESCRIPTION));
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.equalTo(REPOSITORY));
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test
  public void testConstructorWithOptionalVersion() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, null, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.nullValue());
    Assert.assertThat(jfeature.getDescription(), Matchers.equalTo(DESCRIPTION));
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.equalTo(REPOSITORY));
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test
  public void testConstructorWithOptionalDescription() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.equalTo(VERSION));
    Assert.assertThat(jfeature.getDescription(), Matchers.nullValue());
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.equalTo(REPOSITORY));
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test
  public void testConstructorWithOptionalRegion() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, null, REPOSITORY, START);

    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.equalTo(VERSION));
    Assert.assertThat(jfeature.getDescription(), Matchers.equalTo(DESCRIPTION));
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(FeaturesService.ROOT_REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.equalTo(REPOSITORY));
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test
  public void testConstructorWithOptionalRepository() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, null, START);

    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.equalTo(VERSION));
    Assert.assertThat(jfeature.getDescription(), Matchers.equalTo(DESCRIPTION));
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.nullValue());
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test
  public void testConstructorWithNullName() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null name"));

    new JsonFeature(null, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);
  }

  @Test
  public void testConstructorWithNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null id"));

    new JsonFeature(NAME, null, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);
  }

  @Test
  public void testConstructorWithNullState() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null state"));

    new JsonFeature(NAME, ID, VERSION, DESCRIPTION, null, REGION, REPOSITORY, START);
  }

  @Test
  public void testWithConstructorFeature() throws Exception {
    final Feature feature = Mockito.mock(Feature.class);
    final FeaturesService featuresService = Mockito.mock(FeaturesService.class);

    Mockito.when(feature.getId()).thenReturn(ID);
    Mockito.when(feature.getName()).thenReturn(NAME);
    Mockito.when(feature.getVersion()).thenReturn(VERSION);
    Mockito.when(feature.getDescription()).thenReturn(DESCRIPTION);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY);
    Mockito.when(featuresService.getState(ID)).thenReturn(STATE);
    Mockito.when(feature.getStartLevel()).thenReturn(START);

    final JsonFeature jfeature = new JsonFeature(feature, featuresService);

    Assert.assertThat(jfeature.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jfeature.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jfeature.getVersion(), Matchers.equalTo(VERSION));
    Assert.assertThat(jfeature.getDescription(), Matchers.equalTo(DESCRIPTION));
    Assert.assertThat(jfeature.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jfeature.getRegion(), Matchers.equalTo(FeaturesService.ROOT_REGION));
    Assert.assertThat(jfeature.getRepository(), Matchers.equalTo(REPOSITORY));
    Assert.assertThat(jfeature.getStartLevel(), Matchers.equalTo(START));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullFeature() throws Exception {
    final FeaturesService featuresService = Mockito.mock(FeaturesService.class);

    new JsonFeature(null, featuresService);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullFeaturesService() throws Exception {
    final Feature feature = Mockito.mock(Feature.class);

    new JsonFeature(feature, null);
  }

  @Test
  public void testHashCodeWhenEquals() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.hashCode(), Matchers.equalTo(jfeature2.hashCode()));
  }

  @Test
  public void testHashCodeWhenDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID + "2", VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.hashCode(), Matchers.not(Matchers.equalTo(jfeature2.hashCode())));
  }

  @Test
  public void testEqualsWhenEquals() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWhenidentical() throws Exception {
    Assert.assertThat(jfeature.equals(jfeature), Matchers.equalTo(true));
  }

  @SuppressWarnings("PMD.EqualsNull" /* purposely testing equals() when called with null */)
  @Test
  public void testEqualsWhenNull() throws Exception {
    Assert.assertThat(jfeature.equals(null), Matchers.equalTo(false));
  }

  @SuppressWarnings(
      "PMD.PositionLiteralsFirstInComparisons" /* purposely testing equals() when call with something else than expected */)
  @Test
  public void testEqualsWhenNotAFeature() throws Exception {
    Assert.assertThat(jfeature.equals("test"), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenNamesAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME + "2", ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenIdsAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID + "2", VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenVersionsAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION + "2", DESCRIPTION, STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenDescriptionsAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION + "2", STATE, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenStatesAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(
            NAME, ID, VERSION, DESCRIPTION, FeatureState.Uninstalled, REGION, REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenRegionsAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION + "2", REPOSITORY, START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenRepositoriesAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY + "2", START);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testEqualsWhenStartLevelsAreDifferent() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, REPOSITORY, START + 2);

    Assert.assertThat(jfeature.equals(jfeature2), Matchers.not(Matchers.equalTo(true)));
  }

  @Test
  public void testToString() throws Exception {
    Assert.assertThat(jfeature.toString(), Matchers.equalTo("feature [" + ID + ']'));
  }

  @Test
  public void testJsonSerialization() throws Exception {
    JSONAssert.assertEquals(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonUtils.toJson(jfeature),
        true);
  }

  @Test
  public void testJsonDeserialization() throws Exception {
    Assert.assertThat(
        JsonUtils.fromJson(
            JsonUtilsTest.toJsonString(
                "name",
                NAME,
                "id",
                ID,
                "version",
                VERSION,
                "description",
                DESCRIPTION,
                "state",
                STATE,
                "region",
                REGION,
                "repository",
                REPOSITORY,
                "startLevel",
                START),
            JsonFeature.class),
        Matchers.equalTo(jfeature));
  }

  @Test
  public void testJsonDeserializationWhenNameIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required feature name"));

    JsonUtils.fromJson(
        JsonUtilsTest.toJsonString(
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonFeature.class);
  }

  @Test
  public void testJsonDeserializationWhenIdIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required feature id"));

    JsonUtils.fromJson(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonFeature.class);
  }

  @Test
  public void testJsonDeserializationWhenVersionIsMissing() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, null, DESCRIPTION, STATE, REGION, REPOSITORY, START);

    JSONAssert.assertEquals(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonUtils.toJson(jfeature),
        true);
  }

  @Test
  public void testJsonDeserializationWhenDescriptionIsMissing() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, REGION, REPOSITORY, START);

    JSONAssert.assertEquals(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonUtils.toJson(jfeature),
        true);
  }

  @Test
  public void testJsonDeserializationWhenStateIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required feature state"));

    JsonUtils.fromJson(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "region",
            REGION,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonFeature.class);
  }

  @Test
  public void testJsonDeserializationWhenRegionIsMissing() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, null, REPOSITORY, START);

    JSONAssert.assertEquals(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "repository",
            REPOSITORY,
            "startLevel",
            START),
        JsonUtils.toJson(jfeature),
        true);
  }

  @Test
  public void testJsonDeserializationWhenRepositoryIsMissing() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REGION, null, START);

    JSONAssert.assertEquals(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "startLevel",
            START),
        JsonUtils.toJson(jfeature),
        true);
  }

  @Test
  public void testJsonDeserializationWhenStartLevelIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required feature start level"));

    JsonUtils.fromJson(
        JsonUtilsTest.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION,
            "description",
            DESCRIPTION,
            "state",
            STATE,
            "region",
            REGION,
            "repository",
            REPOSITORY),
        JsonFeature.class);
  }
}
