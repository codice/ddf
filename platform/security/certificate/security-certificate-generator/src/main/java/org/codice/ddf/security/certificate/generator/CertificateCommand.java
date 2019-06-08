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

import ddf.security.SecurityConstants;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;

public class CertificateCommand {
  /**
   * Pass in a string to use as the common name of the certificate to be generated. Exception thrown
   * if 0 arguments or more than 2 argument (or more than 4 when -san is used).
   *
   * <pre>
   * Arguments to this program are: <code>(-cn &lt;cn&gt;|-dn &lt;dn&gt;) [-san &lt;tag:name,tag:name,...&gt;]</code>
   *
   * where:
   * &lt;cn&gt; represents a fully qualified common name (e.g. "&lt;FQDN&gt;", where &lt;FQDN&gt; could be something like cluster.yoyo.com)
   * &lt;dn&gt; represents a distinguished name as a comma-delimited string (e.g. "c=US, st=California, o=Yoyodyne, l=San Narciso, cn=&lt;FQDN&gt;")
   * &lt;tag:name,tag:name,...&gt; represents optional subject alternative names to be added to the generated certificate
   *    (e.g. "DNS:&lt;FQDN&gt;,DNS:node1.&lt;FQDN&gt;,DNS:node2.&lt;FQDN&gt;"). The format for subject alternative names
   *    is similar to the OpenSSL X509 configuration format. Supported tags are:
   *      email - email subject
   *      URI - uniformed resource identifier
   *      RID - registered id
   *      DNS - hostname
   *      IP - ip address (V4 or V6)
   *      dirName - directory name
   * </pre>
   *
   * @param args arguments to the certificate program (see above for description)
   */
  public static void main(String[] args) {
    // Try to extract -san if provided, result will be array of strings of the form tag:value
    String[] sans = null;
    int expected = 2;
    int cnDnPosition = 0;

    if (args.length == 4) {
      if ("-san".equalsIgnoreCase(args[0].trim())) {
        sans = Arrays.stream(args[1].split("[,]")).map(String::trim).toArray(String[]::new);
        cnDnPosition = 2;
        expected = 4;
      } else if ("-san".equalsIgnoreCase(args[2].trim())) {
        sans = Arrays.stream(args[3].split("[,]")).map(String::trim).toArray(String[]::new);
        expected = 4;
      }
    }

    if (args.length != expected) {
      throw new IllegalArgumentException(
          String.format(
              "java %s (-cn <cn>|-d <dn>) [-san \"<tag:name,tag:name,...>\"]%n%n" //
                  + "where:%n"
                  + "  <cn> represents a fully qualified common name (e.g. \"<FQDN>\", where <FQDN> could be something like cluster.yoyo.com)%n"
                  + "  <dn> represents a distinguished name as a comma-delimited string (e.g. \"c=US, st=California, o=Yoyodyne, l=San Narciso, cn=<FQDN>\")%n"
                  + "  <tag:name,tag:name,...> represents optional subject alternative names to be added to the generated certificate%n"
                  + "     (e.g. \"DNS:<FQDN>,DNS:node1.<FQDN>,DNS:node2.<FQDN>\"). The format for subject alternative names%n"
                  + "     is similar to the OpenSSL X509 configuration format. Supported tags are:%n"
                  + "       email - email subject%n"
                  + "       URI - uniformed resource identifier%n"
                  + "       RID - registered id%n" //
                  + "       DNS - hostname%n"
                  + "       IP - ip address (V4 or V6)%n"
                  + "       dirName - directory name%n",
              CertificateCommand.class.getCanonicalName()));
    }

    if ("-cn".equalsIgnoreCase(args[cnDnPosition].trim())) {
      configureDemoCert(args[cnDnPosition + 1].trim(), sans);
    } else {
      String[] dn =
          Arrays.stream(args[cnDnPosition + 1].split("[,]"))
              .map(String::trim)
              .toArray(String[]::new);
      configureDemoCertWithDN(dn, sans);
    }
  }

  /**
   * Generates new signed certificate. The input parameter is used as the certificate's common name.
   *
   * <p>Postcondition is the server keystore is updated to include a private entry. The private
   * entry has the new certificate chain that connects the server to the Demo CA. The matching
   * private key is also stored in the entry.
   *
   * @param commonName string to use as the common name in the new certificate.
   * @return the string used as the common name in the new certificate
   */
  public static String configureDemoCert(String commonName) {
    return configureDemoCert(commonName, null);
  }

