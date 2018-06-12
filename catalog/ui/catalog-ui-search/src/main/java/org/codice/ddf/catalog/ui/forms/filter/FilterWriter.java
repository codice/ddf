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
package org.codice.ddf.catalog.ui.forms.filter;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;
import net.opengis.filter.v_2_0.FilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;

@SuppressWarnings("squid:S1075" /* Will parameterize only if necessary. */)
public class FilterWriter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterWriter.class);

  private static final String FILTER_XSD_RESOURCE_PATH = "/schemas/filter.xsd";

  private static final String SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

  private static final String HTTP = "http";

  private final Marshaller marshaller;

  /**
   * Create a {@link FilterWriter}.
   *
   * <p>Accessing {@link javax.xml.XMLConstants#W3C_XML_SCHEMA_NS_URI} through the constants class
   * causes an error: {@code javax.xml.XMLConstants cannot be found by catalog-ui-search}.
   * Workaround is to use {@link #SCHEMA_LANGUAGE} constant instead.
   *
   * <p>See <a
   * href="https://docs.oracle.com/javase/8/docs/api/javax/xml/validation/SchemaFactory.html">
   * SchemaFactory</a> for more information.
   *
   * <p>See <a href="http://schemas.opengis.net/filter/2.0/">Filter 2.0</a> for original OGC schema
   * documents.
   *
   * <p>To permit initialization and installation of this system without an internet connection, the
   * XML schemas have been cached as bundle resources. The root schema document is being loaded
   * directly using standard Java resource loading techniques. This works fine for schema documents
   * within the same namespace being linked using the {@code <include>} directive, but <b>not</b>
   * for linking a separate namespace using the {@code <import>} directive. To support that without
   * modifying the XSD files, a custom {@link org.w3c.dom.ls.LSResourceResolver} is being used.
   *
   * <p>The {@code systemId} is the URI of the resource about to be loaded. The {@code baseURI} is
   * the URI of the resource currently being processed, which references the {@code systemId}. By
   * returning {@code null} in the resource resolver the {@code systemId} resource can be loaded in
   * the default way without any custom handling. This works for {@code <include>} cases because the
   * filter namespace documents live on the same context path, so the {@code bundleresource:<path>}
   * URI that was constructed by OSGi via traditional resource loading can be reused. Instances of
   * {@code systemId} will just be a simple file name in these cases, i.e. {@code sort.xsd}, without
   * any qualifying information.
   *
   * <p>For {@code <import>} cases, and bringing in documents under a separate namespace, it cannot
   * be assumed their context paths are similar. In that regard, {@code systemId}s will be fully
   * qualified HTTP URLs and returning {@code null} will invoke default behavior that performs a
   * network request to the open internet. Instead, the resource resolver will parse the document's
   * name and redirect the network request by overwriting the {@code systemId} URI to point to the
   * local version of the XSD.
   *
   * @param validationEnabled true if all XML writing done by this {@link FilterWriter} should also
   *     be validated against the <a href="http://schemas.opengis.net/filter/2.0/filter.xsd">filter
   *     schema</a>. False otherwise.
   * @throws JAXBException if a problem occurs setting up and configuring JAXB.
   * @see #FILTER_XSD_RESOURCE_PATH for how the root schema document is being loaded.
   */
  public FilterWriter(boolean validationEnabled) throws JAXBException {
    this.marshaller = JAXBContext.newInstance(FilterType.class).createMarshaller();
    if (validationEnabled) {
      LOGGER.info("Loading filter schemas");
      URL schemaLocation = FilterWriter.class.getResource(FILTER_XSD_RESOURCE_PATH);
      SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
      schemaFactory.setResourceResolver(
          (type, namespaceURI, publicId, systemId, baseURI) -> {
            if (!SCHEMA_LANGUAGE.equals(type)) {
              return null;
            }
            URI remoteSchema = URI.create(systemId);
            String protocol = remoteSchema.getScheme();
            if (!HTTP.equals(protocol)) {
              return null;
            }
            String path = remoteSchema.getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            URL remoteSchemaLocal = FilterWriter.class.getResource("/schemas/" + filename);
            return new LSInputImpl(publicId, remoteSchemaLocal.toString());
          });
      try {
        marshaller.setSchema(schemaFactory.newSchema(schemaLocation));
      } catch (SAXException e) {
        throw new JAXBException("Error reading filter schema", e);
      }
    }
  }

  public String marshal(JAXBElement element) throws JAXBException {
    StringWriter writer = new StringWriter();
    marshaller.marshal(element, writer);
    return writer.toString();
  }

  @SuppressWarnings("squid:S1186" /* Minimum impl req's met to rewrite the systemId URI */)
  private static class LSInputImpl implements LSInput {
    private String publicId;
    private String systemId;

    LSInputImpl(String publicId, String systemId) {
      this.publicId = publicId;
      this.systemId = systemId;
    }

    @Override
    public String getPublicId() {
      return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
      this.publicId = publicId;
    }

    @Override
    public String getSystemId() {
      return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
      this.systemId = systemId;
    }

    /* --------------------------------------------------------------------------- */
    /* The below methods are not necessary for merely resending a new systemId URI */

    @Override
    public Reader getCharacterStream() {
      return null;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {}

    @Override
    public InputStream getByteStream() {
      return null;
    }

    @Override
    public void setByteStream(InputStream byteStream) {}

    @Override
    public String getStringData() {
      return null;
    }

    @Override
    public void setStringData(String stringData) {}

    @Override
    public String getBaseURI() {
      return null;
    }

    @Override
    public void setBaseURI(String baseURI) {}

    @Override
    public String getEncoding() {
      return null;
    }

    @Override
    public void setEncoding(String encoding) {}

    @Override
    public boolean getCertifiedText() {
      return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {}
  }
}
