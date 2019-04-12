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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.camel.component.catalog.ingest.PostIngestConsumer;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.model.RoutesDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MetacardStorageRouteTest {

  private MetacardStorageRoute storageRoute =
      mock(MetacardStorageRoute.class, Mockito.CALLS_REAL_METHODS);

  private static final List<String> DEFAULT_TAGS = Arrays.asList("resource");

  @Before
  public void setUp() {
    RoutesDefinition mockRoutesDefinition = mock(RoutesDefinition.class);
    when(storageRoute.getRouteCollection()).thenReturn(mockRoutesDefinition);
    when(mockRoutesDefinition.toString()).thenReturn("test");
    storageRoute.setBackupMetacardTags(DEFAULT_TAGS);
  }

  @Test
  public void testRefreshValid() throws Exception {
    boolean backupInvalidCards = false;
    boolean keepDeletedMetacards = false;
    String metacardTransformerId = "testTransformer";
    List<String> tags = Arrays.asList("resource");

    Map<String, Object> properties = new HashMap<>();
    properties.put("backupInvalidMetacards", backupInvalidCards);
    properties.put("keepDeletedMetacards", keepDeletedMetacards);
    properties.put("metacardTransformerId", metacardTransformerId);
    properties.put("backupMetacardTags", tags);

    storageRoute.refresh(properties);
    assertThat(storageRoute.isBackupInvalidMetacards(), is(backupInvalidCards));
    assertThat(storageRoute.isKeepDeletedMetacards(), is(keepDeletedMetacards));
    assertThat(storageRoute.getMetacardTransformerId(), is(metacardTransformerId));
    assertThat(
        storageRoute.getBackupMetacardTags(), containsInAnyOrder(tags.toArray(new String[0])));
  }

  @Test
  public void testRefreshInvalid() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("backupInvalidMetacards", 2);
    properties.put("keepDeletedMetacards", 15);
    properties.put("metacardTransformerId", 5);
    properties.put("backupMetacardTags", "test");
    storageRoute.refresh(properties);
    assertThat(storageRoute.isBackupInvalidMetacards(), is(false));
    assertThat(storageRoute.isKeepDeletedMetacards(), is(false));
    assertThat(storageRoute.getMetacardTransformerId(), is(nullValue()));
    assertThat(
        storageRoute.getBackupMetacardTags(),
        containsInAnyOrder(DEFAULT_TAGS.toArray(new String[0])));
  }

  @Test
  public void testGetShouldBackupPredicateTestValidCard() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockMessage.getBody()).thenReturn(getMetacard(true));
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_INVALID_RTE_PROP, Boolean.class))
        .thenReturn(false);
    Predicate predicate = storageRoute.getShouldBackupPredicate();
    assertThat(predicate.matches(mockExchange), is(true));
  }

  @Test
  public void testGetShouldBackupPredicateTestInvalidCardTrue() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockMessage.getBody()).thenReturn(getMetacard(false));
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_INVALID_RTE_PROP, Boolean.class))
        .thenReturn(true);
    Predicate predicate = storageRoute.getShouldBackupPredicate();
    assertThat(predicate.matches(mockExchange), is(true));
  }

  @Test
  public void testGetShouldBackupPredicateTestInvalidCardFalse() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockMessage.getBody()).thenReturn(getMetacard(false));
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_INVALID_RTE_PROP, Boolean.class))
        .thenReturn(false);
    Predicate predicate = storageRoute.getShouldBackupPredicate();
    assertThat(predicate.matches(mockExchange), is(false));
  }

  @Test
  public void testGetShouldBackupPredicateNonMetacardBody() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockMessage.getBody()).thenReturn("Test");
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_INVALID_RTE_PROP, Boolean.class))
        .thenReturn(false);
    Predicate predicate = storageRoute.getShouldBackupPredicate();
    assertThat(predicate.matches(mockExchange), is(false));
  }

  @Test
  public void testCheckDeletePredicateDontKeepBackups() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(PostIngestConsumer.ACTION, String.class))
        .thenReturn(PostIngestConsumer.DELETE);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_KEEP_DELETED_RTE_PROP, Boolean.class))
        .thenReturn(false);
    Predicate predicate = storageRoute.getCheckDeletePredicate();
    assertThat(predicate.matches(mockExchange), is(true));
  }

  @Test
  public void testCheckDeletePredicateKeepBackups() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(PostIngestConsumer.ACTION, String.class))
        .thenReturn(PostIngestConsumer.DELETE);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_KEEP_DELETED_RTE_PROP, Boolean.class))
        .thenReturn(true);
    Predicate predicate = storageRoute.getCheckDeletePredicate();
    assertThat(predicate.matches(mockExchange), is(false));
  }

  @Test
  public void testCheckDeletePredicateWhenCreateAction() {
    setTestDefaults();
    Exchange mockExchange = mock(Exchange.class);
    Message mockMessage = mock(Message.class);
    when(mockExchange.getIn()).thenReturn(mockMessage);
    when(mockMessage.getHeader(PostIngestConsumer.ACTION, String.class))
        .thenReturn(PostIngestConsumer.CREATE);
    when(mockMessage.getHeader(
            MetacardStorageRoute.METACARD_BACKUP_KEEP_DELETED_RTE_PROP, Boolean.class))
        .thenReturn(true);
    Predicate predicate = storageRoute.getCheckDeletePredicate();
    assertThat(predicate.matches(mockExchange), is(false));
  }

  @Test
  public void testProperties() {
    storageRoute.setKeepDeletedMetacards(false);
    assertThat(storageRoute.isKeepDeletedMetacards(), is(false));

    storageRoute.setBackupInvalidMetacards(false);
    assertThat(storageRoute.isBackupInvalidMetacards(), is(false));

    storageRoute.setMetacardTransformerId("test");
    assertThat(storageRoute.getMetacardTransformerId(), is("test"));
  }

  @Test
  public void testBackupTrueTagInvalid() {
    boolean shouldBackup = storageRoute.shouldBackupMetacard(getTestInvalidMetacard(), true);
    assertThat(shouldBackup, is(true));
  }

  @Test
  public void testBackupFalseTagInvalid() {
    boolean shouldBackup = storageRoute.shouldBackupMetacard(getTestInvalidMetacard(), false);
    assertThat(shouldBackup, is(false));
  }

  @Test
  public void testBackupFalseTagValid() {
    boolean shouldBackup = storageRoute.shouldBackupMetacard(getTestValidMetacard(), false);
    assertThat(shouldBackup, is(true));
  }

  @Test
  public void testNoTagsValid() {
    boolean shouldBackup = storageRoute.shouldBackupMetacard(getTestNoTagsMetacard(), false);
    assertThat(shouldBackup, is(false));
  }

  @Test
  public void testBackupNonResourceCard() {
    boolean shouldBackup = storageRoute.shouldBackupMetacard(getTestNonResourceMetacard(), false);
    assertThat(shouldBackup, is(false));
  }

  private Metacard getTestInvalidMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(
        new AttributeImpl(Core.METACARD_TAGS, Arrays.asList("INVALID", "resource")));
    return metacard;
  }

  private Metacard getTestValidMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(
        new AttributeImpl(Core.METACARD_TAGS, Arrays.asList("VALID", "resource")));
    return metacard;
  }

  private Metacard getTestNonResourceMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, "VALID"));
    return metacard;
  }

  private Metacard getTestNoTagsMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    return metacard;
  }

  private void setTestDefaults() {
    storageRoute.setKeepDeletedMetacards(true);
    storageRoute.setBackupInvalidMetacards(true);
    storageRoute.setMetacardTransformerId("metacard");
  }

  private Metacard getMetacard(boolean valid) {
    HashSet<String> tags = new HashSet<>();
    if (valid) {
      tags.add("VALID");
    } else {
      tags.add("INVALID");
    }
    tags.add("resource");
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTags(tags);

    return metacard;
  }
}
