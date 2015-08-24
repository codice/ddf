/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package ddf.security.certificate.generator;

import java.security.cert.CertificateException;

public class CertificateGeneratorException extends CertificateException {

    public CertificateGeneratorException(String msg) {
        super(msg);
    }

    public CertificateGeneratorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public static class InvalidIssuer extends CertificateGeneratorException {
        public InvalidIssuer(String msg) {
            super(msg);
        }

        public InvalidIssuer(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public static class InvalidDate extends CertificateGeneratorException {
        public InvalidDate(String msg) {
            super(msg);
        }
    }

    public static class InvalidSubject extends CertificateGeneratorException {
        public InvalidSubject(String msg) {
            super(msg);
        }
    }

    public static class InvalidKey extends CertificateGeneratorException {
        public InvalidKey(String msg) {
            super(msg);
        }

        public InvalidKey(String msg, Throwable cause) {
            super(msg, cause);
        }

    }

    public static class CannotSignCertificate extends CertificateGeneratorException {
        public CannotSignCertificate(String msg) {
            super(msg);
        }

        public CannotSignCertificate(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public static class CannotGenerateKeyPair extends CertificateGeneratorException {
        public CannotGenerateKeyPair(String msg) {
            super(msg);
        }

        public CannotGenerateKeyPair(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public static class InvalidCertificateAuthority extends CertificateGeneratorException {
        public InvalidCertificateAuthority(String msg) {
            super(msg);
        }

        public InvalidCertificateAuthority(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
