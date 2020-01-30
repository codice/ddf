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
package org.codice.ddf.catalog.ui.query.cql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import java.util.Collections;
import org.junit.Test;

public class CqlQueryResponseTest {

  @Test
  public void testChangeInWarningsAfterAdditionOfNewWarning() {

    CqlQueryResponse response =
        new CqlQueryResponse(
            null,
            mock(QueryRequest.class),
            mock(QueryResponse.class),
            null,
            0,
            false,
            mock(FilterAdapter.class),
            null,
            null);

    int hashOfResponseBeforeAdditionOfNewWarning = response.hashCode();
    response.addToWarnings(Collections.singleton("new warning"));

    int hashOfResponseAfterAdditionOfNewWarning = response.hashCode();

    assertThat(
        hashOfResponseBeforeAdditionOfNewWarning, is(not(hashOfResponseAfterAdditionOfNewWarning)));
  }

  @Test
  public void testStabilityOfWarningsAfterAdditionOfNullWarning() {

    CqlQueryResponse response =
        new CqlQueryResponse(
            null,
            mock(QueryRequest.class),
            mock(QueryResponse.class),
            null,
            0,
            false,
            mock(FilterAdapter.class),
            null,
            null);

    int hashOfResponseBeforeAdditionOfNullWarning = response.hashCode();
    response.addToWarnings(null);

    int hashOfResponseAfterAdditionOfNullWarning = response.hashCode();

    assertThat(
        hashOfResponseBeforeAdditionOfNullWarning, is(hashOfResponseAfterAdditionOfNullWarning));
  }

  @Test
  public void testStabilityOfWarningsAfterAdditionOfEmptyWarning() {

    CqlQueryResponse response =
        new CqlQueryResponse(
            null,
            mock(QueryRequest.class),
            mock(QueryResponse.class),
            null,
            0,
            false,
            mock(FilterAdapter.class),
            null,
            null);

    int hashOfResponseBeforeAdditionOfEmptyWarning = response.hashCode();
    response.addToWarnings(Collections.emptySet());

    int hashOfResponseAfterAdditionOfEmptyWarning = response.hashCode();

    assertThat(
        hashOfResponseBeforeAdditionOfEmptyWarning, is(hashOfResponseAfterAdditionOfEmptyWarning));
  }
}
