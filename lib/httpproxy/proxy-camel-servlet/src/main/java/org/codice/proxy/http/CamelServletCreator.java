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
package org.codice.proxy.http;

import java.util.Dictionary;
import org.apache.camel.CamelContext;
import org.codice.ddf.configuration.DictionaryMap;
import org.osgi.framework.BundleContext;

/**
 * Creates a registered Camel Servlet
 *
 * @author ddf
 */
public class CamelServletCreator {
  public static final String SERVLET_PATH = "/proxy";

  public static final String SERVLET_NAME = "CamelServlet";

  private final CamelContext camelContext;

  BundleContext bundleContext = null;

  public CamelServletCreator(BundleContext bundleContext, CamelContext camelContext) {
    this.bundleContext = bundleContext;
    this.camelContext = camelContext;
  }

  /** Register the Camel Servlet with the HTTP Service */
  public void registerServlet() {

    Dictionary<String, Object> props = new DictionaryMap<>();
    props.put("alias", SERVLET_PATH);
    props.put("servlet-name", SERVLET_NAME);
    bundleContext.registerService(
        "javax.servlet.Servlet", new HttpProxyCamelHttpTransportServlet(camelContext), props);
  }
}
