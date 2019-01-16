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
package org.codice.solr.client.solrj

import org.codice.spock.Supplemental
import spock.lang.Specification
import spock.lang.Unroll

@Supplemental
class SolrClientSpec extends Specification {
  @Unroll
  def "test that Apache's #method.simplePrototype method is defined in Codice's interface"() {
    expect:
      SolrClient.getMethodBySimplePrototype(method.simplePrototype)

    where: "each public method of Apache's SolrClient class is duplicated into Codice SolrClient interface"
      method << org.apache.solr.client.solrj.SolrClient.apiMethods
  }
}
