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
package org.codice.ddf.test.mockito;

import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicReference;
import org.mockito.AdditionalAnswers;
import org.mockito.Incubating;
import org.mockito.stubbing.Stubber;

/**
 * Class used to capture and save the stack trace right before a call to a mock object is being
 * made.
 *
 * <p>Usage example: <br>
 *
 * <pre>
 *   StackCaptor stackCaptor = new StackCaptor();
 *
 *   stackCaptor
 *     .doCaptureStack()
 *     .when(mockObject)
 *     .method(eq(1), eq("2"));
 *
 *   // Test code
 *
 *   assertThat(stackCaptor.getStack(), someMatcher());
 * </pre>
 *
 * <p><b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
@Incubating
public class StackCaptor {
  AtomicReference<StackTraceElement[]> stackTraceElements = new AtomicReference<>();

  /**
   * Use {@link #doCaptureStack()} when you want to capture the stack of a void method.
   *
   * @return same as what Mockito's {@code Mockito.doAnswer()} would return
   */
  public Stubber doCaptureStack() {
    return doAnswer(
        AdditionalAnswers.answerVoid(
            o -> stackTraceElements.set(Thread.currentThread().getStackTrace())));
  }

  /**
   * Use {@link #doCaptureStackAndReturn(Object)} when you want to capture the stack of a method
   * that has a return value.
   *
   * @return same as what Mockito's {@code Mockito.doAnswer()} would return
   */
  public Stubber doCaptureStackAndReturn(Object value) {
    return doAnswer(
        AdditionalAnswers.answer(
            o -> {
              stackTraceElements.set(Thread.currentThread().getStackTrace());
              return value;
            }));
  }

  /**
   * Gets the stack information that was captured when the mocked method was called.
   *
   * @return stack elements
   */
  public StackTraceElement[] getStack() {
    return stackTraceElements.get();
  }
}
