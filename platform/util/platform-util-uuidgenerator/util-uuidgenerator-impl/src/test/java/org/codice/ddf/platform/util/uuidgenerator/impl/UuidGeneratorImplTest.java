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
package org.codice.ddf.platform.util.uuidgenerator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class UuidGeneratorImplTest {

  private UuidGeneratorImpl uuidGenerator;

  private static final List<String> INVALID_ID_LIST_NO_HYPHENS =
      Arrays.asList(
          null,
          "",
          " ",
          "1234",
          "fc3f1798fe17_66fbbcc39dcfcb232e6",
          "fc3f1798fe17166fbbcc39dcfcb2232e6");

  private static final List<String> INVALID_ID_LIST_HYPHENS =
      Arrays.asList(
          "fc3f1798fe17166fbbcc39dcfcb2232e6----",
          "fc3f1798fe1766fbbcc39dcfcb232e6---1",
          "fc3f1798fe17166fbbcc39dcfcb2232e61234");

  @Before
  public void setUp() {
    uuidGenerator = new UuidGeneratorImpl();
  }

  @Test
  public void testGenerateIdRemoveHyphens() {
    String uuid = uuidGenerator.generateUuid();
    assertThat(uuid.length(), is(32));
    assertThat(uuidGenerator.validateUuid(uuid), is(true));
  }

  @Test
  public void testGenerateIdHyphens() {
    uuidGenerator.setRemoveHyphens(false);
    String uuid = uuidGenerator.generateUuid();
    assertThat(uuid.length(), is(36));
    assertThat(uuidGenerator.validateUuid(uuid), is(true));
  }

  @Test
  public void testValidateUuidNoHyphens() {
    for (String id : INVALID_ID_LIST_NO_HYPHENS) {
      assertThat(uuidGenerator.validateUuid(id), is(false));
    }
  }

  @Test
  public void testValidateUuid() {
    uuidGenerator.setRemoveHyphens(false);
    for (String id : INVALID_ID_LIST_HYPHENS) {
      assertThat(uuidGenerator.validateUuid(id), is(false));
    }
  }

  @Test
  public void testValidateUuidBothFormats() {
    uuidGenerator.setRemoveHyphens(false);
    assertThat(uuidGenerator.validateUuid(UUID.randomUUID().toString()), is(true));
    uuidGenerator.setRemoveHyphens(true);
    assertThat(uuidGenerator.validateUuid(UUID.randomUUID().toString()), is(false));
  }
}
