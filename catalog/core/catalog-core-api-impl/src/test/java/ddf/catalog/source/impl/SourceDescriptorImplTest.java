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
package ddf.catalog.source.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import ddf.action.Action;
import java.util.Collections;
import org.junit.Test;

public class SourceDescriptorImplTest {

  @Test
  public void testGetActions() {
    Action action = mock(Action.class);
    SourceDescriptorImpl sourceDescriptor =
        new SourceDescriptorImpl("", Collections.emptySet(), Collections.singletonList(action));
    assertThat(sourceDescriptor.getActions(), is(Collections.singletonList(action)));
  }
}
