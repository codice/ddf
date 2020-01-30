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
package ddf.catalog.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import ddf.catalog.operation.impl.SourceProcessingDetailsImpl;
import java.util.Collections;
import org.junit.Test;

public class SourceProcessingDetailsImplTest {

  @Test
  public void testEquality() {
    SourceProcessingDetails sourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    SourceProcessingDetails identicalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    assertThat(sourceProcessingDetails, is(identicalSourceProcessingDetails));
  }

  @Test
  public void testInequality() {
    SourceProcessingDetails sourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    SourceProcessingDetails unequalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("different warning"));
    assertThat(sourceProcessingDetails, not(unequalSourceProcessingDetails));
  }

  @Test
  public void testEqualityWithNullWarnings() {
    SourceProcessingDetails sourceProcessingDetails = new SourceProcessingDetailsImpl(null);
    SourceProcessingDetails identicalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(null);
    assertThat(sourceProcessingDetails, is(identicalSourceProcessingDetails));
  }

  @Test
  public void testEqualityOfHashCodes() {
    SourceProcessingDetails sourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    SourceProcessingDetails identicalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    assertThat(
        "The hashCodes of SourceProcessingDetails with equal warnings should have been equal, but were not.",
        sourceProcessingDetails.hashCode(),
        is(identicalSourceProcessingDetails.hashCode()));
  }

  @Test
  public void testInequalityOfHashCodes() {
    SourceProcessingDetails sourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("warning"));
    SourceProcessingDetails unequalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(Collections.singletonList("different warning"));
    assertThat(
        "The hashCodes of SourceProcessingDetails with unequal warnings should not have been equal, but were.",
        sourceProcessingDetails.hashCode(),
        not(unequalSourceProcessingDetails.hashCode()));
  }

  @Test
  public void testEqualityOfHashCodesForNullWarnings() {
    SourceProcessingDetails sourceProcessingDetails = new SourceProcessingDetailsImpl(null);
    SourceProcessingDetails identicalSourceProcessingDetails =
        new SourceProcessingDetailsImpl(null);
    assertThat(
        "The hashCodes of SourceProcessingDetails with null warnings should be equal, but were not.",
        sourceProcessingDetails.hashCode(),
        is(identicalSourceProcessingDetails.hashCode()));
  }
}
