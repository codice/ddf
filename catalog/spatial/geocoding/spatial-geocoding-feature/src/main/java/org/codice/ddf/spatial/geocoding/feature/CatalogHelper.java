package org.codice.ddf.spatial.geocoding.feature;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.impl.QueryImpl;
import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.GazetteerConstants;
import org.opengis.filter.Filter;

public class CatalogHelper {

  private FilterBuilder filterBuilder;
  private List<Filter> countryFilters;

  public CatalogHelper(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;

    countryFilters = new ArrayList<>();
    countryFilters.add(
        filterBuilder
            .attribute(Core.METACARD_TAGS)
            .is()
            .like()
            .text(GazetteerConstants.DEFAULT_TAG));
    countryFilters.add(
        filterBuilder
            .attribute(Core.METACARD_TAGS)
            .is()
            .like()
            .text(GazetteerConstants.COUNTRY_TAG));
  }

  public Query getQueryForAllCountries() {
    return new QueryImpl(filterBuilder.allOf(countryFilters));
  }

  public Query getQueryForCountryCode(String countryCode) {
    List<Filter> filters = new ArrayList<>(countryFilters);
    filters.add(filterBuilder.attribute(Core.TITLE).is().equalTo().text(countryCode));
    return new QueryImpl(filterBuilder.allOf(filters));
  }
}
