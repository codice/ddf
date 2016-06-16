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

import javax.ws.rs.core.Response.Status;

public class DownloadToCacheOnlyException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final Status status;

    public DownloadToCacheOnlyException(Status status) {
        super();
        this.status = status;
    }

    public DownloadToCacheOnlyException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public DownloadToCacheOnlyException(Status status, String message, Throwable throwable) {
        super(message, throwable);
        this.status = status;
    }

    public DownloadToCacheOnlyException(Status status, Throwable throwable) {
        super(throwable);
        this.status = status;
    }
    
    public Status getStatus() {
        return status;
    }
}
