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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.util.XPathHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.codice.ddf.platform.util.XMLUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class XsltResponseQueueTransformer extends AbstractXsltTransformer
    implements QueryResponseTransformer {

  private static final String GRAND_TOTAL = "grandTotal";

  // private static final String XML_RESULTS_NAMESPACE =
  // "http://ddf/xslt-response-queue-transformer";
  private static final String XML_RESULTS_NAMESPACE = null;

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltResponseQueueTransformer.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  public XsltResponseQueueTransformer() {}

  public XsltResponseQueueTransformer(Bundle bundle, String xslFile) {
    super(bundle, xslFile);
  }

  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    LOGGER.debug("Transforming ResponseQueue with XSLT tranformer");

    long grandTotal = -1;

    try {
      DocumentBuilder builder = XML_UTILS.getSecureDocumentBuilder(true);

      Document doc = builder.newDocument();

      Node resultsElement =
          doc.appendChild(createElement(doc, XML_RESULTS_NAMESPACE, "results", null));

      // TODO use streaming XSLT, not DOM
      List<Result> results = upstreamResponse.getResults();
      grandTotal = upstreamResponse.getHits();

      for (Result result : results) {
        Metacard metacard = result.getMetacard();
        if (metacard != null) {

          String metadata = metacard.getMetadata();
          if (metadata != null) {
            Element metacardElement = createElement(doc, XML_RESULTS_NAMESPACE, "metacard", null);
            if (metacard.getId() != null) {
              metacardElement.appendChild(
                  createElement(doc, XML_RESULTS_NAMESPACE, "id", metacard.getId()));
            }
            if (metacard.getMetacardType().toString() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc, XML_RESULTS_NAMESPACE, "type", metacard.getMetacardType().getName()));
            }
            if (metacard.getTitle() != null) {
              metacardElement.appendChild(
                  createElement(doc, XML_RESULTS_NAMESPACE, "title", metacard.getTitle()));
            }
            if (result.getRelevanceScore() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc, XML_RESULTS_NAMESPACE, "score", result.getRelevanceScore().toString()));
            }
            if (result.getDistanceInMeters() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc,
                      XML_RESULTS_NAMESPACE,
                      "distance",
                      result.getDistanceInMeters().toString()));
            }
            if (metacard.getSourceId() != null) {
              metacardElement.appendChild(
                  createElement(doc, XML_RESULTS_NAMESPACE, "site", metacard.getSourceId()));
            }
            if (metacard.getContentTypeName() != null) {
              String contentType = metacard.getContentTypeName();
              Element typeElement =
                  createElement(doc, XML_RESULTS_NAMESPACE, "content-type", contentType);
              // TODO revisit what to put in the qualifier
              typeElement.setAttribute("qualifier", "content-type");
              metacardElement.appendChild(typeElement);
            }
            if (metacard.getResourceURI() != null) {
              try {
                metacardElement.appendChild(
                    createElement(
                        doc,
                        XML_RESULTS_NAMESPACE,
                        "product",
                        metacard.getResourceURI().toString()));
              } catch (DOMException e) {
                LOGGER.debug(" Unable to create resource uri element", e);
              }
            }
            if (metacard.getThumbnail() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc,
                      XML_RESULTS_NAMESPACE,
                      "thumbnail",
                      Base64.getEncoder().encodeToString(metacard.getThumbnail())));
              try {
                String mimeType =
                    URLConnection.guessContentTypeFromStream(
                        new ByteArrayInputStream(metacard.getThumbnail()));
                metacardElement.appendChild(
                    createElement(doc, XML_RESULTS_NAMESPACE, "t_mimetype", mimeType));
              } catch (IOException e) {
                metacardElement.appendChild(
                    createElement(doc, XML_RESULTS_NAMESPACE, "t_mimetype", "image/png"));
              }
            }
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            if (metacard.getAttribute(Core.CREATED).getValue() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc,
                      XML_RESULTS_NAMESPACE,
                      "created",
                      fmt.print(
                          ((Date) metacard.getAttribute(Core.CREATED).getValue()).getTime())));
            }
            // looking at the date last modified
            if (metacard.getAttribute(Core.MODIFIED).getValue() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc,
                      XML_RESULTS_NAMESPACE,
                      "updated",
                      fmt.print(
                          ((Date) metacard.getAttribute(Core.MODIFIED).getValue()).getTime())));
            }
            if (metacard.getEffectiveDate() != null) {
              metacardElement.appendChild(
                  createElement(
                      doc,
                      XML_RESULTS_NAMESPACE,
                      "effective",
                      fmt.print(metacard.getEffectiveDate().getTime())));
            }
            if (metacard.getLocation() != null) {
              metacardElement.appendChild(
                  createElement(doc, XML_RESULTS_NAMESPACE, "location", metacard.getLocation()));
            }
            Element documentElement = doc.createElementNS(XML_RESULTS_NAMESPACE, "document");
            metacardElement.appendChild(documentElement);
            resultsElement.appendChild(metacardElement);

            Node importedNode =
                doc.importNode(new XPathHelper(metadata).getDocument().getFirstChild(), true);
            documentElement.appendChild(importedNode);
          } else {
            LOGGER.debug("Null content/document returned to XSLT ResponseQueueTransformer");
          }
        }
      }

      if (LOGGER.isDebugEnabled()) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        LOGGER.debug("Generated XML input for transform: " + lsSerializer.writeToString(doc));
      }

      LOGGER.debug("Starting responsequeue xslt transform.");

      Transformer transformer;

      Map<String, Object> mergedMap = new HashMap<String, Object>();
      mergedMap.put(GRAND_TOTAL, grandTotal);
      if (arguments != null) {
        mergedMap.putAll(arguments);
      }

      BinaryContent resultContent;
      StreamResult resultOutput = null;
      Source source = new DOMSource(doc);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      resultOutput = new StreamResult(baos);

      try {
        transformer = templates.newTransformer();
      } catch (TransformerConfigurationException tce) {
        throw new CatalogTransformerException("Could not perform Xslt transform: ", tce);
      }

      for (Map.Entry<String, Object> entry : mergedMap.entrySet()) {
        LOGGER.trace("Adding parameter to transform {{}:{}}", entry.getKey(), entry.getValue());
        transformer.setParameter(entry.getKey(), entry.getValue());
      }

      try {
        transformer.transform(source, resultOutput);
        byte[] bytes = baos.toByteArray();
        LOGGER.debug("Transform complete.");
        resultContent = new XsltTransformedContent(bytes, mimeType);
      } catch (TransformerException te) {
        LOGGER.debug("Could not perform Xslt transform: ", te);
        throw new CatalogTransformerException("Could not perform Xslt transform: ", te);
      }

      return resultContent;
    } catch (ParserConfigurationException e) {
      LOGGER.debug("Error creating new document: ", e);
      throw new CatalogTransformerException("Error merging entries to xml feed.", e);
    }
  }

  private Element createElement(
      Document parentDoc, String namespace, String tagName, String elementValue) {
    Element element = parentDoc.createElementNS(namespace, tagName);
    element.setTextContent(elementValue);
    return element;
  }
}
