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
class SolrClientProxySpec extends Specification {
  @Unroll
  def 'test api method #method.simplePrototype is delegated to the proxy client via handle()'() {
    given: "a mock client"
      def client = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "the following proxy"
      def proxy = Spy(SolrClientProxy)

    when: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = proxy."$method.name"(*parms)

    then: "verify the proxy client is retrieved each time"
      1 * proxy.getProxiedClient() >> client

    and: "verify the call was delegated to the proxy client"
      1 * client."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "verify handle() finished being used to delegate to the above proxy client"
      1 * proxy.handle(!null)

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "each method defined in Apache's SolrClient with the exclusion of getBinder() and close() is tested"
      method << SolrClient.proxyableMethods.findAll { !(it.name in ['getBinder', 'close']) }
  }

  @Unroll
  def 'test api method #method.simplePrototype is delegated to the proxy client directly'() {
    given: "a mock client"
      def client = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "the following proxy"
      def proxy = Spy(SolrClientProxy)

    when: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = proxy."$method.name"(*parms)

    then: "verify the proxy client is retrieved each time"
      1 * proxy.getProxiedClient() >> client

    and: "verify the call was delegated to the proxy client"
      1 * client."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "handle() is never used in the process"
      0 * proxy.handle(!null)

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "getBinder(), close(), and toString() methods defined in Apache's SolrClient are tested"
      method << SolrClient.proxyableMethods.findAll {
        it.name in ['getBinder', 'close']
      } + SolrClient.getMethod('toString')
  }

  def 'test api method clone() is not supported'() {
    given: "the following proxy"
      def proxy = Spy(SolrClientProxy)

    when: "calling the api tested method"
      proxy.clone()

    then: "verify the proxy client and handle() are never used"
      0 * proxy.getProxiedClient()
      0 * proxy.handle(*_)

    and: "a clone not supported exception is thrown out"
      thrown(CloneNotSupportedException)
  }
}
