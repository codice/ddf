/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.source.SourceDescriptorImpl;

public class SourceDescriptorComparatorTest {
    private SourceDescriptorImpl firstSource;
    private SourceDescriptorImpl nextSource;
    private SourceDescriptorImpl lastSource;

    private SourceDescriptorImpl nullSource;

    @Before
    public void setup() throws Exception {
        firstSource = new SourceDescriptorImpl("aSource", null);
        nextSource = new SourceDescriptorImpl("bSource", null);
        lastSource = new SourceDescriptorImpl("cSource", null);
        nullSource = new SourceDescriptorImpl(null, null);

    }

    @Test
    public void testCompare() {
        SourceDescriptorComparator comparer = new SourceDescriptorComparator();
        assertTrue(comparer.compare(firstSource, nextSource) < 0);
        assertEquals(0, comparer.compare(firstSource, firstSource));
        assertTrue(comparer.compare(firstSource, lastSource) < 0);
        assertTrue(comparer.compare(lastSource, nextSource) > 0);
        assertTrue(comparer.compare(nullSource, firstSource) > 0);
    }

}
