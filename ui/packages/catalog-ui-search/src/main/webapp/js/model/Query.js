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

const Query = {}

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

const reducer = (state = [{}], action) => {
  switch (action.type) {
    case 'CLEAR_PAGES':
      return [{}]
    case 'NEXT_PAGE':
      return state.concat({})
    case 'PREVIOUS_PAGE':
      return state.slice(0, -1)
    case 'UPDATE_RESULTS':
      const srcs = action.payload.results
        .map(({ src }) => src)
        .reduce((counts, src) => {
          if (counts[src] === undefined) {
            counts[src] = 0
          }
          counts[src] += 1
          return counts
        }, {})

      return state.slice(0, -1).concat(srcs)
    default:
      return state
  }
}

const currentIndexForSource = state => {
  return state.reduce((counts, page) => {
    return Object.keys(page).reduce((counts, src) => {
      const resultCount = page[src]

      if (counts[src] === undefined) {
        counts[src] = 1
      }
      counts[src] += resultCount

      return counts
    }, counts)
  }, {})
}

const serverPageIndex = state => state.length

const previousPage = () => ({ type: 'PREVIOUS_PAGE' })
const nextPage = () => ({ type: 'NEXT_PAGE' })
const clearPages = () => ({ type: 'CLEAR_PAGES' })
const updateResults = payload => ({ type: 'UPDATE_RESULTS', payload })

Query.Model = PartialAssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'result',
      relatedModel: QueryResponse,
      isTransient: true,
    },
  ],
  dispatch(action) {
    this.state = reducer(this.state, action)
  },
  set(data, ...args) {
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
  toJSON(...args) {
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
  defaults() {
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
        serverPageIndex: 1,
        type: 'text',
        isLocal: false,
        isOutdated: false,
        'detail-level': undefined,
        spellcheck: false,
        phonetics: false,
      },
      user.getQuerySettings().toJSON()
    )
  },
  resetToDefaults(overridenDefaults) {
    const defaults = _.omit(this.defaults(), [
      'isLocal',
      'serverPageIndex',
      'result',
    ])
    this.set(_merge(defaults, overridenDefaults))
    this.trigger('resetToDefaults')
  },
  applyDefaults() {
    this.set(_.pick(this.defaults(), ['sorts', 'federation', 'src']))
  },
  revert() {
    this.trigger('revert')
  },
  isLocal() {
    return this.get('isLocal')
  },
  initialize() {
    this.currentIndexForSource = {}
    this.state = [{}]

    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
    this.set('id', this.getId())
    this.listenTo(this, 'change:cql', () => this.set('isOutdated', true))

    const sync = () => {
      this.dispatch(updateResults(this.get('result').toJSON()))
      this.set('serverPageIndex', serverPageIndex(this.state))

      const totalHits = this.get('result')
        .get('status')
        .reduce((total, status) => {
          return total + status.get('hits')
        }, 0)

      this.set('totalHits', totalHits)
    }

    this.listenTo(this, 'change:result', () => {
      if (this.has('result')) {
        this.listenTo(this.get('result'), 'reset:results', sync)
        this.listenTo(this.get('result'), 'change', sync)
      }
    })
  },
  buildSearchData() {
    const data = this.toJSON()

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
      'spellcheck',
      'phonetics'
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
  startSearchFromFirstPage(options) {
    this.dispatch(clearPages())
    this.set('serverPageIndex', serverPageIndex(this.state))
    this.startSearch(options)
  },
  startTieredSearch(ids) {
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
  async preQueryPlugin(data) {
    return data
  },
  startSearch(options, done) {
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

    const data = Common.duplicate(this.buildSearchData())
    data.batchId = Common.generateUUID()
    if (options.resultCountOnly) {
      data.count = 0
    }
    const sources = data.src
    const initialStatus = sources.map(src => ({
      id: src,
    }))
    let result
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
        result,
      })
    }

    result.set('initiated', Date.now())
    result.set('resultCountOnly', options.resultCountOnly)
    ResultSort.sortResults(this.get('sorts'), result.get('results'))

    if (!properties.isCacheDisabled) {
      sources.unshift('cache')
    }

    let cqlString = data.cql
    if (options.limitToDeleted) {
      cqlString = limitToDeleted(cqlString)
    } else if (options.limitToHistoric) {
      cqlString = limitToHistoric(cqlString)
    }
    const query = this

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
          success(model, response, options) {
            response.options = options
            if (options.resort === true) {
              model.get('results').sort()
            }
          },
          error(model, response, options) {
            const srcStatus = result.get('status').get(search.src)
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
  cancelCurrentSearches() {
    this.currentSearches.forEach(request => {
      request.abort('Canceled')
    })
    this.currentSearches = []
  },
  clearResults() {
    this.cancelCurrentSearches()
    this.set({
      result: undefined,
    })
  },
  setSources(sources) {
    const sourceArr = []
    sources.each(src => {
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
  getId() {
    if (this.get('id')) {
      return this.get('id')
    } else {
      const id = this._cloneOf || this.id || Common.generateUUID()
      this.set('id')
      return id
    }
  },
  setColor(color) {
    this.set('color', color)
  },
  getColor() {
    return this.get('color')
  },
  color() {
    return this.get('color')
  },
  hasPreviousServerPage() {
    return serverPageIndex(this.state) > 1
  },
  hasNextServerPage() {
    const pageSize = user
      .get('user')
      .get('preferences')
      .get('resultCount')

    const totalHits = this.get('totalHits')
    const currentPage = serverPageIndex(this.state)
    return currentPage < Math.ceil(totalHits / pageSize)
  },
  getPreviousServerPage() {
    this.dispatch(previousPage())
    this.set('serverPageIndex', serverPageIndex(this.state))
    this.startSearch()
  },
  getNextServerPage() {
    this.dispatch(nextPage())
    this.set('serverPageIndex', serverPageIndex(this.state))
    this.startSearch()
  },
  // get the starting offset (beginning of the server page) for the given source
  getStartIndexForSource(src) {
    return currentIndexForSource(this.state)[src] || 1
  },
  // if the server page size changes, reset our indices and let them get
  // recalculated on the next fetch
  lengthWithDuplicates(resultsCollection) {
    const lengthWithoutDuplicates = resultsCollection.length
    const numberOfDuplicates = resultsCollection.reduce((count, result) => {
      count += result.duplicates ? result.duplicates.length : 0
      return count
    }, 0)
    return lengthWithoutDuplicates + numberOfDuplicates
  },
  getResultsRangeLabel(resultsCollection) {
    const results = resultsCollection.length
    const hits = _.filter(
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

    const serverPageSize = user.get('user>preferences>resultCount')
    const startingIndex = serverPageIndex(this.state) * serverPageSize
    const endingIndex =
      startingIndex + this.lengthWithDuplicates(resultsCollection)

    return startingIndex + 1 + '-' + endingIndex + ' of ' + hits
  },
})
module.exports = plugin(Query)
