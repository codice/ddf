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

import static org.boon.Boon.toJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codice.ddf.platform.error.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorHandler implements ErrorHandler {

  public static final String SERVER_ERROR_PLEASE_SEE_LOGS = "Server Error, please see logs.";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorHandler.class);

  private static final int BUFFER_SIZE = 4096;

  private String indexHtml = SERVER_ERROR_PLEASE_SEE_LOGS;

  private void initIndexHtml() {
    if (SERVER_ERROR_PLEASE_SEE_LOGS.equals(indexHtml)) {
      Bundle bundle = FrameworkUtil.getBundle(DefaultErrorHandler.class);
      if (null != bundle) {
        try {
          indexHtml = IOUtils.toString(bundle.getEntry("/index.html").openStream());
        } catch (Exception e) {
          LOGGER.debug("Unable to read/parse index.html.", e);
        }
      } else {
        LOGGER.debug("Unable to retrieve Bundle");
      }
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
    initIndexHtml();

    String stack = ExceptionUtils.getFullStackTrace(throwable);

    Map<String, String> jsonMap = new HashMap<>();
    jsonMap.put("code", String.valueOf(code));
    jsonMap.put("message", message);
    jsonMap.put("type", type);
    jsonMap.put("throwable", stack);
    jsonMap.put("uri", uri);
    String data = toJson(jsonMap);
    String encodedBytes = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));

    String localIndexHtml = indexHtml.replace("WINDOW_DATA", "\"" + encodedBytes + "\"");

    response.setStatus(code);
    response.setContentType("text/html");
    try (ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(BUFFER_SIZE)) {
      writer.write(localIndexHtml);
      writer.flush();
      writer.writeTo(response.getOutputStream());
    } catch (IOException e) {
      LOGGER.debug("Unable to write error html data to client.");
    }
  }
}
