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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import static com.google.common.primitives.Doubles.asList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.filter.v_1_1_0.AbstractIdType;
import net.opengis.filter.v_1_1_0.BBOXType;
import net.opengis.filter.v_1_1_0.BinaryComparisonOpType;
import net.opengis.filter.v_1_1_0.BinaryLogicOpType;
import net.opengis.filter.v_1_1_0.BinarySpatialOpType;
import net.opengis.filter.v_1_1_0.ComparisonOpsType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.DistanceType;
import net.opengis.filter.v_1_1_0.FeatureIdType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.LiteralType;
import net.opengis.filter.v_1_1_0.LowerBoundaryType;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import net.opengis.filter.v_1_1_0.PropertyIsBetweenType;
import net.opengis.filter.v_1_1_0.PropertyIsLikeType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SpatialOpsType;
import net.opengis.filter.v_1_1_0.UnaryLogicOpType;
import net.opengis.filter.v_1_1_0.UpperBoundaryType;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.CoordinatesType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.EnvelopeType;
import net.opengis.gml.v_3_1_1.LineStringType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PointType;
import net.opengis.gml.v_3_1_1.PolygonType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants;
import org.codice.ddf.spatial.ogc.wfs.v110.catalog.common.Wfs11Constants.SPATIAL_OPERATORS;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to convert DDF OGC Filters into WFS compatible OGC Filters. This
 * class will return an "Invalid"(null) filter if a translation could not be made. It will return an
 * "Empty" filter, meaning no filters are set, only if it is a Content Type filter.
 */
