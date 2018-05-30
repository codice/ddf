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
package org.codice.ddf.catalog.download.action;

import static org.codice.ddf.catalog.download.action.ResourceDownloadActionEndpoint.CONTEXT_PATH;
import static org.codice.ddf.catalog.download.action.ResourceDownloadActionEndpoint.METACARD_PARAM;
import static org.codice.ddf.catalog.download.action.ResourceDownloadActionEndpoint.SOURCE_PARAM;

import ddf.action.Action;
import ddf.action.impl.ActionImpl;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Optional;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.catalog.actions.AbstractMetacardActionProvider;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action provider that creates {@link Action}s used to asynchronously download resources to the
 * local site.
 */
public class ResourceDownloadActionProvider extends AbstractMetacardActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResourceDownloadActionProvider.class);

  private static final String TITLE = "Copy resource to local site";

  private static final String DESCRIPTION =
      "Copies the resource associated with this metacard to the local site";

  private final ResourceCacheServiceMBean resourceCacheMBean;

  public ResourceDownloadActionProvider(String actionProviderId) {
    super(actionProviderId, TITLE, DESCRIPTION);
    resourceCacheMBean = createResourceCacheMBeanProxy();
  }

  /**
   * Returns {@code true} if the resource associated with the {@link Metacard} is local; false
   * otherwise.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected boolean canHandleMetacard(Metacard metacard) {
    return hasResourceUri(metacard)
        ? (hasRemoteSiteName(metacard) || isResourceUriRemote(metacard))
            && !isResourceCached(metacard)
        : false;
  }

  @Override
  protected Action createMetacardAction(
      String actionProviderId, String title, String description, URL url) {
    return new ActionImpl(actionProviderId, title, description, url);
  }

  @Override
  protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
    String encodedMetacardSource = URLEncoder.encode(metacardSource, CharEncoding.UTF_8);
    return getActionUrl(encodedMetacardSource, encodedMetacardId);
  }

  private URL getActionUrl(String metacardSource, String metacardId)
      throws MalformedURLException, URISyntaxException {
    return new URI(
            SystemBaseUrl.EXTERNAL.constructUrl(
                String.format(
                    "%s?%s=%s&%s=%s",
                    CONTEXT_PATH, SOURCE_PARAM, metacardSource, METACARD_PARAM, metacardId),
                true))
        .toURL();
  }

  private boolean isResourceCached(Metacard metacard) {
    return resourceCacheMBean.contains(metacard);
  }

  private boolean hasResourceUri(Metacard metacard) {
    Optional<String> resourceUri =
        Optional.ofNullable(metacard.getResourceURI()).map(uri -> uri.toString());
    return resourceUri.isPresent();
  }

  private boolean isResourceUriRemote(Metacard metacard) {
    return !metacard.getResourceURI().toString().startsWith(ContentItem.CONTENT_SCHEME);
  }

  private boolean hasRemoteSiteName(Metacard metacard) {
    Optional<String> sourceId = Optional.ofNullable(metacard.getSourceId());

    if (sourceId.isPresent()) {
      return !sourceId.get().equalsIgnoreCase(getLocalSiteName());
    } else {
      LOGGER.debug(
          "Unable to determine if the source id in metacard {} matches the local site name because the "
              + " metacard did not contain a source id attribute.",
          metacard.getId());
      return false;
    }
  }

  String getLocalSiteName() {
    return SystemInfo.getSiteName().toLowerCase(Locale.getDefault());
  }

  ResourceCacheServiceMBean createResourceCacheMBeanProxy() {
    try {
      return JMX.newMBeanProxy(
          ManagementFactory.getPlatformMBeanServer(),
          new ObjectName(ResourceCacheServiceMBean.OBJECT_NAME),
          ResourceCacheServiceMBean.MBEAN_CLASS);
    } catch (MalformedObjectNameException e) {
      String message =
          String.format(
              "Unable to create MBean proxy for [%s].", ResourceCacheServiceMBean.class.getName());
      LOGGER.debug(message, e);
      throw new ResourceDownloadActionException(message, e);
    }
  }
}
