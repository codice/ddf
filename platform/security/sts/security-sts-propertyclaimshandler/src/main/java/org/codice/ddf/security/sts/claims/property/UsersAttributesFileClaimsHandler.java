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
package org.codice.ddf.security.sts.claims.property;

import static org.apache.commons.lang3.Validate.notBlank;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.SystemHighAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersAttributesFileClaimsHandler implements ClaimsHandler, SystemHighAttributes {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UsersAttributesFileClaimsHandler.class);

  private static final Path DDF_HOME_PATH = Paths.get(System.getProperty("ddf.home"));

  private String usersAttributesFileLocation;

  private volatile ImmutableSet<String> supportedClaimTypes;

  private volatile Map<String, Map<String, Set<String>>> json;

  private volatile Map<String, Set<String>> systemHighUserAttributes;

  /**
   * @throws IllegalStateException when the users attributes file cannot be read or when the
   *     contents do meet assumptions. See the documentation section "Updating System Users" for
   *     details about the contents of the users attribute file.
   */
  public void init() {
    Path path = Paths.get(usersAttributesFileLocation);
    if (!path.isAbsolute()) {
      path = DDF_HOME_PATH.resolve(path);
    }

    final Map<String, Map<String, Object>> usersAttributesFileContents;
    final Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
    try (final Reader reader = Files.newBufferedReader(path)) {
      usersAttributesFileContents = new Gson().fromJson(reader, type);
    } catch (NoSuchFileException e) {
      final String errorMessage = createErrorMessage("Cannot find file");
      LOGGER.error(errorMessage, e);
      throw new IllegalStateException(e);
    } catch (JsonIOException | IOException e) {
      final String errorMessage = createErrorMessage("Error reading file");
      LOGGER.error(errorMessage, e);
      throw new IllegalStateException(e);
    } catch (JsonSyntaxException e) {
      final String errorMessage =
          createErrorMessage("File does not contain expected the expected json format");
      LOGGER.error(errorMessage, e);
      throw new IllegalStateException(e);
    }

    final Map<String, Map<String, Set<String>>> newJson = new HashMap<>();
    for (Map.Entry<String, Map<String, Object>> userToAttributesMap :
        usersAttributesFileContents.entrySet()) {
      final Map<String, Set<String>> attributes = new HashMap<>();
      for (Map.Entry<String, Object> attributesToValuesMap :
          userToAttributesMap.getValue().entrySet()) {
        attributes.put(
            attributesToValuesMap.getKey(),
            convertToSetOfStrings(attributesToValuesMap.getValue()));
      }
      newJson.put(userToAttributesMap.getKey(), attributes);
    }
    json = newJson;

    setSupportedClaimTypes();
    setSystemHighUserAttributes();
  }

  @Override
  public List<String> getSupportedClaimTypes() {
    return supportedClaimTypes.asList();
  }

  @Override
  public ProcessedClaimCollection retrieveClaimValues(
      ClaimCollection claimCollection, ClaimsParameters claimsParameters) {
    ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
    Principal principal = claimsParameters.getPrincipal();
    if (principal == null) {
      return claimsColl;
    }

    String name;
    if (principal instanceof X500Principal) {
      name = SubjectUtils.getCommonName((X500Principal) principal);
    } else {
      name = principal.getName();
    }
    Map<String, Set<String>> userMap = json.get(name);
    if (userMap == null) {
      userMap = attemptToFindAMatchingRegexFormatUserEntry(principal, json);
    }
    if (userMap == null) {
      return claimsColl;
    }

    for (Claim claim : claimCollection) {
      Set<String> attributeValue = userMap.get(claim.getClaimType());
      ProcessedClaim c = new ProcessedClaim();
      c.setClaimType(claim.getClaimType());
      c.setPrincipal(principal);
      if (attributeValue != null) {
        attributeValue.forEach(c::addValue);
        claimsColl.add(c);
      }
    }
    return claimsColl;
  }

  /**
   * The `users.attributes` file can contain user entries in a regex format. See the documentation
   * section "Updating System Users" for more details.
   */
  @Nullable
  private static Map<String, Set<String>> attemptToFindAMatchingRegexFormatUserEntry(
      final Principal principal, final Map<String, Map<String, Set<String>>> json) {
    final Set<Map.Entry<String, Map<String, Set<String>>>> entries = json.entrySet();
    for (Map.Entry<String, Map<String, Set<String>>> entry : entries) {
      final String key = entry.getKey();
      final Pattern pattern = Pattern.compile(key);
      final Matcher matcher = pattern.matcher(principal.getName());
      if (matcher.matches()) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public Set<String> getValues(final String attributeName) {
    return Optional.ofNullable(systemHighUserAttributes.get(attributeName))
        .orElse(Collections.emptySet());
  }

  public void setUsersAttributesFileLocation(String usersAttributesFileLocation) {
    this.usersAttributesFileLocation =
        notBlank(
            usersAttributesFileLocation,
            "The location of the users attributes file may not be blank");
    init();
  }

  private void setSupportedClaimTypes() {
    final ImmutableSet.Builder<String> immutableSetBuilder = ImmutableSet.builder();

    for (final Map<String, Set<String>> user : json.values()) {
      for (final String attributeName : user.keySet()) {
        try {
          // Converting to URI despite String return type to maintain the URI validation
          immutableSetBuilder.add(new URI(attributeName).toString());
        } catch (URISyntaxException e) {
          final String reason =
              String.format("Unable to create URI from attributeName \"%s\"", attributeName);
          final String errorMessage = createErrorMessage(reason);
          LOGGER.error(errorMessage);
          throw new IllegalStateException(reason, e);
        }
      }
    }

    supportedClaimTypes = immutableSetBuilder.build();
  }

  private void setSystemHighUserAttributes() {
    final String systemHighUserName = SystemBaseUrl.INTERNAL.getHost(); // will never be null
    final Map<String, Set<String>> newSystemHighUserAttributes = json.get(systemHighUserName);
    if (null == newSystemHighUserAttributes) {
      final String reason =
          String.format(
              "Expected system high user \"%s\" to contain a map of attributes",
              systemHighUserName);
      final String errorMessage = createErrorMessage(reason);
      LOGGER.error(errorMessage);
      throw new IllegalStateException(reason);
    }

    this.systemHighUserAttributes = newSystemHighUserAttributes;
  }

  private Set<String> convertToSetOfStrings(final Object attributeValueObject) {
    if (attributeValueObject instanceof String) {
      final String singleValuedAttributeValue = (String) attributeValueObject;
      return Collections.singleton(singleValuedAttributeValue);
    } else if (attributeValueObject instanceof Collection) {
      final Collection<?> multiValuedAttributeValues = (Collection<?>) attributeValueObject;

      if (multiValuedAttributeValues.isEmpty()) {
        final String reason = "Expected attributes to not have a value of an empty Collection";
        final String errorMessage = createErrorMessage(reason);
        LOGGER.error(errorMessage);
        throw new IllegalStateException(reason);
      }

      ImmutableSet.Builder<String> immutableSetBuilder = ImmutableSet.builder();
      multiValuedAttributeValues.forEach(
          multiValuedAttributeValue -> {
            if (!(multiValuedAttributeValue instanceof String)) {
              final String reason =
                  "Expected the attribute values that are a Collection to only contain String values";
              final String errorMessage = createErrorMessage(reason);
              LOGGER.error(errorMessage);
              throw new IllegalStateException(reason);
            }

            immutableSetBuilder.add((String) multiValuedAttributeValue);
          });

      return immutableSetBuilder.build();
    } else {
      final String reason = "Expected attribute values to be a String or a Collection";
      final String errorMessage = createErrorMessage(reason);
      LOGGER.error(errorMessage);
      throw new IllegalStateException(reason);
    }
  }

  /** Appends information that can tell a system administrator how to recover */
  private String createErrorMessage(final String reason) {
    return String.format(
        "Unexpected error reading the users attributes file at location \"%s\": %s. Try restarting, or check that the users attributes file exists and that it meets all of the assumptions described in the Documentation.",
        usersAttributesFileLocation, reason);
  }
}
