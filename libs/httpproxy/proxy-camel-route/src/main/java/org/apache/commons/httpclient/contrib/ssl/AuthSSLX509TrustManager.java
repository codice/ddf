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
/*
 * $HeadURL$ $Revision$ $Date$
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language g0verning permissions and limitations under
 * the License. ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on behalf of the
 * Apache Software Foundation. For more information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.commons.httpclient.contrib.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.annotation.Nullable;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AuthSSLX509TrustManager can be used to extend the default {@link X509TrustManager} with
 * additional trust decisions.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *     <p>DISCLAIMER: HttpClient developers DO NOT actively support this component. The component is
 *     provided as a reference material, which may be inappropriate for use without additional
 *     customization.
 */
public class AuthSSLX509TrustManager implements X509TrustManager {
  /** Log object for this class. */
  private static final Log LOGGER = LogFactory.getLog(AuthSSLX509TrustManager.class);

  private X509TrustManager defaultTrustManager = null;

  /** Constructor for AuthSSLX509TrustManager. */
  public AuthSSLX509TrustManager(final X509TrustManager defaultTrustManager) {
    super();
    if (defaultTrustManager == null) {
      throw new IllegalArgumentException("Trust manager may not be null");
    }
    this.defaultTrustManager = defaultTrustManager;
  }

  /** @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[], String authType) */
  @Nullable
  public void checkClientTrusted(X509Certificate[] certificates, String authType)
      throws CertificateException {
    if (certificates != null) {
      if (LOGGER.isDebugEnabled()) {
        debugLogCertificates("Client", certificates);
      }
      defaultTrustManager.checkClientTrusted(certificates, authType);
    }
  }

  private void debugLogCertificates(String adjective, X509Certificate[] certificates) {
    int index = 0;
    for (X509Certificate cert : certificates) {
      LOGGER.debug(String.format(" %s certificate %d:", adjective, ++index));
      LOGGER.debug("  Subject DN: " + cert.getSubjectDN());
      LOGGER.debug("  Signature Algorithm: " + cert.getSigAlgName());
      LOGGER.debug("  Valid from: " + cert.getNotBefore());
      LOGGER.debug("  Valid until: " + cert.getNotAfter());
      LOGGER.debug("  Issuer: " + cert.getIssuerDN());
    }
  }

  /** @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[], String authType) */
  public void checkServerTrusted(X509Certificate[] certificates, String authType)
      throws CertificateException {
    if (LOGGER.isDebugEnabled() && certificates != null) {
      debugLogCertificates("Server", certificates);
    }
    defaultTrustManager.checkServerTrusted(certificates, authType);
  }

  /** @see javax.net.ssl.X509TrustManager#getAcceptedIssuers() */
  public X509Certificate[] getAcceptedIssuers() {
    return this.defaultTrustManager.getAcceptedIssuers();
  }
}
