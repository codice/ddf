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
import org.codice.spock.Supplemental
import spock.lang.Specification
import spock.lang.Unroll

@Supplemental
class PingAwareSolrClientProxySpec extends Specification {
  @Unroll
  def 'test constructor with #test_constructor_with'() {
    when:
      new PingAwareSolrClientProxy(client, ping_client)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      test_constructor_with || client           | ping_client      || message
      'null client'         || null             | Stub(SolrClient) || 'invalid null client'
      'null ping client'    || Stub(SolrClient) | null             || 'invalid null ping client'
  }

  def 'test constructor'() {
    given:
      def client = Mock(SolrClient)
      def pingClient = Mock(SolrClient)

    when:
      def proxy = new PingAwareSolrClientProxy(client, pingClient)

    then: "verify the referenced proxied client is not the specified client and not the ping client"
      !proxy.proxiedClient.is(pingClient)
      proxy.proxiedClient.is(client)
  }

  @Unroll
  def 'test api method #method.simplePrototype is delegated to the client and not the ping client'() {
    given: "a mock client and ping client"
      def client = Mock(SolrClient)
      def pingClient = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "the following ping aware proxy"
      def proxy = new PingAwareSolrClientProxy(client, pingClient)

    when: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = proxy."$method.name"(*parms)

    then: "verify the call was delegated to the client and not the ping client"
      0 * pingClient."$method.name"(*_)
      1 * client."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "each methods defined in Apache's SolrClient except for close and ping methods is tested"
      method << SolrClient.proxyableMethods.findAll {
        (it.simplePrototype != 'close()') && (it.name != 'ping')
      }
  }

  @Unroll
  def 'test api method #method.simplePrototype is delegated to the ping client and not the client'() {
    given: "a mock client and ping client"
      def client = Mock(SolrClient)
      def pingClient = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "the following ping aware proxy"
      def proxy = new PingAwareSolrClientProxy(client, pingClient)

    when: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = proxy."$method.name"(*Dummies(method.parameterTypes))

    then: "verify the call was delegated to the ping client and not the client"
      0 * client."$method.name"(*_)
      1 * pingClient."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "each ping methods defined in Apache's SolrClient is tested"
      method << SolrClient.proxyableMethods.findAll { it.name == 'ping' }
  }

  @Unroll
  def 'test api method #method.simplePrototype is delegated to both the ping client and the client'() {
    given: "a mock client and ping client"
      def client = Mock(SolrClient)
      def pingClient = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "the following ping aware proxy"
      def proxy = new PingAwareSolrClientProxy(client, pingClient)

    when: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = proxy."$method.name"(*Dummies(method.parameterTypes))

    then: "verify the call was delegated to both the ping client and the client"
      1 * client."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }
      1 * pingClient."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "the close method in Apache's SolrClient is tested"
      method << SolrClient.getMethod('close')
  }
}
