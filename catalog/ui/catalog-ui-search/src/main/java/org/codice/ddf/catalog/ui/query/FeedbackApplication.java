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
package org.codice.ddf.catalog.ui.query;

import static spark.Spark.exception;
import static spark.Spark.post;
import static spark.route.RouteOverview.enableRouteOverview;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.query.feedback.FeedbackRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.platform.email.SmtpClient;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class FeedbackApplication implements SparkApplication {
  private static final String APPLICATION_JSON = "application/json";

  private static final String UNKNOWN = "Unknown";

  private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackApplication.class);

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .setPrettyPrinting()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private ConfigurationApplication configurationApplication;

  private Template subjectTemplate;

  private Template bodyTemplate;

  private SmtpClient smtpClient;

  private String emailDestination;

  private EndpointUtil util;

  @Override
  public void init() {
    post(
        "/feedback",
        APPLICATION_JSON,
        (req, res) -> {
          if (StringUtils.isNotEmpty(emailDestination)) {
            FeedbackRequest feedback = parseFeedbackRequest(util.safeGetBody(req));
            feedback.setAuthUsername(getCurrentUser());

            String emailSubject = getEmailSubject(feedback);
            String emailBody = getEmailBody(feedback);
            if (emailBody != null) {
              emailBody = emailBody.replaceAll("\\\\n", "\n");
            } else {
              emailBody = "<html/>";
            }

            Session emailSession = smtpClient.createSession();
            MimeMessage message = new MimeMessage(emailSession);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailDestination));
            message.setSubject(emailSubject);
            message.setContent(emailBody, "text/html; charset=utf-8");
            smtpClient.send(message);

            res.body("{}");
            res.status(200);
            return res;
          } else {
            res.status(500);
            res.body("No destination email configured, feedback cannot be submitted.");
            LOGGER.debug("Feedback submission failed, destination email is not configured.");
            return res;
          }
        });

    exception(
        Exception.class,
        (e, request, response) -> {
          response.status(500);
          response.body("Error submitting feedback");
          LOGGER.debug("Feedback submission failed", e);
        });

    enableRouteOverview();
  }

  public void setConfigurationApplication(ConfigurationApplication configurationApplication) {
    this.configurationApplication = configurationApplication;
    initConfigurationAppValues(true);
  }

  public void setSmtpClient(SmtpClient smtpClient) {
    this.smtpClient = smtpClient;
  }

  public void setEndpointUtil(EndpointUtil util) {
    this.util = util;
  }

  public String getEmailDestination() {
    return emailDestination;
  }

  public void setEmailDestination(String emailDestination) {
    this.emailDestination = emailDestination;
  }

  public void refresh(Map<String, Object> configuration) {
    if (MapUtils.isEmpty(configuration)) {
      return;
    }

    SmtpClient configSmtpClient = (SmtpClient) configuration.get("smtpClient");
    if (configSmtpClient != null) {
      this.smtpClient = configSmtpClient;
    }

    this.emailDestination = configurationApplication.getQueryFeedbackEmailDestination();
    initConfigurationAppValues(true);
  }

  private static FeedbackRequest parseFeedbackRequest(String json) {
    FeedbackRequest feedbackRequest = new FeedbackRequest();
    String name = UNKNOWN;
    String email = UNKNOWN;
    String searchStr = UNKNOWN;
    String workspaceId = UNKNOWN;
    String workspaceName = UNKNOWN;
    Date searchInitiated = null;
    List<Object> searchStatus = null;
    List<Object> searchResults = null;

    Map<String, Object> rootObject =
        GSON.fromJson(json, GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE);

    Object userObj = rootObject.get("user");
    if (userObj instanceof Map) {
      Map<String, Object> userObject = (Map<String, Object>) userObj;
      name = (String) userObject.get("name");
      email = (String) userObject.get("email");
    }

    Object searchObj = rootObject.get("search");
    if (searchObj instanceof Map) {
      Map<String, Object> searchObject = (Map<String, Object>) searchObj;
      searchStr = (String) searchObject.get("cql");
      searchInitiated = new Date((Long) searchObject.get("initiated"));
      searchStatus = (List) searchObject.get("status");
      Object resultsObj = searchObject.get("results");
      if (resultsObj instanceof List) {
        searchResults = (List) resultsObj;
      }
    }

    Object workspaceObj = rootObject.get("workspace");
    if (workspaceObj instanceof Map) {
      Map<String, Object> workspaceObject = (Map<String, Object>) workspaceObj;
      workspaceId = (String) workspaceObject.get("id");
      workspaceName = (String) workspaceObject.get("name");
    }

    String comments = (String) rootObject.get("comments");

    feedbackRequest.setUsername(name);
    feedbackRequest.setEmail(email);
    feedbackRequest.setQuery(searchStr);
    if (searchInitiated != null) {
      feedbackRequest.setQueryInitiated(searchInitiated.toString());
    }
    if (searchResults != null) {
      String prettyPrintedJson = GSON.toJson(searchResults);
      feedbackRequest.setQueryResults(prettyPrintedJson);
    }
    if (searchStatus != null) {
      feedbackRequest.setQueryStatus(
          searchStatus.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }
    feedbackRequest.setWorkspaceId(workspaceId);
    feedbackRequest.setWorkspaceName(workspaceName);
    feedbackRequest.setComments(comments);

    return feedbackRequest;
  }

  private String getEmailSubject(FeedbackRequest feedback) {
    initConfigurationAppValues(false);
    return applyTemplate(subjectTemplate, feedback);
  }

  private String getEmailBody(FeedbackRequest feedback) {
    initConfigurationAppValues(false);
    return applyTemplate(bodyTemplate, feedback);
  }

  private String applyTemplate(Template template, FeedbackRequest feedback) {
    String templatedString = null;

    if (template != null) {
      Map<String, String> templateValueMap = getTemplateValueMap(feedback);
      try {
        templatedString = template.apply(templateValueMap);
      } catch (IOException e) {
        LOGGER.error("Unable to apply values to handle bars email body template", e);
      }
    }

    return templatedString;
  }

  private void initConfigurationAppValues(boolean reinit) {
    Handlebars handlebars = new Handlebars();

    if (reinit || subjectTemplate == null || bodyTemplate == null) {
      if (configurationApplication != null) {
        emailDestination = configurationApplication.getQueryFeedbackEmailDestination();
        String subjectTemplateStr = configurationApplication.getQueryFeedbackEmailSubjectTemplate();
        String bodyTemplateStr = configurationApplication.getQueryFeedbackEmailBodyTemplate();

        try {
          if (subjectTemplateStr != null) {
            subjectTemplate = handlebars.compileInline(subjectTemplateStr);
          }

          if (bodyTemplateStr != null) {
            bodyTemplate = handlebars.compileInline(bodyTemplateStr);
          }
        } catch (IOException e) {
          LOGGER.warn("Unable to compile email templates", e);
        }
      } else {
        LOGGER.debug("Feedback configuration is not set");
      }
    }
  }

  private Map<String, String> getTemplateValueMap(FeedbackRequest feedbackRequest) {
    Map<String, String> valueMap = new HashMap<>();

    valueMap.put("auth_username", feedbackRequest.getAuthUsername());
    valueMap.put("username", StringEscapeUtils.escapeHtml4(feedbackRequest.getUsername()));
    valueMap.put("email", StringEscapeUtils.escapeHtml4(feedbackRequest.getEmail()));
    valueMap.put("workspace_id", StringEscapeUtils.escapeHtml4(feedbackRequest.getWorkspaceId()));
    valueMap.put(
        "workspace_name", StringEscapeUtils.escapeHtml4(feedbackRequest.getWorkspaceName()));
    valueMap.put("query", StringEscapeUtils.escapeHtml4(feedbackRequest.getQuery()));
    valueMap.put(
        "query_initiated_time", StringEscapeUtils.escapeHtml4(feedbackRequest.getQueryInitiated()));
    valueMap.put("query_status", StringEscapeUtils.escapeHtml4(feedbackRequest.getQueryStatus()));
    valueMap.put("query_results", StringEscapeUtils.escapeHtml4(feedbackRequest.getQueryResults()));
    valueMap.put("comments", StringEscapeUtils.escapeHtml4(feedbackRequest.getComments()));

    return valueMap;
  }

  private String getCurrentUser() {
    Subject subject = (Subject) SecurityUtils.getSubject();
    return SubjectUtils.getName(subject);
  }
}
