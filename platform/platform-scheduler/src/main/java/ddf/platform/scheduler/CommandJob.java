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
import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.codice.ddf.security.common.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes Felix/Karaf commands when called as a Quartz {@link Job}
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class CommandJob implements Job {

  public static final String COMMAND_KEY = "command";

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandJob.class);

  private static final Security SECURITY = Security.getInstance();

  protected Subject getSystemSubject() {
    return SECURITY.getSystemSubject();
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    SECURITY.runAsAdmin(
        () -> {
          Subject subject = getSystemSubject();

          if (subject != null) {
            subject.execute(
                () -> {
                  doExecute(context);
                  return null;
                });
          } else {
            LOGGER.debug("Could not execute command. Could not get subject to run command");
          }

          return null;
        });
  }

  private Bundle getBundle() {
    return FrameworkUtil.getBundle(getClass());
  }

  protected SessionFactory getSessionFactory() {
    BundleContext bundleContext = getBundle().getBundleContext();
    if (bundleContext == null) {
      return null;
    }
    return bundleContext.getService(bundleContext.getServiceReference(SessionFactory.class));
  }

  private void doExecute(JobExecutionContext context) {
    String commandInput;
    try {
      commandInput = checkInput(context);
    } catch (CommandException e) {
      LOGGER.debug("unable to get command from job execution context", e);
      return;
    }

    SessionFactory sessionFactory = getSessionFactory();

    if (sessionFactory == null) {
      LOGGER.debug("unable to create session factory: command=[{}]", commandInput);
      return;
    }

    Session session = null;

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream output = getPrintStream(byteArrayOutputStream);
        // TODO DDF-3280 remove work-around for NPE when creating session with a null "in" parameter
        // from a SessionFactory
        InputStream emptyInputStream =
            new InputStream() {
              @Override
              public int read() throws IOException {
                LOGGER.error(
                    "This method implementation of an InputStream is just a work-around for a Karaf bug and should never be called. There is an issue with the Platform Command Scheduler implementation.");
                return -1;
              }
            }) {

      session = sessionFactory.create(emptyInputStream, output, output);

      if (session == null) {
        LOGGER.debug("unable to create session: command=[{}]", commandInput);
        return;
      }

      if (commandInput != null) {
        try {
          LOGGER.trace("Executing command [{}]", commandInput);
          session.execute(commandInput);
          LOGGER.trace(
              "Execution Output: {}",
              byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()));
        } catch (CommandNotFoundException e) {
          LOGGER.info(
              "Command could not be found. Make sure the command's library has been loaded and try again: {}",
              e.getLocalizedMessage());
          LOGGER.debug("Command not found.", e);
        } catch (Exception e) {
          LOGGER.info("Error with execution. ", e);
        }
      }
    } catch (UnsupportedEncodingException e) {
      LOGGER.info("Unable to produce output", e);
    } catch (IOException e) {
      LOGGER.warn(
          "Error with the emptyInputStream used as a work-around for a Karaf bug. This should never happen. There is an issue with the Platform Command Scheduler implementation.",
          e);
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  private PrintStream getPrintStream(ByteArrayOutputStream byteArrayOutputStream)
      throws UnsupportedEncodingException {
    return new PrintStream(byteArrayOutputStream, false, StandardCharsets.UTF_8.name());
  }

  private String checkInput(JobExecutionContext context) throws CommandException {
    String command = null;

    if (context == null) {
      LOGGER.debug(
          "No JobExecutionContext found. Could not fire {}", CommandJob.class.getSimpleName());
      throw new CommandException();
    }

    JobDataMap mergedJobDataMap = context.getMergedJobDataMap();

    if (mergedJobDataMap == null) {
      LOGGER.debug("No input found. Could not fire {}", CommandJob.class.getSimpleName());
      throw new CommandException();
    }

    if (mergedJobDataMap.getString(COMMAND_KEY) != null) {
      command = mergedJobDataMap.getString(COMMAND_KEY);
    }

    return command;
  }

  private static class CommandException extends Exception {}
}
