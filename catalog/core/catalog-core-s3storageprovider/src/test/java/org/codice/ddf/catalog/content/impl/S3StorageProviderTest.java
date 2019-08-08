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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class S3StorageProviderTest {

  private static final String MOCK_S3_ENDPOINT = "s3.endpoint";

  private static final String MOCK_S3_REGION = "region";

  private static final String MOCK_S3_BUCKET = "s3-bucket";

  private static final String MOCK_S3_ACCESS_KEY = "ACCESS_KEY";

  private static final String MOCK_S3_SECRET_KEY = "SECRET_KEY";

  private static final String MOCK_CONTENT_PREFIX_1 = "data/content/store/";

  private static final String MOCK_CONTENT_PREFIX_2 = "data/content/store";

  private static final String MOCK_KEY_ID = "arn:aws:kms:region:acct-id:key/key-id";

  private static final Boolean USE_SSE_S3_ENCRYPTION = false;

  private static final String NITF_MIME_TYPE = "image/nitf";

  private static final String MOCK_FILE_CONTENTS = "Hello World";

  private static final String MOCK_UPDATED_FILE_CONTENTS = "Updated NITF";

  private static final String MOCK_FILENAME = "test.ntf";

  private static final String MOCK_UPDATED_FILENAME = "test-updated.ntf";

  private static final String QUALIFIER = "example";

  private S3TestStorageProvider provider;

  class S3TestStorageProvider extends S3StorageProvider {

    S3TestStorageProvider(MimeTypeMapper mapper) {
      super(mapper);
      init();
    }

    @Override
    public void init() {
      this.amazonS3 = mock(AmazonS3.class);
    }
  }

  @Before
  public void setUp() {
    MimeTypeResolver resolver = new MockMimeTypeResolver();
    MimeTypeMapper mapper = new MimeTypeMapperImpl(Collections.singletonList(resolver));

    provider = new S3TestStorageProvider(mapper);
    Map<String, Object> properties = new HashMap<>();
    properties.put("s3Endpoint", MOCK_S3_ENDPOINT);
    properties.put("s3Region", MOCK_S3_REGION);
    properties.put("s3Bucket", MOCK_S3_BUCKET);
    properties.put("s3AccessKey", MOCK_S3_ACCESS_KEY);
    properties.put("s3SecretKey", MOCK_S3_SECRET_KEY);
    properties.put("contentPrefix", MOCK_CONTENT_PREFIX_1);
    properties.put("awsKmsKeyId", MOCK_KEY_ID);
    properties.put("useSseS3Encryption", USE_SSE_S3_ENCRYPTION);
    provider.update(properties);

    ListObjectsV2Result listObjectsV2Result = mock(ListObjectsV2Result.class);
    when(provider.amazonS3.listObjectsV2(eq(MOCK_S3_BUCKET), contains(MOCK_CONTENT_PREFIX_1)))
        .thenReturn(listObjectsV2Result);
    when(listObjectsV2Result.getKeyCount()).thenReturn(1);
  }

  @Test
  public void testCreate() throws IOException, StorageException {
    assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");
  }

  @Test
  public void testCreateWithQualifier() throws IOException, StorageException {
    assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, QUALIFIER, NITF_MIME_TYPE, "");
  }

  @Test
  public void testCreateWithSseS3Encryption() throws IOException, StorageException {
    provider.setUseSseS3Encryption(true);

    assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, QUALIFIER, NITF_MIME_TYPE, "");

    provider.setUseSseS3Encryption(false);
  }

  @Test
  public void testCreateWithUpdatedContentPrefixAndAwsKmSKey()
      throws IOException, StorageException {
    provider.setContentPrefix(MOCK_CONTENT_PREFIX_2);
    provider.setAwsKmsKeyId("");

    assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, QUALIFIER, NITF_MIME_TYPE, "");

    provider.setContentPrefix(MOCK_CONTENT_PREFIX_1);
    provider.setAwsKmsKeyId(MOCK_KEY_ID);
  }

  @Test
  public void testCreateWithQualifierAndOneInvalidItem() throws IOException, StorageException {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream("Invalid Content Item", StandardCharsets.UTF_8);
          }
        };
    ContentItem contentItem =
        new ContentItemImpl(
            uuid,
            null,
            byteSource,
            MediaType.APPLICATION_XML,
            "datadatadata.xml",
            byteSource.size(),
            mock(Metacard.class));
    String invalidUuid = "!invalid-id-format!";
    ContentItem badContentItem =
        new ContentItemImpl(
            invalidUuid,
            null,
            byteSource,
            MediaType.TEXT_PLAIN,
            "invalid-content-item.txt",
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
            MediaType.TEXT_PLAIN,
            "invalid-content-item.txt",
            byteSource.size(),
            mock(Metacard.class));
    UpdateStorageRequest updateRequest =
        new UpdateStorageRequestImpl(Lists.newArrayList(updateContentItem), null);
    UpdateStorageResponse updateResponse = provider.update(updateRequest);

    assertThat(updateResponse.getUpdatedContentItems().size(), is(0));
  }

  @Test
  public void testRead() throws Exception {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");

    assertReadContentItem(
        createStorageResponse.getCreatedContentItems().get(0).getUri(), false, false);
  }

  @Test(expected = StorageException.class)
  public void testReadObjectDoesntExist() throws Exception {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");

    assertReadContentItem(
        createStorageResponse.getCreatedContentItems().get(0).getUri(), false, true);
  }

  @Test
  public void testUpdate() throws IOException, StorageException, URISyntaxException {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");

    String id = createStorageResponse.getCreatedContentItems().get(0).getId();
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(MOCK_UPDATED_FILE_CONTENTS, StandardCharsets.UTF_8);
          }
        };
    ContentItem updateItem =
        new ContentItemImpl(
            id,
            byteSource,
            NITF_MIME_TYPE,
            MOCK_UPDATED_FILENAME,
            MOCK_UPDATED_FILE_CONTENTS.getBytes().length,
            mock(Metacard.class));

    submitAndVerifySuccessfulUpdateStorageRequest(updateItem);
  }

  @Test
  public void testDelete() throws IOException, StorageException {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");

    assertDeletedContentItem(createStorageResponse, false);
  }

  @Test
  public void testDeleteWithQualifier() throws IOException, StorageException {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, QUALIFIER, NITF_MIME_TYPE, "");

    assertDeletedContentItem(createStorageResponse, false);
  }

  @Test
  public void testDeleteInvalidUuid() throws StorageException {
    String uuid = "Invalid UUID";

    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(uuid);
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(Lists.newArrayList(metacard), null);
    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);

    assertThat(deleteResponse.getDeletedContentItems().size(), is(0));
  }

  @Test(expected = StorageException.class)
  public void testDeleteNoObjectFound() throws StorageException, IOException {
    CreateStorageResponse createStorageResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");

    assertDeletedContentItem(createStorageResponse, true);
  }

  @Test
  public void testDeleteWithSimilarIds() throws IOException, StorageException, URISyntaxException {
    CreateStorageResponse createResponse =
        assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, "");
    String id = createResponse.getCreatedContentItems().get(0).getId();
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    String badId = id.substring(0, 6) + uuid.substring(6, uuid.length() - 1);
    boolean hadError = false;
    try {
      CreateStorageResponse badCreateResponse =
          assertCreatedContentItem(MOCK_FILENAME, MOCK_FILE_CONTENTS, "", NITF_MIME_TYPE, badId);
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

    assertEquals(id, item.getId());
    assertThat(item.getFilename(), is(""));
    provider.commit(deleteRequest);

    try {
      assertReadContentItem(createResponse.getCreatedContentItems().get(0).getUri(), true, false);
    } catch (StorageException e) {
      // The item was deleted so it shouldn't have found it
      hadError = true;
    } finally {
      if (!hadError) {
        fail("Read succeeded when it should not have! ");
      }
    }
  }

  @Test
  public void testRollback() throws Exception {
    String id = UUID.randomUUID().toString().replaceAll("-", "");
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(MOCK_FILE_CONTENTS, StandardCharsets.UTF_8);
          }
        };
    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(id);
    ContentItem contentItem =
        new ContentItemImpl(
            id,
            byteSource,
            NITF_MIME_TYPE,
            MOCK_FILENAME,
            MOCK_FILE_CONTENTS.getBytes().length,
            metacard);
    CreateStorageRequest createRequest =
        new CreateStorageRequestImpl(Collections.singletonList(contentItem), null);

    provider.create(createRequest);
    provider.rollback(createRequest);
    provider.commit(createRequest);

    verify(provider.amazonS3, times(0)).listObjectsV2(anyString());
    verify(provider.amazonS3, times(0)).deleteObject(anyString(), anyString());
    verify(provider.amazonS3, times(0)).putObject(any(PutObjectRequest.class));
  }

  public CreateStorageRequest createMockCreateStorageRequest(
      String id, String data, String qualifier, String mimeType, String filename)
      throws IOException {
    String uuid = StringUtils.defaultIfBlank(id, UUID.randomUUID().toString().replaceAll("-", ""));
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return IOUtils.toInputStream(data, StandardCharsets.UTF_8);
          }
        };
    ContentItem contentItem =
        new ContentItemImpl(
            uuid,
            qualifier,
            byteSource,
            mimeType,
            filename,
            byteSource.size(),
            mock(Metacard.class));
    return new CreateStorageRequestImpl(Collections.singletonList(contentItem), null);
  }

  private CreateStorageResponse assertCreatedContentItem(
      String filename, String fileContents, String qualifier, String mimeType, String id)
      throws IOException, StorageException {
    CreateStorageRequest createRequest =
        createMockCreateStorageRequest(id, fileContents, qualifier, NITF_MIME_TYPE, filename);
    CreateStorageResponse createStorageResponse = provider.create(createRequest);

    List<ContentItem> createdContentItems = createStorageResponse.getCreatedContentItems();

    ContentItem createdContentItem =
        createdContentItems.isEmpty() ? null : createdContentItems.get(0);
    assertNotNull(createdContentItem);
    String createdId = createdContentItem.getId();
    assertNotNull(createdId);

    String contentUri = createdContentItem.getUri();
    assertNotNull(contentUri);
    String expectedContentUri =
        ContentItem.CONTENT_SCHEME
            + ":"
            + createdId
            + ((StringUtils.isNotBlank(qualifier)) ? "#" + qualifier : "");
    assertThat(contentUri, equalTo(expectedContentUri));

    assertTrue(createdContentItem.getSize() > 0);
    String createdMimeType = createdContentItem.getMimeTypeRawData().replace(";", "");
    List<String> createdMimeTypeArr = new ArrayList<>(Arrays.asList(createdMimeType.split(" ")));
    List<String> givenMimeTypeArr =
        new ArrayList<>(Arrays.asList(mimeType.replace(";", "").split(" ")));
    assertEquals(createdMimeTypeArr.size(), givenMimeTypeArr.size());
    givenMimeTypeArr.removeAll(createdMimeTypeArr);
    assertThat(givenMimeTypeArr.size(), is(0));

    provider.commit(createRequest);

    verify(provider.amazonS3, times(1))
        .listObjectsV2(eq(MOCK_S3_BUCKET), contains(MOCK_CONTENT_PREFIX_1));
    verify(provider.amazonS3, times(0)).deleteObject(anyString(), anyString());
    verify(provider.amazonS3, times(1)).putObject(any(PutObjectRequest.class));

    return createStorageResponse;
  }

  private void assertReadContentItem(
      String contentItemUriString, boolean getObjectException, boolean listObjectsException)
      throws IOException, StorageException, URISyntaxException {
    URI uriString = new URI(contentItemUriString);

    ReadStorageRequest readRequest = new ReadStorageRequestImpl(uriString, Collections.emptyMap());

    String fullContentPrefix =
        provider.getFullContentPrefix(uriString.getSchemeSpecificPart(), uriString.getFragment());
    initMockRead(fullContentPrefix, MOCK_FILENAME, getObjectException, listObjectsException);

    ReadStorageResponse readResponse = provider.read(readRequest);

    verify(provider.amazonS3, times(1))
        .getObject(MOCK_S3_BUCKET, fullContentPrefix + MOCK_FILENAME);

    ContentItem item = readResponse.getContentItem();

    assertThat(item, notNullValue());
    assertThat(item.getId(), is(uriString.getSchemeSpecificPart()));
    assertThat(item.getFilename(), is(MOCK_FILENAME));
    if (uriString.getFragment() != null) {
      assertThat(item.getQualifier(), is(uriString.getFragment()));
    }
    assertThat(item.getMimeTypeRawData(), is(NITF_MIME_TYPE));
    assertThat(
        IOUtils.toString(item.getInputStream(), StandardCharsets.UTF_8), is(MOCK_FILE_CONTENTS));
  }

  private void submitAndVerifySuccessfulUpdateStorageRequest(ContentItem... requestContentItems)
      throws IOException, StorageException, URISyntaxException {
    final UpdateStorageRequest updateStorageRequest =
        new UpdateStorageRequestImpl(Arrays.asList(requestContentItems), null);
    final UpdateStorageResponse updateStorageResponse = provider.update(updateStorageRequest);

    final List<ContentItem> responseContentItems = updateStorageResponse.getUpdatedContentItems();

    for (final ContentItem responseContentItem : responseContentItems) {
      // assert file exists
      final URI uri = new URI(responseContentItem.getUri());
      assertThat(responseContentItem, notNullValue());
      assertThat(responseContentItem.getId(), is(uri.getSchemeSpecificPart()));
      if (uri.getFragment() != null) {
        assertThat(responseContentItem.getQualifier(), is(uri.getFragment()));
      }
      assertThat(responseContentItem.getMimeTypeRawData(), is(NITF_MIME_TYPE));
      assertThat(responseContentItem.getFilename(), is(MOCK_UPDATED_FILENAME));
      assertThat(
          IOUtils.toString(responseContentItem.getInputStream(), StandardCharsets.UTF_8),
          is(MOCK_UPDATED_FILE_CONTENTS));

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

      String fullContentPrefix = initMockListObjectsV2(responseContentItem.getId(), false);
      provider.commit(updateStorageRequest);

      verify(provider.amazonS3, times(1)).deleteObject(eq(MOCK_S3_BUCKET), eq(fullContentPrefix));
      verify(provider.amazonS3, times(2)).putObject(any(PutObjectRequest.class));
    }

    for (ContentItem responseContentItem : responseContentItems) {
      assertReadContentItem(responseContentItem.getUri(), false, false);
    }
  }

  private void assertDeletedContentItem(
      CreateStorageResponse createStorageResponse, boolean throwExceptionListObjects)
      throws StorageException {
    String id = createStorageResponse.getCreatedContentItems().get(0).getId();
    DeleteStorageRequest deleteRequest =
        new DeleteStorageRequestImpl(
            createStorageResponse
                .getCreatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .collect(Collectors.toList()),
            null);
    when(deleteRequest.getMetacards().get(0).getId()).thenReturn(id);

    DeleteStorageResponse deleteResponse = provider.delete(deleteRequest);
    List<ContentItem> items = deleteResponse.getDeletedContentItems();
    ContentItem item = items.get(0);

    assertEquals(id, item.getId());
    assertThat(item.getFilename(), isEmptyString());

    String fullContentPrefix =
        initMockListObjectsV2(
            deleteRequest.getMetacards().get(0).getId(), throwExceptionListObjects);
    provider.commit(deleteRequest);

    verify(provider.amazonS3, times(1)).deleteObject(MOCK_S3_BUCKET, fullContentPrefix);
  }

  private void initMockRead(
      String contentPrefix,
      String fileName,
      boolean getS3ObjectThrowException,
      boolean isEmptyListObjects) {
    String contentKey = contentPrefix + fileName;
    ListObjectsV2Result listObjectsV2Result = mock(ListObjectsV2Result.class);
    S3ObjectSummary s3ObjectSummary = mock(S3ObjectSummary.class);
    S3Object s3Object = mock(S3Object.class);
    S3ObjectInputStream inputStream =
        new S3ObjectInputStream(new ByteArrayInputStream(MOCK_FILE_CONTENTS.getBytes()), null);
    when(s3ObjectSummary.getKey()).thenReturn(contentKey);
    if (!isEmptyListObjects) {
      when(listObjectsV2Result.getObjectSummaries()).thenReturn(Arrays.asList(s3ObjectSummary));
    } else {
      when(listObjectsV2Result.getObjectSummaries()).thenReturn(Collections.emptyList());
    }
    when(provider.amazonS3.listObjectsV2(eq(MOCK_S3_BUCKET), contains(MOCK_CONTENT_PREFIX_1)))
        .thenReturn(listObjectsV2Result);
    if (!getS3ObjectThrowException) {
      when(provider.amazonS3.getObject(MOCK_S3_BUCKET, contentKey)).thenReturn(s3Object);
    } else {
      when(provider.amazonS3.getObject(MOCK_S3_BUCKET, contentKey))
          .thenThrow(SdkClientException.class);
    }
    when(s3Object.getObjectContent()).thenReturn(inputStream);
  }

  private String initMockListObjectsV2(String id, boolean throwException) {
    String fullContentPrefix = provider.getFullContentPrefix(id, "");
    ListObjectsV2Result listObjectsV2Result = mock(ListObjectsV2Result.class);
    S3ObjectSummary s3ObjectSummary = mock(S3ObjectSummary.class);
    when(s3ObjectSummary.getKey()).thenReturn(fullContentPrefix);
    if (!throwException) {
      when(listObjectsV2Result.getObjectSummaries()).thenReturn(Arrays.asList(s3ObjectSummary));
    } else {
      when(listObjectsV2Result.getObjectSummaries()).thenThrow(SdkClientException.class);
    }
    doReturn(listObjectsV2Result)
        .when(provider.amazonS3)
        .listObjectsV2(eq(MOCK_S3_BUCKET), eq(fullContentPrefix));
    return fullContentPrefix;
  }
}
