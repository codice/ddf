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

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.util.NamedList
import org.apache.solr.core.CoreDescriptor
import org.apache.solr.core.SolrConfig
import org.apache.solr.core.SolrCore
import org.apache.solr.core.SolrResourceLoader
import org.apache.solr.schema.IndexSchema
import org.codice.junit.DeFinalize
import org.codice.junit.DeFinalizer
import org.codice.spock.Supplemental
import org.junit.runner.RunWith
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Path

import static java.util.concurrent.TimeUnit.SECONDS
import static org.codice.solr.factory.impl.HttpSolrClientFactory.DEFAULT_SCHEMA_XML
import static org.codice.solr.factory.impl.HttpSolrClientFactory.DEFAULT_SOLRCONFIG_XML

@RestoreSystemProperties
@Supplemental
@RunWith(DeFinalizer)
@DeFinalize(SolrCore)
class EmbeddedSolrFactorySpec extends Specification {
  static final String CORE = "test_core"
  static final String DATA_DIR = "data_dir"
  static final String CONFIG_XML = "solrconfig.xml"
  static final String SCHEMA_XML = "schema.xml"
  static final int AVAILABLE_TIMEOUT_IN_SECS = 25

  @Shared
  ConfigurationFileProxy configFileProxy = Stub()
  @Shared
  ConfigurationStore configStore = Stub()

  EmbeddedSolrServer server = Stub()
  CoreDescriptor descriptor = Stub()
  Path configHomePath = Stub()
  Path instancePath = Stub()
  IndexSchema index = Stub()

  def cleanup() {
    // reset the config store
    ConfigurationStore.instance.dataDirectoryPath = null
    ConfigurationStore.instance.inMemory = false
    // clear the cache of schema indexes
    EmbeddedSolrFactory.resetIndexCache(Collections.emptyMap())
  }

  @Timeout(EmbeddedSolrFactorySpec.AVAILABLE_TIMEOUT_IN_SECS)
  @Unroll
  def 'test new client becoming available when system property solr.data.dir is #data_dir_is'() {
    given:
      def server = Mock(EmbeddedSolrServer) {
        ping() >> Mock(SolrPingResponse) {
          // verify the Solr embedded server should have been successfully pinged at least once
          // must be done in 'given' because it will be called from a different thread and if
          // declared in 'then', it will be out of scope and not matched
          (1.._) * getResponse() >> Mock(NamedList) {
            get("status") >> "OK"
          }
        }
      }
      def factory = Spy(EmbeddedSolrFactory) {
        // verify an actual Solr embedded server will be created
        // must be done in 'given' because it will be called from a different thread and if
        // declared in 'then', it will be out of scope and not matched
        1 * getEmbeddedSolrServer(CORE, DEFAULT_SOLRCONFIG_XML, DEFAULT_SCHEMA_XML, {
          it.is(ConfigurationStore.instance)
        }, !null) >> server
      }

    and:
      System.setPropertyIfNotNull("solr.data.dir", solr_data_dir)

    when:
      def client = factory.newClient(CORE)

    then: "the reported core should correspond to the requested one"
      client.core == CORE

    when: "checking for the adapter to become available"
      // because the EmbeddedSolrFactory wraps what it returns with a SolrClientAdapter which uses
      // background threads to create and ping the server, we are forced to wait for it to become
      // available. That being said, we mocked the embedded server that will be internally created
      // and its ping will be successful right away. Therefore this should all happen very quickly
      // without a hitch. Worst case scenario we have a bug and it will take about 30 seconds to
      // detect it. Best case scenario, it will return pretty much right away.
      def available = client.isAvailable(AVAILABLE_TIMEOUT_IN_SECS + 5L, SECONDS)

    then: "it should be"
      available

    and: "the underlying server should never be closed"
      0 * server.close()

    and: "the config store is initialized and its data directory was or wasn't updated"
      ConfigurationStore.instance.dataDirectoryPath == data_dir

    where:
      data_dir_is   || solr_data_dir || data_dir
      'defined'     || DATA_DIR      || DATA_DIR
      'not defined' || null          || null
  }

  def 'test new client with a null core'() {
    given:
      def factory = new EmbeddedSolrFactory();

    when:
      factory.newClient(null)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains("invalid null Solr core")
  }

