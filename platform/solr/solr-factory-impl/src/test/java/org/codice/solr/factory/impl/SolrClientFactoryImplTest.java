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
package org.codice.solr.factory.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SolrClientFactoryImplTest {

  @Mock private SolrClient mockClient;

  @Test(expected = IllegalArgumentException.class)
  public void newClientWithNullCoreName() {
    SolrClientFactoryImpl factory = new SolrClientFactoryImpl();
    factory.newClient(null);
  }

  @Test
  public void newCloudSolrClient() {
    System.setProperty("solr.client", "CloudSolrClient");
    SolrClientFactoryImpl factory = new SolrClientFactoryImpl();

    assertThat(factory.getFactory(), is(instanceOf(SolrCloudClientFactory.class)));
  }

  @Test
  public void newClientWithUnknownClientType() {
    System.setProperty("solr.client", "Unknown");
    SolrClientFactoryImpl factory = new SolrClientFactoryImpl();

    assertThat(factory.getFactory(), is(instanceOf(SolrCloudClientFactory.class)));
  }
}
