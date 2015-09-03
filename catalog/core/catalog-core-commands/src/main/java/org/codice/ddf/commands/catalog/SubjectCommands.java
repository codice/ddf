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

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Option;
import org.apache.shiro.subject.Subject;

import ddf.security.common.util.SubjectUtils;

/**
 * SubjectCommands provides the ability to change what subject (user) the extending command is run as.
 * If no user is specified and the user has the admin role, the command will execute as the system
 * user otherwise it will fail.
 */
public abstract class SubjectCommands extends CommandSupport {

    @Option(name = "--user", required = false, aliases = {
            "-u"}, multiValued = false, description = "Run command as a different user")
    protected String user = null;

    protected SubjectUtils subjectUtils = new SubjectUtils();

    protected abstract Object executeAsSubject() throws Exception;

    @Override
    protected Object doExecute() throws Exception {
        Subject subject;
        if (!StringUtils.isEmpty(user)) {
            String password = getLine("Password for " + user + ": ", false);
            subject = subjectUtils.getSubject(user, password);
        } else {
            //verify that java subject has the correct roles (admin)
            if (!subjectUtils.javaSubjectHasAdminRole()) {
                printErrorMessage(
                        "Current user doesn't have sufficient privileges to run this command");
                return null;
            }
            //get system subject
            subject = subjectUtils.getSystemSubject();
        }

        if (subject == null) {
            printErrorMessage("Invalid username/password");
            return null;
        }

        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {
                return executeAsSubject();
            }
        };

        subject.execute(callable);
        return null;
    }

    protected String getLine(String text, boolean showCharacters) throws IOException {
        console.print(text);
        console.flush();
        StringBuffer buffer = new StringBuffer();
        while (true) {
            int byteOfData = session.getKeyboard().read();

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
