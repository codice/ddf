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

const Backbone = require('backbone')
const $ = require('jquery')
const _ = require('underscore')
const properties = require('../properties.js')
const QueryResponse = require('./QueryResponse.js')
const ResultSort = require('./ResultSort.js')
const Sources = require('../../component/singletons/sources-instance.js')
const Common = require('../Common.js')
const CacheSourceSelector = require('../CacheSourceSelector.js')
const announcement = require('../../component/announcement/index.jsx')
const CQLUtils = require('../CQLUtils.js')
const user = require('../../component/singletons/user-instance.js')
const _merge = require('lodash/merge')
require('backbone-associations')
import PartialAssociatedModel from '../../js/extensions/backbone.partialAssociatedModel'
const plugin = require('plugins/query')

var Query = {}

function limitToDeleted(cqlString) {
  return CQLUtils.transformFilterToCQL({
    type: 'AND',
    filters: [
      CQLUtils.transformCQLToFilter(cqlString),
      {
        property: '"metacard-tags"',
        type: 'ILIKE',
        value: 'deleted',
      },
    ],
  })
}

function limitToHistoric(cqlString) {
  return CQLUtils.transformFilterToCQL({
    type: 'AND',
    filters: [
      CQLUtils.transformCQLToFilter(cqlString),
      {
        property: '"metacard-tags"',
        type: 'ILIKE',
        value: 'revision',
      },
    ],
  })
}

const handleTieredSearchLocalFinish = function(ids) {
  const results = this.get('result')
    .get('results')
    .toJSON()

  const status = this.get('result')
    .get('status')
    .toJSON()

  const resultIds = results.map(result => result.metacard.id)
  const missingResult = ids.some(id => !resultIds.includes(id))
  if (!missingResult) {
    return
  }
  this.set('federation', 'enterprise')
  this.startSearch({ results, status })
}