  @Timeout(EmbeddedSolrFactorySpec.AVAILABLE_TIMEOUT_IN_SECS)
  def 'test new client with additional info becoming available'() {
    given:
      def server = Mock(EmbeddedSolrServer) {
        ping() >> Mock(SolrPingResponse) {
          // verify the Solr embedded server should have been successfully pinged at least once
          // must be done in 'given' because it will be called from a different thread and if
          // declared in 'then', it will be out of scope and not matched
          (1.._) * getResponse() >> Mock(NamedList) {
            get("status") >> "OK"
          }
        }
      }
      def factory = Spy(EmbeddedSolrFactory) {
        // verify an actual Solr embedded server will be created
        // must be done in 'given' because it will be called from a different thread and if
        // declared in 'then', it will be out of scope and not matched
        1 * getEmbeddedSolrServer(CORE, DEFAULT_SOLRCONFIG_XML, DEFAULT_SCHEMA_XML, configStore, configFileProxy) >> server
      }

    when:
      def client = factory.newClient(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "the reported core should correspond to the requested one"
      client.core == CORE

    when: "checking for the adapter to become available"
      // because the EmbeddedSolrFactory wraps what it returns with a SolrClientAdapter which uses
      // background threads to create and ping the server, we are forced to wait for it to become
      // available. That being said, we mocked the embedded server that will be internally created
      // and its ping will be successful right away. Therefore this should all happen very quickly
      // without a hitch. Worst case scenario we have a bug and it will take about 30 seconds to
      // detect it. Best case scenario, it will return pretty much right away.
      def available = client.isAvailable(AVAILABLE_TIMEOUT_IN_SECS + 5L, SECONDS)

    then: "it should be"
      available

    and: "the underlying server should never be closed"
      0 * server.close()
  }

  @Unroll
  def 'test new client with additional info and a null #and_a_null'() {
    given:
      def factory = new EmbeddedSolrFactory();

    when:
      factory.newClient(core, config_xml, schema_xml, config_store, config_file_proxy)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      and_a_null      || core | config_xml | schema_xml | config_store | config_file_proxy || message
      'core'          || null | CONFIG_XML | SCHEMA_XML | configStore  | configFileProxy   || 'invalid null Solr core'
      'configuration' || CORE | null       | SCHEMA_XML | configStore  | configFileProxy   || 'invalid null Solr config file'
      'schema'        || CORE | CONFIG_XML | null       | configStore  | configFileProxy   || 'invalid null Solr schema file'
      'config store'  || CORE | CONFIG_XML | SCHEMA_XML | null         | configFileProxy   || 'invalid null Solr config store'
      'config proxy'  || CORE | CONFIG_XML | SCHEMA_XML | configStore  | null              || 'invalid null Solr config proxy'
  }

  @Unroll
  def 'test creating an embedded Solr server with a store that is #store_that_is'() {
    given:
      def factory = Spy(EmbeddedSolrFactory)
      def container = Mock(SolrCoreContainer)
      def loader = Mock(SolrResourceLoader)
      def core = Mock(SolrCore)
      def configFileProxy = Mock(ConfigurationFileProxy)
      def configStore = Mock(ConfigurationStore) {
        isInMemory() >> in_memory
      }
      def config = Mock(SolrConfig) {
        getResourceLoader() >> Mock(SolrResourceLoader) {
          getInstancePath() >> instancePath
        }
      }
      def files = Mock(EmbeddedSolrFiles) {
        getConfigHome() >> Mock(File) {
          toPath() >> configHomePath
        }
        getConfig() >> config
        getDataDirPath() >> DATA_DIR
      }

    when:
      def createdServer = factory.getEmbeddedSolrServer(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "the reported server should be the one we created"
      createdServer.is(server)

    and: "make sure the Solr configuration was or wasn't written to disk"
      expected_write_solr_config * configFileProxy.writeSolrConfiguration(CORE)

    and: "make sure embedded Solr files were properly referenced"
      1 * factory.newFiles(CORE, CONFIG_XML, {
        it == [SCHEMA_XML, "managed-schema"]
      }, configFileProxy) >> files

    and: "make sure all low-level Solr objects are created"
      with(factory) {
        1 * newLoader(configHomePath) >> loader
        1 * newContainer(loader) >> container
        1 * newDescriptor(CORE, instancePath, !null, false) >> descriptor
        1 * newCore(container, CORE, DATA_DIR, config, index, null, descriptor, null, null, null, false) >> core
        1 * newServer(container, CORE) >> server
      }

    and: "make sure we did register the core with the container"
      1 * container.register(CORE, core, false, true)

    and: "make sure we did create a schema index"
      1 * files.schemaIndex >> index

    and: "make sure internal resources are not closed"
      0 * loader.close()
      0 * core.close()
      0 * server.close()

    and: "make sure the new schema index was cached"
      EmbeddedSolrFactory.indexCacheEquals([(files): index])

    where:
      store_that_is   || in_memory || expected_write_solr_config
      'not in memory' || false     || 1
      'in memory'     || true      || 0
  }

  def 'test creating an embedded Solr server when the schema index was already cached'() {
    given:
      def files = Mock(EmbeddedSolrFiles) {
        getConfigHome() >> Mock(File) {
          toPath() >> configHomePath
        }
        getConfig() >> Mock(SolrConfig) {
          getResourceLoader() >> Mock(SolrResourceLoader) {
            getInstancePath() >> instancePath
          }
        }
        getDataDirPath() >> DATA_DIR
      }
      def factory = Spy(EmbeddedSolrFactory) {
        newLoader(*_) >> Stub(SolrResourceLoader)
        newContainer(*_) >> Stub(SolrCoreContainer)
        newDescriptor(*_) >> descriptor
        newCore(*_) >> Stub(SolrCore)
        newServer(*_) >> server
        newFiles(*_) >> files
      }

    and:
      EmbeddedSolrFactory.resetIndexCache([(files): index])

    when:
      def createdServer = factory.getEmbeddedSolrServer(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "the reported server should be the one we created"
      createdServer.is(server)

    and: "make sure no new schema index were created"
      0 * files.schemaIndex

    and: "make sure the original schema index is still cached"
      EmbeddedSolrFactory.indexCacheEquals([(files): index])
  }

  def 'test creating an embedded Solr server when failing to create the schema index'() {
    given:
      def error = new RuntimeException("testing")
      def files = Mock(EmbeddedSolrFiles) {
        getConfigHome() >> Mock(File) {
          toPath() >> configHomePath
        }
        getConfig() >> Mock(SolrConfig) {
          getResourceLoader() >> Mock(SolrResourceLoader) {
            getInstancePath() >> instancePath
          }
        }
        getDataDirPath() >> DATA_DIR
      }
      def factory = Spy(EmbeddedSolrFactory) {
        newLoader(*_) >> Stub(SolrResourceLoader)
        newContainer(*_) >> Stub(SolrCoreContainer)
        newDescriptor(*_) >> descriptor
        newCore(*_) >> Stub(SolrCore)
        newServer(*_) >> server
        newFiles(*_) >> files
      }

    when:
      factory.getEmbeddedSolrServer(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "fail to create the schema index"
      1 * files.schemaIndex >> { throw error }
      def e = thrown(RuntimeException)

      e.is(error)

    and: "make sure the no schema indexes were cached"
      EmbeddedSolrFactory.indexCacheEquals(Collections.emptyMap())
  }

  def 'test creating an embedded Solr server when failing after the creation of the resource loader'() {
    given:
      def error = new RuntimeException("testing")
      def loader = Mock(SolrResourceLoader)
      def factory = Spy(EmbeddedSolrFactory) {
        newLoader(*_) >> loader
        newDescriptor(*_) >> descriptor
        newCore(*_) >> Stub(SolrCore)
        newServer(*_) >> server
        newFiles(*_) >> Mock(EmbeddedSolrFiles) {
          getConfigHome() >> Mock(File) {
            toPath() >> configHomePath
          }
          getConfig() >> Mock(SolrConfig) {
            getResourceLoader() >> Mock(SolrResourceLoader) {
              getInstancePath() >> instancePath
            }
          }
          getDataDirPath() >> DATA_DIR
        }
      }

    when:
      factory.getEmbeddedSolrServer(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "fail to create the container"
      1 * factory.newContainer(*_) >> { throw error }
      def e = thrown(RuntimeException)

      e.is(error)

    and: "make sure the container is closed"
      1 * loader.close()
  }

  def 'test creating an embedded Solr server when failing after the creation of the resource loader and the core'() {
    given:
      def error = new RuntimeException("testing")
      def loader = Mock(SolrResourceLoader)
      def core = Mock(SolrCore)
      def factory = Spy(EmbeddedSolrFactory) {
        newLoader(*_) >> loader
        newContainer(*_) >> Stub(SolrCoreContainer)
        newDescriptor(*_) >> descriptor
        newCore(*_) >> core
        newFiles(*_) >> Mock(EmbeddedSolrFiles) {
          getConfigHome() >> Mock(File) {
            toPath() >> configHomePath
          }
          getConfig() >> Mock(SolrConfig) {
            getResourceLoader() >> Mock(SolrResourceLoader) {
              getInstancePath() >> instancePath
            }
          }
          getDataDirPath() >> DATA_DIR
          getSchemaIndex() >> index
        }
      }

    when:
      factory.getEmbeddedSolrServer(CORE, CONFIG_XML, SCHEMA_XML, configStore, configFileProxy)

    then: "fail to create the server"
      1 * factory.newServer(*_) >> { throw error }
      def e = thrown(RuntimeException)

      e.is(error)

    and: "make sure the container and the core are closed"
      1 * loader.close()
      1 * core.close()
  }
}