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
package org.codice.ddf.spatial.ogc.wcs.catalog;

import java.net.HttpURLConnection;

/**
 * The exception thrown when an exception response is returned by the remote WCS service.
 * 
 * @author rodgersh
 * 
 */
public class WcsException extends Exception {

    private static final long serialVersionUID = 1L;

    private int httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;

    public WcsException(String message) {
        super(message);
    }

    public WcsException(Throwable throwable) {
        super(throwable);
    }

    public WcsException(String message, int httpStatus) {
        super(message);

        this.httpStatus = httpStatus;
    }

    public WcsException(Throwable throwable, int httpStatus) {
        super(throwable);

        this.httpStatus = httpStatus;
    }

    public WcsException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

}
