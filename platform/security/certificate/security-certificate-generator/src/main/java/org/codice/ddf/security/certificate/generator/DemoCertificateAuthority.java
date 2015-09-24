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
 */
package org.codice.ddf.security.certificate.generator;

public class DemoCertificateAuthority extends CertificateAuthority {
    static String pemDemoCaPrivateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALVtFJIVYgb+07/jBZ1KXZVCxuf0hUoOMOw2vYJ8VqhS755Sf74qRcVaPm8BcrWVG80OdutXtzP+ylnO/tjmr+myxsKnpodXZcLqCzQE58rh57bFJRAJSjqJjny+JBSy0MdI3NtJS3yVmrUgZRVHdIquYBPMjxIxgRsT230F1MnfAgMBAAECgYEApZmHaUAzVgdL6J6kBUpX2WI2hIrhDxOc/D+LA4vS3Zm2NmE/UKjtPpJ84n4D4lLUKXvGeFJ8Wu16bjdOz1Thw3kfahTIqJdU4ppZ9ftUR0M1d3gEUVh1nd6zfJRGTR/knyvKInL8K0UKpSueHuMWPSLLe9nU4N1HHYfXRui4LGECQQDhpLmON3MtdJjWLulXz59tCPXOuj8Y8Tz3pSv8zVxkWIdcgNjbIQGxHRgjxVzQQcCswXCA5yXzbkHB4TljiWWFAkEAzdV8tq94i0Bt/O/j8gTd3NvmlOWUrhr2QluvHFwssx3AL9VDk6SoqTPpIpyg7FUKkjIh7dQ2dP0C6+Y90FiNEwJAf8M5naEgAjjm4T+muCXDa4WLSQaD+6d8kexgP8A39El8O5BpOYoy3wpORNLXfsP8SNUu0o4PGwrvCMxyJj4B0QJAWmMUZ/i4G5ZIdlk1pPKkJrdeEyaZ2ra2Sz+Nrwt/CYzX92lUSoJ1GhBUoUFcnUte4AIpyhF1dHwii0rI/DPWhwJAbmxNl+UM3aO82i04e0QChFJDgmoKHNxR9muYNHQ/SEj0ULyTETcqwQjdaXVx7WJRV/5KcWwEdv3h2CP8JIzwkA==";

    static String pemDemoCaCertificate = "MIICvDCCAiWgAwIBAgIJAIzc4FYrIp9lMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYxGTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJvb3RjYUBleGFtcGxlLm9yZzAeFw0xNDEyMTAyMTU2MzBaFw0xNzEyMDkyMTU2MzBaMHcxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYxGTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJvb3RjYUBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAtW0UkhViBv7Tv+MFnUpdlULG5/SFSg4w7Da9gnxWqFLvnlJ/vipFxVo+bwFytZUbzQ5261e3M/7KWc7+2Oav6bLGwqemh1dlwuoLNATnyuHntsUlEAlKOomOfL4kFLLQx0jc20lLfJWatSBlFUd0iq5gE8yPEjGBGxPbfQXUyd8CAwEAAaNQME4wHQYDVR0OBBYEFOFUx5ffCsK/qV94XjsLK+RIF73GMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEAnxCXM5gyM2KoDpgAgn4sZtNJdtzWecCA/q6ee9q1KZ7Dyj+cYjNyuZEJ4aB3kGzH/EedwW1J4tGJ2CnoC9cD3r/SKTtbk9bEXNvDLu9YiuQfH+2daRrpv3n8WUEj4bHJwmIZlkLA1YYUFZdauf9pUJWJxlDur6c8C4KRX5MohqU=";

    /**
     * Create new instance of the DDF Demo Certificate Authority
     */

    public DemoCertificateAuthority() {
        initialize(PkiTools.pemToCertificate(DemoCertificateAuthority.pemDemoCaCertificate),
                PkiTools.pemToPrivateKey(DemoCertificateAuthority.pemDemoCaPrivateKey));
    }
}
