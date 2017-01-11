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
package org.codice.ddf.admin.security.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.ObjectClassType;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public abstract class ServerGuesser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerGuesser.class);

    private static final Map<String, Function<Connection, ServerGuesser>> GUESSER_LOOKUP =
            ImmutableMap.of("activeDirectory",
                    ServerGuesser.ADGuesser::new,
                    "embeddedLdap",
                    ServerGuesser.EmbeddedGuesser::new,
                    "openLdap",
                    ServerGuesser.OpenLdapGuesser::new,
                    "openDj",
                    ServerGuesser.OpenDjGuesser::new);

    protected final Connection connection;

    private ServerGuesser(Connection connection) {
        this.connection = connection;
    }

    public static ServerGuesser buildGuesser(String ldapType, Connection connection) {
        return Optional.ofNullable(GUESSER_LOOKUP.get(ldapType))
                .orElse(DefaultGuesser::new)
                .apply(connection);
    }

    public List<String> getBaseContexts() {
        try {
            ConnectionEntryReader reader = connection.search("",
                    SearchScope.BASE_OBJECT,
                    "(objectClass=*)",
                    "namingContexts");

            ArrayList<String> contexts = new ArrayList<>();
            while (reader.hasNext()) {
                contexts.add(reader.readEntry()
                        .getAttribute("namingContexts")
                        .firstValueAsString());
            }

            if (contexts.isEmpty()) {
                contexts.add("");
            }
            return contexts;
        } catch (LdapException | SearchResultReferenceIOException e) {
            LOGGER.debug("Error getting baseContext", e);
            return Collections.singletonList("");
        }
    }

    public List<String> getUserNameAttribute() {
        return ImmutableList.of("uid");
    }

    public List<String> getGroupObjectClass() {
        return ImmutableList.of("groupOfNames", "group");
    }

    public List<String> getMembershipAttribute() {
        return ImmutableList.of("member", "uniqueMember");
    }

    public List<String> getUserBaseChoices() {
        return getChoices("(|(ou=user*)(name=user*)(cn=user*))");
    }

    public List<String> getGroupBaseChoices() {
        return getChoices("(|(ou=group*)(name=group*)(cn=group*)(objectClass=groupOfUniqueNames))");
    }

    public Set<String> getClaimAttributeOptions(String baseGroupDn, String membershipAttribute)
            throws SearchResultReferenceIOException, LdapException {

        // Using the base group DN and membership attributes to constrain search,
        // find one user's DN
        ConnectionEntryReader reader = connection.search(baseGroupDn,
                SearchScope.WHOLE_SUBTREE,
                String.format("%s=*", membershipAttribute),
                membershipAttribute);
        SearchResultEntry searchResultEntry = reader.readEntry();
        String tokenUserDn = searchResultEntry.getAttribute(membershipAttribute)
                .firstValueAsString();

        // Find the names of all the objectClasses the directory entry of that user has
        Set<String> objectClassNames = connection.readEntry(DN.valueOf(tokenUserDn))
                .getAttribute("objectClass")
                .stream()
                .map(ByteString::toString)
                .collect(Collectors.toSet());

        // Read the schema and filter for structural objectClasses that are in the set associated
        // with the above user
        Schema schema = Schema.readSchemaForEntry(connection, DN.valueOf(tokenUserDn));
        Set<ObjectClass> objectClasses = schema.getObjectClasses()
                .stream()
                .filter(oc -> oc.getObjectClassType() == ObjectClassType.STRUCTURAL)
                .filter(oc -> objectClassNames.contains(oc.getNameOrOID()))
                .collect(Collectors.toSet());

        // Return the required and optional eattributes names from the full set of objectClasses
        return objectClasses.stream()
                .flatMap(oc -> Sets.union(oc.getDeclaredRequiredAttributes(),
                        oc.getDeclaredOptionalAttributes())
                        .stream())
                .map(AttributeType::getNameOrOID)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<String> getChoices(String query) {
        List<String> baseContexts = getBaseContexts();

        List<String> choices = new ArrayList<>();
        for (String baseContext : baseContexts) {
            try (ConnectionEntryReader reader = connection.search(baseContext,
                    SearchScope.WHOLE_SUBTREE,
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
        }

        return choices;
    }

    private static class DefaultGuesser extends ServerGuesser {
        private DefaultGuesser(Connection connection) {
            super(connection);
        }
    }

    private static class ADGuesser extends ServerGuesser {
        private static final Predicate<String> USER_DN_EXC = Pattern.compile(
                ".*(,|^)cn=system(,|$).*|.*(,|^)cn=builtin(,|$).*",
                Pattern.CASE_INSENSITIVE)
                .asPredicate()
                .negate();

        private static final Predicate<String> GROUP_DN_EXC = Pattern.compile(
                ".*(,|^)cn=group policy creator owners(,|$).*",
                Pattern.CASE_INSENSITIVE)
                .asPredicate()
                .negate();

        private ADGuesser(Connection connection) {
            super(connection);
        }

        @Override
        public List<String> getBaseContexts() {
            try {
                ConnectionEntryReader reader = connection.search("",
                        SearchScope.BASE_OBJECT,
                        "(objectClass=*)",
                        "rootDomainNamingContext");

                if (reader.hasNext()) {
                    return Collections.singletonList(reader.readEntry()
                            .getAttribute("rootDomainNamingContext")
                            .firstValueAsString());
                } else {
                    return Collections.singletonList("");
                }
            } catch (LdapException | SearchResultReferenceIOException e) {
                LOGGER.debug("Error getting baseContext", e);
                return Collections.singletonList("");
            }
        }

        @Override
        public List<String> getUserNameAttribute() {
            return Collections.singletonList("sAMAccountName");
        }

        @Override
        public List<String> getGroupObjectClass() {
            return Collections.singletonList("group");
        }

        @Override
        public List<String> getMembershipAttribute() {
            return Collections.singletonList("member");
        }

        @Override
        public List<String> getUserBaseChoices() {
            return super.getUserBaseChoices()
                    .stream()
                    .filter(USER_DN_EXC)
                    .collect(Collectors.toList());
        }

        @Override
        public List<String> getGroupBaseChoices() {
            return super.getGroupBaseChoices()
                    .stream()
                    .filter(GROUP_DN_EXC)
                    .collect(Collectors.toList());
        }
    }

    private static class EmbeddedGuesser extends ServerGuesser {
        private EmbeddedGuesser(Connection connection) {
            super(connection);
        }

        // TODO RAP 07 Dec 16: Will more likely execute queries for these values and remove
        // these custom overrides
        @Override
        public List<String> getUserBaseChoices() {
            return Collections.singletonList("ou=users,dc=example,dc=com");
        }

        @Override
        public List<String> getGroupBaseChoices() {
            return Collections.singletonList("ou=groups,dc=example,dc=com");
        }

        @Override
        public List<String> getGroupObjectClass() {
            return Collections.singletonList("groupOfNames");
        }

        @Override
        public List<String> getMembershipAttribute() {
            return Collections.singletonList("member");
        }
    }

    private static class OpenLdapGuesser extends ServerGuesser {
        private OpenLdapGuesser(Connection connection) {
            super(connection);
        }
    }

    private static class OpenDjGuesser extends ServerGuesser {
        private OpenDjGuesser(Connection connection) {
            super(connection);
        }
    }
}
