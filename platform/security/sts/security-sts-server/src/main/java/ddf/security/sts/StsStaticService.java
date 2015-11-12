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
package ddf.security.sts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.cxf.sts.service.StaticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.SecurityConstants;

/**
 * This class was created as a workaround for the cxf issue
 * https://issues.apache.org/jira/browse/CXF-6673
 * Remove this class and use StaticService once the issue has been addressed
 */
public class StsStaticService extends StaticService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    /**
     * a collection of compiled regular expression patterns
     */
    private final Collection<Pattern> endpointPatterns = new ArrayList<Pattern>();

    /**
     * Return true if the supplied address corresponds to a known address for this service
     */
    public boolean isAddressInEndpoints(String address) {
        String addressToMatch = address;
        if (addressToMatch == null) {
            addressToMatch = "";
        }
        for (Pattern endpointPattern : endpointPatterns) {
            final Matcher matcher = endpointPattern.matcher(addressToMatch);
            if (matcher.matches()) {
                LOGGER.trace("Address {} matches with pattern {}", address, endpointPattern);
                return true;
            }
        }
        return false;
    }

    /**
     * Set the list of endpoint addresses that correspond to this service
     */
    public void setEndpoints(List<String> endpoints) {
        if (endpoints != null && endpoints.size() > 0) {
            endpointPatterns.clear();
            for (String endpoint : endpoints) {
                try {
                    endpointPatterns.add(Pattern.compile(endpoint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOGGER.error(ex.getMessage());
                    throw ex;
                }
            }
        }
    }
}
