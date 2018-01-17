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
package org.codice.ddf.catalog.admin.downloadmanager;

import java.util.List;
import java.util.Map;

/**
 * This class is an MBean endpoint for communication with a front-end Admin UI which would show all
 * current downloads across all users. The methods are ways to retrieve and manipulate the download
 * data.
 */
public interface DownloadManagerServiceMBean {

  /**
   * Function to get information about every download.
   *
   * @return Returns the information in an array of maps, where each map holds information about a
   *     specific download.
   * @see ddf.catalog.resource.download.ReliableResourceStatus.DownloadStatus
   */
  List<Map<String, String>> getAllDownloadsStatus();

  /**
   * Function to get information about a specific download.
   *
   * @param downloadIdentifier The randomly generated downloadId string assigned to the download at
   *     its start.
   * @return Returns a map of attributes describing the download.
   * @see ddf.catalog.resource.download.ReliableResourceStatus.DownloadStatus
   */
  Map<String, String> getDownloadStatus(String downloadIdentifier);

  /**
   * Function to get all downloads.
   *
   * @return Returns an array of downloadIdentifier Strings.
   */
  List<String> getAllDownloads();

  /**
   * Function to get all downloads for a specific user.
   *
   * @param userId The id of the user.
   * @return Returns an array of downloadIdentifier Strings, similar to {@link
   *     DownloadManagerServiceMBean#getAllDownloads()}.
   */
  List<String> getAllDownloads(String userId);

  /**
   * Function to remove the map entry corresponding to the downloadIdentifier passed it. This means
   * it will no longer be returned by {@link DownloadManagerServiceMBean#getAllDownloadsStatus()},
   * {@link DownloadManagerServiceMBean#getAllDownloadsStatus()}, {{@link
   * DownloadManagerServiceMBean#getAllDownloads()}, or {@link
   * DownloadManagerServiceMBean#getAllDownloads(String)}.
   *
   * @param downloadIdentifier The randomly generated downloadId string assigned to the download at
   *     its start.
   */
  void removeDownloadInfo(String downloadIdentifier);

  /**
   * Function to cancel a download.
   *
   * @param userId The id of the user who is performing the download.
   * @param downloadIdentifier The randomly generated downloadId string assigned to the download at
   *     its start.
   */
  void cancelDownload(String userId, String downloadIdentifier);
}
