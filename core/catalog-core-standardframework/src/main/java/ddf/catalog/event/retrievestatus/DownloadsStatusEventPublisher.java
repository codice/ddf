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
import org.codice.ddf.notifications.Notification;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The {@code DownloadsStatusEventPublisher} class creates events and sends them using the {@link EventAdmin} service
 * interface.
 */
public class DownloadsStatusEventPublisher {

    public static final String APPLICATION_NAME = "Downloads";
    
    // Property keys
    public static final String DETAIL = "detail";
    public static final String STATUS = "status";
    public static final String BYTES = "bytes";

    public static enum ProductRetrievalStatus {
        STARTED, RETRYING, CANCELLED, FAILED, COMPLETE;
    }

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(DownloadsStatusEventPublisher.class));
    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");

    private EventAdmin eventAdmin;

    private boolean enabled = false;

    /**
     * Used to publish product retrieval status updates via the OSGi Event Service
     */
    public DownloadsStatusEventPublisher(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Set the current retrieval status for product identified by key.
     *
     * @param resourceResponse - The {@link ResourceResponse} of the request.
     * @param status           - The status of the retrieval.}
     */
    public void postRetrievalStatus(final ResourceResponse resourceResponse, ProductRetrievalStatus status, String detail, Long bytes) {

        logger.debug("ENTERING: postRetrievalStatus(...)");
        logger.debug("status: {}", status);
        logger.debug("detail: {}", detail);
        logger.debug("bytes: {}", bytes);

        if (bytes == null) {
            bytes = 0L;
        }
        
        if(enabled) {
            Long sysTimeMillis = System.currentTimeMillis();
            Notification notification = new Notification(APPLICATION_NAME,
                    resourceResponse.getResource().getName(),
                    generateMessage(status, resourceResponse.getResource().getName(),
                            bytes, sysTimeMillis, detail),
                    sysTimeMillis,
                    getProperty(resourceResponse,
                            Notification.NOTIFICATION_KEY_USER_ID));

            notification.put(STATUS, status.name().toLowerCase());
            notification.put(BYTES, String.valueOf(bytes));

            Event event = new Event(Notification.NOTIFICATION_TOPIC_DOWNLOADS,
                    notification);

            eventAdmin.postEvent(event);
        }
        else {
            logger.debug("Notifications have been disabled so this message will NOT be posted.");
        }

        logger.debug("EXITING: postRetrievalStatus(...)");
        }

    private String getProperty(ResourceResponse resourceResponse, String property) {
        String response = "";

        if (resourceResponse.getRequest().containsPropertyName(property)) {
            response = (String) resourceResponse.getRequest().getPropertyValue(property);
            logger.debug("resourceResponse {} property: {}", property, response);
        }

        return response;
    }

    private String generateMessage(ProductRetrievalStatus status, String title, 
            Long bytes, Long sysTimeMillis, String detail) {
        StringBuilder response = new StringBuilder("Resource retrieval ");

        // There may not be any detail to report, if not, send it along
        if (detail == null) {
            detail = "";
        }

        
        switch (status) {
        case STARTED:
            response.append(" started");
            break;
            
        case COMPLETE:
            response.append(" completed, ");
            response.append(bytes.toString());
            response.append(" bytes retrieved");
            
            break;
            
        case RETRYING:
            response.append(" retrying");
            response.append(" after ");
            response.append(bytes.toString());
            response.append(" bytes");          
            break;
            
        case CANCELLED:
            response.append(" cancelled");
            break;
            
        case FAILED:
            response.append(" failed");
            break;
            
        default:
            break;
        }

        response.append(". ");
        response.append(detail);
        logger.debug("message: {}", response.toString());

        return response.toString();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
