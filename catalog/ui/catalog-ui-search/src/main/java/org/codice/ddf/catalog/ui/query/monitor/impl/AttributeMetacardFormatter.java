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
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

import ddf.catalog.data.Attribute;

/**
 * Replace tags with values from the metacard. For example {@code %[attribute=id]} will be replaced
 * with the metacard identifier. This formatter will continue to loop over the template until all
 * tags are replaced. This allows a tag that gets a another template. For example {@code %[attribute=subjectLine]}
 * could return {@code %[attribute=title] (id: %[attribute=id])}, which will then get expanded. If the
 * metacard attribute returns multiple values, then the values will be separated with a comma. If
 * the metacard does not return a value, then a default value (specified in ctor) will be used.
 * The maximum number of iterations is limited to prevent infinite loops.
 */
public class AttributeMetacardFormatter extends BaseMetacardFormatter {

    private static final String PREFIX = "%[attribute=";

    private static final String SUFFIX = "]";

    private static final String REGEX = ".*" + Pattern.quote(PREFIX) + "([^]]+)" + Pattern.quote(
            SUFFIX) + ".*";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private static final int CAPTURE_GROUP = 1;

    private static final String LIST_SEPARATOR = ", ";

    private static final int MAX_ITERATIONS = 100;

    private final String defaultReplacement;

    /**
     * @param defaultReplacement must be non-null
     */
    public AttributeMetacardFormatter(String defaultReplacement) {
        notNull(defaultReplacement, "defaultReplacement must be non-null");
        this.defaultReplacement = defaultReplacement;
    }

    @Override
    public String toString() {
        return "AttributeMetacardFormatter{" +
                "defaultReplacement='" + defaultReplacement + '\'' +
                '}';
    }

    @Override
    protected String doFormat(String template, WorkspaceMetacardImpl workspaceMetacard,
            Long hitCount) {

        int iterationCount = 0;

        String tmp = template;

        Matcher m = PATTERN.matcher(tmp);
        while (m.matches() && (++iterationCount < MAX_ITERATIONS)) {
            String attributeName = m.group(CAPTURE_GROUP);

            String replacement = defaultReplacement;
            Attribute attribute = workspaceMetacard.getAttribute(attributeName);
            if (attribute != null) {
                List<Serializable> serializables = attribute.getValues();
                if (serializables != null) {
                    replacement = StringUtils.join(serializables, LIST_SEPARATOR);
                }
            }

            tmp = tmp.replace(PREFIX + attributeName + SUFFIX, replacement);

            m = PATTERN.matcher(tmp);

        }

        return tmp;
    }
}
