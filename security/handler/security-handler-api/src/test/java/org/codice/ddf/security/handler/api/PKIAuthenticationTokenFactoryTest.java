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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKIAuthenticationTokenFactoryTest extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UPAuthenticationToken.class);

    protected static final String encodedCert =
      "MIIDejCCAmKgAwIBAgIBBTANBgkqhkiG9w0BAQUFADBgMQswCQYDVQQGEwJVUzEY"
        + "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsT"
        + "A1BLSTEbMBkGA1UEAxMSRG9EIEpJVEMgUm9vdCBDQSAyMB4XDTA1MDcxNTAzMzEz"
        + "MVoXDTMwMDcwNDAzMzEzMVowYDELMAkGA1UEBhMCVVMxGDAWBgNVBAoTD1UuUy4g"
        + "R292ZXJubWVudDEMMAoGA1UECxMDRG9EMQwwCgYDVQQLEwNQS0kxGzAZBgNVBAMT"
        + "EkRvRCBKSVRDIFJvb3QgQ0EgMjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC"
        + "ggEBALRIymMpeEOhlGWsnArSdyuDMN8vs5LUze6XjepHVgXqBPQx2AWb9x2BOIfj"
        + "SqCIyl8GOBynJLwayGYpURJGScnfLZMSXrt6SHPof2gnl+hqA71Ssbw+jtM8tADl"
        + "fgaT21ko1iYm88namlb3FRbTz4G2cBsHIaD0DhkD2DVtPUJhW4abViTQYPf4/n49"
        + "64BdC26O66WtKsftWsgVeQd9D1efCpKfMs/mptwgTEJqIvKuvhV+/rAzGfTkDUm4"
        + "148U1/HuEYJvI++h0pZpS+wzQEkB5QJm8rrp7beHBiLD6YZ0OnATgnlSoAP46OLu"
        + "LfHlX8dn3N0L+xfIMc3wOoatTDsCAwEAAaM/MD0wHQYDVR0OBBYEFPngP4dW/9Ih"
        + "gLo9E37FT1Sw37wCMAsGA1UdDwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MA0GCSqG"
        + "SIb3DQEBBQUAA4IBAQA/sLzl//ueBYzj3C6wRb74sUww3Yya9k5Ny3f17i5YsbOz"
        + "ABv/UcandBbFmfYnrAXw7ZzCpmgXgcpZRntxIHVeJU/WzFhEBuMqXAdc19g+bkzh"
        + "/OE5fEODBMpVzLUsvRQLFBp12GP60AULXkje8k1h0NXx4ENtKFbOYLrb0zwJVMqT"
        + "RGgnnYM9dzfqxA03dXnvCVZaZ5VlYwMvqyfxTt8pSI+i4vMzerNojZqug0PX1tA7"
        + "hjKk7gPS7bD62fwkoVZ6wT+XxtZ0IFcqSxPVm0Kg20TbtfGoHsEKcf2ao1PxEvKx"
        + "mO9cW65lIW96mu2pKjJNb+FmW6RAjaDJXFHkN8uP";

    public void testGetTokenFromString() throws Exception {
  /*      PKIAuthenticationTokenFactory factory = new PKIAuthenticationTokenFactory();
        factory.setRealm("TestRealm");
        factory.setSignaturePropertiesPath("signature.properties");
        factory.init();

        PKIAuthenticationToken token = factory.getTokenFromString(encodedCert, true);
        LOGGER.info("Token: " + token.toString());
  */
    }

    public void testGetTokenFromBytes() throws Exception {

    }

    public void testGetTokenFromCerts() throws Exception {

    }
}
