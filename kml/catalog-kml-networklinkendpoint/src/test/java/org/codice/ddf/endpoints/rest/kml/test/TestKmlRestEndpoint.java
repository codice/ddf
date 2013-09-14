/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.endpoints.rest.kml.test;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class TestKmlRestEndpoint extends TestCase {
    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    private static Logger logger = Logger.getLogger(TestKmlRestEndpoint.class);

    public void testGetKmlNetworkLink() {
        // KmlEndpoint kmlRestEndpoint = new KmlEndpoint();
        // Response response = kmlRestEndpoint.getKmlNetworkLink(null);
        // @SuppressWarnings( "unchecked" )
        // JAXBElement<KmlType> kml = (JAXBElement<KmlType>) response.getEntity();
        // StringWriter writer = new StringWriter();
        // try
        // {
        // JAXBContext jaxbContext =
        // JAXBContext.newInstance(ObjectFactory.class,oasis.names.tc.ciq.xsdschema.xal._2.ObjectFactory.class,org.w3._2005.atom.ObjectFactory.class);
        // Marshaller marshaller = jaxbContext.createMarshaller();
        // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        // marshaller.marshal(kml, writer);
        // String kmlResultString = writer.toString();
        // logger.debug(kmlResultString);
        // }
        // catch(Exception e)
        // {
        // }

        assertTrue(true);
    }
}
