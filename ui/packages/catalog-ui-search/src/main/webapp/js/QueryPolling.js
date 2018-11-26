/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, setInterval, clearInterval*/
const $ = require('jquery')
const _ = require('underscore')
const wreqr = require('./wreqr.js')
const cql = require('./cql.js')
const CQLUtils = require('./CQLUtils.js')
const sources = require('../component/singletons/sources-instance.js')
const moment = require('moment')
require('./jquery.whenAll')

var checkForFailures, startSearch, addFailure, handleReattempts
var pollingQueries = {}
var failedQueries = {}

function getTimeRange(polling) {
  var before = Date.now()
  var after = Date.now() - polling
  return {
    before: moment(before).toISOString(),
    after: moment(after).toISOString(),
  }
}

function buildTimeRangeCQL(timeRange) {
  return {
    type: 'OR',
    filters: [
      {
        type: 'AND',
        filters: [
          {
            property: '"created"',
            type: 'BEFORE',
            value: timeRange.before,
          },
          {
            property: '"created"',
            type: 'AFTER',
            value: timeRange.after,
          },
        ],
      },
      {
        type: 'AND',
        filters: [
          {
            property: '"modified"',
            type: 'BEFORE',
            value: timeRange.before,
          },
          {
            property: '"modified"',
            type: 'AFTER',
            value: timeRange.after,
          },
        ],
      },
      {
        type: 'AND',
        filters: [
          {
            property: '"metacard.created"',
            type: 'BEFORE',
            value: timeRange.before,
          },
          {
            property: '"metacard.created"',
            type: 'AFTER',
            value: timeRange.after,
          },
        ],
      },
      {
        type: 'AND',
        filters: [
          {
            property: '"metacard.modified"',
            type: 'BEFORE',
            value: timeRange.before,
          },
          {
            property: '"metacard.modified"',
            type: 'AFTER',
            value: timeRange.after,
          },
        ],
      },
    ],
  }
}

function buildPollingCQL(cql, timeRange) {
  return {
    type: 'AND',
    filters: [cql, buildTimeRangeCQL(timeRange)],
  }
}

function addTimeRangeToCQLString(originalQuery, timeRange) {
  return CQLUtils.sanitizeGeometryCql(
    '(' +
      cql.write(
        cql.simplify(
          cql.read(
            cql.write(
              buildPollingCQL(
                cql.simplify(cql.read(originalQuery.get('cql'))),
                timeRange
              )
            )
          )
        )
      ) +
      ')'
  )
}

function removeExistingFailures(queryId) {
  delete failedQueries[queryId]
}

function removeImpertinentFailures(queryId) {
  if (failedQueries[queryId]) {
    _.forEach(failedQueries[queryId].failures, function(timeRanges, srcId) {
      if (
        failedQueries[queryId].originalQuery.get('src').indexOf(srcId) === -1
      ) {
        delete failedQueries[queryId].failures[srcId]
      }
    })
  }
}

function removeExistingPolling(queryId) {
  var pollingDetails = pollingQueries[queryId]
  if (pollingDetails) {
    clearInterval(pollingDetails.intervalId)
    delete pollingQueries[queryId]
  }
}

addFailure = function(srcId, originalQuery, timeRange) {
  failedQueries[originalQuery.id] = failedQueries[originalQuery.id] || {
    originalQuery: undefined,
    failures: {},
  }
  failedQueries[originalQuery.id].originalQuery = originalQuery
  failedQueries[originalQuery.id].failures[srcId] =
    failedQueries[originalQuery.id].failures[srcId] || []
  failedQueries[originalQuery.id].failures[srcId].push(timeRange)
}

checkForFailures = function(responses, originalQuery, timeRange) {
  _.forEach(responses, function(response) {
    if (response[1] === 'error' || !response[0].status.successful) {
      var srcId = JSON.parse(response[0].options.data).src
      addFailure(srcId, originalQuery, timeRange)
    }
  })
}

startSearch = function(originalQuery, timeRange, queryToRun) {
  if (!queryToRun) {
    queryToRun = originalQuery.clone()
    queryToRun.set('cql', addTimeRangeToCQLString(originalQuery, timeRange))
  }
  $.whenAll.apply(this, queryToRun.startSearch()).always(function() {
    if (pollingQueries[originalQuery.id]) {
      checkForFailures(arguments, originalQuery, timeRange)
      queryToRun.get('result').mergeNewResults()
      var metacardIds = queryToRun
        .get('result')
        .get('results')
        .map(function(result) {
          return result
            .get('metacard')
            .get('properties')
            .get('id')
        })
      var when = Date.now()
      if (metacardIds.length > 0) {
        wreqr.vent.trigger('alerts:add', {
          queryId: originalQuery.id,
          workspaceId: originalQuery.collection.parents[0].id,
          when: when,
          metacardIds: metacardIds,
        })
      }
    }
  })
}

handleReattempts = function() {
  _.forEach(failedQueries, function(subset) {
    _.forEach(subset.failures, function(timeRanges, srcId) {
      if (sources.get(srcId) && sources.get(srcId).get('available')) {
        var timeRange = {
          after: timeRanges[0].after,
          before: timeRanges[timeRanges.length - 1].before,
        }
        var queryToRun = subset.originalQuery.clone()
        queryToRun.set('federation', 'selected')
        queryToRun.set('src', [srcId])
        queryToRun.set(
          'cql',
          addTimeRangeToCQLString(subset.originalQuery, timeRange)
        )
        delete subset.failures[srcId]
        startSearch(subset.originalQuery, timeRange, queryToRun)
      }
    })
  })
}

sources.on('sync', function() {
  handleReattempts()
})

module.exports = {
  handleAddingQuery: function(query) {
    this.handleRemovingQuery(query)
    query.listenTo(query, 'change:polling', this.handlePollingUpdate.bind(this))
    query.listenTo(query, 'change:src', this.handleSrcUpdate.bind(this))
    query.listenTo(query, 'change:federation', this.handleSrcUpdate.bind(this))
    var polling = query.get('polling')
    if (polling) {
      var intervalId = setInterval(function() {
        var timeRange = getTimeRange(polling)
        startSearch(query, timeRange)
      }, polling)
      pollingQueries[query.id] = {
        intervalId: intervalId,
      }
    }
  },
  handleRemovingQuery: function(query) {
    removeExistingPolling(query.id)
    removeExistingFailures(query.id)
  },
  handlePollingUpdate: function(query) {
    this.handleAddingQuery(query)
  },
  // in the case of a source update we should verify that we don't retry failures pertaining to srcs that don't matter anymore
  handleSrcUpdate: function(query) {
    switch (query.get('federation')) {
      case 'selected':
        removeImpertinentFailures(query.id)
        break
      case 'local':
        removeExistingFailures(query.id)
        break
    }
  },
  getPollingQueries: function() {
    return pollingQueries
  },
}
