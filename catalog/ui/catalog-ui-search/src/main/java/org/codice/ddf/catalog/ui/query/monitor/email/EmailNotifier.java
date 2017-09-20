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
package org.codice.ddf.catalog.ui.query.monitor.email;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Map;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.MetacardFormatter;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.codice.ddf.platform.email.SmtpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sends an email for each workspace to the owner of the workspace. */
public class EmailNotifier implements QueryUpdateSubscriber {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotifier.class);

  private final MetacardFormatter metacardFormatter;

  private String bodyTemplate;

  private String subjectTemplate;

  private String fromEmail;

  private SubscriptionsPersistentStore subscriptionsPersistentStore;

  private SmtpClient smtpClient;

  /**
   * The {@code bodyTemplate} and {@code subjectTemplate} may contain the tags supported by the
   * {@code metacardFormatter}.
   *
   * @param bodyTemplate A string that represents the email body, will be passed to {@link
   *     MetacardFormatter}.
   * @param subjectTemplate A string that represents the subject line of the email, will be passed
   *     to {@link MetacardFormatter}.
   * @param fromEmail The 'from' email address to be set in the email.
   * @param metacardFormatter Used to format the body and subject line.
   * @param subscriptionsPersistentStore Used to find the email address associated with a workspace
   *     id.
   * @param smtpClient The {@link SmtpClient} that will send the email.
   */
  public EmailNotifier(
      String bodyTemplate,
      String subjectTemplate,
      String fromEmail,
      MetacardFormatter metacardFormatter,
      SubscriptionsPersistentStore subscriptionsPersistentStore,
      SmtpClient smtpClient) {
    notNull(bodyTemplate, "bodyTemplate must be non-null");
    notNull(subjectTemplate, "subjectTemplate must be non-null");
    notNull(fromEmail, "fromEmail must be non-null");
    notNull(subscriptionsPersistentStore, "subscriptionsPersistentStore must be non-null");
    notNull(smtpClient, "smtpClient must be non-null");

    this.bodyTemplate = bodyTemplate;
    this.subjectTemplate = subjectTemplate;
    this.fromEmail = fromEmail;
    this.metacardFormatter = metacardFormatter;
    this.subscriptionsPersistentStore = subscriptionsPersistentStore;
    this.smtpClient = smtpClient;
  }

  /**
   * The template string used for the email body.
   *
   * @param bodyTemplate must be non-null
   */
  @SuppressWarnings("unused")
  public void setBodyTemplate(String bodyTemplate) {
    notNull(bodyTemplate, "bodyTemplate must be non-null");
    LOGGER.debug("Setting bodyTemplate : {}", bodyTemplate);
    this.bodyTemplate = bodyTemplate;
  }

  /**
   * The template string used for the email subject.
   *
   * @param subjectTemplate must be non-null
   */
  @SuppressWarnings("unused")
  public void setSubjectTemplate(String subjectTemplate) {
    notNull(subjectTemplate, "subjectTemplate must be non-null");
    LOGGER.debug("Setting subjectTemplate : {}", subjectTemplate);
    this.subjectTemplate = subjectTemplate;
  }

  /**
   * The FROM email address.
   *
   * @param fromEmail must be non-blank
   */
  @SuppressWarnings("unused")
  public void setFromEmail(String fromEmail) {
    notBlank(fromEmail, "fromEmail must be non-blank");
    LOGGER.debug("Setting fromEmail : {}", fromEmail);
    this.fromEmail = fromEmail.trim();
  }

  @Override
  public void notify(Map<String, Pair<WorkspaceMetacardImpl, Long>> workspaceMetacardMap) {
    notNull(workspaceMetacardMap, "workspaceMetacardMap must be non-null");
    workspaceMetacardMap
        .values()
        .forEach(pair -> sendEmailsForWorkspace(pair.getLeft(), pair.getRight()));
  }

  private void sendEmailsForWorkspace(WorkspaceMetacardImpl workspaceMetacard, Long hitCount) {
    subscriptionsPersistentStore
        .getEmails(workspaceMetacard.getId())
        .forEach(email -> sendEmailForWorkspace(workspaceMetacard, hitCount, email));
  }

  private void sendEmailForWorkspace(
      WorkspaceMetacardImpl workspaceMetacard, Long hitCount, String email) {

    String emailBody = metacardFormatter.format(bodyTemplate, workspaceMetacard, hitCount);

    String subject = metacardFormatter.format(subjectTemplate, workspaceMetacard, hitCount);

    Session session = smtpClient.createSession();

    try {
      MimeMessage mimeMessage = new MimeMessage(session);

      mimeMessage.setFrom(new InternetAddress(fromEmail));

      mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email));

      mimeMessage.setSubject(subject);

      mimeMessage.setText(emailBody);

      LOGGER.trace("Attempting to send email");

      smtpClient.send(mimeMessage);

    } catch (MessagingException e) {
      LOGGER.warn("unable to send email to {}", email, e);
    }
  }

  @Override
  public String toString() {
    return "EmailNotifier{"
        + "metacardFormatter="
        + metacardFormatter
        + ", bodyTemplate='"
        + bodyTemplate
        + '\''
        + ", subjectTemplate='"
        + subjectTemplate
        + '\''
        + ", fromEmail='"
        + fromEmail
        + '\''
        + ", subscriptionsPersistentStore="
        + subscriptionsPersistentStore
        + '}';
  }
}
