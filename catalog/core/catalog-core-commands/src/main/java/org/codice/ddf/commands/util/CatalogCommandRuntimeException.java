package org.codice.ddf.commands.util;

public class CatalogCommandRuntimeException extends RuntimeException {
        public CatalogCommandRuntimeException() {
            super();
        }

        public CatalogCommandRuntimeException(String message) {
            super(message);
        }

        public CatalogCommandRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }

        public CatalogCommandRuntimeException(Throwable cause) {
            super(cause);
        }
    }
