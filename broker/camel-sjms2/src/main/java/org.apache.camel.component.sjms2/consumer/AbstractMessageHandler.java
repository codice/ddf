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
package org.apache.camel.component.sjms2.consumer;

import static org.apache.camel.util.ObjectHelper.wrapRuntimeCamelException;

import java.util.concurrent.ExecutorService;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.sjms2.Sjms2Endpoint;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract MessageListener
 */
public abstract class AbstractMessageHandler implements MessageListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ExecutorService executor;

    private final Sjms2Endpoint endpoint;

    private AsyncProcessor processor;

    private Session session;

    private boolean transacted;

    private boolean synchronous = true;

    private Synchronization synchronization;

    private boolean topic;

    public AbstractMessageHandler(Sjms2Endpoint endpoint, ExecutorService executor) {
        this.endpoint = endpoint;
        this.executor = executor;
    }

    public AbstractMessageHandler(Sjms2Endpoint endpoint, ExecutorService executor,
            Synchronization synchronization) {
        this.synchronization = synchronization;
        this.endpoint = endpoint;
        this.executor = executor;
    }

    /*
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     *
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        RuntimeCamelException rce = null;
        try {
            Exchange exchange = getEndpoint().createExchange(message, getSession());

            log.debug("Processing Exchange.id:{}", exchange.getExchangeId());

            try {
                if (isTransacted() || isSynchronous()) {
                    log.debug("Handling synchronous message: {}",
                            exchange.getIn()
                                    .getBody());
                    handleMessage(exchange);
                    if (exchange.isFailed()) {
                        synchronization.onFailure(exchange);
                    } else {
                        synchronization.onComplete(exchange);
                    }
                } else {
                    log.debug("Handling asynchronous message: {}",
                            exchange.getIn()
                                    .getBody());
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                handleMessage(exchange);
                            } catch (Exception e) {
                                exchange.setException(e);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                if (exchange.getException() == null) {
                    exchange.setException(e);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            rce = wrapRuntimeCamelException(e);
        } finally {
            if (rce != null) {
                throw rce;
            }
        }
    }

    public abstract void handleMessage(Exchange exchange);

    /**
     * Method will be called to
     */
    public abstract void close();

    public boolean isTransacted() {
        return transacted;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    public Sjms2Endpoint getEndpoint() {
        return endpoint;
    }

    public AsyncProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(AsyncProcessor processor) {
        this.processor = processor;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean async) {
        synchronous = async;
    }

    public boolean isTopic() {
        return topic;
    }

    public void setTopic(boolean topic) {
        this.topic = topic;
    }
}
