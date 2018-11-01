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

import net.jodah.failsafe.FailsafeException
import net.jodah.failsafe.RetryPolicy
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.SolrException
import org.apache.solr.common.cloud.ClusterState
import org.apache.solr.common.cloud.DocCollection
import org.apache.solr.common.cloud.SolrZkClient
import org.apache.solr.common.cloud.ZkStateReader
import org.apache.solr.common.util.NamedList
import org.apache.zookeeper.KeeperException
import org.codice.spock.ClearInterruptions
import org.codice.spock.Supplemental
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static java.util.concurrent.TimeUnit.SECONDS
import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST

@RestoreSystemProperties
@Supplemental
class SolrFactorySettingsSpec extends Specification {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder();


    def setup() {
        tempFolder.create();
        ConfigurationStore.instance.dataDirectoryPath = tempFolder.root.absolutePath
    }
}
