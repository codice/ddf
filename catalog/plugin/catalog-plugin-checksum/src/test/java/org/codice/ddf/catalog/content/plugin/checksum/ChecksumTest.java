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
package org.codice.ddf.catalog.content.plugin.checksum;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PluginExecutionException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.checksum.ChecksumProvider;
import org.junit.Before;
import org.junit.Test;

public class ChecksumTest {
  private ChecksumProvider mockChecksumProvider;

  private Checksum checksum;

  private CreateStorageRequest mockCreateRequest;

  private UpdateStorageRequest mockUpdateRequest;

  private static final String SAMPLE_CHECKSUM_ALGORITHM = "MD5";

  private static final String SAMPLE_CHECKSUM_VALUE = "324D54D92B2D97471F9F4624596EA9F5";

  @Before
  public void initialize() throws IOException, NoSuchAlgorithmException {
    mockChecksumProvider = mock(ChecksumProvider.class);

    InputStream inputStream = mock(InputStream.class);

    when(mockChecksumProvider.getChecksumAlgorithm()).thenReturn(SAMPLE_CHECKSUM_ALGORITHM);

    when(mockChecksumProvider.calculateChecksum(inputStream)).thenReturn(SAMPLE_CHECKSUM_VALUE);

    checksum = new Checksum(mockChecksumProvider);

    List<ContentItem> mockContentItems = new ArrayList<>();
    ContentItem mockContentItem = mock(ContentItem.class);
    when(mockContentItem.getInputStream()).thenReturn(inputStream);
    when(mockContentItem.getMetacard()).thenReturn(new MetacardImpl());
    mockContentItems.add(mockContentItem);

    mockCreateRequest = mock(CreateStorageRequest.class);
    when(mockCreateRequest.getContentItems()).thenReturn(mockContentItems);
    mockUpdateRequest = mock(UpdateStorageRequest.class);
    when(mockUpdateRequest.getContentItems()).thenReturn(mockContentItems);
  }

  @Test
  public void testProcessCreateWithValidInput() throws PluginExecutionException {
    CreateStorageRequest request = checksum.process(mockCreateRequest);

    String checksumResult =
        (String)
            request
                .getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.CHECKSUM)
                .getValue();
    String checksumAlgorithm =
        (String)
            request
                .getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.CHECKSUM_ALGORITHM)
                .getValue();
    assertThat(checksumResult, is(SAMPLE_CHECKSUM_VALUE));
    assertThat(checksumAlgorithm, is(SAMPLE_CHECKSUM_ALGORITHM));
  }

  @Test
  public void testProcessUpdateWithValidInput() throws PluginExecutionException {
    UpdateStorageRequest request = checksum.process(mockUpdateRequest);

    String checksumResult =
        (String)
            request
                .getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.CHECKSUM)
                .getValue();
    String checksumAlgorithm =
        (String)
            request
                .getContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.CHECKSUM_ALGORITHM)
                .getValue();
    assertThat(checksumResult, is(SAMPLE_CHECKSUM_VALUE));
    assertThat(checksumAlgorithm, is(SAMPLE_CHECKSUM_ALGORITHM));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProcessCreateWithNullInput() throws PluginExecutionException {
    checksum.process((CreateStorageRequest) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProcessUpdateWithNullInput() throws PluginExecutionException {
    checksum.process((UpdateStorageRequest) null);
  }

  @Test
  public void testProcessCreateChecksumCalculationIOException() throws Exception {
    doThrow(IOException.class).when(mockChecksumProvider).calculateChecksum(any(InputStream.class));

    try {
      checksum.process(mockCreateRequest);
      fail("Checksum plugin should have thrown an exception.");
    } catch (PluginExecutionException e) {
      assertThat(e.getCause(), instanceOf(IOException.class));
    }
  }

  @Test
  public void testProcessUpdateChecksumCalculationIOException() throws Exception {
    doThrow(IOException.class).when(mockChecksumProvider).calculateChecksum(any(InputStream.class));

    try {
      checksum.process(mockUpdateRequest);
      fail("Checksum plugin should have thrown an exception.");
    } catch (PluginExecutionException e) {
      assertThat(e.getCause(), instanceOf(IOException.class));
    }
  }

  @Test
  public void testProcessCreateDerivedContentDoesNotSetAttribute() throws Exception {
    Metacard metacard = mock(Metacard.class);
    ContentItem mockContentItem = mock(ContentItem.class);
    when(mockContentItem.getQualifier()).thenReturn("some-qualifier");
    when(mockContentItem.getMetacard()).thenReturn(metacard);

    List<ContentItem> mockContentItems = new ArrayList<>();
    mockContentItems.add(mockContentItem);

    CreateStorageRequest mockCreateRequest = mock(CreateStorageRequest.class);
    when(mockCreateRequest.getContentItems()).thenReturn(mockContentItems);

    checksum.process(mockCreateRequest);

    verify(metacard, never()).setAttribute(any(Attribute.class));
  }

  @Test
  public void testProcessUpdateDerivedContentDoesNotSetAttribute() throws Exception {
    Metacard metacard = mock(Metacard.class);
    ContentItem mockContentItem = mock(ContentItem.class);
    when(mockContentItem.getQualifier()).thenReturn("some-qualifier");
    when(mockContentItem.getMetacard()).thenReturn(metacard);

    List<ContentItem> mockContentItems = new ArrayList<>();
    mockContentItems.add(mockContentItem);

    UpdateStorageRequest mockUpdateRequest = mock(UpdateStorageRequest.class);
    when(mockUpdateRequest.getContentItems()).thenReturn(mockContentItems);

    checksum.process(mockUpdateRequest);

    verify(metacard, never()).setAttribute(any(Attribute.class));
  }
}
