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
package org.codice.ddf.platform.email;

import java.util.concurrent.Future;
import javax.mail.Message;
import javax.mail.Session;

/**
 * Provides a light-weight interface to javax.mail. The user should call {@link #createSession()} to
 * get a session that is preconfigured with the hostname and port number of the email server. Next,
 * construct a {@link Message} and submit it to {@link #send(Message)}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * SmtpClient smtpClient = <get an instance>
 *
 * Session session = emailService.createSession();
 *
 * MimeMessage mimeMessage = new MimeMessage(session);
 * mimeMessage.setFrom(new InternetAddress("from@test.com"));
 * mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("to@test.com"));
 * mimeMessage.setSubject("The Subject Line");
 *
 * BodyPart messageBodyPart = new MimeBodyPart();
 * messageBodyPart.setText("The Body Text");
 *
 * Multipart multipart = new MimeMultipart();
 * multipart.addBodyPart(messageBodyPart);
 *
 * mimeMessage.setContent(multipart);
 *
 * emailService.send(mimeMessage);
 *
 * }</pre>
 *
 * <p>
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface SmtpClient {

  /**
   * Create a session object that is pre-populated with the connection related parameters.
   *
   * @return session object
   */
  Session createSession();

  /**
   * Send the message. May be sent synchronously or asynchronously.
   *
   * @return a future that the caller can use to determine when the operation completes
   */
  Future<Void> send(Message message);
}
