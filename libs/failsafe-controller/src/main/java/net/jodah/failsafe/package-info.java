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
 *
 * <p>The {@link net.jodah.failsafe.FailsafeController} extends on Failsafe to allow deterministic
 * testing of asynchronous code controlled by Failsafe. It is designed to compress the time aspect
 * while allowing full control of the Failsafe behavior while testing one's code.
 *
 * <p>Failsafe is typically used in code to repeatedly execute a given task until it succeeds or
 * aborts. For example:
 *
 * <pre>
 *   +-----------------+ Failsafe execution +----------+
 *   | Code under test |------------------->|          |
 *   |                 |                    |          |
 *   |  +------+       |    1st attempt     |          |
 *   |  |      |<---------------------------| Failsafe |
 *   |  | Task |       |       ...          |          |
 *   |  |      |<<--------------------------|          |
 *   |  |      |       |   final attempt    |          |
 *   |  |      |<---------------------------|          |
 *   |  +------+       |                    +----------+
 *   +-----------------+
 * </pre>
 *
 * <p>In this example, we see one execution from the code under test that results in multiple
 * attempts to execute a specific task. The {@link net.jodah.failsafe.RetryPolicy} configured with
 * Failsafe will typically dictate when a result from the task is considered successful, when it
 * should abort the whole execution, or when another attempt should be made. It will also typically
 * dictate how long to wait between each attempts.
 *
 * <p>Testing such code should be centered around controlling the exact sequence of events that
 * surrounds the task such that one can simulate race conditions that otherwise would be very hard
 * to do in real life. This would allow one to test <i>what if A happens before B</i> and <i>what if
 * A happens after B</i>. The task itself could be easily unit tested without regards to the
 * asynchronous nature of this design which would then allow the tester to mock, control, or script
 * its execution using the Failsafe controller and thus more easily control the sequence of events.
 *
 * <p>The Failsafe controller provides a very simple synchronization mechanism between the test code
 * and what happens in Failsafe using a notion of conditions or latches that can be notified and
 * waited upon from either the mocked task side or from the test code side based on the testing
 * need. For example, one could have Failsafe respond in error to an attempt until such a condition
 * is notified by the test code when it is time to have the asynchronous code return successfully.
 * Or again the asynchronous task could notify the test code it has completed a certain stage thus
 * unblocking the test code from continuing with additional verification.
 *
 * <p>It is also possible to have the asynchronous task either block or wait for the whole operation
 * to be canceled and for the test code to also wait for a given execution to complete successfully
 * or not before proceeding.
 *
 * <p><b>Retry Policies</b>
 *
 * <p>The Failsafe controller will uses the provided retry policy as is except for the following
 * exceptions:
 *
 * <ul>
 *   <li>Time is compress such that it doesn't wait before handling the next attempt (unless
 *       specifically mocked/scripted using <i>wait</i> actions)
 *   <li>It will automatically abort whenever an {@link java.lang.AssertionError} is detected such
 *       that the test would abort in failure.
 * </ul>
 *
 * <p><b>Scheduled Executor</b>
 *
 * <p>The Failsafe controller will uses the provided scheduled executor as is. It does intercept all
 * request for execution to this executor in order to determine when a particular Failsafe execution
 * has completed including dispatching all asynchronous events that are done using this executor.
 *
 * <p><b>Injection</b>
 *
 * <p>The production code has to be modified such that it becomes possible to inject the Failsafe
 * controller in lieu of Failsafe. To better understand how this is done, let's look at how one
 * normally invokes Failsafe with an example:
 *
 * <p><code><pre>
 *     Future&lt;MyObject&gt; future = Failsafe.with(myRetryPolicy)
 *       .with(myScheduledExecutor)
 *       .onRetry(this::logFailure)
 *       .onAbort(this::logInterruptionAndRecreate)
 *       .onFailure(this::logAndRecreateIfNotCancelled)
 *       .onSuccess(this::logAndSetCreated)
 *       .get(this::create);
 * </pre></code>
 *
 * <p>In this example, the first line will create a {@link net.jodah.failsafe.SyncFailsafe} object.
 * The second line will create a {@link net.jodah.failsafe.AsyncFailsafe} object from it and the
 * next 4 lines registers event listeners. Additional lines could be added to further configure
 * Failsafe if required. The final line is the one that actually triggers the start of an execution
 * which returns a {@link java.util.concurrent.Future} allowing access to the result at the end and
 * also provides the ability to cancel the execution.
 *
 * <p>The Failsafe controller is not a replacement for Failsafe but does require replacing the first
 * line above. One way to achieve this is by defining a variable in your class that can be injected
 * through the constructor at build time. This variable could be defined as a function like this:
 *
 * <p><code>
 * private final Function&lt;RetryPolicy, SyncFailsafe&lt;MyObject&gt;&gt; createFailsafeCreator;
 * </code>
 *
 * <p>With a package private constructor like this:
 *
 * <p><code><pre>
 *     MyClass(Function&lt;RetryPolicy, SyncFailsafe&lt;MyObject&gt;&gt; createFailsafeCreator) {
 *       this.createFailsafeCreator = createFailsafeCreator;
 *     }</pre></code>
 *
 * <p>The normal constructor could simply call that constructor like this:
 *
 * <p><code>this(Failsafe::with)</code>
 *
 * <p>From this point the example above would be re-written as:
 *
 * <p><code><pre>
 *     Future&lt;MyObject&gt; future = createFailsafeCreator.apply(myRetryPolicy)
 *       .with(myScheduledExecutor)
 *       .onRetry(this::logFailure)
 *       .onAbort(this::logInterruptionAndRecreate)
 *       .onFailure(this::logAndRecreateIfNotCancelled)
 *       .onSuccess(this::logAndSetCreated)
 *       .get(this::create);
 * </pre></code>
 *
 * <p>While testing, the constructor would be called as follow:
 *
 * <p><code><pre>
 *     FailsafeController controller = new FailsafeController("some name");
 *
 *     MyClass mine = new MyClass(controller::with);
 * </pre></code>
 *
 * <p>From this point on, the <code>controller</code> instance becomes your point of contact to
 * control, mock, or script how Failsafe will respond to each executions and attempts.
 *
 * <p><b>Actions</b>
 *
 * <p>Each time Failsafe makes an attempt, the controller intercepts the call to the task specified
 * (see 7th line in the above example) and figures out what to do. This could be:
 *
 * <ul>
 *   <li>return something
 *   <li>throw something
 *   <li>do nothing
 *   <li>proceed to invoke the real task
 *   <li>simulate an interruption
 *   <li>notify a condition
 *   <li>wait for a condition
 *   <li>wait for the execution to be cancelled by the code under test via the returned future
 * </ul>
 *
 * <p>Some of these actions can be further customized to be conditional, to repeat for a certain
 * number of attempts, to repeat for multiple attempts until a certain condition occurs or until the
 * execution is cancelled, or again can be delayed. Some actions will terminate an attempt like
 * those that returns or throw something and others will just be waiting for something before moving
 * on to the next action before responding to an attempt.
 *
 * <p>
 *
 * <table>
 *   <tr>
 *     <th>Action</th>
 *     <th>Description</th>
 *     <th>Cardinality (invoked for how many attempts)</th>
 *     <th>Terminates the attempt</th>
 *     <th>Short-circuiting (doesn't call the production tasks)</th>
 *   </tr>
 *   <tr>
 *     <td>Proceed</td>
 *     <td>Proceeds to invoke the real tasks that was specified by the production code when the
 *         execution was triggered and returns the result returned by the task or again throws back
 *         any exception that is thrown out of the task</td>
 *     <td>1</td>
 *     <td>Yes</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>Nothing</td>
 *     <td>Simulates a task that returns nothing (useful when invoking void tasks (e.g.
 *         {@link java.lang.Runnable} as these do not return anything</td>
 *     <td>1</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Return</td>
 *     <td>Simulates a task that returns a specific value.</td>
 *     <td>0, 1 or many
 *         <p>depending on how many different values the action was configured with (one per attempt)</td>
 *     <td>Yes if there are any configured values left to be returned
 *         <p>No if no values were configured</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Throw</td>
 *     <td>Simulates a task that throws a specific exception. Exceptions can be configured with actual
 *         objects or with class names. In the later case, an exception is instantiated using:
 *         <ol>
 *           <li>a constructor that accepts a string message; or
 *           <li>a constructor that accepts a string message and a cause exception; or
 *           <li>a constructor that accepts a cause exception; or
 *           <li>a default constructor
 *         </ol>
 *         <p>No matter how the exception is created (passed in or instantiated from a class name),
 *         its stack trace is filled in at the time it is re-thrown</td>
 *     <td>0, 1 or many
 *         <p>depending on how many different exceptions the action was configured with (one per
 *         attempt)</td>
 *     <td>Yes if there are any configured exceptions left to be thrown
 *         <p>No if no exceptions were configured</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Throw or Return</td>
 *     <td>Simulates a task that returns a specific value or throws a specific exception. Exceptions
 *         can be configured with actual objects or with class names. In the later case, an exception
 *         is instantiated using:
 *         <ol>
 *           <li>a constructor that accepts a string message; or
 *           <li>a constructor that accepts a string message and a cause exception; or
 *           <li>a constructor that accepts a cause exception; or
 *           <li>a default constructor
 *         </ol>
 *         <p>No matter how the exception is created (passed in or instantiated from a class name),
 *         its stack trace is filled in at the time it is re-thrown.
 *         <p>Values and exceptions can be intermix. Values that are instance of
 *         {@link java.lang.Throwable} and classes that extends {@link java.lang.Throwable} will be
 *         instantiated and thrown out</td>
 *     <td>0, 1 or many
 *         <p>depending on how many different values/exceptions the action was configured with (one
 *         per attempt)</td>
 *     <td>Yes if there are any configured values/exceptions left to be returned or thrown
 *         <p>No if no values/exceptions were configured</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Interrupt</td>
 *     <td>Simulate a thread interruption by both raising the interrupted flag of the current thread
 *         and throwing back an {@link java.lang.InterruptedException}</td>
 *     <td>1</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Notify</td>
 *     <td>Notifies or raises a particular condition in order to wake up or trigger code that might
 *         be waiting for this condition</td>
 *     <td>1</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Wait</td>
 *     <td>Blocks the attempt and waits for a particular condition to be notified or raised if not already
 *         notified.</td>
 *     <td>1</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>Wait to be Cancelled</td>
 *     <td>Blocks the attempt and waits for the execution to be cancelled via the returned
 *         {@link java.util.concurrent.Future} from Failsafe if not already cancelled.</td>
 *     <td>1</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *   </tr>
 * </table>
 *
 * <p>
 *
 * <table>
 *   <tr>
 *     <th>Customization</th>
 *     <th>Description</th>
 *     <th>New Cardinality of the Customized Action</th>
 *   </tr>
 *   <tr>
 *     <td>Only If</td>
 *     <td>Conditionally invokes the customized action based on a static boolean value provided at
 *         the time the action is constructed or based on the evaluation of a specified predicate
 *         at the time an attempt is made. if the result is to <code>false</code>, it will move on
 *         to the next recorded action to respond to the attempt instead of invoking the customized
 *         action</td>
 *     <td>Changes to 0 if the condition evaluates to <code>false</code>; otherwise no change</td>
 *   </tr>
 *   <tr>
 *     <td>Times</td>
 *     <td>Invokes the customized action for a specified number of attempts. If the number of
 *         attempts configured is 0, the customized action is skipped and it will move on to the next
 *         recorded action to respond to the attempt</td>
 *     <td>The new cardinality is based on the specified number of times to repeat</td>
 *   </tr>
 *   <tr>
 *     <td>Until Cancelled</td>
 *     <td>Invokes the customized action for each attempts until such time the execution is
 *         cancelled via the returned {@link java.util.concurrent.Future} from Failsafe. It will
 *         invoke the customized action if the execution has not been cancelled yet otherwise it will
 *         move on to the next action to respond to the attempt</td>
 *     <td>0 or more based on when the execution is cancelled</td>
 *   </tr>
 *   <tr>
 *     <td>Until Notified</td>
 *     <td>Invokes the customized action for each attempts until such time the specified condition is
 *         notified or raised. It will invoke the customized action if the condition has not been
 *         notified yet otherwise it will move on to the next action to respond to the attempt</td>
 *     <td>0 or more based on when the condition is notified</td>
 *   </tr>
 *   <tr>
 *     <td>Delayed</td>
 *     <td>Blocks the attempt and waits for a specified amount of time before proceeding with the
 *         customized action to respond to an attempt. This form of customization should be used
 *         lightly and avoided if at all possible as it introduces delays in test cases</td>
 *     <td>No changes</td>
 *   </tr>
 *   <tr>
 *     <td>Never</th>
 *     <td>Does not invoke the customized action but instead moves on to the next recorded action
 *         to respond to the attempt</td>
 *     <td>It becomes 0</td>
 *   </tr>
 *   <tr>
 *     <td>Forever</th>
 *     <td>Repeats the customized action for every subsequent attempts of a given execution</td>
 *     <td>Will repeat forever</td>
 *   </tr>
 * </table>
 *
 * <p>Actions are recorded by starting a sequence using one of the static methods in the {@link
 * net.jodah.failsafe.Actions} class and chaining them together. Each of the resulting sequence can
 * then be recorded for a particular execution. For example:
 *
 * <p><code><pre>
 *   Actions.waitTo("connect").before().returning(true);
 * </pre></code>
 *
 * <p>In this example, the first Failsafe attempt for the execution would be blocked until some
 * other part of the code decides to notify the <code>"connect"</code> condition and return <code>
 * true</code> as a result of that attempt.
 *
 * <p><code><pre>
 *   Actions.doThrow(NullPointerException)
 *     .then().waitTo("connect again").before().returning(false)
 *     .then().doReturn(true);
 * </pre></code>
 *
 * <p>In this example, the first Failsafe attempt for the next execution would result in a {@link
 * java.lang.NullPointerException} being thrown back. The expectation is that a second attempt would
 * be made at which point it would block until some other part of the code decides to notify the
 * <code>"connect again"</code> at which point it would return <code>false</code>. When a third
 * attempt is made by Failsafe, <code>true</code> would then be returned and no more attempts would
 * be expected for this execution.
 *
 * <p><b>Execution</b>
 *
 * <p>As indicated above, an execution encompass the point where Failsafe is called using one of the
 * methods where a task is provided to Failsafe. The execution is completed when the {@link
 * java.util.concurrent.Future} for asynchronous tasks is completed and can provide the result for
 * the task or the error that occurred or again when Failsafe return with a result or an exception
 * for synchronous tasks. Whenever an execution starts, the Failsafe controller will retrieve the
 * sequence of actions associated with the next recorded execution. An execution is recorded with
 * the controller using one of the {@link
 * net.jodah.failsafe.FailsafeController#onNextExecution(net.jodah.failsafe.Actions.Done)} method.
 * For Groovy developers, it is also possible to use a closure that returns the sequence of actions
 * using the {@link net.jodah.failsafe.FailsafeController#onNextExecution(groovy.lang.Closure)}
 * method. No matter which way you do it, the sequence of actions will be defined as indicated in
 * the previous section.
 *
 * <p>Each and every expected executions must be recorded and each and every expected attempts must
 * be accounted for with actions otherwise the Failsafe controller will generate an {@link
 * java.lang.AssertionError}.
 *
 * <p>Examples of how to record expected executions:
 *
 * <p><code><pre>
 *   def pingController = new FailsafeController('SolrClient Ping')
 *     .onNextExecution(doNothing())
 *     .and().onNextExecution {
 *       doThrow(pingError)
 *         .then().doThrow(new SolrException(ErrorCode.UNKNOWN, 'failed').untilNotifiedTo('connect')
 *         .then().doNothing()
 *     }
 * </pre></code>
 *
 * <p>In the above example, two expected executions are recorded with a Failsafe controller used to
 * perform ping connections to a Solr server. For the first execution, only one attempt is expected
 * which will result in ding nothing (here we are assuming the task is a void one). On the second
 * execution, an exception will be thrown if the condition <code>'connect'</code> has not been
 * notified. This will continue to happen for all subsequent attempts until such time where the
 * condition is notified. When that happens, the attempt will be handled using the <code>doNothing()
 * </code> action which is interpreted as a successful connection by the code under test.
 *
 * <p><b>Conditions</b>
 *
 * <p>The Failsafe controller maintains a simple set of conditions that have been notified since it
 * was created. Each condition is simply identified using a unique name. As explained before, it
 * provides a very simple latching mechanism between Failsafe's execution attempts and the main test
 * logic.
 *
 * <p>Conditions can be notified via the <i>Notify</i> action when responding to Failsafe attempts
 * or via the {@link net.jodah.failsafe.FailsafeController#notify(java.lang.String)} or {@link
 * net.jodah.failsafe.FailsafeController#notifyTo(java.lang.String)} methods on the controller by
 * the test code. To wait on a condition, simply use the <i>Wait</i> action, customized an action
 * with <i>Until Notified</i> or again call one of the two {@link
 * net.jodah.failsafe.FailsafeController#waitFor(java.lang.String)} or {@link
 * net.jodah.failsafe.FailsafeController#waitTo(java.lang.String)} methods on the controller.
 *
 * <p><b>Verification</b>
 *
 * <p>Once a test is completed, the controllers in play should be verified to see if any errors
 * occurred or again if all recorded expected executions and actions have been processed. This can
 * be done via the {@link net.jodah.failsafe.FailsafeController#verify()} method.
 *
 * <p><b>Timing, delays, and blockages</b>
 *
 * <p>To simplify the design of the controller all waits that are designed to block execution until
 * something happens do not expect a timeout to be specified. This is primarily due to the fact that
 * time should be removed from the equation when we are performing asynchronous testing such that
 * the tests ends up being deterministic. This is also why the Failsafe controller automatically
 * compress time for the specified retry policy. Because of this and because the code under test
 * could also be buggy and not behave as expected, the test might actually block. It is therefore
 * the responsibility of the test writer to take advantage of the test framework capability to
 * associate a timeout to a given test such that when a condition like this occurs, the test is
 * aborted and cleaned up which is where the Failsafe controller could be shutdown to unblock all
 * places where it might be waiting for something to happen.
 *
 * <p>In the best case scenario, your test cases should be quick and not delay (except if using the
 * <i>Delayed</i> customization). In the worst case scenario when a test case is failing, it might
 * actually end up blocking until such configured timeout with your test runner.
 *
 * <p><b>Shutting Down</b>
 *
 * <p>Each created Failsafe controller should be shutdown when cleaning up a test case to ensure
 * that all threads involved are properly released and that we stop waiting on something to happen
 * within the controller. The Failsafe controller was designed to support thread interruption to
 * stop doing what it is doing as soon as requested.
 */
package net.jodah.failsafe;
