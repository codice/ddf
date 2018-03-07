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
package org.codice.ddf.itests.common.cometd;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import com.jayway.restassured.path.json.JsonPath;
import java.net.ConnectException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
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

/**
 * CometD client used to listen for messages on CometD channels and interact with DDF's CometD
 * endpoint.
 *
 * <p>Below is an example on how to listen for messages on the notifications channel:
 *
 * <p>
 *
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

  private static final long TIMEOUT = AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final long MAX_NETWORK_DELAY = 60000;

  private static final String QUERY_PUBLISH_CHANNEL = "/service/query";

  private final BayeuxClient bayeuxClient;

  private final HttpClient httpClient;

  private final List<MessageListener> messageListeners = new ArrayList<>();

  private static final String ACTIVITIES_CHANNEL = "/ddf/activities/**";

  private static final String DATA_MESSAGE = "data.message";

  private static final String DATA_ID = "data.id";

  private static final String DOWNLOAD_CANCELLED = "Resource retrieval cancelled";

  private static final String DOWNLOAD_COMPLETED = "Resource retrieval completed";

  private static final String RETRIEVAL_FAILURE = "Unable to retrieve";

  private Set<String> downloadIds = new HashSet<>();

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
    transport.setOption(ClientTransport.MAX_NETWORK_DELAY_OPTION, MAX_NETWORK_DELAY);
    bayeuxClient = new BayeuxClient(url, transport);
  }

  /**
   * Creates a CometD client with authentication.
   *
   * @param url CometD endpoint
   * @param realm security realm
   * @param username user name
   * @param password password
   * @throws Exception thrown if client setup fails
   */
  public CometDClient(String url, String realm, String username, String password) throws Exception {
    this(url);
    URI uri = new URI(url);
    httpClient
        .getAuthenticationStore()
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
    bayeuxClient.getChannel(Channel.META_HANDSHAKE).addListener(handshakeListener);
    bayeuxClient.handshake();
    boolean connected = bayeuxClient.waitFor(TIMEOUT, BayeuxClient.State.CONNECTED);
    if (!connected) {
      shutdownHttpClient();
      String message =
          String.format(
              "%s failed to connect to the server at %s",
              this.getClass().getName(), bayeuxClient.getURL());
      LOGGER.error(message);
      throw new ConnectException(message);
    }
  }

  /**
   * Publishes a message
   *
   * @param channel channel to publish message to
   * @param message message to publish
   */
  public void publish(String channel, Map<String, Object> message) {
    LOGGER.debug("Publishing message {} to channel {}", message, channel);

    bayeuxClient
        .getChannel(channel)
        .publish(
            message,
            (responseChannel, responseMessage) ->
                LOGGER.debug(
                    "Response {} received for message {} on channel {}",
                    responseMessage.getJSON(),
                    responseChannel,
                    message));
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
      LOGGER.debug("Already subscribed to channel {}", channel);
    }
  }

  /**
   * Gets the first message that matches the search criterion
   *
   * @param searchCriterion a string that will be searched for in the messages
   * @return the desired message if found
   */
  public Optional<String> searchMessages(String searchCriterion) {
    List<String> messages = getAllMessages();

    return messages.stream().filter(query -> query.contains(searchCriterion)).findFirst();
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
    return messageListeners
        .stream()
        .filter(l -> l.getChannel().equals(channel))
        .flatMap(l -> l.getMessages().stream())
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
    return messageListeners
        .stream()
        .flatMap(l -> l.getMessages().stream())
        .collect(Collectors.toList());
  }

  /**
   * Gets the list of messages received on all channels in time ascending order, i.e., from oldest
   * to most recent.
   *
   * @return list of message received since the client was started
   */
  public List<String> getAllMessagesInAscOrder() {
    List<String> messages = getAllMessages();
    Collections.sort(messages, new AscendingTimestampComparator());
    return messages;
  }

  /**
   * Publishes a search message for a specific metacard ID.
   *
   * @param responseChannel ID of the channel where the response should be sent
   * @param source source to query
   * @param metacardId ID of the metacard to retrieve
   */
  public void searchByMetacardId(String responseChannel, String source, String metacardId) {
    Map<String, Object> data = new HashMap<>();

    data.put("cql", String.format("(\"anyText\" ILIKE '%s')", metacardId));
    data.put("id", responseChannel);
    data.put("federation", "enterprise");
    data.put("src", source);
    data.put("radiusUnits", "meters");
    data.put("count", 250L);
    data.put("start", 1L);
    data.put("format", "geojson");
    data.put("scheduleUnits", "minutes");
    data.put("timeType", "modified");
    data.put("locationType", "latlon");
    data.put("sort", "modified:desc");
    data.put("q", metacardId);
    data.put("sortOrder", "desc");
    data.put("sortField", "modified");
    data.put("radius", "0");

    publish(QUERY_PUBLISH_CHANNEL, data);
  }

  /**
   * Cancels a resource download.
   *
   * @param downloadId ID of the download to cancel
   */
  public void cancelDownload(String downloadId) {
    Map<String, Object> jsonMap = new HashMap<>();
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("id", downloadId);
    dataMap.put("action", "cancel");
    data.add(dataMap);
    jsonMap.put("data", data);
    publish("/service/action", jsonMap);
  }

  /** Cancels all resource downloads. */
  public void cancelAllDownloads() {

    downloadIds.stream().forEach(this::cancelDownload);
  }

  /**
   * Un-subscribes from a channel. Un-subscribing from the same channel multiple has no effect.
   *
   * @param channel channel name
   */
  public void unsubscribe(String channel) {
    verifyConnected();
    Optional<MessageListener> optionalMessageListener =
        messageListeners.stream().filter(l -> l.getChannel().equals(channel)).findFirst();

    optionalMessageListener.ifPresent(
        (messageListener) -> {
          bayeuxClient.getChannel(channel).unsubscribe(messageListener);
          messageListeners.remove(messageListener);
        });
  }

  /** Un-subscribes from all channels. */
  public void unsubscribeFromAllChannels() {
    verifyConnected();
    messageListeners.forEach(l -> bayeuxClient.getChannel(l.getChannel()).unsubscribe(l));
    messageListeners.clear();
  }

  /**
   * Shuts down the client.
   *
   * @throws Exception thrown if the shutdown fails
   */
  public void shutdown() throws Exception {
    verifyConnected();
    LOGGER.debug("{} is shutting down!", this.getClass().getName());
    unsubscribeFromAllChannels();
    httpClient.stop();
    bayeuxClient.disconnect();
    bayeuxClient.waitFor(TIMEOUT, BayeuxClient.State.DISCONNECTED);
  }

  private void verifyConnected() {
    if (!bayeuxClient.isConnected()) {
      String message =
          String.format(
              "%s has not connected to the server at %s",
              this.getClass().getName(), bayeuxClient.getURL());
      LOGGER.error(message);
      throw new IllegalStateException(message);
    }
  }

  private void verifySubscribed() {
    if (CollectionUtils.isEmpty(messageListeners)) {
      String message =
          String.format("%s is not subscribed to any channels", this.getClass().getName());
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
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          }
        };

    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, trustAllCerts, new SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    HostnameVerifier hostnameVerifier =
        (s, sslSession) -> s.equalsIgnoreCase(sslSession.getPeerHost());
    HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
  }

  private boolean alreadySubscribed(String channel) {
    return messageListeners.stream().filter(l -> l.getChannel().equals(channel)).count() == 1;
  }

  public Set<String> getDownloadIds() {

    return Collections.unmodifiableSet(downloadIds);
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
      LOGGER.debug("On channel {} received message {}", channel, message.getJSON());
      LOGGER.debug("timestamp of message: {}", message.getDataAsMap().get("timestamp"));

      JsonPath jsonPath = JsonPath.from(message.getJSON().toString());
      String dataMessage = jsonPath.getString(DATA_MESSAGE);

      if (channel.getChannelId().toString().equals(ACTIVITIES_CHANNEL)) {

        String downloadId = jsonPath.getString(DATA_ID);

        if (StringUtils.isNotEmpty(downloadId)) {

          if (dataMessage.contains(DOWNLOAD_CANCELLED)
              || dataMessage.contains(DOWNLOAD_COMPLETED)
              || dataMessage.contains(RETRIEVAL_FAILURE)) {
            downloadIds.remove(downloadId);

          } else {

            downloadIds.add(downloadId);
          }
        }
      }

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
      return LocalDateTime.parse(timestamp, ISO_DATE_TIME).toLocalTime();
    }
  }
}
