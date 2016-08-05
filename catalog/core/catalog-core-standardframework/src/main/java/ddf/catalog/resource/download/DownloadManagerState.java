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

/**
 * State for the entire download of a single product. This state is used to keep
 * the @ReliableResourceInputStream informed of the overall state of the
 *
 * @ReliableResourceDownloadManager as the download progresses.
 */
public class DownloadManagerState {

    private DownloadState state;

    public DownloadState getDownloadState() {
        return state;
    }

    public void setDownloadState(DownloadState state) {
        this.state = state;
    }

    public enum DownloadState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELED,
        FAILED
    }
}
