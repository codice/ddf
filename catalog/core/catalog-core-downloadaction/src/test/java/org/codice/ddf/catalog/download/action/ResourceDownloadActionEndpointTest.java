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
package org.codice.ddf.catalog.download.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import javax.management.MBeanException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codice.ddf.catalog.resource.download.DownloadToLocalSiteException;
import org.codice.ddf.catalog.resource.download.ResourceDownloadMBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDownloadActionEndpointTest {

  private static final String METACARD_ID = "57a4b894e13a455b8cccb87cec778b58";

  private static final String SOURCE_ID = "ddf.distribution";

  private static final String RESPONSE_HTML_TEMPLATE = "response";

  private static final String SUCCESS_MESSAGE = "Download of resource started successfully";

  private static final String FAILURE_MESSAGE = "Failed to start download of resource";

  private static final String HTML_TEMPLATE = "<!DOCTYPE html><html><body><p>%s</p></body></html>";

  @Mock private ResourceDownloadMBean mockResourceDownloadMBeanProxy;

  @Mock private Handlebars mockHandlebars;

  @Mock private Template mockTemplate;

  @Test
  public void testDownloadToLocalSiteMBeanProxyThrowsException() throws Exception {
    // Setup
    setupMockTemplate(null, FAILURE_MESSAGE);
    setupMockHandlebars(null);
    setupMockResourceDownloadMBeanWithException(
        getDownloadToLocalSiteExceptionWrappedInMBeanException());
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    Response response = resourceDownloadEndpoint.copyToLocalSite(SOURCE_ID, METACARD_ID);

    assertThat(response.getEntity(), is(String.format(HTML_TEMPLATE, FAILURE_MESSAGE)));
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(mockResourceDownloadMBeanProxy).copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = DownloadToLocalSiteException.class)
  public void testDownloadToLocalSiteNullSourceId() throws Exception {
    // Setup
    setupMockTemplate(null, SUCCESS_MESSAGE);
    setupMockHandlebars(null);
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    resourceDownloadEndpoint.copyToLocalSite(null, METACARD_ID);
  }

  @Test(expected = DownloadToLocalSiteException.class)
  public void testDownloadToLocalSiteNullMetacardId() throws Exception {
    // Setup
    setupMockTemplate(null, SUCCESS_MESSAGE);
    setupMockHandlebars(null);
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    resourceDownloadEndpoint.copyToLocalSite(SOURCE_ID, null);
  }

  @Test
  public void testDownloadToLocalSite() throws Exception {
    // Setup
    setupMockTemplate(null, SUCCESS_MESSAGE);
    setupMockHandlebars(null);
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    Response response = resourceDownloadEndpoint.copyToLocalSite(SOURCE_ID, METACARD_ID);

    assertThat(response.getEntity(), is(String.format(HTML_TEMPLATE, SUCCESS_MESSAGE)));
    assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    verify(mockResourceDownloadMBeanProxy).copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test
  public void testDownloadToLocalSiteUnexpectedTargetExceptionWrappedInMBeanException()
      throws Exception {
    // Setup
    setupMockTemplate(null, FAILURE_MESSAGE);
    setupMockHandlebars(null);
    setupMockResourceDownloadMBeanWithException(new MBeanException(new RuntimeException(), ""));
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    Response response = resourceDownloadEndpoint.copyToLocalSite(SOURCE_ID, METACARD_ID);

    assertThat(response.getEntity(), is(String.format(HTML_TEMPLATE, FAILURE_MESSAGE)));
    assertThat(response.getStatus(), is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    verify(mockResourceDownloadMBeanProxy).copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = DownloadToLocalSiteException.class)
  public void testDownloadToLocalSiteFailsToGenerateHtmlPage() throws Exception {
    // Setup
    setupMockTemplate(IOException.class, SUCCESS_MESSAGE);
    setupMockHandlebars(null);
    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();

    // Perform Test
    Response response = resourceDownloadEndpoint.copyToLocalSite(SOURCE_ID, METACARD_ID);

    verify(mockResourceDownloadMBeanProxy).copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = RuntimeException.class)
  public void testResourceDownloadActionEndpointCreationFails() throws Exception {
    // Setup
    setupMockTemplate(null, SUCCESS_MESSAGE);
    setupMockHandlebars(IOException.class);

    ResourceDownloadActionEndpoint resourceDownloadEndpoint =
        createResourceDownloadActionEndpoint();
  }

  private ResourceDownloadActionEndpoint createResourceDownloadActionEndpoint() {
    return new ResourceDownloadActionEndpoint(mockHandlebars) {
      @Override
      ResourceDownloadMBean createResourceDownloadMBeanProxy() {
        return mockResourceDownloadMBeanProxy;
      }
    };
  }

  private void setupMockResourceDownloadMBeanWithException(Exception exception)
      throws MBeanException {
    doThrow(exception).when(mockResourceDownloadMBeanProxy).copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  private MBeanException getDownloadToLocalSiteExceptionWrappedInMBeanException() {
    String message = "exception message";
    DownloadToLocalSiteException downloadToCacheException =
        new DownloadToLocalSiteException(Status.INTERNAL_SERVER_ERROR, message);
    MBeanException mBeanException = new MBeanException(downloadToCacheException, message);
    return mBeanException;
  }

  private void setupMockHandlebars(Class<? extends Exception> exceptionClass) throws Exception {
    if (exceptionClass == null) {
      when(mockHandlebars.compile(RESPONSE_HTML_TEMPLATE)).thenReturn(mockTemplate);
    } else {
      doThrow(exceptionClass).when(mockHandlebars).compile(RESPONSE_HTML_TEMPLATE);
    }
  }

  private void setupMockTemplate(Class<? extends Exception> exceptionClass, String message)
      throws Exception {
    if (exceptionClass == null) {
      when(mockTemplate.apply(message)).thenReturn(String.format(HTML_TEMPLATE, message));
    } else {
      doThrow(exceptionClass).when(mockTemplate).apply(message);
    }
  }
}
