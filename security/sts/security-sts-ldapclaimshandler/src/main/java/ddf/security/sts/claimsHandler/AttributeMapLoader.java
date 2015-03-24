/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.sts.claimsHandler;

import ddf.security.PropertiesLoader;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Logic that handles loading attribute maps from an incoming format and returning it as a Map.
 * 
 */
public class AttributeMapLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMapLoader.class);

    /**
     * Parses a file of attributes and returns them as a map.
     * 
     * @param attributeMapFile
     *            File of the listed attributes
     * @return Map containing the fully populated attributes or empty map if file does not exist.
     */
    public static Map<String, String> buildClaimsMapFile(String attributeMapFile) {
        Map<String, String> map = PropertiesLoader.toMap(PropertiesLoader
                .loadProperties(attributeMapFile));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(logLdapClaimsMap(map));
        }

        return map;
    }

    /**
     * Obtains the user name from the principal.
     * 
     * @param principal
     *            Describing the current user that should be used for retrieving claims.
     * @return the user name if the principal has one, null if no name is specified or if principal
     *         is null.
     */
    public static String getUser(Principal principal) {
        String user = null;
        if (principal instanceof KerberosPrincipal) {
            KerberosPrincipal kp = (KerberosPrincipal) principal;
            StringTokenizer st = new StringTokenizer(kp.getName(), "@");
            st = new StringTokenizer(st.nextToken(), "/");
            user = st.nextToken();
        } else if (principal instanceof X500Principal) {
            X500Principal x500p = (X500Principal) principal;
            StringTokenizer st = new StringTokenizer(x500p.getName(), ",");
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
                    user = strArr[1];
                    break;
                }
            }
        } else if (principal != null) {
            user = principal.getName();
        }

        return user;
    }

    public static String getCredentials(Principal principal) {
        String credential = null;
        if (principal instanceof X500Principal) {
            X500Principal x500p = (X500Principal) principal;
            credential = new String(x500p.getEncoded());
        } else if (principal instanceof WSUsernameTokenPrincipalImpl) {
            credential = ((WSUsernameTokenPrincipalImpl) principal).getPassword();
        }

        return credential;
    }

    private static String logLdapClaimsMap(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        builder.append("LDAP claims map:\n");
        for (Map.Entry<String, String> claim : map.entrySet()) {
            builder.append("claim: ").append(claim.getKey()).append("; ").append("LDAP mapping: ").append(claim.getValue()).append("\n");
        }

        return builder.toString();
    }

}
