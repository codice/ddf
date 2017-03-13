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
package org.apache.camel.component.sjms2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.MessageProducerResources;
import org.apache.camel.component.sjms.SjmsProducer;
import org.apache.camel.component.sjms.TransactionCommitStrategy;
import org.apache.camel.component.sjms.jms.ConnectionResource;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Base SjmsProducer class.
 */
public abstract class Sjms2Producer extends DefaultAsyncProducer {

    private GenericObjectPool<MessageProducerResources> producers;

    private ExecutorService executor;

    private Future<?> asyncStart;

    public Sjms2Producer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        executor = getEndpoint().getCamelContext()
                .getExecutorServiceManager()
                .newDefaultThreadPool(this, "SjmsProducer");
        if (getProducers() == null) {
            setProducers(new GenericObjectPool<MessageProducerResources>(new MessageProducerResourcesFactory()));
            getProducers().setMaxActive(getProducerCount());
            getProducers().setMaxIdle(getProducerCount());
            getProducers().setLifo(false);
            if (getEndpoint().isPrefillPool()) {
                if (getEndpoint().isAsyncStartListener()) {
                    asyncStart = getEndpoint().getComponent()
                            .getAsyncStartStopExecutorService()
                            .submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        fillProducersPool();
                                    } catch (Throwable e) {
                                        log.warn("Error filling producer pool for destination: "
                                                + getDestinationName()
                                                + ". This exception will be ignored.", e);
                                    }
                                }

                                @Override
                                public String toString() {
                                    return "AsyncStartListenerTask[" + getDestinationName() + "]";
                                }
                            });
                } else {
                    fillProducersPool();
                }
            }
        }
    }

    private void fillProducersPool() throws Exception {
        while (producers.getNumIdle() < producers.getMaxIdle()) {
            producers.addObject();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (asyncStart != null && !asyncStart.isDone()) {
            asyncStart.cancel(true);
        }
        if (getProducers() != null) {
            if (getEndpoint().isAsyncStopListener()) {
                getEndpoint().getComponent()
                        .getAsyncStartStopExecutorService()
                        .submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    getProducers().close();
                                    setProducers(null);
                                } catch (Throwable e) {
                                    log.warn("Error closing producers on destination: "
                                            + getDestinationName()
                                            + ". This exception will be ignored.", e);
                                }
                            }

                            @Override
                            public String toString() {
                                return "AsyncStopListenerTask[" + getDestinationName() + "]";
                            }
                        });
            } else {
                getProducers().close();
                setProducers(null);
            }
        }
        if (executor != null) {
            getEndpoint().getCamelContext()
                    .getExecutorServiceManager()
                    .shutdownGraceful(executor);
        }
    }

    @Override
    public Sjms2Endpoint getEndpoint() {
        return (Sjms2Endpoint) super.getEndpoint();
    }

    public abstract MessageProducerResources doCreateProducerModel() throws Exception;

    public abstract void sendMessage(Exchange exchange, AsyncCallback callback,
            MessageProducerResources producer) throws Exception;

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (log.isDebugEnabled()) {
            log.debug("Processing Exchange.id:{}", exchange.getExchangeId());
        }

        try {
            MessageProducerResources producer = getProducers().borrowObject();
            if (producer == null) {
                exchange.setException(new Exception(
                        "Unable to send message: connection not available"));
            } else {
                if (!isSynchronous()) {
                    if (log.isDebugEnabled()) {
                        log.debug("  Sending message asynchronously: {}",
                                exchange.getIn()
                                        .getBody());
                    }
                    getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sendMessage(exchange, callback, producer);
                            } catch (Exception e) {
                                ObjectHelper.wrapRuntimeCamelException(e);
                            }
                        }
                    });
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("  Sending message synchronously: {}",
                                exchange.getIn()
                                        .getBody());
                    }
                    sendMessage(exchange, callback, producer);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Processing Exchange.id:{}", exchange.getExchangeId() + " - FAILED");
            }
            if (log.isDebugEnabled()) {
                log.trace("Exception: " + e.getLocalizedMessage(), e);
            }
            exchange.setException(e);
        }
        log.debug("Processing Exchange.id:{}", exchange.getExchangeId() + " - SUCCESS");

        return isSynchronous();
    }

    protected ConnectionResource getConnectionResource() {
        return getEndpoint().getConnectionResource();
    }

    /**
     * Gets the acknowledgment mode for this instance of DestinationProducer.
     *
     * @return int
     */
    public int getAcknowledgeMode() {
        return getEndpoint().getAcknowledgementMode()
                .intValue();
    }

    /**
     * Gets the synchronous value for this instance of DestinationProducer.
     *
     * @return true if synchronous, otherwise false
     */
    public boolean isSynchronous() {
        return getEndpoint().isSynchronous();
    }

    /**
     * Gets the replyTo for this instance of DestinationProducer.
     *
     * @return String
     */
    public String getReplyTo() {
        return getEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the destinationName for this instance of DestinationProducer.
     *
     * @return String
     */
    public String getDestinationName() {
        return getEndpoint().getDestinationName();
    }

    /**
     * Gets the MessageProducerPool value of producers for this instance of
     * SjmsProducer.
     *
     * @return the producers
     */
    public GenericObjectPool<MessageProducerResources> getProducers() {
        return producers;
    }

    /**
     * Sets the producer pool for this instance of SjmsProducer.
     *
     * @param producers A MessageProducerPool
     */
    public void setProducers(GenericObjectPool<MessageProducerResources> producers) {
        this.producers = producers;
    }

    /**
     * Test to verify if this endpoint is a JMS Topic or Queue.
     *
     * @return true if it is a Topic, otherwise it is a Queue
     */
    public boolean isTopic() {
        return getEndpoint().isTopic();
    }

    /**
     * Test to determine if this endpoint should use a JMS Transaction.
     *
     * @return true if transacted, otherwise false
     */
    public boolean isEndpointTransacted() {
        return getEndpoint().isTransacted();
    }

    /**
     * Returns the named reply to value for this producer
     *
     * @return true if it is a Topic, otherwise it is a Queue
     */
    public String getNamedReplyTo() {
        return getEndpoint().getNamedReplyTo();
    }

    /**
     * Gets the producerCount for this instance of SjmsProducer.
     *
     * @return int
     */
    public int getProducerCount() {
        return getEndpoint().getProducerCount();
    }

    /**
     * Gets consumerCount for this instance of SjmsProducer.
     *
     * @return int
     */
    public int getConsumerCount() {
        return getEndpoint().getConsumerCount();
    }

    /**
     * Gets the executor for this instance of SjmsProducer.
     *
     * @return ExecutorService
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Gets the ttl for this instance of SjmsProducer.
     *
     * @return long
     */
    public long getTtl() {
        return getEndpoint().getTtl();
    }

    /**
     * Gets the boolean value of persistent for this instance of SjmsProducer.
     *
     * @return true if persistent, otherwise false
     */
    public boolean isPersistent() {
        return getEndpoint().isPersistent();
    }

    /**
     * Gets responseTimeOut for this instance of SjmsProducer.
     *
     * @return long
     */
    public long getResponseTimeOut() {
        return getEndpoint().getResponseTimeOut();
    }

    /**
     * Gets commitStrategy for this instance of SjmsProducer.
     *
     * @return TransactionCommitStrategy
     */
    public TransactionCommitStrategy getCommitStrategy() {
        return getEndpoint().getTransactionCommitStrategy();
    }

    /**
     * The {@link MessageProducerResources} pool for all {@link SjmsProducer}
     * classes.
     */
    protected class MessageProducerResourcesFactory
            extends BasePoolableObjectFactory<MessageProducerResources> {

        @Override
        public MessageProducerResources makeObject() throws Exception {
            return doCreateProducerModel();
        }

        @Override
        public void destroyObject(MessageProducerResources model) throws Exception {
            if (model.getMessageProducer() != null) {
                model.getMessageProducer()
                        .close();
            }

            if (model.getSession() != null) {
                try {
                    if (model.getSession()
                            .getTransacted()) {
                        try {
                            model.getSession()
                                    .rollback();
                        } catch (Exception e) {
                            // Do nothing. Just make sure we are cleaned up
                        }
                    }
                    model.getSession()
                            .close();
                } catch (Exception e) {
                    // TODO why is the session closed already?
                }
            }
        }
    }

}
