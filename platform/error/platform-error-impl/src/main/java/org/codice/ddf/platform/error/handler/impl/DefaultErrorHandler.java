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
package org.codice.ddf.platform.error.handler.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.error.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorHandler implements ErrorHandler {

  private static final String SERVER_ERROR_PLEASE_SEE_LOGS = "Server Error, please see logs.";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorHandler.class);

  private static final int BUFFER_SIZE = 4096;

  private String errorHtml = SERVER_ERROR_PLEASE_SEE_LOGS;

  private Properties codeMessageProperties = new Properties();

  public DefaultErrorHandler() {
    try (InputStream inputStream = readResource("/codeMessages.properties")) {
      codeMessageProperties.load(inputStream);
    } catch (IOException e) {
      LOGGER.error("Unable to load codeMessages properties", e);
    }
  }

  @Override
  public void handleError(
      int code,
      String message,
      String type,
      Throwable throwable,
      String uri,
      HttpServletRequest request,
      HttpServletResponse response) {
    initErrorHtml();

    Map<String, String> attributesMap = new HashMap<>();
    attributesMap.put("code", String.valueOf(code));
    attributesMap.put("message", message);
    attributesMap.put("uri", uri);

    response.setStatus(code);
    response.setContentType("text/html");

    String output =
        errorHtml.replace(
            "{{codeMessage}}", codeMessageProperties.getProperty(String.valueOf(code)));

    try (ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(BUFFER_SIZE)) {
      for (Entry<String, String> row : attributesMap.entrySet()) {
        output = output.replace("{{" + row.getKey() + "}}", row.getValue());
      }
      writer.write(output);
      writer.flush();
      writer.writeTo(response.getOutputStream());
    } catch (IOException e) {
      LOGGER.debug("Unable to write error html data to client.");
    }
  }

  private void initErrorHtml() {
    if (SERVER_ERROR_PLEASE_SEE_LOGS.equals(errorHtml)) {
      try (InputStream input = readResource("/error.html")) {
        errorHtml = IOUtils.toString(input, StandardCharsets.UTF_8);
      } catch (IOException e) {
        LOGGER.debug("Unable to read/parse error.html.", e);
      }
    }
  }

  private InputStream readResource(String resourcePath) throws IOException {
    Bundle bundle = FrameworkUtil.getBundle(DefaultErrorHandler.class);
    if (null != bundle) {
      return bundle.getEntry(resourcePath).openStream();
    } else {
      throw new IOException("Unable to retrieve bundle");
    }
  }
}
