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
package ddf.catalog.operation;

import static org.junit.Assert.assertArrayEquals;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceDescriptorImpl;
import ddf.catalog.util.SourceDescriptorComparator;

public class SourceInfoResponseImplTest {

    private Set<SourceDescriptor> sourceDescriptors;

    private SourceDescriptor firstSource;

    private SourceDescriptor nextSource;

    private SourceDescriptor lastSource;

    @Before
    public void setup() {
        firstSource = new SourceDescriptorImpl("aSource",
                null);
        nextSource = new SourceDescriptorImpl("BSource", null);
        lastSource = new SourceDescriptorImpl("cSource",
                null);

        sourceDescriptors = new TreeSet<SourceDescriptor>(
                new SourceDescriptorComparator());
        sourceDescriptors.add(lastSource);
        sourceDescriptors.add(firstSource);
        sourceDescriptors.add(nextSource);
    }

    @Test
    public void testSourceInfoResponse() {
        SourceDescriptor[] expectedDescriptorArr = new SourceDescriptor[] {
                firstSource, nextSource, lastSource};

        SourceInfoResponse response = new SourceInfoResponseImpl(
                new SourceInfoRequestLocal(false), null, sourceDescriptors);
        Set<SourceDescriptor> sources = response.getSourceInfo();

        assertArrayEquals(expectedDescriptorArr,
                sources.toArray(new SourceDescriptor[sources.size()]));

    }

    @Test
    public void testSourceInfoResponseNullSourceId() {
        SourceDescriptor desc = new SourceDescriptorImpl(null, null);
        sourceDescriptors.add(desc);

        SourceDescriptor[] expectedDescriptorArr = new SourceDescriptor[] {
                firstSource, nextSource, lastSource, desc};

        SourceInfoResponse response = new SourceInfoResponseImpl(
                new SourceInfoRequestLocal(true), null, sourceDescriptors);
        Set<SourceDescriptor> sources = response.getSourceInfo();

        assertArrayEquals(expectedDescriptorArr,
                sources.toArray(new SourceDescriptor[sources.size()]));

    }
}
