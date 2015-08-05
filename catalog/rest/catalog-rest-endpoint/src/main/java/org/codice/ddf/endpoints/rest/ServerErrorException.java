/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.endpoints.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ServerErrorException extends WebApplicationException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ServerErrorException(String message, Status status) {
        super(Response.status(status).entity("<pre>" + message + "</pre>").type(MediaType.TEXT_HTML)
                .build());

    }

    public ServerErrorException(Throwable t, Status status) {
        super(t, Response.status(status).entity("<pre>" + t.getMessage() + "</pre>")
                .type(MediaType.TEXT_HTML).build());
    }
}
