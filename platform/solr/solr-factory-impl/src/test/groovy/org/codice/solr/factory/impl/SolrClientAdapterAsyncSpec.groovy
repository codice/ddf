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

import net.jodah.failsafe.*
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrRequest
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.apache.solr.common.SolrException
import org.apache.solr.common.SolrException.ErrorCode
import org.apache.solr.common.util.NamedList
import org.codice.junit.DeFinalize
import org.codice.junit.DeFinalizer
import org.codice.solr.client.solrj.SolrClient.Initializer
import org.codice.solr.client.solrj.SolrClient.Listener
import org.codice.solr.client.solrj.UnavailableSolrException
import org.codice.solr.factory.impl.SolrClientAdapter.Creator
import org.codice.spock.ClearInterruptions
import org.junit.runner.RunWith
import org.spockframework.mock.runtime.MockInvocation
import org.spockframework.runtime.SpockTimeoutError
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll
import spock.util.concurrent.AsyncConditions

import java.util.concurrent.CancellationException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.TimeUnit.*
import static net.jodah.failsafe.Actions.*
import static org.codice.solr.factory.impl.SolrClientAdapter.State.CLOSED;

@ClearInterruptions
@Timeout(SolrClientAdapterAsyncSpec.TIMEOUT_IN_SECS)
@RunWith(DeFinalizer)
@DeFinalize(SolrClientAdapter)
class SolrClientAdapterAsyncSpec extends Specification {
  static final String CORE = "test_core"
  static final String COLLECTION = 'collection'
  static final int TIMEOUT_IN_SECS = 25
  static final String PING_ERROR_MSG = 'failing ping'

  @Shared
  def client = Stub(SolrClient)
  def creator = Stub(Creator)

  def request = Stub(SolrRequest)
  def response = Stub(NamedList)

  def pingFail = new UnavailableSolrException("ping failed with 'FAIL'")
  def pingError = new SolrException(ErrorCode.UNKNOWN, PING_ERROR_MSG)

