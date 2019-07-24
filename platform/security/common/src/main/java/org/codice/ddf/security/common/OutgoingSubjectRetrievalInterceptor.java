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
package org.codice.ddf.security.common;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationTokenFactory;

/**
 * OutgoingSubjectRetrievalInterceptor provides a implementation of {@link AbstractPhaseInterceptor}
 * that stores the receivers subject in the header of the response with a key of
 * ddf.security.Subject.
 */
public class OutgoingSubjectRetrievalInterceptor extends AbstractPhaseInterceptor<Message>
    implements Handler<WrappedMessageContext> {

  private final SecurityManager securityManager;

  private final STSAuthenticationTokenFactory tokenFactory;

  private EventSecurityEndingInterceptor ending = new EventSecurityEndingInterceptor();

  public OutgoingSubjectRetrievalInterceptor() {
    super(Phase.PRE_STREAM);
    tokenFactory = new STSAuthenticationTokenFactory();
    tokenFactory.init();
    securityManager = Security.getInstance().getSecurityManager();
  }

  @Override
  public void handleMessage(Message message) throws Fault {

    if (this.isRequestor(message) && "https".equals(message.get("http.scheme"))) {

      MessageTrustDecider originalTrustDecider = message.get(MessageTrustDecider.class);
      ReceiverTrustDecider receiverTrustDecider = new ReceiverTrustDecider(originalTrustDecider);
      message.put(MessageTrustDecider.class, receiverTrustDecider);
      message.getInterceptorChain().add(ending);
    }
  }

  @Override
  public boolean handleMessage(WrappedMessageContext context) {
    handleMessage(context.getWrappedMessage());
    return true;
  }

  @Override
  public boolean handleFault(WrappedMessageContext context) {
    return true;
  }

  @Override
  public void close(MessageContext context) {}

  public static class EventSecurityEndingInterceptor extends AbstractPhaseInterceptor<Message> {
    public EventSecurityEndingInterceptor() {
      super(Phase.SETUP_ENDING);
    }

    public void handleMessage(Message message) throws Fault {
      ((Map) message.getExchange().getInMessage().get(Message.PROTOCOL_HEADERS))
          .put(Subject.class.toString(), Arrays.asList(new Subject[] {message.get(Subject.class)}));
    }
  }

  public class ReceiverTrustDecider extends MessageTrustDecider {
    private final MessageTrustDecider orig;

    ReceiverTrustDecider(MessageTrustDecider orig) {
      super();
      this.orig = orig;
    }

    @Override
    public void establishTrust(
        String conduitName, URLConnectionInfo urlConnectionInfo, Message message)
        throws UntrustedURLConnectionIOException {

      if (this.orig != null) {
        this.orig.establishTrust(conduitName, urlConnectionInfo, message);
      }
      HttpsURLConnectionInfo info = (HttpsURLConnectionInfo) urlConnectionInfo;
      if (info.getServerCertificates() == null && info.getServerCertificates().length == 0) {
        throw new UntrustedURLConnectionIOException(
            "Unable to establish trust because no certificates were found.");
      }

      X509Certificate[] certs = ((X509Certificate[]) info.getServerCertificates());
      try {
        BaseAuthenticationToken token =
            tokenFactory.fromCertificates(certs, urlConnectionInfo.getURI().getHost());

        Subject receiverSubject = securityManager.getSubject(token);
        message.put(Subject.class, receiverSubject);

      } catch (SecurityServiceException e) {
        UntrustedURLConnectionIOException exception =
            new UntrustedURLConnectionIOException(
                "Error trying to get receiver subject for event.");
        exception.initCause(e);
        throw exception;
      }
    }
  }
}
