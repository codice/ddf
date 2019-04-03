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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.SecurityConstants;
import ddf.security.common.audit.SecurityLogger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
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
import org.bouncycastle.x509.extension.X509ExtensionUtil;
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

  private final ClientFactoryFactory factory;
  private final EventAdmin eventAdmin;

  private boolean ocspEnabled;
  private List<String> ocspServerUrls;

  public OcspChecker(ClientFactoryFactory factory, EventAdmin eventAdmin) {
    this.factory = factory;
    this.eventAdmin = eventAdmin;
  }

  /**
   * Checks whether the given {@param certs} are revoked or not against the configured OCSP server
   * urls + the optionally given OCSP server url in the given {@param certs}.
   *
   * @param certs - an array of certificates to verify.
   * @return true if the certificates are good or if they could not be properly checked against the
   *     OCSP server. Returns false if any of them are revoked.
   */
  @Override
  public boolean passesOcspCheck(X509Certificate[] certs) {
    if (!ocspEnabled) {
      LOGGER.debug("OCSP check is not enabled. Skipping.");
      return true;
    }

    for (X509Certificate cert : certs) {
      try {
        Certificate certificate = convertToBouncyCastleCert(cert);
        OCSPReq ocspRequest = generateOcspRequest(certificate);
        List<OCSPResp> ocspResponses = sendOcspRequests(cert, ocspRequest);
        if (anyOcspResponseStatusIsRevoked(ocspResponses)) {
          SecurityLogger.audit(
              "Certificate {} has been revoked by the OCSP server {}.", cert, ocspServerUrls);
          LOGGER.debug("Certificate has been revoked by the OCSP server.");
          return false;
        }
      } catch (OcspCheckerException ignore) {
      }
    }
    // If an error occurred, the certificates will not be validated and will be permitted.
    // An alert will be posted to the admin console.
    return true;
  }

  /**
   * Converts a {@link java.security.cert.X509Certificate} to a {@link Certificate}.
   *
   * @param cert - the X509Certificate to convert.
   * @return a {@link Certificate}.
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs.
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
   * @param cert - the certificate to verify
   * @return the created OCSP request
   * @throws OcspCheckerException after posting an alert to the admin console, if any error occurs
   */
  @VisibleForTesting
  OCSPReq generateOcspRequest(Certificate cert) throws OcspCheckerException {
    try {
      X509CertificateHolder issuerCert = resolveIssuerCertificate(cert);

      JcaDigestCalculatorProviderBuilder digestCalculatorProviderBuilder =
          new JcaDigestCalculatorProviderBuilder();
      DigestCalculatorProvider digestCalculatorProvider = digestCalculatorProviderBuilder.build();
      DigestCalculator digestCalculator = digestCalculatorProvider.get(CertificateID.HASH_SHA1);

      CertificateID certId =
          new CertificateID(digestCalculator, issuerCert, cert.getSerialNumber().getValue());

      OCSPReqBuilder ocspReqGenerator = new OCSPReqBuilder();
      ocspReqGenerator.addRequest(certId);
      return ocspReqGenerator.build();

    } catch (OCSPException | OperatorCreationException e) {
      postErrorEvent(
          "Unable to create an OCSP request. The certificate status could not be verified. "
              + e.getMessage());
      throw new OcspCheckerException();
    }
  }

  /**
   * Returns an {@link X509CertificateHolder} containing the issuer of the passed in {@param cert}.
   * Search is performed in the system truststore.
   *
   * @param cert - the {@link Certificate} to get the issuer from.
   * @return {@link X509CertificateHolder} containing the issuer of the passed in {@param cert}.
   * @throws OcspCheckerException if the issuer cannot be resolved.
   */
  private X509CertificateHolder resolveIssuerCertificate(Certificate cert)
      throws OcspCheckerException {
    X500Name issuerName = cert.getIssuer();

    String trustStorePath =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) SecurityConstants::getTruststorePath);
    String trustStorePass =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) SecurityConstants::getTruststorePassword);

    if (isBlank(trustStorePath) || isBlank(trustStorePass)) {
      throw new OcspCheckerException("Problem retrieving truststore properties.");
    }

    KeyStore truststore;

    try (InputStream truststoreInputStream = new FileInputStream(trustStorePath)) {
      truststore = SecurityConstants.newTruststore();
      truststore.load(truststoreInputStream, trustStorePass.toCharArray());
    } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new OcspCheckerException(
          String.format("Problem loading truststore on path %s", trustStorePath));
    }

    try {
      return getCertFromTruststoreWithX500Name(issuerName, truststore);
    } catch (OcspCheckerException e) {
      throw new OcspCheckerException("Problem finding issuer in truststore.", e);
    }
  }

  /**
   * Returns an {@link X509CertificateHolder} containing the issuer of the given {@param name}.
   * Search is performed in the given {@param truststore}.
   *
   * @param name - the {@link X500Name} of the issuer.
   * @param truststore - the {@link KeyStore} to check.
   * @return {@link X509CertificateHolder} of the certificate with the given {@param name}.
   * @throws OcspCheckerException if the {@param name} cannot be found in the {@param truststore}.
   */
  private X509CertificateHolder getCertFromTruststoreWithX500Name(
      X500Name name, KeyStore truststore) throws OcspCheckerException {
    Enumeration<String> aliases;

    try {
      aliases = truststore.aliases();
    } catch (KeyStoreException e) {
      throw new OcspCheckerException("Problem getting aliases from truststore.", e);
    }

    while (aliases.hasMoreElements()) {
      String currentAlias = aliases.nextElement();

      try {
        java.security.cert.Certificate currentCert = truststore.getCertificate(currentAlias);
        X509CertificateHolder currentCertHolder =
            new X509CertificateHolder(currentCert.getEncoded());
        X500Name currentName = currentCertHolder.getSubject();
        if (name.equals(currentName)) {
          return currentCertHolder;
        }
      } catch (CertificateEncodingException | IOException | KeyStoreException ignore) {
      }
    }

    throw new OcspCheckerException(
        String.format("Could not find cert matching X500Name of %s", name));
  }

  /**
   * Sends the {@param ocspReq} request to all configured {@code cspServerUrls} & the OCSP server
   * urls optionally given in the given {@param cert}.
   *
   * @param cert - the {@link X509Certificate} to check.
   * @param ocspRequest - the {@link OCSPReq} to send.
   * @return a {@link List} of {@link OCSPResp}s for every configured {@code ocspServerUrls} & the
   *     OCSP server * urls optionally given in the given {@param cert}. Problematic responses are
   *     represented as null values.
   */
  @VisibleForTesting
  List<OCSPResp> sendOcspRequests(X509Certificate cert, OCSPReq ocspRequest) {
    Set<String> urlsToCheck = new HashSet<>();
    if (ocspServerUrls != null) {
      urlsToCheck.addAll(ocspServerUrls);
    }

    // try and pull an OCSP server url off of the cert
    urlsToCheck.addAll(getOcspUrlsFromCert(cert));

    List<OCSPResp> ocspResponses = new ArrayList<>();

    for (String ocspServerUrl : urlsToCheck) {
      if (isNotBlank(ocspServerUrl)) {
        try {
          SecureCxfClientFactory cxfClientFactory =
              factory.getSecureCxfClientFactory(ocspServerUrl, WebClient.class);
          WebClient client =
              cxfClientFactory
                  .getWebClient()
                  .accept("application/ocsp-response")
                  .type("application/ocsp-request");

          Response response = client.post(ocspRequest.getEncoded());
          ocspResponses.add(createOcspResponse(response));
          continue;
        } catch (IOException | OcspCheckerException | ProcessingException ignore) {
        }
      }
      ocspResponses.add(null); // if ocspServerUrl is null or if there was an exception
    }

    return ocspResponses;
  }

  /**
   * Attempts to grab additional OCSP server urls off of the given {@param cert}.
   *
   * @param - the {@link X509Certificate} to check.
   * @return {@link List} of additional OCSP server urls found on the given {@param cert}.
   */
  private List<String> getOcspUrlsFromCert(X509Certificate cert) {
    List<String> ocspUrls = new ArrayList<>();

    try {
      byte[] authorityInfoAccess = cert.getExtensionValue(Extension.authorityInfoAccess.getId());

      if (authorityInfoAccess == null) {
        return ocspUrls;
      }

      AuthorityInformationAccess authorityInformationAccess =
          AuthorityInformationAccess.getInstance(
              X509ExtensionUtil.fromExtensionValue(authorityInfoAccess));

      if (authorityInformationAccess == null) {
        return ocspUrls;
      }

      for (AccessDescription description : authorityInformationAccess.getAccessDescriptions()) {
        GeneralName accessLocation = description.getAccessLocation();
        if (accessLocation.getTagNo() == GeneralName.uniformResourceIdentifier)
          ocspUrls.add(((DERIA5String) accessLocation.getName()).getString());
      }
    } catch (IOException ignore) {
    }

    return ocspUrls;
  }

  /**
   * Creates a {@link OCSPResp} from the given {@param response}.
   *
   * @param response - the {@link Response} to convert.
   * @return an {@link OCSPResp} of the given {@param response}.
   * @throws OcspCheckerException if any error occurs.
   */
  private OCSPResp createOcspResponse(Response response) throws OcspCheckerException {
    Object entity = response.getEntity();

    if (!(entity instanceof InputStream)) {
      throw new OcspCheckerException("Response did not contain an entity of type InputStream.");
    }

    try (InputStream inputStream = (InputStream) entity) {
      return new OCSPResp(inputStream);
    } catch (IOException e) {
      throw new OcspCheckerException("Problem converting Response into OCSPResp.");
    }
  }

  /**
   * Verifies the status of the {@param ocspResponse}.
   *
   * @param ocspResponses - the {@link List} of responses with statuses to verify
   * @return true if none of the statuses are revoked, false otherwise
   */
  private boolean anyOcspResponseStatusIsRevoked(List<OCSPResp> ocspResponses) {
    return ocspResponses
        .stream()
        .filter(Objects::nonNull)
        .anyMatch(
            ocspResponse -> {
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

                return status instanceof RevokedStatus;

              } catch (OCSPException ignore) {
                return false;
              }
            });
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

  public void setOcspServerUrls(List<String> ocspServerUrls) {
    this.ocspServerUrls = ocspServerUrls;
  }

  /**
   * Custom exception usually thrown after an unexpected error occurred while validating a
   * certificate. An alert should be posted to the admin console first.
   */
  class OcspCheckerException extends Exception {
    public OcspCheckerException() {
      super();
    }

    public OcspCheckerException(Exception cause) {
      super(cause);
    }

    public OcspCheckerException(String msg) {
      super(msg);
    }

    public OcspCheckerException(String msg, Exception cause) {
      super(msg, cause);
    }
  }
}
