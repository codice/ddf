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
package org.codice.ddf.cache.plugin.metacardtags;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DefaultMetacardTagsPluginTest {

  private DefaultMetacardTagsPlugin setDefaultMetacardTags;

  private Metacard metacard;

  @Before
  public void setup() {
    metacard = mock(Metacard.class);
    setDefaultMetacardTags = new DefaultMetacardTagsPlugin();
  }

  @Test
  public void testMetacardWithTags() {

    Attribute attribute = mock(Attribute.class);
    when(attribute.getValues()).thenReturn(Collections.singletonList("aTag"));

    when(metacard.getAttribute(eq(Core.METACARD_TAGS))).thenReturn(attribute);

    Optional<Metacard> result = setDefaultMetacardTags.process(metacard);

    assertThat(result.isPresent(), is(true));

    verify(metacard, never()).setAttribute(any());
  }

  @Test
  public void testMetacardWithoutTags() {

    Optional<Metacard> result = setDefaultMetacardTags.process(metacard);

    assertThat(result.isPresent(), is(true));

    ArgumentCaptor<Attribute> captor = ArgumentCaptor.forClass(Attribute.class);

    verify(metacard, times(1)).setAttribute(captor.capture());

    assertThat(captor.getValue().getName(), is(Core.METACARD_TAGS));
    assertThat(captor.getValue().getValue(), is(Metacard.DEFAULT_TAG));
  }
}
