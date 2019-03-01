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
package org.codice.ddf.catalog.content.impl;

import static org.codice.ddf.catalog.content.impl.FileSystemStorageProvider.CRYPTER_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.content.operation.impl.ReadStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import ddf.security.SecurityConstants;
import ddf.security.encryption.crypter.Crypter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemStorageProviderTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String baseDir;

  private String baseTmpDir;

  private static final String NITF_MIME_TYPE = "image/nitf";

  private static final String TEST_INPUT_CONTENTS = "Hello World";

  private static final String TEST_INPUT_FILENAME = "myfile.nitf";

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageProviderTest.class);

  private static final String QUALIFIER = "example";

  private FileSystemStorageProvider provider;

  @Before
  public void setUp() throws IOException {
    baseDir = temporaryFolder.getRoot().getAbsolutePath();
    baseTmpDir = temporaryFolder.newFolder(FileSystemStorageProvider.DEFAULT_TMP).getAbsolutePath();

    String keysetHome = temporaryFolder.newFolder("keysets").getAbsolutePath();
    String associatedDataHome = temporaryFolder.newFolder("etc").getAbsolutePath();
    System.setProperty(SecurityConstants.KEYSET_DIR, keysetHome);
    System.setProperty(
        SecurityConstants.ASSOCIATED_DATA_PATH,
        associatedDataHome.concat("/associatedData.properties"));

    MimeTypeResolver resolver = new MockMimeTypeResolver();
    MimeTypeMapper mapper = new MimeTypeMapperImpl(Collections.singletonList(resolver));

    this.provider = new FileSystemStorageProvider();
    try {
      provider.setBaseContentDirectory(baseDir);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    provider.setMimeTypeMapper(mapper);
  }

  @After
  public void clean() {
    System.clearProperty(SecurityConstants.KEYSET_DIR);
    System.clearProperty(SecurityConstants.ASSOCIATED_DATA_PATH);
  }

  @Test
  public void testCreate() throws Exception {
    assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, "test.nitf");
  }

  @Test
  public void testCreateMimeTypeWithNonIdParameter() throws Exception {
    String mimeType = "image/nitf; charset=UTF-8";
    assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.nitf");
  }

  @Test
  public void testCreateMimeTypeWithIdParameter() throws Exception {
    String mimeType = "text/xml; id=xml";
    assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.xml");
  }

  @Test
  public void testCreateMimeTypeWithIdAndOtherParameter() throws Exception {
    String mimeType = "text/xml; charset=UTF-8; id=xml";
    assertContentItem(TEST_INPUT_CONTENTS, mimeType, "test.xml");
  }

  @Test
  public void testRead() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    String uriString = createResponse.getCreatedContentItems().get(0).getUri();

    assertReadRequest(uriString, NITF_MIME_TYPE);
  }

  @Test(expected = StorageException.class)
  public void testReadDeletedReference() throws Exception {
    Path tempFile = Files.createTempFile("test", "nitf");
    Files.write(tempFile, TEST_INPUT_CONTENTS.getBytes());
    CreateStorageResponse createResponse =
        assertContentItem(
            TEST_INPUT_CONTENTS,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            Collections.singletonMap(
                Constants.STORE_REFERENCE_KEY, tempFile.toUri().toASCIIString()));
    URI uri = new URI(createResponse.getCreatedContentItems().get(0).getUri());

    ReadStorageResponse read =
        provider.read(new ReadStorageRequestImpl(uri, Collections.emptyMap()));
    assertThat(read.getContentItem(), notNullValue());
    Files.delete(tempFile);
    provider.read(new ReadStorageRequestImpl(uri, Collections.emptyMap()));
  }

  @Test(expected = StorageException.class)
  public void testReadDeletedRemoteReference() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(
            TEST_INPUT_CONTENTS,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            Collections.singletonMap(
                Constants.STORE_REFERENCE_KEY, "http://testHostName:12345/test.txt"));

    URI uri = new URI(createResponse.getCreatedContentItems().get(0).getUri());
    provider.read(new ReadStorageRequestImpl(uri, Collections.emptyMap()));
  }

  @Test
  public void testUpdate() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("Updated NITF");
          }
        };
    ContentItem updateItem =
        new ContentItemImpl(id, byteSource, NITF_MIME_TYPE, mock(Metacard.class));
    submitAndVerifySuccessfulUpdateStorageRequest(updateItem);
  }

  @Test
  public void testDelete() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);
    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    ContentItem item = items.get(0);

    LOGGER.debug("Item retrieved: {}", item);
    assertEquals(id, item.getId());
    assertThat(item.getFilename(), isEmptyString());
  }

  @Test
  public void testCreateWithQualifier() throws Exception {
    assertContentItemWithQualifier(
        TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", "example");
  }

  @Test
  public void testCreateWithQualifierAndOneInvalidItem() throws Exception {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("This data is my data, this data is your data.");
          }
        };
    ContentItem contentItem =
        new ContentItemImpl(
            uuid,
            null,
            byteSource,
            "application/text",
            "datadatadata",
            byteSource.size(),
            mock(Metacard.class));
    String invalidUuid = "wow-this-isn't-a-valid-uuid-right?!@#%";
    ContentItem badContentItem =
        new ContentItemImpl(
            invalidUuid,
            null,
            byteSource,
            "application/text",
            "datadatadata",
            byteSource.size(),
            mock(Metacard.class));
    CreateStorageRequest createRequest =
        new CreateStorageRequestImpl(Lists.newArrayList(contentItem, badContentItem), null);
    CreateStorageResponse createResponse = provider.create(createRequest);

    assertThat(createResponse.getCreatedContentItems().size(), is(1));
    assertThat(createResponse.getCreatedContentItems().get(0).getId(), is(uuid));

    ContentItem updateContentItem =
        new ContentItemImpl(
            invalidUuid,
            null,
            byteSource,
            "application/text",
            "datadatadata",
            byteSource.size(),
            mock(Metacard.class));
    UpdateStorageRequest updateRequest =
        new UpdateStorageRequestImpl(Lists.newArrayList(updateContentItem), null);
    UpdateStorageResponse updateResponse = provider.update(updateRequest);

    assertThat(updateResponse.getUpdatedContentItems().size(), is(0));
  }

  @Test
  public void testInvalidDelete() throws Exception {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(uuid);
    ContentItem contentItem =
        new ContentItemImpl(uuid, null, null, "application/text", "datadatadata", 0, metacard);
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(Lists.newArrayList(metacard), null);
    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);

    assertThat(deleteResponse.getDeletedContentItems().size(), is(0));
  }

  @Test
  public void testReadWithQualifier() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", QUALIFIER);
    String uriString = createResponse.getCreatedContentItems().get(0).getUri();

    assertReadRequest(uriString, NITF_MIME_TYPE);
  }

  @Test
  public void testUpdateWithQualifier() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", QUALIFIER);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("Updated NITF");
          }
        };
    ContentItem updateItem =
        new ContentItemImpl(id, QUALIFIER, byteSource, NITF_MIME_TYPE, mock(Metacard.class));
    submitAndVerifySuccessfulUpdateStorageRequest(updateItem);
  }

  @Test
  public void testDeleteWithQualifier() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", QUALIFIER);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);

    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    provider.commit(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    ContentItem item = items.get(0);

    LOGGER.debug("Item retrieved: {}", item);
    assertEquals(id, item.getId());
    assertThat(item.getFilename(), is(""));
  }

  @Test
  public void testCreateWithMultipleItems() throws Exception {
    assertContentItemWithQualifier(
        TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", "example");
    assertContentItemWithQualifier(
        TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, "", "example");
  }

  @Test
  public void testReadWithMultipleItems() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    String unqualifiedUriString = createResponse.getCreatedContentItems().get(0).getUri();
    createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            createResponse.getCreatedContentItems().get(0).getId(),
            QUALIFIER);
    String qualifiedUriString = createResponse.getCreatedContentItems().get(0).getUri();

    assertReadRequest(unqualifiedUriString, NITF_MIME_TYPE);

    assertReadRequest(qualifiedUriString, NITF_MIME_TYPE);
  }

  @Test
  public void testUpdateWithMultipleItems() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            createResponse.getCreatedContentItems().get(0).getId(),
            QUALIFIER);

    URI qualifiedUri = new URI(createResponse.getCreatedContentItems().get(0).getUri());

    String id = createResponse.getCreatedContentItems().get(0).getId();
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("Updated NITF");
          }
        };
    ContentItem updateItem =
        new ContentItemImpl(id, byteSource, NITF_MIME_TYPE, mock(Metacard.class));
    submitAndVerifySuccessfulUpdateStorageRequest(updateItem);

    updateItem =
        new ContentItemImpl(
            id, qualifiedUri.getFragment(), byteSource, NITF_MIME_TYPE, mock(Metacard.class));
    submitAndVerifySuccessfulUpdateStorageRequest(updateItem);
  }

  @Test
  public void testUpdateMultipleQualifiedItemsInTheSameRequest() throws Exception {
    // store unqualified content item
    final CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    final ContentItem createdContentItem = createResponse.getCreatedContentItems().get(0);
    final String id = createdContentItem.getId();
    final Metacard metacard = createdContentItem.getMetacard();

    // add 2 new qualified content items with the same id
    final ByteSource q1ByteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("q1 content");
          }
        };
    final ContentItem q1ContentItem =
        new ContentItemImpl(
            id, "q1", q1ByteSource, "image/png", "q1.png", q1ByteSource.size(), metacard);

    final ByteSource q2ByteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("q2 content");
          }
        };
    final ContentItem q2ContentItem =
        new ContentItemImpl(
            id, "q2", q2ByteSource, "image/png", "q2.png", q2ByteSource.size(), metacard);

    submitAndVerifySuccessfulUpdateStorageRequest(q1ContentItem, q2ContentItem);
  }

  @Test
  public void testDeleteIdWithMultpleContent() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    createResponse =
        assertContentItemWithQualifier(
            TEST_INPUT_CONTENTS,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            createResponse.getCreatedContentItems().get(0).getId(),
            QUALIFIER);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);
    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    provider.commit(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    assertThat(items, hasSize(2));
    for (ContentItem item : items) {
      LOGGER.debug("Item retrieved: {}", item);
      assertThat(item.getId(), is(id));
      assertThat(item.getFilename(), isEmptyString());
    }
  }

  @Test(expected = StorageException.class)
  public void testRollback() throws Exception {
    String id = UUID.randomUUID().toString().replaceAll("-", "");
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(TEST_INPUT_CONTENTS);
          }
        };
    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(id);
    ContentItem contentItem =
        new ContentItemImpl(
            id,
            byteSource,
            NITF_MIME_TYPE,
            TEST_INPUT_FILENAME,
            TEST_INPUT_CONTENTS.getBytes().length,
            metacard);
    CreateStorageRequest createRequest =
        new CreateStorageRequestImpl(Collections.singletonList(contentItem), null);

    CreateStorageResponse createStorageResponse = provider.create(createRequest);
    provider.rollback(createRequest);

    ReadStorageRequest readStorageRequest =
        new ReadStorageRequestImpl(new URI("content:" + id), null);
    ReadStorageResponse read = provider.read(readStorageRequest);
  }

  @Test
  public void testDeleteWithSimilarIds() throws Exception {
    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME);
    String id = createResponse.getCreatedContentItems().get(0).getId();
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    String badId = id.substring(0, 6) + uuid.substring(6, uuid.length() - 1);
    boolean hadError = false;
    try {
      CreateStorageResponse badCreateResponse =
          assertContentItemWithQualifier(
              TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, badId, "");
    } catch (AssertionError e) {
      // bad id is not a valid ID
      hadError = true;
    } finally {
      if (!hadError) {
        fail("Create succeeded when it should not have! " + badId + "Should not be valid!");
      }
      hadError = false;
    }

    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);
    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    ContentItem item = items.get(0);

    LOGGER.debug("Item retrieved: {}", item);
    assertEquals(id, item.getId());
    assertThat(item.getFilename(), is(""));
    provider.commit(deleteRequest);

    try {
      assertReadRequest(createResponse.getCreatedContentItems().get(0).getUri(), NITF_MIME_TYPE);
    } catch (StorageException e) {
      // The item was deleted so it shouldn't have found it
      hadError = true;
    } finally {
      if (!hadError) {
        fail("read succeeded when it should not have! ");
      }
    }
  }

  @Test
  public void testDeleteReference() throws Exception {
    String path = baseTmpDir + File.separator + TEST_INPUT_FILENAME;
    File tempFile = new File(path);
    FileUtils.writeStringToFile(tempFile, TEST_INPUT_CONTENTS);

    Map<String, Serializable> properties = new HashMap<>();
    properties.put(Constants.STORE_REFERENCE_KEY, tempFile.toURI().toASCIIString());

    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, properties);

    String id = createResponse.getCreatedContentItems().get(0).getId();
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);
    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    ContentItem item = items.get(0);

    LOGGER.debug("Item retrieved: {}", item);
    assertEquals(id, item.getId());
    assertThat(item.getFilename(), isEmptyString());

    assertTrue(tempFile.exists());
  }

  @Test
  public void testCreateAndReadReference() throws Exception {
    String path = baseTmpDir + File.separator + TEST_INPUT_FILENAME;
    File file = new File(path);
    Crypter crypter = new Crypter(CRYPTER_NAME);
    InputStream encryptedStream =
        crypter.encrypt(new ByteArrayInputStream(TEST_INPUT_CONTENTS.getBytes()));
    Files.copy(encryptedStream, file.toPath());

    Map<String, Serializable> properties = new HashMap<>();
    properties.put(Constants.STORE_REFERENCE_KEY, file.toURI().toASCIIString());

    CreateStorageResponse createResponse =
        assertContentItem(TEST_INPUT_CONTENTS, NITF_MIME_TYPE, TEST_INPUT_FILENAME, properties);

    String uriString = createResponse.getCreatedContentItems().get(0).getUri();

    assertReadRequest(uriString, NITF_MIME_TYPE);
  }

  /** ******************************************************************************* */
  private CreateStorageResponse assertContentItem(
      String data, String mimeTypeRawData, String filename, Map<String, Serializable> properties)
      throws Exception {
    return assertContentItemWithQualifier(data, mimeTypeRawData, filename, "", "", properties);
  }

  private CreateStorageResponse assertContentItem(
      String data, String mimeTypeRawData, String filename) throws Exception {
    return assertContentItemWithQualifier(data, mimeTypeRawData, filename, "", "");
  }

  public CreateStorageResponse assertContentItemWithQualifier(
      String data, String mimeTypeRawData, String filename, String id, String qualifier)
      throws Exception {
    return assertContentItemWithQualifier(data, mimeTypeRawData, filename, id, qualifier, null);
  }

  public CreateStorageResponse assertContentItemWithQualifier(
      String data,
      String mimeTypeRawData,
      String filename,
      String id,
      String qualifier,
      Map<String, Serializable> properties)
      throws Exception {
    // Simulates what ContentFrameworkImpl would do
    String uuid = StringUtils.defaultIfBlank(id, UUID.randomUUID().toString().replaceAll("-", ""));
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(data);
          }
        };
    ContentItem contentItem =
        new ContentItemImpl(
            uuid,
            qualifier,
            byteSource,
            mimeTypeRawData,
            filename,
            byteSource.size(),
            mock(Metacard.class));
    CreateStorageRequest createRequest =
        new CreateStorageRequestImpl(Collections.singletonList(contentItem), properties);
    CreateStorageResponse createResponse = provider.create(createRequest);
    List<ContentItem> createdContentItems = createResponse.getCreatedContentItems();
    ContentItem createdContentItem =
        createdContentItems.isEmpty() ? null : createdContentItems.get(0);

    assertNotNull(createdContentItem);
    String createdId = createdContentItem.getId();
    assertNotNull(createdId);
    assertThat(createdId, equalTo(uuid));

    String contentUri = createdContentItem.getUri();
    LOGGER.debug("contentUri = {}", contentUri);
    assertNotNull(contentUri);
    String expectedContentUri =
        ContentItem.CONTENT_SCHEME
            + ":"
            + uuid
            + ((StringUtils.isNotBlank(qualifier)) ? "#" + qualifier : "");
    assertThat(contentUri, equalTo(expectedContentUri));

    assertTrue(createdContentItem.getSize() > 0);
    String createdMimeType = createdContentItem.getMimeTypeRawData().replace(";", "");
    List<String> createdMimeTypeArr = new ArrayList<>(Arrays.asList(createdMimeType.split(" ")));
    List<String> givenMimeTypeArr =
        new ArrayList<>(Arrays.asList(mimeTypeRawData.replace(";", "").split(" ")));
    assertEquals(createdMimeTypeArr.size(), givenMimeTypeArr.size());
    givenMimeTypeArr.removeAll(createdMimeTypeArr);
    assertThat(givenMimeTypeArr.size(), is(0));
    provider.commit(createRequest);
    return createResponse;
  }

  private void assertReadRequest(String uriString, String mimeType)
      throws StorageException, IOException, URISyntaxException {
    final URI uri = new URI(uriString);

    ReadStorageRequest readRequest = new ReadStorageRequestImpl(uri, Collections.emptyMap());

    ReadStorageResponse readResponse = provider.read(readRequest);
    ContentItem item = readResponse.getContentItem();

    LOGGER.debug("Item retrieved: {}", item);
    assertThat(item, notNullValue());
    assertThat(item.getId(), is(uri.getSchemeSpecificPart()));
    if (uri.getFragment() != null) {
      assertThat(item.getQualifier(), is(uri.getFragment()));
    }

    if (mimeType.equals(NITF_MIME_TYPE)) {
      assertThat(item.getMimeTypeRawData(), is(NITF_MIME_TYPE));
    }

    List<String> parts =
        provider.getContentFilePathParts(uri.getSchemeSpecificPart(), uri.getFragment());
    String expectedFilePath =
        baseDir
            + File.separator
            + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
            + File.separator
            + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
            + File.separator
            + parts.get(0)
            + File.separator
            + parts.get(1)
            + File.separator
            + parts.get(2)
            + (StringUtils.isNotBlank(item.getQualifier())
                ? File.separator + item.getQualifier()
                : "")
            + File.separator
            + item.getFilename();
    assertThat(Files.exists(Paths.get(expectedFilePath)), is(true));
    assertTrue(item.getSize() > 0);
  }

  private void submitAndVerifySuccessfulUpdateStorageRequest(ContentItem... requestContentItems)
      throws Exception {
    final UpdateStorageRequest updateStorageRequest =
        new UpdateStorageRequestImpl(Arrays.asList(requestContentItems), null);
    final UpdateStorageResponse updateStorageResponse = provider.update(updateStorageRequest);
    final List<ContentItem> responseContentItems = updateStorageResponse.getUpdatedContentItems();

    assertThat(
        "Expect number of ContentItems in UpdateStorageResponse",
        responseContentItems,
        hasSize(requestContentItems.length));
    for (final ContentItem responseContentItem : responseContentItems) {
      // assert file exists
      final URI uri = new URI(responseContentItem.getUri());
      final List<String> parts =
          provider.getContentFilePathParts(uri.getSchemeSpecificPart(), uri.getFragment());
      final String expectedFilePath =
          baseDir
              + File.separator
              + FileSystemStorageProvider.DEFAULT_CONTENT_REPOSITORY
              + File.separator
              + FileSystemStorageProvider.DEFAULT_CONTENT_STORE
              + File.separator
              + FileSystemStorageProvider.DEFAULT_TMP
              + File.separator
              + updateStorageRequest.getId()
              + File.separator
              + parts.get(0)
              + File.separator
              + parts.get(1)
              + File.separator
              + parts.get(2)
              + (StringUtils.isNotBlank(responseContentItem.getQualifier())
                  ? File.separator + responseContentItem.getQualifier()
                  : "")
              + File.separator
              + responseContentItem.getFilename();
      assertThat(
          "Expect file exists at " + expectedFilePath, Files.exists(Paths.get(expectedFilePath)));

      // assert metacard attributes set
      final ArgumentCaptor<Attribute> captor = ArgumentCaptor.forClass(Attribute.class);
      final Metacard metacard = responseContentItem.getMetacard();

      if (StringUtils.isBlank(responseContentItem.getQualifier())) {
        verify(metacard, times(2)).setAttribute(captor.capture());
        Attribute resourceUriAttribute = captor.getAllValues().get(0);
        assertThat(resourceUriAttribute.getName(), is(Metacard.RESOURCE_URI));
        assertThat(resourceUriAttribute.getValue(), is(uri.toString()));
        Attribute resourceSizeAttribute = captor.getAllValues().get(1);
        assertThat(resourceSizeAttribute.getName(), is(Metacard.RESOURCE_SIZE));
        assertThat(resourceSizeAttribute.getValue(), is(responseContentItem.getSize()));
      } else {
        verify(metacard, never()).setAttribute(any());
      }
    }

    provider.commit(updateStorageRequest);

    for (ContentItem responseContentItem : responseContentItems) {
      assertReadRequest(responseContentItem.getUri(), responseContentItem.getMimeType().toString());
    }
  }
}
