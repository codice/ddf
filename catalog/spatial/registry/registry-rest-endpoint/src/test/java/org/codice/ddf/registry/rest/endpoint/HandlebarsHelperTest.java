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
package org.codice.ddf.registry.rest.endpoint;

import static org.junit.Assert.assertEquals;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Test;

public class HandlebarsHelperTest {

  @Test
  public void testHelperSameValue() throws IOException {
    HashMap map = new HashMap();
    map.put("String1", "someString");
    map.put("String2", "someString");
    assertEquals("True", inlineTemplate().apply(map));
  }

  @Test
  public void testHelperDifferentValue() throws IOException {
    HashMap map = new HashMap();
    map.put("String1", "someString");
    map.put("String2", "differentString");
    assertEquals("False", inlineTemplate().apply(map));
  }

  @Test
  public void testHelperNullValue() throws IOException {
    HashMap map = new HashMap();
    map.put("String1", "someString");
    map.put("String2", null);
    assertEquals("False", inlineTemplate().apply(map));
  }

  @Test
  public void testHelperNullValues() throws IOException {
    HashMap map = new HashMap();
    map.put("String1", null);
    map.put("String2", null);
    assertEquals("True", inlineTemplate().apply(map));
  }

  private Template inlineTemplate() throws IOException {
    Handlebars handlebars = new Handlebars();
    HandlebarsHelper helper = new HandlebarsHelper();
    handlebars.registerHelpers(helper);
    return handlebars.compileInline("{{#ifeq String1 String2 }}True{{else}}False{{/ifeq}}");
  }
}
