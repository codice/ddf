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
package org.codice.solr.factory.impl

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.util.NamedList
import org.codice.junit.DeFinalize
import org.codice.junit.DeFinalizer
import org.codice.spock.Supplemental
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static java.util.concurrent.TimeUnit.SECONDS

@RestoreSystemProperties
@Supplemental
@RunWith(DeFinalizer)
@DeFinalize(HttpSolrClientFactory)
class HttpSolrClientFactorySpec extends Specification {
  static final String CORE = "test_core"
  static final String DATA_DIR = "data_dir"
  static final String SOLR_HOST = "server"
  static final String SOLR_HOST2 = "server2"
  static final String SOLR_PORT = "1234"
  static final String SOLR_CONTEXT = "solr"
  static final String SOLR_URL = "https://$SOLR_HOST:$SOLR_PORT/$SOLR_CONTEXT"
  static final String CORE_URL = "$SOLR_URL/$CORE"
  static final String SOLR_URL2 = "https://$SOLR_HOST2:$SOLR_PORT/$SOLR_CONTEXT"
  static final String CORE_URL2 = "$SOLR_URL2/$CORE"
  static final String CONFIG_XML = "solrconfig.xml"
  static final String SCHEMA_XML = "schema.xml"
  static final int AVAILABLE_TIMEOUT_IN_SECS = 25

  def cleanup() {
    // reset the config store
    ConfigurationStore.instance.dataDirectoryPath = null
  }

  @Timeout(HttpSolrClientFactorySpec.AVAILABLE_TIMEOUT_IN_SECS)
  @Unroll
  def 'test new client becoming available when system property solr.http url is #solr_url_is'() {
    given:
      def httpClient = Mock(SolrClient) {
        ping() >> Mock(SolrPingResponse) {
          // verify the Http Solr client should have been successfully pinged at least once
          // must be done in 'given' because it will be called from a different thread and if
          // declared in 'then', it will be out of scope and not matched
          (1.._) * getResponse() >> Mock(NamedList) {
            get("status") >> "OK"
          }
        }
      }
      def factory = Spy(HttpSolrClientFactory, constructorArgs: [null]) {
        // verify an actual Http Solr client will be created
        // must be done in 'given' because it will be called from a different thread and if
        // declared in 'then', it will be out of scope and not matched
        1 * createSolrHttpClient(solr_url, CORE, core_url) >> httpClient
      }

    and:
      System.setPropertyIfNotNull("solr.http.url", solr_http_url)
      System.setPropertyIfNotNull("org.codice.ddf.system.hostname", system_host)
      System.setPropertyIfNotNull("org.codice.ddf.system.httpsPort", system_port)
      System.setPropertyIfNotNull("org.codice.ddf.system.rootContext", system_context)

    and:
      System.setProperty("solr.data.dir", DATA_DIR)

    when:
      def client = factory.newClient(CORE)

    then: "the reported core should correspond to the requested one"
      client.core == CORE

    when: "checking for the adapter to become available"
      // because the HttpSolrClientFactory wraps what it returns with a SolrClientAdapter which uses
      // background threads to create and ping the server, we are forced to wait for it to become
      // available. That being said, we mocked the client that will be internally created and its
      // ping will be successful right away. Therefore this should all happen very quickly without
      // a hitch. Worst case scenario we have a bug and it will take about 30 seconds to detect it.
      // Best case scenario, it will return pretty much right away.
      def available = client.isAvailable(AVAILABLE_TIMEOUT_IN_SECS + 5L, SECONDS)

    then: "it should be"
      available

    and: "the underlying client should never be closed"
      0 * httpClient.close()

    and: "the config store is initialized and has its data directory updated"
      ConfigurationStore.instance.dataDirectoryPath == DATA_DIR

    where:
      solr_url_is   || solr_http_url | system_host | system_port | system_context || solr_url  | core_url
      'defined'     || SOLR_URL      | null        | null        | null           || SOLR_URL  | CORE_URL
  }

  @Timeout(HttpSolrClientFactorySpec.AVAILABLE_TIMEOUT_IN_SECS)
  @Unroll
  def 'test new client becoming available when system property solr.data.dir is #data_dir_is'() {
    given:
      def httpClient = Mock(SolrClient) {
        ping() >> Mock(SolrPingResponse) {
          // verify the Http Solr client should have been successfully pinged at least once
          // must be done in 'given' because it will be called from a different thread and if
          // declared in 'then', it will be out of scope and not matched
          (1.._) * getResponse() >> Mock(NamedList) {
            get("status") >> "OK"
          }
        }
      }
      def factory = Spy(HttpSolrClientFactory, constructorArgs: [null]) {
        // verify an actual Http Solr client will be created
        // must be done in 'given' because it will be called from a different thread and if
        // declared in 'then', it will be out of scope and not matched
        1 * createSolrHttpClient(SOLR_URL, CORE, CORE_URL) >> httpClient
      }

    and:
      System.setProperty("solr.http.url", SOLR_URL)

    and:
      System.setPropertyIfNotNull("solr.data.dir", solr_data_dir)

    when:
      def client = factory.newClient(CORE)

    then: "the reported core should correspond to the requested one"
      client.core == CORE

    when: "checking for the adapter to become available"
      // because the HttpSolrClientFactory wraps what it returns with a SolrClientAdapter which uses
      // background threads to create and ping the server, we are forced to wait for it to become
      // available. That being said, we mocked the client that will be internally created and its
      // ping will be successful right away. Therefore this should all happen very quickly without
      // a hitch. Worst case scenario we have a bug and it will take about 30 seconds to detect it.
      // Best case scenario, it will return pretty much right away.
      def available = client.isAvailable(AVAILABLE_TIMEOUT_IN_SECS + 5L, SECONDS)

    then: "it should be"
      available

    and: "the underlying client should never be closed"
      0 * httpClient.close()

    and: "the config store is initialized and its data directory was or wasn't updated"
      ConfigurationStore.instance.dataDirectoryPath == data_dir

    where:
      data_dir_is   || solr_data_dir || data_dir
      'defined'     || DATA_DIR      || DATA_DIR
      'not defined' || null          || null
  }

  def 'test new client with a null core'() {
    given:
      def factory = new HttpSolrClientFactory(null)

    when:
      factory.newClient(null)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains("invalid null Solr core")
  }
}
