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

public final class DownloadStatus {

  /**
   * map key that indicates the randomly generated downloadId assigned to each download at its
   * beginning
   */
  public static final String DOWNLOAD_ID_KEY = "downloadId";

  /** map key that indicates the name of the file being downloaded */
  public static final String FILE_NAME_KEY = "fileName";

  /** map key that indicates the count of bytes that have been downloaded to cache */
  public static final String BYTES_DOWNLOADED_KEY = "bytesDownloaded";

  /** map key that indicates the percent completed */
  public static final String PERCENT_KEY = "percent";

  /** map key that indicates the user performing the download */
  public static final String USER_KEY = "user";

  /** map key that indicates the status of download, e.g. "COMPLETED", "IN_PROGRESS" etc */
  public static final String STATUS_KEY = "status";

  private DownloadStatus() {}
}
