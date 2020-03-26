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

import ddf.security.common.audit.SecurityLogger;
import ddf.security.impl.SubjectUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

public class SecurityLoggerOutInterceptor extends AbstractPhaseInterceptor<Message> {

  public SecurityLoggerOutInterceptor() {
    super(Phase.WRITE);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    if (MessageUtils.isRequestor(message)) {
      Subject subject = ThreadContext.getSubject();
      if (subject != null) {
        String username = SubjectUtils.getName(subject);
        SecurityLogger.audit("{} is making an outbound request.", username);
      } else {
        SecurityLogger.audit("No subject associated with outbound request.");
      }
    }
  }
}
