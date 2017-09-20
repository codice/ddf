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
package org.codice.ddf.catalog.plugin.metacard.util;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used for parsing key-value pairs in the format of <i>'key = value'</i>. Uses a regex
 * under the hood. In plain English, the rule states that any {@link String} is a valid key-value
 * pair if, for key {@code K} and for value {@code V}, the representation is {@code K=V} where the
 * alphabet for {@code K} and {@code V} is <i>any</i> symbol except {@code =}.
 *
 * <p>Note that neither {@code K} or {@code V} can be empty. Any whitespace leading or tailing the
 * key and value after parsing is removed. Whitespace <i>within</i> the key or value is preserved,
 * but a key or value cannot consist entirely of whitespace.
 */
public class KeyValueParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeyValueParser.class);

  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("[^=]+[=][^=]+");

  private static final String SPLIT_REGEX = "=";

  private static final int SPLIT_LIMIT = 2;

  private final boolean failFast;

  /** Setup a default instance of {@link KeyValueParser}. */
  public KeyValueParser() {
    this.failFast = false;
  }

  /**
   * Sets up an instance of {@link KeyValueParser} that throws an {@link IllegalArgumentException}
   * on the first failure during batch operations.
   *
   * @param failFast Parsing failures throw exceptions if true; otherwise they are silently ignored.
   */
  public KeyValueParser(final boolean failFast) {
    this.failFast = failFast;
  }

  /**
   * Given a string, returns true if it is a valid key-value pair. This method is useful for
   * external policy validation.
   *
   * @param pair The string suspected to be of the form "key=value"
   * @return {@code True} if the input is a valid key-value pair. False otherwise.
   */
  public boolean validatePair(final String pair) {
    notNull(pair);
    return KEY_VALUE_PATTERN.matcher(pair.trim()).matches();
  }

  /**
   * Transforms a {@link List} of strings representing key-value pairs into a {@link Map} with only
   * the valid mappings.
   *
   * @param pairList The list of strings of the form "key=value".
   * @return A map of key -> value for only the validated key-value strings in the given list.
   */
  public Map<String, String> parsePairsToMap(final List<String> pairList) {
    notNull(pairList);
    Map<String, String> pairMap = new HashMap<>();
    for (String pair : pairList) {
      if (pair != null) {
        pair = pair.trim();
        if (KEY_VALUE_PATTERN.matcher(pair).matches()) {
          String[] pairSplit = pair.split(SPLIT_REGEX, SPLIT_LIMIT);
          pairMap.put(pairSplit[0].trim(), pairSplit[1].trim());
        } else if (failFast) {
          throw new IllegalArgumentException(
              format("Invalid key-value pair String encountered: %s", pair));
        } else {
          LOGGER.debug("Invalid key-value pair: \"{}\"", pair);
        }
      }
    }
    return pairMap;
  }
}
