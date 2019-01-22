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
package org.codice.ddf.catalog.ui.forms.filter;

import static java.lang.String.format;
import static junit.framework.TestCase.fail;
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertLeafNode;
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertParentNode;
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.assertTemplatedNode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoaderTest;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.codice.ddf.catalog.ui.forms.builder.JsonModelBuilder;
import org.junit.Before;
import org.junit.Test;

public class TransformVisitorJsonTest {
  private static final URL FILTER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/filter2");

  private static final String DEPTH_PROP = "depth";

  private static final String DEPTH_VAL = "100";

  private TransformVisitor<FilterNode> visitor;

  @Before
  public void setup() {
    AttributeRegistry registry = mock(AttributeRegistry.class);
    when(registry.lookup(any())).thenReturn(Optional.empty());
    visitor = new TransformVisitor<>(new JsonModelBuilder(registry));
  }

  @Test
  public void testVisitPropertyIsEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "=", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitPropertyIsNotEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsNotEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "!=", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitPropertyIsLessThan() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsLessThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "<", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitPropertyIsLessThanOrEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsLessThanOrEqualTo.xml")
        .accept(visitor);
    assertLeafNode(visitor.getResult(), "<=", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitPropertyIsGreaterThan() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsGreaterThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), ">", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitPropertyIsGreaterThanOrEqualTo() throws Exception {
    getRootXmlFilterNode("comparison-binary-ops", "PropertyIsGreaterThanOrEqualTo.xml")
        .accept(visitor);
    assertLeafNode(visitor.getResult(), ">=", DEPTH_PROP, DEPTH_VAL, null);
  }

  @Test
  public void testVisitDWithin() throws Exception {
    VisitableElement xmlElem = getRootXmlFilterNode("spatial-distance-ops", "DWithin.xml");
    xmlElem.accept(visitor);
    assertLeafNode(visitor.getResult(), "DWITHIN", "location", "POINT(10, 10)", 150.50);
  }

  @Test
  public void testVisitIntersectsWithFunction() throws Exception {
    getRootXmlFilterNode("function-ops", "Intersects.xml").accept(visitor);
    assertTemplatedNode(visitor.getResult(), "INTERSECTS", "location", null, "id");
  }

  @Test
  public void testVariety2() throws Exception {
    getRootXmlFilterNode("hybrid", "hybrid-example-2.xml").accept(visitor);
    assertParentNode(visitor.getResult(), "AND", 6);
    assertLeafNode(visitor.getResult().getChildren().get(2), "ILIKE", "name", "Bob", null);
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
