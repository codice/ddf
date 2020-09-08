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
package ddf.util;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * This object is used to store compiled {@link XPathExpression} objects for the intention of
 * gaining performance. It retains a single {@link XPathFactory} object and a single {@link XPath}
 * object for the purpose of doing the compilation. As of now it uses a {@link HashMap} to store the
 * compiled {@link XPathExpression} objects.
 *
 * @author Ashraf Barakat
 * @since 1.0.4
 */
public class XPathCache {

  private static final XPathFactory XPF = XPathFactory.newInstance();

  private static final XPath XPATH = XPF.newXPath();

  private static Map<String, XPathExpression> expressionMap =
      new HashMap<String, XPathExpression>();

  private static NamespaceContext namespaceResolver; // = new NamespaceResolver() ;

  private XPathCache() {}

  public static XPath getXPath() {
    return XPATH;
  }

  public static XPathExpression getCompiledExpression(String xpathExpressionkey)
      throws XPathExpressionException, NullPointerException {

    // go to cache, check if we have the compiled expression

    XPathExpression compiledExpression = expressionMap.get(xpathExpressionkey);

    if (compiledExpression == null) {

      // must compile new expression and place in the map

      compiledExpression = XPATH.compile(xpathExpressionkey);

      expressionMap.put(xpathExpressionkey, compiledExpression);
    }

    return compiledExpression;
  }

  public static NamespaceContext getNamespaceResolver() {
    if (namespaceResolver == null) {
      namespaceResolver = new NamespaceResolver();
    }
    return namespaceResolver;
  }

  public static void setNamespaceResolver(NamespaceResolver nr) {
    namespaceResolver = nr;
  }
}
