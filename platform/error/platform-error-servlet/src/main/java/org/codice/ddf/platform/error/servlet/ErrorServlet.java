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
package org.codice.ddf.platform.error.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.error.handler.ErrorHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorServlet extends HttpServlet {

  public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

  public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

  public static final String ERROR_MESSAGE = "javax.servlet.error.message";

  public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

  public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

  public static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

  private static final Logger LOGGER = LoggerFactory.getLogger(ERROR_SERVLET_NAME);

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint. */)
  private ErrorHandler errorHandler;

  @Override
  public void init() {
    setErrorHandler();
  }

  private void setErrorHandler() {
    if (errorHandler == null) {
      Bundle bundle = FrameworkUtil.getBundle(ErrorServlet.class);
      if (bundle != null) {
        BundleContext bundleContext = bundle.getBundleContext();
        if (bundleContext != null) {
          ServiceReference<ErrorHandler> serviceReference =
              bundleContext.getServiceReference(ErrorHandler.class);
          if (serviceReference != null) {
            errorHandler = bundleContext.getService(serviceReference);
          }
        }
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    handleError(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    handleError(request, response);
  }

  private void handleError(HttpServletRequest request, HttpServletResponse response) {
    Object codeObj = request.getAttribute(ERROR_STATUS_CODE);
    Object messageObj = request.getAttribute(ERROR_MESSAGE);
    Object typeObj = request.getAttribute(ERROR_EXCEPTION_TYPE);
    Throwable throwable = (Throwable) request.getAttribute(ERROR_EXCEPTION);
    String uri = (String) request.getAttribute(ERROR_REQUEST_URI);
    String code = "0";
    if (codeObj != null) {
      code = codeObj.toString();
    }
    String type = "";
    if (typeObj != null) {
      type = typeObj.toString();
    }
    String message = "";
    if (messageObj != null) {
      message = messageObj.toString();
    }

    setErrorHandler();

    if (errorHandler != null) {
      int codeInt;

      try {
        codeInt = Integer.parseInt(code);
      } catch (NumberFormatException e) {
        codeInt = 500;
      }

      errorHandler.handleError(codeInt, message, type, throwable, uri, request, response);
    } else {
      org.eclipse.jetty.server.handler.ErrorHandler jettyErrorHandler =
          new org.eclipse.jetty.server.handler.ErrorHandler();
      try {
        jettyErrorHandler.handle(
            request.getRequestURI(), (org.eclipse.jetty.server.Request) request, request, response);
      } catch (IOException e) {
        LOGGER.warn("Problem handling Jetty Error due to: ", e);
      }
    }
  }
}
