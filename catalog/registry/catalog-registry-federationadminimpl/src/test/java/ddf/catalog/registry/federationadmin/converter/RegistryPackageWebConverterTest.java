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
package ddf.catalog.registry.federationadmin.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryPackageWebConverterTest {
    private Parser parser;

    private ParserConfigurator configurator;

    @Before
    public void setUp() {
        parser = new XmlParser();

        configurator = parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                net.opengis.ogc.ObjectFactory.class.getPackage()
                        .getName(),
                net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                        .getName()),
                this.getClass()
                        .getClassLoader());
    }

    @Test
    public void testRoundTrip() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");

        Map<String, Object> registryMap = RegistryPackageWebConverter.getRegistryObjectWebMap(
                registryObject);

        RegistryObjectType convertedRegistryObject =
                RegistryPackageWebConverter.getRegistryPackageFromWebMap(registryMap);

        assertThat(registryObject.getObjectType(),
                is(equalTo(convertedRegistryObject.getObjectType())));
        assertThat(registryObject.getId(), is(equalTo(convertedRegistryObject.getId())));
        assertThat(registryObject.getHome(), is(equalTo(convertedRegistryObject.getHome())));
        assertThat(registryObject.getExternalIdentifier(),
                is(equalTo(convertedRegistryObject.getExternalIdentifier())));
        assertThat(((RegistryPackageType) registryObject).getRegistryObjectList()
                        .getIdentifiable()
                        .size(),
                is(equalTo(((RegistryPackageType) convertedRegistryObject).getRegistryObjectList()
                        .getIdentifiable()
                        .size())));

        // Skipping some of these because the xml to object dates are stored as strings
        // and the converted values for the same are stored as Date objects so they don't match.
        List<Integer> indexes = Arrays.asList(4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15);
        for (int i : indexes) {
            assertThat(((RegistryPackageType) registryObject).getRegistryObjectList()
                            .getIdentifiable()
                            .get(i)
                            .getValue(),
                    is(equalTo(((RegistryPackageType) convertedRegistryObject).getRegistryObjectList()
                            .getIdentifiable()
                            .get(i)
                            .getValue())));
        }
    }

    private RegistryObjectType getRegistryObjectFromResource(String path) throws ParserException {
        RegistryObjectType registryObject = null;
        JAXBElement<RegistryObjectType> jaxbRegistryObject = parser.unmarshal(configurator,
                JAXBElement.class,
                getClass().getResourceAsStream(path));

        if (jaxbRegistryObject != null) {
            registryObject = jaxbRegistryObject.getValue();
        }

        return registryObject;
    }

}
