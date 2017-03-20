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
package org.codice.ddf.admin.core.alert.service.api.internal;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * </p>
 * <p>
 * <b> This API will be modified and built upon as the need for Admin UI notifications evolves.
 * See <a href="https://codice.atlassian.net/wiki/display/DDF/Design+Admin+UI+Notifications">Design Admin UI Notifications</a>.</b>
 * </p>
 * <p>
 * An {@link Alert} holds the content for a system administrator alert.
 */
public class Alert {

    private Type type;

    private String title;

    private List<AlertDetail> details;

    private Optional<String> key;

    public Alert(@Nonnull Type type, @Nonnull @NotEmpty String title, @Nonnull List<AlertDetail> details, @Nullable String key) {
        notNull(type, "type may not be null");
        notEmpty(title, "title may not be empty");
        notNull(details, "details may not be null");
        if (key != null && key.equals("")) {
            throw new IllegalArgumentException("alert key may not be an empty string");
        }
        this.type = type;
        this.title = title;
        this.details = details;
        this.key = Optional.ofNullable(key);
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    @Nonnull
    public List<AlertDetail> getDetails() {
        return details;
    }

    @Nonnull
    public Optional<String> getKey() {
        return key;
    }

    public enum Type {
        INFO, WARNING, DANGER
    }
}