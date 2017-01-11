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
package org.codice.ddf.admin.security.ldap.probe;

import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.LDAP_TYPE;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.QUERY;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.QUERY_BASE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapQueryResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

public class LdapQueryProbe extends ProbeMethod<LdapConfiguration> {
    private static final String DESCRIPTION =
            "Probe to execute arbitrary query against an LDAP server and return the results.";

    private static final Map<String, String> REQUIRED_FIELDS = LdapConfiguration.buildFieldMap(
            LDAP_TYPE,
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD,
            QUERY,
            QUERY_BASE);

    private static final Map<String, String> OPTIONAL_FIELDS = LdapConfiguration.buildFieldMap(
            BIND_REALM,
            BIND_KDC);

    public LdapQueryProbe() {
        super("ldapQuery", DESCRIPTION, REQUIRED_FIELDS, OPTIONAL_FIELDS, null, null, null);
    }

    @Override
    public ProbeReport probe(LdapConfiguration configuration) {
        List<ConfigurationMessage> checkMessages =
                configuration.checkRequiredFields(REQUIRED_FIELDS.keySet());

        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new ProbeReport(checkMessages);
        }

        checkMessages = configuration.testConditionalBindFields();
        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new ProbeReport(checkMessages);
        }

        Connection connection = bindUserToLdapConnection(configuration).connection();
        List<SearchResultEntry> searchResults = getLdapQueryResults(connection,
                configuration.query(),
                configuration.queryBase());
        List<Map<String, String>> convertedSearchResults = new ArrayList<>();

        for (SearchResultEntry entry : searchResults) {
            Map<String, String> entryMap = new HashMap<>();
            for (Attribute attri : entry.getAllAttributes()) {
                entryMap.put("name",
                        entry.getName()
                                .toString());
                entryMap.put(attri.getAttributeDescriptionAsString(), attri.firstValueAsString());
            }
            convertedSearchResults.add(entryMap);
        }

        return new ProbeReport(new ArrayList<>()).addProbeResult("ldapQueryResults",
                convertedSearchResults);
    }
}
