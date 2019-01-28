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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryFilterTransformer;
import ddf.catalog.util.impl.CatalogQueryException;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.InsertResultType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SchemaComponentType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionSummaryType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.EID;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.GeometryOperandsType;
import net.opengis.filter.v_1_1_0.IdCapabilitiesType;
import net.opengis.filter.v_1_1_0.LogicalOperators;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.BoundingBoxType;
import net.opengis.ows.v_1_0_0.CodeType;
import net.opengis.ows.v_1_0_0.DCP;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.HTTP;
import net.opengis.ows.v_1_0_0.OnlineResourceType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import net.opengis.ows.v_1_0_0.RequestMethodType;
import net.opengis.ows.v_1_0_0.ResponsiblePartySubsetType;
import net.opengis.ows.v_1_0_0.ServiceIdentification;
import net.opengis.ows.v_1_0_0.ServiceProvider;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswActionTransformerProvider;
import org.geotools.filter.text.cql2.CQLException;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** CswEndpoint provides a server implementation of the Catalogue Service for Web (CSW) 2.0.2. */
public class CswEndpoint implements Csw {

  protected static final String SERVICE_TITLE = "Catalog Service for the Web";

  protected static final String SERVICE_ABSTRACT = "DDF CSW Endpoint";

  protected static final List<String> SERVICE_TYPE_VERSION =
      Collections.unmodifiableList(Arrays.asList(CswConstants.VERSION_2_0_2));

  protected static final List<SpatialOperatorNameType> SPATIAL_OPERATORS =
      Collections.unmodifiableList(
          Arrays.asList(
              SpatialOperatorNameType.BBOX,
              SpatialOperatorNameType.BEYOND,
              SpatialOperatorNameType.CONTAINS,
              SpatialOperatorNameType.CROSSES,
              SpatialOperatorNameType.DISJOINT,
              SpatialOperatorNameType.D_WITHIN,
              SpatialOperatorNameType.INTERSECTS,
              SpatialOperatorNameType.OVERLAPS,
              SpatialOperatorNameType.TOUCHES,
              SpatialOperatorNameType.WITHIN));

  protected static final List<ComparisonOperatorType> COMPARISON_OPERATORS =
      Collections.unmodifiableList(
          Arrays.asList(
              ComparisonOperatorType.BETWEEN,
              ComparisonOperatorType.NULL_CHECK,
              ComparisonOperatorType.LIKE,
              ComparisonOperatorType.EQUAL_TO,
              ComparisonOperatorType.GREATER_THAN,
              ComparisonOperatorType.GREATER_THAN_EQUAL_TO,
              ComparisonOperatorType.LESS_THAN,
              ComparisonOperatorType.LESS_THAN_EQUAL_TO,
              ComparisonOperatorType.EQUAL_TO,
              ComparisonOperatorType.NOT_EQUAL_TO,
              ComparisonOperatorType.FUZZY));

  protected static final String PROVIDER_NAME = "DDF";

  protected static final String SERVICE_IDENTIFICATION = "ServiceIdentification";

  protected static final String SERVICE_PROVIDER = "ServiceProvider";

  protected static final String OPERATIONS_METADATA = "OperationsMetadata";

  protected static final String FILTER_CAPABILITIES = "Filter_Capabilities";

  protected static final List<String> GET_CAPABILITIES_PARAMS =
      Collections.unmodifiableList(
          Arrays.asList(
              SERVICE_IDENTIFICATION, SERVICE_PROVIDER, OPERATIONS_METADATA, FILTER_CAPABILITIES));

  static final int DEFAULT_BATCH = 500;

  private static final List<String> ELEMENT_NAMES = Arrays.asList("brief", "summary", "full");

  private static final Logger LOGGER = LoggerFactory.getLogger(CswEndpoint.class);

  private static final String DEFAULT_OUTPUT_FORMAT = MediaType.APPLICATION_XML;

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private static final String ERROR_MULTI_PRODUCT_RETRIEVAL =
      "Can only retrieve product for one record.";

  private static final String ERROR_SCHEMA_FORMAT_PRODUCT_RETRIEVAL =
      "Unable to retrieve product because both output format and schema are not set "
          + "to '%s', '%s' respectively, or, cannot get record due to "
          + "incorrect output format and schema.";

  private static final String ERROR_ID_PRODUCT_RETRIEVAL = "Unable to retrieve product for ID: %s";

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static Map<String, Element> documentElements = new HashMap<>();

  private final TransformerManager mimeTypeTransformerManager;

  private final TransformerManager schemaTransformerManager;

  private final TransformerManager inputTransformerManager;

  private final CswActionTransformerProvider cswActionTransformerProvider;

  private CatalogFramework framework;

  private CapabilitiesType capabilitiesType;

  private Validator validator;

  private CswQueryFactory queryFactory;

  @Context private UriInfo uri;

  /** JAX-RS Server that represents a CSW v2.0.2 Server. */
  public CswEndpoint(
      CatalogFramework ddf,
      TransformerManager mimeTypeManager,
      TransformerManager schemaManager,
      TransformerManager inputManager,
      CswActionTransformerProvider cswActionTransformerProvider,
      Validator validator,
      CswQueryFactory queryFactory) {
    LOGGER.trace("Entering: CSW Endpoint constructor.");
    this.framework = ddf;
    this.mimeTypeTransformerManager = mimeTypeManager;
    this.schemaTransformerManager = schemaManager;
    this.inputTransformerManager = inputManager;
    this.cswActionTransformerProvider = cswActionTransformerProvider;
    this.validator = validator;
    this.queryFactory = queryFactory;
    LOGGER.trace("Exiting: CSW Endpoint constructor.");
  }

  @Override
  @GET
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public CapabilitiesType getCapabilities(@QueryParam("") GetCapabilitiesRequest request)
      throws CswException {

    LOGGER.trace("Entering: getCapabilities.");
    capabilitiesType = buildCapabilitiesType();

    if (request.getAcceptVersions() != null) {
      validator.validateVersion(request.getAcceptVersions());
    }

    List<String> sectionList = null;
    if (request.getSections() != null) {
      String[] sections = request.getSections().split(",");
      sectionList = Arrays.asList(sections);
    }

    LOGGER.trace("Exiting: getCapabilities.");

    return buildCapabilitiesType(sectionList);
  }

