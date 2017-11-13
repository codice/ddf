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

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class SignerCondition implements Condition {

  private Bundle bundle;
  private String[] args;

  public SignerCondition(Bundle bundle, ConditionInfo conditionInfo) {
    this.bundle = bundle;
    args = conditionInfo.getArgs();
  }

  @Override
  public boolean isPostponed() {
    return false;
  }

  @Override
  public boolean isSatisfied() {
    Map<X509Certificate, List<X509Certificate>> signerCertificates =
        bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
    Set<X509Certificate> x509Certificates = signerCertificates.keySet();
    for (String arg : args) {
      boolean satisfied = false;
      for (X509Certificate x509Certificate : x509Certificates) {
        String cn = getExtendedCertAttribute(x509Certificate.getSubjectX500Principal(), BCStyle.CN);
        List<String> subjectAlternativeNames = getSubjectAlternativeNames(x509Certificate);
        if (arg.equals(cn) || subjectAlternativeNames.contains(arg)) {
          satisfied = true;
        }
      }
      if (!satisfied) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public boolean isSatisfied(Condition[] conditions, Dictionary<Object, Object> context) {
    for (Condition condition : conditions) {
      if (!condition.isSatisfied()) {
        return false;
      }
    }
    return true;
  }

  private static String getExtendedCertAttribute(
      X500Principal principal, ASN1ObjectIdentifier identifier) {
    RDN[] rdNs = new X500Name(principal.getName()).getRDNs(identifier);
    if (rdNs != null && rdNs.length > 0) {
      AttributeTypeAndValue attributeTypeAndValue = rdNs[0].getFirst();
      if (attributeTypeAndValue != null) {
        return attributeTypeAndValue.getValue().toString();
      }
    }
    return "";
  }

  private List<String> getSubjectAlternativeNames(X509Certificate certificate) {
    List<String> identities = new ArrayList<>();
    try {
      Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
      if (altNames == null) {
        return Collections.emptyList();
      }
      collectAltNames(altNames, identities);
    } catch (CertificateParsingException e) {
      return Collections.emptyList();
    }
    return identities;
  }

  private void collectAltNames(Collection<List<?>> altNames, List<String> identities) {
    for (List item : altNames) {
      Integer type = (Integer) item.get(0);
      if (type == 0 || type == 2) {
        try {
          if (item.toArray()[1] instanceof byte[]) {
            identities.add(getIdentifyFromBytes((byte[]) item.toArray()[1]));
          } else if (item.toArray()[1] instanceof String) {
            identities.add((String) item.toArray()[1]);
          }
        } catch (Exception e) {
          return;
        }
      }
    }
  }

  private String getIdentifyFromBytes(byte[] itemBytes) {
    try (ASN1InputStream decoder = new ASN1InputStream(itemBytes)) {
      ASN1Encodable encoded = decoder.readObject();
      encoded = ((DERSequence) encoded).getObjectAt(1);
      encoded = ((DERTaggedObject) encoded).getObject();
      encoded = ((DERTaggedObject) encoded).getObject();
      return ((DERUTF8String) encoded).getString();
    } catch (IOException e) {
      return "";
    }
  }
}
