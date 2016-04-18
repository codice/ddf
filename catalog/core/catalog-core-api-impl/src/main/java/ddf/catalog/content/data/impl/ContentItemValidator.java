/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.content.data.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ddf.catalog.content.data.ContentItem;

/**
 * Validates {@link ContentItem}s to ensure they adhere to the URI & file-naming conventions
 * required by the {@link ddf.catalog.content.StorageProvider}.
 */
public class ContentItemValidator {

    private static final Pattern QUALIFIER_PATTERN = Pattern.compile("\\w[a-zA-Z0-9_\\-]+");

    private ContentItemValidator() {
    }

    public static void validate(ContentItem item) throws IllegalArgumentException {
        if (StringUtils.isNotBlank(item.getQualifier())) {
            validateInput(item.getQualifier(), QUALIFIER_PATTERN);
        }

        if (StringUtils.isNotBlank(item.getUri())) {
            try {
                new URI(item.getUri());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to create content URI.", e);
            }
        } else {
            throw new IllegalArgumentException("Cannot have an empty content URI.");
        }

    }

    private static void validateInput(final String input, Pattern pattern) {
        if (!pattern.matcher(input)
                .matches()) {
            throw new IllegalArgumentException(
                    "Illegal characters found while validating [" + input +
                            "]. Allowable values must match: " + pattern.pattern());
        }
    }
}
