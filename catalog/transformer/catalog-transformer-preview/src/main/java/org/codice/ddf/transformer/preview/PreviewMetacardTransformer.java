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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
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

  private List<String> previewElements;

  private List<XPathExpression> xPathExpressions;

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
    } else if (previewFromMetadata
        && xPathExpressions != null
        && StringUtils.isNotEmpty(metacard.getMetadata())) {
      String text =
          xPathExpressions
              .stream()
              .map(x -> evaluateXPath(metacard.getMetadata(), x))
              .filter(Objects::nonNull)
              .findFirst()
              .orElse("");
      if (StringUtils.isNotEmpty(text)) {
        preview = StringEscapeUtils.escapeHtml4(text).replaceAll("[\n|\r]", "<br>");
      }
    }

    preview = String.format("<head><meta charset=\"utf-8\"/>%s</head>", preview);

    return new BinaryContentImpl(IOUtils.toInputStream(preview));
  }

  private String evaluateXPath(String metadata, XPathExpression xPathExpression) {
    StringReader metadataReader = new StringReader(metadata);
    InputSource inputXml = new InputSource(metadataReader);

    String text = null;
    try {
      Node result = (Node) xPathExpression.evaluate(inputXml, XPathConstants.NODE);
      if (result != null && result.getTextContent() != null) {
        text = result.getTextContent().trim();
      }
    } catch (XPathExpressionException e) {
      LOGGER.error("Error evaluating xpath");
    }

    return text;
  }

  private List<XPathExpression> createXPathExpressions(List<String> previewElements) {
    XPathFactory xPathFactory = XPathFactory.newInstance();
    List<XPathExpression> xPathExpressions = new ArrayList<>();

    String xPathString = "//*[name()='%s']";

    try {
      for (String element : previewElements) {
        xPathExpressions.add(xPathFactory.newXPath().compile(String.format(xPathString, element)));
      }
    } catch (XPathExpressionException e) {
      LOGGER.error("Error compiling xpath", e);
    }
    return xPathExpressions;
  }

  public Boolean getPreviewFromMetadata() {
    return previewFromMetadata;
  }

  public void setPreviewFromMetadata(Boolean previewFromMetadata) {
    this.previewFromMetadata = previewFromMetadata;
  }

  public List<String> getPreviewElements() {
    return previewElements;
  }

  public void setPreviewElements(List<String> previewElements) {
    this.previewElements = previewElements;
    this.xPathExpressions = createXPathExpressions(previewElements);
  }
}
