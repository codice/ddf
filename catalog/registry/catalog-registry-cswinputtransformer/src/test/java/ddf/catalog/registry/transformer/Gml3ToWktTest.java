/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.registry.transformer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Gml3ToWktTest {

    private Gml3ToWkt gtw;

    private String createWktPoint(int x, int y) {
        return String.format("POINT (%s %s)", x, y);
    }

    private String createGmlPoint(int x, int y) {
        return String
                .format("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos>%s %s</gml:pos></gml:Point>",
                        x, y);
    }

    @Before
    public void setUp() throws Exception {
        gtw = new Gml3ToWkt(new XmlParser());
    }

    @Test
    public void testGmlPointToWkt() {
        assertThat(gtw.convert(createGmlPoint(0, 0)), is(createWktPoint(0, 0)));
        assertThat(gtw.convert(createGmlPoint(0, 1)), is(createWktPoint(0, 1)));
        assertThat(gtw.convert(createGmlPoint(1, 0)), is(createWktPoint(1, 0)));
        assertThat(gtw.convert(createGmlPoint(1, 1)), is(createWktPoint(1, 1)));
    }

    @Test
    public void testBadGml() {
        assertThat(gtw.convert("error"), is(""));
    }
}