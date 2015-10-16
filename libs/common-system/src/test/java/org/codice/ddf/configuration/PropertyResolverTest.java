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
 */
package org.codice.ddf.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PropertyResolverTest {

    @Test
    public void testNoProperties() {
        PropertyResolver pr = new PropertyResolver("/some/string/no/props");
        assertThat(pr.getRawString(), equalTo(pr.getResolvedString()));
    }

    @Test
    public void testPropertyNotFound() {
        System.clearProperty("prop");
        PropertyResolver pr = new PropertyResolver("/some/string/no/${prop}");
        assertThat(pr.getRawString(), equalTo(pr.getResolvedString()));
    }

    @Test
    public void testPropertyReplaced() {
        System.setProperty("prop", "myvalue");
        PropertyResolver pr = new PropertyResolver("/some/string/no/${prop}");
        assertThat(pr.getResolvedString(), equalTo("/some/string/no/myvalue"));
    }

    @Test
    public void testPropertyInListReplaced() {
        System.setProperty("foo", "bar");
        List<String> list = Arrays.asList("/some/value", "/${foo}/value", "/baz/value");
        assertThat(PropertyResolver.resolveProperties(list), contains("/some/value", "/bar/value", "/baz/value"));
    }
}
