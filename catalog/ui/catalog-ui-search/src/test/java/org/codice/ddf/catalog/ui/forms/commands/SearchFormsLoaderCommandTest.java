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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoader;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

@RunWith(JUnit4.class)
public class SearchFormsLoaderCommandTest {

  private static final URL LOADER_RESOURCES_DIR =
      SearchFormsLoaderCommandTest.class.getResource("/forms/loader");

  private static final String ROOT = LOADER_RESOURCES_DIR.getPath();

  private static ByteArrayOutputStream buffer;

  private static PrintStream realSystemOut;
  private @Mock SearchFormsLoader mockLoader;

  private @Mock Metacard mockMetacard1;

  private @Mock Metacard mockMetacard2;

  private SearchFormsLoaderCommand cmd =
      new SearchFormsLoaderCommand() {
        @Override
        public SearchFormsLoader generateLoader(
            CatalogFramework cf, EndpointUtil util, String dir, String forms, String results) {
          return mockLoader;
        }
      };

  @BeforeClass
  public static void setupOutput() {
    interceptSystemOut();
  }

  @Before
  public void setup() {
    initMocks(this);
    when(mockMetacard1.getId()).thenReturn("1");
    when(mockMetacard2.getId()).thenReturn("2");
    when(mockMetacard1.getTitle()).thenReturn("title1");
    when(mockMetacard2.getTitle()).thenReturn("title2");

    when(mockLoader.retrieveSystemTemplateMetacards())
        .thenReturn(ImmutableList.of(mockMetacard1, mockMetacard2));
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
  public void testLoadMetacardsFromDirectory() {
    cmd.formsDir = ROOT + "/valid";
    cmd.formsFile = "forms.json";
    cmd.resultsFile = "results.json";

    cmd.executeWithSubject();
    assertThat(this.getOutput(), containsString("Initializing Search Form Template Loader"));
    assertThat(
        this.getOutput(),
        containsString("Loader initialized, beginning ingestion of system templates."));
    assertThat(this.getOutput(), containsString("System templates successfully ingested."));
    verify(mockLoader, times(1)).retrieveSystemTemplateMetacards();
    verify(mockLoader, times(1)).bootstrap(refEq(ImmutableList.of(mockMetacard1, mockMetacard2)));
  }

  @Test
  public void testNoMetacardsRetrieved() {
    when(mockLoader.retrieveSystemTemplateMetacards()).thenReturn(Collections.emptyList());

    cmd.executeWithSubject();
    assertThat(this.getOutput(), containsString("Initializing Search Form Template Loader"));
    assertThat(this.getOutput(), containsString("No system forms to load, halting ingest."));
    verify(mockLoader, times(1)).retrieveSystemTemplateMetacards();
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
