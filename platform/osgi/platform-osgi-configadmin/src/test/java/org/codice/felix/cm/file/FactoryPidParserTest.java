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
package org.codice.felix.cm.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class FactoryPidParserTest {
  @Test
  public void parseFactoryPidInCreateFactoryConfigurationFormat() {
    final var uuid = UUID.randomUUID();
    final var parsed = FactoryPidParser.parseFactoryParts("org.codice.FactoryService." + uuid);
    assertTrue("The Optional should not be empty.", parsed.isPresent());
    assertThat(parsed.get().factoryPid(), is("org.codice.FactoryService"));
    assertThat(parsed.get().serviceName(), is(uuid.toString()));
  }

  @Test
  public void parseFactoryPidInGetFactoryConfigurationFormat() {
    final var name = "foobar";
    final var parsed = FactoryPidParser.parseFactoryParts("org.codice.FactoryService~" + name);
    assertTrue("The Optional should not be empty.", parsed.isPresent());
    assertThat(parsed.get().factoryPid(), is("org.codice.FactoryService"));
    assertThat(parsed.get().serviceName(), is(name));
  }

  @Test
  public void parseNullFactoryPid() {
    final var parsed = FactoryPidParser.parseFactoryParts(null);
    assertTrue("The Optional should be empty.", parsed.isEmpty());
  }

  @Test
  public void parseNonFactoryPid() {
    final var parsed = FactoryPidParser.parseFactoryParts("org.codice.Service");
    assertTrue("The Optional should be empty.", parsed.isEmpty());
  }
}
