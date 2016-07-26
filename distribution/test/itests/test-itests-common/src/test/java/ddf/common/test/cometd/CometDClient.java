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
package ddf.common.test.cometd;

import java.net.ConnectException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.collections.CollectionUtils;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.path.json.JsonPath;

/**
 * CometD client used to listen for messages on CometD channels.
 * <p>
 * Below is an example on how to listen for messages on the notifications channel:
 * <p>
 * <pre>
 * // Creates a CometDClient that will connect to the CometD Server at the specified URL.
 * CometDClient cometDClient = new CometDClient(cometDEndpointUrl);
 *
 * // Starts the cometDClient and performs the initial handshake with the CometD server
 * cometDClient.start();
 *
 * // Subscribes to the notifications channel
 * cometDClient.subscribe("/ddf/notifications/**"));
 *
 * // Retrieves messages on all subscribed channels (in this example, receives messages on
 * // on the notifications channel).
 * List<String> messages = cometDClient.getAllMessages();
 *
 * // Shutdown the cometD Client and un-subscribes from all channels
 * cometDClient.shutdown();
 * </pre>
 */
public class CometDClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CometDClient.class);

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    private final BayeuxClient bayeuxClient;

    private final HttpClient httpClient;

    private final List<MessageListener> messageListeners = new ArrayList<>();

    /**
     * Creates a CometD client without authentication.
     *
     * @param url CometD endpoint
     * @throws Exception thrown if client setup fails
     */
    public CometDClient(String url) throws Exception {
        SslContextFactory sslContextFactory = new SslContextFactory(true);
        httpClient = new HttpClient(sslContextFactory);
        doTrustAllCertificates();
        ClientTransport transport = new LongPollingTransport(new HashMap<>(), httpClient);
        bayeuxClient = new BayeuxClient(url, transport);
    }

    /**
     * Creates a CometD client with authentication.
     *
     * @param url      CometD endpoint
     * @param realm    security realm
     * @param username user name
     * @param password password
     * @throws Exception thrown if client setup fails
     */
    public CometDClient(String url, String realm, String username, String password)
            throws Exception {
        this(url);
        URI uri = new URI(url);
        httpClient.getAuthenticationStore()
                .addAuthentication(new BasicAuthentication(uri, realm, username, password));
    }

    /**
     * Starts the client.
     *
     * @throws Exception thrown if the client fails to start
     */
    public void start() throws Exception {
        httpClient.start();
        LOGGER.debug("HTTP client started: {}", httpClient.isStarted());
        MessageListener handshakeListener = new MessageListener(Channel.META_HANDSHAKE);
        bayeuxClient.getChannel(Channel.META_HANDSHAKE)
                .addListener(handshakeListener);
        bayeuxClient.handshake();
        boolean connected = bayeuxClient.waitFor(TIMEOUT, BayeuxClient.State.CONNECTED);
        if (!connected) {
            shutdownHttpClient();
            String message = String.format("%s failed to connect to the server at %s",
                    this.getClass()
                            .getName(),
                    bayeuxClient.getURL());
            LOGGER.error(message);
            throw new ConnectException(message);
        }
    }

    /**
     * Subscribes to a channel. Subscribing to the same channel multiple times has no effect.
     *
     * @param channel channel name
     */
    public void subscribe(String channel) {
        verifyConnected();
        if (!alreadySubscribed(channel)) {
            ClientSessionChannel clientSessionChannel = bayeuxClient.getChannel(channel);
            MessageListener messageListener = new MessageListener(channel);
            clientSessionChannel.subscribe(messageListener);
            messageListeners.add(messageListener);
        } else {
            LOGGER.warn("Already subscribed to channel {}", channel);
        }
    }

    /**
     * Gets the list of messages received on a given channel.
     *
     * @param channel channel name
     * @return list of message received since the client was started
     */
    public List<String> getMessages(String channel) {
        verifyConnected();
        verifySubscribed();
        return messageListeners.stream()
                .filter(l -> l.getChannel()
                        .equals(channel))
                .flatMap(l -> l.getMessages()
                        .stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of messages received on a given channel in time ascending order, i.e., from
     * oldest to most recent.
     *
     * @param channel channel name
     * @return list of message received since the client was started
     */
    public List<String> getMessagesInAscOrder(String channel) {
        List<String> messages = getMessages(channel);
        Collections.sort(messages, new AscendingTimestampComparator());
        return messages;
    }

    /**
     * Gets the CometD client ID.
     *
     * @return CometD client ID
     */
    public String getClientId() {
        verifyConnected();
        return bayeuxClient.getId();
    }

    /**
     * Gets the list of messages received on all channels.
     *
     * @return list of message received since the client was started
     */
    public List<String> getAllMessages() {
        verifyConnected();
        verifySubscribed();
        return messageListeners.stream()
                .flatMap(l -> l.getMessages()
                        .stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of messages received on all channels in time ascending order, i.e., from
     * oldest to most recent.
     *
     * @return list of message received since the client was started
     */
    public List<String> getAllMessagesInAscOrder() {
        List<String> messages = getAllMessages();
        Collections.sort(messages, new AscendingTimestampComparator());
        return messages;
    }

    /**
     * Un-subscribes from a channel. Un-subscribing from the same channel multiple has no effect.
     *
     * @param channel channel name
     */
    public void unsubscribe(String channel) {
        verifyConnected();
        org.cometd.bayeux.client.ClientSessionChannel.MessageListener messageListener =
                messageListeners.stream()
                        .filter(l -> l.getChannel()
                                .equals(channel))
                        .findFirst()
                        .get();
        bayeuxClient.getChannel(channel)
                .unsubscribe(messageListener);
        messageListeners.remove(messageListener);
    }

    /**
     * Un-subscribes from all channels.
     */
    public void unsubscribeFromAllChannels() {
        verifyConnected();
        messageListeners.stream()
                .forEach(l -> bayeuxClient.getChannel(l.getChannel())
                        .unsubscribe(l));
        messageListeners.clear();
    }

    /**
     * Shuts down the client.
     *
     * @throws Exception thrown if the shutdown fails
     */
    public void shutdown() throws Exception {
        verifyConnected();
        LOGGER.debug("{} is shutting down!",
                this.getClass()
                        .getName());
        unsubscribeFromAllChannels();
        httpClient.stop();
        bayeuxClient.disconnect();
        bayeuxClient.waitFor(TIMEOUT, BayeuxClient.State.DISCONNECTED);
    }

    private void verifyConnected() {
        if (!bayeuxClient.isConnected()) {
            String message = String.format("%s has not connected to the server at %s",
                    this.getClass()
                            .getName(),
                    bayeuxClient.getURL());
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }

    private void verifySubscribed() {
        if (CollectionUtils.isEmpty(messageListeners)) {
            String message = String.format("%s is not subscribed to any channels",
                    this.getClass()
                            .getName());
            throw new IllegalStateException(message);
        }
    }

    private void shutdownHttpClient() throws Exception {
        if (!httpClient.isStopped()) {
            LOGGER.debug("Stopping http client.");
            httpClient.stop();
        }
    }

    private void doTrustAllCertificates() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {
                return;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {
                return;
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HostnameVerifier hostnameVerifier =
                (s, sslSession) -> s.equalsIgnoreCase(sslSession.getPeerHost());
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    }

    private boolean alreadySubscribed(String channel) {
        return messageListeners.stream()
                .filter(l -> l.getChannel()
                        .equals(channel))
                .count() == 1;
    }

    private class MessageListener
            implements org.cometd.bayeux.client.ClientSessionChannel.MessageListener {

        private final List<String> messages;

        private final String channel;

        MessageListener(String channel) {
            messages = Collections.synchronizedList(new ArrayList<>());
            this.channel = channel;
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            LOGGER.debug("On channel {} received message {} at {}", channel, message.getJSON());
            LOGGER.debug("timestamp of message: {}",
                    message.getDataAsMap()
                            .get("timestamp"));
            messages.add(message.getJSON());
        }

        private String getChannel() {
            return channel;
        }

        private List<String> getMessages() {
            return messages;
        }
    }

    private class AscendingTimestampComparator implements Comparator<String> {

        private static final String TIMESTAMP_PATH = "data.timestamp";

        @Override
        public int compare(String jsonMessage1, String jsonMessage2) {
            LocalTime time1 = getLocalTime(jsonMessage1);
            LocalTime time2 = getLocalTime(jsonMessage2);
            return time1.compareTo(time2);
        }

        private LocalTime getLocalTime(String jsonMessage) {
            JsonPath jsonPath = JsonPath.from(jsonMessage);
            String timestamp = jsonPath.getString(TIMESTAMP_PATH);
            LocalTime time = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
                    .toLocalTime();
            return time;
        }
    }
}
