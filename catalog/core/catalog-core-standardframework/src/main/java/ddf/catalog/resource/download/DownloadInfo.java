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
package ddf.catalog.resource.download;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Class that encapsulates the state of an ongoiong download. */
class DownloadInfo {

  private static final String DOWNLOAD_ID = "downloadId";

  private static final String FILE_NAME = "fileName";

  private static final String BYTES_DOWNLOADED = "bytesDownloaded";

  private static final String PERCENT = "percent";

  private static final String USER = "user";

  private static final String STATUS = "status";

  private String downloadId;

  private String fileName;

  private String status;

  private long bytesDownloaded;

  private String percentDownloaded;

  private List<String> users = new ArrayList<>();

  /**
   * Creates a new {@link DownloadInfo} object from the {@link Map} provided. The map must contain
   * at least the following keys, otherwise an {@link IllegalArgumentException} will be thrown:
   *
   * <ul>
   *   <li>downloadId
   *   <li>fileName
   *   <li>status
   * </ul>
   *
   * @param downloadStatus map containing the different attributes that will be used to initialize
   *     this {@link DownloadInfo} object
   */
  public DownloadInfo(Map<String, String> downloadStatus) {
    parse(downloadStatus);
  }

  public boolean isDownloadInState(DownloadState downloadState) {
    return status.equals(downloadState.name());
  }

  public String getDownloadId() {
    return downloadId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getStatus() {
    return status;
  }

  public long getBytesDownloaded() {
    return bytesDownloaded;
  }

  public String getPercentDownloaded() {
    return percentDownloaded;
  }

  public List<String> getUsers() {
    return users;
  }

  private void parse(Map<String, String> downloadStatus) {
    notNull(downloadStatus, "Download status map cannot be null");

    downloadId = downloadStatus.get(DOWNLOAD_ID);
    fileName = downloadStatus.get(FILE_NAME);
    status = downloadStatus.get(STATUS);

    try {
      bytesDownloaded = Long.parseLong(downloadStatus.getOrDefault(BYTES_DOWNLOADED, "0"));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Bytes downloaded must be a number");
    }

    percentDownloaded = downloadStatus.getOrDefault(PERCENT, "0");

    if (downloadStatus.containsKey(USER)) {
      users.add(downloadStatus.get(USER));
    }

    notBlank(this.downloadId, "Download ID cannot be null");
    notBlank(this.fileName, "File name cannot be null");
    notBlank(this.status, "Status cannot be null");
  }

  public String toString() {
    return String.format(
        "[download Id: %s; file: %s; status: %s; bytesDownloaded: %s, percent: %s; users: %s]",
        downloadId, fileName, status, bytesDownloaded, percentDownloaded, users);
  }
}
