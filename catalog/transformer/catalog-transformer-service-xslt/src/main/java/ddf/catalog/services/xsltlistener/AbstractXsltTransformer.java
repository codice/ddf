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
package ddf.catalog.services.xsltlistener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.codice.ddf.platform.util.XMLUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public abstract class AbstractXsltTransformer {

  protected static final MimeType DEFAULT_MIME_TYPE; // "text/plain"

  protected static final String MIME_TYPE_HEADER_NAME = "DDF-Mime-Type";

  static {
    MimeType unknownMime = null;
    try {
      unknownMime = new MimeType("application/octet-stream");
    } catch (MimeTypeParseException e) {
      throw new IllegalStateException("Unable to create MimeType");
    }
    DEFAULT_MIME_TYPE = unknownMime;
  }

  protected Templates templates;

  protected MimeType mimeType;

  protected BundleContext context;

  public AbstractXsltTransformer() {}

  public AbstractXsltTransformer(Bundle bundle, String xslFile) {
    init(bundle, xslFile);
  }

  /**
   * Retrieves the xslt file from the incoming bundle. Also gets the mime type from the bundle
   * header.
   *
   * @param bundle Incoming bundle that contains an xsl file
   * @param xslFile The xsl file name
   */
  public void init(final Bundle bundle, String xslFile) {
    context = bundle.getBundleContext();
    URL xsltUrl = bundle.getResource(xslFile);
    String type = bundle.getHeaders().get(MIME_TYPE_HEADER_NAME);
    try {
      init(type, xsltUrl.openStream());
    } catch (IOException | TransformerConfigurationException e) {
      throw new IllegalStateException(
          String.format("Could not load xsl file (%s) from system: %s", xslFile, e.getMessage()),
          e.getCause());
    }
  }

  /**
   * Sets the templates and mimeType used to perform a transform. This method can also be used in a
   * non-OSGi environment to setup the transformer. <b>NOTE:</b> When using in a non-OSGi
   * environment, some transformers may not work.
   *
   * @param mimeString String value of the mimeType to be returned.
   * @param xslStream Full, absolute path of the xsl file.
   */
  public void init(String mimeString, InputStream xslStream)
      throws TransformerConfigurationException {

    TransformerFactory tf = XMLUtils.getInstance().getSecureXmlTransformerFactory();
    Source xsltSource;
    xsltSource = new StreamSource(xslStream);
    try {
      templates = tf.newTemplates(xsltSource);
    } catch (TransformerConfigurationException tce) {
      throw new TransformerConfigurationException(
          "Could not create new templates for XsltTransformer ( "
              + xslStream
              + ") : "
              + tce.getException(),
          tce);
    }
    if (mimeString == null) {
      this.mimeType = DEFAULT_MIME_TYPE;
    } else {
      try {
        this.mimeType = new MimeType(mimeString);
      } catch (MimeTypeParseException e) {
        this.mimeType = DEFAULT_MIME_TYPE;
      }
    }
  }
}
