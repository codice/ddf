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
package org.codice.ddf.spatial.geocoding.feature;

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;

import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.impl.QueryImpl;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.geotools.api.filter.Filter;

public class CatalogHelper {

  private FilterBuilder filterBuilder;

  private Filter gazetteerFilter;

  private Filter countryShapeFilter;

  public CatalogHelper(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
    gazetteerFilter =
        filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(GAZETTEER_METACARD_TAG);
    countryShapeFilter =
        filterBuilder
            .attribute(Core.METACARD_TAGS)
            .is()
            .like()
            .text(GeoCodingConstants.COUNTRY_TAG);
  }

  public Query getQueryForAllCountries() {
    return new QueryImpl(filterBuilder.allOf(gazetteerFilter, countryShapeFilter));
  }

  public Query getQueryForName(String countrycode) {
    Filter countryCodeFilter =
        filterBuilder.attribute(Location.COUNTRY_CODE).is().equalTo().text(countrycode);
    return new QueryImpl(
        filterBuilder.allOf(countryCodeFilter, gazetteerFilter, countryShapeFilter));
  }
}
