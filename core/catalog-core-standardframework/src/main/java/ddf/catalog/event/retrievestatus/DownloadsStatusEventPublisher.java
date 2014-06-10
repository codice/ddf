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


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.activities.ActivityEvent.ActivityStatus;
import org.codice.ddf.notifications.Notification;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.security.SubjectUtils;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadsStatusEventPublisher.class);

    private EventAdmin eventAdmin;
    private List<ActionProvider> actionProviders;

    private boolean notificationEnabled = true;
    private boolean activityEnabled = true;

    /**
     * Used to publish product retrieval status updates via the OSGi Event Service
     */
    public DownloadsStatusEventPublisher(EventAdmin eventAdmin, List<ActionProvider> actionProviders) {
        this.eventAdmin = eventAdmin;
        this.actionProviders = actionProviders;
    }

    /**
     * Set the current retrieval status for product identified by key.
     *
     * @param resourceResponse - The {@link ResourceResponse} of the request.
     * @param status           - The status of the retrieval.}
     */
    public void postRetrievalStatus(final ResourceResponse resourceResponse, ProductRetrievalStatus status, Metacard metacard, String detail, Long bytes) {

        LOGGER.debug("ENTERING: postRetrievalStatus(...)");
        LOGGER.debug("status: {}", status);
        LOGGER.debug("detail: {}", detail);
        LOGGER.debug("bytes: {}", bytes);

        if (bytes == null) {
            bytes = 0L;
        }
        
        Long sysTimeMillis = System.currentTimeMillis();
        
        // Add user information to the request properties.
        org.apache.shiro.subject.Subject shiroSubject = null;
        try {
            shiroSubject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Could not determine current user, using session id.");
        }
        String user = SubjectUtils.getName(shiroSubject, getProperty(resourceResponse, ActivityEvent.USER_ID_KEY));
        
        
        if(notificationEnabled) {
            Notification notification = new Notification(APPLICATION_NAME,
                    resourceResponse.getResource().getName(),
                    generateMessage(status, resourceResponse.getResource().getName(),
                            bytes, sysTimeMillis, detail),
                    sysTimeMillis, user);

            notification.put(STATUS, status.name().toLowerCase());
            notification.put(BYTES, String.valueOf(bytes));

            Event event = new Event(Notification.NOTIFICATION_TOPIC_DOWNLOADS,
                    notification);

            eventAdmin.postEvent(event);
        }
        else {
            LOGGER.debug("Notifications have been disabled so this message will NOT be posted.");
        }

        if (activityEnabled) {
            
            // get Action
            Action downloadAction = null;
            if(!actionProviders.isEmpty()) {
                // take the first one
                downloadAction = actionProviders.get(0).getAction(metacard);
            }
            
            // send activity event
            // progress for downloads 
            String progress = "";
            Map<String, String> operations = new HashMap<String, String>();
            ActivityStatus type;
            switch (status) {
            case STARTED:
                type = ActivityStatus.STARTED;
                break;
            case COMPLETE:
                type = ActivityStatus.FINISHED;
                if (downloadAction != null) {
                    operations = ImmutableMap.of("download", downloadAction.getUrl().toString());
                }
                break;
            case FAILED:
                type = ActivityStatus.FAILED;
                break;
            case CANCELLED:
                type = ActivityStatus.STOPPED;
                break;
            default:
                type = ActivityStatus.RUNNING;
            
            }

            ActivityEvent eventProperties = new ActivityEvent(metacard.getId(), new Date(), "Product Retrieval", resourceResponse.getResource().getName(), generateMessage(status, resourceResponse.getResource().getName(), 
                    bytes, sysTimeMillis, detail), progress, operations, user, type);
            Event event = new Event(ActivityEvent.EVENT_TOPIC_BROADCAST, eventProperties);
            eventAdmin.postEvent(event);
        }
        else {
            LOGGER.debug("Activities have been disabled so this message will NOT be posted.");
        }

        LOGGER.debug("EXITING: postRetrievalStatus(...)");
    }

    private String getProperty(ResourceResponse resourceResponse, String property) {
        String response = "";

        if (resourceResponse.getRequest().containsPropertyName(property)) {
            response = (String) resourceResponse.getRequest().getPropertyValue(property);
            LOGGER.debug("resourceResponse {} property: {}", property, response);
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
        LOGGER.debug("message: {}", response.toString());

        return response.toString();
    }

    public void setNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }
    
    public void setActivityEnabled(boolean enabled) {
        this.activityEnabled = enabled;
    }
}
