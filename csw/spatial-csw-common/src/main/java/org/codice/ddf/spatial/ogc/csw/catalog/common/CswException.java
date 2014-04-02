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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.net.HttpURLConnection;

/**
 * The exception thrown when an exception response is returned by the remote CSW service.
 * 
 * @author rodgersh
 * 
 */
public class CswException extends Exception {

    private static final long serialVersionUID = 1L;

    private int httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
    private String exceptionCode = null;
    private String locator = null;

    public CswException(String message) {
        super(message);
    }

    public CswException(Throwable throwable) {
        super(throwable);
    }

    public CswException(String message, int httpStatus) {
        super(message);

        this.httpStatus = httpStatus;
    }

    public CswException(Throwable throwable, int httpStatus) {
        super(throwable);

        this.httpStatus = httpStatus;
    }

    public CswException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public CswException(String message, String exceptionCode, String locator) {
        this(message);
        
        this.exceptionCode = exceptionCode;
        this.locator = locator;
    }

    public CswException(Throwable throwable, String exceptionCode, String locator) {
        this(throwable);

        this.exceptionCode = exceptionCode;
        this.locator = locator;
    }

    public CswException(String message, int httpStatus, String exceptionCode, String locator) {
        this(message, httpStatus);

        this.exceptionCode = exceptionCode;
        this.locator = locator;
    }

    public CswException(Throwable throwable, int httpStatus, String exceptionCode, String locator) {
        this(throwable, httpStatus);

        this.exceptionCode = exceptionCode;
        this.locator = locator;
    }

    public CswException(String message, Throwable throwable, String exceptionCode, String locator) {
        this(message, throwable);

        this.exceptionCode = exceptionCode;
        this.locator = locator;
    }


    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

}
