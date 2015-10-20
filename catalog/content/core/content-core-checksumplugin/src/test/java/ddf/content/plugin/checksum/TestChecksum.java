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

package ddf.content.plugin.checksum;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.plugin.PluginExecutionException;

public class TestChecksum {

    private ChecksumProvider mockChecksumProvider;
    private Checksum checksum;
    private InputStream inputStream;
    private CreateRequest mockRequest;

    private  static  final String SAMPLE_CHECKSUM_ALGORITHM = "MD5";
    private  static  final  String SAMPLE_CHECKSUM_VALUE = "324D54D92B2D97471F9F4624596EA9F5";
    public static final java.lang.String RESOURCE_CHECKSUM = "resource-checksum";
    public static final java.lang.String RESOURCE_CHECKSUM_ALGORITHM = "resource-checksum-algorithm";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(TestChecksum.class));

    @Before
    public  void  initialize() throws IOException {
        mockChecksumProvider = mock(ChecksumProvider.class);

        inputStream = mock(InputStream.class);

        when(mockChecksumProvider.getCheckSumAlgorithm())
                .thenReturn(SAMPLE_CHECKSUM_ALGORITHM);

        when(mockChecksumProvider.calculateChecksum(inputStream))
                .thenReturn(SAMPLE_CHECKSUM_VALUE);

        checksum = new Checksum(mockChecksumProvider);

        ContentItem mockContentItem = mock(ContentItem.class);
        when(mockContentItem.getInputStream()).thenReturn(inputStream);

        mockRequest = mock(CreateRequest.class);
        when(mockRequest.getContentItem()).thenReturn(mockContentItem);

    }

    @Test
    public  void testProcessWithValidInput() throws PluginExecutionException {

        CreateRequest request = checksum.process(mockRequest);

        String checksumResult = request.getPropertyValue(RESOURCE_CHECKSUM).toString();
        String checksumAlgorithm = request.getPropertyValue(RESOURCE_CHECKSUM_ALGORITHM).toString();
        assertEquals(checksumResult, SAMPLE_CHECKSUM_VALUE);
        assertEquals(checksumAlgorithm, SAMPLE_CHECKSUM_ALGORITHM);

    }

    @Test(expected = IllegalArgumentException.class)
    public  void  testProcessWithNullInput() throws  PluginExecutionException {
        checksum.process((CreateRequest)null);
    }
}
