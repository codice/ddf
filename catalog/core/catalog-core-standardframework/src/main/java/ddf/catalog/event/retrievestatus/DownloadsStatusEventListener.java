/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.event.retrievestatus;

import ddf.catalog.operation.ResourceResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.activities.ActivityEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadsStatusEventListener implements EventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DownloadsStatusEventListener.class);

  private static final String ENTERING_STR = "ENTERING: {}";

  private static final String EXITING_STR = "EXITING: {}";

  private static final String DOWNLOAD_MAP_STR = "downloadMap : ";

  private static final String KEY_VALUE_STR = "  Key = {}  Value = {}";

  private Map<String, InputStream> downloadMap = new HashMap<>();

  @Override
  public void handleEvent(Event event) {
    String methodName = "handleEvent";
    LOGGER.debug(ENTERING_STR, methodName);

    if (null != event
        && null != event.getTopic()
        && null != event.getProperty(ActivityEvent.DOWNLOAD_ID_KEY)) {

      // If cancel event, then cancel the download stream
      if (ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL.equals(event.getTopic())) {
        String keyToCancel = event.getProperty(ActivityEvent.DOWNLOAD_ID_KEY).toString();

        LOGGER.debug("downloadKey = {}", keyToCancel);
        if (null != downloadMap) {
          for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
            if (StringUtils.equals(keyToCancel, item.getKey())) {
              InputStream is = item.getValue();
              IOUtils.closeQuietly(is);
              break;
            }
          }
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(DOWNLOAD_MAP_STR);
            for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
              String keyStr = item.getKey();
              InputStream value = item.getValue();
              LOGGER.debug(KEY_VALUE_STR, keyStr, value);
            }
          }
        }
      }
    } else {
      LOGGER.debug("Event is null ");
    }

    LOGGER.debug(EXITING_STR, methodName);
  }

  public void setDownloadMap(String downloadIdentifier, ResourceResponse resourceResponse) {
    String methodName = "setDownloadMap";
    LOGGER.debug(ENTERING_STR, methodName);

    if (null != downloadIdentifier && null != resourceResponse) {

      InputStream is = resourceResponse.getResource().getInputStream();
      if (null != is) {
        LOGGER.debug("added ==> {}:{} ", downloadIdentifier, is);
        this.downloadMap.put(downloadIdentifier, is);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(DOWNLOAD_MAP_STR);
          for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
            String keyStr = item.getKey();
            InputStream value = item.getValue();
            LOGGER.debug(KEY_VALUE_STR, keyStr, value);
          }
        }
      }
    }
    LOGGER.debug(EXITING_STR, methodName);
  }

  public void removeDownloadIdentifier(String downloadIdentifier) {
    String methodName = "removeDownloadMap";
    LOGGER.debug(ENTERING_STR, methodName);

    if (null != downloadIdentifier && null != downloadMap) {
      for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
        if (StringUtils.equals(downloadIdentifier, item.getKey())) {
          downloadMap.remove(downloadIdentifier);
          LOGGER.debug(
              "Removed downloadIdentifier ==> {}:{} ", downloadIdentifier, item.getValue());
          break;
        }
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(DOWNLOAD_MAP_STR);
        for (Map.Entry<String, InputStream> item : downloadMap.entrySet()) {
          String keyStr = item.getKey();
          InputStream value = item.getValue();
          LOGGER.debug(KEY_VALUE_STR, keyStr, value);
        }
      }
    }
    LOGGER.debug(EXITING_STR, methodName);
  }
}
