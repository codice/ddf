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
package ddf.catalog.transformer;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import ddf.action.Action;
import ddf.catalog.data.impl.MetacardImpl;
import org.junit.Before;
import org.junit.Test;

public class OverlayActionProviderTest {
  private static final String TRANSFORMER_ID = "overlay.thumbnail";

  private OverlayActionProvider actionProvider;

  @Before
  public void setUp() {
    actionProvider = new OverlayActionProvider(metacard -> true, TRANSFORMER_ID);
  }

  private MetacardImpl getMetacard() {
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POLYGON ((1 10, 11 9, 10 -1, 0 0, 1 10))");
    return metacard;
  }

  @Test
  public void testCanHandleNonMetacard() {
    assertThat(actionProvider.canHandle("foo"), is(false));
  }

  @Test
  public void testCanHandleMetacardWithoutLocation() {
    assertThat(actionProvider.canHandle(new MetacardImpl()), is(false));
  }

  @Test
  public void testCanHandleMetacardWithUnhandleableGeometry() {
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("LINESTRING (30 10, 10 30, 40 40)");
    assertThat(actionProvider.canHandle(metacard), is(false));
  }

  @Test
  public void testCanHandleMetacardWithoutOverlayImage() {
    actionProvider = new OverlayActionProvider(metacard -> false, TRANSFORMER_ID);
    assertThat(actionProvider.canHandle(getMetacard()), is(false));
  }

  @Test
  public void testCanHandleMetacardWithOverlayImageAndLocation() {
    assertThat(actionProvider.canHandle(getMetacard()), is(true));
  }

  @Test
  public void testGetActionsForNonMetacard() {
    final Action action = actionProvider.getAction("foo");
    assertThat(action, is(nullValue()));
  }

  @Test
  public void testGetActions() {
    final MetacardImpl metacard = getMetacard();
    final String id = "abc123";
    metacard.setId(id);
    final String sourceId = "bar";
    metacard.setSourceId(sourceId);

    final Action action = actionProvider.getAction(metacard);
    assertThat(action, is(notNullValue()));

    assertThat(action.getId(), is("catalog.data.metacard.map.overlay.thumbnail"));
    assertThat(
        action.getUrl().toString(),
        endsWith("/catalog/sources/" + sourceId + "/" + id + "?transform=overlay.thumbnail"));
  }

  @Test
  public void testGetId() {
    assertThat(actionProvider.getId(), is("catalog.data.metacard.map.overlay.thumbnail"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullOverlayPredicate() {
    new OverlayActionProvider(null, TRANSFORMER_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullTransformerId() {
    new OverlayActionProvider(metacard -> true, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTransformerId() {
    new OverlayActionProvider(metacard -> true, "invalid");
  }
}
