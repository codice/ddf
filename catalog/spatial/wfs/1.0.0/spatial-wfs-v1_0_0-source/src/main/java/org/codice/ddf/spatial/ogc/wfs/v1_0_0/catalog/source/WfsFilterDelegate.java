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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import ogc.schema.opengis.filter.v_1_0_0.BBOXType;
import ogc.schema.opengis.filter.v_1_0_0.BinaryComparisonOpType;
import ogc.schema.opengis.filter.v_1_0_0.BinaryLogicOpType;
import ogc.schema.opengis.filter.v_1_0_0.BinarySpatialOpType;
import ogc.schema.opengis.filter.v_1_0_0.ComparisonOpsType;
import ogc.schema.opengis.filter.v_1_0_0.DistanceBufferType;
import ogc.schema.opengis.filter.v_1_0_0.DistanceType;
import ogc.schema.opengis.filter.v_1_0_0.FeatureIdType;
import ogc.schema.opengis.filter.v_1_0_0.FilterType;
import ogc.schema.opengis.filter.v_1_0_0.LiteralType;
import ogc.schema.opengis.filter.v_1_0_0.LowerBoundaryType;
import ogc.schema.opengis.filter.v_1_0_0.ObjectFactory;
import ogc.schema.opengis.filter.v_1_0_0.PropertyIsBetweenType;
import ogc.schema.opengis.filter.v_1_0_0.PropertyIsLikeType;
import ogc.schema.opengis.filter.v_1_0_0.PropertyNameType;
import ogc.schema.opengis.filter.v_1_0_0.SpatialOpsType;
import ogc.schema.opengis.filter.v_1_0_0.UnaryLogicOpType;
import ogc.schema.opengis.filter.v_1_0_0.UpperBoundaryType;
import ogc.schema.opengis.gml.v_2_1_2.AbstractGeometryType;
import ogc.schema.opengis.gml.v_2_1_2.BoxType;
import ogc.schema.opengis.gml.v_2_1_2.CoordinatesType;
import ogc.schema.opengis.gml.v_2_1_2.GeometryCollectionType;
import ogc.schema.opengis.gml.v_2_1_2.LineStringType;
import ogc.schema.opengis.gml.v_2_1_2.LinearRingMemberType;
import ogc.schema.opengis.gml.v_2_1_2.LinearRingType;
import ogc.schema.opengis.gml.v_2_1_2.MultiLineStringType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPointType;
import ogc.schema.opengis.gml.v_2_1_2.MultiPolygonType;
import ogc.schema.opengis.gml.v_2_1_2.PointType;
import ogc.schema.opengis.gml.v_2_1_2.PolygonType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10JTStoGML200Converter;
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

  private FeatureMetacardType featureMetacardType;

  private ObjectFactory filterObjectFactory = new ObjectFactory();

  private ogc.schema.opengis.gml.v_2_1_2.ObjectFactory gmlObjectFactory =
      new ogc.schema.opengis.gml.v_2_1_2.ObjectFactory();

  private List<String> supportedGeo;

  private List<QName> geometryOperands;

  private String srsName;

  private boolean isEpsg4326 = false;

  public WfsFilterDelegate(
      FeatureMetacardType featureMetacardType, List<String> supportedGeo, String srsName) {

    if (featureMetacardType == null) {
      throw new IllegalArgumentException("FeatureMetacardType can not be null");
    }
    this.featureMetacardType = featureMetacardType;
    this.supportedGeo = supportedGeo;
    this.srsName = srsName;
    if (GeospatialUtil.EPSG_4326.equalsIgnoreCase(srsName)) {
      isEpsg4326 = true;
    } else {
      LOGGER.debug(
          "Unable to convert geometry to {}. All geospatial queries for this featureType will be invalidated!",
          srsName);
    }
    setSupportedGeometryOperands(Wfs10Constants.wktOperandsAsList());
  }

  public void setSupportedGeometryOperands(List<QName> geometryOperands) {
    this.geometryOperands = geometryOperands;
  }

  private boolean isGeometryOperandSupported(QName geoOperand) {
    return geometryOperands.contains(geoOperand);
  }

  public void setSupportedGeoFilters(List<String> supportedGeos) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Updating supportedGeos to: {}", Arrays.toString(supportedGeos.toArray()));
    }
    this.supportedGeo = supportedGeos;
  }

  @Override
  public FilterType and(List<FilterType> filtersToBeAnded) {
    return buildAndOrFilter(
        filtersToBeAnded, filterObjectFactory.createAnd(new BinaryLogicOpType()));
  }

  @Override
  public FilterType or(List<FilterType> filtersToBeOred) {
    // Remove invalid filters so they aren't OR'd.
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
    Set<String> ids = new HashSet<String>();

    // This filter delegate requires that if one filter is a featureId
    // filter, they
    // must all be.
    if (!CollectionUtils.isEmpty(filters)) {

      boolean isFeatureIdFilter = filters.get(0) != null && filters.get(0).isSetFeatureId();

      for (FilterType filterType : filters) {

        if ((filterType != null && filterType.isSetFeatureId()) != isFeatureIdFilter) {
          throw new UnsupportedOperationException(
              "Query with mix of feature ID and non-feature ID queries not supported");
        }
        if (isFeatureIdFilter) {
          for (FeatureIdType idType : filterType.getFeatureId()) {
            ids.add(idType.getFid());
          }
        }
      }
    }

    return ids;
  }

  private FilterType buildFeatureIdFilter(Set<String> ids) {
    FilterType filterType = new FilterType();

    for (String id : ids) {
      filterType.getFeatureId().add(createFeatureIdFilter(id));
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

    if (!CollectionUtils.isEmpty(featureIds)) {
      return buildFeatureIdFilter(featureIds);
    }

    // If we have 1 filter don't wrap it with AND/OR
    if (filters.size() == 1) {
      return filters.get(0);
    }

    for (FilterType filterType : filters) {
      // Determine which filterType is set
      if (filterType.isSetComparisonOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filterType.getComparisonOps());
      } else if (filterType.isSetLogicOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filterType.getLogicOps());
      } else if (filterType.isSetSpatialOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filterType.getSpatialOps());
      }
    }
    FilterType returnFilter = new FilterType();
    returnFilter.setLogicOps(andOrFilter);

    return returnFilter;
  }

  private void removeEmptyFilters(List<FilterType> filters) {
    // Loop through the filters and remove any empty filters
    List<FilterType> filtersToBeRemoved = new ArrayList<FilterType>(filters.size());
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
      filters.removeAll(Collections.singleton(null));
      if (filters.isEmpty()) {
        filters.add(new FilterType());
      }
    }
  }

  private Boolean isFilterSet(FilterType filter) {
    return (filter.isSetComparisonOps()
        || filter.isSetLogicOps()
        || filter.isSetSpatialOps()
        || filter.isSetFeatureId());
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
    return buildPropertyIsFilterType(
        propertyName, Integer.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, short literal) {
    return buildPropertyIsFilterType(
        propertyName, Short.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, long literal) {
    return buildPropertyIsFilterType(
        propertyName, Long.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, float literal) {
    return buildPropertyIsFilterType(
        propertyName, Float.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, double literal) {
    return buildPropertyIsFilterType(
        propertyName, Double.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, boolean literal) {
    return buildPropertyIsFilterType(
        propertyName, Boolean.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
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
    FilterType returnFilter = new FilterType();
    // If this is a Content Type filter verify its for this Filter delegate.
    if (Metacard.CONTENT_TYPE.equals(propertyName)) {
      if (featureMetacardType.getName().equals(literal)) {
        return returnFilter;
      }
      return null;
    }
    // Special Case - If we get an ANY_TEXT we want to convert that to a
    // series of OR's
    if ((Metacard.ANY_TEXT.equalsIgnoreCase(propertyName))) {
      if (CollectionUtils.isEmpty(featureMetacardType.getTextualProperties())) {
        LOGGER.debug("Feature Type does not have Textual Properties to query.");
        return null;
      }

      if (featureMetacardType.getTextualProperties().size() == 1) {
        FeatureAttributeDescriptor attrDescriptor =
            (FeatureAttributeDescriptor)
                featureMetacardType.getAttributeDescriptor(
                    featureMetacardType.getTextualProperties().get(0));
        if (attrDescriptor.isIndexed()) {
          returnFilter.setComparisonOps(
              createPropertyIsFilter(attrDescriptor.getPropertyName(), literal, propertyIsType));
        } else {
          LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
          return null;
        }
      } else {
        List<FilterType> binaryCompOpsToBeOred = new ArrayList<FilterType>();
        for (String property : featureMetacardType.getTextualProperties()) {
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
        }
        if (!binaryCompOpsToBeOred.isEmpty()) {
          returnFilter = or(binaryCompOpsToBeOred);
        } else {
          LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
          return null;
        }
      }
      // filter is for a specific property; check to see if it is valid
    } else if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor attrDesc =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (attrDesc.isIndexed()) {
        returnFilter.setComparisonOps(
            createPropertyIsFilter(attrDesc.getPropertyName(), literal, propertyIsType));
      } else {
        // blacklisted property encountered
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else if (Metacard.ID.equals(propertyName)) {
      LOGGER.debug("feature id query for : {}", literal);
      String[] idTokens = literal.toString().split("\\.");
      if (idTokens.length > 1) {
        if (idTokens[0].equals(featureMetacardType.getName())) {
          LOGGER.debug("feature type matches metacard type; creating featureID filter");
          returnFilter.getFeatureId().add(createFeatureIdFilter(literal.toString()));
        } else {
          LOGGER.debug("feature type does not match metacard type; invalidating filter");
          return null;
        }
      } else {
        returnFilter.getFeatureId().add(createFeatureIdFilter(literal.toString()));
      }

    } else {
      return null;
    }
    return returnFilter;
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
        propIsLike.getValue().setEscape(WfsConstants.ESCAPE);
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
    if (literal == null
        || StringUtils.isEmpty(propertyName)
        || StringUtils.isEmpty(literal.toString())) {
      return false;
    }
    return true;
  }

  private boolean isValidInputParameters(String propertyName, String literal, double distance) {
    boolean isValid = isValidInputParameters(propertyName, literal);
    if (Double.valueOf(distance) < 0) {
      isValid = false;
    }
    return isValid;
  }

  private boolean isValidInputParameters(
      String propertyName, Object lowerBoundary, Object upperBoundary) {

    if (lowerBoundary == null || upperBoundary == null) {
      return false;
    }
    if (StringUtils.isEmpty(propertyName)
        || StringUtils.isEmpty(lowerBoundary.toString())
        || StringUtils.isEmpty(upperBoundary.toString())) {

      return false;
    }
    return true;
  }

  private DateTime convertDateToIso8601Format(Date inputDate) {
    return new DateTime(inputDate);
  }

  // spatial operators
  @Override
  public FilterType beyond(String propertyName, String wkt, double distance) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Beyond.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Beyond.toString(), propertyName, wkt, distance);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.DWithin.getValue())) {
      return not(dwithin(propertyName, wkt, distance));
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType contains(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Contains.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Contains.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Within.getValue())) {
      return not(within(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Contains filters");
      return null;
    }
  }

  @Override
  public FilterType crosses(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Crosses.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Crosses.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Crosses filters");
      return null;
    }
  }

  @Override
  public FilterType disjoint(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Disjoint.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Disjoint.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.BBOX.getValue())) {
      return not(bbox(propertyName, wkt));
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Intersect.getValue())) {
      return not(intersects(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Disjoint or BBOX filters");
      return null;
    }
  }

  @Override
  public FilterType dwithin(String propertyName, String wkt, double distance) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.DWithin.getValue())) {
      return this.buildGeospatialFilterType(
          SPATIAL_OPERATORS.DWithin.toString(), propertyName, wkt, distance);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Beyond.getValue())) {
      return not(beyond(propertyName, wkt, distance));
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Intersect.getValue())) {
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
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Intersect.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Intersect.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.BBOX.getValue())) {
      return bbox(propertyName, wkt);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Disjoint.getValue())) {
      return not(disjoint(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Intersect or BBOX");
      return null;
    }
  }

  @Override
  public FilterType overlaps(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Overlaps.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Overlaps.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Overlaps filters");
      return null;
    }
  }

  @Override
  public FilterType touches(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Touches.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Touches.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType within(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (supportedGeo.contains(SPATIAL_OPERATORS.Within.getValue())) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Within.toString(), propertyName, wkt, null);
    } else if (supportedGeo.contains(SPATIAL_OPERATORS.Contains.getValue())) {
      return not(within(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Within filters");
      return null;
    }
  }

  private FilterType bbox(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

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
    FilterType returnFilter = new FilterType();
    if (Metacard.ANY_GEO.equals(propertyName)) {

      if (CollectionUtils.isEmpty(featureMetacardType.getGmlProperties())) {
        LOGGER.debug("Feature Type does not have GEO properties to query");
        return null;
      }

      if (featureMetacardType.getGmlProperties().size() == 1) {
        FeatureAttributeDescriptor attrDesc =
            (FeatureAttributeDescriptor)
                featureMetacardType.getAttributeDescriptor(
                    featureMetacardType.getGmlProperties().get(0));
        if (attrDesc != null && attrDesc.isIndexed()) {
          returnFilter.setSpatialOps(
              createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
        } else {
          LOGGER.debug("All GEO properties have been blacklisted. Removing from query");
          return null;
        }

      } else {
        List<FilterType> filtersToBeOred = new ArrayList<FilterType>();
        for (String property : featureMetacardType.getGmlProperties()) {
          FeatureAttributeDescriptor attrDesc =
              (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(property);
          if (attrDesc != null && attrDesc.isIndexed()) {
            FilterType filter = new FilterType();
            filter.setSpatialOps(
                createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
            filtersToBeOred.add(filter);
          } else {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(String.format(PROPERTY_NOT_QUERYABLE, property));
            }
          }
        }
        if (!filtersToBeOred.isEmpty()) {
          returnFilter = or(filtersToBeOred);
        } else {
          LOGGER.debug("All GEO properties have been blacklisted. Removing from query.");
          returnFilter = null;
        }
      }
    } else if (featureMetacardType.getGmlProperties().contains(propertyName)) {
      FeatureAttributeDescriptor attrDesc =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (attrDesc != null && attrDesc.isIndexed()) {
        FilterType filter = new FilterType();
        filter.setSpatialOps(
            createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
        return filter;
      } else {
        // blacklisted property encountered
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return returnFilter;
  }

  private JAXBElement<? extends SpatialOpsType> createSpatialOpType(
      String operation, String propertyName, String wkt, Double distance) {

    switch (SPATIAL_OPERATORS.valueOf(operation)) {
      case BBOX:
        return buildBBoxType(propertyName, wkt);
      case Contains:
        return buildBinarySpatialOpType(
            filterObjectFactory.createContains(new BinarySpatialOpType()), propertyName, wkt);
      case Crosses:
        return buildBinarySpatialOpType(
            filterObjectFactory.createCrosses(new BinarySpatialOpType()), propertyName, wkt);
      case Disjoint:
        return buildBinarySpatialOpType(
            filterObjectFactory.createDisjoint(new BinarySpatialOpType()), propertyName, wkt);
      case Intersect:
        return buildBinarySpatialOpType(
            filterObjectFactory.createIntersects(new BinarySpatialOpType()), propertyName, wkt);
      case Overlaps:
        return buildBinarySpatialOpType(
            filterObjectFactory.createOverlaps(new BinarySpatialOpType()), propertyName, wkt);
      case Touches:
        return buildBinarySpatialOpType(
            filterObjectFactory.createTouches(new BinarySpatialOpType()), propertyName, wkt);
      case Within:
        return buildBinarySpatialOpType(
            filterObjectFactory.createWithin(new BinarySpatialOpType()), propertyName, wkt);
      case Beyond:
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
      case DWithin:
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

  private JAXBElement<BinarySpatialOpType> buildBinarySpatialOpType(
      JAXBElement<BinarySpatialOpType> bsot, String propertyName, String wkt) {
    bsot.getValue().setPropertyName(createPropertyNameType(propertyName).getValue());
    bsot.getValue().setGeometry(createGeometryOperand(wkt));

    return bsot;
  }

  private JAXBElement<DistanceBufferType> buildDistanceBufferType(
      JAXBElement<DistanceBufferType> dbt, String propertyName, String wkt, double distance) {

    DistanceType distanceType = new DistanceType();
    distanceType.setContent(Double.toString(distance));
    // the filter adapter normalizes all distances to meters
    distanceType.setUnits(WfsConstants.METERS);
    dbt.getValue().setDistance(distanceType);

    dbt.getValue().setGeometry(createPoint(wkt));
    dbt.getValue().setPropertyName(createPropertyNameType(propertyName).getValue());

    return dbt;
  }

  private JAXBElement<BBOXType> buildBBoxType(String propertyName, String wkt) {
    BBOXType bboxType = new BBOXType();
    JAXBElement<BoxType> box = createBoxType(wkt);
    bboxType.setBox(box.getValue());
    bboxType.setPropertyName(createPropertyNameType(propertyName).getValue());

    return filterObjectFactory.createBBOX(bboxType);
  }

  private JAXBElement<BoxType> createBoxType(String wkt) {
    BoxType box = new BoxType();
    box.setSrsName(srsName);
    box.setCoordinates(createCoordinatesTypeFromWkt(wkt).getValue());
    return gmlObjectFactory.createBox(box);
  }

  private JAXBElement<PolygonType> createPolygon(String wkt) {
    PolygonType polygon = new PolygonType();
    LinearRingType linearRing = new LinearRingType();

    Coordinate[] coordinates = getCoordinatesFromWkt(wkt);
    if (coordinates != null && coordinates.length > 0) {
      StringBuilder coordString = new StringBuilder();

      for (Coordinate coordinate : coordinates) {
        coordString.append(coordinate.x).append(",").append(coordinate.y).append(" ");
      }

      CoordinatesType coordinatesType = new CoordinatesType();
      coordinatesType.setValue(coordString.toString());
      coordinatesType.setDecimal(".");
      coordinatesType.setCs(",");
      coordinatesType.setTs(" ");

      linearRing.setCoordinates(coordinatesType);
      LinearRingMemberType member = new LinearRingMemberType();
      member.setGeometry(gmlObjectFactory.createLinearRing(linearRing));
      polygon.setOuterBoundaryIs(member);
      polygon.setSrsName(srsName);

      return gmlObjectFactory.createPolygon(polygon);
    } else {
      throw new IllegalArgumentException("Unable to parse Polygon coordinates from WKT String");
    }
  }

  private JAXBElement<PointType> createPoint(String wkt) {
    Coordinate[] coordinates = getCoordinatesFromWkt(wkt);

    if (coordinates != null && coordinates.length > 0) {
      StringBuilder coordString = new StringBuilder();
      coordString.append(coordinates[0].x).append(",").append(coordinates[0].y);

      CoordinatesType coordinatesType = new CoordinatesType();
      coordinatesType.setValue(coordString.toString());

      PointType point = new PointType();
      point.setSrsName(srsName);
      point.setCoordinates(coordinatesType);

      return gmlObjectFactory.createPoint(point);
    } else {
      throw new IllegalArgumentException("Unable to parse Point coordinates from WKT String");
    }
  }

  private String buildCoordinateString(Envelope envelope) {
    StringBuilder sb = new StringBuilder();

    sb.append(envelope.getMinX())
        .append(",")
        .append(envelope.getMinY())
        .append(" ")
        .append(envelope.getMaxX())
        .append(",")
        .append(envelope.getMaxY());

    return sb.toString();
  }

  private JAXBElement<CoordinatesType> createCoordinatesTypeFromWkt(String wkt) {

    Envelope envelope = createEnvelopeFromWkt(wkt);

    String coords = buildCoordinateString(envelope);
    CoordinatesType coordinatesType = new CoordinatesType();

    coordinatesType.setValue(coords);

    return gmlObjectFactory.createCoordinates(coordinatesType);
  }

  private JAXBElement<LiteralType> createLiteralType(Object literalValue) {
    JAXBElement<LiteralType> literalType = filterObjectFactory.createLiteral(new LiteralType());
    literalType.getValue().getContent().add(literalValue.toString());
    return literalType;
  }

  private JAXBElement<PropertyNameType> createPropertyNameType(String propertyNameValue) {
    JAXBElement<PropertyNameType> propertyNameType =
        filterObjectFactory.createPropertyName(new PropertyNameType());
    propertyNameType.getValue().setContent(propertyNameValue);
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
    Envelope envelope = null;
    try {
      Geometry geo = getGeometryFromWkt(wkt);
      envelope = geo.getEnvelopeInternal();
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNABLE_TO_PARSE_WKT_STRING, e);
    }

    return envelope;
  }

  private Coordinate[] getCoordinatesFromWkt(String wkt) {
    Coordinate[] coordinates = null;
    try {
      Geometry geo = getGeometryFromWkt(wkt);
      coordinates = geo.getCoordinates();
    } catch (ParseException e) {
      throw new IllegalArgumentException(UNABLE_TO_PARSE_WKT_STRING, e);
    }
    return coordinates;
  }

  private Geometry getGeometryFromWkt(String wkt) throws ParseException {
    return new WKTReader().read(wkt);
  }

  private String bufferGeometry(String wkt, double distance) {
    LOGGER.debug("Buffering WKT {} by distance {} meter(s).", wkt, distance);
    String bufferedWkt = null;
    try {
      Geometry geometry = getGeometryFromWkt(wkt);
      double bufferInDegrees = metersToDegrees(distance);
      LOGGER.debug(
          "Buffering {} by {} degree(s).", geometry.getClass().getSimpleName(), bufferInDegrees);
      Geometry bufferedGeometry = geometry.buffer(bufferInDegrees);
      bufferedWkt = new WKTWriter().write(bufferedGeometry);
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
    JAXBElement<LineStringType> jaxbElement = null;
    try {
      String gml = Wfs10JTStoGML200Converter.convertGeometryToGML(geometry);
      LineStringType lineStringType =
          (LineStringType)
              Wfs10JTStoGML200Converter.convertGMLToGeometryType(gml, Wfs10Constants.LINESTRING);
      jaxbElement =
          (JAXBElement<LineStringType>)
              Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(lineStringType);
    } catch (JAXBException jbe) {
      LOGGER.debug("Unable to create LineString with geometry: [{}]", geometry);
    }
    return jaxbElement;
  }

  private JAXBElement<MultiPointType> createMultiPoint(Geometry geometry) {
    JAXBElement<MultiPointType> jaxbElement = null;
    try {
      String gml = Wfs10JTStoGML200Converter.convertGeometryToGML(geometry);
      MultiPointType multiPointType =
          (MultiPointType)
              Wfs10JTStoGML200Converter.convertGMLToGeometryType(gml, Wfs10Constants.POINT);
      jaxbElement =
          (JAXBElement<MultiPointType>)
              Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiPointType);
    } catch (JAXBException jbe) {
      LOGGER.debug("Unable to create MultiPointType with geometry: [{}]", geometry);
    }
    return jaxbElement;
  }

  private JAXBElement<MultiLineStringType> createMultiLineString(Geometry geometry) {
    JAXBElement<MultiLineStringType> jaxbElement = null;
    try {
      String gml = Wfs10JTStoGML200Converter.convertGeometryToGML(geometry);
      MultiLineStringType multiLineStringType =
          (MultiLineStringType)
              Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                  gml, Wfs10Constants.MULTI_LINESTRING);
      jaxbElement =
          (JAXBElement<MultiLineStringType>)
              Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiLineStringType);
    } catch (JAXBException jbe) {
      LOGGER.debug("Unable to create MultiLineStringType with geometry: [{}]", geometry);
    }
    return jaxbElement;
  }

  private JAXBElement<MultiPolygonType> createMultiPolygon(Geometry geometry) {
    JAXBElement<MultiPolygonType> jaxbElement = null;
    try {
      String gml = Wfs10JTStoGML200Converter.convertGeometryToGML(geometry);
      MultiLineStringType multiLineStringType =
          (MultiLineStringType)
              Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                  gml, Wfs10Constants.MULTI_LINESTRING);
      jaxbElement =
          (JAXBElement<MultiPolygonType>)
              Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(multiLineStringType);
    } catch (JAXBException jbe) {
      LOGGER.debug("Unable to create MultiPolygonType with geometry: [{}]", geometry);
    }
    return jaxbElement;
  }

  private JAXBElement<GeometryCollectionType> createGeometryCollection(Geometry geometry) {
    JAXBElement<GeometryCollectionType> jaxbElement = null;
    try {
      String gml = Wfs10JTStoGML200Converter.convertGeometryToGML(geometry);
      GeometryCollectionType geometryCollectionType =
          (GeometryCollectionType)
              Wfs10JTStoGML200Converter.convertGMLToGeometryType(
                  gml, Wfs10Constants.GEOMETRY_COLLECTION);
      jaxbElement =
          (JAXBElement<GeometryCollectionType>)
              Wfs10JTStoGML200Converter.convertGeometryTypeToJAXB(geometryCollectionType);
    } catch (JAXBException jbe) {
      LOGGER.debug("Unable to create MultiPolygonType with geometry: [{}]", geometry);
    }
    return jaxbElement;
  }

  private JAXBElement<? extends AbstractGeometryType> createGeometryOperand(String wkt) {
    String convertedWkt = wkt;
    Geometry wktGeometry = null;
    try {
      wktGeometry = getGeometryFromWkt(convertedWkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse WKT Geometry [" + convertedWkt + "]", e);
    }
    if (wktGeometry instanceof Polygon) {
      if (isGeometryOperandSupported(Wfs10Constants.POLYGON)) {
        return createPolygon(convertedWkt);
      } else {
        throw new IllegalArgumentException("The Polygon operand is not supported.");
      }
    } else if (wktGeometry instanceof Point) {
      if (isGeometryOperandSupported(Wfs10Constants.POINT)) {
        return createPoint(convertedWkt);
      } else {
        throw new IllegalArgumentException("The Point operand is not supported.");
      }
    } else if (wktGeometry instanceof LineString) {
      if (isGeometryOperandSupported(Wfs10Constants.LINESTRING)) {
        return createLineString(wktGeometry);
      } else {
        throw new IllegalArgumentException("The LineString operand is not supported.");
      }
    } else if (wktGeometry instanceof MultiPoint) {
      if (isGeometryOperandSupported(Wfs10Constants.MULTI_POINT)) {
        return createMultiPoint(wktGeometry);
      } else {
        throw new IllegalArgumentException("The MultiPoint operand is not supported.");
      }
    } else if (wktGeometry instanceof MultiLineString) {
      if (isGeometryOperandSupported(Wfs10Constants.MULTI_LINESTRING)) {
        return createMultiLineString(wktGeometry);
      } else {
        throw new IllegalArgumentException("The MultiLineString operand is not supported.");
      }
    } else if (wktGeometry instanceof MultiPolygon) {
      if (isGeometryOperandSupported(Wfs10Constants.MULTI_POLYGON)) {
        return createMultiPolygon(wktGeometry);
      } else {
        throw new IllegalArgumentException("The MultiPolygon operand is not supported.");
      }
    } else if (wktGeometry instanceof GeometryCollection) {
      if (isGeometryOperandSupported(Wfs10Constants.GEOMETRY_COLLECTION)) {
        return createGeometryCollection(wktGeometry);
      } else {
        throw new IllegalArgumentException("The GeometryCollection operand is not supported.");
      }
    }
    throw new IllegalArgumentException("Unable to create Geometry from WKT String");
  }

  @SuppressWarnings("squid:S00115")
  private enum PROPERTY_IS_OPS {
    PropertyIsEqualTo,
    PropertyIsLike,
    PropertyIsNotEqualTo,
    PropertyIsGreaterThan,
    PropertyIsGreaterThanOrEqualTo,
    PropertyIsLessThan,
    PropertyIsLessThanOrEqualTo;
  }
}
