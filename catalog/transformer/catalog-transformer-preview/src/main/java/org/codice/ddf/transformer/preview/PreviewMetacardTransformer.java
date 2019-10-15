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
package org.codice.ddf.transformer.preview;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tika.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class PreviewMetacardTransformer implements MetacardTransformer {

  private Boolean previewFromMetadata = false;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PreviewMetacardTransformer.class.getName());

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Cannot transform null metacard.");
    }

    String preview = "No preview text available.";

    if (metacard.getAttribute(Extracted.EXTRACTED_TEXT) != null
        && metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue() != null) {
      preview =
          StringEscapeUtils.escapeHtml4(
                  metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString())
              .replaceAll("[\n|\r]", "<br>");
    } else if (previewFromMetadata && StringUtils.isNotEmpty(metacard.getMetadata())) {
      preview =
          StringEscapeUtils.escapeHtml4(selectPreviewFromMetadata(metacard.getMetadata()))
              .replaceAll("[\n|\r]", "<br>");
    }

    //    if (preview.equals("")
    //        && (boolean) metacard.getAttribute("internal.local-resource").getValue()) {
    //      preview =
    //          String.format(
    //              "<img src=\"%s\" alt=\"preview-img\">",
    //              metacard.getAttribute("resource-download-url").getValue());
    //    }

    if (StringUtils.isNotEmpty(preview)) {
      preview = String.format("<head><meta charset=\"utf-8\"/>%s</head>", preview);
    }

    return new BinaryContentImpl(IOUtils.toInputStream(preview));
  }

  private String selectPreviewFromMetadata(String metadata) {
    // temp set
    Set<String> textAttributes = new HashSet<>();
    textAttributes.add("text");
    textAttributes.add("TEXT");

    XPathFactory xPathFactory;
    String xPathString = "//*[name()='%s']";
    String text = "";

    for (String attributeName : textAttributes) {
      xPathFactory = XPathFactory.newInstance();
      try {
        String xpath = String.format(xPathString, attributeName);

        StringReader metadataReader = new StringReader(metadata);
        InputSource inputXml = new InputSource(metadataReader);

        Node result =
            (Node) xPathFactory.newXPath().compile(xpath).evaluate(inputXml, XPathConstants.NODE);
        text = result.getTextContent().trim();

        if (StringUtils.isNotEmpty(text)) {
          break;
        }
      } catch (XPathExpressionException | NullPointerException e) {
        LOGGER.warn("Could not find attribute in xPath: {}", xPathFactory);
      }
    }

    return text;
  }

  public Boolean getPreviewFromMetadata() {
    return previewFromMetadata;
  }

  public void setPreviewFromMetadata(Boolean previewFromMetadata) {
    this.previewFromMetadata = previewFromMetadata;
  }
}
