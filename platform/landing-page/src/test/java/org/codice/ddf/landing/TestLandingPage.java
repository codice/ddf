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
package org.codice.ddf.landing;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.webconsole.BrandingPlugin;
import org.codice.ddf.branding.BrandingResourceProvider;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLandingPage {

    private static String productName = "Awesome Product";

    private static String version = productName + " " + "10.4.GOOD-BUDDY";

    private static String imgPath = "/img";

    private static String faviconPath = "/favicon";

    private static String fakeImg = "fake.png";

    private static LandingPage landingPage;

    private static List<String> sorted;

    @BeforeClass
    public static void setupLandingPage() throws IOException {
        BrandingResourceProvider provider = mock(BrandingResourceProvider.class);
        landingPage = new LandingPage(provider);
        when(provider.getResourceAsBytes(imgPath)).thenReturn(fakeImg.getBytes());
        when(provider.getResourceAsBytes(faviconPath)).thenReturn(new byte[0]);
        BrandingPlugin branding = mock(BrandingPlugin.class);
        when(branding.getProductName()).thenReturn(version);
        when(branding.getFavIcon()).thenReturn(faviconPath);
        when(branding.getProductImage()).thenReturn(imgPath);
        landingPage.setBranding(branding);
        String firstDateLeadingZeroes = "05/07/20 stuff happened";
        String secondDateNoLeadingZeroes = "4/3/20 old stuff happened";
        String noDate = "something happened";
        List<String> unsorted = Arrays.asList(secondDateNoLeadingZeroes,
                noDate,
                firstDateLeadingZeroes);
        sorted = Arrays.asList(firstDateLeadingZeroes, secondDateNoLeadingZeroes, noDate);
        landingPage.setAnnouncements(unsorted);
    }

    @Test
    public void testSetBranding() throws IOException {
        assertThat(landingPage.getVersion(), is(equalTo(version)));
        assertThat(landingPage.getTitle(), is(equalTo(productName)));
        assertThat(landingPage.getFavicon(), is(equalTo("")));
        assertThat(landingPage.getProductImage(),
                is(equalTo(Base64.encodeBase64String(fakeImg.getBytes()))));
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
}
