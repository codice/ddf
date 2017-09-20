/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 */
package ddf.catalog.data.impl;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import ddf.catalog.data.InjectableAttribute;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class InjectableAttributeImplTest {
  @Test(expected = IllegalArgumentException.class)
  public void testNullAttributeName() {
    new InjectableAttributeImpl(null, Collections.emptySet());
  }

  @Test
  public void testNullMetacardTypeCollection() {
    final String attribute = "attribute";
    InjectableAttribute injectableAttribute = new InjectableAttributeImpl(attribute, null);

    assertThat(injectableAttribute.attribute(), is(attribute));
    assertThat(injectableAttribute.metacardTypes(), is(empty()));
  }

  @Test
  public void testSpecificMetacardTypes() {
    final String attribute = "attribute";
    final Set<String> metacardTypes = Sets.newHashSet("type1", "type2", "type3");
    InjectableAttribute injectableAttribute = new InjectableAttributeImpl(attribute, metacardTypes);

    assertThat(injectableAttribute.attribute(), is(attribute));
    assertThat(injectableAttribute.metacardTypes(), is(metacardTypes));
  }
}
