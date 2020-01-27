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

import net.jodah.failsafe.AsyncFailsafe
import net.jodah.failsafe.ExecutionContext
import net.jodah.failsafe.FailsafeFuture
import net.jodah.failsafe.SyncFailsafe
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.SolrException
import org.apache.solr.common.util.NamedList
import org.codice.junit.DeFinalize
import org.codice.junit.DeFinalizer
import org.codice.solr.client.solrj.UnavailableSolrException
import org.codice.solr.factory.impl.SolrClientAdapter.Creator
import org.codice.spock.ClearInterruptions
import org.codice.spock.Supplemental
import org.junit.runner.RunWith
import org.spockframework.mock.runtime.MockInvocation
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService

import static org.codice.solr.factory.impl.SolrClientAdapter.State.CLOSED
import static org.codice.solr.factory.impl.SolrClientAdapter.State.CONNECTED
import static org.codice.solr.factory.impl.SolrClientAdapter.State.CONNECTING
import static org.codice.solr.factory.impl.SolrClientAdapter.State.CREATING

@Supplemental
@RunWith(DeFinalizer)
@DeFinalize(SolrClientAdapter)
class SolrClientAdapterSpec extends Specification {
  static final String CORE = "test_core"
  static final int TIMEOUT_IN_SECS = 25

