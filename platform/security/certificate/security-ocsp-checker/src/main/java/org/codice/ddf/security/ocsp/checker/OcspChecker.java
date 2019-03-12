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
package org.codice.ddf.security.ocsp.checker;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.common.audit.SecurityLogger;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.security.OcspService;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcspChecker implements OcspService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OcspChecker.class);
  private static final String HTTPS = "https://";

  private final ClientFactoryFactory factory;
  private final EventAdmin eventAdmin;

  private boolean ocspEnabled;
  private String ocspServerUrl;

  public OcspChecker(ClientFactoryFactory factory, EventAdmin eventAdmin) {
    this.factory = factory;
    this.eventAdmin = eventAdmin;
  }

  /**
   * Checks the whether the given {@param certs} are revoked or not against the OCSP server.
   *
   * @param certs - an array of certificates to verify
   * @return true if the certificates are not revoked or if they could not be properly checked
   *     against the OCSP server. Returns false if any of them are revoked.
   */
  public boolean passesOcspCheck(X509Certificate[] certs) {
    if (!ocspEnabled || ocspServerUrl == null) {
      LOGGER.debug("OCSP check is not enabled. Skipping.");
      return true;
    }

    if (!ocspServerUrl.startsWith(HTTPS)) {
      postErrorEvent(String.format("The provided URL {%s} is not an HTTPS URL.", ocspServerUrl));
      return true;
    }

    for (X509Certificate cert : certs) {
      try {
        Certificate certificate = convertToBouncyCastleCert(cert);
        OCSPReq ocspRequest = generateOcspRequest(certificate);
        Response response = sendOcspRequest(ocspRequest);
        OCSPResp ocspResponse = createOcspResponse(response);
        boolean certIsValid = isOCSPResponseValid(ocspResponse);
        if (!certIsValid) {
          SecurityLogger.audit(
              "Certificate {} has been revoked by the OCSP server {}.", cert, ocspServerUrl);
          LOGGER.debug("Certificate has been revoked by the OCSP server.");
          return false;
        }
      } catch (OcspCheckerException e) {
        // ignore
      }
    }
    // If an error occurred, the certificates will not be validated and will be permitted.
    // An alert will be posted to the admin console.
    return true;
  }

  /**
   * Converts a {@link java.security.cert.X509Certificate} to a {@link
   * org.bouncycastle.asn1.x509.Certificate}
   *
   * @param cert - the X509Certificate to convert
   * @return a Bouncy Castle certificate
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs
   */
  @VisibleForTesting
  Certificate convertToBouncyCastleCert(X509Certificate cert) throws OcspCheckerException {
    try {
      byte[] data = cert.getEncoded();
      return Certificate.getInstance(data);
    } catch (CertificateEncodingException e) {
      postErrorEvent(
          "Unable to convert certificate to a Bouncy Castle certificate. The certificate status could not be verified. "
              + e.getMessage());
      throw new OcspCheckerException();
    }
  }

  /**
   * Creates an {@link OCSPReq} to send to the OCSP server for the given certificate.
   *
   * @param cert - certificate to verify
   * @return the created OCSP request
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs
   */
  @VisibleForTesting
  OCSPReq generateOcspRequest(Certificate cert) throws OcspCheckerException {
    try {
      JcaDigestCalculatorProviderBuilder digestCalculatorProviderBuilder =
          new JcaDigestCalculatorProviderBuilder();
      DigestCalculatorProvider digestCalculatorProvider = digestCalculatorProviderBuilder.build();
      DigestCalculator digestCalculator = digestCalculatorProvider.get(CertificateID.HASH_SHA1);

      CertificateID certId =
          new CertificateID(
              digestCalculator, new X509CertificateHolder(cert), cert.getSerialNumber().getValue());

      OCSPReqBuilder ocspReqGenerator = new OCSPReqBuilder();
      ocspReqGenerator.addRequest(certId);
      return ocspReqGenerator.build();

    } catch (OCSPException | OperatorCreationException e) {
      postErrorEvent(
          "Unable to create a OCSP request. The certificate status could not be verified. "
              + e.getMessage());
      throw new OcspCheckerException();
    }
  }

  /**
   * Sends the {@param ocspReq} request and returns the returned Response
   *
   * @param ocspReq - the OCSP request to send
   * @return the response from the OCSP server
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs
   */
  @VisibleForTesting
  Response sendOcspRequest(OCSPReq ocspReq) throws OcspCheckerException {
    try {
      SecureCxfClientFactory cxfClientFactory =
          factory.getSecureCxfClientFactory(ocspServerUrl, WebClient.class);
      WebClient client =
          cxfClientFactory
              .getWebClient()
              .accept("application/ocsp-response")
              .type("application/ocsp-request");

      return client.post(ocspReq.getEncoded());

    } catch (ProcessingException | IOException e) {
      postErrorEvent(
          "Unable to send OCSP request. The certificate status could not be verified. "
              + e.getMessage());
      throw new OcspCheckerException();
    }
  }

  /**
   * Creates a {@link OCSPResp} from the given {@link Response}
   *
   * @param response - the response to convert to
   * @return an OCSP response of the given response
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs
   */
  @VisibleForTesting
  OCSPResp createOcspResponse(Response response) throws OcspCheckerException {
    Object entity = response.getEntity();

    if (!(entity instanceof InputStream)) {
      postErrorEvent(
          "Unable to send OCSP request. The certificate status could not be verified. Unable to get a response from the OCSP server.");
      throw new OcspCheckerException();
    }

    try (InputStream inputStream = (InputStream) entity) {
      return new OCSPResp(inputStream);
    } catch (IOException e) {
      postErrorEvent(
          "Unable to send OCSP request. The certificate status could not be verified. "
              + e.getMessage());
      throw new OcspCheckerException();
    }
  }

  /**
   * Verifies the status of the {@param ocspResponse}
   *
   * @param ocspResponse - response with status to verify
   * @return true if the status is not revoked or unknown, false otherwise
   */
  @VisibleForTesting
  boolean isOCSPResponseValid(OCSPResp ocspResponse) {
    try {
      BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();

      if (basicResponse == null) {
        return false;
      }

      SingleResp[] singleResps = basicResponse.getResponses();
      if (singleResps == null) {
        return false;
      }

      SingleResp response = Arrays.stream(singleResps).findFirst().orElse(null);
      if (response == null) {
        return false;
      }

      Object status = response.getCertStatus();
      if (status instanceof UnknownStatus) {
        return false;
      }

      return !(status instanceof RevokedStatus);

    } catch (OCSPException e) {
      postErrorEvent(
          "Unable to read a OCSP response. The certificate status could not be verified. "
              + e.getMessage());
      return true;
    }
  }

  /**
   * Posts and error message to the Admin Console.
   *
   * @param errorMessage - The reason for the error.
   */
  private void postErrorEvent(String errorMessage) {
    String title = "Failure checking the revocation status of a Certificate through OCSP.";
    Set<String> details = new HashSet<>();
    details.add(
        "An error occurred while checking the revocation status of a Certificate using an Online Certificate Status Protocol (OCSP) server. "
            + "Please resolve the error to resume validating certificates against the OCSP server.");
    details.add(errorMessage);
    eventAdmin.postEvent(
        new Event(
            SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + "crl",
            new SystemNotice(this.getClass().getName(), NoticePriority.CRITICAL, title, details)
                .getProperties()));
    SecurityLogger.audit(title);
    SecurityLogger.audit(errorMessage);
    LOGGER.debug(errorMessage);
  }

  public void setOcspEnabled(boolean ocspEnabled) {
    this.ocspEnabled = ocspEnabled;
  }

  public void setOcspServerUrl(String ocspServerUrl) {
    this.ocspServerUrl = ocspServerUrl;
  }

  /**
   * Custom exception usually thrown after an unexpected error occurred while validating a
   * certificate. An alert should be posted to the admin console first.
   */
  class OcspCheckerException extends Exception {
    public OcspCheckerException() {
      super();
    }
  }
}