  def pingOkResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> 'OK'
    }
  }
  def pingFailResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> 'FAIL'
    }
  }
  def pingErrorResponse = Mock(SolrPingResponse) {
    getResponse() >> Mock(NamedList) {
      get('status') >> {
        throw pingError
      }
    }
  }

  def failsafeCreator = {
    Mock(SyncFailsafe) {
      with(_ as ScheduledExecutorService) >> Mock(AsyncFailsafe) {
        /on.*/(*_) >> { (delegate as MockInvocation).mockObject.instance } // itself
        get(_) >> Stub(FailsafeFuture)
        run(_) >> Stub(FailsafeFuture)
      }
    }
  }

  @Unroll
  def 'test constructor when client is created after #created_after and becomes available'() {
    given: "a client and creator to verify that failsafe actually delegates to our creator and client"
      def client = Mock(SolrClient) {
        1 * ping() >> pingOkResponse
      }
      def creator = Mock(Creator) {
        1 * create() >> client
      }

    and: "controllers that will actually proceed through Solr"
      def createController = new FailsafeController('SolrClient Creation') >> {
        if (return_or_throw == SolrClient) {
          doProceed()
        } else {
          // null or exception
          doThrowOrReturn(return_or_throw).then().doProceed()
        }
      }
      def pingController = new FailsafeController('SolrClient Ping') >> doProceed()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    then: "the reported core should correspond to the requested one"
      adapter.core == CORE

    and:
      "our creator should be registered with the adapter"
      adapter.creator.is(creator)

    when: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.isAvailable()

    and: "the underlying client should be the one we created"
      adapter.proxiedClient.is(client)

    and: "the underlying client should never be closed"
      0 * client.close()

    and: "there should be no registered initializers or listeners"
      !adapter.hasInitializers() && !adapter.hasListeners()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      // return_or_throw -> null to return null, exception to be thrown, SolrClient to return a client
      created_after                                                    || return_or_throw
      '1 attempt'                                                      || SolrClient
      '2 attempts where the first one returned null'                   || null
      '2 attempts where the first one failed with SolrServerException' || SolrServerException
      '2 attempts where the first one failed with SolrException'       || new SolrException(ErrorCode.UNKNOWN, 'testing')
      '2 attempts where the first one failed with IOException'         || IOException
      '2 attempts where the first one failed with RutimeException'     || RuntimeException
  }

  @Unroll
  // the following needs to be changed as it should be closing the whole thing and not re-execute the creation
  def 'test constructor when client is created after 2 attempts where the first one is interrupted with #exception.simpleName and becomes available'() {
    given: "a client and creator to verify that failsafe actually delegates to our creator and client"
      def client = Mock(SolrClient) {
        1 * ping() >> pingOkResponse
      }
      def creator = Mock(Creator) {
        1 * create() >> client
      }

    and: "controllers that will actually proceed through Solr"
      def createController = new FailsafeController('SolrClient Creation') >>> [
          doThrow(exception),
          doProceed()
      ]
      def pingController = new FailsafeController('SolrClient Ping') >> doProceed()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    then: "the reported core should correspond to the requested one"
      adapter.core == CORE

    and:
      "our creator should be registered with the adapter"
      adapter.creator.is(creator)

    when: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.isAvailable()

    and: "the underlying client should be the one we created"
      adapter.proxiedClient.is(client)

    and: "the underlying client should never be closed"
      0 * client.close()

    and: "there should be no registered initializers or listeners"
      !adapter.hasInitializers() && !adapter.hasListeners()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      exception << [InterruptedException, InterruptedIOException]
  }

  @Unroll
  def 'test client #test_client_how where an API call also fails to connect after the initial failure and finally recover to become available'() {
    given: "a Solr client that fails direct pings"
      def client = Mock(SolrClient) {
        // in case the real client is consulted for the first API call
        // since either we will be rejected right away if the client is not created but if it is
        // a ping will be issued to the client
        ping() >> pingFailResponse
      }

    and: "controllers that mocks every execution/attempts"
      // be careful here as the mocking conditions are changed based on blocking_create and blocking_ping
      // although not necessarly a good practice, I feel like this avoids duplicating this test 3 times
      // the hope is that this comment is enough to warn developers to not use this approach all the times
      def createController = new FailsafeController('SolrClient Creation') >> {
        waitTo('create').onlyIf(blocking_create)
            .then().doReturn(client)
      }
      def pingController = new FailsafeController('SolrClient Ping') >> {
        waitTo('ping').onlyIf(blocking_ping)
            .then().doReturn()
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "calling any api methods"
      adapter.request(request, COLLECTION)

    then: "the method should not have been delegated to the client"
      0 * client.request(request, COLLECTION)

    and: "verify it throws an unavailable exception indicating we are initializing"
      def e = thrown(UnavailableSolrException)

      e.message.contains("initializing '$CORE' client")

    when: "triggering the client creation/connection"
      createController.notifyTo('create')
      pingController.notifyTo('ping')

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.isAvailable()

    when: "calling any api methods"
      def returnedResponse = adapter.request(request, COLLECTION)

    then: "verify the underlying client was called"
      1 * client.request(request, COLLECTION) >> response
      returnedResponse.is(response)

    and: "the underlying client should never be closed"
      0 * client.close()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      test_client_how                                          || blocking_create | blocking_ping
      'creation before it becomes available'                   || true            | false
      'connection as it becomes available'                     || false           | true
      'before creating and connecting as it becomes available' || true            | true
  }

  def 'test client connection as it becomes available where an API call succeeds connecting before the background initialization which is then cancelled'() {
    given: "a client that succeeds direct pings"
      def client = Mock(SolrClient) {
        // called on the first API call to check if we are really still not connected
        1 * ping() >> pingOkResponse
      }

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> {
        waitToBeCancelled()
            .before().throwing(new CancellationException("was cancelled"))
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe creation to complete"
      createController.waitForSuccessfulCompletion()

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "calling any api methods"
      def returnedResponse = adapter.request(request, COLLECTION)

    then: "verify the underlying client was called after all"
      1 * client.request(request, COLLECTION) >> response
      returnedResponse.is(response)

    and: "adapter should be back as available right away"
      adapter.available

    when: "waiting for failsafe ping to complete since it should be cancelled"
      pingController.waitForCompletion()

    then: "verify it was cancelled"
      def e = thrown(ControlledExecutionException)

      e.error instanceof CancellationException

    and: "the adapter should still be available"
      adapter.available

    and: "the underlying client should never be closed"
      0 * client.close()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  @Unroll
  def 'test API calls failing with #exception.class.simpleName and moving state to unavailable while retrying to connect and finally coming back to available'() {
    given: "a client that fails direct pings"
      def client = Mock(SolrClient) {
        // 1st is when 1st api method fails
        // 2nd is when 2nd api method is called
        2 * ping() >> pingErrorResponse
      }

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >>> [
          doReturn(),
          doThrow(pingError)
              .then().doThrow(pingFail).untilNotifiedTo('connect') // simulating a connection that is not yet ready
              .then().doReturn()
      ]

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "calling any api methods"
      adapter.request(request, COLLECTION)

    then: "have it suddenly fail"
      1 * client.request(request, COLLECTION) >> {
        throw exception.fillInStackTrace()
      }
      def e = thrown(Exception)

      e.is(exception)

    and: "the adapter should be unavailable because it failed to connect to the client after the error"
      !adapter.available

    when: "calling any api methods after that should fail right away"
      adapter.request(request, COLLECTION)

    then: "verify it didn't get delegated to the underlying client"
      0 * client.request(request, COLLECTION)

    and: "it threw an unavailable exception indicating we are unavailable because of that failed first api call"
      e = thrown(UnavailableSolrException)
      e.cause.is(exception)

    when: "notifying the ping controller to connect"
      pingController.notifyTo('connect')

    and: "waiting for failsafe to complete"
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available again"
      adapter.available

    when: "calling any api methods"
      def returnedResponse = adapter.request(request, COLLECTION)

    then: "verify the underlying client was called"
      1 * client.request(request, COLLECTION) >> response
      returnedResponse.is(response)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      exception << [new SolrException(ErrorCode.UNKNOWN, 'testing request'), new SolrServerException('testing'), new IOException('testing')]
  }

  def 'test API calls failing with RuntimeException will not move the state to unavailable and will not try to reconnect'() {
    given: "a client"
      def client = Mock(SolrClient)

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    and:
      def exception = new RuntimeException()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "calling any api methods"
      adapter.request(request, COLLECTION)

    then: "have it suddenly fail"
      1 * client.request(request, COLLECTION) >> {
        throw exception.fillInStackTrace()
      }
      def e = thrown(Exception)

      e.is(exception)

    and: "the adapter should still be available"
      adapter.available

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test closing'() {
    given: "a client"
      def client = Mock(SolrClient)

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "closing the adapter (with some initializers and listeners registered)"
      adapter.isAvailable(Stub(Listener))
      adapter.whenAvailable(Stub(Initializer))
      adapter.close()

    then: "underlying client should be closed too"
      1 * client.close() >> null

    and: "the adapter should be unavailable because it was closed"
      !adapter.available

    and: "the adapter is closed"
      adapter.state == CLOSED

    and: "there should be no more registered initializers or listeners"
      !adapter.hasInitializers() && !adapter.hasListeners()

    when: "calling any api methods after that should fail right away"
      adapter.request(request, COLLECTION)

    then: "verify it throws an unavailable exception indicating it was closed"
      def e = thrown(UnavailableSolrException)

      e.message.contains('client was closed')

    and: "verify it didn't get delegated to the underlying client"
      0 * client.request(request, COLLECTION)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test closing when already closed'() {
    given: "a client"
      def client = Mock(SolrClient)

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "closing the adapter for the first time"
      adapter.close()

    then: "underlying client should be closed too"
      1 * client.close() >> null

    when: "closing the adapter again"
      adapter.close()

    then: "it should no longer be relayed to the underlying client"
      0 * client.close()

    and: "the adapter should still be unavailable because it was closed"
      !adapter.available

    and: "the adapter is still closed"
      adapter.state == CLOSED

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  @Unroll
  def 'test closing when underlying client fails with #exception.class.simpleName should still mark the adapter as closed and bubble out the exception'() {
    given: "a client"
      def client = Mock(SolrClient)

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "closing the adapter (with some initializers and listeners registered)"
      adapter.isAvailable(Stub(Listener))
      adapter.whenAvailable(Stub(Initializer))
      adapter.close()

    then: "underlying client should failed to be closed and exception should bubble out"
      1 * client.close() >> { throw exception.fillInStackTrace() }
      def e = thrown(Exception)

      e.is(exception)

    and: "the adapter should be unavailable because it was closed"
      !adapter.available

    and: "the adapter is closed"
      adapter.state == CLOSED

    and: "there should be no more registered initializers or listeners"
      !adapter.hasInitializers() && !adapter.hasListeners()

    when: "calling any api methods after that should fail right away"
      adapter.request(request, COLLECTION)

    then: "verify it throws an unavailable exception indicating it was closed"
      e = thrown(UnavailableSolrException)
      e.message.contains('client was closed')

    and: "verify it didn't get delegated to the underlying client"
      0 * client.request(request, COLLECTION)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      exception << [new IOException("close failed"), new RuntimeException('close failed')]
  }

  def 'test closing just before we finally create a client'() {
    given: "a create controller that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> {
        doNotify('creating')
            .then().waitToBeCancelled().before().returning(client)
      }

    and: "a ping controller that will never be called"
      def pingController = new FailsafeController('SolrClient Ping')

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for the adapter to start creating the client"
      createController.waitFor('creating')

    and: "closing the adapter"
      adapter.close()

    and: "waiting for failsafe create to complete since it should be cancelled and/or complete normally"
      createController.waitForCompletion()

    then: "verify it was cancelled"
      def e = thrown(ControlledExecutionException)

      e.error instanceof CancellationException

    and: "the adapter should be unavailable because it was closed"
      !adapter.available

    and: "the adapter is closed"
      adapter.state == CLOSED

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test closing just before we finally connect to the client'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> {
        doNotify('connecting')
            .then().waitToBeCancelled().before().returning()
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for the adapter to start connecting to the client"
      pingController.waitFor('connecting')

    and: "closing the adapter"
      adapter.close()

    and: "waiting for failsafe to complete since it should be cancelled and/or complete normally"
      createController.waitForCompletion()
      pingController.waitForCompletion()

    then: "verify it was cancelled"
      def e = thrown(ControlledExecutionException)

      e.error instanceof CancellationException

    and: "the adapter should be unavailable because it was closed"
      !adapter.available

    and: "the adapter is closed"
      adapter.state == CLOSED

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test that getting the actual SolrClient should still return the adapter'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    and: "verify the adapter is returned as the Solr client"
      adapter.getClient().is(adapter)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test isAvailable() will not register the listener if the adapter is already closed but will call it once'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "closing the adapter"
      adapter.close()

    and: "registering an availability listener"
      def testThread = Thread.currentThread()
      def conds = new AsyncConditions()
      def listener = Mock(Listener) {
        1 * changed(adapter, false) >> {
          conds.evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
        }
      }
      def available = adapter.isAvailable(listener)

    then: "the adapter should still be reported as unavailable"
      !available

    and: "the listener should not have been registered"
      !adapter.hasListeners()

    and: "the listener should be called back eventually from a different thread"
      conds.await(TIMEOUT_IN_SECS)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test isAvailable() with timeout when the adapter is already available'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    and: "even when checked with a timeout"
      adapter.isAvailable(TIMEOUT_IN_SECS, SECONDS)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test isAvailable() with timeout as the adapter becomes available'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> waitTo('connect').before().returning()

    and: "a waiter that verifies the adapter should be waiting for its client to become available and trigger the client connection"
      def waitCalled = new AtomicInteger()
      def waiter = new SolrClientAdapter.Waiter() {
        public long timedWait(Object lock, long now, long time, TimeUnit unit) {
          waitCalled.incrementAndGet()
          pingController.notifyTo('connect')
          super.timedWait(lock, now, time, unit)
        }
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with, waiter)

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "checking availability with a timeout"
      def available = adapter.isAvailable(TIMEOUT_IN_SECS, SECONDS)

    then: "it should return eventually as available"
      available

    and: "the waiter should have been called at least once"
      waitCalled.get() == 1

    when: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test isAvailable() with a timeout is interrupted if the thread is interrupted'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> {
        doNotify('connecting')
            .then().waitTo('connect').before().returning()
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting to start connecting to the client"
      pingController.waitFor('connecting')

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "simulating the thread to be interrupted"
      Thread.currentThread().interrupt()

    and: "checking availability with a timeout"
      adapter.isAvailable(TIMEOUT_IN_SECS, SECONDS)

    then: "it should be interrupted right away"
      thrown(InterruptedException)

    when: "triggering the client connection to stabilize the test"
      pingController.notifyTo('connect')

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  @Unroll
  def 'test isAvailable() with a #with_a_timeout_of timeout as it times out'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> {
        doThrow(pingFail)
            .then().waitTo('connect').before().returning()
      }

    and: "a waiter that times out the wait faster by returning the time when it woke up as past the end"
      def waitCalled = new AtomicInteger()
      def waiter = new SolrClientAdapter.Waiter() {
        public long timedWait(Object lock, long now, long time, TimeUnit unit) {
          waitCalled.incrementAndGet()
          now + time + 1
        }
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with, waiter)

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "checking availability with a timeout"
      def available = adapter.isAvailable(timeout, unit)

    then: "make sure the waiter was called or not"
      waitCalled.get() == wait_count

    and: "it should timeout and return as not available"
      !available

    when: "triggering the client connection"
      pingController.notifyTo('connect')

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      with_a_timeout_of || timeout | unit         || wait_count
      'negative'        || -25     | NANOSECONDS  || 0
      '0 seconds'       || 0       | SECONDS      || 0
      '1 millisecond'   || 1       | MILLISECONDS || 1
      '5 seconds'       || 5       | SECONDS      || 1
  }

  def 'test isAvailable() with a listener as the adapter changes states'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> waitTo('connect').before().returning()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "registering an availability listener"
      def testThread = Thread.currentThread()
      def conds = [(true): new AsyncConditions(), (false): new AsyncConditions()]
      def listener = Mock(Listener) {
        2 * changed(_, _) >> { c, available ->
          conds[available].evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
        }
      }
      def available = adapter.isAvailable(listener)

    then: "the adapter should still be reported as unavailable"
      !available

    and: "the listener should have been registered"
      adapter.hasListeners()

    when: "waiting for the listener to eventually be called back from a different thread to indicate the adapter is not available"
      conds[false].await(TIMEOUT_IN_SECS)

    then: "it shouldn't timeout"
      noExceptionThrown()

    when: "triggering the client creation/connection"
      pingController.notifyTo('connect')

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    and: "waiting for the listener to eventually be called back from a different thread to indicate the adapter is available"
      conds[true].await(TIMEOUT_IN_SECS)

    then: "it shouldn't timeout"
      noExceptionThrown()

    and: "the adapter should be available"
      adapter.available

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test isAvailable() with a listener as the adapter is closed'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "registering an availability listener"
      def testThread = Thread.currentThread()
      def conds = [(true): new AsyncConditions(), (false): new AsyncConditions()]
      def listener = Mock(Listener) {
        2 * changed(_, _) >> { c, available ->
          conds[available].evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
        }
      }
      def available = adapter.isAvailable(listener)

    then: "the adapter should still be reported as available"
      available

    and: "the listener should have been registered"
      adapter.hasListeners()

    when: "waiting for the listener should be called back eventually from a different thread to indicate the adapter is available"
      conds[true].await(TIMEOUT_IN_SECS)

    then: "it shouldn't timeout"
      noExceptionThrown()

    when: "closing the adapter"
      adapter.close()

    then: "no more listeners are registered"
      !adapter.hasListeners()

    and: "the adapter shoud be unavailable"
      !adapter.available

    when: "waiting for the listener should be called back eventually from a different thread to indicate the adapter is not available"
      conds[false].await(TIMEOUT_IN_SECS)

    then: "it shouldn't timeout"
      noExceptionThrown()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test whenAvailable() as the adapter becomes available'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> waitTo('connect').before().returning()

    and:
      def testThread = Thread.currentThread()
      def conds = new AsyncConditions()
      def initializer = Mock(Initializer) {
        1 * initialized(_) >> {
          conds.evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
        }
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    then: "the adapter should be unavailable because it is being initialized"
      !adapter.available

    when: "registering an initializer"
      adapter.whenAvailable(initializer)

    then: "the initializer should have been registered"
      adapter.hasInitializers()

    when: "checking if the initializer was called"
      conds.await() // will wait for 1 second :-(

    then: "it shouldn't"
      thrown(SpockTimeoutError)

    when: "triggering the client connection"
      pingController.notifyTo('connect')

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the initializer should be called back eventually from a different thread to indicate the adapter is available"
      conds.await(TIMEOUT_IN_SECS)

    and: "no more initializers should be registered"
      !adapter.hasInitializers()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test whenAvailable() when the adapter is already available'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    and:
      def testThread = Thread.currentThread()
      def conds = new AsyncConditions()
      def initializer = Mock(Initializer) {
        1 * initialized(_) >> {
          conds.evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
        }
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.isAvailable()

    when: "registering an initializer"
      adapter.whenAvailable(initializer)

    then: "the initializer should not have been registered"
      !adapter.hasInitializers()

    and: "the initializer should be called back eventually from a different thread to indicate the adapter was available"
      conds.await(TIMEOUT_IN_SECS)

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test whenAvailable() when the adapter never becomes available and gets closed'() {
    given: "a create controller that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> {
        doNotify('creating')
            .then().waitToBeCancelled().before().throwing(new CancellationException("was cancelled"))
      }

    and: "a ping controller that will never be called"
      def pingController = new FailsafeController('SolrClient Ping')

    and:
      def initializer = Mock(Initializer) {
        0 * initialized(_)
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "registering an initializer"
      adapter.whenAvailable(initializer)

    then: "the initializer should have been registered"
      adapter.hasInitializers()

    when: "waiting for the adapter to start creating the client"
      createController.waitFor('creating')

    and: "closing the adapter"
      adapter.close()

    then: "no more initializers should be registered"
      !adapter.hasInitializers()

    when: "waiting for failsafe create to complete since it should be cancelled"
      createController.waitForCompletion()

    then: "verify it was cancelled"
      def e = thrown(ControlledExecutionException)

      e.error instanceof CancellationException

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test whenAvailable() will not register initializers if the adapter is already closed'() {
    given: "a client"
      def client = Mock(SolrClient)

    and: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    and:
      def initializer = Mock(Initializer) {
        0 * initialized(_)
      }

    when: "creating an adapter"
      def adapter = new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the adapter should be available"
      adapter.available

    when: "closing the adapter"
      adapter.close()

    then: "underlying client should be closed too"
      1 * client.close() >> null

    when: "waiting for the adapter to be initialized"
      adapter.whenAvailable(initializer)

    then: "the initializer should not have been registered"
      !adapter.hasInitializers()

    and: "verify failsafe controllers"
      createController.verify()
      pingController.verify()

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  @Unroll
  def 'test isAvailable() when #when and the last ping was not recent'() {
    given:
      def conds = new AsyncConditions(expected_background_ping)

    and:
      def testThread = Thread.currentThread()
      def client = Mock(SolrClient) {
        expected_background_ping * ping() >> {
          conds.evaluate {
            // should be called from a different thread
            assert !Thread.currentThread().is(testThread)
          }
          pingOkResponse
        }
      }

    and: "creating an adapter"
      SolrClientAdapter adapter = Spy(SolrClientAdapter, constructorArgs: [CORE, creator, failsafeCreator, failsafeCreator])

    when: "simulating a successful creation and/or connection by failsafe if requested"
      if (simulate_connecting) {
        adapter.logAndSetConnecting(client, Stub(ExecutionContext))
      }
      if (simulate_connected) {
        adapter.logAndSetConnected(null, Stub(ExecutionContext))
      }

    and: "close the adapter if requested"
      if (close) {
        adapter.close()
      }

    and: "checking if the adapter is available"
      def returnedAvailable = adapter.isAvailable()

    then: "the adapter should have checked to see if the last ping was not recent and responded that it wasn't"
      expected_background_ping * adapter.wasNotRecent(*_) >> true

    and:
      returnedAvailable == available

    and: "a background ping should have been issued (or not)"
      conds.await(TIMEOUT_IN_SECS)

    where:
      when         || simulate_connecting | simulate_connected | close || available | expected_background_ping
      'creating'   || false               | false              | false || false     | 0
      'connecting' || true                | false              | false || false     | 0
      'connected'  || true                | true               | false || true      | 1
      'closed'     || true                | true               | true  || false     | 0
  }

  @ClearInterruptions
  @Unroll
  def 'test client creation retry policy will #policy_will when #when_what'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the client creation retry policy is configured to allow retries"
      createController.retryPolicy.allowsRetries();

    and: "verify it will abort or not"
      createController.retryPolicy.canAbortFor(result, exception) == abort

    and: "verify it will retry or not (if not already aborting)"
      // failsafe never checks for retries if the policy indicates to abort
      abort || (createController.retryPolicy.canRetryFor(result, exception) == retry)

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()

    where:
      policy_will               | when_what                          || result          | exception                                       || abort | retry
      'retry and not abort'     | 'null is returned'                 || null            | null                                            || false | true
      'retry and not abort'     | 'SolrException is thrown'          || null            | new SolrException(ErrorCode.UNKNOWN, 'testing') || false | true
      'retry and not abort'     | 'SolrServerException is thrown'    || null            | new SolrServerException('testing')              || false | true
      'retry and not abort'     | 'IOException is thrown'            || null            | new IOException('testing')                      || false | true
      'retry and not abort'     | 'RuntimeException is thrown'       || null            | new RuntimeException('testing')                 || false | true
      'retry and not abort'     | 'Throwable is thrown'              || null            | new Throwable('testing')                        || false | true
      'retry and not abort'     | 'IOException is thrown'            || null            | new IOException('testing')                      || false | true
      'retry and not abort'     | 'not a client is returned'         || 'a fake client' | null                                            || false | true
      'not retry and not abort' | 'a client is returned'             || client          | null                                            || false | false
      'abort'                   | 'OutOfMemoryError is thrown'       || null            | new OutOfMemoryError('testing')                 || true  | false
      'abort'                   | 'InterruptedException is thrown'   || null            | new InterruptedException('testing')             || true  | false
      'abort'                   | 'InterruptedIOException is thrown' || null            | new InterruptedIOException('testing')           || true  | false
  }

  def 'test client creation retry policy will delay in between attempts'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "verify the client creation retry policy delay configuration"
      with(createController.retryPolicy) {
        delay.toMillis() == 10
        delayFactor == 2
        maxDelay.toMinutes() == 1
        !jitter
        !jitterFactor
      }

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test client creation retry policy will never stop unless aborted'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "verify the client creation retry policy will never stop unless aborted"
      with(createController.retryPolicy) {
        !maxDuration
        maxRetries == -1
      }

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  @ClearInterruptions
  @Unroll
  def 'test client connection retry policy will #policy_will when #when_what'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "the client connection retry policy is configured to allow retries"
      pingController.retryPolicy.allowsRetries();

    and: "verify it will abort or not"
      pingController.retryPolicy.canAbortFor(result, exception) == abort

    and: "verify it will retry or not (if not already aborting)"
      // failsafe never checks for retries if the policy indicates to abort
      abort || (pingController.retryPolicy.canRetryFor(result, exception) == retry)

    cleanup:
      pingController?.shutdown()
      pingController?.shutdown()

    where:
      policy_will               | when_what                            || result     | exception                                       || abort | retry
      'retry and not abort'     | 'SolrException is thrown'            || null       | new SolrException(ErrorCode.UNKNOWN, 'testing') || false | true
      'retry and not abort'     | 'SolrServerException is thrown'      || null       | new SolrServerException('testing')              || false | true
      'retry and not abort'     | 'UnavailableSolrException is thrown' || null       | new UnavailableSolrException('testing')         || false | true
      'retry and not abort'     | 'IOException is thrown'              || null       | new IOException('testing')                      || false | true
      'retry and not abort'     | 'RuntimeException is thrown'         || null       | new RuntimeException('testing')                 || false | true
      'retry and not abort'     | 'Throwable is thrown'                || null       | new Throwable('testing')                        || false | true
      'retry and not abort'     | 'IOException is thrown'              || null       | new IOException('testing')                      || false | true
      'not retry and not abort' | 'it returns normally'                || null       | null                                            || false | false
      'not retry and not abort' | 'anything is returned'               || 'anything' | null                                            || false | false
      'abort'                   | 'OutOfMemoryError is thrown'         || null       | new OutOfMemoryError('testing')                 || true  | false
      'abort'                   | 'InterruptedException is thrown'     || null       | new InterruptedException('testing')             || true  | false
      'abort'                   | 'InterruptedIOException is thrown'   || null       | new InterruptedIOException('testing')           || true  | false
  }

  def 'test client connection retry policy will delay in between attempts'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "verify the client connection retry policy delay configuration"
      with(pingController.retryPolicy) {
        delay.toSeconds() == 1
        delayFactor == 2
        maxDelay.toMinutes() == 2
        !jitter
        !jitterFactor
      }

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }

  def 'test client connection retry policy will never stop unless aborted'() {
    given: "controllers that mocks every execution/attempts"
      def createController = new FailsafeController('SolrClient Creation') >> doReturn(client)
      def pingController = new FailsafeController('SolrClient Ping') >> doReturn()

    when: "creating an adapter that initializes failsafe"
      new SolrClientAdapter(CORE, creator, createController.&with, pingController.&with)

    and: "waiting for failsafe to complete"
      createController.waitForSuccessfulCompletion()
      pingController.waitForSuccessfulCompletion()

    then: "verify the client connection retry policy will never stop unless aborted"
      with(pingController.retryPolicy) {
        !maxDuration
        maxRetries == -1
      }

    cleanup:
      createController?.shutdown()
      pingController?.shutdown()
  }
}
