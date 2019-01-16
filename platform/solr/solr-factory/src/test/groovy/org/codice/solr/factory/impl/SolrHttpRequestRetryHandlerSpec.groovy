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

import org.apache.http.protocol.HttpContext
import org.codice.spock.ClearInterruptions
import spock.lang.Specification
import spock.lang.Unroll

import javax.net.ssl.SSLException

class SolrHttpRequestRetryHandlerSpec extends Specification {
  static final String CORE = "test_core"

  static final long TIME_FACTOR = 1L
  static final int MAX_RETRY_COUNT = 4;

  def httpContext = Stub(HttpContext)

  def 'test constructor with null core'() {
    when:
      new SolrHttpRequestRetryHandler(null)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains('invalid null core')
  }

  @ClearInterruptions
  @Unroll
  def 'test retryRequest() with #exception.simpleName and retry count of #retry_count will #test_will'() {
    given: "a retry handler that reduces the time factor and retry count for testing purpose"
      def retryHandler = new SolrHttpRequestRetryHandler(CORE, TIME_FACTOR, MAX_RETRY_COUNT)

    when: "the delay while checking should be interrupted, then do pre-interrupt the thread to simulate it"
      if (interrupt_delay) {
        Thread.currentThread().interrupt()
      }

    and: "then check if we should retry"
      def returnedResult = retryHandler.retryRequest(exception, retry_count, httpContext)

    then: "verify that we should or shouldn't retry the Http request"
      returnedResult == result

    and: "if it was interrupting, then the interrupted state of the thread should have been reset"
      Thread.currentThread().isInterrupted() == interrupted

    where:
      test_will                             || exception                             | retry_count         | interrupt_delay || result | interrupted
      'not retry'                           || new InterruptedIOException('testing') | 1                   | false           || false  | true
      'not retry'                           || new InterruptedIOException('testing') | 2                   | false           || false  | true
      'not retry when delay is interrupted' || new InterruptedIOException('testing') | 2                   | true            || false  | true
      'not retry'                           || new InterruptedIOException('testing') | MAX_RETRY_COUNT     | false           || false  | true
      'not retry'                           || new InterruptedIOException('testing') | MAX_RETRY_COUNT + 1 | false           || false  | true
      'retry'                               || new UnknownHostException('testing')   | 1                   | false           || true   | false
      'retry'                               || new UnknownHostException('testing')   | 2                   | false           || true   | false
      'not retry when delay is interrupted' || new UnknownHostException('testing')   | 2                   | true            || false  | true
      'retry'                               || new UnknownHostException('testing')   | MAX_RETRY_COUNT     | false           || true   | false
      'not retry'                           || new UnknownHostException('testing')   | MAX_RETRY_COUNT + 1 | false           || false  | false
      'retry'                               || new SSLException('testing')           | 1                   | false           || true   | false
      'retry'                               || new SSLException('testing')           | 2                   | false           || true   | false
      'not retry when delay is interrupted' || new SSLException('testing')           | 2                   | true            || false  | true
      'retry'                               || new SSLException('testing')           | MAX_RETRY_COUNT     | false           || true   | false
      'not retry'                           || new SSLException('testing')           | MAX_RETRY_COUNT + 1 | false           || false  | false
      'retry'                               || new IOException('testing')            | 1                   | false           || true   | false
      'retry'                               || new IOException('testing')            | 2                   | false           || true   | false
      'not retry when delay is interrupted' || new IOException('testing')            | 2                   | true            || false  | true
      'retry'                               || new IOException('testing')            | MAX_RETRY_COUNT     | false           || true   | false
      'not retry'                           || new IOException('testing')            | MAX_RETRY_COUNT + 1 | false           || false  | false
  }
}
