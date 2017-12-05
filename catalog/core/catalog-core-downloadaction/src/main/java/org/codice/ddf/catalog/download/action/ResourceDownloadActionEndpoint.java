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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codice.ddf.catalog.resource.download.DownloadToLocalSiteException;
import org.codice.ddf.catalog.resource.download.ResourceDownloadMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** REST endpoint class used to manage metacard product downloads. */
@Path("/")
public class ResourceDownloadActionEndpoint {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResourceDownloadActionEndpoint.class);

  public static final String CONTEXT_PATH = "/internal/catalog/download/cache";

  public static final String SOURCE_PARAM = "source";

  public static final String METACARD_PARAM = "metacard";

  private static final String RESPONSE_HTML_TEMPLATE = "response";

  private static final String SUCCESS_MESSAGE = "Download of resource started successfully";

  private static final String FAILURE_MESSAGE = "Failed to start download of resource";

  private final ResourceDownloadMBean resourceDownloadMBean;

  private final Template template;

  public ResourceDownloadActionEndpoint(Handlebars handlebars) {
    this.resourceDownloadMBean = createResourceDownloadMBeanProxy();
    try {
      template = handlebars.compile(RESPONSE_HTML_TEMPLATE);
    } catch (IOException e) {
      String message =
          String.format("Unable to compile handlebars template [%s].", RESPONSE_HTML_TEMPLATE);
      LOGGER.debug(message);
      throw new ResourceDownloadActionException(message, e);
    }
  }

  /**
   * Starts an asynchronous download of a specific metacard resource to the local site.
   *
   * @param sourceId ID of the federated source where the resource should be downloaded from
   * @param metacardId ID of the metacard that contains the resource to download
   * @return response object containing successful start of download message
   * @throws DownloadToCacheOnlyException
   */
  @GET
  public Response copyToLocalSite(
      @FormParam(SOURCE_PARAM) String sourceId, @FormParam(METACARD_PARAM) String metacardId)
      throws DownloadToLocalSiteException {
    if (sourceId == null) {
      throw new DownloadToLocalSiteException(Status.BAD_REQUEST, "Source ID missing");
    }

    if (metacardId == null) {
      throw new DownloadToLocalSiteException(Status.BAD_REQUEST, "Metacard ID missing");
    }

    try {
      LOGGER.debug(
          "Downloading resource associated with metacard id [{}] from source [{}] to the local site.",
          metacardId,
          sourceId);
      resourceDownloadMBean.copyToLocalSite(sourceId, metacardId);
      return Response.ok(generateHtmlResponse(SUCCESS_MESSAGE)).build();
    } catch (MBeanException e) {
      LOGGER.debug(e.getTargetException().getMessage(), e.getTargetException());
      if (e.getTargetException() instanceof DownloadToLocalSiteException) {
        Status status = ((DownloadToLocalSiteException) e.getTargetException()).getStatus();
        return Response.status(status).entity(generateHtmlResponse(FAILURE_MESSAGE)).build();
      }
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(generateHtmlResponse(FAILURE_MESSAGE))
          .build();
    }
  }

  private String generateHtmlResponse(String message) {
    try {
      return template.apply(message);
    } catch (IOException e) {
      throw new DownloadToLocalSiteException(
          Status.INTERNAL_SERVER_ERROR, "Unable to display page.");
    }
  }

  ResourceDownloadMBean createResourceDownloadMBeanProxy() {
    try {
      return JMX.newMBeanProxy(
          ManagementFactory.getPlatformMBeanServer(),
          new ObjectName(ResourceDownloadMBean.OBJECT_NAME),
          ResourceDownloadMBean.MBEAN_CLASS);
    } catch (MalformedObjectNameException e) {
      String message =
          String.format(
              "Unable to create MBean proxy for [%s].", ResourceDownloadMBean.class.getName());
      LOGGER.debug(message, e);
      throw new ResourceDownloadActionException(message, e);
    }
  }
}
