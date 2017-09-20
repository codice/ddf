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
package org.codice.ddf.security.certificate.generator;

public class DemoCertificateAuthority extends CertificateAuthority {
  static String pemDemoCaPrivateKey =
      "MIICXAIBAAKBgQC1bRSSFWIG/tO/4wWdSl2VQsbn9IVKDjDsNr2CfFaoUu+eUn++KkXFWj5vAXK1lRvNDnbrV7cz/spZzv7Y5q/pssbCp6aHV2XC6gs0BOfK4ee2xSUQCUo6iY58viQUstDHSNzbSUt8lZq1IGUVR3SKrmATzI8SMYEbE9t9BdTJ3wIDAQABAoGBAKWZh2lAM1YHS+iepAVKV9liNoSK4Q8TnPw/iwOL0t2ZtjZhP1Co7T6SfOJ+A+JS1Cl7xnhSfFrtem43Ts9U4cN5H2oUyKiXVOKaWfX7VEdDNXd4BFFYdZ3es3yURk0f5J8ryiJy/CtFCqUrnh7jFj0iy3vZ1ODdRx2H10bouCxhAkEA4aS5jjdzLXSY1i7pV8+fbQj1zro/GPE896Ur/M1cZFiHXIDY2yEBsR0YI8Vc0EHArMFwgOcl825BweE5Y4llhQJBAM3VfLaveItAbfzv4/IE3dzb5pTllK4a9kJbrxxcLLMdwC/VQ5OkqKkz6SKcoOxVCpIyIe3UNnT9AuvmPdBYjRMCQH/DOZ2hIAI45uE/prglw2uFi0kGg/unfJHsYD/AN/RJfDuQaTmKMt8KTkTS137D/EjVLtKODxsK7wjMciY+AdECQFpjFGf4uBuWSHZZNaTypCa3XhMmmdq2tks/ja8LfwmM1/dpVEqCdRoQVKFBXJ1LXuACKcoRdXR8IotKyPwz1ocCQG5sTZflDN2jvNotOHtEAoRSQ4JqChzcUfZrmDR0P0hI9FC8kxE3KsEI3Wl1ce1iUVf+SnFsBHb94dgj/CSM8JA=";

  static String pemDemoCaCertificate =
      "MIIDdzCCAuCgAwIBAgIJAL0PQLifiHRgMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTEzNDM1MloYDzIxMTUxMTE3MTM0MzUyWjCBhDELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkFaMQwwCgYDVQQKEwNEREYxDDAKBgNVBAsTA0RldjEZMBcGA1UEAxMQRERGIERlbW8gUm9vdCBDQTExMC8GCSqGSIb3DQEJARYiZW1haWxBZGRyZXNzPWRkZnJvb3RjYUBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAtW0UkhViBv7Tv+MFnUpdlULG5/SFSg4w7Da9gnxWqFLvnlJ/vipFxVo+bwFytZUbzQ5261e3M/7KWc7+2Oav6bLGwqemh1dlwuoLNATnyuHntsUlEAlKOomOfL4kFLLQx0jc20lLfJWatSBlFUd0iq5gE8yPEjGBGxPbfQXUyd8CAwEAAaOB7DCB6TAdBgNVHQ4EFgQU4VTHl98Kwr+pX3heOwsr5EgXvcYwgbkGA1UdIwSBsTCBroAU4VTHl98Kwr+pX3heOwsr5EgXvcahgYqkgYcwgYQxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJBWjEMMAoGA1UEChMDRERGMQwwCgYDVQQLEwNEZXYxGTAXBgNVBAMTEERERiBEZW1vIFJvb3QgQ0ExMTAvBgkqhkiG9w0BCQEWImVtYWlsQWRkcmVzcz1kZGZyb290Y2FAZXhhbXBsZS5vcmeCCQC9D0C4n4h0YDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4GBAGrhV8RwRxMiPppXNA0/4zc7g0tKtjggObLMMQhJeIAvIrbjNx0VoX3/fLXDC6KwTCnJaeQ7IKT4fpw31i/2s2gCjecLgDRVR7YFWs37dEJxqWeRyNM5/BYCodwkoNFDqDG89FmsM3xsPGUNuzXMUnXTncGcqEiJo0RayTHZYzvN";

  /** Create new instance of the DDF Demo Certificate Authority */
  public DemoCertificateAuthority() {
    initialize(
        PkiTools.pemToCertificate(DemoCertificateAuthority.pemDemoCaCertificate),
        PkiTools.pemToPrivateKey(DemoCertificateAuthority.pemDemoCaPrivateKey));
  }
}
