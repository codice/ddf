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
import org.codice.solr.client.solrj.UnavailableSolrException
import org.codice.spock.Supplemental
import spock.lang.Specification
import spock.lang.Unroll

@Supplemental
class UnavailableSolrClientSpec extends Specification {
  static def EXCEPTION = new UnavailableSolrException("testing")

  def 'test constructor with a null message'() {
    when:
      new UnavailableSolrClient(null)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains('invalid null cause')
  }

  def 'test that retreiving the proxied client throws UnavailableSolrException'() {
    given:
      def client = new UnavailableSolrClient(EXCEPTION)

    when: "calling retrieving the proxied client"
      client.getProxiedClient()

    then: "verify that it fails"
      def e = thrown(UnavailableSolrException)

      e.cause.is(EXCEPTION)
  }

  def 'test api method close() does nothing'() {
    given:
      def client = new UnavailableSolrClient(EXCEPTION)

    when: "calling the close()"
      client.close()

    then: 'no exceptions must be thrown'
      noExceptionThrown()
  }

  @Unroll
  def 'test api method #method.simplePrototype throws UnavailableSolrException'() {
    given:
      def client = new UnavailableSolrClient(EXCEPTION)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)

    when: "calling the api tested method with dummy values/objects of the right type"
      client."$method.name"(*parms)

    then: "verify that it fails"
      def e = thrown(UnavailableSolrException)

      e.cause.is(EXCEPTION)

    where: "each methods defined in Apache's SolrClient with the exclusion of close() is tested"
      method << SolrClient.proxyableMethods.findAll { it.name != 'close' }
  }
}
