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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.codice.ddf.admin.zookeeper.KeeperUtils.KeeperSupplier;
import org.codice.ddf.admin.zookeeper.KeeperUtils.ZPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class to support working with Zookeeper. It provides the following benefits:
 *
 * <ul>
 *   <li>Promote safer code by passing {@link ZPath}s instead of strings
 *   <li>Enable streaming by reducing exceptions down to {@link UncheckedIOException}s
 *   <li>One location to handle retry logic and error propagation
 * </ul>
 */
public class WrappedKeeper {
  private static final Logger LOGGER = LoggerFactory.getLogger(WrappedKeeper.class);

  private static final String ERR_MSG_GENERIC = "Error occurred during ZooKeeper operation: ";

  private static final String ERR_MSG_CTOR = "Error creating ZooKeeper client: ";

  private static final int MAX_ATTEMPTS = 3;

  // Lock on the supplier's reference because it is final and will not change
  private final KeeperSupplier zooKeeperSupplier;

  // Must protect with a lock so we do not accidentally access a closed client
  private volatile ZooKeeper zooKeeper;

  public WrappedKeeper(KeeperSupplier zooKeeperSupplier) throws IOException {
    this.zooKeeperSupplier = zooKeeperSupplier;
    this.zooKeeper = zooKeeperSupplier.get();
  }

  public Stat exists(ZPath path, boolean watch) {
    return processReturn(path, watch, (p, w) -> zooKeeper.exists(p.toString(), w));
  }

  public List<String> getChildren(ZPath path, boolean watch) {
    return processReturn(path, watch, (p, w) -> zooKeeper.getChildren(p.toString(), w));
  }

  public byte[] getData(ZPath path, boolean watch) {
    return getData(path, watch, exists(path, watch));
  }

  public byte[] getData(ZPath path, boolean watch, Stat stat) {
    return processReturn(path, watch, stat, (p, w, s) -> zooKeeper.getData(p.toString(), w, s));
  }

  public void createIfNecessary(ZPath path) {
    if (exists(path, false) == null) {
      create(path);
    }
  }

  public void create(ZPath path) {
    create(path, null);
  }

  public void create(ZPath path, byte[] data) {
    processReturn(
        path,
        data,
        (p, d) ->
            zooKeeper.create(p.toString(), d, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
  }

  public void setData(ZPath path, byte[] data, int version) {
    processReturn(path, data, version, (p, d, v) -> zooKeeper.setData(p.toString(), d, v));
  }

  public void delete(ZPath path, int version) {
    processVoid(
        path,
        version,
        (p, v) -> {
          zooKeeper.delete(p.toString(), v);
          return null;
        });
  }

  public void close() throws InterruptedException {
    zooKeeper.close();
  }

  /** Reset the zookeeper client without interrupting anyone */
  @SuppressWarnings({"squid:S2142", "squid:S00108"} /* Exception can be ignored */)
  private static void closeQuietly(ZooKeeper zooKeeper) {
    try {
      zooKeeper.close();
    } catch (InterruptedException ignore) {
    }
  }

  private static ZooKeeper reload(KeeperSupplier supplier) {
    try {
      return supplier.get();
    } catch (IOException e) {
      throw new UncheckedIOException(new IOException(ERR_MSG_CTOR, e));
    }
  }

  /** Marker interface for reducing boilerplate */
  @SuppressWarnings("squid:S2326")
  private interface ZooFunction<T, U, V, R> {}

  /** Special case of a {@link BiFunction} that throws the expected exceptions. */
  @FunctionalInterface
  private interface ZooBiFunction<T, U, R> extends ZooFunction<T, U, Void, R> {
    R apply(T t, U u) throws KeeperException, InterruptedException;
  }

  /** TriFunction contract that throws the expected exceptions. */
  @FunctionalInterface
  private interface ZooTriFunction<T, U, V, R> extends ZooFunction<T, U, V, R> {
    R apply(T t, U u, V v) throws KeeperException, InterruptedException;
  }

  private <T, U, V, R> R processReturn(
      T arg1, U arg2, V arg3, ZooTriFunction<T, U, V, R> function) {
    return process(arg1, arg2, arg3, function, 1);
  }

  private <T, U, R> R processReturn(T arg1, U arg2, ZooBiFunction<T, U, R> function) {
    return process(arg1, arg2, null, function, 1);
  }

  private <T, U> void processVoid(T arg1, U arg2, ZooBiFunction<T, U, Void> function) {
    process(arg1, arg2, null, function, 1);
  }

  private <T, U, V, R> R process(
      T arg1, U arg2, @Nullable V arg3, ZooFunction<T, U, V, R> function, int attempt) {
    boolean interrupted = false;
    try {
      synchronized (zooKeeperSupplier) {
        if (arg3 == null) {
          return ((ZooBiFunction<T, U, R>) function).apply(arg1, arg2);
        }
        return ((ZooTriFunction<T, U, V, R>) function).apply(arg1, arg2, arg3);
      }

    } catch (KeeperException.ConnectionLossException | KeeperException.SessionExpiredException e) {
      verifyAttempt(attempt, e);
      LOGGER.info(
          "Could not communicate with ZooKeeper server, {}, retrying with new client...",
          e.getMessage());
      synchronized (zooKeeperSupplier) {
        closeQuietly(zooKeeper);
        zooKeeper = reload(zooKeeperSupplier);
      }
      return process(arg1, arg2, arg3, function, (attempt + 1));

    } catch (InterruptedException e) {
      interrupted = true;
      verifyAttempt(attempt, e);
      LOGGER.info("Configuration operation interrupted, {}, retrying...", e.getMessage());
      return process(arg1, arg2, arg3, function, (attempt + 1));

    } catch (KeeperException.SessionMovedException | KeeperException.BadVersionException e) {
      verifyAttempt(attempt, e);
      LOGGER.info("Configuration operation could not complete, {}, retrying...", e.getMessage());
      return process(arg1, arg2, arg3, function, (attempt + 1));

    } catch (KeeperException e) {
      throw new UncheckedIOException(new IOException(ERR_MSG_GENERIC, e));

    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void verifyAttempt(int attempt, Throwable cause) {
    if (attempt >= MAX_ATTEMPTS) {
      throw new UncheckedIOException(new IOException(ERR_MSG_GENERIC, cause));
    }
  }
}
