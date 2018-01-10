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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import org.apache.karaf.features.FeatureState;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonProfileTest {

  private static final long BUNDLE_ID = 14235L;
  private static final String BUNDLE_NAME = "bundle.test.name";
  private static final Version BUNDLE_VERSION = new Version(1, 2, 3, "what");
  private static final int BUNDLE_STATE = Bundle.STARTING;
  private static final String BUNDLE_LOCATION = "bundle.test.location";
  private static final long BUNDLE_ID2 = 35L; // keep it less then BUNDLE_ID to verify no sorting
  private static final String BUNDLE_NAME2 = "bundle.test.name2";
  private static final Version BUNDLE_VERSION2 = new Version(2, 2, 3, "what2");
  private static final int BUNDLE_STATE2 = Bundle.INSTALLED;
  private static final String BUNDLE_LOCATION2 = "bundle.test.location2";

  private static final String FEATURE_NAME = "feature.test.name";
  private static final String FEATURE_ID = "feature.test.id";
  private static final String FEATURE_VERSION = "1.2";
  private static final String FEATURE_DESCRIPTION = "feature.test.description";
  private static final FeatureState FEATURE_STATE = FeatureState.Installed;
  private static final boolean FEATURE_REQUIRED = false;
  private static final String FEATURE_REGION = "feature.test.region";
  private static final String FEATURE_REPOSITORY = "feature.test.repo";
  private static final int FEATURE_START = 55;
  private static final String FEATURE_NAME2 = "feature.test.name2";
  private static final String FEATURE_ID2 = "feature.test.id2";
  private static final FeatureState FEATURE_STATE2 = FeatureState.Resolved;
  private static final boolean FEATURE_REQUIRED2 = true;
  private static final int FEATURE_START2 = 22;

  private final JsonBundle jbundle =
      new JsonBundle(BUNDLE_NAME, BUNDLE_VERSION, BUNDLE_ID, BUNDLE_STATE, BUNDLE_LOCATION);
  private final JsonBundle jbundle2 =
      new JsonBundle(BUNDLE_NAME2, BUNDLE_VERSION2, BUNDLE_ID2, BUNDLE_STATE2, BUNDLE_LOCATION2);

  private final JsonFeature jfeature =
      new JsonFeature(
          FEATURE_NAME,
          FEATURE_ID,
          FEATURE_VERSION,
          FEATURE_DESCRIPTION,
          FEATURE_STATE,
          FEATURE_REQUIRED,
          FEATURE_REGION,
          FEATURE_REPOSITORY,
          FEATURE_START);
  private final JsonFeature jfeature2 =
      new JsonFeature(
          FEATURE_NAME2,
          FEATURE_ID2,
          null,
          null,
          FEATURE_STATE2,
          FEATURE_REQUIRED2,
          null,
          null,
          FEATURE_START2);

  private final JsonProfile jprofile =
      new JsonProfile(Arrays.asList(jfeature, jfeature2), Arrays.asList(jbundle, jbundle2));

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(jprofile.features().toArray(), Matchers.arrayContaining(jfeature, jfeature2));
    Assert.assertThat(jprofile.bundles().toArray(), Matchers.arrayContaining(jbundle2, jbundle));
  }

  @Test
  public void testHashCodeWhenEquals() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Arrays.asList(jbundle, jbundle2));

    Assert.assertThat(jprofile.hashCode(), Matchers.equalTo(jprofile2.hashCode()));
  }

  @Test
  public void testHashCodeWhenFeaturesAreDifferent() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature2), Arrays.asList(jbundle, jbundle2));

    Assert.assertThat(jprofile.hashCode(), Matchers.not(Matchers.equalTo(jprofile2.hashCode())));
  }

  @Test
  public void testHashCodeWhenBundlesAreDifferent() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Arrays.asList(jbundle2));

    Assert.assertThat(jprofile.hashCode(), Matchers.not(Matchers.equalTo(jprofile2.hashCode())));
  }

  @Test
  public void testEqualsWhenEquals() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Arrays.asList(jbundle, jbundle2));

    Assert.assertThat(jprofile.equals(jprofile2), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWhenIdentical() throws Exception {
    Assert.assertThat(jprofile.equals(jprofile), Matchers.equalTo(true));
  }

  @SuppressWarnings("PMD.EqualsNull" /* purposely testing equals() when called with null */)
  @Test
  public void testEqualsWhenNull() throws Exception {
    Assert.assertThat(jprofile.equals(null), Matchers.equalTo(false));
  }

  @SuppressWarnings(
      "PMD.PositionLiteralsFirstInComparisons" /* purposely testing equals() when call with something else than expected */)
  @Test
  public void testEqualsWhenNotABundle() throws Exception {
    Assert.assertThat(jprofile.equals("test"), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenFeaturesAreDifferent() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature), Arrays.asList(jbundle, jbundle2));

    Assert.assertThat(jprofile.equals(jprofile2), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenBundlesAreDifferent() throws Exception {
    final JsonProfile jprofile2 =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Arrays.asList(jbundle));

    Assert.assertThat(jprofile.equals(jprofile2), Matchers.equalTo(false));
  }

  @Test
  public void testJsonSerialization() throws Exception {
    JSONAssert.assertEquals(
        JsonSupport.toJsonString(
            "features",
            ImmutableList.of(
                JsonSupport.toImmutableMap(
                    "name",
                    FEATURE_NAME,
                    "id",
                    FEATURE_ID,
                    "version",
                    FEATURE_VERSION,
                    "description",
                    FEATURE_DESCRIPTION,
                    "state",
                    FEATURE_STATE,
                    "required",
                    FEATURE_REQUIRED,
                    "region",
                    FEATURE_REGION,
                    "repository",
                    FEATURE_REPOSITORY,
                    "startLevel",
                    FEATURE_START),
                JsonSupport.toImmutableMap(
                    "name",
                    FEATURE_NAME2,
                    "id",
                    FEATURE_ID2,
                    "state",
                    FEATURE_STATE2,
                    "required",
                    FEATURE_REQUIRED2,
                    "startLevel",
                    FEATURE_START2)),
            "bundles",
            ImmutableList.of(
                // order should not be reversed since serialization will not be sorting bundles
                ImmutableMap.of(
                    "name",
                    BUNDLE_NAME,
                    "id",
                    BUNDLE_ID,
                    "version",
                    BUNDLE_VERSION,
                    "state",
                    BUNDLE_STATE,
                    "location",
                    BUNDLE_LOCATION),
                ImmutableMap.of(
                    "name",
                    BUNDLE_NAME2,
                    "id",
                    BUNDLE_ID2,
                    "version",
                    BUNDLE_VERSION2,
                    "state",
                    BUNDLE_STATE2,
                    "location",
                    BUNDLE_LOCATION2))),
        JsonUtils.toJson(jprofile),
        true);
  }

  @Test
  public void testJsonSerializationWithNoFeatures() throws Exception {
    final JsonProfile jprofile =
        new JsonProfile(Collections.emptyList(), Collections.singletonList(jbundle));

    JSONAssert.assertEquals(
        JsonSupport.toJsonString(
            "bundles",
            Collections.singletonList(
                ImmutableMap.of(
                    "name",
                    BUNDLE_NAME,
                    "id",
                    BUNDLE_ID,
                    "version",
                    BUNDLE_VERSION,
                    "state",
                    BUNDLE_STATE,
                    "location",
                    BUNDLE_LOCATION))),
        JsonUtils.toJson(jprofile),
        true);
  }

  @Test
  public void testJsonSerializationWithNoBundles() throws Exception {
    final JsonProfile jprofile = new JsonProfile(Arrays.asList(jfeature), Collections.emptyList());

    JSONAssert.assertEquals(
        JsonSupport.toJsonString(
            "features",
            Collections.singletonList(
                JsonSupport.toImmutableMap(
                    "name",
                    FEATURE_NAME,
                    "id",
                    FEATURE_ID,
                    "version",
                    FEATURE_VERSION,
                    "description",
                    FEATURE_DESCRIPTION,
                    "state",
                    FEATURE_STATE,
                    "required",
                    FEATURE_REQUIRED,
                    "region",
                    FEATURE_REGION,
                    "repository",
                    FEATURE_REPOSITORY,
                    "startLevel",
                    FEATURE_START))),
        JsonUtils.toJson(jprofile),
        true);
  }

  @Test
  public void testJsonDeserialization() throws Exception {
    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "features",
                ImmutableList.of(
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME,
                        "id",
                        FEATURE_ID,
                        "version",
                        FEATURE_VERSION,
                        "description",
                        FEATURE_DESCRIPTION,
                        "state",
                        FEATURE_STATE,
                        "required",
                        FEATURE_REQUIRED,
                        "region",
                        FEATURE_REGION,
                        "repository",
                        FEATURE_REPOSITORY,
                        "startLevel",
                        FEATURE_START),
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME2,
                        "id",
                        FEATURE_ID2,
                        "state",
                        FEATURE_STATE2,
                        "required",
                        FEATURE_REQUIRED2,
                        "startLevel",
                        FEATURE_START2)),
                "bundles",
                ImmutableList.of(
                    ImmutableMap.of(
                        "name",
                        BUNDLE_NAME,
                        "id",
                        BUNDLE_ID,
                        "version",
                        BUNDLE_VERSION,
                        "state",
                        BUNDLE_STATE,
                        "location",
                        BUNDLE_LOCATION),
                    ImmutableMap.of(
                        "name",
                        BUNDLE_NAME2,
                        "id",
                        BUNDLE_ID2,
                        "version",
                        BUNDLE_VERSION2,
                        "state",
                        BUNDLE_STATE2,
                        "location",
                        BUNDLE_LOCATION2))),
            JsonProfile.class),
        Matchers.equalTo(jprofile));
  }

  @Test
  public void testJsonDeserializationWhenFeaturesAreMissing() throws Exception {
    final JsonProfile jprofile =
        new JsonProfile(Collections.emptyList(), Arrays.asList(jbundle, jbundle2));

    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "features",
                Collections.emptyList(),
                "bundles",
                ImmutableList.of(
                    ImmutableMap.of(
                        "name",
                        BUNDLE_NAME,
                        "id",
                        BUNDLE_ID,
                        "version",
                        BUNDLE_VERSION,
                        "state",
                        BUNDLE_STATE,
                        "location",
                        BUNDLE_LOCATION),
                    ImmutableMap.of(
                        "name",
                        BUNDLE_NAME2,
                        "id",
                        BUNDLE_ID2,
                        "version",
                        BUNDLE_VERSION2,
                        "state",
                        BUNDLE_STATE2,
                        "location",
                        BUNDLE_LOCATION2))),
            JsonProfile.class),
        Matchers.equalTo(jprofile));
  }

  @Test
  public void testJsonDeserializationWithInvalidFeatures() throws Exception {
    final JsonProfile jprofile =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Collections.emptyList());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required feature id"));

    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "features",
                ImmutableList.of(
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME,
                        "version",
                        FEATURE_VERSION,
                        "description",
                        FEATURE_DESCRIPTION,
                        "state",
                        FEATURE_STATE,
                        "required",
                        FEATURE_REQUIRED,
                        "region",
                        FEATURE_REGION,
                        "repository",
                        FEATURE_REPOSITORY,
                        "startLevel",
                        FEATURE_START),
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME2,
                        "id",
                        FEATURE_ID2,
                        "state",
                        FEATURE_STATE2,
                        "startLevel",
                        FEATURE_START2)),
                "bundles",
                Collections.emptyList()),
            JsonProfile.class),
        Matchers.equalTo(jprofile));
  }

  @Test
  public void testJsonDeserializationWhenBundlesAreMissing() throws Exception {
    final JsonProfile jprofile =
        new JsonProfile(Arrays.asList(jfeature, jfeature2), Collections.emptyList());

    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "features",
                ImmutableList.of(
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME,
                        "id",
                        FEATURE_ID,
                        "version",
                        FEATURE_VERSION,
                        "description",
                        FEATURE_DESCRIPTION,
                        "state",
                        FEATURE_STATE,
                        "required",
                        FEATURE_REQUIRED,
                        "region",
                        FEATURE_REGION,
                        "repository",
                        FEATURE_REPOSITORY,
                        "startLevel",
                        FEATURE_START),
                    JsonSupport.toImmutableMap(
                        "name",
                        FEATURE_NAME2,
                        "id",
                        FEATURE_ID2,
                        "state",
                        FEATURE_STATE2,
                        "required",
                        FEATURE_REQUIRED2,
                        "startLevel",
                        FEATURE_START2)),
                "bundles",
                Collections.emptyList()),
            JsonProfile.class),
        Matchers.equalTo(jprofile));
  }

  @Test
  public void testJsonDeserializationWithInvalidBundles() throws Exception {
    final JsonProfile jprofile =
        new JsonProfile(Collections.emptyList(), Arrays.asList(jbundle, jbundle2));

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle name"));

    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "features",
                Collections.emptyList(),
                "bundles",
                ImmutableList.of(
                    ImmutableMap.of(
                        "id",
                        BUNDLE_ID,
                        "version",
                        BUNDLE_VERSION,
                        "state",
                        BUNDLE_STATE,
                        "location",
                        BUNDLE_LOCATION),
                    ImmutableMap.of(
                        "name",
                        BUNDLE_NAME2,
                        "id",
                        BUNDLE_ID2,
                        "version",
                        BUNDLE_VERSION2,
                        "state",
                        BUNDLE_STATE2,
                        "location",
                        BUNDLE_LOCATION2))),
            JsonProfile.class),
        Matchers.equalTo(jprofile));
  }
}
