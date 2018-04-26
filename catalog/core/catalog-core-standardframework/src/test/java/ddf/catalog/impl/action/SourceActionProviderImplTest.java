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
package ddf.catalog.impl.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.catalog.source.Source;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class SourceActionProviderImplTest {

  private static final String PROVIDER_ID = "id";

  private SourceActionProviderImpl sourceActionProvider;

  @Before
  public void setup() {
    sourceActionProvider = new SourceActionProviderImpl(PROVIDER_ID);
  }

  @Test
  public void testGetAction() throws MalformedURLException {

    String title = "Title";
    String url = "http://wikipedia.org";
    String sourceId = "ddf.distribution";
    String description = "the description";

    sourceActionProvider.setTitle(title);
    sourceActionProvider.setDescription(description);
    sourceActionProvider.setUrl(url);
    sourceActionProvider.setSourceId(sourceId);

    Source source = mock(Source.class);
    when(source.getId()).thenReturn(sourceId);

    Action action = sourceActionProvider.getAction(source);

    assertThat(action.getTitle(), is(title));
    assertThat(action.getUrl(), is(new URL(url)));
    assertThat(action.getDescription(), is(description));
    assertThat(action.getId(), is(PROVIDER_ID));
  }

  /** Test that getAction returns null when it is called with a non-Source object. */
  @Test
  public void testGetActionNonSource() {
    assertThat(sourceActionProvider.getAction("xyz"), nullValue());
  }

  @Test
  public void testGetId() {
    assertThat(sourceActionProvider.getId(), is(PROVIDER_ID));
  }

  @Test
  public void testGetActionWithDifferentSourceId() {

    sourceActionProvider.setSourceId("sourceId");

    Source source = mock(Source.class);
    when(source.getId()).thenReturn("anotherSourceId");

    assertThat(sourceActionProvider.getAction(source), nullValue());
  }
}
