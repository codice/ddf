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
package ddf.camel.component.content;

import com.google.common.io.FileBackedOutputStream;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.data.impl.IncomingContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.Request;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ContentProducer extends DefaultProducer {
    private ContentEndpoint endpoint;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentProducer.class);
    
    public static final int KB = 1024;

    public static final int MB = 1024 * KB;
    
    private static final int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD = 1 * MB;

    /**
     * Constructs the {@link Producer} for the custom Camel ContentComponent. This producer would
     * map to a Camel <code>&lt;to&gt;</code> route node with a URI like <code>content:framework</code>
     * 
     * @param endpoint
     *            the Camel endpoint that created this consumer
     */
    public ContentProducer(ContentEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;

        LOGGER.debug("INSIDE: ContentProducer constructor");
    }

    @Override
    public void process(Exchange exchange) throws ContentComponentException,
        ContentFrameworkException {
        LOGGER.debug("ENTERING: process");

        if (!exchange.getPattern().equals(ExchangePattern.InOnly)) {
            return;
        }

        Message in = exchange.getIn();
        Object body = in.getBody();
        File ingestedFile = null;
        if (body instanceof GenericFile) {
            GenericFile<File> genericFile = (GenericFile<File>) body;
            ingestedFile = (File) genericFile.getFile();
        } else {
            LOGGER.warn("Unable to cast message body to Camel GenericFile, so unable to process ingested file");
            throw new ContentComponentException(
                    "Unable to cast message body to Camel GenericFile, so unable to process ingested file");
        }

        if (ingestedFile == null) {
            LOGGER.debug("EXITING: process - ingestedFile is NULL");
            return;
        }

        String operation = in.getHeader(Request.OPERATION, String.class);
        if (StringUtils.isEmpty(operation)) {
            throw new ContentComponentException("Unable to process file " + ingestedFile.getName()
                    + "  -  Must specify an operation of create, read, update, or delete");
        }

        String directive = in.getHeader(Request.DIRECTIVE, String.class);
        if (StringUtils.isEmpty(directive)) {
            throw new ContentComponentException("Unable to process file " + ingestedFile.getName()
                    + "  -  Must specify a directive of STORE, PROCESS, or STORE_PROCESS");
        }

        String contentUri = (String) in.getHeader(Request.CONTENT_URI, "");

        LOGGER.debug("operation = {}", operation);
        LOGGER.debug("directive = {}", directive);
        LOGGER.debug("contentUri = {}", contentUri);

        FileInputStream fis = null;
        FileBackedOutputStream fbos = null;
        try {
            fis = FileUtils.openInputStream(ingestedFile);
        } catch (IOException e) {
            throw new ContentComponentException("Unable to open file {}" + ingestedFile.getName());
        }

        Request.Directive requestDirective = Request.Directive.valueOf(directive);

        String fileExtension = FilenameUtils.getExtension(ingestedFile.getAbsolutePath());

        String mimeType = null;
        if (fileExtension != null) {
            MimeTypeMapper mimeTypeMapper = endpoint.getComponent().getMimeTypeMapper();
            if (mimeTypeMapper != null) {
                try {
                    // XML files are mapped to multiple mime types, e.g., CSW, XML Metacard, etc.
                    // If processing a .xml file, then use a stream that allows multiple reads on it
                    // i.e., FileBackedOutputStream (FBOS), since the MimeTypeMapper will need to introspect
                    // the xml file for its root element namespace and later the stream will need to
                    // be read to persist it to the content store and/or create a metacard for ingest.
                    // The FBOS is not used for every ingested file because do not want the performance
                    // hit of potentially reading in a 1 GB NITF file that does not need to be introspected
                    // to determine the mime type.
                    if (fileExtension.equals("xml")) {
                        // FBOS reads file into byte array in memory up to this threshold, then it transitions
                        // to writing to a file.
                        fbos = new FileBackedOutputStream(DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD);
                        IOUtils.copy(fis, fbos);
                        // Using fbos.asByteSource().openStream() allows us to pass in a copy of the InputStream
                        mimeType = mimeTypeMapper.guessMimeType(fbos.asByteSource().openStream(), fileExtension);
                    } else {
                        mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                    }
                    
                } catch (MimeTypeResolutionException | IOException e) {
                    IOUtils.closeQuietly(fis);
                    IOUtils.closeQuietly(fbos);
                    throw new ContentComponentException(e);
                }
            } else {
                IOUtils.closeQuietly(fis);
                LOGGER.error("Did not find a MimeTypeMapper service");
                throw new ContentComponentException(
                        "Unable to find a mime type for the ingested file "
                                + ingestedFile.getName());
            }
        }

        try {
            LOGGER.debug("Preparing content item for mimeType = {}", mimeType);

            if (StringUtils.isNotEmpty(mimeType)) {
                ContentItem newItem;
                // If the FileBackedOutputStream (FBOS) was used, then must be processing an XML file and need to
                // get a fresh InputStream from the FBOS to use in the content item
                if (fbos != null) {
                    newItem = new IncomingContentItem(fbos.asByteSource().openStream(), mimeType, ingestedFile.getName());
                } else {
                    // Otherwise, the InputStream created from the ingested file has never been read,
                    // so can use it in the content item
                    newItem = new IncomingContentItem(fis, mimeType, ingestedFile.getName());
                }
                newItem.setUri(contentUri);

                LOGGER.debug("Creating content item.");

                CreateRequest createRequest = new CreateRequestImpl(newItem, null);
                CreateResponse createResponse = endpoint.getComponent().getContentFramework()
                        .create(createRequest, requestDirective);
                if (createResponse != null) {
                    ContentItem contentItem = createResponse.getCreatedContentItem();

                    LOGGER.debug("content item created with id = {}", contentItem.getId());
                    LOGGER.debug(contentItem.toString());
                }
            } else {
                LOGGER.debug("mimeType is NULL");
                throw new ContentComponentException("Unable to determine mime type for the file "
                        + ingestedFile.getName());
            }
        } catch (IOException e) {
            throw new ContentComponentException("Unable to determine mime type for the file "
                    + ingestedFile.getName());
        } finally {
            IOUtils.closeQuietly(fis);
            if (fbos != null) {
                try {
                    fbos.reset();
                } catch (IOException e) {
                    LOGGER.info("Unable to reset FileBackedOutputStream");
                }
            }
        }

        LOGGER.debug("EXITING: process");
    }

}
