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
package org.codice.ddf.transformer.xml.streaming.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.validation.ValidationException;
import java.io.InputStream;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestGml3ToWktImpl {

  private Gml3ToWkt gtw;

  private String createWktPoint(int x, int y) {
    return String.format("POINT (%s %s)", x, y);
  }

  private String createGmlPoint(int x, int y) {
    return String.format(
        "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos>%s %s</gml:pos></gml:Point>",
        x, y);
  }

  @Before
  public void setUp() throws Exception {
    gtw = new Gml3ToWktImpl(new XmlParser());
  }

  @Test
  public void testGmlPointToWkt() throws ValidationException {
    assertThat(gtw.convert(createGmlPoint(0, 0)), is(createWktPoint(0, 0)));
    assertThat(gtw.convert(createGmlPoint(0, 1)), is(createWktPoint(0, 1)));
    assertThat(gtw.convert(createGmlPoint(1, 0)), is(createWktPoint(1, 0)));
    assertThat(gtw.convert(createGmlPoint(1, 1)), is(createWktPoint(1, 1)));
  }

  @Test(expected = ValidationException.class)
  public void testBadParser() throws ParserException, ValidationException {
    Parser parser = mock(XmlParser.class);
    when(parser.configureParser(anyList(), any(ClassLoader.class))).thenCallRealMethod();
    when(parser.unmarshal(any(ParserConfigurator.class), any(Class.class), any(InputStream.class)))
        .thenThrow(new ParserException());
    gtw = new Gml3ToWktImpl(parser);
    gtw.convert(createGmlPoint(0, 0));
  }
}
