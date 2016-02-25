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
package ddf.catalog.nato.stanag4559.server.server;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("data")
public class MockWebService {

    private static final String PRODUCT_RELATIVE_PATH = "/src/main/java/ddf/catalog/nato/stanag4559/server/data/product.jpg";

    private static final String IOR_RELATIVE_PATH = "/target/ior.txt";

    @GET
    @Path("product.jpg")
    @Produces("text/plain")
    public Response getProductFile() {
        File file = new File(System.getProperty("user.dir") + PRODUCT_RELATIVE_PATH);
        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition", "attachment; filename=product.jpg");
        return response.build();
    }

    @GET
    @Path("ior.txt")
    @Produces("text/plain")
    public Response getIORFile() {
        File file = new File(System.getProperty("user.dir") + IOR_RELATIVE_PATH);
        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition", "attachment; filename=ior.txt");
        return response.build();
    }
}
