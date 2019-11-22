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
package org.codice.ddf.spatial.kml.endpoint;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.RefreshMode;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint used to create KML {@link NetworkLink}s. The KML Network Link will link Google Earth to
 * the Catalog through the OpenSearch Endpoint.
 *
 * @author Keith C Wire
 */
@Path("/")
public class KmlEndpoint {

  private static final String FORWARD_SLASH = "/";

  private static final String CATALOG_URL_PATH = "catalog";

  private static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

  private static final String KML_TRANSFORM_PARAM = "kml";

  private static final String OPENSEARCH_URL_PATH = "query";

  private static final String OPENSEARCH_SORT_KEY = "sort";

  private static final String OPENSEARCH_DEFAULT_SORT = "date:desc";

  private static final String OPENSEARCH_FORMAT_KEY = "format";

  private static final String ICONS_RESOURCE_LOC = "icons/";

  private static final long REFRESH_INTERVAL = 12L * 60 * 60; // 12 Hours in Seconds

  /** Default refresh time after the View stops moving */
  private static final double DEFAULT_VIEW_REFRESH_TIME = 2.0;

  /**
   * The format of the bounding box query parameters Google Earth attaches to the end of the query
   * URL.
   */
  private static final String VIEW_FORMAT_STRING =
      "bbox=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]";

  private static final String SOURCE_PARAM = "src";

  private static final String COUNT_PARAM = "count=";

  private static final Logger LOGGER = LoggerFactory.getLogger(KmlEndpoint.class);

  private CatalogFramework framework;

  private Kml styleDoc;

  private String styleUrl;

  private String iconLoc;

  private String description;

  private Boolean visibleByDefault = false;

  private Integer maxResults = 100;

  private String webSite;

  private String logo;

  private String productName;

  private String baseUrl;

  private ClassPathTemplateLoader templateLoader;

  public KmlEndpoint(BrandingRegistry brandingPlugin, CatalogFramework catalogFramework) {
    Optional<BrandingRegistry> brandingRegistry = Optional.ofNullable(brandingPlugin);
    LOGGER.trace("ENTERING: KML Endpoint Constructor");
    this.framework = catalogFramework;
    templateLoader = new ClassPathTemplateLoader();
    templateLoader.setPrefix("/templates");
    templateLoader.setSuffix(".hbt");
    this.productName = brandingRegistry.map(BrandingRegistry::getProductName).orElse("");
    LOGGER.trace("EXITING: KML Endpoint Constructor");
  }

  /**
   * Attempts to load a KML {@link de.micromata.opengis.kml.v_2_2_0.Style} from a file provided via
   * a file system path.
   *
   * @param url - the path to the file.
   */
  public void setStyleUrl(String url) {
    if (StringUtils.isNotBlank(url)) {
      try {
        styleDoc = null;
        styleUrl = url;
        styleDoc = Kml.unmarshal(new URL(styleUrl).openStream());
      } catch (MalformedURLException e) {
        LOGGER.debug(
            "StyleUrl is not a valid URL. Unable to serve up custom KML de.micromata.opengis.kml.v_2_2_0.Style.",
            e);
      } catch (IOException e) {
        LOGGER.debug(
            "Unable to open de.micromata.opengis.kml.v_2_2_0.Style Document from StyleUrl.", e);
      }
    }
  }

  /**
   * Sets the root directory of icons to be provided via this endpoint.
   *
   * @param iconLoc - the path to the directory of icons
   */
  public void setIconLoc(String iconLoc) {
    this.iconLoc = iconLoc;
  }

  public String getDescription() {
    return this.description;
  }

  /**
   * Sets the Description that will be used as the description of the Root {@link NetworkLink}.
   *
   * @param description - the Description of the Root {@link NetworkLink}
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Sets if the ddf.catalog.source.Source {@link NetworkLink}s should be Visible by Default.
   *
   * @param visibleByDefault - true to enable
   */
  public void setVisibleByDefault(Boolean visibleByDefault) {
    this.visibleByDefault = visibleByDefault;
  }

  /**
   * Sets the Maximum Number of results each {@link NetworkLink} will return.
   *
   * @param maxResults - maximum number of results to return
   */
  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  public String getWebSite() {
    return this.webSite;
  }

  /**
   * Sets the Web Site URL that will be used in the description of the Root {@link NetworkLink}.
   *
   * @param webSite - the URL of the web site
   */
  public void setWebSite(String webSite) {
    this.webSite = webSite;
  }

