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
package org.codice.ddf.catalog.ui.query.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.codice.usng4j.CoordinateSystemTranslator;
import org.codice.usng4j.DecimalDegreesCoordinate;
import org.codice.usng4j.UtmUpsCoordinate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class UtmUpsCoordinateProcessorTest {
  private final UtmUpsCoordinate mockUtmUps = mock(UtmUpsCoordinate.class);

  private final DecimalDegreesCoordinate mockDecimalDegrees = mock(DecimalDegreesCoordinate.class);

  private CoordinateSystemTranslator translatorMock;

  private UtmUpsCoordinateProcessor processor;

  @Before
  public void setup() throws ParseException {
    translatorMock = mock(CoordinateSystemTranslator.class);
    processor = new UtmUpsCoordinateProcessor(translatorMock);
    when(translatorMock.parseUtmUpsString(Matchers.anyString())).thenReturn(mockUtmUps);
    when(translatorMock.toLatLon((UtmUpsCoordinate) Matchers.anyObject()))
        .thenReturn(mockDecimalDegrees);
  }

  @Test
  public void testUtmStringNorthernHemisphere() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN");
    verifyInputs(1, "13N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringNorthernHemisphereWithMultipleSpaces() {
    processor.enhanceResults(new LinkedList<>(), "13N   234789mE   234789mN");
    verifyInputs(1, "13N   234789mE   234789mN");
  }

  @Test
  public void testUtmStringNorthernHemisphereWithTabs() {
    processor.enhanceResults(
        new LinkedList<>(), String.format("%s\t%s\t%s", "13N", "234789mE", "234789mN"));
    verifyInputs(0);
  }

  @Test
  public void testUtmStringSouthernHemisphere() {
    processor.enhanceResults(new LinkedList<>(), "13M 234789mE 234789mN");
    verifyInputs(1, "13M 234789mE 234789mN");
  }

  @Test
  public void testUtmStringZoneTooSmall() {
    processor.enhanceResults(new LinkedList<>(), "00N 234789mE 234789mN");
    verifyInputs(0);
  }

  @Test
  public void testUtmStringZoneTooBig() {
    processor.enhanceResults(new LinkedList<>(), "61N 234789mE 234789mN");
    verifyInputs(1, "1N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringBadLatBand() {
    processor.enhanceResults(new LinkedList<>(), "13I 234789mE 234789mN");
    verifyInputs(0);
  }

  @Test
  public void testUtmStringWithoutLatBand() {
    processor.enhanceResults(new LinkedList<>(), "13 234789mE 234789mN");
    verifyInputs(1, "13N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringWithNegativeNorthing() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE -234789mN");
    verifyInputs(0);
  }

  @Test
  public void testUtmStringWithNorthernHemisphereIndicator() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN N");
    verifyInputs(1, "13N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringWithSouthernHemisphereIndicator() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN S");
    verifyInputs(1, "13N 234789mE 234789mN");
  }

  @Test
  public void testUtmStringWithInvalidNSIndicator() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN R");
    verifyInputs(1, "13N 234789mE 234789mN");
  }

  @Test
  public void testUpsStringWithInvalidLatBand() {
    processor.enhanceResults(new LinkedList<>(), "C 2347891mE 2347891mN");
    verifyInputs(0);
  }

  @Test
  public void testUpsString() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891mN");
    verifyInputs(1, "A 2347891mE 2347891mN");
  }

  @Test
  public void testUpsStringWithoutEastingUnits() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891 2347891mN");
    verifyInputs(1, "A 2347891 2347891mN");
  }

  @Test
  public void testUpsStringWithoutNorthingUnits() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891");
    verifyInputs(1, "A 2347891mE 2347891");
  }

  @Test
  public void testUpsStringWithRedundantZoneZero() {
    processor.enhanceResults(new LinkedList<>(), "0A 2347891mE 2347891mN");
    verifyInputs(1, "0A 2347891mE 2347891mN");
  }

  @Test
  public void testUpsStringDisregardsNorthIndicator() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891mN N");
    verifyInputs(1, "A 2347891mE 2347891mN");
  }

  @Test
  public void testUpsStringDisregardsSouthIndicator() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891mN S");
    verifyInputs(1, "A 2347891mE 2347891mN");
  }

  @Test
  public void testUtmFollowedByUtm() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN 22X 234789mE 234789mN");
    verifyInputs(2, "13N 234789mE 234789mN", "22X 234789mE 234789mN");
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUtm() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789 22X 234789mE 234789mN");
    verifyInputs(2, "13N 234789mE 234789", "22X 234789mE 234789mN");
  }

  @Test
  public void testUpsFollowedByUps() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891mN B 1077891mE 1077891mN");
    verifyInputs(2, "A 2347891mE 2347891mN", "B 1077891mE 1077891mN");
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUps() {
    processor.enhanceResults(new LinkedList<>(), "A 2347891mE 2347891 B 1077891mE 1077891mN");
    verifyInputs(2, "A 2347891mE 2347891", "B 1077891mE 1077891mN");
  }

  @Test
  public void testUtmFollowedByUps() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789mN B 1077891mE 1077891mN");
    verifyInputs(2, "13N 234789mE 234789mN", "B 1077891mE 1077891mN");
  }

  @Test
  public void testUtmWithoutUnitLabelFollowedByUps() {
    processor.enhanceResults(new LinkedList<>(), "13N 234789mE 234789 B 1077891mE 1077891mN");
    verifyInputs(2, "13N 234789mE 234789", "B 1077891mE 1077891mN");
  }

  @Test
  public void testUpsFollowedByUtm() {
    processor.enhanceResults(new LinkedList<>(), "B 1077891mE 1077891mN 13N 234789mE 234789mN");
    verifyInputs(2, "B 1077891mE 1077891mN", "13N 234789mE 234789mN");
  }

  @Test
  public void testUpsWithoutUnitLabelFollowedByUtm() {
    processor.enhanceResults(new LinkedList<>(), "B 1077891mE 1077891 13N 234789mE 234789mN");
    verifyInputs(2, "B 1077891mE 1077891", "13N 234789mE 234789mN");
  }

  private void verifyInputs(int times, String... inputs) {
    try {
      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      List<String> listOfExpectedInputs = Arrays.asList(inputs);
      assertThat(listOfExpectedInputs.size(), is(equalTo(times)));
      verify(translatorMock, times(times)).parseUtmUpsString(captor.capture());
      verify(translatorMock, times(times)).toLatLon(eq(mockUtmUps));
      for (int i = 0; i < times; i++) {
        assertThat(captor.getAllValues().get(i), is(equalTo(listOfExpectedInputs.get(i))));
      }
      verifyNoMoreInteractions(translatorMock);
    } catch (Exception e) {
      fail(
          "No exception should occur when verifying inputs but one occured with message: "
              + e.getMessage());
    }
  }
}
