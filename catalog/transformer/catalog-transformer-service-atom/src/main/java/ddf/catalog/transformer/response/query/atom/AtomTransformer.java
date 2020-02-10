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
package ddf.catalog.transformer.response.query.atom;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.geo.formatter.CompositeGeometry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.namespace.QName;
import org.apache.abdera.Abdera;
import org.apache.abdera.ext.geo.GeoHelper;
import org.apache.abdera.ext.geo.GeoHelper.Encoding;
import org.apache.abdera.ext.geo.Position;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.model.Content.Type;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemInfo;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link QueryResponseTransformer} that transforms query results into an Atom formatted
 * feed. <br>
 * Atom specification referenced and used for this implementation was found at
 * http://tools.ietf.org/html/rfc4287
 */
public class AtomTransformer implements QueryResponseTransformer {
  private static final int ALL_RESULTS_COUNT_VALUE = -1;

  /**
   * This variable is a workaround. If org.apache.abdera.model.Link ever includes a "REL_PREVIEW"
   * member variable, take this variable out and replace it in any function calls with:
   * "Link.REL_PREVIEW"
   */
  public static final String REL_PREVIEW = "preview";

  public static final MimeType MIME_TYPE = new MimeType();

  static final String DEFAULT_FEED_TITLE = "Query Response";

  static final String DEFAULT_AUTHOR = "unknown";

  static final String URN_CATALOG_ID = "urn:catalog:id:";

  static final String URN_UUID = "urn:uuid:";

  static final String DEFAULT_SOURCE_ID = "unknown";

  private static final String FEDERATION_EXTENSION_NAMESPACE =
      "http://a9.com/-/opensearch/extensions/federation/1.0/";

  private static final String COULD_NOT_CREATE_XML_CONTENT_MESSAGE =
      "Could not create xml content. Running default behavior.";

  private static final Logger LOGGER = LoggerFactory.getLogger(AtomTransformer.class);

  private static final String MIME_TYPE_JPEG = "image/jpeg";

  private static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  // expensive creation, meant to be done once
  private static final Abdera ABDERA = new Abdera();

