package org.codice.ddf.security.idp.server;

public class IdpException extends Exception {
    public IdpException(String message) {
        super(message);
    }

    public IdpException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdpException(Throwable cause) {
        super(cause);
    }
}
