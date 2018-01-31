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
package org.codice.ddf.parser.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.codice.ddf.parser.ParserConfigurator;
import org.junit.Before;
import org.junit.Test;

public class XmlParserConfiguratorTest {
  private ParserConfigurator pc;

  private ValidationEventHandler testHandler;

  private XmlAdapter testAdapter;

  @Before
  public void setup() {
    pc = new XmlParserConfigurator();

    testHandler =
        new ValidationEventHandler() {
          @Override
          public boolean handleEvent(ValidationEvent event) {
            return false;
          }
        };

    testAdapter =
        new XmlAdapter() {
          @Override
          public Object unmarshal(Object v) throws Exception {
            return null;
          }

          @Override
          public Object marshal(Object v) throws Exception {
            return null;
          }
        };
  }

  @Test
  public void testContextPath() {
    List<String> inputPaths = ImmutableList.of("hello", "world", "goodnight", "columbus");

    pc.setContextPath(inputPaths);
    List<String> contextPath = pc.getContextPath();

    assertEquals(inputPaths.size(), contextPath.size());

    for (int i = 0; i < inputPaths.size(); i++) {
      assertEquals(inputPaths.get(i), contextPath.get(i));
    }
  }

  @Test
  public void testClassLoader() {
    pc.setClassLoader(null);
    assertNull(pc.getClassLoader());

    pc.setClassLoader(XmlParserConfiguratorTest.class.getClassLoader());
    assertEquals(XmlParserConfiguratorTest.class.getClassLoader(), pc.getClassLoader());
  }

  @Test
  public void testValidationEventHandler() {
    pc.setHandler(testHandler);
    assertEquals(testHandler, pc.getHandler());
  }

  @Test
  public void testAdapter() {
    pc.setAdapter(testAdapter);
    assertEquals(testAdapter, pc.getAdapter());
  }

  @Test
  public void testProperties() {
    assertNotNull(pc.getProperties());
    assertEquals(0, pc.getProperties().size());

    pc.addProperty("aaa", 123);
    assertEquals(1, pc.getProperties().size());
    assertEquals(123, pc.getProperties().get("aaa"));
    pc.addProperty(null, "This will not add");
    assertEquals(1, pc.getProperties().size());

    pc.addProperties(ImmutableMap.<String, Object>of("bbb", 2, "ccc", 3));
    assertEquals(3, pc.getProperties().size());
    assertTrue(pc.getProperties().containsKey("aaa"));
    assertTrue(pc.getProperties().containsKey("bbb"));
    assertTrue(pc.getProperties().containsKey("ccc"));
  }
}