public class WfsFilterDelegate extends SimpleFilterDelegate<FilterType> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsFilterDelegate.class);

  private static final String MISSING_PARAMETERS_MSG = "Required parameters are missing";

  private static final String PROPERTY_NOT_QUERYABLE = "'%s' is not a queryable property.";

  private static final String UNABLE_TO_PARSE_WKT_STRING = "Unable to parse WKT String";

  private static final ThreadLocal<WKTReader> WKT_READER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTReader::new);

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private FeatureMetacardType featureMetacardType;

  private ObjectFactory filterObjectFactory = new ObjectFactory();

  private net.opengis.gml.v_3_1_1.ObjectFactory gmlObjectFactory =
      new net.opengis.gml.v_3_1_1.ObjectFactory();

  private List<String> supportedGeo;

  private List<QName> geometryOperands;

  public WfsFilterDelegate(FeatureMetacardType featureMetacardType, List<String> supportedGeo) {

    if (featureMetacardType == null) {
      throw new IllegalArgumentException("FeatureMetacardType can not be null");
    }
    this.featureMetacardType = featureMetacardType;
    this.supportedGeo = supportedGeo;
    setSupportedGeometryOperands(Wfs11Constants.wktOperandsAsList());
  }

  public void setSupportedGeometryOperands(List<QName> geometryOperands) {
    this.geometryOperands = geometryOperands;
  }

  private boolean isGeometryOperandSupported(QName geoOperand) {
    return geometryOperands.contains(geoOperand);
  }

  public void setSupportedGeoFilters(List<String> supportedGeos) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Updating supportedGeos to: {}", supportedGeos.stream().collect(Collectors.joining(",")));
    }
    this.supportedGeo = supportedGeos;
  }

  @Override
  public FilterType and(List<FilterType> filtersToBeAnded) {
    filtersToBeAnded.removeAll(Collections.singleton(null));

    return buildAndOrFilter(
        filtersToBeAnded, filterObjectFactory.createAnd(new BinaryLogicOpType()));
  }

  @Override
  public FilterType or(List<FilterType> filtersToBeOred) {
    filtersToBeOred.removeAll(Collections.singleton(null));

    return buildAndOrFilter(filtersToBeOred, filterObjectFactory.createOr(new BinaryLogicOpType()));
  }

  @Override
  public FilterType not(FilterType filterToBeNoted) {
    FilterType returnFilter = new FilterType();
    if (filterToBeNoted == null) {
      return returnFilter;
    }
    UnaryLogicOpType notType = new UnaryLogicOpType();
    if (filterToBeNoted.isSetComparisonOps()) {
      notType.setComparisonOps(filterToBeNoted.getComparisonOps());
    } else if (filterToBeNoted.isSetLogicOps()) {
      notType.setLogicOps(filterToBeNoted.getLogicOps());
    } else if (filterToBeNoted.isSetSpatialOps()) {
      notType.setSpatialOps(filterToBeNoted.getSpatialOps());
    } else {
      return returnFilter;
    }
    returnFilter.setLogicOps(filterObjectFactory.createNot(notType));
    return returnFilter;
  }

  private Set<String> getFeatureIds(List<FilterType> filters) {
    Set<String> ids = new HashSet<>();

    if (CollectionUtils.isNotEmpty(filters)) {
      getFeatureIdsSafe(filters, ids);
    }

    return ids;
  }

  private void getFeatureIdsSafe(List<FilterType> filters, Set<String> ids) {
    boolean isFeatureIdFilter = filters.get(0) != null && filters.get(0).isSetId();

    for (FilterType filterType : filters) {
      if ((filterType != null && filterType.isSetId()) != isFeatureIdFilter) {
        throw new UnsupportedOperationException(
            "Query with mix of feature ID and non-feature ID queries not supported");
      }
      if (isFeatureIdFilter) {
        filterType
            .getId()
            .stream()
            .map(
                (Function<JAXBElement<? extends AbstractIdType>, AbstractIdType>)
                    JAXBElement::getValue)
            .filter(FeatureIdType.class::isInstance)
            .map(FeatureIdType.class::cast)
            .forEach(featureIdType -> ids.add(featureIdType.getFid()));
      }
    }
  }

  private FilterType buildFeatureIdFilter(Set<String> ids) {
    FilterType filterType = new FilterType();

    for (String id : ids) {
      List<JAXBElement<? extends AbstractIdType>> idFilterTypeList = filterType.getId();
      FeatureIdType featureIdType = new FeatureIdType();
      featureIdType.setFid(id);
      idFilterTypeList.add(filterObjectFactory.createFeatureId(featureIdType));
    }
    return filterType;
  }

  private FilterType buildAndOrFilter(
      List<FilterType> filters, JAXBElement<BinaryLogicOpType> andOrFilter) {

    if (filters.isEmpty()) {
      return null;
    }
    removeEmptyFilters(filters);

    // Check if these filters contain featureID(s)
    Set<String> featureIds = getFeatureIds(filters);

    if (CollectionUtils.isNotEmpty(featureIds)) {
      return buildFeatureIdFilter(featureIds);
    }

    // If we have 1 filter don't wrap it with AND/OR
    if (filters.size() == 1) {
      return filters.get(0);
    }

    for (FilterType filterType : filters) {
      // Determine which filterType is set
      if (filterType.isSetComparisonOps()) {
        andOrFilter.getValue().getOps().add(filterType.getComparisonOps());
      } else if (filterType.isSetLogicOps()) {
        andOrFilter.getValue().getOps().add(filterType.getLogicOps());
      } else if (filterType.isSetSpatialOps()) {
        andOrFilter.getValue().getOps().add(filterType.getSpatialOps());
      }
    }
    FilterType returnFilter = new FilterType();
    returnFilter.setLogicOps(andOrFilter);

    return returnFilter;
  }

  private void removeEmptyFilters(List<FilterType> filters) {
    // Loop through the filters and remove any empty filters
    List<FilterType> filtersToBeRemoved = new ArrayList<>(filters.size());
    Boolean foundInvalidFilter = false;
    for (FilterType filterType : filters) {
      if (filterType == null) {
        foundInvalidFilter = true;
      } else if (!isFilterSet(filterType)) {
        filtersToBeRemoved.add(filterType);
      }
    }
    // If we found an invalid filter we want to return an invalid filter.
    if (foundInvalidFilter) {
      filters.clear();
      filters.add(null);
    } else {
      filters.removeAll(filtersToBeRemoved);
      filters.removeAll(Collections.singleton((FilterType) null));
      if (filters.isEmpty()) {
        filters.add(new FilterType());
      }
    }
  }

  private Boolean isFilterSet(FilterType filter) {
    return filter.isSetComparisonOps()
        || filter.isSetLogicOps()
        || filter.isSetSpatialOps()
        || filter.isSetId();
  }

  @Override
  public FilterType propertyIsEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, Date date) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(date), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, int literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, short literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, long literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, float literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, double literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, boolean literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsLike(String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLike);
  }

  @Override
  public FilterType propertyIsNotEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, boolean literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, String literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName,
        convertDateToIso8601Format(literal),
        PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, String literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, String literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName,
        convertDateToIso8601Format(literal),
        PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, String lowerBoundary, String upperBoundary) {
    return buildPropertyIsBetweenFilterType(propertyName, lowerBoundary, upperBoundary);
  }

  @Override
  public FilterType propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
    return buildPropertyIsBetweenFilterType(
        propertyName,
        convertDateToIso8601Format(lowerBoundary),
        convertDateToIso8601Format(upperBoundary));
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, Number lowerBoundary, Number upperBoundary) {
    return buildPropertyIsBetweenFilterType(propertyName, lowerBoundary, upperBoundary);
  }

  private FilterType buildPropertyIsBetweenFilterType(
      String propertyName, Object lowerBoundary, Object upperBoundary) {

    if (!isValidInputParameters(propertyName, lowerBoundary, upperBoundary)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    FilterType filter = new FilterType();

    if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor featureAttributeDescriptor =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (featureAttributeDescriptor.isIndexed()) {
        filter.setComparisonOps(
            createPropertyIsBetween(
                featureAttributeDescriptor.getPropertyName(), lowerBoundary, upperBoundary));
      } else {
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return filter;
  }

  private FilterType buildPropertyIsFilterType(
      String propertyName, Object literal, PROPERTY_IS_OPS propertyIsType) {
    if (!isValidInputParameters(propertyName, literal)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (Metacard.CONTENT_TYPE.equals(propertyName)) {
      return new FilterType();
    }

    if ((Metacard.ANY_TEXT.equalsIgnoreCase(propertyName))) {
      return buildPropertyIsFilterTypeForAnyGeo(literal, propertyIsType);
    } else if (featureMetacardType.getProperties().contains(propertyName)) {
      return buildPropertyIsFilterTypeForProperty(propertyName, literal, propertyIsType);
    } else if (Metacard.ID.equals(propertyName)) {
      return buildPropertyIsFilterTypeForId(literal);
    }

    return null;
  }

  private FilterType buildPropertyIsFilterTypeForId(Object literal) {
    LOGGER.debug("feature id query for : {}", literal);
    String[] idTokens = literal.toString().split("\\.");
    FilterType returnFilter = new FilterType();
    if (idTokens.length > 1) {
      if (idTokens[0].equals(featureMetacardType.getName())) {
        LOGGER.debug("feature type matches metacard type; creating featureID filter");
        returnFilter
            .getId()
            .add(filterObjectFactory.createFeatureId(createFeatureIdFilter(literal.toString())));
      } else {
        LOGGER.debug("feature type does not match metacard type; invalidating filter");
        returnFilter = null;
      }
    } else {
      returnFilter
          .getId()
          .add(filterObjectFactory.createFeatureId(createFeatureIdFilter(literal.toString())));
    }
    return returnFilter;
  }

  private FilterType buildPropertyIsFilterTypeForProperty(
      String propertyName, Object literal, PROPERTY_IS_OPS propertyIsType) {
    FilterType returnFilter = new FilterType();
    FeatureAttributeDescriptor attrDesc =
        (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
    if (attrDesc.isIndexed()) {
      returnFilter.setComparisonOps(
          createPropertyIsFilter(attrDesc.getPropertyName(), literal, propertyIsType));
      return returnFilter;
    }
    // blacklisted property encountered
    throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
  }

  private FilterType buildPropertyIsFilterTypeForAnyGeo(
      Object literal, PROPERTY_IS_OPS propertyIsType) {

    if (CollectionUtils.isEmpty(featureMetacardType.getTextualProperties())) {
      LOGGER.debug("Feature Type does not have Textual Properties to query.");
      return null;
    }

    if (featureMetacardType.getTextualProperties().size() == 1) {
      return buildPropertyIsFilterForSingleProperty(literal, propertyIsType);
    }

    return buildPropertyIsFilterForMultipleProperties(literal, propertyIsType);
  }

  private FilterType buildPropertyIsFilterForMultipleProperties(
      final Object literal, final PROPERTY_IS_OPS propertyIsType) {
    List<FilterType> binaryCompOpsToBeOred = new ArrayList<>();

    featureMetacardType
        .getTextualProperties()
        .forEach(
            property -> {
              // only build filters for queryable properties
              FeatureAttributeDescriptor attrDesc =
                  (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(property);
              if (attrDesc.isIndexed()) {
                FilterType filter = new FilterType();
                filter.setComparisonOps(
                    createPropertyIsFilter(attrDesc.getPropertyName(), literal, propertyIsType));
                binaryCompOpsToBeOred.add(filter);
              } else {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug(String.format(PROPERTY_NOT_QUERYABLE, property));
                }
              }
            });

    if (!binaryCompOpsToBeOred.isEmpty()) {
      return or(binaryCompOpsToBeOred);
    }

    LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
    return null;
  }

  private FilterType buildPropertyIsFilterForSingleProperty(
      Object literal, PROPERTY_IS_OPS propertyIsType) {
    FeatureAttributeDescriptor attrDescriptor =
        (FeatureAttributeDescriptor)
            featureMetacardType.getAttributeDescriptor(
                featureMetacardType.getTextualProperties().get(0));
    if (attrDescriptor.isIndexed()) {
      FilterType returnFilter = new FilterType();
      returnFilter.setComparisonOps(
          createPropertyIsFilter(attrDescriptor.getPropertyName(), literal, propertyIsType));
      return returnFilter;
    }
    LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
    return null;
  }

  private JAXBElement<? extends ComparisonOpsType> createPropertyIsFilter(
      String property, Object literal, PROPERTY_IS_OPS operation) {
    switch (operation) {
      case PropertyIsEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsEqualTo =
            filterObjectFactory.createPropertyIsEqualTo(new BinaryComparisonOpType());
        propIsEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsEqualTo.getValue().getExpression().add(createLiteralType(literal));
        return propIsEqualTo;

      case PropertyIsNotEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsNotEqualTo =
            filterObjectFactory.createPropertyIsNotEqualTo(new BinaryComparisonOpType());
        propIsNotEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsNotEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsNotEqualTo;

      case PropertyIsGreaterThan:
        JAXBElement<BinaryComparisonOpType> propIsGreaterThan =
            filterObjectFactory.createPropertyIsGreaterThan(new BinaryComparisonOpType());
        propIsGreaterThan.getValue().getExpression().add(createPropertyNameType(property));
        propIsGreaterThan.getValue().getExpression().add(createLiteralType(literal));

        return propIsGreaterThan;

      case PropertyIsGreaterThanOrEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsGreaterThanOrEqualTo =
            filterObjectFactory.createPropertyIsGreaterThanOrEqualTo(new BinaryComparisonOpType());
        propIsGreaterThanOrEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsGreaterThanOrEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsGreaterThanOrEqualTo;

      case PropertyIsLessThan:
        JAXBElement<BinaryComparisonOpType> propIsLessThan =
            filterObjectFactory.createPropertyIsLessThan(new BinaryComparisonOpType());
        propIsLessThan.getValue().getExpression().add(createPropertyNameType(property));
        propIsLessThan.getValue().getExpression().add(createLiteralType(literal));

        return propIsLessThan;

      case PropertyIsLessThanOrEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsLessThanOrEqualTo =
            filterObjectFactory.createPropertyIsLessThanOrEqualTo(new BinaryComparisonOpType());
        propIsLessThanOrEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsLessThanOrEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsLessThanOrEqualTo;

      case PropertyIsLike:
        JAXBElement<PropertyIsLikeType> propIsLike =
            filterObjectFactory.createPropertyIsLike(new PropertyIsLikeType());
        propIsLike.getValue().setPropertyName(createPropertyNameType(property).getValue());
        propIsLike.getValue().setEscapeChar(WfsConstants.ESCAPE);
        propIsLike.getValue().setSingleChar(SINGLE_CHAR);
        propIsLike.getValue().setWildCard(WfsConstants.WILD_CARD);
        propIsLike.getValue().setLiteral(createLiteralType(literal).getValue());

        return propIsLike;

      default:
        throw new UnsupportedOperationException("Unsupported Property Comparison Type");
    }
  }

  private JAXBElement<PropertyIsBetweenType> createPropertyIsBetween(
      String property, Object lowerBoundary, Object upperBoundary) {
    PropertyIsBetweenType propertyIsBetween = new PropertyIsBetweenType();
    propertyIsBetween.setLowerBoundary(createLowerBoundary(lowerBoundary));
    propertyIsBetween.setUpperBoundary(createUpperBoundary(upperBoundary));
    propertyIsBetween.setExpression(createPropertyNameType(property));

    return filterObjectFactory.createPropertyIsBetween(propertyIsBetween);
  }

  private FeatureIdType createFeatureIdFilter(final String id) {

    FeatureIdType featureIdType = new FeatureIdType();
    featureIdType.setFid(id);

    return featureIdType;
  }

  private boolean isValidInputParameters(String propertyName, Object literal) {
    return !(literal == null
        || StringUtils.isEmpty(propertyName)
        || StringUtils.isEmpty(literal.toString()));
  }

  private boolean isValidInputParameters(String propertyName, String literal, double distance) {
    boolean isValid = isValidInputParameters(propertyName, literal);
    if (distance < 0) {
      isValid = false;
    }
    return isValid;
  }

  private boolean isValidInputParameters(
      String propertyName, Object lowerBoundary, Object upperBoundary) {

    if (lowerBoundary == null || upperBoundary == null) {
      return false;
    }
    return StringUtils.isNotEmpty(propertyName)
        && StringUtils.isNotEmpty(lowerBoundary.toString())
        && StringUtils.isNotEmpty(upperBoundary.toString());
  }

  private DateTime convertDateToIso8601Format(Date inputDate) {
    return new DateTime(inputDate);
  }

  // spatial operators
  @Override
  public FilterType beyond(String propertyName, String wkt, double distance) {

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.BEYOND.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.BEYOND.toString(), propertyName, wkt, distance);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.DWITHIN.getValue())) {
      return not(dwithin(propertyName, wkt, distance));
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType contains(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.CONTAINS.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.CONTAINS.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.WITHIN.getValue())) {
      return not(within(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Contains filters");
      return null;
    }
  }

  @Override
  public FilterType crosses(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.CROSSES.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.CROSSES.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Crosses filters");
      return null;
    }
  }

  @Override
  public FilterType disjoint(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.DISJOINT.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.DISJOINT.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.BBOX.getValue())) {
      return not(bbox(propertyName, wkt));
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.INTERSECTS.getValue())) {
      return not(intersects(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Disjoint or BBOX filters");
      return null;
    }
  }

  @Override
  public FilterType dwithin(String propertyName, String wkt, double distance) {

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.DWITHIN.getValue())) {
      return this.buildGeospatialFilterType(
          SPATIAL_OPERATORS.DWITHIN.toString(), propertyName, wkt, distance);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.BEYOND.getValue())) {
      return not(beyond(propertyName, wkt, distance));
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.INTERSECTS.getValue())) {
      String bufferedWkt = bufferGeometry(wkt, distance);
      return intersects(propertyName, bufferedWkt);
    } else {
      LOGGER.debug(
          "WFS Source does not support the DWithin filter or any of its fallback filters (Not Beyond or Intersects).");
      return null;
    }
  }

  @Override
  public FilterType intersects(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.INTERSECTS.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.INTERSECTS.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.BBOX.getValue())) {
      return bbox(propertyName, wkt);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.DISJOINT.getValue())) {
      return not(disjoint(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Intersect or BBOX");
      return null;
    }
  }

  @Override
  public FilterType overlaps(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.OVERLAPS.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.OVERLAPS.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Overlaps filters");
      return null;
    }
  }

  @Override
  public FilterType touches(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.TOUCHES.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.TOUCHES.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType within(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.WITHIN.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.WITHIN.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.CONTAINS.getValue())) {
      return not(within(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Within filters");
      return null;
    }
  }

  private FilterType bbox(String propertyName, String wkt) {

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.BBOX.getValue())) {
      return buildGeospatialFilterType(SPATIAL_OPERATORS.BBOX.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support BBOX filters");
      return null;
    }
  }

  private FilterType buildGeospatialFilterType(
      String spatialOpType, String propertyName, String wkt, Double distance) {
    if (Metacard.ANY_GEO.equals(propertyName)) {
      return buildGeospatialFilterForAnyGeo(spatialOpType, wkt, distance);
    } else if (featureMetacardType.getGmlProperties().contains(propertyName)) {
      return buildGeospatialFilter(spatialOpType, propertyName, wkt, distance);
    }

    return null;
  }

  private FilterType buildGeospatialFilter(
      String spatialOpType, String propertyName, String wkt, Double distance) {
    FeatureAttributeDescriptor attrDesc =
        (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
    if (attrDesc != null && attrDesc.isIndexed()) {
      FilterType filter = new FilterType();
      filter.setSpatialOps(
          createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
      return filter;
    } else {
      throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
    }
  }

  private FilterType buildGeospatialFilterForAnyGeo(
      String spatialOpType, String wkt, Double distance) {

    if (CollectionUtils.isEmpty(featureMetacardType.getGmlProperties())) {
      LOGGER.debug("Feature Type does not have GEO properties to query");
      return null;
    }

    if (featureMetacardType.getGmlProperties().size() == 1) {
      return buildSingleGeospatialFilter(spatialOpType, wkt, distance);
    }
    return buildMultiGeospatialFilter(spatialOpType, wkt, distance);
  }

  private FilterType buildMultiGeospatialFilter(String spatialOpType, String wkt, Double distance) {
    List<FilterType> filtersToBeOred = new ArrayList<>();

    featureMetacardType
        .getGmlProperties()
        .forEach(
            property -> {
              FeatureAttributeDescriptor attrDesc =
                  (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(property);
              if (attrDesc != null && attrDesc.isIndexed()) {
                FilterType filter = new FilterType();
                filter.setSpatialOps(
                    createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
                filtersToBeOred.add(filter);
              } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(PROPERTY_NOT_QUERYABLE, property));
              }
            });

    if (!filtersToBeOred.isEmpty()) {
      return or(filtersToBeOred);
    }

    LOGGER.debug("All GEO properties have been blacklisted. Removing from query.");

    return null;
  }

  private FilterType buildSingleGeospatialFilter(
      String spatialOpType, String wkt, Double distance) {
    FilterType returnFilter = new FilterType();
    FeatureAttributeDescriptor attrDesc =
        (FeatureAttributeDescriptor)
            featureMetacardType.getAttributeDescriptor(
                featureMetacardType.getGmlProperties().get(0));
    if (attrDesc != null && attrDesc.isIndexed()) {
      returnFilter.setSpatialOps(
          createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
    } else {
      LOGGER.debug("All GEO properties have been blacklisted. Removing from query");
      returnFilter = null;
    }
    return returnFilter;
  }

  private JAXBElement<? extends SpatialOpsType> createSpatialOpType(
      String operation, String propertyName, String wkt, Double distance) {

    switch (SPATIAL_OPERATORS.valueOf(operation)) {
      case BBOX:
        return buildBBoxType(propertyName, wkt);
      case CONTAINS:
        return buildBinarySpatialOpType(
            filterObjectFactory.createContains(new BinarySpatialOpType()), propertyName, wkt);
      case CROSSES:
        return buildBinarySpatialOpType(
            filterObjectFactory.createCrosses(new BinarySpatialOpType()), propertyName, wkt);
      case DISJOINT:
        return buildBinarySpatialOpType(
            filterObjectFactory.createDisjoint(new BinarySpatialOpType()), propertyName, wkt);
      case INTERSECTS:
        return buildBinarySpatialOpType(
            filterObjectFactory.createIntersects(new BinarySpatialOpType()), propertyName, wkt);
      case EQUALS:
        return buildBinarySpatialOpType(
            filterObjectFactory.createEquals(new BinarySpatialOpType()), propertyName, wkt);
      case OVERLAPS:
        return buildBinarySpatialOpType(
            filterObjectFactory.createOverlaps(new BinarySpatialOpType()), propertyName, wkt);
      case TOUCHES:
        return buildBinarySpatialOpType(
            filterObjectFactory.createTouches(new BinarySpatialOpType()), propertyName, wkt);
      case WITHIN:
        return buildBinarySpatialOpType(
            filterObjectFactory.createWithin(new BinarySpatialOpType()), propertyName, wkt);
      case BEYOND:
        if (distance != null) {
          return buildDistanceBufferType(
              filterObjectFactory.createBeyond(new DistanceBufferType()),
              propertyName,
              wkt,
              distance);
        }
        throw new UnsupportedOperationException(
            String.format(
                "Geospatial filter type %s requires distance",
                SPATIAL_OPERATORS.valueOf(operation)));
      case DWITHIN:
        if (distance != null) {
          return buildDistanceBufferType(
              filterObjectFactory.createDWithin(new DistanceBufferType()),
              propertyName,
              wkt,
              distance);
        }
        throw new UnsupportedOperationException(
            String.format(
                "Geospatial filter type %s requires distance",
                SPATIAL_OPERATORS.valueOf(operation)));
      default:
        throw new UnsupportedOperationException(
            "Unsupported geospatial filter type "
                + SPATIAL_OPERATORS.valueOf(operation)
                + " specified");
    }
  }

  @SuppressWarnings("unchecked")
  private JAXBElement<BinarySpatialOpType> buildBinarySpatialOpType(
      JAXBElement<BinarySpatialOpType> bsot, String propertyName, String wkt) {
    bsot.getValue().setPropertyName1(createPropertyNameType(propertyName).getValue());
    bsot.getValue().setGeometry((JAXBElement<AbstractGeometryType>) createGeometryOperand(wkt));

    return bsot;
  }

  @SuppressWarnings("unchecked")
  private JAXBElement<DistanceBufferType> buildDistanceBufferType(
      JAXBElement<DistanceBufferType> dbt, String propertyName, String wkt, double distance) {

    DistanceType distanceType = new DistanceType();
    distanceType.setValue(distance);
    // the filter adapter normalizes all distances to meters
    distanceType.setUnits(WfsConstants.METERS);
    dbt.getValue().setDistance(distanceType);

    dbt.getValue().setGeometry((JAXBElement<AbstractGeometryType>) createGeometryOperand(wkt));
    dbt.getValue().setPropertyName(createPropertyNameType(propertyName).getValue());

    return dbt;
  }

  private JAXBElement<BBOXType> buildBBoxType(String propertyName, String wkt) {
    BBOXType bboxType = new BBOXType();
    bboxType.setPropertyName(createPropertyNameType(propertyName).getValue());

    EnvelopeType envelopeType = gmlObjectFactory.createEnvelopeType();

    Envelope envelope = createEnvelopeFromWkt(wkt);

    DirectPositionType lowerCorner = new DirectPositionType();
    lowerCorner.setValue(asList(envelope.getMinX(), envelope.getMinY()));
    envelopeType.setLowerCorner(lowerCorner);

    DirectPositionType upperCorner = new DirectPositionType();
    upperCorner.setValue(asList(envelope.getMaxX(), envelope.getMaxY()));
    envelopeType.setUpperCorner(upperCorner);

    bboxType.setEnvelope(gmlObjectFactory.createEnvelope(envelopeType));

    return filterObjectFactory.createBBOX(bboxType);
  }

  private JAXBElement<PolygonType> createPolygon(String wkt) {
    PolygonType polygon = new PolygonType();
    LinearRingType linearRing = new LinearRingType();

    Coordinate[] coordinates = getCoordinatesFromWkt(wkt);
    if (coordinates != null && coordinates.length > 0) {
      StringBuilder coordString = new StringBuilder();

      for (Coordinate coordinate : coordinates) {
        coordString.append(coordinate.y).append(",").append(coordinate.x).append(" ");
      }

      CoordinatesType coordinatesType = new CoordinatesType();
      coordinatesType.setValue(coordString.toString());
      coordinatesType.setDecimal(".");
      coordinatesType.setCs(",");
      coordinatesType.setTs(" ");

      linearRing.setCoordinates(coordinatesType);

      AbstractRingPropertyType abstractRingPropertyType =
          gmlObjectFactory.createAbstractRingPropertyType();
      abstractRingPropertyType.setRing(gmlObjectFactory.createLinearRing(linearRing));

      polygon.setExterior(gmlObjectFactory.createExterior(abstractRingPropertyType));

      return gmlObjectFactory.createPolygon(polygon);
    } else {
      throw new IllegalArgumentException("Unable to parse Polygon coordinates from WKT String");
    }
  }

  private JAXBElement<AbstractGeometryType> createPoint(String wkt) {
    Coordinate[] coordinates = getCoordinatesFromWkt(wkt);

    if (coordinates != null && coordinates.length > 0) {

      CoordinatesType coordinatesType = new CoordinatesType();
      coordinatesType.setValue(coordinates[0].y + "," + coordinates[0].x);

      PointType point = new PointType();
      point.setCoordinates(coordinatesType);

      return gmlObjectFactory.createGeometry(point);
    } else {
      throw new IllegalArgumentException("Unable to parse Point coordinates from WKT String");
    }
  }

  private JAXBElement<LiteralType> createLiteralType(Object literalValue) {
    JAXBElement<LiteralType> literalType = filterObjectFactory.createLiteral(new LiteralType());
    literalType.getValue().getContent().add(literalValue.toString());
    return literalType;
  }

  private JAXBElement<PropertyNameType> createPropertyNameType(String propertyNameValue) {
    JAXBElement<PropertyNameType> propertyNameType =
        filterObjectFactory.createPropertyName(new PropertyNameType());
    propertyNameType.getValue().setContent(Collections.singletonList(propertyNameValue));
    return propertyNameType;
  }

  private LowerBoundaryType createLowerBoundary(Object lowerBoundary) {
    LowerBoundaryType lowerBoundaryType = new LowerBoundaryType();
    lowerBoundaryType.setExpression(createLiteralType(lowerBoundary));
    return lowerBoundaryType;
  }

  private UpperBoundaryType createUpperBoundary(Object upperBoundary) {
    UpperBoundaryType upperBoundaryType = new UpperBoundaryType();
    upperBoundaryType.setExpression(createLiteralType(upperBoundary));
    return upperBoundaryType;
  }

  private Envelope createEnvelopeFromWkt(String wkt) {
    Envelope envelope;
    try {
      Geometry geo = getGeometryFromWkt(wkt);
      envelope = geo.getEnvelopeInternal();
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNABLE_TO_PARSE_WKT_STRING, e);
    }

    return envelope;
  }

  private Coordinate[] getCoordinatesFromWkt(String wkt) {
    Coordinate[] coordinates;
    try {
      Geometry geo = getGeometryFromWkt(wkt);
      coordinates = geo.getCoordinates();
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNABLE_TO_PARSE_WKT_STRING, e);
    }
    return coordinates;
  }

  private Geometry getGeometryFromWkt(String wkt) throws ParseException {
    return WKT_READER_THREAD_LOCAL.get().read(wkt);
  }

  private String bufferGeometry(String wkt, double distance) {
    LOGGER.debug("Buffering WKT {} by distance {} meter(s).", wkt, distance);
    String bufferedWkt;
    try {
      Geometry geometry = getGeometryFromWkt(wkt);
      double bufferInDegrees = metersToDegrees(distance);
      LOGGER.debug(
          "Buffering {} by {} degree(s).", geometry.getClass().getSimpleName(), bufferInDegrees);
      Geometry bufferedGeometry = geometry.buffer(bufferInDegrees);
      bufferedWkt = WKT_WRITER_THREAD_LOCAL.get().write(bufferedGeometry);
      LOGGER.debug("Buffered WKT: {}.", bufferedWkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNABLE_TO_PARSE_WKT_STRING, e);
    }

    return bufferedWkt;
  }

  /**
   * This method approximates the degrees in latitude for the given distance (in meters) using the
   * formula for the meridian distance on Earth.
   *
   * <p>degrees = distance in meters/radius of Earth in meters * 180.0/pi
   *
   * <p>The approximate degrees in latitude can be used to compute a buffer around a given geometry
   * (see bufferGeometry()).
   */
  private double metersToDegrees(double distance) {
    double degrees =
        (distance / WfsConstants.EARTH_MEAN_RADIUS_METERS) * WfsConstants.RADIANS_TO_DEGREES;
    LOGGER.debug("{} meter(s) is approximately {} degree(s) of latitude.", distance, degrees);
    return degrees;
  }

  private JAXBElement<LineStringType> createLineString(Geometry geometry) {
    LineStringType lineStringType = gmlObjectFactory.createLineStringType();

    String coordinatesValue =
        Stream.of(geometry.getCoordinates())
            .map(coordinate -> coordinate.y + "," + coordinate.x)
            .collect(Collectors.joining(" "));

    CoordinatesType coordinatesType = gmlObjectFactory.createCoordinatesType();
    coordinatesType.setValue(coordinatesValue);
    coordinatesType.setDecimal(".");
    coordinatesType.setCs(",");
    coordinatesType.setTs(" ");

    lineStringType.setCoordinates(coordinatesType);
    return gmlObjectFactory.createLineString(lineStringType);
  }

  private JAXBElement<? extends AbstractGeometryType> createGeometryOperand(String wkt) {
    Geometry wktGeometry;
    try {
      wktGeometry = getGeometryFromWkt(wkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse WKT Geometry [" + wkt + "]", e);
    }
    if (wktGeometry instanceof Polygon) {
      if (isGeometryOperandSupported(Wfs11Constants.POLYGON)) {
        return createPolygon(wkt);
      } else {
        throw new IllegalArgumentException("The Polygon operand is not supported.");
      }
    } else if (wktGeometry instanceof Point) {
      if (isGeometryOperandSupported(Wfs11Constants.POINT)) {
        return createPoint(wkt);
      } else {
        throw new IllegalArgumentException("The Point operand is not supported.");
      }
    } else if (wktGeometry instanceof LineString) {
      if (isGeometryOperandSupported(Wfs11Constants.LINESTRING)) {
        return createLineString(wktGeometry);
      } else {
        throw new IllegalArgumentException("The LineString operand is not supported.");
      }
    }
    throw new IllegalArgumentException("Unable to create Geometry from WKT String");
  }

  @Override
  public FilterType during(String propertyName, Date startDate, Date endDate) {
    return propertyIsBetween(propertyName, startDate, endDate);
  }

  @Override
  public FilterType before(String propertyName, Date date) {
    return propertyIsLessThan(propertyName, date);
  }

  @Override
  public FilterType after(String propertyName, Date date) {
    return propertyIsGreaterThan(propertyName, date);
  }

  @Override
  public FilterType relative(String propertyName, long duration) {
    final DateTime now = new DateTime();
    final DateTime start = now.minus(duration);
    return during(propertyName, start.toDate(), now.toDate());
  }

  @SuppressWarnings("squid:S00115")
  private enum PROPERTY_IS_OPS {
    PropertyIsEqualTo,
    PropertyIsLike,
    PropertyIsNotEqualTo,
    PropertyIsGreaterThan,
    PropertyIsGreaterThanOrEqualTo,
    PropertyIsLessThan,
    PropertyIsLessThanOrEqualTo
  }
}
