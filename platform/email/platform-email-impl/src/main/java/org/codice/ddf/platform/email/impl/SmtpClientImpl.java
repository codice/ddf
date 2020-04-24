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
package org.codice.ddf.platform.email.impl;

import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import ddf.security.audit.SecurityLogger;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.email.SmtpClient;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports unauthenticated connections to a mail server and username/password authenticated
 * connections over TLS. If the username is not blank and the password is not empty, then auth/tls
 * will be used.
 *
 * <p>The method {@link #send(Message)} will exceute asynchronously in a single thread. The number
 * of operations awaiting execution is unbounded.
 *
 * <p>
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public class SmtpClientImpl implements SmtpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmtpClientImpl.class);

  private static final String SMTP_HOST_PROPERTY = "mail.smtp.host";

  private static final String SMTP_PORT_PROPERTY = "mail.smtp.port";

  private static final String SMTP_AUTH_PROPERTY = "mail.smtp.auth";

  private static final String SMTP_START_TLS_ENABLE_PROPERTY = "mail.smtp.starttls.enable";

  private static final String TRUE = Boolean.TRUE.toString();

  private static final String FALSE = Boolean.FALSE.toString();

  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          1, StandardThreadFactoryBuilder.newThreadFactory("smtpClientImplThread"));

  private String hostName;

  private Integer portNumber;

  private String userName;

  private String password;

  private SecurityLogger securityLogger;

  /**
   * Set the username for the email server.
   *
   * @param userName the username of the email server
   */
  public void setUserName(@Nullable String userName) {
    this.userName = userName;
  }

  /**
   * Set the password for the email server.
   *
   * @param password the password for the email server
   */
  public void setPassword(@Nullable String password) {
    this.password = password;
  }

  /** @param hostName must be non-empty */
  public void setHostName(String hostName) {
    notEmpty(hostName, "hostName must be non-empty");
    this.hostName = hostName;
  }

  /** @param portNumber must be non-null and >0 */
  public void setPortNumber(Integer portNumber) {
    notNull(portNumber, "portNumber must be non-null");
    isTrue(portNumber > 0, "portNumber must be >0");
    this.portNumber = portNumber;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }

  @Override
  public Session createSession() {

    Properties properties = new Properties();

    if (hostName == null) {
      throw new IllegalArgumentException("Hostname cannot be null for smtp client.");
    }

    properties.setProperty(SMTP_HOST_PROPERTY, hostName);
    properties.setProperty(SMTP_PORT_PROPERTY, portNumber.toString());

    if (StringUtils.isNotBlank(userName)) {
      properties.put(SMTP_AUTH_PROPERTY, TRUE);
      properties.put(SMTP_START_TLS_ENABLE_PROPERTY, TRUE);

      return Session.getInstance(properties, createAuthenticator());
    } else {
      properties.setProperty(SMTP_AUTH_PROPERTY, FALSE);
      return Session.getInstance(properties);
    }
  }

  @Override
  public Future<Void> send(Message message) {
    notNull(message, "message must be non-null");
    return executorService.submit(
        () -> {
          final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          Thread.currentThread().setContextClassLoader(Transport.class.getClassLoader());
          try {
            Transport.send(message);
          } catch (MessagingException e) {
            LOGGER.debug("Could not send message {}", message, e);
            return null;
          } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
          }

          securityLogger.audit(
              "Sent an email: recipient={} subject={}",
              Arrays.toString(message.getAllRecipients()),
              message.getSubject());
          return null;
        });
  }

  Authenticator createAuthenticator() {
    return new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
      }
    };
  }
}
