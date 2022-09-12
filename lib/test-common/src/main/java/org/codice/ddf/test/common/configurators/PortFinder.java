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
package org.codice.ddf.test.common.configurators;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to find and reserve a range of ports, and assign ports within that range by name. For
 * instance, creating a new instance of {@link PortFinder} will reserve a block of ports, calling
 * {@link #getPort(String)} or {@link #getPortAsString(String)} the first time will assign a
 * specific port in that range with the name provided, and calling one of those two methods with
 * that name will return the same port number afterwards.
 */
public class PortFinder implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(PortFinder.class);

  private static final int BASE_PORT = 20000;

  private static final int PORT_BLOCK_SIZE = 20;

  private ServerSocket placeHolderSocket;

  private int nextPort;

  private Map<String, Integer> registeredPorts = new HashMap<>();

  /** Default constructor. Finds and reserves a range of ports. */
  public PortFinder() {
    placeHolderSocket = findServerSocket(BASE_PORT);
    nextPort = placeHolderSocket.getLocalPort() + 1;
  }

  /**
   * Gets the port number associated with the key provided. Assigns a new port number if the key
   * couldn't be found.
   *
   * @param portKey key of the port to look up
   * @return port number associated with the key provided
   */
  public int getPort(String portKey) {
    registeredPorts.computeIfAbsent(portKey, k -> nextPort++);
    return registeredPorts.get(portKey);
  }

  /**
   * Gets the port number associated with the key provided as a string. Assigns a new port number if
   * the key couldn't be found.
   *
   * @param portKey key of the port to look up
   * @return port number associated with the key provided
   */
  public String getPortAsString(String portKey) {
    return String.valueOf(getPort(portKey));
  }

  @Override
  public void close() throws IOException {
    placeHolderSocket.close();
  }

  private ServerSocket findServerSocket(int portToTry) {
    try {
      ServerSocket markerSocket = getMarkerSocket(portToTry);

      try {
        checkAllPortsInRangeAvailable(portToTry);
        return markerSocket;
      } catch (Exception e) {
        markerSocket.close();
        throw e;
      }
    } catch (Exception e) {
      LOGGER.debug("Port {} unavailable, trying {}", portToTry, portToTry + PORT_BLOCK_SIZE);
      return findServerSocket(portToTry + PORT_BLOCK_SIZE);
    }
  }

  private ServerSocket getMarkerSocket(int portToTry) throws IOException {
    // No need for try-with-resource, handled by calling method
    @SuppressWarnings("squid:S2095")
    ServerSocket markerSocket = new ServerSocket(portToTry);
    markerSocket.setReuseAddress(true);
    return markerSocket;
  }

  private void checkAllPortsInRangeAvailable(int markerPort) throws IOException {

    for (int i = markerPort + 1; i < markerPort + PORT_BLOCK_SIZE; i++) {
      try (ServerSocket ignored = new ServerSocket(i)) {
        // Nothing to do, just checking port i was available
      }
    }
  }
}
