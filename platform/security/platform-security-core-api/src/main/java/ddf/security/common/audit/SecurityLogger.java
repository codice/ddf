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
package ddf.security.common.audit;

import java.security.AccessController;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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

import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;

/**
 * Class that contains utility methods for logging common security messages.
 */
public final class SecurityLogger {

    private static final Logger LOGGER = LogManager.getLogger(SecurityConstants.SECURITY_LOGGER);

    private static final String NO_USER = "UNKNOWN";

    private SecurityLogger() {

    }

    private static String getUser(Subject subject) {
        try {
            if (subject == null) {
                subject = ThreadContext.getSubject();
            }
            if (subject == null) {
                javax.security.auth.Subject javaSubject = javax.security.auth.Subject.getSubject(
                        AccessController.getContext());
                if (javaSubject != null) {
                    Set<UserPrincipal> userPrincipal = javaSubject.getPrincipals(UserPrincipal.class);
                    if (userPrincipal != null && userPrincipal.size() > 0) {
                        return userPrincipal.toArray(new UserPrincipal[1])[0].getName();
                    }
                }
            } else {
                return SubjectUtils.getName(subject, NO_USER);
            }
        } catch (Exception e) {
           //ignore and return NO_USER
        }
        return NO_USER;
    }

    private static void requestIpAndPortAndUserMessage(Message message,
            StringBuilder messageBuilder) {
        requestIpAndPortAndUserMessage(null, message, messageBuilder);
    }

    private static void requestIpAndPortAndUserMessage(Subject subject, Message message,
            StringBuilder messageBuilder) {
        String user = getUser(subject);
        if (message != null) {
            HttpServletRequest servletRequest = (HttpServletRequest) message.get(
                    AbstractHTTPDestination.HTTP_REQUEST);
            // pull out the ip and port of the incoming connection so we know
            // who is trying to get access
            if (servletRequest != null) {
                messageBuilder.append("Subject: ")
                        .append(user)
                        .append(" Request IP: ")
                        .append(servletRequest.getRemoteAddr())
                        .append(", Port: ")
                        .append(servletRequest.getRemotePort())
                        .append(" ");
            } else if (MessageUtils.isOutbound(message)) {
                messageBuilder.append("Subject: ")
                        .append(user)
                        .append(" Outbound endpoint: ")
                        .append(message.get(Message.ENDPOINT_ADDRESS))
                        .append(" ");
            }
        } else {
            messageBuilder.append("Subject: ")
                    .append(user)
                    .append(" ");
        }
    }

    /**
     * Logs a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message string to log.
     * @param subject the user subject to log
     */
    public static void audit(String message, Subject subject) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString());
    }

    /**
     * Logs a message object with the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message string to log.
     */
    public static void audit(String message) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString());
    }

    /**
     * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param subject the user subject to log
     * @param params  parameters to the message.
     */
    public static void audit(String message, Subject subject, Object... params) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), params);
    }

    /**
     * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#INFO INFO} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    public static void audit(String message, Object... params) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), params);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link org.apache.logging.log4j.Level#INFO
     * INFO} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param subject        the user subject to log
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    public static void audit(String message, Subject subject, Supplier... paramSuppliers) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), paramSuppliers);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link org.apache.logging.log4j.Level#INFO
     * INFO} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    public static void audit(String message, Supplier... paramSuppliers) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), paramSuppliers);
    }

    /**
     * Logs a message at the {@link org.apache.logging.log4j.Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param subject the user subject to log
     * @param t       the exception to log, including its stack trace.
     */
    public static void audit(String message, Subject subject, Throwable t) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), t);
    }

    /**
     * Logs a message at the {@link org.apache.logging.log4j.Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public static void audit(String message, Throwable t) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.info(messageBuilder.append(message)
                .toString(), t);
    }

    /**
     * Logs a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message string to log.
     * @param subject the user subject to log
     */
    public static void auditWarn(String message, Subject subject) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString());
    }

    /**
     * Logs a message object with the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message string to log.
     */
    public static void auditWarn(String message) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString());
    }

    /**
     * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param subject the user subject to log
     * @param params  parameters to the message.
     */
    public static void auditWarn(String message, Subject subject, Object... params) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), params);
    }

    /**
     * Logs a message with parameters at the {@link org.apache.logging.log4j.Level#WARN WARN} level.
     *
     * @param message the message to log; the format depends on the message factory.
     * @param params  parameters to the message.
     */
    public static void auditWarn(String message, Object... params) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), params);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link org.apache.logging.log4j.Level#WARN
     * WARN} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param subject        the user subject to log
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    public static void auditWarn(String message, Subject subject, Supplier... paramSuppliers) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), paramSuppliers);
    }

    /**
     * Logs a message with parameters which are only to be constructed if the logging level is the {@link org.apache.logging.log4j.Level#WARN
     * WARN} level.
     *
     * @param message        the message to log; the format depends on the message factory.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    public static void auditWarn(String message, Supplier... paramSuppliers) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), paramSuppliers);
    }

    /**
     * Logs a message at the {@link org.apache.logging.log4j.Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param subject the user subject to log
     * @param t       the exception to log, including its stack trace.
     */
    public static void auditWarn(String message, Subject subject, Throwable t) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(subject,
                PhaseInterceptorChain.getCurrentMessage(),
                messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), t);
    }

    /**
     * Logs a message at the {@link org.apache.logging.log4j.Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    public static void auditWarn(String message, Throwable t) {
        StringBuilder messageBuilder = new StringBuilder();
        requestIpAndPortAndUserMessage(PhaseInterceptorChain.getCurrentMessage(), messageBuilder);
        LOGGER.warn(messageBuilder.append(message)
                .toString(), t);
    }

}
