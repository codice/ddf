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
package ddf.platform.scheduler;

import ddf.security.Subject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.security.common.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes Felix/Karaf commands when called as a Quartz {@link Job}
 *
 * @author Ashraf Barakat
 */
public class CommandJob implements Job {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandJob.class);

  public static final String COMMAND_KEY = "command";

  private static final Security SECURITY = Security.getInstance();

  protected Subject getSystemSubject() {
    return SECURITY.getSystemSubject();
  }

  @Override
  public void execute(final JobExecutionContext context) {
    final String commandString = getCommandString(context);
    if (StringUtils.isNotBlank(commandString)) {
      try {
        SECURITY.runAsAdmin(
            () -> {
              final Subject subject = getSystemSubject();

              if (subject != null) {
                try {
                  subject.execute(
                      () -> {
                        doExecute(commandString);
                        return null;
                      });
                } catch (ExecutionException e) {
                  logWarningMessage(commandString);
                  LOGGER.debug("Unable to execute as subject.", e);
                }
              } else {
                // This might happen when the system is very slow to start up where not all of the
                // required security bundles are started yet.
                logWarningMessage(commandString);
                LOGGER.debug("Unable to get system subject.");
              }

              return null;
            });
      } catch (RuntimeException e) {
        // This might happen when the system is very slow to start up where not all of the required
        // security bundles are started yet.
        logWarningMessage(commandString);
        LOGGER.debug("Could not execute command as admin.", e);
      }
    } else {
      LOGGER.warn(
          "Could not execute command. Unable to get non-blank command string from job execution context.");
    }
  }

  @Nullable
  private Bundle getBundle() {
    return FrameworkUtil.getBundle(getClass());
  }

  @Nullable
  protected SessionFactory getSessionFactory() {
    final Bundle bundle = getBundle();
    if (bundle != null) {
      final BundleContext bundleContext = bundle.getBundleContext();
      if (bundleContext != null) {
        return bundleContext.getService(bundleContext.getServiceReference(SessionFactory.class));
      }
    }

    return null;
  }

  private void doExecute(final String commandString) {
    final SessionFactory sessionFactory = getSessionFactory();
    if (sessionFactory != null) {
      try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          final PrintStream output = createPrintStream(byteArrayOutputStream);
          // TODO DDF-3280 remove work-around for NPE when creating session with a null "in"
          // parameter from a SessionFactory
          final InputStream emptyInputStream =
              new InputStream() {
                @Override
                public int read() throws IOException {
                  LOGGER.error(
                      "This method implementation of an InputStream is just a work-around for a Karaf bug and should never be called. There is an issue with the Platform Command Scheduler implementation.");
                  return -1;
                }
              };
          final Session session = sessionFactory.create(emptyInputStream, output, output)) {
        if (session != null) {
          LOGGER.trace("Executing command \"{}\"", LogSanitizer.sanitize(commandString));
          try {
            session.execute(commandString);

            try {
              final String commandOutput =
                  byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
              LOGGER.info(
                  "Execution output for command \"{}\": {}",
                  LogSanitizer.sanitize(commandString),
                  LogSanitizer.sanitize(commandOutput));
            } catch (UnsupportedEncodingException e) {
              LOGGER.debug("Unable to get command output.", e);
            }
          } catch (Exception e) {
            logWarningMessage(commandString);
            LOGGER.debug("Unable to execute command.", e);
          }
        } else {
          logWarningMessage(commandString);
          LOGGER.debug("Unable to create session.");
        }
      } catch (IOException e) {
        logWarningMessage(commandString);
        LOGGER.debug("Unable to create session.", e);
      }
    } else {
      logWarningMessage(commandString);
      LOGGER.debug("Unable to create session factory.");
    }
  }

  private PrintStream createPrintStream(ByteArrayOutputStream byteArrayOutputStream)
      throws UnsupportedEncodingException {
    return new PrintStream(byteArrayOutputStream, false, StandardCharsets.UTF_8.name());
  }

  @Nullable
  private static String getCommandString(JobExecutionContext context) {
    if (context != null) {
      final JobDataMap mergedJobDataMap = context.getMergedJobDataMap();

      if (mergedJobDataMap != null) {
        return mergedJobDataMap.getString(COMMAND_KEY);
      }
    }

    return null;
  }

  private void logWarningMessage(String commandString) {
    LOGGER.warn(
        "Unable to execute command \"{}\". See debug log for more details. The command is still scheduled for execution according to the configured interval.",
        LogSanitizer.sanitize(commandString));
  }
}