  /**
   * Generates new signed certificate. The input parameter is used as the certificate's common name
   * and optional subject alternative names.
   *
   * <p>Postcondition is the server keystore is updated to include a private entry. The private
   * entry has the new certificate chain that connects the server to the Demo CA. The matching
   * private key is also stored in the entry.
   *
   * @param commonName string to use as the common name in the new certificate.
   * @param subjectAltNames names in the form {@code tag:name} (format similar to OpenSSL X509
   *     configuration)
   * @return the string used as the common name in the new certificate
   */
  public static String configureDemoCert(String commonName, @Nullable String[] subjectAltNames) {
    CertificateSigningRequest csr = new CertificateSigningRequest();
    csr.setCommonName(commonName);

    // Required for Chrome support (DDF-3104)
    csr.addSubjectAlternativeNames(String.format("DNS:%s", commonName));

    if (subjectAltNames != null) {
      csr.addSubjectAlternativeNames(subjectAltNames);
    }
    return configureCert(commonName, csr);
  }

  /**
   * Generates new signed certificate. The input parameter is the full set of attributes of the
   * distinguished name for the cert. It must include a single {@code CN} value for the common name.
   * <p>
   * Postcondition is the server keystore is updated to include a private entry. The private
   * entry has the new certificate chain that connects the server to the Demo CA. The matching
   * private key is also stored in the entry.
   *
   * @param dn String params in the form {@code attrKey=attrVal} composing a distinguished name.
   *           e.g. {@code configureDemoCertWithDN(new String[] {"cn=John Whorfin", "o=Yoyodyne", "l=San Narciso", "st=California", "c=US")}
   * @return the string used as the common name in the new certificate
   */
  public static String configureDemoCertWithDN(String[] dn) {
    return configureDemoCertWithDN(dn, null);
  }

  /**
   * Generates new signed certificate. The input parameter is the full set of attributes of the
   * distinguished name for the cert and optional subject alternative names. It must include a
   * single {@code CN} value for the common name.
   * <p>
   * Postcondition is the server keystore is updated to include a private entry. The private
   * entry has the new certificate chain that connects the server to the Demo CA. The matching
   * private key is also stored in the entry.
   *
   * @param dn              String params in the form {@code attrKey=attrVal} composing a distinguished name.
   *                        e.g. {@code configureDemoCertWithDN(new String[] {"cn=John Whorfin", "o=Yoyodyne", "l=San Narciso", "st=California", "c=US"), null}
   * @param subjectAltNames names in the form {@code tag:name} (format similar to OpenSSL X509 configuration)
   * @return the string used as the common name in the new certificate
   */
  public static String configureDemoCertWithDN(String[] dn, @Nullable String[] subjectAltNames) {
    CertificateSigningRequest csr = new CertificateSigningRequest();
    csr.setDistinguishedName(dn);

    String commonName =
        csr.getSubjectName().getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
    csr.addSubjectAlternativeNames(String.format("DNS:%s", commonName));

    if (subjectAltNames != null) {
      csr.addSubjectAlternativeNames(subjectAltNames);
    }

    RDN[] rdns = csr.getSubjectName().getRDNs(BCStyle.CN);
    Validate.notNull(rdns, "RDN Array cannot be null");
    Validate.isTrue(rdns.length == 1, "CN attribute must be included in distinguished name");

    return configureCert(rdns[0].getFirst().getValue().toString(), csr);
  }

  protected static KeyStoreFile getKeyStoreFile() {
    return KeyStoreFile.openFile(
        SecurityConstants.getKeystorePath(), SecurityConstants.getKeystorePassword().toCharArray());
  }

  private static String configureCert(String commonName, CertificateSigningRequest csr) {
    CertificateAuthority demoCa = new DemoCertificateAuthority();
    KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
    KeyStoreFile ksFile = getKeyStoreFile();
    for (String alias : ksFile.aliases()) {
      if (ksFile.isKey(alias)) {
        ksFile.deleteEntry(alias);
      }
    }
    ksFile.setEntry(commonName, pkEntry);
    ksFile.save();
    return ((X509Certificate) pkEntry.getCertificate()).getSubjectDN().getName();
  }
}
