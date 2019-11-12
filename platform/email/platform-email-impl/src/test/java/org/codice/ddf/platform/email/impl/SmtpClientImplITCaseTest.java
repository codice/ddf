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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SmtpClientImplITCaseTest {

  private static final String HOSTNAME = "127.0.0.1";

  private static final String ATTACHMENT_TEXT = "text text text";

  private static final String ATTACHMENT_FILENAME = "myFile.txt";

  private static final String SUBJECT = "sample subject";

  private static final String TO_ADDR = "user@example.com";

  private static final String FROM_ADDR = "nobody@example.com";

  private static final String BODY = "test body";

  private static final String SUBJECT_HEADER = "Subject";

  private static final String FROM_HEADER = "From";

  private static final String TO_HEADER = "To";

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testSendWithAttachments()
      throws IOException, MessagingException, ExecutionException, InterruptedException {

    int port = findAvailablePort();

    SimpleSmtpServer server = SimpleSmtpServer.start(port);

    SmtpClientImpl emailService = new SmtpClientImpl();

    emailService.setHostName(HOSTNAME);
    emailService.setPortNumber(port);

    File tmpFile = folder.newFile("email.txt");

    try (OutputStream os = new FileOutputStream(tmpFile)) {
      os.write(ATTACHMENT_TEXT.getBytes());
    }

    Session session = emailService.createSession();

    MimeMessage mimeMessage = new MimeMessage(session);
    mimeMessage.setFrom(new InternetAddress(FROM_ADDR));
    mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(TO_ADDR));
    mimeMessage.setSubject(SUBJECT);

    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setText(BODY);

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    messageBodyPart = new MimeBodyPart();
    messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(tmpFile)));
    messageBodyPart.setFileName(ATTACHMENT_FILENAME);
    multipart.addBodyPart(messageBodyPart);

    mimeMessage.setContent(multipart);

    emailService.send(mimeMessage).get();

    server.stop();

    List<SmtpMessage> emails = server.getReceivedEmails();
    assertThat(emails, is(not(empty())));
    SmtpMessage email = emails.get(0);
    assertNotNull(email);
    assertThat(email.getHeaderValue(SUBJECT_HEADER), is(SUBJECT));
    assertThat(email.getHeaderValue(FROM_HEADER), containsString(FROM_ADDR));
    assertThat(email.getHeaderValue(TO_HEADER), containsString(TO_ADDR));
    assertThat(email.getBody(), containsString(BODY));
    assertThat(email.getBody(), containsString(ATTACHMENT_TEXT));
    assertThat(email.getBody(), containsString(ATTACHMENT_FILENAME));
  }

  @Test
  public void testSend()
      throws IOException, MessagingException, ExecutionException, InterruptedException {

    int port = findAvailablePort();

    SimpleSmtpServer server = SimpleSmtpServer.start(port);

    SmtpClientImpl emailService = new SmtpClientImpl();

    emailService.setHostName(HOSTNAME);
    emailService.setPortNumber(port);

    Session session = emailService.createSession();

    MimeMessage mimeMessage = new MimeMessage(session);
    mimeMessage.setFrom(new InternetAddress(FROM_ADDR));
    mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(TO_ADDR));
    mimeMessage.setSubject(SUBJECT);

    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setText(BODY);

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    mimeMessage.setContent(multipart);

    emailService.send(mimeMessage).get();

    server.stop();

    List<SmtpMessage> emails = server.getReceivedEmails();
    assertThat(emails, is(not(empty())));
    SmtpMessage email = emails.get(0);
    assertNotNull(email);
    assertThat(email.getHeaderValue(SUBJECT_HEADER), is(SUBJECT));
    assertThat(email.getHeaderValue(FROM_HEADER), containsString(FROM_ADDR));
    assertThat(email.getHeaderValue(TO_HEADER), containsString(TO_ADDR));
    assertThat(email.getBody(), containsString(BODY));
  }

  /**
   * There isn't a great way to test that authenticated email sending works in a unit test, but we
   * can at least make sure the PasswordAuthentication object in the Session has the correct
   * username and password.
   */
  @Test
  public void testWithUsernamePassword() throws UnknownHostException {

    String username = "username";
    String password = "password";

    validateUsernamePassword(username, password);
  }

  /** The password is allowed to be empty. */
  @Test
  public void testWithUsernameEmptyPassword() throws UnknownHostException {

    String username = "username";
    String password = "";

    validateUsernamePassword(username, password);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithNullHostname() {
    SmtpClientImpl emailService = new SmtpClientImpl();
    emailService.createSession();
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsIllegalArgumentExceptionWhenPortNumberIsLessThanOne() {
    new SmtpClientImpl().setPortNumber(0);
  }

  private void validateUsernamePassword(String username, String password)
      throws UnknownHostException {
    SmtpClientImpl emailService = new SmtpClientImpl();

    emailService.setHostName("host.com");
    emailService.setPortNumber(25);
    emailService.setUserName(username);
    emailService.setPassword(password);

    Session session = emailService.createSession();

    PasswordAuthentication actual =
        session.requestPasswordAuthentication(
            InetAddress.getByName("127.0.0.1"), 25, "smtp", "prompt", "defaultUserName");

    assertThat(actual.getUserName(), is(username));
    assertThat(actual.getPassword(), is(password));
  }

  private int findAvailablePort() throws IOException {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    }
  }
}
