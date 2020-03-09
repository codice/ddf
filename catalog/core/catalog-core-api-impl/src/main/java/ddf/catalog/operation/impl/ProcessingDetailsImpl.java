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
package ddf.catalog.operation.impl;

import static org.apache.commons.lang3.Validate.notNull;

import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.SourceProcessingDetails;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProcessingDetailsImpl extends SourceProcessingDetailsImpl
    implements ProcessingDetails {

  protected String sourceId = null;

  protected Exception exception = null;

  public ProcessingDetailsImpl() {
    super();
  }

  /**
   * Instantiates a new ProcessingDetailsImpl with a {@link String} sourceID and an $ {@link
   * Exception}
   *
   * @param sourceId the sourceId
   * @param exception the exception
   */
  public ProcessingDetailsImpl(String sourceId, Exception exception) {
    super();
    this.sourceId = sourceId;
    this.exception = exception;
  }

  /**
   * Instantiates a new ProcessingDetailsImpl with a sourceID, an ${@link Exception}, and a warning
   *
   * @param sourceId the sourceId
   * @param exception the exception
   * @param warning the warning
   */
  public ProcessingDetailsImpl(String sourceId, Exception exception, String warning) {
    this(
        sourceId,
        exception,
        Collections.singletonList(
            notNull(
                warning,
                "the constructor of ProcessingDetailsImpl does not accept a null warning")));
  }

  /**
   * Instantiates a new ProcessingDetailsImpl with a sourceID, an ${@link Exception} and a $ {@link
   * List} of warnings
   *
   * @param sourceId the the sourceId
   * @param exception the exception
   * @param warnings the warnings
   */
  public ProcessingDetailsImpl(String sourceId, Exception exception, List<String> warnings) {
    super(warnings);
    this.sourceId = sourceId;
    this.exception = exception;
  }

  /**
   * Instantiates a new ProcessingDetailsImpl with ${@link SourceProcessingDetails} and a warning
   *
   * @param details the source processing details
   * @param warning the warning
   */
  public ProcessingDetailsImpl(SourceProcessingDetails details, String sourceId) {
    super(details.getWarnings());
    this.sourceId = sourceId;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    ProcessingDetailsImpl that = (ProcessingDetailsImpl) o;
    return Objects.equals(this.sourceId, that.sourceId)
        && Objects.equals(this.exception, that.exception);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sourceId, exception);
  }

  @Override
  public boolean hasException() {
    return exception != null;
  }

  @Override
  public Exception getException() {
    return exception;
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }

  @Override
  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }
}
