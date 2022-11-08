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
package org.codice.ddf.ui.search;

import static ddf.catalog.data.types.Core.METACARD_TAGS;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class SearchTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private ServletConfig servletConfig;

  @Mock private ServletContext servletContext;

  @Mock private CatalogFramework catalogFramework;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder filterBuilder;

  private Search search;

  private final StringWriter responseBody = new StringWriter();

  private final Map<String, Object> requestAttributes = new HashMap<>();

  @Before
  public void setUp() throws Exception {
    Catalog catalog = new Catalog(catalogFramework, filterBuilder);
    search = new Search(catalog);
    search.init(servletConfig);
    search.setBackground("BLUE");
    search.setColor("YELLOW");
    search.setHeader("TEST HEADER");
    search.setFooter("TEST FOOTER");

    doAnswer(this::setAttribute).when(request).setAttribute(anyString(), any());
    when(request.getAttribute(anyString())).thenAnswer(this::getAttribute);
    when(request.getLocale()).thenReturn(ENGLISH);
    when(servletConfig.getServletContext()).thenReturn(servletContext);
    when(request.getContextPath()).thenReturn("");
    when(request.getServletPath()).thenReturn("/");
    when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    when(response.encodeURL(anyString())).thenAnswer(this::encodeUrl);
  }

  private Object setAttribute(final InvocationOnMock invocation) throws Throwable {
    final String key = (String) invocation.getArguments()[0];
    final Object value = invocation.getArguments()[1];
    if (value == null) {
      requestAttributes.remove(key);
    } else {
      requestAttributes.put(key, value);
    }
    return null;
  }

  private Object getAttribute(final InvocationOnMock invocation) throws Throwable {
    final String key = (String) invocation.getArguments()[0];
    return requestAttributes.get(key);
  }

  private Object encodeUrl(final InvocationOnMock invocation) throws Throwable {
    return (String) invocation.getArguments()[0];
  }

  @Test
  public void invalidFile() throws Exception {
    when(request.getServletPath()).thenReturn("/unknown");

    search.doGet(request, response);

    verify(response).setStatus(404);
  }

  @Test
  public void invalidPath() throws Exception {
    when(request.getServletPath()).thenReturn("/unknown/foo.css");

    search.doGet(request, response);

    verify(response).setStatus(404);
  }

  @Test
  public void emptyCssFilename() throws Exception {
    when(request.getServletPath()).thenReturn("/.css");

    search.doGet(request, response);

    verify(response).setStatus(400);
  }

  @Test
  public void validCss() throws Exception {
    when(request.getServletPath()).thenReturn("/banner.css");

    search.doGet(request, response);

    verify(response).setContentType("text/css;charset=UTF-8");
    assertThat(responseBody.toString(), containsString(".banner"));
  }

  @Test
  public void metacardPage() throws Exception {
    setIdParameter(request, "123");
    MetacardImpl metacard = createTestMetacard();
    when(catalogFramework.query(any()))
        .thenReturn(new QueryResponseImpl(null, List.of(new ResultImpl(metacard)), true, 1));

    search.doGet(request, response);

    verify(response).setContentType("text/html;charset=UTF-8");

    String body = responseBody.toString();
    assertThat(body, containsString("TEST HEADER"));
    assertThat(body, containsString("TEST FOOTER"));
    assertThat(body, containsString("<title>Test Title</title>"));
    assertThat(body, containsString("tag1, tag2"));
    assertThat(body, containsString("3 bytes"));
    assertThat(body, containsString("/services/catalog/sources/local/123?transform=resource"));
    assertThat(body, containsString("/services/catalog/sources/local/123?transform=thumbnail"));
  }

  @Test
  public void metacardPageWithBlankId() throws Exception {
    setIdParameter(request, "  ");

    search.doGet(request, response);

    assertThat(responseBody.toString(), containsString("<title>Not Found</title>"));
  }

  @Test
  public void metacardPageWithNoMatchingMetacards() throws Exception {
    setIdParameter(request, "123");
    when(catalogFramework.query(any()))
        .thenReturn(new QueryResponseImpl(null, Collections.emptyList(), true, 1));

    search.doGet(request, response);

    assertThat(responseBody.toString(), containsString("<title>Not Found</title>"));
  }

  @Test
  public void metacardPageWithException() throws Exception {
    setIdParameter(request, "123");
    when(catalogFramework.query(any()))
        .thenThrow(new SourceUnavailableException("Local source unavailable"));

    search.doGet(request, response);

    String body = responseBody.toString();
    assertThat(body, containsString("<li>Local source unavailable</li>"));
    assertThat(body, containsString("<title>Not Found</title>"));
  }

  @Test
  public void searchPage() throws Exception {
    when(request.getParameter("q")).thenReturn("test");
    when(request.getParameter("start")).thenReturn("101");
    when(request.getParameter("sort")).thenReturn("relevance");
    when(request.getParameter("past")).thenReturn("year");
    when(request.getParameterMap()).thenReturn(Map.of("q", new String[] {"test"}));
    MetacardImpl metacard1 = createTestMetacard();
    MetacardImpl metacard2 = new MetacardImpl();
    metacard2.setId("456");
    when(catalogFramework.query(any()))
        .thenReturn(
            new QueryResponseImpl(
                null, List.of(new ResultImpl(metacard1), new ResultImpl(metacard2)), true, 102));

    search.doGet(request, response);

    verify(response).setContentType("text/html;charset=UTF-8");

    String body = responseBody.toString();
    assertThat(body, containsString("TEST HEADER"));
    assertThat(body, containsString("TEST FOOTER"));

    assertThat(body, containsString("<input type=\"hidden\" name=\"start\" value=\"1\" />"));
    assertThat(body, containsString("name=\"q\" value=\"test\""));
    assertThat(body, containsString("<option value=\"relevance\" selected=\"selected\">"));
    assertThat(body, containsString("<option value=\"year\" selected=\"selected\">"));

    assertThat(body, containsString("<p>Total results: <span>102</span></p>"));
    assertThat(body, containsString("<td><a href=\"/?id=123\">Test Title</a></td>"));
    assertThat(body, containsString("<td>local</td>"));

    assertThat(body, containsString("<td><a href=\"/?id=456\">No Title</a></td>"));
    assertThat(body, containsString("<td>Unknown Source</td>"));

    assertThat(body, not(containsString("disabled=\"disabled\">Previous")));
    assertThat(body, containsString("<input type=\"hidden\" name=\"start\" value=\"51\" />"));
    assertThat(body, containsString("disabled=\"disabled\">Next"));
  }

  @Test
  public void searchPageWithException() throws Exception {
    when(request.getParameter("q")).thenReturn("test");
    when(catalogFramework.query(any()))
        .thenThrow(new SourceUnavailableException("Local source unavailable"));

    search.doGet(request, response);

    assertThat(responseBody.toString(), containsString("<li>Local source unavailable</li>"));
  }

  @Test
  public void bannerCss() throws Exception {
    when(request.getServletPath()).thenReturn("/banner.css");

    search.doGet(request, response);

    verify(response).setContentType("text/css;charset=UTF-8");

    String body = responseBody.toString();
    assertThat(body, containsString("color: YELLOW;"));
    assertThat(body, containsString("background: BLUE;"));
  }

  private void setIdParameter(HttpServletRequest request, String id) {
    when(request.getParameter("id")).thenReturn(id);
    when(request.getParameterMap()).thenReturn(Map.of("id", new String[] {id}));
  }

  private MetacardImpl createTestMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle("Test Title");
    metacard.setId("123");
    metacard.setSourceId("local");
    metacard.setAttribute(METACARD_TAGS, (Serializable) List.of("tag1", "tag2"));
    metacard.setThumbnail(new byte[] {1, 2, 3});
    metacard.setResourceURI(URI.create("/"));
    return metacard;
  }
}
