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
package ddf.catalog.util.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PollerRunnerTest {

  @Mock private ScheduledExecutorService mockScheduledExecutorService;

  @Mock private Poller mockPoller;

  private PollerRunner pollerRunner;

  @After
  public void tearDown() {
    // TODO verify that nothing else is scheduled
    //    pollerRunner.destroy();
  }

  @Test
  public void testPollingStarted() throws Exception {
    // given:
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mock(ScheduledFuture.class);
            });

    final long pollIntervalMinutes = 1;

    final ImmutableMap itemsToPoll = mock(ImmutableMap.class);

    pollerRunner =
        new PollerRunner(mockPoller, pollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };

    // when:
    pollerRunner.init();

    // then:
    verify(mockScheduledExecutorService)
        .scheduleAtFixedRate(
            any(Runnable.class), eq(0L), eq(pollIntervalMinutes), eq(TimeUnit.MINUTES));
    verify(mockPoller).pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);
  }

  @Test
  public void testSetPollIntervalMinutes() throws Exception {
    // given:
    final ScheduledFuture mockFirstScheduledPollingFuture = mock(ScheduledFuture.class);

    // TODO verify called once
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mockFirstScheduledPollingFuture;
            });

    final long initialPollIntervalMinutes = 1;

    final ImmutableMap itemsToPoll = mock(ImmutableMap.class);

    pollerRunner =
        new PollerRunner(mockPoller, initialPollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };
    pollerRunner.init();

    final long newPollIntervalMinutes = initialPollIntervalMinutes + 1;

    // when:
    pollerRunner.setPollIntervalMinutes(newPollIntervalMinutes);

    // then
    final InOrder inOrder =
        Mockito.inOrder(mockFirstScheduledPollingFuture, mockScheduledExecutorService);
    inOrder.verify(mockFirstScheduledPollingFuture).cancel(true);
    inOrder
        .verify(mockScheduledExecutorService)
        .scheduleAtFixedRate(
            any(Runnable.class), eq(0L), eq(newPollIntervalMinutes), eq(TimeUnit.MINUTES));
    verify(mockPoller).pollItems(newPollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);
  }

  @Rule public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testPollVirtualMachineError() throws Exception {
    // then:
    final VirtualMachineError virtualMachineError = mock(VirtualMachineError.class);
    exception.expect(is(virtualMachineError));

    // given:
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mock(ScheduledFuture.class);
            });

    final long pollIntervalMinutes = 1;

    final ImmutableMap itemsToPoll = mock(ImmutableMap.class);

    doThrow(virtualMachineError)
        .when(mockPoller)
        .pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);

    pollerRunner =
        new PollerRunner(mockPoller, pollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };

    // when:
    pollerRunner.init();
  }

  @Test
  public void testInterruptedWhenPolling() throws Exception {
    // given:
    final ScheduledFuture mockScheduledFuture = mock(ScheduledFuture.class);
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mockScheduledFuture;
            });

    final long pollIntervalMinutes = 1;
    final ImmutableMap itemsToPoll = mock(ImmutableMap.class);

    doThrow(InterruptedException.class)
        .when(mockPoller)
        .pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);

    pollerRunner =
        new PollerRunner(mockPoller, pollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };

    // when:
    pollerRunner.init();

    // then:
    verify(mockScheduledExecutorService, atLeastOnce()).shutdown();
    // noExceptionThrown()
    assertThat(Thread.interrupted(), is(true));
  }

  @Test
  public void testPollerException() throws Exception {
    // given:
    final ScheduledFuture mockScheduledFuture = mock(ScheduledFuture.class);
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mockScheduledFuture;
            });

    final long pollIntervalMinutes = 1;

    final String key1 = "key1";
    final String key2 = "key2";
    final String key3 = "key3";
    final String key4 = "key4";
    final String key5 = "key5";
    final String key6 = "key6";
    final String key7 = "key7";
    final String key8 = "key8";
    final String key9 = "key9";
    final String key10 = "key10";

    final ImmutableMap itemsToPoll =
        ImmutableMap.builder()
            .put(key1, mock(Callable.class))
            .put(key2, mock(Callable.class))
            .put(key3, mock(Callable.class))
            .put(key4, mock(Callable.class))
            .put(key5, mock(Callable.class))
            .put(key6, mock(Callable.class))
            .put(key7, mock(Callable.class))
            .put(key8, mock(Callable.class))
            .put(key9, mock(Callable.class))
            .put(key10, mock(Callable.class))
            .build();

    // These are all of the kinds of Exceptions that could be in the PollerException.
    final Map<String, Throwable> causes = new HashMap<>();
    causes.put(key2, new RejectedExecutionException());
    causes.put(key4, new IllegalArgumentException());
    causes.put(key5, new CancellationException());
    causes.put(key6, new RuntimeException());
    causes.put(key7, new InterruptedException());
    causes.put(key8, new Throwable());
    final PollerException pollerException = new PollerException(causes);

    doThrow(pollerException)
        .when(mockPoller)
        .pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);

    pollerRunner =
        new PollerRunner(mockPoller, pollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };

    // when:
    pollerRunner.init();

    // then:
    verifyZeroInteractions(mockScheduledFuture);
    // noExceptionThrown()
  }

  @Test
  public void testThrowableWhenPolling() throws Exception {
    // given:
    final ScheduledFuture mockScheduledFuture = mock(ScheduledFuture.class);
    when(mockScheduledExecutorService.scheduleAtFixedRate(
            any(Runnable.class), eq(0L), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              invocation.getArgumentAt(0, Runnable.class).run();
              return mockScheduledFuture;
            });

    final long pollIntervalMinutes = 1;
    final ImmutableMap itemsToPoll = mock(ImmutableMap.class);

    doThrow(Throwable.class)
        .when(mockPoller)
        .pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);

    pollerRunner =
        new PollerRunner(mockPoller, pollIntervalMinutes, mockScheduledExecutorService) {
          @Override
          protected ImmutableMap getValueLoaders() {
            return itemsToPoll;
          }
        };

    // when:
    pollerRunner.init();

    // then:
    verifyZeroInteractions(mockScheduledFuture);
    // noExceptionThrown()
  }
}
