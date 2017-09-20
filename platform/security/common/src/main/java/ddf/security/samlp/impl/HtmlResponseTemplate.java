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
package ddf.security.samlp.impl;

import ddf.security.samlp.SamlProtocol;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlResponseTemplate {
  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlResponseTemplate.class);

  private static String redirectTemplate;

  private static String submitTemplate;

  static {
    try (InputStream submitFormStream =
            HtmlResponseTemplate.class.getResourceAsStream("/templates/submitFormTemplate.html");
        InputStream redirectPageStream =
            HtmlResponseTemplate.class.getResourceAsStream("/templates/redirectTemplate.html")) {
      submitTemplate = IOUtils.toString(submitFormStream);
      redirectTemplate = IOUtils.toString(redirectPageStream);
    } catch (Exception e) {
      LOGGER.warn("Unable to load index page for IDP.", e);
    }
  }

  private HtmlResponseTemplate() {}

  /**
   * Gets a Form Submit page with containing the specified information. Will turn any null Strings
   * into blank Strings.
   *
   * @param targetUrl Destination URL
   * @param type the {@link SamlProtocol.Type}
   * @param samlValue Base64 encoded saml object
   * @param relayState the relay State
   * @return Formatted Form submit page
   */
  public static String getPostPage(
      String targetUrl, SamlProtocol.Type type, String samlValue, String relayState) {
    return String.format(
        submitTemplate,
        elvis(targetUrl, ""),
        elvis(type, SamlProtocol.Type.NULL).getKey(),
        elvis(samlValue, ""),
        elvis(relayState, ""));
  }

  /**
   * Gets a Redirect page containing the specified information. Will turn any null Strings into
   * blank Strings.
   *
   * @param targetUrl Destination URL
   * @return Formatted Redirect Page
   */
  public static String getRedirectPage(String targetUrl) {
    return String.format(redirectTemplate, elvis(targetUrl, ""));
  }

  private static <T> T elvis(T object, T valueIfNull) {
    if (object != null) {
      return object;
    } else {
      return valueIfNull;
    }
  }
}
