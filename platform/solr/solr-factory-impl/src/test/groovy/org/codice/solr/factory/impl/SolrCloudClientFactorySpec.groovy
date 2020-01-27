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

import ddf.catalog.source.solr.api.impl.SolrConfigurationDataImpl
import net.jodah.failsafe.FailsafeException
import net.jodah.failsafe.RetryPolicy
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.response.CollectionAdminResponse
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.SolrException
import org.apache.solr.common.cloud.ClusterState
import org.apache.solr.common.cloud.DocCollection
import org.apache.solr.common.cloud.SolrZkClient
import org.apache.solr.common.cloud.ZkStateReader
import org.apache.solr.common.util.NamedList
import org.apache.zookeeper.KeeperException
import org.codice.solr.factory.SolrConfigurationData
import org.codice.spock.ClearInterruptions
import org.codice.spock.Supplemental
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static java.util.concurrent.TimeUnit.SECONDS
import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST

@RestoreSystemProperties
@Supplemental
class SolrCloudClientFactorySpec extends Specification {
  static final String CORE = "test_core"
  static final String DATA_DIR = "data_dir"

  static final int SOLR_SHARD_COUNT = 3
  static final int SOLR_REPLICATION_FACTOR = 3
  static final int SOLR_MAX_SHARDS_PER_NODE = 1
  static final String SOLR_CLOUD_ZOOKEEPERS = "server:1234,server2:2345"

  static final int MAX_RETRIES = 2

  static final int AVAILABLE_TIMEOUT_IN_SECS = 45

  @Shared
  def aliasResponseWithAlias = Mock(NamedList) {
    get("aliases") >> Collections.singletonMap("test_alias", CORE)
  }

  @Shared
  def aliasResponseWithOtherAlias = Mock(NamedList) {
    get("aliases") >> Collections.singletonMap("test_alias", "collection_1,collection_2")
  }

  @Shared
  def aliasResponseWithNullAliases = Mock(NamedList) {
    get("aliases") >> null
  }

  @Shared
  def aliasResponseWithoutAlias = Mock(NamedList)

  @Rule
  TemporaryFolder tempFolder = new TemporaryFolder();

  def setup() {
    tempFolder.create();
    ConfigurationStore.instance.dataDirectoryPath = tempFolder.root.absolutePath
  }

  def cleanup() {
    // reset the config store
    ConfigurationStore.instance.dataDirectoryPath = null
  }

  @Timeout(SolrCloudClientFactorySpec.AVAILABLE_TIMEOUT_IN_SECS)
  def 'test new client becoming available when system property solr.cloud.zookeeper is set'() {
    given:
    def cloudClient = Mock(SolrClient) {
      ping() >> Mock(SolrPingResponse) {
        // verify the Solr cloud client should have been successfully pinged at least once
        // must be done in 'given' because it will be called from a different thread and if
        // declared in 'then', it will be out of scope and not matched
        (1.._) * getResponse() >> Mock(NamedList) {
          get("status") >> "OK"
        }
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      // verify an actual Solr cloud client will be created
      // must be done in 'given' because it will be called from a different thread and if
      // declared in 'then', it will be out of scope and not matched
      1 * createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE) >> cloudClient
    }

    and:
    System.setProperty("solr.cloud.zookeeper", SOLR_CLOUD_ZOOKEEPERS)

    when:
    def client = factory.newClient(CORE)

    then: "the reported core should correspond to the requested one"
    client.core == CORE

    when: "checking for the adapter to become available"
    // because the SolrCloudClientFactory wraps what it returns with a SolrClientAdapter which uses
    // background threads to create and ping the servers, we are forced to wait for it to become
    // available. That being said, we mocked the client that will be internally created and its
    // ping will be successful right away. Therefore this should all happen very quickly without
    // a hitch. Worst case scenario we have a bug and it will take about 30 seconds to detect it.
    // Best case scenario, it will return pretty much right away.
    def available = client.isAvailable(AVAILABLE_TIMEOUT_IN_SECS + 5L, 5L, SECONDS)

    then: "it should be"
    available

    and: "the underlying client should never be closed"
    0 * cloudClient.close()
  }

