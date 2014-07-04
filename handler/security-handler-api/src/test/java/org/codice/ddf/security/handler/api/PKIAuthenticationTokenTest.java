/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.handler.api;

import junit.framework.TestCase;

public class PKIAuthenticationTokenTest extends TestCase {

    protected static final String encodedCert =
      "MIIDejCCAmKgAwIBAgIBBTANBgkqhkiG9w0BAQUFADBgMQswCQYDVQQGEwJVUzEY\n"
        + "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsT\n"
        + "A1BLSTEbMBkGA1UEAxMSRG9EIEpJVEMgUm9vdCBDQSAyMB4XDTA1MDcxNTAzMzEz\n"
        + "MVoXDTMwMDcwNDAzMzEzMVowYDELMAkGA1UEBhMCVVMxGDAWBgNVBAoTD1UuUy4g\n"
        + "R292ZXJubWVudDEMMAoGA1UECxMDRG9EMQwwCgYDVQQLEwNQS0kxGzAZBgNVBAMT\n"
        + "EkRvRCBKSVRDIFJvb3QgQ0EgMjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n"
        + "ggEBALRIymMpeEOhlGWsnArSdyuDMN8vs5LUze6XjepHVgXqBPQx2AWb9x2BOIfj\n"
        + "SqCIyl8GOBynJLwayGYpURJGScnfLZMSXrt6SHPof2gnl+hqA71Ssbw+jtM8tADl\n"
        + "fgaT21ko1iYm88namlb3FRbTz4G2cBsHIaD0DhkD2DVtPUJhW4abViTQYPf4/n49\n"
        + "64BdC26O66WtKsftWsgVeQd9D1efCpKfMs/mptwgTEJqIvKuvhV+/rAzGfTkDUm4\n"
        + "148U1/HuEYJvI++h0pZpS+wzQEkB5QJm8rrp7beHBiLD6YZ0OnATgnlSoAP46OLu\n"
        + "LfHlX8dn3N0L+xfIMc3wOoatTDsCAwEAAaM/MD0wHQYDVR0OBBYEFPngP4dW/9Ih\n"
        + "gLo9E37FT1Sw37wCMAsGA1UdDwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MA0GCSqG\n"
        + "SIb3DQEBBQUAA4IBAQA/sLzl//ueBYzj3C6wRb74sUww3Yya9k5Ny3f17i5YsbOz\n"
        + "ABv/UcandBbFmfYnrAXw7ZzCpmgXgcpZRntxIHVeJU/WzFhEBuMqXAdc19g+bkzh\n"
        + "/OE5fEODBMpVzLUsvRQLFBp12GP60AULXkje8k1h0NXx4ENtKFbOYLrb0zwJVMqT\n"
        + "RGgnnYM9dzfqxA03dXnvCVZaZ5VlYwMvqyfxTt8pSI+i4vMzerNojZqug0PX1tA7\n"
        + "hjKk7gPS7bD62fwkoVZ6wT+XxtZ0IFcqSxPVm0Kg20TbtfGoHsEKcf2ao1PxEvKx\n"
        + "mO9cW65lIW96mu2pKjJNb+FmW6RAjaDJXFHkN8uP";

    public void testGetDn() throws Exception {

    }

    public void testGetCertificate() throws Exception {

    }

    public void testGetEncodedCredentials() throws Exception {

    }
}
