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
package org.codice.ddf.spatial.ogc.wfs.catalog.source.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.apache.ws.commons.schema.XmlSchema;
//import org.junit.BeforeClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlSchemaMessageBodyReader {

    private static BufferedInputStream ser;

    private static BufferedInputStream schemaXml;

    @BeforeClass
    public static void setUp() {
        ser = new BufferedInputStream(TestXmlSchemaMessageBodyReader.class.getClass()
                .getResourceAsStream("/serviceExceptionReport.xml"));
        schemaXml = new BufferedInputStream(TestXmlSchemaMessageBodyReader.class.getClass()
                .getResourceAsStream("/SampleSchema.xsd"));
        ser.mark(1000);
        schemaXml.mark(1000);
    }

    @Test
    public void testReadFromValidSchema() throws WebApplicationException, IOException {
        XmlSchemaMessageBodyReader reader = new XmlSchemaMessageBodyReader();
        XmlSchema schema = reader.readFrom(null, null, null, null, null, schemaXml);
        assertNotNull(schema);
    }

    @Test
    public void testReadFromServiceExceptionReport() throws WebApplicationException, IOException {
        XmlSchemaMessageBodyReader reader = new XmlSchemaMessageBodyReader();
        XmlSchema schema = reader.readFrom(null, null, null, null, null, ser);
        assertNull(schema);
    }
}