  @Shared
  def pingOkResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> 'OK'
    }
  }
  @Shared
  def pingFailResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> 'FAIL'
    }
  }
  @Shared
  def pingNoStatusResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> null
    }
  }

  def creator = Stub(Creator)
  def client = Mock(SolrClient)

  def getFuture = Mock(FailsafeFuture) {
    isDone() >> false
  }
  def getFuture2 = Mock(FailsafeFuture) {
    isDone() >> false
  }
  def runFuture = Mock(FailsafeFuture) {
    isDone() >> false
  }
  def runFuture2 = Mock(FailsafeFuture) {
    isDone() >> false
  }
  def failsafeCreator = {
    Mock(SyncFailsafe) {
      with(_ as ScheduledExecutorService) >> Mock(AsyncFailsafe) {
        /on.*/(*_) >> { (delegate as MockInvocation).mockObject.instance } // itself
        get(_) >>> [getFuture, getFuture2]
        run(_) >>> [runFuture, runFuture2]
      }
    }
  }

  def directExecutor = new SolrClientAdapter.Executor() {
    def <T> Future<T> submit(Callable<T> task) {
      new CompletableFuture<T>(task.call())
    }

    Future<?> submit(Runnable task) {
      task.run()
      new CompletableFuture<Object>(null);
    }
  }

  def 'test constructor'() {
    given: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    expect:
      adapter.state == CREATING
      adapter.creator.is(creator)
      !adapter.isAvailable()
      !adapter.hasListeners()
      !adapter.hasInitializers()
  }

  @Unroll
  def 'test constructor with #test_constructor_with'() {
    when: "creating an adapter"
      new SolrClientAdapter(core, creator)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      test_constructor_with || core | creator       || message
      'null core'           || null | Stub(Creator) || 'invalid null Solr core'
      'null creator'        || CORE | null          || 'invalid null Solr creator'
  }

  def 'test getProxiedClient() when creating'() {
    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be 'creating'"
      adapter.state == CREATING

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the adapter should have checked if it is reachable"
      1 * adapter.checkIfReachable(_) >> { callRealMethod() }

    and: "the proxy solr client returned should indicate the adapter is initializing"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("initializing '$CORE' client")
  }

  def 'test getProxiedClient() when connecting and still unreachable'() {
    given:
      def client = Mock(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be 'creating'"
      adapter.state == CREATING

    when: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should now be 'connecting'"
      adapter.state == CONNECTING

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the adapter should have checked if it is reachable and reported it is not"
      1 * adapter.checkIfReachable(_) >> { callRealMethod() }
      1 * client.ping() >> { throw new SolrException("testing") }

    and: "the proxy solr client returned should indicate the adapter is initializing"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("initializing '$CORE' client")
  }

  def 'test getProxiedClient() when connecting and reachable'() {
    given:
      def client = Mock(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be 'creating'"
      adapter.state == CREATING

    when: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should now be 'connecting'"
      adapter.state == CONNECTING

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the adapter should have checked if it is reachable and reported is"
      1 * adapter.checkIfReachable(_) >> null

    and: "the adapter should now be 'connected'"
      adapter.state == CONNECTED

    and: "the proxy solr client returned should be our client"
      proxyClient.is(client)
  }

  def 'test getProxiedClient() when connected'() {
    given:
      def client = Mock(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be 'creating'"
      adapter.state == CREATING

    when: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should now be 'connecting'"
      adapter.state == CONNECTING

    when: "simulating a successful connection by failsafe"
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    then: "the adapter should now be 'connected'"
      adapter.state == CONNECTED

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the adapter have not checked if it is reachable"
      0 * adapter.checkIfReachable(_)
      0 * client.ping()

    and: "the proxy solr client returned should be our client"
      proxyClient.is(client)
  }

  def 'test getProxiedClient() when closed'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    and: "close the adapter"
      adapter.close();

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the adapter have not checked if it is reachable"
      0 * adapter.checkIfReachable(_)
      0 * client.ping()

    and: "it shouldn't be the real client"
      !proxyClient.is(client)

    and: "the proxy solr client returned should indicate the adapter was closed"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("'$CORE' client was closed")
  }

  @Unroll
  def 'test api method #method.simplePrototype is delegated to the real client via handle()'() {
    given:
      def client = Mock(SolrClient)

    and: "the following parameters and result"
      def parms = Dummies(method.parameterTypes)
      def result = Dummy(method.returnType)

    and: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "calling the api tested method with dummy values/objects of the right type"
      def returnedResult = adapter."$method.name"(*parms)

    then: "verify the call was delegated to the real client"
      1 * client."$method.name"(*_) >> {
        method.verifyInvocation(delegate, *parms)
        result
      }

    and: "the returned result is what we expected"
      returnedResult.is(result)

    where: "each method defined in Apache's SolrClient with the exclusion of ping() and close() is tested"
      method << SolrClient.proxyableMethods.findAll { !(it.name in ['ping', 'close']) }
  }

  @Unroll
  def 'test handle() when #exception.class.simpleName is thrown by either the proxy client'() {
    given:
      def client = Mock(SolrClient)

    and: "some api handle code"
      def code = {
        throw exception
      }

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and:
      adapter.handle(code)

    then: "verify the adapter decided (or not) to recheck if it was reachable"
      expected_check_if_reachable * adapter.checkIfReachable(_) >> null

    and: "it decided (or not) to change state to connected again potentially cancelling the future if failsafe was running in the background"
      expected_set_connected * adapter.setConnected(true) >> null

    and: "the exception should had bubbled out"
      def e = thrown(Throwable);

      e.is(exception)

    where:
      exception                                                     || expected_check_if_reachable | expected_set_connected
      new SolrException(SolrException.ErrorCode.UNKNOWN, 'testing') || 1                           | 1
      new SolrServerException('testing')                            || 1                           | 1
      new IOException('testing')                                    || 1                           | 1
      new UnavailableSolrException("testing")                       || 0                           | 0
      new RuntimeException("testing")                               || 0                           | 0
      new Error("testing")                                          || 0                           | 0
  }

  def 'test api method ping() is delegated to the real client which returns an OK status when connected'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "calling ping"
      def response = adapter.ping()

    then: "verify the call was delegated to the real client"
      1 * client.ping() >> pingOkResponse

    and: "the response is the one from the client and simulate an OK response"
      response.is(pingOkResponse)

    and: "the adapter decided to change state to 'connected' again"
      1 * adapter.setConnected(true) >> null
  }

  @Unroll
  def 'test api method ping() is delegated to the real client which returns #returns_what when #when'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "calling ping"
      def returnedResponse = adapter.ping()

    then: "verify the call was delegated to the real client and it simulates an appropriate response"
      1 * client.ping() >> response

    and: "the response we got was the one the client sent back"
      returnedResponse.is(response)

    and: "the adapter decided to change state to 'connecting' with an unavailable client that indicate an invalid ping response was received"
      1 * adapter.setConnecting(client, {
        it.cause.message.contains(message)
      }, true, CONNECTED) >> null

    where:
      returns_what      | when         || simulate_connecting | simulate_connected | response             || message
      'no response'     | 'connecting' || true                | false              | null                 || 'no response'
      'no response'     | 'connected'  || true                | true               | null                 || 'no response'
      'no status'       | 'connecting' || true                | false              | pingNoStatusResponse || 'with null status'
      'no status'       | 'connected'  || true                | true               | pingNoStatusResponse || 'with null status'
      'a non-OK status' | 'connecting' || true                | false              | pingFailResponse     || 'with FAIL status'
      'a non-OK status' | 'connected'  || true                | true               | pingFailResponse     || 'with FAIL status'
  }

  @Unroll
  def 'test api method ping() is not delegated to the real client when #when'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "closing the adapter if requested"
      if (close) {
        adapter.close()
      }

    then: "the adapter should change its state"
      if (simulate_connecting) {
        1 * adapter.setConnecting(*_) >> { callRealMethod() }
      }
      if (simulate_connected) {
        1 * adapter.setConnected(*_) >> { callRealMethod() }
      }

    when: "calling ping"
      adapter.ping()

    then: "verify the call was not delegated to the real client"
      0 * client.ping()

    and: "check that an unavailable exception bubbled out with a message indicating the current state of the client"
      def e = thrown(UnavailableSolrException)

      e.message.contains(message)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)

    where:
      when       || simulate_connecting | simulate_connected | close || message
      'creating' || false               | false              | false || "initializing '$CORE' client"
      'closed'   || true                | true               | true  || "'$CORE' client was closed"
  }

  @Unroll
  def 'test api method ping() is delegated to the real client which throws #exception.class.simpleName'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "calling ping"
      adapter.ping()

    then: "verify the call was delegated to the real client and let it throw the exception"
      1 * client.ping() >> { throw exception.fillInStackTrace() }

    and: "check that the exception bubbled out"
      def e = thrown(Throwable)

      e.is(exception)

    and: "the adapter decided to change state to 'connecting' with an unavailable client that has the exception as the cause if required"
      expected_set_connecting_count * adapter.setConnecting(client, {
        it.cause.is(exception)
      }, true, CONNECTED) >> null

    where:
      exception                                                     || expected_set_connecting_count
      new SolrServerException("testing")                            || 1
      new SolrException(SolrException.ErrorCode.UNKNOWN, "testing") || 1
      new Error("testing")                                          || 1
      new RuntimeException("testing")                               || 1
      new OutOfMemoryError("testing")                               || 0
  }

  def 'test getClient()'() {
    given:
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    expect:
      adapter.client.is(adapter)
  }

  def 'test getCore()'() {
    given:
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    expect:
      adapter.core == CORE
  }

  @Unroll
  def 'test close() when #when'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and:
      adapter.close()

    then: "verify the real client was or wasn't closed"
      expected_client_close * client.close()

    and: "verify futures were or were not cancelled"
      expected_create_future_cancelled * getFuture.cancel(_)
      expected_ping_future_cancelled * runFuture.cancel(_)

    and: "the state is now 'closed'"
      adapter.state == CLOSED

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "it shouldn't be the real client"
      !proxyClient.is(client)

    and: "the proxied client is unavailable indicating the adapter is closed"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("'$CORE' client was closed")

    where:
      when         || simulate_connecting | simulate_connected || expected_client_close | expected_create_future_cancelled | expected_ping_future_cancelled
      'creating'   || false               | false              || 0                     | 1                                | 0
      'connecting' || true                | false              || 1                     | 0                                | 1
      'connected'  || true                | true               || 1                     | 0                                | 0
  }

  def 'test close() when already closed'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "close the adapter a first time"
      adapter.close()

    then: "verify the real client was closed"
      1 * client.close()

    and: "the state is now 'closed'"
      adapter.state == CLOSED

    when: "closing the adapter again"
      adapter.close()

    then: "verify the real client was not closed again"
      0 * client.close()

    and: "the state is still 'closed'"
      adapter.state == CLOSED

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the proxied client is still unavailable indicating the adapter is closed"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("'$CORE' client was closed")
  }

  def 'test close() when client throws IOException when closing'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "close the adapter"
      adapter.close()

    then: "verify the real client was closed"
      1 * client.close() >> { new IOException('testing') }

    and: "the exception didn't bubble out"
      noExceptionThrown()

    and: "the state is now 'closed'"
      adapter.state == CLOSED

    when:
      def proxyClient = adapter.getProxiedClient()

    then: "the proxied client is unavailable indicating the adapter is closed"
      proxyClient instanceof UnavailableSolrClient
      proxyClient.cause instanceof UnavailableSolrException
      proxyClient.cause.message.contains("'$CORE' client was closed")
  }

  @Unroll
  def 'test isAvailable() when #when'() {
    given: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(Stub(SolrClient), Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "closing the adapter if requested"
      if (close) {
        adapter.close()
      }

    then:
      adapter.isAvailable() == available

    where:
      when         || simulate_connecting | simulate_connected | close || available
      'creating'   || false               | false              | false || false
      'connecting' || true                | false              | false || false
      'connected'  || true                | true               | false || true
      'closed'     || true                | true               | true  || false
  }

  def 'test isAvailable() with null listener'() {
    given:
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "calling isAvailable() with null listener"
      adapter.isAvailable(null)

    then: "it should fail"
      def e = thrown(IllegalArgumentException)

      e.message.contains('invalid null listener')
  }

  def 'test isAvailable() with null time unit'() {
    given: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "calling isAvailable() with null time unit"
      adapter.isAvailable(5L, 1L,null)

    then: "it should fail"
      def e = thrown(IllegalArgumentException)

      e.message.contains('invalid null time unit')
  }

  def 'test whenAvailable() with null initializer'() {
    given: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, failsafeCreator, failsafeCreator)

    when: "calling whenAvailable() with null initializer"
      adapter.whenAvailable(null)

    then: "it should fail"
      def e = thrown(IllegalArgumentException)

      e.message.contains('invalid null initializer')
  }

  def 'test checkIsReachable() is delegated to the real client which returns an OK status when connected'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachable('when testing')

    then: "verify the call was delegated to the real client"
      1 * client.ping() >> pingOkResponse

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)
  }

  @Unroll
  def 'test checkIfReachable() delegates a ping to the real client which returns #returns_what when #when'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    when: "checking if the adapter is reachable"
      adapter.checkIfReachable('when testing')

    then: "verify the call was delegated to the real client and it simulates an appropriate response"
      1 * client.ping() >> response

    and: "verify that an unavailable exception is thrown back with the expected message"
      def e = thrown(UnavailableSolrException)

      e.message.contains(message)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)

    where:
      returns_what      | when         || simulate_connecting | simulate_connected | response             || message
      'no response'     | 'connecting' || true                | false              | null                 || 'no response'
      'no response'     | 'connected'  || true                | true               | null                 || 'no response'
      'no status'       | 'connecting' || true                | false              | pingNoStatusResponse || 'with null status'
      'no status'       | 'connected'  || true                | true               | pingNoStatusResponse || 'with null status'
      'a non-OK status' | 'connecting' || true                | false              | pingFailResponse     || 'with FAIL status'
      'a non-OK status' | 'connected'  || true                | true               | pingFailResponse     || 'with FAIL status'
  }

  @Unroll
  def 'test checkIfReachable() does not delegate a ping to the real client when #when'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "closing the adapter if requested"
      if (close) {
        adapter.close()
      }

    when: "checking if the adapter is reachable"
      adapter.checkIfReachable('when testing')

    then: "verify the call was not delegated to the real client"
      0 * client.ping()

    and: "check that an unavailable exception bubbled out with a message indicating the current state of the client"
      def e = thrown(UnavailableSolrException)

      e.message.contains(message)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)

    where:
      when       || simulate_connecting | simulate_connected | close || message
      'creating' || false               | false              | false || "initializing '$CORE' client"
      'closed'   || true                | true               | true  || "'$CORE' client was closed"
  }

  @Unroll
  def 'test checkIfReachable() delegates a ping to the real client which throws #exception.class.simpleName'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachable('when testing')

    then: "verify the call was delegated to the real client and let it throw the exception"
      1 * client.ping() >> { throw exception.fillInStackTrace() }

    and: "check that an unvailable exception bubbled out with the exception as the cause"
      def e = thrown(Throwable)

      e.message.contains('ping failed')
      e.cause.is(exception)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)

    where:
      exception << [new SolrServerException("testing"), new SolrException(SolrException.ErrorCode.UNKNOWN, "testing"), new Error("testing"),
          new RuntimeException("testing")]
  }

  def 'test checkIfReachable() delegates a ping to the real client which throws OutOfMemoryError'() {
    given:
      def client = Mock(SolrClient)
      def exception = new OutOfMemoryError('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachable('when testing')

    then: "verify the call was delegated to the real client and let it throw the exception"
      1 * client.ping() >> { throw exception.fillInStackTrace() }

    and: "check that the exception bubbled out"
      def e = thrown(Throwable)

      e.is(exception)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)
  }

  def 'test checkIfReachableAndChangeStateAccordingly() delegates a ping to the real client which returns an OK status when connected'() {
    given:
      def client = Mock(SolrClient)
      def error = new Exception('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachableAndChangeStateAccordingly(error, 'when testing')

    then: "verify the call was delegated to the real client"
      1 * client.ping() >> pingOkResponse

    and: "the adapter decided to change state to 'connected' again"
      1 * adapter.setConnected(true) >> null
  }

  @Unroll
  def 'test checkIfReachableAndChangeStateAccordingly() delegates a ping to the real client which returns #returns_what when #when'() {
    given:
      def client = Mock(SolrClient)
      def error = new Exception('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    when: "checking if the adapter is reachable"
      adapter.checkIfReachableAndChangeStateAccordingly(error, 'when testing')

    then: "verify the call was delegated to the real client and it simulates an appropriate response"
      1 * client.ping() >> response

    and: "the adapter decided to change state to 'connecting' with an unavailable client that holds the exception passed in parameter"
      1 * adapter.setConnecting(client, {
        it.cause.is(error)
      }, true, CONNECTED) >> null

    where:
      returns_what      | when         || simulate_connecting | simulate_connected | response
      'no response'     | 'connecting' || true                | false              | null
      'no response'     | 'connected'  || true                | true               | null
      'no status'       | 'connecting' || true                | false              | pingNoStatusResponse
      'no status'       | 'connected'  || true                | true               | pingNoStatusResponse
      'a non-OK status' | 'connecting' || true                | false              | pingFailResponse
      'a non-OK status' | 'connected'  || true                | true               | pingFailResponse
  }

  @Unroll
  def 'test checkIfReachableAndChangeStateAccordingly() does not delegate a ping to the real client when #when'() {
    given:
      def client = Mock(SolrClient)
      def error = new Exception('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "closing the adapter if requested"
      if (close) {
        adapter.close()
      }

    when: "checking if the adapter is reachable"
      adapter.checkIfReachableAndChangeStateAccordingly(error, 'when testing')

    then: "verify the call was not delegated to the real client"
      0 * client.ping()

    and: "check that no exception bubbles out"
      noExceptionThrown()

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)

    where:
      when       || simulate_connecting | simulate_connected | close
      'creating' || false               | false              | false
      'closed'   || true                | true               | true
  }

  @Unroll
  def 'test checkIfReachableAndChangeStateAccordingly() delegates a ping to the real client which throws #exception.class.simpleName'() {
    given:
      def client = Mock(SolrClient)
      def error = new Exception('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachableAndChangeStateAccordingly(error, 'when testing')

    then: "verify the call was delegated to the real client and let it throw the exception"
      1 * client.ping() >> { throw exception.fillInStackTrace() }

    and: "check that no exception bubbles out"
      noExceptionThrown()

    and: "the adapter decided to change state to 'connecting' with an unavailable client that holds the exception passed in parameter"
      1 * adapter.setConnecting(client, {
        it.cause.is(error)
      }, true, CONNECTED) >> null

    where:
      exception << [new SolrServerException("testing"), new SolrException(SolrException.ErrorCode.UNKNOWN, "testing"), new Error("testing"), new RuntimeException("testing")]
  }

  def 'test checkIfReachableAndChangeStateAccordingly() delegates a ping to the real client which throws OutOfMemoryError'() {
    given:
      def client = Mock(SolrClient)
      def exception = new OutOfMemoryError('testing')
      def error = new Exception('testing')

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    when: "checking if the adapter is reachable"
      adapter.checkIfReachableAndChangeStateAccordingly(error, 'when testing')

    then: "verify the call was delegated to the real client and let it throw the exception"
      1 * client.ping() >> { throw exception.fillInStackTrace() }

    and: "check that the exception bubbled out"
      def e = thrown(Throwable)

      e.is(exception)

    and: "the adapter shouldn't decide to change state"
      0 * adapter./set.*/(*_)
  }

  @ClearInterruptions
  def 'test logInterruptionAndRecreate() propagates the interruption and changes the state to creating again'() {
    given:
      def error = new Throwable('testing')

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be creating"
      adapter.state == CREATING

    when:
      adapter.logInterruptionAndRecreate(error)

    then:
      Thread.interrupted()

    and: "the adapter is changing state to creating again with the original unavailable client while asking not to cancel the previous future"
      1 * adapter.setCreating(adapter.unavailableClient, false)
  }

  @ClearInterruptions
  def 'test logInterruptionAndReconnectIfStillConnecting() propagates the interruption and changes the state to connecting again'() {
    given:
      def client = Stub(SolrClient)
      def error = new Throwable('testing')

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should be connecting"
      adapter.state == CONNECTING

    when:
      adapter.logInterruptionAndReconnectIfStillConnecting(error)

    then:
      Thread.interrupted()

    and: "the adapter is changing state to connecting again with the original real & unavailable clients while asking not to cancel the previous future"
      1 * adapter.setConnecting(client, adapter.unavailableClient, false, CONNECTING)
  }

  @Unroll
  def 'test logAndRecreateIfNotCancelled() #does_what when called with #exception.class.simpleName'() {
    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be creating"
      adapter.state == CREATING

    when:
      adapter.logAndRecreateIfNotCancelled(exception)

    then: "the adapter is changing state to creating again if not canceled with the original unavailable client while asking not to cancel the previous future"
      expected_set_creating_count * adapter.setCreating(adapter.unavailableClient, false)

    where:
      does_what                             || exception                            || expected_set_creating_count
      'changes the state to creating again' || new Throwable('testing')             || 1
      'does nothing'                        || new CancellationException('testing') || 0
  }

  @Unroll
  def 'test logAndReconnectIfNotCancelledAndStillConnecting() #does_what when called with #exception.class.simpleName'() {
    given:
      def client = Stub(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation by failsafe if requested"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should be connecting"
      adapter.state == CONNECTING

    when:
      adapter.logAndReconnectIfNotCancelledAndStillConnecting(exception)

    then: "the adapter is changing state to connecting again if not canceled with the original real & unavailable clients while asking not to cancel the previous future"
      expected_set_connecting_count * adapter.setConnecting(client, adapter.unavailableClient, false, CONNECTING)

    where:
      does_what                               || exception                            || expected_set_connecting_count
      'changes the state to connecting again' || new Throwable('testing')             || 1
      'does nothing'                          || new CancellationException('testing') || 0
  }

  def 'test logAndSetConnecting() changes the state to connecting'() {
    given:
      def client = Stub(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    then: "the adapter should be creating"
      adapter.state == CREATING

    when:
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter is changing state to connecting with the original real & unavailable clients while asking not to cancel the previous future"
      1 * adapter.setConnecting(client, adapter.unavailableClient, false, {
        it as Set == [CREATING, CONNECTING, CONNECTED] as Set
      })
  }

  def 'test logAndSetConnected() changes the state to connected'() {
    given:
      def client = Stub(SolrClient)

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    then: "the adapter should be connecting"
      adapter.state == CONNECTING

    when:
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    then: "the adapter is changing state to connected while asking not to cancel the previous future"
      1 * adapter.setConnected(false)
  }

  @Unroll
  def 'test setCreating() triggers failsafe to create a new client when moving from #starting_at and #requesting_or_not to cancel the previous future'() {
    given:
      def unavailableClient = new UnavailableSolrClient(new Exception('testing'))
      def initializer = Mock(org.codice.solr.client.solrj.SolrClient.Initializer)
      def listener = Mock(org.codice.solr.client.solrj.SolrClient.Listener)

      if (start_real_client == 'client') {
        start_real_client = client
      }
      if (start_api_client == 'client') {
        start_api_client = client
      }
      if (start_ping_client == 'client') {
        start_ping_client = client
      }

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator, directExecutor])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "registering the initializer and the listener"
      adapter.whenAvailable(initializer)
      adapter.isAvailable(listener)

    then: "the adapter should be in the state we are starting with"
      adapter.state == start_state

    and: "the unavailable client should be one expected to start with"
      if (start_unavailable_client instanceof String) {
        adapter.unavailableClient.cause.message.contains(start_unavailable_client)
      } else {
        adapter.unavailableClient.is(start_unavailable_client)
      }

    and: "the real client should be what we expect to start with"
      adapter.realClient.is(start_real_client)

    and: "the ping and the api clients should be the ones we expect to start with"
      if (start_ping_client == 'unavailable') {
        adapter.pingClient.is(adapter.unavailableClient)
      } else {
        adapter.pingClient.is(start_ping_client)
      }
      if (start_api_client == 'unavailable') {
        adapter.apiClient.is(adapter.unavailableClient)
      } else {
        adapter.apiClient.is(start_api_client)
      }

    when: "changing the state to creating with a new unavailable client"
      adapter.setCreating(unavailableClient, cancel_future)

    then: "the adapter state should now be creating"
      adapter.state == CREATING

    and: "the unavailable client should be the new one"
      adapter.unavailableClient.is(unavailableClient)

    and: "the real client should now be null"
      adapter.realClient == null

    and: "the ping and the api clients should be the new unavailable one"
      adapter.pingClient.is(unavailableClient)
      adapter.apiClient.is(unavailableClient)

    and: "the future should have been replaced with a new one indicating we triggered failsafe to re-create"
      adapter.future.is(getFuture2)

    and: "the previous get future (a.k.a create) should have been canceled if requested"
      get_future_cancel_count * getFuture.cancel(true)

    and: "the previous run future (a.k.a connect/ping) should have been canceled if requested"
      run_future_cancel_count * runFuture.cancel(true)

    and: "verify if the previous client was closed if we were available"
      client_close_count * client.close()

    and: "verify the initializer should not have been notified since we didn't go available"
      0 * initializer.initialized(_)

    and: "verify the listener should have been called if were were available before since we are changing state back to unavailable"
      listener_count * listener.changed(adapter, false)

    where:
      start_state | requesting_or_not || simulate_connecting | simulate_connected || start_real_client | start_unavailable_client      | start_api_client | start_ping_client || cancel_future || get_future_cancel_count | run_future_cancel_count | client_close_count | listener_count
      CREATING    | 'requesting'      || false               | false              || null              | "initializing '$CORE' client" | 'unavailable'    | 'unavailable'     || true          || 1                       | 0                       | 0                  | 0
      CREATING    | 'not requesting'  || false               | false              || null              | "initializing '$CORE' client" | 'unavailable'    | 'unavailable'     || false         || 0                       | 0                       | 0                  | 0
      CONNECTING  | 'requesting'      || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || true          || 0                       | 1                       | 1                  | 0
      CONNECTING  | 'requesting'      || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || false         || 0                       | 0                       | 1                  | 0
      CONNECTED   | 'requesting'      || true                | true               || 'client'          | null                          | 'client'         | 'client'          || true          || 0                       | 0                       | 1                  | 1
      CONNECTED   | 'not requesting'  || true                | true               || 'client'          | null                          | 'client'         | 'client'          || false         || 0                       | 0                       | 1                  | 1

      starting_at = start_state.name().toLowerCase()
  }

  def 'test setCreating() does nothing when closed'() {
    given:
      def unavailableClient = new UnavailableSolrClient(new Exception('testing'))
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    and: "close the adapter"
      adapter.close();
      def closeClient = adapter.unavailableClient

    when: "changing the state to creating"
      adapter.setCreating(unavailableClient, true)

    then: "the adapter state should still be closed"
      adapter.state == CLOSED

    and: "the unavailable client should be same closed one as before"
      adapter.unavailableClient.is(closeClient)

    and: "the real client should be null"
      adapter.realClient == null

    and: "the ping and the api clients should be the closed one"
      adapter.pingClient.is(closeClient)
      adapter.apiClient.is(closeClient)

    and: "there should be no future"
      adapter.future == null

    and: "no futures should have been canceled"
      0 * getFuture.cancel(_)
      0 * getFuture2.cancel(_)
      0 * runFuture.cancel(_)
      0 * runFuture2.cancel(_)

    and: "verify the original client didn't need to be closed again"
      0 * client.close()
  }

  @Unroll
  def 'test setConnecting() triggers failsafe to connect to a client when moving from #starting_at and #requesting_or_not to cancel the previous future and using #using_client'() {
    given:
      def unavailableClient = new UnavailableSolrClient(new Exception('testing'))
      def initializer = Mock(org.codice.solr.client.solrj.SolrClient.Initializer)
      def listener = Mock(org.codice.solr.client.solrj.SolrClient.Listener)

      if (start_real_client == 'client') {
        start_real_client = client
      }
      if (start_api_client == 'client') {
        start_api_client = client
      }
      if (start_ping_client == 'client') {
        start_ping_client = client
      }
      if (new_real_client == 'client') {
        new_real_client = client
      } else if (new_real_client == 'new client') {
        new_real_client = Mock(SolrClient)
      }

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator, directExecutor])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "registering the initializer and the listener"
      adapter.whenAvailable(initializer)
      adapter.isAvailable(listener)

    then: "the adapter should be in the state we are starting with"
      adapter.state == start_state

    and: "the unavailable client should be one expected to start with"
      if (start_unavailable_client instanceof String) {
        adapter.unavailableClient.cause.message.contains(start_unavailable_client)
      } else {
        adapter.unavailableClient.is(start_unavailable_client)
      }

    and: "the real client should be what we expect to start with"
      adapter.realClient.is(start_real_client)

    and: "the ping and the api clients should be the ones we expect to start with"
      if (start_ping_client == 'unavailable') {
        adapter.pingClient.is(adapter.unavailableClient)
      } else {
        adapter.pingClient.is(start_ping_client)
      }
      if (start_api_client == 'unavailable') {
        adapter.apiClient.is(adapter.unavailableClient)
      } else {
        adapter.apiClient.is(start_api_client)
      }

    when: "changing the state to connecting with new real and unavailable clients"
      adapter.setConnecting(new_real_client, unavailableClient, cancel_future, start_state)

    then: "the adapter state should now be connecting"
      adapter.state == CONNECTING

    and: "the unavailable client should be the new one"
      adapter.unavailableClient.is(unavailableClient)

    and: "the real client should now be the new client"
      adapter.realClient.is(new_real_client)

    and: "the ping client should be the new client"
      adapter.pingClient.is(new_real_client)

    and: "the api client should be the new unavailable one"
      adapter.apiClient.is(unavailableClient)

    and: "the future should have been replaced with a new one indicating we triggered failsafe to re-connect"
      adapter.future.is((start_state == CREATING) ? runFuture : runFuture2) // if we were creating then this is the first time

    and: "the previous get future (a.k.a create) should have been canceled if requested"
      get_future_cancel_count * getFuture.cancel(true)

    and: "the previous run future (a.k.a connect/ping) should have been canceled if requested"
      run_future_cancel_count * runFuture.cancel(true)

    and: "verify if the previous client was closed if requested"
      client_close_count * client.close()

    and: "verify the initializer should not have been notified since we didn't go available"
      0 * initializer.initialized(_)

    and: "verify the listener should have been called if were were available before since we are changing state back to unavailable"
      listener_count * listener.changed(adapter, false)

    where:
      start_state | requesting_or_not | using_client      || simulate_connecting | simulate_connected || start_real_client | start_unavailable_client      | start_api_client | start_ping_client || cancel_future | new_real_client || get_future_cancel_count | run_future_cancel_count | client_close_count | listener_count
      CREATING    | 'requesting'      | 'a new client'    || false               | false              || null              | "initializing '$CORE' client" | 'unavailable'    | 'unavailable'     || true          | 'new client'    || 1                       | 0                       | 0                  | 0
      CREATING    | 'not requesting'  | 'a new client'    || false               | false              || null              | "initializing '$CORE' client" | 'unavailable'    | 'unavailable'     || false         | 'new client'    || 0                       | 0                       | 0                  | 0
      CONNECTING  | 'requesting'      | 'a new client'    || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || true          | 'new client'    || 0                       | 1                       | 1                  | 0
      CONNECTING  | 'requesting'      | 'the same client' || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || true          | 'client'        || 0                       | 1                       | 0                  | 0
      CONNECTING  | 'requesting'      | 'a new client'    || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || false         | 'new client'    || 0                       | 0                       | 1                  | 0
      CONNECTING  | 'requesting'      | 'the same client' || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || false         | 'client'        || 0                       | 0                       | 0                  | 0
      CONNECTED   | 'requesting'      | 'a new client'    || true                | true               || 'client'          | null                          | 'client'         | 'client'          || true          | 'new client'    || 0                       | 0                       | 1                  | 1
      CONNECTED   | 'requesting'      | 'the same client' || true                | true               || 'client'          | null                          | 'client'         | 'client'          || true          | 'client'        || 0                       | 0                       | 0                  | 1
      CONNECTED   | 'not requesting'  | 'a new client'    || true                | true               || 'client'          | null                          | 'client'         | 'client'          || false         | 'new client'    || 0                       | 0                       | 1                  | 1
      CONNECTED   | 'not requesting'  | 'the same client' || true                | true               || 'client'          | null                          | 'client'         | 'client'          || false         | 'client'        || 0                       | 0                       | 0                  | 1

      starting_at = start_state.name().toLowerCase()
  }

  @Unroll
  def 'test setConnecting() does nothing when #starting_at and requesting to only change the state only if the adapter is #only_if_name'() {
    given:
      def unavailableClient = new UnavailableSolrClient(new Exception('testing'))
      def newClient = Mock(SolrClient)
      def initializer = Mock(org.codice.solr.client.solrj.SolrClient.Initializer)
      def listener = Mock(org.codice.solr.client.solrj.SolrClient.Listener)

      if (start_real_client == 'client') {
        start_real_client = client
      }
      if (start_ping_client == 'client') {
        start_ping_client = client
      }
      if (start_api_client == 'client') {
        start_api_client = client
      }

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator, directExecutor])

    and: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }
      def originalUnavailableClient = adapter.unavailableClient
      def originalPingClient = adapter.pingClient
      def originalApiClient = adapter.apiClient
      def originalFuture = adapter.future

    and: "registering the initializer and the listener"
      adapter.whenAvailable(initializer)
      adapter.isAvailable(listener)

    then: "the adapter should be in the state we are starting with"
      adapter.state == start_state

    and: "the unavailable client should be one expected to start with"
      if (start_unavailable_client instanceof String) {
        adapter.unavailableClient.cause.message.contains(start_unavailable_client)
      } else {
        adapter.unavailableClient.is(start_unavailable_client)
      }

    and: "the real client should be what we expect to start with"
      adapter.realClient.is(start_real_client)

    and: "the ping and the api clients should be the ones we expect to start with"
      if (start_ping_client == 'unavailable') {
        adapter.pingClient.is(adapter.unavailableClient)
      } else {
        adapter.pingClient.is(start_ping_client)
      }
      if (start_api_client == 'unavailable') {
        adapter.apiClient.is(adapter.unavailableClient)
      } else {
        adapter.apiClient.is(start_api_client)
      }

    when: "changing the state to connecting with new real and unavailable clients"
      adapter.setConnecting(newClient, unavailableClient, true, only_if)

    then: "the adapter state should not have changed"
      adapter.state == start_state

    and: "the unavailable client should not have changed"
      adapter.unavailableClient.is(originalUnavailableClient)

    and: "the real client should not have changed"
      adapter.realClient.is(start_real_client)

    and: "the ping client should not have changed"
      adapter.pingClient.is(originalPingClient)

    and: "the api client should not have changed"
      adapter.apiClient.is(originalApiClient)

    and: "the future not have changed"
      adapter.future.is(originalFuture)

    and: "no futures should have been canceled"
      0 * getFuture.cancel(_)
      0 * getFuture2.cancel(_)
      0 * runFuture.cancel(_)
      0 * runFuture2.cancel(_)

    and: "verify the original client didn't need to be closed again"
      0 * client.close()

    and: "verify the initializer should not have been notified since we didn't do anything"
      0 * initializer.initialized(_)

    and: "verify the listener should not have been called as we didn't do anything"
      0 * listener.changed(*_)

    where:
      start_state || simulate_connecting | simulate_connected || start_real_client | start_unavailable_client      | start_api_client | start_ping_client || only_if
      CREATING    || false               | false              || null              | "initializing '$CORE' client" | 'unavailable'    | 'unavailable'     || CONNECTED
      CONNECTING  || true                | false              || 'client'          | "initializing '$CORE' client" | 'unavailable'    | 'client'          || CONNECTED
      CONNECTED   || true                | true               || 'client'          | null                          | 'client'         | 'client'          || CONNECTING

      starting_at = start_state.name().toLowerCase()
      only_if_name = only_if.name().toLowerCase()
  }

  def 'test setConnecting() does nothing when closed'() {
    given:
      def newClient = Mock(SolrClient)
      def unavailableClient = new UnavailableSolrClient(new Exception('testing'))
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful client creation by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))

    and: "close the adapter"
      adapter.close();
      def closeClient = adapter.unavailableClient

    when: "changing the state to connecting"
      adapter.setConnecting(newClient, unavailableClient, false, CONNECTED)

    then: "the adapter state should still be closed"
      adapter.state == CLOSED

    and: "the unavailable client should be same closed one as before"
      adapter.unavailableClient.is(closeClient)

    and: "the real client should be null"
      adapter.realClient == null

    and: "the ping and the api clients should be the closed one"
      adapter.pingClient.is(closeClient)
      adapter.apiClient.is(closeClient)

    and: "there should be no future"
      adapter.future == null

    and: "no futures should have been canceled"
      0 * getFuture.cancel(_)
      0 * getFuture2.cancel(_)
      0 * runFuture.cancel(_)
      0 * runFuture2.cancel(_)

    and: "verify the original client or the new client didn't need to be closed again"
      0 * client.close()
      0 * newClient.close()
  }

  @Unroll
  def 'test setConnected() when moving from #starting_at and #requesting_or_not to cancel the previous future'() {
    given:
      def initializer = Mock(org.codice.solr.client.solrj.SolrClient.Initializer)
      def listener = Mock(org.codice.solr.client.solrj.SolrClient.Listener)

      if (start_api_client == 'client') {
        start_api_client = client
      }

    when: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator, directExecutor])

    and: "simulating a successful creation and connection by failsafe if requested"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "registering the initializer and the listener"
      adapter.whenAvailable(initializer)
      adapter.isAvailable(listener)

    then: "the adapter should be in the state we are starting with"
      adapter.state == start_state

    and: "the unavailable client should be one expected to start with"
      if (start_unavailable_client instanceof String) {
        adapter.unavailableClient.cause.message.contains(start_unavailable_client)
      } else {
        adapter.unavailableClient.is(start_unavailable_client)
      }

    and: "the real client should be our client"
      adapter.realClient.is(client)

    and: "the ping client should be our client"
      adapter.pingClient.is(client)

    and: "the api client should be the one we expect to start with"
      if (start_api_client == 'unavailable') {
        adapter.apiClient.is(adapter.unavailableClient)
      } else {
        adapter.apiClient.is(start_api_client)
      }

    when: "changing the state to connected with a new unavailable client"
      adapter.setConnected(cancel_future)

    then: "the adapter state should now be connected"
      adapter.state == CONNECTED

    and: "the unavailable client should be null"
      adapter.unavailableClient == null

    and: "the real client should now be our client"
      adapter.realClient == client

    and: "the ping and the api clients should be our client"
      adapter.pingClient.is(client)
      adapter.apiClient.is(client)

    and: "the future is now null"
      adapter.future == null

    and: "the previous get future (a.k.a create) should have been canceled if requested"
      get_future_cancel_count * getFuture.cancel(true)

    and: "the previous run future (a.k.a connect/ping) should have been canceled if requested"
      run_future_cancel_count * runFuture.cancel(true)

    and: "verify the client is never closed"
      0 * client.close()

    and: "verify the initializer should have been notified if we became available"
      initializer_count * initializer.initialized(adapter)

    and: "verify the listener should have been called if were were available before since we are changing state back to unavailable"
      listener_count * listener.changed(adapter, true)

    where:
      start_state | requesting_or_not || simulate_connected || start_unavailable_client      | start_api_client || cancel_future || get_future_cancel_count | run_future_cancel_count | listener_count | initializer_count
      CONNECTING  | 'requesting'      || false              || "initializing '$CORE' client" | 'unavailable'    || true          || 0                       | 1                       | 1              | 1
      CONNECTING  | 'requesting'      || false              || "initializing '$CORE' client" | 'unavailable'    || false         || 0                       | 0                       | 1              | 1
      CONNECTED   | 'requesting'      || true               || null                          | 'client'         || true          || 0                       | 0                       | 0              | 0
      CONNECTED   | 'not requesting'  || true               || null                          | 'client'         || false         || 0                       | 0                       | 0              | 0

      starting_at = start_state.name().toLowerCase()
  }

  def 'test setConnected() does nothing when closed'() {
    given:
      def client = Mock(SolrClient)

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    and: "simulating a successful client creation and connection by failsafe"
      adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      adapter.logAndSetConnected(null, Stub(ExecutionContext))

    and: "close the adapter"
      adapter.close();
      def closeClient = adapter.unavailableClient

    when: "changing the state to connected"
      adapter.setConnected(true)

    then: "the adapter state should still be closed"
      adapter.state == CLOSED

    and: "the unavailable client should be same closed one as before"
      adapter.unavailableClient.is(closeClient)

    and: "the real client should be null"
      adapter.realClient == null

    and: "the ping and the api clients should be the closed one"
      adapter.pingClient.is(closeClient)
      adapter.apiClient.is(closeClient)

    and: "there should be no future"
      adapter.future == null

    and: "no futures should have been canceled"
      0 * getFuture.cancel(_)
      0 * getFuture2.cancel(_)
      0 * runFuture.cancel(_)
      0 * runFuture2.cancel(_)

    and: "verify the original client didn't need to be closed again"
      0 * client.close()
  }
}
