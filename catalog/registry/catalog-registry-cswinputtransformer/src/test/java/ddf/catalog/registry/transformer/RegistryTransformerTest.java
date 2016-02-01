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
 **/
package ddf.catalog.registry.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import ddf.catalog.data.Metacard;
import ddf.catalog.registry.common.metacard.RegistryMetacardImpl;
import ddf.catalog.registry.common.metacard.RegistryServiceMetacardType;
import ddf.catalog.transform.CatalogTransformerException;

@RunWith(JUnit4.class)
public class RegistryTransformerTest {

    private RegistryTransformer rit;

    private void assertRegistryMetacard(Metacard meta) {
        assertThat(meta.getContentTypeName(),
                is(RegistryServiceMetacardType.SERVICE_REGISTRY_METACARD_TYPE_NAME));
    }

    @Before
    public void setUp() {
        rit = new RegistryTransformer(new Gml3ToWkt(new XmlParser()));
    }

    @Test(expected = IOException.class)
    public void testBadInputStream() throws Exception {
        InputStream is = Mockito.mock(InputStream.class);
        doThrow(new IOException()).when(is).read(any());
        rit.transform(is);
    }

    @Test
    public void testBasicTransformWithoutId() throws Exception {
        assertRegistryMetacard(convert("/csw-rim-service.xml"));
    }

    @Test
    public void testBasicTransformWithId() throws Exception {
        InputStream is = getClass().getResourceAsStream("/csw-rim-service.xml");
        Metacard meta = rit.transform(is, "my-id");
        assertRegistryMetacard(meta);
    }

    @Test
    public void testBasicInfo() throws Exception {
        RegistryMetacardImpl meta = convert("/csw-basic-info.xml");
        assertRegistryMetacard(meta);

        assertThat(meta.getId(), is("2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(meta.getTitle(), is("my service"));
        assertThat(meta.getDescription(), is("something"));
        assertThat(meta.getContentTypeVersion(), is("0.0.0"));
    }

    @Test
    public void testOrgInfo() throws Exception {
        RegistryMetacardImpl meta = convert("/csw-org-info.xml");
        assertRegistryMetacard(meta);

        assertThat(meta.getOrgName(), is("Codice"));
        assertThat(meta.getOrgAddress(), is("1234 Some Street, Phoenix, AZ 85037, USA"));
        assertThat(meta.getOrgPhoneNumber(), is("(555) 555-5555 extension 1234"));
        assertThat(meta.getOrgEmail(), is("emailaddress@something.com"));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadBindingService() throws Exception {
        convert("/bad-binding-service.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadBindingServiceMultiple() throws Exception {
        convert("/bad-binding-service-multiple.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoServiceId() throws Exception {
        convert("/bad-id-service.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadObjectType() throws Exception {
        convert("/bad-binding-service.xml");
    }

    @Test
    public void testCustomSlotSaved() throws Exception {
        // Just test that an unknown slot gets saved to the metadata field and not discarded.
        assertThat(convert("/custom-slot-service.xml").getMetadata().contains("unknowSlotName"),
                is(true));
    }

    @Test
    public void testServiceWithMinimumBinding() throws Exception {
        RegistryMetacardImpl m = convert("/valid-federation-min-service.xml");
        assertThat(m.getAttribute(RegistryServiceMetacardType.SERVICE_BINDING_TYPES).getValue(),
                is("csw"));
    }

    @Test
    public void testServiceWithMultipleBindings() throws Exception {
        RegistryMetacardImpl m = convert("/valid-federation-multiple-service.xml");
        assertThat(m.getAttribute(RegistryServiceMetacardType.SERVICE_BINDING_TYPES).getValue(),
                is("csw, soap"));
    }

    @Test
    public void testMinimumValidService() throws Exception {
        convert("/empty-service.xml");
    }

    @Test
    public void testMetacardToXml() throws Exception {
        String in = IOUtils.toString(getClass().getResourceAsStream("/csw-rim-service.xml"));
        Metacard m = rit.transform(IOUtils.toInputStream(in));
        String out = IOUtils.toString(rit.transform(m, null).getInputStream());
        assertThat(in, is(out));
    }

    @Test
    public void testLastUpdated() throws Exception {
        RegistryMetacardImpl m = convert("/csw-last-updated.xml");
        assertThat(m.getModifiedDate().toString(), is("Tue Jan 26 10:16:34 MST 2016"));
    }

    private RegistryMetacardImpl convert(String path) throws Exception {
        return (RegistryMetacardImpl) rit.transform(getClass().getResourceAsStream(path));
    }
}
