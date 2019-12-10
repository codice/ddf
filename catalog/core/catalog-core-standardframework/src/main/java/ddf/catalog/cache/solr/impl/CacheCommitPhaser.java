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
package ddf.catalog.cache.solr.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;

class CacheCommitPhaser extends Phaser {

  private final ScheduledExecutorService phaseScheduler =
      Executors.newSingleThreadScheduledExecutor(
          StandardThreadFactoryBuilder.newThreadFactory("cacheCommitPhaserThread"));

  private SolrCache cache;

  public CacheCommitPhaser(SolrCache cache) {
    // There will always be at least one party which will be the PhaseAdvancer
    super(1);

    this.cache = cache;

    // PhaseAdvancer blocks waiting for next phase advance, delay 1 second between advances
    // this is used to block queries that request to be indexed before continuing
    // committing Solr more often than 1 second can cause performance issues and exceptions
    phaseScheduler.scheduleWithFixedDelay(new PhaseAdvancer(this), 1, 1, TimeUnit.SECONDS);
  }

  @Override
  protected boolean onAdvance(int phase, int registeredParties) {
    // registeredParties should be 1 since all parties other than the PhaseAdvancer
    // will arriveAndDeregister in the add method
    cache.forceCommit();

    return super.onAdvance(phase, registeredParties);
  }

  /**
   * Adds results to cache and blocks for next phase advance
   *
   * @param results metacards to add to cache
   */
  public void add(List<Result> results) {
    // block next phase
    this.register();
    // add results to cache
    cache.put(getMetacards(results));
    // unblock phase and wait for all other parties to unblock phase
    this.awaitAdvance(this.arriveAndDeregister());
  }

  public void shutdown() {
    this.forceTermination();
    phaseScheduler.shutdown();
  }

  private List<Metacard> getMetacards(List<Result> results) {
    List<Metacard> metacards = new ArrayList<>(results.size());

    for (Result result : results) {
      metacards.add(result.getMetacard());
    }

    return metacards;
  }

  /** Runnable that makes one party arrive to a phaser on each run */
  private static class PhaseAdvancer implements Runnable {

    private final Phaser phaser;

    public PhaseAdvancer(Phaser phaser) {
      this.phaser = phaser;
    }

    @Override
    public void run() {
      phaser.arriveAndAwaitAdvance();
    }
  }
}
