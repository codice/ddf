/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.platform.scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.common.util.Security;

/**
 * Executes Felix/Karaf commands when called as a Quartz {@link Job}
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class CommandJob implements Job {

    public static final String COMMAND_KEY = "command";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandJob.class);

    private CommandProcessor commandProcessor;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        Subject subject = getSystemSubject();
        if (subject != null) {
            subject.execute(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    doExecute(context);
                    return null;
                }
            });
        } else {
            LOGGER.warn("Could not execute command. Could not get subject to run command");
        }
    }

    public void doExecute(JobExecutionContext context) throws JobExecutionException {

        String commandInput = null;
        try {
            commandInput = checkInput(context);
        } catch (CommandException e) {
            return;
        }

        if (getCommandProcessor() == null) {
            LOGGER.warn("No CommandProcessor instance to run commands.");
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(byteArrayOutputStream);

        CommandSession commandSession = getCommandProcessor().createSession(null, output, output);

        try {
            if (commandInput != null) {
                try {
                    LOGGER.info("Executing command [{}]", commandInput);
                    commandSession.execute(commandInput);
                    LOGGER.info("Execution Output: {}",
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
        } finally {

            if (commandSession != null) {
                commandSession.close();
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                LOGGER.info("Could not close output stream", e);
            }
        }

    }

    public Subject getSystemSubject() {
        return Security.getSystemSubject();
    }

    private CommandProcessor getCommandProcessor() {

        if (this.commandProcessor != null) {
            return this.commandProcessor;
        }

        String key = CommandProcessor.class.getSimpleName();

        Object commandProcessorObject = ServiceStore.getInstance().getObject(key);

        if (commandProcessorObject != null) {
            this.commandProcessor = (CommandProcessor) commandProcessorObject;
            return this.commandProcessor;
        }

        return null;
    }

    private String checkInput(JobExecutionContext context) throws CommandException {

        String command = null;

        if (context == null) {
            LOGGER.warn("No JobExecutionContext found. Could not fire {}",
                    CommandJob.class.getSimpleName());
            throw new CommandException();
        }

        JobDataMap mergedJobDataMap = context.getMergedJobDataMap();

        if (mergedJobDataMap == null) {
            LOGGER.warn("No input found. Could not fire {}", CommandJob.class.getSimpleName());
            throw new CommandException();
        }

        if (mergedJobDataMap.getString(COMMAND_KEY) != null) {
            command = mergedJobDataMap.getString(COMMAND_KEY);
        }

        return command;
    }

    private static class CommandException extends Exception {

    }
}
