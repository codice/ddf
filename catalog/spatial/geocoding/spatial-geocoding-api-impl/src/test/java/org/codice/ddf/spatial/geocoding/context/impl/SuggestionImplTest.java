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
package org.codice.ddf.spatial.geocoding.context.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.codice.ddf.spatial.geocoding.Suggestion;
import org.junit.Test;

public class SuggestionImplTest {

  @Test
  public void testConstructor() {
    Suggestion suggestion = new SuggestionImpl("id1", "name1");
    assertThat(suggestion.getId(), is("id1"));
    assertThat(suggestion.getName(), is("name1"));
  }
}
