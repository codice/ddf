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
const $ = require('jquery')
var Backbone = require('backbone')
var _ = require('underscore')
var metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
var properties = require('../properties.js')
var user = require('../../component/singletons/user-instance.js')
var Common = require('../Common.js')
require('backbone-associations')
var QueryResponseSourceStatus = require('./QueryResponseSourceStatus.js')
var QueryResultCollection = require('./QueryResult.collection.js')
var ResultForm = require('../../component/result-form/result-form.js')

let rpc = null

if (properties.webSocketsEnabled && window.WebSocket) {
  const Client = require('rpc-websockets').Client
  const protocol = { 'http:': 'ws:', 'https:': 'wss:' }
  const url = `${protocol[location.protocol]}//${location.hostname}:${
    location.port
  }${location.pathname}ws`
  rpc = new Client(url)
}

function generateThumbnailUrl(url) {
  var newUrl = url
  if (url.indexOf('?') >= 0) {
    newUrl += '&'
  } else {
    newUrl += '?'
  }
  newUrl += '_=' + Date.now()
  return newUrl
}

function humanizeResourceSize(result) {
  if (result.metacard.properties['resource-size']) {
    result.metacard.properties['resource-size'] = Common.getFileSize(
      result.metacard.properties['resource-size']
    )
  }
}

