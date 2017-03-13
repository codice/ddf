/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms2.jms;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.camel.util.ObjectHelper;

/**
 *
 */
public final class Jms2ObjectFactory {

    private Jms2ObjectFactory() {
        //Helper class
    }

    @Deprecated
    public static MessageConsumer createMessageConsumer(Session session, Destination destination,
            String messageSelector, boolean topic, String durableSubscriptionId) throws Exception {
        // noLocal is default false accordingly to JMS spec
        return createMessageConsumer(session,
                destination,
                messageSelector,
                topic,
                durableSubscriptionId,
                false);
    }

    @Deprecated
    public static MessageConsumer createMessageConsumer(Session session, Destination destination,
            String messageSelector, boolean topic, String durableSubscriptionId, boolean noLocal)
            throws Exception {
        return createMessageConsumer(session,
                destination,
                messageSelector,
                topic,
                durableSubscriptionId,
                true,
                false,
                noLocal);
    }

    public static MessageConsumer createMessageConsumer(Session session, Destination destination,
            String messageSelector, boolean topic, String subscriptionId, boolean durable,
            boolean shared) throws Exception {
        return createMessageConsumer(session,
                destination,
                messageSelector,
                topic,
                subscriptionId,
                durable,
                shared,
                false);
    }

    public static MessageConsumer createMessageConsumer(Session session, Destination destination,
            String messageSelector, boolean topic, String subscriptionId, boolean durable,
            boolean shared, boolean noLocal) throws Exception {

        if (topic) {
            return getTopicMessageConsumer(session,
                    destination,
                    messageSelector,
                    subscriptionId,
                    durable,
                    shared,
                    noLocal);
        } else {
            return getQueueMessageConsumer(session, destination, messageSelector);
        }
    }

    private static MessageConsumer getTopicMessageConsumer(Session session, Destination destination,
            String messageSelector, String subscriptionId, boolean durable, boolean shared,
            boolean noLocal) throws JMSException {
        if (ObjectHelper.isNotEmpty(subscriptionId)) {
            return getSubscriptionTopicConsumer(session,
                    destination,
                    messageSelector,
                    subscriptionId,
                    durable,
                    shared,
                    noLocal);
        } else {
            return getSubscriptionlessTopicConsumer(session, destination, messageSelector, noLocal);
        }
    }

    private static MessageConsumer getSubscriptionTopicConsumer(Session session,
            Destination destination, String messageSelector, String subscriptionId, boolean durable,
            boolean shared, boolean noLocal) throws JMSException {
        if (shared) {
            if (durable) {
                if (ObjectHelper.isNotEmpty(messageSelector)) {
                    return session.createSharedDurableConsumer((Topic) destination,
                            subscriptionId,
                            messageSelector);
                } else {
                    return session.createSharedDurableConsumer((Topic) destination, subscriptionId);
                }
            } else {
                if (ObjectHelper.isNotEmpty(messageSelector)) {
                    return session.createSharedConsumer((Topic) destination,
                            subscriptionId,
                            messageSelector);
                } else {
                    return session.createSharedConsumer((Topic) destination, subscriptionId);
                }
            }
        } else {
            if (durable) {
                if (ObjectHelper.isNotEmpty(messageSelector)) {
                    return session.createDurableSubscriber((Topic) destination,
                            subscriptionId,
                            messageSelector,
                            noLocal);
                } else {
                    return session.createDurableSubscriber((Topic) destination, subscriptionId);
                }
            } else {
                return getSubscriptionlessTopicConsumer(session,
                        destination,
                        messageSelector,
                        noLocal);
            }
        }
    }

    private static MessageConsumer getSubscriptionlessTopicConsumer(Session session,
            Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        MessageConsumer messageConsumer;
        if (ObjectHelper.isNotEmpty(messageSelector)) {
            return session.createConsumer(destination, messageSelector, noLocal);
        } else {
            return session.createConsumer(destination);
        }
    }

    private static MessageConsumer getQueueMessageConsumer(Session session, Destination destination,
            String messageSelector) throws JMSException {
        if (ObjectHelper.isNotEmpty(messageSelector)) {
            return session.createConsumer(destination, messageSelector);
        } else {
            return session.createConsumer(destination);
        }
    }

    public static MessageProducer createMessageProducer(Session session, Destination destination,
            boolean persistent, long ttl) throws Exception {
        MessageProducer messageProducer = session.createProducer(destination);
        messageProducer.setDeliveryMode(persistent ?
                DeliveryMode.PERSISTENT :
                DeliveryMode.NON_PERSISTENT);
        if (ttl > 0) {
            messageProducer.setTimeToLive(ttl);
        }
        return messageProducer;
    }
}