  @Override
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws CswException {
    capabilitiesType = buildCapabilitiesType();

    LOGGER.trace("Entering: getCapabilities.");
    if (request.getAcceptVersions() != null) {
      validator.validateVersion(request.getAcceptVersions().toString());
    }

    List<String> sectionList = null;
    if (request.getSections() != null) {
      sectionList = request.getSections().getSection();
    }

    LOGGER.trace("Exiting: getCapabilities.");

    return buildCapabilitiesType(sectionList);
  }

  @Override
  @GET
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public DescribeRecordResponseType describeRecord(@QueryParam("") DescribeRecordRequest request)
      throws CswException {
    if (request == null) {
      throw new CswException("DescribeRecordRequest request is null");
    }

    validator.validateOutputFormat(request.getOutputFormat(), mimeTypeTransformerManager);
    validator.validateSchemaLanguage(request.getSchemaLanguage());

    Map<String, String> namespacePrefixToUriMappings =
        request.parseNamespaces(request.getNamespace());

    validator.validateTypeNameToNamespaceMappings(
        request.getTypeName(), request.getNamespace(), namespacePrefixToUriMappings);

    if (request.getVersion() != null) {
      validator.validateVersion(request.getVersion());
    }

    List<QName> types = typeStringToQNames(request.getTypeName(), namespacePrefixToUriMappings);

    return buildDescribeRecordResponseFromTypes(types);
  }

  @Override
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public DescribeRecordResponseType describeRecord(DescribeRecordType request) throws CswException {
    if (request == null) {
      throw new CswException("DescribeRecordRequest request is null");
    }

    validator.validateOutputFormat(request.getOutputFormat(), mimeTypeTransformerManager);
    validator.validateSchemaLanguage(request.getSchemaLanguage());

    return buildDescribeRecordResponseFromTypes(request.getTypeName());
  }

  @Override
  @GET
  @Produces({MediaType.WILDCARD})
  public CswRecordCollection getRecords(@QueryParam("") GetRecordsRequest request)
      throws CswException {
    if (request == null) {
      throw new CswException("GetRecordsRequest request is null");
    } else {
      LOGGER.debug("{} attempting to get records.", request.getRequest());
    }
    if (StringUtils.isEmpty(request.getVersion())) {
      request.setVersion(CswConstants.VERSION_2_0_2);
    } else {
      validator.validateVersion(request.getVersion());
    }

    LOGGER.trace("Exiting getRecords");

    return getRecords(request.get202RecordsType());
  }

