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

import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BASE_GROUP_DN;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.LDAP_TYPE;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.MEMBERSHIP_ATTRIBUTE;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.services.PolicyManagerServiceProperties.STS_CLAIMS_CONFIGURATION_CONFIG_ID;
import static org.codice.ddf.admin.api.services.PolicyManagerServiceProperties.STS_CLAIMS_PROPS_KEY_CLAIMS;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;

import java.util.List;
import java.util.Set;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.security.ldap.ServerGuesser;
import org.codice.ddf.admin.security.ldap.test.LdapTestingCommons;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class SubjectAttributeProbe extends ProbeMethod<LdapConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectAttributeProbe.class);

    public static final String SUBJECT_ATTRIBUTES_PROBE_ID = "subject-attributes";
    private static final String DESCRIPTION =
            "Searches for the subject attributes for claims mapping.";

    private static final List<String> REQUIRED_FIELDS = ImmutableList.of(
            LDAP_TYPE,
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD,
            BASE_GROUP_DN,
            MEMBERSHIP_ATTRIBUTE);

    private static final List<String> OPTIONAL_FIELDS = ImmutableList.of(
            BIND_REALM,
            BIND_KDC);

    private static final String SUBJECT_CLAIMS_ID = "subjectClaims";

    private static final String USER_ATTRIBUTES = "userAttributes";

    public SubjectAttributeProbe() {
        super(SUBJECT_ATTRIBUTES_PROBE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                null,
                null,
                null);
    }

    @Override
    public ProbeReport probe(LdapConfiguration configuration) {
        ProbeReport probeReport = new ProbeReport(configuration.validate(REQUIRED_FIELDS));

        if (probeReport.containsFailureMessages()) {
            return probeReport;
        }

        Object subjectClaims = new Configurator().getConfig(STS_CLAIMS_CONFIGURATION_CONFIG_ID)
                .get(STS_CLAIMS_PROPS_KEY_CLAIMS);

        LdapTestingCommons.LdapConnectionAttempt ldapConnectionAttempt = bindUserToLdapConnection(
                configuration);
        Set<String> ldapEntryAttributes = null;
        try {
            // TODO: tbatie - 1/19/17 - Don't assume the connection is available, should check result first
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

        // TODO: tbatie - 1/19/17 - Need to return messages about the probe result and/or if something goes wrong
        return probeReport.probeResult(SUBJECT_CLAIMS_ID, subjectClaims)
                .probeResult(USER_ATTRIBUTES, ldapEntryAttributes);
    }
}
