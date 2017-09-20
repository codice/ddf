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
package ddf.camel.component.catalog.content;

/** The ContentComponentException is used to signal errors during Content Component operations. */
public class ContentComponentException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new ContentComponentException from a given string.
   *
   * @param message the string to use for the exception.
   */
  public ContentComponentException(String message) {
    super(message);
  }

  /**
   * Instantiates a new ContentComponentException.
   *
   * @param throwable the throwable
   */
  public ContentComponentException(Throwable throwable) {
    super(throwable);
  }

  /**
   * Instantiates a new ContentComponentException with a message.
   *
   * @param message the message
   * @param throwable the throwable
   */
  public ContentComponentException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
