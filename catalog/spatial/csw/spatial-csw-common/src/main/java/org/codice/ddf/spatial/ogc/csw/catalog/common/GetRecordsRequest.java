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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.io.StringReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import net.opengis.cat.csw.v_2_0_2.DistributedSearchType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS Parameter Bean Class for the GetRecords request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 */
public class GetRecordsRequest extends CswRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsRequest.class);

  private static final JAXBContext JAX_BCONTEXT;

  static {
    JAXBContext context = null;
    String contextPath =
        StringUtils.join(
            new String[] {
              CswConstants.OGC_FILTER_PACKAGE,
              CswConstants.OGC_GML_PACKAGE,
              CswConstants.OGC_OWS_PACKAGE
            },
            ":");

    try {
      LOGGER.debug("Creating JAXB context with context path: {}", contextPath);
      context = JAXBContext.newInstance(contextPath, CswJAXBElementProvider.class.getClassLoader());
    } catch (JAXBException e) {
      LOGGER.info("Unable to create JAXB context using contextPath: {}", contextPath, e);
    }

    JAX_BCONTEXT = context;
  }

  /**
   * Should not set default values for these fields. Otherwise those values will be used by the
   * endpoint GET requests, when the caller fails to specify a parameter.
   */
  private String version;

  private String requestId;

  private String namespace;

  private String resultType;

  private String outputFormat;

  private String outputSchema;

  private BigInteger startPosition;

  private BigInteger maxRecords;

  private String typeNames;

  private String elementName;

  private String elementSetName;

  private String constraintLanguage;

  private String constraint;

  private String sortBy;

  private Boolean distributedSearch;

  private BigInteger hopCount;

  private String responseHandler;

  public GetRecordsRequest() {
    super(CswConstants.GET_RECORDS);
  }

  public GetRecordsRequest(String service, String version) {
    this();
    setService(service);
    setVersion(version);
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getResultType() {
    return resultType;
  }

  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public BigInteger getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(BigInteger startPosition) {
    this.startPosition = startPosition;
  }

  public BigInteger getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(BigInteger maxRecords) {
    this.maxRecords = maxRecords;
  }

  public String getTypeNames() {
    return typeNames;
  }

  public void setTypeNames(String typeNames) {
    this.typeNames = typeNames;
  }

  public String getElementName() {
    return elementName;
  }

  public void setElementName(String elementName) {
    this.elementName = elementName;
  }

  public String getElementSetName() {
    return elementSetName;
  }

  public void setElementSetName(String elementSetName) {
    this.elementSetName = elementSetName;
  }

  public String getConstraintLanguage() {
    return constraintLanguage;
  }

  public void setConstraintLanguage(String constraintLanguage) {
    this.constraintLanguage = constraintLanguage;
  }

  public String getConstraint() {
    return constraint;
  }

  public void setConstraint(String constraint) {
    this.constraint = constraint;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public Boolean getDistributedSearch() {
    return distributedSearch;
  }

  public void setDistributedSearch(Boolean distributedSearch) {
    this.distributedSearch = distributedSearch;
  }

  public BigInteger getHopCount() {
    return hopCount;
  }

  public void setHopCount(BigInteger hopCount) {
    this.hopCount = hopCount;
  }

  public String getResponseHandler() {
    return responseHandler;
  }

  public void setResponseHandler(String responseHandler) {
    this.responseHandler = responseHandler;
  }

  /**
   * Convert the KVP values into a GetRecordsType, validates format of fields and enumeration
   * constraints required to meet the schema requirements of the GetRecordsType. No further
   * validation is done at this point
   *
   * @return GetRecordsType representation of this key-value representation
   * @throws CswException An exception when some field cannot be converted to the equivalent
   *     GetRecordsType value
   */
  public GetRecordsType get202RecordsType() throws CswException {
    GetRecordsType getRecords = new GetRecordsType();

    getRecords.setOutputSchema(getOutputSchema());
    getRecords.setRequestId(getRequestId());

    if (getMaxRecords() != null) {
      getRecords.setMaxRecords(getMaxRecords());
    }
    if (getStartPosition() != null) {
      getRecords.setStartPosition(getStartPosition());
    }
    if (getOutputFormat() != null) {
      getRecords.setOutputFormat(getOutputFormat());
    }
    if (getResponseHandler() != null) {
      getRecords.setResponseHandler(Arrays.asList(getResponseHandler()));
    }
    if (getResultType() != null) {
      try {
        getRecords.setResultType(ResultType.fromValue(getResultType()));
      } catch (IllegalArgumentException iae) {
        LOGGER.debug(
            "Failed to find \"{}\" as a valid ResultType",
            LogSanitizer.sanitize(getResultType()),
            iae);
        throw new CswException(
            "A CSW getRecords request ResultType must be \"hits\", \"results\", or \"validate\"");
      }
    }
    if (getDistributedSearch() != null && getDistributedSearch()) {
      DistributedSearchType disSearch = new DistributedSearchType();
      disSearch.setHopCount(getHopCount());
      getRecords.setDistributedSearch(disSearch);
    }

    QueryType query = new QueryType();

    Map<String, String> namespaces = parseNamespaces(getNamespace());
    List<QName> typeNames = typeStringToQNames(getTypeNames(), namespaces);
    query.setTypeNames(typeNames);

    if (getElementName() != null && getElementSetName() != null) {
      LOGGER.debug(
          "CSW getRecords request received with mutually exclusive ElementName and SetElementName set");
      throw new CswException(
          "A CSW getRecords request can only have an \"ElementName\" or an \"ElementSetName\"");
    }

    if (getElementName() != null) {
      query.setElementName(typeStringToQNames(getElementName(), namespaces));
    }

    if (getElementSetName() != null) {
      try {
        ElementSetNameType eleSetName = new ElementSetNameType();
        eleSetName.setTypeNames(typeNames);
        eleSetName.setValue(ElementSetType.fromValue(getElementSetName()));
        query.setElementSetName(eleSetName);
      } catch (IllegalArgumentException iae) {
        LOGGER.debug(
            "Failed to find \"{}\" as a valid elementSetType, Exception {}",
            LogSanitizer.sanitize(getElementSetName()),
            iae);
        throw new CswException(
            "A CSW getRecords request ElementSetType must be \"brief\", \"summary\", or \"full\"");
      }
    }

    if (getSortBy() != null) {
      SortByType sort = new SortByType();

      List<SortPropertyType> sortProps = new LinkedList<SortPropertyType>();

      String[] sortOptions = getSortBy().split(",");

      for (String sortOption : sortOptions) {
        if (sortOption.lastIndexOf(':') < 1) {
          throw new CswException("Invalid Sort Order format: " + getSortBy());
        }
        SortPropertyType sortProperty = new SortPropertyType();
        PropertyNameType propertyName = new PropertyNameType();

        String propName = StringUtils.substringBeforeLast(sortOption, ":");
        String direction = StringUtils.substringAfterLast(sortOption, ":");
        propertyName.setContent(Arrays.asList((Object) propName));
        SortOrderType sortOrder;

        if (direction.equals("A")) {
          sortOrder = SortOrderType.ASC;
        } else if (direction.equals("D")) {
          sortOrder = SortOrderType.DESC;
        } else {
          throw new CswException("Invalid Sort Order format: " + getSortBy());
        }

        sortProperty.setPropertyName(propertyName);
        sortProperty.setSortOrder(sortOrder);

        sortProps.add(sortProperty);
      }

      sort.setSortProperty(sortProps);

      query.setElementName(typeStringToQNames(getElementName(), namespaces));
      query.setSortBy(sort);
    }

    if (getConstraint() != null) {
      QueryConstraintType queryConstraint = new QueryConstraintType();

      if (getConstraintLanguage().equalsIgnoreCase(CswConstants.CONSTRAINT_LANGUAGE_CQL)) {
        queryConstraint.setCqlText(getConstraint());
      } else if (getConstraintLanguage()
          .equalsIgnoreCase(CswConstants.CONSTRAINT_LANGUAGE_FILTER)) {
        try {
          XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
          xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
          xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
          XMLStreamReader xmlStreamReader =
              xmlInputFactory.createXMLStreamReader(new StringReader(constraint));

          Unmarshaller unmarshaller = JAX_BCONTEXT.createUnmarshaller();
          @SuppressWarnings("unchecked")
          JAXBElement<FilterType> jaxbFilter =
              (JAXBElement<FilterType>) unmarshaller.unmarshal(xmlStreamReader);
          queryConstraint.setFilter(jaxbFilter.getValue());
        } catch (JAXBException e) {
          LOGGER.debug("JAXBException parsing OGC Filter:", e);
          throw new CswException("JAXBException parsing OGC Filter:" + getConstraint());
        } catch (Exception e) {
          LOGGER.debug("Unable to parse OGC Filter:", e);
          throw new CswException("Unable to parse OGC Filter:" + getConstraint());
        }
      } else {
        throw new CswException("Invalid Constraint Language defined: " + getConstraintLanguage());
      }
      query.setConstraint(queryConstraint);
    }

    JAXBElement<QueryType> jaxbQuery =
        new JAXBElement<QueryType>(
            new QName(CswConstants.CSW_OUTPUT_SCHEMA), QueryType.class, query);

    getRecords.setAbstractQuery(jaxbQuery);

    return getRecords;
  }
}
