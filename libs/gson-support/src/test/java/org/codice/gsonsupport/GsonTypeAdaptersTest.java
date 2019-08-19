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
package org.codice.gsonsupport;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

public class GsonTypeAdaptersTest {

  private Gson gson;

  class GsonTypeAdaptersTestPojo {
    Date currentDate;
  }

  @Before
  public void setup() {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateLongFormatTypeAdapter())
            .create();
  }

  @Test
  public void testReadingValidLongDate() {
    String jsonString = "{\"currentDate\": 1546326000000}";
    GsonTypeAdaptersTestPojo parsedDate = gson.fromJson(jsonString, GsonTypeAdaptersTestPojo.class);

    Instant parsedInstant = parsedDate.currentDate.toInstant().atOffset(ZoneOffset.UTC).toInstant();

    assertThat(parsedInstant.toEpochMilli(), equalTo(1546326000000L));
  }

  @Test
  public void testReadingEmptyLongDate() {
    String emptyDateString = "{}";
    GsonTypeAdaptersTestPojo parsedDate =
        gson.fromJson(emptyDateString, GsonTypeAdaptersTestPojo.class);

    assertThat(parsedDate.currentDate, nullValue());
  }

  @Test
  public void testWritingValidLongDate() {
    GsonTypeAdaptersTestPojo testPojo = new GsonTypeAdaptersTestPojo();
    testPojo.currentDate = Date.from(LocalDateTime.of(2019, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));

    String jsonTestPojo = gson.toJson(testPojo);

    assertThat(jsonTestPojo, equalTo("{\"currentDate\":1546344000000}"));
  }

  @Test
  public void testWritingNullLongDate() {
    GsonTypeAdaptersTestPojo testPojo = new GsonTypeAdaptersTestPojo();

    String jsonTestPojo = gson.toJson(testPojo);

    assertThat(jsonTestPojo, equalTo("{}"));
  }
}
