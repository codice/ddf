/*
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

package ddf.catalog.transformer.csv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

public class MetacardIteratorTest {
    private static final Object[][] ATTRIBUTE_DATA = {{"attribute1", "value1"},
            {"attribute2", new Integer(101)},
            {"attribute3", new Double(3.14159)}};

    private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

    private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();

    @Before
    public void setUp() {
        ATTRIBUTE_DESCRIPTOR_LIST.clear();
        METACARD_DATA_MAP.clear();

        for (Object[] entry : ATTRIBUTE_DATA) {
            String attributeName = entry[0].toString();
            Serializable attributeValue = (Serializable) entry[1];
            Attribute attribute = buildAttribute(attributeName, attributeValue);
            METACARD_DATA_MAP.put(attributeName, attribute);
            ATTRIBUTE_DESCRIPTOR_LIST.add(buildAttribute(attributeName));
        }

        Attribute attribute = buildAttribute("skipMe", "value");
        METACARD_DATA_MAP.put("skipMe", attribute);
    }

    @Test
    public void testColumnHeaderIterator() {
        Metacard metacard = buildMetacard();
        Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

        for (int i = 0; i < ATTRIBUTE_DATA.length; i++) {
            assertThat(iterator.hasNext(), is(true));
            assertThat(iterator.next(), is(ATTRIBUTE_DATA[i][1]));
        }

        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testHasNext() {
        Metacard metacard = buildMetacard();
        Iterator<Serializable> iterator = new MetacardIterator(metacard, ATTRIBUTE_DESCRIPTOR_LIST);

        while (iterator.hasNext()) {
            iterator.next();
        }

        iterator.next();
    }

    private Metacard buildMetacard() {
        Metacard metacard = mock(Metacard.class);

        Answer<Serializable> answer = invocation -> {
            String key = invocation.getArgumentAt(0, String.class);
            return METACARD_DATA_MAP.get(key);
        };

        when(metacard.getAttribute(anyString())).thenAnswer(answer);
        return metacard;
    }

    private AttributeDescriptor buildAttribute(String name) {
        AttributeDescriptor attribute = mock(AttributeDescriptor.class);
        when(attribute.getName()).thenReturn(name);
        return attribute;
    }

    private Attribute buildAttribute(String name, Serializable value) {
        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getValue()).thenReturn(value);
        return attribute;
    }
}
