/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.sts.claimsHandler;

import ddf.security.PropertiesLoader;
import ddf.security.SubjectUtils;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang.StringUtils;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logic that handles loading attribute maps from an incoming format and returning it as a Map. */
public class AttributeMapLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMapLoader.class);

  /**
   * Parses a file of attributes and returns them as a map.
   *
   * @param attributeMapFile File of the listed attributes
   * @return Map containing the fully populated attributes or empty map if file does not exist.
   */
  public Map<String, String> buildClaimsMapFile(String attributeMapFile) {
    Map<String, String> map =
        PropertiesLoader.toMap(PropertiesLoader.loadProperties(attributeMapFile));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(logLdapClaimsMap(map));
    }

    return map;
  }

  /**
   * Obtains the user name from the principal.
   *
   * @param principal Describing the current user that should be used for retrieving claims.
   * @return the user name if the principal has one, null if no name is specified or if principal is
   *     null.
   */
  public String getUser(Principal principal) {
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

  /**
   * Determines the base DN for the provided {@code Principal}.
   *
   * <p>If an X500 {@code Principal} is provided, its base DN is extracted; however, if it is empty
   * then then provided {@code defaultBaseDN} is used instead.
   *
   * <p>If any other type of {@code Principal} is provided, the {@code defaultBaseDN} is used.
   *
   * @param principal the Principal to check
   * @param defaultBaseDN the default DN to fall back to
   * @return the base DN
   */
  public String getBaseDN(Principal principal, String defaultBaseDN, boolean overrideCertDn) {
    String baseDN = null;
    if (principal instanceof X500Principal && !overrideCertDn) {
      Predicate<RDN> predicate = rdn -> !rdn.getTypesAndValues()[0].getType().equals(BCStyle.CN);
      baseDN = SubjectUtils.filterDN((X500Principal) principal, predicate);
    }

    if (StringUtils.isEmpty(baseDN)) {
      baseDN = (defaultBaseDN == null) ? "" : defaultBaseDN;
    }

    return baseDN;
  }

  public String getCredentials(Principal principal) {
    String credential = null;
    if (principal instanceof X500Principal) {
      X500Principal x500p = (X500Principal) principal;
      credential = new String(x500p.getEncoded(), StandardCharsets.UTF_8);
    } else if (principal instanceof WSUsernameTokenPrincipalImpl) {
      credential = ((WSUsernameTokenPrincipalImpl) principal).getPassword();
    }

    return credential;
  }

  private String logLdapClaimsMap(Map<String, String> map) {
    StringBuilder builder = new StringBuilder();
    builder.append("LDAP claims map:\n");
    for (Map.Entry<String, String> claim : map.entrySet()) {
      builder
          .append("claim: ")
          .append(claim.getKey())
          .append("; ")
          .append("LDAP mapping: ")
          .append(claim.getValue())
          .append("\n");
    }

    return builder.toString();
  }
}
