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
package ddf.catalog.source;

/** Ingest exception that should spawn a 5xx error back to the client. */
public class InternalIngestException extends IngestException {
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** Instantiates a new exception. */
  public InternalIngestException() {
    super();
  }

  /**
   * Instantiates a new exception with the provided message.
   *
   * @param message the message
   */
  public InternalIngestException(String message) {
    super(message);
  }

  /**
   * Instantiates a new exception with the provided message and {@link Throwable}.
   *
   * @param message the message
   * @param throwable the throwable
   */
  public InternalIngestException(String message, Throwable throwable) {
    super(message, throwable);
  }

  /**
   * Instantiates a new exception with the provided {@link Throwable}.
   *
   * @param throwable the throwable
   */
  public InternalIngestException(Throwable throwable) {
    super(throwable);
  }
}
