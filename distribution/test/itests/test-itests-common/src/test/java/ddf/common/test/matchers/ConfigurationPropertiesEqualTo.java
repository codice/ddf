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
package ddf.common.test.matchers;

import java.util.Dictionary;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Hamcrest {@link BaseMatcher} used to see if a {@link org.osgi.service.cm.Configuration} object's
 * properties match the ones provided.
 */
public class ConfigurationPropertiesEqualTo extends BaseMatcher<Dictionary<String, Object>> {

    protected Dictionary<String, Object> expectedProperties;

    /**
     * Constructor.
     *
     * @param expectedProperties properties required for this matcher to return {@code true}
     */
    public ConfigurationPropertiesEqualTo(Dictionary<String, Object> expectedProperties) {
        this.expectedProperties = expectedProperties;
    }

    /**
     * Utility method used to create an instance of this matcher.
     *
     * @param expectedProperties properties required for this matcher to return {@code true}
     * @return new instance of this class
     */
    public static ConfigurationPropertiesEqualTo equalToConfigurationProperties(
            Dictionary<String, Object> expectedProperties) {
        return new ConfigurationPropertiesEqualTo(expectedProperties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object object) {
        return (object instanceof Dictionary) && new ConfigurationPropertiesComparator()
                .equal((Dictionary<String, Object>) object, expectedProperties);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expectedProperties.toString());
    }
}
