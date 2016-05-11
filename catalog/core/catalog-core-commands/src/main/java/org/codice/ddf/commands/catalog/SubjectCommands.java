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
package org.codice.ddf.commands.catalog;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.felix.gogo.commands.Option;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.service.SecurityServiceException;

/**
 * SubjectCommands provides the ability to change what subject (user) the extending command is run as.
 * If no user is specified and the user has the admin role, the command will execute as the system
 * user otherwise it will fail.
 */
public abstract class SubjectCommands extends CommandSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectCommands.class);

    private final Security security;

    @Option(name = "--user", required = false, aliases = {
            "-u"}, multiValued = false, description = "Run command as a different user")
    protected String user = null;

    protected SubjectCommands() {
        this(Security.getInstance());
    }

    SubjectCommands(Security security) {
        this.security = security;
    }

    /**
     * Executes the command once the user has been properly authorized.
     *
     * @return result of the command execution
     * @throws Exception if the command failed to run
     */
    protected abstract Object executeWithSubject() throws Exception;

    /**
     * Executes the command using the user name provided using the {@code --user} option and
     * prompts for a password. If no user name was provided, tries to run the command using the
     * current {@link Subject}, or elevates to the system subject is the user has permission to do
     * so.
     * <p/>
     * An error message will be printed out if the user name/password combination is invalid or
     * if the user doesn't have the permission to run the command.
     *
     * @return value returned by {@link #executeWithSubject()}, or {@code null} if the command failed
     * @throws InvocationTargetException thrown if an exception occurred while executing
     *                                   the command
     * @throws Exception                 if any other unexpected exception occurred
     */
    @Override
    protected Object doExecute() throws Exception {

        try {
            if (isNotBlank(user)) {
                return runWithUserName();
            } else {
                try {
                    return security.runWithSubjectOrElevate(this::executeWithSubject);
                } catch (SecurityServiceException e) {
                    printErrorMessage(e.getMessage());
                    return null;
                }
            }
        } catch (InvocationTargetException e) {
            printErrorMessage(e.getCause()
                    .getMessage());
            return null;
        }
    }

    private Object runWithUserName() throws InvocationTargetException {
        try {
            String password = getLine("Password for " + user + ": ", false);
            Subject subject = security.getSubject(user, password);

            if (subject == null) {
                printErrorMessage("Invalid username/password");
                return null;
            }

            return subject.execute(this::executeWithSubject);
        } catch (ExecutionException e) {
            LOGGER.error("Failed to run command: {}",
                    e.getCause()
                            .getMessage(),
                    e.getCause());
            throw new InvocationTargetException(e.getCause());
        } catch (IOException e) {
            LOGGER.error("Failed to run command", e);
            printErrorMessage("Failed to read password");
        }

        return null;
    }

    private String getLine(String text, boolean showCharacters) throws IOException {
        console.print(text);
        console.flush();
        StringBuilder buffer = new StringBuilder();

        while (true) {
            int byteOfData = session.getKeyboard()
                    .read();

            if (byteOfData < 0) {
                break;
            }

            if (showCharacters) {
                console.print((char) byteOfData);
                console.flush();
            } else {
                console.print("*");
                console.flush();
            }

            if (byteOfData == '\r' || byteOfData == '\n') {
                break;
            }

            buffer.append((char) byteOfData);
        }

        console.println();
        console.flush();
        return buffer.toString();
    }
}
