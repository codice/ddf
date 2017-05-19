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
package ddf.camel.component.catalog.content;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;

import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

import ddf.catalog.Constants;

public class ContentProducerTest {

    ContentEndpoint mockEndpoint;

    ContentProducer contentProducer;

    ContentProducerDataAccessObject mockContentProducerDao;

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testProcess() throws Exception {
        File testFile = testDir.newFile();

        mockEndpoint = mock(ContentEndpoint.class);

        UuidGenerator uuidGenerator = mock(UuidGenerator.class);
        ContentComponent contentComponent = mock(ContentComponent.class);
        when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID()
                .toString());
        when(mockEndpoint.getComponent()).thenReturn(contentComponent);

        Message mockMessage = mock(Message.class);
        doReturn(ImmutableMap.of(Constants.STORE_REFERENCE_KEY, true)).when(mockMessage)
                .getHeaders();

        mockContentProducerDao = mock(ContentProducerDataAccessObject.class);
        doReturn(testFile).when(mockContentProducerDao)
                .getFileUsingRefKey(true, mockMessage);
        doReturn(null).when(mockContentProducerDao)
                .getEventType(true, mockMessage);
        doReturn("xml").when(mockContentProducerDao)
                .getMimeType(mockEndpoint, testFile);

        contentProducer = new ContentProducer(mockEndpoint);
        contentProducer.contentProducerDataAccessObject = mockContentProducerDao;

        Exchange mockExchange = mock(Exchange.class);
        doReturn(ExchangePattern.InOnly).when(mockExchange)
                .getPattern();
        doReturn(mockMessage).when(mockExchange)
                .getIn();

        contentProducer.process(mockExchange);

        verify(mockContentProducerDao, times(1)).getFileUsingRefKey(true, mockMessage);
        verify(mockContentProducerDao, times(1)).getEventType(true, mockMessage);
        verify(mockContentProducerDao, times(1)).getMimeType(mockEndpoint, testFile);
    }
}
