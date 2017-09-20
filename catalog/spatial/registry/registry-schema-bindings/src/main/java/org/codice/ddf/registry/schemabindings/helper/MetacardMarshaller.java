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
package org.codice.ddf.registry.schemabindings.helper;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.schemabindings.EbrimConstants;

public class MetacardMarshaller {
  private Parser parser;

  private ParserConfigurator unmarshalConfigurator;

  private ParserConfigurator marshalConfigurator;

  public MetacardMarshaller(Parser parser) {
    setParser(parser);
  }

  /**
   * Converts the metacards metadata into a RegistryPackageType object
   *
   * @param mcard A metacard with ebrim metadata
   * @return A RegistryPackageType object created from the ebrim metadata
   * @throws ParserException
   */
  public RegistryPackageType getRegistryPackageFromMetacard(Metacard mcard) throws ParserException {

    JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement;
    String metadata = mcard.getMetadata();
    try (InputStream inputStream =
        new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8))) {
      registryObjectTypeJAXBElement =
          parser.unmarshal(unmarshalConfigurator, JAXBElement.class, inputStream);
    } catch (IOException e) {
      throw new ParserException("Error parsing metacards xml as ebrim", e);
    }

    if (registryObjectTypeJAXBElement == null) {
      throw new ParserException("Error parsing metacards xml as ebrim");
    }
    RegistryPackageType registryPackage =
        (RegistryPackageType) registryObjectTypeJAXBElement.getValue();
    if (registryPackage == null) {
      throw new ParserException("Error parsing metacards xml as ebrim. No value");
    }
    return registryPackage;
  }

  /**
   * Converts the RegistryPackageType into an xml string
   *
   * @param registryPackage Registry package to convert
   * @return Ebrim xml string
   * @throws ParserException
   */
  public String getRegistryPackageAsXml(RegistryPackageType registryPackage)
      throws ParserException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      JAXBElement<RegistryPackageType> registryObjectTypeJAXBElement =
          EbrimConstants.RIM_FACTORY.createRegistryPackage(registryPackage);

      parser.marshal(marshalConfigurator, registryObjectTypeJAXBElement, outputStream);
      return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ParserException("Error parsing registry package to ebrim xml", e);
    }
  }

  /**
   * Turns the passed in registryPackage into an InputStream of xml
   *
   * @param registryPackage RegistryPackageType to create the input stream from
   * @return An InputStream with the xml content of the RegistryPackageTypes
   * @throws ParserException
   */
  public InputStream getRegistryPackageAsInputStream(RegistryPackageType registryPackage)
      throws ParserException {
    return new ByteArrayInputStream(
        getRegistryPackageAsXml(registryPackage).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Converts the registryPackage into xml and puts it in the metacards metadata field
   *
   * @param metacard Metacards to put ebrim xml into
   * @param registryPackage RegistryPackage to put into metacard
   * @throws ParserException
   */
  public void setMetacardRegistryPackage(Metacard metacard, RegistryPackageType registryPackage)
      throws ParserException {
    metacard.setAttribute(
        new AttributeImpl(Metacard.METADATA, getRegistryPackageAsXml(registryPackage)));
  }

  private void setParser(Parser parser) {
    List<String> contextPath =
        Arrays.asList(
            RegistryObjectType.class.getPackage().getName(),
            EbrimConstants.OGC_FACTORY.getClass().getPackage().getName(),
            EbrimConstants.GML_FACTORY.getClass().getPackage().getName());
    ClassLoader classLoader = this.getClass().getClassLoader();

    this.unmarshalConfigurator = parser.configureParser(contextPath, classLoader);
    this.marshalConfigurator = parser.configureParser(contextPath, classLoader);
    this.marshalConfigurator.addProperty(Marshaller.JAXB_FRAGMENT, true);
    this.marshalConfigurator.addProperty(
        "com.sun.xml.bind.namespacePrefixMapper", new RegistryNamespacePrefixMapper());
    this.parser = parser;
  }
}
