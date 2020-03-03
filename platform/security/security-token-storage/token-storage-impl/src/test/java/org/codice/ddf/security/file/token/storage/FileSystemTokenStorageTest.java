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
package org.codice.ddf.security.file.token.storage;

import static org.apache.http.HttpStatus.SC_OK;
import static org.codice.ddf.security.file.token.storage.TokenInformationUtil.GSON;
import static org.codice.ddf.security.token.storage.api.TokenStorage.ACCESS_TOKEN;
import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.EXPIRES_AT;
import static org.codice.ddf.security.token.storage.api.TokenStorage.REFRESH_TOKEN;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.security.encryption.crypter.Crypter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemTokenStorageTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private static final String USERNAME = "username";
  private static final String SOURCE_ID = "CSW";
  private static final String ENCRYPTED = "encrypted";
  private static final String ACCESS_TOKEN_VAL = "myAccessToken";
  private static final String REFRESH_TOKEN_VAL = "myRefreshToken";
  private static final String USERNAME_HASH =
      "16f78a7d6317f102bbd95fc9a4f3ff2e3249287690b8bdad6b7810f82b34ace3";
  private static final String DISCOVERY_URL_VAL = "https://localhost:8080/metadata";

  private FileSystemTokenStorage tokenStorage;
  private Crypter crypter;

  @Before
  public void setUp() throws Exception {
    crypter = mock(Crypter.class);
    tokenStorage = new FileSystemTokenStorage(crypter);
    tokenStorage.setBaseDirectory(folder.getRoot().getAbsolutePath());
  }

  @After
  public void tearDown() {
    folder.delete();
  }

  @Test
  public void testCreate() throws Exception {
    when(crypter.encrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(ENCRYPTED.getBytes()));

    int created =
        tokenStorage.create(
            USERNAME, SOURCE_ID, ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, DISCOVERY_URL_VAL);
    assertEquals(SC_OK, created);

    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    assertTrue(Files.readAllLines(Paths.get(path)).contains(ENCRYPTED));
  }

  @Test
  public void testRead() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));

    TokenInformation tokenInformation = tokenStorage.read(USERNAME);
    TokenInformation.TokenEntry tokenEntry = tokenInformation.getTokenEntries().get(SOURCE_ID);
    assertEquals(USERNAME_HASH, tokenInformation.getId());
    assertEquals(json, tokenInformation.getTokenJson());
    assertEquals(Collections.singleton(DISCOVERY_URL_VAL), tokenInformation.getDiscoveryUrls());
    assertEquals(ACCESS_TOKEN_VAL, tokenEntry.getAccessToken());
    assertEquals(REFRESH_TOKEN_VAL, tokenEntry.getRefreshToken());
    assertEquals(DISCOVERY_URL_VAL, tokenEntry.getDiscoveryUrl());
  }

  @Test
  public void testReadPerSource() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));

    TokenInformation.TokenEntry tokenEntry = tokenStorage.read(USERNAME, SOURCE_ID);
    assertEquals(ACCESS_TOKEN_VAL, tokenEntry.getAccessToken());
    assertEquals(REFRESH_TOKEN_VAL, tokenEntry.getRefreshToken());
    assertEquals(DISCOVERY_URL_VAL, tokenEntry.getDiscoveryUrl());
  }

  @Test
  public void testUpdate() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));
    when(crypter.encrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(ENCRYPTED.getBytes()));

    int updated =
        tokenStorage.create(
            USERNAME, SOURCE_ID, ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, DISCOVERY_URL_VAL);
    assertEquals(SC_OK, updated);
    assertTrue(Files.readAllLines(Paths.get(path)).contains(ENCRYPTED));
  }

  @Test
  public void testUpdateNoExistingValue() throws Exception {
    when(crypter.encrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(ENCRYPTED.getBytes()));

    int updated =
        tokenStorage.create(
            USERNAME, SOURCE_ID, ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, DISCOVERY_URL_VAL);
    assertEquals(SC_OK, updated);

    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    assertTrue(Files.readAllLines(Paths.get(path)).contains(ENCRYPTED));
  }

  @Test
  public void testIsAvailable() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));

    boolean available = tokenStorage.isAvailable(USERNAME, SOURCE_ID);
    assertTrue(available);
  }

  @Test
  public void testDelete() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL),
                "OpenSearch",
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));
    when(crypter.encrypt(any(InputStream.class)))
        .thenAnswer(i -> i.getArgumentAt(0, InputStream.class));

    int deleted = tokenStorage.delete(USERNAME);
    assertEquals(SC_OK, deleted);
    assertFalse(Files.exists(Paths.get(path)));
  }

  @Test
  public void testDeletePerSource() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL),
                "OpenSearch",
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));
    when(crypter.encrypt(any(InputStream.class)))
        .thenAnswer(i -> i.getArgumentAt(0, InputStream.class));

    int deleted = tokenStorage.delete(USERNAME, SOURCE_ID);
    assertEquals(SC_OK, deleted);
    String lines = String.join("", Files.readAllLines(Paths.get(path)));
    assertTrue(lines.contains("OpenSearch"));
    assertFalse(lines.contains(SOURCE_ID));
  }

  @Test
  public void testDeleteOneSource() throws Exception {
    folder.newFile(USERNAME_HASH);
    String path = folder.getRoot().getAbsolutePath() + "/" + USERNAME_HASH;
    Files.write(Paths.get(path), ENCRYPTED.getBytes());

    String json =
        GSON.toJson(
            ImmutableMap.of(
                SOURCE_ID,
                ImmutableMap.of(
                    ACCESS_TOKEN,
                    ACCESS_TOKEN_VAL,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_VAL,
                    DISCOVERY_URL,
                    DISCOVERY_URL_VAL)));

    when(crypter.decrypt(any(InputStream.class)))
        .thenReturn(new ByteArrayInputStream(json.getBytes()));
    when(crypter.encrypt(any(InputStream.class)))
        .thenAnswer(i -> i.getArgumentAt(0, InputStream.class));

    int deleted = tokenStorage.delete(USERNAME, SOURCE_ID);
    assertEquals(SC_OK, deleted);
    assertFalse(Files.exists(Paths.get(path)));
  }

  @Test
  public void testGetStateMap() {
    String state = UUID.randomUUID().toString();
    Map<String, Object> stateMap = new HashMap<>();
    stateMap.put(SOURCE_ID, SOURCE_ID);
    stateMap.put(CLIENT_ID, "ddf-client");
    stateMap.put(SECRET, "secret");
    stateMap.put(DISCOVERY_URL, DISCOVERY_URL_VAL);
    stateMap.put(EXPIRES_AT, Instant.now().minus(3, ChronoUnit.MINUTES).getEpochSecond());
    tokenStorage.getStateMap().put(state, stateMap);

    assertEquals(0, tokenStorage.getStateMap().size());
  }
}
