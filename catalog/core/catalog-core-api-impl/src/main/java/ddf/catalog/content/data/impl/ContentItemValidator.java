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
package ddf.catalog.content.data.impl;

import ddf.catalog.content.data.ContentItem;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * Validates {@link ContentItem}s to ensure they adhere to the URI & file-naming conventions
 * required by the {@link ddf.catalog.content.StorageProvider}.
 */
public class ContentItemValidator {
  private static final Pattern QUALIFIER_PATTERN = Pattern.compile("\\w[a-zA-Z0-9_\\-]+");

  // https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29
  // xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
  //    8      4     3    3       12
  private static final String X = "[0-9a-fA-F]"; // a hex value

  private static final String Y = "[89abAB]"; // second reserved bits

  private static final String UUID_V4 =
      "x{8}-?x{4}-?4x{3}-?yx{3}-?x{12}"
          .replace("x", X)
          .replace("y", Y); // Note that the dashes in the UUID are optional.

  private static final String CONTENT_REGEX =
      "content:uuid(#qualifier)?"
          .replace("uuid", UUID_V4)
          .replace("qualifier", QUALIFIER_PATTERN.pattern()); // Note the qualifier is optional

  private static final Pattern CONTENT_PATTERN = Pattern.compile(CONTENT_REGEX);

  private ContentItemValidator() {}

  public static boolean validate(ContentItem item) {
    if (item == null || StringUtils.isBlank(item.getUri())) {
      return false;
    }

    if (StringUtils.isNotBlank(item.getQualifier())) {
      boolean qualifierValid = validateInput(item.getQualifier(), QUALIFIER_PATTERN);
      if (!qualifierValid) {
        return false;
      }
    }

    if (CONTENT_PATTERN.matcher(item.getUri()).matches()) {
      return true;
    }

    return false;
  }

  private static boolean validateInput(final String input, Pattern pattern) {
    if (pattern.matcher(input).matches()) {
      return true;
    }

    return false;
  }
}
