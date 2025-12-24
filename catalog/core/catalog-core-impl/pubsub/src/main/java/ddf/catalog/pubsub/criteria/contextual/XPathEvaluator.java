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
package ddf.catalog.pubsub.criteria.contextual;

import ddf.util.XPathHelper;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XPathEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(XPathEvaluator.class);

  private XPathEvaluator() {}

  public static boolean evaluate(XPathEvaluationCriteria xpathCriteria) {
    Document document = xpathCriteria.getDocument();
    String xpath = xpathCriteria.getXPath();

    try {
      XPathHelper evaluator = new XPathHelper(document);
      return (Boolean) evaluator.evaluate(xpath, XPathConstants.BOOLEAN);

    } catch (IOException
        | ParserConfigurationException
        | SAXException
        | XPathExpressionException e) {
      LOGGER.debug("Unable to evaluate xpath", e);
    }

    return false;
  }
}
