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
package ddf.security;

import java.security.Principal;
import java.util.StringTokenizer;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.assertion.SecurityAssertion;

/**
 * Utility class used to perform operations on Subjects.
 */
public final class SubjectUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectUtils.class);

    private SubjectUtils() {

    }

    /**
     * Retrieves the user name from a given subject.
     *
     * @param subject Subject to get the user name from.
     * @return String representation of the user name if available or null if no
     * user name could be found.
     */
    public static String getName(org.apache.shiro.subject.Subject subject) {
        return getName(subject, null);
    }

    /**
     * Retrieves the user name from a given subject.
     *
     * @param subject     Subject to get the user name from.
     * @param defaultName Name to send back if no user name was found.
     * @return String representation of the user name if available or
     * defaultName if no user name could be found or incoming subject
     * was null.
     */
    public static String getName(org.apache.shiro.subject.Subject subject, String defaultName) {
        String name = defaultName;
        if (subject != null) {
            PrincipalCollection principals = subject.getPrincipals();
            if (principals != null) {
                SecurityAssertion assertion = principals.oneByType(SecurityAssertion.class);
                if (assertion != null) {
                    Principal principal = assertion.getPrincipal();
                    if (principal instanceof X500Principal) {
                        StringTokenizer st = new StringTokenizer(principal.getName(), ",");
                        while (st.hasMoreElements()) {
                            // token is in the format:
                            // syntaxAndUniqueId
                            // cn
                            // ou
                            // o
                            // loc
                            // state
                            // country
                            String[] strArr = st.nextToken().split("=");
                            if (strArr.length > 1 && strArr[0].equalsIgnoreCase("cn")) {
                                name = strArr[1];
                                break;
                            }
                        }
                    } else if (principal instanceof KerberosPrincipal) {
                        StringTokenizer st = new StringTokenizer(principal.getName(), "@");
                        st = new StringTokenizer(st.nextToken(), "/");
                        name = st.nextToken();
                    } else {
                        name = assertion.getPrincipal().getName();
                    }
                } else {
                    // send back the primary principal as a string
                    name = principals.getPrimaryPrincipal().toString();
                }
            } else {
                LOGGER.debug(
                        "No principals located in the incoming subject, cannot look up user name. Using default name of {}.",
                        defaultName);
            }
        } else {
            LOGGER.debug(
                    "Incoming subject was null, cannot look up user name. Using default name of {}.",
                    defaultName);
        }

        LOGGER.debug("Sending back name {}.", name);
        return name;
    }
}
