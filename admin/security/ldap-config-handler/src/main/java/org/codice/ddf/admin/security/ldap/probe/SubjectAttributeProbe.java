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

import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BASE_GROUP_DN;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.LDAP_TYPE;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.MEMBERSHIP_ATTRIBUTE;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.security.ldap.ServerGuesser;
import org.codice.ddf.admin.security.ldap.test.LdapTestingCommons;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectAttributeProbe extends ProbeMethod<LdapConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectAttributeProbe.class);

    private static final String DESCRIPTION =
            "Searches for the subject attributes for claims mapping.";

    private static final Map<String, String> REQUIRED_FIELDS = LdapConfiguration.buildFieldMap(
            LDAP_TYPE,
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD,
            BASE_GROUP_DN,
            MEMBERSHIP_ATTRIBUTE);

    private static final Map<String, String> OPTIONAL_FIELDS = LdapConfiguration.buildFieldMap(
            BIND_REALM,
            BIND_KDC);

    private static final String SUBJECT_CLAIMS_ID = "subjectClaims";

    private static final String LDAP_USER_ATTRIBUTES = "ldapUserAttributes";

    public SubjectAttributeProbe() {
        super("subjectAttributeMap",
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                null,
                null,
                null);
    }

    @Override
    public ProbeReport probe(LdapConfiguration configuration) {
        // TODO: tbatie - 12/7/16 - Need to also return a default map is embedded ldap and set
        Object subjectClaims = new Configurator().getConfig("ddf.security.sts.client.configuration")
                .get("claims");

        // TODO: tbatie - 12/6/16 - Clean up this naming conventions
        LdapTestingCommons.LdapConnectionAttempt ldapConnectionAttempt = bindUserToLdapConnection(
                configuration);
        Set<String> ldapEntryAttributes = null;
        try {
            ServerGuesser serverGuesser = ServerGuesser.buildGuesser(configuration.ldapType(),
                    ldapConnectionAttempt.connection());
            ldapEntryAttributes =
                    serverGuesser.getClaimAttributeOptions(configuration.baseGroupDn(),
                            configuration.membershipAttribute());
        } catch (SearchResultReferenceIOException | LdapException e) {
            LOGGER.warn("Error retrieving attributes from LDAP server; this may indicate a "
                            + "configuration issue with baseGroupDN {} or membershipAttribute {}",
                    configuration.baseGroupDn(),
                    configuration.membershipAttribute());
        }

        return new ProbeReport(new ArrayList<>()).addProbeResult(SUBJECT_CLAIMS_ID, subjectClaims)
                .addProbeResult(LDAP_USER_ATTRIBUTES, ldapEntryAttributes);
    }
}
