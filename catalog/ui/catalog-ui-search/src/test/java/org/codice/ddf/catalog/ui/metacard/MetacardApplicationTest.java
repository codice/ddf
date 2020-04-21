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
package org.codice.ddf.catalog.ui.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.UpdateRequest;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.ws.rs.NotFoundException;
import org.codice.ddf.catalog.ui.metacard.edit.AttributeChange;
import org.codice.ddf.catalog.ui.metacard.edit.MetacardChanges;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

public class MetacardApplicationTest {
  private static final String ID = "000000000";

  private static final String TITLE_A = "Title A";

  private static final String TITLE_B = "Title B";

  private static final String DATE_A = "2018-12-10T13:09:40Z";

  private static final String DATE_B = "2018-12-10T13:08:40Z";

  private final CatalogFramework mockFramework = mock(CatalogFramework.class);

  private final EndpointUtil mockUtil = mock(EndpointUtil.class);

  private final MetacardApplicationUnderTest app =
      new MetacardApplicationUnderTest(mockFramework, mockUtil);

  @Test(expected = NotFoundException.class)
  public void testPatchMetacardsWhenIdNotFound() throws Exception {
    doReturn(Collections.emptyMap()).when(mockUtil).getMetacardsWithTagById(any(), eq("*"));
    app.doPatchMetacards(generateTitleChange());
  }

  @Test
  public void testPatchMetacardsWhenAttributeIsString() throws Exception {
    ArgumentCaptor<UpdateRequest> requestCaptor = ArgumentCaptor.forClass(UpdateRequest.class);
    when(mockFramework.update(requestCaptor.capture())).thenReturn(null);
    doReturn(generateCatalogStateWithTitle())
        .when(mockUtil)
        .getMetacardsWithTagById(any(), eq("*"));

    app.doPatchMetacards(generateTitleChange());

    Metacard metacard = requestCaptor.getValue().getUpdates().get(0).getValue();
    assertThat(metacard.getId(), is(ID));
    assertThat(metacard.getTitle(), is(TITLE_B));
  }

  @Test
  public void testPatchMetacardsWhenAttributeIsDate() throws Exception {
    ArgumentCaptor<UpdateRequest> requestCaptor = ArgumentCaptor.forClass(UpdateRequest.class);
    when(mockFramework.update(requestCaptor.capture())).thenReturn(null);
    doReturn(generateCatalogStateWithCreatedDate())
        .when(mockUtil)
        .getMetacardsWithTagById(any(), eq("*"));
    doAnswer(MetacardApplicationTest::doParseDate).when(mockUtil).parseDate(any());

    app.doPatchMetacards(generateCreatedDateChange());

    Metacard metacard = requestCaptor.getValue().getUpdates().get(0).getValue();
    assertThat(metacard.getId(), is(ID));
    assertThat(metacard.getCreatedDate(), is(Date.from(Instant.parse(DATE_B))));
  }

  private static List<MetacardChanges> generateTitleChange() {
    return generateChangeTestData(
        attributeChange -> {
          attributeChange.setAttribute(Core.TITLE);
          attributeChange.setValues(Collections.singletonList(TITLE_B));
        });
  }

  private static List<MetacardChanges> generateCreatedDateChange() {
    return generateChangeTestData(
        attributeChange -> {
          attributeChange.setAttribute(Core.CREATED);
          attributeChange.setValues(Collections.singletonList(DATE_B));
        });
  }

  private static Map<String, Result> generateCatalogStateWithTitle() {
    return generateCatalogState(metacard -> metacard.setTitle(TITLE_A));
  }

  private static Map<String, Result> generateCatalogStateWithCreatedDate() {
    return generateCatalogState(
        metacard -> metacard.setCreatedDate(Date.from(Instant.parse(DATE_A))));
  }

  private static List<MetacardChanges> generateChangeTestData(Consumer<AttributeChange> config) {
    MetacardChanges metacardChanges = new MetacardChanges();
    AttributeChange attributeChange = new AttributeChange();

    config.accept(attributeChange);

    metacardChanges.setIds(Collections.singletonList(ID));
    metacardChanges.setAttributes(Collections.singletonList(attributeChange));

    return Collections.singletonList(metacardChanges);
  }

  private static Map<String, Result> generateCatalogState(Consumer<MetacardImpl> config) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(ID);

    config.accept(metacard);

    Result result = new ResultImpl(metacard);
    return Collections.singletonMap(ID, result);
  }

  private static Instant doParseDate(InvocationOnMock in) {
    Serializable arg = in.getArgumentAt(0, Serializable.class);
    if (!(arg instanceof String)) {
      fail("Only testing with string inputs but got something else, " + arg);
    }
    return Instant.parse((String) arg);
  }

  /**
   * Test class that exposes the protected {@link MetacardApplication#patchMetacards(List, String)}.
   *
   * <p>Note the original method returns an UpdateResponse but we're not testing the Catalog
   * Framework's ability to return a good response; we're testing the app's ability to correctly
   * write to the framework, so this return value is meaningless to propagate.
   */
  private class MetacardApplicationUnderTest extends MetacardApplication {
    private MetacardApplicationUnderTest(
        CatalogFramework catalogFramework, EndpointUtil endpointUtil) {
      super(
          catalogFramework,
          null,
          endpointUtil,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    private void doPatchMetacards(List<MetacardChanges> metacardChanges) throws Exception {
      patchMetacards(metacardChanges, null);
    }
  }
}
