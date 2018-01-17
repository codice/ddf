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

import org.codice.ddf.admin.application.service.migratable.JsonBundle.SimpleState;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonBundleTest {

  private static final long ID = 14235L;
  private static final String NAME = "test.name";
  private static final Version VERSION = new Version(1, 2, 3, "what");
  private static final int STATE = Bundle.STARTING;
  private static final String LOCATION = "test.location";

  private final JsonBundle jbundle = new JsonBundle(NAME, VERSION.toString(), ID, STATE, LOCATION);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGetNameFromBundle() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getSymbolicName()).thenReturn(NAME);
    Mockito.when(bundle.getVersion()).thenReturn(VERSION);

    Assert.assertThat(JsonBundle.getFullName(bundle), Matchers.equalTo(NAME + '/' + VERSION));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenUninstalled() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.UNINSTALLED));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenStarting() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.STARTING);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.ACTIVE));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenActive() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.ACTIVE);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.ACTIVE));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenInstalled() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.INSTALLED);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.INSTALLED));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenResolved() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.RESOLVED);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.INSTALLED));
  }

  @Test
  public void testGetSimpleStateFromBundleWhenStopping() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getState()).thenReturn(Bundle.STOPPING);

    Assert.assertThat(JsonBundle.getSimpleState(bundle), Matchers.equalTo(SimpleState.INSTALLED));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(jbundle.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jbundle.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jbundle.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jbundle.getLocation(), Matchers.equalTo(LOCATION));
    Assert.assertThat(jbundle.getVersion(), Matchers.equalTo(VERSION));
  }

  @Test
  public void testConstructorWhenNameIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null name"));

    new JsonBundle(null, VERSION.toString(), ID, STATE, LOCATION);
  }

  @Test
  public void testConstructorWhenVersionIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null version"));

    new JsonBundle(NAME, (String) null, ID, STATE, LOCATION);
  }

  @Test
  public void testConstructorWhenLocationIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null location"));

    new JsonBundle(NAME, VERSION.toString(), ID, STATE, null);
  }

  @Test
  public void testConstructorWhenVersionIsInvalid() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid version"));

    new JsonBundle(NAME, "a.3.bcd", ID, STATE, LOCATION);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullBundle() throws Exception {
    new JsonBundle(null);
  }

  @Test
  public void testConstructorWithBundle() throws Exception {
    final Bundle bundle = Mockito.mock(Bundle.class);

    Mockito.when(bundle.getBundleId()).thenReturn(ID);
    Mockito.when(bundle.getSymbolicName()).thenReturn(NAME);
    Mockito.when(bundle.getVersion()).thenReturn(VERSION);
    Mockito.when(bundle.getState()).thenReturn(STATE);
    Mockito.when(bundle.getLocation()).thenReturn(LOCATION);

    final JsonBundle jbundle = new JsonBundle(bundle);

    Assert.assertThat(jbundle.getName(), Matchers.equalTo(NAME));
    Assert.assertThat(jbundle.getId(), Matchers.equalTo(ID));
    Assert.assertThat(jbundle.getState(), Matchers.equalTo(STATE));
    Assert.assertThat(jbundle.getLocation(), Matchers.equalTo(LOCATION));
    Assert.assertThat(jbundle.getVersion(), Matchers.equalTo(VERSION));
  }

  @Test
  public void testGetSimpleState() throws Exception {
    Assert.assertThat(jbundle.getSimpleState(), Matchers.equalTo(SimpleState.ACTIVE));
  }

  @Test
  public void testGetName() throws Exception {
    Assert.assertThat(jbundle.getFullName(), Matchers.equalTo(NAME + '/' + VERSION));
  }

  @Test
  public void testHashCodeWhenEquals() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);

    Assert.assertThat(jbundle.hashCode(), Matchers.equalTo(jbundle2.hashCode()));
  }

  @Test
  public void testHashCodeWhenNameIsDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME + "2", VERSION, ID, STATE, LOCATION);

    Assert.assertThat(jbundle.hashCode(), Matchers.not(Matchers.equalTo(jbundle2.hashCode())));
  }

  @Test
  public void testHashCodeWhenVersionIsDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION + "2", ID, STATE, LOCATION);

    Assert.assertThat(jbundle.hashCode(), Matchers.not(Matchers.equalTo(jbundle2.hashCode())));
  }

  @Test
  public void tesEqualsWhenEquals() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(true));
  }

  @Test
  public void tesEqualsWhenIdentical() throws Exception {
    Assert.assertThat(jbundle.equals(jbundle), Matchers.equalTo(true));
  }

  @SuppressWarnings("PMD.EqualsNull" /* purposely testing equals() when called with null */)
  @Test
  public void tesEqualsWhenNull() throws Exception {
    Assert.assertThat(jbundle.equals(null), Matchers.equalTo(false));
  }

  @SuppressWarnings(
      "PMD.PositionLiteralsFirstInComparisons" /* purposely testing equals() when call with something else than expected */)
  @Test
  public void tesEqualsWhenNotABundle() throws Exception {
    Assert.assertThat(jbundle.equals("test"), Matchers.equalTo(false));
  }

  @Test
  public void tesEqualsWhenNamesAreDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME + "2", VERSION, ID, STATE, LOCATION);

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(false));
  }

  @Test
  public void tesEqualsWhenVersionsAreDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION + "2", ID, STATE, LOCATION);

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(false));
  }

  @Test
  public void tesEqualsWhenIdsAreDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION, ID + 2L, STATE, LOCATION);

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(false));
  }

  @Test
  public void tesEqualsWhenStatesAreDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION, ID, STATE + 2, LOCATION);

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(false));
  }

  @Test
  public void tesEqualsWhenLocationsAreDifferent() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION + "2");

    Assert.assertThat(jbundle.equals(jbundle2), Matchers.equalTo(false));
  }

  @Test
  public void testToString() throws Exception {
    Assert.assertThat(
        jbundle.toString(), Matchers.equalTo("bundle [" + NAME + '/' + VERSION + ']'));
  }

  @Test
  public void testJsonSerialization() throws Exception {
    JSONAssert.assertEquals(
        JsonSupport.toJsonString(
            "name", NAME, "id", ID, "version", VERSION, "state", STATE, "location", LOCATION),
        JsonUtils.toJson(jbundle),
        true);
  }

  @Test
  public void testJsonDeserialization() throws Exception {
    Assert.assertThat(
        JsonUtils.fromJson(
            JsonSupport.toJsonString(
                "name", NAME, "id", ID, "version", VERSION, "state", STATE, "location", LOCATION),
            JsonBundle.class),
        Matchers.equalTo(jbundle));
  }

  @Test
  public void testJsonDeserializationWhenNameIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle name"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString(
            "id", ID, "version", VERSION, "state", STATE, "location", LOCATION),
        JsonBundle.class);
  }

  @Test
  public void testJsonDeserializationWhenVersionIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle version"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString("name", NAME, "id", ID, "state", STATE, "location", LOCATION),
        JsonBundle.class);
  }

  @Test
  public void testJsonDeserializationWhenVersionIsInvalid() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid version"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString(
            "name",
            NAME,
            "id",
            ID,
            "version",
            VERSION + ".2",
            "state",
            STATE,
            "location",
            LOCATION),
        JsonBundle.class);
  }

  @Test
  public void testJsonDeserializationWhenIdIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle id"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString(
            "name", NAME, "version", VERSION, "state", STATE, "location", LOCATION),
        JsonBundle.class);
  }

  @Test
  public void testJsonDeserializationWhenStateIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle state"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString("name", NAME, "id", ID, "version", VERSION, "location", LOCATION),
        JsonBundle.class);
  }

  @Test
  public void testJsonDeserializationWhenLocationIsMissing() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("missing required bundle location"));

    JsonUtils.fromJson(
        JsonSupport.toJsonString("name", NAME, "id", ID, "version", VERSION, "state", STATE),
        JsonBundle.class);
  }
}
