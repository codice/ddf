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
package ddf.catalog.resource.download;


/**
 * The current status of a single product download. Since a product retrieval
 * may go through several retry attempts during the download process, it is possible
 * for the download status to have several of these values over the entire span
 * of the download. For example, a product download could be interrupted due to
 * a brief network connection drop, and then the download could complete successfully.
 * 
 * @author rodgers
 *
 */
public enum DownloadStatus {
    RESOURCE_DOWNLOAD_COMPLETE,
    RESOURCE_DOWNLOAD_INTERRUPTED,
    RESOURCE_DOWNLOAD_CANCELED,
    CLIENT_OUTPUT_STREAM_EXCEPTION,
    CACHED_FILE_OUTPUT_STREAM_EXCEPTION,
    PRODUCT_INPUT_STREAM_EXCEPTION
};
