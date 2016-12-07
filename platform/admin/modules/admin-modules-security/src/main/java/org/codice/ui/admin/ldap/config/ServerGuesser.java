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
package org.codice.ui.admin.ldap.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

abstract class ServerGuesser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerGuesser.class);

    // TODO RAP 07 Dec 16: Add OpenDJ and OpenLDAP guessers
    private static final Map<String, Function<Connection, ServerGuesser>> GUESSER_LOOKUP =
            ImmutableMap.of("activeDirectory",
                    ServerGuesser.ADGuesser::new,
                    "embeddedLdap",
                    ServerGuesser.EmbeddedGuesser::new);

    protected final Connection connection;

    private ServerGuesser(Connection connection) {
        this.connection = connection;
    }

    static ServerGuesser buildGuesser(String ldapType, Connection connection) {
        return GUESSER_LOOKUP.get(ldapType)
                .apply(connection);
    }

    abstract String getBaseContext() throws Exception;

    abstract String getUserNameAttribute();

    List<String> getUserBaseChoices() {
        return getChoices("(|(ou=user*)(name=user*)(cn=user*))");
    }

    List<String> getGroupBaseChoices() {
        return getChoices("(|(ou=group*)(name=group*)(cn=group*))");
    }

    private List<String> getChoices(String query) {
        String baseContext;
        try {
            baseContext = getBaseContext();
        } catch (Exception e) {
            LOGGER.debug("Error getting baseContext", e);
            return Collections.emptyList();
        }

        List<String> choices = new ArrayList<>();
        try (ConnectionEntryReader reader = connection.search(baseContext,
                SearchScope.SINGLE_LEVEL,
                query)) {
            while (reader.hasNext()) {
                if (!reader.isReference()) {
                    SearchResultEntry resultEntry = reader.readEntry();
                    choices.add(resultEntry.getName()
                            .toString());
                } else {
                    // TODO RAP 07 Dec 16: What do we need to do with remote references?
                    reader.readReference();
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Error getting choices", e);
        }

        return choices;
    }

    private static class ADGuesser extends ServerGuesser {
        private ADGuesser(Connection connection) {
            super(connection);
        }

        @Override
        String getBaseContext() throws Exception {
            ConnectionEntryReader reader = connection.search("",
                    SearchScope.BASE_OBJECT,
                    "(objectClass=*)",
                    "rootDomainNamingContext");

            return reader.readEntry()
                    .getAttribute("rootDomainNamingContext")
                    .firstValueAsString();
        }

        @Override
        String getUserNameAttribute() {
            return "sAMAccountName";
        }
    }

    private static class EmbeddedGuesser extends ServerGuesser {
        private EmbeddedGuesser(Connection connection) {
            super(connection);
        }

        @Override
        String getBaseContext() throws Exception {
            return null;
        }

        @Override
        String getUserNameAttribute() {
            return "uid";
        }

        // TODO RAP 07 Dec 16: Will more likely execute queries for these values and remove
        // these custom overrides
        @Override
        List<String> getUserBaseChoices() {
            return Collections.singletonList("ou=users,dc=example,dc=com");
        }

        @Override
        List<String> getGroupBaseChoices() {
            return Collections.singletonList("ou=groups,dc=example,dc=com");
        }
    }
}
