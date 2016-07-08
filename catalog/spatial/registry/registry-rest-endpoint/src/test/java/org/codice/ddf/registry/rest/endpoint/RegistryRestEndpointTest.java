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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.RegistryPublicationService;
import org.codice.ddf.registry.rest.endpoint.report.RegistryReportBuilder;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryRestEndpointTest {

    private static final String REGISTRY_ID = "REGISTRY_ID";

    private static final String DESTINATION_ID = "DESTINATION_ID";

    private FederationAdminService federationAdminService = mock(FederationAdminService.class);

    private RegistryRestEndpoint restEndpoint;

    private RegistryReportBuilder registryReportBuilder = mock(RegistryReportBuilder.class);

    private RegistryPublicationService registryPublicationService =
            mock(RegistryPublicationService.class);

    private RegistryPackageType registryPackage = mock(RegistryPackageType.class);

    @Before
    public void setup() {
        restEndpoint = new RegistryRestEndpoint();
        restEndpoint.setFederationAdminService(federationAdminService);
        restEndpoint.setRegistryReportBuilder(registryReportBuilder);
        restEndpoint.setRegistryPublicationService(registryPublicationService);

    }

    @Test
    public void testFullReportWithBlankMetacard() throws IOException {
        Response response = restEndpoint.viewRegInfoHtml(null, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testFullReportWithFederationAdminException()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(), anySet())).thenThrow(
                new FederationAdminException());
        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testFullReportRegistryPackageNotFound()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(null);
        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void testFullReportWithRegistryPackage()
            throws ParserException, FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);

        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));

    }

    @Test
    public void testFullReportWithIOException() throws IOException, FederationAdminException {

        when(registryReportBuilder.getHtmlFromRegistryPackage(any(RegistryPackageType.class),
                anyString())).thenThrow(new IOException());
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);
        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testSectionReportWithBlankMetacard() throws IOException {
        Response response = restEndpoint.viewSectionInfoHtml(null, null, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testSectionReportWithFederationAdminException()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(), anySet())).thenThrow(
                new FederationAdminException());
        Response response = restEndpoint.viewSectionInfoHtml("metacardId",
                Arrays.asList("sourceIds"),
                "organizations");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testSectionReportWithUnknownSectionName()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);
        Response response = restEndpoint.viewSectionInfoHtml("metacardId",
                Arrays.asList("sourceIds"),
                "unknownSectionName");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testSectionReportWithSectionName()
            throws ParserException, FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);

        Response response = restEndpoint.viewSectionInfoHtml("metacardId",
                Arrays.asList("sourceIds"),
                "organizations");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));

    }

    @Test
    public void testSectionReportWithIOException() throws IOException, FederationAdminException {

        when(registryReportBuilder.getHtmlFromRegistryPackage(any(RegistryPackageType.class),
                anyString())).thenThrow(new IOException());
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);
        Response response = restEndpoint.viewSectionInfoHtml("metacardId",
                Arrays.asList("sourceIds"),
                "organizations");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testSectionReportRegistryPackageNotFound()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(null);
        Response response = restEndpoint.viewSectionInfoHtml("metacardId",
                Arrays.asList("sourceIds"),
                "organizations");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void testSummaryReportWithBlankRegistryID() throws IOException {
        Response response = restEndpoint.viewSummaryInfoHtml(null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testSummaryReportWithBlankMetacard() throws FederationAdminException, IOException {
        Response response = restEndpoint.viewSummaryInfoHtml("metacardId");
        when(federationAdminService.getRegistryMetacardsByRegistryIds(any(Collections.class).singletonList(
                "metcardId"))).thenReturn(null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void testSummaryReportWithMetacard()
            throws ParserException, FederationAdminException, IOException {

        List<Metacard> metacardList = mock(List.class);
        when(federationAdminService.getRegistryMetacardsByRegistryIds(anyList())).thenReturn(
                metacardList);
        Response response = restEndpoint.viewSummaryInfoHtml("metacardId");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));

    }

    @Test
    public void testSummaryReportWithFederationAdminException()
            throws FederationAdminException, IOException {
        when(federationAdminService.getRegistryMetacardsByRegistryIds(anyList())).thenThrow(new FederationAdminException());

        Response response = restEndpoint.viewSummaryInfoHtml("metacardId");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testSummaryReportWithIOException() throws FederationAdminException, IOException {
        List<Metacard> metacardList = mock(List.class);

        when(federationAdminService.getRegistryMetacardsByRegistryIds(anyList())).thenReturn(
                metacardList);
        when(registryReportBuilder.getSummaryHtmlFromMetacard(any(Metacard.class))).thenThrow(new IOException());

        Response response = restEndpoint.viewSummaryInfoHtml("metacardId");
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testPublishWithNullRegistryId() throws Exception {
        Response response = restEndpoint.publish(null, DESTINATION_ID);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testPublishWithNullDestinationId() throws Exception {
        Response response = restEndpoint.publish(REGISTRY_ID, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testPublishHelperException() throws Exception {
        doThrow(new FederationAdminException("error")).when(registryPublicationService)
                .publish(REGISTRY_ID, DESTINATION_ID);

        Response response = restEndpoint.publish(REGISTRY_ID, DESTINATION_ID);
        verify(registryPublicationService).publish(REGISTRY_ID, DESTINATION_ID);

        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testPublish() throws Exception {
        Response response = restEndpoint.publish(REGISTRY_ID, DESTINATION_ID);
        verify(registryPublicationService).publish(REGISTRY_ID, DESTINATION_ID);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testUnpublishWithNullRegistryId() throws Exception {
        Response response = restEndpoint.unpublish(null, DESTINATION_ID);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testUnpublishWithNullDestinationId() throws Exception {
        Response response = restEndpoint.unpublish(REGISTRY_ID, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testUnpublishHelperException() throws Exception {
        doThrow(new FederationAdminException("error")).when(registryPublicationService)
                .unpublish(anyString(), anyString());

        Response response = restEndpoint.unpublish(REGISTRY_ID, DESTINATION_ID);
        verify(registryPublicationService).unpublish(REGISTRY_ID, DESTINATION_ID);

        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testUnpublish() throws Exception {
        Response response = restEndpoint.unpublish(REGISTRY_ID, DESTINATION_ID);
        verify(registryPublicationService).unpublish(REGISTRY_ID, DESTINATION_ID);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));
    }
}
