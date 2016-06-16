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
package ddf.common.test.cometd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.jayway.restassured.path.json.JsonPath;

public class CometDMessageValidator {

    private static final String TITLE_PATH = "data.title";

    private static final String MESSAGE_PATH = "data.message";

    private static final String STATUS_PATH = "data.status";

    public static void verifyActivity(JsonPath json, String title, String message, String status) {
        verifyJson(json, TITLE_PATH, title);
        verifyJson(json, MESSAGE_PATH, message);
        verifyJson(json, STATUS_PATH, status);
    }

    public static void verifyNotification(JsonPath json, String title, String message,
            String status) {
        verifyJson(json, TITLE_PATH, title);
        verifyJson(json, MESSAGE_PATH, message);
        verifyJson(json, STATUS_PATH, status);
    }

    private static void verifyJson(JsonPath json, String attributePath, String expectedValue) {
        String value = json.getString(attributePath);
        assertThat(value, is(expectedValue));
    }
}
