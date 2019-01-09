/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl

import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.Futures
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue
import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty
import static org.hamcrest.CoreMatchers.instanceOf
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.collection.IsMapContaining.hasEntry
import static spock.util.matcher.HamcrestSupport.expect

@Timeout(value = 3, unit = TimeUnit.SECONDS)
class PollerSpec extends Specification {

    def 'test destroy'() {
        given:
        final ExecutorService mockPollThreadPool = Mock()
        final ExecutorService mockPollTimeoutWatcherThreadPool = Mock()

        final Poller poller = new Poller(mockPollThreadPool, mockPollTimeoutWatcherThreadPool)

        when:
        poller.destroy()

        then: 'both thread pools are shutdown'
        (1.._) * mockPollThreadPool.shutdown()
        (1.._) * mockPollTimeoutWatcherThreadPool.shutdown()
    }

    def 'test get a cached value for an unknown key'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        expect:
        ((Optional) poller.getCachedValue(_)) isEmpty()

        cleanup:
        poller.destroy()
    }

    def 'test get value'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        final key = _
        final Object value = _

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        then:
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test value changes'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        final key = _

        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        and: 'an initial value is loaded'
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): { Mock(Object) }]))

        final Object secondValue = _

        when: 'a new value is loaded'
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): { secondValue }]))

        then:
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(secondValue)

        cleanup:
        poller.destroy()
    }

    def 'test key is removed'() {
        given:
        final key = _

        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { _ }]))

        when: 'itemsToPoll no longer contains that key'
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.of())

        then:
        ((Optional) poller.getCachedValue(key)) isEmpty()

        cleanup:
        poller.destroy()
    }

    def 'test poll with no itemsToPoll'() {
        given:
        final ExecutorService mockPollThreadPool = Mock()
        final ExecutorService mockPollTimeoutWatcherThreadPool = Mock()

        final Poller poller = new Poller(mockPollThreadPool, mockPollTimeoutWatcherThreadPool)

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.of())

        then: 'nothing was submitted to either thread pool'
        0 * mockPollThreadPool.execute(_)
        0 * mockPollThreadPool.submit(*_)
        0 * mockPollThreadPool./invoke.*/(*_)
        0 * mockPollTimeoutWatcherThreadPool.submit(*_)
        0 * mockPollTimeoutWatcherThreadPool.execute(_)
        0 * mockPollTimeoutWatcherThreadPool./invoke.*/(*_)

        cleanup:
        poller.destroy()
    }

    def 'test loader does not complete within pollItems timeout'() {
        given:
        final key = _
        final Callable loader = _ as Callable

        final Closure mockHandleTimeout = Mock()

        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        final Future mockFuture = Mock() {
            get(timeout, timeoutTimeUnit) >> { throw new TimeoutException() }
        }

        // not sure why a Spy doesn't work here
        final Poller poller = new Poller(Mock(ExecutorService) {
            submit(loader) >> mockFuture
        }, Executors.newCachedThreadPool()) {
            void handleTimeout(Object k) {
                assert k == key
                mockHandleTimeout.call(k)
            }
        }

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): loader]))

        then: 'the loading task is cancelled'
        1 * mockFuture.cancel(true)

        and:
        1 * mockHandleTimeout.call(key)

        cleanup:
        poller.destroy()
    }

    def 'test loader throws a RuntimeException'() {
        given:
        final key = _
        final RuntimeException runtimeException = new RuntimeException()

        final Poller poller = Spy(Poller, constructorArgs: [Executors.newCachedThreadPool(), Executors.newCachedThreadPool()])

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): {
            throw runtimeException
        }]))

        then:
        1 * poller.handleException(key, runtimeException)

        cleanup:
        poller.destroy()
    }

    // invalid params tests

    def 'test invalid constructor parameters'() {
        when:
        new Poller(pollThreadPool, pollTimeoutWatcherThreadPool)

        then:
        thrown expectedException

        where:
        pollThreadPool        | pollTimeoutWatcherThreadPool || expectedException
        null                  | Mock(ExecutorService)        || NullPointerException
        Mock(ExecutorService) {
            isShutdown() >> true
        }                     | Mock(ExecutorService)        || IllegalArgumentException
        Mock(ExecutorService) | null                         || NullPointerException
        Mock(ExecutorService) | Mock(ExecutorService) {
            isShutdown() >> true
        }                                                    || IllegalArgumentException
    }

    def 'test get value for null key'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        when:
        poller.getCachedValue(null)

        then:
        thrown NullPointerException

        cleanup:
        poller.destroy()
    }

    def 'test null pollItems parameters'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        when:
        poller.pollItems(timeout, timeoutTimeUnit, itemsToPoll as ImmutableMap)

        then:
        thrown expectedException

        cleanup:
        poller.destroy()

        where:
        timeout | timeoutTimeUnit  | itemsToPoll        || expectedException
        1       | null             | Mock(ImmutableMap) || NullPointerException
        1       | TimeUnit.MINUTES | null               || NullPointerException
    }

    def 'test invalid pollItems timeout'(final TimeUnit timeoutTimeUnit) {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        final long minimumTimeoutNs = TimeUnit.NANOSECONDS.convert(Poller.MINIMUM_TIMEOUT_MS, TimeUnit.MILLISECONDS)

        when:
        poller.pollItems(timeoutTimeUnit.convert(minimumTimeoutNs - 1, TimeUnit.NANOSECONDS), timeoutTimeUnit, Mock(ImmutableMap))

        then:
        thrown IllegalArgumentException

        cleanup:
        poller.destroy()

        where:
        timeoutTimeUnit << TimeUnit.values()
    }

    def 'test thread pool is already shutdown when poll'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        and: 'the thread polls are shutdown after instantiating the poller'
        poller.destroy()

        when:
        poller.pollItems(1, TimeUnit.MINUTES, Mock(ImmutableMap))

        then:
        thrown IllegalStateException
    }

    // unable to commit tests

    def 'test null loaded value'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        final key = _
        final value = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { null }]))

        then:
        final PollerException pollerException = thrown()
        final Map<?, Throwable> causes = pollerException.getCauses()
        causes.size() == 1
        expect(causes, hasEntry(is(key), is(instanceOf(IllegalArgumentException))))

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test unable to start a loader'() {
        given:
        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        final value = _
        final Callable initialLoader = { value }
        final Callable loader = _ as Callable

        final RejectedExecutionException rejectedExecutionException = new RejectedExecutionException()

        final Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { Callable callable ->
                Mock(Future) {
                    get(timeout, timeoutTimeUnit) >> { callable.call() }
                }
            }
            submit(loader) >> {
                throw rejectedExecutionException
            }
        }, Executors.newCachedThreadPool())

        final key = _

        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): loader]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): rejectedExecutionException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test pollTimeoutWatcherThreadPool is unable to schedule a task for execution'() {
        given:
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Mock(ExecutorService) {
            2 * execute(_ as Runnable) >> { Runnable runnable -> runnable.run() } >> {
                throw exception
            }
        })

        final key = _
        final value = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): _ as Callable]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): exception]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()

        where:
        exception << [new RejectedExecutionException(), new RuntimeException()]
    }

    def 'test interrupted while waiting for a loader to complete'() {
        given:
        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        final value = _
        final Callable initialLoader = { value }

        final Callable newLoader = _ as Callable
        final InterruptedException interruptedException = new InterruptedException()

        final Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { final Callable loader -> Futures.immediateFuture(loader.call()) }
            submit(newLoader) >> Mock(Future) {
                get(timeout, timeoutTimeUnit) >> {
                    throw interruptedException
                }
            }
        }, Executors.newCachedThreadPool())

        final key = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): newLoader]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): interruptedException]

        and:
        Thread.interrupted()

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test the loader was cancelled'() {
        given:
        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        final value = _
        final Callable initialLoader = { value }

        final Callable newLoader = _ as Callable
        final CancellationException cancellationException = new CancellationException()

        final Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { final Callable loader -> Futures.immediateFuture(loader.call()) }
            submit(newLoader) >> Mock(Future) {
                get(timeout, timeoutTimeUnit) >> {
                    throw cancellationException
                }
            }
        }, Executors.newCachedThreadPool())

        final key = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): newLoader]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): cancellationException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test handleTimeout throws a RuntimeException'() {
        given:
        final timeout = 1
        final TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        final key = _
        final value = _
        final Callable initialLoader = { value }

        final Callable newLoader = _ as Callable
        final RuntimeException runtimeException = new RuntimeException()

        // not sure why a Spy doesn't work here
        final Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { final Callable loader -> Futures.immediateFuture(loader.call()) }
            submit(newLoader) >> Mock(Future) {
                get(timeout, timeoutTimeUnit) >> { throw new TimeoutException() }
            }
        }, Executors.newCachedThreadPool()) {
            void handleTimeout(Object k) {
                assert k == key
                throw runtimeException
            }
        }

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): newLoader]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): runtimeException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test handleException throws a RuntimeException'() {
        given:
        final key = _

        final RuntimeException runtimeExceptionThrownByHandleException = new RuntimeException()

        // not sure why a Spy doesn't work here
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()) {
            void handleException(Object k, RuntimeException e) {
                assert k == key
                throw runtimeExceptionThrownByHandleException
            }
        }

        final value = _
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): {
            throw new RuntimeException()
        }]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): runtimeExceptionThrownByHandleException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test the loader throws an Exception other than RuntimeException'() {
        given:
        final key = _
        final Throwable throwable = new Throwable()

        // not sure why a Spy doesn't work here
        final Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        final value = _
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { throw throwable }]))

        then:
        final PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): throwable]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    // TODO
//    def 'test try to commit all values even if there are some failures'() {
//
//    }

//    def 'new values are committed when and in the order that they are loaded'() {
//
//    }

//    def 'test pollItems while another pollItems is in progress'() {
//
//    }
}