/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.event.retrievestatus;

import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetrievalStatusEventPublisherTest {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RetrievalStatusEventPublisherTest.class);


//
//    @BeforeClass
//    public static void oneTimeSetup() throws IOException {
//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
//                .getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(ch.qos.logback.classic.Level.DEBUG);
//
//        workingDir = System.getProperty("user.dir");
//        productCacheDirectory = workingDir + "/target/tests/product-cache";
//        productInputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
//        File productInputFile = new File(productInputFilename);
//        expectedFileSize = productInputFile.length();
//        expectedFileContents = FileUtils.readFileToString(productInputFile);
//    }

    @org.junit.Test
    public void testPostRetrievalStatusHappyPath() {

        // Set up for the test
        EventAdmin eventAdmin = mock(EventAdmin.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Resource resource = mock(Resource.class);
        Map<String, Serializable> properties = mock(Map.class);

        when(resource.getName()).thenReturn("testCometDSessionID");

        when(properties.get(RetrievalStatusEventPublisher.USER)).thenReturn("testUser");

        when(resourceRequest.getProperties()).thenReturn(properties);

        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);

        RetrievalStatusEventPublisher publisher = new RetrievalStatusEventPublisher(eventAdmin);

        publisher.postRetrievalStatus(resourceResponse, RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_STARTED, 0L);
        verify(eventAdmin, times(1)).postEvent(any(Event.class));

        // Run/verify the tests
        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_STARTED, 10L);
        verify(eventAdmin, times(2)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRIEVING, 15L);
        verify(eventAdmin, times(3)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_CANCELLED, 20L);
        verify(eventAdmin, times(4)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_FAILED, 250L);
        verify(eventAdmin, times(5)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRYING, 350L);
        verify(eventAdmin, times(6)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_COMPLETE, 500L);
        verify(eventAdmin, times(7)).postEvent(any(Event.class));
    }

    @org.junit.Test
    public void testPostRetrievalStatusWithNoNameProperty() {

        // Set up for the test
        EventAdmin eventAdmin = mock(EventAdmin.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Resource resource = mock(Resource.class);
        Map<String, Serializable> properties = mock(Map.class);

        when(resource.getName()).thenReturn("testCometDSessionID");

        when(resourceRequest.getProperties()).thenReturn(properties);

        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);

        RetrievalStatusEventPublisher publisher = new RetrievalStatusEventPublisher(eventAdmin);

        publisher.postRetrievalStatus(resourceResponse, RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_STARTED, 0L);
        verify(eventAdmin, times(1)).postEvent(any(Event.class));

        // Run/verify the tests
        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_STARTED, 10L);
        verify(eventAdmin, times(2)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRIEVING, 15L);
        verify(eventAdmin, times(3)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_CANCELLED, 20L);
        verify(eventAdmin, times(4)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_FAILED, 250L);
        verify(eventAdmin, times(5)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRYING, 350L);
        verify(eventAdmin, times(6)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_COMPLETE, 500L);
        verify(eventAdmin, times(7)).postEvent(any(Event.class));
    }
}
