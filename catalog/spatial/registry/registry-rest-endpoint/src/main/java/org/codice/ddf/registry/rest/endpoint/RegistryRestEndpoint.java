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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.internal.RegistryPublicationService;
import org.codice.ddf.registry.rest.endpoint.report.RegistryReportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

@Path("/")
public class RegistryRestEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryRestEndpoint.class);

    private FederationAdminService federationAdminService;

    private RegistryPublicationService registryPublicationService;

    private RegistryReportBuilder registryReportBuilder;

    List<String> validSections = Arrays.asList(registryReportBuilder.ORGANIZATIONS);

    @Path("/{registryId}/publication/{sourceId}")
    @POST
    public Response publish(@PathParam("registryId") String registryId,
            @PathParam("sourceId") String destinationId) {

        if (!isValidPublishUnpublishParams(registryId, destinationId)) {
            String message = "Registry ID and destination ID cannot be blank";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        try {
            registryPublicationService.publish(registryId, destinationId);
        } catch (FederationAdminException e) {
            String message = String.format("Error publishing node from destination %s",
                    destinationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
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
            String message = "Registry ID and destination ID cannot be blank";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        try {
            registryPublicationService.unpublish(registryId, destinationId);
        } catch (FederationAdminException e) {
            String message = String.format("Error unpublishing node from destination %s",
                    destinationId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
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
            String message = "Registry ID cannot be blank";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        RegistryPackageType registryPackage;
        try {
            Set<String> sourceIdsSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(sourceIds)) {
                sourceIdsSet.addAll(sourceIds);
            }
            registryPackage = federationAdminService.getRegistryObjectByRegistryId(registryId,
                    sourceIdsSet);
        } catch (FederationAdminException e) {
            String message = "Error getting registry package.";
            LOGGER.debug("{} For registry id: '{}', optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        if (registryPackage == null) {
            String message = "No registry package was found.";
            LOGGER.debug("{} For registry id: '{}', optional source ids: {}.",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        try {
            html = registryReportBuilder.getHtmlFromRegistryPackage(registryPackage,
                    registryReportBuilder.REPORT);
        } catch (IOException e) {
            String message = "Error in compiling and applying report template.";
            LOGGER.debug("{} For registry id: '{}'", message, registryId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        return Response.ok(html)
                .build();
    }

    @Path("/{registryId}/report/summary")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response viewSummaryInfoHtml(@PathParam("registryId") final String registryId) {
        String html = "";
        Metacard metacard;

        if (StringUtils.isBlank(registryId)) {
            String message = "Registry ID cannot be blank";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }
        try {
            List<Metacard> metacardList =
                    federationAdminService.getRegistryMetacardsByRegistryIds((Collections.singletonList(
                            registryId)));
            if (CollectionUtils.isNotEmpty(metacardList)) {
                metacard = metacardList.get(0);
            } else {
                String message = "Could not retrieve Metacard";
                LOGGER.debug("{} For registry id: '{}'", message, registryId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(getErrorHtmlString(message))
                        .build();
            }
        } catch (FederationAdminException e) {
            String message = "Error getting Metacard.";
            LOGGER.debug("{} For registry id: '{}'", message, registryId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        try {
            html = registryReportBuilder.getSummaryHtmlFromMetacard(metacard);
        } catch (IOException e) {
            String message = "Error in compiling and applying summary template.";
            LOGGER.debug("{} For registry id: '{}'", message, registryId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        return Response.ok(html)
                .build();
    }

    @Path("/{registryId}/report/{section}")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response viewSectionInfoHtml(@PathParam("registryId") final String registryId,
            @QueryParam("sourceId") final List<String> sourceIds,
            @PathParam("section") final String section) {
        String html = "";
        if (StringUtils.isBlank(registryId)) {
            String message = "Registry ID cannot be blank";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        if (!isValidSection(section)) {

            String message =
                    String.format("'%s' is an unknown section name. Valid Sections are: %s",
                            section,
                            validSections);
            LOGGER.debug("{} For registry id: '{}'", message, registryId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorHtmlString(message))
                    .build();
        }
        RegistryPackageType registryPackage;
        try {
            Set<String> sourceIdsSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(sourceIds)) {
                sourceIdsSet.addAll(sourceIds);
            }
            registryPackage = federationAdminService.getRegistryObjectByRegistryId(registryId,
                    sourceIdsSet);
        } catch (FederationAdminException e) {
            String message = "Error getting registry package.";
            LOGGER.debug("{} For registry id: '{}', optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        if (registryPackage == null) {
            String message = "No registry package was found.";
            LOGGER.debug("{} For registry id: '{}', optional source ids: {}.",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        try {
            html = registryReportBuilder.getHtmlFromRegistryPackage(registryPackage, section);
        } catch (IOException e) {
            String message = String.format("Error when compiling and applying %s template.",
                    section);
            LOGGER.debug("{} For registry id: '{}'", message, registryId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorHtmlString(message))
                    .build();
        }

        return Response.ok(html)
                .build();
    }

    private String getErrorHtmlString(String message) {
        try {
            return registryReportBuilder.getErrorHtml(message);
        } catch (IOException e) {
            String errorMessage = "Error when compiling and applying error template.";
            LOGGER.debug(message);
            return errorMessage;
        }
    }

    private boolean isValidSection(String section) {
        return validSections.contains(section);
    }

    private boolean isValidPublishUnpublishParams(String registryId, String destinationId) {
        return (StringUtils.isNotBlank(registryId) && StringUtils.isNotBlank(destinationId));
    }

    public void setRegistryPublicationService(
            RegistryPublicationService registryPublicationService) {
        this.registryPublicationService = registryPublicationService;
    }

    public void setRegistryReportBuilder(RegistryReportBuilder registryReportBuilder) {
        this.registryReportBuilder = registryReportBuilder;
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }
}