  public String getLogo() {
    return this.logo;
  }

  /**
   * Sets the URL of the Logo that will be used in the description of the Root {@link NetworkLink} .
   *
   * @param logo - the URL to the logo
   */
  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getProductName() {
    return this.productName;
  }

  public String getContact() {
    return SystemInfo.getSiteContatct();
  }

  public String getBaseUrl() {
    return this.baseUrl;
  }

  /**
   * Creates a {@link NetworkLink} to provide a layer to KML Clients.
   *
   * @param uriInfo - injected resource providing the URI.
   * @return - KML NetworkLink
   */
  @GET
  @Path(FORWARD_SLASH)
  @Produces(KML_MIME_TYPE)
  public Kml getKmlNetworkLink(@Context UriInfo uriInfo) {
    LOGGER.debug("ENTERING: getKmlNetworkLink");
    try {
      return createRootNetworkLink(uriInfo);
    } catch (UnknownHostException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Kml createRootNetworkLink(UriInfo uriInfo) throws UnknownHostException {
    Kml kml = KmlFactory.createKml();
    NetworkLink rootNetworkLink = kml.createAndSetNetworkLink();

    rootNetworkLink.setName(this.productName);
    rootNetworkLink.setSnippet(KmlFactory.createSnippet().withMaxLines(0));
    UriBuilder baseUrlBuidler = UriBuilder.fromUri(uriInfo.getBaseUri());
    baseUrlBuidler.replacePath("");
    this.baseUrl = baseUrlBuidler.build().toString();
    String descriptionHtml = description;
    Handlebars handlebars = new Handlebars(templateLoader);
    try {
      Template template = handlebars.compile("description");
      descriptionHtml = template.apply(this);
      LOGGER.debug(descriptionHtml);
    } catch (IOException e) {
      LOGGER.debug("Failed to apply description Template", e);
    }
    rootNetworkLink.setDescription(descriptionHtml);
    rootNetworkLink.setOpen(true);
    rootNetworkLink.setVisibility(false);
    Link link = rootNetworkLink.createAndSetLink();
    UriBuilder builder = UriBuilder.fromUri(uriInfo.getBaseUri());
    builder =
        generateEndpointUrl(
            SystemBaseUrl.EXTERNAL.constructUrl(
                FORWARD_SLASH
                    + CATALOG_URL_PATH
                    + FORWARD_SLASH
                    + KML_TRANSFORM_PARAM
                    + FORWARD_SLASH
                    + "sources",
                true),
            builder);
    link.setHref(builder.build().toString());
    link.setViewRefreshMode(ViewRefreshMode.NEVER);
    link.setRefreshMode(RefreshMode.ON_INTERVAL);
    link.setRefreshInterval(REFRESH_INTERVAL);

    return kml;
  }

  /**
   * Creates a list of {@link NetworkLink}s, one for each {@link ddf.catalog.source.Source}
   * including the local catalog.
   *
   * @param uriInfo - injected resource provding the URI.
   * @return - {@link Kml} containing a folder of {@link NetworkLink}s.
   */
  @GET
  @Path(FORWARD_SLASH + "sources")
  @Produces(KML_MIME_TYPE)
  public Kml getAvailableSources(@Context UriInfo uriInfo) {
    try {
      SourceInfoResponse response = framework.getSourceInfo(new SourceInfoRequestEnterprise(false));

      Kml kml = KmlFactory.createKml();
      Folder folder = kml.createAndSetFolder();
      folder.setOpen(true);
      for (SourceDescriptor descriptor : response.getSourceInfo()) {
        UriBuilder builder = UriBuilder.fromUri(uriInfo.getBaseUri());
        builder =
            generateEndpointUrl(
                SystemBaseUrl.EXTERNAL.getRootContext()
                    + FORWARD_SLASH
                    + CATALOG_URL_PATH
                    + FORWARD_SLASH
                    + OPENSEARCH_URL_PATH,
                builder);
        builder = builder.queryParam(SOURCE_PARAM, descriptor.getSourceId());
        builder = builder.queryParam(OPENSEARCH_SORT_KEY, OPENSEARCH_DEFAULT_SORT);
        builder = builder.queryParam(OPENSEARCH_FORMAT_KEY, KML_TRANSFORM_PARAM);
        NetworkLink networkLink =
            generateViewBasedNetworkLink(builder.build().toURL(), descriptor.getSourceId());
        folder.getFeature().add(networkLink);
      }

      return kml;
    } catch (SourceUnavailableException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    } catch (UnknownHostException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    } catch (MalformedURLException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    } catch (UriBuilderException e) {
      throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
    }
  }

  /*
   * Generates xml for View-based Network Link
   *
   * @param networkLinkUrl - url to set as the Link href.
   *
   * @return Networklink
   */
  private NetworkLink generateViewBasedNetworkLink(URL networkLinkUrl, String sourceId) {
    // create network link and give it a name
    NetworkLink networkLink = KmlFactory.createNetworkLink();
    networkLink.setName(sourceId);
    networkLink.setOpen(true);
    networkLink.setVisibility(this.visibleByDefault);

    // create link and add it to networkLinkElements
    Link link = networkLink.createAndSetLink();
    LOGGER.debug("View Based Network Link href: {}", networkLinkUrl);
    link.setHref(networkLinkUrl.toString());
    link.setViewRefreshMode(ViewRefreshMode.ON_STOP);
    link.setViewRefreshTime(DEFAULT_VIEW_REFRESH_TIME);
    link.setViewFormat(VIEW_FORMAT_STRING);
    link.setViewBoundScale(1);
    link.setHttpQuery(COUNT_PARAM + maxResults);

    return networkLink;
  }

  /*
   * Creates the URL based on the configured host, port, and services context root path.
   */
  private UriBuilder generateEndpointUrl(String path, UriBuilder uriBuilder)
      throws UnknownHostException {
    UriBuilder builder = uriBuilder;
    builder.host(SystemBaseUrl.EXTERNAL.getHost());

    try {
      builder.port(Integer.parseInt(SystemBaseUrl.EXTERNAL.getPort()));
    } catch (NumberFormatException nfe) {
      LOGGER.debug(
          "Cannot convert the current DDF port: {} to an integer."
              + " Defaulting to port in invocation.",
          SystemBaseUrl.EXTERNAL.getPort());
      throw new UnknownHostException("Unable to determine port DDF is using.");
    }

    builder = builder.replacePath(path);

    return builder;
  }

  /**
   * Kml REST Get. Returns the style Document.
   *
   * @param uriInfo
   * @return stylesDoc
   * @throws WebApplicationException
   */
  @GET
  @Path(FORWARD_SLASH + "styles")
  @Produces(KML_MIME_TYPE)
  public Kml getKmlStyles(@Context UriInfo uriInfo) {
    if (styleDoc != null) {
      return styleDoc;
    }
    throw new WebApplicationException(
        new FileNotFoundException(
            "No KML de.micromata.opengis.kml.v_2_2_0.Style has been configured or unable to load document."),
        Status.NOT_FOUND);
  }

  /**
   * Retrieves an icon from the hosted directory based on the id provided.
   *
   * @param uriInfo - injected resource providing the URI
   * @param id - the id (filename) of the icon
   * @return iconBytes - the icon as a byte[]
   */
  @GET
  @Path("/icons/{id:.+}")
  @Produces({"image/png", "image/jpeg", "image/tiff", "image/gif"})
  public byte[] getIcon(@Context UriInfo uriInfo, @PathParam("id") String id) {

    byte[] iconBytes = null;

    if (StringUtils.isBlank(iconLoc)) {
      String icon = ICONS_RESOURCE_LOC + id;
      try (InputStream iconStream = this.getClass().getClassLoader().getResourceAsStream(icon)) {

        if (iconStream == null) {
          LOGGER.debug("Resource not found for icon {}", LogSanitizer.sanitize(icon));
          throw new WebApplicationException(
              new FileNotFoundException("Resource not found for icon " + icon), Status.NOT_FOUND);
        }

        iconBytes = IOUtils.toByteArray(iconStream);
      } catch (IOException e) {
        LOGGER.debug("Failed to read resource for icon {}", LogSanitizer.sanitize(icon), e);
        throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
      }
    } else {
      String icon = iconLoc + FORWARD_SLASH + id;

      try (InputStream message = new FileInputStream(icon)) {
        iconBytes = IOUtils.toByteArray(message);
      } catch (FileNotFoundException e) {
        LOGGER.debug("File not found for icon {}", LogSanitizer.sanitize(icon), e);
        throw new WebApplicationException(e, Status.NOT_FOUND);
      } catch (IOException e) {
        LOGGER.debug("Failed to read bytes for icon {}", LogSanitizer.sanitize(icon), e);
        throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
      }
    }

    return iconBytes;
  }
}
