/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.clavin;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardEnricher;

public class ClavinMetacardEnricher implements MetacardEnricher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinMetacardEnricher.class);

    private String clavinIndexLocation;

    public void setClavinIndexLocation(final String indexLocation) {
        this.clavinIndexLocation = indexLocation;
    }

    @Override
    public void enrich(Metacard metacard, InputStream input) throws CatalogTransformerException {
        try {
            File directoryFile = new File(clavinIndexLocation);
            LOGGER.info("clavin index: " + directoryFile.getAbsolutePath());
            GeoParser clavinParser = GeoParserFactory.getDefault(directoryFile.getAbsolutePath());

            // TODO: chunk inputstream into strings.
            String document = IOUtils.toString(input, "UTF-8");

            List<ResolvedLocation> resolvedLocationList = clavinParser.parse(document);
            if (!resolvedLocationList.isEmpty()) {
                for (ResolvedLocation resolvedLocation : resolvedLocationList) {
                    // heuristic to filter out "meaningless" locations.
                    if (resolvedLocation.getMatchedName().length() > 1) {
                        GeoName geoName = resolvedLocation.getGeoname();
                        // lat,lon
                        String lon = String.valueOf(geoName.getLongitude());
                        String lat = String.valueOf(geoName.getLatitude());
                        if (!StringUtils.isBlank(lon) && !StringUtils.isBlank(lat)) {
                            String wkt = String.format("POINT(%s %s)", lon, lat);
                            metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));
                        }

                        // country code.
                        String countryCode = geoName.getPrimaryCountryName();
                        metacard.setAttribute(new AttributeImpl("country-code", countryCode));

                        // elevation
                        String elevation = String.valueOf(geoName.getElevation());
                        metacard.setAttribute(new AttributeImpl("elevation", elevation));
                    }
                }
            }
        } catch (Exception e) {
            // clavin Geoparser#parse throws Exception !
            throw new CatalogTransformerException("Clavin failed to resolve geo location. ", e);
        }
    }
}
