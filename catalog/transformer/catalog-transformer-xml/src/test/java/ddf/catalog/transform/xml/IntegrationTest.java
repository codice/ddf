/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.transform.xml;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.xml.XmlInputTransformer;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;

public class IntegrationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory
            .getLogger(IntegrationTest.class);

    @Test
    public void testInputAndOutput() throws CatalogTransformerException,
            IOException {
        InputTransformer inputTransformer = new XmlInputTransformer();
        MetacardTransformer outputTransformer = new XmlMetacardTransformer();

        InputStream input = getClass().getResourceAsStream("/ddms.xml");
        Metacard metacard = inputTransformer.transform(input);

        LOGGER.info("Attributes: ");
        for (AttributeDescriptor descriptor : metacard.getMetacardType()
                .getAttributeDescriptors()) {
            Attribute attribute = metacard.getAttribute(descriptor.getName());
            LOGGER.info("\t" + descriptor.getName() + ": "
                    + ((attribute == null) ? attribute : attribute.getValue()));
        }

        BinaryContent output = outputTransformer.transform(metacard, null);
        String outputString = new String(output.getByteArray());

        // TODO test equivalence with XMLUnit.
        LOGGER.info(outputString);
    }

}
