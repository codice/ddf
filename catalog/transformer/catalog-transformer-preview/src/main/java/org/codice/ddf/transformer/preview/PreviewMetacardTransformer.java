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

  private List<XPathExpression> elementXPathExpressions;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PreviewMetacardTransformer.class.getName());

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Cannot transform null metacard.");
    }

    String preview = "No preview text available.";
    String text = "";

    if (metacard.getAttribute(Extracted.EXTRACTED_TEXT) != null
        && metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue() != null) {

      text = metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString();
    } else if (previewFromMetadata
        && elementXPathExpressions != null
        && StringUtils.isNotEmpty(metacard.getMetadata())) {

      text = getPreviewTextFromMetadata(metacard);
    }

    if (StringUtils.isNotEmpty(text)) {
      preview = StringEscapeUtils.escapeHtml4(text).replaceAll("[\n|\r]", "<br>");
    }

    preview = String.format("<head><meta charset=\"utf-8\"/>%s</head>", preview);

    return new BinaryContentImpl(IOUtils.toInputStream(preview));
  }

  private String getPreviewTextFromMetadata(Metacard metacard) {
    return elementXPathExpressions
                    .stream()
                    .map(x -> evaluateXPath(metacard.getMetadata(), x))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("");
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

  private XPathExpression matchElementExpressionIgnoreNamespace(XPathFactory xpf, String element) {
    XPathExpression expression = null;

    try {
      String matchElementXPath = String.format("//*[name()='%s']", element);
      expression = xpf.newXPath().compile(matchElementXPath);
    } catch (XPathExpressionException e) {
      LOGGER.debug("Cannot compile xpath", e);
    }

    return expression;
  }

  private List<XPathExpression> createElementXPathExpressions(List<String> previewElements) {
    XPathFactory xPathFactory = XPathFactory.newInstance();
    List<XPathExpression> expressions = new ArrayList<>();

    for (String element : previewElements) {
      expressions.add(matchElementExpressionIgnoreNamespace(xPathFactory, element));
    }

    return expressions;
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
    this.elementXPathExpressions = createElementXPathExpressions(previewElements);
  }
}
