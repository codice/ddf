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
package org.codice.ddf.spatial.ogc.csw.catalog.common.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.transform.InputTransformer;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class TransformerManagerTest {

  private static List<ServiceReference> serviceReferences = new ArrayList<>();

  private static InputTransformer transformerA = mock(InputTransformer.class);

  private static InputTransformer transformerB = mock(InputTransformer.class);

  private static final String ID_A = "idA";

  private static final String ID_B = "idB";

  private static final String MIME_TYPE_A = "mimeTypeA";

  private static final String MIME_TYPE_B = "mimeTypeB";

  private static final String SCHEMA_A = "urn:example:schema:A";

  private static final String SCHEMA_B = "urn:example:schema:B";

  private static final String SCHEMA_LOCATION_A = "schema/locationA";

  private static final String SCHEMA_LOCATION_B = "schema/locationB";

  private static BundleContext mockContext = mock(BundleContext.class);

  private static class MockTransformerManager extends TransformerManager {
    public MockTransformerManager(List<ServiceReference> serviceReferences) {
      super(serviceReferences);
    }

    @Override
    protected BundleContext getBundleContext() {
      return mockContext;
    }
  }

  private static MockTransformerManager manager;

  @BeforeClass
  public static void setUp() {
    ServiceReference serviceRefA = mock(ServiceReference.class);
    ServiceReference serviceRefB = mock(ServiceReference.class);
    when(serviceRefA.getPropertyKeys())
        .thenReturn(
            new String[] {
              TransformerManager.ID,
              TransformerManager.SCHEMA,
              TransformerManager.MIME_TYPE,
              TransformerManager.SCHEMA_LOCATION
            });
    when(serviceRefA.getProperty(TransformerManager.ID)).thenReturn(ID_A);
    when(serviceRefA.getProperty(TransformerManager.MIME_TYPE)).thenReturn(MIME_TYPE_A);
    when(serviceRefA.getProperty(TransformerManager.SCHEMA)).thenReturn(SCHEMA_A);
    when(serviceRefA.getProperty(TransformerManager.SCHEMA_LOCATION)).thenReturn(SCHEMA_LOCATION_A);
    when(serviceRefB.getPropertyKeys())
        .thenReturn(
            new String[] {
              TransformerManager.ID,
              TransformerManager.SCHEMA,
              TransformerManager.MIME_TYPE,
              TransformerManager.SCHEMA_LOCATION
            });
    when(serviceRefB.getProperty(TransformerManager.ID)).thenReturn(ID_B);
    when(serviceRefB.getProperty(TransformerManager.MIME_TYPE)).thenReturn(MIME_TYPE_B);
    when(serviceRefB.getProperty(TransformerManager.SCHEMA)).thenReturn(SCHEMA_B);
    when(serviceRefB.getProperty(TransformerManager.SCHEMA_LOCATION)).thenReturn(SCHEMA_LOCATION_B);
    serviceReferences.add(serviceRefA);
    serviceReferences.add(serviceRefB);

    when(mockContext.getService(serviceRefA)).thenReturn(transformerA);
    when(mockContext.getService(serviceRefB)).thenReturn(transformerB);
    manager = new MockTransformerManager(serviceReferences);
  }

  @Test
  public void testGetAvailableMimeTypes() throws Exception {
    List<String> mimeTypes = manager.getAvailableMimeTypes();
    assertThat(mimeTypes, hasItems(MIME_TYPE_A, MIME_TYPE_B));
  }

  @Test
  public void testGetAvailableSchemas() throws Exception {
    List<String> schemas = manager.getAvailableSchemas();
    assertThat(schemas, hasItems(SCHEMA_A, SCHEMA_B));
  }

  @Test
  public void testGetAvailableIds() throws Exception {
    List<String> schemas = manager.getAvailableIds();
    assertThat(schemas, hasItems(ID_A, ID_B));
  }

  @Test
  public void testGetTransformerBySchema() throws Exception {
    assertThat(manager.getTransformerBySchema(SCHEMA_A), is(transformerA));
    assertThat(manager.getTransformerBySchema(SCHEMA_B), is(transformerB));
  }

  @Test
  public void testGetTransformerByMimeType() throws Exception {
    assertThat(manager.getTransformerByMimeType(MIME_TYPE_A), is(transformerA));
    assertThat(manager.getTransformerByMimeType(MIME_TYPE_B), is(transformerB));
  }

  @Test
  public void testGetTransformerById() throws Exception {
    assertThat(manager.getTransformerById(ID_A), is(transformerA));
    assertThat(manager.getTransformerById(ID_B), is(transformerB));
  }

  @Test
  public void testGetTransformerByNullValue() throws Exception {
    assertThat(manager.getTransformerByMimeType(null), is((String) null));
    assertThat(manager.getTransformerBySchema(null), is((String) null));
    assertThat(manager.getTransformerById(null), is((String) null));
  }

  @Test
  public void testGetTransformerByInvalidValue() throws Exception {
    assertThat(manager.getTransformerByMimeType("abc123"), is((String) null));
    assertThat(manager.getTransformerBySchema("abc123"), is((String) null));
    assertThat(manager.getTransformerById("abc123"), is((String) null));
  }

  @Test
  public void testGetTransformerIdForSchema() throws Exception {
    assertThat(manager.getTransformerIdForSchema(SCHEMA_A), is(ID_A));
    assertThat(manager.getTransformerIdForSchema(SCHEMA_B), is(ID_B));
  }

  @Test
  public void testGetTransformerSchemaForId() throws Exception {
    assertThat(manager.getTransformerSchemaForId(ID_A), is(SCHEMA_A));
    assertThat(manager.getTransformerSchemaForId(ID_B), is(SCHEMA_B));
  }

  @Test
  public void testGetTransformerSchemaForInvalidId() throws Exception {
    assertThat(manager.getTransformerSchemaForId("abc123"), is(""));
  }

  @Test
  public void testGetTransformerIdForInvalidSchema() throws Exception {
    assertThat(manager.getTransformerIdForSchema("abc123"), is(""));
  }

  @Test
  public void testGetTransformerSchemaLocationForId() throws Exception {
    assertThat(manager.getTransformerSchemaLocationForId(ID_A), is(SCHEMA_LOCATION_A));
    assertThat(manager.getTransformerSchemaLocationForId(ID_B), is(SCHEMA_LOCATION_B));
  }

  @Test
  public void testGetTransformerSchemaLocationForInvalidId() throws Exception {
    assertThat(manager.getTransformerSchemaLocationForId("abc123"), is(""));
  }
}
