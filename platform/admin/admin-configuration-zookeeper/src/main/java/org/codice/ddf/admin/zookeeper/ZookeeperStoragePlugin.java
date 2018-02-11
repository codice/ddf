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
import static org.codice.ddf.admin.zookeeper.KeeperUtils.decodeData;
import static org.codice.ddf.admin.zookeeper.KeeperUtils.encodeData;
import static org.codice.ddf.admin.zookeeper.KeeperUtils.getConnectionString;
import static org.codice.ddf.admin.zookeeper.KeeperUtils.getNamespaceName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.codice.ddf.admin.zookeeper.KeeperUtils.ZPath;
import org.codice.felix.cm.internal.ConfigurationContext;
import org.codice.felix.cm.internal.ConfigurationStoragePlugin;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides support for synchronizing configs with a Zookeeper cluster.
 *
 * <p>The znode structure is of the form /ddf/[namespace]/[pid] with the root of the structure
 * containing multiple hostnames for use with multiple clusters. Managed service factory
 * configurations have the znode structure of the form /ddf/[namespace]/[factoryPid]/[pid_UUID].
 */
public class ZookeeperStoragePlugin implements ConfigurationStoragePlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperStoragePlugin.class);

  // For use with the zookeeper client
  private static final Integer DEFAULT_SESSION_TIMEOUT = 10000;

  private static final String CONNECTION_STRING = getConnectionString();

  private static final String NAMESPACE_NAME = getNamespaceName();

  // Can we somehow dynamically get the branding?
  private static final String ROOT_NAME = "ddf";

  private static final ZPath ROOT_PATH = ZPath.from(ROOT_NAME);

  private static final ZPath NAMESPACE_PATH = ZPath.from(ROOT_NAME, NAMESPACE_NAME);

  private final ConfigAdminWatcher watcher;

  private final WrappedKeeper keeper;

  /**
   * Create an instance of {@link ZookeeperStoragePlugin}. Pass the wrapped keeper to the watcher
   * first since ZooKeeper starts async communication immediately, which will trigger watches. Thus
   * an {@link IllegalStateException} would be possible from the watcher if watch messages were
   * received without a reference to its keeper so it can respond appropriately.
   *
   * @param configAdmin reference to the OSGi configuration admin service.
   * @throws IOException if a problem occurs initializing ZooKeeper.
   */
  public ZookeeperStoragePlugin(ConfigurationAdmin configAdmin) throws IOException {
    this.watcher = new ConfigAdminWatcher(configAdmin);
    this.watcher.setKeeper(
        new WrappedKeeper(
            () -> new ZooKeeper(CONNECTION_STRING, DEFAULT_SESSION_TIMEOUT, this.watcher)));

    this.keeper = this.watcher.getKeeper();
  }

  public ZookeeperStoragePlugin(ConfigAdminWatcher watcher, WrappedKeeper keeper) {
    this.watcher = watcher;
    this.watcher.setKeeper(keeper);

    this.keeper = keeper;
  }

  /** Clean up the resources opened by the Zookeeper client libraries. */
  public void destroy() throws InterruptedException {
    keeper.close();
  }

  /**
   * Init method for {@link org.codice.felix.cm.internal.ConfigurationInitializable} which occurs
   * <b>after</b> the service is available to be called.
   *
   * <p>Since configuration can technically be performed during startup, this service must
   * <b>not</b> be allowed to start in an automated fashion (i.e. as part of a boot feature). It is
   * assumed that when starting this feature all startup configuration tasks have been performed
   * otherwise node configuration can fall out of sync. If all default systems have equal default
   * configuration, no synchronization should be necessary out of the box.
   *
   * <p>In other words, when starting this plugin across multiple nodes, it is expected that the
   * given {@link Set<ConfigurationContext> state} is equal, without any discrepencies.
   *
   * <p>It is assumed that a Zookeeper cluster being used for configuring DDF is valid if it has a
   * namespace znode with a child count greater than zero. Corrupted or incorrect namespace znodes
   * that have children should be deleted from Zookeeper manually before starting this plugin.
   *
   * @param state the current configuration state of the system.
   */
  @Override
  public void initialize(Set<ConfigurationContext> state) {
    LOGGER.info("Zookeeper::init");
    Stat rootStat = keeper.exists(ROOT_PATH, false);
    if (rootStat == null) {
      keeper.create(ROOT_PATH);
    }

    Stat namespaceStat = keeper.exists(NAMESPACE_PATH, false);
    if (namespaceStat == null) {
      keeper.create(NAMESPACE_PATH);
    }

    // Initial child watch necessary for tracking brand new znodes or complete deletion of znodes
    List<String> configs = keeper.getChildren(NAMESPACE_PATH, true);

    if (configs.isEmpty()) {
      // First DDF that boots up will initialize ZooKeeper and as a result set Watches
      state.forEach(
          config ->
              createNewNode(
                  config.getServicePid(), config.getFactoryPid(), config.getRawProperties()));
    } else {
      // Subsequent DDFs that boot will read from ZooKeeper to set Watches
      configs.forEach(pathStr -> keeper.getData(ZPath.parse(pathStr), true));
    }
  }

  /** Returns {@code true} if a znode exists for the given pid, or false otherwise. */
  @Override
  public boolean exists(String pid) {
    LOGGER.info("Zookeeper::exists");
    ZPath nodePath = pathForAnyConfig(pid);
    try {
      Stat stat = keeper.exists(nodePath, false);
      return stat != null;
    } catch (UncheckedIOException e) {
      // Cannot throw an IOException
      LOGGER.error("Could not determine if pid exists due to an error: {}", e.getCause());
      return false;
    }
  }

  /** Looks up the configuration for the given pid directly in ZooKeeper. */
  @Override
  public Dictionary load(String pid) throws IOException {
    LOGGER.info("Zookeeper::load");
    ZPath nodePath = pathForAnyConfig(pid);
    try {
      Stat stat = keeper.exists(nodePath, false);
      if (stat == null) {
        throw new IOException(format("Configuration znode does not exist [%s]", nodePath));
      }
      return decodeData(keeper.getData(nodePath, true, stat));
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * This is Config Admin's preferred way to initialize the entire system's configuration but we do
   * not need to watch because those are set when the plugin initializes, and reset by the {@link
   * ConfigAdminWatcher}.
   */
  @Override
  public Enumeration getDictionaries() throws IOException {
    LOGGER.info("Zookeeper::getDictionaries");
    try {
      return Collections.enumeration(
          keeper
              .getChildren(NAMESPACE_PATH, false)
              .stream()
              .flatMap(this::fetchDictionariesFor)
              .collect(Collectors.toList()));
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * There are several cases to consider when storing a configuration in ZooKeeper:
   *
   * <p>The expected znode does not exist yet, so it must be constructed from scratch, taking care
   * to create any necessary parent znodes first.
   *
   * <p>The expected znode does exist and would not actually change as a result of this store
   * operation, so the operation terminates to conserve network traffic.
   *
   * <p>The expected znode does exist and will have its data set to reflect the new configuration.
   */
  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    ZPath nodePath = pathForAnyConfig(pid);
    try {
      Stat stat = keeper.exists(nodePath, true);
      if (stat == null) {
        LOGGER.info("Creating znode for pid {}", pid);
        createNewNode(pid, parseFactoryPid(pid), properties);
        return;
      }
      byte[] encodedProperties = encodeData(properties);
      if (Arrays.equals(encodedProperties, keeper.getData(nodePath, true))) {
        LOGGER.info("Redundant call to store for pid {}", pid);
        return;
      }
      LOGGER.info("Updating znode for pid {}", pid);
      keeper.setData(nodePath, encodedProperties, stat.getVersion());
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /** Deletes the appropriate znode, or does nothing if the znode doesn't exist. */
  @Override
  public void delete(String pid) throws IOException {
    ZPath nodePath = pathForAnyConfig(pid);
    try {
      Stat stat = keeper.exists(nodePath, false);
      if (stat == null) {
        LOGGER.info("Configuration node {} already deleted", nodePath);
        return;
      }
      LOGGER.info("Deleting znode for pid {}", pid);
      keeper.delete(nodePath, stat.getVersion());
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Creates a new configuration node in ZooKeeper, along with the parent node if necessary.
   *
   * @param pid service pid for this configuration.
   * @param factoryPid factory responsible for this configuration, or null if this configuration
   *     belongs to a singleton service.
   * @param props the configuration properties to store in the node.
   */
  private void createNewNode(String pid, @Nullable String factoryPid, Dictionary props) {
    if (factoryPid == null) {
      keeper.create(pathForPid(pid), encodeData(props));
    } else {
      String factoryInstance = parseFactoryInstance(pid);
      // Zookeeper requires the creation of parent nodes before attempting to create child
      // nodes
      keeper.createIfNecessary(pathForPid(factoryPid));
      keeper.create(pathForFactoryInstance(factoryPid, factoryInstance), encodeData(props));
    }
  }

  /**
   * Returns a stream of {@link Dictionary} objects that hold configuration for the given pid. It
   * effectively maps the znode structure back into a flat collection of property dictionaries.
   *
   * @param pidName the entire pid for a managed service, or the factory pid of a managed service
   *     factory.
   * @return a stream of dictionaries that belong to the given pidName.
   */
  private Stream<Dictionary> fetchDictionariesFor(String pidName) {
    ZPath configPath = pathForPid(pidName);
    List<String> children = keeper.getChildren(configPath, true);
    if (children.isEmpty()) {
      // pidName was a managed service with a single dictionary
      return Stream.of(decodeData(keeper.getData(configPath, true)));
    }
    // pidName was a managed service factory with a collection of dictionaries
    return children
        .stream()
        .map(factoryInstance -> pathForFactoryInstance(pidName, factoryInstance))
        .map(path -> keeper.getData(path, true))
        .map(KeeperUtils::decodeData);
  }

  /** Create a ZPath used to access the config with the given local pid in Zookeeper */
  private ZPath pathForAnyConfig(String pid) {
    String factoryPid = parseFactoryPid(pid);
    if (factoryPid == null) {
      return pathForPid(pid);
    }
    String factoryInstance = parseFactoryInstance(pid);
    if (factoryInstance != null) {
      return pathForFactoryInstance(factoryPid, factoryInstance);
    }
    throw new IllegalArgumentException(format("Pid [%s] is invalid", pid));
  }

  /**
   * Duplicated from ConfigurationContextImpl in platform-osgi-configadmin. Pids should be expressed
   * as objects - create a helper library for this if use cases like these continue to occur.
   */
  private static String parseFactoryPid(String pid) {
    if (pid != null && pid.contains("-")) {
      return pid.substring(0, pid.lastIndexOf('.'));
    }
    return null;
  }

  /** Get the uuid for the factory instance for use in mapping znodes */
  private static String parseFactoryInstance(String pid) {
    if (pid != null && pid.contains("-")) {
      return pid.substring(pid.lastIndexOf('.') + 1);
    }
    return null;
  }

  /** Create the ZPath used to access a singleton service's configuration */
  private static ZPath pathForPid(String pid) {
    return ZPath.from(ROOT_NAME, NAMESPACE_NAME, pid);
  }

  /** Create the ZPath used to access a instance of a managed service factory's configuration */
  private static ZPath pathForFactoryInstance(String factoryPid, String factoryInstance) {
    return ZPath.from(ROOT_NAME, NAMESPACE_NAME, factoryPid, factoryInstance);
  }
}
