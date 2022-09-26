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

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import com.thoughtworks.xstream.security.NoTypePermission;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.MetacardType;
import ddf.catalog.resource.Resource;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionType;
import net.opengis.ows.v_1_0_0.ExceptionReport;
import net.opengis.ows.v_1_0_0.ExceptionType;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParserConfigurator;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.TransactionRequestConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswServlet.class);

  private static final List<String> XML_MIME_TYPES = List.of("application/xml", "text/xml");

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private final CswEndpoint endpoint;

  private final Parser xmlParser;

  private final TransformerManager transformerManager;

  private ParserConfigurator marshallConfig;

  private ParserConfigurator unmarshallConfig;

  private final Converter cswRecordConverter;

  private final MetacardType metacardType;

  private final AttributeRegistry registry;

  public CswServlet(
      CswEndpoint cswEndpoint,
      TransformerManager transformerManager,
      Parser parser,
      Converter converter,
      MetacardType metacardType,
      AttributeRegistry registry) {
    this.endpoint = cswEndpoint;
    this.transformerManager = transformerManager;
    this.xmlParser = parser;
    this.cswRecordConverter = converter;
    this.metacardType = metacardType;
    this.registry = registry;

    initializeParserConfig();
  }

  private void initializeParserConfig() {
    NamespacePrefixMapper mapper =
        new NamespacePrefixMapper() {

          private final Map<String, String> prefixMap =
              Map.of(
                  CswConstants.CSW_OUTPUT_SCHEMA,
                  CswConstants.CSW_NAMESPACE_PREFIX,
                  CswConstants.OWS_NAMESPACE,
                  CswConstants.OWS_NAMESPACE_PREFIX,
                  CswConstants.XML_SCHEMA_LANGUAGE,
                  CswConstants.XML_SCHEMA_NAMESPACE_PREFIX,
                  CswConstants.OGC_SCHEMA,
                  CswConstants.OGC_NAMESPACE_PREFIX,
                  CswConstants.GML_SCHEMA,
                  CswConstants.GML_NAMESPACE_PREFIX,
                  CswConstants.DUBLIN_CORE_SCHEMA,
                  CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX,
                  CswConstants.DUBLIN_CORE_TERMS_SCHEMA,
                  CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
                  GmdConstants.GMD_NAMESPACE,
                  GmdConstants.GMD_PREFIX);

          @Override
          public String getPreferredPrefix(
              String namespaceUri, String suggestion, boolean requirePrefix) {
            return prefixMap.get(namespaceUri);
          }
        };

    marshallConfig = new XmlParserConfigurator();
    marshallConfig.setClassLoader(ObjectFactory.class.getClassLoader());
    marshallConfig.setContextPath(
        List.of(
            CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE,
            CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE));
    marshallConfig.addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshallConfig.addProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);

    unmarshallConfig = new XmlParserConfigurator();
    unmarshallConfig.setClassLoader(ObjectFactory.class.getClassLoader());
    unmarshallConfig.setContextPath(
        List.of(
            CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE,
            CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    try {
      String service = req.getParameter("service");
      if (service == null) {
        throw new CswException(
            "Missing service value", CswConstants.MISSING_PARAMETER_VALUE, "service");
      }
      if (!"csw".equalsIgnoreCase(service)) {
        throw new CswException(
            "Unknown service (" + service + ")", CswConstants.INVALID_PARAMETER_VALUE, "service");
      }

      String version = req.getParameter("version");
      if (version != null && !version.contains("2.0.2")) {
        throw new CswException(
            "Version(s) ("
                + version
                + ") is not supported, we currently support version "
                + CswConstants.VERSION_2_0_2,
            CswConstants.VERSION_NEGOTIATION_FAILED,
            null);
      }

      String request = req.getParameter("request");
      if ("getcapabilities".equalsIgnoreCase(request)) {
        GetCapabilitiesRequest getCapabilitiesRequest = new GetCapabilitiesRequest();
        getCapabilitiesRequest.setAcceptVersions(version);
        getCapabilitiesRequest.setSections(req.getParameter("sections"));
        getCapabilitiesRequest.setUpdateSequence(req.getParameter("updateSequence"));
        getCapabilitiesRequest.setAcceptFormats(req.getParameter("acceptFormats"));

        CapabilitiesType getCapabilitiesResponse = endpoint.getCapabilities(getCapabilitiesRequest);

        JAXBElement<CapabilitiesType> jaxbElement =
            new ObjectFactory().createCapabilities(getCapabilitiesResponse);
        xmlParser.marshal(marshallConfig, jaxbElement, resp.getOutputStream());
      } else if ("describerecord".equalsIgnoreCase(request)) {
        DescribeRecordRequest describeRecordRequest = new DescribeRecordRequest();
        describeRecordRequest.setVersion(version);
        describeRecordRequest.setNamespace(req.getParameter("namespace"));
        describeRecordRequest.setTypeName(req.getParameter("typeName"));
        describeRecordRequest.setOutputFormat(req.getParameter("outputFormat"));
        describeRecordRequest.setSchemaLanguage(req.getParameter("schemaLanguage"));

        DescribeRecordResponseType describeRecordResponse =
            endpoint.describeRecord(describeRecordRequest);

        JAXBElement<DescribeRecordResponseType> jaxbElement =
            new ObjectFactory().createDescribeRecordResponse(describeRecordResponse);
        xmlParser.marshal(marshallConfig, jaxbElement, resp.getOutputStream());
      } else if ("getrecords".equalsIgnoreCase(request)) {
        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
        getRecordsRequest.setVersion(version);
        getRecordsRequest.setRequestId(req.getParameter("requestId"));
        getRecordsRequest.setNamespace(req.getParameter("namespace"));
        getRecordsRequest.setResultType(req.getParameter("resultType"));
        getRecordsRequest.setOutputFormat(req.getParameter("outputFormat"));
        getRecordsRequest.setOutputSchema(req.getParameter("outputSchema"));
        if (req.getParameter("startPosition") != null) {
          getRecordsRequest.setStartPosition(new BigInteger(req.getParameter("startPosition")));
        }
        if (req.getParameter("maxRecords") != null) {
          getRecordsRequest.setMaxRecords(new BigInteger(req.getParameter("maxRecords")));
        }
        getRecordsRequest.setTypeNames(req.getParameter("typeNames"));
        getRecordsRequest.setElementName(req.getParameter("elementName"));
        getRecordsRequest.setElementSetName(req.getParameter("elementSetName"));
        getRecordsRequest.setConstraintLanguage(req.getParameter("constraintLanguage"));
        getRecordsRequest.setConstraint(req.getParameter("constraint"));
        getRecordsRequest.setSortBy(req.getParameter("sortBy"));
        if (req.getParameter("distributedSearch") != null) {
          getRecordsRequest.setDistributedSearch(
              Boolean.getBoolean(req.getParameter("distributedSearch")));
        }
        if (req.getParameter("hopCount") != null) {
          getRecordsRequest.setHopCount(new BigInteger(req.getParameter("hopCount")));
        }
        getRecordsRequest.setResponseHandler(req.getParameter("responseHandler"));

        CswRecordCollection getRecordByIdResponse = endpoint.getRecords(getRecordsRequest);

        writeRecords(getRecordByIdResponse, resp);
      } else if ("getrecordbyid".equalsIgnoreCase(request)) {
        GetRecordByIdRequest getRecordByIdRequest = new GetRecordByIdRequest();
        getRecordByIdRequest.setId(req.getParameter("id"));
        getRecordByIdRequest.setOutputFormat(req.getParameter("outputFormat"));
        getRecordByIdRequest.setOutputSchema(req.getParameter("outputSchema"));
        getRecordByIdRequest.setElementSetName(req.getParameter("elementSetName"));

        CswRecordCollection getRecordByIdResponse =
            endpoint.getRecordById(getRecordByIdRequest, req.getHeader(CswConstants.RANGE_HEADER));

        writeRecords(getRecordByIdResponse, resp);
      } else {
        throw new CswException(
            "Unknown request (" + request + ") for service (" + service + ")",
            CswConstants.INVALID_PARAMETER_VALUE,
            "request");
      }
    } catch (CswException | ParserException | CatalogTransformerException e) {
      sendException(e, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try (TemporaryFileBackedOutputStream requestStream = new TemporaryFileBackedOutputStream()) {
      req.getInputStream().transferTo(requestStream);
      requestStream.flush();
      JAXBElement request;
      try (InputStream jaxbStream = requestStream.asByteSource().openBufferedStream()) {
        request = xmlParser.unmarshal(unmarshallConfig, JAXBElement.class, jaxbStream);
      }
      if (GetCapabilitiesType.class.equals(request.getDeclaredType())) {
        GetCapabilitiesType getCapabilitiesRequest = (GetCapabilitiesType) request.getValue();

        CapabilitiesType getCapabilitiesResponse = endpoint.getCapabilities(getCapabilitiesRequest);

        JAXBElement<CapabilitiesType> jaxbElement =
            new ObjectFactory().createCapabilities(getCapabilitiesResponse);
        xmlParser.marshal(marshallConfig, jaxbElement, resp.getOutputStream());
      } else if (DescribeRecordType.class.equals(request.getDeclaredType())) {
        DescribeRecordType describeRecordRequest = (DescribeRecordType) request.getValue();

        DescribeRecordResponseType describeRecordResponse =
            endpoint.describeRecord(describeRecordRequest);

        JAXBElement<DescribeRecordResponseType> jaxbElement =
            new ObjectFactory().createDescribeRecordResponse(describeRecordResponse);
        xmlParser.marshal(marshallConfig, jaxbElement, resp.getOutputStream());
      } else if (GetRecordsType.class.equals(request.getDeclaredType())) {
        GetRecordsType getRecordsRequest = (GetRecordsType) request.getValue();

        CswRecordCollection getRecordByIdResponse = endpoint.getRecords(getRecordsRequest);

        writeRecords(getRecordByIdResponse, resp);
      } else if (GetRecordByIdType.class.equals(request.getDeclaredType())) {
        GetRecordByIdType getRecordByIdRequest = (GetRecordByIdType) request.getValue();

        CswRecordCollection getRecordByIdResponse =
            endpoint.getRecordById(getRecordByIdRequest, req.getHeader(CswConstants.RANGE_HEADER));

        writeRecords(getRecordByIdResponse, resp);
      } else if (TransactionType.class.equals(request.getDeclaredType())) {
        CswTransactionRequest cswTransactionRequest;
        try (InputStream xstreamStream = requestStream.asByteSource().openBufferedStream()) {
          cswTransactionRequest = unmarshallTransaction(xstreamStream);
        }

        TransactionResponseType transactionResponse = endpoint.transaction(cswTransactionRequest);

        JAXBElement<TransactionResponseType> jaxbElement =
            new ObjectFactory().createTransactionResponse(transactionResponse);
        xmlParser.marshal(marshallConfig, jaxbElement, resp.getOutputStream());
      } else {
        throw new CswException(
            "Unknown request type for csw service",
            CswConstants.INVALID_PARAMETER_VALUE,
            "request");
      }
    } catch (CswException | ParserException | CatalogTransformerException e) {
      sendException(e, resp);
    }
  }

  private CswTransactionRequest unmarshallTransaction(InputStream inputStream) {
    XStream xStream = new XStream(new Xpp3Driver(new NoNameCoder()));
    xStream.addPermission(NoTypePermission.NONE);
    TransactionRequestConverter transactionRequestConverter =
        new TransactionRequestConverter(cswRecordConverter, registry);
    transactionRequestConverter.setCswRecordConverter(new CswRecordConverter(metacardType));
    xStream.registerConverter(transactionRequestConverter);
    xStream.allowTypeHierarchy(CswTransactionRequest.class);
    xStream.alias("csw:" + CswConstants.TRANSACTION, CswTransactionRequest.class);
    xStream.alias(CswConstants.TRANSACTION, CswTransactionRequest.class);
    return (CswTransactionRequest) xStream.fromXML(inputStream);
  }

  public void writeRecords(CswRecordCollection recordCollection, HttpServletResponse response)
      throws CatalogTransformerException, IOException {

    final String mimeType = recordCollection.getMimeType();
    LOGGER.debug(
        "Attempting to transform RecordCollection with mime-type: {} & outputSchema: {}",
        mimeType,
        recordCollection.getOutputSchema());
    QueryResponseTransformer transformer;
    Map<String, Serializable> arguments = new HashMap<>();
    if (org.apache.commons.lang.StringUtils.isBlank(recordCollection.getOutputSchema())
        && org.apache.commons.lang.StringUtils.isNotBlank(mimeType)
        && !XML_MIME_TYPES.contains(mimeType)) {
      transformer = transformerManager.getTransformerByMimeType(mimeType);
    } else if (OCTET_STREAM_OUTPUT_SCHEMA.equals(recordCollection.getOutputSchema())) {
      Resource resource = recordCollection.getResource();
      response.addHeader("Content-Type", resource.getMimeType().toString());
      response.addHeader(
          "Content-Disposition", String.format("inline; filename=\"%s\"", resource.getName()));
      // Custom HTTP header to represent that the product data will be returned in the response.
      response.addHeader(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, "true");
      // Accept-ranges header to represent that ranges in bytes are accepted.
      response.addHeader(CswConstants.ACCEPT_RANGES_HEADER, CswConstants.BYTES);
      ByteArrayInputStream in = new ByteArrayInputStream(resource.getByteArray());
      in.transferTo(response.getOutputStream());
      return;
    } else {
      transformer = transformerManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA);
      if (recordCollection.getElementName() != null) {
        arguments.put(CswConstants.ELEMENT_NAMES, recordCollection.getElementName().toArray());
      }
      arguments.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, recordCollection.getOutputSchema());
      arguments.put(CswConstants.ELEMENT_SET_TYPE, recordCollection.getElementSetType());
      arguments.put(CswConstants.IS_BY_ID_QUERY, recordCollection.isById());
      arguments.put(CswConstants.GET_RECORDS, recordCollection.getRequest());
      arguments.put(CswConstants.RESULT_TYPE_PARAMETER, recordCollection.getResultType());
      arguments.put(CswConstants.WRITE_NAMESPACES, false);
    }

    if (transformer == null) {
      throw new CatalogTransformerException("Unable to locate Transformer.");
    }

    BinaryContent content = transformer.transform(recordCollection.getSourceResponse(), arguments);

    if (content != null) {
      try (InputStream inputStream = content.getInputStream()) {
        inputStream.transferTo(response.getOutputStream());
      }
    } else {
      throw new CatalogTransformerException("Transformer returned null.");
    }
  }

  private void sendException(Throwable exception, HttpServletResponse response) throws IOException {
    CswException cswException;
    if (exception instanceof CswException) {
      cswException = (CswException) exception;
    } else {
      String message = exception.getMessage();
      if (StringUtils.isBlank(message)) {
        cswException =
            new CswException(
                "Error parsing the request.  XML parameters may be missing or invalid.", exception);
        cswException.setExceptionCode(CswConstants.MISSING_PARAMETER_VALUE);
      } else {
        cswException = new CswException("Error handling request: " + message, exception);
        cswException.setExceptionCode(CswConstants.NO_APPLICABLE_CODE);
      }
    }
    LOGGER.debug("Error in CSW processing", cswException);

    response.setStatus(cswException.getHttpStatus());
    response.setContentType("text/xml");

    ExceptionReport exceptionReport = createServiceException(cswException);
    try {
      xmlParser.marshal(marshallConfig, exceptionReport, response.getOutputStream());
    } catch (ParserException e) {
      response.setStatus(500);
      LOGGER.warn("Unable to write out CSW ExceptionReport", e);
    }
  }

  private ExceptionReport createServiceException(CswException cswException) {
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setVersion("1.2.0");
    ExceptionType exception = new ExceptionType();
    exception.setExceptionCode(cswException.getExceptionCode());
    exception.setLocator(cswException.getLocator());
    exception.getExceptionText().add(cswException.getMessage());
    exceptionReport.getException().add(exception);
    return exceptionReport;
  }
}
