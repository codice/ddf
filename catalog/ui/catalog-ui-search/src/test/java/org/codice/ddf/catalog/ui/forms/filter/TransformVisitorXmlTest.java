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
import static org.codice.ddf.catalog.ui.forms.FilterNodeAssertionSupport.forElement;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import org.boon.Boon;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoaderTest;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.codice.ddf.catalog.ui.forms.builder.XmlModelBuilder;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeMapImpl;
import org.junit.Before;
import org.junit.Test;

public class TransformVisitorXmlTest {
  private static final URL FILTER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/filter-json");

  private static final String EXPECTED_DATE =
      Date.from(Instant.parse("2018-02-08T23:24:12.709Z")).toString();

  private static final String DEPTH_PROP = "depth";

  private static final String DEPTH_VAL = "100";

  private TransformVisitor<JAXBElement> visitor;

  @Before
  public void setup() {
    visitor = new TransformVisitor<>(new XmlModelBuilder());
  }

  @Test
  public void testVisitPropertyIsEqualTo() throws Exception {
    getRootJsonFilterNode("comparison-binary-ops", "Equal.json").accept(visitor);
    forElement(visitor.getResult())
        .withBinding(FilterType.class)
        .forElement(FilterType::getComparisonOps)
        .withBinding(BinaryComparisonOpType.class)
        .verifyExpression(BinaryComparisonOpType::getExpression)
        .withData(DEPTH_PROP, DEPTH_VAL);
  }

  @Test
  public void testVisitVarietyFilter1() throws Exception {
    getRootJsonFilterNode("hybrid", "hybrid-example-1.json").accept(visitor);
    forElement(visitor.getResult())
        .withBinding(FilterType.class)
        .forElement(FilterType::getLogicOps)
        .withBinding(BinaryLogicOpType.class)
        .withExpression(BinaryLogicOpType::getOps)
        .satisfies(
            child1 ->
                forElement(child1)
                    .withBinding(BinaryComparisonOpType.class)
                    .verifyExpression(BinaryComparisonOpType::getExpression)
                    .withData(DEPTH_PROP, DEPTH_VAL),
            child2 ->
                forElement(child2)
                    .withBinding(BinarySpatialOpType.class)
                    .verifyExpressionOrAny(BinarySpatialOpType::getExpressionOrAny)
                    .withData("anyGeo", "WKT()"));
  }

  @Test
  public void testVisitVarietyFilter2() throws Exception {
    getRootJsonFilterNode("hybrid", "hybrid-example-2.json").accept(visitor);
    assertVisitVarietyFilter(visitor, false);
  }

  @Test
  public void testVisitVarietyFilter3() throws Exception {
    getRootJsonFilterNode("hybrid", "hybrid-example-3.json").accept(visitor);
    assertVisitVarietyFilter(visitor, true);
  }

  private void assertVisitVarietyFilter(TransformVisitor<JAXBElement> visitor, boolean matchCase) {
    forElement(visitor.getResult())
        .withBinding(FilterType.class)
        .forElement(FilterType::getLogicOps)
        .withBinding(BinaryLogicOpType.class)
        .withExpression(BinaryLogicOpType::getOps)
        .satisfies(
            depth1Child1 ->
                forElement(depth1Child1)
                    .withBinding(BinaryComparisonOpType.class)
                    .verifyExpression(BinaryComparisonOpType::getExpression)
                    .withData(DEPTH_PROP, DEPTH_VAL),
            depth1Child2 ->
                forElement(depth1Child2)
                    .withBinding(BinaryLogicOpType.class)
                    .withExpression(BinaryLogicOpType::getOps)
                    .satisfies(
                        depth2Child1 ->
                            forElement(depth2Child1)
                                .withBinding(BinaryComparisonOpType.class)
                                .verifyExpression(BinaryComparisonOpType::getExpression)
                                .withData(DEPTH_PROP, DEPTH_VAL),
                        depth2Child2 ->
                            forElement(depth2Child2)
                                .withBinding(BinaryTemporalOpType.class)
                                .verifyExpressionOrAny(BinaryTemporalOpType::getExpressionOrAny)
                                .withData("created", EXPECTED_DATE),
                        depth2Child3 -> {
                          forElement(depth2Child3)
                              .withBinding(PropertyIsLikeType.class)
                              .verifyExpression(PropertyIsLikeType::getExpression)
                              .withData("name", "Bob");
                          forElement(depth2Child3)
                              .withBinding(PropertyIsLikeType.class)
                              .verifyMatchCase(PropertyIsLikeType::getMatchCase)
                              .withValue(matchCase);
                        }));
  }

  private static VisitableElement getRootJsonFilterNode(String... resourceRoute) throws Exception {
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

    File jsonFile = dir.toPath().resolve(route).toFile();
    if (!jsonFile.exists()) {
      fail("File was not found " + jsonFile.getAbsolutePath());
    }

    FilterNodeMapImpl node = new FilterNodeMapImpl(Boon.resourceMap(jsonFile.getPath()));
    return VisitableJsonElementImpl.create(node);
  }
}