  @Unroll
  def 'test new client when system property solr.cloud.zookeeper is #solr_cloud_zookeeper_is'() {
    given:
    def factory = new SolrCloudClientFactory()

    and:
    System.setPropertyIfNotNull("solr.cloud.zookeeper", solr_cloud_zookeeper)

    when:
    factory.newClient(CORE)

    then:
    def e = thrown(IllegalStateException)

    e.message.contains("'solr.cloud.zookeeper' is not configured")

    where:
    solr_cloud_zookeeper_is || solr_cloud_zookeeper
    'blank'                 || ''
    'not defined'           || null
  }

  def 'test new client with a null core'() {
    given:
    def factory = new SolrCloudClientFactory();

    and:
    System.setProperty("solr.cloud.zookeeper", SOLR_CLOUD_ZOOKEEPERS)

    when:
    factory.newClient(null)

    then:
    def e = thrown(IllegalArgumentException)

    e.message.contains("invalid null Solr core")
  }

  def 'test creating a Solr cloud client when configuration is already uploaded and the collection already exists'() {
    given:
    def zkClient = Mock(SolrZkClient)
    def listResponse = Mock(NamedList)
    def aliasResponse = Mock(NamedList)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
      newZkStateProvider() >> zkStateProvider
    }
    def factory = Spy(SolrCloudClientFactory)

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify the Solr cloud client is created"
    1 * factory.newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient

    and: "the returned client is the one we created"
    createdClient.is(cloudClient)

    and: "it is being connected"
    1 * cloudClient.connect() >> null

    then: "verify zookeeper is consulted to see if the alias exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse

    then: "verify zookeeper is consulted to see if the configuration exists"
    1 * zkClient.exists("/configs/$CORE", true) >> true

    and: "therefore, the config is never uploaded to zookeeper"
    0 * zkStateProvider.uploadConfig(*_)

    then: "verify zookeeper is consulted to see if the collection exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> listResponse
    1 * listResponse.get('collections') >> Collections.singletonList(CORE)

