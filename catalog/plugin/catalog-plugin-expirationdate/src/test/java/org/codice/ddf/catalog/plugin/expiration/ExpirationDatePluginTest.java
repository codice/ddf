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
package org.codice.ddf.catalog.plugin.expiration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExpirationDatePluginTest {

  private static final String DATE_FORMAT = "MM-dd-yyyy HH:mm:ss.SSS";

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormat.forPattern(DATE_FORMAT);

  private static final DateTime CREATED_DATE =
      DATE_TIME_FORMATTER.parseDateTime("09-01-1990 00:00:00.000");

  private static final DateTime ORIG_EXPIRATION_DATE =
      DATE_TIME_FORMATTER.parseDateTime("09-10-1990 00:00:00.000");

  private static final int DAYS = 10;

  private ExpirationDatePlugin expirationDatePlugin;

  @Mock private CreateRequest mockCreateRequest;

  @Before
  public void setup() {
    expirationDatePlugin = new ExpirationDatePlugin();
    expirationDatePlugin.setOffsetFromCreatedDate(DAYS);
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is not overwritten when the overwriteIfBlank option
   * is not selected, and remains unchanged.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpiration() throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 1;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsEmpty(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is overwritten with the newly calculated expiration
   * date based on the configurable offset in days, since the overwriteIfBlank option is selected.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpirationOverwriteIfBlank()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(true);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 1;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsNewOffset(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is not overwritten when the overwriteIfBlank option
   * is not selected, and remains unchanged. This test additionally sets the overwriteIfExists
   * option to verify it does not interfere with the overwriteIfBlank option.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpirationOverwriteIfExists()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(true);

    int size = 1;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsEmpty(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is overwritten with the newly calculated expiration
   * date based on the configurable offset in days, since the overwriteIfBlank option is selected.
   * This test additionally sets the overwriteIfExists option to verify it does not interfere with
   * the overwriteIfBlank option.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpirationOverwriteIfBlankOverwriteIfExists()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(true);
    expirationDatePlugin.setOverwriteIfExists(true);

    int size = 1;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsNewOffset(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is not overwritten when the
   * overwriteIfExists option is not selected, and remains unchanged.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpiration() throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 1;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsUnchanged(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is not overwritten when the
   * overwriteIfExists option is not selected, and remains unchanged. This test additionally sets
   * the overwriteIfBlank option to verify it does not interfere with the overwriteIfExists option.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpirationOverwriteIfBlank()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(true);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 1;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsUnchanged(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is overwritten with the newly
   * calculated expiration date based on the configurable offset in days, since the
   * overwriteIfExists option is selected.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpirationOverwriteIfExists()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(true);

    int size = 1;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsNewOffset(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is overwritten with the newly
   * calculated expiration date based on the configurable offset in days, since the
   * overwriteIfExists option is selected. This test additionally sets the overwriteIfBlank option
   * to verify it does not interfere with the overwriteIfExists option.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpirationOverwriteIfBlankOverwriteIfExists()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(true);
    expirationDatePlugin.setOverwriteIfExists(true);

    int size = 1;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    assertThatExpirationIsNewOffset(metacards.get(0));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is not overwritten when the overwriteIfBlank option
   * is not selected, and remains unchanged. This test uses multiple metacards for ingest
   * processing.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpirationMultipleMetacards()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 5;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    List<Metacard> metacards = mockCreateRequest.getMetacards();
    assertThat(metacards, hasSize(size));
    mockCreateRequest.getMetacards().stream().forEach(m -> assertThatExpirationIsEmpty(m));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the empty expiration date is overwritten with the newly calculated expiration
   * date based on the configurable offset in days, since the overwriteIfBlank option is selected.
   * This test uses multiple metacards for ingest processing.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testEmptyExpirationOverwriteIfBlankMultipleMetacards()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(true);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 5;
    when(mockCreateRequest.getMetacards())
        .thenReturn(createMockMetacardsWithNoExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    assertThat(mockCreateRequest.getMetacards(), hasSize(size));
    mockCreateRequest.getMetacards().stream().forEach(m -> assertThatExpirationIsNewOffset(m));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is not overwritten when the
   * overwriteIfExists option is not selected, and remains unchanged. This test uses multiple
   * metacards for ingest processing.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpirationMultipleMetacards()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(false);

    int size = 5;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    assertThat(mockCreateRequest.getMetacards(), hasSize(size));
    mockCreateRequest.getMetacards().stream().forEach(m -> assertThatExpirationIsUnchanged(m));
  }

  /**
   * Tests {@link ExpirationDatePlugin#process(CreateRequest)}
   *
   * <p>Verifies that the existing, non-empty expiration date is overwritten with the newly
   * calculated expiration date based on the configurable offset in days, since the
   * overwriteIfExists option is selected. This test uses multiple metacards for ingest processing.
   *
   * @throws PluginExecutionException
   * @throws StopProcessingException
   */
  @Test
  public void testExistingExpirationOverwriteIfExistsMultipleMetacards()
      throws PluginExecutionException, StopProcessingException {

    // Items configured via the admin console
    expirationDatePlugin.setOverwriteIfBlank(false);
    expirationDatePlugin.setOverwriteIfExists(true);

    int size = 5;
    when(mockCreateRequest.getMetacards()).thenReturn(createMockMetacardsWithExpirationDate(size));

    // Perform Test
    expirationDatePlugin.process(mockCreateRequest);

    // Verify
    assertThat(mockCreateRequest.getMetacards(), hasSize(size));
    mockCreateRequest.getMetacards().stream().forEach(m -> assertThatExpirationIsNewOffset(m));
  }

  private List<Metacard> createMockMetacardsWithNoExpirationDate(int number) {

    List<Metacard> mockMetacards = new ArrayList(number);

    for (int i = 0; i < number; i++) {
      Metacard mockMetacard = new MetacardImpl();
      Attribute id = new AttributeImpl(Metacard.ID, Integer.toString(i));
      mockMetacard.setAttribute(id);
      Attribute title = new AttributeImpl(Metacard.TITLE, Integer.toString(i));
      mockMetacard.setAttribute(title);
      Attribute createdDate = new AttributeImpl(Core.METACARD_CREATED, CREATED_DATE.toDate());
      mockMetacard.setAttribute(createdDate);
      mockMetacards.add(mockMetacard);
    }

    return mockMetacards;
  }

  private List<Metacard> createMockMetacardsWithExpirationDate(int number) {

    List<Metacard> mockMetacards = new ArrayList(number);

    for (int i = 0; i < number; i++) {
      Metacard mockMetacard = new MetacardImpl();
      Attribute id = new AttributeImpl(Metacard.ID, Integer.toString(i));
      mockMetacard.setAttribute(id);
      Attribute title = new AttributeImpl(Metacard.TITLE, Integer.toString(i));
      mockMetacard.setAttribute(title);
      Attribute createdDate = new AttributeImpl(Core.METACARD_CREATED, CREATED_DATE.toDate());
      mockMetacard.setAttribute(createdDate);
      Attribute expirationDate =
          new AttributeImpl(Metacard.EXPIRATION, ORIG_EXPIRATION_DATE.toDate());
      mockMetacard.setAttribute(expirationDate);
      mockMetacards.add(mockMetacard);
    }

    return mockMetacards;
  }

  private void assertThatExpirationIsEmpty(Metacard metacard) {
    assertThat(metacard, notNullValue());
    assertThat(metacard.getExpirationDate(), nullValue());
  }

  private void assertThatExpirationIsUnchanged(Metacard metacard) {
    assertThat(metacard, notNullValue());
    DateTime unchangedExpirationDate = new DateTime(metacard.getExpirationDate());
    assertThat(unchangedExpirationDate.equals(ORIG_EXPIRATION_DATE), is(true));
  }

  private void assertThatExpirationIsNewOffset(Metacard metacard) {
    assertThat(metacard, notNullValue());
    DateTime newExpirationDate = new DateTime(metacard.getExpirationDate());
    assertThat(newExpirationDate.equals(CREATED_DATE.plusDays(DAYS)), is(true));
  }
}
