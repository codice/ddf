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
package ddf.security.audit.impl;

import com.google.common.net.HttpHeaders;
import ddf.security.SecurityConstants;
import ddf.security.SubjectOperations;
import ddf.security.audit.AuditPropertiesPlugin;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.security.util.ThreadContextProperties;

/** Class that contains utility methods for logging common security messages. */
public final class SecurityLoggerImpl implements ddf.security.audit.SecurityLogger {

  private static final Logger LOGGER = LogManager.getLogger(SecurityConstants.SECURITY_LOGGER);

  private static final String NO_USER = "UNKNOWN";

  private static final boolean REQUIRE_AUDIT_ENCODING =
      Boolean.parseBoolean(
          System.getProperty("org.codice.ddf.platform.requireAuditEncoding", "false"));

  private static final String SUBJECT = "Subject: ";

  private static final String EXTRA_ATTRIBUTES_PROP = "security.logger.extra_attributes";

  private static final String LOOPBACK_ADDRESS = "127.0.0.1";

  private final SubjectOperations subjectOperations;

  private List<AuditPropertiesPlugin> auditPropertiesPlugins = new LinkedList<>();

  public SecurityLoggerImpl(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }

  public void setAuditPropertiesPlugins(List<AuditPropertiesPlugin> auditPropertiesPlugins) {
    this.auditPropertiesPlugins = auditPropertiesPlugins;
  }

  private String getUser(Subject subject) {
    try {
      if (subject == null) {
        subject = ThreadContext.getSubject();
      }
      if (subject == null) {
        javax.security.auth.Subject javaSubject =
            javax.security.auth.Subject.getSubject(AccessController.getContext());
        if (javaSubject != null) {
          Set<UserPrincipal> userPrincipal = javaSubject.getPrincipals(UserPrincipal.class);
          if (userPrincipal != null && !userPrincipal.isEmpty()) {
            return userPrincipal.toArray(new UserPrincipal[1])[0].getName();
          }
        }
      } else {
        return subjectOperations.getName(subject, NO_USER);
      }
    } catch (Exception e) {
      // ignore and return NO_USER
    }
    return NO_USER;
  }

  private void appendAdditionalAttributes(Message message, StringBuilder messageBuilder) {
    appendAdditionalAttributes(null, message, messageBuilder);
  }

