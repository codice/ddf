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

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * </p>
 * <p>
 * An {@link AlertDetail} contains a single secondary message of an {@link Alert}. An {@link Alert} may contain
 * a list of {@link AlertDetail}s.
 */
public class AlertDetail {

    private String message;

    private Level level;

    public AlertDetail(@Nonnull Level level, @Nonnull @NotEmpty String message) {
        notNull(level, "level may not be null");
        notEmpty(message, "message may not be empty");
        this.level = level;
        this.message = message;
    }

    @Nonnull
    public Level getLevel() {
        return level;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public enum Level {
        INFO, WARN, ERROR
    }
}