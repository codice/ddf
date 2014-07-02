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
package org.codice.ddf.catalog.admin.downloadmanager;

import java.util.ArrayList;
import java.util.Map;


public interface DownloadManagerServiceMBean {

    /**
     * Function to get information about every download.
     * @return Returns the information in an array of maps, where each map holds information about a specific
     * download; attributes are:
     *     "percent" (percent completed)
     *     "downloadId" (randomly generated downloadId assigned to each download at its beginning)
     *     "status" (status of download, e.g. "COMPLETED", "IN_PROGRESS" etc)
     *     "bytesDownloaded" (count of bytes that have been downloaded to cache)
     *     "fileName" (name of the file being downloaded)
     *     "user" (identifer of the user performing the download)
     */
    ArrayList<Map<String, String>> getAllDownloadsStatus();

    /**
     * Function to get information about a specific download.
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     * @return Returns a map of attributes describing the download; see {@link this.getAllDownloadsStatus} for details.
     */
    Map<String, String> getDownloadStatus(String downloadIdentifier);

    /**
     * Function to get all downloads.
     * @return Returns an array of downloadIdentifier Strings
     */
    ArrayList<String> getAllDownloads();

    /**
     * Function to get all downloads for a specific user.
     * @param userId The id of the user.
     * @return Returns an array of downloadIdentifier Strings, similar to {@link this.getAllDownloads}.
     */
    ArrayList<String> getAllDownloads(String userId);

    /**
     * Function to remove the map entry corresponding to the downloadIdentifer passed it. This means it will no longer be
     * returned by {@link this.getAllDownloadsStatus}, {@link this.getDownloadStatus}, or {@link this.getAllDownloads}.
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     */
    void removeDownloadInfo(String downloadIdentifier);

}
