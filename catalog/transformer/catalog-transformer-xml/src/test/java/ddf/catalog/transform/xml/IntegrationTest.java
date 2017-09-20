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
package ddf.catalog.transform.xml;

import static org.mockito.Mockito.mock;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.xml.MetacardMarshallerImpl;
import ddf.catalog.transformer.xml.PrintWriterProviderImpl;
import ddf.catalog.transformer.xml.XmlInputTransformer;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

  private Map<String, Serializable> mockArguments = mock(Map.class);

  @Test
  public void testInputAndOutput() throws CatalogTransformerException, IOException {
    Parser parser = new XmlParser();

    InputTransformer inputTransformer = new XmlInputTransformer(parser);

    MetacardMarshaller metacardMarshaller =
        new MetacardMarshallerImpl(parser, new PrintWriterProviderImpl());
    MetacardTransformer outputTransformer = new XmlMetacardTransformer(metacardMarshaller);

    InputStream input = getClass().getResourceAsStream("/extensibleMetacard.xml");
    Metacard metacard = inputTransformer.transform(input);

    LOGGER.info("Attributes: ");
    for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      LOGGER.info(
          "\t"
              + descriptor.getName()
              + ": "
              + ((attribute == null) ? attribute : attribute.getValue()));
    }

    BinaryContent output = outputTransformer.transform(metacard, mockArguments);
    String outputString = new String(output.getByteArray());

    // TODO test equivalence with XMLUnit.
    LOGGER.info(outputString);
  }
}