  @Override
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.WILDCARD})
  public CswRecordCollection getRecords(GetRecordsType request) throws CswException {
    if (request == null) {
      throw new CswException("GetRecordsType request is null");
    } else {
      LOGGER.debug("{} attempting to get records.", request.getService());
    }

    validator.validateOutputFormat(request.getOutputFormat(), mimeTypeTransformerManager);
    validator.validateOutputSchema(request.getOutputSchema(), schemaTransformerManager);

    if (request.getAbstractQuery() != null) {
      if (!request.getAbstractQuery().getValue().getClass().equals(QueryType.class)) {
        throw new CswException(
            "Unknown QueryType: " + request.getAbstractQuery().getValue().getClass());
      }

      QueryType query = (QueryType) request.getAbstractQuery().getValue();

      validator.validateTypes(query.getTypeNames(), CswConstants.VERSION_2_0_2);
      validator.validateElementNames(query);

      if (query.getConstraint() != null
          && query.getConstraint().isSetFilter()
          && query.getConstraint().isSetCqlText()) {
        throw new CswException("A Csw Query can only have a Filter or CQL constraint");
      }
    }
    LOGGER.trace("Exiting getRecords.");
    return queryCsw(request);
  }

  @Override
  @GET
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public CswRecordCollection getRecordById(
      @QueryParam("") GetRecordByIdRequest request,
      @HeaderParam(CswConstants.RANGE_HEADER) String rangeValue)
      throws CswException {
    if (request == null) {
      throw new CswException("GetRecordByIdRequest request is null");
    }

    String outputFormat = request.getOutputFormat();
    String outputSchema = request.getOutputSchema();
    validator.validateOutputFormat(outputFormat, mimeTypeTransformerManager);
    validator.validateOutputSchema(outputSchema, schemaTransformerManager);

    if (StringUtils.isNotBlank(request.getId())) {
      List<String> ids = Arrays.asList(request.getId().split(CswConstants.COMMA));
      String id = ids.get(0);
      // Check if the request wants to retrieve a product.
      if (isProductRetrieval(ids, outputFormat, outputSchema)) {
        LOGGER.debug("{} is attempting to retrieve product for ID: {}", request.getService(), id);
        try {
          return queryProductById(id, rangeValue);
        } catch (UnsupportedQueryException e) {
          throw new CswException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, id), e);
        }
      }
      LOGGER.debug("{} is attempting to retrieve records: {}", request.getService(), ids);
      CswRecordCollection response = queryById(ids, outputSchema);
      response.setOutputSchema(outputSchema);
      if (StringUtils.isNotBlank(request.getElementSetName())) {
        response.setElementSetType(ElementSetType.fromValue(request.getElementSetName()));
      } else {
        response.setElementSetType(ElementSetType.SUMMARY);
      }
      LOGGER.debug(
          "{} successfully retrieved record(s): {}", request.getRequest(), request.getId());
      return response;
    } else {
      throw new CswException(
          "A GetRecordById Query must contain an ID.", CswConstants.MISSING_PARAMETER_VALUE, "id");
    }
  }

  @Override
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public CswRecordCollection getRecordById(
      GetRecordByIdType request, @HeaderParam(CswConstants.RANGE_HEADER) String rangeValue)
      throws CswException {
    if (request == null) {
      throw new CswException("GetRecordByIdRequest request is null");
    }

    String outputFormat = request.getOutputFormat();
    String outputSchema = request.getOutputSchema();
    validator.validateOutputFormat(outputFormat, mimeTypeTransformerManager);
    validator.validateOutputSchema(outputSchema, schemaTransformerManager);

    List<String> ids = request.getId();
    if (!ids.isEmpty()) {
      String id = ids.get(0);
      // Check if the request wants to retrieve a product.
      if (isProductRetrieval(ids, outputFormat, outputSchema)) {
        LOGGER.debug("{} is attempting to retrieve product for: {}", request.getService(), id);
        try {
          return queryProductById(id, rangeValue);
        } catch (UnsupportedQueryException e) {
          throw new CswException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, id), e);
        }
      }

      LOGGER.debug("{} is attempting to retrieve records: {}", request.getService(), ids);
      CswRecordCollection response = queryById(ids, outputSchema);
      response.setOutputSchema(outputSchema);
      if (request.isSetElementSetName() && request.getElementSetName().getValue() != null) {
        response.setElementSetType(request.getElementSetName().getValue());
      } else {
        response.setElementSetType(ElementSetType.SUMMARY);
      }
      LOGGER.debug(
          "{} successfully retrieved record(s): {}", request.getService(), request.getId());

      return response;
    } else {
      throw new CswException(
          "A GetRecordById Query must contain an ID.", CswConstants.MISSING_PARAMETER_VALUE, "id");
    }
  }

  @Override
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public TransactionResponseType transaction(CswTransactionRequest request) throws CswException {
    if (request == null) {
      throw new CswException("TransactionRequest request is null");
    }

    TransactionResponseType response = new TransactionResponseType();
    TransactionSummaryType summary = new TransactionSummaryType();
    summary.setTotalInserted(BigInteger.valueOf(0));
    summary.setTotalUpdated(BigInteger.valueOf(0));
    summary.setTotalDeleted(BigInteger.valueOf(0));
    response.setTransactionSummary(summary);
    response.setVersion(CswConstants.VERSION_2_0_2);

    int numInserted = 0;
    for (InsertAction insertAction : request.getInsertActions()) {
      insertAction = transformInsertAction(insertAction);
      CreateRequest createRequest = new CreateRequestImpl(insertAction.getRecords());
      try {
        CreateResponse createResponse = framework.create(createRequest);
        if (request.isVerbose()) {
          response.getInsertResult().add(getInsertResultFromResponse(createResponse));
        }

        numInserted += createResponse.getCreatedMetacards().size();
      } catch (IngestException | SourceUnavailableException e) {
        LOGGER.debug("Unable to insert record(s)", e);
        throw new CswException(
            "Unable to insert record(s).",
            CswConstants.TRANSACTION_FAILED,
            insertAction.getHandle());
      }
    }
    LOGGER.debug("{} records inserted.", numInserted);
    response.getTransactionSummary().setTotalInserted(BigInteger.valueOf(numInserted));

    int numUpdated = 0;
    for (UpdateAction updateAction : request.getUpdateActions()) {
      try {
        numUpdated += updateRecords(updateAction);
      } catch (CswException
          | FederationException
          | IngestException
          | SourceUnavailableException
          | UnsupportedQueryException
          | CatalogQueryException e) {
        LOGGER.debug("Unable to update record(s)", e);
        throw new CswException(
            "Unable to update record(s).",
            CswConstants.TRANSACTION_FAILED,
            updateAction.getHandle());
      }
    }
    LOGGER.debug("{} records updated.", numUpdated);
    response.getTransactionSummary().setTotalUpdated(BigInteger.valueOf(numUpdated));

    int numDeleted = 0;
    for (DeleteAction deleteAction : request.getDeleteActions()) {
      try {
        numDeleted += deleteRecords(deleteAction);
      } catch (Exception e) {
        LOGGER.debug("Unable to delete record(s)", e);
        throw new CswException(
            "Unable to delete record(s).",
            CswConstants.TRANSACTION_FAILED,
            deleteAction.getHandle());
      }
    }
    LOGGER.debug("{} records deleted.", numDeleted);
    response.getTransactionSummary().setTotalDeleted(BigInteger.valueOf(numDeleted));

    return response;
  }

  private InsertResultType getInsertResultFromResponse(CreateResponse createResponse)
      throws CswException {
    InsertResultType result = new InsertResultType();
    WKTReader reader = new WKTReader();
    for (Metacard metacard : createResponse.getCreatedMetacards()) {
      BoundingBoxType boundingBox = new BoundingBoxType();
      Geometry geometry = null;
      String bbox = null;
      try {
        if (metacard.getAttribute(CswConstants.BBOX_PROP) != null) {
          bbox = metacard.getAttribute(CswConstants.BBOX_PROP).getValue().toString();
          geometry = reader.read(bbox);
        } else if (StringUtils.isNotBlank(metacard.getLocation())) {
          bbox = metacard.getLocation();
          geometry = reader.read(bbox);
        }
      } catch (ParseException e) {
        LOGGER.debug("Unable to parse BoundingBox : {}", bbox, e);
      }
      BriefRecordType briefRecordType = new BriefRecordType();
      if (geometry != null) {
        Envelope bounds = geometry.getEnvelopeInternal();
        if (bounds != null) {
          boundingBox.setCrs(CswConstants.SRS_NAME);
          boundingBox.setLowerCorner(Arrays.asList(bounds.getMinX(), bounds.getMinY()));
          boundingBox.setUpperCorner(Arrays.asList(bounds.getMaxX(), bounds.getMaxY()));
          briefRecordType
              .getBoundingBox()
              .add(new net.opengis.ows.v_1_0_0.ObjectFactory().createBoundingBox(boundingBox));
        }
      }
      SimpleLiteral identifier = new SimpleLiteral();
      identifier.getContent().add(metacard.getId());
      briefRecordType
          .getIdentifier()
          .add(
              new JAXBElement<>(CswConstants.DC_IDENTIFIER_QNAME, SimpleLiteral.class, identifier));
      SimpleLiteral title = new SimpleLiteral();
      title.getContent().add(metacard.getTitle());
      briefRecordType
          .getTitle()
          .add(new JAXBElement<>(CswConstants.DC_TITLE_QNAME, SimpleLiteral.class, title));
      SimpleLiteral type = new SimpleLiteral();
      type.getContent().add(metacard.getContentTypeName());
      briefRecordType.setType(type);
      result.getBriefRecord().add(briefRecordType);
    }
    return result;
  }

  private int deleteRecords(DeleteAction deleteAction)
      throws CswException, FederationException, IngestException, SourceUnavailableException,
          UnsupportedQueryException, InterruptedException, ParseException, CQLException {

    deleteAction = transformDeleteAction(deleteAction);

    QueryRequest queryRequest =
        queryFactory.getQuery(deleteAction.getConstraint(), deleteAction.getTypeName());

    queryRequest =
        queryFactory.updateQueryRequestTags(
            queryRequest,
            schemaTransformerManager.getTransformerSchemaForId(deleteAction.getTypeName()));

    int batchCount = 1;
    int deletedCount = 0;

    String[] idsToDelete = getNextQueryBatch(queryRequest);

    while (idsToDelete.length > 0) {
      DeleteRequestImpl deleteRequest = new DeleteRequestImpl(idsToDelete);
      LOGGER.debug(
          "Attempting to delete {} metacards from batch {}.", idsToDelete.length, ++batchCount);
      DeleteResponse deleteResponse = framework.delete(deleteRequest);
      deletedCount += deleteResponse.getDeletedMetacards().size();

      idsToDelete = getNextQueryBatch(queryRequest);
    }

    return deletedCount;
  }

  private String[] getNextQueryBatch(QueryRequest queryRequest) {
    return ResultIterable.resultIterable(framework, queryRequest, DEFAULT_BATCH)
        .stream()
        .filter(Objects::nonNull)
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .map(Metacard::getId)
        .distinct()
        .toArray(String[]::new);
  }

  private InsertAction transformInsertAction(InsertAction insertAction) {
    return cswActionTransformerProvider
        .getTransformer(insertAction.getTypeName())
        .map(tr -> tr.transform(insertAction))
        .orElse(insertAction);
  }

  private DeleteAction transformDeleteAction(DeleteAction deleteAction) {
    return cswActionTransformerProvider
        .getTransformer(deleteAction.getTypeName())
        .map(tr -> tr.transform(deleteAction))
        .orElse(deleteAction);
  }

  private UpdateAction transformUpdateAction(UpdateAction updateAction) {
    return cswActionTransformerProvider
        .getTransformer(updateAction.getTypeName())
        .map(tr -> tr.transform(updateAction))
        .orElse(updateAction);
  }

  private int updateRecords(UpdateAction updateAction)
      throws CswException, FederationException, IngestException, SourceUnavailableException,
          UnsupportedQueryException {

    updateAction = transformUpdateAction(updateAction);

    if (updateAction.getMetacard() != null) {
      Metacard newRecord = updateAction.getMetacard();

      if (newRecord.getId() != null) {
        UpdateRequest updateRequest = new UpdateRequestImpl(newRecord.getId(), newRecord);
        LOGGER.debug("Attempting to update {} ", newRecord.getId());
        UpdateResponse updateResponse = framework.update(updateRequest);
        return updateResponse.getUpdatedMetacards().size();
      } else {
        throw new CswException(
            "Unable to update record.  No ID was specified in the request.",
            CswConstants.MISSING_PARAMETER_VALUE,
            updateAction.getHandle());
      }
    } else if (updateAction.getConstraint() != null) {
      QueryConstraintType constraint = updateAction.getConstraint();
      QueryRequest queryRequest = queryFactory.getQuery(constraint, updateAction.getTypeName());

      queryRequest =
          queryFactory.updateQueryRequestTags(
              queryRequest,
              schemaTransformerManager.getTransformerSchemaForId(updateAction.getTypeName()));

      Map<String, Serializable> recordProperties = updateAction.getRecordProperties();
      Iterable<List<Result>> resultList =
          Iterables.partition(
              ResultIterable.resultIterable(framework, queryRequest), DEFAULT_BATCH);
      int batchCount = 1;
      int updatedCount = 0;

      for (List<Result> results : resultList) {
        updatedCount += updateResultList(recordProperties, batchCount++, results);
      }

      return updatedCount;
    }
    return 0;
  }

  private int updateResultList(
      Map<String, Serializable> recordProperties, int batchCount, List<Result> resultList)
      throws IngestException, SourceUnavailableException {
    List<String> updatedMetacardIdsList = new ArrayList<>();
    List<Metacard> updatedMetacards = new ArrayList<>();

    int updatedCount = 0;
    for (Result result : resultList) {
      Metacard metacard = result.getMetacard();

      if (metacard != null) {
        for (Entry<String, Serializable> recordProperty : recordProperties.entrySet()) {
          Attribute attribute =
              new AttributeImpl(recordProperty.getKey(), recordProperty.getValue());
          metacard.setAttribute(attribute);
        }
        updatedMetacardIdsList.add(metacard.getId());
        updatedMetacards.add(metacard);
      }
    }

    if (updatedMetacardIdsList.size() > 0) {
      String[] updatedMetacardIds = updatedMetacardIdsList.toArray(new String[0]);
      UpdateRequest updateRequest = new UpdateRequestImpl(updatedMetacardIds, updatedMetacards);

      LOGGER.debug(
          "Attempting to update {} metacards in batch {}.",
          updatedMetacardIdsList.size(),
          batchCount);
      UpdateResponse updateResponse = framework.update(updateRequest);
      updatedCount = updateResponse.getUpdatedMetacards().size();
    }

    return updatedCount;
  }

  @GET
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public void unknownService(@QueryParam("") CswRequest request) throws CswException {
    if (request.getService() == null) {
      throw new CswException(
          "Missing service value", CswConstants.MISSING_PARAMETER_VALUE, "service");
    }
    throw new CswException(
        "Unknown service (" + request.getService() + ")",
        CswConstants.INVALID_PARAMETER_VALUE,
        "service");
  }

  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public void unknownService() throws CswException {
    throw new CswException("Unknown Service", CswConstants.INVALID_PARAMETER_VALUE, "service");
  }

  @GET
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public void unknownOperation(@QueryParam("") CswRequest request) throws CswException {
    throw new CswException(
        "No such operation: " + request.getRequest(),
        CswConstants.OPERATION_NOT_SUPPORTED,
        request.getRequest());
  }

  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public void unknownOperation() throws CswException {
    throw new CswException("No such operation", CswConstants.OPERATION_NOT_SUPPORTED, null);
  }

  /**
   * Returns a list of QNames based on typeNames and namespaces given
   *
   * @param typeNames this can be a comma separated list of types which can be prefixed with
   *     prefixes. example csw:Record
   * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri example key=csw
   *     value=http://www.opengis.net/cat/csw/2.0.2
   * @return List of QNames so that types and namespaces are associated
   */
  private List<QName> typeStringToQNames(
      String typeNames, Map<String, String> namespacePrefixToUriMappings) throws CswException {
    List<QName> qNames = new ArrayList<>();
    if (typeNames == null) {
      return qNames;
    }

    String[] types = typeNames.split(CswConstants.COMMA);

    for (String typeName : types) {
      // if type name is in the format prefix:localPart (eg. csw:Record).
      if (typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) != -1) {
        String prefix = typeName.substring(0, typeName.indexOf(CswConstants.NAMESPACE_DELIMITER));
        String localPart =
            typeName.substring(typeName.indexOf(CswConstants.NAMESPACE_DELIMITER) + 1);
        QName qname =
            new QName(
                getNamespaceFromType(prefix, localPart, namespacePrefixToUriMappings),
                localPart,
                prefix);
        qNames.add(qname);
      } else {
        QName qname =
            new QName(getNamespaceFromType("", typeName, namespacePrefixToUriMappings), typeName);
        qNames.add(qname);
      }
    }
    return qNames;
  }

  /**
   * for a single type, or localName, this returns the corresponding namespace from the qualified
   * list of namespaces.
   *
   * @param typePrefix prefix to a typeName example csw is the prefix in the typeName csw:Record
   * @param type a single type that has already been split
   * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri example key=csw
   *     value=http://www.opengis.net/cat/csw/2.0.2
   * @return corresponding namespace for the given type
   */
  private String getNamespaceFromType(
      String typePrefix, String type, Map<String, String> namespacePrefixToUriMappings)
      throws CswException {
    if (namespacePrefixToUriMappings == null) {
      return "";
    }

    String namespaceUri = namespacePrefixToUriMappings.get(typePrefix);

    if (namespaceUri == null) {
      throw createUnknownTypeException(type);
    }
    return namespaceUri;
  }

  private DescribeRecordResponseType buildDescribeRecordResponseFromTypes(List<QName> types)
      throws CswException {

    validator.validateFullyQualifiedTypes(types);

    DescribeRecordResponseType response = new DescribeRecordResponseType();
    List<SchemaComponentType> schemas = new ArrayList<>();

    if (types.isEmpty()) {
      schemas.add(getSchemaComponentType(CswConstants.CSW_OUTPUT_SCHEMA));
      schemas.add(getSchemaComponentType(GmdConstants.GMD_NAMESPACE));
    } else {
      if (types.contains(
          new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CSW_RECORD_LOCAL_NAME))) {
        schemas.add(getSchemaComponentType(CswConstants.CSW_OUTPUT_SCHEMA));
      }

      if (types.contains(new QName(GmdConstants.GMD_NAMESPACE, GmdConstants.GMD_LOCAL_NAME))) {
        schemas.add(getSchemaComponentType(GmdConstants.GMD_NAMESPACE));
      }

      if (types.contains(
          new QName(CswConstants.EBRIM_SCHEMA, CswConstants.EBRIM_RECORD_LOCAL_NAME))) {
        schemas.add(getSchemaComponentType(CswConstants.EBRIM_SCHEMA));
      }
    }

    response.setSchemaComponent(schemas);
    return response;
  }

  private CswRecordCollection queryCsw(GetRecordsType request) throws CswException {
    if (LOGGER.isDebugEnabled()) {
      try {
        Writer writer = new StringWriter();
        try {
          Marshaller marshaller = CswQueryFactory.getJaxBContext().createMarshaller();
          marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

          JAXBElement<GetRecordsType> jaxbElement = new ObjectFactory().createGetRecords(request);
          marshaller.marshal(jaxbElement, writer);
        } catch (JAXBException e) {
          LOGGER.debug("Unable to marshall {} to XML.  Exception {}", GetRecordsType.class, e);
        }
        LOGGER.debug(writer.toString());
      } catch (Exception e) {
        LOGGER.debug("Unable to create debug message for getRecordsType: {}", e);
      }
    }

    QueryType query = (QueryType) request.getAbstractQuery().getValue();

    CswRecordCollection response = new CswRecordCollection();
    response.setRequest(request);
    response.setOutputSchema(request.getOutputSchema());
    response.setMimeType(request.getOutputFormat());
    response.setElementName(query.getElementName());
    response.setElementSetType(
        (query.getElementSetName() != null) ? query.getElementSetName().getValue() : null);
    response.setResultType(
        (ResultType) ObjectUtils.defaultIfNull(request.getResultType(), ResultType.HITS));

    if (ResultType.HITS.equals(request.getResultType())
        || ResultType.RESULTS.equals(request.getResultType())) {
      QueryRequest queryRequest = queryFactory.getQuery(request);
      try {
        queryRequest = queryFactory.updateQueryRequestTags(queryRequest, request.getOutputSchema());

        LOGGER.debug("Attempting to execute paged query: {}", queryRequest);
        AtomicLong hitCount = new AtomicLong(0);

        QueryFunction qf =
            qr -> {
              SourceResponse sr = framework.query(qr);
              hitCount.compareAndSet(0, sr.getHits());
              return sr;
            };

        ResultIterable results =
            ResultIterable.resultIterable(qf, queryRequest, request.getMaxRecords().intValue());
        List<Result> resultList = results.stream().collect(Collectors.toList());

        // The hitCount Atomic is used here instead of just defaulting
        // to the size of the resultList because the size of the resultList
        // can be limited to request.getMaxRecords().intValue() which would
        // lead to an incorrect response for hits.
        // hitCount is set within the QueryFunction and will correspond to
        // all responses.
        long totalHits = hitCount.get();
        totalHits = totalHits != 0 ? totalHits : resultList.size();

        QueryResponse queryResponse = new QueryResponseImpl(queryRequest, resultList, totalHits);

        response.setSourceResponse(queryResponse);
      } catch (UnsupportedQueryException | CatalogQueryException e) {
        LOGGER.debug("Unable to query", e);
        throw new CswException(e);
      }
    }
    return response;
  }

  private SchemaComponentType getSchemaComponentType(String outputSchema) throws CswException {

    SchemaComponentType schemaComponentType = new SchemaComponentType();
    List<Object> listOfObject = new ArrayList<>();

    if (outputSchema.equals(CswConstants.CSW_OUTPUT_SCHEMA)) {
      listOfObject.add(getDocElementFromResourcePath("csw/2.0.2/record.xsd"));
    } else if (outputSchema.equals(GmdConstants.GMD_NAMESPACE)) {
      listOfObject.add(getDocElementFromResourcePath("gmd/record_gmd.xsd"));
    } else if (outputSchema.equals(CswConstants.EBRIM_SCHEMA)) {
      listOfObject.add(getDocElementFromResourcePath("csw-ebrim.1.0.2/csw-ebrim.xsd"));
    }

    schemaComponentType.setContent(listOfObject);
    schemaComponentType.setSchemaLanguage(CswConstants.XML_SCHEMA_LANGUAGE);
    schemaComponentType.setTargetNamespace(outputSchema);
    return schemaComponentType;
  }

  private Element getDocElementFromResourcePath(String resourcePath) throws CswException {
    Element element = documentElements.get(resourcePath);
    if (element == null) {
      element = loadDocElementFromResourcePath(resourcePath);
      documentElements.put(resourcePath, element);
    }
    return element;
  }

  private Element loadDocElementFromResourcePath(String resourcePath) throws CswException {
    URL recordUrl = getBundle().getResource(resourcePath);

    if (recordUrl == null) {
      /* Using DescribeRecordType since that is the bundle where other csw resources live */
      recordUrl =
          Optional.of(FrameworkUtil.getBundle(DescribeRecordType.class))
              .map(b -> b.getResource(resourcePath))
              .orElse(null);
      if (recordUrl /*still*/ == null) {
        throw new CswException("Cannot find the resource: " + resourcePath);
      }
    }

    Document doc;
    try {
      DocumentBuilder docBuilder = XML_UTILS.getSecureDocumentBuilder(true);
      doc = docBuilder.parse(recordUrl.openStream());
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new CswException(e);
    }

    if (doc == null) {
      throw new CswException(
          "Document was NULL in attempting to parse from resource path '" + resourcePath + "'");
    }
    return doc.getDocumentElement();
  }

  /**
   * Creates a CapabilitiesType object with only specified sections to be returned as a
   * GetCapabilities response.
   *
   * @param sections The list of desired sections for the GetCapabilities response
   * @return The constructed CapabilitiesType object, containing only the user-specified sections
   */
  private CapabilitiesType buildCapabilitiesType(List<String> sections) {

    // If no sections are specified, return them all
    if (sections == null || sections.size() == 0) {
      return capabilitiesType;
    }

    CapabilitiesType cswCapabilities = new CapabilitiesType();
    cswCapabilities.setVersion(capabilitiesType.getVersion());

    // Grab the desired sections from the global capabilitiesType variable
    for (String section : sections) {
      if (section.equalsIgnoreCase(SERVICE_IDENTIFICATION)) {
        cswCapabilities.setServiceIdentification(capabilitiesType.getServiceIdentification());
      } else if (section.equalsIgnoreCase(SERVICE_PROVIDER)) {
        cswCapabilities.setServiceProvider(capabilitiesType.getServiceProvider());
      } else if (section.equalsIgnoreCase(OPERATIONS_METADATA)) {
        cswCapabilities.setOperationsMetadata(capabilitiesType.getOperationsMetadata());
      }
    }

    // filterCapabilities is required.  Add it even if it isn't in the sections list.
    cswCapabilities.setFilterCapabilities(capabilitiesType.getFilterCapabilities());

    return cswCapabilities;
  }

  /**
   * Creates a full CapabilitiesType element to be returned as the GetCapabilities response
   *
   * @return The constructed CapabilitiesType object
   */
  private CapabilitiesType buildCapabilitiesType() {
    if (capabilitiesType == null) {
      CapabilitiesType cswCapabilities = new CapabilitiesType();
      cswCapabilities.setVersion(CswConstants.VERSION_2_0_2);
      cswCapabilities.setServiceIdentification(buildServiceIdentification());
      cswCapabilities.setServiceProvider(buildServiceProvider());
      cswCapabilities.setOperationsMetadata(buildOperationsMetadata());
      cswCapabilities.setFilterCapabilities(buildFilterCapabilities());
      return cswCapabilities;
    } else {
      capabilitiesType.setOperationsMetadata(buildOperationsMetadata());
      return capabilitiesType;
    }
  }

  /**
   * Creates the Filter_Capabilities section of the GetCapabilities response. These are defined
   * statically by the DDF FilterAdapter implementation TODO: If the implementation changes, update
   * this method to reflect the changes.
   *
   * @return The constructed FilterCapabilities object
   */
  private FilterCapabilities buildFilterCapabilities() {
    // Create the FilterCapabilites - These are defined statically by the
    // DDF FilterAdapter implementation
    FilterCapabilities filterCapabilities = new FilterCapabilities();
    ScalarCapabilitiesType scalarCapabilities = new ScalarCapabilitiesType();
    ComparisonOperatorsType cot = new ComparisonOperatorsType();
    cot.setComparisonOperator(COMPARISON_OPERATORS);
    scalarCapabilities.setLogicalOperators(new LogicalOperators());
    scalarCapabilities.setComparisonOperators(cot);
    filterCapabilities.setScalarCapabilities(scalarCapabilities);

    SpatialOperatorsType spatialOpsType = new SpatialOperatorsType();
    ArrayList<SpatialOperatorType> spatialOpTypes = new ArrayList<>();
    for (SpatialOperatorNameType sont : SPATIAL_OPERATORS) {
      SpatialOperatorType sot = new SpatialOperatorType();
      sot.setName(sont);
      spatialOpTypes.add(sot);
    }
    GeometryOperandsType geometryOperands = new GeometryOperandsType();
    List<QName> geoOperandsList = geometryOperands.getGeometryOperand();

    geoOperandsList.add(
        new QName(
            CswConstants.GML_SCHEMA, CswConstants.GML_POINT, CswConstants.GML_NAMESPACE_PREFIX));
    geoOperandsList.add(
        new QName(
            CswConstants.GML_SCHEMA,
            CswConstants.GML_LINESTRING,
            CswConstants.GML_NAMESPACE_PREFIX));
    geoOperandsList.add(
        new QName(
            CswConstants.GML_SCHEMA, CswConstants.GML_POLYGON, CswConstants.GML_NAMESPACE_PREFIX));

    spatialOpsType.setSpatialOperator(spatialOpTypes);
    SpatialCapabilitiesType spatialCaps = new SpatialCapabilitiesType();
    spatialCaps.setSpatialOperators(spatialOpsType);
    spatialCaps.setGeometryOperands(geometryOperands);
    filterCapabilities.setSpatialCapabilities(spatialCaps);

    IdCapabilitiesType idCapabilities = new IdCapabilitiesType();
    idCapabilities.getEIDOrFID().add(new EID());

    filterCapabilities.setIdCapabilities(idCapabilities);

    return filterCapabilities;
  }

  private List<String> getQueryFilterTransformerTypeNames() {
    return Stream.of(getBundle())
        .map(Bundle::getBundleContext)
        .map(this::getQueryFilterTransformerServices)
        .flatMap(Collection::stream)
        .map(
            queryFilterTransformerServiceReference ->
                queryFilterTransformerServiceReference.getProperty(
                    QueryFilterTransformer.QUERY_FILTER_TRANSFORMER_TYPE_NAMES_FIELD))
        .filter(Objects::nonNull)
        .filter(List.class::isInstance)
        .map(List.class::cast)
        .flatMap((Function<List, Stream<Object>>) Collection::stream)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .collect(Collectors.toList());
  }

  private Collection<ServiceReference<QueryFilterTransformer>> getQueryFilterTransformerServices(
      BundleContext bundleContext) {
    try {
      return bundleContext.getServiceReferences(QueryFilterTransformer.class, null);
    } catch (InvalidSyntaxException e) {
      LOGGER.debug("Unable to get service references for QueryFilterTransformers.", e);
    }
    return Collections.emptyList();
  }

  private List<String> getTypeNames() {
    return Stream.of(getQueryFilterTransformerTypeNames())
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Creates the OperationsMetadata portion of the GetCapabilities response TODO: As these
   * operations are implemented or added, update their descriptions to ensure they match up with the
   * functionality
   *
   * @return The constructed OperationsMetadata object
   */
  private OperationsMetadata buildOperationsMetadata() {

    List<String> typeNames = getTypeNames();

    OperationsMetadata om = new OperationsMetadata();

    List<QName> getAndPost = Arrays.asList(CswConstants.GET, CswConstants.POST);

    // Builds GetCapabilities operation metadata
    Operation getCapabilitiesOp = buildOperation(CswConstants.GET_CAPABILITIES, getAndPost);
    addOperationParameter("sections", GET_CAPABILITIES_PARAMS, getCapabilitiesOp);

    // Builds DescribeRecord operation metadata
    Operation describeRecordOp = buildOperation(CswConstants.DESCRIBE_RECORD, getAndPost);
    addOperationParameter(CswConstants.TYPE_NAME_PARAMETER, typeNames, describeRecordOp);
    Set<String> mimeTypeSet = new HashSet<>();
    mimeTypeSet.add(DEFAULT_OUTPUT_FORMAT);
    mimeTypeSet.addAll(mimeTypeTransformerManager.getAvailableMimeTypes());
    List<String> mimeTypes = new ArrayList<>(mimeTypeSet);
    addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, describeRecordOp);
    addOperationParameter("schemaLanguage", CswConstants.VALID_SCHEMA_LANGUAGES, describeRecordOp);

    // Builds GetRecords operation metadata
    Operation getRecordsOp = buildOperation(CswConstants.GET_RECORDS, getAndPost);
    addOperationParameter(
        CswConstants.RESULT_TYPE_PARAMETER,
        Arrays.asList("hits", "results", "validate"),
        getRecordsOp);
    addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordsOp);
    addOperationParameter(
        CswConstants.OUTPUT_SCHEMA_PARAMETER,
        schemaTransformerManager.getAvailableSchemas(),
        getRecordsOp);
    addOperationParameter(CswConstants.TYPE_NAMES_PARAMETER, typeNames, getRecordsOp);
    addOperationParameter(
        CswConstants.CONSTRAINT_LANGUAGE_PARAMETER,
        CswConstants.CONSTRAINT_LANGUAGES,
        getRecordsOp);
    addFederatedCatalogs(getRecordsOp);

    // Builds GetRecordById operation metadata
    mimeTypes.add(MediaType.APPLICATION_OCTET_STREAM);
    List<String> supportedSchemas = schemaTransformerManager.getAvailableSchemas();
    supportedSchemas.add(OCTET_STREAM_OUTPUT_SCHEMA);
    Operation getRecordByIdOp = buildOperation(CswConstants.GET_RECORD_BY_ID, getAndPost);
    addOperationParameter(CswConstants.OUTPUT_SCHEMA_PARAMETER, supportedSchemas, getRecordByIdOp);
    addOperationParameter(CswConstants.OUTPUT_FORMAT_PARAMETER, mimeTypes, getRecordByIdOp);
    addOperationParameter(
        CswConstants.RESULT_TYPE_PARAMETER,
        Arrays.asList("hits", "results", "validate"),
        getRecordByIdOp);
    addOperationParameter(CswConstants.ELEMENT_SET_NAME_PARAMETER, ELEMENT_NAMES, getRecordByIdOp);

    // Builds Transactions operation metadata
    Operation transactionOp =
        buildOperation(CswConstants.TRANSACTION, Collections.singletonList(CswConstants.POST));
    addOperationParameter(
        CswConstants.TYPE_NAMES_PARAMETER,
        inputTransformerManager.getAvailableIds(),
        transactionOp);
    addOperationParameter(
        CswConstants.CONSTRAINT_LANGUAGE_PARAMETER,
        CswConstants.CONSTRAINT_LANGUAGES,
        transactionOp);

    List<Operation> ops =
        Arrays.asList(
            getCapabilitiesOp, describeRecordOp, getRecordsOp, getRecordByIdOp, transactionOp);
    om.setOperation(ops);

    om.getParameter().add(createDomainType(CswConstants.SERVICE, CswConstants.CSW));
    om.getParameter().add(createDomainType(CswConstants.VERSION, CswConstants.VERSION_2_0_2));

    return om;
  }

  /**
   * Creates the ServiceIdentification portion of the GetCapabilities response TODO: Add more
   * DDF-specific information if desired (Fees, Keywords, and AccessConstraints are all currently
   * empty, and the abstract is very basic)
   *
   * @return The constructed ServiceIdentification object
   */
  private ServiceIdentification buildServiceIdentification() {

    ServiceIdentification si = new ServiceIdentification();
    si.setTitle(SERVICE_TITLE);
    si.setServiceTypeVersion(SERVICE_TYPE_VERSION);
    CodeType type = new CodeType();
    type.setValue(CswConstants.CSW);
    si.setServiceType(type);
    si.setAbstract(SERVICE_ABSTRACT);
    return si;
  }

  /**
   * Creates the ServiceProvider portion of the GetCapabilities response TODO: Add more DDF-specific
   * information if desired
   *
   * @return The constructed ServiceProvider object
   */
  private ServiceProvider buildServiceProvider() {
    ServiceProvider sp = new ServiceProvider();
    sp.setProviderName(PROVIDER_NAME);
    sp.setProviderSite(new OnlineResourceType());
    sp.setServiceContact(new ResponsiblePartySubsetType());
    return sp;
  }

  /**
   * Creates an Operation object for the OperationsMetadata section TODO: We currently don't use the
   * constraint or metadata elements, those can be added in as desired
   *
   * @param name The name of the operation
   * @param types The request types supported (GET/POST)
   * @return The constructed Operation object
   */
  private Operation buildOperation(String name, List<QName> types) {
    Operation op = new Operation();

    op.setName(name);
    ArrayList<DCP> dcpList = new ArrayList<>();
    DCP dcp = new DCP();
    HTTP http = new HTTP();
    for (QName type : types) {
      RequestMethodType rmt = new RequestMethodType();
      rmt.setHref(uri.getBaseUri().toASCIIString());
      JAXBElement<RequestMethodType> requestElement =
          new JAXBElement<>(type, RequestMethodType.class, rmt);
      if (type.equals(CswConstants.POST)) {
        requestElement
            .getValue()
            .getConstraint()
            .add(createDomainType(CswConstants.POST_ENCODING, CswConstants.XML));
      }
      http.getGetOrPost().add(requestElement);
    }
    dcp.setHTTP(http);
    dcpList.add(dcp);
    op.setDCP(dcpList);
    return op;
  }

  private void addOperationParameter(String name, List<String> params, Operation op) {
    DomainType dt = createDomainType(name, params);
    op.getParameter().add(dt);
  }

  private DomainType createDomainType(String name, String value) {
    return createDomainType(name, Collections.singletonList(value));
  }

  private DomainType createDomainType(String name, List<String> params) {
    DomainType dt = new DomainType();
    dt.setName(name);
    dt.setValue(params);
    return dt;
  }

  private void addFederatedCatalogs(Operation operation) {
    List<String> sourceIds = new ArrayList<>(framework.getSourceIds());
    sourceIds.remove(framework.getId());
    operation.getConstraint().add(createDomainType(CswConstants.FEDERATED_CATALOGS, sourceIds));
  }

  private CswException createUnknownTypeException(final String type) {
    return new CswException(
        "The type '" + type + "' is not known to this service.",
        CswConstants.INVALID_PARAMETER_VALUE,
        null);
  }

  private CswRecordCollection queryById(List<String> ids, String outputSchema) throws CswException {
    QueryRequest queryRequest = queryFactory.getQueryById(ids);
    try {
      CswRecordCollection response = new CswRecordCollection();
      response.setById(true);
      queryRequest = queryFactory.updateQueryRequestTags(queryRequest, outputSchema);
      QueryResponse queryResponse = framework.query(queryRequest);
      response.setSourceResponse(queryResponse);

      List<Metacard> metacards = new LinkedList<>();
      for (Result result : queryResponse.getResults()) {
        metacards.add(result.getMetacard());
      }
      response.setCswRecords(metacards);

      return response;
    } catch (FederationException | SourceUnavailableException | UnsupportedQueryException e) {
      throw new CswException(e);
    }
  }

  private boolean isProductRetrieval(List<String> ids, String outputFormat, String outputSchema)
      throws CswException {
    if (outputSchema.equals(OCTET_STREAM_OUTPUT_SCHEMA)
        && outputFormat.equals(MediaType.APPLICATION_OCTET_STREAM)) {
      if (ids.size() == 1) {
        return true;
      } else {
        throw new CswException(ERROR_MULTI_PRODUCT_RETRIEVAL);
      }
    } else if ((outputSchema.equals(OCTET_STREAM_OUTPUT_SCHEMA)
            && !outputFormat.equals(MediaType.APPLICATION_OCTET_STREAM))
        || (!outputSchema.equals(OCTET_STREAM_OUTPUT_SCHEMA)
            && outputFormat.equals(MediaType.APPLICATION_OCTET_STREAM))) {
      throw new CswException(
          String.format(
              ERROR_SCHEMA_FORMAT_PRODUCT_RETRIEVAL,
              MediaType.APPLICATION_OCTET_STREAM,
              OCTET_STREAM_OUTPUT_SCHEMA));
    }
    return false;
  }

  private CswRecordCollection queryProductById(String id, String rangeValue)
      throws CswException, UnsupportedQueryException {

    final ResourceRequestById resourceRequest = new ResourceRequestById(id);

    long bytesToSkip = getRange(rangeValue);
    if (bytesToSkip > 0) {
      LOGGER.debug("Bytes to skip: {}", bytesToSkip);
      resourceRequest.getProperties().put(CswConstants.BYTES_TO_SKIP, bytesToSkip);
    }
    ResourceResponse resourceResponse;
    try {
      resourceResponse = framework.getLocalResource(resourceRequest);
    } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
      throw new CswException(String.format(ERROR_ID_PRODUCT_RETRIEVAL, id), e);
    }

    Resource resource = resourceResponse.getResource();
    MimeType mimeType = resource.getMimeType();
    if (mimeType == null) {
      try {
        mimeType = new MimeType(MediaType.APPLICATION_OCTET_STREAM);
        resource = new ResourceImpl(resource.getInputStream(), mimeType, resource.getName());
      } catch (MimeTypeParseException e) {
        throw new CswException(
            String.format(
                "Could not create mime type upon null mimeType, for mime %s.",
                MediaType.APPLICATION_OCTET_STREAM),
            e);
      }
    }
    CswRecordCollection cswRecordCollection = new CswRecordCollection();
    cswRecordCollection.setResource(resource);
    cswRecordCollection.setOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA);
    LOGGER.debug("Successfully retrieved product for ID: {}", id);
    return cswRecordCollection;
  }

  protected long getRange(String rangeValue) throws UnsupportedQueryException {
    long response = -1;

    if (StringUtils.isBlank(rangeValue)) {
      return response;
    }
    if (rangeValue.startsWith(CswConstants.BYTES_EQUAL)) {
      String tempString = rangeValue.substring(CswConstants.BYTES_EQUAL.length());
      List<String> range = Splitter.on('-').splitToList(tempString);
      if (!range.isEmpty() && range.size() <= 2 && StringUtils.isNumeric(range.get(0))) {
        return Long.parseLong(range.get(0));
      }
    }
    throw new UnsupportedQueryException(String.format("Invalid range header: %s", rangeValue));
  }

  /* For unit testing */
  void setUri(UriInfo uri) {
    this.uri = uri;
  }

  Bundle getBundle() {
    return FrameworkUtil.getBundle(this.getClass());
  }
}
