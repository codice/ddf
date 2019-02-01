package org.codice.ddf.catalog.ui.ws;

/** Exception to */
public class SecureWebSocketException extends RuntimeException {
  private final String wsMessage;

  SecureWebSocketException(String message) {
    this(message, null);
  }

  SecureWebSocketException(String message, String wsMessage) {
    super(message);
    this.wsMessage = wsMessage;
  }

  public String getWsMessage() {
    return wsMessage;
  }
}
