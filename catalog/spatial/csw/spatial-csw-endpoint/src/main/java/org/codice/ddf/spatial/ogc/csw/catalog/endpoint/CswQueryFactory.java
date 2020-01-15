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

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import ddf.security.permission.Permissions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.SortByType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.PropertyIsFuzzyFunction;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.SourceIdFilterVisitor;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer.CswRecordMap;
import org.geotools.feature.NameImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

/** CswQueryFactory provides utility methods for creating a {@link QueryRequest} */
public class CswQueryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswQueryFactory.class);

  private static final Configuration PARSER_CONFIG =
      new org.geotools.filter.v1_1.OGCConfiguration();

  private static JAXBContext jaxBContext;

  private final FilterBuilder builder;

  private final FilterAdapter adapter;

  private final CswRecordMap cswRecordMap;

  private Map<String, Set<String>> schemaToTagsMapping = new HashMap<>();

  private AttributeRegistry attributeRegistry;

  private QueryFilterTransformerProvider queryFilterTransformerProvider;

  public CswQueryFactory(
      CswRecordMap cswRecordMap, FilterBuilder filterBuilder, FilterAdapter adapter) {
    this.cswRecordMap = cswRecordMap;
    this.builder = filterBuilder;
    this.adapter = adapter;
  }

  public static synchronized JAXBContext getJaxBContext() throws JAXBException {
    if (jaxBContext == null) {

      jaxBContext =
          JAXBContext.newInstance(
              "net.opengis.cat.csw.v_2_0_2:"
                  + "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0",
              CswQueryFactory.class.getClassLoader());
    }
    return jaxBContext;
  }

  public QueryRequest getQueryById(List<String> ids) {
    List<Filter> filters =
        ids.stream()
            .map(id -> builder.attribute(Core.ID).is().equalTo().text(id))
            .collect(Collectors.toList());

    Filter anyOfFilter = builder.anyOf(filters);
    return new QueryRequestImpl(new QueryImpl(anyOfFilter), false);
  }

  public QueryRequest getQuery(GetRecordsType request) throws CswException {

    QueryType query = (QueryType) request.getAbstractQuery().getValue();

    Filter filter = buildFilter(query.getConstraint());
    QueryImpl frameworkQuery = new QueryImpl(filter);
    SortBy[] sortBys = buildSort(query.getSortBy());
    SortBy[] extSortBys = null;
    if (sortBys != null && sortBys.length > 0) {
      frameworkQuery.setSortBy(sortBys[0]);
      extSortBys = Arrays.copyOfRange(sortBys, 1, sortBys.length);
    }

    if (ResultType.HITS.equals(request.getResultType()) || request.getMaxRecords().intValue() < 1) {
      frameworkQuery.setStartIndex(1);
      frameworkQuery.setPageSize(1);
    } else {
      frameworkQuery.setStartIndex(request.getStartPosition().intValue());
      frameworkQuery.setPageSize(request.getMaxRecords().intValue());
    }
    boolean isEnterprise =
        request.getDistributedSearch() != null
            && (request.getDistributedSearch().getHopCount().longValue() > 1);

    Map<String, Serializable> properties = new HashMap<>();
    if (extSortBys != null && extSortBys.length > 0) {
      properties.put(ADDITIONAL_SORT_BYS, extSortBys);
    }

    QueryRequest queryRequest = getQueryRequest(frameworkQuery, isEnterprise, properties);
    return transformQuery(queryRequest, query.getTypeNames());
  }

  public QueryRequest getQuery(QueryConstraintType constraint, String typeName)
      throws CswException {
    Filter filter = buildFilter(constraint);
    QueryImpl query = new QueryImpl(filter);

    QueryRequest request = new QueryRequestImpl(query);

    return transformQuery(request, typeName);
  }

  private Filter buildFilter(QueryConstraintType constraint) throws CswException {
    Filter filter = null;
    if (constraint != null) {
      if (constraint.isSetCqlText()) {
        try {
          filter = ECQL.toFilter(constraint.getCqlText());
        } catch (CQLException e) {
          throw new CswException("Unable to parse CQL Constraint: " + e.getMessage(), e);
        }
      } else if (constraint.isSetFilter()) {
        FilterType constraintFilter = constraint.getFilter();
        filter = parseFilter(constraintFilter);
      }
    } else {
      // not supported by catalog:
      // filter = Filter.INCLUDE;
      filter = builder.attribute(Core.ID).is().like().text(FilterDelegate.WILDCARD_CHAR);
    }

    if (filter == null) {
      throw new CswException("Invalid Filter Expression", CswConstants.NO_APPLICABLE_CODE, null);
    }

    return transformCustomFunctionToFilter(filter);
  }

  /**
   * Transforms the filter if it contains a custom function from the {@link
   * org.codice.ddf.spatial.ogc.csw.catalog.common.ExtendedGeotoolsFunctionFactory}. If the filter
   * does not contain a custom function then the original filter is returned.
   *
   * @param filter
   * @return
   */
  private Filter transformCustomFunctionToFilter(Filter filter) {
    if (filter instanceof IsEqualsToImpl
        && ((IsEqualsToImpl) filter).getExpression1() instanceof PropertyIsFuzzyFunction) {

      PropertyIsFuzzyFunction fuzzyProperty =
          (PropertyIsFuzzyFunction) ((IsEqualsToImpl) filter).getExpression1();

      return builder
          .attribute(fuzzyProperty.getPropertyName().toString())
          .is()
          .like()
          .fuzzyText(fuzzyProperty.getLiteral().toString());
    }

    return filter;
  }

  private QueryRequest normalizeSort(QueryRequest request) {
    if (request == null) {
      return null;
    }

    Query query = request.getQuery();
    if (query == null) {
      return request;
    }

    List<SortBy> sortBys = new ArrayList<>();

    // Primary sort parameter
    SortBy sortBy = normalizeSortBy(query.getSortBy());
    if (sortBy != null) {
      sortBys.add(sortBy);
    }

    // Additional sort parameters
    Map<String, Serializable> newProperties = request.getProperties();
    if (newProperties != null) {
      Serializable extraSortBys = request.getPropertyValue(ADDITIONAL_SORT_BYS);
      if (extraSortBys instanceof SortBy[]) {
        List<SortBy> extraSortBysList = Arrays.asList((SortBy[]) extraSortBys);
        extraSortBysList
            .stream()
            .map(this::normalizeSortBy)
            .filter(Objects::nonNull)
            .forEach(sortBys::add);
      } else {
        LOGGER.debug("The \"{}\" query request property could not be read", ADDITIONAL_SORT_BYS);
      }

      if (sortBys.size() > 1) {
        SortBy[] extraSortBysArray = sortBys.subList(1, sortBys.size()).toArray(new SortBy[0]);
        newProperties.put(ADDITIONAL_SORT_BYS, extraSortBysArray);
      } else {
        newProperties.remove(ADDITIONAL_SORT_BYS);
      }
    }

    SortBy normalizedSortBy = null;
    if (!sortBys.isEmpty()) {
      normalizedSortBy = sortBys.get(0);
    }

    Query newQuery =
        new QueryImpl(
            query,
            query.getStartIndex(),
            query.getPageSize(),
            normalizedSortBy,
            query.requestsTotalResultsCount(),
            query.getTimeoutMillis());

    return new QueryRequestImpl(
        newQuery, request.isEnterprise(), request.getSourceIds(), newProperties);
  }

  private SortBy normalizeSortBy(SortBy cswSortBy) {
    if (cswSortBy == null || cswSortBy.getPropertyName() == null) {
      return null;
    }

    String propertyName = cswSortBy.getPropertyName().getPropertyName();
    if (propertyName == null) {
      LOGGER.debug("Property in SortBy Field is null");
      return null;
    }

    NamespaceSupport namespaceContext = cswSortBy.getPropertyName().getNamespaceContext();
    if (!attributeRegistry.lookup(propertyName).isPresent()
        && !cswRecordMap.hasProperty(propertyName, namespaceContext)) {
      LOGGER.debug("Property {} is not a valid SortBy Field", propertyName);
      return null;
    }

    String name = cswRecordMap.getProperty(propertyName, namespaceContext);

    PropertyName propName = new AttributeExpressionImpl(new NameImpl(name));
    return new SortByImpl(propName, cswSortBy.getSortOrder());
  }

  private SortBy[] buildSort(SortByType sort) throws CswException {
    if (sort == null || sort.getSortProperty() == null) {
      return null;
    }

    SortBy[] sortByArr = parseSortBy(sort);
    if (sortByArr == null || sortByArr.length == 0) {
      return null;
    }

    List<SortBy> sortBys = new ArrayList<>(sortByArr.length);

    for (SortBy cswSortBy : sortByArr) {
      if (cswSortBy.getPropertyName() == null) {
        LOGGER.debug("No property name in primary sort criteria");
        return null;
      }

      String name = cswSortBy.getPropertyName().getPropertyName();
      PropertyName propName = new AttributeExpressionImpl(new NameImpl(name));
      SortBy sortBy = new SortByImpl(propName, cswSortBy.getSortOrder());
      sortBys.add(sortBy);
    }

    if (sortBys.isEmpty()) {
      return null;
    }

    return sortBys.toArray(new SortBy[0]);
  }

  private SortBy[] parseSortBy(SortByType sortByType) throws CswException {
    JAXBElement<SortByType> sortByElement =
        new net.opengis.filter.v_1_1_0.ObjectFactory().createSortBy(sortByType);

    return (SortBy[]) parseJaxB(sortByElement);
  }

  private Object parseJaxB(JAXBElement<?> element) throws CswException {
    Parser parser = new Parser(PARSER_CONFIG);
    InputStream inputStream;

    try {
      inputStream = marshalJaxB(element);
      return parser.parse(inputStream);
    } catch (JAXBException
        | IOException
        | SAXException
        | ParserConfigurationException
        | RuntimeException e) {
      throw new CswException(
          String.format(
              "Failed to parse Element: (%s): %s", e.getClass().getSimpleName(), e.getMessage()),
          CswConstants.INVALID_PARAMETER_VALUE,
          null);
    }
  }

  private InputStream marshalJaxB(JAXBElement<?> filterElement) throws JAXBException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    getJaxBContext().createMarshaller().marshal(filterElement, os);
    ByteArrayInputStream input = new ByteArrayInputStream(os.toByteArray());
    IOUtils.closeQuietly(os);

    return input;
  }

  private Filter parseFilter(FilterType filterType) throws CswException {
    if (!filterType.isSetComparisonOps()
        && !filterType.isSetId()
        && !filterType.isSetLogicOps()
        && !filterType.isSetSpatialOps()) {
      throw new CswException(
          "Empty Filter provided. Unable to perform query.",
          CswConstants.INVALID_PARAMETER_VALUE,
          "Filter");
    }
    JAXBElement<FilterType> filterElement =
        new net.opengis.filter.v_1_1_0.ObjectFactory().createFilter(filterType);

    return (Filter) parseJaxB(filterElement);
  }

  private QueryRequest transformQuery(QueryRequest request, String typeName) {
    QueryRequest result =
        queryFilterTransformerProvider
            .getTransformer(typeName)
            .map(it -> it.transform(request, null))
            .orElse(request);
    return normalizeSort(result);
  }

  private QueryRequest transformQuery(QueryRequest request, List<QName> typeNames) {
    QueryRequest result = request;
    for (QName typeName : typeNames) {
      final QueryRequest temp = result;
      result =
          queryFilterTransformerProvider
              .getTransformer(typeName)
              .map(it -> it.transform(temp, null))
              .orElse(result);
    }
    return normalizeSort(result);
  }

  private QueryRequest getQueryRequest(
      Query query, boolean isEnterprise, Map<String, Serializable> properties) {
    QueryRequest request;

    SourceIdFilterVisitor sourceIdFilterVisitor = new SourceIdFilterVisitor();
    query.accept(sourceIdFilterVisitor, new FilterFactoryImpl());

    if (isEnterprise && CollectionUtils.isEmpty(sourceIdFilterVisitor.getSourceIds())) {
      request = new QueryRequestImpl(query, true, null, properties);
    } else if (isEnterprise && CollectionUtils.isNotEmpty(sourceIdFilterVisitor.getSourceIds())) {
      request =
          new QueryRequestImpl(query, false, sourceIdFilterVisitor.getSourceIds(), properties);
    } else {
      request = new QueryRequestImpl(query, false, null, properties);
    }
    return request;
  }

  public QueryRequest updateQueryRequestTags(QueryRequest queryRequest, String schema)
      throws UnsupportedQueryException {
    QueryRequest newRequest = queryRequest;
    Set<String> tags = schemaToTagsMapping.get(schema);
    if (CollectionUtils.isEmpty(tags)) {
      return queryRequest;
    }
    Query origQuery = queryRequest.getQuery();
    if (!adapter.adapt(queryRequest.getQuery(), new TagsFilterDelegate(tags, true))) {
      List<Filter> filters = new ArrayList<>(tags.size());
      for (String tag : tags) {
        filters.add(builder.attribute(Core.METACARD_TAGS).is().like().text(tag));
      }
      QueryImpl newQuery =
          new QueryImpl(
              builder.allOf(builder.anyOf(filters), origQuery),
              origQuery.getStartIndex(),
              origQuery.getPageSize(),
              origQuery.getSortBy(),
              origQuery.requestsTotalResultsCount(),
              origQuery.getTimeoutMillis());
      newRequest =
          new QueryRequestImpl(
              newQuery,
              queryRequest.isEnterprise(),
              queryRequest.getSourceIds(),
              queryRequest.getProperties());
    }
    return newRequest;
  }

  public void setSchemaToTagsMapping(String[] schemaToTagsMappingStrings) {
    if (schemaToTagsMappingStrings != null) {
      schemaToTagsMapping.clear();
      schemaToTagsMapping.putAll(
          Permissions.parsePermissionsFromString(schemaToTagsMappingStrings));
    }
  }

  public void setAttributeRegistry(AttributeRegistry attributeRegistry) {
    this.attributeRegistry = attributeRegistry;
  }

  public void setQueryFilterTransformerProvider(
      QueryFilterTransformerProvider queryFilterTransformerProvider) {
    this.queryFilterTransformerProvider = queryFilterTransformerProvider;
  }
}
