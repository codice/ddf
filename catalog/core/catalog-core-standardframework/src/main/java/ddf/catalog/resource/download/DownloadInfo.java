/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.download;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ddf.catalog.resource.download.DownloadManagerState.DownloadState;

public class DownloadInfo {

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
        // TODO - Check for nulls
        downloadId = downloadStatus.get(DOWNLOAD_ID);
        fileName = downloadStatus.get(FILE_NAME);
        status = downloadStatus.get(STATUS);
        bytesDownloaded = Long.parseLong(downloadStatus.get(BYTES_DOWNLOADED));
        percentDownloaded = downloadStatus.get(PERCENT);
        users.add(downloadStatus.get(USER));
    }

    public String toString() {
        return String.format(
                "[download Id: %s; file: %s; status: %s; bytesDownloaded: %s, percent: %s; users: %s]",
                downloadId,
                fileName,
                status,
                bytesDownloaded,
                percentDownloaded,
                users);
    }
}
