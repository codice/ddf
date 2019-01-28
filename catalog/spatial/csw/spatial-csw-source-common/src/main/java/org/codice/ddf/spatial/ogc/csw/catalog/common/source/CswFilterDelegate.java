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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.GeometryOperandsType;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants.BinarySpatialOperand;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CswFilterDelegate is an implementation of a {@link ddf.catalog.filter.FilterDelegate}. It extends
 * {@link CswAbstractFilterDelegate} and converts a {@link org.opengis.filter.Filter} into a {@link
 * net.opengis.filter.v_1_1_0.FilterType}.
 *
 * <p>Generic type that the FilterDelegate will return as a final result
 */
public class CswFilterDelegate extends CswAbstractFilterDelegate<FilterType> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswFilterDelegate.class);

  // 1000 Nautical Miles in meters
  private static final double NEAREST_NEIGHBOR_DISTANCE_LIMIT = 1852000;

  private final CswSourceConfiguration cswSourceConfiguration;

  private final CswFilterFactory cswFilterFactory;

  // according to the spec, this will include at a minimum: equal, not equal,
  // less, greater, greater or equal, less or equal, and like
  private Set<ComparisonOperatorType> comparisonOps;

  // according to the spec, this will include at a minimum: bbox
  private Map<SpatialOperatorNameType, SpatialOperatorType> spatialOps;

  private List<QName> globalGeometryOperands;

  // according to the spec, logicalOps (and/or/not) are always supported
  private boolean logicalOps;

  /**
   * Instantiates a CswFilterDelegate instance
   *
   * @param getRecordsOp An {@link net.opengis.ows.v_1_0_0.Operation} for the getRecords feature of
   *     the Csw service
   * @param filterCapabilities The {@link net.opengis.filter.v_1_1_0.FilterCapabilities} understood
   *     by the Csw service
   * @param outputFormatValues An {@link net.opengis.ows.v_1_0_0.DomainType} containing a list of
   *     valid Output Formats supported
   * @param resultTypesValues An {@link net.opengis.ows.v_1_0_0.DomainType} containing a list of
   *     Result Types supported
   */
  public CswFilterDelegate(
      Operation getRecordsOp,
      FilterCapabilities filterCapabilities,
      DomainType outputFormatValues,
      DomainType resultTypesValues,
      CswSourceConfiguration cswSourceConfiguration) {
    super(getRecordsOp, outputFormatValues, resultTypesValues);
    this.cswSourceConfiguration = cswSourceConfiguration;
    cswFilterFactory =
        new CswFilterFactory(
            cswSourceConfiguration.getCswAxisOrder(), cswSourceConfiguration.isSetUsePosList());
    updateAllowedOperations(filterCapabilities);
  }

  @Override
  public FilterType propertyIsEqualTo(String functionName, List<Object> arguments, Object literal) {
    // This assumes the first argument is a property name followed by 0-N literals.
    // If a new function is added that doesn't conform to this a switch can be added to handle
    // that case. Most cases will work even if they don't conform to the signature.
    return cswFilterFactory.buildPropertyIsEqualTo(functionName, arguments, literal);
  }

  @Override
  public FilterType and(List<FilterType> filters) {
    areLogicalOperationsSupported();
    return cswFilterFactory.buildAndFilter(filters);
  }

  @Override
  public FilterType or(List<FilterType> filters) {
    areLogicalOperationsSupported();
    if (filters.contains(null)) {
      throw new UnsupportedOperationException("Invalid filters found in list of filters!");
    }
    return cswFilterFactory.buildOrFilter(filters);
  }

  @Override
  public FilterType not(FilterType filter) {
    areLogicalOperationsSupported();
    return cswFilterFactory.buildNotFilter(filter);
  }

  @Override
  public FilterType propertyIsEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(propertyName, literal, isCaseSensitive);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, convertDateToIso8601Format(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Integer.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Short.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Long.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Float.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Double.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, boolean literal) {
    isComparisonOperationSupported(ComparisonOperatorType.EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsEqualToFilter(
          propertyName, Boolean.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, literal, isCaseSensitive);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, convertDateToIso8601Format(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Integer.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Short.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Long.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Float.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Double.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, boolean literal) {
    isComparisonOperationSupported(ComparisonOperatorType.NOT_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNotEqualToFilter(
          propertyName, Boolean.valueOf(literal), true);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, String literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(propertyName, literal);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(
          propertyName, convertDateToIso8601Format(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(
          propertyName, Integer.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(
          propertyName, Short.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(propertyName, Long.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(
          propertyName, Float.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanFilter(
          propertyName, Double.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(propertyName, literal);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, convertDateToIso8601Format(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, Integer.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, Short.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, Long.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, Float.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsGreaterThanOrEqualToFilter(
          propertyName, Double.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, String literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, literal);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(
          propertyName, convertDateToIso8601Format(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, Integer.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, Short.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, Long.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, Float.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanFilter(propertyName, Double.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, String literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(propertyName, literal);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, convertDateToIso8601Format(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, int literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, Integer.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, short literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, Short.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, long literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, Long.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, float literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, Float.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, double literal) {
    isComparisonOperationSupported(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLessThanOrEqualToFilter(
          propertyName, Double.valueOf(literal));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, String lowerBoundary, String upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, lowerBoundary, upperBoundary);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName,
          convertDateToIso8601Format(lowerBoundary),
          convertDateToIso8601Format(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(String propertyName, int lowerBoundary, int upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, Integer.valueOf(lowerBoundary), Integer.valueOf(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, short lowerBoundary, short upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, Short.valueOf(lowerBoundary), Short.valueOf(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(String propertyName, long lowerBoundary, long upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, Long.valueOf(lowerBoundary), Long.valueOf(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, float lowerBoundary, float upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, Float.valueOf(lowerBoundary), Float.valueOf(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, double lowerBoundary, double upperBoundary) {
    isComparisonOperationSupported(ComparisonOperatorType.BETWEEN);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsBetweenFilter(
          propertyName, Double.valueOf(lowerBoundary), Double.valueOf(upperBoundary));
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsNull(String propertyName) {
    isComparisonOperationSupported(ComparisonOperatorType.NULL_CHECK);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsNullFilter(propertyName);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
    isComparisonOperationSupported(ComparisonOperatorType.LIKE);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsLikeFilter(propertyName, pattern, isCaseSensitive);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType propertyIsFuzzy(String propertyName, String pattern) {
    isComparisonOperationSupported(ComparisonOperatorType.FUZZY);
    propertyName = mapPropertyName(propertyName);
    if (isPropertyQueryable(propertyName)) {
      return cswFilterFactory.buildPropertyIsFuzzyFilter(propertyName, pattern);
    } else {
      return new FilterType();
    }
  }

  @Override
  public FilterType xpathIsFuzzy(String xpath, String literal) {
    return propertyIsFuzzy(xpath, literal);
  }

  @Override
  public FilterType xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
    return propertyIsLike(xpath, pattern, isCaseSensitive);
  }

  @Override
  public FilterType xpathExists(String xpath) {
    return not(propertyIsNull(xpath));
  }

  @Override
  public FilterType beyond(String propertyName, String wkt, double distance) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.BEYOND,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.BEYOND)) {
      BinarySpatialOperand beyondBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.BEYOND, wkt);
      if (beyondBinarySpatialOperand == BinarySpatialOperand.GEOMETRY) {
        return cswFilterFactory.buildBeyondGeospatialFilter(propertyName, wkt, distance);
      }
    }

    // If beyond is not supported, fallback to not(dwithin())
    if (isSpatialOperationSupported(SpatialOperatorNameType.D_WITHIN)) {
      BinarySpatialOperand dwithinBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.D_WITHIN, wkt);
      if (dwithinBinarySpatialOperand == BinarySpatialOperand.GEOMETRY) {
        return not(dwithin(propertyName, wkt, distance));
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.BEYOND.name()
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType contains(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.CONTAINS.name(),
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.CONTAINS)) {
      BinarySpatialOperand containsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.CONTAINS, wkt);
      if (containsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildContainsGeospatialFilter(
            propertyName, wkt, containsBinarySpatialOperand);
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.WITHIN)) {
      BinarySpatialOperand withinBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.WITHIN, wkt);
      if (withinBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return not(within(propertyName, wkt));
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.CONTAINS.name()
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType disjoint(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order in LON/LAT order.",
        SpatialOperatorNameType.DISJOINT,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.DISJOINT)) {
      BinarySpatialOperand disjointBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.DISJOINT, wkt);
      if (disjointBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildDisjointGeospatialFilter(
            propertyName, wkt, disjointBinarySpatialOperand);
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.BBOX)) {
      BinarySpatialOperand bboxBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.BBOX, wkt);
      // BBOX only supports Envelope
      if (bboxBinarySpatialOperand == BinarySpatialOperand.ENVELOPE) {
        return not(cswFilterFactory.buildBBoxGeospatialFilter(propertyName, wkt));
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.INTERSECTS)) {
      BinarySpatialOperand intersectsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.INTERSECTS, wkt);
      if (intersectsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return not(intersects(propertyName, wkt));
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.DISJOINT.name()
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType crosses(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.CROSSES,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.CROSSES)) {
      BinarySpatialOperand binarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.CROSSES, wkt);
      if (binarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildCrossesGeospatialFilter(
            propertyName, wkt, binarySpatialOperand);
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.CROSSES.name()
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType dwithin(String propertyName, String wkt, double distance) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.D_WITHIN,
        propertyName,
        wkt,
        distance);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.D_WITHIN)) {
      BinarySpatialOperand dwithinBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.D_WITHIN, wkt);
      if (dwithinBinarySpatialOperand == BinarySpatialOperand.GEOMETRY) {
        return cswFilterFactory.buildDWithinGeospatialFilter(propertyName, wkt, distance);
      }
    }

    LOGGER.debug(
        "Unable to construct {} spatial filter. Attempting to fall back to NOT {}.",
        SpatialOperatorNameType.D_WITHIN,
        SpatialOperatorNameType.BEYOND);
    if (isSpatialOperationSupported(SpatialOperatorNameType.BEYOND)) {
      BinarySpatialOperand beyondBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.BEYOND, wkt);
      if (beyondBinarySpatialOperand == BinarySpatialOperand.GEOMETRY) {
        return not(beyond(propertyName, wkt, distance));
      }
    }

    LOGGER.debug(
        "Unable to construct NOT {} spatial filter. Attempting to fall back to {}.",
        SpatialOperatorNameType.BEYOND,
        SpatialOperatorNameType.INTERSECTS);
    if (isSpatialOperationSupported(SpatialOperatorNameType.INTERSECTS)) {
      String bufferedWkt = bufferGeometry(wkt, distance);
      BinarySpatialOperand intersectsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.INTERSECTS, bufferedWkt);
      if (intersectsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return intersects(propertyName, bufferedWkt);
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.D_WITHIN.name()
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType nearestNeighbor(String propertyName, String wkt) {
    // Convert Nearest Neighbor to DWithin
    return dwithin(propertyName, wkt, NEAREST_NEIGHBOR_DISTANCE_LIMIT);
  }

  @Override
  public FilterType intersects(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.INTERSECTS,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.INTERSECTS)) {
      LOGGER.debug("Building INTERSECTS filter");

      BinarySpatialOperand intersectsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.INTERSECTS, wkt);
      LOGGER.debug("BinarySpatialOperand: {}", intersectsBinarySpatialOperand);

      if (intersectsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildIntersectsGeospatialFilter(
            propertyName, wkt, intersectsBinarySpatialOperand);
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.BBOX)) {
      BinarySpatialOperand bboxBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.BBOX, wkt);
      // BBOX only supports Envelope
      if (bboxBinarySpatialOperand == BinarySpatialOperand.ENVELOPE) {
        LOGGER.debug("Falling back to {} filter.", SpatialOperatorNameType.BBOX);
        return cswFilterFactory.buildBBoxGeospatialFilter(propertyName, wkt);
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.DISJOINT)) {
      BinarySpatialOperand disjointBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.DISJOINT, wkt);
      if (disjointBinarySpatialOperand != BinarySpatialOperand.NONE) {
        LOGGER.debug("Falling back to {} filter.", SpatialOperatorNameType.DISJOINT);
        return not(disjoint(propertyName, wkt));
      }
    }

    String message =
        "CSW source does not support "
            + SpatialOperatorNameType.INTERSECTS.name()
            + " for "
            + getGeometryOperandFromWkt(wkt)
            + " filter or any of its fallback spatial filters. This may be due to spatial operators not being supported "
            + "or geometry operands not being supported.  See the Get Capabilities Response to determine the cause.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType overlaps(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.OVERLAPS,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.OVERLAPS)) {
      BinarySpatialOperand overlapsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.OVERLAPS, wkt);
      if (overlapsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildOverlapsGeospatialFilter(
            propertyName, wkt, overlapsBinarySpatialOperand);
      }
    }

    String message =
        "CSW source does not support " + SpatialOperatorNameType.OVERLAPS.name() + " filter.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType touches(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.TOUCHES,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.TOUCHES)) {
      BinarySpatialOperand touchesBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.TOUCHES, wkt);
      if (touchesBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildTouchesGeospatialFilter(
            propertyName, wkt, touchesBinarySpatialOperand);
      }
    }

    String message =
        "CSW source does not support " + SpatialOperatorNameType.TOUCHES.name() + " filter.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType within(String propertyName, String wkt) {

    LOGGER.debug(
        "Attempting to build {} filter for property {} and WKT {} in LON/LAT order.",
        SpatialOperatorNameType.WITHIN,
        propertyName,
        wkt);

    if (isAnyGeo(propertyName)) {
      propertyName = mapPropertyName(propertyName);
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.WITHIN)) {
      BinarySpatialOperand withinBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.WITHIN, wkt);
      if (withinBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return cswFilterFactory.buildWithinGeospatialFilter(
            propertyName, wkt, withinBinarySpatialOperand);
      }
    }

    if (isSpatialOperationSupported(SpatialOperatorNameType.CONTAINS)) {
      LOGGER.debug("Falling back to {} filter.", SpatialOperatorNameType.CONTAINS);
      BinarySpatialOperand containsBinarySpatialOperand =
          useGeometryOrEnvelope(SpatialOperatorNameType.CONTAINS, wkt);
      if (containsBinarySpatialOperand != BinarySpatialOperand.NONE) {
        return contains(propertyName, wkt);
      }
    }
    String message =
        "CSW source does not support " + SpatialOperatorNameType.WITHIN.name() + " filter.";
    throw new UnsupportedOperationException(message);
  }

  @Override
  public FilterType during(String propertyName, Date startDate, Date endDate) {
    return cswFilterFactory.buildPropertyIsBetweenFilter(
        mapPropertyName(propertyName),
        convertDateToIso8601Format(startDate),
        convertDateToIso8601Format(endDate));
  }

  @Override
  public FilterType relative(String propertyName, long duration) {
    Date currentDate = new Date();
    return during(propertyName, new Date(currentDate.getTime() - duration), currentDate);
  }

  @Override
  public FilterType before(String propertyName, Date date) {
    return cswFilterFactory.buildPropertyIsLessThanFilter(
        mapPropertyName(propertyName), convertDateToIso8601Format(date));
  }

  @Override
  public FilterType after(String propertyName, Date date) {
    return cswFilterFactory.buildPropertyIsGreaterThanFilter(
        mapPropertyName(propertyName), convertDateToIso8601Format(date));
  }

  private DateTime convertDateToIso8601Format(Date inputDate) {
    return new DateTime(inputDate);
  }

  protected String mapPropertyName(String propertyName) {
    if (isAnyText(propertyName) || isMetadata(propertyName)) {
      propertyName = CswConstants.ANY_TEXT;
    } else if (isAnyGeo(propertyName)) {
      propertyName = CswConstants.BBOX_PROP;
    }
    propertyName =
        StringUtils.defaultIfBlank(
            cswSourceConfiguration.getMetacardMapping(propertyName), propertyName);

    return propertyName;
  }

  private boolean isAnyText(String propertyName) {
    return Metacard.ANY_TEXT.equalsIgnoreCase(propertyName);
  }

  private boolean isAnyGeo(String propertyName) {
    return Metacard.ANY_GEO.equalsIgnoreCase(propertyName);
  }

  protected boolean isContentTypeVersion(String propertyName) {
    return Metacard.CONTENT_TYPE_VERSION.equalsIgnoreCase(propertyName);
  }

  protected boolean isMetadata(String propertyName) {
    return Core.METADATA.equalsIgnoreCase(propertyName);
  }

  private boolean isSpatialOperationSupported(SpatialOperatorNameType operation) {
    return spatialOps.containsKey(operation);
  }

  private Geometry getGeometryFromWkt(String wkt) {
    try {
      return new WKTReader().read(wkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse WKT: " + wkt, e);
    }
  }

  private BinarySpatialOperand useGeometryOrEnvelope(
      SpatialOperatorNameType spatialOperatorName, String wkt) {
    BinarySpatialOperand binarySpatialOperand = null;
    boolean isEnvelopeSupported = false;
    boolean isGeometrySupported = false;

    String wktGeometryOperand = getGeometryOperandFromWkt(wkt);

    LOGGER.debug(
        "Attempting to determine if geometry operand [{}] is supported for spatial operator [{}].",
        wktGeometryOperand,
        spatialOperatorName);

    /**
     * Check if the geometry operand is supported for the specific spatial operation. For example,
     * check if the geometry operand POINT is supported for the spatial operator INTERSECTS.
     */
    for (QName geometryOperand : getGeometryOperands(spatialOperatorName)) {
      String localPart = geometryOperand.getLocalPart();
      LOGGER.debug("Geometry operand from Get Capabilities Response: {}", localPart);
      if (wktGeometryOperand.equalsIgnoreCase(localPart)) {
        isGeometrySupported = true;
      } else if (BinarySpatialOperand.ENVELOPE.toString().equalsIgnoreCase(localPart)) {
        isEnvelopeSupported = true;
      }
    }

    /**
     * Check if the geometry operand is supported for the specific spatial operation by checking the
     * global geometry operands (geometry operands that apply to all spatial operators).
     */
    if (globalGeometryOperands != null) {
      for (QName geometryOperand : globalGeometryOperands) {
        String localPart = geometryOperand.getLocalPart();
        LOGGER.debug(
            "Geometry operand from Get Capabilities Response (Global to all Spatial Operators): {}",
            localPart);
        if (wktGeometryOperand.equalsIgnoreCase(localPart)) {
          isGeometrySupported = true;
        } else if (BinarySpatialOperand.ENVELOPE.toString().equalsIgnoreCase(localPart)) {
          isEnvelopeSupported = true;
        }
      }
    }

    LOGGER.debug("Is geometry [{}] supported? {}", wktGeometryOperand, isGeometrySupported);
    LOGGER.debug("Is envelope supported? {}", isEnvelopeSupported);

    /**
     * In most cases, if the Geometry is supported use it; otherwise, use an Envelope. Geometry is
     * not valid for BBOX, only use Envelope. Envelope is not valid for Beyond and D_Within, only
     * use Geometry.
     */
    if (isGeometrySupported && spatialOperatorName != SpatialOperatorNameType.BBOX) {
      binarySpatialOperand = BinarySpatialOperand.GEOMETRY;
    } else if (isEnvelopeSupported
        && spatialOperatorName != SpatialOperatorNameType.BEYOND
        && spatialOperatorName != SpatialOperatorNameType.D_WITHIN) {
      binarySpatialOperand = BinarySpatialOperand.ENVELOPE;
    } else {
      binarySpatialOperand = BinarySpatialOperand.NONE;
    }

    LOGGER.debug("Use geometry or envelope? {}", binarySpatialOperand);
    return binarySpatialOperand;
  }

  private String getGeometryOperandFromWkt(String wkt) {
    return wkt.split("\\(")[0].trim();
  }

  private List<QName> getGeometryOperands(SpatialOperatorNameType spatialOperatorName) {
    SpatialOperatorType spatialOperatorType = spatialOps.get(spatialOperatorName);
    List<QName> geometryOperands = new ArrayList<>();
    if (spatialOperatorType != null) {
      GeometryOperandsType geometryOperandsType = spatialOperatorType.getGeometryOperands();
      if (geometryOperandsType != null) {
        geometryOperands = geometryOperandsType.getGeometryOperand();
      }
    }

    return geometryOperands;
  }

  private String bufferGeometry(String wkt, double distance) {
    LOGGER.debug("Buffering WKT {} by distance {} meter(s).", wkt, distance);
    Geometry geometry = getGeometryFromWkt(wkt);
    double bufferInDegrees = metersToDegrees(distance);
    LOGGER.debug(
        "Buffering {} by {} degree(s).", geometry.getClass().getSimpleName(), bufferInDegrees);
    Geometry bufferedGeometry = geometry.buffer(bufferInDegrees);
    String bufferedWkt = new WKTWriter().write(bufferedGeometry);
    LOGGER.debug("Buffered WKT: {}.", bufferedWkt);
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
        (distance / CswConstants.EARTH_MEAN_RADIUS_METERS) * CswConstants.RADIANS_TO_DEGREES;
    LOGGER.debug("{} meter(s) is approximately {} degree(s) of latitude.", distance, degrees);
    return degrees;
  }

  private void isComparisonOperationSupported(ComparisonOperatorType operation) {
    if (!comparisonOps.contains(operation)) {
      throw new UnsupportedOperationException(
          "Unsupported Property Comparison Type of [" + operation.value() + "].");
    }
  }

  private void areLogicalOperationsSupported() {
    if (!logicalOps) {
      throw new UnsupportedOperationException("Logical Operations are not supported.");
    }
  }

  /**
   * Reads the {@link net.opengis.filter.v_1_1_0.FilterCapabilities} in order to determine what
   * types of queries the server can handle.
   *
   * @param filterCapabilities The {@link net.opengis.filter.v_1_1_0.FilterCapabilities} understood
   *     by the Csw service
   */
  private final void updateAllowedOperations(FilterCapabilities filterCapabilities) {
    comparisonOps =
        Collections.newSetFromMap(
            new ConcurrentHashMap<>(new EnumMap<>(ComparisonOperatorType.class)));

    spatialOps = new ConcurrentHashMap<>(new EnumMap<>(SpatialOperatorNameType.class));
    logicalOps = true;
    if (null == filterCapabilities) {
      LOGGER.info("CSW Service doesn't support any filters");
      return;
    }

    ScalarCapabilitiesType scalarCapabilities = filterCapabilities.getScalarCapabilities();

    if (null != scalarCapabilities) {
      ComparisonOperatorsType comparisonOperators = scalarCapabilities.getComparisonOperators();
      if (null != comparisonOperators) { // filter out nulls
        for (ComparisonOperatorType comp : comparisonOperators.getComparisonOperator()) {
          if (null != comp) {
            comparisonOps.add(comp);
          }
        }
      }
      logicalOps = (null != scalarCapabilities.getLogicalOperators());
    }

    SpatialCapabilitiesType spatialCapabilities = filterCapabilities.getSpatialCapabilities();
    if (null != spatialCapabilities && null != spatialCapabilities.getSpatialOperators()) {
      setSpatialOps(spatialCapabilities.getSpatialOperators());
    }

    GeometryOperandsType geometryOperandsType = null;

    if (spatialCapabilities != null) {
      geometryOperandsType = spatialCapabilities.getGeometryOperands();
    }
    if (geometryOperandsType != null) {
      globalGeometryOperands = geometryOperandsType.getGeometryOperand();
      LOGGER.debug("globalGeometryOperands: {}", globalGeometryOperands);
    }
  }

  public GeometryOperandsType getGeoOpsForSpatialOp(SpatialOperatorNameType name) {
    SpatialOperatorType sot = spatialOps.get(name);
    if (sot != null) {
      return sot.getGeometryOperands();
    }
    return null;
  }

  public void setSpatialOps(SpatialOperatorsType spatialOperators) {
    spatialOps = new ConcurrentHashMap<>(new EnumMap<>(SpatialOperatorNameType.class));

    for (SpatialOperatorType spatialOp : spatialOperators.getSpatialOperator()) {
      LOGGER.debug("Adding key [spatialOp Name: {}]", spatialOp.getName());
      spatialOps.put(spatialOp.getName(), spatialOp);
      LOGGER.debug("spatialOps Map: {}", spatialOps);
    }
  }
}
