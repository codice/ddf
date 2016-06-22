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

package org.codice.ddf.registry.rest.endpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.RegistryPublicationService;
import org.codice.ddf.registry.rest.endpoint.report.RegistryReportMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

@Path("/")
public class RegistryRestEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryRestEndpoint.class);

    private FederationAdminService federationAdminService;

    private ClassPathTemplateLoader templateLoader;

    private RegistryPublicationService registryPublicationService;

    private RegistryReportMapBuilder reportMapBuilder;

    public RegistryRestEndpoint() {
        templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix("/templates");
        templateLoader.setSuffix(".hbt");
    }

    @Path("/{registryId}/publication/{sourceId}")
    @POST
    public Response publish(@PathParam("registryId") String registryId,
            @PathParam("sourceId") String destinationId) {

        if (!isValidPublishUnpublishParams(registryId, destinationId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Registry ID and destination ID cannot be blank")
                    .build();
        }

        try {
            registryPublicationService.publish(registryId, destinationId);
        } catch (FederationAdminException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error publishing node to destination " + destinationId)
                    .build();
        }

        return Response.status(Response.Status.OK)
                .build();
    }

    @Path("/{registryId}/publication/{sourceId}")
    @DELETE
    public Response unpublish(@PathParam("registryId") String registryId,
            @PathParam("sourceId") String destinationId) {
        if (!isValidPublishUnpublishParams(registryId, destinationId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Registry ID and destination ID cannot be blank")
                    .build();
        }

        try {
            registryPublicationService.unpublish(registryId, destinationId);
        } catch (FederationAdminException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error unpublishing node from destination " + destinationId)
                    .build();
        }

        return Response.status(Response.Status.OK)
                .build();
    }

    @Path("/{registryId}/report")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response viewRegInfoHtml(@PathParam("registryId") final String registryId,
            @QueryParam("sourceId") final List<String> sourceIds) {
        String html = "";
        if (StringUtils.isBlank(registryId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Registry Id cannot be blank")
                    .build();
        }

        RegistryPackageType registryPackage;
        try {
            registryPackage = federationAdminService.getRegistryObjectByRegistryId(registryId,
                    sourceIds);
        } catch (FederationAdminException e) {
            String message = "Error getting registry package.";
            LOGGER.error("{} For registry id: '{}', optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build();
        }

        if (registryPackage == null) {
            String message = "No registry package was found.";
            LOGGER.error("{} For registry id: '{}', optional source ids: {}.",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(message)
                    .build();
        }

        Map<String, Object> registryMap = reportMapBuilder.buildRegistryMap(registryPackage);

        try {
            Handlebars handlebars = new Handlebars(templateLoader);
            Template template = handlebars.compile("report");
            html = template.apply(registryMap);
        } catch (IOException e) {
            LOGGER.error("Error compiling and applying report template.");
        }

        return Response.ok(html)
                .build();
    }

    private boolean isValidPublishUnpublishParams(String registryId, String destinationId) {
        return (StringUtils.isNotBlank(registryId) && StringUtils.isNotBlank(destinationId));
    }

    public void setRegistryPublicationService(
            RegistryPublicationService registryPublicationService) {
        this.registryPublicationService = registryPublicationService;
    }

    public void setRegistryReportMapBuilder(RegistryReportMapBuilder reportMapBuilder) {
        this.reportMapBuilder = reportMapBuilder;
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }
}
