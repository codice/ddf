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

import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ProcessingDetailsImplTest {

  @Test
  public void testEquality() {
    Exception exception = new UnsupportedQueryException("We do not support this query");
    ProcessingDetails processingDetails =
        new ProcessingDetailsImpl("test source", exception, "warning");
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl("test source", exception, "warning");
    assertThat(processingDetails, is(identicalProcessingDetails));
  }

  @Test
  public void testInequalityOfSourceIds() {
    String sourceId = "test source";
    String differentSourceId = "different test source";
    Exception exception = new UnsupportedQueryException("We do not support this query");
    List<String> warning = Collections.singletonList("warning");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentSourceId =
        new ProcessingDetailsImpl(differentSourceId, exception, warning);
    assertThat(processingDetails, is(not(processingDetailsWithDifferentSourceId)));
  }

  @Test
  public void testInequalityOfExceptions() {
    String sourceId = "test source";
    List<String> warning = Collections.singletonList("warning");
    Exception exception = new UnsupportedQueryException("We do not support this query");
    Exception differentException =
        new UnsupportedQueryException("We do not support this query either");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentException =
        new ProcessingDetailsImpl(sourceId, differentException, warning);
    assertThat(processingDetails, is(not(processingDetailsWithDifferentException)));
  }

  @Test
  public void testInequalityOfWarnings() {
    String sourceId = "test source";
    Exception exception = new UnsupportedQueryException("We do not support this query");
    List<String> warning = Collections.singletonList("warning");
    List<String> differentWarning = Collections.singletonList("different warning");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentWarnings =
        new ProcessingDetailsImpl(sourceId, exception, differentWarning);
    assertThat(processingDetails, is(not(processingDetailsWithDifferentWarnings)));
  }

  @Test
  public void testInequalityWithNull() {
    ProcessingDetails processingDetails = new ProcessingDetailsImpl();
    assertThat(processingDetails.equals(null), is(false));
  }

  @Test
  public void testEqualityWithNullSourceIds() {
    Exception exception = new UnsupportedQueryException("We do not support this query");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(null, exception, "warning");
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl(null, exception, "warning");
    assertThat(processingDetails, is(identicalProcessingDetails));
  }

  @Test
  public void testEqualityWithNullExceptions() {
    ProcessingDetails processingDetails = new ProcessingDetailsImpl("test source", null, "warning");
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl("test source", null, "warning");
    assertThat(processingDetails, is(identicalProcessingDetails));
  }

  @Test
  public void testEqualityOfHashCodes() {
    Exception exception = new UnsupportedQueryException("We do not support this query");
    ProcessingDetails processingDetails =
        new ProcessingDetailsImpl("test source", exception, "warning");
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl("test source", exception, "warning");
    assertThat(
        "\nThe hashCodes of ProcessingDetails with equal sourceIds, equal\n"
            + "exceptions, and equal warnings should have been equal, but were not.\n",
        processingDetails.hashCode(),
        is(identicalProcessingDetails.hashCode()));
  }

  @Test
  public void testInequalityOfHashCodesForUnequalSourceIds() {
    String sourceId = "test source";
    String differentSourceId = "different test source";
    Exception exception = new UnsupportedQueryException("We do not support this query");
    List<String> warning = Collections.singletonList("warning");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentSourceId =
        new ProcessingDetailsImpl(differentSourceId, exception, warning);
    assertThat(
        "\nThe hashCodes of ProcessingDetails with unequal sourceIds should\n"
            + "not have been equal, but were.\n",
        processingDetails.hashCode(),
        not(processingDetailsWithDifferentSourceId.hashCode()));
  }

  @Test
  public void testInequalityOfHashCodesForUnequalExceptions() {
    String sourceId = "test source";
    List<String> warning = Collections.singletonList("warning");
    Exception exception = new UnsupportedQueryException("We do not support this query");
    Exception differentException =
        new UnsupportedQueryException("We do not support this query either");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentException =
        new ProcessingDetailsImpl(sourceId, differentException, warning);
    assertThat(
        "\nThe hashCodes of ProcessingDetails with unequal exceptions should\n"
            + "not have been equal, but were.\n",
        processingDetails.hashCode(),
        not(processingDetailsWithDifferentException.hashCode()));
  }

  @Test
  public void testInequalityOfHashCodesForUnequalWarnings() {
    String sourceId = "test source";
    Exception exception = new UnsupportedQueryException("We do not support this query");
    List<String> warning = Collections.singletonList("warning");
    List<String> differentWarning = Collections.singletonList("different warning");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, exception, warning);
    ProcessingDetails processingDetailsWithDifferentWarnings =
        new ProcessingDetailsImpl(sourceId, exception, differentWarning);
    assertThat(
        "\nThe hashCodes of ProcessingDetails with unequal warnings should not\n"
            + "have been equal, but were.\n",
        processingDetails.hashCode(),
        not(processingDetailsWithDifferentWarnings.hashCode()));
  }

  @Test
  public void testEqualityOfHashCodesForNullSourceIds() {
    List<String> warnings = Collections.singletonList("warning");
    Exception exception = new UnsupportedQueryException("We do not support this query");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(null, exception, warnings);
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl(null, exception, warnings);
    assertThat(
        "\nThe hashCodes of ProcessingDetails with null sourceIds, equal\n"
            + "exceptions, and equal warnings should have been equal, but were not.\n",
        processingDetails.hashCode(),
        is(identicalProcessingDetails.hashCode()));
  }

  @Test
  public void testEqualityOfHashCodesForNullExceptions() {
    String sourceId = "test source";
    List<String> warnings = Collections.singletonList("warning");
    ProcessingDetails processingDetails = new ProcessingDetailsImpl(sourceId, null, warnings);
    ProcessingDetails identicalProcessingDetails =
        new ProcessingDetailsImpl(sourceId, null, warnings);
    assertThat(
        "\nThe hashCodes of ProcessingDetails with equal sourceIds, null\n"
            + "exceptions, and equal warnings should have been equal, but were not.\n",
        processingDetails.hashCode(),
        is(identicalProcessingDetails.hashCode()));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorThrowsNullPointerExceptionWhenGivenNullWarning() {
    new ProcessingDetailsImpl("test source", new UnsupportedQueryException(), (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsIllegalArgumentExceptionWhenGivenNullWarnings() {
    new ProcessingDetailsImpl("test source", new UnsupportedQueryException(), (List<String>) null);
  }
}
