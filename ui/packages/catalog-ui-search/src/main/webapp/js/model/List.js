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
const Backbone = require('backbone')
const Query = require('./Query')
const cql = require('../cql.js')
const Common = require('../Common.js')
const _ = require('lodash')
const CQLUtils = require('../CQLUtils.js')
require('backbone-associations')

const iconMap = {
  folder: 'fa fa-folder',
  target: 'fa fa-bullseye',
  video: 'fa fa-file-video-o',
  text: 'fa fa-file-text-o',
  word: 'fa fa-file-word-o',
  powerpoint: 'fa fa-file-powerpoint-o',
  excel: 'fa fa-file-excel-o',
  pdf: 'fa fa-file-pdf-o',
  image: 'fa fa-file-image-o',
  audio: 'fa fa-file-audio-o',
  code: 'fa fa-file-code-o',
  archive: 'fa fa-file-archive-o',
  tasks: 'fa fa-tasks',
}

function getRelevantIcon(iconName) {
  return iconMap[iconName]
}

function generateCql(bookmarks) {
  if (bookmarks.length === 0) {
    return ''
  }
  return cql.write({
    type: 'OR',
    filters: bookmarks.map(id => ({
      type: '=',
      value: id,
      property: '"id"',
    })),
  })
}

function simplifyListFilter(listFilters) {
  if (!listFilters) {
    return listFilters
  }
  let filtersArray = listFilters.filters || [listFilters]
  filtersArray = filtersArray
    .filter(filter => !filter.filters) //Only supports one filter group type, no nested filter groups
    .map(filter => {
      return {
        ...filter,
        value:
          filter.value && filter.value.value
            ? filter.value.value
            : filter.value,
      }
    })
  return {
    type: listFilters.filters ? listFilters.type : 'AND',
    filters: filtersArray,
  }
}

function parseList(data) {
  // for backwards compatability
  if (data['list.cql']) {
    try {
      if (!data['list.filters']) {
        const filterTree = CQLUtils.transformCQLToFilter(data['list.cql'])
        data['list.filters'] = simplifyListFilter(filterTree)
      }
    } catch (e) {
      console.log('Invalid cql: ' + data['list.cql'])
    }
    delete data['list.cql']
  } else if (data['list.filters'] && typeof data['list.filters'] === 'string') {
    try {
      data['list.filters'] = JSON.parse(data['list.filters'])
    } catch (e) {
      data['list.filters'] = undefined
    }
  }
}

module.exports = Backbone.AssociatedModel.extend(
  {
    defaults() {
      return {
        id: Common.generateUUID(),
        title: 'Untitled List',
        'list.filters': undefined, // 'list.cql' is being deprecated in favor of 'list.filters'
        'list.icon': 'folder',
        'list.bookmarks': [],
        query: undefined,
      }
    },
    relations: [
      {
        type: Backbone.One,
        key: 'query',
        relatedModel: Query.Model,
        isTransient: true,
      },
    ],
    initialize() {
      this.set(
        'query',
        new Query.Model({
          cql: generateCql(this.get('list.bookmarks')),
        })
      )
      this.listenTo(
        this,
        'update:list.bookmarks change:list.bookmarks',
        this.updateQuery
      )
    },
    set(data, ...args) {
      if (typeof data === 'object') {
        parseList(data)
      } else if (typeof data === 'string') {
        if (data === 'list.cql') {
          throw new Error(
            '"list.cql" is deprecated, please use "list.filter" instead'
          )
        } else if (data === 'list.filters' && args.length == 1) {
          args = [simplifyListFilter(args[0])]
        }
      }
      return Backbone.AssociatedModel.prototype.set.call(this, data, ...args)
    },
    toJSON(...args) {
      const json = Backbone.AssociatedModel.prototype.toJSON.call(this, ...args)
      if (typeof json['list.filters'] === 'object') {
        json['list.filters'] = JSON.stringify(json['list.filters'])
      }
      return json
    },
    removeBookmarks(bookmarks) {
      if (!Array.isArray(bookmarks)) {
        bookmarks = [bookmarks]
      }
      this.set(
        'list.bookmarks',
        this.get('list.bookmarks').filter(id => bookmarks.indexOf(id) === -1)
      )
    },
    addBookmarks(bookmarks) {
      if (!Array.isArray(bookmarks)) {
        bookmarks = [bookmarks]
      }
      this.set('list.bookmarks', _.union(this.get('list.bookmarks'), bookmarks))
    },
    updateQuery() {
      this.get('query').set('cql', generateCql(this.get('list.bookmarks')))
    },
    getIcon() {
      return getRelevantIcon(this.get('list.icon'))
    },
    isEmpty() {
      return this.get('list.bookmarks').length === 0
    },
  },
  {
    getIconMapping() {
      return iconMap
    },
    getIconMappingForSelect() {
      return _.map(iconMap, (value, key) => {
        return {
          label: key,
          value: key,
          class: value,
        }
      })
    },
  }
)
