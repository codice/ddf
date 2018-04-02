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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.felix.utils.properties.ConfigurationHandler;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.codice.ddf.admin.zookeeper.KeeperUtils.ZPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of the {@link ConfigAdminWatcher} is to respond to events from the Zookeeper server.
 * It will monitor the async operations that get started when an {@link
 * org.apache.zookeeper.Watcher} receives a configuration event.
 *
 * <p>Due to the stamp lock, we must spin off a config admin update operation in a separate thread.
 * The operation must be tracked so we do not update the Zookeeper cluster itself <b>again</b> with
 * the same configuration change. Consider caching the relevant transaction id's.
 *
 * <p>Note that the zookeeper javadoc says watches are executed on their own thread already.
 */
public class ConfigAdminWatcher implements Watcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAdminWatcher.class);

  private final ConfigurationAdmin configurationAdmin;

  private WrappedKeeper keeper;

  private Set<ZPath> eventCache = Collections.synchronizedSet(new HashSet<>());

  public ConfigAdminWatcher(ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
  }

  WrappedKeeper getKeeper() {
    return keeper;
  }

  void setKeeper(WrappedKeeper keeper) {
    this.keeper = keeper;
  }

  /**
   * Informs this node that a redundant callback is coming.
   *
   * @param path the path that the callback is referring to.
   */
  public void notify(ZPath path) {
    eventCache.add(path);
  }

  /**
   * Process a Zookeeper callback.
   *
   * @param watchedEvent data about the operation this callback is about.
   */
  @Override
  public void process(WatchedEvent watchedEvent) {
    if (keeper == null) {
      throw new IllegalStateException("ConfigAdminWatcher requires a keeper");
    }

    String path = watchedEvent.getPath();
    ZPath zPath = ZPath.parse(path);

    if (eventCache.contains(zPath)) {
      LOGGER.info("Skipping configuration event handler, this node initiated the event");
      eventCache.remove(zPath);
      return;
    }

    Event.EventType type = watchedEvent.getType();
    Event.KeeperState state = watchedEvent.getState();
    String stateName = state.name();

    LOGGER.info("Watched Event: {}, Type: {}, State: {}", path, type, stateName);

    try {
      switch (type) {
        case None:
          LOGGER.info("Sync watch received, current state: {}", stateName);
          break;

        case NodeDataChanged:
          LOGGER.info("Responding to znode update: {}", path);
          processCreateOrUpdate(zPath);
          break;

        case NodeCreated:
          LOGGER.info("Responding to znode creation: {}", path);
          processCreateOrUpdate(zPath);
          break;

        case NodeDeleted:
          LOGGER.info("Responding to znode deletion: {}", path);
          processDelete(zPath);
          break;

        case NodeChildrenChanged:
          LOGGER.info(
              "Configuration collection [{}] changed in size, attempting to reset watch.", path);
          keeper.getChildren(zPath, true);
          break;

        default:
          LOGGER.error("Watched event type not accounted for: {}", type);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Problem responding to ZooKeeper event: ", e);
    }
  }

  private void processCreateOrUpdate(ZPath path) throws IOException {
    String pid = path.getPid();
    if (pid == null
        || pid.equals("org_apache_felix_cm_impl_DynamicBindings")
        || pid.endsWith(".factory")) {
      LOGGER.warn("Refusing to respond to create or update due to invalid pid: {}", pid);
      return;
    }
    Configuration config = configurationAdmin.getConfiguration(pid);
    try (ByteArrayInputStream stream = new ByteArrayInputStream(keeper.getData(path, true))) {
      // Attempt to bind visibility to this bundle by setting a wildcard location
      config.setBundleLocation("?");
      config.update(ConfigurationHandler.read(stream));
    }
  }

  private void processDelete(ZPath path) throws IOException {
    String pid = path.getPid();
    if (pid == null) {
      LOGGER.warn("Refusing to respond to delete due to invalid pid: {}", pid);
      return;
    }
    Configuration config = configurationAdmin.getConfiguration(pid);
    config.delete();
  }
}