  static {
    try {
      MIME_TYPE.setPrimaryType("application");
      MIME_TYPE.setSubType("atom+xml");
    } catch (MimeTypeParseException e) {
      LOGGER.info("MimeType exception during static setup", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  private MetacardTransformer metacardTransformer;

  private ActionProvider viewMetacardActionProvider;

  private ActionProvider resourceActionProvider;

  private ActionProvider thumbnailActionProvider;

  public void setViewMetacardActionProvider(ActionProvider viewMetacardActionProvider) {
    this.viewMetacardActionProvider = viewMetacardActionProvider;
  }

  public void setResourceActionProvider(ActionProvider resourceActionProvider) {
    this.resourceActionProvider = resourceActionProvider;
  }

  public void setThumbnailActionProvider(ActionProvider thumbnailActionProvider) {
    this.thumbnailActionProvider = thumbnailActionProvider;
  }

  public void setMetacardTransformer(MetacardTransformer metacardTransformer) {
    this.metacardTransformer = metacardTransformer;
  }

  @Override
  public BinaryContent transform(SourceResponse sourceResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (sourceResponse == null) {
      throw new CatalogTransformerException(
          "Cannot transform null " + SourceResponse.class.getName());
    }

    final Date currentDate = new Date();
    final Feed feed = createFeed();

    /*
     * Atom spec text (rfc4287) Sect 4.2.14: "The "atom:title" element is a Text construct that
     * conveys a human- readable title for an entry or feed."
     */
    feed.setTitle(DEFAULT_FEED_TITLE);

    feed.setUpdated(currentDate);

    // TODO Use the same id for the same query
    // one challenge is a query in one site should not have the same feed id
    // as a query in another site probably could factor in ddf.host and port
    // into the algorithm

    feed.setId(URN_UUID + UUID.randomUUID().toString());

    // TODO SELF LINK For the Feed, possible design --> serialize Query into
    // a URL
    /*
     * Atom spec text (rfc4287): "atom:feed elements SHOULD contain one atom:link element with a
     * rel attribute value of self. This is the preferred URI for retrieving Atom Feed Documents
     * representing this Atom feed. "
     */
    feed.addLink("#", Link.REL_SELF);

    if (!StringUtils.isEmpty(SystemInfo.getOrganization())) {
      feed.addAuthor(SystemInfo.getOrganization());
    } else {
      feed.addAuthor(DEFAULT_AUTHOR);
    }

    /*
     * Atom spec text (rfc4287 sect. 4.2.4): "The "atom:generator" element's content identifies
     * the agent used to generate a feed, for debugging and other purposes." Generator is not
     * required in the atom:feed element.
     */
    if (!StringUtils.isEmpty(SystemInfo.getSiteName())) {
      // text is required.
      feed.setGenerator(null, SystemInfo.getVersion(), SystemInfo.getSiteName());
    }

    /*
     * According to http://www.opensearch.org/Specifications/OpenSearch/1.1 specification,
     * totalResults must be a non-negative integer. Requirements: This attribute is optional.
     */
    if (sourceResponse.getHits() > -1) {
      Element hits = feed.addExtension(OpenSearchConstants.TOTAL_RESULTS);
      hits.setText(Long.toString(sourceResponse.getHits()));
    }

    if (sourceResponse.getRequest() != null && sourceResponse.getRequest().getQuery() != null) {
      Element itemsPerPage = feed.addExtension(OpenSearchConstants.ITEMS_PER_PAGE);
      Element startIndex = feed.addExtension(OpenSearchConstants.START_INDEX);

      /*
       * According to http://www.opensearch.org/Specifications/OpenSearch/1.1 specification,
       * itemsPerPage must be a non-negative integer. It is possible that Catalog pageSize is
       * set to a non-negative integer though. When non-negative we will instead we will
       * change it to the number of search results on current page.
       */
      if (sourceResponse.getRequest().getQuery().getPageSize() > -1) {
        itemsPerPage.setText(
            Integer.toString(sourceResponse.getRequest().getQuery().getPageSize()));
      } else {
        if (sourceResponse.getResults() != null) {
          itemsPerPage.setText(Integer.toString(sourceResponse.getResults().size()));
        }
      }

      startIndex.setText(Integer.toString(sourceResponse.getRequest().getQuery().getStartIndex()));
    }

    if (getCount(sourceResponse) != 0) {
      sourceResponse.getResults().stream().forEach(r -> addSingleResult(currentDate, feed, r));
    }

    byte[] bytes = createOutputStream(feed);
    return new BinaryContentImpl(new ByteArrayInputStream(bytes), MIME_TYPE);
  }

  private byte[] createOutputStream(Feed feed) throws CatalogTransformerException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(AtomTransformer.class.getClassLoader());
      feed.writeTo(baos);
    } catch (IOException e) {
      LOGGER.info("Could not write to output stream.", e);
      throw new CatalogTransformerException("Could not transform into Atom.", e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
    return baos.toByteArray();
  }

  private Feed createFeed() {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(AtomTransformer.class.getClassLoader());
      return ABDERA.newFeed();
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  private void addSingleResult(Date currentDate, Feed feed, Result result) {
    Metacard metacard = result.getMetacard();

    if (metacard == null) {
      return;
    }

    Entry entry = feed.addEntry();
    entry.setId(URN_CATALOG_ID + metacard.getId());

    addSourceExtension(result, entry);
    addRelevanceScoreExtension(result, entry);

    /*
     * Atom spec text (rfc4287): "The "atom:title" element is a Text construct that conveys
     * a human- readable title for an entry or feed."
     */
    entry.setTitle(metacard.getTitle());

    /*
     * Atom spec text (rfc4287): "The "atom:updated" element is a Date construct indicating
     * the most recent instant in time when an entry or feed was modified in a way the
     * publisher considers significant." Therefore, a new Date is used because we are making
     * the entry for the first time.
     */
    entry.setUpdated(Optional.ofNullable(metacard.getModifiedDate()).orElse(currentDate));

    /*
     * Atom spec text (rfc4287): "Typically, atom:published will be associated with the
     * initial creation or first availability of the resource."
     */
    Optional.ofNullable(metacard.getCreatedDate()).ifPresent(entry::setPublished);

    /*
     * For atom:link elements, Atom spec text (rfc4287): "The value "related" signifies that
     * the IRI in the value of the href attribute identifies a resource related to the
     * resource described by the containing element."
     */
    addLink(resourceActionProvider, metacard, entry, Link.REL_RELATED);
    addLink(viewMetacardActionProvider, metacard, entry, Link.REL_ALTERNATE);
    addLink(thumbnailActionProvider, metacard, entry, REL_PREVIEW);

    /*
     * Atom spec text (rfc4287) Sect. 4.2.2.: "The "atom:category" element conveys
     * information about a category associated with an entry or feed. This specification
     * assigns no meaning to the content (if any) of this element."
     */

    Optional.ofNullable(metacard.getContentTypeName()).ifPresent(entry::addCategory);
    addPosition(metacard, entry);
    setContent(metacard, entry);
  }

  private void addPosition(Metacard metacard, Entry entry) {
    for (Position position : getGeoRssPositions(metacard)) {
      GeoHelper.addPosition(entry, position, Encoding.GML);
    }
  }

  private void setContent(Metacard metacard, Entry entry) {
    String contentOutput = metacard.getId();
    Type atomContentType = Type.TEXT;

    if (metacardTransformer != null) {
      BinaryContent binaryContent = getBinaryContent(metacard);

      String content = getContentOutput(binaryContent);
      contentOutput = Optional.ofNullable(content).orElse(metacard.getId());

      if (content != null) {
        atomContentType = Type.XML;
      }
    }

    ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(AtomTransformer.class.getClassLoader());
      entry.setContent(contentOutput, atomContentType);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  private String getContentOutput(BinaryContent binaryContent) {
    if (binaryContent != null) {
      try {
        byte[] xmlBytes = binaryContent.getByteArray();

        if (xmlBytes != null && xmlBytes.length > 0) {
          return new String(xmlBytes, StandardCharsets.UTF_8);
        }
      } catch (IOException e) {
        LOGGER.debug(COULD_NOT_CREATE_XML_CONTENT_MESSAGE, e);
      }
    }

    return null;
  }

  private BinaryContent getBinaryContent(Metacard metacard) {
    try {
      return metacardTransformer.transform(metacard, new HashMap<>());
    } catch (CatalogTransformerException | RuntimeException e) {
      LOGGER.debug(COULD_NOT_CREATE_XML_CONTENT_MESSAGE, e);
    }

    return null;
  }

  private void addRelevanceScoreExtension(Result result, Entry entry) {
    if (result.getRelevanceScore() != null) {
      Element relevance =
          entry.addExtension(
              new QName(
                  "http://a9.com/-/opensearch/extensions/relevance/1.0/", "score", "relevance"));
      relevance.setText(result.getRelevanceScore().toString());
    }
  }

  private void addSourceExtension(Result result, Entry entry) {
    String sourceName =
        Optional.ofNullable(result.getMetacard().getSourceId()).orElse(DEFAULT_SOURCE_ID);

    Element source =
        entry.addExtension(new QName(FEDERATION_EXTENSION_NAMESPACE, "resultSource", "fs"));

    /*
     * According to the os-federation.xsd, the resultSource element text has a max length of
     * 16 and is the shortname of the source id. Previously, we were duplicating the names
     * in both positions, but since we truly do not have a shortname for our source ids, I
     * am purposely omitting the shortname text and leaving it as the empty string. The real
     * source id can still be found in the attribute instead.
     */

    source.setAttributeValue(new QName(FEDERATION_EXTENSION_NAMESPACE, "sourceId"), sourceName);
  }

  // a Link object could not be made and returned without a classpath problem in the OSGi runtime
  // therefore this was a workaround that did not require me to add special logic for
  // contextclassloader
  private void addLink(
      ActionProvider actionProvider, Metacard metacard, Entry entry, String linkType) {

    if (actionProvider != null) {
      try {

        Action action = actionProvider.getAction(metacard);

        if (action != null) {
          if (actionProvider.equals(resourceActionProvider) && metacard.getResourceURI() != null) {

            Link viewLink = addLinkHelper(action, entry, linkType, MIME_TYPE_OCTET_STREAM);
            try {
              Long length = Long.parseLong(metacard.getResourceSize(), 10);
              viewLink.setLength(length);
            } catch (NumberFormatException e) {
              LOGGER.debug("Could not cast {} as Long type.", metacard.getResourceSize());
            }

          } else if (actionProvider.equals(thumbnailActionProvider)
              && metacard.getThumbnail() != null) {

            addLinkHelper(action, entry, linkType, MIME_TYPE_JPEG);
          } else if (!actionProvider.equals(resourceActionProvider)
              && !actionProvider.equals(thumbnailActionProvider)) {

            addLinkHelper(action, entry, linkType, MIME_TYPE_OCTET_STREAM);
          }
        }

      } catch (RuntimeException e) {
        // ActionProvider is injected but not available
        LOGGER.debug("Could not retrieve action.", e);
      }
    }
  }

  private Link addLinkHelper(Action action, Entry entry, String linkType, String mimeType) {
    Link viewLink = entry.addLink(action.getUrl().toString(), linkType);
    viewLink.setTitle(action.getTitle());
    viewLink.setMimeType(mimeType);
    return viewLink;
  }

  private List<Position> getGeoRssPositions(Metacard metacard) {

    List<Position> georssPositions = new ArrayList<Position>();

    for (AttributeDescriptor ad : metacard.getMetacardType().getAttributeDescriptors()) {

      if (ad != null
          && ad.getType() != null
          && BasicTypes.GEO_TYPE.getAttributeFormat().equals(ad.getType().getAttributeFormat())) {

        Attribute geoAttribute = metacard.getAttribute(ad.getName());

        if (geoAttribute == null) {
          continue;
        }

        for (Serializable geo : geoAttribute.getValues()) {

          if (geo != null) {

            try {
              Geometry geometry = new WKTReader(GEOMETRY_FACTORY).read(geo.toString());

              CompositeGeometry formatter = CompositeGeometry.getCompositeGeometry(geometry);

              if (null != formatter) {
                georssPositions.addAll(formatter.toGeoRssPositions());
              } else {
                LOGGER.debug(
                    "When cycling through geometries, could not get composite geometry [{}]", geo);
              }

            } catch (ParseException e) {
              LOGGER.info("When cycling through geometries, could not parse [{}]", geo, e);
            }
          }
        }
      }
    }
    return georssPositions;
  }

  private int getCount(SourceResponse queryResponse) {
    if (queryResponse == null) {
      return ALL_RESULTS_COUNT_VALUE;
    }

    QueryRequest queryRequest = queryResponse.getRequest();

    if (queryRequest == null) {
      return ALL_RESULTS_COUNT_VALUE;
    }

    Serializable countSerializable = queryRequest.getProperties().get("count");

    if (countSerializable == null) {
      return ALL_RESULTS_COUNT_VALUE;
    }

    try {
      return Integer.valueOf((String) countSerializable);
    } catch (NumberFormatException nfe) {
      return ALL_RESULTS_COUNT_VALUE;
    }
  }
}