function handleResultFormFields(result, selectedResultTemplate) {
  if (selectedResultTemplate) {
    let resultAttributes = selectedResultTemplate.descriptors
    if (resultAttributes.length > 0) {
      let newProperties = {}
      newProperties.id = result.metacard.properties.id
      newProperties.title = result.metacard.properties.title
        ? result.metacard.properties.title
        : ''
      newProperties['metacard-type'] =
        result.metacard.properties['metacard-type']
      newProperties['metacard-tags'] =
        result.metacard.properties['metacard-tags']
      newProperties['source-id'] = result.metacard.properties['source-id']
      _.each(resultAttributes, function(value, index) {
        if (result.metacard.properties[value]) {
          newProperties[value] = result.metacard.properties[value]
        }
      })
      result.metacard.properties = newProperties
    }
  }
}

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    queryId: undefined,
    results: [],
    queuedResults: [],
    merged: true,
    currentlyViewed: false,
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'queuedResults',
      collectionType: QueryResultCollection,
    },
    {
      type: Backbone.Many,
      key: 'results',
      collectionType: QueryResultCollection,
    },
    {
      type: Backbone.Many,
      key: 'status',
      relatedModel: QueryResponseSourceStatus,
    },
  ],
  url: './internal/cql',
  useAjaxSync: true,
  initialize: function() {
    this.listenTo(
      this.get('queuedResults'),
      'add change remove reset',
      _.throttle(this.updateMerged, 2500, {
        leading: false,
      })
    )
    this.listenTo(
      this.get('queuedResults'),
      'reset',
      _.throttle(this.mergeQueue, 30, {
        leading: false,
      })
    )
    this.listenTo(this, 'change:currentlyViewed', this.handleCurrentlyViewed)
    this.listenTo(this, 'error', this.handleError)
    this.listenTo(this, 'sync', this.handleSync)
    this.resultCountsBySource = {}
  },
  sync: function(method, model, options) {
    if (rpc !== null) {
      let handled = false
      const promise = rpc
        .call('query', [options.data], options.timeout)
        .then(res => {
          if (!handled) {
            handled = true
            options.success(res)
            return [res, 'success']
          }
        })
        .catch(res => {
          if (!handled) {
            handled = true
            res.options = options
            switch (res.code) {
              case -32000:
                if (rpc !== null) {
                  rpc.close()
                  rpc = null
                }
                options.error({
                  message: 'User not logged in.',
                })
                break
              default:
                // notify user and fallback to http
                if (rpc !== null) {
                  rpc.close()
                  rpc = null
                }
                options.error({
                  responseJSON: {
                    message:
                      'Search failed due to unknown reasons, please try again.',
                  },
                })
            }
            return [res, 'error']
          }
        })
      model.trigger('request', model, null, options)
      return {
        abort() {
          if (!handled) {
            handled = true
            options.error({
              responseJSON: {
                message: 'Stopped',
              },
            })
          }
        },
        promise() {
          const d = $.Deferred()
          promise
            .then(value => {
              d.resolve(value)
            })
            .catch(err => {
              d.reject(err)
            })
          return d
        },
      }
    } else {
      return Backbone.AssociatedModel.prototype.sync.call(
        this,
        method,
        model,
        options
      )
    }
  },
  handleError: function(resultModel, response, sent) {
    var dataJSON = JSON.parse(sent.data)
    this.updateMessages(
      response.responseJSON
        ? response.responseJSON.message
        : response.statusText,
      dataJSON.src
    )
  },
  handleSync: function(resultModel, response, sent) {
    this.updateStatus()
    if (sent) {
      var dataJSON = JSON.parse(sent.data)
      this.updateMessages(
        response.status.messages,
        dataJSON.src,
        response.status
      )
    }
  },
  parse: function(resp, options) {
    metacardDefinitions.addMetacardDefinitions(resp.types)
    if (resp.results) {
      var queryId = this.getQueryId()
      var selectedResultTemplate = ResultForm.getResultCollection().filteredList.filter(
        form =>
          form.id === this.get('selectedResultTemplate') ||
          form.value === this.get('selectedResultTemplate')
      )[0]
      var color = this.getColor()
      _.forEach(resp.results, function(result) {
        result.propertyTypes =
          resp.types[result.metacard.properties['metacard-type']]
        result.metacardType = result.metacard.properties['metacard-type']
        result.metacard.id = result.metacard.properties.id
        if (resp.status.id !== 'cache') {
          result.uncached = true
        }
        result.id = result.metacard.id + result.metacard.properties['source-id']
        result.metacard.queryId = queryId
        result.metacard.color = color
        humanizeResourceSize(result)
        handleResultFormFields(result, selectedResultTemplate)
        result.actions.forEach(action => (action.queryId = queryId))

        var thumbnailAction = _.findWhere(result.actions, {
          id: 'catalog.data.metacard.thumbnail',
        })
        if (result.hasThumbnail && thumbnailAction) {
          result.metacard.properties.thumbnail = generateThumbnailUrl(
            thumbnailAction.url
          )
        }
        result.src = resp.status.id // store the name of the source that this result came from
      })

      if (this.allowAutoMerge()) {
        this.lastMerge = Date.now()
        options.resort = true
      }
    }

    if (_.isEmpty(this.resultCountsBySource)) {
      var metacardIdToSourcesIndex = this.createIndexOfMetacardToSources(
        resp.results
      )
      this.updateResultCountsBySource(
        this.createIndexOfSourceToResultCount(
          metacardIdToSourcesIndex,
          resp.results
        )
      )
    }

    this.addQueuedResults(resp.results)

    if (this.get('queuedResults').fullCollection.length !== 0) {
      // merges the remaining queued results not from the cache
      this.mergeQueue(true, false)
    }

    return {
      queuedResults: [],
      results: [],
      status: resp.status,
      merged: this.get('merged') === false ? false : resp.results.length === 0,
    }
  },
  // we have to do a reset because adding is so slow that it will cause a partial merge to initiate
  addQueuedResults(results) {
    const existingQueue = this.get('queuedResults').fullCollection.models
    this.get('queuedResults').fullCollection.reset(
      existingQueue.concat(results)
    )
  },
  allowAutoMerge: function() {
    if (this.get('results').length === 0 || !this.get('currentlyViewed')) {
      return true
    } else {
      return Date.now() - this.lastMerge < properties.getAutoMergeTime()
    }
  },
  mergeQueue: function(userTriggered, includeQueuedCache = true) {
    if (userTriggered === true || this.allowAutoMerge()) {
      this.lastMerge = Date.now()

      var resultsIncludingDuplicates = this.get('results')
        .fullCollection.map(function(m) {
          return m.pick('id', 'src')
        })
        .concat(
          this.get('queuedResults').fullCollection.map(function(m) {
            return m.pick('id', 'src')
          })
        )
      var metacardIdToSourcesIndex = this.createIndexOfMetacardToSources(
        resultsIncludingDuplicates
      )

      var interimCollection = new QueryResultCollection(
        this.get('results').fullCollection.models
      )

      const queuedResults = this.get('queuedResults').fullCollection.models
      const resultsToAdd = includeQueuedCache
        ? queuedResults
        : queuedResults.filter(result => result.get('src') !== 'cache')
      interimCollection.add(resultsToAdd, {
        merge: true,
      })
      interimCollection.fullCollection.comparator = this.get(
        'results'
      ).fullCollection.comparator
      interimCollection.fullCollection.sort()
      var maxResults = user
        .get('user')
        .get('preferences')
        .get('resultCount')
      this.get('results').fullCollection.reset(
        interimCollection.fullCollection.slice(0, maxResults)
      )

      this.updateResultCountsBySource(
        this.createIndexOfSourceToResultCount(
          metacardIdToSourcesIndex,
          this.get('results').fullCollection
        )
      )

      this.get('queuedResults').fullCollection.reset()
      this.updateStatus()
      this.set('merged', true)
    }
  },
  updateResultCountsBySource(resultCounts) {
    for (var src in resultCounts) {
      this.resultCountsBySource[src] = Math.max(
        this.resultCountsBySource[src] || 0,
        resultCounts[src]
      )
    }
  },
  getSourceList() {
    return Object.keys(this.resultCountsBySource)
  },
  getLastResultCountForSource(src) {
    return this.resultCountsBySource[src] || 0
  },
  resetResultCountsBySource() {
    this.resultCountsBySource = {}
  },
  // create an index of metacard id -> list of sources it appears in
  createIndexOfMetacardToSources: function(models) {
    return models.reduce(function(index, metacard) {
      index[metacard.id] = index[metacard.id] || []
      index[metacard.id].push(metacard.src)
      return index
    }, {})
  },
  // create an index of source -> last number of results from server
  createIndexOfSourceToResultCount: function(metacardIdToSourcesIndex, models) {
    return models.reduce(function(index, metacard) {
      var sourcesForMetacard = metacardIdToSourcesIndex[metacard.id]
      sourcesForMetacard.forEach(src => {
        index[src] = index[src] || 0
        index[src]++
      })
      return index
    }, {})
  },
  cacheHasReturned: function() {
    return this.get('status')
      .filter(function(statusModel) {
        return statusModel.id === 'cache'
      })
      .reduce(function(hasReturned, statusModel) {
        return statusModel.get('successful') !== undefined
      }, false)
  },
  setCacheChecked: function() {
    if (this.cacheHasReturned()) {
      this.get('status').forEach(
        function(statusModel) {
          statusModel.setCacheHasReturned()
        }.bind(this)
      )
    }
  },
  updateMessages: function(message, id, status) {
    this.get('status').forEach(
      function(statusModel) {
        statusModel.updateMessages(message, id, status)
      }.bind(this)
    )
  },
  updateStatus: function() {
    this.setCacheChecked()
    this.get('status').forEach(
      function(statusModel) {
        statusModel.updateStatus(this.get('results').fullCollection)
      }.bind(this)
    )
  },
  updateMerged: function() {
    this.set('merged', this.get('queuedResults').fullCollection.length === 0)
  },
  isUnmerged: function() {
    return !this.get('merged')
  },
  mergeNewResults: function() {
    this.mergeQueue(true)
    this.trigger('sync')
  },
  handleCurrentlyViewed: function() {
    if (!this.get('currentlyViewed') && !this.get('merged')) {
      this.mergeNewResults()
    }
  },
  isSearching: function() {
    return this.get('status').some(function(status) {
      return status.get('successful') === undefined
    })
  },
  setQueryId: function(queryId) {
    this.set('queryId', queryId)
  },
  setColor: function(color) {
    this.set('color', color)
  },
  getQueryId: function() {
    return this.get('queryId')
  },
  getColor: function() {
    return this.get('color')
  },
  cancel: function() {
    this.unsubscribe()
    if (this.has('status')) {
      var statuses = this.get('status')
      statuses.forEach(function(status) {
        if (status.get('state') === 'ACTIVE') {
          status.set({
            canceled: true,
          })
        }
      })
    }
  },
})
