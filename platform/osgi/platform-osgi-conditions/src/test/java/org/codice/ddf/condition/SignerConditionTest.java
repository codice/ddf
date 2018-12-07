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
package org.codice.ddf.condition;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.ConditionInfo;

public class SignerConditionTest {

  @Test
  public void testIsNotSatisfied() throws CertificateException {
    Bundle bundle = mock(Bundle.class);
    Map<X509Certificate, List<X509Certificate>> trustedCerts = new HashMap<>();
    X509Certificate key =
        (X509Certificate)
            CertificateFactory.getInstance("X.509")
                .generateCertificate(SignerConditionTest.class.getResourceAsStream("/asdf.der"));
    trustedCerts.put(key, new ArrayList<>());
    when(bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED)).thenReturn(trustedCerts);
    SignerCondition principalCondition =
        new SignerCondition(
            bundle,
            new ConditionInfo(
                SignerCondition.class.getName(), new String[] {"signer1", "signer2", "signer3"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(false));
  }

  @Test
  public void testIsSatisfied() throws CertificateException {
    Bundle bundle = mock(Bundle.class);
    Map<X509Certificate, List<X509Certificate>> trustedCerts = new HashMap<>();
    X509Certificate key =
        (X509Certificate)
            CertificateFactory.getInstance("X.509")
                .generateCertificate(SignerConditionTest.class.getResourceAsStream("/asdf.der"));
    trustedCerts.put(key, new ArrayList<>());
    when(bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED)).thenReturn(trustedCerts);
    SignerCondition principalCondition =
        new SignerCondition(
            bundle, new ConditionInfo(SignerCondition.class.getName(), new String[] {"asdf"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsSatisfiedBrokenSan() throws CertificateException {
    Bundle bundle = mock(Bundle.class);
    Map<X509Certificate, List<X509Certificate>> trustedCerts = new HashMap<>();
    X509Certificate key = mock(X509Certificate.class);
    X500Principal principal = new X500Principal("CN=test, OU=Dev, O=DDF, ST=AZ, C=US");
    when(key.getSubjectX500Principal()).thenReturn(principal);
    when(key.getSubjectAlternativeNames()).thenThrow(new CertificateParsingException("boom"));
    trustedCerts.put(key, new ArrayList<>());
    when(bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED)).thenReturn(trustedCerts);
    SignerCondition principalCondition =
        new SignerCondition(
            bundle, new ConditionInfo(SignerCondition.class.getName(), new String[] {"test"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsSatisfiedUnexpectedErrorInByteSan() throws CertificateException {
    Bundle bundle = mock(Bundle.class);
    Map<X509Certificate, List<X509Certificate>> trustedCerts = new HashMap<>();
    X509Certificate key = mock(X509Certificate.class);
    X500Principal principal = new X500Principal("CN=test, OU=Dev, O=DDF, ST=AZ, C=US");
    when(key.getSubjectX500Principal()).thenReturn(principal);
    List<List<?>> altNames = new ArrayList<>();
    List<Object> objects = new ArrayList<>();
    objects.add(0);
    objects.add(new byte[0]);
    altNames.add(objects);
    when(key.getSubjectAlternativeNames()).thenReturn(altNames);
    trustedCerts.put(key, new ArrayList<>());
    when(bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED)).thenReturn(trustedCerts);
    SignerCondition principalCondition =
        new SignerCondition(
            bundle, new ConditionInfo(SignerCondition.class.getName(), new String[] {"test"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsAltNameSatisfied() throws CertificateException {
    Bundle bundle = mock(Bundle.class);
    Map<X509Certificate, List<X509Certificate>> trustedCerts = new HashMap<>();
    X509Certificate key =
        (X509Certificate)
            CertificateFactory.getInstance("X.509")
                .generateCertificate(SignerConditionTest.class.getResourceAsStream("/test.der"));
    trustedCerts.put(key, new ArrayList<>());
    when(bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED)).thenReturn(trustedCerts);
    SignerCondition principalCondition =
        new SignerCondition(
            bundle, new ConditionInfo(SignerCondition.class.getName(), new String[] {"test"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(true));

    // also check alt name
    principalCondition =
        new SignerCondition(
            bundle, new ConditionInfo(SignerCondition.class.getName(), new String[] {"alt-test"}));
    satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(true));
  }
}
