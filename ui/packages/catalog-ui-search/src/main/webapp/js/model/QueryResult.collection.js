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
const resultSort = require('./ResultSort.js')
const filterUtility = require('../filter.js')
const QueryResultModel = require('./QueryResult.js')
require('backbone-associations')

module.exports = Backbone.Collection.extend({
  model: QueryResultModel,
  amountFiltered: 0,
  generateFilteredVersion(filter) {
    const filteredCollection = new this.constructor()
    filteredCollection.set(this.updateFilteredVersion(filter))
    filteredCollection.amountFiltered = this.amountFiltered
    return filteredCollection
  },
  updateFilteredVersion(filter) {
    this.amountFiltered = 0
    if (filter) {
      return this.filter(result => {
        const passFilter = filterUtility.matchesFilters(
          result.get('metacard').toJSON(),
          filter
        )
        if (!passFilter) {
          this.amountFiltered++
        }
        return passFilter
      })
    } else {
      return this.models
    }
  },
  updateSorting(sorting) {
    if (sorting) {
      resultSort.sortResults(sorting, this)
    }
  },
  collapseDuplicates() {
    const collapsedCollection = new this.constructor()
    collapsedCollection.set(this.models)
    collapsedCollection.amountFiltered = this.amountFiltered
    let endIndex = collapsedCollection.length
    for (let i = 0; i < endIndex; i++) {
      var currentResult = collapsedCollection.models[i]
      var currentChecksum = currentResult
        .get('metacard')
        .get('properties')
        .get('checksum')
      var currentId = currentResult
        .get('metacard')
        .get('properties')
        .get('id')
      const duplicates = collapsedCollection.filter(result => {
        const comparedChecksum = result
          .get('metacard')
          .get('properties')
          .get('checksum')
        const comparedId = result
          .get('metacard')
          .get('properties')
          .get('id')
        return (
          result.id !== currentResult.id &&
          (comparedId === currentId ||
            (Boolean(comparedChecksum) &&
              Boolean(currentChecksum) &&
              comparedChecksum === currentChecksum))
        )
      })
      currentResult.duplicates = undefined
      if (duplicates.length > 0) {
        currentResult.duplicates = duplicates
        collapsedCollection.remove(duplicates)
        endIndex = collapsedCollection.length
      }
    }
    return collapsedCollection
  },
  selectBetween(startIndex, endIndex) {
    const allModels = []
    this.forEach(model => {
      allModels.push(model)
      if (model.duplicates) {
        model.duplicates.forEach(duplicate => {
          allModels.push(duplicate)
        })
      }
    })
    return allModels.slice(startIndex, endIndex)
  },
})
