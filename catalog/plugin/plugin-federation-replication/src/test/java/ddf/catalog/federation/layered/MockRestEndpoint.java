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
package ddf.catalog.federation.layered;

import java.io.InputStream;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

@Path("/")
public class MockRestEndpoint {

    // private static final String RESOURCE_OWNER_USERNAME_KEY =
    // "resource.owner.username";
    private static final Logger LOGGER = Logger.getLogger(MockRestEndpoint.class);

    public MockRestEndpoint() {
        LOGGER.debug("Constructing rest endpoint");
    }

    @GET
    @Path("/{id:.*}")
    public Response getDocument(@PathParam("id")
    String id, @QueryParam("transform")
    String transformerParam, @Context
    UriInfo uriInfo) {
        return Response.ok().build();
    }

    @PUT
    @Path("/{id:.*}")
    public Response updateDocument(@PathParam("id")
    String id, @Context
    HttpHeaders headers, InputStream message) {

        LOGGER.info("id=" + id);
        return Response.ok().build();
    }

    /**
     * REST Post. Creates a new metadata entry in the catalog.
     * 
     * @param message
     * @return
     */
    @POST
    public Response addDocument(@Context
    HttpHeaders headers, @Context
    UriInfo requestUriInfo, InputStream message) {
        LOGGER.debug("POST");
        return Response.ok().build();
    }

    /**
     * REST Delete. Deletes a record from the catalog.
     * 
     * @param id
     * @return
     */
    @DELETE
    @Path("/{id:.*}")
    public Response deleteDocument(@PathParam("id")
    String id) {
        LOGGER.debug("DELETE");
        return Response.ok().build();
    }

}
