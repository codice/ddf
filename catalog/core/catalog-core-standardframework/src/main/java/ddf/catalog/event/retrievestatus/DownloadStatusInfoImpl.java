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
import ddf.catalog.resource.download.DownloadStatus;
import ddf.catalog.resource.download.ReliableResourceDownloader;
import ddf.security.SubjectOperations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.activities.ActivityEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadStatusInfoImpl implements DownloadStatusInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(DownloadStatusInfoImpl.class);

  private static final String UNKNOWN = "UNKNOWN";

  private Map<String, ReliableResourceDownloader> downloaders = new HashMap<>();

  private Map<String, String> downloadUsers = new HashMap<>();

  private EventAdmin eventAdmin;

  private SubjectOperations subjectOperations;

  public void setEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  public void setSubjectOperations(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }

  public void addDownloadInfo(
      String downloadIdentifier,
      ReliableResourceDownloader downloader,
      ResourceResponse resourceResponse) {
    downloaders.put(downloadIdentifier, downloader);
    org.apache.shiro.subject.Subject shiroSubject = null;
    try {
      shiroSubject = SecurityUtils.getSubject();
    } catch (Exception e) {
      LOGGER.debug("Could not determine current user, using session id.");
    }
    if (subjectOperations == null) {
      throw new IllegalStateException("Unable to perform subject operations at this time.");
    }
    String user =
        subjectOperations.getName(
            shiroSubject, getProperty(resourceResponse, ActivityEvent.USER_ID_KEY));
    downloadUsers.put(downloadIdentifier, user);
  }

  public List<String> getAllDownloads() {
    return getAllDownloads(null);
  }

  public List<String> getAllDownloads(String userId) {

    List<String> allDownloads = new ArrayList<String>();
    if (null == userId) {
      for (Map.Entry<String, ReliableResourceDownloader> item : downloaders.entrySet()) {
        allDownloads.add(item.getKey());
      }
    } else {
      for (Map.Entry<String, ReliableResourceDownloader> item : downloaders.entrySet()) {
        if (item.getKey().substring(0, userId.length()).equals(userId)) {
          allDownloads.add(item.getKey());
        }
      }
    }

    return allDownloads;
  }

  public Map<String, String> getDownloadStatus(String downloadIdentifier) {
    Map<String, String> statusMap = new HashMap<String, String>();
    ReliableResourceDownloader downloader = downloaders.get(downloadIdentifier);
    if (downloader != null) {
      Long downloadedBytes = downloader.getReliableResourceInputStreamBytesCached();
      try {
        Long totalBytes = Long.parseLong(downloader.getResourceSize());
        statusMap.put(
            DownloadStatus.PERCENT_KEY, Long.toString((downloadedBytes * 100) / totalBytes));

      } catch (Exception e) {
        statusMap.put(DownloadStatus.PERCENT_KEY, UNKNOWN);
      }
      statusMap.put(DownloadStatus.DOWNLOAD_ID_KEY, downloadIdentifier);
      statusMap.put(DownloadStatus.STATUS_KEY, downloader.getReliableResourceInputStreamState());
      statusMap.put(DownloadStatus.BYTES_DOWNLOADED_KEY, Long.toString(downloadedBytes));
      statusMap.put(
          DownloadStatus.FILE_NAME_KEY, downloader.getResourceResponse().getResource().getName());
      statusMap.put(DownloadStatus.USER_KEY, downloadUsers.get(downloadIdentifier));
    }
    return statusMap;
  }

  public void removeDownloadInfo(String downloadIdentifier) {
    downloaders.remove(downloadIdentifier);
    downloadUsers.remove(downloadIdentifier);
  }

  public void cancelDownload(String userId, String downloadIdentifier) {
    String downloadId = userId + downloadIdentifier;

    Map<String, String> propertiesMap = new HashMap<String, String>();
    propertiesMap.put(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);

    Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, propertiesMap);
    eventAdmin.postEvent(event);
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
