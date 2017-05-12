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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;

public class ContentProducer extends DefaultProducer {
    public static final int KB = 1024;

    public static final int MB = 1024 * KB;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentProducer.class);

    private static final int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD = 1 * MB;

    private final FileSystemPersistenceProvider fileIdMap = new FileSystemPersistenceProvider(
            "processed");

    private ContentEndpoint endpoint;

    ContentProducerDataAccessObject contentProducerDataAccessObject;

    /**
     * Constructs the {@link org.apache.camel.Producer} for the custom Camel ContentComponent. This producer would
     * map to a Camel <code>&lt;to&gt;</code> route node with a URI like <code>content:framework</code>
     *
     * @param endpoint the Camel endpoint that created this consumer
     */
    public ContentProducer(ContentEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        ContentComponent contentComponent = endpoint.getComponent();
        UuidGenerator uuidGenerator = null;
        if (contentComponent != null) {
            uuidGenerator = contentComponent.getUuidGenerator();
        }
        this.contentProducerDataAccessObject = new ContentProducerDataAccessObject(uuidGenerator);
        LOGGER.trace("INSIDE: ContentProducer constructor");
    }

    @Override
    public void process(Exchange exchange)
            throws ContentComponentException, SourceUnavailableException, IngestException {
        LOGGER.trace("ENTERING: process");

        if (!exchange.getPattern()
                .equals(ExchangePattern.InOnly)) {
            return;
        }

        Message in = exchange.getIn();

        Map<String, Object> headers = exchange.getIn()
                .getHeaders();

        boolean storeRefKey = headers.containsKey(Constants.STORE_REFERENCE_KEY);

        File ingestedFile = contentProducerDataAccessObject.getFileUsingRefKey(storeRefKey, in);

        WatchEvent.Kind<Path> eventType = contentProducerDataAccessObject.getEventType(storeRefKey,
                in);

        if (ingestedFile == null) {
            LOGGER.trace("EXITING: process - ingestedFile is NULL");
            return;
        }

        String mimeType = contentProducerDataAccessObject.getMimeType(endpoint, ingestedFile);

        LOGGER.trace("Preparing content item for mimeType = {}", mimeType);

        if (StringUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }

        contentProducerDataAccessObject.createContentItem(fileIdMap,
                endpoint,
                ingestedFile,
                eventType,
                mimeType,
                headers);

        LOGGER.trace("EXITING: process");
    }
}
