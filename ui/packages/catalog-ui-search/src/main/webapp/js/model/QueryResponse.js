/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
const $ = require('jquery')
const Backbone = require('backbone')
const _ = require('underscore')
const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const properties = require('../properties.js')
const user = require('../../component/singletons/user-instance.js')
const Common = require('../Common.js')
require('backbone-associations')
const QueryResponseSourceStatus = require('./QueryResponseSourceStatus.js')
const QueryResultCollection = require('./QueryResult.collection.js')
const QueryResult = require('./QueryResult.js')
const spliceAmount = 5
import { LazyQueryResult } from './LazyQueryResult'

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
  let newUrl = url
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

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    queryId: undefined,
    results: [],
    queuedResults: [],
    merged: true,
    currentlyViewed: false,
    showingResultsForFields: [],
    didYouMeanFields: [],
    userSpellcheckIsOn: false,
    processingResults: false,
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
  initialize() {
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
    this.lazyResults = []
    this.queuedResults = []
    this.processedResults = []
  },
  sync(method, model, options) {
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
              case 400:
              case 404:
              case 500:
                options.error({
                  responseJSON: res,
                })
                break
              case -32000:
                if (rpc !== null) {
                  rpc.close()
                  rpc = null
                }
                options.error({
                  responseJSON: {
                    message: 'User not logged in.',
                  },
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
  handleError(resultModel, response, sent) {
    const dataJSON = JSON.parse(sent.data)
    this.updateMessages(
      response.responseJSON
        ? response.responseJSON.message
        : response.statusText,
      dataJSON.src
    )
  },
  handleSync(resultModel, response, sent) {
    this.updateStatus()
    if (sent) {
      const dataJSON = JSON.parse(sent.data)
      if (response.status) {
        this.updateMessages(
          response.status.messages,
          dataJSON.src,
          response.status
        )
      }
    }
  },
  parse(resp, options) {
    metacardDefinitions.addMetacardDefinitions(resp.types)
    if (resp.results) {
      const queryId = this.getQueryId()
      const color = this.getColor()
      _.forEach(resp.results, result => {
        result.propertyTypes =
          resp.types[result.metacard.properties['metacard-type']]
        result.metacardType = result.metacard.properties['metacard-type']
        result.metacard.id = result.metacard.properties.id
        if (resp.statusBySource.cache === undefined) {
          result.uncached = true
        }
        result.id = result.metacard.id + result.metacard.properties['source-id']
        result.metacard.queryId = queryId
        result.metacard.color = color
        humanizeResourceSize(result)
        result.actions.forEach(action => (action.queryId = queryId))

        const thumbnailAction = _.findWhere(result.actions, {
          id: 'catalog.data.metacard.thumbnail',
        })
        if (result.hasThumbnail && thumbnailAction) {
          result.metacard.properties.thumbnail = generateThumbnailUrl(
            thumbnailAction.url
          )
        }

        result.src = result.metacard.properties['source-id']
        // Andrew, what do we do here?
        // result.src = resp.statusBySource.id // store the name of the source that this result came from
      })

      if (this.allowAutoMerge()) {
        this.lastMerge = Date.now()
        // options.resort = true
      }
    }

    if (_.isEmpty(this.resultCountsBySource)) {
      const metacardIdToSourcesIndex = this.createIndexOfMetacardToSources(
        resp.results
      )
      this.updateResultCountsBySource(
        this.createIndexOfSourceToResultCount(
          metacardIdToSourcesIndex,
          resp.results
        )
      )
    }

    this.addToQueue(resp.results)

    const status = Object.keys(resp.statusBySource).map(id => {
      return {
        ...resp.statusBySource[id],
      }
    })

    return {
      showingResultsForFields: resp.showingResultsForFields,
      didYouMeanFields: resp.didYouMeanFields,
      userSpellcheckIsOn: resp.userSpellcheckIsOn,
      queuedResults: [],
      results: [],
      status,
      merged: this.get('merged') === false ? false : resp.results.length === 0,
    }
  },
  processQueue() {
    if (this.processQueueTimeoutId !== undefined) {
      return
    }

    if (this.queuedResults.length === 0 && this.processedResults.length > 0) {
      // stop and update actual models
      this.get('queuedResults').reset(this.processedResults)
      this.set('processingResults', false)
      this.trigger('sync')
    } else if (this.queuedResults.length !== 0) {
      if (this.get('processingResults') === false) {
        this.set('processingResults', true)
      }
      // make some models and add to processed queue
      const slice = this.queuedResults.splice(
        0,
        window.spliceAmount | spliceAmount
      )
      this.processedResults = this.processedResults.concat(
        slice.map(plainResult => {
          const objRef = plainResult._objRef
          delete plainResult._objRef // prevent a circular ref
          const backboneVersion = new QueryResult(plainResult)
          objRef.setBackbone(backboneVersion)
          return backboneVersion
        })
      )
      // sorry for this awful attempt to keep the page churning
      this.processQueueTimeoutId = window.requestAnimationFrame(() => {
        this.processQueueTimeoutId = window.requestAnimationFrame(() => {
          this.processQueueTimeoutId = undefined
          this.processQueue()
        })
      })
    }
  },
  emptyQueue() {
    clearTimeout(this.processQueueTimeoutId)
    cancelAnimationFrame(this.processQueueTimeoutId)
    this.processQueueTimeoutId = undefined
    this.set('processingResults', false)
    this.trigger('sync')
    this.lazyResults = []
    this.queuedResults = []
    this.processedResults = []
  },
  addToQueue(results) {
    /**
     * dedupe results ahead of time (only really due to the cache)
     * 250 results at 6x slowdown took 7mx
     * 2000 results at a 4x slowdown took 207 ms
     * 2000 results at 6x slowodown took 353 ms
     *
     * So this could end up being significant, maybe worth chunking this dedupe up as well to save some time (or webworker it)
     */
    const resultsDeduped = results.filter(result => {
      return (
        this.queuedResults.find(
          queuedResult => queuedResult.id === result.id
        ) === undefined
      )
    })

    // const now = Date.now()
    this.lazyResults = this.lazyResults.concat(
      resultsDeduped.map(result => {
        const objRef = new LazyQueryResult(result)
        result._objRef = objRef // so we can later quickly add the backbone version
        return objRef
      })
    )
    // console.log(`finished ${resultsDeduped.length}: ${Date.now() - now}`)
    this.queuedResults = this.queuedResults.concat(resultsDeduped)
    this.processQueue()
  },
  // we have to do a reset because adding is so slow that it will cause a partial merge to initiate
  addQueuedResults(results) {
    const existingQueue = this.get('queuedResults').models
    this.get('queuedResults').reset(existingQueue.concat(results))
  },
  allowAutoMerge() {
    if (this.get('results').length === 0 || !this.get('currentlyViewed')) {
      return true
    } else {
      return Date.now() - this.lastMerge < properties.getAutoMergeTime()
    }
  },
  mergeQueue(userTriggered) {
    if (
      userTriggered === true ||
      (this.allowAutoMerge() && this.get('queuedResults').length > 0)
    ) {
      this.lastMerge = Date.now()

      const resultsIncludingDuplicates = this.get('results')
        .map(m => m.pick('id', 'src'))
        .concat(this.get('queuedResults').map(m => m.pick('id', 'src')))
      const metacardIdToSourcesIndex = this.createIndexOfMetacardToSources(
        resultsIncludingDuplicates
      )

      const interimCollection = new QueryResultCollection(
        this.get('results').models
      )

      interimCollection.add(this.get('queuedResults').models, {
        merge: true,
      })
      interimCollection.comparator = this.get('results').comparator
      // interimCollection.sort()
      const maxResults = user
        .get('user')
        .get('preferences')
        .get('resultCount')
      // this.get('results').reset(interimCollection.slice(0, maxResults))
      this.get('results').reset(interimCollection.slice(0))

      this.updateResultCountsBySource(
        this.createIndexOfSourceToResultCount(
          metacardIdToSourcesIndex,
          this.get('results')
        )
      )

      this.get('queuedResults').reset()
      this.updateStatus()
      this.set('merged', true)
    }
  },
  updateResultCountsBySource(resultCounts) {
    for (const src in resultCounts) {
      this.resultCountsBySource[src] = resultCounts[src]
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
  createIndexOfMetacardToSources(models) {
    return models.reduce((index, metacard) => {
      index[metacard.id] = index[metacard.id] || []
      index[metacard.id].push(metacard.src)
      return index
    }, {})
  },
  // create an index of source -> last number of results from server
  createIndexOfSourceToResultCount(metacardIdToSourcesIndex, models) {
    return models.reduce((index, metacard) => {
      const sourcesForMetacard = metacardIdToSourcesIndex[metacard.id]
      sourcesForMetacard.forEach(src => {
        index[src] = index[src] || 0
        index[src]++
      })
      return index
    }, {})
  },
  cacheHasReturned() {
    return this.get('status')
      .filter(statusModel => statusModel.id === 'cache')
      .reduce(
        (hasReturned, statusModel) =>
          statusModel.get('successful') !== undefined,
        false
      )
  },
  setCacheChecked() {
    if (this.cacheHasReturned()) {
      this.get('status').forEach(statusModel => {
        statusModel.setCacheHasReturned()
      })
    }
  },
  updateMessages(message, id, status) {
    this.get('status').forEach(statusModel => {
      statusModel.updateMessages(message, id, status)
    })
  },
  updateStatus() {
    this.setCacheChecked()
    this.get('status').forEach(statusModel => {
      statusModel.updateStatus(this.get('results'))
    })
  },
  updateMerged() {
    this.set('merged', this.get('queuedResults').length === 0)
  },
  isUnmerged() {
    return !this.get('merged') && this.get('queuedResults').length > 0
  },
  mergeNewResults() {
    this.mergeQueue(true)
    this.trigger('sync')
  },
  handleCurrentlyViewed() {
    if (!this.get('currentlyViewed') && !this.get('merged')) {
      this.mergeNewResults()
    }
  },
  isJustSearching() {
    return this.get('status').some(
      status => status.get('successful') === undefined
    )
  },
  isSearching() {
    return this.isJustSearching() || this.get('processingResults')
  },
  setQueryId(queryId) {
    this.set('queryId', queryId)
  },
  setColor(color) {
    this.set('color', color)
  },
  getQueryId() {
    return this.get('queryId')
  },
  getColor() {
    return this.get('color')
  },
  cancel() {
    this.unsubscribe()
    if (this.has('status')) {
      const statuses = this.get('status')
      statuses.forEach(status => {
        if (status.get('state') === 'ACTIVE') {
          status.set({
            canceled: true,
          })
        }
      })
    }
  },
})
