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
package org.codice.ddf.admin.zookeeper;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.felix.utils.properties.ConfigurationHandler;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.PathUtils;

public final class KeeperUtils {
  private static final String PROP_HOSTNAME = "org.codice.ddf.system.hostname";

  // Name of the system property for the config namespace
  private static final String PROP_ZOO_NAMESPACE = "zookeeper.config.namespace";

  // Name of the system property for the separate configuration zookeeper connection
  private static final String PROP_ZOO_CONNECTION = "zookeeper.config.connection";

  // Name of the system property for the solr cloud zookeeper connection
  private static final String PROP_ZOO_SOLR_CLOUD = "solr.cloud.zookeeper";

  private static final String DEFAULT_CONNECT_STRING = "localhost:2181";

  // Only used if the hostname isn't set
  private static final String DEFAULT_NAMESPACE_NAME = "default";

  private KeeperUtils() {}

  public static String getConnectionString() {
    String configZookeeper = System.getProperty(PROP_ZOO_CONNECTION);
    if (configZookeeper != null) {
      return configZookeeper;
    }
    String solrCloudZookeeper = System.getProperty(PROP_ZOO_SOLR_CLOUD);
    if (solrCloudZookeeper != null) {
      return solrCloudZookeeper;
    }
    return DEFAULT_CONNECT_STRING;
  }

  public static String getNamespaceName() {
    String zookeeperConfigNamespace = System.getProperty(PROP_ZOO_NAMESPACE);
    if (zookeeperConfigNamespace != null) {
      return zookeeperConfigNamespace;
    }
    String hostname = System.getProperty(PROP_HOSTNAME);
    if (hostname != null) {
      return hostname;
    }
    return DEFAULT_NAMESPACE_NAME;
  }

  /** Convert a dictionary to a configuration byte array for writing to zookeeper */
  public static byte[] encodeData(Dictionary props) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      ConfigurationHandler.write(stream, props);
      return stream.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Create a dictionary from a configuration byte array for reading from zookeeper */
  public static Dictionary decodeData(byte[] bytes) {
    try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
      return ConfigurationHandler.read(stream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Special case of a {@link Supplier} that throws an {@link IOException}. */
  @FunctionalInterface
  public interface KeeperSupplier {
    ZooKeeper get() throws IOException;
  }

  /** Object representing a path to a znode. */
  public static class ZPath {
    private static final String FWRD_SLASH = "/";

    private static final String BLANK = "";

    private final List<String> components;

    // Use static methods for construction
    private ZPath(final List<String> components) {
      this.components = components;
    }

    /**
     * @return {@code null} if this {@link ZPath} does not point to any specific configuration.
     *     Otherwise, it returns the service pid of the configuration it points to.
     */
    @Nullable
    public String getPid() {
      if (isManagedService()) {
        return components.get(2);
      }
      if (isFactoryInstance()) {
        return format("%s.%s", components.get(2), components.get(3));
      }
      return null;
    }

    private boolean isManagedService() {
      return components.size() == 3;
    }

    private boolean isFactoryInstance() {
      return components.size() == 4;
    }

    @Override
    public String toString() {
      return components.stream().collect(Collectors.joining(FWRD_SLASH, FWRD_SLASH, BLANK));
    }

    /**
     * Given a valid String representation of a znode, parse it into a {@link ZPath).
     *
     * @param path the path to a znode.
     * @return a {@link ZPath} that points to the znode.
     */
    public static ZPath parse(String path) {
      PathUtils.validatePath(path);
      return new ZPath(
          Arrays.stream(path.split(FWRD_SLASH))
              .filter(str -> !str.isEmpty())
              .collect(Collectors.toList()));
    }

    /**
     * Given an array of znode names, obtain a {@link ZPath} pointing to the terminal znode.
     *
     * @param routeComponents array of znode names.
     * @return a {@link ZPath} that traverses the route components.
     */
    public static ZPath from(String... routeComponents) {
      if (Arrays.stream(routeComponents).anyMatch(str -> str.contains(FWRD_SLASH))) {
        throw new IllegalArgumentException(
            "Invalid path components: " + Arrays.toString(routeComponents));
      }
      return new ZPath(Arrays.asList(routeComponents));
    }
  }
}
