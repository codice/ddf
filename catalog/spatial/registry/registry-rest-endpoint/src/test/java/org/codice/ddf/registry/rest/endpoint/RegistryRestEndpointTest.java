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
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.RegistryPublicationService;
import org.codice.ddf.registry.rest.endpoint.report.RegistryReportMapBuilder;
import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryRestEndpointTest {

    private FederationAdminService federationAdminService = mock(FederationAdminService.class);

    private RegistryRestEndpoint restEndpoint;

    private RegistryReportMapBuilder reportMapBuilder = mock(RegistryReportMapBuilder.class);

    private RegistryPublicationService registryPublicationService =
            mock(RegistryPublicationService.class);

    private RegistryPackageType registryPackage = mock(RegistryPackageType.class);

    private static final String REGISTRY_ID = "REGISTRY_ID";

    private static final String DESTINATION_ID = "DESTINATION_ID";

    @Before
    public void setup() {
        restEndpoint = new RegistryRestEndpoint();
        restEndpoint.setFederationAdminService(federationAdminService);
        restEndpoint.setRegistryReportMapBuilder(reportMapBuilder);
        restEndpoint.setRegistryPublicationService(registryPublicationService);

        when(reportMapBuilder.buildRegistryMap(any(RegistryPackageType.class))).thenReturn(new HashMap<>());
    }

    @Test
    public void testReportWithBlankMetacard() {
        RegistryRestEndpoint reportViewer = new RegistryRestEndpoint();
        Response response = reportViewer.viewRegInfoHtml(null, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testReportWithFederationAdminException() throws FederationAdminException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(), anySet())).thenThrow(
                new FederationAdminException());
        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testReportRegistryPackageNotFound() throws FederationAdminException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(null);
        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void testReportWithRegistryPackage() throws ParserException, FederationAdminException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anySet())).thenReturn(registryPackage);

        Response response = restEndpoint.viewRegInfoHtml("metacardId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));

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
