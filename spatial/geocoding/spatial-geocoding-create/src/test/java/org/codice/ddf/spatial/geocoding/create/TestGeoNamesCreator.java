/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoding.create;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.junit.Test;

public class TestGeoNamesCreator extends TestBase {
    @Test
    public void testNoEmptyFields() {
        final String geoNamesEntryStr = "5289282\tChandler\tChandler\t" +
                "Candler,Candleris,Chandler,Chandlur\t33.30616\t-111.84125\tP\tPPL\tUS\tUS\tAZ\t" +
                "013\t012\t011\t236123\t370\t368\tAmerica/Phoenix\t2011-05-14";
        final GeoNamesCreator geoNamesCreator = new GeoNamesCreator();
        final GeoEntry geoEntry = geoNamesCreator.createGeoEntry(geoNamesEntryStr);
        verifyGeoEntry(geoEntry, "Chandler", 33.30616, -111.84125, "PPL", 236123);
    }

    @Test
    public void testSomeEmptyFields() {
        final String geoNamesEntryStr = "5288858\tCave Creek\tCave Creek\t\t33.83333\t" +
                "-111.95083\tP\tPPL\tUS\t\tAZ\t013\t\t\t5015\t648\t649\tAmerica/Phoenix\t" +
                "2011-05-14";
        final GeoNamesCreator geoNamesCreator = new GeoNamesCreator();
        final GeoEntry geoEntry = geoNamesCreator.createGeoEntry(geoNamesEntryStr);
        verifyGeoEntry(geoEntry, "Cave Creek", 33.83333, -111.95083, "PPL", 5015);
    }
}