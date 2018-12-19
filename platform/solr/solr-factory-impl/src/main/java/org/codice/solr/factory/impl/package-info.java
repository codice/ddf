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
 *
 * <p>The internal design of the Solr client adapter layer is as follow:
 *
 * <pre>
 *                 after creation when connected
 * +-------------+ and for pings    +------------------+ all others +----------------+
 * | Solr Client |----------------->|   Solr Client    |----------->|   SolrJ Http   |
 * |   Adapter   |----+             | Ping Aware Proxy |----+       |  Solr Client   |
 * +-------------+    |             +------------------+    |       | (with retries) |
 *                    |                                     |       +----------------+
 *          while     |     +-------------+           pings |
 *          creating  +---->| Unavailable |                 |     +-------------------+
 *          SolrJ           | Solr Client |                 +---->|     SolrJ Http    |
 *          Solr Client,    +-------------+                       |    Solr Client    |
 *          while connecting (for non pings)                      | (with no retries) |
 *          and after being closed                                +-------------------+
 * </pre>
 *
 * <p>The {@link org.codice.solr.factory.impl.SolrClientAdapter} will be returned right away by the
 * {@link org.codice.solr.factory.impl.HttpSolrClientFactory}, {@link
 * org.codice.solr.factory.impl.SolrCloudClientFactory}, and {@link
 * org.codice.solr.factory.impl.EmbeddedSolrFactory} before the actual SolrJ {@link
 * org.apache.solr.client.solrj.SolrClient} has been created and whether or not the Solr server is
 * reachable. At initialization time, it will redirect all API calls to an {@link
 * org.codice.solr.factory.impl.UnavailableSolrClient} that will fail all of them. During this time,
 * it will attempt to create an actual SolrJ {@link org.apache.solr.client.solrj.SolrClient} in the
 * background (either Http, embedded, or Solr cloud - only Http depicted above) and it will retry
 * the creation with an exponential backoff strategy until such time it succeeds. At this point, it
 * will cancel the task and direct all API traffic to the created Solr client.
 *
 * <p>From that point on, it will only react to failures to detect the Solr server/core is no longer
 * available. This new design reduces the traffic to the Solr server by first sending the API calls
 * to the server and only upon failures ping the server. If the ping fails, the state is changed to
 * <i>connecting</i> which will start failing all API calls coming through except for pings. A
 * background task will then continuously retry to ping the server with an exponential backoff
 * strategy until such time the server comes back up. When that happens, the background task is
 * cancelled and the state of the proxy is switched back to <i>connected</i> letting all API calls
 * through to the SolrJ {@link org.apache.solr.client.solrj.SolrClient} again. All calls to the new
 * {@link org.codice.solr.client.solrj.SolrClient#isAvailable()}, {@link
 * org.codice.solr.client.solrj.SolrClient#isAvailable(org.codice.solr.client.solrj.SolrClient.Listener)},
 * and {@link org.codice.solr.client.solrj.SolrClient#isAvailable(long,
 * java.util.concurrent.TimeUnit)} methods will returned the currently known available state. If it
 * determines a ping request was done too long ago, it will initiate one in the background while
 * returning with the currently known state.
 *
 * <p>The {@link org.apache.solr.client.solrj.impl.HttpSolrClient} used to be instantiated with an
 * {@link org.apache.http.client.HttpClient} that would retry the request forever in case of
 * failures. This was preventing high-level retries from actually being useful. Instead, the retry
 * handler was changed to limit the low-level retries. That being said, we didn't want ping requests
 * to benefit from this retry mechanism as they are internally used to determine the state of the
 * Solr server/core and they should fail fast. So to address the ping traffic to the Solr server
 * differently then all other traffic, we will now be creating 2 HTTP Solr clients one with retries
 * and one without and wrap them in a new {@link
 * org.codice.solr.factory.impl.PingAwareSolrClientProxy} proxy which will use one or the other
 * based on whether the API call is a ping or not.
 */
package org.codice.solr.factory.impl;
