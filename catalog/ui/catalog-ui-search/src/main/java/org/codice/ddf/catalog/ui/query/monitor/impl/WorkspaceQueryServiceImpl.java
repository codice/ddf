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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.google.common.collect.Lists;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.impl.QueryMetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceQueryService;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceService;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

  public static final String JOB_IDENTITY = "WorkspaceQueryServiceJob";

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceQueryServiceImpl.class);

  private static final String UNKNOWN_SOURCE = "unknown";

  private static final String TRIGGER_NAME = "WorkspaceQueryTrigger";

  private static final Security SECURITY = Security.getInstance();

  private final QueryUpdateSubscriber queryUpdateSubscriber;

  private final WorkspaceService workspaceService;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  @SuppressWarnings("FieldCanBeLocal")
  private Scheduler scheduler;

  private SecurityService securityService;

  private FilterService filterService;

  private long queryTimeoutMinutes;

  private Integer queryTimeInterval;

  private JobDetail jobDetail;

  private Subject subject;

  /**
   * @param queryUpdateSubscriber must be non-null
   * @param workspaceService must be non-null
   * @param catalogFramework must be non-null
   * @param filterBuilder must be non-null
   * @param schedulerSupplier must be non-null
   * @param securityService must be non-null
   * @param filterService must be non-null
   */
  public WorkspaceQueryServiceImpl(
      QueryUpdateSubscriber queryUpdateSubscriber,
      WorkspaceService workspaceService,
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      Supplier<Optional<Scheduler>> schedulerSupplier,
      SecurityService securityService,
      FilterService filterService)
      throws SchedulerException {

    notNull(queryUpdateSubscriber, "queryUpdateSubscriber must be non-null");
    notNull(workspaceService, "workspaceService must be non-null");
    notNull(catalogFramework, "catalogFramework must be non-null");
    notNull(filterBuilder, "filterBuilder must be non-null");
    notNull(schedulerSupplier, "scheduleSupplier must be non-null");
    notNull(securityService, "securityService must be non-null");
    notNull(filterService, "filterService must be non-null");

    this.queryUpdateSubscriber = queryUpdateSubscriber;
    this.workspaceService = workspaceService;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.securityService = securityService;
    this.filterService = filterService;

    Optional<Scheduler> schedulerOptional = schedulerSupplier.get();

    if (schedulerOptional.isPresent()) {
      scheduler = schedulerOptional.get();
      scheduler.getContext().put(JOB_IDENTITY, this);
      jobDetail = newJob(QueryJob.class).withIdentity(JOB_IDENTITY).build();
      scheduler.start();
    } else {
      LOGGER.warn("unable to get a quartz scheduler object, email notifications will not run");
    }
  }

  public Integer getQueryTimeInterval() {
    return this.queryTimeInterval;
  }

  public void setQueryTimeInterval(Integer queryTimeInterval) {
    notNull(queryTimeInterval, "queryTimeInterval must be non-null");
    if (queryTimeInterval > 0 && queryTimeInterval <= 1440) {
      LOGGER.debug("Setting query time interval : {}", queryTimeInterval);
      this.queryTimeInterval = queryTimeInterval;
    } else if (this.queryTimeInterval == null) {
      this.queryTimeInterval = 1440;
    }
  }

  /** @param cronString cron string (must be non-null) */
  @SuppressWarnings("unused")
  public void setCronString(String cronString) {
    notNull(cronString, "cronString must be non-null");
    notNull(scheduler, "scheduler must be non-null");
    notNull(jobDetail, "jobDetail must be non-null");

    try {
      scheduler.deleteJob(jobDetail.getKey());
      LOGGER.debug("Scheduling job {}", jobDetail);
      CronTrigger trigger =
          newTrigger()
              .withIdentity(TRIGGER_NAME)
              .startNow()
              .withSchedule(cronSchedule(cronString))
              .build();
      scheduler.scheduleJob(jobDetail, trigger);
      LOGGER.debug("Setting cron string : {}", cronString);
    } catch (SchedulerException e) {
      LOGGER.warn("Unable to update scheduler with cron string: cron=[{}]", cronString, e);
    }
  }

  /** @param queryTimeoutMinutes minutes (must be non-null) */
  @SuppressWarnings("unused")
  public void setQueryTimeoutMinutes(Long queryTimeoutMinutes) {
    notNull(queryTimeoutMinutes, "queryTimeoutMinutes must be non-null");
    LOGGER.debug("Setting queryTimeOutMinutes : {}", queryTimeoutMinutes);
    this.queryTimeoutMinutes = queryTimeoutMinutes;
  }

  public void setSubject(Subject subject) {
    this.subject = subject;
  }

  public void destroy() {
    LOGGER.trace("Shutting down");
    try {
      scheduler.shutdown();
    } catch (SchedulerException e) {
      LOGGER.warn("Unable to shut down scheduler", e);
    }
  }

  /** Main entry point, should be called by a scheduler. */
  public void run() {
    SECURITY.runAsAdmin(
        () -> {
          Subject runSubject = subject != null ? subject : SECURITY.getSystemSubject();

          return runSubject.execute(
              () -> {
                LOGGER.trace("running workspace query service");

                Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> queryMetacards =
                    workspaceService.getQueryMetacards();

                LOGGER.debug("queryMetacards: size={}", queryMetacards.size());

                List<WorkspaceTask> workspaceTasks = createWorkspaceTasks(queryMetacards);

                LOGGER.debug("workspaceTasks: size={}", workspaceTasks.size());

                Map<String, Pair<WorkspaceMetacardImpl, Long>> results =
                    executeWorkspaceTasks(workspaceTasks, queryTimeoutMinutes, TimeUnit.MINUTES);

                LOGGER.debug("results: {}", results);

                queryUpdateSubscriber.notify(results);

                return null;
              });
        });
  }

  private Map<String, Pair<WorkspaceMetacardImpl, Long>> executeWorkspaceTasks(
      List<WorkspaceTask> workspaceTasks, long timeout, TimeUnit timeoutUnit) {
    Map<String, Pair<WorkspaceMetacardImpl, Long>> results = new ConcurrentHashMap<>();

    workspaceTasks
        .stream()
        .map(ForkJoinPool.commonPool()::submit)
        .map(task -> getTaskResult(task, timeout, timeoutUnit))
        .filter(Objects::nonNull)
        .forEach(
            pair ->
                results.put(
                    pair.getLeft().getId(), new ImmutablePair<>(pair.getLeft(), pair.getRight())));

    return results;
  }

  private Pair<WorkspaceMetacardImpl, Long> getTaskResult(
      ForkJoinTask<Pair<WorkspaceMetacardImpl, Long>> workspaceTask,
      long timeout,
      TimeUnit timeoutUnit) {
    try {
      return workspaceTask.get(timeout, timeoutUnit);
    } catch (TimeoutException e) {
      LOGGER.warn("Timeout", e);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.warn("ForkJoinPool error", e);
    }
    return null;
  }

  private List<WorkspaceTask> createWorkspaceTasks(
      Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> queryMetacards) {
    List<WorkspaceTask> workspaceTasks = new ArrayList<>();

    for (Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>> workspaceQueryPair :
        queryMetacards.values()) {
      Map<String, List<QueryMetacardImpl>> queryMetacardsGroupedBySource =
          groupBySource(workspaceQueryPair.getRight());
      List<QueryRequest> queryRequests =
          getQueryRequests(queryMetacardsGroupedBySource.values().stream());
      if (!queryRequests.isEmpty()) {
        workspaceTasks.add(new WorkspaceTask(workspaceQueryPair.getLeft(), queryRequests));
      }
    }

    return workspaceTasks;
  }

  private Map<String, List<QueryMetacardImpl>> groupBySource(
      List<QueryMetacardImpl> queryMetacards) {
    final Map<String, List<QueryMetacardImpl>> groupedBySource = new HashMap<>();
    for (QueryMetacardImpl queryMetacard : queryMetacards) {
      List<String> sources = queryMetacard.getSources();
      if (!sources.isEmpty()) {
        sources.forEach(sourceId -> groupedBySource.compute(sourceId, addToList(queryMetacard)));
      } else {
        groupedBySource.compute(UNKNOWN_SOURCE, addToList(queryMetacard));
      }
    }
    return groupedBySource;
  }

  private BiFunction<String, List<QueryMetacardImpl>, List<QueryMetacardImpl>> addToList(
      QueryMetacardImpl queryMetacard) {
    return (id, queries) -> {
      if (queries == null) {
        return Lists.newArrayList(queryMetacard);
      } else {
        queries.add(queryMetacard);
        return queries;
      }
    };
  }

  private List<QueryRequest> getQueryRequests(
      Stream<List<QueryMetacardImpl>> queriesGroupedBySource) {
    final Filter modifiedFilter = filterService.getModifiedDateFilter(calculateQueryTimeInterval());
    return queriesGroupedBySource
        .map(this::queryMetacardsToFilters)
        .map(filterBuilder::anyOf)
        .map(filter -> filterBuilder.allOf(modifiedFilter, filter))
        .map(this::filterToQuery)
        .map(this::queryToQueryRequest)
        .collect(Collectors.toList());
  }

  private List<Filter> queryMetacardsToFilters(List<QueryMetacardImpl> queriesForSource) {
    return queriesForSource
        .stream()
        .map(this::metacardToFilter)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private QueryRequestImpl queryToQueryRequest(QueryImpl query) {
    final Map<String, Serializable> properties = securityService.addSystemSubject(new HashMap<>());
    return new QueryRequestImpl(query, properties);
  }

  private QueryImpl filterToQuery(And filter) {
    final QueryImpl query = new QueryImpl(filter);
    query.setRequestsTotalResultsCount(true);
    return query;
  }

  private Filter metacardToFilter(QueryMetacardImpl queryMetacard) {
    try {
      return ECQL.toFilter(queryMetacard.getCql());
    } catch (CQLException e) {
      LOGGER.warn("Error parsing CQL", e);
      return null;
    }
  }

  private Date calculateQueryTimeInterval() {
    return Date.from(Instant.now().minus(queryTimeInterval, ChronoUnit.MINUTES));
  }

  private class QueryTask extends RecursiveTask<Long> {
    private final QueryRequest queryRequest;

    private QueryTask(QueryRequest queryRequest) {
      this.queryRequest = queryRequest;
    }

    @Override
    protected Long compute() {
      try {
        final QueryResponse response = catalogFramework.query(queryRequest);
        return response.getHits();
      } catch (UnsupportedQueryException | FederationException | SourceUnavailableException e) {
        LOGGER.warn("Query error", e);
        return 0L;
      }
    }
  }

  private class WorkspaceTask extends RecursiveTask<Pair<WorkspaceMetacardImpl, Long>> {
    private final WorkspaceMetacardImpl workspaceMetacard;

    private final List<QueryRequest> queryRequests;

    private WorkspaceTask(
        WorkspaceMetacardImpl workspaceMetacard, List<QueryRequest> queryRequests) {
      notNull(workspaceMetacard, "WorkspaceMetacardImpl must be non-null");
      notNull(queryRequests, "queryRequests must be non-null");
      notEmpty(queryRequests, "queryRequests must be non-empty");
      this.workspaceMetacard = workspaceMetacard;
      this.queryRequests = queryRequests;
    }

    @Override
    protected Pair<WorkspaceMetacardImpl, Long> compute() {
      final long result =
          queryRequests
              .stream()
              .map(QueryTask::new)
              .map(QueryTask::fork)
              .mapToLong(ForkJoinTask::join)
              .sum();

      return Pair.of(workspaceMetacard, result);
    }
  }
}
