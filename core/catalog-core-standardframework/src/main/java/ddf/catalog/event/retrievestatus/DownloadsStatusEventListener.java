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
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.security.SubjectUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.activities.ActivityEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DownloadsStatusEventListener implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadsStatusEventListener.class);

    public DownloadsStatusEventListener() {}

    private Map<String, InputStream> downloadMap = new HashMap<String, InputStream>();



    @Override
    public void handleEvent(Event event) {
        String methodName = "handleEvent";
        LOGGER.debug("ENTERING: {}", methodName);


        if (null != event && null != event.getTopic() && null != event.getProperty(ActivityEvent.DOWNLOAD_ID_KEY)) {

            // If cancel event, then cancel the download stream
            if (ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL.equals(event.getTopic())) {
                String keyToCancel = event.getProperty(ActivityEvent.DOWNLOAD_ID_KEY).toString();

                LOGGER.debug("downloadKey = {}", keyToCancel);
                if (null != downloadMap){
                    for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
                        if(StringUtils.equals(keyToCancel, item.getKey())) {
                            InputStream is = item.getValue();
                            IOUtils.closeQuietly(is);
                            break;
                        }
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("downloadMap : ");
                        for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
                            String keyStr = item.getKey();
                            InputStream value = item.getValue();
                            LOGGER.debug("  Key = {}  Value = {}", keyStr, value);
                        }
                    }

                }

            }
        } else {
            LOGGER.debug("Event is null ");
        }

        LOGGER.debug("EXITING: {}", methodName);
    }

    public void setDownloadMap(String downloadIdentifier, ResourceResponse resourceResponse) {
        String methodName = "setDownloadMap";
        LOGGER.debug("ENTERING: {}", methodName);

        if (null != downloadIdentifier && null != resourceResponse) {

            // Add user information to the request properties.
            org.apache.shiro.subject.Subject shiroSubject = null;
            try {
                shiroSubject = SecurityUtils.getSubject();
            } catch (Exception e) {
                LOGGER.debug("Could not determine current user, using session id.");
            }
            String user = SubjectUtils.getName(shiroSubject, getProperty(resourceResponse, ActivityEvent.USER_ID_KEY));

            String id = user + downloadIdentifier;

            InputStream  is = resourceResponse.getResource().getInputStream();
            if (null != is) {
                LOGGER.debug("added ==> {}:{} ", id, is);
                this.downloadMap.put(id, is);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("downloadMap : ");
                    for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
                        String keyStr = item.getKey();
                        InputStream value = item.getValue();
                        LOGGER.debug("  Key = {}  Value = {}", keyStr, value);
                    }
                }
            }
        }
        LOGGER.debug("EXITING: {}", methodName);
    }

    /**
     * Public getter of downloadMap
     *
     * @return
     */
    public Map<String, InputStream> getDownloadMap() {return downloadMap; }

    public void removeDownloadIdentifier(String downloadIdentifier, ResourceResponse resourceResponse) {
        String methodName = "removeDownloadMap";
        LOGGER.debug("ENTERING: {}", methodName);

        if (null != downloadIdentifier) {

            // Add user information to the request properties.
            org.apache.shiro.subject.Subject shiroSubject = null;
            try {
                shiroSubject = SecurityUtils.getSubject();
            } catch (Exception e) {
                LOGGER.debug("Could not determine current user, using session id.");
            }
            String user = SubjectUtils.getName(shiroSubject, getProperty(resourceResponse, ActivityEvent.USER_ID_KEY));

            String id = user + downloadIdentifier;
            if (null != downloadMap){
                for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
                    if(StringUtils.equals(id, item.getKey())) {
                        downloadMap.remove(id);
                        LOGGER.debug("Removed downloadIdentifier ==> {}:{} ", id, item.getValue());
                        break;
                    }
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("downloadMap : ");
                    for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
                        String keyStr = item.getKey();
                        InputStream value = item.getValue();
                        LOGGER.debug("  Key = {}  Value = {}", keyStr, value);
                    }
                }

            }
        }
        LOGGER.debug("EXITING: {}", methodName);
    }

    private String getProperty(ResourceResponse resourceResponse, String property) {
        String response = "";

        if (resourceResponse.getRequest().containsPropertyName(property)) {
            response = (String) resourceResponse.getRequest().getPropertyValue(property);
            LOGGER.debug("resourceResponse {} property: {}", property, response);
        }

        return response;
    }
}