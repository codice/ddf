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
package org.codice.ddf.platform.serviceflag.inputtransformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class InputTransformerIdsTest {

  private Path testValidTransformerPath =
      Paths.get(
          new File(getClass().getClassLoader().getResource("valid-transformers").getFile())
              .getAbsolutePath());

  private Path testInvalidTransformerPath =
      Paths.get(
          new File(getClass().getClassLoader().getResource("invalid-transformers").getFile())
              .getAbsolutePath());

  @Test
  public void testNoInputTransformerFiles() {
    Set<String> transformerIds = new InputTransformerIds(Paths.get("doesntexist")).getIds();
    assertThat(transformerIds, equalTo(Collections.emptySet()));
  }

  @Test
  public void testInputTransformerFiles() {
    Set<String> transformerIds = new InputTransformerIds(testValidTransformerPath).getIds();
    assertThat(transformerIds, hasItems("id1", "id2", "id3"));
  }

  @Test(expected = IllegalStateException.class)
  public void testInvalidInputTransformerFile() {
    new InputTransformerIds(testInvalidTransformerPath).getIds();
  }
}