Query.Model = PartialAssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'result',
      relatedModel: QueryResponse,
      isTransient: true,
    },
  ],
  set: function(data, ...args) {
    if (
      typeof data === 'object' &&
      data.filterTree !== undefined &&
      typeof data.filterTree === 'string'
    ) {
      // for backwards compatability
      try {
        data.filterTree = JSON.parse(data.filterTree)
      } catch (e) {
        data.filterTree = CQLUtils.transformCQLToFilter(data.cql)
      }
    }
    return PartialAssociatedModel.prototype.set.call(this, data, ...args)
  },
  toJSON: function(...args) {
    const json = PartialAssociatedModel.prototype.toJSON.call(this, ...args)
    if (typeof json.filterTree === 'object') {
      json.filterTree = JSON.stringify(json.filterTree)
    }
    return json
  },
  //in the search we are checking for whether or not the model
  //only contains 5 items to know if we can search or not
  //as soon as the model contains more than 5 items, we assume
  //that we have enough values to search
  defaults: function() {
    return _merge(
      {
        cql: "anyText ILIKE ''",
        filterTree: { property: 'anyText', value: '', type: 'ILIKE' },
        associatedFormModel: undefined,
        excludeUnnecessaryAttributes: true,
        count: properties.resultCount,
        start: 1,
        federation: 'enterprise',
        sorts: [
          {
            attribute: 'modified',
            direction: 'descending',
          },
        ],
        result: undefined,
        serverPageIndex: 0,
        type: 'text',
        isLocal: false,
        isOutdated: false,
        'detail-level': undefined,
        spellcheck: true,
      },
      user.getQuerySettings().toJSON()
    )
  },
  resetToDefaults: function(overridenDefaults) {
    const defaults = _.omit(this.defaults(), [
      'isLocal',
      'serverPageIndex',
      'result',
    ])
    this.set(_merge(defaults, overridenDefaults))
    this.trigger('resetToDefaults')
  },
  applyDefaults: function() {
    this.set(_.pick(this.defaults(), ['sorts', 'federation', 'src']))
  },
  revert: function() {
    this.trigger('revert')
  },
  isLocal: function() {
    return this.get('isLocal')
  },
  initialize: function() {
    this.currentIndexForSource = {}

    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
    this.set('id', this.getId())
    this.listenTo(
      user.get('user>preferences'),
      'change:resultCount',
      this.handleChangeResultCount
    )
    this.listenTo(this, 'change:cql', () => this.set('isOutdated', true))
  },
  buildSearchData: function() {
    var data = this.toJSON()

    switch (data.federation) {
      case 'local':
        data.src = [Sources.localCatalog]
        break
      case 'enterprise':
        data.src = _.pluck(Sources.toJSON(), 'id')
        break
      case 'selected':
        // already in correct format
        break
    }

    data.count = user
      .get('user')
      .get('preferences')
      .get('resultCount')

    data.sorts = this.get('sorts')

    return _.pick(
      data,
      'src',
      'start',
      'count',
      'timeout',
      'cql',
      'sorts',
      'id',
      'spellcheck'
    )
  },
  isOutdated() {
    return this.get('isOutdated')
  },
  startTieredSearchIfOutdated(ids) {
    if (this.isOutdated()) {
      this.startTieredSearch(ids)
    }
  },
  startSearchIfOutdated() {
    if (this.isOutdated()) {
      this.startSearch()
    }
  },
  startSearchFromFirstPage: function(options) {
    this.handleChangeResultCount()
    this.startSearch(options)
  },
  startTieredSearch: function(ids) {
    this.set('federation', 'local')
    this.startSearch(undefined, searches => {
      $.when(...searches).then(() => {
        const queryResponse = this.get('result')
        if (queryResponse && queryResponse.isUnmerged()) {
          this.listenToOnce(
            queryResponse,
            'change:merged',
            handleTieredSearchLocalFinish.bind(this, ids)
          )
        } else {
          handleTieredSearchLocalFinish.call(this, ids)
        }
      })
    })
  },
  preQueryPlugin: async function(data) {
    return data
  },
  startSearch: function(options, done) {
    this.set('isOutdated', false)
    if (this.get('cql') === '') {
      return
    }
    options = _.extend(
      {
        limitToDeleted: false,
        limitToHistoric: false,
      },
      options
    )
    this.cancelCurrentSearches()

    var data = Common.duplicate(this.buildSearchData())
    data.batchId = Common.generateUUID()
    if (options.resultCountOnly) {
      data.count = 0
    }
    var sources = data.src
    var initialStatus = sources.map(function(src) {
      return {
        id: src,
      }
    })
    var result
    if (this.get('result') && this.get('result').get('results')) {
      result = this.get('result')
      result.setColor(this.getColor())
      result.setQueryId(this.getId())
      result.set('selectedResultTemplate', this.get('detail-level'))
      result.set('merged', true)
      result.get('queuedResults').reset()
      result.get('results').reset(options.results || [])
      result
        .get('status')
        .reset(
          options.status ? options.status.concat(initialStatus) : initialStatus
        )
    } else {
      result = new QueryResponse({
        queryId: this.getId(),
        color: this.getColor(),
        status: initialStatus,
        selectedResultTemplate: this.get('detail-level'),
      })
      this.set({
        result: result,
      })
    }

    result.set('initiated', Date.now())
    result.set('resultCountOnly', options.resultCountOnly)
    ResultSort.sortResults(this.get('sorts'), result.get('results'))

    if (!properties.isCacheDisabled) {
      sources.unshift('cache')
    }

    var cqlString = data.cql
    if (options.limitToDeleted) {
      cqlString = limitToDeleted(cqlString)
    } else if (options.limitToHistoric) {
      cqlString = limitToHistoric(cqlString)
    }
    var query = this

    const currentSearches = this.preQueryPlugin(
      sources.map(src => ({
        ...data,
        src,
        start: query.getStartIndexForSource(src),
        // since the "cache" source will return all cached results, need to
        // limit the cached results to only those from a selected source
        cql:
          src === 'cache'
            ? CacheSourceSelector.trimCacheSources(cqlString, sources)
            : cqlString,
      }))
    )

    currentSearches.then(currentSearches => {
      if (currentSearches.length === 0) {
        announcement.announce({
          title: 'Search "' + this.get('title') + '" cannot be run.',
          message: properties.i18n['search.sources.selected.none.message'],
          type: 'warn',
        })
        this.currentSearches = []
        return
      }

      this.currentSearches = currentSearches.map(search => {
        return result.fetch({
          customErrorHandling: true,
          data: JSON.stringify(search),
          remove: false,
          dataType: 'json',
          contentType: 'application/json',
          method: 'POST',
          processData: false,
          timeout: properties.timeout,
          success: function(model, response, options) {
            response.options = options
            if (options.resort === true) {
              model.get('results').sort()
            }
          },
          error: function(model, response, options) {
            var srcStatus = result.get('status').get(search.src)
            if (srcStatus) {
              srcStatus.set({
                successful: false,
                pending: false,
              })
            }
            response.options = options
          },
        })
      })
      if (typeof done === 'function') {
        done(this.currentSearches)
      }
    })
  },
  currentSearches: [],
  cancelCurrentSearches: function() {
    this.currentSearches.forEach(request => {
      request.abort('Canceled')
    })
    this.currentSearches = []
  },
  clearResults: function() {
    this.cancelCurrentSearches()
    this.set({
      result: undefined,
    })
  },
  setSources: function(sources) {
    var sourceArr = []
    sources.each(function(src) {
      if (src.get('available') === true) {
        sourceArr.push(src.get('id'))
      }
    })
    if (sourceArr.length > 0) {
      this.set('src', sourceArr.join(','))
    } else {
      this.set('src', '')
    }
  },
  getId: function() {
    if (this.get('id')) {
      return this.get('id')
    } else {
      var id = this._cloneOf || this.id || Common.generateUUID()
      this.set('id')
      return id
    }
  },
  setColor: function(color) {
    this.set('color', color)
  },
  getColor: function() {
    return this.get('color')
  },
  color: function() {
    return this.get('color')
  },
  hasPreviousServerPage: function() {
    return Boolean(
      _.find(this.currentIndexForSource, function(index) {
        return index > 1
      })
    )
  },
  hasNextServerPage: function() {
    var pageSize = user
      .get('user')
      .get('preferences')
      .get('resultCount')
    return Boolean(
      this.get('result')
        .get('status')
        .find(
          function(status) {
            var startingIndex = this.getStartIndexForSource(status.id)
            var total = status.get('hits')
            return total - startingIndex >= pageSize
          }.bind(this)
        )
    )
  },
  getPreviousServerPage: function() {
    this.get('result')
      .getSourceList()
      .forEach(
        function(src) {
          var increment = this.get('result').getLastResultCountForSource(src)
          this.currentIndexForSource[src] = Math.max(
            this.getStartIndexForSource(src) - increment,
            1
          )
        }.bind(this)
      )
    this.set('serverPageIndex', Math.max(0, this.get('serverPageIndex') - 1))
    this.startSearch()
  },
  getNextServerPage: function() {
    this.get('result')
      .getSourceList()
      .forEach(
        function(src) {
          var increment = this.get('result').getLastResultCountForSource(src)
          this.currentIndexForSource[src] =
            this.getStartIndexForSource(src) + increment
        }.bind(this)
      )
    this.set('serverPageIndex', this.get('serverPageIndex') + 1)
    this.startSearch()
  },
  // get the starting offset (beginning of the server page) for the given source
  getStartIndexForSource: function(src) {
    return this.currentIndexForSource[src] || 1
  },
  // if the server page size changes, reset our indices and let them get
  // recalculated on the next fetch
  handleChangeResultCount: function() {
    this.currentIndexForSource = {}
    this.set('serverPageIndex', 0)
    if (this.get('result')) {
      this.get('result').resetResultCountsBySource()
    }
  },
  lengthWithDuplicates(resultsCollection) {
    const lengthWithoutDuplicates = resultsCollection.length
    const numberOfDuplicates = resultsCollection.reduce((count, result) => {
      count += result.duplicates ? result.duplicates.length : 0
      return count
    }, 0)
    return lengthWithoutDuplicates + numberOfDuplicates
  },
  getResultsRangeLabel: function(resultsCollection) {
    var results = resultsCollection.length
    var hits = _.filter(
      this.get('result')
        .get('status')
        .toJSON(),
      status => status.id !== 'cache'
    ).reduce((hits, status) => (status.hits ? hits + status.hits : hits), 0)

    if (results === 0) {
      return '0 results'
    } else if (results > hits) {
      return results + ' results'
    }

    var serverPageSize = user.get('user>preferences>resultCount')
    var startingIndex = this.get('serverPageIndex') * serverPageSize
    var endingIndex =
      startingIndex + this.lengthWithDuplicates(resultsCollection)

    return startingIndex + 1 + '-' + endingIndex + ' of ' + hits
  },
})
module.exports = plugin(Query)
