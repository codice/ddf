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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Testbed {
  private static final String DEFAULT_CONNECT_STRING = "localhost:2181";

  private static final Integer DEFAULT_SESSION_TIMEOUT = 10000;

  public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
    ZooKeeper localZookeeper =
        new ZooKeeper(
            DEFAULT_CONNECT_STRING, DEFAULT_SESSION_TIMEOUT, new ConfigAdminWatcher(null));

    localZookeeper.getChildren("/", true).forEach(System.out::println);
    //    localZookeeper.exists("/main", true);

    localZookeeper.create("/main", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    localZookeeper.exists("/main", true);

    localZookeeper.getChildren("/test", true).forEach(System.out::println);
    Stat child3 = localZookeeper.exists("/test/child3", false);
    if (child3 == null) {
      throw new IllegalStateException("Child3 is null");
    }

    List<Op> operations = new ArrayList<>();

    int version = child3.getVersion();
    String nextData = format("child3::%s", UUID.randomUUID().toString());

    //    System.out.println("Version: " + version);
    //    System.out.println("Next Data: " + nextData);

    operations.add(Op.delete("/test/child3", version));
    operations.add(
        Op.create(
            "/test/child3",
            nextData.getBytes(),
            ZooDefs.Ids.OPEN_ACL_UNSAFE,
            CreateMode.PERSISTENT));

    //        localZookeeper.delete("/test/child3", version);
    //    localZookeeper.create("/test/child3", nextData.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
    //            CreateMode.PERSISTENT);
    //    localZookeeper.multi(operations).forEach(ZookeeperPlugin::printResult);

    TimeUnit.SECONDS.sleep(40);

    localZookeeper.close();
  }
}
