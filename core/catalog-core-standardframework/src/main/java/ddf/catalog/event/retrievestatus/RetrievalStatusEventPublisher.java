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


import ddf.catalog.operation.ResourceResponse;
import org.apache.log4j.Logger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * The {@code RetrievalStatusEventPublisher} class creates events and sends them using the {@code EventAdmin} service
 * interface.
 */
public class RetrievalStatusEventPublisher {

    // Topic
    public static final String EVENTS_TOPIC_PRODUCT_RETRIEVAL = "ddf/notification/catalog/downloads";

    // Property keys
    public static final String APPLICATION = "application";
    public static final String APPLICATION_NAME = "Downloads";
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String USER = "user";
    public static final String STATUS = "status";
    public static final String BYTES = "bytes";

    //  status values
    public static final String PRODUCT_RETRIEVAL_STARTED = "started";
    public static final String PRODUCT_RETRIEVAL_RETRIEVING = "retrieving";
    public static final String PRODUCT_RETRIEVAL_RETRYING = "retrying";
    public static final String PRODUCT_RETRIEVAL_CANCELLED = "cancelled";
    public static final String PRODUCT_RETRIEVAL_FAILED = "failed";
    public static final String PRODUCT_RETRIEVAL_COMPLETE = "complete";

    private static Logger logger = Logger.getLogger(RetrievalStatusEventPublisher.class);
    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");

    private EventAdmin eventAdmin;

    /**
     * Used to publish product retrieval status updates via the OSGi Event Service
     */
    public RetrievalStatusEventPublisher(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Set the current retrieval status for product identified by key.
     * @param resourceResponse - The {@link ResourceResponse} of the request.
     * @param status - The status of the retrieval.}
     */
    public void postRetrievalStatus(final ResourceResponse resourceResponse, String status, Long bytes) {

        logger.debug("ENTERING: postRetrievalStatus(...)");
        logger.debug("status:" + status);
        logger.debug("bytes:" + bytes);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(APPLICATION, APPLICATION_NAME);
        properties.put(TITLE, resourceResponse.getResource().getName());
        Long sysTimeMillis = System.currentTimeMillis();
        properties.put(MESSAGE, generateMessage(status,
                resourceResponse.getResource().getName(), bytes, sysTimeMillis));
        properties.put(USER, getProperty(resourceResponse, USER));
        properties.put(STATUS, status);
        properties.put(BYTES, bytes);
        properties.put(TIMESTAMP, sysTimeMillis);

        Event event = new Event(EVENTS_TOPIC_PRODUCT_RETRIEVAL, properties);

        eventAdmin.postEvent(event);

        logger.debug("EXITING: postRetrievalStatus(...)");
    }

    private String getProperty(ResourceResponse resourceResponse, String property) {
        String response = "";

        if (resourceResponse.getRequest().containsPropertyName(property)) {
            response = (String) resourceResponse.getRequest().getPropertyValue(property);
            logger.debug("resourceResponse " + property + " property: " + response);
        }

        return response;
    }

    private String generateMessage(String status, String title, Long bytes, Long sysTimeMillis) {
        StringBuilder response = new StringBuilder("Product retrieval for ");
        response.append(title);

        if (status.equals(PRODUCT_RETRIEVAL_RETRIEVING)) {
            response.append(", ");
            response.append(bytes.toString());
            response.append(" bytes retrieved");
        } else if (status.equals(PRODUCT_RETRIEVAL_STARTED)) {
            response.append(" started at ");
            Date date = new Date(sysTimeMillis);
            response.append(formatter.format(date));
        }
        else if (status.equals(PRODUCT_RETRIEVAL_RETRYING)) {
            response.append(" retrying at ");
            Date date = new Date(sysTimeMillis);
            response.append(formatter.format(date));
            response.append(" after ");
            response.append(bytes.toString());
            response.append(" bytes");
        }
        else if (status.equals(PRODUCT_RETRIEVAL_CANCELLED)) {
            response.append(" cancelled at ");
            Date date = new Date(sysTimeMillis);
            response.append(formatter.format(date));
        }
        else if (status.equals(PRODUCT_RETRIEVAL_COMPLETE)) {
            response.append(" completed at ");
            Date date = new Date(sysTimeMillis);
            response.append(formatter.format(date));
            response.append(", bytes retrieved: ");
            response.append(bytes.toString());
        }
        else if (status.equals(PRODUCT_RETRIEVAL_FAILED)) {
            response.append(" failed at ");
            Date date = new Date(sysTimeMillis);
            response.append(formatter.format(date));
        }

        logger.debug("message: " + response.toString());

        return response.toString();
    }

}
