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
package org.codice.ddf.cxf.client;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.shiro.subject.Subject;

/**
 * Secure client factory that adds hooks for authenticating via SAML and SAML ECP
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be * removed in a future version of the library. </b>
 *
 * @param <T>
 */
public interface SecureCxfClientFactory<T> {

  /**
   * Returns the client
   *
   * @return
   */
  T getClient();

  /**
   * Returns the WebClient
   *
   * @return
   */
  WebClient getWebClient();

  /**
   * Adds subject to a new client and returns
   *
   * @param subject
   * @return
   */
  T getClientForSubject(Subject subject);

  /**
   * Adds system subject to a new client and returns the client
   *
   * @param subject
   * @return
   */
  T getClientForSystemSubject(Subject subject);

  /**
   * Adds subject to a new WebClient and returns
   *
   * @param subject
   * @return
   */
  WebClient getWebClientForSubject(Subject subject);

  /**
   * Get max redirect
   *
   * @return
   */
  Integer getSameUriRedirectMax();

  /**
   * Add out interceptors
   *
   * @param inteceptor
   */
  void addOutInterceptors(Interceptor<? extends Message> inteceptor);
}
