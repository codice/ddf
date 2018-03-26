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
package org.codice.ddf.catalog.ui.forms.model;

import static java.lang.String.format;
import static junit.framework.TestCase.fail;
import static org.codice.ddf.catalog.ui.forms.model.FilterNodeAssertionSupport.assertLeafNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoaderTest;
import org.codice.ddf.catalog.ui.forms.filter.VisitableFilterNode;
import org.codice.ddf.catalog.ui.forms.filter.VisitableXmlElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JsonTransformVisitorTest {
  private static final URL FILTER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/filter2");

  private JsonTransformVisitor visitor;

  @Before
  public void setup() {
    visitor = new JsonTransformVisitor();
  }

  @Test
  public void testVisitPropertyIsEqualTo() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "=", "depth", "100");
  }

  @Test
  public void testVisitPropertyIsNotEqualTo() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsNotEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "!=", "depth", "100");
  }

  @Test
  public void testVisitPropertyIsLessThan() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsLessThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "<", "depth", "100");
  }

  @Test
  public void testVisitPropertyIsLessThanOrEqualTo() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsLessThanOrEqualTo.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), "<=", "depth", "100");
  }

  @Test
  public void testVisitPropertyIsGreaterThan() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsGreaterThan.xml").accept(visitor);
    assertLeafNode(visitor.getResult(), ">", "depth", "100");
  }

  @Test
  public void testVisitPropertyIsGreaterThanOrEqualTo() throws Exception {
    getRootFilterNode("comparison-binary-ops", "PropertyIsGreaterThanOrEqualTo.xml")
        .accept(visitor);
    assertLeafNode(visitor.getResult(), ">=", "depth", "100");
  }

  private static VisitableXmlElement getRootFilterNode(String... resourceRoute) throws Exception {
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

    return new VisitableFilterNode(
        new FilterReader().unmarshal(new FileInputStream(xmlFile), FilterType.class));
  }

  private static class FilterReader {
    private final JAXBContext context;

    public FilterReader() throws JAXBException {
      String pkgName = FilterType.class.getPackage().getName();
      this.context = JAXBContext.newInstance(format("%s:%s", pkgName, pkgName));
    }

    @SuppressWarnings("unchecked")
    public <T> JAXBElement<T> unmarshal(InputStream inputStream, Class<T> tClass)
        throws JAXBException {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      Object result = unmarshaller.unmarshal(inputStream);
      if (result instanceof JAXBElement) {
        JAXBElement element = (JAXBElement) result;
        if (tClass.isInstance(element.getValue())) {
          return (JAXBElement<T>) element;
        }
      }
      return null;
    }
  }
}
