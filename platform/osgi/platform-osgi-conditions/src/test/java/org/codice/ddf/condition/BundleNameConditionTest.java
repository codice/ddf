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
package org.codice.ddf.condition;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class BundleNameConditionTest {

  @Test
  public void testIsSatisfied() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle2");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(),
                new String[] {"bundle1", "bundle2", "bundle3"}));
    boolean satisfied = bundleNameCondition.isSatisfied();
    // no cache this time around
    assertThat(satisfied, is(true));
    satisfied = bundleNameCondition.isSatisfied();
    // cached
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsNotSatisfied() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle4");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(),
                new String[] {"bundle1", "bundle2", "bundle3"}));
    boolean satisfied = bundleNameCondition.isSatisfied();
    // no cache this time around
    assertThat(satisfied, is(false));
    satisfied = bundleNameCondition.isSatisfied();
    // cached
    assertThat(satisfied, is(false));
  }

  @Test
  public void testIsNotSatisfiedNoArgs() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle4");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition =
        new BundleNameCondition(
            bundle, new ConditionInfo(BundleNameCondition.class.getName(), new String[0]));
    boolean satisfied = bundleNameCondition.isSatisfied();
    // no cache this time around
    assertThat(satisfied, is(false));
  }

  @Test
  public void testIsAllSatisfied() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle2");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition2 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    bundle = mock(Bundle.class);
    headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle1");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition1 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    boolean satisfied =
        bundleNameCondition1.isSatisfied(
            new Condition[] {bundleNameCondition1, bundleNameCondition2}, null);
    assertThat(satisfied, is(true));
    // cached
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsAllNotSatisfied() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle3");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition2 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    bundle = mock(Bundle.class);
    headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle4");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition1 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    boolean satisfied =
        bundleNameCondition1.isSatisfied(
            new Condition[] {bundleNameCondition1, bundleNameCondition2}, null);
    assertThat(satisfied, is(false));
  }

  @Test
  public void testSomeSatisfied() {
    Bundle bundle = mock(Bundle.class);
    Dictionary<String, String> headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle1");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition2 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    bundle = mock(Bundle.class);
    headers = new Hashtable<>();
    headers.put("Bundle-SymbolicName", "bundle4");
    when(bundle.getHeaders()).thenReturn(headers);
    BundleNameCondition bundleNameCondition1 =
        new BundleNameCondition(
            bundle,
            new ConditionInfo(
                BundleNameCondition.class.getName(), new String[] {"bundle1", "bundle2"}));

    boolean satisfied =
        bundleNameCondition1.isSatisfied(
            new Condition[] {bundleNameCondition1, bundleNameCondition2}, null);
    assertThat(satisfied, is(false));
  }
}
