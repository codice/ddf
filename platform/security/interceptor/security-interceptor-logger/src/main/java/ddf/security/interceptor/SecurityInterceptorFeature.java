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
package ddf.security.interceptor;

import ddf.security.SubjectOperations;
import ddf.security.audit.SecurityLogger;
import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

public class SecurityInterceptorFeature extends AbstractFeature {

  private static final SecurityLoggerInInterceptor SECURITY_LOGGER_IN =
      new SecurityLoggerInInterceptor();

  private static final SecurityLoggerOutInterceptor SECURITY_LOGGER_OUT =
      new SecurityLoggerOutInterceptor();

  public SecurityInterceptorFeature(
      SubjectOperations subjectOperations, SecurityLogger securityLogger) {
    SECURITY_LOGGER_IN.setSubjectOperations(subjectOperations);
    SECURITY_LOGGER_IN.setSecurityLogger(securityLogger);
    SECURITY_LOGGER_OUT.setSubjectOperations(subjectOperations);
    SECURITY_LOGGER_OUT.setSecurityLogger(securityLogger);
  }

  @Override
  protected void initializeProvider(InterceptorProvider provider, Bus bus) {
    provider.getInInterceptors().add(SECURITY_LOGGER_IN);
    provider.getOutInterceptors().add(SECURITY_LOGGER_OUT);
  }
}
