/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.kml.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.junit.BeforeClass;
import org.junit.Test;

public class KmlEndpointTest {

  private static final String BOMBER_ICON = "bomber-2.png";

  private static final String JET_ICON = "jetfighter.png";

  private static final String ICONS_DIR = "/icons/";

  private static final String TEST_ICONS_DIR = "/TestIcons/";

  private static final String TEST_HOST = "host";

  private static final String TEST_PORT = "80";

  private static final String LOCAL_SITE_NAME = "localSite";

  private static final String REMOTE_SITE_NAME = "remoteSite";

  private static UriInfo mockUriInfo = mock(UriInfo.class);

  private static MultivaluedMap<String, String> mockMap = mock(MultivaluedMap.class);

  private static CatalogFramework mockFramework = mock(CatalogFramework.class);

  private static SourceInfoResponse mockSourceInfoResponse = mock(SourceInfoResponse.class);

  private static Set<SourceDescriptor> descriptors = new HashSet<>();

  private static BrandingRegistry mockBranding = mock(BrandingRegistry.class);

  private static byte[] bomberBytes;

  private static byte[] jetBtyes;

  private static String bomberPath;

  private static String jetPath;

  @BeforeClass
  public static void setUp() throws IOException, URISyntaxException, SourceUnavailableException {
    when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://example.com"));

    URL bomberLocation = KmlEndpointTest.class.getResource(ICONS_DIR + BOMBER_ICON);
    bomberPath = bomberLocation.getPath().replaceAll(BOMBER_ICON, "");
    bomberBytes = IOUtils.toByteArray(bomberLocation.openStream());

    URL jetLocation = KmlEndpointTest.class.getResource(TEST_ICONS_DIR + JET_ICON);
    jetPath = jetLocation.getPath().replaceAll(JET_ICON, "");
    jetBtyes = IOUtils.toByteArray(jetLocation.openStream());

    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, TEST_HOST);
    System.setProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT, TEST_PORT);
    System.setProperty(SystemBaseUrl.EXTERNAL_HTTP_PORT, TEST_PORT);
    System.setProperty(SystemBaseUrl.INTERNAL_ROOT_CONTEXT, "/services");
    System.setProperty(SystemInfo.SITE_CONTACT, "example@example.com");

    when(mockFramework.getSourceInfo(any(SourceInfoRequest.class)))
        .thenReturn(mockSourceInfoResponse);
    SourceDescriptorImpl localDescriptor =
        new SourceDescriptorImpl(LOCAL_SITE_NAME, null, Collections.emptyList());
    SourceDescriptorImpl remoteDescriptor =
        new SourceDescriptorImpl(REMOTE_SITE_NAME, null, Collections.emptyList());
    descriptors.add(localDescriptor);
    descriptors.add(remoteDescriptor);
    when(mockSourceInfoResponse.getSourceInfo()).thenReturn(descriptors);
    when(mockBranding.getProductName()).thenReturn("PRODUCT 0.0.1");
  }

  @Test
  public void testGetKmlNetworkLink() {
    when(mockUriInfo.getQueryParameters(false)).thenReturn(mockMap);
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    kmlEndpoint.setDescription("This is some description.");
    kmlEndpoint.setLogo(
        "https://tools.codice.org/wiki/download/attachments/3047457/DDF?version=1&modificationDate=1369422662164&api=v2");
    kmlEndpoint.setWebSite("https://tools.codice.org/wiki/display/DDF/DDF+Home");
    Kml response = kmlEndpoint.getKmlNetworkLink(mockUriInfo);
    assertThat(response, notNullValue());
    assertThat(response.getFeature(), instanceOf(NetworkLink.class));
    NetworkLink networkLink = (NetworkLink) response.getFeature();
    Link link = networkLink.getLink();
    assertThat(link, notNullValue());
    assertThat(link.getHref(), notNullValue());
    UriBuilder builder = UriBuilder.fromUri(link.getHref());
    URI uri = builder.build();
    assertThat(uri.getHost(), is(TEST_HOST));
    assertThat(String.valueOf(uri.getPort()), is(TEST_PORT));
    assertThat(uri.getPath(), is("/services/catalog/kml/sources"));
  }

  @Test
  public void testGetAvailableSources()
      throws UnknownHostException, MalformedURLException, IllegalArgumentException,
          UriBuilderException, SourceUnavailableException {
    when(mockUriInfo.getQueryParameters(false)).thenReturn(mockMap);
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    Kml response = kmlEndpoint.getAvailableSources(mockUriInfo);
    assertThat(response, notNullValue());
    assertThat(response.getFeature(), instanceOf(Folder.class));
    Folder folder = (Folder) response.getFeature();
    assertThat(folder.getFeature(), notNullValue());
    assertThat(folder.getFeature().size(), is(2));
    assertThat(folder.getFeature().get(0), instanceOf(NetworkLink.class));
    assertThat(folder.getFeature().get(1), instanceOf(NetworkLink.class));
    NetworkLink nl1 = (NetworkLink) folder.getFeature().get(0);
    assertThat(nl1.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
    NetworkLink nl2 = (NetworkLink) folder.getFeature().get(1);
    assertThat(nl2.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
  }

  @Test
  public void testGetAvailableSourcesVisibleByDefault()
      throws UnknownHostException, MalformedURLException, IllegalArgumentException,
          UriBuilderException, SourceUnavailableException {
    when(mockUriInfo.getQueryParameters(false)).thenReturn(mockMap);
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    Kml response = kmlEndpoint.getAvailableSources(mockUriInfo);
    assertThat(response, notNullValue());
    assertThat(response.getFeature(), instanceOf(Folder.class));
    Folder folder = (Folder) response.getFeature();
    assertThat(folder.getFeature(), notNullValue());
    assertThat(folder.getFeature().size(), is(2));
    assertThat(folder.getFeature().get(0), instanceOf(NetworkLink.class));
    assertThat(folder.getFeature().get(1), instanceOf(NetworkLink.class));
    NetworkLink nl1 = (NetworkLink) folder.getFeature().get(0);
    assertThat(nl1.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
    assertThat(nl1.isVisibility(), is(false));
    NetworkLink nl2 = (NetworkLink) folder.getFeature().get(1);
    assertThat(nl2.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
    assertThat(nl2.isVisibility(), is(false));
  }

  @Test
  public void testGetAvailableSourcesWithCount()
      throws UnknownHostException, MalformedURLException, IllegalArgumentException,
          UriBuilderException, SourceUnavailableException {
    when(mockUriInfo.getQueryParameters(false)).thenReturn(mockMap);
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    kmlEndpoint.setMaxResults(250);
    Kml response = kmlEndpoint.getAvailableSources(mockUriInfo);
    assertThat(response, notNullValue());
    assertThat(response.getFeature(), instanceOf(Folder.class));
    Folder folder = (Folder) response.getFeature();
    assertThat(folder.getFeature(), notNullValue());
    assertThat(folder.getFeature().size(), is(2));
    assertThat(folder.getFeature().get(0), instanceOf(NetworkLink.class));
    assertThat(folder.getFeature().get(1), instanceOf(NetworkLink.class));
    NetworkLink nl1 = (NetworkLink) folder.getFeature().get(0);
    assertThat(nl1.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
    assertThat(nl1.getLink().getHttpQuery(), is("count=250"));
    NetworkLink nl2 = (NetworkLink) folder.getFeature().get(1);
    assertThat(nl2.getName(), anyOf(is(REMOTE_SITE_NAME), is(LOCAL_SITE_NAME)));
    assertThat(nl2.getLink().getHttpQuery(), is("count=250"));
  }

  /** Tests setting the icon directory location */
  @Test
  public void testGetIconLocation() {
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    byte[] response = kmlEndpoint.getIcon(null, BOMBER_ICON);
    assertThat(response, is(bomberBytes));
  }

  /** Tests missing icon in the default resource */
  @Test(expected = WebApplicationException.class)
  public void testExceptionGetIconLocation() {
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    kmlEndpoint.getIcon(null, JET_ICON);
  }

  @Test
  public void testGetIconCustomLocation() {
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    kmlEndpoint.setIconLoc(jetPath);
    byte[] response = kmlEndpoint.getIcon(null, JET_ICON);
    assertThat(response, is(jetBtyes));
  }

  /** Tests missing icon in the directory location */
  @Test(expected = WebApplicationException.class)
  public void testExceptionGetCustomIconLocation() {
    KmlEndpoint kmlEndpoint = new KmlEndpoint(mockBranding, mockFramework);
    kmlEndpoint.setIconLoc(bomberPath);
    kmlEndpoint.getIcon(null, JET_ICON);
  }
}
