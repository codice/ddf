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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryResponse;
import org.junit.Test;

public class InspectCommandTest extends CommandCatalogFrameworkCommon {

  private static final String VALUE_1 = "value1";
  private static final String VALUE_2 = "value2";
  private static final String VALUE_3 = "value3";

  @Test
  public void testMultivaluedAttr() throws Exception {
    Metacard metacard = getMockMultiValuedMetacard();

    // mock out catalog framework
    QueryResponse mockQueryResponse = mock(QueryResponse.class);
    Result mockResult = mock(Result.class);
    doReturn(metacard).when(mockResult).getMetacard();
    doReturn(ImmutableList.of(mockResult)).when(mockQueryResponse).getResults();

    CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    doReturn(mockQueryResponse).when(mockCatalogFramework).query(any());

    InspectCommand command = new InspectCommand();
    command.catalogFramework = mockCatalogFramework;
    command.id = "id";
    command.filterBuilder = new GeotoolsFilterBuilder();

    command.executeWithSubject();

    String output = consoleOutput.getOutput();

    assertThat(output, containsString(VALUE_1));
    assertThat(output, containsString(VALUE_2));
    assertThat(output, containsString(VALUE_3));
  }

  private Metacard getMockMultiValuedMetacard() {
    // mock attribute descriptor
    AttributeDescriptor mockAD = mock(AttributeDescriptor.class);
    doReturn("multi-valued").when(mockAD).getName();
    doReturn(true).when(mockAD).isMultiValued();

    // mock attribute
    Attribute mockAttr = mock(Attribute.class);
    doReturn("value1").when(mockAttr).getValue();
    doReturn(ImmutableList.of(VALUE_1, VALUE_2, VALUE_3)).when(mockAttr).getValues();

    // mock metacard type
    MetacardType mockMetacardType = mock(MetacardType.class);
    doReturn(ImmutableSet.of(mockAD)).when(mockMetacardType).getAttributeDescriptors();

    // mock metacard
    Metacard mockMetacard = mock(Metacard.class);
    doReturn(mockMetacardType).when(mockMetacard).getMetacardType();
    doReturn("sourceID").when(mockMetacard).getSourceId();
    doReturn(mockAttr).when(mockMetacard).getAttribute("multi-valued");

    return mockMetacard;
  }
}
