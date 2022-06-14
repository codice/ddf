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
package org.codice.ddf.commands.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheCommandTest extends SolrCommandTest {

  @Test
  public void testCacheClear() throws Exception {
    SolrClient cloudClient = mock(SolrClient.class);
    NamedList<Object> pingStatus = new NamedList<>();
    pingStatus.add("status", "OK");

    UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);

    when(cloudClient.deleteByQuery(any(String.class), any(String.class)))
        .thenReturn(mockUpdateResponse);

    SolrPingResponse pingResponse = mock(SolrPingResponse.class);
    when(cloudClient.ping()).thenReturn(pingResponse);
    when(pingResponse.getResponse()).thenReturn(new NamedList<>(Map.of("status", "OK")));

    when(mockUpdateResponse.getStatus()).thenReturn(0);

    CacheCommand command = new CacheCommand();
    command.setClient(cloudClient);
    command.force = true;
    command.clear = true;
    Object result = command.execute();

    assertThat(result, is(Boolean.TRUE));
  }
}
