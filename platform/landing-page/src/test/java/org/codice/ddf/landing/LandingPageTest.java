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
package org.codice.ddf.landing;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.platform.resource.bundle.locator.ResourceBundleLocator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.branding.impl.BrandingRegistryImpl;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LandingPageTest {

  private static String productName = "Awesome Product";

  private static String version = productName + " " + "10.4.GOOD-BUDDY";

  private static String fakeImg = "fake.png";

  private static LandingPage landingPage;

  private static List<String> sorted;

  private static ResourceBundleLocator resourceBundleLocator;

  @BeforeClass
  public static void setupLandingPage() throws IOException {
    BrandingPlugin brandingPlugin = mock(BrandingPlugin.class);
    when(brandingPlugin.getBase64FavIcon()).thenReturn("");
    when(brandingPlugin.getBase64ProductImage())
        .thenReturn(Base64.getEncoder().encodeToString(fakeImg.getBytes()));
    landingPage = new LandingPage();
    BrandingRegistry branding = mock(BrandingRegistryImpl.class);
    when(branding.getProductName()).thenReturn(productName);
    when(branding.getProductVersion()).thenReturn(version);
    when(branding.getBrandingPlugins()).thenReturn(Collections.singletonList(brandingPlugin));
    when(branding.getAttributeFromBranding(any())).thenCallRealMethod();
    landingPage.setBranding(branding);
    String firstDateLeadingZeroes = "05/07/20 stuff happened";
    String secondDateNoLeadingZeroes = "4/3/20 old stuff happened";
    String noDate = "something happened";
    List<String> unsorted =
        Arrays.asList(secondDateNoLeadingZeroes, noDate, firstDateLeadingZeroes);
    sorted = Arrays.asList(firstDateLeadingZeroes, secondDateNoLeadingZeroes, noDate);
    landingPage.setAnnouncements(unsorted);
    resourceBundleLocator = mock(ResourceBundleLocator.class);
  }

  @Test
  public void testSetBranding() {
    assertThat(landingPage.getVersion(), is(equalTo(version)));
    assertThat(landingPage.getTitle(), is(equalTo(productName)));
    assertThat(landingPage.getFavicon(), is(equalTo("")));
    assertThat(
        landingPage.getProductImage(),
        is(equalTo(Base64.getEncoder().encodeToString(fakeImg.getBytes()))));
  }

  @Test
  public void testSetAnnouncements() {
    assertThat(landingPage.getAnnouncements(), is(equalTo(sorted)));
  }

  @Test
  public void testCompileTemplate() {
    String html = landingPage.compileTemplateWithProperties();
    assertThat(html, containsString(productName));
    assertThat(html, containsString("src=\"data:image/png;base64,ZmF"));
    assertThat(html, containsString("stuff happened"));
  }

  @Test
  public void testDefaultTitle() {
    BrandingRegistry brandingPlugin = mock(BrandingRegistry.class);
    when(brandingPlugin.getProductName()).thenReturn("DDF");
    LandingPage landingPage = new LandingPage();
    landingPage.setBranding(brandingPlugin);
    assertThat(landingPage.getTitle(), is(equalTo("DDF")));
  }

  @Test
  public void testDoGet() throws IOException, ServletException {
    String compiledTemplate = landingPage.compileTemplateWithProperties();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("");
    PrintWriter writer = mock(PrintWriter.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(writer);
    landingPage.doGet(request, response);
    verify(writer, times(1)).write(compiledTemplate);
  }

  @Test
  public void testGetKeywords() throws IOException {
    doReturn(ResourceBundle.getBundle("LandingPageBundle"))
        .when(resourceBundleLocator)
        .getBundle(any(String.class));
    landingPage.setSourceAvailabilityHeader(resourceBundleLocator);
    MatcherAssert.assertThat(
        landingPage.getSourceAvailabilityHeader(), is("Data Source Availability"));
  }
}
