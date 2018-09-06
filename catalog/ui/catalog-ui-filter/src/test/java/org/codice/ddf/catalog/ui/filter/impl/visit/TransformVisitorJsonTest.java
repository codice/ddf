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
package org.codice.ddf.catalog.ui.filter.impl.visit;

import static java.lang.String.format;
import static junit.framework.TestCase.fail;
import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertFunctionNode;
import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertLeafNode;
import static org.codice.ddf.catalog.ui.filter.FilterNodeAssertionSupport.assertParentNode;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.filter.VisitableElement;
import org.codice.ddf.catalog.ui.filter.impl.FilterReader;
import org.codice.ddf.catalog.ui.filter.impl.builder.JsonModelBuilder;
import org.junit.Before;
import org.junit.Test;

public class TransformVisitorJsonTest {
  private static final URL FILTER_RESOURCES_DIR =
      TransformVisitorJsonTest.class.getResource("/filter2");

  private static final String DEPTH_PROP = "depth";

  private static final Integer DEPTH_VAL = 100;

  private TransformVisitor<Map<String, Object>> visitor;

  @Before
  public void setup() {
    visitor = new TransformVisitor<>(new JsonModelBuilder());
  }

  @Test
  public void testVisitPropertyIsEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "=", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitPropertyIsNotEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsNotEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "!=", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitPropertyIsLessThan() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsLessThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "<", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitPropertyIsLessThanOrEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsLessThanOrEqualTo.xml")
        .accept(visitor);
    assertLeafNode(visitor.getResult(), "<=", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitPropertyIsGreaterThan() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsGreaterThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), ">", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitPropertyIsGreaterThanOrEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsGreaterThanOrEqualTo.xml")
        .accept(visitor);
    assertLeafNode(visitor.getResult(), ">=", DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitIntersectsWithFunction() throws Exception {
    List<Object> expectedArgs = new ArrayList<>();
    expectedArgs.add(null);
    expectedArgs.addAll(ImmutableList.of("id", true, false));
    getRootXmlFilterNode("function-ops", "Intersects.xml").accept(visitor);
    assertLeafNode(
        visitor.getResult(),
        "INTERSECTS",
        node -> assertThat(node, is("location")),
        node -> assertFunctionNode(node, "template.value.v1", expectedArgs));
  }

  @Test
  public void testVariety2() throws Exception {
    getRootXmlFilterNode("hybrid", "hybrid-example-2.xml").accept(visitor);
    assertParentNode(visitor.getResult(), "AND", 6);
    List<Object> children = (List<Object>) visitor.getResult().get("filters");
    assertThat(children, notNullValue());
    assertLeafNode(children.get(2), "ILIKE", "name", "Bob");
  }

  private static VisitableElement getRootXmlFilterNode(String... resourceRoute) throws Exception {
    File dir = new File(FILTER_RESOURCES_DIR.toURI());
    if (!dir.exists()) {
      fail(
          format(
              "Invalid setup parameter '%s', the directory does not exist",
              FILTER_RESOURCES_DIR.toString()));
    }

    Path route = Arrays.stream(resourceRoute).map(Paths::get).reduce(Path::resolve).orElse(null);
    if (route == null) {
      fail("Could not reduce resource route to a single path");
    }

    File xmlFile = dir.toPath().resolve(route).toFile();
    if (!xmlFile.exists()) {
      fail("File was not found " + xmlFile.getAbsolutePath());
    }

    JAXBElement<FilterType> filter =
        new FilterReader().unmarshalFilter(new FileInputStream(xmlFile));
    return VisitableXmlElementImpl.create(filter);
  }
}
