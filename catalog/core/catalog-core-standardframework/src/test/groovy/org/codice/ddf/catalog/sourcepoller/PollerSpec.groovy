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
package org.codice.ddf.catalog.sourcepoller

import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.Futures
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification

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

@RunWith(JUnitPlatform.class)
class PollerSpec extends Specification {

    def 'test destroy'() {
        given:
        ExecutorService mockPollThreadPool = Mock()
        ExecutorService mockPollTimeoutWatcherThreadPool = Mock()

        Poller poller = new Poller(mockPollThreadPool, mockPollTimeoutWatcherThreadPool)

        when:
        poller.destroy()

        then: 'both thread pools are shutdown'
        (1.._) * mockPollThreadPool.shutdown()
        (1.._) * mockPollTimeoutWatcherThreadPool.shutdown()
    }

    def 'test get a cached value for an unknown key'() {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        expect:
        ((Optional) poller.getCachedValue(_)) isEmpty()

        cleanup:
        poller.destroy()
    }

    def 'test get value'() {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        def key = _
        Object value = _

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        then:
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test value changes'() {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        def key = _

        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        and: 'an initial value is loaded'
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): { Mock(Object) }]))

        Object secondValue = _

        when: 'a new value is loaded'
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): { secondValue }]))

        then:
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(secondValue)

        cleanup:
        poller.destroy()
    }

    def 'test key is removed'() {
        given:
        def key = _

        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

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
        ExecutorService mockPollThreadPool = Mock()
        ExecutorService mockPollTimeoutWatcherThreadPool = Mock()

        Poller poller = new Poller(mockPollThreadPool, mockPollTimeoutWatcherThreadPool)

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
        def key = _
        Callable loader = _ as Callable

        Closure mockHandleTimeout = Mock()

        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        Future mockFuture = Mock() {
            get(timeout, timeoutTimeUnit) >> { throw new TimeoutException() }
        }

        // not sure why a Spy doesn't work here
        Poller poller = new Poller(Mock(ExecutorService) {
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
        def key = _
        RuntimeException runtimeException = new RuntimeException()

        Poller poller = Spy(Poller, constructorArgs: [Executors.newCachedThreadPool(), Executors.newCachedThreadPool()])

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
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        when:
        poller.getCachedValue(null)

        then:
        thrown NullPointerException

        cleanup:
        poller.destroy()
    }

    def 'test null pollItems parameters'() {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

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

    def 'test invalid pollItems timeout'(TimeUnit timeoutTimeUnit) {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        long minimumTimeoutNs = TimeUnit.NANOSECONDS.convert(Poller.MINIMUM_TIMEOUT_MS, TimeUnit.MILLISECONDS)

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
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

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
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        def key = _
        def value = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { null }]))

        then:
        PollerException pollerException = thrown()
        Map<?, Throwable> causes = pollerException.getCauses()
        causes.size() == 1
        expect(causes, hasEntry(is(key), is(instanceOf(IllegalArgumentException))))

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test unable to start a loader'() {
        given:
        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        def value = _
        Callable initialLoader = { value }
        Callable loader = _ as Callable

        RejectedExecutionException rejectedExecutionException = new RejectedExecutionException()

        Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { Callable callable ->
                Mock(Future) {
                    get(timeout, timeoutTimeUnit) >> { callable.call() }
                }
            }
            submit(loader) >> {
                throw rejectedExecutionException
            }
        }, Executors.newCachedThreadPool())

        def key = _

        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): loader]))

        then:
        PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): rejectedExecutionException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test pollTimeoutWatcherThreadPool is unable to schedule a task for execution'() {
        given:
        Poller poller = new Poller(Executors.newCachedThreadPool(), Mock(ExecutorService) {
            2 * execute(_ as Runnable) >> { Runnable runnable -> runnable.run() } >> {
                throw exception
            }
        })

        def key = _
        def value = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): _ as Callable]))

        then:
        PollerException pollerException = thrown()
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
        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        def value = _
        Callable initialLoader = { value }

        Callable newLoader = _ as Callable
        InterruptedException interruptedException = new InterruptedException()

        Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { def Callable loader -> Futures.immediateFuture(loader.call()) }
            submit(newLoader) >> Mock(Future) {
                get(timeout, timeoutTimeUnit) >> {
                    throw interruptedException
                }
            }
        }, Executors.newCachedThreadPool())

        def key = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): newLoader]))

        then:
        PollerException pollerException = thrown()
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
        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        def value = _
        Callable initialLoader = { value }

        Callable newLoader = _ as Callable
        CancellationException cancellationException = new CancellationException()

        Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { def Callable loader -> Futures.immediateFuture(loader.call()) }
            submit(newLoader) >> Mock(Future) {
                get(timeout, timeoutTimeUnit) >> {
                    throw cancellationException
                }
            }
        }, Executors.newCachedThreadPool())

        def key = _

        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): initialLoader]))

        when:
        poller.pollItems(timeout, timeoutTimeUnit, ImmutableMap.copyOf([(key): newLoader]))

        then:
        PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): cancellationException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test handleTimeout throws a RuntimeException'() {
        given:
        def timeout = 1
        TimeUnit timeoutTimeUnit = TimeUnit.MINUTES

        def key = _
        def value = _
        Callable initialLoader = { value }

        Callable newLoader = _ as Callable
        RuntimeException runtimeException = new RuntimeException()

        // not sure why a Spy doesn't work here
        Poller poller = new Poller(Mock(ExecutorService) {
            submit(initialLoader) >> { def Callable loader -> Futures.immediateFuture(loader.call()) }
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
        PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): runtimeException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test handleException throws a RuntimeException'() {
        given:
        def key = _

        RuntimeException runtimeExceptionThrownByHandleException = new RuntimeException()

        // not sure why a Spy doesn't work here
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()) {
            void handleException(Object k, RuntimeException e) {
                assert k == key
                throw runtimeExceptionThrownByHandleException
            }
        }

        def value = _
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): {
            throw new RuntimeException()
        }]))

        then:
        PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): runtimeExceptionThrownByHandleException]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }

    def 'test the loader throws an Exception other than RuntimeException'() {
        given:
        def key = _
        Throwable testThrowable = new Throwable()

        // not sure why a Spy doesn't work here
        Poller poller = new Poller(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

        def value = _
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { value }]))

        when:
        poller.pollItems(1, TimeUnit.MINUTES, ImmutableMap.copyOf([(key): { throw testThrowable }]))

        then:
        PollerException pollerException = thrown()
        pollerException.getCauses() == [(key): testThrowable]

        and: 'any old value was not overridden or removed'
        ((Optional<Object>) poller.getCachedValue(key)) hasValue(value)

        cleanup:
        poller.destroy()
    }
}