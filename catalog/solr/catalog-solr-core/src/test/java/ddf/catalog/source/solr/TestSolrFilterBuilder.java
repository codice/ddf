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
package ddf.catalog.source.solr;

import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.ProximityFunction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.temporal.Instant;
import org.geotools.api.temporal.Period;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.FunctionImpl;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;

public class TestSolrFilterBuilder extends GeotoolsFilterBuilder {

  private FilterFactory factory = new FilterFactoryImpl();

  public Filter dateLessThan(String attribute, Date literal) {
    return factory.less(factory.property(attribute), factory.literal(literal));
  }

  public Filter dateLessThanOrEqual(String attribute, Date literal) {
    return factory.lessOrEqual(factory.property(attribute), factory.literal(literal));
  }

  public Filter dateGreaterThan(String attribute, Date literal) {
    return factory.greater(factory.property(attribute), factory.literal(literal));
  }

  public Filter dateGreaterThanOrEqual(String attribute, Date literal) {
    return factory.greaterOrEqual(factory.property(attribute), factory.literal(literal));
  }

  public Filter dateIsDuring(String attribute, Date lowerBoundary, Date upperBoundary) {
    return factory.during(
        factory.property(attribute), factory.literal(makePeriod(lowerBoundary, upperBoundary)));
  }

  public Filter proximity(String attribute, int distance, String searchTerms) {
    FunctionImpl function = new FunctionImpl();
    function.setName(ProximityFunction.FUNCTION_NAME.getName());
    List<Expression> parameters = new ArrayList<>();
    parameters.add(factory.literal(attribute));
    parameters.add(factory.literal(distance));
    parameters.add(factory.literal(searchTerms));
    function.setParameters(parameters);
    return factory.equals(function, factory.literal(true));
  }

  private Period makePeriod(Date start, Date end) {
    DefaultPosition defaultPosition = new DefaultPosition(start);
    Instant startInstant = new DefaultInstant(defaultPosition);
    Instant endInstant = new DefaultInstant(new DefaultPosition(end));
    Period period = new DefaultPeriod(startInstant, endInstant);
    return period;
  }
}
