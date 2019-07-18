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
package org.codice.ddf.rest.impl;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.action.Action;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.mime.MimeTypeMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.attachment.impl.AttachmentParserImpl;
import org.codice.ddf.rest.service.CatalogService;
import org.junit.Before;
import org.junit.Test;

public class CatalogServiceImplSourcesTest {

  private static final String SRC_ID_LOCAL = "local";
  private static final String SRC_ID_FED_1 = "fed1";
  private static final String SRC_ID_FED_2 = "fed2";
  private static final String VERSION = "4.0";
  private static final String MIME_TYPE_STRING_JSON = "application/json";

  private static final Set<ContentType> CONTENT_TYPES =
      ImmutableSet.<ContentType>builder()
          .add(new ContentTypeImpl("ct1", "v1"))
          .add(new ContentTypeImpl("ct2", "v2"))
          .add(new ContentTypeImpl("ct3", null))
          .build();

  private AttachmentParser attachmentParser;

  // =============================================================
  //    SETUP TOOLS
  // =============================================================

  @Before
  public void setup() throws Exception {
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");
    attachmentParser = new AttachmentParserImpl(mimeTypeMapper);
  }

  private CatalogService setupCatalogService(Set<SourceDescriptor> descriptors) throws Exception {
    SourceInfoResponse sourceInfoResponse = new SourceInfoResponseImpl(null, null, descriptors);
    CatalogFramework catalogFramework = mock(CatalogFramework.class);
    when(catalogFramework.getSourceInfo(isA(SourceInfoRequestEnterprise.class)))
        .thenReturn(sourceInfoResponse);
    return new CatalogServiceImpl(
        catalogFramework, attachmentParser, mock(AttributeRegistry.class));
  }

  private static SourceDescriptor createSourceDescriptor(
      String sourceId, Set<ContentType> catalogedTypes, List<Action> actions) {
    return createSourceDescriptor(sourceId, catalogedTypes, actions, null);
  }

  private static SourceDescriptor createSourceDescriptor(
      String sourceId, Set<ContentType> catalogedTypes, List<Action> actions, String version) {
    SourceDescriptorImpl descriptor = new SourceDescriptorImpl(sourceId, catalogedTypes, actions);
    descriptor.setAvailable(true);
    if (version != null) {
      descriptor.setVersion(version);
    }
    return descriptor;
  }

  // =============================================================
  //    TESTS
  // =============================================================

  @Test
  public void testGetDocumentIsValidNoSources() throws Exception {
    Set<SourceDescriptor> sourceDescriptors = Collections.emptySet();

    CatalogService catalogService = setupCatalogService(sourceDescriptors);
    BinaryContent content = catalogService.getSourcesInfo();
    assertEquals(MIME_TYPE_STRING_JSON, content.getMimeTypeValue());

    JSONArray srcList = binaryToJsonArray(content);
    assertEquals(0, srcList.size());
  }

  @Test
  public void testGetDocumentSourcesSuccess() throws Exception {
    Set<SourceDescriptor> sourceDescriptors =
        Stream.of(
                createSourceDescriptor(SRC_ID_LOCAL, CONTENT_TYPES, emptyList(), VERSION),
                createSourceDescriptor(SRC_ID_FED_1, CONTENT_TYPES, emptyList(), VERSION),
                createSourceDescriptor(SRC_ID_FED_2, null, emptyList()))
            .collect(Collectors.toSet());

    CatalogService catalogService = setupCatalogService(sourceDescriptors);
    BinaryContent content = catalogService.getSourcesInfo();
    assertEquals(MIME_TYPE_STRING_JSON, content.getMimeTypeValue());

    JSONArray srcList = binaryToJsonArray(content);
    assertEquals(3, srcList.size());

    JSONObject source1 = (JSONObject) srcList.get(0);
    JSONObject source2 = (JSONObject) srcList.get(1);
    JSONObject source3 = (JSONObject) srcList.get(2);
    JSONArray contentTypesInJSON = contentTypesAsJson();

    assertThat(asIterable(source1.get("contentTypes")), hasItems(contentTypesInJSON.toArray()));
    assertEquals(true, source1.get("available"));
    assertEquals(CONTENT_TYPES.size(), ((JSONArray) source1.get("contentTypes")).size());
    assertEquals(VERSION, source1.get("version"));

    assertThat(asIterable(source2.get("contentTypes")), hasItems(contentTypesInJSON.toArray()));
    assertEquals(true, source2.get("available"));
    assertEquals(CONTENT_TYPES.size(), ((JSONArray) source2.get("contentTypes")).size());
    assertEquals(VERSION, source2.get("version"));

    assertThat(asIterable(source3.get("contentTypes")), is(emptyIterable()));
    assertEquals(true, source3.get("available"));
    assertEquals(0, ((JSONArray) source3.get("contentTypes")).size());
    assertEquals("", source3.get("version"));
  }

  // =============================================================
  //    COMMON HELPER METHODS / FUNCTIONS
  // =============================================================

  private JSONArray contentTypesAsJson() {
    JSONArray contentTypesInJSON = new JSONArray();
    for (ContentType ct : CONTENT_TYPES) {
      JSONObject ob = new JSONObject();
      ob.put("name", ct.getName());
      ob.put("version", ct.getVersion() != null ? ct.getVersion() : "");
      contentTypesInJSON.add(ob);
    }
    return contentTypesInJSON;
  }

  private static JSONArray binaryToJsonArray(BinaryContent binaryContent) throws Exception {
    String responseMessage = IOUtils.toString(binaryContent.getInputStream());
    return (JSONArray) new JSONParser().parse(responseMessage);
  }

  private static Iterable<Object> asIterable(Object o) {
    if (!(o instanceof Iterable)) {
      fail("Object was not iterable");
    }
    final Iterable<?> wildcardIterable = (Iterable) o;
    return StreamSupport.stream(wildcardIterable.spliterator(), false)
        .map(Object.class::cast)
        .collect(Collectors.toSet());
  }
}
