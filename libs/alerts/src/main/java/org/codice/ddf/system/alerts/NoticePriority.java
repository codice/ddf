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
package org.codice.ddf.system.alerts;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum for the different notice/alert priority levels. The priority ranges from critical
 * (something important has failed) to low (simple notice that something occurred)
 */
public enum NoticePriority {
    CRITICAL(4), IMPORTANT(3), NORMAL(2), LOW(1);

    private int value;

    private static Map<Integer, NoticePriority> map = new HashMap<>();

    static {
        for (NoticePriority priority : NoticePriority.values()) {
            map.put(priority.value, priority);
        }
    }

    NoticePriority(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static NoticePriority valueOf(int priority) {
        return map.get(priority);
    }
}
