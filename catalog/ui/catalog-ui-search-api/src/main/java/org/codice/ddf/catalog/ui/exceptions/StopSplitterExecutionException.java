package org.codice.ddf.catalog.ui.exceptions;

/**
 * This exception should be used to terminate execution when parsing files of certain mimetypes.
 *
 * @param str the {@link String} exception message
 */
public class StopSplitterExecutionException extends Exception {
  public StopSplitterExecutionException(String str) {
    super(str);
  }
}
