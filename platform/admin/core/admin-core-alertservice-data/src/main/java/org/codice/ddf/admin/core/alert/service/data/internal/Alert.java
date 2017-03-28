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
package org.codice.ddf.admin.core.alert.service.data.internal;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

/**
 * An {@link Alert} holds the content of a system administrator alert which includes a level, a
 * primary alert message, and, possibly, some secondary alert messages.
 */
public class Alert {

    public static final String ALERT_EVENT_TOPIC_VALUE = "ddf/alertservice";

    public static final String ALERT_EVENT_ALERT_PROPERTY = "alert";

    public enum Level {
        INFO, WARN, ERROR
    }

    private String key;

    private Level level;

    private String title;

    private List<String> details;

    /**
     * Constructs an {@link Alert} with a {@param key}
     *
     * @param key     non-empty key of the {@link Alert} which may be used to manage the {@link Alert} in a collection
     * @param level   level of the {@link Alert} which signifies the severity of the primary message
     * @param title   not empty primary message of the {@link Alert}
     * @param details (may be empty) secondary messages of the {@link Alert}
     */
    public Alert(@Size(min = 1) String key, Level level, @Size(min = 1) String title,
            List<String> details) {
        this(level, title, details);
        setKey(key);
    }

    /**
     * Constructs an {@link Alert} without specifying a key
     *
     * @param level   level of the {@link Alert} which signifies the severity of the primary message
     * @param title   not empty primary message of the {@link Alert}
     * @param details (may be empty) secondary messages of the {@link Alert}
     */
    public Alert(Level level, @Size(min = 1) String title, List<String> details) {
        setLevel(level);
        setTitle(title);
        setDetails(details);
    }

    /**
     * This may return null if the {@link #Alert(String, Level, String, List)} method was used to
     * construct the {@link Alert}. If the returned key is not null, it should not be blank.
     *
     * @return the nullable key of the {@link Alert} which may be used to manage the {@link Alert} in a collection
     */
    @Nullable
    public String getKey() {
        return this.key;
    }

    /**
     * Sets the key of the {@link Alert}
     *
     * @param key non-empty key of the {@link Alert} which may be used to manage the {@link Alert} in a collection
     */
    public void setKey(@Size(min = 1) String key) {
        notEmpty(key, "key may not be empty");
        this.key = key;
    }

    /**
     * @return the level of the {@link Alert} which signifies the severity of the primary message
     */
    public Level getLevel() {
        return this.level;
    }

    /**
     * Sets the level of the {@link Alert} which signifies the severity of the primary message
     *
     * @param level level of the {@link Alert} which signifies the severity of the primary message
     */
    private void setLevel(Level level) {
        notNull(level, "level may not be null");
        this.level = level;
    }

    /**
     * @return the not empty primary message of the {@link Alert}
     */
    @Size(min = 1)
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the primary message of the {@link Alert}
     *
     * @param title not empty primary message of the {@link Alert}
     */
    private void setTitle(@Size(min = 1) String title) {
        notEmpty(title, "title may not be empty");
        this.title = title;
    }

    /**
     * @return (may be empty) secondary messages of the {@link Alert}
     */
    public List<String> getDetails() {
        return this.details;
    }

    /**
     * Sets the details of the {@link Alert}
     *
     * @param details (may be empty) secondary messages of the {@link Alert}
     */
    private void setDetails(List<String> details) {
        notNull(details, "details may not be null");
        this.details = details;
    }
}