    and: "the collection is never created"
    0 * cloudClient.request({ it instanceof CollectionAdminRequest.Create }, null)

  }

  def 'test creating a Solr cloud client when configuration is already uploaded and the collection alias already exists'() {
    given:
    def zkClient = Mock(SolrZkClient)
    def listResponse = Mock(NamedList)
    def aliasResponse = Mock(NamedList)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
      newZkStateProvider() >> zkStateProvider
    }
    def factory = Spy(SolrCloudClientFactory)

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify the Solr cloud client is created"
    1 * factory.newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient

    and: "the returned client is the one we created"
    createdClient.is(cloudClient)

    and: "it is being connected"
    1 * cloudClient.connect() >> null

    then: "verify zookeeper is consulted to see if the alias exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse
    aliasResponse.get('aliases') >> Collections.singletonMap(CORE, Collections.singletonList("CORE"))

    then: "verify zookeeper is consulted to see if the configuration exists"
    0 * zkClient.exists("/configs/$CORE", true)

    and: "therefore, the config is never uploaded to zookeeper"
    0 * zkStateProvider.uploadConfig(*_)

    then: "verify zookeeper is consulted to see if the collection exists"
    0 * cloudClient.createCollection(*_)

    and: "the collection is never created"
    0 * cloudClient.request({ it instanceof CollectionAdminRequest.Create }, null)

    and: "close() is never called on the underlying client"
    0 * cloudClient.close()
  }

  def 'test creating a Solr cloud client when configuration is already uploaded and the collection does not exists'() {
    given:
    System.setProperty("solr.cloud.shardCount", SOLR_SHARD_COUNT as String)
    System.setProperty("solr.cloud.maxShardPerNode", SOLR_MAX_SHARDS_PER_NODE as String)
    System.setProperty("solr.cloud.replicationFactor", SOLR_REPLICATION_FACTOR as String)

    and:
    def zkClient = Mock(SolrZkClient)
    def listResponse = Mock(NamedList)
    def createResponse = Mock(NamedList)
    def aliasResponse = Mock(NamedList)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def clusterState = Mock(ClusterState) {
      hasCollection(CORE) >> true
      getCollection(CORE) >> Mock(DocCollection)
    }
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
        getClusterState() >> clusterState
      }
      newZkStateProvider() >> zkStateProvider
    }
    def factory = Spy(SolrCloudClientFactory)

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify the Solr cloud client is created"
    1 * factory.newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient

    and: "the returned client is the one we created"
    createdClient.is(cloudClient)

    and: "it is being connected"
    1 * cloudClient.connect() >> null

    then: "verify zookeeper is consulted to see if the alias exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse

    then: "verify zookeeper is consulted to see if the configuration exists"
    1 * zkClient.exists("/configs/$CORE", true) >> true

    and: "therefore, the config is never uploaded to zookeeper"
    0 * zkStateProvider.uploadConfig(*_)

    then: "verify zookeeper is consulted to see if the collection exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> listResponse
    1 * listResponse.get('collections') >> Collections.emptyList()

    and: "the collection is created"
    1 * cloudClient.request({
      ((it instanceof CollectionAdminRequest.Create)
              && (it.numShards == SOLR_SHARD_COUNT)
              && (it.maxShardsPerNode == SOLR_MAX_SHARDS_PER_NODE)
              && (it.replicationFactor == SOLR_REPLICATION_FACTOR)
              && (it.collectionName == CORE))
    }, null) >> createResponse
    1 * createResponse.get('success') >> true

    and: "verify zookeeper is consulted to see if the collection was created"
    1 * clusterState.hasCollection(CORE) >> true

    and: "verify zookeeper is consulted to see if the shards were started"
    1 * clusterState.getCollection(CORE).getSlices() >> ['shard'] * SOLR_SHARD_COUNT

    and: "close() is never called on the underlying client"
    0 * cloudClient.close()
  }

  @Unroll
  def 'test creating a Solr cloud client when configuration is uploaded and the collection already exists and system property solr.data.dir is #data_dir_is'() {
    given:
    if (solr_data_dir) {
      solr_data_dir = new File(tempFolder.root, solr_data_dir).absolutePath
      System.setProperty("solr.data.dir", solr_data_dir)
    } else {
      System.clearProperty("solr.data.dir")
    }

    and:
    def initialDataDir = ConfigurationStore.instance.dataDirectoryPath
    def zkClient = Mock(SolrZkClient)
    def listResponse = Mock(NamedList)
    def aliasResponse = Mock(NamedList)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory)

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify the Solr cloud client is created"
    1 * factory.newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient

    and: "the returned client is the one we created"
    createdClient.is(cloudClient)

    and: "it is being connected"
    1 * cloudClient.connect() >> null

    then: "verify zookeeper is consulted to see if the alias exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse

    then: "verify zookeeper is consulted to see if the configuration exists"
    1 * zkClient.exists("/configs/$CORE", true) >> false

    and: "the zookeeper state provider is the one we created"
    1 * factory.newZkStateProvider(cloudClient) >> zkStateProvider

    and: "the config is uploaded to zookeeper"
    1 * zkStateProvider.uploadConfig(*_) >> null

    then: "verify zookeeper is consulted to see if the collection exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> listResponse
    1 * listResponse.get('collections') >> Collections.singletonList(CORE)

    and: "the collection is never created"
    0 * cloudClient.request({ it instanceof CollectionAdminRequest.Create }, null)

    and: "close() is never called on the underlying client"
    0 * cloudClient.close()

    and: "the config store is initialized and its data directory was or wasn't updated"
    ConfigurationStore.instance.dataDirectoryPath == data_dir_updated ? solr_data_dir : initialDataDir

    where:
    data_dir_is   || solr_data_dir || data_dir_updated
    'defined'     || DATA_DIR      || true
    'not defined' || null          || false
  }

  // @ClearInterruptions because Failsafe is affected whenever it catches an InterruptedException
  // and decides to propagate it; thus affecting the next test cases that will call system methods
  // that checks for interruptions
  @ClearInterruptions
  @Unroll
  def 'test creating a Solr cloud client when failing to check if the configuration was already uploaded with #exception.class.noSpockSimpleName'() {
    given:
    def zkClient = Mock(SolrZkClient)
    def aliasResponse = Mock(NamedList)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify zookeeper is consulted to see if the alias exists"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse

    then: "fail when zookeeper is consulted to see if the configuration exists"
    1 * zkClient.exists("/configs/$CORE", true) >> { throw exception }

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    exception << [Stub(KeeperException), new InterruptedException()]
  }

  @Unroll
  def 'test creating a Solr cloud client when failing to upload the configuration with #exception.class.simpleName'() {
    given:
    System.setProperty("solr.cloud.shardCount", SOLR_SHARD_COUNT as String)
    System.setProperty("solr.cloud.maxShardPerNode", SOLR_MAX_SHARDS_PER_NODE as String)
    System.setProperty("solr.cloud.replicationFactor", SOLR_REPLICATION_FACTOR as String)

    and:
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> false
        }
      }
      request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> Mock(NamedList)
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "verify the Solr cloud client is created"
    1 * factory.newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient

    and: "the zookeeper state provider is the one we created"
    1 * factory.newZkStateProvider(cloudClient) >> zkStateProvider

    and: "the config is uploaded to zookeeper"
    1 * zkStateProvider.uploadConfig(*_) >> null

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    exception << [new IOException()]
  }

  @Unroll
  def 'test creating a Solr cloud client when failing to list existing collections with #exception.class.simpleName'() {
    given:
    def aliasResponse = Mock(NamedList)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "fail when trying to list existing collections"
    2 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> aliasResponse
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> {
      throw exception
    }

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    exception << [new SolrServerException('test'), new SolrException(BAD_REQUEST, 'test'), new IOException()]
  }

  @Unroll
  def 'test creating a Solr cloud client when failing to list existing collections with #fail_to_list_existing_collections_with'() {
    given:
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "fail when trying to list existing collections"
    2 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> Mock(NamedList)
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> list_response

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    fail_to_list_existing_collections_with || list_response
    'a null response'                      || null
    'no collections returned'              || Mock(NamedList) { 1 * get("collections") >> null }
  }

  @Unroll
  def 'test creating a Solr cloud client when failing to create the collection with #exception.class.simpleName'() {
    given:
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
      request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> Mock(NamedList)
      request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
        1 * get("collections") >> Collections.emptyList()
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "fail when trying to create the collection"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.Create }, null) >> {
      throw exception
    }

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    exception << [new SolrServerException('test'), new SolrException(BAD_REQUEST, 'test'), new IOException()]
  }

  def 'test creating a Solr cloud client when failing to create the collection with a failure response'() {
    given:
    def createResponse = Mock(NamedList)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
      request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> Mock(NamedList)
      request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
        1 * get("collections") >> Collections.emptyList()
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "fail when trying to create the collection"
    1 * cloudClient.request({
      it instanceof CollectionAdminRequest.Create
    }, null) >> createResponse
    1 * createResponse.get('success') >> null

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null
  }

  @Unroll
  def 'test creating a Solr cloud client when failing to check if the collection is active because #collection_is_not_active_because'() {
    given:
    def clusterState = Mock(ClusterState) {
      getCollection(CORE) >> Mock(DocCollection)
    }
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
        getClusterState() >> clusterState
      }
      request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> Mock(NamedList)
      request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
        1 * get("collections") >> Collections.emptyList()
      }
      request({ it instanceof CollectionAdminRequest.Create }, null) >> Mock(NamedList) {
        get('success') >> true
      }
    }
    def factory = Spy(SolrCloudClientFactory) {
      newCloudSolrClient(SOLR_CLOUD_ZOOKEEPERS) >> cloudClient
    }

    when:
    def createdClient = factory.createSolrCloudClient(SOLR_CLOUD_ZOOKEEPERS, CORE)

    then: "fail or not when checking if the collection was created"
    expected_retries_for_has_collection * factory.withRetry() >> {
      if (!has_collection_retry) {
        throw new FailsafeException()
      }
      new RetryPolicy().withMaxRetries(MAX_RETRIES)
    }
    expected_cluster_state_has_collection * clusterState.hasCollection(CORE) >> has_collection

    then: "fail or not zookeeper when consulted to see if the shards were started"
    expected_retries_for_get_slices * factory.withRetry() >> {
      if (!get_slices_retry) {
        throw new FailsafeException()
      }
      new RetryPolicy().withMaxRetries(MAX_RETRIES)
    }
    expected_cluster_state_get_slices * clusterState.getCollection(CORE).getSlices() >> ['shard'] * shard_count

    then: "verify the underlying client is closed"
    1 * cloudClient.close()

    and: "verify no client was returned"
    createdClient == null

    where:
    collection_is_not_active_because                       || expected_retries_for_has_collection | has_collection_retry | expected_cluster_state_has_collection | has_collection | expected_retries_for_get_slices | get_slices_retry | expected_cluster_state_get_slices | shard_count
    'it was not created'                                   || 1                                   | true                 | MAX_RETRIES + 1                       | false          | 0                               | null             | 0                                 | SOLR_SHARD_COUNT
    'a retry failure occurred while creating it'           || 1                                   | false                | 0                                     | false          | 0                               | null             | 0                                 | SOLR_SHARD_COUNT
    'the shards are not started'                           || 1                                   | true                 | 1                                     | true           | 1                               | true             | MAX_RETRIES + 1                   | 0
    'a retry failure occurred while retrieving the shards' || 1                                   | true                 | 1                                     | true           | 1                               | false            | 0                                 | 0
  }

  @Unroll
  def 'test adding collection to alias because #alias_creation_because'() {
    given:
    def zkClient = Mock(SolrZkClient)  {
      exists(*_) >> true
    }
    def aliasCreateResponse = Mock(NamedList)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.addCollectionToAlias("test_alias", CORE, "test_")

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    then: "list aliases called"
    list_invocations * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >>> alias_response

    and: "create alias called"
    create_invocations * cloudClient.request({ it instanceof CollectionAdminRequest.CreateAlias }, null) >> aliasCreateResponse

    where:
    alias_creation_because      || list_invocations | create_invocations | alias_response
    'no existing alias'         || 3                | 1                  | [aliasResponseWithoutAlias, aliasResponseWithoutAlias, aliasResponseWithAlias]
    'existing other aliases'    || 2                | 1                  | [aliasResponseWithOtherAlias, aliasResponseWithOtherAlias, aliasResponseWithAlias]
    'alias already exists'      || 1                | 0                  | [aliasResponseWithAlias]
  }

  @Unroll
  def 'test is_available because #is_available_because'() {
    given:
    def zkClient = Mock(SolrZkClient) {
      exists(*_) >> true
      getIdField() >> id_field
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.isAvailable() == available

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    where:
    is_available_because       || id_field     | available
    'required data exists'     || "id_txt"     | true
    'single entry collection'  || null         | false
  }

  @Unroll
  def 'test getting collections for alias because #get_collection_for_alias_because'() {
    given:
    def zkClient = Mock(SolrZkClient)  {
      exists(*_) >> true
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.getCollectionsForAlias("test_alias").size() == expected_data_size

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    then: "list aliases called"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases }, null) >> alias_response

    where:
    get_collection_for_alias_because      || alias_response               | expected_data_size
    'null alias response'                 || aliasResponseWithNullAliases | 0
    'no aliases response'                 || aliasResponseWithoutAlias    | 0
    'single alias response'               || aliasResponseWithAlias       | 1
    'multi alias response'                || aliasResponseWithOtherAlias  | 2
  }

  def "test is solr cloud"() {
    given:
    def factory = Spy(SolrCloudClientFactory)

    when: "test solr cloud is true"
    factory.isSolrCloud()

    then:
    true
  }

  @Unroll
  def 'test collections exists because #collection_exists_because'() {
    given:
    def zkClient = Mock(SolrZkClient)  {
      exists(*_) >> true
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
      request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
        1 * get("collections") >> collections
      }
    }
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.collectionExists(collection_name) == collection_exists

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    where:
    collection_exists_because  || collections                                  | collection_name   | collection_exists
    'no collections'           || Collections.emptyList()                      | "test_collection" | false
    'does not exist'           || Collections.singletonList("coll2")           | "test_collection" | false
    'does not exist multi'     || Arrays.asList("coll1","coll2")               | "test_collection" | false
    'exist single'             || Collections.singletonList("test_collection") | "test_collection" | true
    'exist multi'              || Arrays.asList("coll1","test_collection")     | "test_collection" | true
  }

  def 'test remove collection'() {
    given:
    def zkClient = Mock(SolrZkClient)  {
      exists(*_) >> true
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
    }
    def deleteResponse = Mock(NamedList)
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.removeCollection("test_collection")

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    then: "remove collection"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.Delete }, null) >> deleteResponse
  }

  def 'test remove alias'() {
    given:
    def zkClient = Mock(SolrZkClient)  {
      exists(*_) >> true
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      connect() >> null
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> Mock(SolrZkClient) {
          exists(*_) >> true
        }
      }
    }
    def deleteAliasResponse = Mock(NamedList)
    def factory = Spy(SolrCloudClientFactory)

    when: "add collection to alias"
    factory.removeAlias("test_alias")

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    then: "remove alias"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.DeleteAlias }, null) >> deleteAliasResponse
  }

  @Unroll
  def 'test add configuration #add_config_because'() {
    given:
    def zkClient = Mock(SolrZkClient)
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
      request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
        get("collections") >> Collections.emptyList()
      }
    }
    def factory = Spy(SolrCloudClientFactory)
    SolrConfigurationData configData = new SolrConfigurationDataImpl("test_config_file.txt", new ByteArrayInputStream());
    System.setProperty("solr.cloud.zookeeper", SOLR_CLOUD_ZOOKEEPERS)

    when: "add collection to alias"
    factory.addConfiguration(CORE, Collections.singletonList(configData));

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    and: "verify zookeeper is consulted to see if the configuration exists"
    1 * zkClient.exists("/configs/$CORE", true) >> zk_config_exists

    and: "check configuration uploaded"
    upload_count * factory.newZkStateProvider(*_) >> zkStateProvider
    upload_count * zkStateProvider.uploadConfig(*_) >> null

    where:
    add_config_because       || zk_config_exists | upload_count
    'config does not exist'  || false            | 1
    'config exists'          || true             | 0
  }

  def 'test add collection'() {
    given:
    def zkClient = Mock(SolrZkClient) {
      exists(*_) >> true
    }
    def zkStateProvider = Mock(ZkClientClusterStateProvider)
    def cloudClient = Mock(CloudSolrClient) {
      getZkStateReader() >> Mock(ZkStateReader) {
        getZkClient() >> zkClient
      }
    }
    def factory = Spy(SolrCloudClientFactory)
    SolrConfigurationData configData = new SolrConfigurationDataImpl("test_config_file.txt", new ByteArrayInputStream());
    System.setProperty("solr.cloud.zookeeper", SOLR_CLOUD_ZOOKEEPERS)

    when: "add collection to alias"
    factory.addCollection(CORE, 1, CORE);

    then: "configure client"
    1 * factory.newCloudSolrClient(*_) >> cloudClient
    1 * cloudClient.connect() >> null

    and: "check that create collection called"
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.List }, null) >> Mock(NamedList) {
      get("collections") >> Collections.emptyList()
    }
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.ListAliases}, null) >> aliasResponseWithNullAliases
    1 * cloudClient.request({ it instanceof CollectionAdminRequest.Create }, null) >> Mock(NamedList)
  }
}