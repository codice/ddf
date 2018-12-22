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
package org.codice.ddf.catalog.ui.forms.commands;

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchFormsManageCommandTest {
  private @Mock CatalogFramework catalogFramework;

  private @Mock EndpointUtil endpointUtil;

  private @Mock DeleteResponse deleteResponse;

  private @Mock Metacard mockMetacard1;

  private @Mock Metacard mockMetacard2;

  private static ByteArrayOutputStream buffer;

  private static PrintStream realSystemOut;

  private SearchFormsManageCommand cmd;

  @BeforeClass
  public static void setupOutput() {
    interceptSystemOut();
  }

  @Before
  public void setup() throws SourceUnavailableException, IngestException {
    when(mockMetacard1.getId()).thenReturn("1");
    when(mockMetacard2.getId()).thenReturn("2");
    when(mockMetacard1.getTitle()).thenReturn("title1");
    when(mockMetacard2.getTitle()).thenReturn("title2");
    when(deleteResponse.getDeletedMetacards()).thenReturn(ImmutableList.of(mockMetacard1));
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);
    when(endpointUtil.getMetacardListByTag(SYSTEM_TEMPLATE))
        .thenReturn(ImmutableList.of(mockMetacard1, mockMetacard2));

    cmd = new SearchFormsManageCommand(catalogFramework, endpointUtil);
  }

  @After
  public void resetBuffer() {
    buffer.reset();
  }

  @AfterClass
  public static void closeOutput() throws IOException {
    System.setOut(realSystemOut);
    buffer.close();
  }

  @Test
  public void testLoadingEmptyMetacards() {
    when(endpointUtil.getMetacardListByTag(SYSTEM_TEMPLATE)).thenReturn(Collections.emptyList());
    cmd.massDeletion = true;
    cmd.executeWithSubject();
    assertThat(this.getOutput(), containsString("No system forms exist."));
  }

  @Test
  public void testSingleDeletionDeletesMetacard()
      throws SourceUnavailableException, IngestException {
    cmd.singleDeletion = "1";
    cmd.executeWithSubject();
    List<String> idToDelete = Collections.singletonList("1");
    DeleteRequest mockDeleteReq = new DeleteRequestImpl(idToDelete.toArray(new String[0]));
    verify(catalogFramework).delete(refEq(mockDeleteReq));
    assertThat(this.getOutput(), containsString("[93mDeleted: \u001B[32m1\u001B[m"));
  }

  @Test
  public void testRemovalAllMultiDeletion() throws SourceUnavailableException, IngestException {
    when(deleteResponse.getDeletedMetacards())
        .thenReturn(ImmutableList.of(mockMetacard1, mockMetacard2));
    cmd.massDeletion = true;
    cmd.executeWithSubject();
    List<String> idToDelete = ImmutableList.of("1", "2");
    DeleteRequest mockDeleteReq = new DeleteRequestImpl(idToDelete.toArray(new String[0]));
    verify(catalogFramework).delete(refEq(mockDeleteReq));
    assertThat(this.getOutput(), containsString("\u001B[93mDeleted: \u001B[32m1\u001B[m\n"));
    assertThat(this.getOutput(), containsString("\u001B[93mDeleted: \u001B[32m2\u001B[m\n"));
  }

  @Test
  public void testPrintAllSystemTemplates() {
    cmd.viewAll = true;
    cmd.executeWithSubject();
    assertThat(
        this.getOutput(),
        containsString(
            "\u001B[93mTitle: title1\n"
                + "\u001B[32m\t- 1\u001B[m\n"
                + "\u001B[93mTitle: title2\n"
                + "\u001B[32m\t- 2\u001B[m\n"));
  }

  private static void interceptSystemOut() {
    realSystemOut = System.out;
    buffer = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buffer));
  }

  public String getOutput() {
    return buffer.toString();
  }
}
