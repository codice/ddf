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
package org.codice.ddf.registry.schemabindings.builder.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;
import org.codice.ddf.registry.schemabindings.converter.type.PersonNameTypeConverter;
import org.junit.Before;
import org.junit.Test;

public class PersonNameTypeConverterTest {

  private static final String FIRST_NAME = "firstName";

  private static final String MIDDLE_NAME = "middleName";

  private static final String LAST_NAME = "lastName";

  private Map<String, Object> personMap = new HashMap<>();

  @Before
  public void setup() {
    personMap.put(FIRST_NAME, FIRST_NAME);
    personMap.put(MIDDLE_NAME, MIDDLE_NAME);
    personMap.put(LAST_NAME, LAST_NAME);
  }

  @Test
  public void testConvertFullName() throws Exception {
    PersonNameTypeConverter pntConverter = new PersonNameTypeConverter();

    Optional<PersonNameType> optionalPersonName = pntConverter.convert(personMap);
    assertThat(optionalPersonName, notNullValue());
    assertThat(optionalPersonName.isPresent(), is(true));

    PersonNameType personName = optionalPersonName.get();
    assertThat(personName, notNullValue());

    assertThat(personName.getFirstName(), is(equalTo(FIRST_NAME)));
    assertThat(personName.getMiddleName(), is(equalTo(MIDDLE_NAME)));
    assertThat(personName.getLastName(), is(equalTo(LAST_NAME)));
  }
}
