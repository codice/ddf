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

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.XMLUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class XsltMetacardTransformer extends AbstractXsltTransformer
    implements MetacardTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltMetacardTransformer.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private Map<String, Object> localMap = new ConcurrentHashMap<String, Object>();

  public XsltMetacardTransformer() {}

  public XsltMetacardTransformer(Bundle bundle, String xslFile) {
    super(bundle, xslFile);
  }

  private String getValueOrEmptyString(Object value) {
    return (value == null) ? "" : value.toString();
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    LOGGER.debug("Entering metacard xslt transform.");

    Transformer transformer;
    Map<String, Object> mergedMap = new HashMap<String, Object>(localMap);

    if (arguments != null) {
      mergedMap.putAll(arguments);
    }

    // adding metacard data not in document
    mergedMap.put("id", getValueOrEmptyString(metacard.getId()));
    mergedMap.put("siteName", getValueOrEmptyString(metacard.getSourceId()));
    mergedMap.put("title", getValueOrEmptyString(metacard.getTitle()));
    mergedMap.put("type", getValueOrEmptyString(metacard.getMetacardType()));
    mergedMap.put("date", getValueOrEmptyString(metacard.getAttribute(Core.CREATED).getValue()));
    mergedMap.put("product", getValueOrEmptyString(metacard.getResourceURI()));
    mergedMap.put("thumbnail", getValueOrEmptyString(metacard.getThumbnail()));
    mergedMap.put("geometry", getValueOrEmptyString(metacard.getLocation()));

    ServiceReference[] refs = null;
    try {
      LOGGER.debug("Searching for other Metacard Transformers.");
      // TODO INJECT THESE!!!
      refs = context.getServiceReferences(MetacardTransformer.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      // can't happen because filter is null
    }

    if (refs != null) {
      List<String> serviceList = new ArrayList<String>();
      LOGGER.debug("Found other Metacard transformers, adding them to a service reference list.");
      for (ServiceReference ref : refs) {
        if (ref != null) {
          String title = null;
          String shortName = (String) ref.getProperty(Constants.SERVICE_SHORTNAME);

          if ((title = (String) ref.getProperty(Constants.SERVICE_TITLE)) == null) {
            title = "View as " + shortName.toUpperCase();
          }

          String url = "/services/catalog/" + metacard.getId() + "?transform=" + shortName;

          // define the services
          serviceList.add(title);
          serviceList.add(url);
        }
      }
      mergedMap.put("services", serviceList);
    } else {
      LOGGER.debug("No other Metacard transformers were found.");
    }

    // TODO: maybe add updated, type, and uuid here?
    // map.put("updated", fmt.print(result.getPostedDate().getTime()));
    // map.put("type", card.getSingleType().getValue());

    BinaryContent resultContent;
    StreamResult resultOutput = null;
    XMLReader xmlReader = null;
    try {
      XMLReader xmlParser = XML_UTILS.getSecureXmlParser();
      xmlReader = new XMLFilterImpl(xmlParser);
    } catch (SAXException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    Source source =
        new SAXSource(xmlReader, new InputSource(new StringReader(metacard.getMetadata())));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    resultOutput = new StreamResult(baos);

    try {
      transformer = templates.newTransformer();
    } catch (TransformerConfigurationException tce) {
      throw new CatalogTransformerException(
          "Could not perform Xslt transform: " + tce.getException(), tce.getCause());
    }

    if (!mergedMap.isEmpty()) {
      for (Map.Entry<String, Object> entry : mergedMap.entrySet()) {
        LOGGER.debug("Adding parameter to transform {}:{}", entry.getKey(), entry.getValue());
        transformer.setParameter(entry.getKey(), entry.getValue());
      }
    }

    try {
      transformer.transform(source, resultOutput);
      byte[] bytes = baos.toByteArray();
      IOUtils.closeQuietly(baos);
      LOGGER.debug("Transform complete.");
      resultContent = new XsltTransformedContent(bytes, mimeType);
    } catch (TransformerException te) {
      throw new CatalogTransformerException(
          "Could not perform Xslt transform: " + te.getMessage(), te.getCause());
    } finally {
      // TODO: if we ever start to reuse transformers, we should add this
      // code back in
      // transformer.reset();
    }

    return resultContent;
  }

  /**
   * Gets the dataItemStatus value.
   *
   * @return String representation of the dataItemStatus value (defaults to "Unknown")
   */
  public String getDataItemStatus() {
    return localMap.get("dataItemStatus").toString();
  }

  /**
   * Sets the dataItemStatus value that is passed to the translation and put in the dataItemStatus
   * element.
   *
   * @param dataItemStatus
   */
  public void setDataItemStatus(String dataItemStatus) {
    localMap.put("dataItemStatus", dataItemStatus);
  }
}
