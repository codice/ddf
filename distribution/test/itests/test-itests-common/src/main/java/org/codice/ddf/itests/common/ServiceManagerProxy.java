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
package org.codice.ddf.itests.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;

import ddf.security.service.SecurityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.common.Security;

/** Runs the ServiceManager methods as the system subject */
public class ServiceManagerProxy implements InvocationHandler {

  private static final Security SECURITY = Security.getInstance();

  private ServiceManager serviceManager;

  ServiceManagerProxy(ServiceManager serviceManager) {
    this.serviceManager = serviceManager;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // wait until the security manager is available otherwise the getSystemSubject command will fail
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .until(() -> serviceManager.getServiceReference(SecurityManager.class) != null);

    RetryPolicy retryPolicy =
        new RetryPolicy().withMaxRetries(10).withDelay(1, SECONDS).retryWhen(null);
    Subject subject =
        Failsafe.with(retryPolicy).get(() -> SECURITY.runAsAdmin(SECURITY::getSystemSubject));
    return subject.execute(() -> method.invoke(serviceManager, args));
  }
}
