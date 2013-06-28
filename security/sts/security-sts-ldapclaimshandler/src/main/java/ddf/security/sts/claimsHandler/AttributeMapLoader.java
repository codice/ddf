/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.sts.claimsHandler;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.util.PropertiesLoader;


/**
 * Logic that handles loading attribute maps from an incoming format and
 * returning it as a Map.
 * 
 */
public class AttributeMapLoader
{

    private static final String ATTRIBUTE_DELIMITER = ", ";

    private static final String EQUALS_DELIMITER = "=";

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMapLoader.class);

    /**
     * Parses a string of attributes and returns them as a map.
     * 
     * @param attributesToMap String of attributes separated by a ,
     * @return Map containing the fully populated attribute map.
     */
    public static Map<String, String> buildClaimsMap( String attributesToMap )
    {
        // Remove first and last character since they are "[" and "]"
        String cleanedAttributesToMap = attributesToMap.substring(1, attributesToMap.length() - 1);
        String[] attributes = cleanedAttributesToMap.split(ATTRIBUTE_DELIMITER);
        Map<String, String> map = new HashMap<String, String>();
        for ( String attribute : attributes )
        {
            String[] attrSplit = attribute.split(EQUALS_DELIMITER);
            map.put(attrSplit[0], attrSplit[1]);
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(logLdapClaimsMap(map));
        }

        return map;
    }

    /**
     * Parses a file of attributes and returns them as a map.
     * 
     * @param attributeMapFile File of the listed attributes
     * @return Map containing the fully populated attributes.
     */
    public static Map<String, String> buildClaimsMapFile( String attributeMapFile )
    {
        Map<String, String> map = PropertiesLoader.toMap(PropertiesLoader.loadProperties(attributeMapFile));
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(logLdapClaimsMap(map));
        }
        
        return map;
    }

    private static String logLdapClaimsMap( Map<String, String> map )
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LDAP claims map:\n");
        for ( String claim : map.keySet() )
        {
            builder.append("claim: " + claim + "; " + "LDAP mapping: " + map.get(claim) + "\n");
        }

        return builder.toString();
    }
}
