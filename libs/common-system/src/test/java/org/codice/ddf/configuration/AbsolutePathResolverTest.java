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
package org.codice.ddf.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Paths;
import org.junit.Test;

public class AbsolutePathResolverTest {

  private static final AbsolutePathResolver TEST_APR =
      new AbsolutePathResolver(Paths.get("test", "path").toString());

  private static final String USER_DIRECTORY = System.getProperty("user.dir");

  private static final String DEFAULT_ABSOLUTE_PATH =
      Paths.get(USER_DIRECTORY, "test", "path").toString();

  @Test
  public void testNullPath() {
    System.clearProperty("ddf.home");
    AbsolutePathResolver apr = new AbsolutePathResolver(null);
    assertThat(apr.getPath(), nullValue());
  }

  @Test
  public void testNullPathWithRoot() {
    AbsolutePathResolver apr = new AbsolutePathResolver(null);
    assertThat(apr.getPath(USER_DIRECTORY), nullValue());
  }

  @Test
  public void getPathWhenDdfHomeNotSet() {
    System.clearProperty("ddf.home");
    assertThat(TEST_APR.getPath(), equalTo(DEFAULT_ABSOLUTE_PATH));
  }

  @Test
  public void testTransformedPath() {
    System.setProperty("ddf.home", Paths.get(USER_DIRECTORY, "testTransformedPath").toString());
    assertThat(
        TEST_APR.getPath(),
        equalTo(Paths.get(USER_DIRECTORY, "testTransformedPath", "test", "path").toString()));
  }

  @Test
  public void trailingFileSeparator() {
    System.setProperty(
        "ddf.home", Paths.get(USER_DIRECTORY, "trailingFileSeparator").toString() + File.separator);
    assertThat(
        new AbsolutePathResolver("test" + File.separator + "path" + File.separator).getPath(),
        equalTo(
            Paths.get(USER_DIRECTORY, "trailingFileSeparator", "test", "path").toString()
                + File.separator));
  }

  @Test
  public void getPathWithNullRootPath() {
    assertThat(TEST_APR.getPath(null), equalTo(DEFAULT_ABSOLUTE_PATH));
  }

  @Test
  public void getPathWithEmptyRootPath() {
    assertThat(TEST_APR.getPath(""), equalTo(DEFAULT_ABSOLUTE_PATH));
  }

  @Test
  public void getPathWithValidRootPath() {
    assertThat(
        TEST_APR.getPath(Paths.get(USER_DIRECTORY, "valid").toString()),
        equalTo(Paths.get(USER_DIRECTORY, "valid", "test", "path").toString()));
  }
}
