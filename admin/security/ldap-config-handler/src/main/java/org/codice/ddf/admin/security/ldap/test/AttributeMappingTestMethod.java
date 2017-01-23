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

package org.codice.ddf.admin.security.ldap.test;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;

import java.util.Map;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableMap;

public class AttributeMappingTestMethod extends TestMethod<LdapConfiguration> {

    public static final String LDAP_ATTRIBUTE_MAPPING_TEST_ID = "attribute-mapping";

    public static final String DESCRIPTION = "Verifies that mapping values are valid and exist.";

    public static final String VALIDATED = "validated";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(VALIDATED,
            "Attribute mapping was successfully validated.");

    public AttributeMappingTestMethod() {
        super(LDAP_ATTRIBUTE_MAPPING_TEST_ID, DESCRIPTION, null, null, SUCCESS_TYPES, null, null);
    }

    @Override
    public Report test(LdapConfiguration configuration) {
        // TODO: tbatie - 12/15/16 - Make sure the attributes are in the schema, if they aren't report error. Give a warning there are no users in group or base user dn with the given attributes
        return new Report(new ConfigurationMessage(SUCCESS, VALIDATED, SUCCESS_TYPES.get(VALIDATED)));
    }

}
