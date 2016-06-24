/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.email;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.MetacardFormatter;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends an email for each workspace to the owner of the workspace.
 */
public class EmailNotifier implements QueryUpdateSubscriber {

    private static final String SMTP_HOST_PROPERTY = "mail.smtp.host";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotifier.class);

    private final MetacardFormatter metacardFormatter;

    private String bodyTemplate;

    private String subjectTemplate;

    private String fromEmail;

    private String mailHost;

    private SubscriptionsPersistentStore subscriptionsPersistentStore;

    /**
     * The {@code bodyTemplate} and {@code subjectTemplate} may contain the tags supported by the
     * {@code metacardFormatter}.
     *
     * @param bodyTemplate                 must be non-null
     * @param subjectTemplate              must be non-null
     * @param fromEmail                    must be non-null
     * @param mailHost                     must be non-null
     * @param metacardFormatter            must be non-null
     * @param subscriptionsPersistentStore must be non-null
     */
    public EmailNotifier(String bodyTemplate, String subjectTemplate, String fromEmail,
            String mailHost, MetacardFormatter metacardFormatter,
            SubscriptionsPersistentStore subscriptionsPersistentStore) {
        notNull(bodyTemplate, "bodyTemplate must be non-null");
        notNull(subjectTemplate, "subjectTemplate must be non-null");
        notNull(fromEmail, "fromEmail must be non-null");
        notNull(mailHost, "mailHost must be non-null");
        notNull(subscriptionsPersistentStore, "subscriptionsPersistentStore must be non-null");

        this.bodyTemplate = bodyTemplate;
        this.subjectTemplate = subjectTemplate;
        this.fromEmail = fromEmail;
        this.mailHost = mailHost;
        this.metacardFormatter = metacardFormatter;
        this.subscriptionsPersistentStore = subscriptionsPersistentStore;
    }

    /**
     * The hostname of the mail server.
     *
     * @param mailHost must be non-blank
     */
    @SuppressWarnings("unused")
    public void setMailHost(String mailHost) {
        notBlank(mailHost, "mailHost must be non-blank");
        this.mailHost = mailHost.trim();
    }

    /**
     * The template string used for the email body.
     *
     * @param bodyTemplate must be non-null
     */
    @SuppressWarnings("unused")
    public void setBodyTemplate(String bodyTemplate) {
        notNull(bodyTemplate, "bodyTemplate must be non-null");
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
        this.fromEmail = fromEmail.trim();
    }

    @Override
    public void notify(Map<String, Pair<WorkspaceMetacardImpl, Long>> workspaceMetacardMap) {
        notNull(workspaceMetacardMap, "workspaceMetacardMap must be non-null");

        workspaceMetacardMap.values()
                .forEach(pair -> sendEmailsForWorkspace(pair.getLeft(), pair.getRight()));

    }

    private void sendEmailsForWorkspace(WorkspaceMetacardImpl workspaceMetacard, Long hitCount) {
        subscriptionsPersistentStore.getEmails(workspaceMetacard.getId())
                .forEach(email -> sendEmailForWorkspace(workspaceMetacard, hitCount, email));
    }

    private void sendEmailForWorkspace(WorkspaceMetacardImpl workspaceMetacard, Long hitCount,
            String email) {

        String emailBody = metacardFormatter.format(bodyTemplate, workspaceMetacard, hitCount);

        String subject = metacardFormatter.format(subjectTemplate, workspaceMetacard, hitCount);

        Properties properties = createSessionProperies();

        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setFrom(new InternetAddress(fromEmail));

            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email));

            mimeMessage.setSubject(subject);

            mimeMessage.setText(emailBody);

            Transport.send(mimeMessage);

        } catch (MessagingException e) {
            LOGGER.warn("unable to send email to {}", email, e);
        }

    }

    private Properties createSessionProperies() {
        Properties properties = System.getProperties();

        properties.setProperty(SMTP_HOST_PROPERTY, mailHost);

        return properties;
    }

    @Override
    public String toString() {
        return "EmailNotifier{" +
                "metacardFormatter=" + metacardFormatter +
                ", bodyTemplate='" + bodyTemplate + '\'' +
                ", subjectTemplate='" + subjectTemplate + '\'' +
                ", fromEmail='" + fromEmail + '\'' +
                ", mailHost='" + mailHost + '\'' +
                ", subscriptionsPersistentStore=" + subscriptionsPersistentStore +
                '}';
    }
}
