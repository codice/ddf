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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceKeyTransformation;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class WorkspaceTransformerImplTest {
  private static final String KEY_TRANSFORMATION_JSON_KEY = "jsonKey";

  private static final String KEY_TRANSFORMATION_METACARD_KEY = "metacardKey";

  private static final Serializable KEY_TRANSFORMATION_VALUE = 15.5;

  private static final WorkspaceKeyTransformation KEY_TRANSFORMATION =
      new WorkspaceKeyTransformation() {
        @Override
        public String getMetacardKey() {
          return KEY_TRANSFORMATION_METACARD_KEY;
        }

        @Override
        public String getJsonKey() {
          return KEY_TRANSFORMATION_JSON_KEY;
        }
      };

  private static final String VALUE_TRANSFORMATION_KEY = "targetKey";

  private static final int VALUE_TRANSFORMATION_METACARD_VALUE = 10;

  private static final double VALUE_TRANSFORMATION_JSON_VALUE = 10.5;

  private static final WorkspaceValueTransformation<Integer, Double> VALUE_TRANSFORMATION =
      new WorkspaceValueTransformation<Integer, Double>() {
        @Override
        public String getKey() {
          return VALUE_TRANSFORMATION_KEY;
        }

        @Override
        public Class<Integer> getMetacardValueType() {
          return Integer.class;
        }

        @Override
        public Class<Double> getJsonValueType() {
          return Double.class;
        }

        @Override
        public Optional<Double> metacardValueToJsonValue(
            WorkspaceTransformer transformer, Integer metacardValue, Metacard workspaceMetacard) {
          return Optional.of(VALUE_TRANSFORMATION_JSON_VALUE);
        }

        @Override
        public Optional<Integer> jsonValueToMetacardValue(
            WorkspaceTransformer transformer, Double jsonValue) {
          return Optional.of(VALUE_TRANSFORMATION_METACARD_VALUE);
        }
      };

  private static final String UNTRANSFORMED_KEY = "justAKey";

  private static final String UNTRANSFORMED_VALUE = "justAValue";

  private static final String METACARD_KEY_TO_REMOVE = "unwantedMetacardKey";

  private static final WorkspaceValueTransformation<Object, Object> METACARD_ATTRIBUTE_REMOVER =
      new WorkspaceValueTransformation<Object, Object>() {
        @Override
        public String getKey() {
          return METACARD_KEY_TO_REMOVE;
        }

        @Override
        public Class<Object> getMetacardValueType() {
          return Object.class;
        }

        @Override
        public Class<Object> getJsonValueType() {
          return Object.class;
        }

        @Override
        public Optional<Object> metacardValueToJsonValue(
            WorkspaceTransformer transformer, Object metacardValue, Metacard workspaceMetacard) {
          return Optional.empty(); // remove the metacard attribute
        }

        @Override
        public Optional<Object> jsonValueToMetacardValue(
            WorkspaceTransformer transformer, Object jsonValue) {
          return Optional.of(jsonValue);
        }
      };

  private static final String JSON_KEY_TO_REMOVE = "unwantedJsonKey";

  private static final WorkspaceValueTransformation<Object, Object> JSON_KEY_VALUE_REMOVER =
      new WorkspaceValueTransformation<Object, Object>() {
        @Override
        public String getKey() {
          return JSON_KEY_TO_REMOVE;
        }

        @Override
        public Class<Object> getMetacardValueType() {
          return Object.class;
        }

        @Override
        public Class<Object> getJsonValueType() {
          return Object.class;
        }

        @Override
        public Optional<Object> metacardValueToJsonValue(
            WorkspaceTransformer transformer, Object metacardValue, Metacard workspaceMetacard) {
          return Optional.of(metacardValue);
        }

        @Override
        public Optional<Object> jsonValueToMetacardValue(
            WorkspaceTransformer transformer, Object jsonValue) {
          return Optional.empty(); // remove the JSON key-value pair
        }
      };

  private static final MetacardType DUMMY_METACARD_TYPE =
      new MetacardTypeImpl(
          "workspace-transformer-test-type",
          ImmutableSet.<AttributeDescriptor>builder()
              .add(
                  dummyAttributeDescriptor(KEY_TRANSFORMATION_METACARD_KEY, BasicTypes.DOUBLE_TYPE))
              .add(dummyAttributeDescriptor(VALUE_TRANSFORMATION_KEY, BasicTypes.INTEGER_TYPE))
              .add(dummyAttributeDescriptor(UNTRANSFORMED_KEY, BasicTypes.STRING_TYPE))
              .add(dummyAttributeDescriptor(METACARD_KEY_TO_REMOVE, BasicTypes.STRING_TYPE))
              .build());

  private WorkspaceTransformer workspaceTransformer;

  private MetacardImpl metacard;

  private static AttributeDescriptor dummyAttributeDescriptor(String name, AttributeType<?> type) {
    return new AttributeDescriptorImpl(name, false, true, false, false, type);
  }

  @Before
  public void setUp() throws CatalogTransformerException, IOException {
    final CatalogFramework mockCatalogFramework = Mockito.mock(CatalogFramework.class);

    doReturn(new BinaryContentImpl(IOUtils.toInputStream("<xml></xml>", Charset.defaultCharset())))
        .when(mockCatalogFramework)
        .transform(any(Metacard.class), any(String.class), any(Map.class));

    final InputTransformer mockInputTransformer = Mockito.mock(InputTransformer.class);

    doReturn(new MetacardImpl(DUMMY_METACARD_TYPE))
        .when(mockInputTransformer)
        .transform(any(InputStream.class));

    final EndpointUtil mockEndpointUtils = Mockito.mock(EndpointUtil.class);
    when(mockEndpointUtils.convertDateEntries(any(Map.Entry.class)))
        .then(invocation -> invocation.getArgument(0));

    metacard = new MetacardImpl(DUMMY_METACARD_TYPE);

    workspaceTransformer =
        new WorkspaceTransformerImpl(
            mockCatalogFramework,
            mockInputTransformer,
            mockEndpointUtils,
            ImmutableList.of(
                KEY_TRANSFORMATION,
                VALUE_TRANSFORMATION,
                METACARD_ATTRIBUTE_REMOVER,
                JSON_KEY_VALUE_REMOVER));
  }

  @Test
  public void testMetacardToMapDirectMapping() {
    metacard.setAttribute(UNTRANSFORMED_KEY, UNTRANSFORMED_VALUE);
    final Map<String, Object> json = workspaceTransformer.transform(metacard);
    assertThat(json, hasEntry(UNTRANSFORMED_KEY, UNTRANSFORMED_VALUE));
  }

  @Test
  public void testMetacardToMapRemapKeys() {
    metacard.setAttribute(KEY_TRANSFORMATION_METACARD_KEY, KEY_TRANSFORMATION_VALUE);
    final Map<String, Object> json = workspaceTransformer.transform(metacard);
    assertThat(json, not(hasKey(KEY_TRANSFORMATION_METACARD_KEY)));
    assertThat(json, hasEntry(KEY_TRANSFORMATION_JSON_KEY, KEY_TRANSFORMATION_VALUE));
  }

  @Test
  public void testMetacardToMapRemapValues() {
    metacard.setAttribute(VALUE_TRANSFORMATION_KEY, VALUE_TRANSFORMATION_METACARD_VALUE);
    final Map<String, Object> json = workspaceTransformer.transform(metacard);
    assertThat(json, hasEntry(VALUE_TRANSFORMATION_KEY, VALUE_TRANSFORMATION_JSON_VALUE));
  }

  @Test
  public void testMetacardAttributeRemoval() {
    metacard.setAttribute(METACARD_KEY_TO_REMOVE, KEY_TRANSFORMATION_VALUE);
    final Map<String, Object> json = workspaceTransformer.transform(metacard);
    assertThat(json, not(hasKey(METACARD_KEY_TO_REMOVE)));
  }

  @Test
  public void testMapToMetacardDirectMapping() {
    workspaceTransformer.transformIntoMetacard(
        ImmutableMap.of(UNTRANSFORMED_KEY, UNTRANSFORMED_VALUE), metacard);
    assertThat(metacard.getAttribute(UNTRANSFORMED_KEY), notNullValue());
    assertThat(metacard.getAttribute(UNTRANSFORMED_KEY).getValue(), is(UNTRANSFORMED_VALUE));
  }

  @Test
  public void testMapToMetacardRemapKeys() {
    workspaceTransformer.transformIntoMetacard(
        ImmutableMap.of(KEY_TRANSFORMATION_JSON_KEY, KEY_TRANSFORMATION_VALUE), metacard);
    assertThat(metacard.getAttribute(KEY_TRANSFORMATION_METACARD_KEY), notNullValue());
    assertThat(
        metacard.getAttribute(KEY_TRANSFORMATION_METACARD_KEY).getValue(),
        is(KEY_TRANSFORMATION_VALUE));
  }

  @Test
  public void testMapToMetacardRemapValues() {
    workspaceTransformer.transformIntoMetacard(
        ImmutableMap.of(VALUE_TRANSFORMATION_KEY, VALUE_TRANSFORMATION_JSON_VALUE), metacard);
    assertThat(metacard.getAttribute(VALUE_TRANSFORMATION_KEY), notNullValue());
    assertThat(
        metacard.getAttribute(VALUE_TRANSFORMATION_KEY).getValue(),
        is(VALUE_TRANSFORMATION_METACARD_VALUE));
  }

  @Test
  public void testMapKeyRemoval() {
    workspaceTransformer.transformIntoMetacard(
        ImmutableMap.of(JSON_KEY_TO_REMOVE, KEY_TRANSFORMATION_VALUE), metacard);
    assertThat(metacard.getAttribute(JSON_KEY_TO_REMOVE), nullValue());
  }
}
