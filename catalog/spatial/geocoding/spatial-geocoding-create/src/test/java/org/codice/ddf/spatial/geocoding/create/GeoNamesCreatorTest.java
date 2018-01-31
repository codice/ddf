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
package org.codice.ddf.spatial.geocoding.create;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.junit.Test;

public class GeoNamesCreatorTest extends TestBase {
  private static final GeoNamesCreator GEONAMES_CREATOR = new GeoNamesCreator();

  @Test
  public void testNoEmptyFields() {
    final String geoNamesEntryStr =
        "5289282\tChandler\tChandler\t"
            + "Candler,Candleris,Chandler,Chandlur\t33.30616\t-111.84125\tP\tPPL\tUS\tUS\tAZ\t"
            + "013\t012\t011\t236123\t370\t368\tAmerica/Phoenix\t2011-05-14";
    final GeoEntry geoEntry = GEONAMES_CREATOR.createGeoEntry(geoNamesEntryStr);
    verifyGeoEntry(
        geoEntry,
        "Chandler",
        33.30616,
        -111.84125,
        "PPL",
        236123,
        "Candler,Candleris,Chandler,Chandlur",
        "US");
  }

  @Test
  public void testSomeEmptyFields() {
    final String geoNamesEntryStr =
        "5288858\tCave Creek\tCave Creek\t\t33.83333\t"
            + "-111.95083\tP\tPPL\tUS\t\tAZ\t013\t\t\t5015\t648\t649\tAmerica/Phoenix\t"
            + "2011-05-14";
    final GeoEntry geoEntry = GEONAMES_CREATOR.createGeoEntry(geoNamesEntryStr);
    verifyGeoEntry(geoEntry, "Cave Creek", 33.83333, -111.95083, "PPL", 5015, "", "US");
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testNotEnoughFields() {
    final String wrongFormat = "5288858\tCave Creek\tCave Creek\tAlternate names\t33.83333";
    GEONAMES_CREATOR.createGeoEntry(wrongFormat);
  }

  @Test(expected = NumberFormatException.class)
  public void testWrongFieldOrder() {
    /* This string has the correct number of fields but the fields are in the wrong order. The
    GeoNamesCreator will attempt to parse the latitude and longitude from the string, but
    because there are non-double values where the latitude and longitude should be, a
    NumberFormatException should be thrown. */
    final String wrongFormat =
        "5289282\t33.30616\t-111.84125\t"
            + "Candler,Candleris,Chandler,Chandlur\tChandler\tChandler\tP\tPPL\tUS\tUS\tAZ\t"
            + "013\t012\t011\t236123\t370\t368\tAmerica/Phoenix\t2011-05-14";
    GEONAMES_CREATOR.createGeoEntry(wrongFormat);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testNotTabDelimited() {
    final String wrongFormat =
        "5289282,Chandler,Chandler,"
            + "Candler,Candleris,Chandler,Chandlu,33.30616,-111.84125,P,PPL,US,US,AZ,"
            + "013,012,011,236123,370,368,America/Phoenix,2011-05-14";
    GEONAMES_CREATOR.createGeoEntry(wrongFormat);
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testEmptyLine() {
    GEONAMES_CREATOR.createGeoEntry("");
  }
}