  private void appendAdditionalAttributes(
      Subject subject, Message message, StringBuilder messageBuilder) {

    String additionAuditProperties =
        auditPropertiesPlugins.stream()
            .map(AuditPropertiesPlugin::generate)
            .filter(Objects::nonNull)
            .map(pair -> pair.getKey() + " " + pair.getValue())
            .collect(Collectors.joining(", "));

    if (StringUtils.isNotEmpty(additionAuditProperties)) {
      messageBuilder.append(additionAuditProperties).append(", ");
    }

    String user = getUser(subject);
    messageBuilder.append(SUBJECT).append(user).append(", ");
    appendConditionalAttributes(subject, messageBuilder);

    if (message == null) {
      addIpAndPort(
          ThreadContextProperties.getRemoteAddress(),
          ThreadContextProperties.getRemotePort(),
          messageBuilder);
    } else {
      HttpServletRequest servletRequest =
          (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
      // pull out the ip and port of the incoming connection so we know
      // who is trying to get access
      if (servletRequest != null) {
        String clientIP;
        String clientPort;
        String xForwardedFor = servletRequest.getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (StringUtils.isNotEmpty(xForwardedFor)) {
          // A proxy has set the client information in the x-forwarded-* headers.
          clientIP = xForwardedFor;
          clientPort = servletRequest.getHeader(HttpHeaders.X_FORWARDED_PORT);
        } else {
          // otherwise the remote_addr/remote_port headers should contain the actual client info
          clientIP = servletRequest.getRemoteAddr();
          clientPort = Integer.toString(servletRequest.getRemotePort());
        }
        addIpAndPort(clientIP, clientPort, messageBuilder);
      } else if (MessageUtils.isOutbound(message)) {
        messageBuilder
            .append(" Outbound endpoint: ")
            .append(message.get(Message.ENDPOINT_ADDRESS))
            .append(", ");
      }
    }
  }

  private void addIpAndPort(String remoteAddress, String remotePort, StringBuilder messageBuilder) {
    if (remoteAddress != null) {
      try {
        // ServletRequest#getRemoteAddr() can return a long loopback IP6 address which is less than
        // ideal for audit logging
        remoteAddress =
            InetAddress.getByName(remoteAddress).isLoopbackAddress()
                ? LOOPBACK_ADDRESS
                : remoteAddress;
      } catch (UnknownHostException e) {
        // ignore
      }
      messageBuilder.append(" Client IP: ").append(remoteAddress);
      if (remotePort != null) {
        messageBuilder.append(", Port: ").append(remotePort).append(", ");
      } else {
        messageBuilder.append(", ");
      }
    } else {
      messageBuilder.append(" ");
    }
  }
  /**
   * Appends any additional attributes as defined in the comma-delimited system property {@link
   * #EXTRA_ATTRIBUTES_PROP}.
   *
   * @param subject the subject of the logging request
   * @param messageBuilder buffer to which to append attribute text, if any
   */
  private void appendConditionalAttributes(Subject subject, StringBuilder messageBuilder) {
    String attributes = System.getProperty(EXTRA_ATTRIBUTES_PROP);
    if (attributes == null) {
      messageBuilder.append(", ");
      return;
    }

    if (subject == null) {
      subject = ThreadContext.getSubject();
    }

    List<String> attributeList = Arrays.asList(attributes.split(","));
    for (String attribute : attributeList) {
      List<String> attributeValueList = subjectOperations.getAttribute(subject, attribute);
      if (CollectionUtils.isNotEmpty(attributeValueList)) {
        messageBuilder.append(" ").append(attribute).append(": {");
        if (attributeValueList.size() > 1) {
          messageBuilder.append(attributeValueList);
        } else {
          messageBuilder.append(attributeValueList.get(0));
        }
        messageBuilder.append("}");
      }
    }
  }

  /**
   * Ensure that logs cannot be forged.
   *
   * @param message
   * @return clean message
   */
  private String cleanAndEncode(String message) {
    String clean = message.replace('\n', '_').replace('\r', '_');
    if (REQUIRE_AUDIT_ENCODING) {
      clean = StringEscapeUtils.escapeHtml(clean);
    }
    return clean;
  }

  /**
   * Logs a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message string to log.
   * @param subject the user subject to log
   */
  @Override
  public void audit(String message, Subject subject) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString());
  }

  /**
   * Logs a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message string to log.
   */
  @Override
  public void audit(String message) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString());
  }

  /**
   * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param subject the user subject to log
   * @param params parameters to the message.
   */
  @Override
  public void audit(String message, Subject subject, Object... params) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), params);
  }

  /**
   * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param params parameters to the message.
   */
  @Override
  public void audit(String message, Object... params) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), params);
  }

  /**
   * Logs a message with parameters which are only to be constructed if the logging level is the
   * {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param subject the user subject to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message
   *     parameters.
   */
  @Override
  public void audit(String message, Subject subject, Supplier... paramSuppliers) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), paramSuppliers);
  }

  /**
   * Logs a message with parameters which are only to be constructed if the logging level is the
   * {@link org.apache.logging.log4j.Level#INFO INFO} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param paramSuppliers An array of functions, which when called, produce the desired log message
   *     parameters.
   */
  @Override
  public void audit(String message, Supplier... paramSuppliers) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), paramSuppliers);
  }

  /**
   * Logs a message at the {@link org.apache.logging.log4j.Level#INFO INFO} level including the
   * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param subject the user subject to log
   * @param t the exception to log, including its stack trace.
   */
  @Override
  public void audit(String message, Subject subject, Throwable t) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), t);
  }

  /**
   * Logs a message at the {@link org.apache.logging.log4j.Level#INFO INFO} level including the
   * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t the exception to log, including its stack trace.
   */
  @Override
  public void audit(String message, Throwable t) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.info(messageBuilder.append(cleanAndEncode(message)).toString(), t);
  }

  /**
   * Logs a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message string to log.
   * @param subject the user subject to log
   */
  @Override
  public void auditWarn(String message, Subject subject) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString());
  }

  /**
   * Logs a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message string to log.
   */
  @Override
  public void auditWarn(String message) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString());
  }

  /**
   * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param subject the user subject to log
   * @param params parameters to the message.
   */
  @Override
  public void auditWarn(String message, Subject subject, Object... params) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), params);
  }

  /**
   * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param params parameters to the message.
   */
  @Override
  public void auditWarn(String message, Object... params) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), params);
  }

  /**
   * Logs a message with parameters which are only to be constructed if the logging level is the
   * {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param subject the user subject to log
   * @param paramSuppliers An array of functions, which when called, produce the desired log message
   *     parameters.
   */
  @Override
  public void auditWarn(String message, Subject subject, Supplier... paramSuppliers) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), paramSuppliers);
  }

  /**
   * Logs a message with parameters which are only to be constructed if the logging level is the
   * {@link org.apache.logging.log4j.Level#WARN WARN} level.
   *
   * @param message the message to log; the format depends on the message factory.
   * @param paramSuppliers An array of functions, which when called, produce the desired log message
   *     parameters.
   */
  @Override
  public void auditWarn(String message, Supplier... paramSuppliers) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), paramSuppliers);
  }

  /**
   * Logs a message at the {@link org.apache.logging.log4j.Level#WARN WARN} level including the
   * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param subject the user subject to log
   * @param t the exception to log, including its stack trace.
   */
  @Override
  public void auditWarn(String message, Subject subject, Throwable t) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(subject, PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), t);
  }

  /**
   * Logs a message at the {@link org.apache.logging.log4j.Level#WARN WARN} level including the
   * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
   *
   * @param message the message object to log.
   * @param t the exception to log, including its stack trace.
   */
  @Override
  public void auditWarn(String message, Throwable t) {
    StringBuilder messageBuilder = new StringBuilder();
    appendAdditionalAttributes(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
    LOGGER.warn(messageBuilder.append(cleanAndEncode(message)).toString(), t);
  }
}
