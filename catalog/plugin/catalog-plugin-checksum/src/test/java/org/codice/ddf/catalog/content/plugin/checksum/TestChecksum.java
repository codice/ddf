/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.catalog.content.plugin.checksum;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.checksum.ChecksumProvider;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PluginExecutionException;

public class TestChecksum {
    private ChecksumProvider mockChecksumProvider;

    private Checksum checksum;

    private CreateStorageRequest mockRequest;

    private static final String SAMPLE_CHECKSUM_ALGORITHM = "MD5";

    private static final String SAMPLE_CHECKSUM_VALUE = "324D54D92B2D97471F9F4624596EA9F5";

    @Before
    public void initialize() throws IOException, NoSuchAlgorithmException {
        mockChecksumProvider = mock(ChecksumProvider.class);

        InputStream inputStream = mock(InputStream.class);

        when(mockChecksumProvider.getChecksumAlgorithm()).thenReturn(SAMPLE_CHECKSUM_ALGORITHM);

        when(mockChecksumProvider.calculateChecksum(inputStream)).thenReturn(SAMPLE_CHECKSUM_VALUE);

        checksum = new Checksum(mockChecksumProvider);

        List<ContentItem> mockContentItems = new ArrayList<>();
        ContentItem mockContentItem = mock(ContentItem.class);
        when(mockContentItem.getInputStream()).thenReturn(inputStream);
        when(mockContentItem.getMetacard()).thenReturn(new MetacardImpl());
        mockContentItems.add(mockContentItem);

        mockRequest = mock(CreateStorageRequest.class);
        when(mockRequest.getContentItems()).thenReturn(mockContentItems);
    }

    @Test
    public void testProcessWithValidInput() throws PluginExecutionException {
        CreateStorageRequest request = checksum.process(mockRequest);

        String checksumResult = (String) request.getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.RESOURCE_CHECKSUM)
                .getValue();
        String checksumAlgorithm = (String) request.getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.RESOURCE_CHECKSUM_ALGORITHM)
                .getValue();
        assertThat(checksumResult, is(SAMPLE_CHECKSUM_VALUE));
        assertThat(checksumAlgorithm, is(SAMPLE_CHECKSUM_ALGORITHM));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessWithNullInput() throws PluginExecutionException {
        checksum.process(null);
    }

    @Test
    public void testChecksumCalculationIOException() throws Exception {
        doThrow(IOException.class).when(mockChecksumProvider)
                .calculateChecksum(any(InputStream.class));

        try {
            checksum.process(mockRequest);
            fail("Checksum plugin should have thrown an exception.");
        } catch (PluginExecutionException e) {
            assertThat(e.getCause(), instanceOf(IOException.class));
        }
    }
}
