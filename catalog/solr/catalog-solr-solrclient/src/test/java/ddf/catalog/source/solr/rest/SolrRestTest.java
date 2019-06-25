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
package ddf.catalog.source.solr.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.encryption.EncryptionService;
import java.util.HashMap;
import java.util.Map;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.junit.Before;
import org.junit.Test;

public class SolrRestTest {

  private SolrRest solrRest;

  @Before
  public void setUp() {
    EncryptionService encryptionService = mock(EncryptionService.class);
    when(encryptionService.decrypt(anyString())).thenReturn("test");
    solrRest = new SolrRest(mock(ClientFactoryFactory.class), encryptionService);
  }

  @Test
  public void testK1() {
    float testK1 = 1.0f;
    solrRest.setK1(testK1);
    assertThat(solrRest.getK1(), is(testK1));
  }

  @Test
  public void testB() {
    float testB = 0.5f;
    solrRest.setB(testB);
    assertThat(solrRest.getB(), is(testB));
  }

  @Test
  public void testUrl() {
    String testUrl = "https://www.example.com/solr";
    solrRest.setSolrBaseUrl(testUrl);
    assertThat(solrRest.getSolrBaseUrl(), is("https://www.example.com/solr"));
  }

  @Test
  public void testProperties() {
    String testUrl = "https://www.example.com/solr";
    float testK1 = 1.23f;
    float testB = 0.63f;

    Map<String, Object> testProps = new HashMap<>();
    testProps.put("solrBaseUrl", testUrl);
    testProps.put("k1", testK1);
    testProps.put("b", testB);
    solrRest.refresh(testProps);

    assertThat(solrRest.getSolrBaseUrl(), is(testUrl));
    assertThat(solrRest.getB(), is(testB));
    assertThat(solrRest.getK1(), is(testK1));
  }

  @Test
  public void testNoProperties() {
    System.setProperty("solr.username", "test");
    System.setProperty("solr.password", "test");
    String testUrl = "https://www.example.com/solr";
    float testK1 = 1.25f;
    float testB = 0.5f;

    Map<String, Object> testProps = new HashMap<>();
    testProps.put("solrBaseUrl", testUrl);
    testProps.put("k1", testK1);
    testProps.put("b", testB);
    solrRest.refresh(testProps);

    Map<String, Object> noProps = new HashMap<>();
    solrRest.refresh(noProps);

    assertThat(solrRest.getSolrBaseUrl(), is(testUrl));
    assertThat(solrRest.getB(), is(testB));
    assertThat(solrRest.getK1(), is(testK1));
  }

  @Test
  public void testBadProps() {
    String testUrl = "https://www.example.com/solr";
    float testK1 = 1.25f;
    float testB = 0.5f;

    Map<String, Object> testProps = new HashMap<>();
    testProps.put("solrBaseUrl", testUrl);
    testProps.put("k1", testK1);
    testProps.put("b", testB);
    solrRest.refresh(testProps);

    Boolean testBool = Boolean.TRUE;
    Map<String, Object> badProps = new HashMap<>();
    badProps.put("solrBaseUrl", testBool);
    badProps.put("k1", testBool);
    badProps.put("b", testBool);
    solrRest.refresh(badProps);

    assertThat(solrRest.getSolrBaseUrl(), is(testUrl));
    assertThat(solrRest.getB(), is(testB));
    assertThat(solrRest.getK1(), is(testK1));
  }
}
