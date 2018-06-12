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
package org.codice.ddf.opensearch.source;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Builds DDF REST URL. Limited to single metacard retrieval.
 *
 * @author Ashraf Barakat
 */
public class RestUrl {

  private static final String RESOURCE_QUERY_PARAM = "transform=resource";

  private final String baseUrl;

  private String id;

  private boolean retrieveResource;

  private RestUrl(String protocol, String host, String port, String contextPath) {
    baseUrl = protocol + "://" + host + ":" + port + contextPath;
  }

  /**
   * Creates a {@link RestUrl} object from an endpoint url such as <code>
   *  http://localhost:8181/services/catalog/query?q={searchTerms}&src={fs:routeTo?}&count={count?}
   * </code> .
   *
   * @param urlTemplate - endpoint url template
   */
  public static RestUrl newInstance(String urlTemplate)
      throws URISyntaxException, MalformedURLException {

    int indexOf = urlTemplate.indexOf('{');
    if (indexOf == -1) {
      indexOf = urlTemplate.length();
    }
    URI uri = new URI(urlTemplate.substring(0, indexOf));
    URL url = uri.toURL();

    String protocol = url.getProtocol();
    String host = url.getHost();
    int port = url.getPort();
    String path = url.getPath();

    return new RestUrl(
        protocol, host, Integer.toString(port), path.substring(0, path.lastIndexOf('/') + 1));
  }

  public String getId() {
    return id;
  }

  public void setId(String literal) {
    this.id = literal;
  }

  public boolean isRetrieveResource() {
    return retrieveResource;
  }

  public void setRetrieveResource(boolean retrieveResource) {
    this.retrieveResource = retrieveResource;
  }

  @Override
  public String toString() {
    return buildUrl();
  }

  public String buildUrl() {
    String url = baseUrl;

    if (id != null) {
      url += id;
    }
    if (retrieveResource) {
      url += "?" + RESOURCE_QUERY_PARAM;
    }

    return url;
  }
}
